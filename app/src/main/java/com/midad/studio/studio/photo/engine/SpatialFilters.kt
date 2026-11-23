package com.midad.studio.studio.photo.engine

import android.graphics.Bitmap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Separable box blur, processed in bands so a full-resolution export never needs a second
 * full-size pixel array. Edges are clamped (repeat) to avoid dark borders.
 */
internal object BlurEngine {

    suspend fun blurInPlace(bitmap: Bitmap, radius: Int) {
        if (radius < 1) return
        val width = bitmap.width
        val height = bitmap.height
        if (width < 2 || height < 2) return
        val temporary = runCatching {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }.getOrNull() ?: return
        try {
            horizontalPass(bitmap, temporary, radius)
            verticalPass(temporary, bitmap, radius)
        } finally {
            temporary.recycle()
        }
    }

    private suspend fun horizontalPass(source: Bitmap, target: Bitmap, radius: Int) {
        val width = source.width
        val height = source.height
        val bandHeight = 32
        val input = IntArray(width * bandHeight)
        val output = IntArray(width * bandHeight)
        val prefix = IntArray(width + 1)
        var top = 0
        while (top < height) {
            currentCoroutineContext().ensureActive()
            val rows = minOf(bandHeight, height - top)
            source.getPixels(input, 0, width, 0, top, width, rows)
            blurRows(input, output, prefix, width, rows, radius)
            target.setPixels(output, 0, width, 0, top, width, rows)
            top += rows
        }
    }

    private fun blurRows(
        input: IntArray,
        output: IntArray,
        prefix: IntArray,
        width: Int,
        rows: Int,
        radius: Int,
    ) {
        output.fill(0)
        for (channel in 0 until 4) {
            val shift = 24 - channel * 8
            for (row in 0 until rows) {
                val rowOffset = row * width
                prefix[0] = 0
                for (x in 0 until width) {
                    prefix[x + 1] = prefix[x] + ((input[rowOffset + x] shr shift) and 0xFF)
                }
                for (x in 0 until width) {
                    val left = (x - radius).coerceAtLeast(0)
                    val right = (x + radius).coerceAtMost(width - 1)
                    val sum = prefix[right + 1] - prefix[left]
                    val value = (sum / (right - left + 1)).coerceIn(0, 255)
                    output[rowOffset + x] = output[rowOffset + x] or (value shl shift)
                }
            }
        }
    }

    private suspend fun verticalPass(source: Bitmap, target: Bitmap, radius: Int) {
        val width = source.width
        val height = source.height
        val stripWidth = 96
        var left = 0
        while (left < width) {
            currentCoroutineContext().ensureActive()
            val columns = minOf(stripWidth, width - left)
            val input = IntArray(columns * height)
            val output = IntArray(columns * height)
            val prefix = IntArray(height + 1)
            source.getPixels(input, 0, columns, left, 0, columns, height)
            blurColumns(input, output, prefix, columns, height, radius)
            target.setPixels(output, 0, columns, left, 0, columns, height)
            left += columns
        }
    }

    private fun blurColumns(
        input: IntArray,
        output: IntArray,
        prefix: IntArray,
        columns: Int,
        height: Int,
        radius: Int,
    ) {
        output.fill(0)
        for (channel in 0 until 4) {
            val shift = 24 - channel * 8
            for (column in 0 until columns) {
                prefix[0] = 0
                for (y in 0 until height) {
                    prefix[y + 1] = prefix[y] + ((input[y * columns + column] shr shift) and 0xFF)
                }
                for (y in 0 until height) {
                    val top = (y - radius).coerceAtLeast(0)
                    val bottom = (y + radius).coerceAtMost(height - 1)
                    val sum = prefix[bottom + 1] - prefix[top]
                    val value = (sum / (bottom - top + 1)).coerceIn(0, 255)
                    output[y * columns + column] = output[y * columns + column] or (value shl shift)
                }
            }
        }
    }
}

/** 3x3 unsharp mask: out = centre + amount * (centre - average of the four neighbours). */
internal object SharpenFilter {

    suspend fun applyInPlace(bitmap: Bitmap, amount: Float) {
        if (amount <= 0f) return
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return
        val bandHeight = 48
        val input = IntArray(width * (bandHeight + 2))
        val output = IntArray(width * bandHeight)
        var top = 0
        while (top < height) {
            currentCoroutineContext().ensureActive()
            val rows = minOf(bandHeight, height - top)
            val readTop = (top - 1).coerceAtLeast(0)
            val readBottom = (top + rows).coerceAtMost(height - 1)
            val readCount = readBottom - readTop + 1
            bitmap.getPixels(input, 0, width, 0, readTop, width, readCount)
            for (row in 0 until rows) {
                val centerRow = top + row - readTop
                val upRow = (centerRow - 1).coerceAtLeast(0)
                val downRow = (centerRow + 1).coerceAtMost(readCount - 1)
                val outOffset = row * width
                val centerOffset = centerRow * width
                val upOffset = upRow * width
                val downOffset = downRow * width
                for (x in 0 until width) {
                    val center = input[centerOffset + x]
                    val leftPixel = input[centerOffset + (x - 1).coerceAtLeast(0)]
                    val rightPixel = input[centerOffset + (x + 1).coerceAtMost(width - 1)]
                    val upPixel = input[upOffset + x]
                    val downPixel = input[downOffset + x]
                    val alpha = center ushr 24
                    val red = sharpenChannel(
                        (center shr 16) and 0xFF,
                        (leftPixel shr 16) and 0xFF,
                        (rightPixel shr 16) and 0xFF,
                        (upPixel shr 16) and 0xFF,
                        (downPixel shr 16) and 0xFF,
                        amount,
                    )
                    val green = sharpenChannel(
                        (center shr 8) and 0xFF,
                        (leftPixel shr 8) and 0xFF,
                        (rightPixel shr 8) and 0xFF,
                        (upPixel shr 8) and 0xFF,
                        (downPixel shr 8) and 0xFF,
                        amount,
                    )
                    val blue = sharpenChannel(
                        center and 0xFF,
                        leftPixel and 0xFF,
                        rightPixel and 0xFF,
                        upPixel and 0xFF,
                        downPixel and 0xFF,
                        amount,
                    )
                    output[outOffset + x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
                }
            }
            bitmap.setPixels(output, 0, width, 0, top, width, rows)
            top += rows
        }
    }

    private fun sharpenChannel(
        center: Int,
        left: Int,
        right: Int,
        up: Int,
        down: Int,
        amount: Float,
    ): Int {
        val average = (left + right + up + down) / 4f
        val sharpened = center + amount * (center - average)
        return sharpened.toInt().coerceIn(0, 255)
    }
}
