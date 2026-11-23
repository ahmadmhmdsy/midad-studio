package com.midad.studio.studio.photo.engine

import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.midad.studio.R
import com.midad.studio.core.image.BitmapTools
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * All colour adjustments. Exposure is in stops; every other slider is -100..100, while
 * vignette, grain, sharpen and blur run 0..100.
 */
data class Adjustments(
    val exposure: Float = 0f,
    val brightness: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val warmth: Int = 0,
    val tint: Int = 0,
    val highlights: Int = 0,
    val shadows: Int = 0,
    val vignette: Int = 0,
    val grain: Int = 0,
    val sharpen: Int = 0,
    val blur: Int = 0,
) {
    val isNeutral: Boolean
        get() = exposure == 0f && brightness == 0 && contrast == 0 && saturation == 0 &&
            warmth == 0 && tint == 0 && highlights == 0 && shadows == 0 && vignette == 0 &&
            grain == 0 && sharpen == 0 && blur == 0
}

/**
 * One of Midad's own film looks. Curves are per channel; [matrix] carries the cross-channel
 * part (saturation, gains and offsets) and is interpreted in the 0..1 range.
 */
class PhotoFilter internal constructor(
    @StringRes val nameRes: Int,
    val swatches: List<Int>,
    internal val redCurve: (Float) -> Float,
    internal val greenCurve: (Float) -> Float,
    internal val blueCurve: (Float) -> Float,
    internal val matrix: FloatArray?,
)

/** The preset looks. Names, curves and swatches are original to Midad. */
object PhotoFilters {

    val all: List<PhotoFilter> = listOf(
        look(
            R.string.p_filter_paper,
            listOf(0xFFF2E9D8.toInt(), 0xFFD8C6A8.toInt(), 0xFF8C7A62.toInt()),
            red = liftGammaGain(0.10f, 0.96f, 1.02f),
            green = liftGammaGain(0.10f, 0.97f, 1.00f),
            blue = liftGammaGain(0.12f, 0.98f, 0.94f),
            saturation = 0.86f,
            contrast = 0.92f,
        ),
        look(
            R.string.p_filter_saffron,
            listOf(0xFFE8B75A.toInt(), 0xFFC8923F.toInt(), 0xFF8A5A22.toInt()),
            red = liftGammaGain(0.03f, 0.98f, 1.08f),
            green = liftGammaGain(0.03f, 0.99f, 1.02f),
            blue = liftGammaGain(0.04f, 1.02f, 0.88f),
            saturation = 0.95f,
            contrast = 1.03f,
        ),
        look(
            R.string.p_filter_noir,
            listOf(0xFF2A2622.toInt(), 0xFF8C8882.toInt(), 0xFFE6E2DA.toInt()),
            saturation = 0f,
            contrast = 1.22f,
            redOffset = 0.01f,
            greenOffset = 0.01f,
            blueOffset = 0.01f,
        ),
        look(
            R.string.p_filter_dunes,
            listOf(0xFFE3C9A1.toInt(), 0xFFB99B72.toInt(), 0xFF7A6244.toInt()),
            red = liftGammaGain(0.07f, 0.99f, 1.05f),
            green = liftGammaGain(0.07f, 1.00f, 0.99f),
            blue = liftGammaGain(0.08f, 1.02f, 0.86f),
            saturation = 0.75f,
            contrast = 0.97f,
        ),
        look(
            R.string.p_filter_mint,
            listOf(0xFFBFD9CB.toInt(), 0xFF7FA995.toInt(), 0xFF446B5E.toInt()),
            red = liftGammaGain(0.05f, 1.02f, 0.94f),
            green = liftGammaGain(0.04f, 0.98f, 1.04f),
            blue = liftGammaGain(0.05f, 1.00f, 1.00f),
            saturation = 0.90f,
            contrast = 1.00f,
        ),
        look(
            R.string.p_filter_dusk,
            listOf(0xFF6E6A8F.toInt(), 0xFF9E8FA6.toInt(), 0xFFD9B98C.toInt()),
            red = liftGammaGain(0.04f, 1.00f, 1.02f),
            green = liftGammaGain(0.04f, 1.02f, 0.92f),
            blue = liftGammaGain(0.05f, 0.97f, 1.08f),
            saturation = 1.02f,
            contrast = 1.04f,
            redOffset = 0.01f,
            blueOffset = 0.02f,
        ),
        look(
            R.string.p_filter_warm_film,
            listOf(0xFFEBC9A0.toInt(), 0xFFC08A5A.toInt(), 0xFF6E4A32.toInt()),
            red = liftGammaGain(0.08f, 0.95f, 1.08f),
            green = liftGammaGain(0.08f, 0.97f, 1.00f),
            blue = liftGammaGain(0.10f, 1.03f, 0.90f),
            saturation = 0.90f,
            contrast = 0.95f,
        ),
        look(
            R.string.p_filter_cold_chrome,
            listOf(0xFF9DB4C6.toInt(), 0xFF5A7590.toInt(), 0xFF2C3A4C.toInt()),
            red = liftGammaGain(0.02f, 1.02f, 0.90f),
            green = liftGammaGain(0.02f, 1.00f, 0.98f),
            blue = liftGammaGain(0.02f, 0.97f, 1.10f),
            saturation = 0.80f,
            contrast = 1.12f,
        ),
        look(
            R.string.p_filter_fade,
            listOf(0xFFE0D9CC.toInt(), 0xFFB0A794.toInt(), 0xFF7E7566.toInt()),
            red = liftGammaGain(0.16f, 1.05f, 0.96f),
            green = liftGammaGain(0.16f, 1.05f, 0.96f),
            blue = liftGammaGain(0.17f, 1.05f, 0.97f),
            saturation = 0.65f,
            contrast = 0.85f,
        ),
        look(
            R.string.p_filter_amber,
            listOf(0xFFF0C25E.toInt(), 0xFFBF842A.toInt(), 0xFF6B4A1C.toInt()),
            red = liftGammaGain(0.04f, 0.98f, 1.12f),
            green = liftGammaGain(0.04f, 1.00f, 1.00f),
            blue = liftGammaGain(0.06f, 1.04f, 0.78f),
            saturation = 0.85f,
            contrast = 1.02f,
        ),
        look(
            R.string.p_filter_forest,
            listOf(0xFF9FB08C.toInt(), 0xFF5E7550.toInt(), 0xFF2F4032.toInt()),
            red = liftGammaGain(0.03f, 1.02f, 0.90f),
            green = liftGammaGain(0.02f, 0.98f, 1.06f),
            blue = liftGammaGain(0.03f, 1.00f, 0.92f),
            saturation = 0.80f,
            contrast = 1.04f,
            greenOffset = 0.01f,
        ),
        look(
            R.string.p_filter_rosewater,
            listOf(0xFFF1D4D0.toInt(), 0xFFC99692.toInt(), 0xFF8A5D63.toInt()),
            red = liftGammaGain(0.07f, 0.97f, 1.06f),
            green = liftGammaGain(0.07f, 1.00f, 0.96f),
            blue = liftGammaGain(0.06f, 0.99f, 1.03f),
            saturation = 0.88f,
            contrast = 0.96f,
            redOffset = 0.01f,
        ),
    )

