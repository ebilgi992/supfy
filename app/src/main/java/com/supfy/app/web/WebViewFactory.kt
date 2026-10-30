package com.supfy.app.web

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebViewFeature
import com.supfy.app.R

object WebViewFactory {

    @SuppressLint("SetJavaScriptEnabled") // Spotify Web Player JavaScript olmadan çalışmaz.
    fun create(
        context: Context,
        desktopMode: Boolean,
        webViewClient: SupfyWebViewClient,
        chromeClient: SupfyChromeClient,
    ): WebView = WebView(context).apply {
        settings.apply {
            // Gerekli olanlar
            javaScriptEnabled = true
            domStorageEnabled = true
            // Çalma listesinde sonraki parçanın kendiliğinden başlayabilmesi için.
            mediaPlaybackRequiresUserGesture = false

            // Kapalı tutulanlar
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            builtInZoomControls = false
            displayZoomControls = false
            safeBrowsingEnabled = true
        }
        applyDesktopMode(this, desktopMode)

        CookieManager.getInstance().also { cookies ->
            cookies.setAcceptCookie(true)
            // Spotify girişindeki güvenlik doğrulaması (reCAPTCHA) iframe içinde çalışır ve
            // üçüncü taraf çerezi ister; kapalıyken giriş ekranı takılabiliyordu.
            cookies.setAcceptThirdPartyCookies(this, true)
        }

        // Müzik çalarken ekran kapanınca sayfa motoru düşük önceliğe alınıp kapatılmasın.
        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
        setBackgroundColor(ContextCompat.getColor(context, R.color.supfy_bg))
        this.webViewClient = webViewClient
        this.webChromeClient = chromeClient
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
