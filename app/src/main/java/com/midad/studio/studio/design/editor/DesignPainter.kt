package com.midad.studio.studio.design.editor

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.midad.studio.studio.design.model.DesignBackground
import com.midad.studio.studio.design.model.DesignDocument
import com.midad.studio.studio.design.model.DesignLayer
import com.midad.studio.studio.design.model.GradientDirection
import com.midad.studio.studio.design.model.ImageFit
import com.midad.studio.studio.design.model.LayerContent
import com.midad.studio.studio.design.model.PatternKind
import com.midad.studio.studio.design.model.ShapeKind
import com.midad.studio.studio.design.model.designColor
import com.midad.studio.studio.design.ui.starPath
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Paints a document in preview space. The same scale factor is used for hit testing, so what a
 * finger selects is exactly what the eye sees; export draws the same numbers at scale 1.
 */
fun DrawScope.drawDesignDocument(
    document: DesignDocument,
    scale: Float,
    textLayouts: Map<Long, TextLayoutResult>,
    images: Map<Long, ImageBitmap>,
    selectedId: Long?,
    showGuides: Boolean,
    guideColor: Color,
    selectionColor: Color,
    checkerLight: Color,
    checkerDark: Color,
) {
    val canvasSize = Size(document.width * scale, document.height * scale)
    drawDesignBackground(document.background, canvasSize, checkerLight, checkerDark)
    document.layers.forEach { layer ->
        drawDesignLayer(layer, scale, textLayouts[layer.id], images[layer.id])
    }
    if (showGuides) {
        val inset = document.safeInset * scale
        val dash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
        drawRect(
            color = guideColor,
            topLeft = Offset(inset, inset),
            size = Size(canvasSize.width - inset * 2f, canvasSize.height - inset * 2f),
            style = Stroke(width = 1.2.dp.toPx(), pathEffect = dash),
        )
    }
    if (selectedId != null) {
        val layer = document.layer(selectedId)
        if (layer != null) {
            val layerHeight = designLayerHeight(layer, textLayouts, scale)
            val left = layer.x * scale
            val top = layer.y * scale
            val layerWidth = layer.width * scale
            val center = Offset(left + layerWidth / 2f, top + layerHeight / 2f)
            rotate(layer.rotation, center) {
                drawRect(
                    color = selectionColor,
                    topLeft = Offset(left, top),
                    size = Size(layerWidth, layerHeight),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                val handle = 3.5.dp.toPx()
                drawCircle(selectionColor, handle, Offset(left, top))
                drawCircle(selectionColor, handle, Offset(left + layerWidth, top))
                drawCircle(selectionColor, handle, Offset(left, top + layerHeight))
                drawCircle(selectionColor, handle, Offset(left + layerWidth, top + layerHeight))
            }
        }
    }
}

/** Height in preview pixels: text takes the height of its wrapped layout, other layers their box. */
fun designLayerHeight(
    layer: DesignLayer,
    textLayouts: Map<Long, TextLayoutResult>,
    scale: Float,
): Float {
    return if (layer.content is LayerContent.Text) {
        val layout = textLayouts[layer.id]
        if (layout != null) layout.size.height.toFloat() else layer.height * scale
    } else {
        layer.height * scale
    }
}

private fun DrawScope.drawDesignBackground(
    background: DesignBackground,
    canvasSize: Size,
    checkerLight: Color,
    checkerDark: Color,
) {
    when (background) {
        is DesignBackground.Solid -> drawRect(designColor(background.color), size = canvasSize)
        is DesignBackground.Gradient -> {
            val from = designColor(background.from)
            val to = designColor(background.to)
            val end = when (background.direction) {
                GradientDirection.VERTICAL -> Offset(0f, canvasSize.height)
                GradientDirection.HORIZONTAL -> Offset(canvasSize.width, 0f)
                GradientDirection.DIAGONAL -> Offset(canvasSize.width, canvasSize.height)
            }
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(from, to),
                    start = Offset.Zero,
                    end = end,
                ),
                size = canvasSize,
            )
        }
        is DesignBackground.Pattern -> {
            drawRect(designColor(background.base), size = canvasSize)
            drawDesignPattern(background.kind, designColor(background.ink), canvasSize)
        }
        DesignBackground.Transparent -> drawCheckerboard(canvasSize, checkerLight, checkerDark)
    }
}

