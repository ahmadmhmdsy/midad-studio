package com.midad.studio.core.draft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.midad.studio.studio.design.DesignScreen
import com.midad.studio.studio.design.collage.CollageAspect
import com.midad.studio.studio.design.model.CanvasPreset
import com.midad.studio.studio.design.model.DesignBackground
import com.midad.studio.studio.design.model.DesignDocument
import com.midad.studio.studio.design.model.DesignLayer
import com.midad.studio.studio.design.model.DesignPalette
import com.midad.studio.studio.design.model.LayerContent
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
class DesignDraftStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = DesignDraftStore(context)

    private fun bitmap(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun document(id: Long = 1L) = DesignDocument(
        preset = CanvasPreset.SQUARE,
        background = DesignBackground.Solid(DesignPalette.PAPER),
        layers = listOf(
            DesignLayer(
                id = id,
                x = 0f,
                y = 0f,
                width = 100f,
                height = 100f,
                content = LayerContent.Image(uri = "content://photo"),
            ),
        ),
    )

    @Test
    fun documentImagesAndCollageTilesSurviveAFullCycle() {
        store.clear()
        val layerBitmap = bitmap(40, 30, Color.rgb(200, 100, 50))
        val first = bitmap(20, 20, Color.rgb(10, 20, 30))
        val second = bitmap(24, 18, Color.rgb(90, 80, 70))
        val draft = DesignDraft(
            screen = DesignScreen.COLLAGE,
            document = document(),
            collage = CollageDraft(
                images = listOf("content://a", "content://b"),
                layoutId = "duo",
                aspect = CollageAspect.WIDE,
                spacing = 8,
                background = DesignPalette.SAND,
            ),
        )
        assertTrue(store.save(draft, mapOf(1L to layerBitmap), listOf(first, second), writeImages = true))
        assertTrue(store.hasDraft())

        val restored = store.load()
        assertNotNull(restored)
        assertEquals(1, restored!!.documentBitmaps.size)
        assertEquals(40, restored.documentBitmaps.getValue(1L).width)
        assertEquals(2, restored.collageBitmaps.size)
        assertEquals(24, restored.collageBitmaps[1].width)
        assertEquals(draft, restored.draft)
        store.clear()
        assertFalse(store.hasDraft())
        assertNull(store.load())
    }

    @Test
    fun metadataOnlySaveKeepsExistingPixels() {
        store.clear()
        val layerBitmap = bitmap(12, 12, Color.RED)
        val draft = DesignDraft(DesignScreen.EDITOR, document(), null)
        assertTrue(store.save(draft, mapOf(1L to layerBitmap), emptyList(), writeImages = true))
        val moved = draft.copy(document = document().let { doc ->
            doc.copy(layers = doc.layers.map { it.copy(x = 50f) })
        })
        assertTrue(store.save(moved, mapOf(1L to layerBitmap), emptyList(), writeImages = false))
        val restored = store.load()
        assertEquals(50f, restored!!.draft.document!!.layers.single().x, 0f)
        store.clear()
    }

    @Test
    fun anAbandonedSaveDoesNotCommitMetadata() {
        store.clear()
        val layerBitmap = bitmap(8, 8, Color.BLUE)
        val draft = DesignDraft(DesignScreen.EDITOR, document(), null)
        val saved = store.save(
            draft = draft,
            documentBitmaps = mapOf(1L to layerBitmap),
            collageBitmaps = emptyList(),
            writeImages = true,
            shouldCommit = { false },
        )
        assertFalse(saved)
        assertFalse(store.hasDraft())
        assertNull(store.load())
    }

    @Test
    fun missingImageFileMakesTheDraftUnreadableInsteadOfHalfRestored() {
        store.clear()
        val layerBitmap = bitmap(10, 10, Color.GREEN)
        val draft = DesignDraft(DesignScreen.EDITOR, document(), null)
        assertTrue(store.save(draft, mapOf(1L to layerBitmap), emptyList(), writeImages = true))
        File(context.noBackupFilesDir, "design-draft/layer-1.png").delete()
        assertNull(store.load())
        store.clear()
    }

    @Test
    fun corruptedMetadataIsReportedAsNoDraft() {
        store.clear()
        val layerBitmap = bitmap(10, 10, Color.GREEN)
        val draft = DesignDraft(DesignScreen.EDITOR, document(), null)
        assertTrue(store.save(draft, mapOf(1L to layerBitmap), emptyList(), writeImages = true))
        File(context.noBackupFilesDir, "design-draft/draft.bin").writeBytes(byteArrayOf(9, 9, 9))
        assertNull(store.load())
        store.clear()
    }
}
