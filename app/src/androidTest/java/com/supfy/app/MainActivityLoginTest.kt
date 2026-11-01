package com.supfy.app

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Compose screen and menu, not the old FrameLayout popup substitute. No credentials entered. */
@RunWith(AndroidJUnit4::class)
class MainActivityLoginTest {
    private var lastRootPackage: String?=null
    @Test(timeout=120000) fun productionLoginPaintsAnInteractiveLocalFormInTheComposeWindow() {productionFixture(false)}
    @Test(timeout=180000) fun aHitTestMismatchCannotBlackOutTheExistingLoginForm() {productionFixture(true)}

    @Test(timeout=180000) fun normalRenderingRecoveryDoesNotReloadOrRemovePageLayers() {productionFixture(true,true)}

    @Test(timeout=180000) fun publicLayoutReplayAcceptsRealWindowTouch() {productionFixture(false,false,true)}

    private fun productionFixture(uncertainHit:Boolean,recover:Boolean=false,replay:Boolean=false) {
        val replayHtml=if(replay) {
            val path=InstrumentationRegistry.getArguments().getString("supfyReplayPath")
            org.junit.Assume.assumeTrue("Public HTML replay is a separate local diagnostic fixture",path=="/data/local/tmp/supfy-login-replay.html")
            val descriptor=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cat /data/local/tmp/supfy-login-replay.html")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {it.readText()}.also {require(it.startsWith("<!DOCTYPE")||it.startsWith("<!doctype"))}
        } else null
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            val selected=AtomicReference<WebView?>()
            repeat(100) {
                if(selected.get()==null)scenario.onActivity {selected.set(webViews(it.window.decorView).singleOrNull())}
                if(selected.get()==null)Thread.sleep(100)
            }
            val web=requireNotNull(selected.get())
            var initialLayoutHeight=0
            scenario.onActivity {
                initialLayoutHeight=web.layoutParams.height
                assertEquals("The production factory must prevent zero CSS viewport height",ViewGroup.LayoutParams.MATCH_PARENT,web.layoutParams.height)
                assertEquals(ViewGroup.LayoutParams.MATCH_PARENT,web.layoutParams.width)
            }
            val committed=java.util.concurrent.CountDownLatch(1)
            scenario.onActivity {
                val delegate=web.webViewClient
                web.webViewClient=object:android.webkit.WebViewClient() {
                    override fun shouldInterceptRequest(v:WebView,r:android.webkit.WebResourceRequest):android.webkit.WebResourceResponse? {
                        delegate.shouldInterceptRequest(v,r)?.let {return it}
                        if(r.isForMainFrame && com.supfy.app.web.AuthenticationPolicy.isAuthenticationUrl(r.url.toString())) {
                            val html=replayHtml?:"""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                              <body style="margin:0;background:rgb(12,210,140);min-height:100vh;color:#111;font:20px sans-serif">
                              <h1>Yerel giriş testi</h1><label>E-posta<input type="email" value="PRIVATE_FIXTURE_VALUE"></label>
                              <button id="next" onclick="document.body.dataset.clicked='yes'" style="display:block;width:180px;height:60px;background:magenta">Devam</button>
                              ${if(uncertainHit) "<div id='page-overlay' aria-hidden='true' style='position:fixed;top:0;left:0;right:0;bottom:0;background:transparent;z-index:99999'></div>" else ""}
                              </body></html>"""
                            return android.webkit.WebResourceResponse("text/html","UTF-8",java.io.ByteArrayInputStream(html.toByteArray()))
                        }
                        return null
                    }
                    override fun shouldOverrideUrlLoading(v:WebView,r:android.webkit.WebResourceRequest)=delegate.shouldOverrideUrlLoading(v,r)
                    override fun onPageStarted(v:WebView,u:String?,b:android.graphics.Bitmap?)=delegate.onPageStarted(v,u,b)
                    override fun onPageFinished(v:WebView,u:String?)=delegate.onPageFinished(v,u)
                    override fun onPageCommitVisible(v:WebView,u:String?) {
                        delegate.onPageCommitVisible(v,u)
                        if(com.supfy.app.web.AuthenticationPolicy.isAuthenticationUrl(u))committed.countDown()
                    }
                    override fun doUpdateVisitedHistory(v:WebView,u:String?,reload:Boolean)=delegate.doUpdateVisitedHistory(v,u,reload)
                    override fun onReceivedError(v:WebView,r:android.webkit.WebResourceRequest,e:android.webkit.WebResourceError)=delegate.onReceivedError(v,r,e)
                    override fun onReceivedHttpError(v:WebView,r:android.webkit.WebResourceRequest,e:android.webkit.WebResourceResponse)=delegate.onReceivedHttpError(v,r,e)
                    override fun onReceivedSslError(v:WebView,h:android.webkit.SslErrorHandler,e:android.net.http.SslError)=delegate.onReceivedSslError(v,h,e)
                    override fun onRenderProcessGone(v:WebView,d:android.webkit.RenderProcessGoneDetail)=delegate.onRenderProcessGone(v,d)
                }
            }
            assertTrue(click("Diğer seçenekler"));assertTrue(click("Spotify giriş ekranı"))
            assertTrue("Production authentication must commit",committed.await(45,java.util.concurrent.TimeUnit.SECONDS))
            if(replay) {
                val prepared=java.util.concurrent.CountDownLatch(1)
                scenario.onActivity {web.evaluateJavascript("var button=document.querySelector('button');button.id='next';button.addEventListener('click',function(e){e.preventDefault();document.body.dataset.clicked='yes'});true") {prepared.countDown()}}
                assertTrue(prepared.await(10,java.util.concurrent.TimeUnit.SECONDS))
            }
            if(recover) {
                val marked=java.util.concurrent.CountDownLatch(1)
                scenario.onActivity {web.evaluateJavascript("window.__supfyRecoverySentinel=17;true") {marked.countDown()}}
                assertTrue(marked.await(10,java.util.concurrent.TimeUnit.SECONDS))
                assertTrue("An unproven interaction must offer a document-preserving recovery",click("Normal çizimi dene",6))
                scenario.onActivity {assertEquals(View.LAYER_TYPE_NONE,web.layerType)}
            }
            val jsDone=java.util.concurrent.CountDownLatch(1)
            val clicked=AtomicReference<String>()
            val interaction=if(uncertainHit)"!!document.getElementById('page-overlay') && document.body.dataset.clicked===undefined"+(if(recover)" && window.__supfyRecoverySentinel===17" else "")
                else "document.body.dataset.clicked"
            if(replay) {
                // First web frame can trigger Compose footer/layout updates. Wait for that
                // native layout and its following web frame before sampling touch coordinates.
                repeat(2) { frameIndex->
                    val frame=java.util.concurrent.CountDownLatch(1)
                    scenario.onActivity {web.postVisualStateCallback(101+frameIndex.toLong(),object:WebView.VisualStateCallback(){override fun onComplete(id:Long){frame.countDown()}})}
                    assertTrue(frame.await(15,java.util.concurrent.TimeUnit.SECONDS))
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                }
            }
            val touchEvidence=org.json.JSONObject().put("initialLayoutHeight",initialLayoutHeight)
            if(!uncertainHit) {
                val coordinates=AtomicReference<String>()
                val boundsReady=java.util.concurrent.CountDownLatch(1)
                scenario.onActivity {web.evaluateJavascript("(function(){var r=document.getElementById('next').getBoundingClientRect();return [r.left+r.width/2,r.top+r.height/2,innerWidth,innerHeight,r.left,r.top,r.width,r.height]})()") {v->coordinates.set(v);boundsReady.countDown()}}
                assertTrue(boundsReady.await(10,java.util.concurrent.TimeUnit.SECONDS))
                val point=org.json.JSONArray(coordinates.get())
                var x=0f;var y=0f
                scenario.onActivity {
                    val location=IntArray(2);web.getLocationOnScreen(location)
                    val scale=web.width.toFloat()/point.getDouble(2).toFloat()
                    x=location[0]+point.getDouble(0).toFloat()*scale
                    y=location[1]+point.getDouble(1).toFloat()*scale
                }
                touchEvidence.put("cssPoint",point).put("nativeX",x).put("nativeY",y)
                scenario.onActivity {touchEvidence.put("width",web.width).put("height",web.height).put("layer",web.layerType).put("layoutWidth",web.layoutParams?.width).put("layoutHeight",web.layoutParams?.height).put("contentHeight",web.contentHeight)}
                val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
                val now=SystemClock.uptimeMillis()
                val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0)
                val up=MotionEvent.obtain(now,now+70,MotionEvent.ACTION_UP,x,y,0)
                down.source=android.view.InputDevice.SOURCE_TOUCHSCREEN;up.source=down.source
                try {assertTrue(automation.injectInputEvent(down,true));assertTrue(automation.injectInputEvent(up,true))}
                finally {down.recycle();up.recycle()}
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                Thread.sleep(500)
            }
            scenario.onActivity {web.evaluateJavascript(interaction) {value->clicked.set(value);jsDone.countDown()}}
            assertTrue(jsDone.await(10,java.util.concurrent.TimeUnit.SECONDS))
            if(replay) {
                val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
                val image=automation.takeScreenshot()
                image?.let {bmp->java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"login-replay-debug.png").outputStream().use {bmp.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bmp.recycle()}
                val probeDone=java.util.concurrent.CountDownLatch(1)
                scenario.onActivity {
                    val source=it.assets.open("login_probe.js").bufferedReader().use {reader->reader.readText()}
                    web.evaluateJavascript(source) {raw->touchEvidence.put("probe",com.supfy.app.web.LoginProbeSnapshot.decode(raw).metrics.let {m->org.json.JSONObject(m)});probeDone.countDown()}
                }
                assertTrue(probeDone.await(10,java.util.concurrent.TimeUnit.SECONDS))
                val afterDone=java.util.concurrent.CountDownLatch(1)
                scenario.onActivity {web.evaluateJavascript("(function(){var el=document.getElementById('next'),r=el.getBoundingClientRect(),hit=document.elementFromPoint(r.left+r.width/2,r.top+r.height/2);return [r.left+r.width/2,r.top+r.height/2,innerWidth,innerHeight,r.left,r.top,r.width,r.height,hit===el||el.contains(hit)]})()") {raw->touchEvidence.put("afterPoint",org.json.JSONArray(raw));afterDone.countDown()}}
                assertTrue(afterDone.await(10,java.util.concurrent.TimeUnit.SECONDS))
                touchEvidence.put("clicked",clicked.get())
                java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"login-replay-debug.json").writeText(touchEvidence.toString())
            }
            assertEquals(if(uncertainHit)"true" else "\"yes\"",clicked.get())
            if(uncertainHit) {
                // Remain past the real 30-second initial guard deadline. The page must stay painted.
                val deadline=SystemClock.uptimeMillis()+34_000
                while(SystemClock.uptimeMillis()<deadline)Thread.sleep(250)
            }
            val visual=java.util.concurrent.CountDownLatch(1)
            scenario.onActivity {web.postVisualStateCallback(1,object:WebView.VisualStateCallback(){override fun onComplete(id:Long){visual.countDown()}})}
            assertTrue(visual.await(15,java.util.concurrent.TimeUnit.SECONDS))
            // PixelCopy is test-only, sampling our hardcoded local fixture, never a user login.
            val bitmap=android.graphics.Bitmap.createBitmap(128,256,android.graphics.Bitmap.Config.ARGB_8888)
            val copied=java.util.concurrent.CountDownLatch(1)
            val status=java.util.concurrent.atomic.AtomicInteger(-1)
            scenario.onActivity {activity->
                val location=IntArray(2);web.getLocationInWindow(location)
                android.view.PixelCopy.request(activity.window,Rect(location[0],location[1],location[0]+web.width,location[1]+web.height),bitmap,
                    {code->status.set(code);copied.countDown()},android.os.Handler(android.os.Looper.getMainLooper()))
            }
            assertTrue(copied.await(10,java.util.concurrent.TimeUnit.SECONDS));assertEquals(android.view.PixelCopy.SUCCESS,status.get())
            var green=0;var magenta=0;var light=0
            for(y in 0 until bitmap.height)for(x in 0 until bitmap.width) {
                val c=bitmap.getPixel(x,y)
                if(android.graphics.Color.red(c)>150 && android.graphics.Color.green(c)>150 && android.graphics.Color.blue(c)>150)light++
                if(replay && android.graphics.Color.green(c)>120 && android.graphics.Color.red(c)<100 && android.graphics.Color.blue(c)<150)green++
                if(!replay && android.graphics.Color.green(c)>170 && android.graphics.Color.red(c)<60 && android.graphics.Color.blue(c) in 90..180)green++
                if(android.graphics.Color.red(c)>200 && android.graphics.Color.blue(c)>200 && android.graphics.Color.green(c)<50)magenta++
            }
            val evidence=java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"login-local-"+(if(replay)"public-replay" else if(recover)"recovery" else if(uncertainHit)"unknown-hit" else "native-touch"))
            evidence.outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            java.io.File(evidence.path+".json").writeText(org.json.JSONObject().put("localFixture",true).put("green",green).put("magenta",magenta).put("light",light).put("nativeTouch",!uncertainHit).toString())
            bitmap.recycle()
            assertTrue("The production window must paint the local form, not just expose DOM geometry",green>if(replay)100 else 2000)
            assertTrue("The Continue button and text must actually paint",if(replay)light>100 else magenta>100)
            assertTrue("Diagnostics must be reachable in the actual production screen",click("Tanıyı kopyala"))
            var report=""
            scenario.onActivity {activity->
                report=activity.getSystemService(android.content.ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
            }
            assertTrue(report.startsWith("Supfy login diagnostics v1"))
            assertFalse("Clipboard diagnostics must not read form values",report.contains("PRIVATE_FIXTURE_VALUE"))
            assertFalse(report.contains("https://"))
        } finally {scenario.close()}
    }
    @Test(timeout=120000) fun loginMenuKeepsTheExistingPlayerWebViewAndWindow() {
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            val original=AtomicReference<WebView?>()
            repeat(50) {
                if(original.get()==null) {
                    scenario.onActivity {activity->original.set(webViews(activity.window.decorView).singleOrNull())}
                    if(original.get()==null)Thread.sleep(100)
                }
            }
            val player=requireNotNull(original.get()) {"The production player must attach before testing login"}
            scenario.onActivity {assertFalse(player.settings.supportMultipleWindows())}
            val menuOpened=click("Diğer seçenekler")
            assertTrue("Production menu must open; window packages: $lastRootPackage",menuOpened)
            val loginSelected=click("Spotify giriş ekranı")
            assertTrue("Production login action must be available; window packages: $lastRootPackage",loginSelected)
            var reached=false
            repeat(50) {
                if(!reached) {
                    scenario.onActivity {activity->
                        val views=webViews(activity.window.decorView)
                        assertEquals(1,views.size)
                        assertSame("Login must not replace the working native view",player,views.single())
                        reached=Uri.parse(player.url.orEmpty()).host=="accounts.spotify.com"
                    }
                    if(!reached)Thread.sleep(100)
                }
            }
            assertTrue("The existing WebView must navigate to Spotify authentication",reached)
        } finally {scenario.close()}
    }

    private fun webViews(view: View): List<WebView> = when(view) {
        is WebView->listOf(view)
        is ViewGroup->(0 until view.childCount).flatMap {webViews(view.getChildAt(it))}
        else->emptyList()
    }

    private fun click(label: String,attempts:Int=80): Boolean {
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        // Compose dropdowns have their own window. The active root can lag behind a popup
        // on older Android/slow software emulators; inspect all app windows as well.
        automation.serviceInfo=automation.serviceInfo.apply {
            flags=flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        repeat(attempts) {
            val roots=(listOfNotNull(automation.rootInActiveWindow)+automation.windows.mapNotNull {it.root})
            lastRootPackage=roots.map {it.packageName?.toString()}.distinct().joinToString()
            for(root in roots) {
              if(root.packageName?.toString()=="com.supfy.app") {
                val target=findLabel(root,label)
                if(target!=null) {
                    var actionTarget:AccessibilityNodeInfo?=target
                    while(actionTarget!=null && !actionTarget.isClickable)actionTarget=actionTarget.parent
                    if(actionTarget?.isEnabled==true && actionTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true
                    val bounds=Rect();target.getBoundsInScreen(bounds)
                    if(!bounds.isEmpty) {
                        val downTime=SystemClock.uptimeMillis()
                        val down=MotionEvent.obtain(downTime,downTime,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY(),0)
                        val up=MotionEvent.obtain(downTime,downTime+50,MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY(),0)
                        down.source=android.view.InputDevice.SOURCE_TOUCHSCREEN
                        up.source=android.view.InputDevice.SOURCE_TOUCHSCREEN
                        try {return automation.injectInputEvent(down,true) && automation.injectInputEvent(up,true)}
                        finally {down.recycle();up.recycle()}
                    }
                }
              }
            }
            Thread.sleep(250)
        }
        return false
    }

    private fun findLabel(node: AccessibilityNodeInfo,label: String): AccessibilityNodeInfo? {
        if(node.isVisibleToUser && (node.text?.toString()==label || node.contentDescription?.toString()==label))return node
        for(index in 0 until node.childCount) {
            node.getChild(index)?.let {findLabel(it,label)?.let {match->return match}}
        }
        return null
    }
}
