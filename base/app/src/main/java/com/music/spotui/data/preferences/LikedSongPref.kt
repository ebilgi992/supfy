@file:Suppress("UNUSED_PARAMETER")

package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.SongsModel

private val songLikesLock = Any()
private val likedSongs = mutableSetOf<String>()
private val spotifyLikedSongs = mutableSetOf<String>()

fun addLikedSongId(context: Context, songId: String) { synchronized(songLikesLock) { likedSongs.add(songId) } }
fun removeLikedSongId(context: Context, songId: String) { synchronized(songLikesLock) { likedSongs.remove(songId) } }
fun isSongLiked(context: Context, songId: String): Boolean = synchronized(songLikesLock) { songId in likedSongs }
fun getLikedSongIds(context: Context): Set<Int> = synchronized(songLikesLock) { likedSongs.mapNotNull { it.toIntOrNull() }.toSet() }
fun getSongsByIds(songIds: Set<Int>, songs: List<SongsModel>): List<SongsModel> = songs.filter { it.id in songIds }

/** Fresh Spotify results update only this session's button state. */
fun syncSpotifyLikedSongIds(context: Context, ids: Set<String>, complete: Boolean) {
    synchronized(songLikesLock) {
        if (complete) likedSongs.removeAll(spotifyLikedSongs - ids)
        likedSongs.addAll(ids)
        if (complete) { spotifyLikedSongs.clear(); spotifyLikedSongs.addAll(ids) }
    }
}

/** Called on logout/session loss; durable user commands are owned by LibraryOutbox. */
fun clearLibrarySelectionMemory() {
    synchronized(songLikesLock) { likedSongs.clear(); spotifyLikedSongs.clear() }
    clearLikedAlbumMemory()
    clearFollowedArtistMemory()
}

/** Removes only known cached selection entries from their dedicated legacy preference stores. */
fun clearPersistedLibrarySelectionCaches(context: Context) {
    for (name in listOf("LikedSongs", "LikedAlbums")) {
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        preferences.all.forEach { (key, value) -> if (value is String && value == key) editor.remove(key) }
        editor.apply()
    }
    val artists = context.getSharedPreferences("FollowedArtists", Context.MODE_PRIVATE)
    val artistEditor = artists.edit()
    artists.all.forEach { (key, value) -> if (value is String) artistEditor.remove(key) }
    artistEditor.apply()
    context.getSharedPreferences("LikedSongsFromSpotify", Context.MODE_PRIVATE).edit().remove("ids").apply()
}
