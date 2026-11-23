package com.midad.studio.core.draft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.midad.studio.studio.ai.AiBackdrop
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File

data class AiDraft(val hasCutout: Boolean, val backdrop: AiBackdrop)

data class RestoredAiDraft(
    val source: Bitmap,
    val cutout: Bitmap?,
    val backdrop: AiBackdrop,
)

/**
 * Background-removal drafts: the source and the cutout are copied into app-private
 * storage, so a process kill does not lose an inference the user waited for.
 */
class AiDraftStore(context: Context) {

    companion object {
        /** The folder this store reads and writes; the projects library copies it verbatim. */
        fun rootDirectory(context: Context): File = File(context.noBackupFilesDir, "ai-draft")
    }

    private val directory = rootDirectory(context)
    private val metadataFile = File(directory, "draft.bin")
    private val sourceFile = File(directory, "source.png")
    private val cutoutFile = File(directory, "cutout.png")

    private var savedSource: Bitmap? = null
    private var savedCutout: Bitmap? = null

    fun hasDraft(): Boolean = metadataFile.isFile && sourceFile.isFile

    fun save(
        draft: AiDraft,
        source: Bitmap,
        cutout: Bitmap?,
        shouldCommit: () -> Boolean = { true },
    ): Boolean = runCatching {
        if (!directory.exists() && !directory.mkdirs()) return false
        if (source !== savedSource || !sourceFile.isFile) writeBitmap(source, sourceFile)
        if (cutout != null) {
            if (cutout !== savedCutout || !cutoutFile.isFile) writeBitmap(cutout, cutoutFile)
        } else {
            cutoutFile.delete()
        }
        if (!shouldCommit()) return false
        writeAtomic(metadataFile, encode(draft))
        savedSource = source
        savedCutout = cutout
        true
    }.getOrDefault(false)

    fun load(): RestoredAiDraft? = runCatching {
        val draft = decode(metadataFile.takeIf { it.isFile }?.readBytes() ?: return null) ?: return null
        val source = decodeBitmap(sourceFile) ?: return null
        val cutout = if (draft.hasCutout) {
            decodeBitmap(cutoutFile) ?: run {
                source.recycle()
                return null
            }
        } else {
            null
        }
        savedSource = source
        savedCutout = cutout
        RestoredAiDraft(source, cutout, draft.backdrop)
    }.getOrNull()

    fun clear() {
        runCatching {
            metadataFile.delete()
            sourceFile.delete()
            cutoutFile.delete()
            directory.delete()
        }
        savedSource = null
        savedCutout = null
    }

    private fun encode(draft: AiDraft): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(0x4D414431) // "MAD1"
            out.writeInt(1)
            out.writeBoolean(draft.hasCutout)
            out.writeInt(draft.backdrop.ordinal)
        }
        return bytes.toByteArray()
    }

    private fun decode(bytes: ByteArray): AiDraft? = try {
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != 0x4D414431) return null
            if (input.readInt() != 1) return null
            val hasCutout = input.readBoolean()
            val backdrop = AiBackdrop.entries[
                input.readInt().coerceIn(0, AiBackdrop.entries.size - 1)
            ]
            AiDraft(hasCutout, backdrop)
        }
    } catch (eof: EOFException) {
        null
    } catch (throwable: Throwable) {
        null
    }

    private fun decodeBitmap(file: File): Bitmap? {
        if (!file.isFile) return null
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun writeBitmap(bitmap: Bitmap, file: File) {
        if (bitmap.isRecycled) error("draft bitmap was recycled")
        val temp = File(directory, file.name + ".tmp")
        temp.outputStream().use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) error("draft encode failed")
        }
        if (file.exists()) file.delete()
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    private fun writeAtomic(target: File, bytes: ByteArray) {
        val temp = File(directory, target.name + ".tmp")
        temp.writeBytes(bytes)
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }
}
