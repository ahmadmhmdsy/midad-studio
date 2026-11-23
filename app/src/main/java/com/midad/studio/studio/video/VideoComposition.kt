package com.midad.studio.studio.video

import android.graphics.Bitmap
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects

/**
 * The one place a video project is turned into a Media3 [Composition]. Both the exporter and the
 * in-app preview call this, so what the preview plays is the same sequence, trim, colour look,
 * output frame and title overlay that the exporter writes.
 */
internal object VideoComposition {

    fun build(spec: VideoExportSpec, titleBitmap: Bitmap?): Composition {
        val lookEffects = VideoEffects.lookEffects(spec.look)
        val frameEffects = VideoEffects.frameEffects(spec, titleBitmap)
        val items = spec.clips.map { clip ->
            val mediaItem = buildVideoMediaItem(clip.uri, clip.startMs, clip.endMs)
            EditedMediaItem.Builder(mediaItem)
                // CompositionPlayer needs an explicit duration for every item; Transformer derives
                // it itself, so declaring it here is safe for both consumers.
                .setDurationUs((clip.endMs - clip.startMs).coerceAtLeast(0L) * 1_000L)
                .setEffects(Effects(emptyList<AudioProcessor>(), lookEffects + frameEffects))
                .setRemoveAudio(spec.muted)
                .build()
        }
        val sequence = EditedMediaItemSequence.Builder(items).build()
        return Composition.Builder(listOf(sequence)).build()
    }
}
