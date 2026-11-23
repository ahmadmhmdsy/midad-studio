package com.midad.studio.core.ai.internal

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

/**
 * Builds the feathered alpha matte and composites it over a backdrop.
 *
 * Every function returns a new bitmap and leaves its inputs usable, because callers keep the
 * source photo and the transparent cut-out for previews and exports.
 */
object MaskComposer {

    /** White ARGB mask whose alpha channel holds the subject confidence. */
    fun maskFromAlpha(alpha: FloatArray, width: Int, height: Int): Bitmap {
        val pixels = IntArray(width * height)
        val count = min(pixels.size, alpha.size)
        for (index in 0 until count) {
            val value = (alpha[index].coerceIn(0f, 1f) * 255f + 0.5f).toInt().coerceIn(0, 255)
            pixels[index] = (value shl 24) or 0x00FFFFFF
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * Feathers the model-sized mask (256-512 px) and only then upsamples it to the
     * photo size. Feathering after upscaling allocated seven full-resolution
     * IntArrays (about 340 MB for a 12 MP photo); feathering first costs about 2 MB.
     */
    fun scaleAndFeather(mask: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val radius = min(max(min(mask.width, mask.height) / 450, 1), 8)
        val feathered = featherAlpha(mask, radius)
        val scaled = if (feathered.width == targetWidth && feathered.height == targetHeight) {
            feathered
        } else {
            Bitmap.createScaledBitmap(feathered, targetWidth, targetHeight, true)
        }
        if (scaled !== feathered) feathered.recycle()
        return scaled
    }

    /** Separable box blur on the alpha channel only; the colour stays white. */
    fun featherAlpha(mask: Bitmap, radius: Int): Bitmap {
        val width = mask.width
        val height = mask.height
        val source = IntArray(width * height)
        mask.getPixels(source, 0, width, 0, 0, width, height)
        val alpha = IntArray(source.size) { index -> (source[index] ushr 24) and 0xFF }
        if (radius <= 0 || width < 3 || height < 3) {
            val untouched = IntArray(source.size) { index ->
                (alpha[index] shl 24) or (source[index] and 0x00FFFFFF)
            }
            return Bitmap.createBitmap(untouched, width, height, Bitmap.Config.ARGB_8888)
        }
        val horizontal = IntArray(source.size)
        val vertical = IntArray(source.size)
        boxBlurHorizontal(alpha, horizontal, width, height, radius)
        boxBlurHorizontal(horizontal, vertical, width, height, radius)
        val horizontal2 = IntArray(source.size)
        boxBlurVertical(vertical, horizontal2, width, height, radius)
        val vertical2 = IntArray(source.size)
        boxBlurVertical(horizontal2, vertical2, width, height, radius)
        val pixels = IntArray(source.size) { index ->
            (vertical2[index] shl 24) or (source[index] and 0x00FFFFFF)
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /** Paints the transparent cut-out over the source colours. */
    fun applyAlphaMask(source: Bitmap, mask: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val sourcePixels = IntArray(width * height)
        source.getPixels(sourcePixels, 0, width, 0, 0, width, height)
        val scaledMask = if (mask.width == width && mask.height == height) {
            mask
        } else {
            Bitmap.createScaledBitmap(mask, width, height, true)
        }
        val maskPixels = IntArray(width * height)
        scaledMask.getPixels(maskPixels, 0, width, 0, 0, width, height)
        if (scaledMask !== mask) scaledMask.recycle()
        val pixels = IntArray(sourcePixels.size)
        for (index in pixels.indices) {
            val alpha = (maskPixels[index] ushr 24) and 0xFF
            pixels[index] = (alpha shl 24) or (sourcePixels[index] and 0x00FFFFFF)
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    fun overSolid(cutout: Bitmap, color: Int): Bitmap {
        val width = cutout.width
        val height = cutout.height
        val pixels = IntArray(width * height)
        cutout.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(pixels.size) { index -> blendOver(pixels[index], color) }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    fun overBitmap(cutout: Bitmap, backdrop: Bitmap): Bitmap {
        val width = cutout.width
        val height = cutout.height
        val cutoutPixels = IntArray(width * height)
        cutout.getPixels(cutoutPixels, 0, width, 0, 0, width, height)
        val scaledBackdrop = if (backdrop.width == width && backdrop.height == height) {
            backdrop
        } else {
            Bitmap.createScaledBitmap(backdrop, width, height, true)
        }
        val backdropPixels = IntArray(width * height)
        scaledBackdrop.getPixels(backdropPixels, 0, width, 0, 0, width, height)
        if (scaledBackdrop !== backdrop) scaledBackdrop.recycle()
        val output = IntArray(cutoutPixels.size) { index ->
            blendOver(cutoutPixels[index], backdropPixels[index])
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    fun overBlur(cutout: Bitmap, source: Bitmap): Bitmap = overBitmap(cutout, blur(source))

    /** Cheap portrait blur: downscale a copy, box-blur it, scale it back over the photo. */
    fun blur(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val smallWidth = max(1, width / 10)
        val smallHeight = max(1, height / 10)
        val small = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
        val pixels = IntArray(smallWidth * smallHeight)
        small.getPixels(pixels, 0, smallWidth, 0, 0, smallWidth, smallHeight)
        small.recycle()
        val red = IntArray(pixels.size)
        val green = IntArray(pixels.size)
        val blue = IntArray(pixels.size)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            red[index] = (pixel shr 16) and 0xFF
            green[index] = (pixel shr 8) and 0xFF
            blue[index] = pixel and 0xFF
        }
        val radius = max(1, min(smallWidth, smallHeight) / 16)
        val blurredRed = blurChannel(red, smallWidth, smallHeight, radius)
        val blurredGreen = blurChannel(green, smallWidth, smallHeight, radius)
        val blurredBlue = blurChannel(blue, smallWidth, smallHeight, radius)
        val smallPixels = IntArray(pixels.size) { index ->
            (0xFF shl 24) or (blurredRed[index] shl 16) or (blurredGreen[index] shl 8) or blurredBlue[index]
        }
        val blurredSmall = Bitmap.createBitmap(smallPixels, smallWidth, smallHeight, Bitmap.Config.ARGB_8888)
        val output = Bitmap.createScaledBitmap(blurredSmall, width, height, true)
        blurredSmall.recycle()
        return output
    }

    private fun blurChannel(values: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val horizontal = IntArray(values.size)
        val vertical = IntArray(values.size)
        boxBlurHorizontal(values, horizontal, width, height, radius)
        boxBlurHorizontal(horizontal, vertical, width, height, radius)
        val horizontal2 = IntArray(values.size)
        boxBlurVertical(vertical, horizontal2, width, height, radius)
        val vertical2 = IntArray(values.size)
        boxBlurVertical(horizontal2, vertical2, width, height, radius)
        return vertical2
    }

    private fun boxBlurHorizontal(input: IntArray, output: IntArray, width: Int, height: Int, radius: Int) {
        val window = radius * 2 + 1
        for (row in 0 until height) {
            val offset = row * width
            var sum = 0
            for (step in -radius..radius) {
                sum += input[offset + step.coerceIn(0, width - 1)]
            }
            for (column in 0 until width) {
                output[offset + column] = sum / window
                val removeIndex = (column - radius).coerceIn(0, width - 1)
                val addIndex = (column + radius + 1).coerceIn(0, width - 1)
                sum += input[offset + addIndex] - input[offset + removeIndex]
            }
        }
    }

    private fun boxBlurVertical(input: IntArray, output: IntArray, width: Int, height: Int, radius: Int) {
        val window = radius * 2 + 1
        for (column in 0 until width) {
            var sum = 0
            for (step in -radius..radius) {
                sum += input[step.coerceIn(0, height - 1) * width + column]
            }
            for (row in 0 until height) {
                output[row * width + column] = sum / window
                val removeIndex = (row - radius).coerceIn(0, height - 1)
                val addIndex = (row + radius + 1).coerceIn(0, height - 1)
                sum += input[addIndex * width + column] - input[removeIndex * width + column]
            }
        }
    }

    private fun blendOver(foreground: Int, background: Int): Int {
        val alpha = (foreground ushr 24) and 0xFF
        if (alpha == 255) return foreground
        if (alpha == 0) return background or (0xFF shl 24)
        val inverse = 255 - alpha
        val red = (((foreground shr 16) and 0xFF) * alpha + ((background shr 16) and 0xFF) * inverse) / 255
        val green = (((foreground shr 8) and 0xFF) * alpha + ((background shr 8) and 0xFF) * inverse) / 255
        val blue = ((foreground and 0xFF) * alpha + (background and 0xFF) * inverse) / 255
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
