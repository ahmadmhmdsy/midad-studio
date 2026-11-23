package com.midad.studio.studio.design

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.midad.studio.core.draft.CollageDraft
import com.midad.studio.core.draft.DesignDraft
import com.midad.studio.core.draft.DesignDraftStore
import com.midad.studio.core.draft.RestoredDesignDraft
import com.midad.studio.core.export.ExportError
import com.midad.studio.core.export.ExportManager
import com.midad.studio.core.export.ExportOutcome
import com.midad.studio.core.export.ExportResult
import com.midad.studio.core.image.BitmapTools
import com.midad.studio.core.project.ProjectKind
import com.midad.studio.core.project.ProjectRepository
import com.midad.studio.studio.design.collage.CollageAspect
import com.midad.studio.studio.design.collage.CollageLayout
import com.midad.studio.studio.design.collage.CollageLayouts
import com.midad.studio.studio.design.collage.CollageRenderer
import com.midad.studio.studio.design.collage.CollageSpec
import com.midad.studio.studio.design.model.CanvasPreset
import com.midad.studio.studio.design.model.DesignBackground
import com.midad.studio.studio.design.model.DesignDocument
import com.midad.studio.studio.design.model.DesignLayer
import com.midad.studio.studio.design.model.DesignPalette
import com.midad.studio.studio.design.model.DesignSelection
import com.midad.studio.studio.design.model.DesignTemplate
import com.midad.studio.studio.design.model.LayerContent
import com.midad.studio.studio.design.model.ShapeKind
import com.midad.studio.studio.design.model.TextFont
import com.midad.studio.studio.design.model.accentInk
import com.midad.studio.studio.design.model.readableInk
import com.midad.studio.studio.design.render.DesignRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

enum class DesignScreen { START, TEMPLATES, EDITOR, COLLAGE }

enum class ExportTarget { DOCUMENT, COLLAGE }

sealed interface ExportState {
    data object Idle : ExportState
    data object Rendering : ExportState
    data object Saving : ExportState
    data class Success(val result: ExportResult, val png: Boolean) : ExportState
    data class Failed(val error: ExportError) : ExportState
}

/** The collage workspace: picked photos, chosen grid, gap and paper colour. */
data class CollageState(
    val images: List<Uri> = emptyList(),
    val bitmaps: List<Bitmap> = emptyList(),
    val layoutId: String = CollageLayouts.defaultFor(2).id,
    val aspect: CollageAspect = CollageAspect.SQUARE,
    val spacing: Int = 12,
    val background: Long = DesignPalette.PAPER_HIGH,
    val loading: Boolean = false,
    val error: Boolean = false,
)

/**
 * Holds the whole design flow. Documents are immutable values; a checkpoint batching scheme
 * turns a gesture or a slider drag into a single undo step.
 */
class DesignViewModel : ViewModel() {

    var screen by mutableStateOf(DesignScreen.START)
        private set
    var document by mutableStateOf<DesignDocument?>(null)
        private set
    var selection by mutableStateOf<DesignSelection>(DesignSelection.None)
        private set
    var showGuides by mutableStateOf(true)
        private set
    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set
    var lastExportTarget by mutableStateOf(ExportTarget.DOCUMENT)
        private set
    var collage by mutableStateOf(CollageState())
        private set
    var templates by mutableStateOf<List<DesignTemplate>>(emptyList())
        private set
    var undoCount by mutableStateOf(0)
        private set
    var redoCount by mutableStateOf(0)
        private set

    val canUndo: Boolean get() = undoCount > 0
    val canRedo: Boolean get() = redoCount > 0

    val collageLayout: CollageLayout
        get() = CollageLayouts.layoutFor(collage.layoutId, collage.bitmaps.size)

    private val bitmaps = mutableStateMapOf<Long, Bitmap>()
    private val undoStack = ArrayDeque<DesignDocument>()
    private val redoStack = ArrayDeque<DesignDocument>()
    private var checkpoint: DesignDocument? = null
    private var layerIdSeed: Long = 1L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var draftStore: DesignDraftStore? = null
    private var draftSaveJob: Job? = null
    private var lastDraftImages: List<Bitmap>? = null
    private var restoredAttempted = false
    private var draftEpoch = 0
    private var draftAllowed = true
    private var draftContext: Context? = null
    private var leaving = false

