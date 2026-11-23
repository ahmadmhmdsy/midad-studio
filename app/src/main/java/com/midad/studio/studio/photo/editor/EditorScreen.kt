package com.midad.studio.studio.photo.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.navigation.StudioKind
import com.midad.studio.projects.ProjectSaveButton
import com.midad.studio.projects.ProjectSaveStyle
import com.midad.studio.studio.photo.ui.RedoGlyph
import com.midad.studio.studio.photo.ui.UndoGlyph
import com.midad.studio.ui.components.StudioGlyph
import com.midad.studio.ui.components.StudioScaffold

private enum class LeaveAction { BACK, NEW_PHOTO }

/** The ready-state photo editor: stage, top actions, tool rail and dialogs. */
@Composable
fun PhotoEditorScreen(
    state: PhotoEditorState,
    viewModel: PhotoEditorViewModel,
    onBack: () -> Unit,
    onPickNewPhoto: () -> Unit,
) {
    val context = LocalContext.current
    var pendingLeave by remember { mutableStateOf<LeaveAction?>(null) }
    val undoDescription = stringResource(R.string.p_action_undo)
    val redoDescription = stringResource(R.string.p_action_redo)
    val aiDescription = stringResource(R.string.p_action_ai)
    val exportDescription = stringResource(R.string.p_action_export)

    val handleBack: () -> Unit = {
        when {
            state.exportDialogOpen -> if (!state.exporting) viewModel.closeExportDialog()
            state.aiHint -> viewModel.dismissAiHint()
            state.activeTool != null -> viewModel.closeToolPanel()
            state.hasEdits -> pendingLeave = LeaveAction.BACK
            else -> onBack()
        }
    }
    BackHandler(onBack = handleBack)

    Box(modifier = Modifier.fillMaxSize()) {
        StudioScaffold(
            // A reopened project shows its own name, so the user can see which edit is open.
            title = state.projectTitle ?: stringResource(R.string.studio_photo_title),
            onBack = handleBack,
            actions = {
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = state.canUndo,
                    modifier = Modifier.semantics { contentDescription = undoDescription },
                ) {
                    UndoGlyph(
                        color = if (state.canUndo) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = state.canRedo,
                    modifier = Modifier.semantics { contentDescription = redoDescription },
                ) {
                    RedoGlyph(
                        color = if (state.canRedo) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(
                    onClick = { viewModel.runCutout(context) },
                    enabled = !state.aiBusy,
                    modifier = Modifier.semantics { contentDescription = aiDescription },
                ) {
                    StudioGlyph(
                        kind = StudioKind.AI,
                        color = if (state.aiBusy) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Button(
                    onClick = { viewModel.openExportDialog() },
                    enabled = !state.exporting,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .semantics { contentDescription = exportDescription },
                ) {
                    Text(
                        text = stringResource(R.string.p_action_export),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            bottomBar = { EditorBottomBar(state = state, viewModel = viewModel) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val plan = state.plan
                    Text(
                        text = if (plan == null) "" else stringResource(
                            R.string.p_stage_size_fmt,
                            plan.outputWidth,
                            plan.outputHeight,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    ProjectSaveButton(
                        style = ProjectSaveStyle.TEXT_ACTION,
                        hasContent = state.stage == EditorStage.READY,
                        activeProjectTitle = state.projectTitle,
                        defaultTitle = stringResource(R.string.projects_default_title_photo),
                        onSave = { title -> viewModel.saveProject(context, title) },
                    )
                    TextButton(
                        onClick = {
                            if (state.hasEdits) {
                                pendingLeave = LeaveAction.NEW_PHOTO
                            } else {
                                onPickNewPhoto()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.p_choose_new))
                    }
                }
                if (state.recoveredDraft) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.draft_recovered),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.dismissDraftRecovered() }) {
                            Text(stringResource(R.string.action_ok))
                        }
                    }
                }
                PhotoStage(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
                if (state.aiBusy) {
                    AiBusyRow()
                } else if (state.aiHint) {
                    AiHintRow(onDismiss = { viewModel.dismissAiHint() })
                }
            }
        }
        if (state.exportDialogOpen) {
            ExportDialog(state = state, viewModel = viewModel)
        }
        val leave = pendingLeave
        if (leave != null) {
            AlertDialog(
                onDismissRequest = { pendingLeave = null },
                title = { Text(stringResource(R.string.p_discard_title)) },
                text = { Text(stringResource(R.string.p_discard_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingLeave = null
                            when (leave) {
                                LeaveAction.BACK -> onBack()
                                LeaveAction.NEW_PHOTO -> onPickNewPhoto()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.p_discard_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingLeave = null }) {
                        Text(stringResource(R.string.p_keep_editing))
                    }
                },
            )
        }
    }
}

@Composable
private fun AiBusyRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.p_ai_busy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AiHintRow(onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.p_ai_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.p_action_done))
            }
        }
    }
}
