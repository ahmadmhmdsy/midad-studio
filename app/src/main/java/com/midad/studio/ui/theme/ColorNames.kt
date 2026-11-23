package com.midad.studio.ui.theme

import androidx.annotation.StringRes
import com.midad.studio.R

/**
 * The palettes are authored as plain hex values, so a swatch has no name of its own.
 * TalkBack users get a real word ("Terracotta") instead of a button with no label; values
 * that are not in the shared palette fall back to their hex code.
 */
@StringRes
fun colorNameRes(argb: Long): Int? = when (argb) {
    0xFFF4EFE5L -> R.string.color_paper
    0xFFFBF8F1L -> R.string.color_paper_high
    0xFF1C1915L -> R.string.color_ink
    0xFF3A342CL -> R.string.color_ink_soft
    0xFFC8923FL -> R.string.color_saffron
    0xFFEBD7B0L -> R.string.color_saffron_soft
    0xFFB1552FL -> R.string.color_terracotta
    0xFF66795FL -> R.string.color_sage
    0xFF344866L -> R.string.color_indigo
    0xFFE7DCC7L -> R.string.color_sand
    0xFFCDBFA8L -> R.string.color_line
    0xFF7B7264L -> R.string.color_muted
    0xFFC77B7BL -> R.string.color_rose
    0xFF7FA3B5L -> R.string.color_sky
    0xFFFFFFFFL -> R.string.color_white
    0xFF000000L -> R.string.color_black
    else -> null
}

/** Stable spoken fallback for an unnamed swatch colour. */
fun hexColor(argb: Long): String =
    "#" + (0xFFFFFFL and argb).toString(16).uppercase().padStart(6, '0')
