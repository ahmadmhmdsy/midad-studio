package com.midad.studio.studio.video

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.midad.studio.core.export.ExportError
import com.midad.studio.core.export.ExportManager
import com.midad.studio.core.draft.VideoDraftStore
import com.midad.studio.core.draft.VideoProjectCodec
import com.midad.studio.core.draft.VideoProjectPayload
import com.midad.studio.core.draft.toProjectClip
import com.midad.studio.core.draft.toVideoClip
import com.midad.studio.core.export.ExportOutcome
import com.midad.studio.core.image.BitmapTools
import com.midad.studio.core.media.MediaVideoHelper
import com.midad.studio.core.project.ProjectKind
import com.midad.studio.core.project.ProjectRepository
import com.midad.studio.core.project.ProjectStore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single source of truth for the video studio. Reading metadata/thumbnails happens on
 * [Dispatchers.IO]; the export itself is owned by [VideoExporter] and cancelled on clear.
 */
class VideoStudioViewModel : ViewModel() {

    var state by mutableStateOf(VideoUiState())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var exportJob: Job? = null
    private var exporter: VideoExporter? = null

    private var draftStore: VideoDraftStore? = null
    private var draftJob: Job? = null
    private var draftEpoch = 0
    private var draftAllowed = true
    private var draftContext: Context? = null
    private var restoredAttempted = false
    private var leaving = false

    fun addClips(context: Context, uris: List<Uri>) {
        if (uris.isEmpty() || state.loading) return
        val appContext = context.applicationContext
        val existing = state.clips
        val capacity = MAX_VIDEO_CLIPS - existing.size
        if (capacity <= 0) {
            state = state.copy(notice = VideoNotice.LIMIT_REACHED)
            return
        }
        val accepted = uris.take(capacity)
        val overflowed = uris.size > accepted.size
        state = state.copy(loading = true, notice = null)
        scope.launch {
            val loaded = try {
                withContext(Dispatchers.IO) {
                    val result = mutableListOf<VideoClip>()
                    for (uri in accepted) {
                        persistReadPermission(appContext, uri)
                        val clip = try {
                            loadClip(appContext, uri)
                        } catch (t: Throwable) {
                            if (t is CancellationException) throw t
                            Log.w(TAG, "Skipping a video that could not be read", t)
                            null
                        }
                        if (clip != null) result.add(clip)
                    }
                    result
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "Reading the selected videos failed", t)
                emptyList()
            }
            val unreadable = accepted.size - loaded.size
            // P1 length policy: keep the project inside MAX_VIDEO_PROJECT_DURATION_MS, trimming the
            // clip that crosses the boundary and dropping whatever would exceed it.
            var budget = (MAX_VIDEO_PROJECT_DURATION_MS -
                existing.sumOf { it.trimmedDurationMs }).coerceAtLeast(0L)
            val kept = ArrayList<VideoClip>(loaded.size)
            var capHit = false
            for (clip in loaded) {
                if (budget < MIN_CLIP_DURATION_MS) {
                    capHit = true
                    break
                }
                if (clip.trimmedDurationMs <= budget) {
                    kept.add(clip)
                    budget -= clip.trimmedDurationMs
                } else {
                    kept.add(clip.copy(trimEndMs = clip.trimStartMs + budget))
                    budget = 0L
                    capHit = true
                }
            }
            state = state.copy(
                clips = existing + kept,
                selectedClipId = state.selectedClipId ?: kept.firstOrNull()?.id,
                loading = false,
                notice = when {
                    loaded.isEmpty() -> VideoNotice.ALL_UNREADABLE
                    overflowed -> VideoNotice.LIMIT_REACHED
                    capHit -> VideoNotice.DURATION_LIMIT
                    unreadable > 0 -> VideoNotice.SOME_UNREADABLE
                    else -> null
                },
            )
            scheduleDraftSave()
        }
    }

    // ---- draft + projects -------------------------------------------------------

    /** Reopens an interrupted edit after process death; an explicit exit clears the draft. */
    fun restoreDraftIfAny(context: Context) {
        if (restoredAttempted) return
        restoredAttempted = true
        if (state.clips.isNotEmpty()) return
        val appContext = context.applicationContext
        draftContext = appContext
        val store = draftStore ?: VideoDraftStore(appContext).also { draftStore = it }
        if (!store.hasDraft()) return
        scope.launch {
            val payload = withContext(Dispatchers.IO) { store.load() }
            if (payload == null) {
                withContext(Dispatchers.IO) { store.clear() }
                return@launch
            }
            applyPayload(appContext, payload, projectId = null, projectTitle = null)
        }
    }

