package com.supfy.app.web

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebViewFeature
import com.supfy.app.R

object WebViewFactory {

    /** Authentication has no protected playback. Avoid GPU-layer composition for this document only. */
    fun configureDocumentRendering(webView:WebView,url:String?,normalAuthenticationRendering:Boolean=false) {
        val layer=if(AuthenticationPolicy.isAuthenticationUrl(url) && !normalAuthenticationRendering) android.view.View.LAYER_TYPE_SOFTWARE
            else android.view.View.LAYER_TYPE_NONE
        if(webView.layerType!=layer) {
            webView.setLayerType(layer,null)
            webView.requestLayout();webView.postInvalidateOnAnimation()
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility") // Spotify Web Player JavaScript olmadan çalışmaz.
    fun create(
        context: Context,
        desktopMode: Boolean,
        webViewClient: SupfyWebViewClient,
        chromeClient: SupfyChromeClient,
    ): WebView = WebView(context).apply {
        // Compose otherwise supplies WRAP_CONTENT. Chromium uses that flag to force
        // CSS layout height to zero even when the native view is measured full-size.
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        settings.apply {
            // Gerekli olanlar
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = true

            // Kapalı tutulanlar
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            javaScriptCanOpenWindowsAutomatically = false
            // Android routes target=_blank/window.open into this same top-level view.
            // Do not construct a second WebView/Dialog for the sign-in document.
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            builtInZoomControls = false
            displayZoomControls = false
            safeBrowsingEnabled = true
        }
        applyDesktopMode(this, desktopMode)

        CookieManager.getInstance().also { cookies ->
            cookies.setAcceptCookie(true)
            cookies.setAcceptThirdPartyCookies(this, false)
        }

        setBackgroundColor(ContextCompat.getColor(context, R.color.supfy_bg))
        this.webViewClient = webViewClient
        this.webChromeClient = chromeClient
        webViewClient.attach(this)
        // Observe without consuming: WebView keeps its own touch/accessibility behavior.
        setOnTouchListener { _,event->
            if(event.actionMasked==android.view.MotionEvent.ACTION_UP)webViewClient.userInteraction(this)
            false
        }
        // Not: addJavascriptInterface kullanılmaz; sayfa ile uygulama arasında köprü yoktur.
    }

    /**
     * Spotify'ın Service Worker'ları sayfa dışında çalışır; onlar için de dosya ve
     * içerik erişimini kapatır. İstekleri değiştirmez veya engellemez.
     */
    fun hardenServiceWorkers() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) return
        val settings = ServiceWorkerControllerCompat.getInstance().serviceWorkerWebSettings
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_FILE_ACCESS)) {
            settings.setAllowFileAccess(false)
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS)) {
            settings.setAllowContentAccess(false)
        }
    }

    fun applyDesktopMode(webView: WebView, enabled: Boolean) {
        webView.settings.apply {
            userAgentString = if (enabled) {
                UserAgents.desktopFrom(WebSettings.getDefaultUserAgent(webView.context))
            } else {
                null // WebView'in varsayılan tanımı
            }
            useWideViewPort = enabled
            loadWithOverviewMode = enabled
        }
    }
}
