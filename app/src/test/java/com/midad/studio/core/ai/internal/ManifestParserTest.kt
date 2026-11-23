package com.midad.studio.core.ai.internal

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ManifestParserTest {

    @Test
    fun shippedCatalogueContainsTheAddedModels() {
        val result = ManifestParser.readFromAssets(ApplicationProvider.getApplicationContext())
        assertTrue(result is ManifestResult.Ok)
        val models = (result as ManifestResult.Ok).models
        assertEquals(6, models.size)

        val deeplab = models.first { it.id == "mediapipe-deeplab-v3-257-f32" }
        assertTrue(deeplab.inputSpec!!.layoutNhwc)
        assertEquals(257, deeplab.inputSpec!!.width)
        assertEquals(21, deeplab.outputSpec!!.channels)
        assertEquals(OutputActivation.SOFTMAX, deeplab.outputSpec!!.activation)

        val modnet = models.first { it.id == "modnet-portrait-matting-512-f32" }
        assertFalse(modnet.inputSpec!!.layoutNhwc)
        assertEquals(3, modnet.inputSpec!!.channels)
        assertEquals(512, modnet.inputSpec!!.width)
        assertFalse(modnet.outputSpec!!.layoutNhwc)
        assertEquals(1, modnet.outputSpec!!.channels)
        assertEquals(OutputActivation.NONE, modnet.outputSpec!!.activation)
    }

    @Test
    fun parsesOutputChannelsFromTheLayout() {
        val raw = """
            {"models":[{
              "id":"nchw","fileName":"nchw.tflite",
              "inputSpec":{"shape":[1,3,512,512],"dtype":"float32","layout":"NCHW","normalization":"(pixel / 255.0 - 0.5) / 0.5"},
              "outputSpec":{"shape":[1,1,512,512],"dtype":"float32","layout":"NCHW","activation":"NONE"}
            }]}
        """.trimIndent()
        val model = ManifestParser.parse(raw).single()
        assertEquals(512, model.inputSpec!!.height)
        assertEquals(512, model.inputSpec!!.width)
        assertEquals(3, model.inputSpec!!.channels)
        assertEquals(1, model.outputSpec!!.channels)
        assertFalse(model.outputSpec!!.layoutNhwc)
    }
}
