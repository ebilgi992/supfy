package com.music.spotui.ui

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.platform.app.InstrumentationRegistry
import app.myspoti.V5ConsentTestActivity
import app.myspoti.engine.R
import com.music.spotui.data.preferences.Look
import com.music.spotui.data.preferences.LookPref
import com.music.spotui.privacy.ConsentPolicy
import com.music.spotui.ui.components.V5PrivacyConsentHost
import com.music.spotui.ui.components.V5PrivacySettings
import com.music.spotui.ui.look.ProvideLook
import com.music.spotui.ui.theme.SpotuiTheme
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Isolated real Compose components in each look; no authenticated navigation is simulated. */
class PrivacyConsentUiTest {
    @Test fun bothLooksShowDisclosureAndHonorRealDeclineAndAllowButtons() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val feature = ConsentPolicy.Feature.TRANSLATION
        val prefs = context.getSharedPreferences("supfy_optional_services", Context.MODE_PRIVATE)
        val savedGrant = prefs.getString(feature.name, null)
        val savedLook = LookPref.look
        val locale = AppCompatDelegate.getApplicationLocales()
        instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("tr-TR")) }
        val activity = instrumentation.startActivitySync(Intent(context, V5ConsentTestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as V5ConsentTestActivity
        fun waitFor(message: String, condition: () -> Boolean) {
            val deadline = android.os.SystemClock.elapsedRealtime() + 45_000
            while (!condition()) { if (android.os.SystemClock.elapsedRealtime() >= deadline) fail(message); Thread.sleep(150) }
        }
        fun find(text: String): AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow
            ?.findAccessibilityNodeInfosByText(text)?.firstOrNull { it.isVisibleToUser && it.text?.toString() == text }
        fun click(text: String) {
            waitFor("Missing visible button: $text") { find(text) != null }
            var node = find(text)
            repeat(5) {
                if (node?.isClickable == true) { assertTrue(node!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)); return }
                node = node?.parent
            }
            fail("No clickable parent: $text")
        }
        // Test-only, bounded method samples for an input-dispatch timeout. No UI
        // text, account data or production diagnostic/export path is collected.
        val stage = java.util.concurrent.atomic.AtomicReference("focus")
        val watch = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
        val samples = java.util.concurrent.atomic.AtomicInteger()
        watch.scheduleAtFixedRate({
            if (samples.incrementAndGet() <= 20) {
                val frames = android.os.Looper.getMainLooper().thread.stackTrace
                if (frames.none { it.methodName == "nativePollOnce" }) {
                    instrumentation.sendStatus(2, android.os.Bundle().apply {
                        putString("v5_consent_stage", stage.get())
                        putString("v5_consent_main_methods", frames.take(30).joinToString("\n"))
                    })
                }
            }
        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS)
        try {
            waitFor("Consent test Activity did not become focused") {
                var focused = false
                instrumentation.runOnMainSync { focused = !activity.isFinishing && activity.hasWindowFocus() }
                focused
            }
            // AppCompat localizes the Activity on API <33; application resources can
            // still follow the English emulator locale. Assert the actual UI locale
            // and query the visible Activity's Turkish resources.
            assertEquals("tr", activity.resources.configuration.locales[0].language)
            for (look in listOf(Look.IMMERSIVE, Look.CLASSIC)) {
                stage.set("compose_${look.name}")
                instrumentation.runOnMainSync {
                    LookPref.set(context, look)
                    activity.setContent { SpotuiTheme { ProvideLook { V5PrivacySettings(); V5PrivacyConsentHost() } } }
                    ConsentPolicy.revoke(feature)
                    ConsentPolicy.requestConsent(feature)
                }
                stage.set("disclosure_${look.name}")
                waitFor("Turkish disclosure did not render in $look") { find(activity.getString(R.string.v5_privacy_translation_disclosure)) != null }
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    File(context.cacheDir, "v5-ui-${look.name.lowercase(java.util.Locale.ROOT)}.png").outputStream().use {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }; bitmap.recycle()
                }
                stage.set("decline_${look.name}")
                click(activity.getString(R.string.v5_privacy_decline))
                waitFor("Decline did not resolve $look") { ConsentPolicy.pending.value == null }
                assertFalse(ConsentPolicy.isGranted(feature))
                instrumentation.runOnMainSync { ConsentPolicy.requestConsent(feature) }
                stage.set("allow_${look.name}")
                click(activity.getString(R.string.v5_privacy_allow))
                waitFor("Allow did not persist $look") { ConsentPolicy.isGranted(feature) }
            }
        } finally {
            watch.shutdownNow()
            prefs.edit().apply { if (savedGrant == null) remove(feature.name) else putString(feature.name, savedGrant) }.commit()
            ConsentPolicy.initialize(context)
            instrumentation.runOnMainSync { LookPref.set(context, savedLook); activity.finish(); AppCompatDelegate.setApplicationLocales(locale) }
        }
    }
}
