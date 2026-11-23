package com.midad.studio.core.draft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.midad.studio.studio.ai.AiBackdrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AiDraftStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = AiDraftStore(context)

    private fun bitmap(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    @Test
    fun sourceCutoutAndBackdropSurviveAFullCycle() {
        store.clear()
        val source = bitmap(64, 48, Color.rgb(180, 120, 60))
        val cutout = bitmap(64, 48, Color.TRANSPARENT)
        val draft = AiDraft(hasCutout = true, backdrop = AiBackdrop.PAPER)
        assertTrue(store.save(draft, source, cutout))
        assertTrue(store.hasDraft())

        val restored = store.load()
        assertNotNull(restored)
        assertEquals(64, restored!!.source.width)
        assertNotNull(restored.cutout)
        assertEquals(AiBackdrop.PAPER, restored.backdrop)
        store.clear()
        assertFalse(store.hasDraft())
        assertNull(store.load())
    }

    @Test
    fun transparentSourceOnlyDraftRestoresWithoutCutout() {
        store.clear()
        val source = bitmap(32, 32, Color.GRAY)
        assertTrue(store.save(AiDraft(hasCutout = false, backdrop = AiBackdrop.TRANSPARENT), source, null))
        val restored = store.load()
        assertNotNull(restored)
        assertNull(restored!!.cutout)
        assertEquals(AiBackdrop.TRANSPARENT, restored.backdrop)
        store.clear()
    }

    @Test
    fun anAbandonedSaveDoesNotCommitMetadata() {
        store.clear()
        val source = bitmap(16, 16, Color.WHITE)
        val saved = store.save(
            draft = AiDraft(hasCutout = true, backdrop = AiBackdrop.BLUR),
            source = source,
            cutout = source,
            shouldCommit = { false },
        )
        assertFalse(saved)
        assertFalse(store.hasDraft())
        assertNull(store.load())
    }

    @Test
    fun corruptedMetadataIsReportedAsNoDraft() {
        store.clear()
        val source = bitmap(16, 16, Color.WHITE)
        assertTrue(store.save(AiDraft(hasCutout = false, backdrop = AiBackdrop.WHITE), source, null))
        File(context.noBackupFilesDir, "ai-draft/draft.bin").writeBytes(byteArrayOf(1, 2))
        assertNull(store.load())
        store.clear()
    }
}
