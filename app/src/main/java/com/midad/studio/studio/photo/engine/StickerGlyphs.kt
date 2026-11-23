package com.midad.studio.studio.photo.engine

import kotlin.math.cos
import kotlin.math.sin

/**
 * Sticker artwork, described as thin vector paths in a 0..1 box. The same description is
 * rendered by Compose (preview) and by the Android Canvas (export), so a shape can never
 * drift between the two.
 */
internal sealed interface GlyphCommand {
    data class Move(val x: Float, val y: Float) : GlyphCommand
    data class Line(val x: Float, val y: Float) : GlyphCommand
    data class Cubic(
        val c1x: Float, val c1y: Float,
        val c2x: Float, val c2y: Float,
        val x: Float, val y: Float,
    ) : GlyphCommand
    data object Close : GlyphCommand
}

internal class GlyphPath(
    val commands: MutableList<GlyphCommand> = mutableListOf(),
    var filled: Boolean = true,
    /** Stroke width as a fraction of the sticker size; ignored for filled paths. */
    var strokeWidth: Float = 0f,
)

internal class PathBuilder {
    val commands = mutableListOf<GlyphCommand>()
    fun move(x: Float, y: Float) { commands += GlyphCommand.Move(x, y) }
    fun line(x: Float, y: Float) { commands += GlyphCommand.Line(x, y) }
    fun cubic(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) {
        commands += GlyphCommand.Cubic(c1x, c1y, c2x, c2y, x, y)
    }
    fun close() { commands += GlyphCommand.Close }
    /** Circle approximated with four cubic segments (k = 0.5523). */
    fun circle(cx: Float, cy: Float, rx: Float, ry: Float = rx) {
        val kx = rx * 0.5522848f
        val ky = ry * 0.5522848f
        move(cx + rx, cy)
        cubic(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry)
        cubic(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy)
        cubic(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry)
        cubic(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy)
        close()
    }
    fun oval(cx: Float, cy: Float, rx: Float, ry: Float) {
        val kx = rx * 0.5522848f
        val ky = ry * 0.5522848f
        move(cx + rx, cy)
        cubic(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry)
        cubic(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy)
        cubic(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry)
        cubic(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy)
        close()
    }
    fun roundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        val r = radius.coerceAtMost(minOf(right - left, bottom - top) / 2f)
        move(left + r, top)
        line(right - r, top)
        cubic(right - r + r * 0.55f, top, right, top + r - r * 0.55f, right, top + r)
        line(right, bottom - r)
        cubic(right, bottom - r + r * 0.55f, right - r + r * 0.55f, bottom, right - r, bottom)
        line(left + r, bottom)
        cubic(left + r - r * 0.55f, bottom, left, bottom - r + r * 0.55f, left, bottom - r)
        line(left, top + r)
        cubic(left, top + r - r * 0.55f, left + r - r * 0.55f, top, left + r, top)
        close()
    }
    fun strokePath(width: Float, block: PathBuilder.() -> Unit): GlyphPath {
        val builder = PathBuilder()
        builder.block()
        return GlyphPath(builder.commands, filled = false, strokeWidth = width)
    }
    fun fillPath(block: PathBuilder.() -> Unit): GlyphPath {
        val builder = PathBuilder()
        builder.block()
        return GlyphPath(builder.commands, filled = true, strokeWidth = 0f)
    }
}

internal object StickerGlyphs {

    fun paths(shape: StickerShape): List<GlyphPath> = when (shape) {
        StickerShape.ARROW -> arrow()
        StickerShape.BUBBLE -> bubble()
        StickerShape.STAR -> star()
        StickerShape.HEART -> heart()
        StickerShape.UNDERLINE -> underline()
        StickerShape.HIGHLIGHT -> highlight()
        StickerShape.QUOTE -> quote()
        StickerShape.FRAME -> frame()
        StickerShape.BURST -> burst()
    }

    private fun arrow(): List<GlyphPath> {
        val builder = PathBuilder()
        builder.move(0.08f, 0.42f)
        builder.line(0.52f, 0.42f)
        builder.line(0.52f, 0.2f)
        builder.line(0.92f, 0.5f)
        builder.line(0.52f, 0.8f)
        builder.line(0.52f, 0.58f)
        builder.line(0.08f, 0.58f)
        builder.close()
        return listOf(GlyphPath(builder.commands))
    }

    private fun bubble(): List<GlyphPath> {
        val body = PathBuilder()
        body.move(0.2f, 0.1f)
        body.line(0.8f, 0.1f)
        body.cubic(0.87f, 0.1f, 0.92f, 0.16f, 0.92f, 0.24f)
        body.line(0.92f, 0.58f)
        body.cubic(0.92f, 0.66f, 0.87f, 0.72f, 0.8f, 0.72f)
        body.line(0.5f, 0.72f)
        body.line(0.33f, 0.9f)
        body.line(0.34f, 0.72f)
        body.line(0.2f, 0.72f)
        body.cubic(0.13f, 0.72f, 0.08f, 0.66f, 0.08f, 0.58f)
        body.line(0.08f, 0.24f)
        body.cubic(0.08f, 0.16f, 0.13f, 0.1f, 0.2f, 0.1f)
        body.close()
        val dots = PathBuilder()
        val dotRadius = 0.035f
        listOf(0.33f, 0.5f, 0.67f).forEach { x ->
            dots.circle(x, 0.41f, dotRadius)
        }
        return listOf(
            GlyphPath(body.commands),
            GlyphPath(dots.commands),
        )
    }

