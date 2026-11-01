package com.supfy.app

import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.webkit.WebResourceResponse
import android.webkit.WebResourceError
import android.webkit.SslErrorHandler
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import com.supfy.app.testing.WebViewTestActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.supfy.app.data.AppSettings
import com.supfy.app.protection.*
import com.supfy.app.web.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android WebView with local synthetic HTML. Not a Spotify account/ad/DRM test. */
@RunWith(AndroidJUnit4::class)
class WebViewProtectionTest {
    @Test fun loginRenderingCompatibilityPreservesSecurityAndRestoresPlayerRendering() {
        ui {
            val agent=web.settings.userAgentString
            web.webViewClient.onPageStarted(web,PlayerNavigation.LOGIN_URL,null)
            assertEquals("Login must use its isolated rendering compatibility path",android.view.View.LAYER_TYPE_SOFTWARE,web.layerType)
            assertTrue(web.settings.safeBrowsingEnabled)
            assertFalse(web.settings.allowFileAccess)
            assertFalse(web.settings.allowContentAccess)
            assertFalse(android.webkit.CookieManager.getInstance().acceptThirdPartyCookies(web))
            assertEquals(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW,web.settings.mixedContentMode)
            assertEquals(agent,web.settings.userAgentString)
            web.webViewClient.onPageStarted(web,UrlPolicy.HOME_URL,null)
            assertEquals("Spotify media must regain normal rendering",android.view.View.LAYER_TYPE_NONE,web.layerType)
            assertEquals(agent,web.settings.userAgentString)
        }
    }
    @Test(timeout=90000) fun targetBlankLoginLinkNavigatesInTheExistingView() {
        loginNavigationFixture("<a id='login' target='_blank' href='https://accounts.spotify.com/supfy-local-login'>Oturum aç</a>")
    }
    @Test(timeout=90000) fun userGestureWindowOpenLoginNavigatesInTheExistingView() {
        loginNavigationFixture("<button id='login' onclick=\"window.open('https://accounts.spotify.com/supfy-local-login','_blank')\">Oturum aç</button>")
    }

