package com.midad.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = MidadColors.SaffronInk,
    onPrimary = MidadColors.PaperElevated,
    primaryContainer = MidadColors.SaffronSoft,
    onPrimaryContainer = MidadColors.Ink,
    secondary = MidadColors.TerracottaInk,
    onSecondary = MidadColors.PaperElevated,
    secondaryContainer = MidadColors.Sand,
    onSecondaryContainer = MidadColors.Ink,
    tertiary = MidadColors.Indigo,
    onTertiary = MidadColors.PaperElevated,
    tertiaryContainer = MidadColors.Sand,
    onTertiaryContainer = MidadColors.Ink,
    background = MidadColors.Paper,
    onBackground = MidadColors.Ink,
    surface = MidadColors.PaperElevated,
    onSurface = MidadColors.Ink,
    surfaceVariant = MidadColors.Sand,
    onSurfaceVariant = MidadColors.InkSoft,
    outline = MidadColors.LineStrong,
    outlineVariant = MidadColors.Sand,
    error = MidadColors.Danger,
    onError = MidadColors.PaperElevated,
    errorContainer = MidadColors.Sand,
    onErrorContainer = MidadColors.Danger,
    scrim = MidadColors.Ink,
    surfaceDim = MidadColors.Sand,
    surfaceBright = MidadColors.PaperElevated,
    surfaceContainerLowest = MidadColors.PaperElevated,
    surfaceContainerLow = MidadColors.Paper,
    surfaceContainer = MidadColors.Sand,
    surfaceContainerHigh = MidadColors.Sand,
    surfaceContainerHighest = MidadColors.Line,
    inverseSurface = MidadColors.Ink,
    inverseOnSurface = MidadColors.PaperElevated,
    inversePrimary = MidadColors.SaffronSoft,
    surfaceTint = MidadColors.SaffronInk,
)

private val DarkColors = darkColorScheme(
    primary = MidadColors.SaffronSoft,
    onPrimary = MidadColors.NightBackground,
    primaryContainer = MidadColors.Saffron,
    onPrimaryContainer = MidadColors.NightBackground,
    secondary = MidadColors.Saffron,
    onSecondary = MidadColors.NightBackground,
    secondaryContainer = MidadColors.NightSurfaceHigh,
    onSecondaryContainer = MidadColors.NightInk,
    tertiary = MidadColors.NightMuted,
    onTertiary = MidadColors.NightBackground,
    tertiaryContainer = MidadColors.NightSurfaceHigh,
    onTertiaryContainer = MidadColors.NightInk,
    background = MidadColors.NightBackground,
    onBackground = MidadColors.NightInk,
    surface = MidadColors.NightSurface,
    onSurface = MidadColors.NightInk,
    surfaceVariant = MidadColors.NightSurfaceHigh,
    onSurfaceVariant = MidadColors.NightMuted,
    outline = MidadColors.NightLineStrong,
    outlineVariant = MidadColors.NightSurfaceHigh,
    error = MidadColors.NightDanger,
    onError = MidadColors.NightBackground,
    errorContainer = MidadColors.NightSurfaceHigh,
    onErrorContainer = MidadColors.NightDanger,
    scrim = MidadColors.NightBackground,
    surfaceDim = MidadColors.NightBackground,
    surfaceBright = MidadColors.NightSurfaceHigh,
    surfaceContainerLowest = MidadColors.NightBackground,
    surfaceContainerLow = MidadColors.NightSurface,
    surfaceContainer = MidadColors.NightSurface,
    surfaceContainerHigh = MidadColors.NightSurfaceHigh,
    surfaceContainerHighest = MidadColors.NightSurfaceHigh,
    inverseSurface = MidadColors.NightInk,
    inverseOnSurface = MidadColors.NightBackground,
    inversePrimary = MidadColors.SaffronInk,
    surfaceTint = MidadColors.Saffron,
)

val MidadShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun MidadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MidadTypography,
        shapes = MidadShapes,
        content = content,
    )
}
