package com.music.spotui.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BetaLogsTest {
    @Test fun diagnosticTextIsDiscardedEvenWithoutRecognizedSecretSyntax() {
        val canary = "SYNTHETIC_PRIVACY_CANARY_57319"
        assertEquals("", BetaLogs.sanitize(canary))
        assertEquals("", BetaLogs.useful(canary))
    }
}
