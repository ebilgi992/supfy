package com.supfy.app.web

import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView

class SupfyChromeClient(private val state: WebUiState) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        state.progress = newProgress
    }

    /** Sayfanın yalnızca hata düzeyindeki konsol mesajları tanılama kaydına girer. */
    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
        if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
            state.log(DiagKind.CONSOLE, message.message())
        }
        return true
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
        state.log(
            DiagKind.PERMISSION,
            "${DiagLog.sanitizeUrl(request.origin?.toString())} istedi=${request.resources.size} verildi=${granted.size}",
        )
        if (granted.isNotEmpty()) {
            request.grant(granted)
            state.stats = state.stats.withDrmGranted()
        } else {
            request.deny()
        }
        if (granted.size < request.resources.size) {
            state.stats = state.stats.withDeniedPermission()
        }
    }
}
