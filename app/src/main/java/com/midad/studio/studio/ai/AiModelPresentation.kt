package com.midad.studio.studio.ai

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.midad.studio.R
import com.midad.studio.core.ai.AiModelStatus

/**
 * Human-facing names for the models this build ships. The manifest keeps the technical file
 * name and provenance; the studio should talk about what a model does, not what it is called.
 * Unknown catalogue entries fall back to the manifest text so a future model still renders.
 */
private val modelCopy: Map<String, Pair<Int, Int>> = mapOf(
    "mediapipe-selfie-segmenter-256-f16" to
        (R.string.a_model_selfie_title to R.string.a_model_selfie_desc),
    "mediapipe-selfie-segmenter-landscape-256x144-f16" to
        (R.string.a_model_selfie_landscape_title to R.string.a_model_selfie_landscape_desc),
    "mediapipe-selfie-multiclass-256-f32" to
        (R.string.a_model_multiclass_title to R.string.a_model_multiclass_desc),
    "mediapipe-hair-segmenter-512-f32" to
        (R.string.a_model_hair_title to R.string.a_model_hair_desc),
    "mediapipe-deeplab-v3-257-f32" to
        (R.string.a_model_deeplab_title to R.string.a_model_deeplab_desc),
    "modnet-portrait-matting-512-f32" to
        (R.string.a_model_modnet_title to R.string.a_model_modnet_desc),
)

@Composable
fun modelTitle(status: AiModelStatus): String = localizedCopy(status, title = true)

@Composable
fun modelDescription(status: AiModelStatus): String = localizedCopy(status, title = false)

@Composable
private fun localizedCopy(status: AiModelStatus, title: Boolean): String {
    val entry = modelCopy[status.model.id]
    if (entry == null) {
        return if (title) status.model.displayName else status.model.description
    }
    @StringRes val res = if (title) entry.first else entry.second
    return stringResource(res)
}
