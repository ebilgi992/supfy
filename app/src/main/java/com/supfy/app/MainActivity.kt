package com.supfy.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.supfy.app.update.AvailableUpdate
import com.supfy.app.update.ReleaseSelector
import com.supfy.app.update.UpdateRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.supfy.app.data.AppSettings
import com.supfy.app.data.ConnectivityObserver
import com.supfy.app.data.ThemeMode
import com.supfy.app.ui.SupfyScreen
import com.supfy.app.ui.ProtectionScreen
import com.supfy.app.ui.SettingsScreen
import com.supfy.app.ui.theme.SupfyTheme
import com.supfy.app.web.SupfyChromeClient
import com.supfy.app.web.SupfyWebViewClient
import com.supfy.app.web.DeviceSupport
import com.supfy.app.web.PageError
import com.supfy.app.web.PageErrorKind
import com.supfy.app.web.UrlPolicy
import com.supfy.app.web.WebUiState
import com.supfy.app.web.WebViewFactory
import com.supfy.app.protection.ProtectionEngine
import com.supfy.app.protection.ProtectionOptions
import com.supfy.app.protection.ProtectionWebController
import com.supfy.app.ui.quantum.*
import com.supfy.app.playback.PlaybackCommand
import com.supfy.app.playback.PlaybackNotificationService
import com.supfy.app.account.AccountStatus
import com.supfy.app.web.PlayerNavigation

class MainActivity : ComponentActivity() {

    private lateinit var appSettings: AppSettings
    private lateinit var connectivity: ConnectivityObserver
    private val uiState = WebUiState()
    private var webView: WebView? by mutableStateOf<WebView?>(null)
    private val updates = UpdateRepository(BuildConfig.UPDATE_REPO)
    private var pendingUpdate: AvailableUpdate? by mutableStateOf<AvailableUpdate?>(null)
    private var updateCheckRunning = false
    private val protectionEngine = ProtectionEngine()
    private lateinit var protection: ProtectionWebController
    private lateinit var playback:com.supfy.app.playback.PlaybackWebController
    private lateinit var account:com.supfy.app.account.AccountController
    private var providerUri:Uri? by mutableStateOf<Uri?>(null)
    private var mainChrome: SupfyChromeClient? = null
    private var mainClient: SupfyWebViewClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        appSettings = AppSettings(applicationContext)
        connectivity = ConnectivityObserver(applicationContext)
        protection = ProtectionWebController(applicationContext, protectionEngine, ::recoverAudio)
        protection.configure(appSettings.protectionOptions)

        playback=com.supfy.app.playback.PlaybackWebController(applicationContext) {message->Toast.makeText(this,message,Toast.LENGTH_SHORT).show()}
        playback.setEnabled(appSettings.backgroundPlayback.value)
        account=com.supfy.app.account.AccountController(applicationContext)
        WebViewFactory.hardenServiceWorkers()
        protection.installServiceWorkerFilter()
        val wv = buildWebView()
        webView = wv
        // Süreç sistem tarafından kapatıldıysa kaldığı sayfaya döner.
        val restored = savedInstanceState?.let { wv.restoreState(it) } != null
        if (!restored) mainClient?.loadUrl(wv,UrlPolicy.HOME_URL)
        else mainClient?.trackRestoredPage(wv)

