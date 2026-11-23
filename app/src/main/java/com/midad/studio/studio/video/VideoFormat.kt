package com.midad.studio.studio.video

import com.midad.studio.ui.text.MidadNumbers
import java.util.Locale

/** Compact numeral-only duration label, e.g. "1:07" (Arabic digits under an Arabic locale). */
internal fun formatClipDuration(durationMs: Long): String {
    val safe = durationMs.coerceAtLeast(0L)
    val totalSeconds = safe / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return MidadNumbers.localizeDigits(String.format(Locale.US, "%d:%02d", minutes, seconds))
}
