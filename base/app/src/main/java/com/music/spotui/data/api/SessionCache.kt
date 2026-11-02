package com.music.spotui.data.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** An account-scoped memory cache. Invalidation also rejects already running old requests. */
internal class SessionCache<K, V> {
    private val requests = Mutex()
    private val state = Any()
    private var generation = 0L
    private val entries = mutableMapOf<K, Pair<Long, V>>()

    suspend fun get(key: K, now: Long, ttlMillis: Long, force: Boolean = false, load: suspend () -> V): V = requests.withLock {
        val epoch = synchronized(state) {
            entries[key]?.takeIf { !force && now >= it.first && now - it.first < ttlMillis }?.let { return@withLock it.second }
            generation
        }
        val answer = load()
        synchronized(state) {
            if (generation != epoch) throw CancellationException("Session changed")
            entries[key] = now to answer
        }
        answer
    }

    fun cached(key: K): V? = synchronized(state) { entries[key]?.second }
    fun clear() = synchronized(state) { generation++; entries.clear() }
}
