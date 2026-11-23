package com.midad.studio.core.ai.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the .part -> installed move on real app storage, including size validation. */
@RunWith(AndroidJUnit4::class)
class ModelStorageInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val id = "instrumented_model_" + System.currentTimeMillis()
    private val entry = ModelManifestEntry(
        id = id,
        displayName = "Instrumented model",
        description = "",
        licenseSpdx = "Apache-2.0",
        licenseUrl = "",
        downloadUrl = "",
        version = "1",
        fileName = id + ".tflite",
        assetPath = "",
        sizeBytes = 4L,
        sha256 = "",
        distribution = ModelDistribution.DOWNLOAD,
        runtime = "tflite",
        attribution = "",
        modelCardUrl = "",
        inputSpec = null,
        outputSpec = null,
    )

    @After
    fun cleanup() {
        ModelStorage.deleteQuietly(ModelStorage.partFile(context, entry))
        ModelStorage.deleteQuietly(ModelStorage.modelFile(context, entry))
    }

    @Test
    fun commitPartMovesBytesAndReportsInstalled() {
        val part = ModelStorage.partFile(context, entry)
        part.writeBytes(byteArrayOf(0x49, 0x44, 0x46, 0x0A))
        assertTrue(part.isFile)
        assertFalse(ModelStorage.isInstalled(context, entry))

        assertTrue(ModelStorage.commitPart(context, entry))
        assertFalse("part must be gone after commit", part.isFile)
        assertTrue(ModelStorage.isInstalled(context, entry))
        assertEquals(4L, ModelStorage.modelFile(context, entry).length())
    }

    @Test
    fun emptyPartIsRejectedWithoutTouchingInstalledFile() {
        val target = ModelStorage.modelFile(context, entry)
        target.writeBytes(byteArrayOf(1, 2, 3, 4))
        ModelStorage.partFile(context, entry).writeBytes(ByteArray(0))

        assertFalse(ModelStorage.commitPart(context, entry))
        assertTrue(ModelStorage.isInstalled(context, entry))
        assertEquals(4L, target.length())
    }
}
