package com.midad.studio.core.draft

import android.graphics.Bitmap
import com.midad.studio.studio.video.MAX_VIDEO_CLIPS
import com.midad.studio.studio.video.MAX_VIDEO_PROJECT_DURATION_MS
import com.midad.studio.studio.video.MIN_CLIP_DURATION_MS
import com.midad.studio.studio.video.OutputFit
import com.midad.studio.studio.video.TitleColor
import com.midad.studio.studio.video.TitlePosition
import com.midad.studio.studio.video.TitleSize
import com.midad.studio.studio.video.VideoClip
import com.midad.studio.studio.video.VideoLook
import com.midad.studio.studio.video.VideoTitle
import org.json.JSONArray
import org.json.JSONObject

/** One clip as it is written into a video project payload; the live Bitmap thumbnail is not persisted. */
data class VideoProjectClipPayload(
    val uri: String,
    val name: String,
    val durationMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val frameWidth: Int,
    val frameHeight: Int,
) {
    val trimmedDurationMs: Long get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}

/**
 * Everything a video project needs to be reopened: source clip URIs, trims, frame size and the
 * output settings. The clips themselves stay in the user's picker/gallery scope; a URI that can no
 * longer be read is skipped when the project is opened, never treated as fatal.
 */
data class VideoProjectPayload(
    val clips: List<VideoProjectClipPayload> = emptyList(),
    val look: VideoLook = VideoLook.NONE,
    val fit: OutputFit = OutputFit.FIT,
    val muted: Boolean = false,
    val title: VideoTitle = VideoTitle(),
) {
    val totalDurationMs: Long get() = clips.sumOf { it.trimmedDurationMs }
}

fun VideoClip.toProjectClip(): VideoProjectClipPayload = VideoProjectClipPayload(
    uri = uri.toString(),
    name = name,
    durationMs = durationMs.coerceAtLeast(1L),
    trimStartMs = trimStartMs,
    trimEndMs = trimEndMs.coerceAtLeast(trimStartMs + 1L),
    frameWidth = frameWidth,
    frameHeight = frameHeight,
)

fun VideoProjectClipPayload.toVideoClip(index: Int, thumbnail: Bitmap? = null): VideoClip = VideoClip(
    id = "restored-" + index + "-" + uri.hashCode(),
    uri = android.net.Uri.parse(uri),
    name = name,
    durationMs = durationMs,
    trimStartMs = trimStartMs,
    trimEndMs = trimEndMs,
    frameWidth = frameWidth,
    frameHeight = frameHeight,
    thumbnail = thumbnail,
)

/**
 * Versioned JSON codec for the video project payload. Decoding is deliberately tolerant: a damaged
 * clip is skipped, unknown enum names fall back to the default look/fit, and a payload longer than
 * the P1 duration cap is trimmed to fit instead of being rejected, so an old or hand-edited file
 * still opens with a localized notice.
 */
object VideoProjectCodec {

    const val VERSION: Int = 1
    const val MAX_TITLE_CHARS: Int = 120

    fun encode(payload: VideoProjectPayload): String {
        val capped = clampToDurationCap(payload)
        val clips = JSONArray()
        for (clip in capped.clips.take(MAX_VIDEO_CLIPS)) {
            clips.put(
                JSONObject().apply {
                    put("uri", clip.uri)
                    put("name", clip.name)
                    put("durationMs", clip.durationMs)
                    put("trimStartMs", clip.trimStartMs)
                    put("trimEndMs", clip.trimEndMs)
                    put("frameWidth", clip.frameWidth)
                    put("frameHeight", clip.frameHeight)
                },
            )
        }
        val title = JSONObject().apply {
            put("text", capped.title.text.take(MAX_TITLE_CHARS))
            put("position", capped.title.position.name)
            put("size", capped.title.size.name)
            put("color", capped.title.color.name)
        }
        return JSONObject().apply {
            put("version", VERSION)
            put("clips", clips)
            put("look", capped.look.name)
            put("fit", capped.fit.name)
            put("muted", capped.muted)
            put("title", title)
        }.toString()
    }

    /** Null only when the text is not a readable payload of a known version. */
    fun decode(text: String?): VideoProjectPayload? {
        if (text.isNullOrBlank()) return null
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (json.optInt("version", 0) != VERSION) return null
        val array = json.optJSONArray("clips") ?: JSONArray()
        val clips = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val uri = item.optString("uri").orEmpty()
                if (uri.isBlank()) continue
                val duration = item.optLong("durationMs", 0L)
                if (duration <= 0L) continue
                val start = item.optLong("trimStartMs", 0L).coerceIn(0L, duration - 1L)
                val end = item.optLong("trimEndMs", duration)
                    .coerceIn(start + 1L, duration)
                if (end - start < 1L) continue
                add(
                    VideoProjectClipPayload(
                        uri = uri,
                        name = item.optString("name").orEmpty().ifBlank { uri },
                        durationMs = duration,
                        trimStartMs = start,
                        trimEndMs = end,
                        frameWidth = item.optInt("frameWidth", 0).coerceAtLeast(0),
                        frameHeight = item.optInt("frameHeight", 0).coerceAtLeast(0),
                    ),
                )
            }
        }
        val titleJson = json.optJSONObject("title") ?: JSONObject()
        val title = VideoTitle(
            text = titleJson.optString("text").orEmpty().take(MAX_TITLE_CHARS),
            position = enumOr(titleJson.optString("position"), TitlePosition.BOTTOM),
            size = enumOr(titleJson.optString("size"), TitleSize.MEDIUM),
            color = enumOr(titleJson.optString("color"), TitleColor.PAPER),
        )
        val payload = VideoProjectPayload(
            clips = clips.take(MAX_VIDEO_CLIPS),
            look = enumOr(json.optString("look"), VideoLook.NONE),
            fit = enumOr(json.optString("fit"), OutputFit.FIT),
            muted = json.optBoolean("muted", false),
            title = title,
        )
        return clampToDurationCap(payload)
    }

    /**
     * Trims clips until the trimmed total fits [maxDurationMs]. The clip that crosses the cap is cut
     * at the boundary when at least [MIN_CLIP_DURATION_MS] remains, later clips are dropped.
     */
    fun clampToDurationCap(
        payload: VideoProjectPayload,
        maxDurationMs: Long = MAX_VIDEO_PROJECT_DURATION_MS,
    ): VideoProjectPayload {
        if (payload.clips.isEmpty() || payload.totalDurationMs <= maxDurationMs) return payload
        var budget = maxDurationMs
        val kept = ArrayList<VideoProjectClipPayload>(payload.clips.size)
        for (clip in payload.clips) {
            if (budget < MIN_CLIP_DURATION_MS) break
            val trimmed = clip.trimmedDurationMs
            if (trimmed <= budget) {
                kept.add(clip)
                budget -= trimmed
            } else {
                kept.add(clip.copy(trimEndMs = clip.trimStartMs + budget))
                budget = 0L
            }
        }
        return payload.copy(clips = kept)
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(fallback)
}
