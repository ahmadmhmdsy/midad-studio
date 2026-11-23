package com.midad.studio.core.draft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.midad.studio.studio.photo.engine.Adjustments
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PhotoDraftStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun draft() = PhotoDraft(
        sourceUri = "content://media/picker/0/photo",
        baseFromOriginal = true,
        sourcePng = false,
        adjustments = Adjustments(exposure = 0.5f),
        filterIndex = null,
        filterIntensity = 100,
        geometry = com.midad.studio.studio.photo.engine.GeometryState(),
        textLayers = emptyList(),
        stickers = emptyList(),
        strokes = emptyList(),
        nextLayerId = 1L,
    )

    @Test
    fun draftSurvivesAFullWriteAndReadCycle() {
        val store = PhotoDraftStore(context)
        store.clear()
        val source = Bitmap.createBitmap(96, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(200, 150, 70))
        }
        assertTrue(store.save(draft(), source, sourceChanged = true))
        assertTrue(store.hasDraft())

        val restored = store.load()
        assertNotNull(restored)
        assertEquals(96, restored!!.source.width)
        assertEquals(64, restored.source.height)
        assertEquals(draft().sourceUri, restored.draft.sourceUri)
        assertEquals(0.5f, restored.draft.adjustments.exposure, 0.0001f)
        store.clear()
        assertFalse(store.hasDraft())
        assertNull(store.load())
    }

    @Test
    fun metadataOnlyWriteKeepsTheExistingPixels() {
        val store = PhotoDraftStore(context)
        store.clear()
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        assertTrue(store.save(draft(), source, sourceChanged = true))
        val updated = draft().copy(filterIndex = 3, filterIntensity = 40)
        assertTrue(store.save(updated, source, sourceChanged = false))
        val restored = store.load()
        assertEquals(3, restored!!.draft.filterIndex)
        assertEquals(40, restored.draft.filterIntensity)
    }

    @Test
    fun corruptedMetadataIsReportedAsNoDraft() {
        val store = PhotoDraftStore(context)
        store.clear()
        val source = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        assertTrue(store.save(draft(), source, sourceChanged = true))
        val metadata = java.io.File(context.noBackupFilesDir, "photo-draft/draft.bin")
        metadata.writeBytes(byteArrayOf(1, 2, 3))
        assertNull(store.load())
    }
}
