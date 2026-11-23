package com.midad.studio.core.project

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.midad.studio.R
import java.io.File

/**
 * The projects library must survive a crash mid-write, never leak half a project and never remove
 * the studio draft it copied from. These tests drive the real filesystem in the Robolectric sandbox.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ProjectRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() = runBlocking {
        ProjectRepository.root(context).deleteRecursively()
        ProjectRepository.refresh(context)
    }

    @After
    fun tearDown() {
        ProjectRepository.root(context).deleteRecursively()
    }

    private fun bitmap(): Bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).also {
        it.eraseColor(Color.RED)
    }

    private fun draftFolder(name: String): File {
        val folder = File(context.cacheDir, name)
        folder.deleteRecursively()
        assertTrue(folder.mkdirs())
        File(folder, "draft.bin").writeBytes(byteArrayOf(0x4D, 0x44, 0x50, 0x31, 1))
        File(folder, "pixels.bin").writeBytes(ByteArray(16))
        return folder
    }

    @Test
    fun saveFromDraftPublishesPayloadAndThumbnail() = runBlocking {
        val source = draftFolder("photo-draft-save")
        val record = ProjectRepository.saveFromDraft(
            context = context,
            projectId = null,
            kind = ProjectKind.PHOTO,
            title = "  Beach day  ",
            sourceDraftDir = source,
            thumbnail = bitmap(),
        )
        assertNotNull(record)
        val saved = ProjectRepository.find(record!!.id)
        assertNotNull(saved)
        assertEquals("Beach day", saved!!.title)
        assertEquals(ProjectKind.PHOTO, saved.kind)
        assertTrue(saved.hasThumbnail)
        assertTrue(ProjectRepository.thumbnailFile(context, saved.id).isFile)
        assertTrue(File(ProjectRepository.payloadDir(context, saved.id), "draft.bin").isFile)
        // The source draft is only read, never moved or deleted.
        assertTrue(File(source, "draft.bin").isFile)
    }

    @Test
    fun blankTitleFallsBackToTheLocalizedStudioDefault() = runBlocking {
        val record = ProjectRepository.saveFromDraft(
            context = context,
            projectId = null,
            kind = ProjectKind.DESIGN,
            title = "   ",
            sourceDraftDir = draftFolder("design-draft-blank"),
            thumbnail = null,
        )
        assertNotNull(record)
        assertEquals(context.getString(R.string.projects_default_title_design), record!!.title)
    }

    @Test
    fun unreadableDraftFailsWithoutCreatingAProject() = runBlocking {
        val empty = File(context.cacheDir, "photo-draft-empty").apply {
            deleteRecursively()
            mkdirs()
        }
        val record = ProjectRepository.saveFromDraft(
            context = context,
            projectId = null,
            kind = ProjectKind.PHOTO,
            title = "Broken",
            sourceDraftDir = empty,
            thumbnail = null,
        )
        assertNull(record)
        assertTrue(ProjectRepository.projects.value.isEmpty())
        assertFalse(projectDirExists())
    }

    @Test
    fun updateKeepsIdAndReplacesTitle() = runBlocking {
        val first = ProjectRepository.saveFromDraft(
            context = context,
            projectId = null,
            kind = ProjectKind.AI,
            title = "First",
            sourceDraftDir = draftFolder("ai-draft-first"),
            thumbnail = null,
        )!!
        val updated = ProjectRepository.saveFromDraft(
            context = context,
            projectId = first.id,
            kind = ProjectKind.AI,
            title = "Second",
            sourceDraftDir = draftFolder("ai-draft-second"),
            thumbnail = null,
        )!!
        assertEquals(first.id, updated.id)
        assertEquals("Second", updated.title)
        assertEquals(1, ProjectRepository.projects.value.size)
        assertEquals("Second", ProjectRepository.titleOf(first.id))
    }

    @Test
    fun renameAndDeleteUpdateTheIndexAndRemoveTheFolder() = runBlocking {
        val record = ProjectRepository.saveFromDraft(
            context = context,
            projectId = null,
            kind = ProjectKind.PHOTO,
            title = "Old",
            sourceDraftDir = draftFolder("photo-draft-rename"),
            thumbnail = bitmap(),
        )!!
        assertTrue(ProjectRepository.rename(context, record.id, "New name"))
        assertEquals("New name", ProjectRepository.titleOf(record.id))
        val dir = ProjectRepository.projectDir(context, record.id)
        assertTrue(dir.isDirectory)
        assertTrue(ProjectRepository.delete(context, record.id))
        assertFalse(dir.exists())
        assertNull(ProjectRepository.find(record.id))
        assertTrue(ProjectRepository.projects.value.isEmpty())
    }

    @Test
    fun restoreIntoDraftDirValidatesAndCopiesThePayload() = runBlocking {
        val record = ProjectRepository.saveFromDraft(
            context = context,
            projectId = null,
            kind = ProjectKind.DESIGN,
            title = "Restore me",
            sourceDraftDir = draftFolder("design-draft-restore"),
            thumbnail = null,
        )!!
        val target = File(context.noBackupFilesDir, "design-draft")
        target.deleteRecursively()
        assertTrue(ProjectRepository.hasValidPayload(context, record))
        assertTrue(ProjectRepository.restoreIntoDraftDir(context, record.id, target))
        assertTrue(File(target, "draft.bin").isFile)
    }

    @Test
    fun videoPayloadRoundTripsAndValidates() = runBlocking {
        val json = "{\"version\":1,\"clips\":[]}".toByteArray()
        val record = ProjectRepository.savePayload(
            context = context,
            projectId = null,
            kind = ProjectKind.VIDEO,
            title = "Video one",
            payload = json,
            thumbnail = bitmap(),
        )!!
        assertEquals(ProjectKind.VIDEO, record.kind)
        assertTrue(ProjectRepository.hasValidPayload(context, record))
        assertEquals(String(json), ProjectRepository.loadPayload(context, record.id))
    }

    @Test
    fun refreshDropsEntriesWhoseFolderDisappeared() = runBlocking {
        val record = ProjectRepository.saveFromDraft(
            context = context,
            projectId = null,
            kind = ProjectKind.PHOTO,
            title = "Vanishing",
            sourceDraftDir = draftFolder("photo-draft-vanish"),
            thumbnail = null,
        )!!
        ProjectRepository.projectDir(context, record.id).deleteRecursively()
        ProjectRepository.refresh(context)
        assertTrue(ProjectRepository.projects.value.none { it.id == record.id })
    }

    private fun projectDirExists(): Boolean {
        val root = ProjectRepository.root(context)
        return root.isDirectory && root.listFiles().orEmpty().any { file -> !file.name.startsWith(".") }
    }
}
