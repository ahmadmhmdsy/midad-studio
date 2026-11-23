package com.midad.studio.core.export

import com.midad.studio.core.project.ProjectStore
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportMimeTest {

    @Test
    fun imageMimeFollowsTheFileExtension() {
        assertEquals("image/png", ExportManager.mimeTypeFor("Midad_1.png", ProjectStore.KIND_PHOTO))
        assertEquals("image/webp", ExportManager.mimeTypeFor("Midad_1.webp", ProjectStore.KIND_PHOTO))
        assertEquals("image/jpeg", ExportManager.mimeTypeFor("Midad_1.jpg", ProjectStore.KIND_PHOTO))
        assertEquals("image/jpeg", ExportManager.mimeTypeFor("Midad_1", ProjectStore.KIND_PHOTO))
    }

    @Test
    fun videoMimeIsMp4() {
        assertEquals("video/mp4", ExportManager.mimeTypeFor("Midad_1.mp4", ProjectStore.KIND_VIDEO))
        assertEquals("video/mp4", ExportManager.mimeTypeFor("Midad_1", ProjectStore.KIND_VIDEO))
    }

    @Test
    fun extensionCheckIsCaseInsensitive() {
        assertEquals("image/png", ExportManager.mimeTypeFor("MIDAD.PNG", ProjectStore.KIND_PHOTO))
        assertEquals("video/mp4", ExportManager.mimeTypeFor("MIDAD.MP4", ProjectStore.KIND_VIDEO))
    }

    @Test
    fun generatedNamesCarryThePrefix() {
        val image = ExportManager.newImageName()
        assertEquals(true, image.startsWith("Midad_"))
        val video = ExportManager.newVideoName()
        assertEquals(true, video.startsWith("Midad_"))
    }
}
