package com.music.spotui.di

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSession
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import app.myspoti.MainActivity
import app.myspoti.SupfyDefaults
import app.myspoti.host.android.PlaybackService
import com.music.spotui.data.local.LocalTrack
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Real V5 LocalPlayer -> SongPlayer -> PlaybackService path with owned PCM, no Spotify account. */
class LocalPlaybackDeviceTest {
    @Test fun localPcmHasNotificationSystemControlsAndCorrectBackgroundPreference() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val oldBackground = SupfyDefaults.backgroundAllowed.value
        val file = File(context.cacheDir, "v5-owned-pcm-fixture.wav")
        val samples = 8000 * 90
        file.outputStream().use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray()).putInt(samples * 2 + 36).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples * 2)
            out.write(header.array())
            val data = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN)
            repeat(samples) { data.putShort((kotlin.math.sin(it * 2 * Math.PI * 220 / 8000) * 1200).toInt().toShort()) }
            out.write(data.array())
        }
        val tracks = (1L..2L).map { LocalTrack(-102900L - it, Uri.fromFile(file), "Supfy PCM $it", "Test", "V5", 90000, Uri.EMPTY) }
        fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
        fun <T> read(action: () -> T): T { var result: T? = null; main { result = action() }; @Suppress("UNCHECKED_CAST") return result as T }
        fun waitFor(message: String, condition: () -> Boolean) {
            val deadline = android.os.SystemClock.elapsedRealtime() + 45_000
            while (!condition()) { if (android.os.SystemClock.elapsedRealtime() >= deadline) fail(message); Thread.sleep(150) }
        }
        fun foreground() {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("MainActivity did not become resumed and focused") {
                read {
                    androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                        .filterIsInstance<MainActivity>()
                        .any { !it.isFinishing && !it.isChangingConfigurations && it.hasWindowFocus() }
                }
            }
        }
        val stage = java.util.concurrent.atomic.AtomicReference("foreground")
        fun checkpoint(value: String) {
            stage.set(value)
            instrumentation.sendStatus(2, android.os.Bundle().apply { putString("v5_media_step", value) })
        }
        // Bounded, test-only method stacks; no user values or application diagnostic export.
        val watch = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
        val samplesTaken = java.util.concurrent.atomic.AtomicInteger()
        watch.scheduleAtFixedRate({
            if (samplesTaken.incrementAndGet() <= 20) {
                val frames = android.os.Looper.getMainLooper().thread.stackTrace
                if (frames.none { it.methodName == "nativePollOnce" })
                    instrumentation.sendStatus(2, android.os.Bundle().apply {
                        putString("v5_main_stage", stage.get())
                        putString("v5_main_methods", frames.take(18).joinToString("\n"))
                    })
            }
        }, 2, 2, java.util.concurrent.TimeUnit.SECONDS)
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            foreground()
            checkpoint("start_pcm")
            main { SupfyDefaults.setBackgroundAllowed(true); SongPlayer.attachEngine(Graph.currentSongState); LocalPlayer.play(tracks, 0) }
            waitFor("Owned PCM did not play") { read { LocalPlayer.isPlaying() && LocalPlayer.position() > 500 } }
            waitFor("No media notification") { notifications.activeNotifications.any { it.id == 1 } }
            checkpoint("pcm_playing_notification_present")
            assertTrue(PlaybackService.isRunning)
            val notification = notifications.activeNotifications.first { it.id == 1 }.notification
            @Suppress("DEPRECATION")
            val token = notification.extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
            assertNotNull(token)
            val controller = MediaController(context, token!!)
            checkpoint("system_pause")
            controller.transportControls.pause()
            waitFor("System pause did not reach LocalPlayer") { read { !LocalPlayer.isPlaying() } }
            controller.transportControls.play()
            waitFor("System play did not reach LocalPlayer") { read { LocalPlayer.isPlaying() } }
            checkpoint("system_next")
            controller.transportControls.skipToNext()
            waitFor("System next did not change the queue") { read { LocalPlayer.current()?.id == tracks[1].id } }
            controller.transportControls.pause()
            waitFor("Pause after next failed") { read { !LocalPlayer.isPlaying() } }
            controller.transportControls.seekTo(0)
            checkpoint("system_previous")
            controller.transportControls.skipToPrevious()
            waitFor("System previous did not change the queue") { read { LocalPlayer.current()?.id == tracks[0].id } }
            controller.transportControls.play()
            waitFor("Resume failed") { read { LocalPlayer.isPlaying() } }
            checkpoint("background_allowed_home")
            val before = read { LocalPlayer.position() }
            assertTrue(instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME))
            waitFor("PCM stopped advancing in background") { read { LocalPlayer.isPlaying() && LocalPlayer.position() > before + 1000 } }
            assertTrue(notifications.activeNotifications.any { it.id == 1 })
            checkpoint("foreground_for_background_disabled")
            foreground()
            main { SupfyDefaults.setBackgroundAllowed(false) }
            checkpoint("background_disabled_home")
            assertTrue(instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME))
            waitFor("Disabled background preference did not pause PCM") { read { !LocalPlayer.isPlaying() } }
            assertFalse(File(context.filesDir, "listen_history.jsonl").exists())
            checkpoint("passed")
        } finally {
            watch.shutdownNow()
            main { LocalPlayer.stop(); PlaybackService.end(); SupfyDefaults.setBackgroundAllowed(oldBackground) }
            file.delete()
        }
    }
}
