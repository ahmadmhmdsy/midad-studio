package com.midad.studio.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bitmap plumbing shared by every studio. All heavy work stays off the main thread. */
object BitmapTools {

    suspend fun decodeSampled(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                }
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return@runCatching null
                val orientation = resolver.openInputStream(uri)?.use { stream ->
                    runCatching {
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL,
                        )
                    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
                rotateForExif(decoded, orientation)
            }.getOrNull()
        }

    /**
     * Largest power-of-two inSampleSize whose decoded longest edge stays within
     * [maxDimension]. This is a true cap: the previous implementation divided only
     * while half the edge still fit, so a 4032 px photo requesting 2048 decoded at
     * full 4032 px (48.8 MB instead of 12.2 MB).
     */
    fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
        val longest = maxOf(width, height)
        var sample = 1
        while (longest / sample > maxDimension) {
            sample *= 2
        }
        return sample
    }

    /** Decoded longest edge produced by [sampleSize] for a source of this size. */
    fun sampledEdge(width: Int, height: Int, maxDimension: Int): Int =
        maxOf(width, height) / sampleSize(width, height, maxDimension)

    fun rotateForExif(source: Bitmap, orientation: Int): Bitmap = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotate(source, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotate(source, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotate(source, 270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flip(source, horizontal = true)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> flip(source, horizontal = false)
        ExifInterface.ORIENTATION_TRANSPOSE -> rotate(flip(source, horizontal = true), 90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> rotate(flip(source, horizontal = true), 270f)
        else -> source
    }

    fun rotate(source: Bitmap, degrees: Float): Bitmap {
        val normalized = ((degrees % 360f) + 360f) % 360f
        if (normalized == 0f) return source
        val matrix = Matrix().apply { postRotate(normalized) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated != source) safeRecycle(source)
        return rotated
    }

    fun flip(source: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
        }
        val flipped = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (flipped != source) safeRecycle(source)
        return flipped
    }

    /** Crop using normalized (0..1) coordinates. */
    fun cropNormalized(source: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Bitmap {
        val l = (left.coerceIn(0f, 1f) * source.width).toInt().coerceIn(0, source.width - 1)
        val t = (top.coerceIn(0f, 1f) * source.height).toInt().coerceIn(0, source.height - 1)
        val r = (right.coerceIn(0f, 1f) * source.width).toInt().coerceIn(l + 1, source.width)
        val b = (bottom.coerceIn(0f, 1f) * source.height).toInt().coerceIn(t + 1, source.height)
        val cropped = Bitmap.createBitmap(source, l, t, r - l, b - t)
        if (cropped != source) safeRecycle(source)
        return cropped
    }

    /** Downscale so the longest edge is at most [maxDimension]; never upscales. */
    fun scaleToMax(source: Bitmap, maxDimension: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxDimension) return source
        val scale = maxDimension.toFloat() / longest.toFloat()
        return createScaled(source, scale)
    }

    /** Fit inside [maxWidth] x [maxHeight] without upscaling. */
    fun scaleToFit(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scale = minOf(
            maxWidth.toFloat() / source.width.toFloat(),
            maxHeight.toFloat() / source.height.toFloat(),
            1f,
        )
        if (scale >= 1f) return source
        return createScaled(source, scale)
    }

    fun createScaled(source: Bitmap, scale: Float): Bitmap {
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    fun toArgb8888(source: Bitmap): Bitmap =
        if (source.config == Bitmap.Config.ARGB_8888) source
        else source.copy(Bitmap.Config.ARGB_8888, true)

    fun copyOf(source: Bitmap): Bitmap =
        source.copy(Bitmap.Config.ARGB_8888, true) ?: source

    fun safeRecycle(bitmap: Bitmap?) {
        runCatching {
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        }
    }
}
