package com.midad.studio.core.draft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

data class RestoredPhotoDraft(val draft: PhotoDraft, val source: Bitmap)

/**
 * Owns the on-disk photo draft: one metadata record and one working-pixel file.
 * Both live in noBackupFilesDir so they survive process death but are never backed
 * up, shared, or indexed by the gallery.
 */
class PhotoDraftStore(context: Context) {

    companion object {
        /** The folder this store reads and writes; the projects library copies it verbatim. */
        fun rootDirectory(context: Context): File = File(context.noBackupFilesDir, "photo-draft")
    }

    private val directory = rootDirectory(context)
    private val metadataFile = File(directory, "draft.bin")
    private val pngFile = File(directory, "source.png")
    private val jpgFile = File(directory, "source.jpg")

    fun hasDraft(): Boolean = metadataFile.isFile && (pngFile.isFile || jpgFile.isFile)

    fun save(draft: PhotoDraft, source: Bitmap, sourceChanged: Boolean): Boolean = runCatching {
        if (source.isRecycled) return false
        if (!directory.exists() && !directory.mkdirs()) return false
        val usePng = source.hasAlpha()
        val target = if (usePng) pngFile else jpgFile
        if (sourceChanged || !target.isFile) {
            writeBitmap(source, target, usePng)
            (if (usePng) jpgFile else pngFile).delete()
        }
        writeAtomic(metadataFile, PhotoDraftCodec.encode(draft.copy(sourcePng = usePng)))
        true
    }.getOrDefault(false)

    fun load(): RestoredPhotoDraft? = runCatching {
        val metadata = metadataFile.takeIf { it.isFile }?.readBytes() ?: return null
        val draft = PhotoDraftCodec.decode(metadata) ?: return null
        val file = if (draft.sourcePng) pngFile else jpgFile
        if (!file.isFile) return null
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            bitmap.recycle()
            return null
        }
        RestoredPhotoDraft(draft, bitmap)
    }.getOrNull()

    fun clear() {
        runCatching {
            metadataFile.delete()
            pngFile.delete()
            jpgFile.delete()
            directory.delete()
        }
    }

    private fun writeBitmap(source: Bitmap, target: File, png: Boolean) {
        val temp = File(directory, target.name + ".tmp")
        temp.outputStream().use { stream ->
            val format = if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val quality = if (png) 100 else 92
            if (!source.compress(format, quality, stream)) error("draft encode failed")
        }
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
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
