package com.midad.studio.core.ai.internal

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.midad.studio.core.ai.CutoutBackground
import com.midad.studio.core.ai.SubjectSegmenter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

/** Why the last cut-out attempt failed; the UI maps this to a localized message. */
enum class SegmenterFailure {
    NO_MODEL,
    MODEL_UNREADABLE,
    RUNTIME_UNAVAILABLE,
    UNSUPPORTED_MODEL,
    INFERENCE_FAILED,
}

/**
 * On-device subject cut-out with TensorFlow Lite.
 *
 * The installed model is picked by the caller or automatically from the catalogue (see
 * [ModelStorage.findModel] and [ModelStorage.findCutoutModel]), input is resized and normalized
 * exactly as the model card declares, and the raw output tensor is
 * turned into a confidence mask (softmax/sigmoid/threshold are applied only when the catalogue
 * says the graph does not do it itself). The mask is bilinearly upscaled to the photo, feathered
 * and composited over the requested backdrop.
 *
 * Inference runs on Dispatchers.Default. Every failure returns null (with [lastFailure] set and
 * optionally [lastFailureDetail]) instead of throwing, so a missing model or a broken native
 * runtime can never crash the studio.
 */
class TfliteSegmenter(context: Context) : SubjectSegmenter {

    private val appContext = context.applicationContext
    private val lock = Any()
    private var interpreter: Interpreter? = null
    private var loaded: LoadedModel? = null

    /** Reason for the most recent failed cut-out, null after a successful one. */
    @Volatile
    var lastFailure: SegmenterFailure? = null
        private set

    /** Short technical detail for logs and diagnostics; never shown raw to the user. */
    @Volatile
    var lastFailureDetail: String? = null
        private set

