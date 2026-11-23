package com.midad.studio.studio.photo.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.midad.studio.R
import com.midad.studio.core.ai.AiServices
import com.midad.studio.core.ai.CutoutBackground
import com.midad.studio.core.ai.SubjectSegmenter
import com.midad.studio.core.draft.PhotoDraft
import com.midad.studio.core.draft.PhotoDraftStore
import com.midad.studio.core.draft.RestoredPhotoDraft
import com.midad.studio.core.export.ExportError
import com.midad.studio.core.export.ExportOutcome
import com.midad.studio.core.image.BitmapTools
import com.midad.studio.core.project.ProjectKind
import com.midad.studio.core.project.ProjectRepository
import com.midad.studio.studio.photo.engine.Adjustments
import com.midad.studio.studio.photo.engine.ColorPipeline
import com.midad.studio.studio.photo.engine.CropAspect
import com.midad.studio.studio.photo.engine.CropRect
import com.midad.studio.studio.photo.engine.DrawStroke
import com.midad.studio.studio.photo.engine.ExportFormat
import com.midad.studio.studio.photo.engine.ExportLongEdge
import com.midad.studio.studio.photo.engine.GeometryEngine
import com.midad.studio.studio.photo.engine.GeometryPlan
import com.midad.studio.studio.photo.engine.GeometryState
import com.midad.studio.studio.photo.engine.PhotoExporter
import com.midad.studio.studio.photo.engine.StickerLayer
import com.midad.studio.studio.photo.engine.StickerShape
import com.midad.studio.studio.photo.engine.StrokePoint
import com.midad.studio.studio.photo.engine.TextLayer
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREVIEW_MAX_EDGE = 1280
private const val WORKING_MAX_EDGE = 2048
private const val HISTORY_LIMIT = 40

/**
 * Holds the photo document, the debounced preview pipeline, undo history and export.
 * No Android view references are kept; bitmaps are only recycled when they are not part of
 * the document or of a history snapshot.
 */
class PhotoEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(PhotoEditorState())
    val state: StateFlow<PhotoEditorState> = _state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var previewJob: Job? = null
    private var exportJob: Job? = null
    private var aiJob: Job? = null
    private var segmenter: SubjectSegmenter? = null
    private var liveCheckpoint: EditorSnapshot? = null
    private var strokeCheckpoint: EditorSnapshot? = null
    private val history = History(HISTORY_LIMIT)
    private var nextLayerId = 1L

    private var draftStore: PhotoDraftStore? = null
    private var draftContext: Context? = null
    private var draftSaveJob: Job? = null
    private var lastDraftSource: Bitmap? = null
    private var restoredAttempted = false
    private var abandoned = false
    private var leaving = false

    // ---------------------------------------------------------------- loading

    fun checkAiAvailability(context: Context) {
        if (_state.value.aiAvailable) return
        val available = runCatching { AiServices.segmenter(context) != null }.getOrDefault(false)
        _state.update { it.copy(aiAvailable = available) }
    }

    fun load(context: Context, uri: Uri) {
        previewJob?.cancel()
        exportJob?.cancel()
        aiJob?.cancel()
        draftSaveJob?.cancel()
        abandoned = false
        leaving = false
        lastDraftSource = null
        draftContext = context.applicationContext
        draftStore = PhotoDraftStore(context.applicationContext)
        history.clear()
        liveCheckpoint = null
        strokeCheckpoint = null
        val aiAvailable = _state.value.aiAvailable
        _state.value = PhotoEditorState(stage = EditorStage.LOADING, aiAvailable = aiAvailable)
        scope.launch {
            val decoded = try {
                BitmapTools.decodeSampled(context, uri, WORKING_MAX_EDGE)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                null
            }
            if (decoded == null) {
                _state.value = PhotoEditorState(stage = EditorStage.ERROR, aiAvailable = aiAvailable)
                return@launch
            }
            val base = BitmapTools.toArgb8888(decoded)
            val previewBase = if (maxOf(base.width, base.height) > PREVIEW_MAX_EDGE) {
                BitmapTools.scaleToMax(base, PREVIEW_MAX_EDGE)
            } else {
                base
            }
            _state.value = PhotoEditorState(
                stage = EditorStage.READY,
                sourceUri = uri,
                source = base,
                previewBase = previewBase,
                baseFromOriginal = true,
                aiAvailable = aiAvailable,
            )
            renderPreviewOnce()
            scheduleDraftSave()
        }
    }

    // ---------------------------------------------------------------- draft recovery

    /**
     * Reopens an unfinished edit after process death. Only runs once, and only when no
     * document is open, so a normal back navigation never resurrects stale work.
     */
    fun restoreDraftIfAny(context: Context) {
        if (restoredAttempted) return
        restoredAttempted = true
        if (_state.value.stage != EditorStage.EMPTY) return
        val appContext = context.applicationContext
        draftContext = appContext
        val store = draftStore ?: PhotoDraftStore(appContext).also { draftStore = it }
        if (!store.hasDraft()) return
        scope.launch {
            val restored = withContext(Dispatchers.IO) { store.load() }
            if (restored == null) {
                withContext(Dispatchers.IO) { store.clear() }
                return@launch
            }
            applyRestoredDraft(restored, projectId = null, projectTitle = null)
        }
    }

    /**
     * Reopens a saved project. The Projects screen has already copied the project payload into the
     * photo draft folder, so this only loads that folder and binds the project id and title.
     */
    fun openProject(context: Context, projectId: String) {
        if (restoredAttempted) return
        restoredAttempted = true
        if (_state.value.stage != EditorStage.EMPTY) return
        val appContext = context.applicationContext
        draftContext = appContext
        val store = draftStore ?: PhotoDraftStore(appContext).also { draftStore = it }
        scope.launch {
            ProjectRepository.ensureLoaded(appContext)
            val record = ProjectRepository.find(projectId)
            val restored = withContext(Dispatchers.IO) { store.load() }
            if (restored == null) {
                _state.value = PhotoEditorState(
                    stage = EditorStage.ERROR,
                    aiAvailable = _state.value.aiAvailable,
                    projectId = projectId,
                    projectTitle = record?.title,
                )
                return@launch
            }
            applyRestoredDraft(restored, record?.id ?: projectId, record?.title)
        }
    }

    private suspend fun applyRestoredDraft(
        restored: RestoredPhotoDraft,
        projectId: String?,
        projectTitle: String?,
    ) {
        val source = BitmapTools.toArgb8888(restored.source)
        val previewBase = if (maxOf(source.width, source.height) > PREVIEW_MAX_EDGE) {
            BitmapTools.scaleToMax(source, PREVIEW_MAX_EDGE)
        } else {
            source
        }
        val draft = restored.draft
        history.clear()
        liveCheckpoint = null
        strokeCheckpoint = null
        val highestLayer = (draft.textLayers.map { it.id } + draft.stickers.map { it.id })
            .maxOrNull() ?: 0L
        nextLayerId = maxOf(draft.nextLayerId, highestLayer + 1L)
        _state.value = PhotoEditorState(
            stage = EditorStage.READY,
            sourceUri = draft.sourceUri?.let { value -> runCatching { Uri.parse(value) }.getOrNull() },
            source = source,
            previewBase = previewBase,
            baseFromOriginal = draft.baseFromOriginal,
            adjustments = draft.adjustments,
            filterIndex = draft.filterIndex,
            filterIntensity = draft.filterIntensity,
            geometry = draft.geometry,
            textLayers = draft.textLayers,
            stickers = draft.stickers,
            strokes = draft.strokes,
            aiAvailable = _state.value.aiAvailable,
            recoveredDraft = projectId == null,
            projectId = projectId,
            projectTitle = projectTitle,
        )
        lastDraftSource = source
        renderPreviewOnce()
    }

    /**
     * Flushes the debounced draft immediately and copies it into the projects library. Returns false
     * when the draft could not be written or the library write failed; the current edit is kept.
     */
    suspend fun saveProject(context: Context, title: String): Boolean {
        val appContext = context.applicationContext
        draftContext = appContext
        val current = _state.value
        val source = current.source
        if (current.stage != EditorStage.READY || source == null) return false
        draftSaveJob?.cancel()
        draftSaveJob = null
        val store = draftStore ?: PhotoDraftStore(appContext).also { draftStore = it }
        val sourceChanged = lastDraftSource !== source
        val saved = withContext(Dispatchers.IO) {
            store.save(current.toDraft(), source, sourceChanged)
        }
        if (!saved) return false
        lastDraftSource = source
        val record = ProjectRepository.saveFromDraft(
            context = appContext,
            projectId = current.projectId,
            kind = ProjectKind.PHOTO,
            title = title,
            sourceDraftDir = PhotoDraftStore.rootDirectory(appContext),
            thumbnail = current.previewFrame ?: source,
        ) ?: return false
        _state.update { it.copy(projectId = record.id, projectTitle = record.title) }
        return true
    }

    /**
     * Explicit exit. A bound project is flushed one last time so the newest edits are stored, then
     * the transient crash-recovery draft is cleared; without a project this is the old discard.
     */
    fun leaveStudio(context: Context, onExit: () -> Unit) {
        if (leaving) return
        leaving = true
        abandoned = true
        draftSaveJob?.cancel()
        draftSaveJob = null
        val appContext = context.applicationContext
        val current = _state.value
        val boundTitle = current.projectTitle?.takeIf { current.projectId != null }
        scope.launch {
            val flushed = boundTitle?.let { title ->
                runCatching { saveProject(appContext, title) }.getOrDefault(false)
            } ?: true
            // Keep the recovery draft when a bound project could not be written, so the newest
            // edits are still recoverable after a process death instead of being lost.
            if (flushed) {
                lastDraftSource = null
                withContext(Dispatchers.IO) { draftStore?.clear() }
            }
            withContext(Dispatchers.Main) { onExit() }
        }
    }

    private fun PhotoEditorState.toDraft(): PhotoDraft = PhotoDraft(
        sourceUri = sourceUri?.toString(),
        baseFromOriginal = baseFromOriginal,
        sourcePng = source?.hasAlpha() == true,
        adjustments = adjustments,
        filterIndex = filterIndex,
        filterIntensity = filterIntensity,
        geometry = geometry,
        textLayers = textLayers,
        stickers = stickers,
        strokes = strokes,
        nextLayerId = nextLayerId,
    )

    fun dismissDraftRecovered() {
        if (!_state.value.recoveredDraft) return
        publish { it.copy(recoveredDraft = false) }
    }

    /** Leaves the editor for good: the draft must not survive an explicit exit. */
    fun discardDraft() {
        abandoned = true
        draftSaveJob?.cancel()
        draftSaveJob = null
        lastDraftSource = null
        draftStore?.clear()
    }

    private fun scheduleDraftSave() {
        if (abandoned) return
        val store = draftStore ?: return
        val snapshot = _state.value
        if (snapshot.stage != EditorStage.READY || snapshot.exporting || snapshot.source == null) return
        draftSaveJob?.cancel()
        draftSaveJob = scope.launch {
            delay(650)
            val current = _state.value
            if (current.stage != EditorStage.READY || current.exporting) return@launch
            val source = current.source ?: return@launch
            val sourceChanged = lastDraftSource !== source
            val saved = withContext(Dispatchers.IO) {
                store.save(current.toDraft(), source, sourceChanged)
            }
            // The transient draft is enough for crash recovery. A bound project is written on
            // explicit Save/leave only, because copying its payload on every edit is expensive.
            if (saved) lastDraftSource = source
        }
    }

    // ---------------------------------------------------------------- preview

    private data class PreviewFrame(val bitmap: Bitmap, val plan: GeometryPlan)

    private fun requestPreview(delayMillis: Long = 80L) {
        previewJob?.cancel()
        previewJob = scope.launch {
            if (delayMillis > 0) delay(delayMillis)
            renderPreviewOnce()
        }
    }

    private suspend fun renderPreviewOnce() {
        val snapshot = _state.value
        val base = snapshot.previewBase ?: return
        _state.update { it.copy(renderingPreview = true) }
        val frame = try {
            computePreview(snapshot, base)
        } catch (cancelled: CancellationException) {
            _state.update { it.copy(renderingPreview = false) }
            throw cancelled
        } catch (throwable: Throwable) {
            null
        }
        if (frame == null) {
            _state.update { it.copy(renderingPreview = false) }
            return
        }
        _state.update { current ->
            current.copy(previewFrame = frame.bitmap, plan = frame.plan, renderingPreview = false)
        }
    }

    private suspend fun computePreview(snapshot: PhotoEditorState, base: Bitmap): PreviewFrame =
        withContext(Dispatchers.Default) {
            val geometry = if (snapshot.activeTool == PhotoTool.CROP) {
                snapshot.geometry.copy(crop = CropRect.Full)
            } else {
                snapshot.geometry
            }
            val plan = GeometryEngine.plan(base.width, base.height, geometry)
            val geometryOutput = GeometryEngine.render(base, plan)
            val working = if (geometryOutput === base) BitmapTools.copyOf(base) else geometryOutput
            val colored = ColorPipeline.render(
                source = working,
                adjustments = snapshot.adjustments,
                filter = snapshot.activeFilter,
                filterIntensity = snapshot.filterIntensity,
                inPlace = true,
            )
            if (colored !== working) BitmapTools.safeRecycle(working)
            PreviewFrame(colored, plan)
        }

    // ---------------------------------------------------------------- history

    private fun publish(mutator: (PhotoEditorState) -> PhotoEditorState) {
        _state.update { current ->
            mutator(current).copy(canUndo = history.canUndo, canRedo = history.canRedo)
        }
        scheduleDraftSave()
    }

    private fun captureSnapshot(): EditorSnapshot {
        val current = _state.value
        return EditorSnapshot(
            source = current.source,
            previewBase = current.previewBase,
            baseFromOriginal = current.baseFromOriginal,
            adjustments = current.adjustments,
            filterIndex = current.filterIndex,
            filterIntensity = current.filterIntensity,
            geometry = current.geometry,
            textLayers = current.textLayers,
            stickers = current.stickers,
            strokes = current.strokes,
        )
    }

    private fun beginLiveEdit() {
        if (liveCheckpoint == null) liveCheckpoint = captureSnapshot()
    }

    fun commitLiveEdit() {
        val checkpoint = liveCheckpoint ?: return
        liveCheckpoint = null
        history.pushUndo(checkpoint)
        publish { it }
    }

    private fun applyDiscrete(mutator: (PhotoEditorState) -> PhotoEditorState) {
        history.pushUndo(captureSnapshot())
        liveCheckpoint = null
        publish(mutator)
    }

    fun undo() {
        val previous = history.undo(captureSnapshot()) ?: return
        liveCheckpoint = null
        restore(previous)
    }

    fun redo() {
        val next = history.redo(captureSnapshot()) ?: return
        liveCheckpoint = null
        restore(next)
    }

    private fun restore(snapshot: EditorSnapshot) {
        val current = _state.value
        val needsPreview = current.source !== snapshot.source ||
            current.geometry != snapshot.geometry ||
            current.adjustments != snapshot.adjustments ||
            current.filterIndex != snapshot.filterIndex ||
            current.filterIntensity != snapshot.filterIntensity
        publish { state ->
            state.copy(
                source = snapshot.source,
                previewBase = snapshot.previewBase,
                baseFromOriginal = snapshot.baseFromOriginal,
                adjustments = snapshot.adjustments,
                filterIndex = snapshot.filterIndex,
                filterIntensity = snapshot.filterIntensity,
                geometry = snapshot.geometry,
                textLayers = snapshot.textLayers,
                stickers = snapshot.stickers,
                strokes = snapshot.strokes,
                draftStroke = null,
                selectedTextId = state.selectedTextId?.takeIf { id -> snapshot.textLayers.any { it.id == id } },
                selectedStickerId = state.selectedStickerId?.takeIf { id -> snapshot.stickers.any { it.id == id } },
            )
        }
        if (needsPreview) requestPreview(0)
    }

    // ---------------------------------------------------------------- tools

    fun selectTool(tool: PhotoTool) {
        val current = _state.value
        if (current.stage != EditorStage.READY) return
        commitLiveEdit()
        if (current.activeTool == tool) {
            publish { it.copy(activeTool = null) }
        } else {
            publish { it.copy(activeTool = tool) }
        }
        requestPreview(0)
    }

    fun closeToolPanel() {
        if (_state.value.activeTool == null) return
        commitLiveEdit()
        publish { it.copy(activeTool = null) }
        requestPreview(0)
    }

    // ---------------------------------------------------------------- adjustments

    fun setAdjustmentLive(key: AdjustKey, value: Float) {
        beginLiveEdit()
        publish { it.copy(adjustments = key.write(it.adjustments, value)) }
        requestPreview()
    }

    /** Resets every slider, or only the [keys] of one adjust group. */
    fun resetAdjustments(keys: Collection<AdjustKey> = AdjustKey.entries) {
        val current = _state.value.adjustments
        val reset = keys.fold(current) { adjustments, key -> key.write(adjustments, 0f) }
        if (reset == current) return
        applyDiscrete { it.copy(adjustments = reset) }
        requestPreview(0)
    }

    // ---------------------------------------------------------------- filters

    fun selectFilter(index: Int?) {
        applyDiscrete { it.copy(filterIndex = index) }
        requestPreview(0)
    }

    fun setFilterIntensityLive(value: Int) {
        beginLiveEdit()
        publish { it.copy(filterIntensity = value.coerceIn(0, 100)) }
        requestPreview()
    }

    // ---------------------------------------------------------------- crop / geometry

    fun selectCropAspect(aspect: CropAspect) {
        val base = _state.value.previewBase ?: return
        applyDiscrete { state ->
            val geometry = state.geometry.copy(aspect = aspect)
            val plan = GeometryEngine.plan(base.width, base.height, geometry.copy(crop = CropRect.Full))
            state.copy(geometry = geometry.copy(crop = plan.fittedCrop(aspect)))
        }
        requestPreview(0)
    }

    fun rotate(clockwise: Boolean) {
        val base = _state.value.previewBase ?: return
        applyDiscrete { state ->
            val turns = (((state.geometry.quarterTurns + if (clockwise) 1 else 3) % 4) + 4) % 4
            val geometry = state.geometry.copy(quarterTurns = turns)
            val plan = GeometryEngine.plan(base.width, base.height, geometry.copy(crop = CropRect.Full))
            state.copy(geometry = geometry.copy(crop = plan.fittedCrop(geometry.aspect)))
        }
        requestPreview(0)
    }

    fun flip(horizontal: Boolean) {
        applyDiscrete { state ->
            val geometry = if (horizontal) {
                state.geometry.copy(flipHorizontal = !state.geometry.flipHorizontal)
            } else {
                state.geometry.copy(flipVertical = !state.geometry.flipVertical)
            }
            state.copy(geometry = geometry)
        }
        requestPreview(0)
    }

    fun setStraightenLive(value: Float) {
        val base = _state.value.previewBase ?: return
        beginLiveEdit()
        val geometry = _state.value.geometry.copy(straighten = value.coerceIn(-45f, 45f))
        val plan = GeometryEngine.plan(base.width, base.height, geometry.copy(crop = CropRect.Full))
        val crop = if (geometry.aspect.ratio == null) geometry.crop.clamped() else plan.fittedCrop(geometry.aspect)
        publish { it.copy(geometry = geometry.copy(crop = crop)) }
        requestPreview()
    }

    fun resetGeometry() {
        if (_state.value.geometry.isIdentity) return
        applyDiscrete { it.copy(geometry = GeometryState()) }
        requestPreview(0)
    }

    /** Live crop-rect drag. The crop tool already previews the uncropped image, so no re-render. */
    fun updateCropRectLive(rect: CropRect) {
        beginLiveEdit()
        publish { it.copy(geometry = it.geometry.copy(crop = rect.clamped())) }
    }

    // ---------------------------------------------------------------- text layers

    fun addText(defaultText: String) {
        val layer = TextLayer(
            id = nextLayerId++,
            text = defaultText,
            color = PhotoPalette.White,
            sizeFraction = 0.085f,
            serif = true,
            opacity = 1f,
            x = 0.5f,
            y = 0.5f,
        )
        applyDiscrete { state ->
            state.copy(textLayers = state.textLayers + layer, selectedTextId = layer.id, selectedStickerId = null)
        }
    }

    fun selectText(id: Long) {
        publish { it.copy(selectedTextId = id, selectedStickerId = null) }
    }

    fun clearSelection() {
        publish { it.copy(selectedTextId = null, selectedStickerId = null) }
    }

    fun setTextContent(text: String) {
        val selected = _state.value.selectedText ?: return
        applyDiscrete { state ->
            state.copy(textLayers = state.textLayers.map { if (it.id == selected.id) it.copy(text = text) else it })
        }
    }

    fun setTextColor(color: Int) {
        val selected = _state.value.selectedText ?: return
        applyDiscrete { state ->
            state.copy(textLayers = state.textLayers.map { if (it.id == selected.id) it.copy(color = color) else it })
        }
    }

    fun setTextSizeLive(fraction: Float) {
        val selected = _state.value.selectedText ?: return
        beginLiveEdit()
        val clamped = fraction.coerceIn(0.02f, 0.5f)
        publish { state ->
            state.copy(textLayers = state.textLayers.map { if (it.id == selected.id) it.copy(sizeFraction = clamped) else it })
        }
    }

    fun setTextOpacityLive(opacity: Float) {
        val selected = _state.value.selectedText ?: return
        beginLiveEdit()
        val clamped = opacity.coerceIn(0.05f, 1f)
        publish { state ->
            state.copy(textLayers = state.textLayers.map { if (it.id == selected.id) it.copy(opacity = clamped) else it })
        }
    }

    fun toggleTextSerif() {
        val selected = _state.value.selectedText ?: return
        applyDiscrete { state ->
            state.copy(textLayers = state.textLayers.map { if (it.id == selected.id) it.copy(serif = !it.serif) else it })
        }
    }

    fun deleteSelectedText() {
        val selected = _state.value.selectedText ?: return
        applyDiscrete { state ->
            state.copy(
                textLayers = state.textLayers.filterNot { it.id == selected.id },
                selectedTextId = null,
            )
        }
    }

    fun transformSelectedText(zoom: Float, rotationDelta: Float, deltaX: Float, deltaY: Float) {
        val selected = _state.value.selectedText ?: return
        beginLiveEdit()
        val updated = selected.copy(
            sizeFraction = (selected.sizeFraction * zoom).coerceIn(0.02f, 0.5f),
            rotation = selected.rotation + rotationDelta,
            x = (selected.x + deltaX).coerceIn(-0.1f, 1.1f),
            y = (selected.y + deltaY).coerceIn(-0.1f, 1.1f),
        )
        publish { state ->
            state.copy(textLayers = state.textLayers.map { if (it.id == updated.id) updated else it })
        }
    }

    // ---------------------------------------------------------------- stickers

    fun addSticker(shape: StickerShape) {
        val layer = StickerLayer(
            id = nextLayerId++,
            shape = shape,
            color = PhotoPalette.Saffron,
            sizeFraction = 0.24f,
            x = 0.5f,
            y = 0.5f,
        )
        applyDiscrete { state ->
            state.copy(stickers = state.stickers + layer, selectedStickerId = layer.id, selectedTextId = null)
        }
    }

    fun selectSticker(id: Long) {
        publish { it.copy(selectedStickerId = id, selectedTextId = null) }
    }

    fun setStickerColor(color: Int) {
        val selected = _state.value.selectedSticker ?: return
        applyDiscrete { state ->
            state.copy(stickers = state.stickers.map { if (it.id == selected.id) it.copy(color = color) else it })
        }
    }

    fun setStickerSizeLive(fraction: Float) {
        val selected = _state.value.selectedSticker ?: return
        beginLiveEdit()
        val clamped = fraction.coerceIn(0.04f, 0.8f)
        publish { state ->
            state.copy(stickers = state.stickers.map { if (it.id == selected.id) it.copy(sizeFraction = clamped) else it })
        }
    }

    fun deleteSelectedSticker() {
        val selected = _state.value.selectedSticker ?: return
        applyDiscrete { state ->
            state.copy(stickers = state.stickers.filterNot { it.id == selected.id }, selectedStickerId = null)
        }
    }

    fun transformSelectedSticker(zoom: Float, rotationDelta: Float, deltaX: Float, deltaY: Float) {
        val selected = _state.value.selectedSticker ?: return
        beginLiveEdit()
        val updated = selected.copy(
            sizeFraction = (selected.sizeFraction * zoom).coerceIn(0.04f, 0.8f),
            rotation = selected.rotation + rotationDelta,
            x = (selected.x + deltaX).coerceIn(-0.1f, 1.1f),
            y = (selected.y + deltaY).coerceIn(-0.1f, 1.1f),
        )
        publish { state ->
            state.copy(stickers = state.stickers.map { if (it.id == updated.id) updated else it })
        }
    }

    // ---------------------------------------------------------------- brush

    fun setBrushColor(color: Int) {
        publish { it.copy(brushColor = color) }
    }

    fun setBrushWidthLive(fraction: Float) {
        publish { it.copy(brushWidth = fraction.coerceIn(0.004f, 0.2f)) }
    }

    fun setErasing(erasing: Boolean) {
        publish { it.copy(erasing = erasing) }
    }

    fun clearStrokes() {
        if (_state.value.strokes.isEmpty()) return
        applyDiscrete { it.copy(strokes = emptyList(), draftStroke = null) }
    }

    fun beginStroke() {
        val current = _state.value
        strokeCheckpoint = captureSnapshot()
        publish {
            it.copy(
                draftStroke = DrawStroke(
                    points = emptyList(),
                    color = current.brushColor,
                    widthFraction = current.brushWidth,
                    erase = current.erasing,
                ),
            )
        }
    }

    fun appendStrokePoint(x: Float, y: Float) {
        val draft = _state.value.draftStroke ?: return
        val last = draft.points.lastOrNull()
        if (last != null && abs(last.x - x) < 0.0012f && abs(last.y - y) < 0.0012f) return
        val point = StrokePoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        publish { it.copy(draftStroke = draft.copy(points = draft.points + point)) }
    }

    fun endStroke() {
        val draft = _state.value.draftStroke
        val checkpoint = strokeCheckpoint
        strokeCheckpoint = null
        publish { it.copy(draftStroke = null) }
        if (draft == null || draft.points.size < 2) return
        if (checkpoint != null) history.pushUndo(checkpoint)
        val completed = draft.copy(points = draft.points.toList())
        publish { it.copy(strokes = it.strokes + completed) }
    }

    fun cancelStroke() {
        strokeCheckpoint = null
        publish { it.copy(draftStroke = null) }
    }

    // ---------------------------------------------------------------- AI cutout

    fun runCutout(context: Context) {
        val current = _state.value
        if (current.aiBusy || current.stage != EditorStage.READY) return
        val base = current.source ?: return
        aiJob?.cancel()
        publish { it.copy(aiBusy = true, aiHint = false) }
        aiJob = scope.launch {
            val cut = try {
                val subjectSegmenter = segmenter ?: AiServices.segmenter(context)
                if (subjectSegmenter == null) {
                    null
                } else {
                    segmenter = subjectSegmenter
                    withContext(Dispatchers.Default) {
                        val plan = GeometryEngine.plan(base.width, base.height, current.geometry)
                        val geometryOutput = GeometryEngine.render(base, plan)
                        val working = if (geometryOutput === base) BitmapTools.copyOf(base) else geometryOutput
                        val flattened = ColorPipeline.render(
                            source = working,
                            adjustments = current.adjustments,
                            filter = current.activeFilter,
                            filterIntensity = current.filterIntensity,
                            inPlace = true,
                        )
                        val result = runCatching {
                            subjectSegmenter.cutout(context, flattened, CutoutBackground.TRANSPARENT)
                        }.getOrNull()
                        if (flattened !== base) BitmapTools.safeRecycle(flattened)
                        if (result != null) BitmapTools.toArgb8888(result) else null
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                null
            }
            if (cut == null) {
                publish { it.copy(aiBusy = false, aiHint = true) }
                return@launch
            }
            history.pushUndo(captureSnapshot())
            liveCheckpoint = null
            val previewBase = if (maxOf(cut.width, cut.height) > PREVIEW_MAX_EDGE) {
                BitmapTools.scaleToMax(cut, PREVIEW_MAX_EDGE)
            } else {
                cut
            }
            lastDraftSource = null
            publish { state ->
                state.copy(
                    source = cut,
                    previewBase = previewBase,
                    baseFromOriginal = false,
                    geometry = GeometryState(),
                    adjustments = Adjustments(),
                    filterIndex = null,
                    filterIntensity = 100,
                    aiBusy = false,
                    aiHint = false,
                )
            }
            requestPreview(0)
        }
    }

    fun dismissAiHint() {
        if (!_state.value.aiHint) return
        publish { it.copy(aiHint = false) }
    }

    // ---------------------------------------------------------------- export

    fun openExportDialog() {
        if (_state.value.stage != EditorStage.READY) return
        commitLiveEdit()
        publish {
            it.copy(
                exportDialogOpen = true,
                exporting = false,
                exportProgress = 0f,
                exportFailure = null,
                exportResult = null,
            )
        }
    }

    fun closeExportDialog() {
        if (_state.value.exporting) return
        publish { it.copy(exportDialogOpen = false) }
    }

    fun setExportFormat(format: ExportFormat) {
        publish { it.copy(exportOptions = it.exportOptions.copy(format = format)) }
    }

    fun setExportQuality(quality: Int) {
        publish { it.copy(exportOptions = it.exportOptions.copy(quality = quality.coerceIn(60, 100))) }
    }

    fun setExportLongEdge(longEdge: ExportLongEdge) {
        publish { it.copy(exportOptions = it.exportOptions.copy(longEdge = longEdge)) }
    }

    fun startExport(context: Context) {
        val current = _state.value
        if (current.exporting || current.stage != EditorStage.READY) return
        val source = current.source ?: return
        exportJob?.cancel()
        publish { it.copy(exporting = true, exportProgress = 0f, exportFailure = null, exportResult = null) }
        val request = PhotoExporter.Request(
            workingSource = source,
            sourceUri = current.sourceUri,
            baseFromOriginal = current.baseFromOriginal,
            adjustments = current.adjustments,
            filter = current.activeFilter,
            filterIntensity = current.filterIntensity,
            geometry = current.geometry,
            textLayers = current.textLayers,
            stickers = current.stickers,
            strokes = current.strokes,
            options = current.exportOptions,
        )
        exportJob = scope.launch {
            val outcome = try {
                PhotoExporter.export(context, request) { fraction ->
                    publish { it.copy(exportProgress = fraction) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                ExportOutcome.Failure(ExportError.UNKNOWN, throwable)
            }
            publish {
                when (outcome) {
                    is ExportOutcome.Success -> it.copy(
                        exporting = false,
                        exportProgress = 1f,
                        exportResult = outcome.result,
                        exportFailure = null,
                    )
                    is ExportOutcome.Failure -> it.copy(
                        exporting = false,
                        exportResult = null,
                        exportFailure = outcome.error,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        previewJob?.cancel()
        exportJob?.cancel()
        aiJob?.cancel()
        draftSaveJob?.cancel()
        runCatching { segmenter?.close() }
        segmenter = null
        scope.cancel()
    }
}

private class History(private val limit: Int) {
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun pushUndo(snapshot: EditorSnapshot) {
        undoStack.addLast(snapshot)
        while (undoStack.size > limit) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: EditorSnapshot): EditorSnapshot? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    fun redo(current: EditorSnapshot): EditorSnapshot? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return next
    }
}
