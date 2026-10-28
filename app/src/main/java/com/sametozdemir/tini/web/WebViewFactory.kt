package com.sametozdemir.tini.web

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.content.ContextCompat
import com.sametozdemir.tini.R

object WebViewFactory {

    @SuppressLint("SetJavaScriptEnabled") // Spotify Web Player JavaScript olmadan çalışmaz.
    fun create(
        context: Context,
        desktopMode: Boolean,
        webViewClient: TiniWebViewClient,
        chromeClient: TiniChromeClient,
    ): WebView = WebView(context).apply {
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

        setBackgroundColor(ContextCompat.getColor(context, R.color.tini_bg))
        this.webViewClient = webViewClient
        this.webChromeClient = chromeClient
        // Not: addJavascriptInterface kullanılmaz; sayfa ile uygulama arasında köprü yoktur.
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
