package com.sametozdemir.supfy.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Uygulama arayüzünün teması (Spotify sayfasının kendi teması değişmez). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Yalnızca üç tercih saklanır; kişisel veri tutulmaz. */
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
    }
}
