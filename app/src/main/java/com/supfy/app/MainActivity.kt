package com.supfy.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import com.supfy.app.web.DiagLog
import com.supfy.app.web.PageError
import com.supfy.app.web.PageErrorKind
import com.supfy.app.web.UrlPolicy
import com.supfy.app.web.WebUiState
import com.supfy.app.web.WebViewFactory

class MainActivity : ComponentActivity() {

    private lateinit var appSettings: AppSettings
    private lateinit var connectivity: ConnectivityObserver
    private val uiState = WebUiState()
    private var webView: WebView? by mutableStateOf<WebView?>(null)
    private val updates = UpdateRepository(BuildConfig.UPDATE_REPO)
    private var pendingUpdate: AvailableUpdate? by mutableStateOf<AvailableUpdate?>(null)
    private var updateCheckRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        appSettings = AppSettings(applicationContext)
        connectivity = ConnectivityObserver(applicationContext)

        WebViewFactory.hardenServiceWorkers()
        val wv = buildWebView()
        webView = wv
        // Süreç sistem tarafından kapatıldıysa kaldığı sayfaya döner.
        val restored = savedInstanceState?.let { wv.restoreState(it) } != null
        if (!restored) wv.loadUrl(UrlPolicy.HOME_URL)

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
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var showProtection by rememberSaveable { mutableStateOf(false) }

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
                    SupfyScreen(
                        webView = webView,
                        state = uiState,
                        online = online,
                        onRefresh = { webView?.reload() },
                        onHome = { webView?.loadUrl(UrlPolicy.HOME_URL) },
                        onRetry = ::retry,
                        onOpenSettings = { showSettings = true },
                        onOpenProtection = { showProtection = true },
                    )

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
                        )
                    }

                    AnimatedVisibility(
                        visible = showProtection,
                        enter = slideInHorizontally { it } + fadeIn(),
                        exit = slideOutHorizontally { it } + fadeOut(),
                    ) {
                        ProtectionScreen(
                            support = support,
                            stats = uiState.stats,
                            diag = uiState.diag,
                            desktopMode = desktop,
                            onResetPage = ::resetPage,
                            onDisableDesktopMode = { setDesktopMode(false) },
                            onCopyDiagnostics = { copyDiagnostics(webViewVersion, desktop) },
                            onClose = { showProtection = false },
                        )
                    }
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
                BackHandler(enabled = !showSettings && !showProtection && uiState.canGoBack) { webView?.goBack() }
                BackHandler(enabled = showSettings) { showSettings = false }
                BackHandler(enabled = showProtection) { showProtection = false }
            }
        }
    }

    private fun buildWebView(): WebView = WebViewFactory.create(
        context = this,
        desktopMode = appSettings.desktopMode.value,
        webViewClient = SupfyWebViewClient(uiState, ::openExternally, ::onRendererGone),
        chromeClient = SupfyChromeClient(uiState),
    )

    private fun retry() {
        val failing = uiState.error?.failingUrl
        uiState.error = null
        val wv = webView ?: return
        if (failing != null && UrlPolicy.decide(failing) == UrlPolicy.Decision.LOAD_IN_APP) {
            wv.loadUrl(failing)
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
        val now = System.currentTimeMillis()
        val repeated = now - lastRendererCrashMs < RENDERER_CRASH_WINDOW_MS
        lastRendererCrashMs = now
        if (repeated) {
            // Kısa sürede ikinci çökme: döngüye girmemek için sayfayı otomatik yüklemeyiz;
            // kullanıcı hata ekranından "Tekrar dene" ile kendisi başlatır.
            webView?.let { destroyWebView(it) }
            webView = buildWebView()
            uiState.canGoBack = false
            uiState.error = PageError(PageErrorKind.GENERIC, UrlPolicy.HOME_URL)
        } else {
            replaceWebView(UrlPolicy.HOME_URL)
            Toast.makeText(this, R.string.error_renderer, Toast.LENGTH_SHORT).show()
        }
    }

    private fun replaceWebView(url: String) {
        webView?.let { destroyWebView(it) }
        webView = null
        uiState.error = null
        uiState.canGoBack = false
        uiState.progress = 0
        webView = buildWebView().also { it.loadUrl(url) }
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

    /** Tanılama kaydını panoya kopyalar (adreslerde sorgu kısmı yoktur; çerez/token içermez). */
    private fun copyDiagnostics(webViewVersion: String?, desktop: Boolean) {
        val header = "Supfy ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · " +
            "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · " +
            "WebView ${webViewVersion ?: "?"} · masaüstü=${if (desktop) "açık" else "kapalı"} · " +
            "sayfa=${DiagLog.sanitizeUrl(uiState.currentUrl)}"
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Supfy tanılama", uiState.diag.format(header)))
        Toast.makeText(this, R.string.diag_copied, Toast.LENGTH_SHORT).show()
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

    private fun destroyWebView(wv: WebView) {
        (wv.parent as? ViewGroup)?.removeView(wv)
        wv.stopLoading()
        wv.destroy()
    }

    override fun onStart() {
        super.onStart()
        connectivity.start()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
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
        super.onDestroy()
    }
}
