package com.midad.studio.studio.photo.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws brush strokes, text layers and stickers onto the finished bitmap at any resolution.
 * All layers are stored in normalised base-image coordinates, so preview and export share
 * the exact same placement.
 */
object PhotoOverlayRenderer {

    fun render(
        canvas: Canvas,
        plan: GeometryPlan,
        strokes: List<DrawStroke>,
        textLayers: List<TextLayer>,
        stickers: List<StickerLayer>,
    ) {
        drawStrokes(canvas, plan, strokes)
        drawTexts(canvas, plan, textLayers)
        drawStickers(canvas, plan, stickers)
    }

    private fun drawStrokes(canvas: Canvas, plan: GeometryPlan, strokes: List<DrawStroke>) {
        if (strokes.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        if (strokes.none { it.erase }) {
            strokes.forEach { stroke -> drawStroke(canvas, plan, stroke, paint) }
            return
        }
        // An eraser needs an isolated layer; otherwise "clear" would punch holes in the photo.
        val layer = runCatching {
            Bitmap.createBitmap(plan.outputWidth, plan.outputHeight, Bitmap.Config.ARGB_8888)
        }.getOrNull()
        if (layer != null) {
            val layerCanvas = Canvas(layer)
            strokes.forEach { stroke -> drawStroke(layerCanvas, plan, stroke, paint) }
            canvas.drawBitmap(layer, 0f, 0f, null)
            layer.recycle()
        } else {
            strokes.filterNot { it.erase }.forEach { stroke -> drawStroke(canvas, plan, stroke, paint) }
        }
    }

    private fun drawStroke(canvas: Canvas, plan: GeometryPlan, stroke: DrawStroke, paint: Paint) {
        if (stroke.points.size < 2) return
        val width = (stroke.widthFraction * plan.baseWidth).coerceAtLeast(1f)
        paint.strokeWidth = width
        if (stroke.erase) {
            paint.color = 0xFF000000.toInt()
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            paint.color = stroke.color
            paint.xfermode = null
        }
        val path = Path()
        stroke.points.forEachIndexed { index, point ->
            val mapped = plan.baseNormalizedToOutputPx(point.x, point.y)
            if (index == 0) path.moveTo(mapped[0], mapped[1]) else path.lineTo(mapped[0], mapped[1])
        }
        canvas.drawPath(path, paint)
    }

    private fun drawTexts(canvas: Canvas, plan: GeometryPlan, textLayers: List<TextLayer>) {
        textLayers.forEach { layer ->
            if (layer.text.isEmpty()) return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = layer.color
                alpha = (layer.opacity.coerceIn(0f, 1f) * 255f).roundToInt()
                textSize = (layer.sizeFraction * plan.baseWidth).coerceAtLeast(4f)
                typeface = if (layer.serif) Typeface.SERIF else Typeface.SANS_SERIF
                textAlign = Paint.Align.CENTER
            }
            val lines = layer.text.split('\n')
            val metrics = paint.fontMetrics
            val lineHeight = metrics.descent - metrics.ascent
            val center = plan.baseNormalizedToOutputPx(layer.x, layer.y)
            canvas.save()
            canvas.rotate(layer.rotation + plan.rotationDegrees, center[0], center[1])
            val blockTop = center[1] - lineHeight * lines.size / 2f
            var baseline = blockTop - metrics.ascent
            lines.forEach { line ->
                canvas.drawText(line, center[0], baseline, paint)
                baseline += lineHeight
            }
            canvas.restore()
        }
    }

    private fun drawStickers(canvas: Canvas, plan: GeometryPlan, stickers: List<StickerLayer>) {
        stickers.forEach { sticker ->
            val center = plan.baseNormalizedToOutputPx(sticker.x, sticker.y)
            val size = (sticker.sizeFraction * plan.baseWidth).coerceAtLeast(2f)
            val radians = Math.toRadians((sticker.rotation + plan.rotationDegrees).toDouble())
            val cosAngle = cos(radians).toFloat()
            val sinAngle = sin(radians).toFloat()
            val alpha = (sticker.opacity.coerceIn(0f, 1f) * 255f).roundToInt()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            StickerGlyphs.paths(sticker.shape).forEach { glyph ->
                val path = Path()
                var started = false
                glyph.commands.forEach { command ->
                    when (command) {
                        is GlyphCommand.Move -> {
                            val point = mapPoint(command.x, command.y, center[0], center[1], size, cosAngle, sinAngle)
                            path.moveTo(point.x, point.y)
                            started = true
                        }
                        is GlyphCommand.Line -> {
                            if (!started) {
                                val point = mapPoint(command.x, command.y, center[0], center[1], size, cosAngle, sinAngle)
                                path.moveTo(point.x, point.y)
                                started = true
                            } else {
                                val point = mapPoint(command.x, command.y, center[0], center[1], size, cosAngle, sinAngle)
                                path.lineTo(point.x, point.y)
                            }
                        }
                        is GlyphCommand.Cubic -> {
                            val first = mapPoint(command.c1x, command.c1y, center[0], center[1], size, cosAngle, sinAngle)
                            val second = mapPoint(command.c2x, command.c2y, center[0], center[1], size, cosAngle, sinAngle)
                            val end = mapPoint(command.x, command.y, center[0], center[1], size, cosAngle, sinAngle)
                            path.cubicTo(first.x, first.y, second.x, second.y, end.x, end.y)
                        }
                        GlyphCommand.Close -> path.close()
                    }
                }
                paint.color = sticker.color
                paint.alpha = alpha
                if (glyph.filled) {
                    paint.style = Paint.Style.FILL
                } else {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = (glyph.strokeWidth * size).coerceAtLeast(1f)
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun mapPoint(
        u: Float,
        v: Float,
        centerX: Float,
        centerY: Float,
        size: Float,
        cosAngle: Float,
        sinAngle: Float,
    ): PointF {
        val localX = (u - 0.5f) * size
        val localY = (v - 0.5f) * size
        return PointF(
            centerX + localX * cosAngle - localY * sinAngle,
            centerY + localX * sinAngle + localY * cosAngle,
        )
    }
}
