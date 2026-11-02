package com.music.spotui.privacy

import org.junit.Assert.*
import org.junit.Test

class ConsentRevisionTest {
    @Test fun missingAndOldProviderRevisionsFailClosed() {
        assertFalse(ConsentRevision.accepts(null, "google:1"))
        assertFalse(ConsentRevision.accepts("google:1", "google:2"))
        assertFalse(ConsentRevision.accepts("genius:1", "google:1"))
        assertTrue(ConsentRevision.accepts("google:1", "google:1"))
    }
}
