package com.midad.studio.studio.video

import android.graphics.Bitmap
import android.net.Uri

/** A single source clip in the project timeline. All trim values are milliseconds. */
data class VideoClip(
    val id: String,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val thumbnail: Bitmap? = null,
) {
    val trimmedDurationMs: Long get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}

/** Colour looks, all built from stable Media3 effects in VideoEffects. */
enum class VideoLook { NONE, WARM, COOL, NOIR, PUNCH }

/** Output framing: keep the whole frame, crop to fill, or a fixed social ratio. */
enum class OutputFit { FIT, FILL, RATIO_9_16, RATIO_1_1, RATIO_16_9 }

enum class TitlePosition { TOP, CENTER, BOTTOM }

enum class TitleSize(val fractionOfWidth: Float) {
    SMALL(0.032f),
    MEDIUM(0.050f),
    LARGE(0.072f),
}

/** Colours are data from the Midad palette, not hardcoded UI colours. */
enum class TitleColor(val argb: Int) {
    PAPER(0xFFF4EFE5.toInt()),
    INK(0xFF1C1915.toInt()),
    SAFFRON(0xFFC8923F.toInt()),
    TERRACOTTA(0xFFB1552F.toInt()),
}

data class VideoTitle(
    val text: String = "",
    val position: TitlePosition = TitlePosition.BOTTOM,
    val size: TitleSize = TitleSize.MEDIUM,
    val color: TitleColor = TitleColor.PAPER,
)

enum class VideoNotice { LIMIT_REACHED, SOME_UNREADABLE, ALL_UNREADABLE, DURATION_LIMIT }

enum class VideoExportError { GENERIC, DECODE, ENCODE, IO, EFFECT, SAVE, NO_SPACE }

sealed interface VideoExportState {
    data object Idle : VideoExportState
    data class Running(val progress: Float?) : VideoExportState
    data object Saving : VideoExportState
    data class Success(val fileName: String, val uri: Uri, val inGallery: Boolean) : VideoExportState
    data class Failure(val error: VideoExportError) : VideoExportState
}

data class VideoUiState(
    val clips: List<VideoClip> = emptyList(),
    val selectedClipId: String? = null,
    val look: VideoLook = VideoLook.NONE,
    val fit: OutputFit = OutputFit.FIT,
    val muted: Boolean = false,
    val title: VideoTitle = VideoTitle(),
    val loading: Boolean = false,
    val notice: VideoNotice? = null,
    val export: VideoExportState = VideoExportState.Idle,
    /** The saved project this edit belongs to, null until the first save. */
    val projectId: String? = null,
    val projectTitle: String? = null,
) {
    val selectedClip: VideoClip? get() = clips.firstOrNull { it.id == selectedClipId }
    val selectedIndex: Int get() = clips.indexOfFirst { it.id == selectedClipId }
    val totalDurationMs: Long get() = clips.sumOf { it.trimmedDurationMs }
    val busy: Boolean
        get() = export is VideoExportState.Running || export is VideoExportState.Saving
}

/** Immutable snapshot handed to the exporter; the UI can keep changing meanwhile. */
data class VideoExportSpec(
    val clips: List<Clip>,
    val look: VideoLook,
    val fit: OutputFit,
    val muted: Boolean,
    val title: VideoTitle,
    val outputWidth: Int,
    val outputHeight: Int,
) {
    data class Clip(val uri: Uri, val startMs: Long, val endMs: Long)
}

const val MAX_VIDEO_CLIPS: Int = 5
const val MIN_CLIP_DURATION_MS: Long = 300L
const val FALLBACK_CLIP_DURATION_MS: Long = 5_000L

/**
 * P1 length policy: three minutes of trimmed footage per project and export. There is no
 * foreground-service permission, so a longer encode could not run reliably while the app is
 * backgrounded; capping the project keeps an export inside one stay in the app.
 */
const val MAX_VIDEO_PROJECT_DURATION_MS: Long = 180_000L

internal fun VideoUiState.toExportSpec(): VideoExportSpec {
    val (width, height) = outputSizeFor(fit, clips)
    return VideoExportSpec(
        clips = clips.map { VideoExportSpec.Clip(it.uri, it.trimStartMs, it.trimEndMs) },
        look = look,
        fit = fit,
        muted = muted,
        title = title.copy(text = title.text.trim()),
        outputWidth = width,
        outputHeight = height,
    )
}

/** The output canvas grows from the first clip for Fit/Fill and is fixed for ratio modes. */
internal fun outputSizeFor(fit: OutputFit, clips: List<VideoClip>): Pair<Int, Int> {
    when (fit) {
        OutputFit.RATIO_9_16 -> return 720 to 1280
        OutputFit.RATIO_1_1 -> return 1080 to 1080
        OutputFit.RATIO_16_9 -> return 1280 to 720
        OutputFit.FIT, OutputFit.FILL -> Unit
    }
    val first = clips.firstOrNull()
    val width = first?.frameWidth ?: 0
    val height = first?.frameHeight ?: 0
    if (width <= 0 || height <= 0) return 1280 to 720
    return scaleToLongEdge(width, height, 1280)
}

private fun scaleToLongEdge(width: Int, height: Int, maxLongEdge: Int): Pair<Int, Int> {
    val longEdge = maxOf(width, height)
    val scale = if (longEdge > maxLongEdge) maxLongEdge.toFloat() / longEdge else 1f
    val outWidth = ((width * scale).toInt().coerceAtLeast(2) / 2) * 2
    val outHeight = ((height * scale).toInt().coerceAtLeast(2) / 2) * 2
    return outWidth to outHeight
}
