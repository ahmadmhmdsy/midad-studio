package com.midad.studio.core.export

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.midad.studio.R
import com.midad.studio.core.media.MediaVideoHelper
import com.midad.studio.core.project.ProjectStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExportResult(
    val uri: Uri,
    val fileName: String,
    val inGallery: Boolean,
)

/** Saves finished artwork to the device gallery (or app storage on legacy devices). */
object ExportManager {

    fun newImageName(prefix: String = "Midad"): String =
        prefix + "_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    fun newVideoName(prefix: String = "Midad"): String =
        prefix + "_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    /** Whether the target volume has room for an image of this size. */
    fun hasRoomForImage(context: Context, width: Int, height: Int, format: Bitmap.CompressFormat): Boolean =
        ExportStorage.hasRoom(context, ExportSizing.requiredImageBytes(width, height, format == Bitmap.CompressFormat.PNG))

    suspend fun saveImage(
        context: Context,
        bitmap: Bitmap,
        fileName: String = newImageName(),
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95,
    ): ExportOutcome = withContext(Dispatchers.IO) {
        if (bitmap.isRecycled) return@withContext ExportOutcome.Failure(ExportError.ENCODE)
        if (!bitmap.hasContent()) return@withContext ExportOutcome.Failure(ExportError.ENCODE)
        val required = ExportSizing.requiredImageBytes(bitmap.width, bitmap.height, format == Bitmap.CompressFormat.PNG)
        if (!ExportStorage.hasRoom(context, required)) {
            return@withContext ExportOutcome.Failure(ExportError.NO_SPACE)
        }
        try {
            val isPng = format == Bitmap.CompressFormat.PNG
            val mime = if (isPng) "image/png" else "image/jpeg"
            val extension = if (isPng) "png" else "jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImageModern(context, bitmap, fileName, extension, mime, format, quality)
            } else {
                saveImageLegacy(context, bitmap, fileName, extension, format, quality)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            ExportOutcome.Failure(classify(throwable), throwable)
        }
    }

