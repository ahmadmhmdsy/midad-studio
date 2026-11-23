package com.midad.studio.studio.design.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Every mark in the design studio is drawn here; no icon font or third-party vector is used. */
enum class DesignGlyph {
    UNDO, REDO, TEXT, IMAGE, SHAPE, BACKGROUND, LAYERS, MARGINS, CLOSE,
    UP, DOWN, TRASH, EXPORT, SHARE, TEMPLATE, COLLAGE, PLUS,
    RECTANGLE, ELLIPSE, LINE, STAR, RULE,
}

@Composable
fun DesignGlyphMark(glyph: DesignGlyph, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) { drawGlyph(glyph, color) }
}

private fun DrawScope.drawGlyph(glyph: DesignGlyph, color: Color) {
    val width = size.width
    val height = size.height
    val side = minOf(width, height)
    val stroke = Stroke(width = side * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val thin = Stroke(width = side * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun at(x: Float, y: Float) = Offset(width * x, height * y)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, style: Stroke = stroke) {
        drawLine(color, at(x1, y1), at(x2, y2), style.width, StrokeCap.Round)
    }

    fun rect(x1: Float, y1: Float, x2: Float, y2: Float, style: Stroke = stroke) {
        drawRect(color, topLeft = at(x1, y1), size = Size(width * (x2 - x1), height * (y2 - y1)), style = style)
    }

    when (glyph) {
        DesignGlyph.UNDO -> drawUndoArrow(color, side)
        DesignGlyph.REDO -> scale(-1f, 1f, pivot = Offset(width / 2f, height / 2f)) {
            drawUndoArrow(color, side)
        }
        DesignGlyph.TEXT -> {
            line(0.24f, 0.28f, 0.76f, 0.28f)
            line(0.50f, 0.28f, 0.50f, 0.74f)
            line(0.38f, 0.74f, 0.62f, 0.74f, thin)
        }
        DesignGlyph.IMAGE -> {
            rect(0.16f, 0.22f, 0.84f, 0.78f, thin)
            drawCircle(color, radius = side * 0.055f, center = at(0.33f, 0.38f))
            val path = Path().apply {
                moveTo(width * 0.18f, height * 0.72f)
                lineTo(width * 0.42f, height * 0.48f)
                lineTo(width * 0.56f, height * 0.62f)
                lineTo(width * 0.66f, height * 0.52f)
                lineTo(width * 0.82f, height * 0.70f)
            }
            drawPath(path, color = color, style = thin)
        }
        DesignGlyph.SHAPE -> {
            rect(0.14f, 0.16f, 0.56f, 0.58f, thin)
            drawCircle(color, radius = side * 0.24f, center = at(0.66f, 0.64f), style = thin)
        }
        DesignGlyph.BACKGROUND -> {
            rect(0.16f, 0.20f, 0.84f, 0.80f, thin)
            line(0.22f, 0.56f, 0.56f, 0.22f, thin)
            line(0.22f, 0.74f, 0.74f, 0.22f, thin)
            line(0.40f, 0.74f, 0.78f, 0.40f, thin)
        }
        DesignGlyph.LAYERS -> {
            rect(0.16f, 0.20f, 0.70f, 0.42f, thin)
            rect(0.22f, 0.38f, 0.76f, 0.60f, thin)
            rect(0.28f, 0.56f, 0.82f, 0.78f, thin)
        }
        DesignGlyph.MARGINS -> {
            rect(0.14f, 0.18f, 0.86f, 0.82f, thin)
            val dashed = Stroke(
                width = side * 0.055f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(side * 0.10f, side * 0.09f)),
            )
            drawRect(
                color = color,
                topLeft = at(0.28f, 0.32f),
                size = Size(width * 0.44f, height * 0.36f),
                style = dashed,
            )
        }
        DesignGlyph.CLOSE -> {
            line(0.28f, 0.28f, 0.72f, 0.72f)
            line(0.72f, 0.28f, 0.28f, 0.72f)
        }
        DesignGlyph.UP -> {
            line(0.26f, 0.60f, 0.50f, 0.36f)
            line(0.50f, 0.36f, 0.74f, 0.60f)
        }
        DesignGlyph.DOWN -> {
            line(0.26f, 0.40f, 0.50f, 0.64f)
            line(0.50f, 0.64f, 0.74f, 0.40f)
        }
        DesignGlyph.TRASH -> {
            line(0.28f, 0.30f, 0.72f, 0.30f)
            line(0.42f, 0.30f, 0.42f, 0.22f, thin)
            line(0.42f, 0.22f, 0.58f, 0.22f, thin)
            line(0.58f, 0.22f, 0.58f, 0.30f, thin)
            rect(0.32f, 0.30f, 0.68f, 0.80f, thin)
            line(0.44f, 0.40f, 0.44f, 0.70f, thin)
            line(0.56f, 0.40f, 0.56f, 0.70f, thin)
        }
        DesignGlyph.EXPORT -> {
            line(0.20f, 0.56f, 0.20f, 0.80f, thin)
            line(0.20f, 0.80f, 0.80f, 0.80f, thin)
            line(0.80f, 0.80f, 0.80f, 0.56f, thin)
            line(0.50f, 0.68f, 0.50f, 0.24f)
            line(0.34f, 0.40f, 0.50f, 0.24f)
            line(0.66f, 0.40f, 0.50f, 0.24f)
        }
        DesignGlyph.SHARE -> {
            val radius = side * 0.10f
            val dot = Stroke(width = side * 0.065f, cap = StrokeCap.Round)
            drawCircle(color, radius, at(0.70f, 0.24f), style = dot)
            drawCircle(color, radius, at(0.30f, 0.50f), style = dot)
            drawCircle(color, radius, at(0.70f, 0.76f), style = dot)
            line(0.39f, 0.45f, 0.61f, 0.29f, thin)
            line(0.39f, 0.55f, 0.61f, 0.71f, thin)
        }
        DesignGlyph.TEMPLATE -> {
            rect(0.16f, 0.16f, 0.84f, 0.84f, thin)
            line(0.50f, 0.16f, 0.50f, 0.84f, thin)
            line(0.16f, 0.50f, 0.84f, 0.50f, thin)
        }
        DesignGlyph.COLLAGE -> {
            fun quad(x1: Float, y1: Float, x2: Float, y2: Float) {
                drawRoundRect(
                    color = color,
                    topLeft = at(x1, y1),
                    size = Size(width * (x2 - x1), height * (y2 - y1)),
                    cornerRadius = CornerRadius(side * 0.05f),
                    style = thin,
                )
            }
            quad(0.16f, 0.16f, 0.46f, 0.46f)
            quad(0.54f, 0.16f, 0.84f, 0.46f)
            quad(0.16f, 0.54f, 0.46f, 0.84f)
            quad(0.54f, 0.54f, 0.84f, 0.84f)
        }
        DesignGlyph.PLUS -> {
            line(0.50f, 0.24f, 0.50f, 0.76f)
            line(0.24f, 0.50f, 0.76f, 0.50f)
        }
        DesignGlyph.RECTANGLE -> rect(0.18f, 0.24f, 0.82f, 0.76f)
        DesignGlyph.ELLIPSE -> drawOval(
            color = color,
            topLeft = at(0.14f, 0.26f),
            size = Size(width * 0.72f, height * 0.48f),
            style = stroke,
        )
        DesignGlyph.LINE -> line(0.22f, 0.74f, 0.78f, 0.26f)
        DesignGlyph.STAR -> drawPath(starPath(width / 2f, height / 2f, side * 0.32f, 5), color)
        DesignGlyph.RULE -> line(0.18f, 0.50f, 0.82f, 0.50f, Stroke(width = side * 0.18f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawUndoArrow(color: Color, side: Float) {
    val width = size.width
    val height = size.height
    val centerX = width * 0.5f
    val centerY = height * 0.52f
    val radius = side * 0.29f
    drawArc(
        color = color,
        startAngle = 320f,
        sweepAngle = -280f,
        useCenter = false,
        topLeft = Offset(centerX - radius, centerY - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = side * 0.09f, cap = StrokeCap.Round),
    )
    val angle = 40.0 * PI / 180.0
    val tipX = centerX + radius * cos(angle).toFloat()
    val tipY = centerY + radius * sin(angle).toFloat()
    val tangentX = sin(angle).toFloat()
    val tangentY = -cos(angle).toFloat()
    val backX = -tangentX * side * 0.20f
    val backY = -tangentY * side * 0.20f
    val wingX = tangentY * side * 0.14f
    val wingY = -tangentX * side * 0.14f
    val strokeWidth = side * 0.09f
    drawLine(color, Offset(tipX, tipY), Offset(tipX + backX + wingX, tipY + backY + wingY), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(tipX, tipY), Offset(tipX + backX - wingX, tipY + backY - wingY), strokeWidth, StrokeCap.Round)
}

fun starPath(centerX: Float, centerY: Float, radius: Float, points: Int): Path {
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
