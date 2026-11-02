package com.music.spotui.privacy

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One disclosure per feature, shared by concurrent callers and presented in arrival order. */
class ConsentQueue<F : Any> {
    private val lock = Any()
    private val waiting = LinkedHashMap<F, CompletableDeferred<Boolean>>()
    private val declined = HashSet<F>()
    private val _pending = MutableStateFlow<F?>(null)
    val pending: StateFlow<F?> = _pending.asStateFlow()

    fun decision(feature: F, granted: Boolean): CompletableDeferred<Boolean> = synchronized(lock) {
        when {
            granted -> CompletableDeferred(true)
            feature in declined -> CompletableDeferred(false)
            else -> waiting.getOrPut(feature) { CompletableDeferred() }.also { _pending.value = waiting.keys.firstOrNull() }
        }
    }
    fun reopen(feature: F) { synchronized(lock) { declined.remove(feature) } }
    fun finish(feature: F, granted: Boolean) {
        val answer = synchronized(lock) {
            if (granted) declined.remove(feature) else declined.add(feature)
            waiting.remove(feature).also { _pending.value = waiting.keys.firstOrNull() }
        }
        answer?.complete(granted)
    }
}