    fun at(index: Int?): PhotoFilter? {
        if (index == null) return null
        return all.getOrNull(index)
    }

    private fun liftGammaGain(lift: Float, gamma: Float, gain: Float): (Float) -> Float =
        { value ->
            val lifted = (value + lift * (1f - value)).coerceIn(0f, 1f)
            (lifted.pow(gamma) * gain).coerceIn(0f, 1f)
        }

    private fun look(
        @StringRes nameRes: Int,
        swatches: List<Int>,
        red: (Float) -> Float = { it },
        green: (Float) -> Float = { it },
        blue: (Float) -> Float = { it },
        saturation: Float = 1f,
        contrast: Float = 1f,
        redGain: Float = 1f,
        greenGain: Float = 1f,
        blueGain: Float = 1f,
        redOffset: Float = 0f,
        greenOffset: Float = 0f,
        blueOffset: Float = 0f,
    ): PhotoFilter {
        val grading = diagonalMatrix(redGain, greenGain, blueGain, redOffset, greenOffset, blueOffset)
        val contrasted = combine(contrastMatrix(contrast), grading)
        val matrix = combine(saturationMatrix(saturation), contrasted)
        return PhotoFilter(nameRes, swatches, red, green, blue, matrix)
    }

    /** 3x4 matrix maths; offsets are stored in the 0..1 range. */
    private fun combine(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(12)
        for (row in 0 until 3) {
            for (column in 0 until 3) {
                out[row * 4 + column] =
                    a[row * 4] * b[column] +
                        a[row * 4 + 1] * b[4 + column] +
                        a[row * 4 + 2] * b[8 + column]
            }
            out[row * 4 + 3] =
                a[row * 4] * b[3] + a[row * 4 + 1] * b[7] + a[row * 4 + 2] * b[11] + a[row * 4 + 3]
        }
        return out
    }

