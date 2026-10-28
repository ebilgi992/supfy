package com.sametozdemir.supfy.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentsTest {

    @Test
    fun chromeSurumuKorunurVeMobilIbaresiKalkar() {
        val mobile = "Mozilla/5.0 (Linux; Android 15; 24117RK2CG Build/AP3A; wv) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Version/4.0 Chrome/141.0.7390.122 Mobile Safari/537.36"
        val desktop = UserAgents.desktopFrom(mobile)
        assertTrue(desktop.contains("Chrome/141.0.7390.122"))
        assertTrue(desktop.contains("X11; Linux x86_64"))
        assertFalse(desktop.contains("Mobile"))
        assertFalse(desktop.contains("Android"))
    }

    @Test
    fun surumBulunamazsaVarsayilanKullanilir() {
        assertTrue(UserAgents.desktopFrom(null).contains("Chrome/"))
        assertTrue(UserAgents.desktopFrom("bilinmeyen").contains("Chrome/"))
    }
}
