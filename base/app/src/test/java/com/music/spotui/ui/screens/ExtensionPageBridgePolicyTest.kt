package com.music.spotui.ui.screens

import org.junit.Assert.*
import org.junit.Test

class ExtensionPageBridgePolicyTest {
    @Test fun eachExtensionGetsAStableIsolatedSyntheticOrigin() {
        val a = ExtensionPageBridgePolicy.origin("extension-a")
        assertEquals(a, ExtensionPageBridgePolicy.origin("extension-a"))
        assertNotEquals(a, ExtensionPageBridgePolicy.origin("extension-b"))
        assertTrue(a.matches(Regex("https://[a-f0-9]{32}\\.[a-f0-9]{32}\\.extension\\.invalid")))
        assertFalse(ExtensionPageBridgePolicy.origin("../user@evil.test").contains("evil.test"))
    }

    @Test fun onlyOwnMainFrameCanPost() {
        val origin = ExtensionPageBridgePolicy.origin("a")
        assertTrue(ExtensionPageBridgePolicy.acceptMessage(origin, origin, true, "ok"))
        assertFalse(ExtensionPageBridgePolicy.acceptMessage(origin, origin, false, "ok"))
        assertFalse(ExtensionPageBridgePolicy.acceptMessage(origin, ExtensionPageBridgePolicy.origin("b"), true, "ok"))
        assertFalse(ExtensionPageBridgePolicy.acceptMessage(origin, "$origin.evil.test", true, "ok"))
        assertFalse(ExtensionPageBridgePolicy.acceptMessage(origin, "null", true, "ok"))
    }

    @Test fun messageBudgetCountsUtf8Bytes() {
        val origin = ExtensionPageBridgePolicy.origin("a")
        assertTrue(ExtensionPageBridgePolicy.acceptMessage(origin, origin, true, "a".repeat(64 * 1024)))
        assertFalse(ExtensionPageBridgePolicy.acceptMessage(origin, origin, true, "a".repeat(64 * 1024 + 1)))
        assertTrue(ExtensionPageBridgePolicy.withinLimit("ü".repeat(32 * 1024)))
        assertFalse(ExtensionPageBridgePolicy.withinLimit("ü".repeat(32 * 1024 + 1)))
    }
}
