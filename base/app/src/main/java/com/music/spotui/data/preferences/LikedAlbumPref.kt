@file:Suppress("UNUSED_PARAMETER")

package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.AlbumsModel

private val albumLikesLock = Any()
private val likedAlbums = mutableSetOf<String>()

fun addLikedAlbumId(context: Context, albumId: String) { synchronized(albumLikesLock) { likedAlbums.add(albumId) } }
fun removeLikedAlbumId(context: Context, albumId: String) { synchronized(albumLikesLock) { likedAlbums.remove(albumId) } }
fun isAlbumLiked(context: Context, albumId: String): Boolean = synchronized(albumLikesLock) { albumId in likedAlbums }
fun getLikedAlbumIds(context: Context): Set<Int> = synchronized(albumLikesLock) { likedAlbums.mapNotNull { it.toIntOrNull() }.toSet() }
fun getAlbumsByIds(albumIds: Set<Int>, albums: List<AlbumsModel>): List<AlbumsModel> = albums.filter { it.id in albumIds }
internal fun clearLikedAlbumMemory() { synchronized(albumLikesLock) { likedAlbums.clear() } }
