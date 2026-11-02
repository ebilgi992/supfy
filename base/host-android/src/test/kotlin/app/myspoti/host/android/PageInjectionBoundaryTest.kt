package app.myspoti.host.android

import app.myspoti.spotify.bridge.Bridge
import app.myspoti.spotify.bridge.BridgeException
import app.myspoti.spotify.bridge.BridgeHost
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.*
import org.junit.Test

class PageInjectionBoundaryTest {
    @Test fun expectedInjectionRejectionDoesNotEscapeTheCallbackAndClearsReadiness() = runBlocking {
        for (code in listOf("PLAYER_ORIGIN_REQUIRED", "PLAYER_PAGE_UNAVAILABLE", "SECURE_MESSAGE_CHANNEL_UNAVAILABLE")) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val events = MutableSharedFlow<String>()
            val bridge = Bridge(object : BridgeHost {
                override val messages = events
                override suspend fun evaluate(script: String) { throw BridgeException(code) }
            }, scope)
            try {
                events.emit("""{"type":"ready"}""")
                assertTrue(bridge.ready.value)
                launch { injectPlayerPage(bridge::pageGone, bridge::inject) }.join()
                assertFalse(bridge.ready.value)
            } finally { scope.cancel() }
        }
    }

    @Test fun unexpectedTypedAndUntypedFailuresStillPropagate() = runBlocking {
        for (failure in listOf(BridgeException("unexpected bridge defect"), IllegalStateException("defect"))) {
            var invalidated = false
            try {
                injectPlayerPage({ invalidated = true }) { throw failure }
                fail("Unexpected failure was swallowed")
            } catch (actual: Exception) { assertSame(failure, actual) }
            assertFalse(invalidated)
        }
    }

    @Test fun cancellationStillPropagates() = runBlocking {
        val cancelled = CancellationException("cancelled")
        var invalidated = false
        try {
            injectPlayerPage({ invalidated = true }) { throw cancelled }
            fail("Cancellation was swallowed")
        } catch (actual: CancellationException) { assertSame(cancelled, actual) }
        assertFalse(invalidated)
    }

    @Test fun onlyTheSuccessfulCurrentPlayerDocumentCanTriggerInjection() {
        val player = "https://open.spotify.com/"
        assertTrue(PageInjectionBoundary.finishedDocument(player, player, true, false, null))
        assertFalse(PageInjectionBoundary.finishedDocument(player, "chrome-error://chromewebdata/", true, false, null))
        assertFalse(PageInjectionBoundary.finishedDocument(player, player, true, true, null))
        assertFalse(PageInjectionBoundary.finishedDocument(player, player, false, false, null))
        assertFalse(PageInjectionBoundary.finishedDocument(player, "$player?new-page", true, false, null))
        assertFalse(PageInjectionBoundary.finishedDocument(player, player, true, false, "SECURE_MESSAGE_CHANNEL_UNAVAILABLE"))
    }

    @Test fun queuedEvaluationCannotCrossDocumentEpochsEvenWhenTheNewOriginIsTrusted() {
        assertEquals("PLAYER_PAGE_UNAVAILABLE", PageInjectionBoundary.evaluationError(7, 8, "https://open.spotify.com/", null))
        assertEquals("PLAYER_ORIGIN_REQUIRED", PageInjectionBoundary.evaluationError(7, 7, "https://accounts.spotify.com/", null))
        assertEquals("SECURE_MESSAGE_CHANNEL_UNAVAILABLE", PageInjectionBoundary.evaluationError(7, 7, "https://open.spotify.com/", "SECURE_MESSAGE_CHANNEL_UNAVAILABLE"))
        assertEquals("PLAYER_PAGE_UNAVAILABLE", PageInjectionBoundary.evaluationError(7, 7, "https://open.spotify.com/", null, failed = true))
        assertNull(PageInjectionBoundary.evaluationError(7, 7, "https://open.spotify.com/", null))
    }
}
