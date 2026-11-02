@file:Suppress("UNUSED_PARAMETER")

package com.music.spotui.data.preferences

import android.content.Context

private val followedArtists = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

/** Follow state is kept only for this session; names are not retained by this helper. */
fun addFollowedArtist(context: Context, artistId: String, name: String) { if (artistId.isNotBlank()) followedArtists.add(artistId) }
fun removeFollowedArtist(context: Context, artistId: String) { followedArtists.remove(artistId) }
fun isArtistFollowed(context: Context, artistId: String): Boolean = artistId.isNotBlank() && artistId in followedArtists
internal fun clearFollowedArtistMemory() { followedArtists.clear() }
