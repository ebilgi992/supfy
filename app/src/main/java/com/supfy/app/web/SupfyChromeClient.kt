package com.supfy.app.web

import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.ConsoleMessage

/** One native WebView/window. Window requests are handled by WebView's single-window mode. */
class SupfyChromeClient(private val state: WebUiState) : WebChromeClient() {
    private var closed=false

    fun close() {closed=true}

    override fun onConsoleMessage(message:ConsoleMessage):Boolean {
        // Count only. Do not read, retain or forward console text/source URLs.
        if(!closed && state.authenticating && message.messageLevel()==ConsoleMessage.MessageLevel.ERROR)state.loginDiagnostics.consoleError()
        return true
    }

    override fun onProgressChanged(view: WebView,newProgress: Int) {
        if(!closed)state.progress=newProgress
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        if(closed) {request.deny();return}
        val trusted=UrlPolicy.isTrustedPlayerOrigin(request.origin?.toString())
        val granted=if(trusted)request.resources.filter {
            it==PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
        }.toTypedArray() else emptyArray()
        if(granted.isNotEmpty()) {
            request.grant(granted)
            state.stats=state.stats.withDrmGranted()
        } else request.deny()
        if(granted.size<request.resources.size)state.stats=state.stats.withDeniedPermission()
    }
}
