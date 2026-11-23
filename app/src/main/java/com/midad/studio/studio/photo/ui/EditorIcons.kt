package com.midad.studio.studio.photo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.midad.studio.studio.photo.editor.PhotoTool
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hand-drawn marks for the photo studio. Drawn in code so the app ships no icon artwork.
 */
@Composable
fun UndoGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) { drawUndoArc(color, mirrored = false) }
}

@Composable
fun RedoGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) { drawUndoArc(color, mirrored = true) }
}

@Composable
fun ExportGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        drawLine(
            color = color,
            start = Offset(w * 0.22f, h * 0.64f),
            end = Offset(w * 0.22f, h * 0.84f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.78f, h * 0.64f),
            end = Offset(w * 0.78f, h * 0.84f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.22f, h * 0.84f),
            end = Offset(w * 0.78f, h * 0.84f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.68f),
            end = Offset(w * 0.5f, h * 0.18f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.18f),
            end = Offset(w * 0.32f, h * 0.36f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.18f),
            end = Offset(w * 0.68f, h * 0.36f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun ToolGlyph(tool: PhotoTool, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = size.minDimension * 0.085f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (tool) {
            PhotoTool.ADJUST -> {
                val rows = listOf(0.24f to 0.34f, 0.5f to 0.66f, 0.76f to 0.44f)
                rows.forEach { (y, knob) ->
                    drawLine(
                        color = color,
                        start = Offset(w * 0.12f, h * y),
                        end = Offset(w * 0.88f, h * y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(color = color, radius = strokeWidth * 1.25f, center = Offset(w * knob, h * y))
                }
            }

            PhotoTool.FILTERS -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.32f,
                    style = stroke,
                )
                drawArc(
                    color = color,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
            }

            PhotoTool.CROP -> {
                val inset = 0.18f
                drawLine(color = color, start = Offset(w * inset, h * inset), end = Offset(w * (inset + 0.26f), h * inset), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * inset, h * inset), end = Offset(w * inset, h * (inset + 0.26f)), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * (1f - inset), h * (1f - inset)), end = Offset(w * (1f - inset - 0.26f), h * (1f - inset)), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * (1f - inset), h * (1f - inset)), end = Offset(w * (1f - inset), h * (1f - inset - 0.26f)), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }

            PhotoTool.TEXT -> {
                drawLine(color = color, start = Offset(w * 0.22f, h * 0.22f), end = Offset(w * 0.78f, h * 0.22f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.5f, h * 0.22f), end = Offset(w * 0.5f, h * 0.78f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.34f, h * 0.78f), end = Offset(w * 0.66f, h * 0.78f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }

            PhotoTool.STICKERS -> {
                val path = Path()
                val center = Offset(w / 2f, h / 2f)
                val outer = size.minDimension * 0.34f
                val inner = outer * 0.44f
                repeat(10) { step ->
                    val radius = if (step % 2 == 0) outer else inner
                    val angle = Math.toRadians((-90.0 + step * 36.0))
                    val x = center.x + (cos(angle) * radius).toFloat()
                    val y = center.y + (sin(angle) * radius).toFloat()
                    if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color = color, style = stroke)
            }

            PhotoTool.DRAW -> {
                drawLine(color = color, start = Offset(w * 0.28f, h * 0.72f), end = Offset(w * 0.68f, h * 0.32f), strokeWidth = strokeWidth * 1.35f, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.22f, h * 0.82f), end = Offset(w * 0.34f, h * 0.78f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.24f, h * 0.78f), end = Offset(w * 0.3f, h * 0.9f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun RotateGlyph(clockwise: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) { drawTurnArc(color, clockwise = clockwise, fullCircle = false) }
}

@Composable
fun FlipGlyph(horizontal: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = size.minDimension * 0.07f
        if (horizontal) {
            drawLine(
                color = color.copy(alpha = 0.6f),
                start = Offset(w * 0.5f, h * 0.16f),
                end = Offset(w * 0.5f, h * 0.84f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            val left = Path().apply {
                moveTo(w * 0.42f, h * 0.32f)
                lineTo(w * 0.2f, h * 0.5f)
                lineTo(w * 0.42f, h * 0.68f)
                close()
            }
            val right = Path().apply {
                moveTo(w * 0.58f, h * 0.32f)
                lineTo(w * 0.8f, h * 0.5f)
                lineTo(w * 0.58f, h * 0.68f)
                close()
            }
            drawPath(left, color = color)
            drawPath(right, color = color)
        } else {
            drawLine(
                color = color.copy(alpha = 0.6f),
                start = Offset(w * 0.16f, h * 0.5f),
                end = Offset(w * 0.84f, h * 0.5f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            val top = Path().apply {
                moveTo(w * 0.32f, h * 0.42f)
                lineTo(w * 0.5f, h * 0.2f)
                lineTo(w * 0.68f, h * 0.42f)
                close()
            }
            val bottom = Path().apply {
                moveTo(w * 0.32f, h * 0.58f)
                lineTo(w * 0.5f, h * 0.8f)
                lineTo(w * 0.68f, h * 0.58f)
                close()
            }
            drawPath(top, color = color)
            drawPath(bottom, color = color)
        }
    }
}

@Composable
fun EraserGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.085f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.24f, size.height * 0.2f),
            size = Size(size.width * 0.32f, size.height * 0.44f),
            cornerRadius = CornerRadius(size.minDimension * 0.08f),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.56f, size.height * 0.42f),
            end = Offset(size.width * 0.78f, size.height * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.3f, size.height * 0.82f),
            end = Offset(size.width * 0.78f, size.height * 0.82f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

/** Shared arc-with-arrowhead mark; [mirrored] flips it for redo. */
private fun DrawScope.drawUndoArc(color: Color, mirrored: Boolean) {
    val w = size.width
    val h = size.height
    val pivot = Offset(w / 2f, h / 2f)
    val strokeWidth = size.minDimension * 0.085f
    val arcWidth = w * 0.62f
    val arcHeight = h * 0.54f
    val topLeft = Offset((w - arcWidth) / 2f, (h - arcHeight) / 2f)
    scale(scaleX = if (mirrored) -1f else 1f, scaleY = 1f, pivot = pivot) {
        val startAngle = 40f
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(arcWidth, arcHeight),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        val radians = Math.toRadians(startAngle.toDouble())
        val rx = arcWidth / 2f
        val ry = arcHeight / 2f
        val cx = topLeft.x + rx
        val cy = topLeft.y + ry
        val point = Offset(cx + (cos(radians) * rx).toFloat(), cy + (sin(radians) * ry).toFloat())
        drawLine(color = color, start = point, end = Offset(point.x - w * 0.14f, point.y - h * 0.02f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(color = color, start = point, end = Offset(point.x - w * 0.05f, point.y + h * 0.13f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

/** Circular turn arrow used by the crop tool. */
private fun DrawScope.drawTurnArc(color: Color, clockwise: Boolean, fullCircle: Boolean) {
    val w = size.width
    val h = size.height
    val strokeWidth = size.minDimension * 0.085f
    val inset = size.minDimension * 0.2f
    val startAngle = if (clockwise) -60f else 30f
    val sweep = if (clockwise) 250f else -250f
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = Size(w - inset * 2f, h - inset * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
    val rx = (w - inset * 2f) / 2f
    val ry = (h - inset * 2f) / 2f
    val cx = w / 2f
    val cy = h / 2f
    val endAngle = startAngle + sweep
    val radians = Math.toRadians(endAngle.toDouble())
    val point = Offset(cx + (cos(radians) * rx).toFloat(), cy + (sin(radians) * ry).toFloat())
    val tangent = Math.toRadians((endAngle + if (clockwise) -90f else 90f).toDouble())
    val dirX = cos(tangent).toFloat()
    val dirY = sin(tangent).toFloat()
    val arrow = size.minDimension * 0.16f
    drawLine(color = color, start = point, end = Offset(point.x - dirX * arrow + dirY * arrow * 0.7f, point.y - dirY * arrow - dirX * arrow * 0.7f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = point, end = Offset(point.x - dirX * arrow - dirY * arrow * 0.7f, point.y - dirY * arrow + dirX * arrow * 0.7f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}
