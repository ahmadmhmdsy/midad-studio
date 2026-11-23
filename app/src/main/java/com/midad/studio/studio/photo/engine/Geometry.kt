package com.midad.studio.studio.photo.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin

/** Aspect presets offered by the crop tool. The ratio is width / height in pixels. */
enum class CropAspect(val ratio: Float?) {
    FREE(null),
    SQUARE(1f),
    PORTRAIT_4_5(4f / 5f),
    LANDSCAPE_16_9(16f / 9f),
    PORTRAIT_9_16(9f / 16f),
    LANDSCAPE_3_2(3f / 2f),
}

/** A rectangle in normalised (0..1) coordinates of the straightened image. */
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    val width: Float get() = (right - left).coerceAtLeast(0.002f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.002f)

    fun clamped(): CropRect {
        val l = left.coerceIn(0f, 0.998f)
        val t = top.coerceIn(0f, 0.998f)
        val r = right.coerceIn(l + 0.002f, 1f)
        val b = bottom.coerceIn(t + 0.002f, 1f)
        return CropRect(l, t, r, b)
    }

    fun isFull(): Boolean = left <= 0.0001f && top <= 0.0001f && right >= 0.9999f && bottom >= 0.9999f

    companion object {
        val Full = CropRect()

        /** Largest centred rect with [aspectRatio] (w/h) that fits an image of the given pixel size. */
        fun fitAspect(imageWidth: Int, imageHeight: Int, aspectRatio: Float?): CropRect {
            if (aspectRatio == null || imageWidth <= 0 || imageHeight <= 0) return Full
            val imageRatio = imageWidth.toFloat() / imageHeight.toFloat()
            return if (imageRatio > aspectRatio) {
                val w = aspectRatio / imageRatio
                val left = (1f - w) / 2f
                CropRect(left, 0f, left + w, 1f)
            } else {
                val h = imageRatio / aspectRatio
                val top = (1f - h) / 2f
                CropRect(0f, top, 1f, top + h)
            }
        }
    }
}

/**
 * Geometry edits stored independently of the working bitmap. Rotation happens first, then
 * the crop rect is applied to the resulting (possibly expanded) image.
 */
data class GeometryState(
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val straighten: Float = 0f,
    val crop: CropRect = CropRect.Full,
    val aspect: CropAspect = CropAspect.FREE,
) {
    val rotationDegrees: Float get() = quarterTurns * 90f + straighten

    val isIdentity: Boolean
        get() = quarterTurns == 0 && !flipHorizontal && !flipVertical &&
            straighten == 0f && crop.isFull() && aspect == CropAspect.FREE
}

/**
 * Conversion between the base bitmap and the geometry output. The matrix is rigid
 * (rotation + translation, no scaling), which keeps overlay sizes resolution independent.
 */
class GeometryPlan(
    val baseWidth: Int,
    val baseHeight: Int,
    val expandedWidth: Int,
    val expandedHeight: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val matrix: Matrix,
    val rotationDegrees: Float,
) {
    private val inverse: Matrix? = Matrix().let { candidate ->
        if (matrix.invert(candidate)) candidate else null
    }

    fun baseNormalizedToOutputPx(nx: Float, ny: Float): FloatArray {
        val point = floatArrayOf(nx * baseWidth, ny * baseHeight)
        matrix.mapPoints(point)
        return point
    }

    fun outputPxToBaseNormalized(px: Float, py: Float): FloatArray {
        val point = floatArrayOf(px, py)
        val inv = inverse ?: return floatArrayOf(px / baseWidth, py / baseHeight)
        inv.mapPoints(point)
        return floatArrayOf(point[0] / baseWidth, point[1] / baseHeight)
    }

    fun outputDeltaToBaseDelta(dx: Float, dy: Float): FloatArray {
        val inv = inverse ?: return floatArrayOf(dx / baseWidth, dy / baseHeight)
        val origin = floatArrayOf(0f, 0f)
        val edge = floatArrayOf(dx, dy)
        inv.mapPoints(origin)
        inv.mapPoints(edge)
        return floatArrayOf((edge[0] - origin[0]) / baseWidth, (edge[1] - origin[1]) / baseHeight)
    }

    /** Crop preset fitted to the expanded image, in the same normalised space as [GeometryState.crop]. */
    fun fittedCrop(aspect: CropAspect): CropRect =
        CropRect.fitAspect(expandedWidth, expandedHeight, aspect.ratio)
}

