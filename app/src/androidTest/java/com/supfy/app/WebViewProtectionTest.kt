package com.supfy.app

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.supfy.app.protection.EngineStatus
import com.supfy.app.protection.ProtectionEngine
import com.supfy.app.protection.ProtectionOptions
import com.supfy.app.protection.ProtectionWebController
import com.supfy.app.web.SupfyChromeClient
import com.supfy.app.web.WebUiState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android WebView, synthetic HTML. No Spotify account, advertisements or DRM tests. */
@RunWith(AndroidJUnit4::class)
class WebViewProtectionTest {
    private fun findWebView(v: View): WebView? {
        if (v is WebView) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) findWebView(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun evaluate(s: ActivityScenario<MainActivity>, js: String): String {
        val done = CountDownLatch(1)
        var result = ""
        s.onActivity { a ->
            val web = findWebView(a.window.decorView) ?: error("WebView missing")
            web.evaluateJavascript(js) { result = it; done.countDown() }
        }
        check(done.await(45, TimeUnit.SECONDS)) { "JS timed out" }
        return result
    }

    private fun fixture(s: ActivityScenario<MainActivity>, ad: Boolean = false, waitForScript: Boolean = true) {
        s.onActivity { a ->
            val web = findWebView(a.window.decorView) ?: error("WebView missing")
            web.stopLoading()
            web.loadDataWithBaseURL("https://open.spotify.com/", """
                <html><body data-fixture="ready"><div data-testid="now-playing-widget">
                ${if (ad) "<i data-testid='track-info-advertiser'></i>" else "Song"}
                </div><audio></audio><div data-testid="ad-banner">Ad</div></body></html>
            """.trimIndent(), "text/html", "UTF-8", "https://open.spotify.com/")
        }
        repeat(60) {
            if (waitForScript) {
                if (evaluate(s, "document.body && document.body.dataset.fixture") == "\"ready\"") return
            } else {
                var ready = false
                s.onActivity { ready = findWebView(it.window.decorView)?.url == "https://open.spotify.com/" }
                if (ready) return
            }
            Thread.sleep(200)
        }
        error("Fixture did not load")
    }

    @Test fun popupRendererLossIsHandledWithoutKillingActivity() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            fixture(s, waitForScript = false)
            var created = false
            var handled = false
            var finishing = true
            s.onActivity { a ->
                val web = findWebView(a.window.decorView)!!
                val chrome = SupfyChromeClient(WebUiState())
                val transport = web.WebViewTransport()
                val message = Message.obtain(Handler(Looper.getMainLooper())).apply { obj = transport }
                created = chrome.onCreateWindow(web, false, true, message)
                val popup = requireNotNull(transport.webView)
                handled = popup.webViewClient.onRenderProcessGone(popup, object : RenderProcessGoneDetail() {
                    override fun didCrash() = true
                    override fun rendererPriorityAtExit() = WebView.RENDERER_PRIORITY_IMPORTANT
                })
                chrome.close(); chrome.close()
                finishing = a.isFinishing
            }
            assertTrue(created)
            assertTrue("Popup renderer loss must be handled", handled)
            assertFalse(finishing)
        }
    }

    @Test fun realWebViewMutesFixtureAndRestoresOnDisable() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            fixture(s, ad = true)
            val engine = ProtectionEngine()
            lateinit var controller: ProtectionWebController
            s.onActivity { a ->
                val web = findWebView(a.window.decorView)!!
                controller = ProtectionWebController(a, engine)
                controller.attach(web)
                controller.pageStarted(web.url)
                controller.configure(ProtectionOptions(enabled = true))
            }
            repeat(40) { if (engine.state.value.status != EngineStatus.ACTIVE) Thread.sleep(200) }
            assertEquals(EngineStatus.ACTIVE, engine.state.value.status)
            assertEquals("true", evaluate(s, "document.querySelector('audio').muted"))
            assertEquals(1L, engine.state.value.detectedAds)
            assertEquals(1L, engine.state.value.mutedMedia)
            assertEquals(1L, engine.state.value.hiddenBanners)
            s.onActivity { controller.configure(ProtectionOptions(enabled = false)) }
            assertEquals("false", evaluate(s, "document.querySelector('audio').muted"))
            assertEquals(EngineStatus.DISABLED, engine.state.value.status)
            s.onActivity { controller.close() }
        }
    }
}
