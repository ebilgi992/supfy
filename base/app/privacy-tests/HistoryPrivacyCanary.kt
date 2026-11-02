package privacy

import android.content.Context
import com.music.spotui.data.PrivacyCleanup
import com.music.spotui.data.local.ListenHistory
import com.music.spotui.data.preferences.*
import java.io.File
import java.nio.file.Files

fun main() {
    val directory = Files.createTempDirectory("v5-history-canary-").toFile()
    try {
        val context = Context(directory)
        val canary = "SYNTHETIC_HISTORY_CANARY_57319"
        val playbackPreferences = context.getSharedPreferences("PlaybackState", 0)
        playbackPreferences.values["song"] = "keep-playback"
        val history = File(directory, "listen_history.jsonl").apply { writeText(canary) }
        val session = File(directory, "session.json").apply { writeText("keep-session") }
        ListenHistory.init(context)
        check(!history.exists()) { "Legacy listening history was retained" }
        ListenHistory.record(ListenHistory.Listen(1, canary, canary, canary, canary, canary, 60_000, ListenHistory.Source.SPOTIFY))
        check(!history.exists()) { "Recording recreated listening history" }
        check(ListenHistory.all().isEmpty()) { "Recording retained personal history" }
        check(ListenHistory.recap(0).listens == 0) { "Recap retained personal history" }
        ListenHistory.clear()
        check(session.readText() == "keep-session") { "History cleanup changed session data" }

        val libraryPreferences = context.getSharedPreferences("LibraryCache", Context.MODE_PRIVATE)
        libraryPreferences.values["entries"] = canary
        libraryPreferences.values["followedArtists"] = canary
        libraryPreferences.values["unrelated-option"] = "keep-library-option"
        check(getCachedLibraryEntries(context).isEmpty())
        check(getCachedFollowedArtists(context).isEmpty())
        check(!libraryPreferences.values.containsKey("entries")) { "Legacy private library cache was retained" }
        check(!libraryPreferences.values.containsKey("followedArtists")) { "Legacy followed artist cache was retained" }
        cacheLibraryEntries(context, listOf(com.music.spotui.data.entity.LibraryEntry(canary, canary, canary, canary, true)))
        cacheFollowedArtists(context, listOf(com.music.spotui.data.entity.ArtistsModel(canary, canary, canary)))
        check(!libraryPreferences.values.containsKey("entries")) { "Private library cache was recreated" }
        check(!libraryPreferences.values.containsKey("followedArtists")) { "Followed artist cache was recreated" }
        check(libraryPreferences.values["unrelated-option"] == "keep-library-option")

        val preferences = context.getSharedPreferences("RecentItems", Context.MODE_PRIVATE)
        preferences.values["items"] = canary
        preferences.values["unrelated-option"] = "keep-option"
        check(getRecentItems(context).isEmpty()) { "Selected search history was exposed" }
        check(!preferences.values.containsKey("items")) { "Legacy selected search history was retained" }
        addRecentItem(context, RecentItem("song", canary, canary))
        check(!preferences.values.containsKey("items")) { "Selecting a result recreated search history" }
        removeRecentItem(context, RecentItem("song", canary, canary))
        check(preferences.values["unrelated-option"] == "keep-option") { "Search cleanup changed unrelated preferences" }
        check(playbackPreferences.values["song"] == "keep-playback")
        val external = context.getExternalFilesDir(null)!!.apply { mkdirs() }
        val cache = context.cacheDir.apply { mkdirs() }
        val reports = listOf(
            File(directory, "last_crash.txt"),
            File(directory, "diagnostic-20261101-100000.txt"),
            File(external, "diagnostic-20261101-100001.txt"),
            File(cache, "ad-samples/request.json"),
            File(directory, "extensions/test/log.txt"),
        ).onEach { it.parentFile!!.mkdirs(); it.writeText(canary) }
        val preserved = listOf(
            File(directory, "extensions/test/settings.json"),
            File(directory, "extensions/test/storage.json"),
            File(directory, "extensions/registry.json"),
            File(external, "download.mp3"),
            File(cache, "other-artifact"),
            File(directory, "diagnostic-not-a-report.db"),
        ).onEach { it.parentFile!!.mkdirs(); it.writeText("keep") }
        preferences.values["items"] = canary
        libraryPreferences.values["entries"] = canary
        libraryPreferences.values["followedArtists"] = canary
        val generalPreferences = context.getSharedPreferences("myspoti_prefs", Context.MODE_PRIVATE)
        generalPreferences.values["CurrentAccountName"] = canary
        generalPreferences.values["LoggedIn"] = "true"
        generalPreferences.values["chosen-theme"] = "keep-theme"
        PrivacyCleanup.run(context)
        check(!preferences.values.containsKey("items")) { "Startup retained selected search history" }
        check(!libraryPreferences.values.containsKey("entries"))
        check(!libraryPreferences.values.containsKey("followedArtists"))
        check(!generalPreferences.values.containsKey("CurrentAccountName"))
        check(generalPreferences.values["LoggedIn"] == "true")
        check(generalPreferences.values["chosen-theme"] == "keep-theme")
        check(reports.none { it.exists() }) { "Known legacy diagnostic artifacts were retained" }
        check(preserved.all { it.readText() == "keep" }) { "Diagnostic cleanup changed unrelated data" }
        check(session.readText() == "keep-session") { "Diagnostic cleanup changed session data" }
        PrivacyCleanup.run(context)
        val outside = Files.createTempDirectory("v5-cleanup-unrelated-").toFile()
        try {
            val outsideLog = File(outside, "log.txt").apply { writeText("keep-outside") }
            Files.createSymbolicLink(File(directory, "extensions/link").toPath(), outside.toPath())
            Files.createSymbolicLink(File(cache, "ad-samples").toPath(), outside.toPath())
            PrivacyCleanup.run(context)
            check(outsideLog.readText() == "keep-outside") { "Cleanup followed a directory link outside its scope" }
        } finally {
            outside.deleteRecursively()
        }
        check(playbackPreferences.values["song"] == "keep-playback")
        println("PASS: history and diagnostic canaries removed; session, options and media preserved")
    } finally {
        directory.deleteRecursively()
    }
}
