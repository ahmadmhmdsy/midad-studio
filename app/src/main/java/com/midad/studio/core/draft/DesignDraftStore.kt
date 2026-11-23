package com.midad.studio.core.draft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.midad.studio.studio.design.model.LayerContent
import java.io.File

data class RestoredDesignDraft(
    val draft: DesignDraft,
    val documentBitmaps: Map<Long, Bitmap>,
    val collageBitmaps: List<Bitmap>,
)

/**
 * Design drafts keep their pixels next to the metadata: one PNG per image layer and
 * one per collage tile. Pixels are written only when the set of source bitmaps changes,
 * so moving text does not rewrite megabytes.
 */
class DesignDraftStore(context: Context) {

    companion object {
        /** The folder this store reads and writes; the projects library copies it verbatim. */
        fun rootDirectory(context: Context): File = File(context.noBackupFilesDir, "design-draft")
    }

    private val directory = rootDirectory(context)
    private val metadataFile = File(directory, "draft.bin")

    fun hasDraft(): Boolean = metadataFile.isFile

    fun save(
        draft: DesignDraft,
        documentBitmaps: Map<Long, Bitmap>,
        collageBitmaps: List<Bitmap>,
        writeImages: Boolean,
        shouldCommit: () -> Boolean = { true },
    ): Boolean = runCatching {
        if (!directory.exists() && !directory.mkdirs()) return false
        val document = draft.document
        val needed = HashSet<String>()
        if (document != null) {
            document.layers.forEach { layer ->
                if (layer.content is LayerContent.Image) {
                    val bitmap = documentBitmaps[layer.id] ?: return false
                    val file = layerFile(layer.id)
                    needed += file.name
                    if (writeImages || !file.isFile) writeBitmap(bitmap, file)
                }
            }
        }
        draft.collage?.let { collage ->
            collageBitmaps.forEachIndexed { index, bitmap ->
                val file = collageFile(index)
                needed += file.name
                if (writeImages || !file.isFile) writeBitmap(bitmap, file)
            }
        }
        directory.listFiles()?.forEach { file ->
            if ((file.name.startsWith("layer-") || file.name.startsWith("collage-")) && file.name !in needed) {
                file.delete()
            }
        }
        // A cancelled/abandoned save must not resurrect a draft after an explicit exit.
        if (!shouldCommit()) return false
        writeAtomic(metadataFile, DesignDraftCodec.encode(draft))
        true
    }.getOrDefault(false)

    fun load(): RestoredDesignDraft? = runCatching {
        val metadata = metadataFile.takeIf { it.isFile }?.readBytes() ?: return null
        val draft = DesignDraftCodec.decode(metadata) ?: return null
        val documentBitmaps = LinkedHashMap<Long, Bitmap>()
        val document = draft.document
        if (document != null) {
            for (layer in document.layers) {
                if (layer.content !is LayerContent.Image) continue
                val bitmap = decode(layerFile(layer.id)) ?: run {
                    documentBitmaps.values.forEach { it.recycle() }
                    return null
                }
                documentBitmaps[layer.id] = bitmap
            }
        }
        val collageBitmaps = ArrayList<Bitmap>()
        draft.collage?.images?.indices?.forEach { index ->
            val bitmap = decode(collageFile(index)) ?: run {
                documentBitmaps.values.forEach { it.recycle() }
                collageBitmaps.forEach { it.recycle() }
                return null
            }
            collageBitmaps += bitmap
        }
        RestoredDesignDraft(draft, documentBitmaps, collageBitmaps)
    }.getOrNull()

    fun clear() {
        runCatching {
            metadataFile.delete()
            directory.listFiles()?.forEach { it.delete() }
            directory.delete()
        }
    }

    private fun layerFile(id: Long) = File(directory, "layer-$id.png")

    private fun collageFile(index: Int) = File(directory, "collage-$index.png")

    private fun decode(file: File): Bitmap? {
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
