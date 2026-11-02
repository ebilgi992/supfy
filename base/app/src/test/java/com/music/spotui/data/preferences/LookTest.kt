package com.music.spotui.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class LookTest {
    @Test fun newInstallUsesImmersive() {
        assertEquals(Look.IMMERSIVE, Look.of(null))
    }

    @Test fun explicitClassicChoiceIsPreserved() {
        assertEquals(Look.CLASSIC, Look.of("CLASSIC"))
    }

    @Test fun explicitImmersiveChoiceIsPreserved() {
        assertEquals(Look.IMMERSIVE, Look.of("IMMERSIVE"))
    }

    @Test fun unknownStoredChoiceUsesImmersive() {
        assertEquals(Look.IMMERSIVE, Look.of("unknown"))
    }
}
