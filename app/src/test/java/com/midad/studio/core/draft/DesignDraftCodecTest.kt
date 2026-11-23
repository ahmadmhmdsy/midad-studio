package com.midad.studio.core.draft

import com.midad.studio.studio.design.DesignScreen
import com.midad.studio.studio.design.collage.CollageAspect
import com.midad.studio.studio.design.model.CanvasPreset
import com.midad.studio.studio.design.model.DesignBackground
import com.midad.studio.studio.design.model.DesignDocument
import com.midad.studio.studio.design.model.DesignLayer
import com.midad.studio.studio.design.model.DesignPalette
import com.midad.studio.studio.design.model.GradientDirection
import com.midad.studio.studio.design.model.LayerContent
import com.midad.studio.studio.design.model.ShapeKind
import com.midad.studio.studio.design.model.TextAlignChoice
import com.midad.studio.studio.design.model.TextFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesignDraftCodecTest {

    private fun document() = DesignDocument(
        preset = CanvasPreset.STORY,
        background = DesignBackground.Gradient(
            from = DesignPalette.PAPER,
            to = DesignPalette.SAFFRON_SOFT,
            direction = GradientDirection.DIAGONAL,
        ),
        layers = listOf(
            DesignLayer(
                id = 1L,
                x = 40f,
                y = 50f,
                width = 600f,
                height = 300f,
                rotation = 12f,
                opacity = 0.8f,
                content = LayerContent.Text(
                    text = "مِداد يستحق أن يُرى",
                    size = 72f,
                    font = TextFont.SANS,
                    color = DesignPalette.INK,
                    align = TextAlignChoice.CENTER,
                    bold = true,
                ),
            ),
            DesignLayer(
                id = 2L,
                x = 10f,
                y = 20f,
                width = 300f,
                height = 300f,
                content = LayerContent.Image(uri = "content://media/picker/0/photo"),
            ),
            DesignLayer(
                id = 3L,
                x = 5f,
                y = 6f,
                width = 100f,
                height = 8f,
                content = LayerContent.Shape(
                    kind = ShapeKind.RULE,
                    color = DesignPalette.TERRACOTTA,
                    filled = false,
                    strokeWidth = 6f,
                ),
            ),
        ),
    )

    @Test
    fun fullDesignAndCollageSurviveARoundTrip() {
        val draft = DesignDraft(
            screen = DesignScreen.COLLAGE,
            document = document(),
            collage = CollageDraft(
                images = listOf("content://one", "content://two"),
                layoutId = "duo",
                aspect = CollageAspect.STORY,
                spacing = 14,
                background = DesignPalette.PAPER,
            ),
        )
        val decoded = DesignDraftCodec.decode(DesignDraftCodec.encode(draft))
        assertEquals(draft, decoded)
    }

    @Test
    fun editorDraftWithoutCollageRoundTrips() {
        val draft = DesignDraft(DesignScreen.EDITOR, document(), null)
        assertEquals(draft, DesignDraftCodec.decode(DesignDraftCodec.encode(draft)))
    }

    @Test
    fun garbageAndTruncatedBytesAreRejected() {
        assertNull(DesignDraftCodec.decode(ByteArray(0)))
        assertNull(DesignDraftCodec.decode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)))
        val encoded = DesignDraftCodec.encode(DesignDraft(DesignScreen.EDITOR, document(), null))
        assertNull(DesignDraftCodec.decode(encoded.copyOf(encoded.size / 2)))
    }

    @Test
    fun unknownFutureVersionIsRejected() {
        val encoded = DesignDraftCodec.encode(DesignDraft(DesignScreen.EDITOR, document(), null))
        encoded[4] = 0
        encoded[5] = 0
        encoded[6] = 0
        encoded[7] = 2
        assertNull(DesignDraftCodec.decode(encoded))
    }

    @Test
    fun nonFiniteGeometryAndOpacityAreClampedOnDecode() {
        val broken = document().copy(
            layers = listOf(
                DesignLayer(
                    id = 9L,
                    x = Float.NaN,
                    y = Float.NEGATIVE_INFINITY,
                    width = 10f,
                    height = 10f,
                    opacity = 2f,
                    content = LayerContent.Text(text = "x", size = 10f),
                ),
            ),
        )
        val decoded = DesignDraftCodec.decode(
            DesignDraftCodec.encode(DesignDraft(DesignScreen.EDITOR, broken, null)),
        )
        val layer = requireNotNull(decoded).document!!.layers.single()
        assertEquals(0f, layer.x, 0f)
        assertEquals(0f, layer.y, 0f)
        assertEquals(1f, layer.opacity, 0f)
    }
}
