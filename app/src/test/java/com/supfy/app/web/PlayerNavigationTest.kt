package com.supfy.app.web

import org.junit.Assert.*
import org.junit.Test

class PlayerNavigationTest {
    @Test fun onlySupportedSpotifyContentLinksBecomeHttps() {
        assertEquals("https://open.spotify.com/track/0123456789ABCDEFGHIJKL", PlayerNavigation.webUrl("spotify:track:0123456789ABCDEFGHIJKL"))
        assertNull(PlayerNavigation.webUrl("spotify:track:bad"))
        assertNull(PlayerNavigation.webUrl("spotify:login:0123456789ABCDEFGHIJKL"))
        assertNull(PlayerNavigation.webUrl("intent://evil/#Intent;scheme=javascript;end"))
    }
    @Test fun signupDeepLinksUseTheOfficialPublicPage() {
        assertEquals("https://www.spotify.com/signup/",PlayerNavigation.webUrl("spotify:signup"))
        assertNull(PlayerNavigation.webUrl("spotify:signup:other"))
    }
    @Test fun loginUsesOfficialHttpsAndReturnsToPlayer() {
        assertEquals(UrlPolicy.Decision.LOAD_IN_APP, UrlPolicy.decide(PlayerNavigation.LOGIN_URL))
        assertTrue(PlayerNavigation.LOGIN_URL.startsWith("https://accounts.spotify.com/login?"))
        assertTrue(PlayerNavigation.LOGIN_URL.contains("continue=https%3A%2F%2Fopen.spotify.com%2F"))
    }
}
