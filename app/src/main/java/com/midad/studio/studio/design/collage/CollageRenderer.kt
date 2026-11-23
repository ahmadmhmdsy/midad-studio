package com.midad.studio.studio.design.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.roundToInt

object CollageRenderer {

    fun render(spec: CollageSpec, bitmaps: List<Bitmap>): Bitmap {
        val width = spec.width
        val height = spec.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(spec.background.toInt())
        val rects = CollageLayouts.cellRects(
            layout = spec.layout,
            canvasWidth = width.toFloat(),
            canvasHeight = height.toFloat(),
            spacing = spec.spacingPx(),
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        rects.forEachIndexed { index, rect ->
            val bitmap = bitmaps.getOrNull(index) ?: return@forEachIndexed
            drawCover(canvas, bitmap, rect, paint)
        }
        return output
    }

    private fun drawCover(canvas: Canvas, bitmap: Bitmap, rect: RectF, paint: Paint) {
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()
        val width = rect.width()
        val height = rect.height()
        if (imageWidth <= 0f || imageHeight <= 0f || width <= 0f || height <= 0f) return
        val factor = max(width / imageWidth, height / imageHeight)
        val sourceWidth = (width / factor).roundToInt().coerceIn(1, bitmap.width)
        val sourceHeight = (height / factor).roundToInt().coerceIn(1, bitmap.height)
        val sourceLeft = (bitmap.width - sourceWidth) / 2
        val sourceTop = (bitmap.height - sourceHeight) / 2
        val source = Rect(sourceLeft, sourceTop, sourceLeft + sourceWidth, sourceTop + sourceHeight)
        canvas.drawBitmap(bitmap, source, rect, paint)
    }
}
