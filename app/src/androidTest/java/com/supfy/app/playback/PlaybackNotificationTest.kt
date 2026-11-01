package com.supfy.app.playback

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.supfy.app.testing.WebViewTestActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** Local transport/notification contract only, not real Spotify playback or a device lock test. */
@RunWith(AndroidJUnit4::class)
class PlaybackNotificationTest {
    @Test(timeout=90000) fun playingSnapshotCreatesMediaNotificationAndPauseReachesOwner() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val command=AtomicReference<PlaybackCommand?>()
        val owner=object:PlaybackCommandReceiver {override fun onPlaybackCommand(value:PlaybackCommand){command.set(value)}}
        val scenario=ActivityScenario.launch(WebViewTestActivity::class.java)
        var publishedAt=0L
        try {
            scenario.onActivity {
                publishedAt=android.os.SystemClock.elapsedRealtime()
                PlaybackConnection.attach(owner)
                PlaybackConnection.publish(owner,PlaybackSnapshot(true,true,"Local fixture","Fixture artist",9000,150000,true,true))
                ContextCompat.startForegroundService(context,Intent(context,PlaybackNotificationService::class.java))
            }
            val manager=context.getSystemService(NotificationManager::class.java)
            val deadline=android.os.SystemClock.uptimeMillis()+20_000
            var notification:Notification?=null
            while(notification==null&&android.os.SystemClock.uptimeMillis()<deadline){notification=manager.activeNotifications.firstOrNull {it.id==PlaybackNotificationService.NOTIFICATION_ID}?.notification;if(notification==null)Thread.sleep(100)}
            val actual=requireNotNull(notification){"The media notification must actually be posted"}
            assertEquals("Local fixture",actual.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            assertEquals(Notification.CATEGORY_TRANSPORT,actual.category)
            @Suppress("DEPRECATION") val token=actual.extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
            assertNotNull("A real session token is required for Android lock screen controls",token)
            val controller=MediaController(context,requireNotNull(token))
            assertEquals(PlaybackState.STATE_PLAYING,controller.playbackState?.state)
            val position=requireNotNull(controller.playbackState).position
            assertTrue("Playing position may advance with the system clock",position>=9000L && position<=9000L+android.os.SystemClock.elapsedRealtime()-publishedAt+1000L)
            controller.transportControls.pause()
            val pauseDeadline=android.os.SystemClock.uptimeMillis()+10_000
            while(command.get()==null&&android.os.SystemClock.uptimeMillis()<pauseDeadline)Thread.sleep(100)
            assertEquals(PlaybackCommand.PAUSE,command.get())
        } finally {
            instrumentation.runOnMainSync {PlaybackConnection.detach(owner);context.stopService(Intent(context,PlaybackNotificationService::class.java))}
            val stopDeadline=android.os.SystemClock.uptimeMillis()+5000
            val manager=context.getSystemService(NotificationManager::class.java)
            while(manager.activeNotifications.any {it.id==PlaybackNotificationService.NOTIFICATION_ID}&&android.os.SystemClock.uptimeMillis()<stopDeadline)Thread.sleep(100)
            scenario.close()
        }
    }
}
