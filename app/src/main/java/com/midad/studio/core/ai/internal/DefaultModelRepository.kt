package com.midad.studio.core.ai.internal

import android.content.Context
import com.midad.studio.core.ai.AiModel
import com.midad.studio.core.ai.AiModelStatus
import com.midad.studio.core.ai.ModelRepository
import com.midad.studio.core.ai.ModelState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Read-only catalogue facts shown in the model details dialog. */
data class ModelDetails(
    val id: String,
    val version: String,
    val attribution: String,
    val sourceUrl: String,
    val modelCardUrl: String,
    val licenseSpdx: String,
    val licenseUrl: String,
)

/** Stable failure codes stored in AiModelStatus.message; the UI turns them into localized text. */
object ModelFailure {
    const val CHECKSUM = "checksum"
    const val NETWORK = "network"
    const val STORAGE = "storage"
    const val NO_URL = "no_url"
}

/**
 * Model catalogue + file manager.
 *
 * The catalogue is read from assets; bundled binaries are copied into filesDir/models on first
 * refresh and verified against their SHA-256. Downloads stream to a .part file with progress,
 * verify the checksum and only then replace the installed file. Every disk or network operation
 * runs on Dispatchers.IO.
 */
class DefaultModelRepository(context: Context) : ModelRepository {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateLock = Any()
    private val jobs = mutableMapOf<String, Job>()

    private val _statuses = MutableStateFlow<List<AiModelStatus>>(emptyList())
    override val statuses: StateFlow<List<AiModelStatus>> = _statuses.asStateFlow()

    /** True once the shipped catalogue has been read; lets the UI tell loading from empty. */
    private val _catalogueReady = MutableStateFlow(false)
    val catalogueReady: StateFlow<Boolean> = _catalogueReady.asStateFlow()

    private val _manifestProblem = MutableStateFlow<ManifestProblem?>(null)
    val manifestProblem: StateFlow<ManifestProblem?> = _manifestProblem.asStateFlow()

    @Volatile
    private var entries: List<ModelManifestEntry> = emptyList()

    @Volatile
    private var refreshGeneration = 0

    override fun refresh() {
        val generation = synchronized(stateLock) { ++refreshGeneration }
        scope.launch {
            val result = ManifestParser.readFromAssets(appContext)
            if (generation != refreshGeneration) return@launch
            _manifestProblem.value = when (result) {
                is ManifestResult.Missing -> ManifestProblem.MISSING
                is ManifestResult.Invalid -> ManifestProblem.INVALID
                is ManifestResult.Ok -> null
            }
            val models = (result as? ManifestResult.Ok)?.models.orEmpty()
            entries = models
            publishCatalogue(models)
            _catalogueReady.value = true
            installMissingBundled(models)
        }
    }

    override fun download(modelId: String) {
        val entry = findEntry(modelId) ?: return
        startJob(entry) {
            when {
                ModelStorage.isInstalled(appContext, entry) -> publish(entry, ModelState.INSTALLED)
                entry.distribution == ModelDistribution.BUNDLED && ModelStorage.assetExists(appContext, entry) -> {
                    installBundled(entry)
                }
                else -> downloadRemote(entry)
            }
        }
    }

    override fun cancel(modelId: String) {
        val entry = findEntry(modelId) ?: return
        synchronized(stateLock) { jobs.remove(modelId)?.cancel() }
        publish(entry, ModelState.NOT_INSTALLED)
    }

    override fun remove(modelId: String) {
        val entry = findEntry(modelId) ?: return
        synchronized(stateLock) { jobs.remove(modelId)?.cancel() }
        scope.launch {
            ModelStorage.deleteQuietly(ModelStorage.modelFile(appContext, entry))
            ModelStorage.deleteQuietly(ModelStorage.partFile(appContext, entry))
            if (entry.distribution == ModelDistribution.BUNDLED) {
                ModelStorage.markRemoved(appContext, entry.id)
            }
            publish(entry, ModelState.NOT_INSTALLED)
        }
    }

    override fun isReady(modelId: String): Boolean =
        _statuses.value.firstOrNull { it.model.id == modelId }?.state == ModelState.INSTALLED

