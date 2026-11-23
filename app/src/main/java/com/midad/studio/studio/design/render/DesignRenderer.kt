package com.midad.studio.studio.design.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.midad.studio.studio.design.editor.isRtlDesignText
import com.midad.studio.studio.design.model.DesignBackground
import com.midad.studio.studio.design.model.DesignDocument
import com.midad.studio.studio.design.model.DesignLayer
import com.midad.studio.studio.design.model.GradientDirection
import com.midad.studio.studio.design.model.ImageFit
import com.midad.studio.studio.design.model.LayerContent
import com.midad.studio.studio.design.model.PatternKind
import com.midad.studio.studio.design.model.ShapeKind
import com.midad.studio.studio.design.model.TextAlignChoice
import com.midad.studio.studio.design.model.TextFont
import com.midad.studio.studio.design.model.withOpacity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders a document at its real pixel size for export. It follows the same geometry as the
 * preview painter, so the saved file matches what the stage shows.
 */
object DesignRenderer {

    fun renderDocument(document: DesignDocument, images: Map<Long, Bitmap>): Bitmap {
        val output = Bitmap.createBitmap(document.width, document.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawBackground(canvas, paint, document)
        document.layers.forEach { layer ->
            drawLayer(canvas, paint, layer, images[layer.id])
        }
        return output
    }

    private fun drawBackground(canvas: Canvas, paint: Paint, document: DesignDocument) {
        val width = document.width.toFloat()
        val height = document.height.toFloat()
        when (val background = document.background) {
            is DesignBackground.Solid -> {
                paint.style = Paint.Style.FILL
                paint.shader = null
                paint.color = background.color.toInt()
                canvas.drawRect(0f, 0f, width, height, paint)
            }
            is DesignBackground.Gradient -> {
                val endX = when (background.direction) {
                    GradientDirection.VERTICAL -> 0f
                    GradientDirection.HORIZONTAL -> width
                    GradientDirection.DIAGONAL -> width
                }
                val endY = when (background.direction) {
                    GradientDirection.VERTICAL -> height
                    GradientDirection.HORIZONTAL -> 0f
                    GradientDirection.DIAGONAL -> height
                }
                paint.style = Paint.Style.FILL
                paint.shader = LinearGradient(
                    0f,
                    0f,
                    endX,
                    endY,
                    background.from.toInt(),
                    background.to.toInt(),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, width, height, paint)
                paint.shader = null
            }
            is DesignBackground.Pattern -> {
                paint.style = Paint.Style.FILL
                paint.shader = null
                paint.color = background.base.toInt()
                canvas.drawRect(0f, 0f, width, height, paint)
                drawPattern(canvas, paint, background.kind, background.ink.toInt(), width, height)
            }
            DesignBackground.Transparent -> Unit
        }
    }

    private fun drawPattern(
        canvas: Canvas,
        paint: Paint,
        kind: PatternKind,
        color: Int,
        width: Float,
        height: Float,
    ) {
        val step = min(width, height) * 0.05f
        if (step < 1f) return
        paint.color = color
        paint.strokeCap = Paint.Cap.ROUND
        when (kind) {
            PatternKind.DOTS -> {
                paint.style = Paint.Style.FILL
                var y = step / 2f
                while (y < height) {
                    var x = step / 2f
                    while (x < width) {
                        canvas.drawCircle(x, y, step * 0.11f, paint)
                        x += step
                    }
                    y += step
                }
            }
            PatternKind.LINES -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = step * 0.08f
                var offset = -height
                while (offset < width) {
                    canvas.drawLine(offset, height, offset + height, 0f, paint)
                    offset += step * 1.4f
                }
            }
            PatternKind.GRID -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = step * 0.06f
                var x = step
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height, paint)
                    x += step
                }
                var y = step
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, paint)
                    y += step
                }
            }
            PatternKind.ARCS -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = step * 0.07f
                var radius = step * 2f
                val limit = width + height
                while (radius < limit) {
                    canvas.drawCircle(width, 0f, radius, paint)
                    radius += step * 2f
                }
            }
        }
    }

    private fun drawLayer(canvas: Canvas, paint: Paint, layer: DesignLayer, bitmap: Bitmap?) {
        if (layer.opacity <= 0.001f) return
        when (val content = layer.content) {
            is LayerContent.Text -> drawTextLayer(canvas, layer, content)
            is LayerContent.Shape -> drawShapeLayer(canvas, paint, layer, content)
            is LayerContent.Image -> if (bitmap != null) {
                drawImageLayer(canvas, layer, content, bitmap)
            }
        }
    }

    private fun drawTextLayer(canvas: Canvas, layer: DesignLayer, content: LayerContent.Text) {
        if (content.text.isEmpty()) return
        val width = layer.width.roundToInt().coerceAtLeast(1)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = content.color.withOpacity(layer.opacity).toInt()
        textPaint.textSize = content.size
        textPaint.typeface = Typeface.create(
            if (content.font == TextFont.SERIF) Typeface.SERIF else Typeface.SANS_SERIF,
            if (content.bold) Typeface.BOLD else Typeface.NORMAL,
        )
        val alignment = when (content.align) {
            TextAlignChoice.START -> Layout.Alignment.ALIGN_NORMAL
            TextAlignChoice.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignChoice.END -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val layout = StaticLayout.Builder
            .obtain(content.text, 0, content.text.length, textPaint, width)
            .setAlignment(alignment)
            .setTextDirection(
                if (isRtlDesignText(content.text)) {
                    TextDirectionHeuristics.RTL
                } else {
                    TextDirectionHeuristics.LTR
                },
            )
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.rotate(layer.rotation, layer.x + layer.width / 2f, layer.y + layout.height / 2f)
        canvas.translate(layer.x, layer.y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawShapeLayer(
        canvas: Canvas,
        paint: Paint,
        layer: DesignLayer,
        content: LayerContent.Shape,
    ) {
        val left = layer.x
        val top = layer.y
        val width = layer.width
        val height = layer.height
        if (width <= 0f || height <= 0f) return
        paint.shader = null
        paint.color = content.color.withOpacity(layer.opacity).toInt()
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = content.strokeWidth
        paint.style = if (content.filled) Paint.Style.FILL else Paint.Style.STROKE
        canvas.save()
        canvas.rotate(layer.rotation, left + width / 2f, top + height / 2f)
        when (content.kind) {
            ShapeKind.RECTANGLE -> canvas.drawRect(left, top, left + width, top + height, paint)
            ShapeKind.ELLIPSE -> canvas.drawOval(left, top, left + width, top + height, paint)
            ShapeKind.LINE -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (content.filled) height else content.strokeWidth
                canvas.drawLine(left, top + height / 2f, left + width, top + height / 2f, paint)
            }
            ShapeKind.STAR -> {
                val path = starPath(
                    centerX = left + width / 2f,
                    centerY = top + height / 2f,
                    radius = min(width, height) / 2f,
                    points = 5,
                )
                canvas.drawPath(path, paint)
            }
            ShapeKind.RULE -> {
                paint.style = Paint.Style.FILL
                val radius = min(width, height) / 2f
                canvas.drawRoundRect(left, top, left + width, top + height, radius, radius, paint)
            }
        }
        canvas.restore()
    }

    private fun drawImageLayer(
        canvas: Canvas,
        layer: DesignLayer,
        content: LayerContent.Image,
        bitmap: Bitmap,
    ) {
        val left = layer.x
        val top = layer.y
        val width = layer.width
        val height = layer.height
        if (width <= 0f || height <= 0f) return
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()
        if (imageWidth <= 0f || imageHeight <= 0f) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255f).roundToInt()
        val source = Rect()
        val destination = RectF()
        when (content.fit) {
            ImageFit.STRETCH -> {
                source.set(0, 0, bitmap.width, bitmap.height)
                destination.set(left, top, left + width, top + height)
            }
            ImageFit.CONTAIN -> {
                val factor = min(width / imageWidth, height / imageHeight)
                val drawWidth = imageWidth * factor
                val drawHeight = imageHeight * factor
                val offsetX = left + (width - drawWidth) / 2f
                val offsetY = top + (height - drawHeight) / 2f
                source.set(0, 0, bitmap.width, bitmap.height)
                destination.set(offsetX, offsetY, offsetX + drawWidth, offsetY + drawHeight)
            }
            ImageFit.COVER -> {
                val factor = max(width / imageWidth, height / imageHeight)
                val sourceWidth = (width / factor).roundToInt().coerceIn(1, bitmap.width)
                val sourceHeight = (height / factor).roundToInt().coerceIn(1, bitmap.height)
                val sourceLeft = (bitmap.width - sourceWidth) / 2
                val sourceTop = (bitmap.height - sourceHeight) / 2
                source.set(sourceLeft, sourceTop, sourceLeft + sourceWidth, sourceTop + sourceHeight)
                destination.set(left, top, left + width, top + height)
            }
        }
        canvas.save()
        canvas.rotate(layer.rotation, left + width / 2f, top + height / 2f)
        val radius = content.cornerRadius.coerceAtLeast(0f)
        if (radius > 0.01f) {
            val clip = Path()
            clip.addRoundRect(
                RectF(left, top, left + width, top + height),
                radius,
                radius,
                Path.Direction.CW,
            )
            canvas.clipPath(clip)
        }
        canvas.drawBitmap(bitmap, source, destination, paint)
        canvas.restore()
    }

    private fun starPath(centerX: Float, centerY: Float, radius: Float, points: Int): Path {
        val path = Path()
        val innerRadius = radius * 0.44f
        val step = PI / points
        val start = -PI / 2.0
        for (index in 0 until points * 2) {
            val currentRadius = if (index % 2 == 0) radius else innerRadius
            val angle = start + index * step
            val x = centerX + (currentRadius * cos(angle)).toFloat()
            val y = centerY + (currentRadius * sin(angle)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }
}
