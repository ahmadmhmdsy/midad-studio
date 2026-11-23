package com.midad.studio.studio.photo.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.midad.studio.core.export.ExportError
import com.midad.studio.core.export.ExportManager
import com.midad.studio.core.export.ExportOutcome
import com.midad.studio.core.image.BitmapTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

enum class ExportFormat { JPEG, PNG }

/** Long-edge presets for the exported image. */
enum class ExportLongEdge(val pixels: Int?) {
    ORIGINAL(null),
    PX_2048(2048),
    PX_1080(1080),
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 95,
    val longEdge: ExportLongEdge = ExportLongEdge.ORIGINAL,
)

/**
 * Composes the final image at export resolution: geometry, colour, then overlays, and hands
 * it to the shared ExportManager. All large intermediates are recycled.
 */
object PhotoExporter {

    data class Request(
        val workingSource: Bitmap,
        val sourceUri: Uri?,
        val baseFromOriginal: Boolean,
        val adjustments: Adjustments,
        val filter: PhotoFilter?,
        val filterIntensity: Int,
        val geometry: GeometryState,
        val textLayers: List<TextLayer>,
        val stickers: List<StickerLayer>,
        val strokes: List<DrawStroke>,
        val options: ExportOptions,
    )

    suspend fun export(
        context: Context,
        request: Request,
        onProgress: (Float) -> Unit,
    ): ExportOutcome = withContext(Dispatchers.Default) {
        val temporaries = mutableListOf<Bitmap>()
        try {
            onProgress(0.02f)
            var base: Bitmap = request.workingSource
            if (request.baseFromOriginal && request.sourceUri != null) {
                val decoded = decodeOriginal(context, request.sourceUri, MAX_ORIGINAL_EDGE)
                if (decoded != null) {
                    temporaries += decoded
                    base = decoded
                }
            }
            val targetEdge = request.options.longEdge.pixels
            if (targetEdge != null && maxOf(base.width, base.height) > targetEdge) {
                val scaled = BitmapTools.scaleToMax(base, targetEdge)
                if (scaled !== base) {
                    temporaries += scaled
                    base = scaled
                }
            }
            currentCoroutineContext().ensureActive()
            onProgress(0.1f)

            val plan = GeometryEngine.plan(base.width, base.height, request.geometry)
            // Check the volume before spending memory on colour and overlay work.
            val compressFormat = if (request.options.format == ExportFormat.PNG) {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }
            if (!ExportManager.hasRoomForImage(context, plan.outputWidth, plan.outputHeight, compressFormat)) {
                return@withContext ExportOutcome.Failure(ExportError.NO_SPACE)
            }
            val geometryOutput = GeometryEngine.render(base, plan)
            // Mutating the editor's working bitmap in place would corrupt the open edit,
            // so a copy is only needed when [base] is still owned by the caller. When the
            // export decoded its own original, it already owns the pixels: this removes a
            // full-resolution copy (about 49 MB for a 12 MP photo) from the export peak.
            val ownsBase = base !== request.workingSource
            var composed: Bitmap
            if (geometryOutput !== base) {
                composed = geometryOutput
                temporaries += composed
            } else if (ownsBase) {
                composed = base
            } else {
                composed = BitmapTools.copyOf(base)
                temporaries += composed
            }

            currentCoroutineContext().ensureActive()
            onProgress(0.2f)
            composed = ColorPipeline.render(
                composed,
                request.adjustments,
                request.filter,
                request.filterIntensity,
                inPlace = true,
            ) { fraction -> onProgress(0.2f + fraction * 0.35f) }
            if (temporaries.none { it === composed }) temporaries += composed
            currentCoroutineContext().ensureActive()
            onProgress(0.62f)

            val canvas = Canvas(composed)
            PhotoOverlayRenderer.render(
                canvas,
                plan,
                request.strokes,
                request.textLayers,
                request.stickers,
            )
            onProgress(0.82f)

            val quality = if (request.options.format == ExportFormat.PNG) 100
            else request.options.quality.coerceIn(60, 100)
            val result = ExportManager.saveImage(
                context = context,
                bitmap = composed,
                fileName = ExportManager.newImageName(),
                format = compressFormat,
                quality = quality,
            )
            onProgress(1f)
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (outOfMemory: OutOfMemoryError) {
            ExportOutcome.Failure(ExportError.ENCODE, outOfMemory)
        } catch (throwable: Throwable) {
            ExportOutcome.Failure(ExportError.UNKNOWN, throwable)
        } finally {
            temporaries.forEach { bitmap -> BitmapTools.safeRecycle(bitmap) }
        }
    }

    /** "Original" means the largest source the editor holds, capped for memory safety. */
    const val MAX_ORIGINAL_EDGE = 3200

    /**
     * Decodes up to [maxLongEdge] pixels of the original file, honouring EXIF orientation and
     * keeping the sample size as small as possible for the requested edge.
     */
    private suspend fun decodeOriginal(context: Context, uri: Uri, maxLongEdge: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxLongEdge) {
                    sample *= 2
                }
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return@withContext null
                val orientation = resolver.openInputStream(uri)?.use { stream ->
                    runCatching {
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL,
                        )
                    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
                BitmapTools.rotateForExif(decoded, orientation)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                null
            }
        }
}
