package com.supfy.app.protection

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.*
import com.supfy.app.web.UrlPolicy
import java.io.ByteArrayInputStream
import org.json.JSONObject
import org.json.JSONTokener

/** Main-frame only, no native JS bridge. Feature guarded per-WebView audio and early script. */
class ProtectionWebController(context: Context, val engine: ProtectionEngine,
    private val onAudioRecoveryRequired: () -> Unit = {}) {
    private val script = context.assets.open("protection.js").bufferedReader().use { it.readText() }
    private val handler = Handler(Looper.getMainLooper())
    private var view: WebView? = null
    private var generation = 0
    private var foreground = true
    private var pending = false
    private var workerInstalled = false
    private var needsApply = true
    private var options = ProtectionOptions()
    private var documentScript: ScriptHandler? = null
    private var nativeAudio: AudioRecovery? = null
    private var nativeFailed = false
    private var restoreAttempts = 0
    private var restoreScheduled = false
    private var recoveryRequested = false
    private var restoreOwner: WebView? = null
    private val restoreTask = Runnable {
        restoreScheduled=false
        if (view === restoreOwner && view != null) {
            restoreAttempts++
            if (restoreAttempts <= 3) restoreNative()
            else if (!recoveryRequested) { recoveryRequested=true; onAudioRecoveryRequired() }
            else {
                engine.stopForAudioFailure(); handler.removeCallbacks(poll)
                view?.let(::stopScript); removeDocumentScript()
            }
        }
    }
    val serviceWorkerFiltering get() = workerInstalled
    val documentStartSupported get() = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
    private val poll = Runnable { synchronizePage() }

    fun installServiceWorkerFilter() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST)) return
        ServiceWorkerControllerCompat.getInstance().setServiceWorkerClient(object : ServiceWorkerClientCompat() {
            override fun shouldInterceptRequest(request: WebResourceRequest) = intercept(request)
        })
        workerInstalled = true
    }
    fun attach(webView: WebView) {
        view = webView
        nativeFailed=false; restoreAttempts=0; restoreScheduled=false
        val supported = WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)
        engine.nativeAudioSupport(supported)
        if (supported) nativeAudio = AudioRecovery(object : AudioPort {
            override var muted: Boolean
                get() {
                    return if (WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) {
                        WebViewCompat.isAudioMuted(webView)
                    } else throw UnsupportedOperationException()
                }
                set(value) {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) {
                        WebViewCompat.setAudioMuted(webView, value)
                    } else throw UnsupportedOperationException()
                }
        })
        installDocumentScript()
    }
    private fun config(active: Boolean) = JSONObject().put("enabled",active).put("muteAds",options.muteAds)
        .put("hideBanners",options.hideBanners).put("hidePromos",options.hidePromos)
        .put("nativeAudio",nativeAudio != null && !nativeFailed)

    private fun installDocumentScript() {
        removeDocumentScript()
        val wv = view ?: return
        if (options.enabled && engine.state.value.status != EngineStatus.ERROR &&
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            val boot = "$script\n;window.__supfyProtection && window.__supfyProtection.configure(${config(true)});"
            documentScript = WebViewCompat.addDocumentStartJavaScript(wv,boot,setOf("https://open.spotify.com"))
        }
    }
    fun configure(next: ProtectionOptions) {
        generation++; pending = false; handler.removeCallbacksAndMessages(null)
        restoreScheduled=false
        options = next
        if (!next.enabled) {
            restoreNative(); recoveryRequested=false
            if (nativeAudio?.restorationPending != true) {nativeFailed=false;nativeAudio?.reset()}
        }
        if (!next.muteAds) restoreNative()
        engine.configure(next)
        needsApply = true
        installDocumentScript()
        synchronizePage()
    }
    fun pageStarted(url: String?) {
        generation++; pending = false; needsApply = true
        handler.removeCallbacksAndMessages(null)
        restoreScheduled=false; restoreNative()
        engine.pageStarted(url)
    }
    fun pageFinished() { synchronizePage() }
    fun setForeground(value: Boolean) {
        foreground = value
        handler.removeCallbacks(poll)
        synchronizePage()
    }
    private fun synchronizePage() {
        val wv = view ?: return
        if (!UrlPolicy.isTrustedPlayerOrigin(wv.url) || pending) return
        val active = options.enabled && engine.state.value.status != EngineStatus.ERROR
        val token = generation
        val apply = needsApply
        val code = if (apply) "$script\n;" else ""
        val configure = if (apply) "p.configure(${config(active)});" else ""
        pending = true
        val timeout = Runnable { if (token == generation && pending) { pending = false; failOpen(wv) } }
        handler.postDelayed(timeout,8_000)
        wv.evaluateJavascript("$code(function(){const p=window.__supfyProtection;if(!p)return null;${configure}return JSON.stringify(p.snapshot());})();") { raw ->
            handler.removeCallbacks(timeout)
            if (token != generation || view !== wv || !UrlPolicy.isTrustedPlayerOrigin(wv.url)) return@evaluateJavascript
            pending = false; needsApply = false
            if (active) {
                runCatching { decodeSnapshot(raw) }.onSuccess { snapshot ->
                    engine.acceptSnapshot(snapshot)
                    if (nativeFailed || !options.muteAds || snapshot.phase != "ad" || !snapshot.ready) restoreNative()
                    else if (engine.state.value.status != EngineStatus.ERROR && snapshot.evidence >= 3) {
                        handleAudioResult(nativeAudio?.apply(true) ?: AudioResult.NONE)
                    }
                }.onFailure { failOpen(wv) }
                if (engine.state.value.status == EngineStatus.ERROR) {
                    restoreNative(); stopScript(wv)
                    removeDocumentScript()
                }
            }
            if (active && engine.state.value.status != EngineStatus.ERROR) {
                handler.removeCallbacks(poll)
                handler.postDelayed(poll,if (foreground) 1_000 else 5_000)
            }
        }
    }
    private fun decodeSnapshot(raw: String): PageSnapshot {
        check(raw.length <= 4096)
        val value = JSONTokener(raw).nextValue() as? String ?: error("No snapshot")
        val json = JSONObject(value)
        check(json.getInt("version") == 2)
        check(json.getBoolean("enabled") || json.getString("phase") == "error")
        return PageSnapshot(detected=json.getInt("detected"),muted=json.getInt("muted"),banners=json.getInt("banners"),
            promos=json.getInt("promos"),restored=json.getInt("restored"),errors=json.getInt("errors"),
            phase=json.getString("phase"),eme=json.getBoolean("eme"),playing=json.getBoolean("playing"),
            ready=json.getBoolean("ready"),audioReady=json.getBoolean("audioReady"),
            audioError=json.getBoolean("audioError"),visualError=json.getBoolean("visualError"),
            evidence=json.getInt("evidence"),resumed=json.getInt("resumed"))
    }
    private fun handleAudioResult(result: AudioResult, retry: Boolean = true) {
        when (result) {
            AudioResult.MUTED -> engine.nativeAudioChanged(true)
            AudioResult.RESTORED -> {
                engine.nativeAudioChanged(false); restoreAttempts=0
                handler.removeCallbacks(restoreTask); restoreScheduled=false;restoreOwner=null
            }
            AudioResult.FAILED -> {
                if (!nativeFailed) {
                    nativeFailed=true; needsApply=true; engine.nativeAudioFailed(); installDocumentScript()
                }
                if (retry && nativeAudio?.restorationPending == true && !restoreScheduled) {
                    restoreScheduled=true;restoreOwner=view
                    handler.postDelayed(restoreTask,500)
                }
            }
            AudioResult.NONE -> Unit
        }
    }
    private fun restoreNative(retry: Boolean = true) {
        handleAudioResult(nativeAudio?.apply(false) ?: AudioResult.NONE,retry)
    }
    private fun failOpen(wv: WebView) {
        engine.fail(); handler.removeCallbacksAndMessages(null);restoreScheduled=false;restoreNative()
        removeDocumentScript()
        stopScript(wv)
    }
    private fun stopScript(wv: WebView) {
        if (UrlPolicy.isTrustedPlayerOrigin(wv.url)) wv.evaluateJavascript("window.__supfyProtection && window.__supfyProtection.stop();",null)
    }
    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        if (!engine.shouldBlock(request.url.toString(),request.method,request.isForMainFrame)) return null
        val response=WebResourceResponse("text/plain","UTF-8",403,"Blocked by Supfy",
            mapOf("Cache-Control" to "no-store"),ByteArrayInputStream(ByteArray(0)))
        engine.requestBlocked();return response
    }
    fun detach() {
        generation++;pending=false;handler.removeCallbacksAndMessages(null)
        restoreScheduled=false;restoreNative(false);nativeAudio=null
        removeDocumentScript()
        view?.let(::stopScript);view=null;restoreOwner=null;engine.pageStarted(null)
    }
    /** Renderer is already gone: discard ownership, never call JS/audio APIs on that WebView. */
    fun rendererGone() {
        generation++;pending=false;handler.removeCallbacksAndMessages(null)
        nativeAudio=null;documentScript=null;view=null;restoreOwner=null;restoreScheduled=false
        engine.pageStarted(null)
        engine.rendererFailed()
    }
    fun close() {
        detach()
        if (workerInstalled && WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE))
            ServiceWorkerControllerCompat.getInstance().setServiceWorkerClient(null)
        workerInstalled=false
    }
    private fun removeDocumentScript() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) documentScript?.remove()
        documentScript=null
    }
}
