package com.midad.studio.core.format

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A compact storage size in the digits and decimal separator of [locale]. Arabic readers see
 * Arabic-Indic digits here, matching the rest of the interface; the unit abbreviation stays
 * Latin because KB/MB/GB are understood as storage units even in Arabic UI.
 */
fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String {
    if (bytes < 1024L) return String.format(locale, "%d B", bytes)
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return String.format(locale, "%.1f %s", value, units[unitIndex])
}

fun formatShortDate(timestamp: Long, locale: Locale = Locale.getDefault()): String =
    SimpleDateFormat("d MMM yyyy", locale).format(Date(timestamp))

fun formatDateTime(timestamp: Long, locale: Locale = Locale.getDefault()): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", locale).format(Date(timestamp))
