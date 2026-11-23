package com.midad.studio.core.ai

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

/**
 * Contracts between the AI Lab and the other studios.
 * The implementation lives in com.midad.studio.studio.ai and registers itself through [AiServices].
 */

enum class ModelState { NOT_INSTALLED, DOWNLOADING, VERIFYING, INSTALLED, BUNDLED, FAILED }

data class AiModel(
    val id: String,
    val displayName: String,
    val description: String,
    val licenseName: String,
    val licenseUrl: String,
    val sizeBytes: Long,
    val bundled: Boolean,
)

data class AiModelStatus(
    val model: AiModel,
    val state: ModelState,
    val progress: Float = 0f,
    val message: String? = null,
)

interface ModelRepository {
    val statuses: StateFlow<List<AiModelStatus>>
    fun refresh()
    fun download(modelId: String)
    fun cancel(modelId: String)
    fun remove(modelId: String)
    fun isReady(modelId: String): Boolean
}

enum class CutoutBackground { TRANSPARENT, BLUR, LIGHT_SOLID, DARK_SOLID }

interface SubjectSegmenter {
    /**
     * Returns an ARGB bitmap the same size as [source] where the chosen subject is opaque
     * and everything else is transparent, blurred or replaced with a solid backdrop.
     */
    suspend fun cutout(
        context: Context,
        source: Bitmap,
        background: CutoutBackground = CutoutBackground.TRANSPARENT,
        modelId: String? = null,
    ): Bitmap?
    fun close()
}

object AiServices {

    @Volatile
    private var repositoryFactory: ((Context) -> ModelRepository)? = null

    @Volatile
    private var segmenterFactory: ((Context) -> SubjectSegmenter)? = null

    fun install(
        repository: (Context) -> ModelRepository,
        segmenter: (Context) -> SubjectSegmenter,
    ) {
        repositoryFactory = repository
        segmenterFactory = segmenter
    }

    fun models(context: Context): ModelRepository? = repositoryFactory?.invoke(context)

    fun segmenter(context: Context): SubjectSegmenter? = segmenterFactory?.invoke(context)
}
