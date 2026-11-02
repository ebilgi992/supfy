package app.myspoti.host.android

import app.myspoti.spotify.bridge.BridgeException

/** Document navigation can legitimately reject an injection already queued by onPageFinished. */
internal suspend fun injectPlayerPage(invalidate: () -> Unit, inject: suspend () -> Unit) {
    try {
        inject()
    } catch (error: BridgeException) {
        when (error.message) {
            "PLAYER_ORIGIN_REQUIRED", "PLAYER_PAGE_UNAVAILABLE", WebPlayerSecurityPolicy.SECURE_MESSAGE_CHANNEL_UNAVAILABLE -> invalidate()
            else -> throw error
        }
    }
}

internal object PageInjectionBoundary {
    fun finishedDocument(callbackUrl: String?, currentUrl: String?, currentView: Boolean, failed: Boolean, capabilityError: String?): Boolean =
        currentView && !failed && capabilityError == null && callbackUrl == currentUrl && WebPlayerSecurityPolicy.isTrustedPlayer(currentUrl)

    fun evaluationError(requestedEpoch: Long, currentEpoch: Long, currentUrl: String?, capabilityError: String?, failed: Boolean = false): String? = when {
        requestedEpoch != currentEpoch || failed -> "PLAYER_PAGE_UNAVAILABLE"
        capabilityError != null -> capabilityError
        !WebPlayerSecurityPolicy.isTrustedPlayer(currentUrl) -> "PLAYER_ORIGIN_REQUIRED"
        else -> null
    }
}
