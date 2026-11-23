package com.midad.studio.studio.video

import android.graphics.Bitmap
import androidx.media3.common.Effect
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextureOverlay

/**
 * Stable Media3 effects for the export pipeline. Every look is built from first-party effect
 * classes, so the studio ships no LUT files, presets or downloaded assets.
 */
internal object VideoEffects {

    fun lookEffects(look: VideoLook): List<Effect> = when (look) {
        VideoLook.NONE -> emptyList()
        VideoLook.WARM -> listOf<Effect>(
            RgbAdjustment.Builder()
                .setRedScale(1.08f)
                .setGreenScale(1.01f)
                .setBlueScale(0.88f)
                .build(),
            HslAdjustment.Builder()
                .adjustSaturation(8f)
                .build(),
        )
        VideoLook.COOL -> listOf<Effect>(
            RgbAdjustment.Builder()
                .setRedScale(0.92f)
                .setGreenScale(1.00f)
                .setBlueScale(1.10f)
                .build(),
            HslAdjustment.Builder()
                .adjustSaturation(-4f)
                .build(),
        )
        VideoLook.NOIR -> listOf<Effect>(
            RgbFilter.createGrayscaleFilter(),
            // Media3 documents Contrast in [-1, 1]; values above 1 throw at runtime.
            Contrast(1f),
        )
        VideoLook.PUNCH -> listOf<Effect>(
            Contrast(1f),
            HslAdjustment.Builder()
                .adjustSaturation(18f)
                .build(),
        )
    }

    /**
     * Frame effects shared by every clip: one common canvas, then the optional title overlay.
     * ScaleAndRotateTransformation keeps the matrix stage explicit; its identity values are a
     * no-op for Media3, so the exported frame size stays exactly the requested canvas size.
     */
    fun frameEffects(spec: VideoExportSpec, titleBitmap: Bitmap?): List<Effect> {
        val effects = mutableListOf<Effect>()
        effects += Presentation.createForWidthAndHeight(
            spec.outputWidth,
            spec.outputHeight,
            presentationLayout(spec.fit),
        )
        effects += ScaleAndRotateTransformation.Builder()
            .setScale(1f, 1f)
            .setRotationDegrees(0f)
            .build()
        if (titleBitmap != null) {
            val overlay = BitmapOverlay.createStaticBitmapOverlay(titleBitmap)
            effects += OverlayEffect(listOf<TextureOverlay>(overlay))
        }
        return effects
    }

    private fun presentationLayout(fit: OutputFit): Int = when (fit) {
        OutputFit.FIT -> Presentation.LAYOUT_SCALE_TO_FIT
        else -> Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
    }
}
