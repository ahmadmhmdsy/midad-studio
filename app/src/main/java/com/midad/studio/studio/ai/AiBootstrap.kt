package com.midad.studio.studio.ai

import android.content.Context
import com.midad.studio.core.ai.AiServices
import com.midad.studio.core.ai.internal.DefaultModelRepository
import com.midad.studio.core.ai.internal.TfliteSegmenter

/**
 * Wires the on-device AI implementations into AiServices.
 *
 * AiServices asks a factory for an instance on every lookup, so this object creates the
 * repository and the segmenter once and always hands out those shared instances: a per-call
 * repository would split the download state and a per-call segmenter would leak a native
 * interpreter each time it is used. Call it from the application before any studio opens;
 * repeated calls are ignored.
 */
object AiBootstrap {
    private val lock = Any()

    @Volatile
    private var modelRepository: DefaultModelRepository? = null

    @Volatile
    private var subjectSegmenter: TfliteSegmenter? = null

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        synchronized(lock) {
            if (installed) return
            val appContext = context.applicationContext
            val repository = DefaultModelRepository(appContext)
            val segmenter = TfliteSegmenter(appContext)
            AiServices.install(
                { repository },
                { segmenter },
            )
            modelRepository = repository
            subjectSegmenter = segmenter
            installed = true
            repository.refresh()
        }
    }

    /** Shared model catalogue, or null before [install] runs. */
    fun repository(): DefaultModelRepository? = modelRepository

    /** Shared segmenter, or null before [install] runs. */
    fun segmenter(): TfliteSegmenter? = subjectSegmenter

    /**
     * Releases the native inference thread pool and model mapping under memory pressure.
     * The next cut-out reloads the model lazily, so this only costs a short re-open.
     */
    fun releaseInterpreter() {
        runCatching { subjectSegmenter?.close() }
    }
}
