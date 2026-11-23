package com.midad.studio.core.ai.internal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException

/** How raw 8-bit channel values are scaled before they are fed to the model. */
enum class InputNormalization { ZERO_TO_ONE, MINUS_ONE_TO_ONE, ZERO_TO_255, IMAGENET }

/** Channel order of the input tensor. */
enum class ColorOrder { RGB, RGBA }

/** Post-processing that must be applied to the raw output tensor. */
enum class OutputActivation { NONE, SIGMOID, SOFTMAX }

enum class ModelDistribution { BUNDLED, DOWNLOAD }

/** Why the shipped catalogue could not be read; the UI turns this into a localized message. */
enum class ManifestProblem { MISSING, INVALID }

data class InputSpec(
    val shape: List<Int>,
    val dtype: String,
    val layoutNhwc: Boolean,
    val normalization: InputNormalization,
    val colorOrder: ColorOrder,
) {
    val width: Int get() = shape.getOrNull(if (layoutNhwc) 2 else 3) ?: 0
    val height: Int get() = shape.getOrNull(if (layoutNhwc) 1 else 2) ?: 0
    val channels: Int get() = shape.getOrNull(if (layoutNhwc) 3 else 1) ?: 3
    val valid: Boolean get() = shape.size == 4 && width > 0 && height > 0 && channels in 1..4
}

data class OutputSpec(
    val shape: List<Int>,
    val dtype: String,
    val activation: OutputActivation,
    val threshold: Float?,
    val channels: Int,
    val channelLabels: List<String>,
    val layoutNhwc: Boolean = true,
) {
    /** Index of the "background" class for multi-class outputs; -1 when the label is absent. */
    val backgroundChannel: Int
        get() = channelLabels.indexOfFirst { it.contains("background", ignoreCase = true) }

    val valid: Boolean get() = channels in 1..64
}

/** One model catalogue entry parsed from assets/models/manifest.json. */
data class ModelManifestEntry(
    val id: String,
    val displayName: String,
    val description: String,
    val licenseSpdx: String,
    val licenseUrl: String,
    val downloadUrl: String,
    val version: String,
    val fileName: String,
    val assetPath: String,
    val sizeBytes: Long,
    val sha256: String,
    val distribution: ModelDistribution,
    val runtime: String,
    val attribution: String,
    val modelCardUrl: String,
    val inputSpec: InputSpec?,
    val outputSpec: OutputSpec?,
) {
    val bundled: Boolean get() = distribution == ModelDistribution.BUNDLED
}

sealed class ManifestResult {
    data class Ok(val models: List<ModelManifestEntry>) : ManifestResult()
    data object Missing : ManifestResult()
    data class Invalid(val detail: String) : ManifestResult()
}

/**
 * Reads the model catalogue from assets.
 *
 * The parser never throws. A missing or malformed document becomes [ManifestResult.Missing] or
 * [ManifestResult.Invalid] so the UI can show an empty state, and a single bad entry is skipped
 * without discarding the rest. Both the documented {"schemaVersion":1,"models":[...]} document
 * and a bare JSON array are accepted; unknown fields are ignored.
 */
object ManifestParser {
    const val MANIFEST_ASSET = "models/manifest.json"

    fun readFromAssets(context: Context): ManifestResult {
        val raw = try {
            context.assets.open(MANIFEST_ASSET).bufferedReader().use { it.readText() }
        } catch (missing: FileNotFoundException) {
            return ManifestResult.Missing
        } catch (io: IOException) {
            return ManifestResult.Invalid(io.message ?: "unreadable")
        } catch (unexpected: Throwable) {
            return ManifestResult.Invalid(unexpected.message ?: "unreadable")
        }
        val models = parseDocument(raw) ?: return ManifestResult.Invalid("malformed catalogue")
        return ManifestResult.Ok(models)
    }

    fun parse(raw: String): List<ModelManifestEntry> = parseDocument(raw).orEmpty()