        val webViewVersion = WebViewCompat.getCurrentWebViewPackage(this)?.versionName
        val support = DeviceSupport(
            safeBrowsing = WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING),
            serviceWorkerHardening = WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) &&
                WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_FILE_ACCESS) &&
                WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS),
        )

        setContent {
            val themeMode by appSettings.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Durum ve gezinme çubuğu simgeleri seçilen temaya uysun.
            DisposableEffect(darkTheme) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (darkTheme) {
                    SystemBarStyle.dark(transparent)
                } else {
                    SystemBarStyle.light(transparent, transparent)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            SupfyTheme(darkTheme = darkTheme) {
                val online by connectivity.online.collectAsStateWithLifecycle()
                val background by appSettings.backgroundPlayback.collectAsStateWithLifecycle()
                val desktop by appSettings.desktopMode.collectAsStateWithLifecycle()
                val engineState by protectionEngine.state.collectAsStateWithLifecycle()
                val accountState by account.state.collectAsStateWithLifecycle()
                val mediaState by playback.state.collectAsStateWithLifecycle()
                val mediaHealth by playback.health.collectAsStateWithLifecycle()
                val serviceHealth by PlaybackNotificationService.health.collectAsStateWithLifecycle()
                LaunchedEffect(background){playback.setEnabled(background)}
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var showProtection by rememberSaveable { mutableStateOf(false) }
                var showNowPlaying by rememberSaveable { mutableStateOf(false) }
                var showDiagnostics by rememberSaveable { mutableStateOf(false) }
                var showWelcome by rememberSaveable { mutableStateOf(appSettings.welcomeRequired) }
                var welcomeDismissed by rememberSaveable { mutableStateOf(false) }
                var destination by rememberSaveable { mutableStateOf(QuantumDestination.HOME) }
                fun dismissWelcome() { showWelcome=false;welcomeDismissed=true;appSettings.acknowledgeWelcome() }
                fun login() { dismissWelcome();openLogin() }
                fun signUp() { dismissWelcome();webView?.let {mainClient?.loadUrl(it,PlayerNavigation.SIGNUP_URL)} }
                fun togglePlayback() { playback.command(if(mediaState.playing)PlaybackCommand.PAUSE else PlaybackCommand.PLAY) }
                LaunchedEffect(accountState.status,uiState.authenticating) {
                    if(accountState.status==AccountStatus.AUTHENTICATED) {showWelcome=false;appSettings.acknowledgeWelcome()}
                    else if(accountState.status==AccountStatus.GUEST && !uiState.authenticating && !welcomeDismissed)showWelcome=true
                }

                // Açılışta (en fazla 12 saatte bir) güncelleme denetimi; bağlantı yoksa beklenir.
                LaunchedEffect(online) {
                    if (online) checkForUpdates(manual = false)
                }

                // Bağlantı geri gelince ağ hatası ekranındaysak otomatik yeniden dene.
                LaunchedEffect(online) {
                    val kind = uiState.error?.kind
                    if (online && (kind == PageErrorKind.NO_CONNECTION || kind == PageErrorKind.TIMEOUT)) {
                        retry()
                    }
                }

                Box(Modifier.fillMaxSize()) {
                  Column(Modifier.fillMaxSize()) {
                   Box(Modifier.weight(1f)) {
                    SupfyScreen(
                        webView = webView,
                        state = uiState,
                        online = online,
                        onRefresh = { webView?.reload() },
                        onHome = { webView?.let {mainClient?.loadUrl(it,UrlPolicy.HOME_URL)} },
                        onLogin = ::login,
                        onRetry = ::retry,
                        onOpenSettings = { showSettings = true },
                        onOpenProtection = { showProtection = true },
                        onCopyLoginDiagnostics = ::copyLoginDiagnostics,
                        onRetryLoginRendering = {webView?.let {mainClient?.retryLoginRendering(it)}},
                    )
                   }
                   if(!uiState.authenticating && !showWelcome) {
                    QuantumMiniPlayer(mediaState,onExpand={showNowPlaying=true},onPlayPause=::togglePlayback)
                    QuantumBottomBar(destination) {selected->
                        if(selected==QuantumDestination.SETTINGS)showSettings=true
                        else {
                            destination=selected
                            val url=when(selected) {
                                QuantumDestination.SEARCH->"https://open.spotify.com/search"
                                QuantumDestination.LIBRARY->"https://open.spotify.com/collection"
                                else->UrlPolicy.HOME_URL
                            }
                            webView?.let {mainClient?.loadUrl(it,url)}
                        }
                    }
                   }
                  }

                    AnimatedVisibility(
                        visible = showSettings,
                        enter = slideInHorizontally { it } + fadeIn(),
                        exit = slideOutHorizontally { it } + fadeOut(),
                    ) {
                        SettingsScreen(
                            backgroundPlayback = background,
                            desktopMode = desktop,
                            themeMode = themeMode,
                            webViewVersion = webViewVersion,
                            onBackgroundPlaybackChange = appSettings::setBackgroundPlayback,
                            onDesktopModeChange = ::setDesktopMode,
                            onThemeModeChange = appSettings::setThemeMode,
                            onClearCache = ::clearCache,
                            onCheckUpdates = { checkForUpdates(manual = true) },
                            onClearData = ::clearSiteData,
                            onClose = { showSettings = false },
                            account = accountState,
                            onAccountLogin = {showSettings=false;login()},
                            onAccountSignUp = {showSettings=false;signUp()},
                            onAccountMenu = {
                                showSettings=false
                                fun fallback(){webView?.let {mainClient?.loadUrl(it,com.supfy.app.account.AccountController.ACCOUNT_URL)}}
                                if(!account.openAccountMenu(::fallback))fallback()
                            },
                            onAccountLogout = {
                                showSettings=false;welcomeDismissed=false;account.markUnknown()
                                webView?.let {mainClient?.loadUrl(it,com.supfy.app.account.AccountController.LOGOUT_URL)}
                            },
                            onAccountRefresh = account::refresh,
                            onOpenDiagnostics = {showDiagnostics=true},
                        )
                    }

                    AnimatedVisibility(
                        visible = showProtection,
                        enter = slideInHorizontally { it } + fadeIn(),
                        exit = slideOutHorizontally { it } + fadeOut(),
                    ) {
                        ProtectionScreen(
                            engine = engineState,
                            serviceWorkerFiltering = protection.serviceWorkerFiltering,
                            onOptionsChange = ::setProtectionOptions,
                            support = support,
                            stats = uiState.stats,
                            desktopMode = desktop,
                            onResetPage = ::resetPage,
                            onDisableDesktopMode = { setDesktopMode(false) },
                            onClose = { showProtection = false },
                        )
                    }
                    if(showWelcome) QuantumLoginScreen(
                        loading=false,error=if(!online)"Bağlantı yok. Giriş yapmak için internete bağlan." else null,
                        onLogin=::login,onSignUp=::signUp,onContinue=::dismissWelcome)
                    if(showNowPlaying) QuantumNowPlaying(mediaState,onClose={showNowPlaying=false},
                        onPlayPause=::togglePlayback,onNext={playback.command(PlaybackCommand.NEXT)},
                        onPrevious={playback.command(PlaybackCommand.PREVIOUS)},onQueue={
                            showNowPlaying=false;webView?.let {mainClient?.loadUrl(it,"https://open.spotify.com/queue")}
                        })
                    if(showDiagnostics) RuntimeDiagnosticsScreen(mediaHealth,serviceHealth,engineState.audioDiagnostics,
                        onClose={showDiagnostics=false},onCopy={report->
                            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Supfy durum tanısı",report))
                            Toast.makeText(this@MainActivity,"Sayısal tanı kopyalandı.",Toast.LENGTH_SHORT).show()
                        },onNotificationSettings={
                            try {
                                startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName))
                            } catch(_:ActivityNotFoundException) {
                                Toast.makeText(this@MainActivity,"Android bildirim ayarları açılamadı.",Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                providerUri?.let {uri->
                    AlertDialog(onDismissRequest={providerUri=null},
                        title={Text("Sağlayıcı ile giriş")},
                        text={Text("Bu giriş sağlayıcısı tarayıcı kullanır. Tarayıcı oturumu Supfy oynatıcısına aktarılmaz. Supfy içinde Spotify e-posta veya kod girişini kullanabilirsiniz.")},
                        confirmButton={TextButton(onClick={providerUri=null;launchExternal(uri)}){Text("Tarayıcıda devam et")}},
                        dismissButton={TextButton(onClick={providerUri=null;openLogin()}){Text("E-posta ile giriş")}})
                }
                pendingUpdate?.let { update ->
                    AlertDialog(
                        onDismissRequest = { snooze(update) },
                        title = { Text(stringResource(R.string.update_title)) },
                        text = {
                            Text(
                                stringResource(
                                    R.string.update_body,
                                    BuildConfig.VERSION_NAME,
                                    BuildConfig.VERSION_CODE,
                                    update.versionName,
                                    update.versionCode,
                                ),
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                pendingUpdate = null
                                openExternally(Uri.parse(update.pageUrl))
                            }) { Text(stringResource(R.string.update_now)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { snooze(update) }) { Text(stringResource(R.string.update_later)) }
                        },
                    )
                }

                // Sonra tanımlanan BackHandler önceliklidir: önce ayarlar kapanır, sonra sayfa geri gider.
                BackHandler(enabled = !showSettings && !showProtection && !showNowPlaying && !showDiagnostics && !showWelcome && uiState.canGoBack) { webView?.goBack() }
                BackHandler(enabled = showSettings) { showSettings = false }
                BackHandler(enabled = showProtection) { showProtection = false }
                BackHandler(enabled = showWelcome) { dismissWelcome() }
                BackHandler(enabled = showNowPlaying) { showNowPlaying=false }
                BackHandler(enabled = showDiagnostics) {showDiagnostics=false}
            }
        }
    }

    private fun buildWebView(): WebView {
        val chrome=SupfyChromeClient(uiState)
        mainChrome=chrome
        val client=SupfyWebViewClient(uiState, ::openExternally, ::onRendererGone, protection)
        mainClient=client
        return WebViewFactory.create(
        context = this,
        desktopMode = appSettings.desktopMode.value,
        webViewClient = client,
        chromeClient = chrome,
    ).also {protection.attach(it);playback.attach(it);account.attach(it)}
    }

    private fun openLogin() {
        // Keep the view, window, history and CookieManager already used by the player.
        // WebView handles subsequent redirects and form POSTs without replaying them.
        webView?.let {mainClient?.loadUrl(it,PlayerNavigation.LOGIN_URL)}
    }

    private fun copyLoginDiagnostics() {
        val web=webView?:return
        fun b(value:Boolean)=if(value)1 else 0
        val origin=when(com.supfy.app.web.LoginDiagnostics.host(web.url)) {
            com.supfy.app.web.LoginDiagnostics.Host.ACCOUNTS->1
            com.supfy.app.web.LoginDiagnostics.Host.PLAYER->2
            else->3
        }
        val visibleRect=android.graphics.Rect()
        val visible=web.getGlobalVisibleRect(visibleRect)
        val native=mapOf("version" to BuildConfig.VERSION_CODE,"sdk" to android.os.Build.VERSION.SDK_INT,
            "stage" to uiState.pageStage.ordinal,"error" to (uiState.error?.kind?.ordinal?:-1),
            "nativeOrigin" to origin,"nativeRoute" to com.supfy.app.web.LoginDiagnostics.route(web.url),"width" to web.width,"height" to web.height,
            "attached" to b(web.isAttachedToWindow),"visibility" to web.visibility,
            "javascript" to b(web.settings.javaScriptEnabled),"domStorage" to b(web.settings.domStorageEnabled),
            "thirdPartyCookies" to b(CookieManager.getInstance().acceptThirdPartyCookies(web)),
            "desktop" to b(appSettings.desktopMode.value),"layer" to web.layerType,
            "earlySupported" to b(mainClient?.earlyProbeInstalled==true),"cacheMode" to web.settings.cacheMode,
            "probePending" to b(uiState.loginProbePending),"renderRetry" to b(uiState.loginRenderingRetried),
            "wrapContentHeight" to b(web.layoutParams?.height==ViewGroup.LayoutParams.WRAP_CONTENT),
            "shown" to b(web.isShown),"windowVisibility" to web.windowVisibility,
            "windowFocus" to b(web.hasWindowFocus()),"hardwareAccelerated" to b(web.isHardwareAccelerated),
            "nativeVisibleWidth" to if(visible)visibleRect.width() else 0,
            "nativeVisibleHeight" to if(visible)visibleRect.height() else 0,
            "alpha1000" to (web.alpha*1000).toInt())
        val report=uiState.loginDiagnostics.report(native,WebViewCompat.getCurrentWebViewPackage(this)?.versionName)
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Supfy giriş tanılaması",report))
        Toast.makeText(this,R.string.login_diagnostics_copied,Toast.LENGTH_LONG).show()
    }

    private fun setProtectionOptions(options: ProtectionOptions) {
        appSettings.setProtectionOptions(options)
        protection.configure(options)
    }

    private fun recoverAudio() {
        if (isDestroyed) return
        val url=uiState.currentUrl?.takeIf { UrlPolicy.decide(it)==UrlPolicy.Decision.LOAD_IN_APP } ?: UrlPolicy.HOME_URL
        CookieManager.getInstance().flush()
        replaceWebView(url)
        Toast.makeText(this,R.string.audio_recovery_reset,Toast.LENGTH_LONG).show()
    }

    private fun retry() {
        val failing = uiState.error?.failingUrl
        uiState.error = null
        val wv = webView ?: return
        if (failing != null && UrlPolicy.decide(failing) == UrlPolicy.Decision.LOAD_IN_APP) {
            mainClient?.loadUrl(wv,failing)
        } else {
            wv.reload()
        }
    }

    private fun setDesktopMode(enabled: Boolean) {
        appSettings.setDesktopMode(enabled)
        webView?.let {
            WebViewFactory.applyDesktopMode(it, enabled)
            it.reload()
        }
    }

    /** Yalnızca HTTPS bağlantılar dışarıda açılır. */
    private fun openExternally(uri: Uri) {
        if (uri.scheme?.lowercase() != "https") return
        if(uiState.authenticating && com.supfy.app.web.AuthRedirectGate.isProvider(uri.toString())) {providerUri=uri;return}
        launchExternal(uri)
    }
    private fun launchExternal(uri:Uri) {
        if(uri.scheme?.lowercase()!="https")return
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        try {
            startActivity(intent)
            uiState.stats = uiState.stats.withExternalOpen()
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    private var lastRendererCrashMs = 0L

    private fun onRendererGone() {
        protection.rendererGone()
        val now = System.currentTimeMillis()
        val repeated = now - lastRendererCrashMs < RENDERER_CRASH_WINDOW_MS
        lastRendererCrashMs = now
        if (repeated) {
            // Kısa sürede ikinci çökme: döngüye girmemek için sayfayı otomatik yüklemeyiz;
            // kullanıcı hata ekranından "Tekrar dene" ile kendisi başlatır.
            webView?.let { destroyWebView(it, rendererGone = true) }
            webView = buildWebView()
            uiState.canGoBack = false
            uiState.error = PageError(PageErrorKind.GENERIC, UrlPolicy.HOME_URL)
        } else {
            replaceWebView(UrlPolicy.HOME_URL, rendererGone = true)
            Toast.makeText(this, R.string.error_renderer, Toast.LENGTH_SHORT).show()
        }
    }

    private fun replaceWebView(url: String, rendererGone: Boolean = false) {
        webView?.let { destroyWebView(it, rendererGone) }
        webView = null
        uiState.error = null
        uiState.canGoBack = false
        uiState.progress = 0
        webView = buildWebView().also { mainClient?.loadUrl(it,url) }
    }

    /**
     * GitHub Releases'tan yeni sürüm denetler. Otomatik denetim 12 saatte bir yapılır;
     * hata olursa sessizce geçilir. Elle denetimde sonuç kullanıcıya bildirilir.
     */
    private fun checkForUpdates(manual: Boolean) {
        if (updateCheckRunning) return
        val now = System.currentTimeMillis()
        if (!manual && !ReleaseSelector.shouldAutoCheck(now, appSettings.lastUpdateCheckMs)) return
        updateCheckRunning = true
        lifecycleScope.launch {
            val result = updates.check(BuildConfig.VERSION_CODE)
            updateCheckRunning = false
            when (result) {
                is UpdateRepository.Result.Available -> {
                    appSettings.markUpdateChecked(now)
                    val offer = manual || ReleaseSelector.shouldOffer(
                        result.update, appSettings.snoozedVersionCode, appSettings.snoozedUntilMs, now,
                    )
                    if (offer) pendingUpdate = result.update
                }
                is UpdateRepository.Result.UpToDate -> {
                    appSettings.markUpdateChecked(now)
                    if (manual) Toast.makeText(this@MainActivity, R.string.update_none, Toast.LENGTH_SHORT).show()
                }
                is UpdateRepository.Result.Failed ->
                    if (manual) Toast.makeText(this@MainActivity, R.string.update_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun snooze(update: AvailableUpdate) {
        pendingUpdate = null
        appSettings.snoozeUpdate(update.versionCode, System.currentTimeMillis() + ReleaseSelector.SNOOZE_MS)
    }

    /** WebView'i yeniden kurar; çerezler ve oturum korunur. */
    private fun resetPage() {
        val url = uiState.currentUrl
            ?.takeIf { UrlPolicy.decide(it) == UrlPolicy.Decision.LOAD_IN_APP }
            ?: UrlPolicy.HOME_URL
        CookieManager.getInstance().flush()
        replaceWebView(url)
        Toast.makeText(this, R.string.page_reset_done, Toast.LENGTH_SHORT).show()
    }

    /** Yalnızca önbelleği siler; oturum ve çerezler korunur. */
    private fun clearCache() {
        webView?.clearCache(true)
        Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun clearSiteData() {
        val cookies = CookieManager.getInstance()
        WebStorage.getInstance().deleteAllData()
        webView?.clearCache(true)
        // removeAllCookies asenkron çalışır; sayfa silme bittikten sonra yüklenmeli,
        // yoksa eski oturum çerezleriyle yeniden açılabilir.
        cookies.removeAllCookies {
            cookies.flush()
            if (!isDestroyed) {
                replaceWebView(UrlPolicy.HOME_URL)
                Toast.makeText(this, R.string.clear_done, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun destroyWebView(wv: WebView, rendererGone: Boolean = false) {
        mainClient?.close()
        playback.detach();account.detach()
        protection.detach()
        mainChrome?.close()
        (wv.parent as? ViewGroup)?.removeView(wv)
        if (!rendererGone) wv.stopLoading()
        wv.destroy()
    }

    override fun onStart() {
        super.onStart()
        connectivity.start()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        mainClient?.setForeground(true)
        if(::playback.isInitialized)playback.setForeground(true)
        if(::account.isInitialized)account.setForeground(true)
        if (::protection.isInitialized) protection.setForeground(true)
    }

    override fun onPause() {
        if(!appSettings.backgroundPlayback.value)playback.command(com.supfy.app.playback.PlaybackCommand.PAUSE)
        playback.setForeground(false);account.setForeground(false)
        mainClient?.setForeground(false)
        protection.setForeground(false)
        CookieManager.getInstance().flush()
        if (!appSettings.backgroundPlayback.value) webView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        connectivity.stop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Geçmiş çok büyükse sistemin kayıt sınırını (≈1 MB) aşıp çökmeye yol açabilir;
        // sınırı aşarsa geçmiş kaydedilmez, açılışta ana sayfa yüklenir (oturum çerezlerde durur).
        val wv = webView ?: return
        val state = Bundle()
        wv.saveState(state)
        if (bundleSize(state) <= MAX_SAVED_STATE_BYTES) outState.putAll(state)
    }

    private fun bundleSize(bundle: Bundle): Int {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(bundle)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    private companion object {
        const val RENDERER_CRASH_WINDOW_MS = 30_000L
        const val MAX_SAVED_STATE_BYTES = 200 * 1024
    }

    override fun onDestroy() {
        webView?.let { destroyWebView(it) }
        webView = null
        if (::protection.isInitialized) protection.close()
        if(::playback.isInitialized)playback.close()
        if(::account.isInitialized)account.close()
        super.onDestroy()
    }
}
