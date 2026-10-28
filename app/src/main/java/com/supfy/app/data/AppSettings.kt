package com.supfy.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Uygulama arayüzünün teması (Spotify sayfasının kendi teması değişmez). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Yalnızca uygulama tercihleri ve güncelleme denetimi zamanları saklanır; kişisel veri tutulmaz. */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _backgroundPlayback = MutableStateFlow(prefs.getBoolean(KEY_BACKGROUND, true))
    val backgroundPlayback: StateFlow<Boolean> = _backgroundPlayback.asStateFlow()

    private val _desktopMode = MutableStateFlow(prefs.getBoolean(KEY_DESKTOP, false))
    val desktopMode: StateFlow<Boolean> = _desktopMode.asStateFlow()

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeMode.SYSTEM),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(value: ThemeMode) {
        prefs.edit().putString(KEY_THEME, value.name).apply()
        _themeMode.value = value
    }

    // --- Güncelleme denetimi
    val lastUpdateCheckMs: Long get() = prefs.getLong(KEY_LAST_CHECK, 0L)
    val snoozedVersionCode: Int get() = prefs.getInt(KEY_SNOOZED_CODE, -1)
    val snoozedUntilMs: Long get() = prefs.getLong(KEY_SNOOZED_UNTIL, 0L)

    fun markUpdateChecked(nowMs: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK, nowMs).apply()
    }

    fun snoozeUpdate(versionCode: Int, untilMs: Long) {
        prefs.edit().putInt(KEY_SNOOZED_CODE, versionCode).putLong(KEY_SNOOZED_UNTIL, untilMs).apply()
    }

    fun setBackgroundPlayback(value: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND, value).apply()
        _backgroundPlayback.value = value
    }

    fun setDesktopMode(value: Boolean) {
        prefs.edit().putBoolean(KEY_DESKTOP, value).apply()
        _desktopMode.value = value
    }

    private companion object {
        const val FILE = "supfy_ayarlar"
        const val KEY_BACKGROUND = "arka_plan_oynatma"
        const val KEY_DESKTOP = "masaustu_modu"
        const val KEY_THEME = "tema"
        const val KEY_LAST_CHECK = "son_guncelleme_denetimi"
        const val KEY_SNOOZED_CODE = "ertelenen_surum"
        const val KEY_SNOOZED_UNTIL = "erteleme_bitisi"
    }
}
