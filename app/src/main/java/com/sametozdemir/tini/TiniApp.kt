package com.sametozdemir.tini

import android.app.Application
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class TiniApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Safe Browsing listelerini erkenden hazırla (destekleniyorsa).
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(this) { }
        }
    }
}
