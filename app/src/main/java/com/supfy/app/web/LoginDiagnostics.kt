package com.supfy.app.web

import org.json.JSONObject
import org.json.JSONTokener
import java.net.URI

/** Fixed numeric schema only. Never retains a JavaScript response, URL or message string. */
data class LoginProbeSnapshot(val result:Result, val ready:Boolean=false, val metrics:Map<String,Int> = emptyMap(),val contentPresent:Boolean=ready) {
    enum class Result { NOT_RUN, VALID, NULL_RESULT, MALFORMED, SCRIPT_EXCEPTION }
    companion object {
        val metricKeys=listOf("state","origin","route","body","bodyChildren","rootChildren","width","height",
            "visualWidth","visualHeight","scrollX","scrollY","inputs","iframes","scripts","styles","headings",
            "nextData","nextChildren","early","maxInputs","removedInputs","errors","rejections","csp",
            "candidates","enabled","size","viewport","style","hit","disabled","readonly","hidden","ariaHidden",
            "displayNone","visibilityHidden","opacityZero","contentHidden","hitNull","hitOther",
            "centerOther","hitHtml","hitBody","hitAncestor","hitIframe","hitDialog","hitUnrelated","pointerNone","inert","clipped",
            "dpr1000","visualScale1000","visualOffsetX","visualOffsetY",
            "documentVisible","documentFocused","htmlWidth","htmlHeight","bodyWidth","bodyHeight","mainWidth","mainHeight",
            "fieldWidth","fieldHeight","fieldLeft","fieldTop","rootPseudo","modal","animations","visibilityApi",
            "transforms","backfaceHidden","filters","perspectives","overflowClips","contentAuto","checkInvisible")

        fun decode(raw:String?):LoginProbeSnapshot {
            if(raw==null || raw=="null")return LoginProbeSnapshot(Result.NULL_RESULT)
            if(raw.length>8192)return LoginProbeSnapshot(Result.MALFORMED)
            return runCatching {
                val outer=JSONTokener(raw).nextValue()
                val json=when(outer) {is String->JSONObject(outer);is JSONObject->outer;else->error("schema")}
                check(json.opt("v")==1)
                check(json.opt("ready") is Boolean)
                check(!json.has("contentPresent") || json.opt("contentPresent") is Boolean)
                val code=json.opt("code") as? Number ?: error("schema")
                check(code.toDouble()==0.0 || code.toDouble()==1.0)
                val source=json.optJSONObject("metrics")?:error("schema")
                val metrics=metricKeys.mapNotNull {key->
                    val n=(source.opt(key) as? Number)?.toDouble()
                    if(n!=null && n.isFinite() && n%1.0==0.0)key to n.coerceIn(0.0,32768.0).toInt() else null
                }.toMap()
                LoginProbeSnapshot(if(code.toInt()==1)Result.SCRIPT_EXCEPTION else Result.VALID,
                    code.toInt()==0 && json.getBoolean("ready"),metrics,
                    code.toInt()==0 && json.optBoolean("contentPresent",json.getBoolean("ready")))
            }.getOrElse {LoginProbeSnapshot(Result.MALFORMED)}
        }
    }
}

/** Explicitly user-copied diagnostics. All strings below are application constants. */
class LoginDiagnostics {
    enum class Host { ACCOUNTS, ACCOUNT_CDN, PLAYER, SPOTIFY, CHALLENGE, OTHER }
    enum class Failure { NETWORK, HTTP, SSL }
    private data class Event(val host:Host,val failure:Failure,val code:Int)
    private var first:LoginProbeSnapshot?=null
    private var last=LoginProbeSnapshot(LoginProbeSnapshot.Result.NOT_RUN)
    private var samples=0
    private var consoleErrors=0
    private var overflow=0
    private val events=linkedMapOf<Event,Int>()

    @Synchronized fun reset() {beginDocument();consoleErrors=0;overflow=0;events.clear()}
    @Synchronized fun beginDocument() {first=null;last=LoginProbeSnapshot(LoginProbeSnapshot.Result.NOT_RUN);samples=0}
    @Synchronized fun observed(snapshot:LoginProbeSnapshot) {
        if(first==null)first=snapshot
        last=snapshot;samples=(samples+1).coerceAtMost(32768)
    }
    @Synchronized fun consoleError() {consoleErrors=(consoleErrors+1).coerceAtMost(32768)}
    @Synchronized fun failure(url:String?,type:Failure,code:Int) {
        val event=Event(host(url),type,code.coerceIn(-100,599))
        if(event in events || events.size<12)events[event]=((events[event]?:0)+1).coerceAtMost(32768)
        else overflow=(overflow+1).coerceAtMost(32768)
    }

    @Synchronized fun report(native:Map<String,Int>,providerVersion:String?):String=buildString {
        appendLine("Supfy login diagnostics v1")
        val provider=providerVersion?.takeIf {it.length<=64 && it.matches(Regex("[0-9]+(?:\\.[0-9]+){1,4}"))}?:"UNKNOWN"
        appendLine("webView=$provider")
        for(key in nativeKeys)native[key]?.let {appendLine("$key=${it.coerceIn(-1,32768)}")}
        appendLine("samples=$samples consoleErrors=$consoleErrors overflow=$overflow")
        fun snapshot(name:String,value:LoginProbeSnapshot) {
            appendLine("$name.result=${value.result.name} ready=${if(value.ready)1 else 0} contentPresent=${if(value.contentPresent)1 else 0}")
            for(key in LoginProbeSnapshot.metricKeys)value.metrics[key]?.let {appendLine("$name.$key=$it")}
        }
        first?.let {snapshot("first",it)}
        snapshot("last",last)
        for((event,count) in events)appendLine("resource=${event.host.name}/${event.failure.name}/${event.code}/$count")
    }

    companion object {
        val nativeKeys=listOf("version","sdk","stage","error","nativeOrigin","nativeRoute","width","height","attached",
            "visibility","javascript","domStorage","thirdPartyCookies","desktop","layer","earlySupported","cacheMode","probePending","renderRetry","shown","windowVisibility","windowFocus",
            "hardwareAccelerated","wrapContentHeight","nativeVisibleWidth","nativeVisibleHeight","alpha1000")
        fun route(url:String?):Int {
            val path=runCatching {URI(url.orEmpty()).path.orEmpty()}.getOrDefault("")
            return when {
                Regex("(?:^|/)login(?:/|$)").containsMatchIn(path)->1
                Regex("(?:^|/)authorize(?:/|$)").containsMatchIn(path)->2
                Regex("(?:^|/)challenge(?:/|$)").containsMatchIn(path)->3
                else->0
            }
        }
        fun host(url:String?):Host {
            val name=runCatching {URI(url.orEmpty()).host?.lowercase()?.trimEnd('.')}.getOrNull()
            return when {
                name=="accounts.spotify.com"->Host.ACCOUNTS
                name=="accounts.scdn.co"->Host.ACCOUNT_CDN
                name=="open.spotify.com"->Host.PLAYER
                name=="spotify.com" || name?.endsWith(".spotify.com")==true || name=="scdn.co" || name?.endsWith(".scdn.co")==true->Host.SPOTIFY
                name=="google.com" || name?.endsWith(".google.com")==true || name=="recaptcha.net" || name?.endsWith(".recaptcha.net")==true->Host.CHALLENGE
                else->Host.OTHER
            }
        }
    }
}