    /** Reopens a saved video project from its versioned JSON payload. */
    fun openProject(context: Context, projectId: String) {
        if (restoredAttempted) return
        restoredAttempted = true
        if (state.clips.isNotEmpty()) return
        val appContext = context.applicationContext
        draftContext = appContext
        scope.launch {
            ProjectRepository.ensureLoaded(appContext)
            val record = ProjectRepository.find(projectId)
            val payload = VideoProjectCodec.decode(
                ProjectRepository.loadPayload(appContext, projectId),
            )
            if (payload == null) {
                state = state.copy(
                    notice = VideoNotice.ALL_UNREADABLE,
                    projectId = record?.id ?: projectId,
                    projectTitle = record?.title,
                )
                return@launch
            }
            applyPayload(appContext, payload, record?.id ?: projectId, record?.title)
        }
    }

    /**
     * Rebuilds the timeline from a payload. A clip whose content URI can no longer be read is
     * skipped (the user may have moved or removed the file) and the rest still opens; an
     * over-length payload is trimmed here as well as in the codec.
     */
    private suspend fun applyPayload(
        context: Context,
        payload: VideoProjectPayload,
        projectId: String?,
        projectTitle: String?,
    ) {
        val capped = VideoProjectCodec.clampToDurationCap(payload)
        val clips = withContext(Dispatchers.IO) {
            capped.clips.mapIndexedNotNull { index, clip ->
                val uri = runCatching { Uri.parse(clip.uri) }.getOrNull()
                    ?: return@mapIndexedNotNull null
                persistReadPermission(context, uri)
                val duration = MediaVideoHelper.durationMs(context, uri)
                val thumbnail = MediaVideoHelper.thumbnail(context, uri, THUMBNAIL_SIZE_PX)
                if (duration <= 0L && thumbnail == null) return@mapIndexedNotNull null
                clip.toVideoClip(index, thumbnail)
            }
        }
        val skipped = capped.clips.size - clips.size
        val cappedForLimit = payload.totalDurationMs > capped.totalDurationMs
        state = state.copy(
            clips = clips,
            selectedClipId = clips.firstOrNull()?.id,
            look = capped.look,
            fit = capped.fit,
            muted = capped.muted,
            title = capped.title,
            loading = false,
            projectId = projectId,
            projectTitle = projectTitle,
            notice = when {
                clips.isEmpty() -> VideoNotice.ALL_UNREADABLE
                skipped > 0 -> VideoNotice.SOME_UNREADABLE
                cappedForLimit -> VideoNotice.DURATION_LIMIT
                else -> null
            },
        )
        draftAllowed = true
        draftEpoch++
        if (clips.isNotEmpty()) scheduleDraftSave()
    }

    /**
     * Flushes the draft and copies it into the projects library. Returns false when the draft or
     * the library write failed; the timeline is kept either way.
     */
    suspend fun saveProject(context: Context, title: String): Boolean {
        val appContext = context.applicationContext
        draftContext = appContext
        val payload = state.toProjectPayload()
        if (payload.clips.isEmpty()) return false
        draftJob?.cancel()
        draftJob = null
        val store = draftStore ?: VideoDraftStore(appContext).also { draftStore = it }
        if (!withContext(Dispatchers.IO) { store.save(payload) }) return false
        val thumbnail = withContext(Dispatchers.IO) {
            state.clips.firstOrNull()?.uri?.let { uri ->
                MediaVideoHelper.thumbnail(appContext, uri, THUMBNAIL_SIZE_PX)
            }
        }
        val record = try {
            ProjectRepository.savePayload(
                context = appContext,
                projectId = state.projectId,
                kind = ProjectKind.VIDEO,
                title = title,
                payload = VideoProjectCodec.encode(payload).toByteArray(Charsets.UTF_8),
                thumbnail = thumbnail,
            )
        } finally {
            thumbnail?.let { bitmap -> BitmapTools.safeRecycle(bitmap) }
        } ?: return false
        state = state.copy(projectId = record.id, projectTitle = record.title)
        return true
    }

