package com.midad.studio.studio.video

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.ui.components.ExportMediaKind
import com.midad.studio.ui.components.ExportSuccessDialog
import com.midad.studio.ui.text.MidadNumbers

/** One dialog per export phase; the export itself lives in the state holder. */
@Composable
internal fun ExportDialogs(
    export: VideoExportState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onShare: (Uri) -> Unit,
    onOpen: (Uri) -> Unit,
) {
    when (export) {
        VideoExportState.Idle -> Unit
        is VideoExportState.Running -> ExportProgressDialog(progress = export.progress, onCancel = onCancel)
        VideoExportState.Saving -> ExportSavingDialog()
        is VideoExportState.Success -> ExportSuccessDialog(
            title = stringResource(R.string.v_export_done_title),
            message = stringResource(R.string.v_export_done_body),
            fileName = export.fileName,
            uri = export.uri,
            inGallery = export.inGallery,
            kind = ExportMediaKind.VIDEO,
            onOpen = { onOpen(export.uri) },
            onShare = { onShare(export.uri) },
            onClose = onDismiss,
        )
        is VideoExportState.Failure -> ExportFailureDialog(
            error = export.error,
            onDismiss = onDismiss,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun ExportProgressDialog(progress: Float?, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.v_exporting_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (progress != null) {
                    ProgressTrack(progress = progress)
                    Text(
                        text = stringResource(
                            R.string.v_export_progress_fmt,
                            MidadNumbers.format((progress * 100).toInt().toLong()),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.v_export_progress_unknown),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.v_exporting_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.v_export_cancel))
            }
        },
    )
}

@Composable
private fun ExportSavingDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.v_export_saving_title)) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.v_export_saving_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun ExportFailureDialog(
    error: VideoExportError,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.v_export_failed_title)) },
        text = {
            Text(
                text = stringResource(videoExportErrorRes(error)),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.v_export_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun ProgressTrack(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // Exposes the bar to TalkBack as a real progress indicator instead of an
            // unlabelled decorative box.
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress.coerceIn(0f, 1f),
                    range = 0f..1f,
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@StringRes
internal fun videoExportErrorRes(error: VideoExportError): Int = when (error) {
    VideoExportError.GENERIC -> R.string.v_error_generic
    VideoExportError.DECODE -> R.string.v_error_decode
    VideoExportError.ENCODE -> R.string.v_error_encode
    VideoExportError.IO -> R.string.v_error_io
    VideoExportError.EFFECT -> R.string.v_error_effect
    VideoExportError.SAVE -> R.string.v_error_save
    VideoExportError.NO_SPACE -> R.string.export_error_no_space
}
