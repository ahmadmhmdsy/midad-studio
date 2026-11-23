package com.midad.studio.ui.text

import java.util.Locale

/**
 * Digits and number formatting for the bilingual UI.
 *
 * Arabic text uses Arabic-Indic digits (٠١٢٣…) almost everywhere, except the Maghreb,
 * which uses Western digits. Numbers that Kotlin formats itself - durations, percentages,
 * counts - were printed with Latin digits inside Arabic sentences, which reads as
 * machine-translated. Strings that Android formats from a resource already follow the
 * locale, so only the hand-formatted values need this helper.
 */
object MidadNumbers {

    private val ARABIC_INDIC = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /** Arabic locales whose readers expect Western digits (ISO 3166-1 alpha-2). */
    private val WESTERN_DIGIT_COUNTRIES = setOf("MA", "DZ", "TN", "LY", "MR", "EH")

    /** True when [locale] is Arabic and writes Arabic-Indic digits. */
    fun usesArabicIndic(locale: Locale = Locale.getDefault()): Boolean =
        locale.language == "ar" &&
            locale.country.uppercase(Locale.US) !in WESTERN_DIGIT_COUNTRIES

    /** [value] as an integer string in the digits of [locale]. */
    fun format(value: Long, locale: Locale = Locale.getDefault()): String =
        localizeDigits(String.format(Locale.US, "%d", value), locale)

    /** One decimal place in the digits and separator of [locale] (e.g. ٠٫٤ / 0.4). */
    fun formatDecimal(value: Float, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%.1f", value)

    /** A whole percentage, using the Arabic percent sign in Arabic-script locales. */
    fun percent(value: Int, locale: Locale = Locale.getDefault()): String =
        format(value.toLong(), locale) + if (usesArabicIndic(locale)) "٪" else "%"

    /**
     * Rewrites every ASCII digit inside an already formatted string, leaving separators,
     * colons and percent signs untouched.
     */
    fun localizeDigits(text: String, locale: Locale = Locale.getDefault()): String {
        if (!usesArabicIndic(locale)) return text
        val builder = StringBuilder(text.length)
        for (character in text) {
            builder.append(if (character in '0'..'9') ARABIC_INDIC[character - '0'] else character)
        }
        return builder.toString()
    }
}