private fun DrawScope.drawDesignPattern(kind: PatternKind, color: Color, canvasSize: Size) {
    val step = min(canvasSize.width, canvasSize.height) * 0.05f
    if (step < 1f) return
    when (kind) {
        PatternKind.DOTS -> {
            var y = step / 2f
            while (y < canvasSize.height) {
                var x = step / 2f
                while (x < canvasSize.width) {
                    drawCircle(color, radius = step * 0.11f, center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        PatternKind.LINES -> {
            var offset = -canvasSize.height
            while (offset < canvasSize.width) {
                drawLine(
                    color = color,
                    start = Offset(offset, canvasSize.height),
                    end = Offset(offset + canvasSize.height, 0f),
                    strokeWidth = step * 0.08f,
                )
                offset += step * 1.4f
            }
        }
        PatternKind.GRID -> {
            var x = step
            while (x < canvasSize.width) {
                drawLine(color, Offset(x, 0f), Offset(x, canvasSize.height), step * 0.06f)
                x += step
            }
            var y = step
            while (y < canvasSize.height) {
                drawLine(color, Offset(0f, y), Offset(canvasSize.width, y), step * 0.06f)
                y += step
            }
        }
        PatternKind.ARCS -> {
            var radius = step * 2f
            val limit = canvasSize.width + canvasSize.height
            while (radius < limit) {
                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(canvasSize.width, 0f),
                    style = Stroke(width = step * 0.07f),
                )
                radius += step * 2f
            }
        }
    }
}

private fun DrawScope.drawCheckerboard(canvasSize: Size, light: Color, dark: Color) {
    val cell = 14.dp.toPx()
    drawRect(light, size = canvasSize)
    var row = 0
    var y = 0f
    while (y < canvasSize.height) {
        var x = if (row % 2 == 0) 0f else cell
        while (x < canvasSize.width) {
            drawRect(dark, topLeft = Offset(x, y), size = Size(cell, cell))
            x += cell * 2f
        }
        y += cell
        row++
    }
}

private fun DrawScope.drawDesignLayer(
    layer: DesignLayer,
    scale: Float,
    textLayout: TextLayoutResult?,
    image: ImageBitmap?,
) {
    if (layer.opacity <= 0.001f) return
    val content = layer.content
    val left = layer.x * scale
    val top = layer.y * scale
    val width = layer.width * scale
    val height = designLayerHeight(layer, if (textLayout == null) emptyMap() else mapOf(layer.id to textLayout), scale)
    if (height <= 0.001f) return
    val center = Offset(left + width / 2f, top + height / 2f)
    rotate(layer.rotation, center) {
        when (content) {
            is LayerContent.Text -> if (textLayout != null) {
                drawText(textLayout, topLeft = Offset(left, top))
            }
            is LayerContent.Shape -> drawDesignShape(content, left, top, width, height, scale, layer.opacity)
            is LayerContent.Image -> if (image != null) {
                drawDesignImage(content, image, left, top, width, height, scale, layer.opacity)
            }
        }
    }
}

private fun DrawScope.drawDesignShape(
    content: LayerContent.Shape,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    opacity: Float,
) {
    val baseColor = designColor(content.color)
    val color = baseColor.copy(alpha = baseColor.alpha * opacity.coerceIn(0f, 1f))
    val strokeWidth = content.strokeWidth * scale
    val layerSize = Size(width, height)
    when (content.kind) {
        ShapeKind.RECTANGLE -> if (content.filled) {
            drawRect(color, Offset(left, top), layerSize)
        } else {
            drawRect(color, Offset(left, top), layerSize, style = Stroke(width = strokeWidth))
        }
        ShapeKind.ELLIPSE -> if (content.filled) {
            drawOval(color, Offset(left, top), layerSize)
        } else {
            drawOval(color, Offset(left, top), layerSize, style = Stroke(width = strokeWidth))
        }
        ShapeKind.LINE -> drawLine(
            color = color,
            start = Offset(left, top + height / 2f),
            end = Offset(left + width, top + height / 2f),
            strokeWidth = if (content.filled) height else strokeWidth,
            cap = StrokeCap.Round,
        )
        ShapeKind.STAR -> {
            val path = starPath(left + width / 2f, top + height / 2f, min(width, height) / 2f, 5)
            if (content.filled) {
                drawPath(path, color)
            } else {
                drawPath(path, color, style = Stroke(width = strokeWidth))
            }
        }
        ShapeKind.RULE -> drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = layerSize,
            cornerRadius = CornerRadius(min(width, height) / 2f),
        )
    }
}

private fun DrawScope.drawDesignImage(
    content: LayerContent.Image,
    image: ImageBitmap,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    scale: Float,
    opacity: Float,
) {
    if (width <= 0f || height <= 0f) return
    val imageWidth = image.width.toFloat()
    val imageHeight = image.height.toFloat()
    if (imageWidth <= 0f || imageHeight <= 0f) return
    val alpha = opacity.coerceIn(0f, 1f)
    val radius = (content.cornerRadius * scale).coerceAtLeast(0f)
    val bounds = Rect(left, top, left + width, top + height)
    val clip = Path().apply {
        addRoundRect(RoundRect(bounds, CornerRadius(radius, radius)))
    }
    clipPath(clip) {
        when (content.fit) {
            ImageFit.STRETCH -> drawImage(
                image = image,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(
                    width.roundToInt().coerceAtLeast(1),
                    height.roundToInt().coerceAtLeast(1),
                ),
                alpha = alpha,
            )
            ImageFit.CONTAIN -> {
                val factor = min(width / imageWidth, height / imageHeight)
                val dstWidth = (imageWidth * factor).roundToInt().coerceAtLeast(1)
                val dstHeight = (imageHeight * factor).roundToInt().coerceAtLeast(1)
                val dstLeft = (left + (width - dstWidth) / 2f).roundToInt()
                val dstTop = (top + (height - dstHeight) / 2f).roundToInt()
                drawImage(
                    image = image,
                    dstOffset = IntOffset(dstLeft, dstTop),
                    dstSize = IntSize(dstWidth, dstHeight),
                    alpha = alpha,
                )
            }
            ImageFit.COVER -> {
                val factor = max(width / imageWidth, height / imageHeight)
                val srcWidth = (width / factor).roundToInt().coerceIn(1, image.width)
                val srcHeight = (height / factor).roundToInt().coerceIn(1, image.height)
                drawImage(
                    image = image,
                    srcOffset = IntOffset((image.width - srcWidth) / 2, (image.height - srcHeight) / 2),
                    srcSize = IntSize(srcWidth, srcHeight),
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(
                        width.roundToInt().coerceAtLeast(1),
                        height.roundToInt().coerceAtLeast(1),
                    ),
                    alpha = alpha,
                )
            }
        }
    }
}
