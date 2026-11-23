package com.midad.studio.studio.design.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.midad.studio.R

/**
 * Every colour, shape and layout in the design studio is authored here as plain data.
 * The palette deliberately mirrors the paper/ink/saffron language of the app.
 */
object DesignPalette {
    const val PAPER = 0xFFF4EFE5L
    const val PAPER_HIGH = 0xFFFBF8F1L
    const val INK = 0xFF1C1915L
    const val INK_SOFT = 0xFF3A342CL
    const val SAFFRON = 0xFFC8923FL
    const val SAFFRON_SOFT = 0xFFEBD7B0L
    const val TERRACOTTA = 0xFFB1552FL
    const val SAGE = 0xFF66795FL
    const val INDIGO = 0xFF344866L
    const val SAND = 0xFFE7DCC7L
    const val LINE = 0xFFCDBFA8L
    const val MUTED = 0xFF7B7264L
    const val WHITE = 0xFFFFFFFFL
    const val BLACK = 0xFF000000L

    val swatches: List<Long> = listOf(
        PAPER, PAPER_HIGH, INK, INK_SOFT, SAFFRON, SAFFRON_SOFT,
        TERRACOTTA, SAGE, INDIGO, SAND, LINE, MUTED, WHITE, BLACK,
    )
}

/** The starting sizes offered on the design start screen. */
enum class CanvasPreset(val width: Int, val height: Int, @StringRes val labelRes: Int) {
    SQUARE(1080, 1080, R.string.d_preset_square),
    STORY(1080, 1920, R.string.d_preset_story),
    LANDSCAPE(1280, 720, R.string.d_preset_landscape),
    LINK(1200, 628, R.string.d_preset_link),
    A4(2480, 3508, R.string.d_preset_a4);

    val aspectRatio: Float get() = width.toFloat() / height.toFloat()
}

enum class GradientDirection { VERTICAL, HORIZONTAL, DIAGONAL }

enum class PatternKind { DOTS, LINES, GRID, ARCS }

/** Canvas background: one of four authored styles. */
sealed interface DesignBackground {
    data class Solid(val color: Long) : DesignBackground
    data class Gradient(
        val from: Long,
        val to: Long,
        val direction: GradientDirection = GradientDirection.VERTICAL,
    ) : DesignBackground

    data class Pattern(
        val base: Long,
        val ink: Long,
        val kind: PatternKind = PatternKind.DOTS,
    ) : DesignBackground

    data object Transparent : DesignBackground
}

fun DesignBackground.isDark(): Boolean = when (this) {
    is DesignBackground.Solid -> color.isDarkColor()
    is DesignBackground.Gradient -> from.isDarkColor() && to.isDarkColor()
    is DesignBackground.Pattern -> base.isDarkColor()
    DesignBackground.Transparent -> false
}

fun DesignBackground.readableInk(): Long = if (isDark()) DesignPalette.PAPER else DesignPalette.INK

fun DesignBackground.accentInk(): Long = if (isDark()) DesignPalette.SAFFRON_SOFT else DesignPalette.TERRACOTTA

enum class TextFont { SERIF, SANS }

enum class TextAlignChoice { START, CENTER, END }

enum class ShapeKind { RECTANGLE, ELLIPSE, LINE, STAR, RULE }

enum class ImageFit { CONTAIN, COVER, STRETCH }

/** What is painted inside a layer. The transform (position, size, rotation) lives on the layer. */
sealed interface LayerContent {
    data class Text(
        val text: String,
        val size: Float,
        val font: TextFont = TextFont.SERIF,
        val color: Long = DesignPalette.INK,
        val align: TextAlignChoice = TextAlignChoice.START,
        val bold: Boolean = false,
    ) : LayerContent

    data class Image(
        val uri: String,
        val fit: ImageFit = ImageFit.CONTAIN,
        val cornerRadius: Float = 0f,
    ) : LayerContent

    data class Shape(
        val kind: ShapeKind,
        val color: Long = DesignPalette.SAFFRON,
        val filled: Boolean = true,
        val strokeWidth: Float = 12f,
    ) : LayerContent
}

/**
 * One element of the canvas. All geometry is expressed in canvas pixels so the preview
 * and the full-resolution export share the same numbers.
 */
data class DesignLayer(
    val id: Long,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val content: LayerContent,
)

/** A canvas: preset size, background and layers ordered bottom to top. */
data class DesignDocument(
    val preset: CanvasPreset,
    val background: DesignBackground,
    val layers: List<DesignLayer> = emptyList(),
) {
    val width: Int get() = preset.width
    val height: Int get() = preset.height

    /** Text is kept this far from every edge by the templates and the margin guides. */
    val safeInset: Float get() = minOf(width, height) * SAFE_MARGIN

    fun layer(id: Long): DesignLayer? = layers.firstOrNull { it.id == id }

    companion object {
        const val SAFE_MARGIN = 0.06f
    }
}

/** What the editor is currently touching. */
sealed interface DesignSelection {
    data object None : DesignSelection
    data object Background : DesignSelection
    data class Layer(val id: Long) : DesignSelection
}

/** One of the authored starting points. Documents are immutable, so a template can be reused. */
data class DesignTemplate(
    val id: String,
    @StringRes val nameRes: Int,
    val document: DesignDocument,
)

// ---- colour helpers -------------------------------------------------------

fun designColor(argb: Long): Color = Color(argb.toInt())

/** Multiplies the alpha channel of an ARGB value. */
fun Long.withOpacity(opacity: Float): Long {
    val alpha = (((this ushr 24) and 0xFF).toFloat() * opacity.coerceIn(0f, 1f))
        .toInt().coerceIn(0, 255)
    return (this and 0x00FFFFFFL) or (alpha.toLong() shl 24)
}

fun Long.isDarkColor(): Boolean {
    val red = ((this shr 16) and 0xFF).toFloat() / 255f
    val green = ((this shr 8) and 0xFF).toFloat() / 255f
    val blue = (this and 0xFF).toFloat() / 255f
    val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return luminance < 0.5f
}
