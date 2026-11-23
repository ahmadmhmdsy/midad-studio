package com.midad.studio.studio.video

import android.net.Uri
import androidx.media3.common.MediaItem

/** One media item plus the required trim, shared by the preview and the exporter. */
internal fun buildVideoMediaItem(uri: Uri, startMs: Long, endMs: Long): MediaItem =
    MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs.coerceAtLeast(0L))
                .setEndPositionMs(endMs)
                .build(),
        )
        .build()
