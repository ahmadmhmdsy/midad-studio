package com.midad.studio.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.midad.studio.core.prefs.MidadPreferences
import com.midad.studio.core.prefs.ThemeMode

/** Session-wide UI state that every screen may read (theme mode today, more later). */
class MidadAppState(private val prefs: MidadPreferences) {

    var themeMode by mutableStateOf(prefs.themeMode)
        private set

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.themeMode = mode
    }
}

val LocalAppState = staticCompositionLocalOf<MidadAppState> {
    error("MidadAppState was not provided")
}
