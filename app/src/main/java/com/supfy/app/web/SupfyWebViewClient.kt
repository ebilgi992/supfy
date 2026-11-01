package com.supfy.app.web

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
import com.supfy.app.protection.ProtectionWebController
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Güvenli gezinme ve hata yönetimi; isteğe bağlı koruma adaptörüne olay iletir.
 */
class SupfyWebViewClient(
    private val state: WebUiState,
    private val openExternally: (Uri) -> Unit,
    private val onRendererGone: () -> Unit,
    private val protection: ProtectionWebController,
) : WebViewClient() {
    private val pageGuard=WebPageGuard(state)
    private var closed=false
    private val authRedirects=AuthRedirectGate()
    fun userInteraction(view:WebView){authRedirects.arm(view.url,android.os.SystemClock.elapsedRealtime())}
    private var earlyProbe:ScriptHandler?=null
    val earlyProbeInstalled:Boolean get()=earlyProbe!=null

    fun attach(view:WebView) {
        if(WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            val code=view.context.assets.open("login_observer.js").bufferedReader().use {it.readText()}
            earlyProbe=WebViewCompat.addDocumentStartJavaScript(view,code,setOf("https://accounts.spotify.com"))
        }
    }

    fun close() {
        closed=true;authRedirects.clear()
        if(WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))earlyProbe?.remove()
        earlyProbe=null;pageGuard.close()
    }
    fun setForeground(value:Boolean)=pageGuard.setForeground(value)

    fun retryLoginRendering(view:WebView):Boolean = !closed && pageGuard.retryRendering(view)

    private fun configureRendering(view:WebView,url:String?) {
        // Retain the chosen path throughout accounts redirects and form steps.
        if(!AuthenticationPolicy.isAuthenticationUrl(url))state.loginRenderingRetried=false
        WebViewFactory.configureDocumentRendering(view,url,state.loginRenderingRetried)
    }

    fun loadUrl(view:WebView,url:String) {
        if(closed || UrlPolicy.decide(url)!=UrlPolicy.Decision.LOAD_IN_APP)return
        if(AuthenticationPolicy.isAuthenticationUrl(url))state.loginDiagnostics.reset()
        configureRendering(view,url)
        pageGuard.requested(view,url)
        protection.pageStarted(url)
        view.loadUrl(url)
    }

    fun trackRestoredPage(view:WebView) {view.url?.let {configureRendering(view,it);pageGuard.requested(view,it)}}

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        NavigationRequestGate.intercept(request) ?: protection.intercept(request)

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if(closed)return true
        val url = request.url?.toString()
        if(request.isForMainFrame && request.hasGesture())userInteraction(view)
        if (request.isForMainFrame && request.hasGesture()) {
            PlayerNavigation.webUrl(url)?.let { target ->
                if (view.url != target) view.loadUrl(target)
                else state.playbackNotice = true
                return true
            }
        }
        if (!request.isForMainFrame) {
            val allowed = UrlPolicy.allowSubframe(url)
            if (!allowed) state.stats = state.stats.withBlockedNavigation()
            return !allowed
        }
        return when (UrlPolicy.decide(url)) {
            UrlPolicy.Decision.LOAD_IN_APP -> false
            UrlPolicy.Decision.OPEN_EXTERNALLY -> {
                // Yalnızca kullanıcı dokunuşuyla başlayan dış bağlantılar tarayıcıda açılır.
                val allowed=if(state.authenticating && AuthRedirectGate.isProvider(url))
                    authRedirects.consume(url,android.os.SystemClock.elapsedRealtime(),request.hasGesture()) else request.hasGesture()
                if (allowed) {
                    request.url?.let(openExternally)
                } else {
                    state.stats = state.stats.withBlockedNavigation()
                }
                true
            }
            UrlPolicy.Decision.BLOCK -> {
                if (url?.startsWith("spotify:") == true || url?.startsWith("intent:") == true) state.playbackNotice = true
                state.stats = state.stats.withBlockedNavigation()
                true
            }
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        if(closed)return
        if(AuthenticationPolicy.isAuthenticationUrl(url) && !state.authenticating)state.loginDiagnostics.reset()
        if(!AuthenticationPolicy.isAuthenticationUrl(url))authRedirects.clear()
        configureRendering(view,url)
        pageGuard.started(view,url)
        protection.pageStarted(url)
        state.error = null
        state.currentUrl = url
        state.playbackNotice = false
    }

    override fun onPageFinished(view: WebView, url: String?) {
        if(closed)return
        state.currentUrl = url
        state.canGoBack = view.canGoBack()
        CookieManager.getInstance().flush()
        if (state.error == null) protection.pageFinished()
    }

    override fun onPageCommitVisible(view: WebView, url: String?) {
        if(closed)return
        configureRendering(view,url)
        pageGuard.committed(url)
        if (state.error == null) protection.pageFinished()
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        if(closed)return
        configureRendering(view,url)
        pageGuard.historyChanged(view,url)
        // Spotify tek sayfalık uygulama; sayfa içi gezinmeler burada yakalanır.
        state.currentUrl = url
        state.canGoBack = view.canGoBack()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if(closed)return
        if(state.authenticating)state.loginDiagnostics.failure(request.url.toString(),LoginDiagnostics.Failure.NETWORK,error.errorCode)
        if(!request.isForMainFrame)return
        val kind = when (error.errorCode) {
            ERROR_HOST_LOOKUP, ERROR_CONNECT -> PageErrorKind.NO_CONNECTION
            ERROR_TIMEOUT -> PageErrorKind.TIMEOUT
            ERROR_FAILED_SSL_HANDSHAKE -> PageErrorKind.SSL
            else -> PageErrorKind.GENERIC
        }
        pageGuard.failed(request.url?.toString(),kind)
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
        if(closed)return
        if(state.authenticating)state.loginDiagnostics.failure(request.url.toString(),LoginDiagnostics.Failure.HTTP,response.statusCode)
        if(request.isForMainFrame)pageGuard.failed(request.url.toString(),PageErrorKind.HTTP,response.statusCode)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // Sertifika hataları asla atlanmaz.
        handler.cancel()
        if(closed)return
        if(state.authenticating)state.loginDiagnostics.failure(error.url,LoginDiagnostics.Failure.SSL,error.primaryError)
        state.stats = state.stats.withSslRejected()
        if (view.url == null || view.url == error.url) {
            pageGuard.failed(error.url,PageErrorKind.SSL)
        }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        if(closed)return true
        close()
        // Sayfa motoru çöktüyse uygulama çökmeden yeni bir WebView kurulur.
        onRendererGone()
        return true
    }
}
