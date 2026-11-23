package com.midad.studio.studio.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Entry point of the AI Lab studio. Model state and inference are shared app-wide through
 * [AiBootstrap]; this screen only owns its transient UI state.
 */
@Composable
fun AiStudioScreen(projectId: String? = null, onBack: () -> Unit) {
    val viewModel: AiViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // A bound project is flushed before the transient draft is cleared on an explicit exit.
    val leave: () -> Unit = {
        viewModel.leaveStudio(onExit = onBack)
    }
    LaunchedEffect(Unit) {
        if (projectId != null) viewModel.openProject(projectId) else viewModel.restoreDraft()
    }

    when (state.page) {
        AiPage.HOME -> AiHomeContent(
            state = state,
            onBack = leave,
            onOpenCutout = viewModel::openCutout,
            onOpenModels = viewModel::openModels,
            onRefresh = viewModel::refreshModels,
            onDismissError = viewModel::dismissError,
        )

        AiPage.CUTOUT -> BackgroundRemovalContent(
            state = state,
            onBack = viewModel::openHome,
            onPickImage = viewModel::pickImage,
            onRetry = viewModel::retryCutout,
            onStartOver = viewModel::startOver,
            onSelectBackdrop = viewModel::selectBackdrop,
            onSave = viewModel::save,
            onShare = viewModel::shareSaved,
            onOpenSaved = viewModel::openSaved,
            onDismissSaved = viewModel::dismissSavedDialog,
            onSelectModel = viewModel::selectModel,
            onOpenModels = viewModel::openModels,
            onDismissError = viewModel::dismissError,
            onSaveProject = { title -> viewModel.saveProject(title) },
        )

        AiPage.MODELS -> ModelManagerContent(
            state = state,
            onBack = viewModel::leaveModels,
            onRefresh = viewModel::refreshModels,
            onDownload = viewModel::downloadModel,
            onCancel = viewModel::cancelModel,
            onRemove = viewModel::removeModel,
            onTry = viewModel::tryModel,
            onOpenLink = viewModel::openLink,
            onDismissError = viewModel::dismissError,
        )
    }
}
