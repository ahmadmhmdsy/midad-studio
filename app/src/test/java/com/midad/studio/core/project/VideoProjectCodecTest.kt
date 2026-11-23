package com.midad.studio.core.project

import androidx.test.core.app.ApplicationProvider
import com.midad.studio.core.draft.VideoDraftStore
import com.midad.studio.core.draft.VideoProjectClipPayload
import com.midad.studio.core.draft.VideoProjectCodec
import com.midad.studio.core.draft.VideoProjectPayload
import com.midad.studio.studio.video.MAX_VIDEO_PROJECT_DURATION_MS
import com.midad.studio.studio.video.OutputFit
import com.midad.studio.studio.video.TitleColor
import com.midad.studio.studio.video.TitlePosition
import com.midad.studio.studio.video.TitleSize
import com.midad.studio.studio.video.VideoLook
import com.midad.studio.studio.video.VideoTitle
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

private fun clip(
    uri: String = "content://media/video/1",
    duration: Long = 10_000L,
    start: Long = 0L,
    end: Long = duration,
) = VideoProjectClipPayload(
    uri = uri,
    name = "clip.mp4",
    durationMs = duration,
    trimStartMs = start,
    trimEndMs = end,
    frameWidth = 1920,
    frameHeight = 1080,
)

/** The versioned video payload is the only thing a video project keeps; it must round-trip and
 *  tolerate damaged or over-length data instead of failing to open. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoProjectCodecTest {

    @Test
    fun encodeDecodeRoundTrip() {
        val payload = VideoProjectPayload(
            clips = listOf(clip(), clip(uri = "content://media/video/2", duration = 7_500L)),
            look = VideoLook.WARM,
            fit = OutputFit.RATIO_9_16,
            muted = true,
            title = VideoTitle(
                text = "Hello",
                position = TitlePosition.TOP,
                size = TitleSize.LARGE,
                color = TitleColor.SAFFRON,
            ),
        )
        val decoded = VideoProjectCodec.decode(VideoProjectCodec.encode(payload))
        assertNotNull(decoded)
        assertEquals(2, decoded!!.clips.size)
        assertEquals(VideoLook.WARM, decoded.look)
        assertEquals(OutputFit.RATIO_9_16, decoded.fit)
        assertTrue(decoded.muted)
        assertEquals("Hello", decoded.title.text)
        assertEquals(TitlePosition.TOP, decoded.title.position)
        assertEquals(TitleSize.LARGE, decoded.title.size)
        assertEquals(TitleColor.SAFFRON, decoded.title.color)
        assertEquals(payload.totalDurationMs, decoded.totalDurationMs)
    }

    @Test
    fun unreadableInputIsRejectedWithoutThrowing() {
        assertNull(VideoProjectCodec.decode(null))
        assertNull(VideoProjectCodec.decode(""))
        assertNull(VideoProjectCodec.decode("not json"))
        assertNull(VideoProjectCodec.decode("{\"version\":99,\"clips\":[]}"))
    }

    @Test
    fun unknownEnumNamesFallBackToDefaults() {
        val json = JSONObject()
            .put("version", VideoProjectCodec.VERSION)
            .put("look", "SPARKLE")
            .put("fit", "TILTED")
            .put("clips", org.json.JSONArray().put(JSONObject().put("uri", "content://x").put("durationMs", 1000)))
        val decoded = VideoProjectCodec.decode(json.toString())
        assertNotNull(decoded)
        assertEquals(VideoLook.NONE, decoded!!.look)
        assertEquals(OutputFit.FIT, decoded.fit)
    }

    @Test
    fun unreadableClipUriIsSkippedButTheRestSurvives() {
        val clips = org.json.JSONArray()
        clips.put(JSONObject().put("uri", "").put("durationMs", 5000))
        clips.put(JSONObject().put("uri", "content://ok").put("durationMs", 5000))
        clips.put(JSONObject().put("uri", "content://zero").put("durationMs", 0))
        val json = JSONObject()
            .put("version", VideoProjectCodec.VERSION)
            .put("clips", clips)
        val decoded = VideoProjectCodec.decode(json.toString())
        assertNotNull(decoded)
        assertEquals(1, decoded!!.clips.size)
        assertEquals("content://ok", decoded.clips[0].uri)
    }

    @Test
    fun overLengthPayloadIsTrimmedToTheP1Cap() {
        val threeMinutes = 180_000L
        val payload = VideoProjectPayload(
            clips = listOf(
                clip(uri = "content://a", duration = 120_000L),
                clip(uri = "content://b", duration = 120_000L),
                clip(uri = "content://c", duration = 60_000L),
            ),
        )
        val decoded = VideoProjectCodec.decode(VideoProjectCodec.encode(payload))
        assertNotNull(decoded)
        assertTrue(decoded!!.totalDurationMs <= threeMinutes)
        assertEquals(2, decoded.clips.size)
        assertEquals(120_000L, decoded.clips[0].trimmedDurationMs)
        assertEquals(60_000L, decoded.clips[1].trimmedDurationMs)
        assertEquals(threeMinutes, decoded.totalDurationMs)
        assertEquals(MAX_VIDEO_PROJECT_DURATION_MS, threeMinutes)
    }

    @Test
    fun clampDropsClipsThatDoNotFit() {
        val payload = VideoProjectPayload(
            clips = listOf(
                clip(uri = "content://a", duration = 180_000L),
                clip(uri = "content://b", duration = 5_000L),
            ),
        )
        val clamped = VideoProjectCodec.clampToDurationCap(payload)
        assertEquals(1, clamped.clips.size)
        assertEquals(180_000L, clamped.totalDurationMs)
    }

    @Test
    fun encodeNeverWritesMoreThanTheCap() {
        val payload = VideoProjectPayload(
            clips = listOf(clip(uri = "content://a", duration = 300_000L)),
        )
        val decoded = VideoProjectCodec.decode(VideoProjectCodec.encode(payload))
        assertNotNull(decoded)
        assertTrue(decoded!!.totalDurationMs <= MAX_VIDEO_PROJECT_DURATION_MS)
    }
}

/** The draft store is the crash-recovery copy of a video project: one atomic JSON file. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoDraftStoreTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val store = VideoDraftStore(context)

    @Before
    fun setUp() {
        VideoDraftStore.rootDirectory(context).deleteRecursively()
    }

    @After
    fun tearDown() {
        VideoDraftStore.rootDirectory(context).deleteRecursively()
    }

    @Test
    fun saveLoadAndClear() {
        val payload = VideoProjectPayload(
            clips = listOf(clip(uri = "content://draft/1")),
            look = VideoLook.COOL,
        )
        assertTrue(store.save(payload))
        assertTrue(store.hasDraft())
        val loaded = store.load()
        assertNotNull(loaded)
        assertEquals(1, loaded!!.clips.size)
        assertEquals("content://draft/1", loaded.clips[0].uri)
        assertEquals(VideoLook.COOL, loaded.look)
        store.clear()
        assertTrue(!store.hasDraft())
        assertNull(store.load())
    }

    @Test
    fun saveRejectsAnEmptyTimelineAndLeavesNoFileBehind() {
        assertTrue(!store.save(VideoProjectPayload()))
        assertTrue(!store.hasDraft())
    }

    @Test
    fun damagedDraftLoadsAsNull() {
        val root = VideoDraftStore.rootDirectory(context)
        root.mkdirs()
        File(root, "draft.json").writeText("{ broken")
        assertNull(store.load())
    }

    @Test
    fun onlyOneDraftFileExistsAfterRepeatedSaves() {
        repeat(3) { index ->
            assertTrue(
                store.save(
                    VideoProjectPayload(clips = listOf(clip(uri = "content://draft/" + index))),
                ),
            )
        }
        val files = VideoDraftStore.rootDirectory(context).listFiles().orEmpty()
        assertEquals(1, files.size)
        assertTrue(files[0].length() > 0L)
    }
}