    /** The saved project this design belongs to; null until the first save. */
    var activeProjectId by mutableStateOf<String?>(null)
        private set
    var activeProjectTitle by mutableStateOf<String?>(null)
        private set

    // ---- setup -----------------------------------------------------------------

    fun updateTemplates(list: List<DesignTemplate>) {
        templates = list
    }

    fun bitmapSnapshot(): Map<Long, Bitmap> = bitmaps.toMap()

    // ---- navigation ------------------------------------------------------------

    fun openTemplates() {
        screen = DesignScreen.TEMPLATES
    }

    fun openCollage() {
        screen = DesignScreen.COLLAGE
    }

    // ---- draft recovery ---------------------------------------------------------

    /**
     * Reopens a design or collage that an OS kill interrupted. Runs once, and only at a
     * fresh start screen, so an explicit back never resurrects old work.
     */
    fun restoreDraftIfAny(context: Context) {
        if (restoredAttempted) return
        restoredAttempted = true
        if (screen != DesignScreen.START || document != null || collage.bitmaps.isNotEmpty()) return
        val appContext = context.applicationContext
        draftContext = appContext
        val store = draftStore ?: DesignDraftStore(appContext).also { draftStore = it }
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
     * design draft folder, so this only loads it and binds the project id and title.
     */
    fun openProject(context: Context, projectId: String) {
        if (restoredAttempted) return
        restoredAttempted = true
        if (screen != DesignScreen.START || document != null || collage.bitmaps.isNotEmpty()) return
        val appContext = context.applicationContext
        draftContext = appContext
        val store = draftStore ?: DesignDraftStore(appContext).also { draftStore = it }
        scope.launch {
            ProjectRepository.ensureLoaded(appContext)
            val record = ProjectRepository.find(projectId)
            val restored = withContext(Dispatchers.IO) { store.load() }
            if (restored == null) return@launch
            applyRestoredDraft(restored, record?.id ?: projectId, record?.title)
        }
    }

    private suspend fun applyRestoredDraft(
        restored: RestoredDesignDraft,
        projectId: String?,
        projectTitle: String?,
    ) {
        when (restored.draft.screen) {
            DesignScreen.COLLAGE -> {
                val collageDraft = restored.draft.collage ?: return
                collage = CollageState(
                    images = collageDraft.images.mapNotNull { value ->
                        runCatching { Uri.parse(value) }.getOrNull()
                    },
                    bitmaps = restored.collageBitmaps,
                    layoutId = collageDraft.layoutId,
                    aspect = collageDraft.aspect,
                    spacing = collageDraft.spacing,
                    background = collageDraft.background,
                )
                screen = DesignScreen.COLLAGE
            }
            DesignScreen.EDITOR -> {
                val restoredDocument = restored.draft.document ?: return
                releaseDocumentBitmaps()
                bitmaps.putAll(restored.documentBitmaps)
                document = restoredDocument
                layerIdSeed = (restoredDocument.layers.maxOfOrNull { layer -> layer.id } ?: 0L) + 1L
                undoStack.clear()
                redoStack.clear()
                checkpoint = null
                selection = DesignSelection.None
                refreshHistory()
                screen = DesignScreen.EDITOR
            }
            DesignScreen.START, DesignScreen.TEMPLATES -> Unit
        }
        activeProjectId = projectId
        activeProjectTitle = projectTitle
        draftAllowed = true
        draftEpoch++
        lastDraftImages = currentDraftImages()
    }

    /**
     * Flushes the debounced draft and copies it into the projects library. Returns false when the
     * draft or the library write failed; the current design is kept either way.
     */
    suspend fun saveProject(context: Context, title: String): Boolean {
        val appContext = context.applicationContext
        draftContext = appContext
        val currentDocument = document
        val currentCollage = collage
        if (currentDocument == null && currentCollage.bitmaps.isEmpty()) return false
        draftSaveJob?.cancel()
        draftSaveJob = null
        val store = draftStore ?: DesignDraftStore(appContext).also { draftStore = it }
        val saved = withContext(Dispatchers.IO) {
            store.save(
                draft = designDraft(currentDocument, currentCollage),
                documentBitmaps = bitmaps.toMap(),
                collageBitmaps = currentCollage.bitmaps,
                writeImages = true,
            )
        }
        if (!saved) return false
        lastDraftImages = currentDraftImages()
        val thumbnail = withContext(Dispatchers.Default) {
            renderProjectThumbnail(currentDocument, currentCollage)
        }
        val record = try {
            ProjectRepository.saveFromDraft(
                context = appContext,
                projectId = activeProjectId,
                kind = ProjectKind.DESIGN,
                title = title,
                sourceDraftDir = DesignDraftStore.rootDirectory(appContext),
                thumbnail = thumbnail,
            )
        } finally {
            BitmapTools.safeRecycle(thumbnail)
        }
        if (record == null) return false
        activeProjectId = record.id
        activeProjectTitle = record.title
        return true
    }

    /**
     * Explicit exit. A bound project is flushed one last time before the transient crash-recovery
     * draft is cleared; without a project this is the old discard path.
     */
    fun leaveStudio(context: Context, onExit: () -> Unit) {
        if (leaving) return
        leaving = true
        val appContext = context.applicationContext
        val projectTitle = activeProjectTitle
        if (activeProjectId == null || projectTitle == null) {
            discardDraft()
            onExit()
            return
        }
        scope.launch {
            val flushed = runCatching { saveProject(appContext, projectTitle) }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                // Keep the recovery draft when the project write failed, so edits are not lost.
                if (flushed) discardDraft()
                onExit()
            }
        }
    }

