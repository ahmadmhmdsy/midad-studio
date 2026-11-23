package com.midad.studio.studio.photo.engine

/** A point in normalised (0..1) base-image coordinates. */
data class StrokePoint(val x: Float, val y: Float)

/** The sticker shapes drawn by Midad itself. No image assets are used. */
enum class StickerShape {
    ARROW,
    BUBBLE,
    STAR,
    HEART,
    UNDERLINE,
    HIGHLIGHT,
    QUOTE,
    FRAME,
    BURST,
}

data class TextLayer(
    val id: Long,
    val text: String,
    val color: Int,
    /** Glyph size as a fraction of the base image width. */
    val sizeFraction: Float,
    val serif: Boolean,
    val opacity: Float,
    /** Centre of the layer in normalised base-image coordinates. */
    val x: Float,
    val y: Float,
    val rotation: Float = 0f,
)

data class StickerLayer(
    val id: Long,
    val shape: StickerShape,
    val color: Int,
    /** Bounding box size as a fraction of the base image width. */
    val sizeFraction: Float,
    val x: Float,
    val y: Float,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
)

/** One freehand stroke. Eraser strokes are replayed with a clear blend mode. */
data class DrawStroke(
    val points: List<StrokePoint>,
    val color: Int,
    /** Brush width as a fraction of the base image width. */
    val widthFraction: Float,
    val erase: Boolean = false,
)
