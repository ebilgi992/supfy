package com.supfy.app.web

import org.junit.Assert.*
import org.junit.Test

class AuthenticationPolicyTest {
    @Test fun spotifySignInRoutesStayInAuthenticationUntilPlayerReturn() {
        for(url in listOf("https://accounts.spotify.com/en/login?continue=x",
            "https://open.spotify.com/login", "https://open.spotify.com/intl-tr/login?continue=x")) {
            assertTrue(url,AuthenticationPolicy.isAuthenticationUrl(url))
            assertFalse(url,AuthenticationPolicy.isPlayerReturn(url))
        }
        assertTrue(AuthenticationPolicy.isPlayerReturn("https://open.spotify.com/collection/tracks"))
        assertTrue(AuthenticationPolicy.isPlayerReturn("https://open.spotify.com/"))
    }
    @Test fun foreignOrMalformedLoginUrlsNeverGetAnAuthenticationWindow() {
        for(url in listOf("http://accounts.spotify.com/login","https://accounts.spotify.com.evil.example/login",
            "https://user@accounts.spotify.com/login","https://accounts.spotify.com:8443/login",
            "javascript:alert(1)","https://example.org/login","https://open.spotify.com/track/login")) {
            assertFalse(url,AuthenticationPolicy.isAuthenticationUrl(url))
        }
    }
}
