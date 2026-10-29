package com.supfy.app.web

import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.os.Message
import android.os.Handler
import android.os.Looper

class SupfyChromeClient(private val state: WebUiState) : WebChromeClient() {

    private val pendingWindows = mutableSetOf<WebView>()
    private val handler = Handler(Looper.getMainLooper())

    /** Resolve a user initiated Spotify target into the existing, cookie sharing WebView. */
    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        if (!isUserGesture || UrlPolicy.decide(view.url) != UrlPolicy.Decision.LOAD_IN_APP) return false
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val target = WebView(view.context)
        pendingWindows.add(target)
        target.settings.apply {
            javaScriptEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        var handled = false
        fun route(url: String?) {
            if (handled || url == "about:blank") return
            handled = true
            if (UrlPolicy.decide(url) == UrlPolicy.Decision.LOAD_IN_APP) view.loadUrl(url!!)
            else state.stats = state.stats.withBlockedNavigation()
            handler.post { if (pendingWindows.remove(target)) target.destroy() }
        }
        target.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(wv: WebView, request: WebResourceRequest): Boolean {
                route(request.url.toString()); return true
            }
            override fun onPageStarted(wv: WebView, url: String?, icon: android.graphics.Bitmap?) {
                if (url == "about:blank") return
                wv.stopLoading(); route(url)
            }
            override fun onReceivedSslError(wv: WebView, h: android.webkit.SslErrorHandler, e: android.net.http.SslError) { h.cancel() }
            override fun onRenderProcessGone(wv: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                handled = true
                if (pendingWindows.remove(wv)) wv.destroy()
                return true
            }
        }
        transport.webView = target
        resultMsg.sendToTarget()
        handler.postDelayed({ if (pendingWindows.remove(target)) target.destroy() }, 10_000)
        return true
    }

    fun close() {
        handler.removeCallbacksAndMessages(null)
        pendingWindows.forEach { it.destroy() }
        pendingWindows.clear()
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        state.progress = newProgress
    }

    /**
     * Web sayfası izin kapısı. Yalnızca open.spotify.com'un korumalı medya (Widevine/EME)
     * isteği kabul edilir; bu, DRM'yi atlatmak değil, tarayıcıların da sorduğu standart
     * "korumalı içeriği oynat" iznidir. Mikrofon, kamera vb. her zaman reddedilir.
     */
    override fun onPermissionRequest(request: PermissionRequest) {
        val trusted = UrlPolicy.isTrustedPlayerOrigin(request.origin?.toString())
        val granted = if (trusted) {
            request.resources
                .filter { it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID }
                .toTypedArray()
        } else {
            emptyArray()
        }
        if (granted.isNotEmpty()) {
            request.grant(granted)
            state.stats = state.stats.withDrmGranted()
        } else {
            request.deny()
        }
        if (granted.size < request.resources.size) {
            state.stats = state.stats.withDeniedPermission()
        }
    }
}
