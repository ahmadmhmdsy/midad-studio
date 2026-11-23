package com.midad.studio.core.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.midad.studio.core.image.BitmapTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ExportRecord(
    val id: String,
    val kind: String,
    val uri: String,
    val fileName: String,
    val createdAt: Long,
    val thumbnailPath: String?,
)

/** Local, offline index of everything the app has exported. No database, no cloud. */
object ProjectStore {

    const val KIND_PHOTO = "photo"
    const val KIND_VIDEO = "video"

    private val _records = MutableStateFlow<List<ExportRecord>>(emptyList())
    val records: StateFlow<List<ExportRecord>> = _records.asStateFlow()

    @Volatile
    private var loaded = false

    /**
     * Exported-media index and thumbnails live outside Auto Backup: they are derived
     * from the user's gallery and must never be uploaded by Android Backup.
     */
    fun libraryDir(context: Context): File = File(context.noBackupFilesDir, "library").apply { mkdirs() }

    fun thumbnailsDir(context: Context): File =
        File(libraryDir(context), "thumbs").apply { mkdirs() }

    private fun indexFile(context: Context) = File(libraryDir(context), "exports.json")

    suspend fun ensureLoaded(context: Context) {
        if (loaded) return
        withContext(Dispatchers.IO) {
            _records.value = readIndex(context)
            loaded = true
        }
    }

    suspend fun refresh(context: Context) = withContext(Dispatchers.IO) {
        _records.value = readIndex(context)
        loaded = true
    }

    suspend fun recordExport(
        context: Context,
        uri: Uri,
        kind: String,
        fileName: String,
        thumbnailSource: Bitmap?,
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val id = UUID.randomUUID().toString()
                var thumbPath: String? = null
                if (thumbnailSource != null) {
                    val thumb = BitmapTools.toArgb8888(BitmapTools.scaleToMax(thumbnailSource, 512))
                    val output = File(thumbnailsDir(context), id + ".jpg")
                    output.outputStream().use { stream ->
                        thumb.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    }
                    if (thumb !== thumbnailSource) BitmapTools.safeRecycle(thumb)
                    thumbPath = output.absolutePath
                }
                val record = ExportRecord(
                    id = id,
                    kind = kind,
                    uri = uri.toString(),
                    fileName = fileName,
                    createdAt = System.currentTimeMillis(),
                    thumbnailPath = thumbPath,
                )
                val updated = listOf(record) + readIndex(context).filterNot { it.id == id }
                writeIndex(context, updated)
                _records.value = updated
                loaded = true
            }
        }
    }

    suspend fun remove(context: Context, id: String): Boolean = withContext(Dispatchers.IO) {
        val current = readIndex(context)
        val target = current.firstOrNull { it.id == id } ?: return@withContext false
        target.thumbnailPath?.let { path -> runCatching { File(path).delete() } }
        // Removing a library entry must never delete the user's exported file: this
        // index is only Midad's memory of the export. Users delete their own gallery
        // files in the Gallery app, so destructive deletion has no place here.
        val updated = current.filterNot { it.id == id }
        writeIndex(context, updated)
        _records.value = updated
        true
    }

    suspend fun thumbnail(record: ExportRecord): Bitmap? = withContext(Dispatchers.IO) {
        record.thumbnailPath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        }
    }

    suspend fun cacheSizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        listOf(context.cacheDir, File(libraryDir(context), "tmp")).sumOf { dir ->
            runCatching {
                dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }.getOrDefault(0L)
        }
    }

    suspend fun clearCache(context: Context): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        runCatching {
            context.cacheDir.listFiles()?.forEach { file ->
                freed += file.length()
                file.deleteRecursively()
            }
        }
        freed
    }

    private fun readIndex(context: Context): List<ExportRecord> = runCatching {
        val file = indexFile(context)
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    ExportRecord(
                        id = item.getString("id"),
                        kind = item.getString("kind"),
                        uri = item.getString("uri"),
                        fileName = item.getString("fileName"),
                        createdAt = item.getLong("createdAt"),
                        thumbnailPath = if (item.isNull("thumbnailPath")) null else item.getString("thumbnailPath"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun writeIndex(context: Context, records: List<ExportRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            val item = JSONObject()
            item.put("id", record.id)
            item.put("kind", record.kind)
            item.put("uri", record.uri)
            item.put("fileName", record.fileName)
            item.put("createdAt", record.createdAt)
            item.put("thumbnailPath", record.thumbnailPath ?: JSONObject.NULL)
            array.put(item)
        }
        runCatching { indexFile(context).writeText(array.toString()) }
    }
}
