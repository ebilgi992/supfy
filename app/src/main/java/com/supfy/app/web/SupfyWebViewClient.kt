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

/**
 * Gezinme kuralları ve hata yönetimi. Sayfaya hiçbir JavaScript veya CSS enjekte edilmez,
 * ağ istekleri değiştirilmez veya engellenmez (reklam engelleme yoktur).
 */
class SupfyWebViewClient(
    private val state: WebUiState,
    private val openExternally: (Uri) -> Unit,
    private val onRendererGone: () -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url?.toString()
        if (!request.isForMainFrame) {
            val allowed = UrlPolicy.allowSubframe(url)
            if (!allowed) {
                state.stats = state.stats.withBlockedNavigation()
                state.log(DiagKind.BLOCKED, "alt çerçeve ${DiagLog.sanitizeUrl(url)}")
            }
            return !allowed
        }
        return when (UrlPolicy.decide(url)) {
            UrlPolicy.Decision.LOAD_IN_APP -> false
            UrlPolicy.Decision.OPEN_EXTERNALLY -> {
                // Yalnızca kullanıcı dokunuşuyla başlayan dış bağlantılar tarayıcıda açılır.
                if (request.hasGesture()) {
                    state.log(DiagKind.BLOCKED, "dışarıda açıldı ${DiagLog.sanitizeUrl(url)}")
                    request.url?.let(openExternally)
                } else {
                    // Kullanıcı dokunmadan Spotify dışına yönlendirme: giriş akışı buraya takılırsa
                    // kayıtta görünür.
                    state.stats = state.stats.withBlockedNavigation()
                    state.log(DiagKind.BLOCKED, "otomatik yönlendirme ${DiagLog.sanitizeUrl(url)}")
                }
                true
            }
            UrlPolicy.Decision.BLOCK -> {
                state.stats = state.stats.withBlockedNavigation()
                state.log(DiagKind.BLOCKED, "şema ${DiagLog.sanitizeUrl(url)}")
                true
            }
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        state.error = null
        state.currentUrl = url
        state.log(DiagKind.PAGE_START, DiagLog.sanitizeUrl(url))
    }

    override fun onPageFinished(view: WebView, url: String?) {
        state.currentUrl = url
        state.log(DiagKind.PAGE_DONE, DiagLog.sanitizeUrl(url))
        state.canGoBack = view.canGoBack()
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        // Spotify tek sayfalık uygulama; sayfa içi gezinmeler burada yakalanır.
        state.currentUrl = url
        state.canGoBack = view.canGoBack()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if (!request.isForMainFrame) return
        state.log(DiagKind.NET_ERROR, "${error.errorCode} ${error.description} ${DiagLog.sanitizeUrl(request.url?.toString())}")
        val kind = when (error.errorCode) {
            ERROR_HOST_LOOKUP, ERROR_CONNECT -> PageErrorKind.NO_CONNECTION
            ERROR_TIMEOUT -> PageErrorKind.TIMEOUT
            ERROR_FAILED_SSL_HANDSHAKE -> PageErrorKind.SSL
            else -> PageErrorKind.GENERIC
        }
        state.error = PageError(kind, request.url?.toString())
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        // Ana sayfa veya Spotify kaynaklarındaki HTTP hataları (ör. 403/429) siyah ekranın
        // en olası nedenidir; kaydedilir, sayfanın kendi davranışı değiştirilmez.
        val host = request.url?.host
        if (request.isForMainFrame || UrlPolicy.isTrustedHost(host)) {
            val frame = if (request.isForMainFrame) "ana" else "kaynak"
            state.log(DiagKind.HTTP_ERROR, "${errorResponse.statusCode} $frame ${DiagLog.sanitizeUrl(request.url?.toString())}")
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // Sertifika hataları asla atlanmaz.
        handler.cancel()
        state.stats = state.stats.withSslRejected()
        state.log(DiagKind.SSL, "${error.primaryError} ${DiagLog.sanitizeUrl(error.url)}")
        if (view.url == null || view.url == error.url) {
            state.error = PageError(PageErrorKind.SSL, error.url)
        }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        // Sayfa motoru çöktüyse uygulama çökmeden yeni bir WebView kurulur.
        state.log(DiagKind.RENDERER, if (detail.didCrash()) "çöktü" else "sistem kapattı (bellek)")
        onRendererGone()
        return true
    }
}