    /** Catalogue facts for the details dialog; empty before the first refresh. */
    fun allDetails(): Map<String, ModelDetails> = entries.associate { entry ->
        entry.id to ModelDetails(
            id = entry.id,
            version = entry.version,
            attribution = entry.attribution,
            sourceUrl = entry.downloadUrl,
            modelCardUrl = entry.modelCardUrl,
            licenseSpdx = entry.licenseSpdx,
            licenseUrl = entry.licenseUrl,
        )
    }

    private fun findEntry(modelId: String): ModelManifestEntry? =
        entries.firstOrNull { it.id == modelId }

    private fun publishCatalogue(models: List<ModelManifestEntry>) {
        synchronized(stateLock) {
            val previous = _statuses.value.associateBy { status -> status.model.id }
            _statuses.value = models.map { entry ->
                val current = previous[entry.id]
                when {
                    current != null && current.state == ModelState.DOWNLOADING -> current
                    current != null && current.state == ModelState.VERIFYING -> current
                    ModelStorage.isInstalled(appContext, entry) -> status(entry, ModelState.INSTALLED)
                    current != null && current.state == ModelState.FAILED -> current
                    else -> status(entry, ModelState.NOT_INSTALLED)
                }
            }
        }
    }

    private fun installMissingBundled(models: List<ModelManifestEntry>) {
        for (entry in models) {
            if (entry.distribution != ModelDistribution.BUNDLED) continue
            if (entry.runtime.isNotBlank() && !entry.runtime.equals("tflite", ignoreCase = true)) continue
            if (ModelStorage.isInstalled(appContext, entry)) continue
            if (!ModelStorage.assetExists(appContext, entry)) continue
            if (ModelStorage.isRemoved(appContext, entry.id)) continue
            startJob(entry) { installBundled(entry) }
        }
    }

    private fun startJob(entry: ModelManifestEntry, block: suspend () -> Unit) {
        synchronized(stateLock) {
            jobs.remove(entry.id)?.cancel()
            jobs[entry.id] = scope.launch {
                try {
                    block()
                } catch (cancelled: CancellationException) {
                    // A remote download keeps its .part file so the next attempt can resume.
                    if (entry.distribution == ModelDistribution.BUNDLED) {
                        ModelStorage.deleteQuietly(ModelStorage.partFile(appContext, entry))
                    }
                    publish(entry, ModelState.NOT_INSTALLED)
                    throw cancelled
                } catch (failure: Exception) {
                    // Keep a remote .part file: a later tap resumes it. Bundled copies start over.
                    if (entry.distribution == ModelDistribution.BUNDLED) {
                        ModelStorage.deleteQuietly(ModelStorage.partFile(appContext, entry))
                    }
                    val message = when {
                        ModelStorage.isOutOfSpace(failure) -> ModelFailure.STORAGE
                        entry.distribution == ModelDistribution.BUNDLED -> ModelFailure.STORAGE
                        else -> ModelFailure.NETWORK
                    }
                    publish(entry, ModelState.FAILED, message = message)
                } finally {
                    synchronized(stateLock) { jobs.remove(entry.id) }
                }
            }
        }
    }

