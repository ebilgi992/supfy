package com.supfy.app.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionsTest {

    private val full = DeviceSupport(safeBrowsing = true, serviceWorkerHardening = true)
    private val none = DeviceSupport(safeBrowsing = false, serviceWorkerHardening = false)

    @Test
    fun tumOzelliklerDestekleniyorsaHepsiEtkin() {
        assertEquals(Protection.entries.size, Protections.activeCount(full))
    }

    @Test
    fun desteklenmeyenOzellikDogruGosterilir() {
        assertEquals(ProtectionStatus.UNSUPPORTED, Protections.statusOf(Protection.SAFE_BROWSING, none))
        assertEquals(ProtectionStatus.UNSUPPORTED, Protections.statusOf(Protection.SERVICE_WORKER_HARDENING, none))
        assertEquals(Protection.entries.size - 2, Protections.activeCount(none))
    }

    @Test
    fun sabitKorumalarHerCihazdaEtkin() {
        for (p in listOf(Protection.HTTPS_ONLY, Protection.DOMAIN_ALLOWLIST, Protection.DRM_GATE, Protection.NO_JS_BRIDGE)) {
            assertEquals(ProtectionStatus.ACTIVE, Protections.statusOf(p, none))
        }
    }

    @Test
    fun reklamEngellemeKorumaListesindeYok() {
        assertTrue(Protection.entries.none { it.name.startsWith("AD_") || it.name.contains("_AD") })
    }

    @Test
    fun sayaclarYalnizcaOlayBasinaBirArtar() {
        var s = SessionStats()
        assertEquals(0, s.total)
        s = s.withBlockedNavigation().withBlockedNavigation().withSslRejected()
            .withDeniedPermission().withDrmGranted().withExternalOpen()
        assertEquals(2, s.blockedNavigations)
        assertEquals(1, s.sslRejected)
        assertEquals(1, s.deniedPermissions)
        assertEquals(1, s.drmGranted)
        assertEquals(1, s.externalOpens)
        assertEquals(6, s.total)
    }
}
