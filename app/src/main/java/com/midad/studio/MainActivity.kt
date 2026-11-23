package com.midad.studio

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.midad.studio.core.prefs.MidadPreferences
import com.midad.studio.core.prefs.ThemeMode
import com.midad.studio.navigation.MidadNavHost
import com.midad.studio.ui.LocalAppState
import com.midad.studio.ui.MidadAppState
import com.midad.studio.ui.theme.MidadColors
import com.midad.studio.ui.theme.MidadTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = MidadPreferences(this)
        installEdgeToEdge(resolveDarkTheme(preferences.themeMode))
        setContent {
            val appState = remember { MidadAppState(preferences) }
            val darkTheme = when (appState.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // The window theme only knows the system night mode, but Midad's theme is a
            // setting. Re-apply the system-bar icon contrast and the window background
            // whenever the app's own choice changes.
            LaunchedEffect(darkTheme) { applyWindowChrome(darkTheme) }
            CompositionLocalProvider(LocalAppState provides appState) {
                MidadTheme(darkTheme = darkTheme) {
                    MidadNavHost()
                }
            }
        }
    }

    private fun resolveDarkTheme(mode: ThemeMode): Boolean = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM ->
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Edge-to-edge with transparent bars: the app's own surface runs underneath them, so the
     * default light navigation scrim (white below a dark screen) never appears. API 29+ also
     * disables the framework contrast scrims, which would otherwise lighten the bars.
     */
    private fun installEdgeToEdge(darkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        applyWindowChrome(darkTheme)
    }

    /** System-bar icons and the window layer must follow the app theme, not just the system. */
    private fun applyWindowChrome(darkTheme: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        window.setBackgroundDrawable(
            ColorDrawable(
                if (darkTheme) MidadColors.NightBackground.toArgb() else MidadColors.Paper.toArgb(),
            ),
        )
    }
}
