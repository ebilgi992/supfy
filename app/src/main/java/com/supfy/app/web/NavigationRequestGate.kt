package com.supfy.app.web

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/** shouldOverrideUrlLoading does not see POST. Enforce main-frame policy before I/O as well. */
object NavigationRequestGate {
    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        if (!request.isForMainFrame || UrlPolicy.decide(request.url.toString()) == UrlPolicy.Decision.LOAD_IN_APP) return null
        return WebResourceResponse("text/plain", "UTF-8", 403, "Navigation refused",
            mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(ByteArray(0)))
    }
}