    private suspend fun saveImageModern(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        extension: String,
        mime: String,
        format: Bitmap.CompressFormat,
        quality: Int,
    ): ExportOutcome {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + "." + extension)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Midad")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return ExportOutcome.Failure(ExportError.IO)
        try {
            val stream = resolver.openOutputStream(uri)
                ?: throw IOException("MediaStore returned no output stream")
            stream.use { output ->
                if (!bitmap.compress(format, quality, output)) {
                    throw IOException("Bitmap.compress returned false")
                }
            }
            val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, publish, null, null)
            val result = ExportResult(uri, fileName + "." + extension, true)
            runCatching { ProjectStore.recordExport(context, uri, ProjectStore.KIND_PHOTO, result.fileName, bitmap) }
            return ExportOutcome.Success(result)
        } catch (throwable: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            if (throwable is CancellationException) throw throwable
            return ExportOutcome.Failure(classify(throwable), throwable)
        }
    }

    private suspend fun saveImageLegacy(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        extension: String,
        format: Bitmap.CompressFormat,
        quality: Int,
    ): ExportOutcome {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        if (!dir.exists() && !dir.mkdirs()) return ExportOutcome.Failure(ExportError.IO)
        val file = File(dir, fileName + "." + extension)
        try {
            FileOutputStream(file).use { stream ->
                if (!bitmap.compress(format, quality, stream)) {
                    throw IOException("Bitmap.compress returned false")
                }
            }
            val uri = shareableUri(context, file)
            val result = ExportResult(uri, file.name, false)
            runCatching { ProjectStore.recordExport(context, uri, ProjectStore.KIND_PHOTO, result.fileName, bitmap) }
            return ExportOutcome.Success(result)
        } catch (throwable: Throwable) {
            runCatching { file.delete() }
            return ExportOutcome.Failure(classify(throwable), throwable)
        }
    }

    suspend fun saveVideo(
        context: Context,
        sourceFile: File,
        fileName: String = newVideoName(),
    ): ExportOutcome = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            return@withContext ExportOutcome.Failure(ExportError.ENCODE)
        }
        val required = ExportSizing.requiredVideoBytes(sourceFile.length())
        if (!ExportStorage.hasRoom(context, required)) {
            return@withContext ExportOutcome.Failure(ExportError.NO_SPACE)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveVideoModern(context, sourceFile, fileName)
            } else {
                saveVideoLegacy(context, sourceFile, fileName)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            ExportOutcome.Failure(classify(throwable), throwable)
        }
    }

    private suspend fun saveVideoModern(
        context: Context,
        sourceFile: File,
        fileName: String,
    ): ExportOutcome {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + ".mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Midad")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return ExportOutcome.Failure(ExportError.IO)
        try {
            val stream = resolver.openOutputStream(uri)
                ?: throw IOException("MediaStore returned no output stream")
            stream.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            }
            val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, publish, null, null)
            val result = ExportResult(uri, fileName + ".mp4", true)
            runCatching {
                ProjectStore.recordExport(
                    context, uri, ProjectStore.KIND_VIDEO, result.fileName,
                    MediaVideoHelper.thumbnailFromFile(sourceFile),
                )
            }
            return ExportOutcome.Success(result)
        } catch (throwable: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            if (throwable is CancellationException) throw throwable
            return ExportOutcome.Failure(classify(throwable), throwable)
        }
    }

    private suspend fun saveVideoLegacy(
        context: Context,
        sourceFile: File,
        fileName: String,
    ): ExportOutcome {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        if (!dir.exists() && !dir.mkdirs()) return ExportOutcome.Failure(ExportError.IO)
        val output = File(dir, fileName + ".mp4")
        try {
            sourceFile.copyTo(output, overwrite = true)
            val uri = shareableUri(context, output)
            val result = ExportResult(uri, output.name, false)
            runCatching {
                ProjectStore.recordExport(
                    context, uri, ProjectStore.KIND_VIDEO, result.fileName,
                    MediaVideoHelper.thumbnailFromFile(output),
                )
            }
            return ExportOutcome.Success(result)
        } catch (throwable: Throwable) {
            runCatching { output.delete() }
            return ExportOutcome.Failure(classify(throwable), throwable)
        }
    }

    /**
     * Deletes this app's MediaStore rows that were inserted as pending but never
     * published (the process can die between insert and update). Called once at
     * startup; only rows owned by this package are touched.
     */
    fun cleanupPendingExports(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val resolver = context.contentResolver
        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
        for (collection in collections) {
            runCatching {
                resolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    MediaStore.MediaColumns.IS_PENDING + "=1 AND " +
                        MediaStore.MediaColumns.OWNER_PACKAGE_NAME + "=?",
                    arrayOf(context.packageName),
                    null,
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        runCatching {
                            resolver.delete(ContentUris.withAppendedId(collection, id), null, null)
                        }
                    }
                }
            }
        }
    }

    private fun classify(throwable: Throwable): ExportError {
        if (throwable is OutOfMemoryError) return ExportError.ENCODE
        val message = throwable.message.orEmpty()
        val noSpace = message.contains("ENOSPC", ignoreCase = true) ||
            message.contains("No space", ignoreCase = true) ||
            message.contains("not enough space", ignoreCase = true)
        return when {
            noSpace -> ExportError.NO_SPACE
            throwable is IOException -> ExportError.IO
            throwable is IllegalStateException -> ExportError.ENCODE
            else -> ExportError.UNKNOWN
        }
    }

    fun share(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /**
     * Opens a finished export with whatever app handles its mime type. Returns false and explains
     * the fallback when no viewer exists, so the success dialog can keep Share available.
     */
    fun open(context: Context, uri: Uri, mimeType: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (error: Exception) {
            Toast.makeText(context, R.string.export_open_no_app, Toast.LENGTH_LONG).show()
            false
        }
    }

    fun mimeTypeFor(kind: String): String =
        if (kind == ProjectStore.KIND_VIDEO) "video/mp4" else "image/jpeg"

    /** Mime type derived from the exported file name so PNG stays image/png when shared. */
    fun mimeTypeFor(fileName: String, kind: String): String = when {
        fileName.endsWith(".png", ignoreCase = true) -> "image/png"
        fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
        fileName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
        fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
        else -> mimeTypeFor(kind)
    }

    /** Legacy exports live in app storage; wrap them so other apps can read via FileProvider. */
    private fun shareableUri(context: Context, file: File): Uri = runCatching {
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }.getOrElse { Uri.fromFile(file) }

    private fun Bitmap.hasContent(): Boolean = width > 0 && height > 0
}

/** Filesystem room check; unknown stats must never block an export. */
internal object ExportStorage {
    fun freeBytes(context: Context): Long {
        val path = (context.getExternalFilesDir(null) ?: context.filesDir).absolutePath
        return runCatching { StatFs(path).availableBytes }.getOrDefault(Long.MAX_VALUE)
    }

    fun hasRoom(context: Context, requiredBytes: Long): Boolean {
        if (requiredBytes <= 0L) return true
        return freeBytes(context) >= requiredBytes
    }
}