    /** Both documents are local synthetic responses. No Spotify network/account request is made. */
    private fun loginNavigationFixture(control: String) {
        val source="https://open.spotify.com/supfy-local-navigation"
        val target="https://accounts.spotify.com/supfy-local-login"
        val first=CountDownLatch(1)
        val login=CountDownLatch(1)
        ui {
            val delegate=web.webViewClient
            web.webViewClient=object: WebViewClient() {
                override fun shouldInterceptRequest(v:WebView,r:WebResourceRequest):WebResourceResponse? {
                    delegate.shouldInterceptRequest(v,r)?.let {return it}
                    val html=when(r.url.toString()) {
                        source->"<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head><body>$control</body></html>"
                        target->"<html><body><label>E-posta<input type='email'></label><button>Devam</button></body></html>"
                        else->return null
                    }
                    return WebResourceResponse("text/html","UTF-8",java.io.ByteArrayInputStream(html.toByteArray(Charsets.UTF_8)))
                }
                override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest)=delegate.shouldOverrideUrlLoading(v,r)
                override fun onPageStarted(v:WebView,url:String?,icon:android.graphics.Bitmap?)=delegate.onPageStarted(v,url,icon)
                override fun onPageFinished(v:WebView,url:String?)=delegate.onPageFinished(v,url)
                override fun onPageCommitVisible(v:WebView,url:String?) {
                    delegate.onPageCommitVisible(v,url)
                    when(url) {source->first.countDown();target->login.countDown()}
                }
                override fun onReceivedError(v:WebView,r:WebResourceRequest,e:WebResourceError)=delegate.onReceivedError(v,r,e)
                override fun onReceivedHttpError(v:WebView,r:WebResourceRequest,e:WebResourceResponse)=delegate.onReceivedHttpError(v,r,e)
                override fun onReceivedSslError(v:WebView,h:SslErrorHandler,e:android.net.http.SslError)=delegate.onReceivedSslError(v,h,e)
                override fun onRenderProcessGone(v:WebView,d:RenderProcessGoneDetail)=delegate.onRenderProcessGone(v,d)
            }
            web.loadUrl(source)
        }
        awaitCommit(first)
        val point=org.json.JSONArray(evaluate("(function(){var r=document.getElementById('login').getBoundingClientRect();return [r.left+r.width/2,r.top+r.height/2,window.innerWidth]})()"))
        // Native touch supplies a real user gesture; evaluateJavascript(...click()) does not.
        ui {
            val scale=web.width.toFloat()/point.getDouble(2).toFloat()
            val x=point.getDouble(0).toFloat()*scale
            val y=point.getDouble(1).toFloat()*scale
            val now=android.os.SystemClock.uptimeMillis()
            val down=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,x,y,0)
            val up=android.view.MotionEvent.obtain(now,now+50,android.view.MotionEvent.ACTION_UP,x,y,0)
            try {web.dispatchTouchEvent(down);web.dispatchTouchEvent(up)}
            finally {down.recycle();up.recycle()}
        }
        awaitCommit(login)
        assertEquals(target,ui{web.url})
        assertSame(web,ui{activity.container.getChildAt(0)})
        assertEquals(1,ui{activity.container.childCount})
        assertEquals("true",evaluate("!!document.querySelector('input[type=email]')"))
    }

    private fun awaitCommit(committed: CountDownLatch) {
        val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(60)
        while(!committed.await(100,TimeUnit.MILLISECONDS)) {
            check(!rendererLost) {"Renderer exited before the local form committed"}
            check(System.nanoTime()<deadline) {"Local form did not commit"}
        }
    }

    @Test(timeout=90000) fun mobileAuthenticationFixtureRendersAndAcceptsInteraction() { authenticationFixture() }

    @Test(timeout=90000) fun authenticationThatTurnsBlackAfterReadinessShowsAnError() {
        authenticationFixture()
        evaluate("document.body.innerHTML='';true")
        awaitEmptyError()
        evaluate("document.body.innerHTML='<label>E-posta<input type=email></label><button>Devam</button>';true")
        val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8)
        while(ui{state.error}!=null && System.nanoTime()<deadline)Thread.sleep(100)
        assertNull("A late form must recover without a reload or a replayed POST",ui{state.error})
    }
    @Test(timeout=90000) fun aBrokenJavascriptProbeIsNotReportedAsAnEmptyDocument() {
        authenticationFixture()
        evaluate("document.querySelectorAll=function(){throw new Error('synthetic diagnostic failure')};true")
        val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(16)
        while(ui{state.error}==null && System.nanoTime()<deadline)Thread.sleep(100)
        assertEquals("A script failure is different from a confirmed empty document",PageErrorKind.GENERIC,ui{state.error?.kind})
    }

    @Test(timeout=90000) fun singlePageLoginNavigationRearmsTheBlankPageGuard() {
        authenticationFixture("https://open.spotify.com/supfy-home")
        evaluate("history.pushState({},'', '/login');document.body.innerHTML='';true")
        val navigationDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
        while(!ui{state.authenticating} && System.nanoTime()<navigationDeadline)Thread.sleep(100)
        assertTrue("A same-document login must re-arm authentication checks",ui{state.authenticating})
        assertEquals("/login",Uri.parse(ui{state.currentUrl}).path)
        // A new authentication navigation gets the normal 30-second initial load budget.
        awaitEmptyError(36)
    }

    @Test(timeout=90000) fun opaquePageOverlaysArePreservedWithoutCallingTheDocumentEmpty() {
        authenticationFixture()
        evaluate("var cover=document.createElement('div');cover.style='position:fixed;top:0;right:0;bottom:0;left:0;background:#121212;z-index:2147483647';document.body.appendChild(cover);true")
        assertEquals("The overlay fixture must cover the viewport even on older WebView", "true",evaluate("cover.getBoundingClientRect().height>=innerHeight && cover.getBoundingClientRect().width>=innerWidth"))
        Thread.sleep(14_000)
        assertNull("Existing form/challenge content must not be covered by a native empty-page error",ui{state.error})
        assertFalse(ui{state.loading})
        assertNotEquals("Occluded controls are not proven interactive",PageStage.READY,ui{state.pageStage})
        assertEquals("true",evaluate("document.body.contains(cover) && getComputedStyle(cover).pointerEvents==='auto'"))
    }

    @Test(timeout=90000) fun visibleControlsWithDifferentHitTargetsAreNotAnEmptyDocument() {
        authenticationFixture()
        // Remove the associated label: a real label is intentionally accepted as a forwarding target.
        evaluate("var input=document.querySelector('input');input.parentElement.replaceWith(input);document.querySelectorAll('input,button').forEach(function(e){e.style.pointerEvents='none'});true")
        Thread.sleep(14_000)
        assertNull("Hit-test failure cannot mean the form disappeared",ui{state.error})
        assertFalse("The native loading card must leave existing content accessible",ui{state.loading})
        assertNotEquals(PageStage.READY,ui{state.pageStage})
    }

    @Test(timeout=90000) fun aRenderingRetrySurvivesAuthStepsAndCannotClearTerminalErrors() {
        authenticationFixture()
        evaluate("window.__supfyLocalSentinel=17;var cover=document.createElement('div');cover.style='position:fixed;top:0;left:0;right:0;bottom:0;background:transparent;z-index:9999';document.body.appendChild(cover);true")
        val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
        while(ui{state.pageStage}!=PageStage.INTERACTION && System.nanoTime()<deadline)Thread.sleep(100)
        assertTrue(ui{client.retryLoginRendering(web)})
        assertEquals("true",evaluate("window.__supfyLocalSentinel===17 && document.body.contains(cover)"))
        ui {
            assertEquals(android.view.View.LAYER_TYPE_NONE,web.layerType)
            assertTrue(state.loginRenderingRetried)
            assertFalse(client.retryLoginRendering(web))
            client.onPageStarted(web,web.url,null)
            assertEquals("An accounts redirect must retain the selected path",android.view.View.LAYER_TYPE_NONE,web.layerType)
            client.onReceivedHttpError(web,request(web.url!!,"GET"),WebResourceResponse("text/plain","UTF-8",503,"Unavailable",emptyMap(),java.io.ByteArrayInputStream(ByteArray(0))))
            assertFalse(client.retryLoginRendering(web))
            assertEquals(PageErrorKind.HTTP,state.error?.kind)
            assertTrue(web.settings.safeBrowsingEnabled)
            assertFalse(android.webkit.CookieManager.getInstance().acceptThirdPartyCookies(web))
            client.onPageStarted(web,UrlPolicy.HOME_URL,null)
            assertFalse(state.loginRenderingRetried)
            assertFalse(client.retryLoginRendering(web))
            client.onPageStarted(web,PlayerNavigation.LOGIN_URL,null)
            assertEquals(android.view.View.LAYER_TYPE_SOFTWARE,web.layerType)
        }
    }

    private fun awaitEmptyError(seconds:Long=16) {
        val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds)
        while(ui{state.error}==null && System.nanoTime()<deadline)Thread.sleep(100)
        assertEquals("Persistent blank authentication must become a visible error",PageErrorKind.EMPTY,ui{state.error?.kind})
    }

    private fun authenticationFixture(url:String="https://accounts.spotify.com/supfy-local-fixture") {
        val committed=CountDownLatch(1)
        ui {
            val delegate=web.webViewClient
            web.webViewClient=object: WebViewClient() {
                override fun shouldInterceptRequest(v:WebView,r:WebResourceRequest)=delegate.shouldInterceptRequest(v,r)
                override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest)=delegate.shouldOverrideUrlLoading(v,r)
                override fun onPageStarted(v:WebView,url:String?,icon:android.graphics.Bitmap?)=delegate.onPageStarted(v,url,icon)
                override fun onPageFinished(v:WebView,url:String?)=delegate.onPageFinished(v,url)
                override fun doUpdateVisitedHistory(v:WebView,url:String?,reload:Boolean)=delegate.doUpdateVisitedHistory(v,url,reload)
                override fun onPageCommitVisible(v:WebView,url:String?) {
                    delegate.onPageCommitVisible(v,url)
                    committed.countDown()
                }
                override fun onReceivedError(v:WebView,r:WebResourceRequest,e:WebResourceError)=delegate.onReceivedError(v,r,e)
                override fun onReceivedHttpError(v:WebView,r:WebResourceRequest,e:WebResourceResponse)=delegate.onReceivedHttpError(v,r,e)
                override fun onReceivedSslError(v:WebView,h:SslErrorHandler,e:android.net.http.SslError)=delegate.onReceivedSslError(v,h,e)
                override fun onRenderProcessGone(v:WebView,d:RenderProcessGoneDetail)=delegate.onRenderProcessGone(v,d)
            }
            web.loadDataWithBaseURL(url, """
                <html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body data-fixture="ready"><h1>Sentetik giriş</h1><label>E-posta<input type="email"></label>
                <button id="next" onclick="document.body.dataset.step='next'">Devam</button></body></html>
            """.trimIndent(),"text/html","UTF-8",url)
        }
        // Evaluating during navigation can lose the callback when the old JS context goes away.
        // Wait for the real new-document commit instead of treating an early JS timeout as proof.
        awaitCommit(committed)
        var ready=false
        for(attempt in 0 until 30) {
            if(evaluate("document.body && document.body.dataset.fixture")=="\"ready\"") {ready=true;break}
            Thread.sleep(100)
        }
        assertTrue("Local authentication document must render",ready)
        assertEquals("\"next\"",evaluate("document.getElementById('next').click();document.body.dataset.step"))
        repeat(30) {if(ui{state.loading})Thread.sleep(100)}
        assertFalse(ui{state.loading})
        assertNull(ui{state.error})
    }
    @Test fun authenticationStaysInTheExistingWindowAndShowsMainDocumentErrors() {
        ui {
            assertFalse(web.settings.supportMultipleWindows())
            assertFalse(web.webViewClient.shouldOverrideUrlLoading(web,request(PlayerNavigation.LOGIN_URL,"GET")))
            web.webViewClient.onPageStarted(web,PlayerNavigation.LOGIN_URL,null)
            web.webViewClient.onReceivedHttpError(web,request(PlayerNavigation.LOGIN_URL,"GET"),
                android.webkit.WebResourceResponse("text/plain","UTF-8",503,"Unavailable",emptyMap(),
                    java.io.ByteArrayInputStream(ByteArray(0))))
            assertEquals(PageErrorKind.HTTP,state.error?.kind)
            assertEquals(503,state.error?.httpStatus)
            assertFalse(state.loading)
        }
    }
    @Test fun aClosedGuardCannotCoverANewLoginWindowWithAnOldError() {
        ui {
            val shared=WebUiState()
            val old=WebPageGuard(shared)
            val current=WebPageGuard(shared)
            try {
                old.started(web,PlayerNavigation.LOGIN_URL)
                old.close()
                current.started(web,PlayerNavigation.LOGIN_URL)
                old.failed(PlayerNavigation.LOGIN_URL,PageErrorKind.HTTP,503)
                assertNull(shared.error)
                assertTrue(shared.loading)
            } finally {old.close();current.close()}
        }
    }
    @Test fun mobileDefaultDoesNotOverwriteAnExplicitDesktopPreference() {
        val testContext=instrumentation.context
        val fixture=testContext.getSharedPreferences("mobile-fixture-${System.nanoTime()}",android.content.Context.MODE_PRIVATE)
        val isolated=object:android.content.ContextWrapper(testContext) {
            override fun getSharedPreferences(name:String,mode:Int)=fixture
        }
        val defaults=AppSettings(isolated)
        assertFalse(defaults.desktopMode.value)
        defaults.setDesktopMode(true)
        assertTrue(AppSettings(isolated).desktopMode.value)
    }
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var web: WebView
    private lateinit var controller: ProtectionWebController
    private lateinit var chrome: SupfyChromeClient
    private lateinit var client: SupfyWebViewClient
    private lateinit var engine: ProtectionEngine
    private lateinit var scenario: ActivityScenario<WebViewTestActivity>
    private lateinit var activity: WebViewTestActivity
    private lateinit var state: WebUiState
    @Volatile private var rendererLost=false

    private fun <T> ui(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }
    @Before fun setUp() {
        scenario=ActivityScenario.launch(WebViewTestActivity::class.java)
        scenario.onActivity { activity=it }
        ui {
        engine = ProtectionEngine()
        controller = ProtectionWebController(instrumentation.targetContext, engine)
        state = WebUiState()
        chrome = SupfyChromeClient(state)
        client=SupfyWebViewClient(state, {}, { rendererLost=true;controller.rendererGone() }, controller)
        web = WebViewFactory.create(activity, false, client, chrome)
        controller.attach(web)
        activity.container.addView(web, android.widget.FrameLayout.LayoutParams(-1,-1))
        web.onResume()
        }
    }
    @After fun tearDown() {
      ui {
        if (::client.isInitialized) client.close()
        if (::controller.isInitialized) controller.close()
        if (::chrome.isInitialized) chrome.close()
        if (::web.isInitialized) { activity.container.removeView(web); if(!rendererLost)web.stopLoading(); web.destroy() }
      }
      if (::scenario.isInitialized) scenario.close()
    }
    private fun evaluate(js: String, target: WebView = web): String {
        val done = CountDownLatch(1)
        var result = ""
        ui { target.evaluateJavascript(js) { result = it; done.countDown() } }
        val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30)
        while(!done.await(100,TimeUnit.MILLISECONDS)) {
            check(!rendererLost) { "Android renderer exited before synthetic JavaScript completed" }
            check(System.nanoTime()<deadline) { "Synthetic WebView JS timed out" }
        }
        return result
    }
    private fun fixture(ad: Boolean = false, url: String = "https://open.spotify.com/") {
        ui {
            web.loadDataWithBaseURL(url, """
                <html><body data-fixture="ready"><div data-testid="now-playing-widget">
                ${if (ad) "<i data-testid='track-info-advertiser'></i>" else "Song"}
                </div><audio></audio><div data-testid="ad-banner">Ad</div></body></html>
            """.trimIndent(), "text/html", "UTF-8", url)
        }
        repeat(30) {
            if (evaluate("document.body && document.body.dataset.fixture") == "\"ready\"") return
            Thread.sleep(100)
        }
        error("Synthetic fixture did not load")
    }
    @Test(timeout = 120000) fun realWebViewMutesFixtureAndRestoresOnDisable() {
        fixture(ad = true)
        ui { controller.configure(ProtectionOptions(enabled = true)) }
        repeat(60) { if (engine.state.value.status != EngineStatus.ACTIVE) Thread.sleep(100) }
        assertEquals(EngineStatus.ACTIVE, engine.state.value.status)
        assertEquals(1L, engine.state.value.detectedAds)
        assertEquals(1L, engine.state.value.hiddenBanners)
        assertEquals("true", evaluate("document.querySelector('audio').muted"))
        ui { controller.configure(ProtectionOptions(enabled = false)) }
        assertEquals("false", evaluate("document.querySelector('audio').muted"))
        assertEquals(EngineStatus.DISABLED, engine.state.value.status)
        val metrics=JSONObject().put("synthetic",true).put("detected",engine.state.value.detectedAds)
            .put("hiddenBanners",engine.state.value.hiddenBanners).put("domMutes",engine.state.value.mutedMedia)
            .put("nativeSupported",engine.state.value.nativeAudioSupported).put("nativeMutes",engine.state.value.nativeMutes)
            .put("nativeRestores",engine.state.value.nativeRestores).put("disabled",true)
        java.io.File(instrumentation.targetContext.cacheDir,"supfy-v11-media-evidence.json").writeText(metrics.toString())
    }
    @Test(timeout = 120000) fun authenticationDocumentReceivesNoProtection() {
        fixture(ad = true, url = "https://accounts.spotify.com/login")
        ui { controller.configure(ProtectionOptions(enabled = true)) }
        assertEquals("\"undefined\"", evaluate("typeof window.__supfyProtection"))
        assertEquals("false", evaluate("document.querySelector('audio').muted"))
        assertEquals(EngineStatus.WAITING_FOR_PLAYER, engine.state.value.status)
    }
    @Test fun preferencesSurviveSettingsRecreation() {
        val context = instrumentation.targetContext
        val settings = AppSettings(context)
        val before = settings.protectionOptions
        try {
            settings.setProtectionOptions(ProtectionOptions(enabled = true, muteAds = false, hidePromos = false))
            val restored = AppSettings(context).protectionOptions
            assertTrue(restored.enabled); assertFalse(restored.muteAds); assertFalse(restored.hidePromos)
        } finally { settings.setProtectionOptions(before) }
    }
    private fun request(url: String, methodName: String, main: Boolean=true) = object : WebResourceRequest {
        override fun getUrl()=Uri.parse(url)
        override fun isForMainFrame()=main
        override fun isRedirect()=false
        override fun hasGesture()=false
        override fun getMethod()=methodName
        override fun getRequestHeaders(): Map<String,String> = emptyMap()
    }
    @Test(timeout=120000) fun networkFilterReceivesAnActualWebViewFetch() {
        fixture()
        ui {controller.configure(ProtectionOptions(enabled=true))}
        repeat(60) {if(engine.state.value.status!=EngineStatus.ACTIVE)Thread.sleep(100)}
        assertEquals(EngineStatus.ACTIVE,engine.state.value.status)
        evaluate("fetch('https://doubleclick.net/supfy-synthetic-fixture').catch(function(){});true")
        repeat(60) {if(engine.state.value.blockedRequests==0L)Thread.sleep(100)}
        assertEquals(1L,engine.state.value.blockedRequests)
        java.io.File(instrumentation.targetContext.cacheDir,"supfy-v11-network-evidence.json")
            .writeText(JSONObject().put("synthetic",true).put("blockedRequests",engine.state.value.blockedRequests).toString())
    }
    @Test(timeout=120000) fun plainWebViewRendersWithoutAnyAdEngineClient() {
        ui {
            controller.detach()
            web.webChromeClient=android.webkit.WebChromeClient()
            web.webViewClient=object:android.webkit.WebViewClient(){
                override fun onRenderProcessGone(v:WebView,d:RenderProcessGoneDetail):Boolean {
                    rendererLost=true;return true
                }
            }
        }
        fixture()
        assertEquals("2",evaluate("1+1"))
    }
    @Test fun postRequestGateEnforcesOriginBeforeNetworkAccess() {
        assertNull(NavigationRequestGate.intercept(request("https://accounts.spotify.com/login","POST")))
        assertEquals(403,NavigationRequestGate.intercept(request("https://example.org/untrusted","POST"))!!.statusCode)
        assertEquals(403,NavigationRequestGate.intercept(request("https://user@accounts.spotify.com/login","POST"))!!.statusCode)
    }
}
