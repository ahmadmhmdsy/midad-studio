package com.midad.studio.core.export

import androidx.annotation.StringRes
import com.midad.studio.R

/**
 * Every way an export can fail, in the words the user needs. A single generic
 * "export failed" hides the two cases the user can actually act on: a full disk
 * and a write that could not complete.
 */
enum class ExportError(@StringRes val messageRes: Int) {
    NO_SPACE(R.string.export_error_no_space),
    IO(R.string.export_error_io),
    ENCODE(R.string.export_error_encode),
    UNKNOWN(R.string.export_error_unknown),
}

sealed interface ExportOutcome {
    data class Success(val result: ExportResult) : ExportOutcome
    data class Failure(val error: ExportError, val cause: Throwable? = null) : ExportOutcome
}

/**
 * Space estimates are deliberately conservative. JPEG output rarely exceeds
 * half a byte per pixel; PNG can approach four. The margin absorbs the MediaStore
 * row, thumbnails and filesystem overhead.
 */
object ExportSizing {
    const val SAFETY_MARGIN_BYTES: Long = 8L * 1024L * 1024L

    fun requiredImageBytes(width: Int, height: Int, png: Boolean): Long {
        val pixels = width.coerceAtLeast(1).toLong() * height.coerceAtLeast(1).toLong()
        val perPixel = if (png) 4L else 2L
        return pixels * perPixel + SAFETY_MARGIN_BYTES
    }

    fun requiredVideoBytes(sourceBytes: Long): Long =
        sourceBytes.coerceAtLeast(1L) * 2L + 32L * 1024L * 1024L
}
