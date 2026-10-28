package com.sametozdemir.tini.web

import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView

class TiniChromeClient(private val state: WebUiState) : WebChromeClient() {

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
        if (granted.isNotEmpty()) request.grant(granted) else request.deny()
    }
}
