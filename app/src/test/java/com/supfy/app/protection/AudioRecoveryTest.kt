package com.supfy.app.protection

import org.junit.Assert.*
import org.junit.Test

class AudioRecoveryTest {
    @Test fun failedUnmuteIsRetriedEvenAfterDisablingNewMutes() {
        val port = object : AudioPort {
            private var value = false
            var failRestore = true
            override var muted: Boolean
                get() = value
                set(v) {
                    if (!v && failRestore) { failRestore=false; error("transient provider failure") }
                    value=v
                }
        }
        val audio=AudioRecovery(port)
        assertEquals(AudioResult.MUTED,audio.apply(true))
        assertEquals(AudioResult.FAILED,audio.apply(false))
        assertTrue(port.muted)
        assertEquals(AudioResult.RESTORED,audio.apply(false))
        assertFalse(port.muted)
        assertEquals(AudioResult.NONE,audio.apply(true))
        assertFalse(port.muted)
    }
}
