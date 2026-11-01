package com.supfy.app.quantum

import android.app.Notification
import android.app.NotificationManager
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.supfy.app.playback.PlaybackNotificationService
import com.supfy.app.playback.PlaybackWebController
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Local PCM playback and Android transport evidence; no account, Spotify network, or DOM stand-in. */
@RunWith(AndroidJUnit4::class)
class RealAudioPlaybackTest {
    @Test(timeout = 120_000)
    fun nativeTapBackgroundAudioAndMediaSessionPauseUseVisibleMobileControl() {
        LocalAudioFixture().use { fixture ->
            val context = fixture.instrumentation.targetContext
            val manager = context.getSystemService(NotificationManager::class.java)
            lateinit var playback: PlaybackWebController
            fixture.onMain {
                playback = PlaybackWebController(context)
                playback.setEnabled(true)
                playback.attach(fixture.web)
            }
            try {
                fixture.nativePlayTap()
                fixture.assertAdvances("Visible activity after native gesture")
                var posted: Notification? = null
                LocalAudioFixture.await("A fresh real-audio notification must be posted", 20_000) {
                    posted = manager.activeNotifications.firstOrNull {
                        it.id == PlaybackNotificationService.NOTIFICATION_ID &&
                            it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() == LocalAudioFixture.TITLE
                    }?.notification
                    posted != null && posted!!.extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION) != null
                }
                val notification = requireNotNull(posted)
                assertEquals(Notification.CATEGORY_TRANSPORT, notification.category)
                assertEquals("android.app.Notification\$MediaStyle", notification.extras.getString(Notification.EXTRA_TEMPLATE))
                @Suppress("DEPRECATION")
                val token = notification.extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
                assertNotNull("Actual framework MediaSession token required", token)
                val session = MediaController(context, requireNotNull(token))
                LocalAudioFixture.await("MediaSession must publish playing", 10_000) {
                    session.playbackState?.state == PlaybackState.STATE_PLAYING
                }

                fixture.onMain { playback.setForeground(false) }
                fixture.scenario.moveToState(Lifecycle.State.CREATED)
                assertEquals("Host must actually be stopped", Lifecycle.State.CREATED, fixture.scenario.state)
                fixture.assertAdvances("Stopped activity with background controller", waitMs = 2_000)

                // The hidden exact desktop control used to suppress this scoped mobile-label fallback.
                // Its onclick never pauses audio; only the visible mobile button can satisfy this check.
                session.transportControls.pause()
                LocalAudioFixture.await("MediaSession pause must reach the real HTMLAudioElement", 12_000) {
                    fixture.state().getBoolean("paused")
                }
                val paused = fixture.state()
                assertEquals("Hidden desktop control must never be activated", 0, paused.getInt("hiddenClicks"))
                assertEquals("Native play and MediaSession pause must both use mobile control", 2, paused.getInt("mobileClicks"))
                Thread.sleep(500)
                assertEquals("Paused media clock must stop", paused.getDouble("time"), fixture.state().getDouble("time"), 0.15)
                LocalAudioFixture.await("Session must reflect actual paused playback", 12_000) {
                    session.playbackState?.state == PlaybackState.STATE_PAUSED
                }
                fixture.onMain { playback.setEnabled(false) }
                LocalAudioFixture.await("Disabling background playback must remove notification", 5_000) {
                    manager.activeNotifications.none { it.id == PlaybackNotificationService.NOTIFICATION_ID }
                }
            } finally {
                fixture.onMain { playback.close() }
                fixture.scenario.moveToState(Lifecycle.State.RESUMED)
            }
        }
    }
}
