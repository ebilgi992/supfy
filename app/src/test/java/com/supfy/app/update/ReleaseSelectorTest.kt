package com.supfy.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseSelectorTest {

    private val repo = "ebilgi992/supfy"
    private fun rel(tag: String, name: String? = "Supfy 0.4.0 (1020)", url: String? = null, draft: Boolean = false) =
        ReleaseCandidate(tag, name, url ?: "https://github.com/$repo/releases/tag/$tag", draft)

    @Test
    fun etiketlerdenSurumKoduOkunur() {
        assertEquals(1020, ReleaseSelector.versionCodeOf("build-1020"))
        assertEquals(16, ReleaseSelector.versionCodeOf("build-16"))
        assertNull(ReleaseSelector.versionCodeOf("v1.0"))
        assertNull(ReleaseSelector.versionCodeOf("build-abc"))
        assertNull(ReleaseSelector.versionCodeOf("build-1020-x"))
    }

    @Test
    fun enYeniSurumSecilir() {
        val u = ReleaseSelector.pickUpdate(
            listOf(rel("build-1018"), rel("build-1021", "Supfy 0.4.1 (1021)"), rel("build-1019")),
            installedVersionCode = 1018,
            repo = repo,
        )
        assertEquals(1021, u?.versionCode)
        assertEquals("0.4.1", u?.versionName)
    }

    @Test
    fun yukluSurumGuncelseOneriYok() {
        assertNull(ReleaseSelector.pickUpdate(listOf(rel("build-1020")), 1020, repo))
        assertNull(ReleaseSelector.pickUpdate(listOf(rel("build-16")), 1020, repo))
        assertNull(ReleaseSelector.pickUpdate(emptyList(), 5, repo))
    }

    @Test
    fun taslakVeGuvenilmeyenAdreslerYokSayilir() {
        assertNull(ReleaseSelector.pickUpdate(listOf(rel("build-2000", draft = true)), 5, repo))
        assertNull(ReleaseSelector.pickUpdate(listOf(rel("build-2000", url = "https://evil.example/ebilgi992/supfy/releases/x")), 5, repo))
        assertNull(ReleaseSelector.pickUpdate(listOf(rel("build-2000", url = "http://github.com/ebilgi992/supfy/releases/x")), 5, repo))
        assertNull(ReleaseSelector.pickUpdate(listOf(rel("build-2000", url = "https://github.com/someone/else/releases/x")), 5, repo))
        assertNull(ReleaseSelector.pickUpdate(listOf(rel("build-2000", url = "https://github.com/YASMANTH-VEMALA/spotify-ad-blocker/releases/x")), 5, repo))
    }

    @Test
    fun guvenilirAdresKontrolu() {
        assertTrue(ReleaseSelector.isTrustedReleaseUrl("https://github.com/ebilgi992/supfy/releases/tag/build-1020", repo))
        assertFalse(ReleaseSelector.isTrustedReleaseUrl("https://github.com:8443/ebilgi992/supfy/releases/tag/x", repo))
        assertFalse(ReleaseSelector.isTrustedReleaseUrl("https://github.com/ebilgi992/supfy/releases/../../evil", repo))
        assertFalse(ReleaseSelector.isTrustedReleaseUrl("https://github.com.evil.io/ebilgi992/supfy/releases/x", repo))
        assertFalse(ReleaseSelector.isTrustedReleaseUrl("bozuk adres", repo))
    }

    @Test
    fun surumAdiBulunamazsaEtiketKullanilir() {
        assertEquals("build-1020", ReleaseSelector.versionNameOf(rel("build-1020", name = null)))
        assertEquals("build-1020", ReleaseSelector.versionNameOf(rel("build-1020", name = "Başka bir ad")))
    }

    @Test
    fun denetimSikligiSinirlanir() {
        val h = 60L * 60 * 1000
        assertTrue(ReleaseSelector.shouldAutoCheck(nowMs = 100 * h, lastCheckMs = 0))
        assertFalse(ReleaseSelector.shouldAutoCheck(nowMs = 100 * h, lastCheckMs = 95 * h))
        assertTrue(ReleaseSelector.shouldAutoCheck(nowMs = 100 * h, lastCheckMs = 88 * h))
        assertTrue(ReleaseSelector.shouldAutoCheck(nowMs = 100 * h, lastCheckMs = 200 * h)) // saat geri alındıysa
    }

    @Test
    fun ertelenenSurumTekrarOnerilmez() {
        val u = AvailableUpdate(1021, "0.4.1", "https://github.com/$repo/releases/tag/build-1021")
        assertFalse(ReleaseSelector.shouldOffer(u, snoozedCode = 1021, snoozedUntilMs = 2000, nowMs = 1000))
        assertTrue(ReleaseSelector.shouldOffer(u, snoozedCode = 1021, snoozedUntilMs = 2000, nowMs = 3000))
        assertTrue(ReleaseSelector.shouldOffer(u, snoozedCode = 1020, snoozedUntilMs = 2000, nowMs = 1000))
    }
}
