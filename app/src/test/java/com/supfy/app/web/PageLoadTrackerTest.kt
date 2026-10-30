package com.supfy.app.web

import org.junit.Assert.*
import org.junit.Test

class PageLoadTrackerTest {
    @Test fun visibleOutgoingContentCannotMarkANewNavigationReady() {
        val load=PageLoadTracker()
        val document=load.begin()
        load.observed(document,true)
        assertTrue("The new document has not committed yet",load.waiting)
        load.timeout(document)
        assertEquals(PageLoadProblem.TIMEOUT,load.problem)
    }
    @Test fun anEmptyCommittedDocumentDoesNotCountAsSuccessfulLoginRendering() {
        val load=PageLoadTracker()
        val ticket=load.begin()
        load.committed(ticket)
        load.observed(ticket,false)
        assertTrue(load.waiting)
        load.timeout(ticket)
        assertEquals(PageLoadProblem.EMPTY,load.problem)
    }
    @Test fun aLateCallbackFromThePreviousDocumentCannotClearTheNewFailure() {
        val load=PageLoadTracker()
        val first=load.begin()
        val second=load.begin()
        load.failed(second,PageLoadProblem.HTTP)
        load.observed(first,true)
        assertEquals(PageLoadProblem.HTTP,load.problem)
        assertFalse(load.waiting)
    }
    @Test fun timeoutWithoutAnyJavascriptResponseIsVisibleAndRetryCanRecover() {
        val load=PageLoadTracker()
        val first=load.begin()
        load.timeout(first)
        assertEquals(PageLoadProblem.TIMEOUT,load.problem)
        val second=load.begin()
        load.committed(second)
        load.observed(second,true)
        load.timeout(second)
        assertNull(load.problem)
        assertFalse(load.waiting)
    }
}
