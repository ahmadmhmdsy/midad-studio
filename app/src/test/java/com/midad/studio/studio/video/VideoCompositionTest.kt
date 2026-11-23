package com.midad.studio.studio.video

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoCompositionTest {

    private fun clip(secondsStart: Long, secondsEnd: Long): VideoExportSpec.Clip =
        VideoExportSpec.Clip(
            uri = Uri.parse("file:///clip.mp4"),
            startMs = secondsStart * 1_000L,
            endMs = secondsEnd * 1_000L,
        )

    private fun spec(muted: Boolean = false): VideoExportSpec = VideoExportSpec(
        clips = listOf(clip(1, 3), clip(0, 2)),
        look = VideoLook.WARM,
        fit = OutputFit.RATIO_9_16,
        muted = muted,
        title = VideoTitle(text = ""),
        outputWidth = 720,
        outputHeight = 1280,
    )

    @Test
    fun buildKeepsTheExactTrimmedDurations() {
        val composition = VideoComposition.build(spec(), null)
        assertEquals(1, composition.sequences.size)
        val items = composition.sequences[0].editedMediaItems
        assertEquals(2, items.size)
        assertEquals(2_000_000L, items[0].durationUs)
        assertEquals(2_000_000L, items[1].durationUs)
    }

    @Test
    fun buildCarriesMuteAndEffectsIntoEveryClip() {
        val composition = VideoComposition.build(spec(muted = true), null)
        val items = composition.sequences[0].editedMediaItems
        assertTrue(items.all { it.removeAudio })
        assertTrue(items.all { it.effects.videoEffects.isNotEmpty() })
    }
}
