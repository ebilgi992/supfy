package com.music.spotui.data.preferences

import android.content.Context

/**
 * One "Recent searches" entry — Spotify-style: not the typed query but the
 * actual item the user opened from the results (song / artist / album / show).
 * Carries enough payload to re-launch the item on tap.
 */
data class RecentItem(
    val type: String,            // "song" | "artist" | "album" | "show"
    val key: String,             // stable id for dedupe (spotify id, or name)
    val name: String,
    val singer: String = "",     // artist line (songs/albums)
    val image: String = "",
    // Song-only payload so a tap can replay it.
    val songId: Int = -1,
    val songAlbum: String = "",
    val songUrl: String = "",
    val spotifyTrackId: String = "",
    val explicit: Boolean = false,
    val durationMs: Int = 0,
)

private const val PREF = "RecentItems"
private const val KEY = "items"
/** Forget only the old selected-search-history key, preserving all other preferences. */
fun getRecentItems(context: Context): List<RecentItem> {
    clearRecentItems(context)
    return emptyList()
}

@Suppress("UNUSED_PARAMETER")
fun addRecentItem(context: Context, item: RecentItem) = Unit

@Suppress("UNUSED_PARAMETER")
fun removeRecentItem(context: Context, item: RecentItem) = clearRecentItems(context)

fun clearRecentItems(context: Context) {
    context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply()
}
