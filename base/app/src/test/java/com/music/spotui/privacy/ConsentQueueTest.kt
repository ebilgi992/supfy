package com.music.spotui.privacy

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ConsentQueueTest {
    @Test fun concurrentCallersResumeFromOneExplicitGrant() = runBlocking {
        val queue = ConsentQueue<String>()
        val first = queue.decision("lyrics", false)
        val second = queue.decision("lyrics", false)
        assertSame(first, second)
        assertFalse(first.isCompleted)
        assertEquals("lyrics", queue.pending.value)
        queue.finish("lyrics", true)
        assertTrue(first.await())
        assertTrue(second.await())
        assertNull(queue.pending.value)
    }
    @Test fun denialStopsWaitersAndExplicitRetryCanAskAgain() = runBlocking {
        val queue = ConsentQueue<String>()
        val first = queue.decision("translation", false)
        queue.finish("translation", false)
        assertFalse(first.await())
        assertFalse(queue.decision("translation", false).await())
        assertNull(queue.pending.value)
        queue.reopen("translation")
        val retry = queue.decision("translation", false)
        assertFalse(retry.isCompleted)
        queue.finish("translation", true)
        assertTrue(retry.await())
    }
    @Test fun separateProvidersAreShownInArrivalOrder() = runBlocking {
        val queue = ConsentQueue<String>()
        val lyrics = queue.decision("lyrics", false)
        val video = queue.decision("video", false)
        assertEquals("lyrics", queue.pending.value)
        queue.finish("lyrics", false)
        assertFalse(lyrics.await())
        assertEquals("video", queue.pending.value)
        assertFalse(video.isCompleted)
        queue.finish("video", true)
        assertTrue(video.await())
        assertNull(queue.pending.value)
    }
    @Test fun cancellingOneWaiterDoesNotCancelTheSharedDisclosure() = runBlocking {
        val queue = ConsentQueue<String>()
        val decision = queue.decision("lyrics", false)
        val waiter = launch(start = CoroutineStart.UNDISPATCHED) { decision.await() }
        waiter.cancelAndJoin()
        assertFalse(decision.isCancelled)
        queue.finish("lyrics", true)
        assertTrue(decision.await())
    }
}
