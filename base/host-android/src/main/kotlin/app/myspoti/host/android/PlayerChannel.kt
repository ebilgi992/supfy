package app.myspoti.host.android

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * A native message endpoint available only to the official player's main frame.
 * Unsupported WebViews receive no endpoint; WebPlayerHost publishes the capability error.
 * There is deliberately no JavascriptInterface fallback.
 */
internal fun WebView.exposeToPlayer(name: String, onMessage: (String) -> Unit): PlayerChannel? {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return null
    WebViewCompat.addWebMessageListener(this, name, setOf(WebPlayerSecurityPolicy.PLAYER_ORIGIN)) { source, message, origin, isMainFrame, _ ->
        if (WebPlayerSecurityPolicy.acceptMessage(origin.toString(), isMainFrame) &&
            WebPlayerSecurityPolicy.isTrustedPlayer(source.url)) {
            runCatching { message.data }.getOrNull()?.let(onMessage)
        }
    }
    return null
}

/** Retains the relay's nullable lifecycle type; no JavaScript object or secret is exposed. */
internal class PlayerChannel
