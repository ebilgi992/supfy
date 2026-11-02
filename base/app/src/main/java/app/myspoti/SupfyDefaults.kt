package app.myspoti

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Migrates preferences without copying or deleting cookies, media, or account data. */
object SupfyDefaults {
    private lateinit var context: Context
    private val mutableBackground = MutableStateFlow(true)
    val backgroundAllowed = mutableBackground.asStateFlow()
    private val preferences get() = context.getSharedPreferences("supfy_v5_defaults", Context.MODE_PRIVATE)

    internal fun languageTag(saved: String?, platform: String?, platformAuthoritative: Boolean): String =
        if (platformAuthoritative) platform.orEmpty() else saved ?: platform?.takeIf { it.isNotEmpty() } ?: "tr-TR"

    fun initialize(value: Context) {
        context = value.applicationContext
        val legacy = context.getSharedPreferences("supfy_ayarlar", Context.MODE_PRIVATE)
        mutableBackground.value = preferences.getBoolean("background_allowed", legacy.getBoolean("arka_plan_oynatma", true))
        val platform = if (Build.VERSION.SDK_INT >= 33)
            context.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales?.toLanguageTags()
        else AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotEmpty() }
        val saved = preferences.getString("locale_tag", null)
        val tag = languageTag(saved, platform,
            Build.VERSION.SDK_INT >= 33 && preferences.getBoolean("locale_initialized", false))
        // On Android 10–12 AppCompat restores its old locale only when the first activity
        // attaches. Leave the missing V5 key unset until that restoration has happened.
        if (saved != null || Build.VERSION.SDK_INT >= 33)
            preferences.edit().putString("locale_tag", tag).apply()
        applyServiceLocale(tag)
    }

    fun setBackgroundAllowed(value: Boolean) {
        preferences.edit().putBoolean("background_allowed", value).apply()
        mutableBackground.value = value
    }

    /** Also used for the explicit "follow system" choice; empty is distinct from missing. */
    fun setLanguagePreference(tag: String) {
        preferences.edit().putString("locale_tag", tag).apply()
        applyServiceLocale(tag)
    }

    fun applyInitialLocale() {
        val restored = AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotEmpty() }
        val tag = preferences.getString("locale_tag", null) ?: restored ?: "tr-TR"
        preferences.edit().putString("locale_tag", tag).apply()
        val selected = LocaleListCompat.forLanguageTags(tag)
        if (AppCompatDelegate.getApplicationLocales() != selected) AppCompatDelegate.setApplicationLocales(selected)
        preferences.edit().putBoolean("locale_initialized", true).apply()
        applyServiceLocale(tag)
    }

    private fun applyServiceLocale(tag: String) {
        val locale = tag.takeIf { it.isNotEmpty() }?.let(Locale::forLanguageTag)
            ?: android.content.res.Resources.getSystem().configuration.locales[0]
        Locale.setDefault(locale)
        // Align owned service/widget resources even when no activity has started this process.
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(Configuration(context.resources.configuration).apply { setLocale(locale) }, context.resources.displayMetrics)
    }
}
