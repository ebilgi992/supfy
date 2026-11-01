package com.supfy.app.protection

import org.junit.Assert.*
import org.junit.Test

class ProtectionEngineTest {
    @Test fun successfulRecoveryDoesNotShowAnOldErrorBesideReadyComponents() {
        val e=active()
        e.fail()
        assertNotNull(e.state.value.lastError)
        e.configure(ProtectionOptions(enabled=false))
        e.configure(ProtectionOptions(enabled=true))
        e.acceptSnapshot(PageSnapshot())
        assertEquals(ComponentStatus.READY,e.state.value.detectionStatus)
        assertNull(e.state.value.lastError)
    }
    @Test fun rendererFailureStopsFilteringUntilExplicitRecovery() {
        val e=active()
        e.rendererFailed()
        assertEquals(EngineStatus.ERROR,e.state.value.status)
        assertFalse(e.shouldBlock("https://doubleclick.net/ad","GET",false))
        e.configure(ProtectionOptions(enabled=false));e.configure(ProtectionOptions(enabled=true))
        e.acceptSnapshot(PageSnapshot())
        assertEquals(EngineStatus.ACTIVE,e.state.value.status)
    }
    @Test fun componentErrorsRemainSeparateFromDetectionAndFiltering() {
        val e = active()
        e.acceptSnapshot(PageSnapshot(audioError = true, visualError = false, errors = 1))
        assertEquals(EngineStatus.ACTIVE, e.state.value.status)
        assertEquals(ComponentStatus.ERROR, e.state.value.audioStatus)
        assertEquals(ComponentStatus.READY, e.state.value.visualStatus)
        assertTrue(e.shouldBlock("https://doubleclick.net/ad", "GET", false))
    }
    @Test fun temporaryPageDelayKeepsRulesAvailableAndAllowsAutomaticRecovery() {
        val e=active();e.pageTemporarilyUnavailable()
        assertEquals(EngineStatus.STARTING,e.state.value.status)
        assertEquals(0L,e.state.value.errors)
        assertTrue(e.shouldBlock("https://doubleclick.net/ad","GET",false))
        e.acceptSnapshot(PageSnapshot(phase="music"))
        assertEquals(EngineStatus.ACTIVE,e.state.value.status)
        assertNull(e.state.value.lastError)
    }
    @Test fun anAdTimeoutAndDomFailureHaveDistinctSafeDiagnostics() {
        val timed=active();timed.acceptSnapshot(PageSnapshot(phase="error",errors=1,reason=2))
        val dom=active();dom.acceptSnapshot(PageSnapshot(phase="error",errors=1,reason=3))
        assertNotEquals(timed.state.value.lastError,dom.state.value.lastError)
        assertEquals(EngineStatus.ERROR,timed.state.value.status)
        assertFalse(timed.shouldBlock("https://doubleclick.net/ad","GET",false))
    }
    private fun active(): ProtectionEngine = ProtectionEngine().apply {
        configure(ProtectionOptions(enabled = true))
        pageStarted("https://open.spotify.com/")
        acceptSnapshot(PageSnapshot())
    }

    @Test fun disabledAndLoginRequestsAreAlwaysAllowed() {
        val e = ProtectionEngine()
        e.pageStarted("https://open.spotify.com/")
        assertFalse(e.shouldBlock("https://doubleclick.net/ad", "GET", false))
        e.configure(ProtectionOptions(enabled = true))
        e.pageStarted("https://accounts.spotify.com/login")
        assertFalse(e.shouldBlock("https://doubleclick.net/ad", "GET", false))
    }

    @Test fun matchesHostBoundariesAndPreservesPlaybackAuthAndDrm() {
        val e = active()
        assertTrue(e.shouldBlock("https://ad.doubleclick.net/x", "GET", false))
        for (url in listOf("https://doubleclick.net.evil.example/x", "https://notdoubleclick.net/x",
            "https://open.spotify.com/", "https://accounts.spotify.com/login",
            "https://spclient.wg.spotify.com/ad-logic/", "https://audio-ak-spotify-com.akamaized.net/ad/",
            "https://gew1-spclient.spotify.com/widevine-license/v1/audio/license")) {
            assertFalse(url, e.shouldBlock(url, "GET", false))
        }
        assertFalse(e.shouldBlock("https://doubleclick.net/x", "POST", false))
        assertFalse(e.shouldBlock("https://doubleclick.net/x", "GET", true))
        assertFalse(e.shouldBlock("https://doubleclick.net:8443/x", "GET", false))
    }

