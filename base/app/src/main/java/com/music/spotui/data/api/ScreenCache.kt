package com.music.spotui.data.api

import android.content.Context
import android.util.AtomicFile
import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * General account screen data is never persisted. Only pages explicitly selected for download
 * keep their existing offline metadata, alongside the user's downloaded collections.
 */
object ScreenCache {
    private const val DIR = "screen_cache1"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val dir: File by lazy { File(Graph.app.filesDir, DIR).also { it.mkdirs() } }

    val home = Entry("home", HomeFeedModel.serializer())
    val libraryRoot = Entry("library_root", ListSerializer(LibraryEntry.serializer()))
    val liked = Entry("liked", ListSerializer(SongsModel.serializer()))

    fun likedFullAt(): Long? = null
    fun markLikedFull() = Unit

    fun albumPage(id: String) = Entry("album_page_$id", com.music.spotui.data.entity.ContextPageModel.serializer())
    @Suppress("UNUSED_PARAMETER")
    fun pruneAlbumPages(keep: Int) = Unit

    fun page(id: String) = Entry("page_$id", com.music.spotui.data.entity.ContextPageModel.serializer())
    @Suppress("UNUSED_PARAMETER")
    fun pageSavedAt(id: String): Long? = null
    @Suppress("UNUSED_PARAMETER")
    fun prunePages(keep: Int) = Unit

    /** Metadata needed to open a user-selected downloaded collection without the network. */
    fun offlinePage(id: String) = Entry("offline_page_$id", com.music.spotui.data.entity.ContextPageModel.serializer(), true)

    fun dropOfflinePage(id: String) {
        io.launch { runCatching { AtomicFile(File(dir, "offline_page_$id.json")).delete() } }
    }

    class Entry<T>(private val name: String, private val serializer: KSerializer<T>, private val offline: Boolean = false) {
        private val file get() = AtomicFile(File(dir, "$name.json"))

        fun read(): T? {
            if (!offline) return null
            return runCatching {
                if (!file.baseFile.exists()) return null
                json.decodeFromString(serializer, file.readFully().decodeToString())
            }.getOrNull()
        }

        fun write(value: T) {
            if (!offline) return
            io.launch {
                runCatching {
                    val f = file
                    val out = f.startWrite()
                    try {
                        out.write(json.encodeToString(serializer, value).encodeToByteArray())
                        f.finishWrite(out)
                    } catch (t: Throwable) { f.failWrite(out); throw t }
                }
            }
        }
    }

    /** Invalidation removes general private cache files while preserving selected offline pages. */
    fun clear() { runCatching { clearPrivateDisk(Graph.app) } }

    fun clearPrivateDisk(context: Context) {
        val files = context.filesDir
        val cache = File(files, DIR)
        if (cache.canonicalFile != File(files.canonicalFile, DIR)) return
        cache.listFiles()?.forEach { file ->
            val name = file.name.removeSuffix(".bak").removeSuffix(".new")
            val known = name in setOf("home.json", "library_root.json", "liked.json", "liked_full.stamp") ||
                (name.startsWith("page_") || name.startsWith("album_page_")) && name.endsWith(".json")
            if (known && !file.isDirectory) runCatching { file.delete() }
        }
    }
}
