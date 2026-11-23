package com.midad.studio.studio.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the display size of a video so the output canvas can follow the first clip. */
internal object VideoProbe {

    suspend fun size(context: Context, uri: Uri): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val width = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                val height = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
                val rotation = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull() ?: 0
                if (width <= 0 || height <= 0) {
                    null
                } else if (rotation == 90 || rotation == 270) {
                    height to width
                } else {
                    width to height
                }
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()
    }
}