    private fun parseDocument(raw: String): List<ModelManifestEntry>? {
        val text = raw.trim().removePrefix("\uFEFF").trim()
        if (text.isEmpty()) return null
        val array: JSONArray = try {
            when {
                text.startsWith("[") -> JSONArray(text)
                text.startsWith("{") -> JSONObject(text).optJSONArray("models")
                else -> null
            }
        } catch (malformed: Throwable) {
            null
        } ?: return null
        val entries = ArrayList<ModelManifestEntry>(array.length())
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            parseEntry(json)?.let(entries::add)
        }
        return entries
    }

    private fun parseEntry(json: JSONObject): ModelManifestEntry? {
        val id = stringValue(json, "id")
        val fileName = sanitizeFileName(stringValue(json, "fileName"))
        if (id.isEmpty() || fileName.isEmpty()) return null
        val distribution = if (stringValue(json, "distribution").equals("BUNDLED", ignoreCase = true)) {
            ModelDistribution.BUNDLED
        } else {
            ModelDistribution.DOWNLOAD
        }
        val licenseSpdx = stringValue(json, "licenseSpdx").ifBlank { stringValue(json, "licenseName") }
        val downloadUrl = stringValue(json, "downloadUrl").ifBlank { stringValue(json, "sourceUrl") }
        return ModelManifestEntry(
            id = id,
            displayName = stringValue(json, "displayName").ifBlank { id },
            description = stringValue(json, "description"),
            licenseSpdx = licenseSpdx,
            licenseUrl = stringValue(json, "licenseUrl"),
            downloadUrl = downloadUrl,
            version = stringValue(json, "version"),
            fileName = fileName,
            assetPath = assetPathFor(json, fileName),
            sizeBytes = longValue(json, "sizeBytes", 0L),
            sha256 = normalizeSha256(stringValue(json, "sha256")),
            distribution = distribution,
            runtime = stringValue(json, "runtime").ifBlank { "tflite" },
            attribution = stringValue(json, "attribution"),
            modelCardUrl = stringValue(json, "modelCardUrl").ifBlank { stringValue(json, "licenseEvidenceUrl") },
            inputSpec = parseInputSpec(json.optJSONObject("inputSpec")),
            outputSpec = parseOutputSpec(json.optJSONObject("outputSpec")),
        )
    }

    private fun parseInputSpec(json: JSONObject?): InputSpec? {
        if (json == null) return null
        val shape = parseShape(json)
        if (shape.size != 4) return null
        val layout = stringValue(json, "layout").uppercase()
        return InputSpec(
            shape = shape,
            dtype = stringValue(json, "dtype").ifBlank { "float32" },
            layoutNhwc = !layout.contains("NCHW"),
            normalization = parseNormalization(stringValue(json, "normalization")),
            colorOrder = parseColorOrder(stringValue(json, "colorOrder")),
        )
    }

    private fun parseOutputSpec(json: JSONObject?): OutputSpec? {
        if (json == null) return null
        val shape = parseShape(json)
        val layoutNhwc = !stringValue(json, "layout").uppercase().contains("NCHW")
        val declaredChannels = intValue(json, "channels", 0)
        val channels = when {
            declaredChannels > 0 -> declaredChannels
            shape.size == 4 -> if (layoutNhwc) shape[3] else shape[1]
            shape.size >= 3 -> shape.last()
            else -> 1
        }
        return OutputSpec(
            shape = shape,
            dtype = stringValue(json, "dtype").ifBlank { "float32" },
            activation = parseActivation(stringValue(json, "activation")),
            threshold = floatValue(json, "threshold"),
            channels = channels,
            channelLabels = parseStringArray(json.optJSONArray("channelLabels")),
            layoutNhwc = layoutNhwc,
        )
    }

    private fun parseShape(json: JSONObject): List<Int> {
        val array = json.optJSONArray("shape") ?: return emptyList()
        val values = ArrayList<Int>(array.length())
        for (index in 0 until array.length()) {
            val value = array.optInt(index, 0)
            if (value <= 0) return emptyList()
            values.add(value)
        }
        return values
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val values = ArrayList<String>(array.length())
        for (index in 0 until array.length()) {
            val value = array.optString(index, "").trim()
            if (value.isNotEmpty()) values.add(value)
        }
        return values
    }

    /** Maps every wording found in the catalogue to one of the four supported preprocessing modes. */
    fun parseNormalization(raw: String): InputNormalization {
        val text = raw.lowercase()
        return when {
            text.contains("imagenet") -> InputNormalization.IMAGENET
            text.contains("127.5") -> InputNormalization.MINUS_ONE_TO_ONE
            text.contains("-1.0, 1.0") || text.contains("[-1, 1]") || text.contains("-1,1") ||
                text.contains("-1 to 1") || text.contains("-1-1") -> InputNormalization.MINUS_ONE_TO_ONE
            text.contains("0-1") || text.contains("0.0, 1.0") || text.contains("0.0,1.0") ||
                text.contains("[0, 1]") -> InputNormalization.ZERO_TO_ONE
            text.contains("0-255") || text.contains("0..255") || text.contains("[0, 255]") -> {
                InputNormalization.ZERO_TO_255
            }
            // Wording such as "(pixel - 0.0) / 255.0" means the usual 0..1 scale.
            text.contains("255") -> InputNormalization.ZERO_TO_ONE
            else -> InputNormalization.ZERO_TO_ONE
        }
    }

    fun parseActivation(raw: String): OutputActivation {
        val text = raw.lowercase()
        return when {
            text.contains("softmax") -> OutputActivation.SOFTMAX
            text.contains("sigmoid") -> OutputActivation.SIGMOID
            else -> OutputActivation.NONE
        }
    }

    private fun parseColorOrder(raw: String): ColorOrder =
        if (raw.trim().uppercase().startsWith("RGBA")) ColorOrder.RGBA else ColorOrder.RGB

    private fun stringValue(json: JSONObject, key: String): String {
        if (!json.has(key) || json.isNull(key)) return ""
        return json.optString(key, "").trim()
    }

    private fun intValue(json: JSONObject, key: String, fallback: Int): Int {
        if (!json.has(key) || json.isNull(key)) return fallback
        return when (val raw = json.opt(key)) {
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun longValue(json: JSONObject, key: String, fallback: Long): Long {
        if (!json.has(key) || json.isNull(key)) return fallback
        return when (val raw = json.opt(key)) {
            is Number -> raw.toLong()
            is String -> raw.trim().toLongOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun floatValue(json: JSONObject, key: String): Float? {
        if (!json.has(key) || json.isNull(key)) return null
        val value = when (val raw = json.opt(key)) {
            is Number -> raw.toFloat()
            is String -> raw.trim().toFloatOrNull()
            else -> null
        } ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return value.coerceIn(0f, 1f)
    }

    private fun normalizeSha256(raw: String): String {
        val text = raw.trim().lowercase().removePrefix("sha256:")
        if (text.length != 64) return ""
        if (!text.all { character -> character in "0123456789abcdef" }) return ""
        return text
    }

    private fun sanitizeFileName(raw: String): String {
        val name = raw.trim().substringAfterLast('/').substringAfterLast('\\')
        if (name.isEmpty() || name == "." || name == ".." || name.contains("..")) return ""
        if (!name.all { character -> character.isLetterOrDigit() || character == '.' || character == '_' || character == '-' }) {
            return ""
        }
        return name
    }

    private fun assetPathFor(json: JSONObject, fileName: String): String {
        val declared = stringValue(json, "assetPath")
        if (declared.startsWith("models/") && !declared.contains("..") && declared.endsWith(".tflite")) {
            return declared
        }
        return "models/" + fileName
    }
}
