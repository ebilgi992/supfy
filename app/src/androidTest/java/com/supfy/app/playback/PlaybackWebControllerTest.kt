package com.supfy.app.playback

import android.app.Notification
import android.app.NotificationManager
import android.media.session.MediaController
import android.media.session.MediaSession
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.supfy.app.testing.WebViewTestActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real service -> MediaSession -> WebView DOM command. Local fixture, no Spotify audio/account. */
@RunWith(AndroidJUnit4::class)
class PlaybackWebControllerTest {
 @Test(timeout=120000) fun notificationPauseReachesLocalPlayerAndDisableRemovesNotification() {
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  val scenario=ActivityScenario.launch(WebViewTestActivity::class.java)
  lateinit var web:WebView;lateinit var controller:PlaybackWebController
  var initialized=false
  val commit=CountDownLatch(1)
  try {
   scenario.onActivity {activity->
    web=WebView(activity).apply {settings.javaScriptEnabled=true;layoutParams=android.view.ViewGroup.LayoutParams(-1,-1)
     webViewClient=object:WebViewClient(){override fun onPageCommitVisible(v:WebView,u:String?){commit.countDown()}}}
    activity.container.addView(web)
    web.loadDataWithBaseURL("https://open.spotify.com/supfy-local-media","""<html><body>
     <div data-testid="now-playing-widget"><span data-testid="context-item-info-title">Yerel test şarkısı</span></div>
     <button data-testid="control-button-playpause" aria-label="Duraklat" onclick="this.setAttribute('aria-label','Oynat');document.body.dataset.paused='yes'">Pause</button>
     </body></html>""","text/html","UTF-8","https://open.spotify.com/supfy-local-media")
    controller=PlaybackWebController(activity.applicationContext);controller.setEnabled(true);initialized=true
   }
   assertTrue(commit.await(30,TimeUnit.SECONDS))
   scenario.onActivity {
    assertTrue("The fixture must expose the same trusted top-level URL as production",com.supfy.app.web.UrlPolicy.isTrustedPlayerOrigin(web.url))
    controller.attach(web)
   }
   val manager=instrumentation.targetContext.getSystemService(NotificationManager::class.java)
   val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20)
   var posted:Notification?=null
   while(posted==null&&System.nanoTime()<deadline) {
    posted=manager.activeNotifications.firstOrNull {it.id==PlaybackNotificationService.NOTIFICATION_ID && it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()=="Yerel test şarkısı"}?.notification
    if(posted==null)Thread.sleep(100)
   }
   scenario.onActivity {java.io.File(it.cacheDir,"playback-test-state.txt").writeText(controller.health.value.toString()+PlaybackNotificationService.health.value.toString())}
   val notification=requireNotNull(posted){"Fresh local player notification must be posted"}
   assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString().contains("Yerel test"))
   @Suppress("DEPRECATION") val token=notification.extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
   assertNotNull(token)
   scenario.onActivity {controller.setForeground(false)}
   MediaController(instrumentation.targetContext,token!!).transportControls.pause()
   var paused=false
   val pauseDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
   while(!paused&&System.nanoTime()<pauseDeadline) {
    val done=CountDownLatch(1)
    scenario.onActivity {web.evaluateJavascript("document.body.dataset.paused==='yes'"){v->paused=v=="true";done.countDown()}}
    assertTrue(done.await(5,TimeUnit.SECONDS));if(!paused)Thread.sleep(100)
   }
   assertTrue("Notification command must reach the actual local WebView",paused)
   scenario.onActivity {controller.setEnabled(false)}
   val stopDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
   while(manager.activeNotifications.any {it.id==PlaybackNotificationService.NOTIFICATION_ID}&&System.nanoTime()<stopDeadline)Thread.sleep(100)
   assertFalse(manager.activeNotifications.any {it.id==PlaybackNotificationService.NOTIFICATION_ID})
  } finally {
   scenario.onActivity {if(initialized){controller.close();it.container.removeView(web);web.destroy()}}
   scenario.close()
  }
 }
}
