package privacy

import android.content.Context
import com.music.spotui.data.preferences.*
import java.nio.file.Files

fun main() {
    val directory = Files.createTempDirectory("v5-selection-canary-").toFile()
    try {
        val context = Context(directory)
        val canary = "SYNTHETIC_SELECTION_CANARY_57319"
        addLikedSongId(context, canary)
        addLikedAlbumId(context, canary)
        addFollowedArtist(context, canary, canary)
        check(isSongLiked(context, canary) && isAlbumLiked(context, canary) && isArtistFollowed(context, canary))
        for (name in listOf("LikedSongs", "LikedAlbums", "FollowedArtists")) {
            check(context.getSharedPreferences(name, 0).values.isEmpty()) { "Library selection cache was persisted" }
        }
        syncSpotifyLikedSongIds(context, setOf("1", "2"), true)
        syncSpotifyLikedSongIds(context, setOf("2", "3"), true)
        check(!isSongLiked(context, "1") && isSongLiked(context, "2") && isSongLiked(context, "3"))
        check(isSongLiked(context, canary))
        check(context.getSharedPreferences("LikedSongsFromSpotify", 0).values.isEmpty())
        removeLikedSongId(context, canary)
        removeLikedAlbumId(context, canary)
        removeFollowedArtist(context, canary)
        check(!isSongLiked(context, canary) && !isAlbumLiked(context, canary) && !isArtistFollowed(context, canary))
        addLikedSongId(context, canary)
        addLikedAlbumId(context, canary)
        addFollowedArtist(context, canary, canary)
        clearLibrarySelectionMemory()
        check(!isSongLiked(context, canary) && !isAlbumLiked(context, canary) && !isArtistFollowed(context, canary))
        for (name in listOf("LikedSongs", "LikedAlbums")) {
            val prefs = context.getSharedPreferences(name, 0)
            prefs.values[canary] = canary
            prefs.values["unrelated-option"] = "keep-option"
        }
        context.getSharedPreferences("FollowedArtists", 0).values[canary] = canary
        context.getSharedPreferences("LikedSongsFromSpotify", 0).values["ids"] = setOf(canary)
        context.getSharedPreferences("LibraryWrites", 0).values["book"] = "keep-unsent-command"
        clearPersistedLibrarySelectionCaches(context)
        for (name in listOf("LikedSongs", "LikedAlbums")) {
            val prefs = context.getSharedPreferences(name, 0)
            check(!prefs.values.containsKey(canary))
            check(prefs.values["unrelated-option"] == "keep-option")
        }
        check(!context.getSharedPreferences("FollowedArtists", 0).values.containsKey(canary))
        check(!context.getSharedPreferences("LikedSongsFromSpotify", 0).values.containsKey("ids"))
        check(context.getSharedPreferences("LibraryWrites", 0).values["book"] == "keep-unsent-command")
        println("PASS: favorite/follow state is functional without private disk copies")
    } finally { directory.deleteRecursively() }
}