object GeometryEngine {

    /** Builds the transform for [geometry] over a base bitmap of the given pixel size. */
    fun plan(baseWidth: Int, baseHeight: Int, geometry: GeometryState): GeometryPlan {
        val safeWidth = baseWidth.coerceAtLeast(1)
        val safeHeight = baseHeight.coerceAtLeast(1)
        val quarter = ((geometry.quarterTurns % 4) + 4) % 4
        val rotation = quarter * 90f + geometry.straighten
        val centerX = safeWidth / 2f
        val centerY = safeHeight / 2f

        val transform = Matrix()
        transform.postTranslate(-centerX, -centerY)
        transform.postScale(
            if (geometry.flipHorizontal) -1f else 1f,
            if (geometry.flipVertical) -1f else 1f,
        )
        if (rotation != 0f) transform.postRotate(rotation)
        transform.postTranslate(centerX, centerY)

        val corners = floatArrayOf(
            0f, 0f,
            safeWidth.toFloat(), 0f,
            safeWidth.toFloat(), safeHeight.toFloat(),
            0f, safeHeight.toFloat(),
        )
        transform.mapPoints(corners)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var index = 0
        while (index < corners.size) {
            minX = minOf(minX, corners[index])
            maxX = maxOf(maxX, corners[index])
            minY = minOf(minY, corners[index + 1])
            maxY = maxOf(maxY, corners[index + 1])
            index += 2
        }
        val expandedWidth = (ceil(maxX - minX).toInt()).coerceAtLeast(1)
        val expandedHeight = (ceil(maxY - minY).toInt()).coerceAtLeast(1)

        val expanded = Matrix(transform)
        expanded.postTranslate(-minX, -minY)

        val crop = geometry.crop.clamped()
        val left = (crop.left * expandedWidth).roundToInt().coerceIn(0, expandedWidth - 1)
        val top = (crop.top * expandedHeight).roundToInt().coerceIn(0, expandedHeight - 1)
        val right = (crop.right * expandedWidth).roundToInt().coerceIn(left + 1, expandedWidth)
        val bottom = (crop.bottom * expandedHeight).roundToInt().coerceIn(top + 1, expandedHeight)

        val finalMatrix = Matrix(expanded)
        finalMatrix.postTranslate(-left.toFloat(), -top.toFloat())

        return GeometryPlan(
            baseWidth = safeWidth,
            baseHeight = safeHeight,
            expandedWidth = expandedWidth,
            expandedHeight = expandedHeight,
            outputWidth = right - left,
            outputHeight = bottom - top,
            matrix = finalMatrix,
            rotationDegrees = rotation,
        )
    }

    /**
     * Renders the geometry output. Returns [base] itself when no work is needed, so callers
     * must check identity before recycling.
     */
    fun render(base: Bitmap, plan: GeometryPlan): Bitmap {
        if (plan.matrix.isIdentity) return base
        val output = Bitmap.createBitmap(plan.outputWidth, plan.outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(base, plan.matrix, paint)
        return output
    }

    /**
     * Re-centres the crop after a transform that changed the expanded canvas size
     * (rotation, flip or straighten), keeping the current aspect preset.
     */
    fun refitCrop(geometry: GeometryState, imageWidth: Int, imageHeight: Int): GeometryState {
        val plan = plan(imageWidth, imageHeight, geometry.copy(crop = CropRect.Full))
        return geometry.copy(crop = plan.fittedCrop(geometry.aspect))
    }

    /** Distance in degrees used by the straighten slider. */
    fun normalizeAngle(degrees: Float): Float = degrees.coerceIn(-45f, 45f)

    /** Rotates [rect] around the image centre by [degrees] (used when overlays must follow). */
    fun rotatedPoint(nx: Float, ny: Float, degrees: Float): FloatArray {
        val radians = Math.toRadians(degrees.toDouble())
        val c = cos(radians).toFloat()
        val s = sin(radians).toFloat()
        val dx = nx - 0.5f
        val dy = ny - 0.5f
        return floatArrayOf(0.5f + dx * c - dy * s, 0.5f + dx * s + dy * c)
    }
}
