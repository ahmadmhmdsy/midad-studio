package com.midad.studio.core.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.StringRes
import com.midad.studio.R
import com.midad.studio.core.image.BitmapTools
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** The four studios a project can belong to. [id] is the value written to the index. */
enum class ProjectKind(
    val id: String,
    @StringRes val labelRes: Int,
    @StringRes val defaultTitleRes: Int,
) {
    PHOTO("photo", R.string.projects_label_photo, R.string.projects_default_title_photo),
    DESIGN("design", R.string.projects_label_design, R.string.projects_default_title_design),
    AI("ai", R.string.projects_label_ai, R.string.projects_default_title_ai),
    VIDEO("video", R.string.projects_label_video, R.string.projects_default_title_video);

    /** Photo, design and cut-out payloads are the studio's own draft folder; video is JSON. */
    val storesDraftFolder: Boolean get() = this != VIDEO

    companion object {
        fun fromId(id: String?): ProjectKind? = entries.firstOrNull { it.id == id }
    }
}

/** One saved project row. The payload lives in the directory the repository derives from [id]. */
data class ProjectRecord(
    val id: String,
    val kind: ProjectKind,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val hasThumbnail: Boolean,
)

/**
 * Owns the project library under [Context.getNoBackupFilesDir], so Auto Backup never uploads it:
 *
 * - projects/index.json  versioned list of projects, replaced atomically
 * - projects/ID/payload/  photo, design and cut-out draft folders, or project.json for video
 * - projects/ID/thumbnail.jpg  small preview used by the Projects grid
 *
 * Writes go to a sibling staging directory first and are swapped in with a rename, so a crash or a
 * full disk never leaves a half-written project. A failed save never removes the source draft.
 * Payload formats stay owned by the studios' draft stores; this repository copies bytes and checks
 * the metadata file, it never decodes an edit.
 */
object ProjectRepository {

    const val INDEX_VERSION: Int = 1
    const val PAYLOAD_DIR_NAME: String = "payload"
    const val PAYLOAD_FILE_NAME: String = "project.json"
    const val THUMBNAIL_FILE_NAME: String = "thumbnail.jpg"
    const val THUMBNAIL_MAX_PX: Int = 512
    private const val INDEX_FILE_NAME: String = "index.json"
    private const val DRAFT_METADATA_FILE: String = "draft.bin"
    private const val MAX_TITLE_CHARS: Int = 80
    private val SAFE_ID = Regex("[A-Za-z0-9-]{1,64}")

    private val _projects = MutableStateFlow<List<ProjectRecord>>(emptyList())

    /** Newest-updated first; collected by the Projects screen. */
    val projects: StateFlow<List<ProjectRecord>> = _projects.asStateFlow()

    @Volatile
    private var loaded = false

    private val writeLock = Mutex()

    fun root(context: Context): File = File(context.noBackupFilesDir, "projects")

    fun projectDir(context: Context, id: String): File = File(root(context), id)

    fun payloadDir(context: Context, id: String): File =
        File(projectDir(context, id), PAYLOAD_DIR_NAME)

    fun payloadFile(context: Context, id: String, fileName: String = PAYLOAD_FILE_NAME): File =
        File(payloadDir(context, id), fileName)

    fun thumbnailFile(context: Context, id: String): File =
        File(projectDir(context, id), THUMBNAIL_FILE_NAME)

    fun find(id: String?): ProjectRecord? {
        if (id == null) return null
        return _projects.value.firstOrNull { it.id == id }
    }

    fun titleOf(id: String?): String? = find(id)?.title

    suspend fun ensureLoaded(context: Context) {
        if (loaded) return
        refresh(context)
    }

