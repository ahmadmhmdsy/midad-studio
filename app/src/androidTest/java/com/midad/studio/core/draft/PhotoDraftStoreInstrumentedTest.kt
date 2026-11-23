package com.midad.studio.core.draft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midad.studio.studio.photo.engine.Adjustments
import com.midad.studio.studio.photo.engine.GeometryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Process-death recovery must round-trip both the edit record and its working pixels. */
@RunWith(AndroidJUnit4::class)
class PhotoDraftStoreInstrumentedTest {

    @Test
    fun draftSurvivesStoreRecreationAndClears() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = PhotoDraftStore(context)
        store.clear()
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(20, 90, 160))
        }
        val draft = PhotoDraft(
            sourceUri = null,
            baseFromOriginal = false,
            sourcePng = false,
            adjustments = Adjustments(brightness = 14),
            filterIndex = null,
            filterIntensity = 100,
            geometry = GeometryState(quarterTurns = 1),
            textLayers = emptyList(),
            stickers = emptyList(),
            strokes = emptyList(),
            nextLayerId = 7L,
        )
        try {
            assertTrue(store.save(draft, bitmap, sourceChanged = true))
            assertTrue(store.hasDraft())

            // A new store instance represents a fresh app process reading the same files.
            val restored = PhotoDraftStore(context).load()
            assertNotNull(restored)
            assertEquals(14, restored!!.draft.adjustments.brightness)
            assertEquals(1, restored.draft.geometry.quarterTurns)
            assertEquals(7L, restored.draft.nextLayerId)
            assertEquals(32, restored.source.width)
            restored.source.recycle()
        } finally {
            bitmap.recycle()
            store.clear()
        }
        assertFalse(store.hasDraft())
    }
}
