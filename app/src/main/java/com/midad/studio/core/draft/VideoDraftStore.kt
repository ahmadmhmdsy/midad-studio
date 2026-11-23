package com.midad.studio.core.draft

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Owns the on-disk video draft: one versioned JSON payload in noBackupFilesDir, so an interrupted
 * edit survives process death without touching the gallery or Auto Backup. Clip pixels are not
 * copied: the payload stores the picker URIs and is validated against the provider when it loads.
 */
class VideoDraftStore(context: Context) {

    companion object {
        /** The folder this store reads and writes. */
        fun rootDirectory(context: Context): File = File(context.noBackupFilesDir, "video-draft")
    }

    private val directory = rootDirectory(context)
    private val draftFile = File(directory, "draft.json")

    fun hasDraft(): Boolean = draftFile.isFile && draftFile.length() > 0L

    fun save(payload: VideoProjectPayload): Boolean {
        if (payload.clips.isEmpty()) return false
        if (!directory.isDirectory && !directory.mkdirs()) return false
        val temp = File(directory, "draft.json.tmp-" + UUID.randomUUID())
        // A failed save keeps the previous draft: writing goes to a temp file and is only swapped
        // in on success, so a full disk never destroys the last good state.
        return try {
            temp.writeText(VideoProjectCodec.encode(payload))
            replaceFile(temp, draftFile)
            draftFile.isFile && draftFile.length() > 0L
        } catch (throwable: Throwable) {
            false
        } finally {
            temp.delete()
        }
    }

    fun load(): VideoProjectPayload? = runCatching {
        if (!draftFile.isFile || draftFile.length() <= 0L) null
        else VideoProjectCodec.decode(draftFile.readText())
    }.getOrNull()

    fun clear() {
        runCatching {
            draftFile.delete()
            directory.listFiles()?.forEach { file ->
                if (file.name.startsWith("draft.json.tmp-")) file.delete()
            }
        }
    }

    private fun replaceFile(source: File, target: File) {
        runCatching {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.onFailure {
            runCatching {
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }.onFailure {
                target.delete()
                source.renameTo(target)
            }
        }
    }
}
