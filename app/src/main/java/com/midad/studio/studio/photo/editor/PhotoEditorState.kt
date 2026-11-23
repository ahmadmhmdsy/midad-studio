package com.midad.studio.studio.photo.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import com.midad.studio.R
import com.midad.studio.core.export.ExportError
import com.midad.studio.core.export.ExportResult
import com.midad.studio.studio.photo.engine.Adjustments
import com.midad.studio.studio.photo.engine.CropAspect
import com.midad.studio.studio.photo.engine.CropRect
import com.midad.studio.studio.photo.engine.DrawStroke
import com.midad.studio.studio.photo.engine.ExportOptions
import com.midad.studio.studio.photo.engine.GeometryPlan
import com.midad.studio.studio.photo.engine.GeometryState
import com.midad.studio.studio.photo.engine.PhotoFilter
import com.midad.studio.studio.photo.engine.PhotoFilters
import com.midad.studio.studio.photo.engine.StickerLayer
import com.midad.studio.studio.photo.engine.TextLayer

enum class EditorStage { EMPTY, LOADING, READY, ERROR }

/** The editor tools shown in the bottom rail. */
enum class PhotoTool(@StringRes val labelRes: Int) {
    ADJUST(R.string.p_tool_adjust),
    FILTERS(R.string.p_tool_filters),
    CROP(R.string.p_tool_crop),
    TEXT(R.string.p_tool_text),
    STICKERS(R.string.p_tool_stickers),
    DRAW(R.string.p_tool_draw),
}

/** The adjust panel's three groups; each group resets on its own. */
enum class AdjustGroup(@StringRes val labelRes: Int, val keys: List<AdjustKey>) {
    LIGHT(
        R.string.p_adj_group_light,
        listOf(AdjustKey.EXPOSURE, AdjustKey.BRIGHTNESS, AdjustKey.CONTRAST, AdjustKey.HIGHLIGHTS, AdjustKey.SHADOWS),
    ),
    COLOR(
        R.string.p_adj_group_color,
        listOf(AdjustKey.SATURATION, AdjustKey.WARMTH, AdjustKey.TINT),
    ),
    DETAIL(
        R.string.p_adj_group_detail,
        listOf(AdjustKey.SHARPEN, AdjustKey.BLUR, AdjustKey.VIGNETTE, AdjustKey.GRAIN),
    ),
}

/** Identifies one adjustment slider and how to read/write it. */
enum class AdjustKey(
    @StringRes val labelRes: Int,
    val min: Float,
    val max: Float,
    val isExposure: Boolean = false,
) {
    EXPOSURE(R.string.p_adj_exposure, -2f, 2f, true),
    BRIGHTNESS(R.string.p_adj_brightness, -100f, 100f),
    CONTRAST(R.string.p_adj_contrast, -100f, 100f),
    SATURATION(R.string.p_adj_saturation, -100f, 100f),
    WARMTH(R.string.p_adj_warmth, -100f, 100f),
    TINT(R.string.p_adj_tint, -100f, 100f),
    HIGHLIGHTS(R.string.p_adj_highlights, -100f, 100f),
    SHADOWS(R.string.p_adj_shadows, -100f, 100f),
    VIGNETTE(R.string.p_adj_vignette, 0f, 100f),
    GRAIN(R.string.p_adj_grain, 0f, 100f),
    SHARPEN(R.string.p_adj_sharpen, 0f, 100f),
    BLUR(R.string.p_adj_blur, 0f, 100f);

    fun read(adjustments: Adjustments): Float = when (this) {
        EXPOSURE -> adjustments.exposure
        BRIGHTNESS -> adjustments.brightness.toFloat()
        CONTRAST -> adjustments.contrast.toFloat()
        SATURATION -> adjustments.saturation.toFloat()
        WARMTH -> adjustments.warmth.toFloat()
        TINT -> adjustments.tint.toFloat()
        HIGHLIGHTS -> adjustments.highlights.toFloat()
        SHADOWS -> adjustments.shadows.toFloat()
        VIGNETTE -> adjustments.vignette.toFloat()
        GRAIN -> adjustments.grain.toFloat()
        SHARPEN -> adjustments.sharpen.toFloat()
        BLUR -> adjustments.blur.toFloat()
    }

    fun write(adjustments: Adjustments, value: Float): Adjustments {
        val rounded = if (isExposure) value else value.toInt().toFloat()
        return when (this) {
            EXPOSURE -> adjustments.copy(exposure = rounded)
            BRIGHTNESS -> adjustments.copy(brightness = rounded.toInt())
            CONTRAST -> adjustments.copy(contrast = rounded.toInt())
            SATURATION -> adjustments.copy(saturation = rounded.toInt())
            WARMTH -> adjustments.copy(warmth = rounded.toInt())
            TINT -> adjustments.copy(tint = rounded.toInt())
            HIGHLIGHTS -> adjustments.copy(highlights = rounded.toInt())
            SHADOWS -> adjustments.copy(shadows = rounded.toInt())
            VIGNETTE -> adjustments.copy(vignette = rounded.toInt())
            GRAIN -> adjustments.copy(grain = rounded.toInt())
            SHARPEN -> adjustments.copy(sharpen = rounded.toInt())
            BLUR -> adjustments.copy(blur = rounded.toInt())
        }
    }
}

