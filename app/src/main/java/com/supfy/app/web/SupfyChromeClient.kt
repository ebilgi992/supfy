package com.supfy.app.web

import android.os.Message
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class SupfyChromeClient(
    private val state: WebUiState,
    private val onPopupChanged: (WebView?) -> Unit = {},
    private val openExternally: (android.net.Uri) -> Unit = {},
) : WebChromeClient() {
    private var popup: WebView? = null
    val authenticationState=WebUiState()
    private var popupGuard: WebPageGuard?=null

    /** Keeps the actual popup document alive, including POST bodies and window session. */
    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        if (!isUserGesture || UrlPolicy.decide(view.url) != UrlPolicy.Decision.LOAD_IN_APP) return false
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val target=createPopup(view)
        transport.webView=target
        resultMsg.sendToTarget()
        return true
    }

    /** GET entry only. The popup's subsequent form submissions stay in that exact document. */
    fun openAuthentication(view: WebView,url: String): Boolean {
        if(!AuthenticationPolicy.isAuthenticationUrl(url))return false
        createPopup(view).loadUrl(url)
        return true
    }

    fun retryAuthentication() {popup?.loadUrl(PlayerNavigation.LOGIN_URL)}

    private fun createPopup(view: WebView): WebView {
        closePopup()
        val target = WebView(view.context)
        popup = target
        val guard=WebPageGuard(authenticationState)
        popupGuard=guard
        authenticationState.error=null
        authenticationState.loading=true
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
                if(popup!==wv)return true
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
                if(popup!==wv)return
                CookieManager.getInstance().flush()
                if (url != "about:blank" && UrlPolicy.decide(url) != UrlPolicy.Decision.LOAD_IN_APP) {
                    state.stats=state.stats.withBlockedNavigation()
                    closePopup()
                    return
                }
                if (AuthenticationPolicy.isPlayerReturn(url) && popup === wv && authenticationState.error==null) {
                    view.loadUrl(url!!)
                    closePopup()
                }
            }
            override fun onPageCommitVisible(wv: WebView,url: String?) {if(popup===wv)guard.committed(url)}
            override fun onPageStarted(wv: WebView, url: String?, icon: android.graphics.Bitmap?) {
                if(popup!==wv)return
                guard.started(wv,url)
                if (url != "about:blank" && UrlPolicy.decide(url) != UrlPolicy.Decision.LOAD_IN_APP) {
                    wv.stopLoading()
                    state.stats=state.stats.withBlockedNavigation()
                    closePopup()
                }
            }
            override fun onReceivedSslError(wv: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                handler.cancel()
                if(popup!==wv)return
                state.stats = state.stats.withSslRejected()
                guard.failed(error.url,PageErrorKind.SSL)
            }
            override fun onReceivedHttpError(wv: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if(popup!==wv)return
                if(request.isForMainFrame)guard.failed(request.url.toString(),PageErrorKind.HTTP,response.statusCode)
            }
            override fun onReceivedError(wv: WebView,request: WebResourceRequest,error: WebResourceError) {
                if(popup!==wv)return
                if(!request.isForMainFrame)return
                guard.failed(request.url.toString(),when(error.errorCode) {
                    ERROR_HOST_LOOKUP,ERROR_CONNECT->PageErrorKind.NO_CONNECTION
                    ERROR_TIMEOUT->PageErrorKind.TIMEOUT
                    ERROR_FAILED_SSL_HANDSHAKE->PageErrorKind.SSL
                    else->PageErrorKind.GENERIC
                })
            }
            override fun onRenderProcessGone(wv: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (popup === wv) closePopup(rendererGone = true)
                return true
            }
        }
        target.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(wv: WebView,progress: Int) {if(popup===wv)authenticationState.progress=progress}
            override fun onCloseWindow(window: WebView) { if (popup === window) closePopup() }
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
        }
        onPopupChanged(target)
        return target
    }

    fun closePopup(rendererGone: Boolean = false) {
        val target = popup ?: return
        popupGuard?.close();popupGuard=null
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
