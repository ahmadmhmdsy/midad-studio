package com.midad.studio.core.ai.internal

import android.content.Context
import android.os.StatFs
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** A verified model file that lives in the app's private storage. */
data class InstalledModel(
    val entry: ModelManifestEntry,
    val file: File,
)

/**
 * Owns the private model directory (filesDir/models): paths, checksums and the small marker
 * that remembers a user removed a bundled model on purpose.
 */
object ModelStorage {
    const val MODELS_DIRECTORY = "models"
    private const val REMOVED_PREFIX = ".removed-"
    private const val BUFFER_SIZE = 64 * 1024
    private const val DOWNLOAD_HEADROOM_BYTES = 32L * 1024L * 1024L

    fun modelsDirectory(context: Context): File {
        val directory = File(context.filesDir, MODELS_DIRECTORY)
        if (!directory.exists()) directory.mkdirs()
        return directory
    }

    fun modelFile(context: Context, entry: ModelManifestEntry): File =
        File(modelsDirectory(context), entry.fileName)

    fun partFile(context: Context, entry: ModelManifestEntry): File =
        File(modelsDirectory(context), entry.fileName + ".part")

    fun assetExists(context: Context, entry: ModelManifestEntry): Boolean =
        runCatching { context.assets.open(entry.assetPath).close() }.isSuccess

    /** True when the installed file exists and matches the catalogue size (when one is given). */
    fun isInstalled(context: Context, entry: ModelManifestEntry): Boolean {
        val file = modelFile(context, entry)
        if (!file.isFile || file.length() <= 0L) return false
        if (entry.sizeBytes > 0L && file.length() != entry.sizeBytes) return false
        return true
    }

    /** Streams a file through SHA-256; null when the file cannot be read. */
    fun sha256(file: File): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        toHex(digest.digest())
    }.getOrNull()

    fun matches(entry: ModelManifestEntry, file: File, digestHex: String?): Boolean {
        if (entry.sha256.isNotBlank()) {
            if (digestHex == null) return false
            if (!digestHex.equals(entry.sha256, ignoreCase = true)) return false
        }
        if (entry.sizeBytes > 0L && file.length() != entry.sizeBytes) return false
        return true
    }

    /** Moves a finished .part file into place, replacing the previous file if any. */
    fun commitPart(context: Context, entry: ModelManifestEntry): Boolean {
        val part = partFile(context, entry)
        val target = modelFile(context, entry)
        if (!part.isFile || part.length() <= 0L) return false
        deleteQuietly(target)
        if (part.renameTo(target)) return true
        return runCatching {
            part.copyTo(target, overwrite = true)
            part.delete()
            target.isFile && target.length() > 0L
        }.getOrDefault(false)
    }

    fun deleteQuietly(file: File?) {
        runCatching { if (file != null && file.exists()) file.delete() }
    }

    /** Free bytes on the volume that holds the model directory; null when it cannot be read. */
    fun freeBytes(context: Context): Long? = runCatching {
        val stat = StatFs(modelsDirectory(context).absolutePath)
        stat.availableBytes
    }.getOrNull()

    /** A download must fit with headroom for the .part file, the final file and a rename. */
    fun hasRoomFor(context: Context, sizeBytes: Long): Boolean {
        if (sizeBytes <= 0L) return true
        val free = freeBytes(context) ?: return true
        return free > sizeBytes + DOWNLOAD_HEADROOM_BYTES
    }

    /** Android reports a full disk as an IOException whose message carries ENOSPC. */
    fun isOutOfSpace(failure: Throwable): Boolean =
        generateSequence(failure) { throwable -> throwable.cause }.any { cause ->
            cause is IOException && cause.message.orEmpty().contains("ENOSPC", ignoreCase = true)
        }

    /** Bundled models the user removed on purpose must not be reinstalled behind their back. */
    fun markRemoved(context: Context, modelId: String) {
        runCatching {
            File(modelsDirectory(context), REMOVED_PREFIX + safeId(modelId)).writeText("1")
        }
    }

    fun clearRemoved(context: Context, modelId: String) {
        runCatching { File(modelsDirectory(context), REMOVED_PREFIX + safeId(modelId)).delete() }
    }

    fun isRemoved(context: Context, modelId: String): Boolean =
        runCatching { File(modelsDirectory(context), REMOVED_PREFIX + safeId(modelId)).isFile }.getOrDefault(false)

    /**
     * The installed model with exactly this id. Unlike [findCutoutModel] this allows a hair-only
     * model to be chosen explicitly, because the user asked for that effect by name.
     */
    fun findModel(context: Context, modelId: String): InstalledModel? {
        val result = ManifestParser.readFromAssets(context)
        if (result !is ManifestResult.Ok) return null
        return result.models.asSequence()
            .filter { entry -> entry.id == modelId }
            .filter { entry -> entry.runtime.isBlank() || entry.runtime.equals("tflite", ignoreCase = true) }
            .filter { entry -> entry.inputSpec?.valid == true && entry.outputSpec?.valid == true }
            .filter { entry -> isInstalled(context, entry) }
            .firstOrNull()
            ?.let { entry -> InstalledModel(entry, modelFile(context, entry)) }
    }

    /**
     * Best installed model for subject cut-out. Portrait models are preferred over the general
     * DeepLab model, and hair-only models are ignored because they cannot describe a whole person.
     */
    fun findCutoutModel(context: Context): InstalledModel? {
        val result = ManifestParser.readFromAssets(context)
        if (result !is ManifestResult.Ok) return null
        return result.models.asSequence()
            .filter { entry -> entry.runtime.isBlank() || entry.runtime.equals("tflite", ignoreCase = true) }
            .filter { entry -> entry.inputSpec?.valid == true && entry.outputSpec?.valid == true }
            .filter { entry -> !isHairOnly(entry) }
            .filter { entry -> isInstalled(context, entry) }
            .sortedBy { entry -> rank(entry) }
            .firstOrNull()
            ?.let { entry -> InstalledModel(entry, modelFile(context, entry)) }
    }

    fun toHex(bytes: ByteArray): String {
        val characters = CharArray(bytes.size * 2)
        val digits = "0123456789abcdef"
        var index = 0
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            characters[index++] = digits[value ushr 4]
            characters[index++] = digits[value and 0x0F]
        }
        return String(characters)
    }

    private fun isHairOnly(entry: ModelManifestEntry): Boolean {
        val spec = entry.outputSpec ?: return false
        if (spec.channels > 2) return false
        return spec.channelLabels.any { label -> label.contains("hair", ignoreCase = true) }
    }

    /**
     * Preference order for the automatic choice. Portrait models come before the general
     * object model (DeepLab-V3), because a portrait cut-out is the common case; an explicitly
     * chosen model always wins over this ranking.
     */
    private fun rank(entry: ModelManifestEntry): Int {
        val channels = entry.outputSpec?.channels ?: 1
        return when {
            entry.id.contains("multiclass", ignoreCase = true) -> 0
            entry.id.contains("modnet", ignoreCase = true) -> 1
            entry.id.contains("deeplab", ignoreCase = true) -> 5
            entry.id.contains("landscape", ignoreCase = true) -> 3
            channels == 1 -> 2
            else -> 4
        }
    }

    private fun safeId(modelId: String): String = modelId.map { character ->
        if (character.isLetterOrDigit() || character == '-' || character == '_' || character == '.') {
            character
        } else {
            '_'
        }
    }.joinToString("")
}
