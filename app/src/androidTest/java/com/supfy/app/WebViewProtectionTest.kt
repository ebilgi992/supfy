package com.supfy.app

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebResourceRequest
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
    @Test(timeout=90000) fun mobileAuthenticationFixtureRendersAndAcceptsInteraction() {
        var popup: WebView?=null
        val auth=ui {SupfyChromeClient(state,{popup=it})}
        try {
            val target=ui {
                auth.openAuthentication(web,PlayerNavigation.LOGIN_URL)
                requireNotNull(popup).also {
                    it.stopLoading()
                    activity.container.addView(it,android.widget.FrameLayout.LayoutParams(-1,-1))
                    it.loadDataWithBaseURL("https://accounts.spotify.com/supfy-local-fixture", """
                        <html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                        <body data-fixture="ready"><h1>Sentetik giriş</h1><label>E-posta<input type="email"></label>
                        <button id="next" onclick="document.body.dataset.step='next'">Devam</button></body></html>
                    """.trimIndent(),"text/html","UTF-8","https://accounts.spotify.com/supfy-local-fixture")
                }
            }
            var ready=false
            for(attempt in 0 until 30) {
                if(evaluate("document.body && document.body.dataset.fixture",target)=="\"ready\"") {ready=true;break}
                Thread.sleep(100)
            }
            assertTrue("Local authentication document must render",ready)
            assertEquals("\"next\"",evaluate("document.getElementById('next').click();document.body.dataset.step",target))
            repeat(30) {if(ui{auth.authenticationState.loading})Thread.sleep(100)}
            assertFalse(ui{auth.authenticationState.loading})
            assertNull(ui{auth.authenticationState.error})
        } finally {ui {auth.close()}}
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
    @Test fun aClosedPopupCannotOverwriteItsReplacementThroughLateCallbacks() {
        var popup: WebView?=null
        val auth=ui {SupfyChromeClient(state,{popup=it})}
        try {
            ui {
                auth.openAuthentication(web,PlayerNavigation.LOGIN_URL)
                val old=requireNotNull(popup)
                val oldClient=old.webViewClient
                auth.openAuthentication(web,PlayerNavigation.LOGIN_URL)
                val current=requireNotNull(popup)
                current.webViewClient.onPageStarted(current,PlayerNavigation.LOGIN_URL,null)
                oldClient.onPageStarted(old,PlayerNavigation.LOGIN_URL,null)
                oldClient.onReceivedHttpError(old,request(PlayerNavigation.LOGIN_URL,"GET"),
                    android.webkit.WebResourceResponse("text/plain","UTF-8",503,"Unavailable",emptyMap(),
                        java.io.ByteArrayInputStream(ByteArray(0))))
                assertSame(current,popup)
                assertNull(auth.authenticationState.error)
            }
        } finally {ui {auth.close()}}
    }
    @Test fun separateAuthenticationWindowUsesMobileSettingsAndShowsMainDocumentHttpErrors() {
        var popup: WebView?=null
        val auth=ui {SupfyChromeClient(state,{popup=it})}
        try {
            ui {
                WebViewFactory.applyDesktopMode(web,true)
                assertTrue(auth.openAuthentication(web,PlayerNavigation.LOGIN_URL))
                val target=requireNotNull(popup)
                assertNotSame(web,target)
                assertTrue(target.settings.javaScriptEnabled)
                assertTrue(target.settings.domStorageEnabled)
                assertTrue(target.settings.userAgentString.contains("Android"))
                assertFalse(target.settings.useWideViewPort)
                assertFalse(target.settings.allowFileAccess)
                target.webViewClient.onPageStarted(target,PlayerNavigation.LOGIN_URL,null)
                target.webViewClient.onReceivedHttpError(target,request(PlayerNavigation.LOGIN_URL,"GET"),
                    android.webkit.WebResourceResponse("text/plain","UTF-8",503,"Unavailable",emptyMap(),
                        java.io.ByteArrayInputStream(ByteArray(0))))
                assertEquals(PageErrorKind.HTTP,auth.authenticationState.error?.kind)
                assertEquals(503,auth.authenticationState.error?.httpStatus)
                assertFalse(auth.authenticationState.loading)
            }
        } finally {ui{auth.close()}}
    }
    @Test fun playerLoginIntermediatePageDoesNotPrematurelyCloseAuthentication() {
        var popup: WebView?=null
        val auth=ui {SupfyChromeClient(state,{popup=it})}
        try {
            ui {
                assertTrue(auth.openAuthentication(web,"https://open.spotify.com/login"))
                val target=requireNotNull(popup)
                target.webViewClient.onPageFinished(target,"https://open.spotify.com/login")
                assertSame(target,popup)
            }
        } finally {ui{auth.close()}}
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
        web = WebViewFactory.create(activity, false,
            SupfyWebViewClient(state, {}, { rendererLost=true;controller.rendererGone() }, controller), chrome)
        controller.attach(web)
        activity.container.addView(web, android.widget.FrameLayout.LayoutParams(-1,-1))
        web.onResume()
        }
    }
    @After fun tearDown() {
      ui {
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
    @Test(timeout = 120000) fun popupKeepsJavaScriptAndDomStorageForAuthentication() {
        fixture()
        val target = ui {
            val transport = web.WebViewTransport()
            val message = Message.obtain(Handler(Looper.getMainLooper())).apply { obj = transport }
            check(chrome.onCreateWindow(web, false, true, message))
            requireNotNull(transport.webView)
        }
        assertTrue(ui { target.settings.javaScriptEnabled })
        assertTrue(ui { target.settings.domStorageEnabled })
        assertFalse(ui { target.settings.allowFileAccess })
        assertFalse(ui { target.settings.allowContentAccess })
        val handled = ui { target.webViewClient.onRenderProcessGone(target, object : RenderProcessGoneDetail() {
            override fun didCrash() = true
            override fun rendererPriorityAtExit() = WebView.RENDERER_PRIORITY_IMPORTANT
        }) }
        assertTrue(handled)
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
    @Test(timeout=120000) fun popupPostIsAllowedOnlyOnTrustedOrigins() {
        fixture()
        val target=ui {
            val transport=web.WebViewTransport()
            val message=Message.obtain(Handler(Looper.getMainLooper())).apply {obj=transport}
            check(chrome.onCreateWindow(web,false,true,message))
            requireNotNull(transport.webView)
        }
        val client=ui {target.webViewClient}
        assertNull(client.shouldInterceptRequest(target,request("https://accounts.spotify.com/login","POST")))
        val response=client.shouldInterceptRequest(target,request("https://example.org/untrusted","POST"))
        assertNotNull("Foreign POST must be refused before network access",response)
        assertEquals(403,response!!.statusCode)
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
    @Test(timeout=120000) fun actualPopupFormCannotNavigateToAnUntrustedOrigin() {
        fixture()
        val target=ui {
            val transport=web.WebViewTransport()
            val message=Message.obtain(Handler(Looper.getMainLooper())).apply {obj=transport}
            check(chrome.onCreateWindow(web,false,true,message))
            requireNotNull(transport.webView).also {
                activity.container.addView(it,android.widget.FrameLayout.LayoutParams(-1,-1))
                it.loadDataWithBaseURL("https://accounts.spotify.com/", """
                    <html><body><form method="post" action="https://example.org/supfy-fixture">
                    <input name="fixture" value="synthetic"></form></body></html>
                """.trimIndent(),"text/html","UTF-8","https://accounts.spotify.com/")
            }
        }
        repeat(30) {
            if(evaluate("document.forms.length",target)=="1")return@repeat
            Thread.sleep(100)
        }
        ui {target.evaluateJavascript("document.forms[0].submit();",null)}
        repeat(60) {if(state.stats.blockedNavigations==0)Thread.sleep(100)}
        assertTrue("External popup POST must be blocked",state.stats.blockedNavigations>0)
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
