package com.sametozdemir.tini.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Yalnızca iki tercih saklanır; kişisel veri tutulmaz. */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _backgroundPlayback = MutableStateFlow(prefs.getBoolean(KEY_BACKGROUND, true))
    val backgroundPlayback: StateFlow<Boolean> = _backgroundPlayback.asStateFlow()

    private val _desktopMode = MutableStateFlow(prefs.getBoolean(KEY_DESKTOP, false))
    val desktopMode: StateFlow<Boolean> = _desktopMode.asStateFlow()

    fun setBackgroundPlayback(value: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND, value).apply()
        _backgroundPlayback.value = value
    }

    fun setDesktopMode(value: Boolean) {
        prefs.edit().putBoolean(KEY_DESKTOP, value).apply()
        _desktopMode.value = value
    }

    private companion object {
        const val FILE = "tini_ayarlar"
        const val KEY_BACKGROUND = "arka_plan_oynatma"
        const val KEY_DESKTOP = "masaustu_modu"
    }
}
