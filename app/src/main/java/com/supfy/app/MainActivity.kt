package com.supfy.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.supfy.app.web.PageErrorKind
import com.supfy.app.web.UrlPolicy
import com.supfy.app.web.WebUiState
import com.supfy.app.web.WebViewFactory

class MainActivity : ComponentActivity() {

    private lateinit var appSettings: AppSettings
    private lateinit var connectivity: ConnectivityObserver
    private val uiState = WebUiState()
    private var webView: WebView? by mutableStateOf<WebView?>(null)

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
                        onOpenBrowser = { openExternally(Uri.parse(uiState.currentUrl ?: UrlPolicy.HOME_URL)) },
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
                            desktopMode = desktop,
                            onResetPage = ::resetPage,
                            onDisableDesktopMode = { setDesktopMode(false) },
                            onClose = { showProtection = false },
                        )
                    }
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

    private fun onRendererGone() {
        replaceWebView(UrlPolicy.HOME_URL)
        Toast.makeText(this, R.string.error_renderer, Toast.LENGTH_SHORT).show()
    }

    private fun replaceWebView(url: String) {
        webView?.let { destroyWebView(it) }
        webView = null
        uiState.error = null
        uiState.canGoBack = false
        uiState.progress = 0
        webView = buildWebView().also { it.loadUrl(url) }
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
        cookies.removeAllCookies { cookies.flush() }
        WebStorage.getInstance().deleteAllData()
        webView?.clearCache(true)
        replaceWebView(UrlPolicy.HOME_URL)
        Toast.makeText(this, R.string.clear_done, Toast.LENGTH_SHORT).show()
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
        webView?.saveState(outState)
    }

    override fun onDestroy() {
        webView?.let { destroyWebView(it) }
        webView = null
        super.onDestroy()
    }
}
