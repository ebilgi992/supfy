package com.supfy.app.web

enum class PageLoadProblem { EMPTY, TIMEOUT, HTTP, NETWORK, SSL, RENDERER }

/** One document at a time. Stale JS/timeout callbacks cannot complete a newer navigation. */
class PageLoadTracker {
    private var ticket=0L
    private var sawEmpty=false
    private var committed=false
    var waiting=false
        private set
    var problem: PageLoadProblem?=null
        private set

    fun begin(): Long {
        ticket++;waiting=true;sawEmpty=false;committed=false;problem=null
        return ticket
    }
    val canProbe: Boolean get()=waiting && committed
    fun committed(document: Long) {if(document==ticket && waiting)committed=true}
    fun observed(document: Long, visible: Boolean) {
        if(document!=ticket || !canProbe)return
        if(visible)waiting=false else sawEmpty=true
    }
    fun timeout(document: Long) {
        if(document==ticket && waiting)failed(document,if(sawEmpty)PageLoadProblem.EMPTY else PageLoadProblem.TIMEOUT)
    }
    fun failed(document: Long, failure: PageLoadProblem) {
        if(document!=ticket)return
        waiting=false;problem=failure
    }
    fun cancel() {ticket++;waiting=false}
}
