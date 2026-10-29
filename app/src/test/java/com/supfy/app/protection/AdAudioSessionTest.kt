package com.supfy.app.protection

import org.junit.Assert.*
import org.junit.Test

class AdAudioSessionTest {
    private class Port(override var muted: Boolean = false) : AudioPort
    @Test fun adMuteRestoresOriginalStateWithoutTouchingDeviceVolume() {
        val port = Port(); val session = AdAudioSession(port)
        assertTrue(session.enterAd()); assertTrue(port.muted)
        assertFalse(session.enterAd())
        assertTrue(session.restore()); assertFalse(port.muted)
        assertFalse(session.restore())
    }
    @Test fun alreadyMutedAndUserUnmutePreferencesArePreserved() {
        val port = Port(true); val session = AdAudioSession(port)
        assertFalse(session.enterAd()); session.restore(); assertTrue(port.muted)
        port.muted = false; session.enterAd(); port.muted = false
        session.restore(); assertFalse(port.muted)
    }
}