    private fun star(): List<GlyphPath> {
        val builder = PathBuilder()
        val outer = 0.46f
        val inner = 0.19f
        var started = false
        for (step in 0 until 10) {
            val radius = if (step % 2 == 0) outer else inner
            val angle = Math.toRadians((-90.0 + step * 36.0))
            val x = 0.5f + (cos(angle) * radius).toFloat()
            val y = 0.5f + (sin(angle) * radius).toFloat()
            if (!started) {
                builder.move(x, y)
                started = true
            } else {
                builder.line(x, y)
            }
        }
        builder.close()
        return listOf(GlyphPath(builder.commands))
    }

    private fun heart(): List<GlyphPath> {
        val builder = PathBuilder()
        builder.move(0.5f, 0.88f)
        builder.cubic(0.16f, 0.64f, 0.04f, 0.38f, 0.16f, 0.22f)
        builder.cubic(0.26f, 0.08f, 0.44f, 0.12f, 0.5f, 0.28f)
        builder.cubic(0.56f, 0.12f, 0.74f, 0.08f, 0.84f, 0.22f)
        builder.cubic(0.96f, 0.38f, 0.84f, 0.64f, 0.5f, 0.88f)
        builder.close()
        return listOf(GlyphPath(builder.commands))
    }

    private fun underline(): List<GlyphPath> {
        val builder = PathBuilder()
        builder.move(0.08f, 0.58f)
        builder.cubic(0.26f, 0.44f, 0.4f, 0.68f, 0.55f, 0.56f)
        builder.cubic(0.68f, 0.46f, 0.8f, 0.62f, 0.92f, 0.52f)
        return listOf(GlyphPath(builder.commands, filled = false, strokeWidth = 0.065f))
    }

    private fun highlight(): List<GlyphPath> {
        val loop = PathBuilder()
        loop.oval(0.5f, 0.48f, 0.44f, 0.32f)
        val tail = PathBuilder()
        tail.move(0.86f, 0.7f)
        tail.cubic(0.9f, 0.78f, 0.84f, 0.84f, 0.76f, 0.84f)
        return listOf(
            GlyphPath(loop.commands, filled = false, strokeWidth = 0.055f),
            GlyphPath(tail.commands, filled = false, strokeWidth = 0.05f),
        )
    }

    private fun quote(): List<GlyphPath> {
        val first = PathBuilder()
        first.move(0.32f, 0.76f)
        first.cubic(0.18f, 0.62f, 0.2f, 0.3f, 0.38f, 0.18f)
        val second = PathBuilder()
        second.move(0.7f, 0.76f)
        second.cubic(0.56f, 0.62f, 0.58f, 0.3f, 0.76f, 0.18f)
        return listOf(
            GlyphPath(first.commands, filled = false, strokeWidth = 0.11f),
            GlyphPath(second.commands, filled = false, strokeWidth = 0.11f),
        )
    }

    private fun frame(): List<GlyphPath> {
        val paths = mutableListOf<GlyphPath>()
        val brackets = listOf(
            listOf(0.1f to 0.34f, 0.1f to 0.1f, 0.34f to 0.1f),
            listOf(0.66f to 0.1f, 0.9f to 0.1f, 0.9f to 0.34f),
            listOf(0.9f to 0.66f, 0.9f to 0.9f, 0.66f to 0.9f),
            listOf(0.34f to 0.9f, 0.1f to 0.9f, 0.1f to 0.66f),
        )
        brackets.forEach { points ->
            val builder = PathBuilder()
            points.forEachIndexed { index, point ->
                if (index == 0) builder.move(point.first, point.second)
                else builder.line(point.first, point.second)
            }
            paths += GlyphPath(builder.commands, filled = false, strokeWidth = 0.055f)
        }
        return paths
    }

    private fun burst(): List<GlyphPath> {
        val builder = PathBuilder()
        builder.move(0.5f, 0.04f)
        builder.cubic(0.56f, 0.36f, 0.64f, 0.44f, 0.96f, 0.5f)
        builder.cubic(0.64f, 0.56f, 0.56f, 0.64f, 0.5f, 0.96f)
        builder.cubic(0.44f, 0.64f, 0.36f, 0.56f, 0.04f, 0.5f)
        builder.cubic(0.36f, 0.44f, 0.44f, 0.36f, 0.5f, 0.04f)
        builder.close()
        return listOf(GlyphPath(builder.commands))
    }
}
