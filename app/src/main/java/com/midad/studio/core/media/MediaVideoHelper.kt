package com.midad.studio.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.midad.studio.core.image.BitmapTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaVideoHelper {

    suspend fun thumbnail(context: Context, uri: Uri, maxSizePx: Int = 512): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    extractFrame(retriever, maxSizePx)
                } finally {
                    runCatching { retriever.release() }
                }
            }.getOrNull()
        }

    fun thumbnailFromFile(file: File, maxSizePx: Int = 512): Bitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            extractFrame(retriever, maxSizePx)
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()

    suspend fun durationMs(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrDefault(0L)
    }

    fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()

    private fun extractFrame(retriever: MediaMetadataRetriever, maxSizePx: Int): Bitmap? {
        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.frameAtTime
        if (frame == null) return null
        val scaled = BitmapTools.toArgb8888(BitmapTools.scaleToMax(frame, maxSizePx))
        if (scaled !== frame) BitmapTools.safeRecycle(frame)
        return scaled
    }
}
