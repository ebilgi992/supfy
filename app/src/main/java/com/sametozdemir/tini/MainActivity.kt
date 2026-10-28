package com.sametozdemir.tini

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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewCompat
import com.sametozdemir.tini.data.AppSettings
import com.sametozdemir.tini.data.ConnectivityObserver
import com.sametozdemir.tini.ui.TiniScreen
import com.sametozdemir.tini.ui.SettingsScreen
import com.sametozdemir.tini.ui.theme.TiniTheme
import com.sametozdemir.tini.web.TiniChromeClient
import com.sametozdemir.tini.web.TiniWebViewClient
import com.sametozdemir.tini.web.PageErrorKind
import com.sametozdemir.tini.web.UrlPolicy
import com.sametozdemir.tini.web.WebUiState
import com.sametozdemir.tini.web.WebViewFactory

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

        val wv = buildWebView()
        webView = wv
        // Süreç sistem tarafından kapatıldıysa kaldığı sayfaya döner.
        val restored = savedInstanceState?.let { wv.restoreState(it) } != null
        if (!restored) wv.loadUrl(UrlPolicy.HOME_URL)

        val webViewVersion = WebViewCompat.getCurrentWebViewPackage(this)?.versionName

        setContent {
            TiniTheme {
                val online by connectivity.online.collectAsStateWithLifecycle()
                val background by appSettings.backgroundPlayback.collectAsStateWithLifecycle()
                val desktop by appSettings.desktopMode.collectAsStateWithLifecycle()
                var showSettings by rememberSaveable { mutableStateOf(false) }

                // Bağlantı geri gelince ağ hatası ekranındaysak otomatik yeniden dene.
                LaunchedEffect(online) {
                    val kind = uiState.error?.kind
                    if (online && (kind == PageErrorKind.NO_CONNECTION || kind == PageErrorKind.TIMEOUT)) {
                        retry()
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    TiniScreen(
                        webView = webView,
                        state = uiState,
                        online = online,
                        onRefresh = { webView?.reload() },
                        onHome = { webView?.loadUrl(UrlPolicy.HOME_URL) },
                        onOpenBrowser = { openExternally(Uri.parse(uiState.currentUrl ?: UrlPolicy.HOME_URL)) },
                        onRetry = ::retry,
                        onOpenSettings = { showSettings = true },
                    )

                    AnimatedVisibility(
                        visible = showSettings,
                        enter = slideInHorizontally { it } + fadeIn(),
                        exit = slideOutHorizontally { it } + fadeOut(),
                    ) {
                        SettingsScreen(
                            backgroundPlayback = background,
                            desktopMode = desktop,
                            webViewVersion = webViewVersion,
                            onBackgroundPlaybackChange = appSettings::setBackgroundPlayback,
                            onDesktopModeChange = ::setDesktopMode,
                            onClearData = ::clearSiteData,
                            onClose = { showSettings = false },
                        )
                    }
                }

                // Sonra tanımlanan BackHandler önceliklidir: önce ayarlar kapanır, sonra sayfa geri gider.
                BackHandler(enabled = !showSettings && uiState.canGoBack) { webView?.goBack() }
                BackHandler(enabled = showSettings) { showSettings = false }
            }
        }
    }

    private fun buildWebView(): WebView = WebViewFactory.create(
        context = this,
        desktopMode = appSettings.desktopMode.value,
        webViewClient = TiniWebViewClient(uiState, ::openExternally, ::onRendererGone),
        chromeClient = TiniChromeClient(uiState),
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