    private suspend fun installBundled(entry: ModelManifestEntry) {
        if (ModelStorage.isInstalled(appContext, entry)) {
            publish(entry, ModelState.INSTALLED)
            return
        }
        if (!ModelStorage.assetExists(appContext, entry)) {
            publish(entry, ModelState.NOT_INSTALLED)
            return
        }
        val part = ModelStorage.partFile(appContext, entry)
        ModelStorage.deleteQuietly(part)
        publish(entry, ModelState.DOWNLOADING, progress = 0f)
        val digest = MessageDigest.getInstance("SHA-256")
        var copied = 0L
        var reported = -1f
        appContext.assets.open(entry.assetPath).use { input ->
            FileOutputStream(part).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    copied += read
                    if (entry.sizeBytes > 0L) {
                        val progress = (copied.toFloat() / entry.sizeBytes.toFloat()).coerceIn(0f, 1f)
                        if (progress - reported >= PROGRESS_STEP) {
                            reported = progress
                            publish(entry, ModelState.DOWNLOADING, progress)
                        }
                    }
                }
                output.flush()
            }
        }
        publish(entry, ModelState.VERIFYING, progress = 1f)
        if (!ModelStorage.matches(entry, part, ModelStorage.toHex(digest.digest()))) {
            ModelStorage.deleteQuietly(part)
            publish(entry, ModelState.FAILED, message = ModelFailure.CHECKSUM)
            return
        }
        if (!ModelStorage.commitPart(appContext, entry)) {
            ModelStorage.deleteQuietly(part)
            publish(entry, ModelState.FAILED, message = ModelFailure.STORAGE)
            return
        }
        ModelStorage.clearRemoved(appContext, entry.id)
        publish(entry, ModelState.INSTALLED)
    }

    private suspend fun downloadRemote(entry: ModelManifestEntry) {
        val url = entry.downloadUrl
        // HTTPS only: the model checksum protects against corruption, not against a
        // network attacker who can rewrite both the bytes and the manifest.
        if (!url.startsWith("https://")) {
            publish(entry, ModelState.FAILED, message = ModelFailure.NO_URL)
            return
        }
        val part = ModelStorage.partFile(appContext, entry)
        var existing = if (part.isFile) part.length() else 0L
        if (existing > MAX_DOWNLOAD_BYTES) {
            ModelStorage.deleteQuietly(part)
            existing = 0L
        }
        if (entry.sizeBytes > 0L && existing > entry.sizeBytes) {
            ModelStorage.deleteQuietly(part)
            existing = 0L
        }

        // A previous attempt may already have received the whole file but died before
        // verification. Verify and commit it without touching the network.
        if (entry.sizeBytes > 0L && existing == entry.sizeBytes) {
            publish(entry, ModelState.VERIFYING, progress = 1f)
            val digest = ModelStorage.sha256(part)
            if (ModelStorage.matches(entry, part, digest)) {
                finishDownload(entry, part)
                return
            }
            ModelStorage.deleteQuietly(part)
            existing = 0L
        }

        val remaining = if (entry.sizeBytes > 0L) (entry.sizeBytes - existing).coerceAtLeast(0L) else entry.sizeBytes
        if (remaining > 0L && !ModelStorage.hasRoomFor(appContext, remaining)) {
            publish(entry, ModelState.FAILED, message = ModelFailure.STORAGE)
            return
        }

        var offset = existing
        var restarts = 0
        while (true) {
            when (val attempt = streamRemoteAttempt(entry, url, part, offset)) {
                is DownloadAttempt.Complete -> {
                    publish(entry, ModelState.VERIFYING, progress = 1f)
                    if (!ModelStorage.matches(entry, part, attempt.sha256)) {
                        ModelStorage.deleteQuietly(part)
                        publish(entry, ModelState.FAILED, message = ModelFailure.CHECKSUM)
                        return
                    }
                    finishDownload(entry, part)
                    return
                }
                DownloadAttempt.Restart -> {
                    restarts++
                    if (restarts > MAX_RESTARTS) {
                        publish(entry, ModelState.FAILED, message = ModelFailure.NETWORK)
                        return
                    }
                    ModelStorage.deleteQuietly(part)
                    offset = 0L
                }
                DownloadAttempt.Failed -> return
            }
        }
    }

    private fun finishDownload(entry: ModelManifestEntry, part: java.io.File) {
        if (!ModelStorage.commitPart(appContext, entry)) {
            // The verified .part file is kept; retrying can still commit it.
            publish(entry, ModelState.FAILED, message = ModelFailure.STORAGE)
            return
        }
        ModelStorage.clearRemoved(appContext, entry.id)
        publish(entry, ModelState.INSTALLED)
    }

    /**
     * One HTTP attempt. It resumes from [startOffset] when the server supports Range, falls
     * back to a clean restart when it ignores Range, and keeps the .part file on network
     * failures so a later tap continues where this one stopped.
     */
    private suspend fun streamRemoteAttempt(
        entry: ModelManifestEntry,
        url: String,
        part: java.io.File,
        startOffset: Long,
    ): DownloadAttempt {
        var connection: HttpURLConnection? = null
        try {
            // Follow redirects manually so every hop is required to stay on HTTPS.
            var target = URL(url)
            var redirects = 0
            var opened: HttpURLConnection? = null
            while (opened == null) {
                val candidate = target.openConnection() as? HttpURLConnection
                if (candidate == null) {
                    publish(entry, ModelState.FAILED, message = ModelFailure.NO_URL)
                    return DownloadAttempt.Failed
                }
                candidate.instanceFollowRedirects = false
                candidate.connectTimeout = CONNECT_TIMEOUT_MS
                candidate.readTimeout = READ_TIMEOUT_MS
                candidate.setRequestProperty("Accept", "application/octet-stream")
                candidate.setRequestProperty("Accept-Encoding", "identity")
                candidate.setRequestProperty("User-Agent", "MidadStudio")
                if (startOffset > 0L) {
                    candidate.setRequestProperty("Range", "bytes=$startOffset-")
                }
                val code = candidate.responseCode
                if (code in 300..399) {
                    val location = candidate.getHeaderField("Location")
                    candidate.disconnect()
                    redirects++
                    val next = if (location.isNullOrBlank()) null else runCatching { URL(target, location) }.getOrNull()
                    if (next == null || redirects > MAX_REDIRECTS || next.protocol != "https") {
                        publish(entry, ModelState.FAILED, message = ModelFailure.NETWORK)
                        return DownloadAttempt.Failed
                    }
                    target = next
                    continue
                }
                opened = candidate
            }
            connection = opened
            val responseCode = opened.responseCode
            if (responseCode == HTTP_RANGE_NOT_SATISFIABLE) {
                // A stale or too-large partial cannot be extended; start clean once.
                return if (startOffset > 0L) DownloadAttempt.Restart else {
                    publish(entry, ModelState.FAILED, message = ModelFailure.NETWORK)
                    DownloadAttempt.Failed
                }
            }
            if (responseCode !in 200..299) {
                publish(entry, ModelState.FAILED, message = ModelFailure.NETWORK)
                return DownloadAttempt.Failed
            }

            var offset = startOffset
            var total = entry.sizeBytes
            val contentLength = opened.contentLengthLong
            if (responseCode == 206) {
                val range = parseContentRange(opened.getHeaderField("Content-Range"))
                if (range == null || range.first != offset) return DownloadAttempt.Restart
                if (total <= 0L && range.second > 0L) total = range.second
            } else {
                // 200 means the server ignored Range: replace whatever was on disk.
                offset = 0L
                if (total <= 0L && contentLength > 0L) total = contentLength
            }
            if (total > MAX_DOWNLOAD_BYTES || contentLength > MAX_DOWNLOAD_BYTES) {
                ModelStorage.deleteQuietly(part)
                publish(entry, ModelState.FAILED, message = ModelFailure.NETWORK)
                return DownloadAttempt.Failed
            }
            if (total > 0L && entry.sizeBytes > 0L && total != entry.sizeBytes) {
                ModelStorage.deleteQuietly(part)
                publish(entry, ModelState.FAILED, message = ModelFailure.CHECKSUM)
                return DownloadAttempt.Failed
            }

            val digest = if (offset > 0L) {
                digestOf(part) ?: return DownloadAttempt.Restart
            } else {
                MessageDigest.getInstance("SHA-256")
            }
            var downloaded = offset
            var reported = if (total > 0L) (offset.toFloat() / total.toFloat()).coerceIn(0f, 0.99f) else 0f
            publish(entry, ModelState.DOWNLOADING, progress = reported)
            opened.inputStream.use { input ->
                FileOutputStream(part, offset > 0L).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        if (downloaded > MAX_DOWNLOAD_BYTES) {
                            ModelStorage.deleteQuietly(part)
                            publish(entry, ModelState.FAILED, message = ModelFailure.NETWORK)
                            return DownloadAttempt.Failed
                        }
                        if (entry.sizeBytes > 0L && downloaded > entry.sizeBytes) {
                            ModelStorage.deleteQuietly(part)
                            publish(entry, ModelState.FAILED, message = ModelFailure.CHECKSUM)
                            return DownloadAttempt.Failed
                        }
                        if (total > 0L) {
                            val progress = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 0.99f)
                            if (progress - reported >= PROGRESS_STEP) {
                                reported = progress
                                publish(entry, ModelState.DOWNLOADING, progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            val length = part.length()
            if (total > 0L && length != total) {
                // Preserve the partial file; the next tap sends the new offset.
                publish(entry, ModelState.FAILED, message = ModelFailure.NETWORK)
                return DownloadAttempt.Failed
            }
            if (entry.sizeBytes > 0L && length != entry.sizeBytes) {
                if (length < entry.sizeBytes) {
                    publish(entry, ModelState.FAILED, message = ModelFailure.NETWORK)
                } else {
                    ModelStorage.deleteQuietly(part)
                    publish(entry, ModelState.FAILED, message = ModelFailure.CHECKSUM)
                }
                return DownloadAttempt.Failed
            }
            return DownloadAttempt.Complete(ModelStorage.toHex(digest.digest()))
        } catch (failure: IOException) {
            val message = if (ModelStorage.isOutOfSpace(failure)) ModelFailure.STORAGE else ModelFailure.NETWORK
            publish(entry, ModelState.FAILED, message = message)
            return DownloadAttempt.Failed
        } catch (failure: SecurityException) {
            publish(entry, ModelState.FAILED, message = ModelFailure.NETWORK)
            return DownloadAttempt.Failed
        } finally {
            connection?.disconnect()
        }
    }

    private fun digestOf(file: java.io.File): MessageDigest? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest
    }.getOrNull()

    private fun publish(
        entry: ModelManifestEntry,
        state: ModelState,
        progress: Float = 0f,
        message: String? = null,
    ) {
        synchronized(stateLock) {
            val updated = AiModelStatus(
                model = entry.toAiModel(),
                state = state,
                progress = progress.coerceIn(0f, 1f),
                message = message,
            )
            val current = _statuses.value
            val index = current.indexOfFirst { it.model.id == entry.id }
            _statuses.value = if (index >= 0) {
                current.toMutableList().also { list -> list[index] = updated }
            } else {
                current + updated
            }
        }
    }

    private fun status(entry: ModelManifestEntry, state: ModelState): AiModelStatus =
        AiModelStatus(model = entry.toAiModel(), state = state)

    private fun ModelManifestEntry.toAiModel(): AiModel = AiModel(
        id = id,
        displayName = displayName.ifBlank { id },
        description = description,
        licenseName = licenseSpdx,
        licenseUrl = licenseUrl,
        sizeBytes = sizeBytes,
        bundled = distribution == ModelDistribution.BUNDLED,
    )

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val PROGRESS_STEP = 0.01f
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 60_000
        const val MAX_REDIRECTS = 3
        const val MAX_RESTARTS = 1

        /** Absolute ceiling for one model file; prevents a malicious manifest from filling storage. */
        const val MAX_DOWNLOAD_BYTES = 512L * 1024 * 1024
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
    }
}

private sealed interface DownloadAttempt {
    data class Complete(val sha256: String) : DownloadAttempt
    data object Restart : DownloadAttempt
    data object Failed : DownloadAttempt
}

/**
 * Parses an HTTP Content-Range header. Returns (firstByte, totalBytes), where totalBytes is
 * 0 for "*". Visible for unit tests.
 */
internal fun parseContentRange(header: String?): Pair<Long, Long>? {
    if (header.isNullOrBlank()) return null
    val match = Regex("^bytes\\s+(\\d+)-(\\d+)/(\\d+|\\*)$", RegexOption.IGNORE_CASE)
        .matchEntire(header.trim()) ?: return null
    val first = match.groupValues[1].toLongOrNull() ?: return null
    val last = match.groupValues[2].toLongOrNull() ?: return null
    val total = if (match.groupValues[3] == "*") 0L else match.groupValues[3].toLongOrNull() ?: return null
    if (first < 0L || last < first || (total > 0L && last >= total)) return null
    return first to total
}
