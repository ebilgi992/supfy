package app.myspoti

import org.junit.Assert.assertEquals
import org.junit.Test

class SupfyLanguageTest {
    @Test fun firstInstallationDefaultsToTurkish() { assertEquals("tr-TR", SupfyDefaults.languageTag(null, null, false)) }
    @Test fun olderAndroidRestoresTheExplicitChoiceAfterProcessDeath() {
        assertEquals("tr-TR", SupfyDefaults.languageTag("tr-TR", null, false))
        assertEquals("en", SupfyDefaults.languageTag("en", null, false))
        assertEquals("", SupfyDefaults.languageTag("", null, false))
    }
    @Test fun existingPlatformChoiceIsPreservedOnMigration() { assertEquals("de-DE", SupfyDefaults.languageTag(null, "de-DE", false)) }
    @Test fun android13SystemSettingsIncludingAutomaticRemainAuthoritative() {
        assertEquals("fr", SupfyDefaults.languageTag("tr-TR", "fr", true))
        assertEquals("", SupfyDefaults.languageTag("tr-TR", "", true))
    }
}
