package com.midad.studio.studio.design.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.studio.design.ExportState
import com.midad.studio.ui.components.ExportMediaKind
import com.midad.studio.ui.components.ExportSuccessDialog

/** One dialog that owns the whole save flow: progress, success with share, and retry. */
@Composable
fun ExportDialog(
    state: ExportState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onShare: (ExportState.Success) -> Unit,
    onOpen: (ExportState.Success) -> Unit,
) {
    when (state) {
        ExportState.Idle -> Unit
        ExportState.Rendering, ExportState.Saving -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.d_export_title)) },
            text = { ExportProgress(isSaving = state is ExportState.Saving) },
            confirmButton = {},
        )
        is ExportState.Success -> ExportSuccessDialog(
            title = stringResource(R.string.d_export_done_title),
            message = stringResource(
                if (state.png) R.string.d_export_png_note else R.string.d_export_jpg_note,
            ),
            fileName = state.result.fileName,
            uri = state.result.uri,
            inGallery = state.result.inGallery,
            kind = ExportMediaKind.IMAGE,
            onOpen = { onOpen(state) },
            onShare = { onShare(state) },
            onClose = onDismiss,
        )
        is ExportState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.d_export_title)) },
            text = {
                Text(
                    text = stringResource(state.error.messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.d_export_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ExportProgress(isSaving: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        Text(
            text = stringResource(
                if (isSaving) R.string.d_export_saving else R.string.d_export_rendering,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}


