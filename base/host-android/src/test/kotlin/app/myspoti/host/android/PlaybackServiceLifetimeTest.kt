package app.myspoti.host.android

import org.junit.Assert.*
import org.junit.Test

class PlaybackServiceLifetimeTest {
    @Test fun endBeforeOnCreateSurvivesUntilThePendingStartIsDelivered() {
        val starts = PlaybackStartRequests()
        starts.begin();starts.requestStop()
        assertTrue(starts.pending);assertTrue(starts.stopRequested)
        val lifetime = PlaybackServiceLifetime()
        assertFalse(lifetime.requestStop(starts.pending))
        starts.complete()
        assertTrue(lifetime.onStart())
    }
    @Test fun playbackAfterSignOutClearsThePendingEndForTheNewSession() {
        val starts = PlaybackStartRequests()
        starts.requestStop();starts.resumePlayback()
        assertFalse(starts.stopRequested)
    }
    @Test fun pendingForegroundStartsAreDeduplicatedUntilDelivered() {
        val starts=PlaybackStartRequests()
        assertTrue(starts.begin());assertTrue(starts.pending)
        assertFalse(starts.begin())
        starts.complete();assertFalse(starts.pending);assertTrue(starts.begin())
    }
    @Test fun endBeforeFirstStartDefersDestructionUntilPromotionCanHappen() {
        val lifetime=PlaybackServiceLifetime()
        assertFalse(lifetime.requestStop(pendingStart=true))
        assertTrue(lifetime.onStart())
    }
    @Test fun aBoundOnlyServiceDoesNotBecomeAStartedServiceOnSignOut() {
        val lifetime=PlaybackServiceLifetime()
        assertFalse(lifetime.requestStop(pendingStart=false))
        assertTrue(lifetime.stopRequested)
    }
    @Test fun aNewPendingStartPreventsStoppingAnAlreadyStartedService() {
        val lifetime=PlaybackServiceLifetime();assertFalse(lifetime.onStart())
        assertFalse(lifetime.requestStop(pendingStart=true));assertTrue(lifetime.onStart())
    }
    @Test fun anActualNewPlaybackCanReplaceADeferredStop() {
        val lifetime=PlaybackServiceLifetime();lifetime.requestStop(true)
        lifetime.resumePlayback();assertFalse(lifetime.onStart())
    }
    @Test fun anOrdinaryStoppedSessionMayEndAfterItsStartWasDelivered() {
        val lifetime=PlaybackServiceLifetime();lifetime.onStart()
        assertTrue(lifetime.requestStop(pendingStart=false))
    }
}
