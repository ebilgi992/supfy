package com.supfy.app.quantum

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.supfy.app.data.AppSettings
import com.supfy.app.data.ThemeMode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Every preference operation is redirected to a UUID namespace; real app preferences are untouched. */
@RunWith(AndroidJUnit4::class)
class AppSettingsMigrationTest {
    private fun isolated(block: (Context, SharedPreferences) -> Unit) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "quantum_test_${UUID.randomUUID()}"
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(ignored: String, mode: Int): SharedPreferences =
                base.getSharedPreferences(name, mode)
        }
        try { block(context, context.getSharedPreferences("ignored", Context.MODE_PRIVATE)) }
        finally { base.deleteSharedPreferences(name) }
    }

    @Test fun freshPreferencesRequireWelcomeUntilAcknowledgedAndReloaded() = isolated { context, prefs ->
        val fresh = AppSettings(context)
        assertTrue(fresh.welcomeRequired)
        assertTrue(prefs.all.isEmpty())
        fresh.acknowledgeWelcome()
        assertFalse(AppSettings(context).welcomeRequired)
        assertEquals(true, prefs.getBoolean("supfy_karsilama_goruldu", false))
    }

    @Test fun upgradePreferencesSkipWelcomeAndPreserveExistingOptions() = isolated { context, prefs ->
        assertTrue(prefs.edit().putBoolean("arka_plan_oynatma", false)
            .putBoolean("masaustu_modu", true).putString("tema", "LIGHT")
            .putLong("son_guncelleme_denetimi", 12345L).commit())
        val before = prefs.all.toMap()
        val upgraded = AppSettings(context)
        assertFalse("Any preexisting preferences identify an upgrade", upgraded.welcomeRequired)
        assertFalse(upgraded.backgroundPlayback.value)
        assertTrue(upgraded.desktopMode.value)
        assertEquals(ThemeMode.LIGHT, upgraded.themeMode.value)
        assertEquals(12345L, upgraded.lastUpdateCheckMs)
        assertEquals("Migration reads must not rewrite existing values", before, prefs.all)
        upgraded.acknowledgeWelcome()
        assertFalse(AppSettings(context).welcomeRequired)
        for ((key, value) in before) assertEquals(value, prefs.all[key])
    }

    @Test fun explicitUnacknowledgedMarkerStillRequiresWelcomeOnReload() = isolated { context, prefs ->
        assertTrue(prefs.edit().putBoolean("supfy_karsilama_goruldu", false).putString("tema", "DARK").commit())
        assertTrue(AppSettings(context).welcomeRequired)
    }
}
