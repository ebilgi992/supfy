package com.sametozdemir.supfy.web

import java.net.URI

/**
 * Hangi adresin uygulama içinde açılacağına karar veren saf (Android'den bağımsız) kural seti.
 * Birim testleriyle doğrulanır.
 */
object UrlPolicy {

    const val HOME_URL = "https://open.spotify.com/"

    /** Yalnızca bu alan adları ve alt alan adları uygulama içinde açılır. */
    private val trustedSuffixes = listOf("spotify.com", "scdn.co", "spotifycdn.com")

    /** DRM izni yalnızca bu kaynağa verilir. */
    private const val PLAYER_HOST = "open.spotify.com"

    enum class Decision { LOAD_IN_APP, OPEN_EXTERNALLY, BLOCK }

    fun isTrustedHost(host: String?): Boolean {
        val h = host?.lowercase()?.trimEnd('.') ?: return false
        if (h.isEmpty()) return false
        return trustedSuffixes.any { h == it || h.endsWith(".$it") }
    }

    /** Ana çerçevedeki gezinme isteği için karar. */
    fun decide(url: String?): Decision {
        val uri = parse(url) ?: return Decision.BLOCK
        return when (uri.scheme?.lowercase()) {
            "https" -> if (isTrustedHost(uri.host)) Decision.LOAD_IN_APP else Decision.OPEN_EXTERNALLY
            // http, intent, spotify, javascript, file, data ... hepsi engellenir.
            else -> Decision.BLOCK
        }
    }

    /** Alt çerçeveler (iframe) yalnızca HTTPS ise yüklenebilir. */
    fun allowSubframe(url: String?): Boolean =
        parse(url)?.scheme?.lowercase() == "https"

    /** Korumalı medya izni isteyen kaynağın Spotify oynatıcısı olup olmadığı. */
    fun isTrustedPlayerOrigin(origin: String?): Boolean {
        val uri = parse(origin) ?: return false
        return uri.scheme?.lowercase() == "https" &&
            uri.host?.lowercase()?.trimEnd('.') == PLAYER_HOST
    }

    private fun parse(url: String?): URI? {
        if (url.isNullOrBlank()) return null
        return try {
            URI(url.trim())
        } catch (_: Exception) {
            null
        }
    }
}
