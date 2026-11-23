package com.midad.studio.studio.photo.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.midad.studio.R
import com.midad.studio.core.export.ExportManager
import com.midad.studio.studio.photo.engine.ExportFormat
import com.midad.studio.studio.photo.engine.ExportLongEdge
import com.midad.studio.studio.photo.ui.PanelHint
import com.midad.studio.studio.photo.ui.SliderRow
import com.midad.studio.ui.components.ExportMediaKind
import com.midad.studio.ui.components.ExportResultCard
import com.midad.studio.ui.components.MidadChoiceChip
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.text.MidadNumbers
import kotlin.math.roundToInt

/** Format, size and quality options, then progress and the saved result. */
@Composable
fun ExportDialog(
    state: PhotoEditorState,
    viewModel: PhotoEditorViewModel,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = { if (!state.exporting) viewModel.closeExportDialog() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val result = state.exportResult
                when {
                    result != null -> {
                        Text(
                            text = stringResource(R.string.p_export_done_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val mime = if (state.exportOptions.format == ExportFormat.PNG) "image/png" else "image/jpeg"
                        ExportResultCard(
                            fileName = result.fileName,
                            uri = result.uri,
                            inGallery = result.inGallery,
                            kind = ExportMediaKind.IMAGE,
                            onOpen = { ExportManager.open(context, result.uri, mime) },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { viewModel.closeExportDialog() }) {
                                Text(stringResource(R.string.action_close))
                            }
                            TextButton(
                                onClick = { ExportManager.share(context, result.uri, mime) },
                                modifier = Modifier.padding(start = 4.dp),
                            ) {
                                Text(stringResource(R.string.action_share))
                            }
                            Button(
                                onClick = { ExportManager.open(context, result.uri, mime) },
                                modifier = Modifier.padding(start = 8.dp),
                            ) {
                                Text(stringResource(R.string.action_open))
                            }
                        }
                    }

                    state.exporting -> {
                        Text(
                            text = stringResource(R.string.p_export_working),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LinearProgressIndicator(
                            progress = { state.exportProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(
                                R.string.p_export_progress_fmt,
                                (state.exportProgress * 100f).roundToInt(),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        Text(
                            text = stringResource(R.string.p_export_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        SectionLabel(stringResource(R.string.p_export_format))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MidadChoiceChip(
                                label = stringResource(R.string.p_export_format_jpeg),
                                selected = state.exportOptions.format == ExportFormat.JPEG,
                                onClick = { viewModel.setExportFormat(ExportFormat.JPEG) },
                            )
                            MidadChoiceChip(
                                label = stringResource(R.string.p_export_format_png),
                                selected = state.exportOptions.format == ExportFormat.PNG,
                                onClick = { viewModel.setExportFormat(ExportFormat.PNG) },
                            )
                        }
                        if (state.exportOptions.format == ExportFormat.JPEG) {
                            SliderRow(
                                label = stringResource(R.string.p_export_quality),
                                valueText = MidadNumbers.format(state.exportOptions.quality.toLong()),
                                value = state.exportOptions.quality.toFloat(),
                                valueRange = 60f..100f,
                                onValueChange = { viewModel.setExportQuality(it.roundToInt()) },
                            )
                        } else {
                            PanelHint(stringResource(R.string.p_export_png_note))
                        }
                        SectionLabel(stringResource(R.string.p_export_size))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ExportLongEdge.entries.forEach { edge ->
                                MidadChoiceChip(
                                    label = stringResource(exportLongEdgeLabel(edge)),
                                    selected = state.exportOptions.longEdge == edge,
                                    onClick = { viewModel.setExportLongEdge(edge) },
                                )
                            }
                        }
                        if (state.exportOptions.longEdge == ExportLongEdge.ORIGINAL) {
                            PanelHint(stringResource(R.string.p_export_size_original_note))
                        }
                        PanelHint(stringResource(R.string.p_export_metadata_note))
                        val failure = state.exportFailure
                        if (failure != null) {
                            Text(
                                text = stringResource(failure.messageRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { viewModel.closeExportDialog() }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                            Button(
                                onClick = { viewModel.startExport(context) },
                                modifier = Modifier.padding(start = 8.dp),
                            ) {
                                Text(stringResource(R.string.p_action_export))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun exportLongEdgeLabel(edge: ExportLongEdge): Int = when (edge) {
    ExportLongEdge.ORIGINAL -> R.string.p_export_size_original
    ExportLongEdge.PX_2048 -> R.string.p_export_size_2048
    ExportLongEdge.PX_1080 -> R.string.p_export_size_1080
}
