package com.midad.studio.core.draft

import com.midad.studio.studio.photo.engine.Adjustments
import com.midad.studio.studio.photo.engine.CropAspect
import com.midad.studio.studio.photo.engine.CropRect
import com.midad.studio.studio.photo.engine.DrawStroke
import com.midad.studio.studio.photo.engine.GeometryState
import com.midad.studio.studio.photo.engine.StickerLayer
import com.midad.studio.studio.photo.engine.StickerShape
import com.midad.studio.studio.photo.engine.StrokePoint
import com.midad.studio.studio.photo.engine.TextLayer

/** A recoverable photo edit. Only plain values: the pixels live next to this record. */
data class PhotoDraft(
    val sourceUri: String?,
    val baseFromOriginal: Boolean,
    val sourcePng: Boolean,
    val adjustments: Adjustments,
    val filterIndex: Int?,
    val filterIntensity: Int,
    val geometry: GeometryState,
    val textLayers: List<TextLayer>,
    val stickers: List<StickerLayer>,
    val strokes: List<DrawStroke>,
    val nextLayerId: Long,
)
