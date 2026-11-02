package app.myspoti.host.android

import java.net.URI

/** Android-independent boundary rules for the official player and native sign-in surface. */
object WebPlayerSecurityPolicy {
    const val PLAYER_ORIGIN = "https://open.spotify.com"
    const val SECURE_MESSAGE_CHANNEL_UNAVAILABLE = "SECURE_MESSAGE_CHANNEL_UNAVAILABLE"
    private const val PROTECTED_MEDIA = "android.webkit.resource.PROTECTED_MEDIA_ID"
    private val spotifyDocuments = setOf("open.spotify.com", "accounts.spotify.com", "challenge.spotify.com", "www.spotify.com", "spotify.com", "consent.spotify.com")
    private val providers = setOf("accounts.google.com", "facebook.com", "www.facebook.com", "m.facebook.com", "appleid.apple.com")
    private val loginPath = Regex("^/(?:intl-[a-z]{2,3}(?:-[a-zA-Z]{2,4})?/)?(?:login|signup|logout)(?:/.*)?$")

    fun isTrustedPlayer(url: String?): Boolean = secureUri(url)?.host?.equals("open.spotify.com", true) == true

    fun acceptMessage(origin: String?, isMainFrame: Boolean): Boolean = isMainFrame && isTrustedPlayer(origin)

    fun allowedPermissions(origin: String?, resources: List<String>): List<String> =
        if (isTrustedPlayer(origin)) resources.filter { it == PROTECTED_MEDIA } else emptyList()

    /** Provider redirects remain possible only within a login explicitly started by the user. */
    fun allowNavigation(url: String?, isMainFrame: Boolean, providerFlow: Boolean): Boolean {
        val uri = secureUri(url) ?: return false
        if (!isMainFrame) return true
        val host = uri.host.lowercase()
        return host in spotifyDocuments || (providerFlow && host in providers)
    }

    fun isProvider(url: String?): Boolean = secureUri(url)?.host?.lowercase() in providers

    fun isLoginPage(url: String?): Boolean {
        val uri = secureUri(url) ?: return false
        val host = uri.host.lowercase()
        return host in providers || host in setOf("accounts.spotify.com", "challenge.spotify.com") ||
            (host in setOf("open.spotify.com", "www.spotify.com", "spotify.com") && loginPath.matches(uri.path.orEmpty()))
    }

    private fun secureUri(value: String?): URI? {
        if (value.isNullOrBlank()) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        return uri.takeIf {
            it.scheme.equals("https", true) && !it.host.isNullOrEmpty() &&
                it.rawUserInfo == null && it.port in listOf(-1, 443)
        }
    }
}

internal class NativeRequestEpoch {
    private val epoch = java.util.concurrent.atomic.AtomicLong()
    fun current(): Long = epoch.get()
    fun isCurrent(epoch: Long): Boolean = this.epoch.get() == epoch
    fun invalidate() { epoch.incrementAndGet() }
}
