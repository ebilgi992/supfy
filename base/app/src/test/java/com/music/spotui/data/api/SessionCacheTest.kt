package com.music.spotui.data.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SessionCacheTest {
    @Test fun clearingDuringARequestCannotPublishThePreviousAccount() = runBlocking {
        val cache = SessionCache<String, String>()
        val release = CompletableDeferred<Unit>()
        val old = async(start = CoroutineStart.UNDISPATCHED) {
            cache.get("top", 0L, 1000L) { release.await(); "old-account" }
        }
        cache.clear()
        release.complete(Unit)
        try { old.await(); fail("A stale session response must be cancelled") }
        catch (_: CancellationException) { }
        assertNull(cache.cached("top"))
        assertEquals("new-account", cache.get("top", 1L, 1000L) { "new-account" })
        assertEquals("new-account", cache.cached("top"))
    }

    @Test fun freshAnswersAreSharedButExpiredAndForcedAnswersReload() = runBlocking {
        val cache = SessionCache<String, Int>()
        var calls = 0
        suspend fun get(now: Long, force: Boolean = false) = cache.get("x", now, 100L, force) { ++calls }
        assertEquals(1, get(0))
        assertEquals(1, get(99))
        assertEquals(2, get(100))
        assertEquals(3, get(101, true))
        cache.clear()
        assertEquals(4, get(102))
    }
}
