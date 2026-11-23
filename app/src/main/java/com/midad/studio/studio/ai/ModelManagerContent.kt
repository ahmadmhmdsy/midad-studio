package com.midad.studio.studio.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.core.ai.AiModelStatus
import com.midad.studio.core.ai.ModelState
import com.midad.studio.core.ai.internal.ManifestProblem
import com.midad.studio.core.ai.internal.ModelDetails
import com.midad.studio.core.format.formatBytes
import com.midad.studio.ui.components.BorderedPanel
import com.midad.studio.ui.components.HairlineDivider
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.components.StudioScaffold
import com.midad.studio.ui.theme.MidadColors
import kotlin.math.roundToInt

@Composable
fun ModelManagerContent(
    state: AiUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDownload: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRemove: (String) -> Unit,
    onTry: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onDismissError: () -> Unit,
) {
    var removeTarget by remember { mutableStateOf<AiModelStatus?>(null) }
    var detailsTarget by remember { mutableStateOf<AiModelStatus?>(null) }

    StudioScaffold(title = stringResource(R.string.a_models_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.a_models_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AiErrorBanner(error = state.error, onDismiss = onDismissError)
            when {
                state.catalogueLoading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.a_models_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.models.isEmpty() -> EmptyCataloguePanel(
                    problem = state.catalogueProblem,
                    onRefresh = onRefresh,
                )

                else -> {
                    state.models.forEach { status ->
                        ModelRow(
                            status = status,
                            onDownload = onDownload,
                            onCancel = onCancel,
                            onTry = onTry,
                            onAskRemove = { removeTarget = it },
                            onAskDetails = { detailsTarget = it },
                            onOpenLink = onOpenLink,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onRefresh) {
                            Text(stringResource(R.string.a_action_refresh))
                        }
                    }
                    Text(
                        text = stringResource(R.string.a_models_download_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HairlineDivider()
            ModelLicenceNote()
            Spacer(Modifier.height(20.dp))
        }
    }

    val removing = removeTarget
    if (removing != null) {
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text(stringResource(R.string.a_model_remove_title)) },
            text = { Text(stringResource(R.string.a_model_remove_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        removeTarget = null
                        onRemove(removing.model.id)
                    },
                ) {
                    Text(stringResource(R.string.a_action_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val details = detailsTarget
    if (details != null) {
        ModelDetailsDialog(
            status = details,
            details = state.details[details.model.id],
            onOpenLink = onOpenLink,
            onDismiss = { detailsTarget = null },
        )
    }
}

@Composable
private fun ModelRow(
    status: AiModelStatus,
    onDownload: (String) -> Unit,
    onCancel: (String) -> Unit,
    onTry: (String) -> Unit,
    onAskRemove: (AiModelStatus) -> Unit,
    onAskDetails: (AiModelStatus) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    BorderedPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = modelTitle(status),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (status.model.description.isNotBlank()) {
                        Text(
                            text = modelDescription(status),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = status.model.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                ModelStateChip(status)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.a_model_size_fmt,
                        formatBytes(status.model.sizeBytes),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (status.model.licenseName.isNotBlank()) {
                    Text(
                        text = status.model.licenseName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (status.state == ModelState.DOWNLOADING || status.state == ModelState.VERIFYING) {
                AiProgressBar(progress = status.progress)
                Text(
                    text = stringResource(
                        R.string.a_models_progress_fmt,
                        (status.progress * 100f).roundToInt().coerceIn(0, 100),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (status.state == ModelState.FAILED) {
                Text(
                    text = modelFailureText(status.message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (status.state) {
                    ModelState.INSTALLED, ModelState.BUNDLED -> {
                        Button(onClick = { onTry(status.model.id) }) {
                            Text(stringResource(R.string.a_action_try))
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { onAskRemove(status) }) {
                            Text(stringResource(R.string.a_action_remove))
                        }
                    }

                    ModelState.NOT_INSTALLED, ModelState.FAILED -> {
                        Button(onClick = { onDownload(status.model.id) }) {
                            Text(
                                stringResource(
                                    if (status.state == ModelState.FAILED) {
                                        R.string.a_action_retry
                                    } else {
                                        R.string.a_action_install
                                    },
                                ),
                            )
                        }
                    }

                    ModelState.DOWNLOADING, ModelState.VERIFYING -> {
                        OutlinedButton(onClick = { onCancel(status.model.id) }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onAskDetails(status) }) {
                    Text(stringResource(R.string.a_action_details))
                }
            }
            if (status.model.licenseUrl.isNotBlank()) {
                TextButton(onClick = { onOpenLink(status.model.licenseUrl) }) {
                    Text(stringResource(R.string.a_action_license))
                }
            }
        }
    }
}

@Composable
private fun EmptyCataloguePanel(problem: ManifestProblem?, onRefresh: () -> Unit) {
    val detail = when (problem) {
        ManifestProblem.MISSING -> stringResource(R.string.a_models_missing_catalogue)
        ManifestProblem.INVALID -> stringResource(R.string.a_models_invalid_catalogue)
        null -> stringResource(R.string.a_models_empty_body)
    }
    BorderedPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.a_models_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRefresh) { Text(stringResource(R.string.a_action_refresh)) }
        }
    }
}

@Composable
private fun ModelDetailsDialog(
    status: AiModelStatus,
    details: ModelDetails?,
    onOpenLink: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val model = status.model
    val version = details?.version.orEmpty()
    val attribution = details?.attribution.orEmpty()
    val sourceUrl = details?.sourceUrl.orEmpty()
    val modelCardUrl = details?.modelCardUrl.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.a_model_details_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = modelTitle(status), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (model.description.isNotBlank()) {
                    Text(
                        text = modelDescription(status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.a_model_size_fmt,
                        formatBytes(model.sizeBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (version.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.a_model_version_fmt, version),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (model.licenseName.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.a_model_license_fmt, model.licenseName),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (attribution.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.a_model_attribution_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = attribution,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (model.licenseUrl.isNotBlank()) {
                    TextButton(onClick = { onOpenLink(model.licenseUrl) }) {
                        Text(stringResource(R.string.a_action_license))
                    }
                }
                if (modelCardUrl.isNotBlank()) {
                    TextButton(onClick = { onOpenLink(modelCardUrl) }) {
                        Text(stringResource(R.string.a_model_card_link))
                    }
                }
                if (sourceUrl.isNotBlank()) {
                    TextButton(onClick = { onOpenLink(sourceUrl) }) {
                        Text(stringResource(R.string.a_model_source_link))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}