    private fun designDraft(
        currentDocument: DesignDocument?,
        currentCollage: CollageState,
    ): DesignDraft = DesignDraft(
        screen = screen,
        document = currentDocument,
        collage = if (currentCollage.bitmaps.isNotEmpty()) {
            CollageDraft(
                images = currentCollage.images.map { uri -> uri.toString() },
                layoutId = currentCollage.layoutId,
                aspect = currentCollage.aspect,
                spacing = currentCollage.spacing,
                background = currentCollage.background,
            )
        } else {
            null
        },
    )

    /** Renders the design or collage for the project grid; the caller recycles the result. */
    private fun renderProjectThumbnail(
        currentDocument: DesignDocument?,
        currentCollage: CollageState,
    ): Bitmap? {
        val source = if (currentCollage.bitmaps.isNotEmpty()) {
            val spec = CollageSpec(
                aspect = currentCollage.aspect,
                layout = CollageLayouts.layoutFor(
                    currentCollage.layoutId,
                    currentCollage.bitmaps.size,
                ),
                spacing = currentCollage.spacing,
                background = currentCollage.background,
            )
            CollageRenderer.render(spec, currentCollage.bitmaps)
        } else {
            val document = currentDocument ?: return null
            DesignRenderer.renderDocument(document, bitmaps.toMap())
        }
        val scaled = BitmapTools.scaleToMax(source, 512)
        if (scaled !== source) BitmapTools.safeRecycle(source)
        return scaled
    }

    /** Leaves the design studio for good; the draft must not survive an explicit exit. */
    fun discardDraft() {
        draftAllowed = false
        draftEpoch++
        draftSaveJob?.cancel()
        draftSaveJob = null
        lastDraftImages = null
        draftStore?.clear()
    }

    private fun currentDraftImages(): List<Bitmap> {
        val documentImages = document?.layers.orEmpty().mapNotNull { layer ->
            if (layer.content is LayerContent.Image) bitmaps[layer.id] else null
        }
        return documentImages + collage.bitmaps
    }

    private fun scheduleDraftSave() {
        val store = draftStore ?: return
        val currentDocument = document
        val currentCollage = collage
        if (currentDocument == null && currentCollage.bitmaps.isEmpty()) return
        draftSaveJob?.cancel()
        val epoch = draftEpoch
        draftSaveJob = scope.launch {
            delay(600)
            val document = document
            val collageState = collage
            if (document == null && collageState.bitmaps.isEmpty()) return@launch
            val images = currentDraftImages()
            val imagesChanged = images != lastDraftImages
            val saved = withContext(Dispatchers.IO) {
                store.save(
                    draft = designDraft(document, collageState),
                    documentBitmaps = bitmaps.toMap(),
                    collageBitmaps = collageState.bitmaps,
                    writeImages = imagesChanged,
                    shouldCommit = { draftAllowed && epoch == draftEpoch },
                )
            }
            // The transient draft is enough for crash recovery; a bound project is copied on
            // explicit Save/leave only, because the payload includes every layer bitmap.
            if (saved) lastDraftImages = images
        }
    }

    fun openNewDocument(preset: CanvasPreset) {
        replaceDocument(
            DesignDocument(
                preset = preset,
                background = DesignBackground.Solid(DesignPalette.PAPER),
            ),
        )
        screen = DesignScreen.EDITOR
        scheduleDraftSave()
    }

