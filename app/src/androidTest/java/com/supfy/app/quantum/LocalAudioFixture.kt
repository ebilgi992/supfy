package com.supfy.app.quantum

import android.os.SystemClock
import android.util.Base64
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.supfy.app.testing.WebViewTestActivity
import com.supfy.app.web.UrlPolicy
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.*
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

/** Real decoded PCM; local HTML with a trusted base/history URL, never a Spotify request. */
internal class LocalAudioFixture : AutoCloseable {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val scenario = ActivityScenario.launch(WebViewTestActivity::class.java)
    lateinit var web: WebView
        private set
    private val loaded = CountDownLatch(1)

    init {
        val html = html()
        scenario.onActivity { activity ->
            web = WebView(activity).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                layoutParams = ViewGroup.LayoutParams(-1, -1)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) { loaded.countDown() }
                    // This fixture cannot fetch an account, API, image, or media over the network.
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                        WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
            }
            activity.container.addView(web)
            web.loadDataWithBaseURL(URL, html, "text/html", "UTF-8", URL)
        }
        assertTrue("Local document must load within 30 seconds", loaded.await(30, TimeUnit.SECONDS))
        onMain {
            assertEquals("History URL must preserve the trusted document URL", URL, web.url)
            assertTrue(UrlPolicy.isTrustedPlayerOrigin(web.url))
            assertTrue(web.settings.mediaPlaybackRequiresUserGesture)
        }
        await("PCM WAV must decode", 15_000) { state().getInt("ready") >= 2 }
        assertTrue("No playback before the native user gesture", state().getBoolean("paused"))
        assertEquals("https://open.spotify.com", json("JSON.stringify({origin:location.origin})").getString("origin"))
    }

    fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    fun json(expression: String): JSONObject {
        val result = arrayOfNulls<String>(1)
        val done = CountDownLatch(1)
        onMain { web.evaluateJavascript(expression) { result[0] = it; done.countDown() } }
        assertTrue("WebView JavaScript callback timed out", done.await(8, TimeUnit.SECONDS))
        val value = JSONTokener(requireNotNull(result[0])).nextValue()
        return if (value is String) JSONObject(value) else value as JSONObject
    }

    fun state() = json("""JSON.stringify({time:audio.currentTime,paused:audio.paused,ready:audio.readyState,
        error:audio.error?audio.error.code:0,trustedTap:window.trustedTap===true,
        hiddenClicks:window.hiddenClicks||0,mobileClicks:window.mobileClicks||0})""")

    /** Inject actual Android touch events. evaluateJavascript(button.click()) is deliberately absent. */
    fun nativePlayTap() {
        val rect = json("""JSON.stringify((function(){var r=document.getElementById('mobile-toggle').getBoundingClientRect();
            return {x:r.left+r.width/2,y:r.top+r.height/2,width:innerWidth};})())""")
        var x = 0f
        var y = 0f
        onMain {
            val location = IntArray(2)
            web.getLocationOnScreen(location)
            val scale = web.width.toDouble() / rect.getDouble("width")
            x = (location[0] + rect.getDouble("x") * scale).toFloat()
            y = (location[1] + rect.getDouble("y") * scale).toFloat()
        }
        val down = SystemClock.uptimeMillis()
        val press = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0)
        val release = MotionEvent.obtain(down, down + 80, MotionEvent.ACTION_UP, x, y, 0)
        try {
            instrumentation.sendPointerSync(press)
            instrumentation.sendPointerSync(release)
        } finally { press.recycle(); release.recycle() }
        await("Native tap must start real PCM audio", 10_000) {
            val s = state()
            s.getBoolean("trustedTap") && !s.getBoolean("paused") && s.getDouble("time") > 0.05
        }
        assertEquals("PCM decoder must not report an error", 0, state().getInt("error"))
    }

    fun assertAdvances(label: String, waitMs: Long = 1_600) {
        val before = state()
        assertFalse("$label: audio must be playing before wait", before.getBoolean("paused"))
        SystemClock.sleep(waitMs)
        val after = state()
        assertFalse("$label: audio must remain playing", after.getBoolean("paused"))
        // Thirty-second media loops; modulo handles a late/slow device crossing the loop boundary.
        val elapsed = (after.getDouble("time") - before.getDouble("time") + 30.0) % 30.0
        assertTrue("$label: real currentTime must advance by >= 0.35s, delta=$elapsed", elapsed >= 0.35 && elapsed < 10.0)
    }

    override fun close() {
        onMain { web.stopLoading(); web.loadUrl("about:blank"); (web.parent as? ViewGroup)?.removeView(web); web.destroy() }
        scenario.close()
    }

    companion object {
        const val URL = "https://open.spotify.com/supfy-audio"
        const val TITLE = "Supfy quantum local PCM"

        fun await(message: String, timeoutMs: Long, condition: () -> Boolean) {
            val deadline = SystemClock.uptimeMillis() + timeoutMs
            while (SystemClock.uptimeMillis() < deadline) {
                if (condition()) return
                SystemClock.sleep(100)
            }
            assertTrue(message, condition())
        }

        private fun html(): String {
            val rate = 16_000
            val samples = rate * 30
            val wav = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
            wav.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1.toShort()).putShort(1.toShort()).putInt(rate).putInt(rate * 2)
                .putShort(2.toShort()).putShort(16.toShort()).put("data".toByteArray()).putInt(samples * 2)
            for (i in 0 until samples) wav.putShort((sin(2 * PI * 440 * i / rate) * 1_600).toInt().toShort())
            val data = Base64.encodeToString(wav.array(), Base64.NO_WRAP)
            return """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                <style>body{margin:0;font:18px sans-serif}button{margin:40px 20px;width:260px;height:100px}</style></head><body>
                <div data-testid="now-playing-widget"><span data-testid="context-item-info-title">$TITLE</span></div>
                <button style="display:none" data-testid="control-button-playpause" aria-label="Duraklat"
                    onclick="window.hiddenClicks=(window.hiddenClicks||0)+1">Hidden desktop</button>
                <div data-testid="mobile-player"><button id="mobile-toggle" aria-label="Oynat">Play local sine</button></div>
                <audio id="audio" preload="auto" loop src="data:audio/wav;base64,$data"></audio>
                <script>
                var audio=document.getElementById('audio'),toggle=document.getElementById('mobile-toggle');
                toggle.onclick=function(event){window.mobileClicks=(window.mobileClicks||0)+1;
                  if(event.isTrusted)window.trustedTap=true;
                  if(audio.paused){audio.play().catch(function(e){window.playError=String(e);});}else{audio.pause();}};
                audio.onplay=function(){toggle.setAttribute('aria-label','Duraklat');};
                audio.onpause=function(){toggle.setAttribute('aria-label','Oynat');};
                </script></body></html>""".trimIndent()
        }
    }
}
