package com.midad.studio.ui.theme

import androidx.compose.ui.graphics.Color

/** The Midad palette: paper, ink and three mineral accents. */
object MidadColors {
    val Paper = Color(0xFFF4EFE5)
    val PaperElevated = Color(0xFFFBF8F1)
    val Ink = Color(0xFF1C1915)
    val InkSoft = Color(0xFF3A342C)
    val Saffron = Color(0xFFC8923F)
    val SaffronSoft = Color(0xFFEBD7B0)
    val Terracotta = Color(0xFFB1552F)
    val Sage = Color(0xFF66795F)
    val Indigo = Color(0xFF344866)
    val Sand = Color(0xFFE7DCC7)
    val Line = Color(0xFFCDBFA8)
    val Muted = Color(0xFF7B7264)
    val Danger = Color(0xFF9B3A2E)

    /**
     * Text-safe versions of the mineral accents. The light fills above are decorative and
     * only reach about 2.4:1 against Paper, so they must never carry small text or icons.
     * These reach 6.1-6.3:1 on Paper (WCAG 2.2 AA, 1.4.3).
     */
    val SaffronInk = Color(0xFF7D5312)
    val TerracottaInk = Color(0xFF8F3F1E)
    val SageInk = Color(0xFF4C5E46)
    val MutedInk = Color(0xFF6D6455)

    /** Boundary colour that reaches 3:1 against Paper (WCAG 2.2 AA, 1.4.11). */
    val LineStrong = Color(0xFF8F8069)
    val NightLineStrong = Color(0xFF857459)

    val NightBackground = Color(0xFF15120E)
    val NightSurface = Color(0xFF1F1A14)
    val NightSurfaceHigh = Color(0xFF2A241C)
    val NightInk = Color(0xFFF0E7D8)
    val NightMuted = Color(0xFFB3A897)
    val NightLine = Color(0xFF4A4032)

    /**
     * Failure colour for the dark scheme. The light Danger (#9B3A2E) reaches only ~3:1 on
     * NightBackground, so it cannot carry text there; this tone reaches 7.4:1 on the highest
     * night surface and stays clearly warmer/redder than the saffron accent.
     */
    val NightDanger = Color(0xFFE8A48C)
}
