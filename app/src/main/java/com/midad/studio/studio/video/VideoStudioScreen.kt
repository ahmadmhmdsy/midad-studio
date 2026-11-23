package com.midad.studio.studio.video

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.midad.studio.R
import com.midad.studio.projects.ProjectSaveButton
import com.midad.studio.projects.ProjectSaveStyle
import com.midad.studio.ui.components.StudioScaffold

/**
 * Video studio entry point: pick up to five clips, trim and order them, choose a look and frame,
 * add a title, preview the exact export composition and export a real mp4 through Media3
 * Transformer.
 */
@Composable
fun VideoStudioScreen(projectId: String? = null, onBack: () -> Unit) {
    val viewModel: VideoStudioViewModel = viewModel()
    val state = viewModel.state
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (projectId == null) {
            viewModel.restoreDraftIfAny(context)
        } else {
            viewModel.openProject(context, projectId)
        }
    }
    val pickVideos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_VIDEO_CLIPS),
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addClips(context, uris)
    }
    val launchPicker: () -> Unit = {
        pickVideos.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
        )
    }

    // Backing out of a project with clips silently discarded all of them. Both the toolbar
    // arrow and the system back gesture now ask first.
    var showLeave by remember { mutableStateOf(false) }
    var showFullPreview by remember { mutableStateOf(false) }
    // A bound project is flushed before the transient draft is cleared on an explicit exit.
    val leave: () -> Unit = {
        viewModel.leaveStudio(context) { onBack() }
    }
    val requestBack: () -> Unit = {
        // A bound project is flushed to the library on exit, so leaving is safe; ask only when the
        // timeline exists solely in this session and would be discarded.
        if (state.clips.isEmpty() || state.projectId != null) leave() else showLeave = true
    }
    BackHandler(enabled = state.clips.isNotEmpty() || showLeave) {
        if (showLeave) showLeave = false else requestBack()
    }

    StudioScaffold(
        title = state.projectTitle ?: stringResource(R.string.v_studio_title),
        onBack = requestBack,
        actions = {
            if (state.clips.isNotEmpty()) {
                ProjectSaveButton(
                    style = ProjectSaveStyle.TOOLBAR_ACTION,
                    hasContent = true,
                    activeProjectTitle = state.projectTitle,
                    defaultTitle = stringResource(R.string.projects_default_title_video),
                    onSave = { title -> viewModel.saveProject(context, title) },
                )
            }
        },
        bottomBar = {
            if (state.clips.isNotEmpty()) {
                VideoExportBar(
                    enabled = !state.busy,
                    totalDurationMs = state.totalDurationMs,
                    onPreview = { showFullPreview = true },
                    onExport = {
                        showFullPreview = false
                        viewModel.startExport(context)
                    },
                )
            }
        },
    ) { padding ->
        if (state.clips.isEmpty()) {
            VideoEmptyState(
                modifier = Modifier.padding(padding),
                loading = state.loading,
                notice = state.notice,
                onPick = launchPicker,
                onDismissNotice = viewModel::clearNotice,
            )
        } else {
            VideoProjectView(
                state = state,
                viewModel = viewModel,
                onPickMore = launchPicker,
                onPreview = { showFullPreview = true },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showFullPreview && state.clips.isNotEmpty()) {
        VideoFullPreviewDialog(
            spec = state.toExportSpec(),
            totalDurationMs = state.totalDurationMs,
            onDismiss = { showFullPreview = false },
        )
    }

    ExportDialogs(
        export = state.export,
        onCancel = viewModel::cancelExport,
        onDismiss = viewModel::dismissExportResult,
        onRetry = { viewModel.startExport(context) },
        onShare = { uri: android.net.Uri -> viewModel.shareResult(context, uri) },
        onOpen = { uri: android.net.Uri -> viewModel.openResult(context, uri) },
    )

    if (showLeave) {
        AlertDialog(
            onDismissRequest = { showLeave = false },
            title = { Text(stringResource(R.string.v_leave_title)) },
            text = { Text(stringResource(R.string.v_leave_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeave = false
                        leave()
                    },
                ) { Text(stringResource(R.string.v_leave_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showLeave = false }) {
                    Text(stringResource(R.string.v_leave_cancel))
                }
            },
        )
    }
}
