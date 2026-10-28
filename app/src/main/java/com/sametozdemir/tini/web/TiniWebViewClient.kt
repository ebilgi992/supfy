package com.sametozdemir.tini.web

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Gezinme kuralları ve hata yönetimi. Sayfaya hiçbir JavaScript veya CSS enjekte edilmez,
 * ağ istekleri değiştirilmez veya engellenmez (reklam engelleme yoktur).
 */
class TiniWebViewClient(
    private val state: WebUiState,
    private val openExternally: (Uri) -> Unit,
    private val onRendererGone: () -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url?.toString()
        if (!request.isForMainFrame) {
            return !UrlPolicy.allowSubframe(url)
        }
        return when (UrlPolicy.decide(url)) {
            UrlPolicy.Decision.LOAD_IN_APP -> false
            UrlPolicy.Decision.OPEN_EXTERNALLY -> {
                // Yalnızca kullanıcı dokunuşuyla başlayan dış bağlantılar tarayıcıda açılır.
                if (request.hasGesture()) request.url?.let(openExternally)
                true
            }
            UrlPolicy.Decision.BLOCK -> true
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        state.error = null
        state.currentUrl = url
    }

    override fun onPageFinished(view: WebView, url: String?) {
        state.currentUrl = url
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
        val kind = when (error.errorCode) {
            ERROR_HOST_LOOKUP, ERROR_CONNECT -> PageErrorKind.NO_CONNECTION
            ERROR_TIMEOUT -> PageErrorKind.TIMEOUT
            ERROR_FAILED_SSL_HANDSHAKE -> PageErrorKind.SSL
            else -> PageErrorKind.GENERIC
        }
        state.error = PageError(kind, request.url?.toString())
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // Sertifika hataları asla atlanmaz.
        handler.cancel()
        if (view.url == null || view.url == error.url) {
            state.error = PageError(PageErrorKind.SSL, error.url)
        }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        // Sayfa motoru çöktüyse uygulama çökmeden yeni bir WebView kurulur.
        onRendererGone()
        return true
    }
}
