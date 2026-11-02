package app.myspoti.host.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LogbookPrivacyTest {
    @Test fun diagnosticUrlDoesNotExposePathOrUnrecognizedPersonalData() {
        assertEquals("", Logbook.noQuery("https://example.invalid/SYNTHETIC_PRIVACY_CANARY_57319"))
    }

    @Test fun loggerDiscardsAllInputsAndDoesNotRetainOrInvokeHeaderSupplier() {
        val canary = "SYNTHETIC_PRIVACY_CANARY_57319"
        var invoked = false
        Logbook.log(canary)
        Logbook.logEvery(canary, 1, canary)
        Logbook.fact(canary, canary)
        Logbook.headerExtras = { invoked = true; listOf(canary to canary) }
        assertEquals("", Logbook.recent())
        assertNull(Logbook.headerExtras)
        assertFalse(invoked)
        assertFalse(Logbook.javaClass.declaredFields.any {
            Map::class.java.isAssignableFrom(it.type) || Collection::class.java.isAssignableFrom(it.type)
        })
    }
}