    fun openTemplate(template: DesignTemplate) {
        replaceDocument(template.document)
        screen = DesignScreen.EDITOR
        scheduleDraftSave()
    }

    fun backToStart() {
        when (screen) {
            DesignScreen.EDITOR -> discardDocument()
            DesignScreen.COLLAGE -> clearCollage()
            else -> Unit
        }
        exportState = ExportState.Idle
        screen = DesignScreen.START
        discardDraft()
    }

    fun toggleGuides() {
        showGuides = !showGuides
    }

    private fun replaceDocument(next: DesignDocument) {
        draftAllowed = true
        releaseDocumentBitmaps()
        undoStack.clear()
        redoStack.clear()
        checkpoint = null
        refreshHistory()
        layerIdSeed = (next.layers.maxOfOrNull { layer -> layer.id } ?: 0L) + 1L
        document = next
        selection = DesignSelection.None
        scheduleDraftSave()
    }

    private fun discardDocument() {
        releaseDocumentBitmaps()
        document = null
        selection = DesignSelection.None
        undoStack.clear()
        redoStack.clear()
        checkpoint = null
        refreshHistory()
    }

    private fun releaseDocumentBitmaps() {
        bitmaps.values.forEach { bitmap -> BitmapTools.safeRecycle(bitmap) }
        bitmaps.clear()
    }

    /**
     * Recycles bitmaps that no reachable document refers to (current, checkpoint, undo, redo).
     * Without this the map only ever grew: every replaced or deleted photo kept a full-size
     * bitmap alive for the rest of the session, which is how one design session reached
     * hundreds of megabytes.
     */
    private fun pruneBitmaps() {
        val live = HashSet<Long>()
        val collect: (DesignDocument?) -> Unit = { doc ->
            doc?.layers?.forEach { layer ->
                if (layer.content is LayerContent.Image) live += layer.id
            }
        }
        collect(document)
        collect(checkpoint)
        undoStack.forEach(collect)
        redoStack.forEach(collect)
        val stale = bitmaps.keys.filterNot { id -> id in live }
        stale.forEach { id -> bitmaps.remove(id)?.let { bitmap -> BitmapTools.safeRecycle(bitmap) } }
    }

    // ---- history ---------------------------------------------------------------

    fun checkpointStart() {
        if (checkpoint == null) checkpoint = document
    }

