package com.supfy.app.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTest {

    @Test
    fun sorguVeParcaSilinir() {
        assertEquals(
            "https://accounts.spotify.com/tr/login",
            DiagLog.sanitizeUrl("https://accounts.spotify.com/tr/login?continue=x&code=GIZLI#frag"),
        )
        assertEquals("intent://open", DiagLog.sanitizeUrl("intent://open#Intent;scheme=spotify;end"))
        assertEquals("-", DiagLog.sanitizeUrl(null))
    }

    @Test
    fun metindekiAdreslerTemizlenir() {
        val s = DiagLog.scrubText("Failed to load https://open.spotify.com/get_access_token?reason=init&token=abc123 (403)")
        assertFalse(s.contains("abc123"))
        assertFalse(s.contains("reason="))
        assertTrue(s.contains("https://open.spotify.com/get_access_token"))
    }

    @Test
    fun kayitSinirliVeTekrarBirlesir() {
        var log = DiagLog()
        repeat(DiagLog.MAX_EVENTS + 15) { log = log.add(DiagKind.PAGE_START, "u$it", nowMs = it.toLong()) }
        assertEquals(DiagLog.MAX_EVENTS, log.events.size)
        assertEquals("u${DiagLog.MAX_EVENTS + 14}", log.events.last().detail)
        val before = log.events.size
        log = log.add(DiagKind.PAGE_START, "u${DiagLog.MAX_EVENTS + 14}")
        assertEquals(before, log.events.size)
    }

    @Test
    fun bicimBaslikVeOlaylariIcerir() {
        val text = DiagLog().add(DiagKind.HTTP_ERROR, "403 https://accounts.spotify.com/login", 0).format("Supfy 0.4.0")
        assertTrue(text.startsWith("Supfy 0.4.0"))
        assertTrue(text.contains("HTTP_ERROR 403 https://accounts.spotify.com/login"))
    }
}