/** A full undoable snapshot of the document. Bitmap references are shared, never copied. */
data class EditorSnapshot(
    val source: Bitmap?,
    val previewBase: Bitmap?,
    val baseFromOriginal: Boolean,
    val adjustments: Adjustments,
    val filterIndex: Int?,
    val filterIntensity: Int,
    val geometry: GeometryState,
    val textLayers: List<TextLayer>,
    val stickers: List<StickerLayer>,
    val strokes: List<DrawStroke>,
)

data class PhotoEditorState(
    val stage: EditorStage = EditorStage.EMPTY,
    val sourceUri: Uri? = null,
    val source: Bitmap? = null,
    val previewBase: Bitmap? = null,
    val baseFromOriginal: Boolean = false,
    /** The colour-adjusted frame currently shown on the stage. */
    val previewFrame: Bitmap? = null,
    val plan: GeometryPlan? = null,
    val renderingPreview: Boolean = false,
    val activeTool: PhotoTool? = null,
    val adjustments: Adjustments = Adjustments(),
    val filterIndex: Int? = null,
    val filterIntensity: Int = 100,
    val geometry: GeometryState = GeometryState(),
    val textLayers: List<TextLayer> = emptyList(),
    val selectedTextId: Long? = null,
    val stickers: List<StickerLayer> = emptyList(),
    val selectedStickerId: Long? = null,
    val strokes: List<DrawStroke> = emptyList(),
    val draftStroke: DrawStroke? = null,
    val brushColor: Int = PhotoPalette.White,
    val brushWidth: Float = 0.035f,
    val erasing: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val aiAvailable: Boolean = false,
    val aiBusy: Boolean = false,
    val aiHint: Boolean = false,
    val exportDialogOpen: Boolean = false,
    val exportOptions: ExportOptions = ExportOptions(),
    val exporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportFailure: ExportError? = null,
    val exportResult: ExportResult? = null,
    /** True when the editor reopened an unfinished edit after the process died. */
    val recoveredDraft: Boolean = false,
    /** The saved project this edit belongs to; null until the first save. */
    val projectId: String? = null,
    val projectTitle: String? = null,
) {
    val activeFilter: PhotoFilter? get() = PhotoFilters.at(filterIndex)

    val selectedText: TextLayer? get() = selectedTextId?.let { id -> textLayers.firstOrNull { it.id == id } }

    val selectedSticker: StickerLayer? get() = selectedStickerId?.let { id -> stickers.firstOrNull { it.id == id } }

    val hasEdits: Boolean
        get() = adjustments != Adjustments() || filterIndex != null || !geometry.isIdentity ||
            textLayers.isNotEmpty() || stickers.isNotEmpty() || strokes.isNotEmpty() ||
            !baseFromOriginal

    /** Convenience for panels that reset a single tool. */
    val isCropAspectFree: Boolean get() = geometry.aspect == CropAspect.FREE

    val normalizedCrop: CropRect get() = geometry.crop
}
