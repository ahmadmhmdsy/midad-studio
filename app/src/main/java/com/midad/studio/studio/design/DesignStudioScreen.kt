package com.midad.studio.studio.design

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.midad.studio.core.export.ExportManager
import com.midad.studio.studio.design.collage.CollageScreen
import com.midad.studio.studio.design.model.DesignTemplates
import com.midad.studio.studio.design.editor.DesignEditorScreen
import com.midad.studio.studio.design.export.ExportDialog
import com.midad.studio.studio.design.start.DesignStartScreen
import com.midad.studio.studio.design.start.TemplateGalleryScreen
import kotlinx.coroutines.launch

/** Entry point of the design studio: one view model, four screens, one export dialog. */
@Composable
fun DesignStudioScreen(projectId: String? = null, onBack: () -> Unit) {
    val viewModel: DesignViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val templates = remember(context) { DesignTemplates.all(context) }

    // A bound project is flushed before the transient draft is cleared on an explicit exit.
    val leave: () -> Unit = {
        viewModel.leaveStudio(context) { onBack() }
    }

    LaunchedEffect(templates) {
        viewModel.updateTemplates(templates)
    }
    LaunchedEffect(Unit) {
        if (projectId == null) {
            viewModel.restoreDraftIfAny(context)
        } else {
            viewModel.openProject(context, projectId)
        }
    }

    BackHandler(enabled = viewModel.screen == DesignScreen.START) {
        leave()
    }

    when (viewModel.screen) {
        DesignScreen.START -> DesignStartScreen(
            onBack = leave,
            templates = templates,
            onPreset = viewModel::openNewDocument,
            onOpenTemplates = viewModel::openTemplates,
            onOpenCollage = viewModel::openCollage,
            onOpenTemplate = viewModel::openTemplate,
        )
        DesignScreen.TEMPLATES -> TemplateGalleryScreen(
            onBack = viewModel::backToStart,
            templates = templates,
            onOpenTemplate = viewModel::openTemplate,
        )
        DesignScreen.EDITOR -> DesignEditorScreen(viewModel = viewModel)
        DesignScreen.COLLAGE -> CollageScreen(viewModel = viewModel)
    }

    ExportDialog(
        state = viewModel.exportState,
        onDismiss = viewModel::dismissExport,
        onRetry = { scope.launch { viewModel.retryExport(context) } },
        onShare = { success ->
            val mimeType = if (success.png) "image/png" else "image/jpeg"
            ExportManager.share(context, success.result.uri, mimeType)
        },
        onOpen = { success ->
            val mimeType = if (success.png) "image/png" else "image/jpeg"
            ExportManager.open(context, success.result.uri, mimeType)
        },
    )
}
