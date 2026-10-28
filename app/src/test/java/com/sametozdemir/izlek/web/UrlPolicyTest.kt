package com.sametozdemir.izlek.web

import com.sametozdemir.izlek.web.UrlPolicy.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlPolicyTest {

    @Test
    fun spotifySayfalariUygulamaIcindeAcilir() {
        assertEquals(Decision.LOAD_IN_APP, UrlPolicy.decide("https://open.spotify.com/"))
        assertEquals(Decision.LOAD_IN_APP, UrlPolicy.decide("https://open.spotify.com/track/abc?si=1"))
        assertEquals(Decision.LOAD_IN_APP, UrlPolicy.decide("https://accounts.spotify.com/tr/login"))
        assertEquals(Decision.LOAD_IN_APP, UrlPolicy.decide("https://OPEN.SPOTIFY.COM/album/x"))
    }

    @Test
    fun benzerGorunenAlanAdlariGuvenilmez() {
        assertEquals(Decision.OPEN_EXTERNALLY, UrlPolicy.decide("https://evilspotify.com/login"))
        assertEquals(Decision.OPEN_EXTERNALLY, UrlPolicy.decide("https://spotify.com.evil.example/"))
        assertEquals(Decision.OPEN_EXTERNALLY, UrlPolicy.decide("https://www.google.com/"))
    }

    @Test
    fun httpsDisiSemalarEngellenir() {
        assertEquals(Decision.BLOCK, UrlPolicy.decide("http://open.spotify.com/"))
        assertEquals(Decision.BLOCK, UrlPolicy.decide("javascript:alert(1)"))
        assertEquals(Decision.BLOCK, UrlPolicy.decide("intent://open#Intent;scheme=spotify;end"))
        assertEquals(Decision.BLOCK, UrlPolicy.decide("spotify:track:123"))
        assertEquals(Decision.BLOCK, UrlPolicy.decide("file:///sdcard/a.html"))
        assertEquals(Decision.BLOCK, UrlPolicy.decide(""))
        assertEquals(Decision.BLOCK, UrlPolicy.decide(null))
        assertEquals(Decision.BLOCK, UrlPolicy.decide("https://bad url with spaces"))
    }

    @Test
    fun altCerceveYalnizcaHttps() {
        assertTrue(UrlPolicy.allowSubframe("https://www.recaptcha.net/x"))
        assertFalse(UrlPolicy.allowSubframe("http://example.com"))
        assertFalse(UrlPolicy.allowSubframe("about:blank"))
    }

    @Test
    fun drmIzniYalnizcaOynaticiKaynaginaVerilir() {
        assertTrue(UrlPolicy.isTrustedPlayerOrigin("https://open.spotify.com"))
        assertTrue(UrlPolicy.isTrustedPlayerOrigin("https://open.spotify.com/"))
        assertFalse(UrlPolicy.isTrustedPlayerOrigin("http://open.spotify.com"))
        assertFalse(UrlPolicy.isTrustedPlayerOrigin("https://accounts.spotify.com"))
        assertFalse(UrlPolicy.isTrustedPlayerOrigin("https://open.spotify.com.evil.example"))
        assertFalse(UrlPolicy.isTrustedPlayerOrigin(null))
    }
}