    private fun diagonalMatrix(
        r: Float,
        g: Float,
        b: Float,
        redOffset: Float,
        greenOffset: Float,
        blueOffset: Float,
    ): FloatArray = floatArrayOf(
        r, 0f, 0f, redOffset,
        0f, g, 0f, greenOffset,
        0f, 0f, b, blueOffset,
    )

    private fun contrastMatrix(contrast: Float): FloatArray {
        val offset = 0.5f - 0.5f * contrast
        return floatArrayOf(
            contrast, 0f, 0f, offset,
            0f, contrast, 0f, offset,
            0f, 0f, contrast, offset,
        )
    }

    private fun saturationMatrix(saturation: Float): FloatArray {
        val inv = 1f - saturation
        val lr = 0.2126f * inv
        val lg = 0.7152f * inv
        val lb = 0.0722f * inv
        return floatArrayOf(
            lr + saturation, lg, lb, 0f,
            lr, lg + saturation, lb, 0f,
            lr, lg, lb + saturation, 0f,
        )
    }
}

/**
 * Pre-computes the 256-entry look-up tables and the remaining per-pixel parameters so the
 * whole colour stage is a single pass over the bitmap.
 */
internal class ColorProgram(
    adjustments: Adjustments,
    filter: PhotoFilter?,
    filterIntensity: Int,
) {
    val lutRed = IntArray(256)
    val lutGreen = IntArray(256)
    val lutBlue = IntArray(256)
    val saturation: Float = adjustments.saturation / 100f
    val matrix: FloatArray? = filter?.matrix
    val matrixMix: Float = (filterIntensity.coerceIn(0, 100) / 100f)
    val vignette: Float = adjustments.vignette / 100f * 0.85f
    val grain: Float = adjustments.grain / 100f * 0.34f

    val isNeutral: Boolean =
        (adjustments.isNeutral) && (filter == null || matrixMix <= 0f)

    init {
        val intensity = matrixMix
        for (index in 0 until 256) {
            val value = index / 255f
            lutRed[index] = buildChannel(value, 0, adjustments, filter, intensity)
            lutGreen[index] = buildChannel(value, 1, adjustments, filter, intensity)
            lutBlue[index] = buildChannel(value, 2, adjustments, filter, intensity)
        }
    }

    private fun buildChannel(
        value: Float,
        channel: Int,
        adjustments: Adjustments,
        filter: PhotoFilter?,
        intensity: Float,
    ): Int {
        val tuned = tune(value, channel, adjustments)
        val curved = when (channel) {
            0 -> filter?.redCurve?.invoke(tuned) ?: tuned
            1 -> filter?.greenCurve?.invoke(tuned) ?: tuned
            else -> filter?.blueCurve?.invoke(tuned) ?: tuned
        }
        val mixed = tuned + (curved - tuned) * intensity
        return (mixed.coerceIn(0f, 1f) * 255f).roundToInt()
    }

    private fun tune(value: Float, channel: Int, adjustments: Adjustments): Float {
        var result = value
        if (adjustments.exposure != 0f) result *= 2f.pow(adjustments.exposure)
        if (adjustments.brightness != 0) result += adjustments.brightness / 100f * 0.42f
        if (adjustments.contrast != 0) {
            val contrast = adjustments.contrast / 100f
            result = (result - 0.5f) * (1f + contrast * 0.9f) + 0.5f
        }
        if (adjustments.shadows != 0) {
            val shadows = adjustments.shadows / 100f
            result += shadows * 0.35f * (1f - result.coerceIn(0f, 1f)).pow(2f)
        }
        if (adjustments.highlights != 0) {
            val highlights = adjustments.highlights / 100f
            result += highlights * 0.35f * result.coerceIn(0f, 1f).pow(2f)
        }
        if (adjustments.warmth != 0) {
            val warmth = adjustments.warmth / 100f
            when (channel) {
                0 -> result += warmth * 0.13f
                2 -> result -= warmth * 0.13f
            }
        }
        if (adjustments.tint != 0) {
            val tint = adjustments.tint / 100f
            when (channel) {
                1 -> result -= tint * 0.12f
                else -> result += tint * 0.06f
            }
        }
        return result.coerceIn(0f, 1f)
    }
}

/** Colour stage: optional creative blur, optional sharpen, then one LUT/matrix pass. */
object ColorPipeline {

