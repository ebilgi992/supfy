package com.supfy.app.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PageErrorKind { NO_CONNECTION, TIMEOUT, SSL, GENERIC, HTTP, EMPTY }

data class PageError(val kind: PageErrorKind, val failingUrl: String?, val httpStatus: Int? = null)

enum class PageStage { IDLE, REQUESTED, STARTED, COMMITTED, CHECKING, DRAWING, READY, FAILED, INTERACTION }

/** WebView'den gelen olayların Compose arayüzüne yansıyan durumu. */
class WebUiState {
    val loginDiagnostics=LoginDiagnostics()
    var loginProbePending=false
    var loginRenderingRetried by mutableStateOf(false)
    var authenticating by mutableStateOf(false)
    var pageStage by mutableStateOf(PageStage.IDLE)
    var loading by mutableStateOf(false)
    var playbackNotice by mutableStateOf(false)
    var progress by mutableIntStateOf(0)
    var error by mutableStateOf<PageError?>(null)
    var canGoBack by mutableStateOf(false)
    var currentUrl by mutableStateOf<String?>(null)
    var stats by mutableStateOf(SessionStats())
}
