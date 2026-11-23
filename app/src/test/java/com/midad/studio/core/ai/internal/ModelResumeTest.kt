package com.midad.studio.core.ai.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The Range parser guards every resumed model download. */
class ModelResumeTest {

    @Test
    fun parsesPartialContentRange() {
        assertEquals(100L to 1000L, parseContentRange("bytes 100-999/1000"))
        assertEquals(0L to 512L, parseContentRange("bytes 0-511/512"))
    }

    @Test
    fun parsesUnknownTotal() {
        assertEquals(100L to 0L, parseContentRange("bytes 100-999/*"))
    }

    @Test
    fun rejectsMalformedOrImpossibleRanges() {
        assertNull(parseContentRange(null))
        assertNull(parseContentRange(""))
        assertNull(parseContentRange("bytes */1000"))
        assertNull(parseContentRange("bytes 100-99/1000"))
        assertNull(parseContentRange("bytes 0-1000/1000"))
        assertNull(parseContentRange("items 0-10/20"))
    }

    @Test
    fun acceptsCaseInsensitiveUnit() {
        assertEquals(5L to 20L, parseContentRange("BYTES 5-9/20"))
    }
}
