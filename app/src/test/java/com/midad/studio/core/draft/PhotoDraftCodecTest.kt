package com.midad.studio.core.draft

import com.midad.studio.studio.photo.engine.Adjustments
import com.midad.studio.studio.photo.engine.CropAspect
import com.midad.studio.studio.photo.engine.CropRect
import com.midad.studio.studio.photo.engine.DrawStroke
import com.midad.studio.studio.photo.engine.GeometryState
import com.midad.studio.studio.photo.engine.StickerLayer
import com.midad.studio.studio.photo.engine.StickerShape
import com.midad.studio.studio.photo.engine.StrokePoint
import com.midad.studio.studio.photo.engine.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoDraftCodecTest {

    private fun fullDraft() = PhotoDraft(
        sourceUri = "content://media/picker/0/photo%20one",
        baseFromOriginal = false,
        sourcePng = true,
        adjustments = Adjustments(
            exposure = -0.75f,
            brightness = 12,
            contrast = -30,
            saturation = 44,
            warmth = 8,
            tint = -4,
            highlights = -20,
            shadows = 30,
            vignette = 25,
            grain = 10,
            sharpen = 15,
            blur = 3,
        ),
        filterIndex = 2,
        filterIntensity = 65,
        geometry = GeometryState(
            quarterTurns = 3,
            flipHorizontal = true,
            straighten = -7.5f,
            crop = CropRect(0.1f, 0.2f, 0.85f, 0.9f),
            aspect = CropAspect.PORTRAIT_9_16,
        ),
        textLayers = listOf(
            TextLayer(
                id = 7L,
                text = "مرحبًا بالعالم",
                color = 0xFFFFFFFF.toInt(),
                sizeFraction = 0.09f,
                serif = true,
                opacity = 0.8f,
                x = 0.4f,
                y = 0.6f,
                rotation = 12f,
            ),
        ),
        stickers = listOf(
            StickerLayer(
                id = 8L,
                shape = StickerShape.BURST,
                color = 0xFFC8923F.toInt(),
                sizeFraction = 0.3f,
                x = 0.2f,
                y = 0.3f,
                rotation = -5f,
                opacity = 0.7f,
            ),
        ),
        strokes = listOf(
            DrawStroke(
                points = listOf(StrokePoint(0.1f, 0.1f), StrokePoint(0.2f, 0.25f)),
                color = 0xFF1C1915.toInt(),
                widthFraction = 0.02f,
            ),
            DrawStroke(
                points = listOf(StrokePoint(0.5f, 0.5f)),
                color = 0xFFFFFFFF.toInt(),
                widthFraction = 0.04f,
                erase = true,
            ),
        ),
        nextLayerId = 9L,
    )

    @Test
    fun roundTripPreservesEveryEditingField() {
        val original = fullDraft()
        val decoded = PhotoDraftCodec.decode(PhotoDraftCodec.encode(original))
        assertNotNull(decoded)
        assertEquals(original, decoded)
    }

    @Test
    fun nullSourceUriSurvivesRoundTrip() {
        val original = fullDraft().copy(sourceUri = null, textLayers = emptyList(), strokes = emptyList())
        val decoded = PhotoDraftCodec.decode(PhotoDraftCodec.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun garbageAndTruncatedBytesAreRejected() {
        assertNull(PhotoDraftCodec.decode(ByteArray(0)))
        assertNull(PhotoDraftCodec.decode(ByteArray(64) { 0x7F }))
        val valid = PhotoDraftCodec.encode(fullDraft())
        assertNull(PhotoDraftCodec.decode(valid.copyOf(valid.size / 2)))
    }

    @Test
    fun invalidCropIsClampedInsteadOfCrashing() {
        val bytes = PhotoDraftCodec.encode(
            fullDraft().copy(geometry = GeometryState(crop = CropRect(0.9f, 0.9f, 0.1f, 0.1f))),
        )
        val decoded = PhotoDraftCodec.decode(bytes)
        assertNotNull(decoded)
        val crop = decoded!!.geometry.crop
        assertEquals(true, crop.right > crop.left)
        assertEquals(true, crop.bottom > crop.top)
    }

    @Test
    fun futureVersionIsRejected() {
        val bytes = PhotoDraftCodec.encode(fullDraft())
        // The version follows the magic int; bumping it must not be guessed at.
        bytes[7] = 99
        assertNull(PhotoDraftCodec.decode(bytes))
    }
}
