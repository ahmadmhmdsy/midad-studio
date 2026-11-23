package com.midad.studio.core.prefs

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Small, synchronous preference store. No analytics, no account identifiers. */
class MidadPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("midad_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
        }

    var reducedMotion: Boolean
        get() = prefs.getBoolean(KEY_REDUCED_MOTION, false)
        set(value) {
            prefs.edit().putBoolean(KEY_REDUCED_MOTION, value).apply()
        }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_REDUCED_MOTION = "reduced_motion"
    }
}
