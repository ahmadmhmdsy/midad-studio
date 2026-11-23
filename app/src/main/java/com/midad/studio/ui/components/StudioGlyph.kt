package com.midad.studio.ui.components

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
import androidx.compose.ui.graphics.drawscope.Stroke
import com.midad.studio.navigation.StudioKind
import kotlin.math.cos
import kotlin.math.sin

/** Hand-drawn canvas marks for the four studios. No icon font, no third-party art. */
@Composable
fun StudioGlyph(kind: StudioKind, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val side = minOf(size.width, size.height)
        val stroke = Stroke(
            width = side * 0.075f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val center = Offset(size.width / 2f, size.height / 2f)

        when (kind) {
            StudioKind.PHOTO -> {
                drawCircle(color = color, radius = side * 0.30f, center = center, style = stroke)
                repeat(3) { index ->
                    val angle = Math.toRadians((index * 60 + 15).toDouble())
                    val dx = (cos(angle) * side * 0.30f).toFloat()
                    val dy = (sin(angle) * side * 0.30f).toFloat()
                    drawLine(
                        color = color,
                        start = Offset(center.x - dx, center.y - dy),
                        end = Offset(center.x + dx, center.y + dy),
                        strokeWidth = side * 0.055f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            StudioKind.VIDEO -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.28f),
                    size = Size(size.width * 0.68f, size.height * 0.44f),
                    cornerRadius = CornerRadius(side * 0.08f),
                    style = stroke,
                )
                val play = Path().apply {
                    moveTo(size.width * 0.44f, size.height * 0.40f)
                    lineTo(size.width * 0.66f, size.height * 0.50f)
                    lineTo(size.width * 0.44f, size.height * 0.60f)
                    close()
                }
                drawPath(play, color = color)
            }

            StudioKind.DESIGN -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.18f),
                    size = Size(size.width * 0.54f, size.height * 0.54f),
                    cornerRadius = CornerRadius(side * 0.07f),
                    style = stroke,
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.30f, size.height * 0.30f),
                    size = Size(size.width * 0.54f, size.height * 0.54f),
                    cornerRadius = CornerRadius(side * 0.07f),
                    style = stroke,
                )
                drawCircle(
                    color = color,
                    radius = side * 0.07f,
                    center = Offset(size.width * 0.74f, size.height * 0.26f),
                )
            }

            StudioKind.AI -> {
                val sparkle = Path().apply {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    moveTo(cx, cy - side * 0.30f)
                    cubicTo(cx + side * 0.05f, cy - side * 0.08f, cx + side * 0.08f, cy - side * 0.05f, cx + side * 0.30f, cy)
                    cubicTo(cx + side * 0.08f, cy + side * 0.05f, cx + side * 0.05f, cy + side * 0.08f, cx, cy + side * 0.30f)
                    cubicTo(cx - side * 0.05f, cy + side * 0.08f, cx - side * 0.08f, cy + side * 0.05f, cx - side * 0.30f, cy)
                    cubicTo(cx - side * 0.08f, cy - side * 0.05f, cx - side * 0.05f, cy - side * 0.08f, cx, cy - side * 0.30f)
                    close()
                }
                drawPath(sparkle, color = color)
                drawCircle(
                    color = color,
                    radius = side * 0.055f,
                    center = Offset(size.width * 0.78f, size.height * 0.76f),
                )
            }
        }
    }
}