    @Test fun countersReflectCommittedEventsAndNoDuplicateSnapshots() {
        val e = active()
        assertEquals(0L, e.state.value.blockedRequests)
        e.shouldBlock("https://doubleclick.net/x", "GET", false)
        assertEquals(0L, e.state.value.blockedRequests)
        e.requestBlocked()
        val report = PageSnapshot(detected = 1, muted = 1, banners = 2)
        e.acceptSnapshot(report); e.acceptSnapshot(report)
        assertEquals(1L, e.state.value.detectedAds)
        assertEquals(1L, e.state.value.mutedMedia)
        assertEquals(2L, e.state.value.hiddenBanners)
        assertEquals(1L, e.state.value.blockedRequests)
        e.pageStarted("https://open.spotify.com/album/x")
        e.acceptSnapshot(PageSnapshot(detected = 1))
        assertEquals(2L, e.state.value.detectedAds)
    }

    @Test fun errorsFailOpenAndRequireExplicitReenable() {
        val e = active()
        e.fail()
        assertEquals(EngineStatus.ERROR, e.state.value.status)
        assertFalse(e.shouldBlock("https://doubleclick.net/x", "GET", false))
        e.acceptSnapshot(PageSnapshot())
        assertEquals(EngineStatus.ERROR, e.state.value.status)
        e.configure(ProtectionOptions(enabled = false))
        e.configure(ProtectionOptions(enabled = true))
        e.acceptSnapshot(PageSnapshot())
        assertEquals(EngineStatus.ACTIVE, e.state.value.status)
    }

    @Test fun disablingNetworkOptionStopsFilteringAndLogsAreBounded() {
        val e = active()
        e.configure(ProtectionOptions(enabled = true, filterNetwork = false))
        assertFalse(e.shouldBlock("https://doubleclick.net/x", "GET", false))
        repeat(100) { e.requestBlocked() }
        assertTrue(e.state.value.logs.size <= 40)
    }

    @Test fun malformedOrDecreasingCountersFailOpen() {
        for (bad in listOf(PageSnapshot(detected = -1), PageSnapshot(banners = 1_000_001))) {
            val e = active(); e.acceptSnapshot(bad)
            assertEquals(EngineStatus.ERROR, e.state.value.status)
        }
        val e = active(); e.acceptSnapshot(PageSnapshot(detected = 2)); e.acceptSnapshot(PageSnapshot(detected = 1))
        assertEquals(EngineStatus.ERROR, e.state.value.status)
    }

    @Test fun navigatingToUntrustedOriginStopsProtection() {
        val e = active()
        e.pageStarted("https://open.spotify.com:8443/")
        assertEquals(EngineStatus.WAITING_FOR_PLAYER, e.state.value.status)
        assertFalse(e.shouldBlock("https://doubleclick.net/x", "GET", false))
    }

    @Test fun disablingOrNavigatingClearsPlaybackObservation() {
        val e = active(); e.acceptSnapshot(PageSnapshot(playing = true))
        assertTrue(e.state.value.playing)
        e.configure(ProtectionOptions(enabled = false))
        assertFalse(e.state.value.playing)
        e.configure(ProtectionOptions(enabled = true)); e.acceptSnapshot(PageSnapshot(playing = true))
        assertTrue(e.state.value.playing)
        e.pageStarted("https://accounts.spotify.com/login")
        assertFalse(e.state.value.playing)
    }

    @Test fun unseenHistoricalJsErrorDoesNotBreakExplicitOffOnRecovery() {
        val e = active()
        e.configure(ProtectionOptions(enabled = false))
        e.configure(ProtectionOptions(enabled = true))
        e.acceptSnapshot(PageSnapshot(errors = 1, phase = "music"))
        assertEquals(EngineStatus.ACTIVE, e.state.value.status)
        assertEquals(1L, e.state.value.errors)
    }
}
