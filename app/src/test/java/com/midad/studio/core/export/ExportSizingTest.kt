package com.midad.studio.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportSizingTest {

    @Test
    fun pngEstimateIsLargerThanJpegForTheSamePixels() {
        val jpeg = ExportSizing.requiredImageBytes(4000, 3000, png = false)
        val png = ExportSizing.requiredImageBytes(4000, 3000, png = true)
        assertTrue(png > jpeg)
        assertTrue(jpeg >= ExportSizing.SAFETY_MARGIN_BYTES)
    }

    @Test
    fun estimateHandlesLargePixelsWithoutIntegerOverflow() {
        val bytes = ExportSizing.requiredImageBytes(20_000, 20_000, png = true)
        assertTrue(bytes > 20_000L * 20_000L)
    }

    @Test
    fun videoEstimateCoversTwiceTheSourcePlusMargin() {
        val source = 50L * 1024L * 1024L
        assertTrue(ExportSizing.requiredVideoBytes(source) > source * 2)
    }

    @Test
    fun degenerateDimensionsNeverEstimateZero() {
        assertEquals(ExportSizing.SAFETY_MARGIN_BYTES + 2L, ExportSizing.requiredImageBytes(0, 0, png = false))
    }
}
