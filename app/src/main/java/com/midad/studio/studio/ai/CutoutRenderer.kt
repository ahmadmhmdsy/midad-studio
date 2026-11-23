package com.midad.studio.studio.ai

import android.graphics.Bitmap
import android.graphics.Color
import com.midad.studio.core.ai.internal.MaskComposer
import com.midad.studio.core.image.BitmapTools

/**
 * Composes the transparent cut-out over the chosen backdrop for the preview and for the file
 * the user saves. Backdrop colours are fixed artwork colours (they are named colours: paper,
 * ink, white), not theme tokens, so a saved file looks the same in light and dark themes.
 */
object CutoutRenderer {

    private const val PREVIEW_MAX = 1400
    private val PAPER = 0xFFF4EFE5.toInt()
    private val INK = 0xFF1C1915.toInt()

    fun renderPreview(cutout: Bitmap, source: Bitmap, backdrop: AiBackdrop): Bitmap {
        if (backdrop == AiBackdrop.TRANSPARENT) return cutout
        val previewCutout = BitmapTools.scaleToFit(cutout, PREVIEW_MAX, PREVIEW_MAX)
        val previewSource = BitmapTools.scaleToFit(source, PREVIEW_MAX, PREVIEW_MAX)
        val rendered = compose(previewCutout, previewSource, backdrop)
        if (previewCutout !== cutout) BitmapTools.safeRecycle(previewCutout)
        if (previewSource !== source) BitmapTools.safeRecycle(previewSource)
        return rendered
    }

    fun renderFull(cutout: Bitmap, source: Bitmap, backdrop: AiBackdrop): Bitmap =
        compose(cutout, source, backdrop)

    private fun compose(cutout: Bitmap, source: Bitmap, backdrop: AiBackdrop): Bitmap = when (backdrop) {
        AiBackdrop.TRANSPARENT -> cutout
        AiBackdrop.BLUR -> MaskComposer.overBlur(cutout, source)
        AiBackdrop.PAPER -> MaskComposer.overSolid(cutout, PAPER)
        AiBackdrop.INK -> MaskComposer.overSolid(cutout, INK)
        AiBackdrop.WHITE -> MaskComposer.overSolid(cutout, Color.WHITE)
    }
}
