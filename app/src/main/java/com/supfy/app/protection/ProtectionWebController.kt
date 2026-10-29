package com.supfy.app.protection

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebViewFeature
import com.supfy.app.web.UrlPolicy
import java.io.ByteArrayInputStream
import org.json.JSONObject
import org.json.JSONTokener

/** Owns only this Activity's WebView; no native JS bridge or global audio changes. */
class ProtectionWebController(context: Context, val engine: ProtectionEngine) {
    private val script = context.assets.open("protection.js").bufferedReader().use { it.readText() }
    private val handler = Handler(Looper.getMainLooper())
    private var view: WebView? = null
    private var generation = 0
    private var foreground = true
    private var pending = false
    private var workerInstalled = false
    private var options = ProtectionOptions()
    val serviceWorkerFiltering: Boolean
        get() = workerInstalled

    private val poll = object : Runnable {
        override fun run() { synchronizePage() }
    }

    fun installServiceWorkerFilter() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST)) return
        ServiceWorkerControllerCompat.getInstance().setServiceWorkerClient(object : ServiceWorkerClientCompat() {
            override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? = intercept(request)
        })
        workerInstalled = true
    }

    fun attach(webView: WebView) { view = webView }

    fun configure(next: ProtectionOptions) {
        generation++
        pending = false
        handler.removeCallbacksAndMessages(null)
        options = next
        engine.configure(next)
        synchronizePage()
    }

    fun pageStarted(url: String?) {
        generation++
        pending = false
        handler.removeCallbacksAndMessages(null)
        engine.pageStarted(url)
    }

    fun pageFinished() { synchronizePage() }

    fun setForeground(value: Boolean) {
        foreground = value
        handler.removeCallbacks(poll)
        // Background uses a slower heartbeat; the JS lease fails open if the renderer freezes.
        synchronizePage()
    }

    private fun synchronizePage() {
        val wv = view ?: return
        if (!UrlPolicy.isTrustedPlayerOrigin(wv.url) || pending) return
        val active = options.enabled && engine.state.value.status != EngineStatus.ERROR
        val token = generation
        val config = JSONObject().put("enabled", active).put("muteAds", options.muteAds)
            .put("hideBanners", options.hideBanners).put("hidePromos", options.hidePromos)
        pending = true
        val timeout = Runnable {
            if (token == generation && pending) { pending = false; failOpen(wv) }
        }
        handler.postDelayed(timeout, 8_000)
        wv.evaluateJavascript(
            "$script\n;(function(){const p=window.__supfyProtection;if(!p)return null;p.configure($config);return JSON.stringify(p.snapshot());})();",
        ) { raw ->
            handler.removeCallbacks(timeout)
            if (token != generation || view !== wv || !UrlPolicy.isTrustedPlayerOrigin(wv.url)) return@evaluateJavascript
            pending = false
            if (active) {
                runCatching { decodeSnapshot(raw) }.onSuccess(engine::acceptSnapshot).onFailure { failOpen(wv) }
                if (engine.state.value.status == EngineStatus.ERROR) stopScript(wv)
            }
            if (active && engine.state.value.status != EngineStatus.ERROR) {
                handler.removeCallbacks(poll)
                handler.postDelayed(poll, if (foreground) 3_000 else 10_000)
            }
        }
    }

    private fun decodeSnapshot(raw: String): PageSnapshot {
        val value = JSONTokener(raw).nextValue() as? String ?: error("No snapshot")
        val json = JSONObject(value)
        check(json.getInt("version") == 1)
        return PageSnapshot(
            detected = json.getInt("detected"), muted = json.getInt("muted"), banners = json.getInt("banners"),
            promos = json.getInt("promos"), restored = json.getInt("restored"), errors = json.getInt("errors"),
            phase = json.getString("phase"), eme = json.getBoolean("eme"), playing = json.getBoolean("playing"),
        )
    }

    private fun failOpen(wv: WebView) {
        engine.fail()
        handler.removeCallbacksAndMessages(null)
        stopScript(wv)
    }

    private fun stopScript(wv: WebView) {
        if (UrlPolicy.isTrustedPlayerOrigin(wv.url)) {
            wv.evaluateJavascript("window.__supfyProtection && window.__supfyProtection.stop();", null)
        }
    }

    /** Called on WebView/Service Worker IO thread: bounded pure URL checks only. */
    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        if (!engine.shouldBlock(request.url.toString(), request.method, request.isForMainFrame)) return null
        val response = WebResourceResponse("text/plain", "UTF-8", 403, "Blocked by Supfy",
            mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(ByteArray(0)))
        engine.requestBlocked()
        return response
    }

    fun detach() {
        generation++
        pending = false
        handler.removeCallbacksAndMessages(null)
        view?.let(::stopScript)
        view = null
        engine.pageStarted(null)
    }

    fun close() {
        detach()
        if (workerInstalled && WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            ServiceWorkerControllerCompat.getInstance().setServiceWorkerClient(null)
        }
        workerInstalled = false
    }
}
