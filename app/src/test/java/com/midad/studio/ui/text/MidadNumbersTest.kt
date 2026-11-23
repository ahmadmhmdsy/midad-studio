package com.midad.studio.ui.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MidadNumbersTest {

    @Test
    fun arabicUsesArabicIndicDigits() {
        assertTrue(MidadNumbers.usesArabicIndic(Locale("ar")))
        assertTrue(MidadNumbers.usesArabicIndic(Locale("ar", "IQ")))
        assertEquals("١٢٣", MidadNumbers.format(123L, Locale("ar")))
    }

    @Test
    fun maghrebKeepsWesternDigits() {
        assertFalse(MidadNumbers.usesArabicIndic(Locale("ar", "MA")))
        assertEquals("123", MidadNumbers.format(123L, Locale("ar", "MA")))
    }

    @Test
    fun englishIsUnchanged() {
        assertEquals("2026", MidadNumbers.format(2026L, Locale.US))
        assertEquals("1:07", MidadNumbers.localizeDigits("1:07", Locale.US))
    }

    @Test
    fun localizeKeepsSeparatorsAndSigns() {
        assertEquals("١:٠٧", MidadNumbers.localizeDigits("1:07", Locale("ar")))
        assertEquals("٩٩٪", MidadNumbers.localizeDigits("99٪", Locale("ar")))
    }
}
