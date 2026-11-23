package com.midad.studio.studio.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.midad.studio.R
import com.midad.studio.studio.photo.editor.EditorStage
import com.midad.studio.studio.photo.editor.PhotoEditorScreen
import com.midad.studio.studio.photo.editor.PhotoEditorViewModel
import com.midad.studio.ui.components.EmptyState
import com.midad.studio.ui.components.StudioScaffold

/**
 * Photo studio entry point. Picks a picture with the system photo picker, then hands the
 * document to the editor; no storage permission is needed.
 */
@Composable
fun PhotoStudioScreen(projectId: String? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: PhotoEditorViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.load(context, uri)
    }
    val openPicker: () -> Unit = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    // An explicit back leaves the edit for good; a bound project is flushed before the draft is
    // dropped so the library copy keeps the newest edits. Only process death keeps the draft.
    val leave: () -> Unit = {
        viewModel.leaveStudio(context) { onBack() }
    }
    LaunchedEffect(Unit) {
        viewModel.checkAiAvailability(context)
        if (projectId == null) {
            viewModel.restoreDraftIfAny(context)
        } else {
            viewModel.openProject(context, projectId)
        }
    }

    when (state.stage) {
        EditorStage.EMPTY -> StudioScaffold(
            title = stringResource(R.string.studio_photo_title),
            onBack = leave,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyState(
                    title = stringResource(R.string.p_empty_title),
                    message = stringResource(R.string.p_empty_message),
                )
                Button(onClick = openPicker) {
                    Text(stringResource(R.string.p_pick_photo))
                }
                Text(
                    text = stringResource(R.string.p_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        EditorStage.LOADING -> StudioScaffold(
            title = stringResource(R.string.studio_photo_title),
            onBack = leave,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.p_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        EditorStage.ERROR -> StudioScaffold(
            title = stringResource(R.string.studio_photo_title),
            onBack = leave,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyState(
                    title = stringResource(R.string.p_error_title),
                    message = stringResource(R.string.p_error_message),
                )
                Button(onClick = openPicker) {
                    Text(stringResource(R.string.p_error_retry))
                }
            }
        }

        EditorStage.READY -> PhotoEditorScreen(
            state = state,
            viewModel = viewModel,
            onBack = leave,
            onPickNewPhoto = openPicker,
        )
    }
}
