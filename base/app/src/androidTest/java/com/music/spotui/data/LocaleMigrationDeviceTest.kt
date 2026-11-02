package com.music.spotui.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import app.myspoti.SupfyDefaults
import org.junit.Assert.*
import org.junit.Test

class LocaleMigrationDeviceTest {
    @SdkSuppress(maxSdkVersion = 32)
    @Test fun firstMigrationWaitsForAppCompatRestorationBeforePersistingLocale() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        val prefix = "synthetic-v5-locale-${java.util.UUID.randomUUID()}-"
        val names = mutableSetOf<String>()
        val isolated = object : ContextWrapper(target) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                names += prefix + name
                return target.getSharedPreferences(prefix + name, mode)
            }
        }
        val savedTag = target.getSharedPreferences("supfy_v5_defaults", Context.MODE_PRIVATE).getString("locale_tag", null)
        val storedAppCompat = target.getFileStreamPath("androidx.appcompat.app.AppCompatDelegate.application_locales_record_file")
            .takeIf { it.isFile }?.readText()?.let { Regex("application_locales=\"([^\"]*)\"").find(it)?.groupValues?.get(1) }
        // The getter alone cannot distinguish uninitialized process state from explicit system.
        val before = LocaleListCompat.forLanguageTags(savedTag ?: storedAppCompat ?: "tr-TR")
        try {
            instrumentation.runOnMainSync {
                try {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    SupfyDefaults.initialize(isolated)
                    val prefs = isolated.getSharedPreferences("supfy_v5_defaults", Context.MODE_PRIVATE)
                    assertFalse("Application startup must not overwrite a not-yet-restored locale", prefs.contains("locale_tag"))
                    // Simulate the delegate restoration between application and activity phases.
                    // This is not a process-death/real storage restoration claim.
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("de-DE"))
                    SupfyDefaults.applyInitialLocale()
                    assertEquals("de-DE", prefs.getString("locale_tag", null))
                    assertEquals("de-DE", AppCompatDelegate.getApplicationLocales().toLanguageTags())
                } finally {
                    AppCompatDelegate.setApplicationLocales(before)
                    SupfyDefaults.initialize(target)
                }
            }
        } finally {
            names.forEach { target.deleteSharedPreferences(it) }
        }
    }
}
