package com.music.spotui.ui

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.platform.app.InstrumentationRegistry
import app.myspoti.MainActivity
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
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
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
        try {
            for (look in listOf(Look.IMMERSIVE, Look.CLASSIC)) {
                instrumentation.runOnMainSync {
                    LookPref.set(context, look)
                    activity.setContent { SpotuiTheme { ProvideLook { V5PrivacySettings(); V5PrivacyConsentHost() } } }
                    ConsentPolicy.revoke(feature)
                    ConsentPolicy.requestConsent(feature)
                }
                waitFor("Turkish disclosure did not render in $look") { find(context.getString(R.string.v5_privacy_translation_disclosure)) != null }
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    File(context.cacheDir, "v5-ui-${look.name.lowercase(java.util.Locale.ROOT)}.png").outputStream().use {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }; bitmap.recycle()
                }
                click(context.getString(R.string.v5_privacy_decline))
                waitFor("Decline did not resolve $look") { ConsentPolicy.pending.value == null }
                assertFalse(ConsentPolicy.isGranted(feature))
                instrumentation.runOnMainSync { ConsentPolicy.requestConsent(feature) }
                click(context.getString(R.string.v5_privacy_allow))
                waitFor("Allow did not persist $look") { ConsentPolicy.isGranted(feature) }
            }
        } finally {
            prefs.edit().apply { if (savedGrant == null) remove(feature.name) else putString(feature.name, savedGrant) }.commit()
            ConsentPolicy.initialize(context)
            instrumentation.runOnMainSync { LookPref.set(context, savedLook); activity.finish(); AppCompatDelegate.setApplicationLocales(locale) }
        }
    }
}
