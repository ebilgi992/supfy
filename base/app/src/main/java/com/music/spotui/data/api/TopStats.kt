package com.music.spotui.data.api

import app.myspoti.api.TimeRange
import app.myspoti.api.TopContent
import app.myspoti.host.android.MyspotiRuntime

/**
 * The account's top artists and tracks as Spotify ranks them (`userTopContent`), per window,
 * kept for half an hour: the stats screen and every artist page's "your top artist" line share
 * one answer instead of asking again on each open.
 */
object TopStats {
    const val LIMIT = 50
    private const val TTL_MS = 30 * 60_000L

    private val cache = SessionCache<TimeRange, TopContent>()

    suspend fun get(range: TimeRange, force: Boolean = false): TopContent =
        cache.get(range, System.currentTimeMillis(), TTL_MS, force) {
            MyspotiRuntime.api.users.top(range, LIMIT)
        }

    /** What is cached, without asking Spotify. */
    fun cached(range: TimeRange): TopContent? = cache.cached(range)

    /** Forgets answers and prevents in-flight requests from restoring the old account. */
    fun clear() = cache.clear()

    /**
     * Where an artist ranks in the account's top artists, as the badge on its page says it:
     * the shortest window that has it in its top 10 (the official "top artist this month"
     * comes first), otherwise its best rank in any window. [ranked] holds each window's artist
     * uris, best first. Null when the artist is in none of them.
     */
    fun rankOf(artistUri: String, ranked: Map<TimeRange, List<String>>): Pair<TimeRange, Int>? {
        val ranks = TimeRange.entries.mapNotNull { r ->
            ranked[r]?.indexOf(artistUri)?.takeIf { it >= 0 }?.let { r to it + 1 }
        }
        return ranks.firstOrNull { it.second <= 10 } ?: ranks.minByOrNull { it.second }
    }
}