    suspend fun render(
        source: Bitmap,
        adjustments: Adjustments,
        filter: PhotoFilter?,
        filterIntensity: Int,
        inPlace: Boolean = false,
        onProgress: ((Float) -> Unit)? = null,
    ): Bitmap {
        currentCoroutineContext().ensureActive()
        val working = if (inPlace && source.isMutable) source else BitmapTools.copyOf(source)

        if (adjustments.blur > 0) {
            val radius = blurRadius(adjustments.blur, minOf(working.width, working.height))
            BlurEngine.blurInPlace(working, radius)
            onProgress?.invoke(0.4f)
        }
        if (adjustments.sharpen > 0) {
            currentCoroutineContext().ensureActive()
            SharpenFilter.applyInPlace(working, adjustments.sharpen / 100f * 1.5f)
            onProgress?.invoke(0.55f)
        }

        val program = ColorProgram(adjustments, filter, filterIntensity)
        if (!program.isNeutral) {
            applyProgram(working, program)
            onProgress?.invoke(1f)
        }
        return working
    }

    internal fun blurRadius(blur: Int, minSide: Int): Int {
        if (blur <= 0) return 0
        val raw = (blur / 100f) * (minSide * 0.02f)
        return raw.roundToInt().coerceIn(1, 64)
    }

    private suspend fun applyProgram(bitmap: Bitmap, program: ColorProgram) {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return
        val bandHeight = 64
        val buffer = IntArray(width * bandHeight)
        var top = 0
        while (top < height) {
            currentCoroutineContext().ensureActive()
            val rows = minOf(bandHeight, height - top)
            bitmap.getPixels(buffer, 0, width, 0, top, width, rows)
            processBand(buffer, width, rows, top, height, program)
            bitmap.setPixels(buffer, 0, width, 0, top, width, rows)
            top += rows
        }
    }

    private fun processBand(
        buffer: IntArray,
        width: Int,
        rows: Int,
        top: Int,
        imageHeight: Int,
        program: ColorProgram,
    ) {
        val matrix = program.matrix
        val matrixMix = program.matrixMix
        val useMatrix = matrix != null && matrixMix > 0f
        val saturation = program.saturation
        val vignette = program.vignette
        val grain = program.grain
        val useVignette = vignette > 0f
        val useGrain = grain > 0f
        val inverseDiagonal = 1f / 1.41421356f

        for (row in 0 until rows) {
            val y = top + row
            val normalizedY = (y + 0.5f) / imageHeight
            val rowOffset = row * width
            for (x in 0 until width) {
                val index = rowOffset + x
                val color = buffer[index]
                val alpha = color ushr 24
                var red = program.lutRed[(color shr 16) and 0xFF].toFloat()
                var green = program.lutGreen[(color shr 8) and 0xFF].toFloat()
                var blue = program.lutBlue[color and 0xFF].toFloat()

                if (saturation != 0f) {
                    val luma = 0.2126f * red + 0.7152f * green + 0.0722f * blue
                    val factor = 1f + saturation
                    red = luma + (red - luma) * factor
                    green = luma + (green - luma) * factor
                    blue = luma + (blue - luma) * factor
                }

                if (useMatrix) {
                    val mixedRed = matrix!![0] * red + matrix[1] * green + matrix[2] * blue + matrix[3] * 255f
                    val mixedGreen = matrix[4] * red + matrix[5] * green + matrix[6] * blue + matrix[7] * 255f
                    val mixedBlue = matrix[8] * red + matrix[9] * green + matrix[10] * blue + matrix[11] * 255f
                    red += (mixedRed - red) * matrixMix
                    green += (mixedGreen - green) * matrixMix
                    blue += (mixedBlue - blue) * matrixMix
                }

                if (useVignette) {
                    val normalizedX = (x + 0.5f) / width
                    val dx = normalizedX * 2f - 1f
                    val dy = normalizedY * 2f - 1f
                    val distance = sqrt(dx * dx + dy * dy) * inverseDiagonal
                    val falloff = smoothStep(0.35f, 1f, distance)
                    val factor = 1f - vignette * falloff
                    red *= factor
                    green *= factor
                    blue *= factor
                }

                if (useGrain) {
                    val noise = noiseAt(x, y) * grain * 255f
                    red += noise
                    green += noise
                    blue += noise
                }

                buffer[index] = (alpha shl 24) or
                    (red.toInt().coerceIn(0, 255) shl 16) or
                    (green.toInt().coerceIn(0, 255) shl 8) or
                    blue.toInt().coerceIn(0, 255)
            }
        }
    }

    private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Deterministic monochrome film grain; no allocation, same result for a given pixel. */
    private fun noiseAt(x: Int, y: Int): Float {
        var seed = x * 374761393 + y * 668265263
        seed = (seed xor (seed shr 13)) * 1274126177
        seed = seed xor (seed shr 16)
        return ((seed and 0xFFFF) / 65535f) - 0.5f
    }
}