    override suspend fun cutout(
        context: Context,
        source: Bitmap,
        background: CutoutBackground,
        modelId: String?,
    ): Bitmap? = withContext(Dispatchers.Default) {
        lastFailure = null
        lastFailureDetail = null
        if (source.isRecycled || source.width <= 0 || source.height <= 0) {
            lastFailure = SegmenterFailure.INFERENCE_FAILED
            lastFailureDetail = "empty source bitmap"
            return@withContext null
        }
        try {
            val installed = if (modelId != null) {
                ModelStorage.findModel(appContext, modelId)
            } else {
                ModelStorage.findCutoutModel(appContext)
            }
            if (installed == null) {
                lastFailure = SegmenterFailure.NO_MODEL
                return@withContext null
            }
            val model = prepare(installed) ?: return@withContext null
            val confidence = synchronized(lock) { runInference(model, source) } ?: return@withContext null
            val mask = MaskComposer.maskFromAlpha(confidence.values, confidence.width, confidence.height)
            val feathered = MaskComposer.scaleAndFeather(mask, source.width, source.height)
            mask.recycle()
            val cutout = MaskComposer.applyAlphaMask(source, feathered)
            feathered.recycle()
            when (background) {
                CutoutBackground.TRANSPARENT -> cutout
                CutoutBackground.BLUR -> {
                    val blurred = MaskComposer.overBlur(cutout, source)
                    cutout.recycle()
                    blurred
                }
                CutoutBackground.LIGHT_SOLID -> {
                    val solid = MaskComposer.overSolid(cutout, LIGHT_BACKDROP)
                    cutout.recycle()
                    solid
                }
                CutoutBackground.DARK_SOLID -> {
                    val solid = MaskComposer.overSolid(cutout, DARK_BACKDROP)
                    cutout.recycle()
                    solid
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (outOfMemory: OutOfMemoryError) {
            lastFailure = SegmenterFailure.INFERENCE_FAILED
            lastFailureDetail = "out of memory"
            Log.w(TAG, "cut-out ran out of memory")
            null
        } catch (failure: Throwable) {
            lastFailure = failureOf(failure)
            lastFailureDetail = failure.message
            Log.w(TAG, "cut-out failed", failure)
            null
        }
    }

    /** Releases the native interpreter; a later cut-out lazily loads it again. */
    override fun close() {
        synchronized(lock) { closeLocked() }
    }

    // ---------------------------------------------------------------------------------------
    // Model loading
    // ---------------------------------------------------------------------------------------

    private fun prepare(installed: InstalledModel): LoadedModel? {
        synchronized(lock) {
            val active = interpreter
            val current = loaded
            if (active != null && current != null &&
                current.file.absolutePath == installed.file.absolutePath &&
                current.file.lastModified() == installed.file.lastModified()
            ) {
                return current
            }
            closeLocked()
            val inputSpec = installed.entry.inputSpec
            val outputSpec = installed.entry.outputSpec
            if (inputSpec == null || outputSpec == null || !inputSpec.valid || !outputSpec.valid) {
                lastFailure = SegmenterFailure.UNSUPPORTED_MODEL
                lastFailureDetail = "catalogue entry has no usable tensor spec"
                return null
            }
            if (!inputSpec.dtype.equals("float32", ignoreCase = true) ||
                !outputSpec.dtype.equals("float32", ignoreCase = true)
            ) {
                lastFailure = SegmenterFailure.UNSUPPORTED_MODEL
                lastFailureDetail = "only float32 tensors are supported"
                return null
            }
            if (!installed.file.isFile || installed.file.length() <= 0L) {
                lastFailure = SegmenterFailure.MODEL_UNREADABLE
                lastFailureDetail = "model file is missing"
                return null
            }
            val created = try {
                Interpreter(installed.file, Interpreter.Options().apply { setNumThreads(2) })
            } catch (failure: Throwable) {
                lastFailure = failureOf(failure)
                lastFailureDetail = failure.message
                Log.w(TAG, "interpreter could not load the model", failure)
                return null
            }
            try {
                val inputTensor = created.getInputTensor(0)
                val outputTensor = created.getOutputTensor(0)
                if (inputTensor.dataType() != DataType.FLOAT32 || outputTensor.dataType() != DataType.FLOAT32) {
                    lastFailure = SegmenterFailure.UNSUPPORTED_MODEL
                    lastFailureDetail = "model tensor is not float32"
                    created.close()
                    return null
                }
                val inputDims = imageDims(inputTensor.shape(), inputSpec.layoutNhwc)
                if (inputDims == null || inputDims.channels !in 3..4) {
                    lastFailure = SegmenterFailure.UNSUPPORTED_MODEL
                    lastFailureDetail = "unsupported input shape"
                    created.close()
                    return null
                }
                val outputDims = imageDims(outputTensor.shape(), outputSpec.layoutNhwc)
                if (outputDims == null || outputDims.channels !in 1..64) {
                    lastFailure = SegmenterFailure.UNSUPPORTED_MODEL
                    lastFailureDetail = "unsupported output shape"
                    created.close()
                    return null
                }
                val model = LoadedModel(
                    file = installed.file,
                    inputSpec = inputSpec,
                    outputSpec = outputSpec,
                    inputWidth = inputDims.width,
                    inputHeight = inputDims.height,
                    inputChannels = inputDims.channels,
                    outputWidth = outputDims.width,
                    outputHeight = outputDims.height,
                    outputChannels = outputDims.channels,
                    // The output tensor shape is independent of the input shape; sizing the
                    // buffer from the input dimensions overflows on models with a larger output.
                    outputElements = outputDims.width * outputDims.height * outputDims.channels,
                )
                interpreter = created
                loaded = model
                return model
            } catch (failure: Throwable) {
                lastFailure = failureOf(failure)
                lastFailureDetail = failure.message
                Log.w(TAG, "model metadata could not be read", failure)
                runCatching { created.close() }
                return null
            }
        }
    }

    private fun closeLocked() {
        runCatching { interpreter?.close() }
        interpreter = null
        loaded = null
    }

    // ---------------------------------------------------------------------------------------
    // Inference
    // ---------------------------------------------------------------------------------------

    private fun runInference(model: LoadedModel, source: Bitmap): ConfidenceMask? {
        val resized = if (source.width == model.inputWidth && source.height == model.inputHeight) {
            null
        } else {
            Bitmap.createScaledBitmap(source, model.inputWidth, model.inputHeight, true)
        }
        val pixels = IntArray(model.inputWidth * model.inputHeight)
        try {
            (resized ?: source).getPixels(
                pixels, 0, model.inputWidth, 0, 0, model.inputWidth, model.inputHeight,
            )
        } finally {
            resized?.recycle()
        }
        val active = interpreter
        if (active == null) {
            lastFailure = SegmenterFailure.MODEL_UNREADABLE
            return null
        }
        val inputBuffer = encodeInput(pixels, model)
        val outputBuffer = ByteBuffer.allocateDirect(model.outputElements * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())
        try {
            active.run(inputBuffer, outputBuffer)
        } catch (outOfMemory: OutOfMemoryError) {
            throw outOfMemory
        } catch (failure: Throwable) {
            lastFailure = failureOf(failure)
            lastFailureDetail = failure.message
            Log.w(TAG, "inference failed", failure)
            return null
        }
        outputBuffer.rewind()
        val raw = FloatArray(model.outputElements)
        outputBuffer.asFloatBuffer().get(raw)
        val planar = if (!model.outputSpec.layoutNhwc && model.outputChannels > 1) {
            pixelMajorFromPlanar(raw, model.outputWidth, model.outputHeight, model.outputChannels)
        } else {
            raw
        }
        val values = interpretOutput(planar, model)
        return ConfidenceMask(values, model.outputWidth, model.outputHeight)
    }

    private fun encodeInput(pixels: IntArray, model: LoadedModel): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(model.inputWidth * model.inputHeight * model.inputChannels * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())
        val spec = model.inputSpec
        if (spec.layoutNhwc) {
            for (index in pixels.indices) {
                val pixel = pixels[index]
                val red = ((pixel shr 16) and 0xFF).toFloat()
                val green = ((pixel shr 8) and 0xFF).toFloat()
                val blue = (pixel and 0xFF).toFloat()
                buffer.putFloat(normalize(red, 0, spec))
                buffer.putFloat(normalize(green, 1, spec))
                buffer.putFloat(normalize(blue, 2, spec))
                if (model.inputChannels >= 4) {
                    // Extra channel is an alpha matte channel; the models expect it zeroed.
                    buffer.putFloat(0f)
                }
            }
        } else {
            for (channel in 0 until 3) {
                for (index in pixels.indices) {
                    val pixel = pixels[index]
                    val channelValue = when (channel) {
                        0 -> ((pixel shr 16) and 0xFF).toFloat()
                        1 -> ((pixel shr 8) and 0xFF).toFloat()
                        else -> (pixel and 0xFF).toFloat()
                    }
                    buffer.putFloat(normalize(channelValue, channel, spec))
                }
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun normalize(value: Float, channel: Int, spec: InputSpec): Float = when (spec.normalization) {
        InputNormalization.ZERO_TO_ONE -> value / 255f
        InputNormalization.MINUS_ONE_TO_ONE -> (value - 127.5f) / 127.5f
        InputNormalization.ZERO_TO_255 -> value
        InputNormalization.IMAGENET -> (value / 255f - IMAGENET_MEAN[channel]) / IMAGENET_STD[channel]
    }

    private fun interpretOutput(raw: FloatArray, model: LoadedModel): FloatArray {
        val channels = model.outputChannels
        val pixelCount = model.outputWidth * model.outputHeight
        val alpha = FloatArray(pixelCount)
        val spec = model.outputSpec
        when (spec.activation) {
            OutputActivation.SOFTMAX -> fillSoftmaxAlpha(raw, pixelCount, channels, spec, alpha)
            OutputActivation.SIGMOID -> fillSigmoidAlpha(raw, pixelCount, channels, spec, alpha)
            OutputActivation.NONE -> {
                if (channels > 1 && looksLikeLogits(raw)) {
                    fillSoftmaxAlpha(raw, pixelCount, channels, spec, alpha)
                } else {
                    fillDirectAlpha(raw, pixelCount, channels, spec, alpha)
                }
            }
        }
        val threshold = spec.threshold
        if (threshold != null && threshold > 0.01f && threshold < 0.99f) {
            for (index in alpha.indices) {
                alpha[index] = smoothstep(threshold - THRESHOLD_SOFTNESS, threshold + THRESHOLD_SOFTNESS, alpha[index])
            }
        } else {
            for (index in alpha.indices) {
                alpha[index] = alpha[index].coerceIn(0f, 1f)
            }
        }
        return alpha
    }

    private fun fillSoftmaxAlpha(raw: FloatArray, pixelCount: Int, channels: Int, spec: OutputSpec, alpha: FloatArray) {
        if (channels <= 1) {
            for (index in 0 until pixelCount) alpha[index] = sigmoid(raw[index])
            return
        }
        val background = spec.backgroundChannel
        val probabilities = FloatArray(channels)
        for (pixel in 0 until pixelCount) {
            val offset = pixel * channels
            var maximum = -Float.MAX_VALUE
            for (channel in 0 until channels) {
                if (raw[offset + channel] > maximum) maximum = raw[offset + channel]
            }
            var sum = 0f
            for (channel in 0 until channels) {
                val value = exp((raw[offset + channel] - maximum).toDouble()).toFloat()
                probabilities[channel] = value
                sum += value
            }
            if (sum <= 0f || sum.isNaN()) {
                alpha[pixel] = 0f
            } else {
                for (channel in 0 until channels) probabilities[channel] /= sum
                alpha[pixel] = foregroundFrom(probabilities, background)
            }
        }
    }

    private fun fillSigmoidAlpha(raw: FloatArray, pixelCount: Int, channels: Int, spec: OutputSpec, alpha: FloatArray) {
        if (channels <= 1) {
            for (index in 0 until pixelCount) alpha[index] = sigmoid(raw[index])
            return
        }
        val background = spec.backgroundChannel
        val values = FloatArray(channels)
        for (pixel in 0 until pixelCount) {
            val offset = pixel * channels
            for (channel in 0 until channels) values[channel] = sigmoid(raw[offset + channel])
            alpha[pixel] = foregroundFrom(values, background)
        }
    }

    private fun fillDirectAlpha(raw: FloatArray, pixelCount: Int, channels: Int, spec: OutputSpec, alpha: FloatArray) {
        if (channels <= 1) {
            for (index in 0 until pixelCount) alpha[index] = raw[index].coerceIn(0f, 1f)
            return
        }
        val background = spec.backgroundChannel
        val values = FloatArray(channels)
        for (pixel in 0 until pixelCount) {
            val offset = pixel * channels
            for (channel in 0 until channels) values[channel] = raw[offset + channel].coerceIn(0f, 1f)
            alpha[pixel] = foregroundFrom(values, background)
        }
    }

    /** Class 0 is background for these models; fall back to "everything but the first class". */
    private fun foregroundFrom(values: FloatArray, backgroundChannel: Int): Float {
        if (backgroundChannel in values.indices) return (1f - values[backgroundChannel]).coerceIn(0f, 1f)
        if (values.size == 2) return values[1].coerceIn(0f, 1f)
        return (1f - values[0]).coerceIn(0f, 1f)
    }

    private fun looksLikeLogits(raw: FloatArray): Boolean {
        for (value in raw) {
            if (value < -0.05f || value > 1.05f) return true
        }
        return false
    }

    private fun sigmoid(value: Float): Float = (1.0 / (1.0 + exp(-value.toDouble()))).toFloat()

    private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
        if (edge1 <= edge0) return if (value >= edge1) 1f else 0f
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun imageDims(shape: IntArray, layoutNhwc: Boolean): Dims? {
        if (shape.size != 4 || shape[0] != 1) return null
        val height = if (layoutNhwc) shape[1] else shape[2]
        val width = if (layoutNhwc) shape[2] else shape[3]
        val channels = if (layoutNhwc) shape[3] else shape[1]
        if (height <= 0 || width <= 0 || channels <= 0) return null
        return Dims(width, height, channels)
    }

    /** NCHW output is channel-planar; the mask maths expects pixel-major channels. */
    private fun pixelMajorFromPlanar(raw: FloatArray, width: Int, height: Int, channels: Int): FloatArray {
        val pixelCount = width * height
        if (raw.size < pixelCount * channels) return raw
        val result = FloatArray(pixelCount * channels)
        for (channel in 0 until channels) {
            val plane = channel * pixelCount
            for (pixel in 0 until pixelCount) {
                result[pixel * channels + channel] = raw[plane + pixel]
            }
        }
        return result
    }

    private fun failureOf(failure: Throwable): SegmenterFailure = when (failure) {
        is UnsatisfiedLinkError, is ExceptionInInitializerError, is NoClassDefFoundError -> {
            SegmenterFailure.RUNTIME_UNAVAILABLE
        }
        else -> SegmenterFailure.INFERENCE_FAILED
    }

    private data class Dims(val width: Int, val height: Int, val channels: Int)

    private data class ConfidenceMask(val values: FloatArray, val width: Int, val height: Int)

    private data class LoadedModel(
        val file: File,
        val inputSpec: InputSpec,
        val outputSpec: OutputSpec,
        val inputWidth: Int,
        val inputHeight: Int,
        val inputChannels: Int,
        val outputWidth: Int,
        val outputHeight: Int,
        val outputChannels: Int,
        val outputElements: Int,
    )

    private companion object {
        const val TAG = "TfliteSegmenter"
        const val FLOAT_BYTES = 4
        const val THRESHOLD_SOFTNESS = 0.18f
        val IMAGENET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        val IMAGENET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
        val LIGHT_BACKDROP = 0xFFF4EFE5.toInt()
        val DARK_BACKDROP = 0xFF1C1915.toInt()
    }
}