    /**
     * Explicit exit. A bound project is flushed one last time before the crash-recovery draft is
     * cleared; without a project this is the old discard path.
     */
    fun leaveStudio(context: Context, onExit: () -> Unit) {
        if (leaving) return
        leaving = true
        val appContext = context.applicationContext
        val projectTitle = state.projectTitle
        if (state.projectId == null || projectTitle == null || state.clips.isEmpty()) {
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

    /** Leaves the video studio for good; the draft must not survive an explicit exit. */
    fun discardDraft() {
        draftAllowed = false
        draftEpoch++
        draftJob?.cancel()
        draftJob = null
        draftStore?.clear()
    }

    /** Debounced mirror of the timeline in the draft folder and, when bound, in the project. */
    private fun scheduleDraftSave() {
        val appContext = draftContext ?: return
        if (!draftAllowed || state.clips.isEmpty()) return
        draftJob?.cancel()
        val epoch = draftEpoch
        draftJob = scope.launch {
            delay(DRAFT_DEBOUNCE_MS)
            val payload = state.toProjectPayload()
            if (payload.clips.isEmpty()) return@launch
            val store = draftStore ?: VideoDraftStore(appContext).also { draftStore = it }
            val saved = withContext(Dispatchers.IO) {
                if (draftAllowed && epoch == draftEpoch) store.save(payload) else false
            }
            if (!saved) return@launch
            val projectId = state.projectId
            val projectTitle = state.projectTitle
            if (projectId != null && projectTitle != null) {
                ProjectRepository.savePayload(
                    context = appContext,
                    projectId = projectId,
                    kind = ProjectKind.VIDEO,
                    title = projectTitle,
                    payload = VideoProjectCodec.encode(payload).toByteArray(Charsets.UTF_8),
                    thumbnail = null,
                )
            }
        }
    }

    private fun VideoUiState.toProjectPayload(): VideoProjectPayload = VideoProjectPayload(
        clips = clips.map { clip -> clip.toProjectClip() },
        look = look,
        fit = fit,
        muted = muted,
        title = title,
    )

    /**
     * Keeps the picker grant across process death and app restarts, so a saved video project can
     * reopen its clips later. Providers that do not offer a persistable grant simply reject it.
     */
    private fun persistReadPermission(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private suspend fun loadClip(context: Context, uri: Uri): VideoClip? {
        val duration = MediaVideoHelper.durationMs(context, uri)
        val thumbnail = MediaVideoHelper.thumbnail(context, uri, THUMBNAIL_SIZE_PX)
        val name = MediaVideoHelper.displayName(context, uri)
        val size = VideoProbe.size(context, uri)
        if (duration <= 0L && thumbnail == null) return null
        val safeDuration = if (duration > 0L) duration else FALLBACK_CLIP_DURATION_MS
        return VideoClip(
            id = UUID.randomUUID().toString(),
            uri = uri,
            name = name.orEmpty().trim(),
            durationMs = safeDuration,
            trimStartMs = 0L,
            trimEndMs = safeDuration,
            frameWidth = size?.first ?: 0,
            frameHeight = size?.second ?: 0,
            thumbnail = thumbnail,
        )
    }

    fun selectClip(clipId: String) {
        if (state.selectedClipId == clipId) return
        state = state.copy(selectedClipId = clipId)
    }

    fun updateTrim(clipId: String, startMs: Long, endMs: Long) {
        val clips = state.clips
        val index = clips.indexOfFirst { it.id == clipId }
        if (index < 0) return
        val clip = clips[index]
        val duration = clip.durationMs.coerceAtLeast(1L)
        val maxStart = (duration - MIN_CLIP_DURATION_MS).coerceAtLeast(0L)
        val start = startMs.coerceIn(0L, maxStart)
        val minEnd = (start + MIN_CLIP_DURATION_MS).coerceAtMost(duration)
        val end = endMs.coerceIn(minEnd, duration)
        if (clip.trimStartMs == start && clip.trimEndMs == end) return
        val updated = clips.toMutableList()
        updated[index] = clip.copy(trimStartMs = start, trimEndMs = end)
        state = state.copy(clips = updated)
        scheduleDraftSave()
    }

    fun moveClip(clipId: String, offset: Int) {
        val clips = state.clips
        val index = clips.indexOfFirst { it.id == clipId }
        if (index < 0) return
        val target = index + offset
        if (target !in clips.indices || target == index) return
        val reordered = clips.toMutableList()
        val moved = reordered.removeAt(index)
        reordered.add(target, moved)
        state = state.copy(clips = reordered)
        scheduleDraftSave()
    }

    fun deleteClip(clipId: String) {
        val clips = state.clips
        val index = clips.indexOfFirst { it.id == clipId }
        if (index < 0) return
        val remaining = clips.toMutableList().also { it.removeAt(index) }
        val nextSelection = when {
            state.selectedClipId != clipId -> state.selectedClipId
            remaining.isEmpty() -> null
            else -> remaining[index.coerceAtMost(remaining.lastIndex)].id
        }
        state = state.copy(clips = remaining, selectedClipId = nextSelection)
        scheduleDraftSave()
    }

    fun setLook(look: VideoLook) {
        state = state.copy(look = look)
        scheduleDraftSave()
    }

    fun setFit(fit: OutputFit) {
        state = state.copy(fit = fit)
        scheduleDraftSave()
    }

    fun setMuted(muted: Boolean) {
        state = state.copy(muted = muted)
        scheduleDraftSave()
    }

    fun setTitleText(text: String) {
        state = state.copy(title = state.title.copy(text = text.take(MAX_TITLE_CHARS)))
        scheduleDraftSave()
    }

    fun setTitlePosition(position: TitlePosition) {
        state = state.copy(title = state.title.copy(position = position))
        scheduleDraftSave()
    }

    fun setTitleSize(size: TitleSize) {
        state = state.copy(title = state.title.copy(size = size))
        scheduleDraftSave()
    }

    fun setTitleColor(color: TitleColor) {
        state = state.copy(title = state.title.copy(color = color))
        scheduleDraftSave()
    }

    fun clearNotice() {
        if (state.notice != null) state = state.copy(notice = null)
    }

    fun startExport(context: Context) {
        val current = state
        if (current.clips.isEmpty() || current.busy) return
        val appContext = context.applicationContext
        val spec = current.toExportSpec()
        val newExporter = VideoExporter(appContext)
        exporter = newExporter
        state = current.copy(export = VideoExportState.Running(null), notice = null)
        exportJob = scope.launch {
            var tempFile: File? = null
            try {
                val rendered = newExporter.export(spec) { progress ->
                    val export = state.export
                    if (export is VideoExportState.Running) {
                        state = state.copy(export = VideoExportState.Running(progress))
                    }
                }
                tempFile = rendered
                state = state.copy(export = VideoExportState.Saving)
                val outcome = ExportManager.saveVideo(appContext, rendered, ExportManager.newVideoName())
                state = state.copy(
                    export = when (outcome) {
                        is ExportOutcome.Success -> VideoExportState.Success(
                            outcome.result.fileName,
                            outcome.result.uri,
                            outcome.result.inGallery,
                        )
                        is ExportOutcome.Failure -> VideoExportState.Failure(
                            when (outcome.error) {
                                ExportError.NO_SPACE -> VideoExportError.NO_SPACE
                                ExportError.IO -> VideoExportError.IO
                                ExportError.ENCODE, ExportError.UNKNOWN -> VideoExportError.SAVE
                            },
                        )
                    },
                )
            } catch (cancelled: CancellationException) {
                state = state.copy(export = VideoExportState.Idle)
            } catch (t: Throwable) {
                Log.w(TAG, "Video export failed", t)
                state = state.copy(export = VideoExportState.Failure(VideoExporter.classify(t)))
            } finally {
                tempFile?.let { file -> runCatching { file.delete() } }
                newExporter.releaseTitleBitmap()
                if (exporter === newExporter) exporter = null
                if (exportJob === coroutineContext[Job]) exportJob = null
            }
        }
    }

    fun cancelExport() {
        val export = state.export
        if (export !is VideoExportState.Running && export !is VideoExportState.Saving) return
        exporter?.cancel()
        exportJob?.cancel()
        exportJob = null
        state = state.copy(export = VideoExportState.Idle)
    }

    fun dismissExportResult() {
        val export = state.export
        if (export is VideoExportState.Success || export is VideoExportState.Failure) {
            state = state.copy(export = VideoExportState.Idle)
        }
    }

    fun openResult(context: Context, uri: Uri) {
        val fileName = (state.export as? VideoExportState.Success)?.fileName.orEmpty()
        runCatching {
            ExportManager.open(context, uri, ExportManager.mimeTypeFor(fileName, ProjectStore.KIND_VIDEO))
        }.onFailure { Log.w(TAG, "Opening the exported video failed", it) }
    }

    fun shareResult(context: Context, uri: Uri) {
        runCatching {
            ExportManager.share(context, uri, ExportManager.mimeTypeFor(ProjectStore.KIND_VIDEO))
        }.onFailure { Log.w(TAG, "Sharing the exported video failed", it) }
    }

    override fun onCleared() {
        exportJob?.cancel()
        exporter?.cancel()
        exporter = null
        exportJob = null
        scope.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "VideoStudio"
        const val THUMBNAIL_SIZE_PX = 512
        const val MAX_TITLE_CHARS = 120
        const val DRAFT_DEBOUNCE_MS = 600L
    }
}
