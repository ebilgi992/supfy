package com.music.spotui.data

import android.content.Context
import com.music.spotui.data.preferences.clearRecentItems
import com.music.spotui.data.preferences.clearPrivateLibraryCache
import com.music.spotui.data.preferences.clearPersistedLibrarySelectionCaches
import com.music.spotui.data.api.ScreenCache
import java.io.File

/** Removes only known diagnostic/history artifacts, preserving media, settings and sessions. */
object PrivacyCleanup {
    fun run(context: Context) {
        runCatching { clearRecentItems(context) }
        runCatching { clearPrivateLibraryCache(context) }
        runCatching { clearPersistedLibrarySelectionCaches(context) }
        runCatching { ScreenCache.clearPrivateDisk(context) }
        runCatching {
            context.getSharedPreferences("myspoti_prefs", Context.MODE_PRIVATE)
                .edit().remove("CurrentAccountName").apply()
        }
        runCatching {
            val files = context.filesDir
            File(files, "last_crash.txt").delete()
            deleteReports(files)
            deleteExtensionLogs(files)
        }
        runCatching { context.getExternalFilesDir(null)?.let(::deleteReports) }
        runCatching { deleteAdSamples(context.cacheDir) }
    }

    private fun deleteReports(directory: File) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("diagnostic-") && file.name.endsWith(".txt")) {
                runCatching { file.delete() }
            }
        }
    }

    private fun deleteExtensionLogs(files: File) {
        val extensions = File(files, "extensions")
        if (!isDirectDirectory(extensions, files)) return
        extensions.listFiles()?.forEach { extension ->
            if (isDirectDirectory(extension, extensions)) {
                runCatching { File(extension, "log.txt").delete() }
            }
        }
    }

    private fun deleteAdSamples(cache: File) {
        val samples = File(cache, "ad-samples")
        if (isDirectDirectory(samples, cache)) {
            samples.listFiles()?.forEach { file ->
                if (!file.isDirectory) runCatching { file.delete() }
            }
        }
        // Deletes an empty known directory or a link itself, never a linked directory's contents.
        samples.delete()
    }

    private fun isDirectDirectory(directory: File, parent: File): Boolean = runCatching {
        directory.isDirectory && directory.canonicalFile == File(parent.canonicalFile, directory.name)
    }.getOrDefault(false)
}
