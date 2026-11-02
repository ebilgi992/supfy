package privacy

import android.content.Context
import com.music.spotui.data.api.ScreenCache
import com.music.spotui.di.Graph
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

fun main() = runBlocking {
    val directory = Files.createTempDirectory("v5-screen-cache-canary-").toFile()
    try {
        Graph.app = Context(directory)
        val cache = File(directory, "screen_cache1").apply { mkdirs() }
        val canary = "SYNTHETIC_SCREEN_CACHE_CANARY_57319"
        File(cache, "home.json").writeText("\"$canary\"")
        check(ScreenCache.home.read() == null) { "Private home cache was exposed" }
        ScreenCache.home.write(canary)
        ScreenCache.libraryRoot.write(listOf(canary))
        ScreenCache.liked.write(listOf(canary))
        ScreenCache.page("privateplaylist").write(canary)
        ScreenCache.albumPage("privatealbum").write(canary)
        ScreenCache.markLikedFull()
        check(ScreenCache.likedFullAt() == null)
        check(ScreenCache.pageSavedAt("privateplaylist") == null)
        val offline = File(cache, "offline_page_downloaded.json").apply { writeText("\"keep-offline-metadata\"") }
        val unknown = File(cache, "unrelated-artwork.png").apply { writeText("keep-artwork") }
        ScreenCache.clear()
        delay(300)
        check(offline.readText() == "\"keep-offline-metadata\"") { "Clearing private cache changed selected offline metadata" }
        check(unknown.readText() == "keep-artwork")
        check(cache.listFiles()!!.map { it.name }.toSet() == setOf(offline.name, unknown.name)) { "Private screen cache was retained or recreated" }
        check(ScreenCache.offlinePage("downloaded").read() == "keep-offline-metadata")
        ScreenCache.offlinePage("downloaded").write("updated-offline-metadata")
        repeat(100) { if (!offline.readText().contains("updated-offline-metadata")) delay(10) }
        check(ScreenCache.offlinePage("downloaded").read() == "updated-offline-metadata") { "Selected offline metadata could not be saved" }
        println("PASS: private screen cache disabled; selected offline metadata preserved and functional")
    } finally {
        directory.deleteRecursively()
    }
}
