package com.music.spotui.data.api

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.InvocationTargetException

/** Preflight rejection tests. These do not claim a successful update download or installation. */
@RunWith(AndroidJUnit4::class)
class AppUpdaterTest {
    @Test fun foreignReleaseIsRefusedWithoutAnInstallerSession() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun shell(command: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
        shell("appops set --user ${android.os.Process.myUid() / 100000} ${context.packageName} REQUEST_INSTALL_PACKAGES allow")
        try {
            assertTrue(AppUpdater.canInstall(context))
            val before = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
            val release = UpdateChecker.Release(
                "99.0.0", "",
                apkUrl = "https://github.com/kota-zo/myspoti/releases/download/v99/myspoti-99.apk",
            )
            AppUpdater.reset()
            AppUpdater.update(context, release)
            assertTrue(AppUpdater.state.value is AppUpdater.State.Failed)
            assertEquals(before, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
            assertFalse(File(context.cacheDir, "updates/supfy-update.apk").exists())
        } finally {
            shell("appops set --user ${android.os.Process.myUid() / 100000} ${context.packageName} REQUEST_INSTALL_PACKAGES default")
            AppUpdater.reset()
        }
    }

    @Test fun installedApkCannotBeReinstalledAsANewerUpdate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Exercise Android's real package and signer parsing on this exact installed APK.
        val verify = AppUpdater::class.java.getDeclaredMethod("verifyApk", Context::class.java, File::class.java)
        verify.isAccessible = true
        try {
            verify.invoke(AppUpdater, context, File(context.applicationInfo.sourceDir))
            fail("Same-version APK must fail preflight")
        } catch (e: InvocationTargetException) {
            assertTrue(e.targetException is IllegalArgumentException)
            assertEquals("Update identity verification failed", e.targetException.message)
        }
    }
}
