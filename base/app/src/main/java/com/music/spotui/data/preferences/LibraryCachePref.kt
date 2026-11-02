package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.ArtistsModel
import com.music.spotui.data.entity.LibraryEntry

private const val PREF = "LibraryCache"

/** Private library data stays in the active screen/session and is fetched fresh after restart. */
@Suppress("UNUSED_PARAMETER")
fun cacheLibraryEntries(context: Context, entries: List<LibraryEntry>) = Unit

fun getCachedLibraryEntries(context: Context): List<LibraryEntry> {
    clearPrivateLibraryCache(context)
    return emptyList()
}

@Suppress("UNUSED_PARAMETER")
fun cacheFollowedArtists(context: Context, artists: List<ArtistsModel>) = Unit

fun getCachedFollowedArtists(context: Context): List<ArtistsModel> {
    clearPrivateLibraryCache(context)
    return emptyList()
}

/** Removes only the known private cache keys, preserving other options and offline metadata. */
fun clearPrivateLibraryCache(context: Context) {
    context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
        .remove("entries").remove("followedArtists").apply()
}