    /** Re-reads the index; dead entries whose directory disappeared are dropped. */
    suspend fun refresh(context: Context) {
        val appContext = context.applicationContext
        // Publish inside the lock: a concurrent rename/delete would otherwise be overwritten by
        // this stale list when the lock is released.
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                val stored = readIndex(appContext)
                val alive = stored.filter { projectDir(appContext, it.id).isDirectory }
                if (alive.size != stored.size) {
                    writeIndex(appContext, alive)
                }
                _projects.value = alive
            }
        }
        loaded = true
    }

    /**
     * Saves a new project, or updates the one named by [projectId], from the studio draft folder
     * [sourceDraftDir]. The source folder is only read, never modified or deleted. Returns the
     * record on success and null when the draft was unreadable or the write failed.
     */
    suspend fun saveFromDraft(
        context: Context,
        projectId: String?,
        kind: ProjectKind,
        title: String,
        sourceDraftDir: File,
        thumbnail: Bitmap?,
    ): ProjectRecord? = writeProject(
        context = context,
        projectId = projectId,
        kind = kind,
        title = title,
        thumbnail = thumbnail,
        writePayload = { payloadDir ->
            if (!File(sourceDraftDir, DRAFT_METADATA_FILE).isFile) return@writeProject false
            sourceDraftDir.copyRecursively(payloadDir, overwrite = true)
            val metadata = File(payloadDir, DRAFT_METADATA_FILE)
            metadata.isFile && metadata.length() > 0L
        },
    )

    /** Saves a JSON payload (currently the video project) with the same atomic handling. */
    suspend fun savePayload(
        context: Context,
        projectId: String?,
        kind: ProjectKind,
        title: String,
        payloadFileName: String = PAYLOAD_FILE_NAME,
        payload: ByteArray,
        thumbnail: Bitmap?,
    ): ProjectRecord? = writeProject(
        context = context,
        projectId = projectId,
        kind = kind,
        title = title,
        thumbnail = thumbnail,
        writePayload = { payloadDir ->
            if (payload.isEmpty()) return@writeProject false
            if (!payloadDir.isDirectory && !payloadDir.mkdirs()) return@writeProject false
            val target = File(payloadDir, payloadFileName)
            target.writeBytes(payload)
            target.isFile && target.length() == payload.size.toLong()
        },
    )

    /**
     * Copies a draft-folder project into [targetDraftDir], which is the studio's own draft folder.
     * The payload is validated and fully copied to a staging folder first; the current draft folder
     * is replaced only when the copy succeeded. The project payload itself is never modified.
     */
    suspend fun restoreIntoDraftDir(
        context: Context,
        id: String,
        targetDraftDir: File,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!SAFE_ID.matches(id)) return@withContext false
        val source = payloadDir(context, id)
        if (!File(source, DRAFT_METADATA_FILE).isFile) return@withContext false
        val parent = targetDraftDir.absoluteFile.parentFile ?: return@withContext false
        if (!parent.isDirectory && !parent.mkdirs()) return@withContext false
        val staging = File(parent, targetDraftDir.name + ".restore-" + UUID.randomUUID())
        val backup = File(parent, targetDraftDir.name + ".old-" + UUID.randomUUID())
        try {
            if (!staging.mkdirs()) return@withContext false
            source.copyRecursively(staging, overwrite = true)
            if (!File(staging, DRAFT_METADATA_FILE).isFile) {
                staging.deleteRecursively()
                return@withContext false
            }
            backup.deleteRecursively()
            if (targetDraftDir.exists() && !targetDraftDir.renameTo(backup)) {
                staging.deleteRecursively()
                return@withContext false
            }
            if (!staging.renameTo(targetDraftDir)) {
                if (backup.exists()) backup.renameTo(targetDraftDir)
                staging.deleteRecursively()
                return@withContext false
            }
            backup.deleteRecursively()
            true
        } catch (throwable: Throwable) {
            runCatching { staging.deleteRecursively() }
            if (!targetDraftDir.exists() && backup.exists()) {
                runCatching { backup.renameTo(targetDraftDir) }
            }
            false
        }
    }

    /** True when the project has the metadata file its studio needs to reopen it. */
    suspend fun hasValidPayload(context: Context, record: ProjectRecord): Boolean =
        withContext(Dispatchers.IO) {
            if (!SAFE_ID.matches(record.id)) return@withContext false
            if (record.kind.storesDraftFolder) {
                val metadata = File(payloadDir(context, record.id), DRAFT_METADATA_FILE)
                metadata.isFile && metadata.length() > 0L
            } else {
                val payload = payloadFile(context, record.id)
                payload.isFile && payload.length() > 0L
            }
        }

    /** The raw JSON payload of a non-draft project, or null when it is missing or unreadable. */
    suspend fun loadPayload(
        context: Context,
        id: String,
        payloadFileName: String = PAYLOAD_FILE_NAME,
    ): String? = withContext(Dispatchers.IO) {
        if (!SAFE_ID.matches(id)) return@withContext null
        runCatching {
            val file = payloadFile(context, id, payloadFileName)
            if (!file.isFile || file.length() <= 0L) null else file.readText()
        }.getOrNull()
    }

    suspend fun rename(context: Context, id: String, title: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!SAFE_ID.matches(id)) return@withContext false
            writeLock.withLock {
                val current = readIndex(context)
                val index = current.indexOfFirst { it.id == id }
                if (index < 0) return@withLock false
                val renamed = current[index].copy(
                    title = normalizeTitle(context, title, current[index].kind),
                    updatedAt = System.currentTimeMillis(),
                )
                val updated = listOf(renamed) + current.filterNot { it.id == id }
                if (!writeIndex(context, updated)) return@withLock false
                _projects.value = updated
                true
            }
        }

    /**
     * Removes the index row first, then the project directory. A directory left behind by a failed
     * index write is an orphan; it is harmless and never shown, whereas the old order could leave
     * the index pointing at a directory that was already gone.
     */
    suspend fun delete(context: Context, id: String): Boolean = withContext(Dispatchers.IO) {
        if (!SAFE_ID.matches(id)) return@withContext false
        writeLock.withLock {
            val current = readIndex(context)
            if (current.none { it.id == id }) return@withLock false
            val updated = current.filterNot { it.id == id }
            if (!writeIndex(context, updated)) return@withLock false
            _projects.value = updated
            projectDir(context, id).deleteRecursively()
            true
        }
    }

    suspend fun thumbnail(context: Context, id: String): Bitmap? = withContext(Dispatchers.IO) {
        if (!SAFE_ID.matches(id)) return@withContext null
        val file = thumbnailFile(context, id)
        if (!file.isFile) return@withContext null
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val options = BitmapFactory.Options().apply {
                inSampleSize = BitmapTools.sampleSize(
                    bounds.outWidth,
                    bounds.outHeight,
                    THUMBNAIL_MAX_PX,
                )
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        }.getOrNull()
    }

    private suspend fun writeProject(
        context: Context,
        projectId: String?,
        kind: ProjectKind,
        title: String,
        thumbnail: Bitmap?,
        writePayload: (File) -> Boolean,
    ): ProjectRecord? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        if (projectId != null && !SAFE_ID.matches(projectId)) return@withContext null
        writeLock.withLock {
            val current = readIndex(appContext)
            val existing = projectId?.let { id -> current.firstOrNull { it.id == id } }
            if (projectId != null && existing == null) return@withLock null
            val id = existing?.id ?: UUID.randomUUID().toString()
            val root = root(appContext)
            if (!root.isDirectory && !root.mkdirs()) return@withLock null
            val now = System.currentTimeMillis()
            val staging = File(root, ".staging-" + UUID.randomUUID())
            val target = projectDir(appContext, id)
            val backup = File(root, ".old-" + UUID.randomUUID())
            try {
                if (!staging.mkdirs()) return@withLock null
                if (!writePayload(File(staging, PAYLOAD_DIR_NAME))) {
                    staging.deleteRecursively()
                    return@withLock null
                }
                val wroteThumbnail = writeThumbnail(thumbnail, File(staging, THUMBNAIL_FILE_NAME))
                if (!wroteThumbnail && existing?.hasThumbnail == true) {
                    val previous = thumbnailFile(appContext, id)
                    if (previous.isFile) {
                        previous.copyTo(File(staging, THUMBNAIL_FILE_NAME), overwrite = true)
                    }
                }
                backup.deleteRecursively()
                if (target.exists() && !target.renameTo(backup)) {
                    staging.deleteRecursively()
                    return@withLock null
                }
                if (!staging.renameTo(target)) {
                    if (backup.exists()) backup.renameTo(target)
                    staging.deleteRecursively()
                    return@withLock null
                }
                backup.deleteRecursively()
                val record = ProjectRecord(
                    id = id,
                    kind = kind,
                    title = normalizeTitle(context, title, kind),
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    hasThumbnail = File(target, THUMBNAIL_FILE_NAME).isFile,
                )
                val updated = listOf(record) + current.filterNot { it.id == id }
                if (!writeIndex(appContext, updated)) return@withLock null
                _projects.value = updated
                loaded = true
                record
            } catch (throwable: Throwable) {
                runCatching { staging.deleteRecursively() }
                if (!target.exists() && backup.exists()) {
                    runCatching { backup.renameTo(target) }
                }
                null
            }
        }
    }

    private fun writeThumbnail(bitmap: Bitmap?, target: File): Boolean {
        if (bitmap == null || bitmap.isRecycled) return false
        return runCatching {
            val scaled = BitmapTools.scaleToMax(bitmap, THUMBNAIL_MAX_PX)
            val matte = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
            Canvas(matte).apply {
                drawColor(Color.WHITE)
                drawBitmap(scaled, 0f, 0f, null)
            }
            val written = runCatching {
                target.outputStream().use { output ->
                    matte.compress(Bitmap.CompressFormat.JPEG, 88, output)
                }
            }.getOrDefault(false)
            if (matte !== scaled) BitmapTools.safeRecycle(matte)
            if (scaled !== bitmap) BitmapTools.safeRecycle(scaled)
            written && target.isFile && target.length() > 0L
        }.getOrDefault(false)
    }

    /** Falls back to the studio's localized default name, never to a hardcoded English word. */
    private fun normalizeTitle(context: Context, title: String, kind: ProjectKind): String =
        title.trim().take(MAX_TITLE_CHARS).ifBlank { context.getString(kind.defaultTitleRes) }

    private fun readIndex(context: Context): List<ProjectRecord> {
        val file = File(root(context), INDEX_FILE_NAME)
        if (!file.isFile || file.length() <= 0L) return emptyList()
        return runCatching {
            val root = JSONObject(file.readText())
            if (root.optInt("version", 0) != INDEX_VERSION) return emptyList()
            val array = root.optJSONArray("projects") ?: JSONArray()
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").orEmpty()
                    val kind = ProjectKind.fromId(item.optString("kind")) ?: continue
                    if (!SAFE_ID.matches(id)) continue
                    val title = item.optString("title").orEmpty()
                    if (title.isBlank()) continue
                    val createdAt = item.optLong("createdAt", 0L).coerceAtLeast(0L)
                    val updatedAt = item.optLong("updatedAt", createdAt).coerceAtLeast(createdAt)
                    add(
                        ProjectRecord(
                            id = id,
                            kind = kind,
                            title = title,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            hasThumbnail = thumbnailFile(context, id).isFile,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeIndex(context: Context, records: List<ProjectRecord>): Boolean {
        val root = root(context)
        if (!root.isDirectory && !root.mkdirs()) return false
        val target = File(root, INDEX_FILE_NAME)
        val temp = File(root, INDEX_FILE_NAME + ".tmp-" + UUID.randomUUID())
        return runCatching {
            val projects = JSONArray()
            for (record in records) {
                projects.put(
                    JSONObject().apply {
                        put("id", record.id)
                        put("kind", record.kind.id)
                        put("title", record.title)
                        put("createdAt", record.createdAt)
                        put("updatedAt", record.updatedAt)
                        put("hasThumbnail", record.hasThumbnail)
                    },
                )
            }
            val rootJson = JSONObject().apply {
                put("version", INDEX_VERSION)
                put("projects", projects)
            }
            temp.writeText(rootJson.toString())
            replaceFile(temp, target)
            true
        }.getOrDefault(false).also { temp.delete() }
    }

    /** Atomic on Android (POSIX rename replaces); Windows test hosts fall back to a plain move. */
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
