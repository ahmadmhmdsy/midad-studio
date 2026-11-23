package com.midad.studio.studio.video

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult as Media3ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Renders the whole project with Media3 Transformer: one sequence of trimmed clips, the chosen
 * colour look, one shared output canvas, the optional title overlay and optional audio removal.
 * The caller owns the returned cache file and must delete it after saving it.
 */
class VideoExporter(private val context: Context) {

    private var transformer: Transformer? = null
    private var titleBitmap: Bitmap? = null

    @Volatile
    private var cancelled = false

    fun cancel() {
        cancelled = true
        runCatching { transformer?.cancel() }
    }

    fun releaseTitleBitmap() {
        titleBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        titleBitmap = null
    }

    suspend fun export(spec: VideoExportSpec, onProgress: (Float?) -> Unit): File {
        check(!cancelled) { "Export was cancelled" }
        val bitmap = withContext(Dispatchers.Default) {
            TitleRenderer.render(spec.title, spec.outputWidth, spec.outputHeight)
        }
        titleBitmap = bitmap
        val composition = VideoComposition.build(spec, bitmap)
        val output = File(context.cacheDir, "midad_video_" + System.currentTimeMillis() + ".mp4")
        if (output.exists()) runCatching { output.delete() }
        try {
            runExport(composition, output, onProgress)
        } catch (t: Throwable) {
            runCatching { output.delete() }
            throw t
        }
        return output
    }

    private suspend fun runExport(
        composition: Composition,
        output: File,
        onProgress: (Float?) -> Unit,
    ) = coroutineScope {
        val holder = ProgressHolder()
        var active: Transformer? = null
        val poller = launch {
            while (isActive) {
                delay(PROGRESS_POLL_MS)
                val running = active ?: continue
                val progressState = runCatching { running.getProgress(holder) }.getOrNull()
                    ?: continue
                if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress((holder.progress / 100f).coerceIn(0f, 1f))
                } else {
                    onProgress(null)
                }
            }
        }
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                val listener = object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        exportResult: Media3ExportResult,
                    ) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: Media3ExportResult,
                        exportException: ExportException,
                    ) {
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                }
                val newTransformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(listener)
                    .build()
                active = newTransformer
                transformer = newTransformer
                continuation.invokeOnCancellation {
                    runCatching { newTransformer.cancel() }
                }
                try {
                    newTransformer.start(composition, output.absolutePath)
                } catch (t: Throwable) {
                    if (continuation.isActive) continuation.resumeWithException(t)
                }
            }
        } finally {
            poller.cancel()
            active = null
            transformer = null
        }
    }

    companion object {
        private const val PROGRESS_POLL_MS = 400L

        /** Maps Media3 export failures to one of the studio's localized error kinds. */
        fun classify(error: Throwable): VideoExportError {
            val exportException = error as? ExportException ?: error.cause as? ExportException
            val code = exportException?.errorCode ?: return VideoExportError.GENERIC
            return when (code) {
                ExportException.ERROR_CODE_ENCODER_INIT_FAILED,
                ExportException.ERROR_CODE_ENCODING_FAILED,
                ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED,
                -> VideoExportError.ENCODE

                ExportException.ERROR_CODE_DECODER_INIT_FAILED,
                ExportException.ERROR_CODE_DECODING_FAILED,
                ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                -> VideoExportError.DECODE

                ExportException.ERROR_CODE_IO_UNSPECIFIED,
                ExportException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                ExportException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                ExportException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
                ExportException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                ExportException.ERROR_CODE_IO_FILE_NOT_FOUND,
                ExportException.ERROR_CODE_IO_NO_PERMISSION,
                ExportException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
                ExportException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                -> VideoExportError.IO

                ExportException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED,
                ExportException.ERROR_CODE_AUDIO_PROCESSING_FAILED,
                -> VideoExportError.EFFECT

                else -> VideoExportError.GENERIC
            }
        }
    }
}
