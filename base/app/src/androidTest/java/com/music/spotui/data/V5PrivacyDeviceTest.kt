package com.music.spotui.data

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import app.myspoti.SupfyDefaults
import app.myspoti.engine.R
import com.music.spotui.data.api.UpdateChecker
import com.music.spotui.data.preferences.Look
import com.music.spotui.data.preferences.LookPref
import com.music.spotui.privacy.ConsentPolicy
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

/** Device checks use synthetic preferences only and restore the exact original values. */
class V5PrivacyDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun optionalServiceIsDeniedUntilDisclosureIsAcceptedAndRevocationStopsNewWork() = runBlocking {
        val feature = ConsentPolicy.Feature.TRANSLATION
        val prefs = context.getSharedPreferences("supfy_optional_services", Context.MODE_PRIVATE)
        val saved = prefs.getString(feature.name, null)
        try {
            ConsentPolicy.revoke(feature)
            ConsentPolicy.requestConsent(feature)
            val denied = async(start = CoroutineStart.UNDISPATCHED) { ConsentPolicy.await(feature) }
            assertFalse(denied.isCompleted)
            assertEquals(feature, ConsentPolicy.pending.value)
            ConsentPolicy.dismiss(feature)
            assertFalse(withTimeout(2000) { denied.await() })
            assertFalse(ConsentPolicy.isGranted(feature))
            ConsentPolicy.requestConsent(feature)
            val allowed = async(start = CoroutineStart.UNDISPATCHED) { ConsentPolicy.await(feature) }
            ConsentPolicy.grant(feature)
            assertTrue(withTimeout(2000) { allowed.await() })
            assertTrue(ConsentPolicy.isGranted(feature))
            ConsentPolicy.initialize(context)
            assertTrue(ConsentPolicy.isGranted(feature))
            ConsentPolicy.revoke(feature)
            assertFalse(withTimeout(2000) { ConsentPolicy.await(feature) })
        } finally {
            prefs.edit().apply { if (saved == null) remove(feature.name) else putString(feature.name, saved) }.commit()
            ConsentPolicy.initialize(context)
        }
    }

    @Test fun automaticUpdatesDefaultOffAndTheChoicePersists() {
        val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
        val existed = prefs.contains("automatic_v5")
        val saved = prefs.getBoolean("automatic_v5", false)
        try {
            prefs.edit().remove("automatic_v5").commit()
            assertFalse(UpdateChecker.automaticEnabled(context))
            UpdateChecker.setAutomaticEnabled(context, true)
            assertTrue(UpdateChecker.automaticEnabled(context))
            UpdateChecker.setAutomaticEnabled(context, false)
            assertFalse(UpdateChecker.automaticEnabled(context))
        } finally {
            prefs.edit().apply { if (existed) putBoolean("automatic_v5", saved) else remove("automatic_v5") }.commit()
        }
    }

    @Test fun legacyBackgroundChoiceIsReadWithoutChangingLegacyData() {
        val legacy = context.getSharedPreferences("supfy_ayarlar", Context.MODE_PRIVATE)
        val current = context.getSharedPreferences("supfy_v5_defaults", Context.MODE_PRIVATE)
        val oldLegacy = legacy.all["arka_plan_oynatma"] as? Boolean
        val oldCurrent = current.all["background_allowed"] as? Boolean
        try {
            legacy.edit().putBoolean("arka_plan_oynatma", false).commit()
            current.edit().remove("background_allowed").commit()
            SupfyDefaults.initialize(context)
            assertFalse(SupfyDefaults.backgroundAllowed.value)
            SupfyDefaults.setBackgroundAllowed(true)
            assertTrue(SupfyDefaults.backgroundAllowed.value)
            assertFalse(legacy.getBoolean("arka_plan_oynatma", true))
            SupfyDefaults.initialize(context)
            assertTrue(SupfyDefaults.backgroundAllowed.value)
        } finally {
            legacy.edit().apply { if (oldLegacy == null) remove("arka_plan_oynatma") else putBoolean("arka_plan_oynatma", oldLegacy) }.commit()
            current.edit().apply { if (oldCurrent == null) remove("background_allowed") else putBoolean("background_allowed", oldCurrent) }.commit()
            SupfyDefaults.initialize(context)
        }
    }

    @Test fun immersiveFallbackAndClassicSelectionSurvivePreferenceReload() {
        val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("look", null)
        try {
            assertEquals(Look.IMMERSIVE, Look.of(null))
            LookPref.set(context, Look.CLASSIC)
            assertEquals(Look.CLASSIC, Look.of(prefs.getString("look", null)))
            LookPref.set(context, Look.IMMERSIVE)
            assertEquals(Look.IMMERSIVE, Look.of(prefs.getString("look", null)))
        } finally {
            LookPref.set(context, Look.of(saved))
            if (saved == null) prefs.edit().remove("look").commit()
        }
    }

    @Test fun packagedIdentityAndTurkishPrivacyResourcesArePresent() {
        assertEquals("com.supfy.app", context.packageName)
        assertEquals("Supfy", context.getString(R.string.app_name))
        val tr = context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale.forLanguageTag("tr-TR"))
        })
        assertEquals("İzin ver", tr.getString(R.string.v5_privacy_allow))
        assertEquals("Şimdi değil", tr.getString(R.string.v5_privacy_decline))
    }
}
