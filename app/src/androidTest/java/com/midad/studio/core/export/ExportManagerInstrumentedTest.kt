package com.midad.studio.core.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midad.studio.core.project.ProjectStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-storage check for the export path. The JVM tests cover sizing and failures; this test
 * proves a bitmap reaches MediaStore, is readable, and is indexed by the local export library.
 */
@RunWith(AndroidJUnit4::class)
class ExportManagerInstrumentedTest {

    @Test
    fun saveImageWritesReadableIndexedJpeg() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(180, 90, 40))
        }
        val name = "MidadInstrumented_" + System.currentTimeMillis()
        try {
            val outcome = ExportManager.saveImage(
                context = context,
                bitmap = bitmap,
                fileName = name,
                format = Bitmap.CompressFormat.JPEG,
                quality = 90,
            )
            assertTrue("export failed: " + outcome, outcome is ExportOutcome.Success)
            val result = (outcome as ExportOutcome.Success).result
            assertTrue("expected gallery row on API 29+", result.inGallery)
            assertNotNull("export URI must be readable", context.contentResolver.openInputStream(result.uri).use { it?.read() })

            ProjectStore.refresh(context)
            val record = ProjectStore.records.value.firstOrNull { it.fileName == result.fileName }
            assertNotNull("export must appear in the local library index", record)
            ProjectStore.remove(context, record!!.id)
        } finally {
            bitmap.recycle()
            // Best-effort cleanup; the test may run again on a dirty emulator.
            runCatching {
                context.contentResolver.delete(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    android.provider.MediaStore.MediaColumns.DISPLAY_NAME + "=?",
                    arrayOf(name + ".jpg"),
                )
            }
        }
    }
}
