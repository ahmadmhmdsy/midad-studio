package com.midad.studio.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormattersTest {

    @Test
    fun englishBytesUseLatinDigits() {
        assertEquals("512 B", formatBytes(512, Locale.US))
        assertEquals("1.5 KB", formatBytes(1536, Locale.US))
        assertEquals("2.0 MB", formatBytes(2L * 1024 * 1024, Locale.US))
    }

    @Test
    fun arabicBytesUseArabicIndicDigits() {
        val formatted = formatBytes(1536, Locale("ar"))
        assertEquals("١٫٥ KB", formatted)
    }

    @Test
    fun maghrebArabicKeepsWesternDigits() {
        // ar-MA uses Latin digits but the Maghrebi decimal comma, not the Gulf dot.
        assertEquals("1,5 KB", formatBytes(1536, Locale("ar", "MA")))
    }
}
