package com.midad.studio.core.draft

import com.midad.studio.studio.design.DesignScreen
import com.midad.studio.studio.design.collage.CollageAspect
import com.midad.studio.studio.design.model.DesignDocument

/** Collage workspace values; the pixels are stored as numbered PNG files next to this. */
data class CollageDraft(
    val images: List<String>,
    val layoutId: String,
    val aspect: CollageAspect,
    val spacing: Int,
    val background: Long,
)

/** A recoverable design session. The document is the same immutable model the editor uses. */
data class DesignDraft(
    val screen: DesignScreen,
    val document: DesignDocument?,
    val collage: CollageDraft?,
)
