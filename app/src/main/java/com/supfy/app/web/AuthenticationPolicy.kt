package com.supfy.app.web

import java.net.URI

/** Public Spotify sign-in documents; never treat the intermediate /login as a player return. */
object AuthenticationPolicy {
    private val playerAuthPath=Regex("^/(?:intl-[a-z]{2}(?:-[a-z]{2})?/)?(?:login|signup|logout)(?:/.*)?$")

    private val publicAuthPath=Regex("^/(?:[a-z]{2}(?:-[a-z]{2})?/)?(?:signup|login)(?:/.*)?$")

    fun isAuthenticationUrl(url: String?): Boolean {
        if(UrlPolicy.decide(url)!=UrlPolicy.Decision.LOAD_IN_APP)return false
        val uri=runCatching { URI(url!!) }.getOrNull()?:return false
        val host=uri.host?.lowercase()?.trimEnd('.')
        return host=="accounts.spotify.com" ||
            (host=="open.spotify.com" && playerAuthPath.matches(uri.path.orEmpty())) ||
            (host in setOf("www.spotify.com","spotify.com") && publicAuthPath.matches(uri.path.orEmpty()))
    }

    fun isPlayerReturn(url: String?): Boolean =
        UrlPolicy.isTrustedPlayerOrigin(url) && !isAuthenticationUrl(url)
}
