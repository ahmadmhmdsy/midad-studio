package com.midad.studio.core.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the decode cap. The first implementation divided only while half
 * the edge still fit, so a 4032 px photo decoded at full size for a 2048 px request and
 * allocated 48.8 MB instead of 12.2 MB.
 */
class BitmapToolsTest {

    @Test
    fun twelveMegapixelPhotoIsCappedAt2048() {
        assertEquals(2, BitmapTools.sampleSize(4032, 3024, 2048))
        assertEquals(2016, BitmapTools.sampledEdge(4032, 3024, 2048))
    }

    @Test
    fun fortyEightMegapixelPhotoIsCappedAt2048() {
        val sample = BitmapTools.sampleSize(8000, 6000, 2048)
        assertEquals(4, sample)
        assertTrue(BitmapTools.sampledEdge(8000, 6000, 2048) <= 2048)
    }

    @Test
    fun smallImageIsNotDownsampled() {
        assertEquals(1, BitmapTools.sampleSize(1600, 1200, 2048))
        assertEquals(1, BitmapTools.sampleSize(2048, 2048, 2048))
    }

    @Test
    fun portraitAndLandscapeUseTheLongEdge() {
        assertEquals(2, BitmapTools.sampleSize(3024, 4032, 2048))
        assertEquals(4, BitmapTools.sampleSize(6000, 8000, 2048))
    }

    @Test
    fun invalidDimensionsAndLimitsAreSafe() {
        assertEquals(1, BitmapTools.sampleSize(0, 1000, 2048))
        assertEquals(1, BitmapTools.sampleSize(1000, 1000, 0))
        assertEquals(1, BitmapTools.sampleSize(-5, -5, 2048))
    }

    @Test
    fun decodeNeverExceedsTheRequestedEdge() {
        val sizes = listOf(4032 to 3024, 4000 to 3000, 6001 to 4001, 8000 to 6000, 12000 to 9000)
        val limits = listOf(1024, 2048, 3200)
        for ((width, height) in sizes) {
            for (limit in limits) {
                val edge = BitmapTools.sampledEdge(width, height, limit)
                assertTrue("edge $edge exceeded limit $limit for ${width}x${height}", edge <= limit)
            }
        }
    }
}
