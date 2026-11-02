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
        fun foreground() { context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); Thread.sleep(1500) }
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            foreground()
            main { SupfyDefaults.setBackgroundAllowed(true); SongPlayer.attachEngine(Graph.currentSongState); LocalPlayer.play(tracks, 0) }
            waitFor("Owned PCM did not play") { read { LocalPlayer.isPlaying() && LocalPlayer.position() > 500 } }
            waitFor("No media notification") { notifications.activeNotifications.any { it.id == 1 } }
            assertTrue(PlaybackService.isRunning)
            val notification = notifications.activeNotifications.first { it.id == 1 }.notification
            @Suppress("DEPRECATION")
            val token = notification.extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
            assertNotNull(token)
            val controller = MediaController(context, token!!)
            controller.transportControls.pause()
            waitFor("System pause did not reach LocalPlayer") { read { !LocalPlayer.isPlaying() } }
            controller.transportControls.play()
            waitFor("System play did not reach LocalPlayer") { read { LocalPlayer.isPlaying() } }
            controller.transportControls.skipToNext()
            waitFor("System next did not change the queue") { read { LocalPlayer.current()?.id == tracks[1].id } }
            controller.transportControls.pause()
            waitFor("Pause after next failed") { read { !LocalPlayer.isPlaying() } }
            controller.transportControls.seekTo(0)
            controller.transportControls.skipToPrevious()
            waitFor("System previous did not change the queue") { read { LocalPlayer.current()?.id == tracks[0].id } }
            controller.transportControls.play()
            waitFor("Resume failed") { read { LocalPlayer.isPlaying() } }
            val before = read { LocalPlayer.position() }
            assertTrue(instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME))
            waitFor("PCM stopped advancing in background") { read { LocalPlayer.isPlaying() && LocalPlayer.position() > before + 1000 } }
            assertTrue(notifications.activeNotifications.any { it.id == 1 })
            foreground()
            main { SupfyDefaults.setBackgroundAllowed(false) }
            assertTrue(instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME))
            waitFor("Disabled background preference did not pause PCM") { read { !LocalPlayer.isPlaying() } }
            assertFalse(File(context.filesDir, "listen_history.jsonl").exists())
        } finally {
            main { LocalPlayer.stop(); PlaybackService.end(); SupfyDefaults.setBackgroundAllowed(oldBackground) }
            file.delete()
        }
    }
}
