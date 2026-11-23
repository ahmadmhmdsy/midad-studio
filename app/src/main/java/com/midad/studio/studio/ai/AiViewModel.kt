package com.midad.studio.studio.ai

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.midad.studio.core.ai.AiModelStatus
import com.midad.studio.core.ai.AiServices
import com.midad.studio.core.ai.CutoutBackground
import com.midad.studio.core.ai.ModelRepository
import com.midad.studio.core.ai.ModelState
import com.midad.studio.core.ai.SubjectSegmenter
import com.midad.studio.core.ai.internal.DefaultModelRepository
import com.midad.studio.core.ai.internal.ManifestProblem
import com.midad.studio.core.ai.internal.ModelDetails
import com.midad.studio.core.ai.internal.SegmenterFailure
import com.midad.studio.core.ai.internal.TfliteSegmenter
import com.midad.studio.core.draft.AiDraft
import com.midad.studio.core.draft.AiDraftStore
import com.midad.studio.core.export.ExportManager
import com.midad.studio.core.export.ExportOutcome
import com.midad.studio.core.image.BitmapTools
import com.midad.studio.core.project.ProjectKind
import com.midad.studio.core.project.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AiPage { HOME, CUTOUT, MODELS }

enum class CutoutStage { IDLE, OPENING, SEGMENTING, COMPOSITING, SAVING }

/** Backdrops the app can compose locally; the segmenter only needs the contract's four. */
enum class AiBackdrop { TRANSPARENT, BLUR, PAPER, INK, WHITE }

enum class AiError { DECODE, NO_MODEL, MODEL_UNREADABLE, RUNTIME_UNAVAILABLE, INFERENCE, SAVE, LINK }

data class SavedCutout(val fileName: String, val uri: Uri, val inGallery: Boolean)

data class AiUiState(
    val page: AiPage = AiPage.HOME,
    val modelsReturnPage: AiPage = AiPage.HOME,
    val models: List<AiModelStatus> = emptyList(),
    val details: Map<String, ModelDetails> = emptyMap(),
    val catalogueLoading: Boolean = true,
    val catalogueProblem: ManifestProblem? = null,
    val installedCount: Int = 0,
    val source: Bitmap? = null,
    val cutout: Bitmap? = null,
    val preview: Bitmap? = null,
    val previewStyle: AiBackdrop? = null,
    val backdrop: AiBackdrop = AiBackdrop.TRANSPARENT,
    val stage: CutoutStage = CutoutStage.IDLE,
    val error: AiError? = null,
    val saved: SavedCutout? = null,
    val savedDialog: Boolean = false,
    val selectedModelId: String? = null,
    val projectId: String? = null,
    val projectTitle: String? = null,
) {
    val busy: Boolean get() = stage != CutoutStage.IDLE
    val hasModel: Boolean get() = installedCount > 0
    val installedModels: List<AiModelStatus>
        get() = models.filter { it.state == ModelState.INSTALLED }
    val selectedModel: AiModelStatus?
        get() = selectedModelId?.let { id -> models.firstOrNull { it.model.id == id } }
}

/**
 * Screen state and actions for AI Lab. Bitmap work is delegated to [CutoutRenderer] on
 * Dispatchers.Default; model files are owned by the shared repository in [AiBootstrap].
 */
class AiViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository: ModelRepository?
    private val catalogue: DefaultModelRepository?
    private val segmenter: SubjectSegmenter?

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val draftStore = AiDraftStore(appContext)
    private var draftJob: Job? = null
    private var draftEpoch = 0
    private var draftAllowed = true

    init {
        AiBootstrap.install(application)
        repository = AiServices.models(appContext)
        catalogue = repository as? DefaultModelRepository
        segmenter = AiServices.segmenter(appContext)
        observeModels()
        repository?.refresh()
        // Draft recovery is started by the screen, after it knows whether this entry is a saved
        // project or a fresh/recovered session. Doing it here raced openProject() and could leave a
        // reopened project without its title binding.
    }

    // ---- draft recovery ---------------------------------------------------------

    /** Reopens the cut-out workstation after an OS kill; an explicit exit clears the draft. */
    fun restoreDraft() {
        if (!draftStore.hasDraft()) return
        viewModelScope.launch { restore(projectId = null, projectTitle = null) }
    }

    /** Reopens a saved cut-out project; the Projects screen copied its payload into the draft. */
    fun openProject(projectId: String) {
        viewModelScope.launch {
            ProjectRepository.ensureLoaded(appContext)
            val record = ProjectRepository.find(projectId)
            val id = record?.id ?: projectId
            val title = record?.title
            if (!withContext(Dispatchers.IO) { draftStore.hasDraft() }) {
                _uiState.update {
                    it.copy(
                        page = AiPage.CUTOUT,
                        projectId = id,
                        projectTitle = title,
                        error = AiError.DECODE,
                    )
                }
                return@launch
            }
            restore(projectId = id, projectTitle = title)
        }
    }

    private suspend fun restore(projectId: String?, projectTitle: String?) {
        val restored = withContext(Dispatchers.IO) { draftStore.load() }
        if (restored == null) {
            withContext(Dispatchers.IO) { draftStore.clear() }
            return
        }
        val current = _uiState.value
        if (current.page != AiPage.HOME || current.source != null) {
            restored.source.recycle()
            restored.cutout?.recycle()
            return
        }
        val cutout = restored.cutout
        val backdrop = restored.backdrop
        _uiState.update {
            it.copy(
                page = AiPage.CUTOUT,
                source = restored.source,
                cutout = cutout,
                preview = if (cutout == null || backdrop == AiBackdrop.TRANSPARENT) cutout else null,
                previewStyle = if (cutout != null) backdrop.takeIf { style -> style == AiBackdrop.TRANSPARENT } else null,
                backdrop = backdrop,
                stage = CutoutStage.IDLE,
                error = null,
                saved = null,
                projectId = projectId,
                projectTitle = projectTitle,
            )
        }
        if (cutout != null && backdrop != AiBackdrop.TRANSPARENT) {
            _uiState.update { it.copy(stage = CutoutStage.COMPOSITING) }
            val preview = withContext(Dispatchers.Default) {
                runCatching { CutoutRenderer.renderPreview(cutout, restored.source, backdrop) }.getOrNull()
            }
            _uiState.update { state ->
                if (state.source !== restored.source) {
                    state
                } else if (preview != null) {
                    state.copy(preview = preview, previewStyle = backdrop, stage = CutoutStage.IDLE)
                } else {
                    state.copy(
                        backdrop = AiBackdrop.TRANSPARENT,
                        preview = state.cutout,
                        previewStyle = AiBackdrop.TRANSPARENT,
                        stage = CutoutStage.IDLE,
                    )
                }
            }
        }
    }

    /** Leaves AI Lab for good; a saved draft must not resurrect after an explicit exit. */
    fun discardDraft() {
        draftAllowed = false
        draftEpoch++
        draftJob?.cancel()
        draftJob = null
        draftStore.clear()
    }

    private fun saveDraft() {
        if (!draftAllowed) return
        val state = _uiState.value
        val source = state.source ?: return
        val cutout = state.cutout
        val epoch = draftEpoch
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            // The transient draft is enough for crash recovery; a bound project is copied on
            // explicit Save/leave only, because its payload includes the full-size PNGs.
            withContext(Dispatchers.IO) {
                draftStore.save(
                    draft = AiDraft(hasCutout = cutout != null, backdrop = state.backdrop),
                    source = source,
                    cutout = cutout,
                    shouldCommit = { draftAllowed && epoch == draftEpoch },
                )
            }
        }
    }

    /**
     * Flushes the draft and copies it into the projects library. Returns false when the draft or
     * the library write failed; the cut-out on screen is kept either way.
     */
    suspend fun saveProject(title: String): Boolean {
        val state = _uiState.value
        val source = state.source ?: return false
        val cutout = state.cutout
        draftJob?.cancel()
        draftJob = null
        val saved = withContext(Dispatchers.IO) {
            draftStore.save(
                draft = AiDraft(hasCutout = cutout != null, backdrop = state.backdrop),
                source = source,
                cutout = cutout,
            )
        }
        if (!saved) return false
        val thumbnail = state.preview ?: cutout ?: source
        val record = withContext(Dispatchers.IO) {
            ProjectRepository.saveFromDraft(
                context = appContext,
                projectId = state.projectId,
                kind = ProjectKind.AI,
                title = title,
                sourceDraftDir = AiDraftStore.rootDirectory(appContext),
                thumbnail = thumbnail,
            )
        } ?: return false
        _uiState.update { it.copy(projectId = record.id, projectTitle = record.title) }
        return true
    }

    /**
     * Explicit exit. A bound project is flushed one last time before the crash-recovery draft is
     * cleared; without a project this is the old discard path.
     */
    fun leaveStudio(onExit: () -> Unit) {
        val state = _uiState.value
        val projectTitle = state.projectTitle
        if (state.projectId == null || projectTitle == null || state.source == null) {
            discardDraft()
            onExit()
            return
        }
        viewModelScope.launch {
            val flushed = runCatching { saveProject(projectTitle) }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                // Keep the recovery draft when the project write failed, so edits are not lost.
                if (flushed) discardDraft()
                onExit()
            }
        }
    }

    fun openHome() {
        _uiState.update { it.copy(page = AiPage.HOME, error = null) }
    }

    fun openCutout() {
        _uiState.update {
            it.copy(page = AiPage.CUTOUT, error = null, saved = null, savedDialog = false)
        }
    }

    /**
     * Opens the cut-out tool from a model row's Try button. If a photo is already loaded, the
     * cut-out is recomputed with the chosen model right away; otherwise the next picked photo uses
     * it. The choice is shown in the cut-out screen and can be changed there.
     */
    fun tryModel(modelId: String) {
        val state = _uiState.value
        if (state.busy) return
        val status = state.models.firstOrNull { it.model.id == modelId } ?: return
        if (status.state != ModelState.INSTALLED) return
        _uiState.update {
            it.copy(
                page = AiPage.CUTOUT,
                selectedModelId = modelId,
                error = null,
                saved = null,
                savedDialog = false,
            )
        }
        val source = state.source ?: return
        _uiState.update {
            it.copy(cutout = null, preview = null, previewStyle = null, backdrop = AiBackdrop.TRANSPARENT)
        }
        viewModelScope.launch { segment(source) }
    }

    /** Switches the model inside the cut-out screen; null means the automatically ranked model. */
    fun selectModel(modelId: String?) {
        val state = _uiState.value
        if (state.busy || state.selectedModelId == modelId) return
        _uiState.update {
            it.copy(
                selectedModelId = modelId,
                error = null,
                saved = null,
                savedDialog = false,
            )
        }
        val source = state.source ?: return
        _uiState.update {
            it.copy(
                cutout = null,
                preview = null,
                previewStyle = null,
                backdrop = AiBackdrop.TRANSPARENT,
                stage = CutoutStage.IDLE,
            )
        }
        viewModelScope.launch { segment(source) }
    }

    fun openModels() {
        _uiState.update {
            it.copy(
                page = AiPage.MODELS,
                modelsReturnPage = if (it.page == AiPage.CUTOUT) AiPage.CUTOUT else AiPage.HOME,
                error = null,
                savedDialog = false,
            )
        }
    }

    /** Back from the model manager returns to the page that opened it (setup after a failed cut-out). */
    fun leaveModels() {
        _uiState.update { it.copy(page = it.modelsReturnPage, error = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refreshModels() {
        repository?.refresh()
    }

    fun pickImage(uri: Uri) {
        if (_uiState.value.busy) return
        _uiState.update {
            it.copy(
                stage = CutoutStage.OPENING,
                error = null,
                saved = null,
                savedDialog = false,
                source = null,
                cutout = null,
                preview = null,
                previewStyle = null,
                backdrop = AiBackdrop.TRANSPARENT,
            )
        }
        viewModelScope.launch {
            val decoded = BitmapTools.decodeSampled(appContext, uri, MAX_SOURCE_DIMENSION)
            if (decoded == null) {
                _uiState.update { it.copy(stage = CutoutStage.IDLE, error = AiError.DECODE) }
                return@launch
            }
            draftAllowed = true
            _uiState.update { it.copy(source = decoded) }
            saveDraft()
            segment(decoded)
        }
    }

    fun retryCutout() {
        if (_uiState.value.busy) return
        val source = _uiState.value.source ?: return
        _uiState.update {
            it.copy(cutout = null, preview = null, previewStyle = null, saved = null, error = null)
        }
        viewModelScope.launch { segment(source) }
    }

    fun startOver() {
        if (_uiState.value.busy) return
        discardDraft()
        _uiState.update {
            it.copy(
                source = null,
                cutout = null,
                preview = null,
                previewStyle = null,
                backdrop = AiBackdrop.TRANSPARENT,
                saved = null,
                savedDialog = false,
                error = null,
                stage = CutoutStage.IDLE,
            )
        }
    }

    fun selectBackdrop(style: AiBackdrop) {
        val state = _uiState.value
        if (state.busy) return
        val cutout = state.cutout ?: return
        if (state.backdrop == style && state.previewStyle == style) return
        val source = state.source ?: return
        if (style == AiBackdrop.TRANSPARENT) {
            _uiState.update { it.copy(backdrop = style, preview = cutout, previewStyle = style) }
            saveDraft()
            return
        }
        _uiState.update { it.copy(backdrop = style) }
        viewModelScope.launch {
            _uiState.update { it.copy(stage = CutoutStage.COMPOSITING) }
            val preview = withContext(Dispatchers.Default) {
                runCatching { CutoutRenderer.renderPreview(cutout, source, style) }.getOrNull()
            }
            _uiState.update { current ->
                if (current.backdrop != style) {
                    current.copy(stage = CutoutStage.IDLE)
                } else if (preview != null) {
                    current.copy(preview = preview, previewStyle = style, stage = CutoutStage.IDLE)
                } else {
                    // Composing failed (e.g. out of memory): keep a truthful preview instead of crashing.
                    current.copy(
                        backdrop = AiBackdrop.TRANSPARENT,
                        preview = current.cutout,
                        previewStyle = AiBackdrop.TRANSPARENT,
                        stage = CutoutStage.IDLE,
                    )
                }
            }
            saveDraft()
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.busy) return
        val cutout = state.cutout ?: return
        val source = state.source ?: return
        val style = state.backdrop
        _uiState.update {
            it.copy(stage = CutoutStage.SAVING, error = null, saved = null, savedDialog = false)
        }
        viewModelScope.launch {
            val format = if (style == AiBackdrop.TRANSPARENT) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val composed = withContext(Dispatchers.Default) {
                runCatching { CutoutRenderer.renderFull(cutout, source, style) }.getOrNull()
            }
            if (composed == null) {
                _uiState.update { it.copy(stage = CutoutStage.IDLE, error = AiError.SAVE) }
                return@launch
            }
            val name = ExportManager.newImageName("Midad_Cutout")
            val outcome = ExportManager.saveImage(appContext, composed, name, format, JPEG_QUALITY)
            if (composed !== cutout) BitmapTools.safeRecycle(composed)
            when (outcome) {
                is ExportOutcome.Failure -> {
                    _uiState.update { it.copy(stage = CutoutStage.IDLE, error = AiError.SAVE) }
                }
                is ExportOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            stage = CutoutStage.IDLE,
                            saved = SavedCutout(
                                outcome.result.fileName,
                                outcome.result.uri,
                                outcome.result.inGallery,
                            ),
                            savedDialog = true,
                        )
                    }
                }
            }
        }
    }

    fun shareSaved() {
        val saved = _uiState.value.saved ?: return
        val mime = if (saved.fileName.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg"
        ExportManager.share(appContext, saved.uri, mime)
    }

    fun openSaved() {
        val saved = _uiState.value.saved ?: return
        val mime = if (saved.fileName.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg"
        ExportManager.open(appContext, saved.uri, mime)
    }

    fun dismissSavedDialog() {
        if (_uiState.value.savedDialog) _uiState.update { it.copy(savedDialog = false) }
    }

    fun openLink(url: String) {
        if (url.isBlank()) {
            _uiState.update { it.copy(error = AiError.LINK) }
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = runCatching { appContext.startActivity(intent) }.isSuccess
        if (!opened) _uiState.update { it.copy(error = AiError.LINK) }
    }

    fun downloadModel(modelId: String) {
        repository?.download(modelId)
    }

    fun cancelModel(modelId: String) {
        repository?.cancel(modelId)
    }

    fun removeModel(modelId: String) {
        repository?.remove(modelId)
    }

    private fun observeModels() {
        val repository = repository ?: return
        viewModelScope.launch {
            repository.statuses.collect { statuses ->
                val installed = statuses.count { it.state == ModelState.INSTALLED }
                _uiState.update { current ->
                    val selection = current.selectedModelId
                    val stillInstalled = selection == null || statuses.any {
                        it.model.id == selection && it.state == ModelState.INSTALLED
                    }
                    current.copy(
                        models = statuses,
                        installedCount = installed,
                        selectedModelId = if (stillInstalled) selection else null,
                    )
                }
            }
        }
        val concrete = catalogue ?: return
        viewModelScope.launch {
            concrete.catalogueReady.collect { ready ->
                _uiState.update { current ->
                    current.copy(
                        catalogueLoading = !ready,
                        details = if (ready) concrete.allDetails() else current.details,
                    )
                }
            }
        }
        viewModelScope.launch {
            concrete.manifestProblem.collect { problem ->
                _uiState.update { it.copy(catalogueProblem = problem) }
            }
        }
    }

    private suspend fun segment(source: Bitmap) {
        val engine = segmenter
        if (engine == null) {
            _uiState.update { it.copy(stage = CutoutStage.IDLE, error = AiError.RUNTIME_UNAVAILABLE) }
            return
        }
        _uiState.update { it.copy(stage = CutoutStage.SEGMENTING) }
        val result = engine.cutout(
            appContext,
            source,
            CutoutBackground.TRANSPARENT,
            _uiState.value.selectedModelId,
        )
        if (result == null) {
            _uiState.update { it.copy(stage = CutoutStage.IDLE, error = failureFor(engine)) }
            return
        }
        _uiState.update { it.copy(stage = CutoutStage.COMPOSITING) }
        val preview = withContext(Dispatchers.Default) {
            runCatching { CutoutRenderer.renderPreview(result, source, AiBackdrop.TRANSPARENT) }.getOrNull()
        } ?: result
        _uiState.update {
            it.copy(
                cutout = result,
                preview = preview,
                previewStyle = AiBackdrop.TRANSPARENT,
                backdrop = AiBackdrop.TRANSPARENT,
                stage = CutoutStage.IDLE,
                error = null,
            )
        }
        saveDraft()
    }

    private fun failureFor(engine: SubjectSegmenter): AiError = when ((engine as? TfliteSegmenter)?.lastFailure) {
        SegmenterFailure.NO_MODEL -> AiError.NO_MODEL
        SegmenterFailure.MODEL_UNREADABLE -> AiError.MODEL_UNREADABLE
        SegmenterFailure.RUNTIME_UNAVAILABLE -> AiError.RUNTIME_UNAVAILABLE
        SegmenterFailure.UNSUPPORTED_MODEL -> AiError.MODEL_UNREADABLE
        SegmenterFailure.INFERENCE_FAILED, null -> AiError.INFERENCE
    }

    override fun onCleared() {
        super.onCleared()
        draftJob?.cancel()
    }

    private companion object {
        const val MAX_SOURCE_DIMENSION = 1800
        const val JPEG_QUALITY = 95
    }
}
