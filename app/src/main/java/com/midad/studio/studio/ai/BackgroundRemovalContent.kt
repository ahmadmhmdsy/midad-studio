package com.midad.studio.studio.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.projects.ProjectSaveButton
import com.midad.studio.projects.ProjectSaveStyle
import com.midad.studio.ui.components.BorderedPanel
import com.midad.studio.ui.components.ExportMediaKind
import com.midad.studio.ui.components.ExportSuccessDialog
import com.midad.studio.ui.components.MidadChoiceChip
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.components.StudioScaffold
import com.midad.studio.ui.theme.MidadColors

@Composable
fun BackgroundRemovalContent(
    state: AiUiState,
    onBack: () -> Unit,
    onPickImage: (Uri) -> Unit,
    onRetry: () -> Unit,
    onStartOver: () -> Unit,
    onSelectBackdrop: (AiBackdrop) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onOpenSaved: () -> Unit,
    onDismissSaved: () -> Unit,
    onSelectModel: (String?) -> Unit,
    onOpenModels: () -> Unit,
    onDismissError: () -> Unit,
    onSaveProject: suspend (String) -> Boolean,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPickImage(uri)
    }
    val pick: () -> Unit = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val saved = state.saved
    if (saved != null && state.savedDialog) {
        ExportSuccessDialog(
            title = stringResource(R.string.a_saved_dialog_title),
            message = stringResource(R.string.a_saved_dialog_body),
            fileName = saved.fileName,
            uri = saved.uri,
            inGallery = saved.inGallery,
            kind = ExportMediaKind.IMAGE,
            onOpen = onOpenSaved,
            onShare = onShare,
            onClose = onDismissSaved,
        )
    }
    StudioScaffold(
        title = state.projectTitle ?: stringResource(R.string.a_cutout_title),
        onBack = onBack,
        actions = {
            if (state.source != null) {
                ProjectSaveButton(
                    style = ProjectSaveStyle.TOOLBAR_ACTION,
                    hasContent = true,
                    activeProjectTitle = state.projectTitle,
                    defaultTitle = stringResource(R.string.projects_default_title_ai),
                    onSave = onSaveProject,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AiErrorBanner(
                error = state.error,
                onDismiss = onDismissError,
                actionLabel = if (state.error == AiError.NO_MODEL) {
                    stringResource(R.string.a_action_open_models)
                } else {
                    null
                },
                onAction = if (state.error == AiError.NO_MODEL) onOpenModels else null,
            )
            if (state.source == null) {
                CutoutEmptyState(state = state, onPick = pick, onOpenModels = onOpenModels)
            } else {
                PreviewPanel(state = state)
                if (state.cutout != null) {
                    ModelPicker(
                        state = state,
                        onSelectModel = onSelectModel,
                        onOpenModels = onOpenModels,
                    )
                    SectionLabel(stringResource(R.string.a_backdrop_label))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AiBackdrop.entries.forEach { backdrop ->
                            MidadChoiceChip(
                                label = backdropLabel(backdrop),
                                selected = state.backdrop == backdrop,
                                onClick = { if (!state.busy) onSelectBackdrop(backdrop) },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onSave,
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                        OutlinedButton(onClick = pick, enabled = !state.busy) {
                            Text(stringResource(R.string.a_action_change_photo))
                        }
                    }
                    if (state.error != null) {
                        TextButton(onClick = onRetry, enabled = !state.busy) {
                            Text(stringResource(R.string.a_action_retry))
                        }
                    }
                    if (saved != null) {
                        SavedPanel(saved = saved, onOpen = onOpenSaved, onShare = onShare)
                    }
                    TextButton(onClick = onStartOver, enabled = !state.busy) {
                        Text(stringResource(R.string.a_action_start_over))
                    }
                    Text(
                        text = stringResource(
                            if (state.backdrop == AiBackdrop.TRANSPARENT) {
                                R.string.a_cutout_export_note
                            } else {
                                R.string.a_cutout_export_note_jpg
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (state.error == null) {
                    TextButton(onClick = pick, enabled = !state.busy) {
                        Text(stringResource(R.string.a_action_change_photo))
                    }
                } else {
                    TextButton(onClick = onRetry, enabled = !state.busy) {
                        Text(stringResource(R.string.a_action_retry))
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun ModelPicker(
    state: AiUiState,
    onSelectModel: (String?) -> Unit,
    onOpenModels: () -> Unit,
) {
    SectionLabel(stringResource(R.string.a_model_label))
    val models = state.installedModels
    val listState = rememberLazyListState()
    // Keep the active model visible: with five models the chosen chip can start off-screen.
    LaunchedEffect(state.selectedModelId, models.size) {
        val index = models.indexOfFirst { it.model.id == state.selectedModelId }
        if (index >= 0) listState.animateScrollToItem(index + 1)
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            MidadChoiceChip(
                label = stringResource(R.string.a_model_auto),
                selected = state.selectedModelId == null,
                onClick = { if (!state.busy) onSelectModel(null) },
            )
        }
        items(models, key = { it.model.id }) { status ->
            MidadChoiceChip(
                label = modelTitle(status),
                selected = state.selectedModelId == status.model.id,
                onClick = { if (!state.busy) onSelectModel(status.model.id) },
            )
        }
        item {
            TextButton(onClick = onOpenModels, enabled = !state.busy) {
                Text(stringResource(R.string.a_action_open_models))
            }
        }
    }
    state.selectedModel?.let { status ->
        Text(
            text = modelDescription(status),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreviewPanel(state: AiUiState) {
    val shown = (if (state.backdrop == AiBackdrop.TRANSPARENT) {
        state.cutout
    } else {
        state.preview ?: state.cutout
    }) ?: state.source
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 430.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                RoundedCornerShape(20.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state.cutout != null && state.backdrop == AiBackdrop.TRANSPARENT) {
            CheckerboardBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp)),
            )
        }
        if (shown != null) {
            Image(
                bitmap = shown.asImageBitmap(),
                contentDescription = stringResource(R.string.a_cutout_preview_desc),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
            )
        }
        if (state.busy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = cutoutStageLabel(state.stage),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }
    }
}

@Composable
private fun CutoutEmptyState(state: AiUiState, onPick: () -> Unit, onOpenModels: () -> Unit) {
    BorderedPanel {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AiGlyph(AiGlyphKind.CUTOUT, MidadColors.Sage, Modifier.size(46.dp))
            Text(
                text = stringResource(R.string.a_cutout_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.a_cutout_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (!state.hasModel) {
                Text(
                    text = stringResource(R.string.a_cutout_no_model_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onOpenModels) {
                    Text(stringResource(R.string.a_action_open_models))
                }
            }
            if (state.busy) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                Text(
                    text = cutoutStageLabel(state.stage),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Button(onClick = onPick) {
                    Text(stringResource(R.string.a_action_choose_photo))
                }
            }
        }
    }
}

@Composable
private fun SavedPanel(saved: SavedCutout, onOpen: () -> Unit, onShare: () -> Unit) {
    BorderedPanel {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.a_saved_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (saved.inGallery) {
                    stringResource(R.string.export_result_location_gallery_fmt, "Pictures/Midad")
                } else {
                    stringResource(R.string.export_result_location_app)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = saved.fileName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onShare) { Text(stringResource(R.string.action_share)) }
                Button(onClick = onOpen) { Text(stringResource(R.string.action_open)) }
            }
        }
    }
}

@Composable
private fun backdropLabel(backdrop: AiBackdrop): String = stringResource(
    when (backdrop) {
        AiBackdrop.TRANSPARENT -> R.string.a_backdrop_transparent
        AiBackdrop.BLUR -> R.string.a_backdrop_blur
        AiBackdrop.PAPER -> R.string.a_backdrop_paper
        AiBackdrop.INK -> R.string.a_backdrop_ink
        AiBackdrop.WHITE -> R.string.a_backdrop_white
    },
)
