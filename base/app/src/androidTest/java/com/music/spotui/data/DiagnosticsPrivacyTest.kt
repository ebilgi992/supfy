package com.music.spotui.data

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import app.myspoti.host.android.LastExit
import app.myspoti.host.android.Logbook
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class DiagnosticsPrivacyTest {
    @Test fun disabledExportCannotReadStorageOrCreateAReport() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(target.cacheDir, "synthetic-diagnostic-export-test")
        directory.deleteRecursively()
        directory.mkdirs()
        var storageReads = 0
        val context = object : ContextWrapper(target) {
            override fun getFilesDir(): File { storageReads++; return directory }
            override fun getExternalFilesDir(type: String?): File { storageReads++; return directory }
        }
        try {
            try {
                Logbook.export(context)
                fail("Diagnostic export must be disabled")
            } catch (_: UnsupportedOperationException) {
                // An export must fail before consulting storage.
            }
            assertEquals(0, storageReads)
            assertEquals(0, directory.listFiles()!!.size)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun disabledCollectionDoesNotInstallCrashHandlerOrReadLogcat() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        LastExit.install(context)
        assertSame(previous, Thread.getDefaultUncaughtExceptionHandler())
        assertEquals("", BetaLogs.recent())
    }
}
