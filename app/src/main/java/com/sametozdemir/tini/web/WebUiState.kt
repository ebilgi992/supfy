package com.sametozdemir.tini.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PageErrorKind { NO_CONNECTION, TIMEOUT, SSL, GENERIC }

data class PageError(val kind: PageErrorKind, val failingUrl: String?)

/** WebView'den gelen olayların Compose arayüzüne yansıyan durumu. */
class WebUiState {
    var progress by mutableIntStateOf(0)
    var error by mutableStateOf<PageError?>(null)
    var canGoBack by mutableStateOf(false)
    var currentUrl by mutableStateOf<String?>(null)
}