    fun checkpointEnd() {
        val before = checkpoint ?: return
        checkpoint = null
        val current = document ?: return
        if (before == current) return
        undoStack.addLast(before)
        while (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        refreshHistory()
        scheduleDraftSave()
    }

    fun edit(block: (DesignDocument) -> DesignDocument) {
        val current = document ?: return
        checkpointStart()
        val next = block(current)
        if (next != current) document = next
        checkpointEnd()
    }

    fun undo() {
        val current = document ?: return
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeLast()
        redoStack.addLast(current)
        while (redoStack.size > MAX_HISTORY) redoStack.removeFirst()
        document = previous
        selection = validSelection(previous, selection)
        refreshHistory()
        pruneBitmaps()
        scheduleDraftSave()
    }

    fun redo() {
        val current = document ?: return
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        while (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        document = next
        selection = validSelection(next, selection)
        refreshHistory()
        pruneBitmaps()
        scheduleDraftSave()
    }

    private fun refreshHistory() {
        undoCount = undoStack.size
        redoCount = redoStack.size
    }

    private fun validSelection(
        document: DesignDocument,
        selection: DesignSelection,
    ): DesignSelection = when (selection) {
        is DesignSelection.Layer ->
            if (document.layer(selection.id) != null) selection else DesignSelection.None
        else -> selection
    }

    // ---- selection -------------------------------------------------------------

    fun selectLayer(layerId: Long?) {
        selection = if (layerId == null) {
            DesignSelection.None
        } else {
            DesignSelection.Layer(layerId)
        }
    }

    fun selectBackground() {
        selection = DesignSelection.Background
    }

    fun clearSelection() {
        selection = DesignSelection.None
    }

    // ---- layers ----------------------------------------------------------------

    fun updateLayer(layerId: Long, block: (DesignLayer) -> DesignLayer) {
        edit { doc ->
            doc.copy(
                layers = doc.layers.map { layer ->
                    if (layer.id == layerId) block(layer) else layer
                },
            )
        }
    }

    fun updateText(layerId: Long, block: (LayerContent.Text) -> LayerContent.Text) {
        updateLayer(layerId) { layer ->
            val content = layer.content
            if (content is LayerContent.Text) layer.copy(content = block(content)) else layer
        }
    }

    fun updateShape(layerId: Long, block: (LayerContent.Shape) -> LayerContent.Shape) {
        updateLayer(layerId) { layer ->
            val content = layer.content
            if (content is LayerContent.Shape) layer.copy(content = block(content)) else layer
        }
    }

    fun updateImage(layerId: Long, block: (LayerContent.Image) -> LayerContent.Image) {
        updateLayer(layerId) { layer ->
            val content = layer.content
            if (content is LayerContent.Image) layer.copy(content = block(content)) else layer
        }
    }

    fun setLayerText(layerId: Long, text: String) {
        updateText(layerId) { it.copy(text = text) }
    }

    fun transformLayer(
        layerId: Long,
        centroidX: Float,
        centroidY: Float,
        panX: Float,
        panY: Float,
        zoom: Float,
        rotationDelta: Float,
        layerHeight: Float,
    ) {
        val current = document ?: return
        val layer = current.layer(layerId) ?: return
        if (layer.width <= 0f || layerHeight <= 0f) return
        val safeZoom = if (zoom.isFinite()) zoom.coerceIn(0.2f, 5f) else 1f
        val newWidth = (layer.width * safeZoom)
            .coerceIn(current.width * 0.04f, current.width * 3f)
        val applied = newWidth / layer.width
        val oldCenterX = layer.x + layer.width / 2f
        val oldCenterY = layer.y + layerHeight / 2f
        val newCenterX = centroidX + (oldCenterX - centroidX) * applied + panX
        val newCenterY = centroidY + (oldCenterY - centroidY) * applied + panY
        val newHeight = (layerHeight * applied).coerceAtLeast(1f)
        val rotation = if (rotationDelta.isFinite()) layer.rotation + rotationDelta else layer.rotation
        var next = layer.copy(
            x = newCenterX - newWidth / 2f,
            y = newCenterY - newHeight / 2f,
            width = newWidth,
            height = newHeight,
            rotation = rotation,
        )
        val content = next.content
        if (content is LayerContent.Text) {
            next = next.copy(
                content = content.copy(size = (content.size * applied).coerceAtLeast(6f)),
            )
        }
        document = current.copy(
            layers = current.layers.map { item -> if (item.id == layerId) next else item },
        )
    }

    fun addTextLayer(text: String) {
        val current = document ?: return
        val layer = DesignLayer(
            id = nextLayerId(current),
            x = current.safeInset,
            y = current.height * 0.42f,
            width = current.width - current.safeInset * 2f,
            height = current.width * 0.1f,
            content = LayerContent.Text(
                text = text,
                size = current.width * 0.075f,
                font = TextFont.SERIF,
                color = current.background.readableInk(),
            ),
        )
        edit { doc -> doc.copy(layers = doc.layers + layer) }
        selection = DesignSelection.Layer(layer.id)
    }

    fun addShapeLayer(kind: ShapeKind) {
        val current = document ?: return
        val fullWidth = current.width - current.safeInset * 2f
        val isLine = kind == ShapeKind.LINE || kind == ShapeKind.RULE
        val width = if (isLine) fullWidth else current.width * 0.34f
        val height = when (kind) {
            ShapeKind.RULE -> (current.width * 0.014f).coerceAtLeast(6f)
            ShapeKind.LINE -> (current.width * 0.012f).coerceAtLeast(6f)
            ShapeKind.RECTANGLE, ShapeKind.ELLIPSE, ShapeKind.STAR -> current.width * 0.28f
        }
        val layer = DesignLayer(
            id = nextLayerId(current),
            x = (current.width - width) / 2f,
            y = current.height * 0.46f,
            width = width,
            height = height,
            content = LayerContent.Shape(
                kind = kind,
                color = current.background.accentInk(),
                filled = true,
                strokeWidth = (current.width * 0.012f).coerceAtLeast(4f),
            ),
        )
        edit { doc -> doc.copy(layers = doc.layers + layer) }
        selection = DesignSelection.Layer(layer.id)
    }

    fun attachImage(layerId: Long?, uri: Uri, bitmap: Bitmap) {
        val current = document ?: return
        val existing = layerId?.let { current.layer(it) }
        if (layerId != null && existing != null && existing.content is LayerContent.Image) {
            // A replaced photo gets a fresh layer id: the old id stays reachable from the
            // undo stack, so its bitmap must not be overwritten or recycled in place.
            val replacementId = nextLayerId(current)
            bitmaps[replacementId] = bitmap
            val replacement = existing.copy(
                id = replacementId,
                content = (existing.content as LayerContent.Image).copy(uri = uri.toString()),
            )
            edit { doc ->
                doc.copy(layers = doc.layers.map { layer -> if (layer.id == layerId) replacement else layer })
            }
            selection = DesignSelection.Layer(replacementId)
            pruneBitmaps()
            return
        }
        val maxWidth = current.width - current.safeInset * 2f
        val aspect = if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height.toFloat() else 1f
        var width = maxWidth * 0.86f
        var height = width / aspect
        val maxHeight = current.height * 0.62f
        if (height > maxHeight) {
            height = maxHeight
            width = height * aspect
        }
        val layer = DesignLayer(
            id = nextLayerId(current),
            x = (current.width - width) / 2f,
            y = (current.height - height) / 2f,
            width = width,
            height = height,
            content = LayerContent.Image(uri = uri.toString()),
        )
        bitmaps[layer.id] = bitmap
        edit { doc -> doc.copy(layers = doc.layers + layer) }
        selection = DesignSelection.Layer(layer.id)
    }

    fun deleteLayer(layerId: Long) {
        edit { doc -> doc.copy(layers = doc.layers.filterNot { it.id == layerId }) }
        if ((selection as? DesignSelection.Layer)?.id == layerId) {
            selection = DesignSelection.None
        }
        pruneBitmaps()
    }

    fun deleteSelectedLayer() {
        val selected = selection as? DesignSelection.Layer ?: return
        deleteLayer(selected.id)
    }

    fun moveLayer(layerId: Long, towardTop: Boolean) {
        edit { doc -> reorder(doc, layerId, if (towardTop) 1 else -1) }
    }

    private fun reorder(document: DesignDocument, layerId: Long, delta: Int): DesignDocument {
        val index = document.layers.indexOfFirst { it.id == layerId }
        if (index < 0) return document
        val target = index + delta
        if (target !in document.layers.indices) return document
        val mutable = document.layers.toMutableList()
        val layer = mutable.removeAt(index)
        mutable.add(target, layer)
        return document.copy(layers = mutable)
    }

    fun setBackground(background: DesignBackground) {
        edit { doc -> doc.copy(background = background) }
    }

    /** Ids never repeat inside one document, even after a delete and an undo. */
    private fun nextLayerId(document: DesignDocument): Long {
        val maxExisting = document.layers.maxOfOrNull { layer -> layer.id } ?: 0L
        if (layerIdSeed <= maxExisting) layerIdSeed = maxExisting + 1L
        val id = layerIdSeed
        layerIdSeed += 1L
        return id
    }

    // ---- export ----------------------------------------------------------------

    suspend fun exportDocument(context: Context) {
        val current = document ?: return
        if (exportBusy()) return
        lastExportTarget = ExportTarget.DOCUMENT
        val png = current.background is DesignBackground.Transparent
        val format = if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        if (!ExportManager.hasRoomForImage(context, current.width, current.height, format)) {
            exportState = ExportState.Failed(ExportError.NO_SPACE)
            return
        }
        exportState = ExportState.Rendering
        try {
            val rendered = withContext(Dispatchers.Default) {
                DesignRenderer.renderDocument(current, bitmaps.toMap())
            }
            try {
                exportState = ExportState.Saving
                val outcome = ExportManager.saveImage(
                    context = context,
                    bitmap = rendered,
                    fileName = ExportManager.newImageName("MidadDesign"),
                    format = format,
                    quality = if (png) 100 else 95,
                )
                exportState = when (outcome) {
                    is ExportOutcome.Success -> ExportState.Success(outcome.result, png)
                    is ExportOutcome.Failure -> ExportState.Failed(outcome.error)
                }
            } finally {
                rendered.recycle()
            }
        } catch (cancellation: CancellationException) {
            exportState = ExportState.Idle
            throw cancellation
        } catch (failure: Throwable) {
            exportState = ExportState.Failed(ExportError.UNKNOWN)
        }
    }

    suspend fun exportCollage(context: Context) {
        val state = collage
        if (state.bitmaps.size < 2) {
            collage = state.copy(error = true)
            return
        }
        if (exportBusy()) return
        lastExportTarget = ExportTarget.COLLAGE
        exportState = ExportState.Rendering
        try {
            val spec = CollageSpec(
                aspect = state.aspect,
                layout = CollageLayouts.layoutFor(state.layoutId, state.bitmaps.size),
                spacing = state.spacing,
                background = state.background,
            )
            val rendered = withContext(Dispatchers.Default) {
                CollageRenderer.render(spec, state.bitmaps)
            }
            try {
                exportState = ExportState.Saving
                val outcome = ExportManager.saveImage(
                    context = context,
                    bitmap = rendered,
                    fileName = ExportManager.newImageName("MidadCollage"),
                    format = Bitmap.CompressFormat.JPEG,
                    quality = 95,
                )
                exportState = when (outcome) {
                    is ExportOutcome.Success -> ExportState.Success(outcome.result, false)
                    is ExportOutcome.Failure -> ExportState.Failed(outcome.error)
                }
            } finally {
                rendered.recycle()
            }
        } catch (cancellation: CancellationException) {
            exportState = ExportState.Idle
            throw cancellation
        } catch (failure: Throwable) {
            exportState = ExportState.Failed(ExportError.UNKNOWN)
        }
    }

    suspend fun retryExport(context: Context) {
        when (lastExportTarget) {
            ExportTarget.DOCUMENT -> exportDocument(context)
            ExportTarget.COLLAGE -> exportCollage(context)
        }
    }

    fun dismissExport() {
        if (exportState is ExportState.Rendering || exportState is ExportState.Saving) return
        exportState = ExportState.Idle
    }

    private fun exportBusy(): Boolean =
        exportState is ExportState.Rendering || exportState is ExportState.Saving

    // ---- collage ---------------------------------------------------------------

    suspend fun decodeImage(context: Context, uri: Uri, maxDimension: Int): Bitmap? =
        withContext(Dispatchers.IO) { BitmapTools.decodeSampled(context, uri, maxDimension) }

    suspend fun loadCollageImages(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        draftAllowed = true
        collage = collage.copy(loading = true, error = false)
        val decoded = withContext(Dispatchers.IO) {
            uris.mapNotNull { uri -> BitmapTools.decodeSampled(context, uri, 1024) }
        }
        if (decoded.size < 2) {
            decoded.forEach { bitmap -> BitmapTools.safeRecycle(bitmap) }
            collage = collage.copy(loading = false, error = true)
            return
        }
        collage.bitmaps.forEach { bitmap -> BitmapTools.safeRecycle(bitmap) }
        val accepted = decoded.take(MAX_COLLAGE_IMAGES)
        if (decoded.size > accepted.size) {
            decoded.drop(accepted.size).forEach { bitmap -> BitmapTools.safeRecycle(bitmap) }
        }
        collage = collage.copy(
            images = uris.take(accepted.size),
            bitmaps = accepted,
            layoutId = CollageLayouts.defaultFor(accepted.size).id,
            loading = false,
            error = false,
        )
        scheduleDraftSave()
    }

    fun selectCollageLayout(layoutId: String) {
        collage = collage.copy(layoutId = layoutId)
        scheduleDraftSave()
    }

    fun selectCollageAspect(aspect: CollageAspect) {
        collage = collage.copy(aspect = aspect)
        scheduleDraftSave()
    }

    fun setCollageSpacing(value: Int) {
        collage = collage.copy(spacing = value.coerceIn(0, 48))
        scheduleDraftSave()
    }

    fun setCollageBackground(color: Long) {
        collage = collage.copy(background = color)
        scheduleDraftSave()
    }

    fun dismissCollageError() {
        collage = collage.copy(error = false)
    }

    private fun clearCollage() {
        collage.bitmaps.forEach { bitmap -> BitmapTools.safeRecycle(bitmap) }
        collage = CollageState()
    }

    override fun onCleared() {
        super.onCleared()
        draftSaveJob?.cancel()
        scope.cancel()
        releaseDocumentBitmaps()
        collage.bitmaps.forEach { bitmap -> BitmapTools.safeRecycle(bitmap) }
    }

    companion object {
        private const val MAX_HISTORY = 40
        const val MAX_COLLAGE_IMAGES = 9
    }
}
