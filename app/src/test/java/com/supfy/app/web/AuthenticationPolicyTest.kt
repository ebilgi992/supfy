package com.supfy.app.web

import org.junit.Assert.*
import org.junit.Test

class AuthenticationPolicyTest {
    @Test fun spotifySignInRoutesStayInAuthenticationUntilPlayerReturn() {
        for(url in listOf("https://accounts.spotify.com/en/login?continue=x",
            "https://www.spotify.com/signup/","https://www.spotify.com/tr/signup", "https://open.spotify.com/login", "https://open.spotify.com/intl-tr/login?continue=x")) {
            assertTrue(url,AuthenticationPolicy.isAuthenticationUrl(url))
            assertFalse(url,AuthenticationPolicy.isPlayerReturn(url))
        }
        assertTrue(AuthenticationPolicy.isPlayerReturn("https://open.spotify.com/collection/tracks"))
        assertTrue(AuthenticationPolicy.isPlayerReturn("https://open.spotify.com/"))
    }
    @Test fun foreignOrMalformedLoginUrlsNeverGetAnAuthenticationWindow() {
        for(url in listOf("http://accounts.spotify.com/login","https://accounts.spotify.com.evil.example/login",
            "https://user@accounts.spotify.com/login","https://accounts.spotify.com:8443/login",
            "https://www.spotify.com/account/overview/", "https://www.spotify.com/track/signup", "javascript:alert(1)","https://example.org/login","https://open.spotify.com/track/login")) {
            assertFalse(url,AuthenticationPolicy.isAuthenticationUrl(url))
        }
    }
}
