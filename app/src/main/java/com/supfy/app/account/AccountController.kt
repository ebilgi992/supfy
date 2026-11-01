package com.supfy.app.account

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.supfy.app.web.UrlPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.json.JSONTokener

/** Foreground-only public UI observation. No JS bridge, account API, token or persistent identity store. */
class AccountController(context: Context) {
    private val script=context.assets.open("account_probe.js").bufferedReader().use {it.readText()}
    private val handler=Handler(Looper.getMainLooper())
    private val mutableState=MutableStateFlow(AccountSnapshot())
    val state:StateFlow<AccountSnapshot> = mutableState.asStateFlow()
    private var view:WebView?=null
    private var foreground=true
    private var generation=0L
    private var sequence=0L
    private var pending=false
    private val poll=Runnable {probe()}

    fun attach(webView: WebView) {detach();view=webView;refresh()}
    fun setForeground(value:Boolean) {
        generation++;pending=false;foreground=value;handler.removeCallbacksAndMessages(null)
        if(value)refresh()
    }
    fun markUnknown() {
        generation++;pending=false;handler.removeCallbacksAndMessages(null)
        mutableState.value=AccountSnapshot();schedule()
    }
    fun refresh() {handler.removeCallbacks(poll);if(foreground)probe()}
    private fun schedule() {handler.removeCallbacks(poll);if(view!=null&&foreground)handler.postDelayed(poll,3_000)}
    private fun probe() {
        val web=view?:return
        if(!foreground||pending)return
        if(!UrlPolicy.isTrustedPlayerOrigin(web.url)){markUnknown();schedule();return}
        pending=true
        val token=generation;val request=++sequence
        val timeout=Runnable {
            if(token==generation&&request==sequence&&pending){sequence++;pending=false;markUnknown();schedule()}
        }
        handler.postDelayed(timeout,4_000)
        try {
            web.evaluateJavascript(script) {raw->
                if(token!=generation||request!=sequence||view!==web)return@evaluateJavascript
                handler.removeCallbacks(timeout);pending=false
                if(foreground&&UrlPolicy.isTrustedPlayerOrigin(web.url))mutableState.value=decode(raw)
                else markUnknown()
                schedule()
            }
        } catch(_:RuntimeException) {handler.removeCallbacks(timeout);pending=false;markUnknown();schedule()}
    }

    /** A user-requested action on known Spotify public UI; no arbitrary link/text extraction. */
    fun openAccountMenu(onMissing:()->Unit = {}):Boolean {
        val web=view?:return false
        if(!foreground||!UrlPolicy.isTrustedPlayerOrigin(web.url))return false
        val token=generation
        return try {
            web.evaluateJavascript("""(function(){
                if(location.origin!=='https://open.spotify.com')return false;
                var nodes=document.querySelectorAll('[data-testid="user-widget-link"],[data-testid="user-widget-dropdown-toggle"]');
                for(var i=0;i<Math.min(nodes.length,8);i++){var r=nodes[i].getBoundingClientRect();if(r.width>0&&r.height>0){nodes[i].click();return true}}
                return false;
            })();""") { result->if(token==generation&&view===web&&foreground&&UrlPolicy.isTrustedPlayerOrigin(web.url)&&result!="true")onMissing() }
            true
        }catch(_:RuntimeException){false}
    }
    fun detach() {
        generation++;pending=false;handler.removeCallbacksAndMessages(null);view=null;markUnknown()
    }
    fun close()=detach()

    companion object {
        // Public website journeys. Parent navigates with its existing trusted WebView client.
        const val ACCOUNT_URL="https://www.spotify.com/account/"
        const val SIGNUP_URL="https://www.spotify.com/signup/"
        const val LOGOUT_URL="https://accounts.spotify.com/logout?continue=https%3A%2F%2Fopen.spotify.com%2F"

        fun decode(raw:String?):AccountSnapshot {
            if(raw==null||raw.length>2048)return AccountSnapshot()
            return runCatching {
                val outer=JSONTokener(raw).nextValue()
                val json=when(outer){is String->JSONObject(outer);is JSONObject->outer;else->return AccountSnapshot()}
                if(json.optInt("v")!=1)return AccountSnapshot()
                val signals=json.optInt("signals",-1)
                if(signals !in 0..5)return AccountSnapshot()
                when(json.optInt("status")) {
                    1->if(signals==0)AccountSnapshot(AccountStatus.GUEST)else AccountSnapshot()
                    2->if(signals>=2)AccountSnapshot(AccountStatus.AUTHENTICATED,AccountNames.clean(json.optString("name")))else AccountSnapshot()
                    else->AccountSnapshot()
                }
            }.getOrDefault(AccountSnapshot())
        }
    }
}
