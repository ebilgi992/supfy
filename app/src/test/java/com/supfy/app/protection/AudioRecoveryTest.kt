package com.supfy.app.protection

import org.junit.Assert.*
import org.junit.Test

class AudioRecoveryTest {
    @Test fun aNativeSetterThatSilentlyIgnoresMuteDoesNotReportSuccess() {
        val port=object:AudioPort {
            override var muted:Boolean
                get()=false
                set(value) {}
        }
        val audio=AudioRecovery(port)
        assertEquals(AudioResult.FAILED,audio.apply(true))
        assertTrue(audio.failed)
        assertFalse(port.muted)
    }
    @Test fun silentlyIgnoredRestoreRetainsOwnershipForRetry() {
        val port=object:AudioPort {
            private var value=false
            var ignoreRestore=true
            override var muted:Boolean
                get()=value
                set(next) { if(next || !ignoreRestore)value=next }
        }
        val audio=AudioRecovery(port)
        assertEquals(AudioResult.MUTED,audio.apply(true))
        assertEquals(AudioResult.FAILED,audio.apply(false))
        assertTrue(audio.restorationPending)
        port.ignoreRestore=false
        assertEquals(AudioResult.RESTORED,audio.apply(false))
        assertFalse(audio.restorationPending)
    }
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
