package com.midad.studio.studio.design.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignTemplatesMirrorTest {

    @Test
    fun mirrorKeepsFullWidthTextBoxInPlace() {
        val document = DesignDocument(
            preset = CanvasPreset.SQUARE,
            background = DesignBackground.Solid(DesignPalette.PAPER),
            layers = listOf(
                DesignLayer(
                    id = 1,
                    x = 64.8f,
                    y = 236f,
                    width = 1080f - 64.8f * 2f,
                    height = 200f,
                    content = LayerContent.Text(text = "مرحبا", size = 124f),
                ),
            ),
        )
        val mirrored = document.mirroredForRtl()
        assertEquals(document.layers[0].x, mirrored.layers[0].x, 0.01f)
    }

    @Test
    fun mirrorMovesRightDecorationToTheLeft() {
        val document = DesignDocument(
            preset = CanvasPreset.SQUARE,
            background = DesignBackground.Gradient(
                from = DesignPalette.SAFFRON,
                to = DesignPalette.TERRACOTTA,
                direction = GradientDirection.HORIZONTAL,
            ),
            layers = listOf(
                DesignLayer(
                    id = 1,
                    x = 812f,
                    y = 150f,
                    width = 132f,
                    height = 132f,
                    rotation = 12f,
                    content = LayerContent.Shape(kind = ShapeKind.ELLIPSE),
                ),
            ),
        )
        val mirrored = document.mirroredForRtl()
        assertEquals(136f, mirrored.layers[0].x, 0.01f)
        assertEquals(-12f, mirrored.layers[0].rotation, 0.01f)
        val gradient = mirrored.background as DesignBackground.Gradient
        assertEquals(DesignPalette.TERRACOTTA, gradient.from)
        assertEquals(DesignPalette.SAFFRON, gradient.to)
        assertTrue(mirrored.layers[0].x >= 0f)
    }
}
