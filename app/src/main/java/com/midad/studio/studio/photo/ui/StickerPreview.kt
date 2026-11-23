package com.midad.studio.studio.photo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.midad.studio.studio.photo.engine.GlyphCommand
import com.midad.studio.studio.photo.engine.StickerGlyphs
import com.midad.studio.studio.photo.engine.StickerShape

/** Draws one sticker in Compose, using the same vector description as the exporter. */
internal fun DrawScope.drawStickerGlyph(
    shape: StickerShape,
    center: Offset,
    sizePx: Float,
    rotationDegrees: Float,
    color: Color,
) {
    rotate(degrees = rotationDegrees, pivot = center) {
        StickerGlyphs.paths(shape).forEach { glyph ->
            val path = Path()
            glyph.commands.forEach { command ->
                when (command) {
                    is GlyphCommand.Move -> path.moveTo(
                        center.x + (command.x - 0.5f) * sizePx,
                        center.y + (command.y - 0.5f) * sizePx,
                    )
                    is GlyphCommand.Line -> path.lineTo(
                        center.x + (command.x - 0.5f) * sizePx,
                        center.y + (command.y - 0.5f) * sizePx,
                    )
                    is GlyphCommand.Cubic -> path.cubicTo(
                        center.x + (command.c1x - 0.5f) * sizePx,
                        center.y + (command.c1y - 0.5f) * sizePx,
                        center.x + (command.c2x - 0.5f) * sizePx,
                        center.y + (command.c2y - 0.5f) * sizePx,
                        center.x + (command.x - 0.5f) * sizePx,
                        center.y + (command.y - 0.5f) * sizePx,
                    )
                    GlyphCommand.Close -> path.close()
                }
            }
            if (glyph.filled) {
                drawPath(path = path, color = color)
            } else {
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = (glyph.strokeWidth * sizePx).coerceAtLeast(0.5f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

@Composable
fun StickerGlyphView(shape: StickerShape, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawStickerGlyph(
            shape = shape,
            center = Offset(size.width / 2f, size.height / 2f),
            sizePx = size.minDimension * 0.78f,
            rotationDegrees = 0f,
            color = color,
        )
    }
}
