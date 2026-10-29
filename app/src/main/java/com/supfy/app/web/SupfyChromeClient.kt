package com.supfy.app.web

import android.os.Message
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class SupfyChromeClient(
    private val state: WebUiState,
    private val onPopupChanged: (WebView?) -> Unit = {},
    private val openExternally: (android.net.Uri) -> Unit = {},
) : WebChromeClient() {
    private var popup: WebView? = null

    /** Keeps the actual popup document alive, including POST bodies and window session. */
    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        if (!isUserGesture || UrlPolicy.decide(view.url) != UrlPolicy.Decision.LOAD_IN_APP) return false
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        closePopup()
        val target = WebView(view.context)
        popup = target
        target.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(target, false)
        }
        target.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(wv: WebView, request: WebResourceRequest): android.webkit.WebResourceResponse? =
                NavigationRequestGate.intercept(request)

            override fun shouldOverrideUrlLoading(wv: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return !UrlPolicy.allowSubframe(request.url.toString())
                return when (UrlPolicy.decide(request.url.toString())) {
                    UrlPolicy.Decision.LOAD_IN_APP -> false // Never replay POST as GET.
                    UrlPolicy.Decision.OPEN_EXTERNALLY -> {
                        if (request.hasGesture()) openExternally(request.url)
                        else state.stats = state.stats.withBlockedNavigation()
                        true
                    }
                    UrlPolicy.Decision.BLOCK -> { state.stats = state.stats.withBlockedNavigation(); true }
                }
            }
            override fun onPageFinished(wv: WebView, url: String?) {
                CookieManager.getInstance().flush()
                if (url != "about:blank" && UrlPolicy.decide(url) != UrlPolicy.Decision.LOAD_IN_APP) {
                    state.stats=state.stats.withBlockedNavigation()
                    closePopup()
                    return
                }
                if (UrlPolicy.isTrustedPlayerOrigin(url) && popup === wv) {
                    view.loadUrl(url!!)
                    closePopup()
                }
            }
            override fun onPageStarted(wv: WebView, url: String?, icon: android.graphics.Bitmap?) {
                if (url != "about:blank" && UrlPolicy.decide(url) != UrlPolicy.Decision.LOAD_IN_APP) {
                    wv.stopLoading()
                    state.stats=state.stats.withBlockedNavigation()
                    closePopup()
                }
            }
            override fun onReceivedSslError(wv: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                handler.cancel()
                state.stats = state.stats.withSslRejected()
            }
            override fun onRenderProcessGone(wv: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (popup === wv) closePopup(rendererGone = true)
                return true
            }
        }
        target.webChromeClient = object : WebChromeClient() {
            override fun onCloseWindow(window: WebView) { if (popup === window) closePopup() }
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
        }
        transport.webView = target
        resultMsg.sendToTarget()
        onPopupChanged(target)
        return true
    }

    fun closePopup(rendererGone: Boolean = false) {
        val target = popup ?: return
        popup = null
        onPopupChanged(null)
        (target.parent as? android.view.ViewGroup)?.removeView(target)
        if (!rendererGone) target.stopLoading()
        target.destroy()
    }
    fun close(rendererGone: Boolean = false) = closePopup(rendererGone)

    override fun onProgressChanged(view: WebView, newProgress: Int) { state.progress = newProgress }

    override fun onPermissionRequest(request: PermissionRequest) {
        val trusted = UrlPolicy.isTrustedPlayerOrigin(request.origin?.toString())
        val granted = if (trusted) request.resources.filter {
            it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
        }.toTypedArray() else emptyArray()
        if (granted.isNotEmpty()) {
            request.grant(granted)
            state.stats = state.stats.withDrmGranted()
        } else request.deny()
        if (granted.size < request.resources.size) state.stats = state.stats.withDeniedPermission()
    }
}
