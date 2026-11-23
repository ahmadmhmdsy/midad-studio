package com.midad.studio.studio.design.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.projects.ProjectSaveButton
import com.midad.studio.projects.ProjectSaveStyle
import com.midad.studio.studio.design.DesignViewModel
import com.midad.studio.studio.design.model.DesignSelection
import com.midad.studio.studio.design.model.LayerContent
import com.midad.studio.studio.design.ui.DesignGlyph
import com.midad.studio.studio.design.ui.DesignGlyphMark
import com.midad.studio.studio.design.ui.GlyphIconButton
import com.midad.studio.ui.components.StudioScaffold
import kotlinx.coroutines.launch

/** The working canvas: stage, tool rail, floating properties and every dialog it needs. */
@Composable
fun DesignEditorScreen(viewModel: DesignViewModel) {
    val document = viewModel.document ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmaps = viewModel.bitmapSnapshot()
    val previewImages = remember(bitmaps) {
        bitmaps.mapValues { entry -> entry.value.asImageBitmap() }
    }
    val selection = viewModel.selection
    val selectedLayerId = (selection as? DesignSelection.Layer)?.id

    var showTextDialog by remember { mutableStateOf(false) }
    var editingTextLayerId by remember { mutableStateOf<Long?>(null) }
    var textDialogInitial by remember { mutableStateOf("") }
    var showShapeSheet by remember { mutableStateOf(false) }
    var showLayerSheet by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showImageError by remember { mutableStateOf(false) }
    var imageLoading by remember { mutableStateOf(false) }
    var pendingReplaceLayerId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteLayerId by remember { mutableStateOf<Long?>(null) }

    fun requestExit() {
        if (viewModel.canUndo) showLeaveDialog = true else viewModel.backToStart()
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            pendingReplaceLayerId = null
        } else {
            imageLoading = true
            scope.launch {
                val bitmap = viewModel.decodeImage(context, uri, 2048)
                imageLoading = false
                if (bitmap == null) {
                    showImageError = true
                } else {
                    viewModel.attachImage(pendingReplaceLayerId, uri, bitmap)
                }
                pendingReplaceLayerId = null
            }
        }
    }

    fun launchImagePicker(replaceLayerId: Long?) {
        pendingReplaceLayerId = replaceLayerId
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val interaction = remember(viewModel) {
        DesignStageInteraction(
            onSelect = { layerId -> viewModel.selectLayer(layerId) },
            onBegin = { viewModel.checkpointStart() },
            onEnd = { viewModel.checkpointEnd() },
            onTransform = { centroid, pan, zoom, rotation, height ->
                val layerId = (viewModel.selection as? DesignSelection.Layer)?.id
                if (layerId != null) {
                    viewModel.transformLayer(
                        layerId = layerId,
                        centroidX = centroid.x,
                        centroidY = centroid.y,
                        panX = pan.x,
                        panY = pan.y,
                        zoom = zoom,
                        rotationDelta = rotation,
                        layerHeight = height,
                    )
                }
            },
        )
    }

    BackHandler { requestExit() }

    StudioScaffold(
        title = viewModel.activeProjectTitle ?: stringResource(R.string.d_editor_title),
        onBack = { requestExit() },
        actions = {
            DesignEditorActions(
                canUndo = viewModel.canUndo,
                canRedo = viewModel.canRedo,
                showGuides = viewModel.showGuides,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onToggleGuides = viewModel::toggleGuides,
                onExport = { scope.launch { viewModel.exportDocument(context) } },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    DesignStage(
                        document = document,
                        images = previewImages,
                        modifier = Modifier.fillMaxSize(),
                        selectedId = selectedLayerId,
                        showGuides = viewModel.showGuides,
                        interaction = interaction,
                    )
                    if (imageLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp,
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Text(
                                        text = stringResource(R.string.d_image_loading),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                ToolRail(
                    projectTitle = viewModel.activeProjectTitle,
                    onSaveProject = { title -> viewModel.saveProject(context, title) },
                    onAddText = {
                        editingTextLayerId = null
                        textDialogInitial = ""
                        showTextDialog = true
                    },
                    onAddImage = { launchImagePicker(null) },
                    onAddShape = { showShapeSheet = true },
                    onBackground = { viewModel.selectBackground() },
                    onLayers = { showLayerSheet = true },
                )
            }
            if (selection != DesignSelection.None) {
                DesignPropertyPanel(
                    viewModel = viewModel,
                    onEditText = { layerId ->
                        val layer = document.layer(layerId)
                        val content = layer?.content as? LayerContent.Text
                        editingTextLayerId = layerId
                        textDialogInitial = content?.text.orEmpty()
                        showTextDialog = true
                    },
                    onReplaceImage = { layerId -> launchImagePicker(layerId) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 88.dp),
                )
            }
        }
    }

    if (showTextDialog) {
        TextEditDialog(
            initial = textDialogInitial,
            onDismiss = { showTextDialog = false },
            onConfirm = { text ->
                showTextDialog = false
                val layerId = editingTextLayerId
                if (layerId == null) {
                    viewModel.addTextLayer(text)
                } else {
                    viewModel.setLayerText(layerId, text)
                }
            },
        )
    }

    if (showShapeSheet) {
        ShapePickerSheet(
            onDismiss = { showShapeSheet = false },
            onPick = { kind ->
                showShapeSheet = false
                viewModel.addShapeLayer(kind)
            },
        )
    }

    if (showLayerSheet) {
        LayerListSheet(
            document = document,
            selection = selection,
            onSelectLayer = { layerId ->
                viewModel.selectLayer(layerId)
                showLayerSheet = false
            },
            onSelectBackground = {
                viewModel.selectBackground()
                showLayerSheet = false
            },
            onMoveUp = { layerId -> viewModel.moveLayer(layerId, towardTop = true) },
            onMoveDown = { layerId -> viewModel.moveLayer(layerId, towardTop = false) },
            onDelete = { layerId -> pendingDeleteLayerId = layerId },
            onDismiss = { showLayerSheet = false },
        )
    }

    if (showLeaveDialog) {
        LeaveCanvasDialog(
            onStay = { showLeaveDialog = false },
            onLeave = {
                showLeaveDialog = false
                viewModel.backToStart()
            },
        )
    }

    val pendingDelete = pendingDeleteLayerId
    if (pendingDelete != null) {
        DeleteLayerDialog(
            onCancel = { pendingDeleteLayerId = null },
            onConfirm = {
                viewModel.deleteLayer(pendingDelete)
                pendingDeleteLayerId = null
            },
        )
    }

    if (showImageError) {
        ImageLoadErrorDialog(onDismiss = { showImageError = false })
    }
}

/** The editor's top-bar actions; extracted so the bar can be measured without a document. */
@Composable
internal fun DesignEditorActions(
    canUndo: Boolean,
    canRedo: Boolean,
    showGuides: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleGuides: () -> Unit,
    onExport: () -> Unit,
) {
    GlyphIconButton(
        glyph = DesignGlyph.UNDO,
        description = stringResource(R.string.d_action_undo),
        onClick = onUndo,
        enabled = canUndo,
    )
    GlyphIconButton(
        glyph = DesignGlyph.REDO,
        description = stringResource(R.string.d_action_redo),
        onClick = onRedo,
        enabled = canRedo,
    )
    GlyphIconButton(
        glyph = DesignGlyph.MARGINS,
        description = stringResource(R.string.d_action_margins),
        onClick = onToggleGuides,
        active = showGuides,
    )
    // Export is the one action that leaves the editor, so it is the primary button;
    // undo, redo and margins stay quiet glyphs beside it (same hierarchy as Photo).
    Button(
        onClick = onExport,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.padding(end = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.d_action_export),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun ToolRail(
    projectTitle: String?,
    onSaveProject: suspend (String) -> Boolean,
    onAddText: () -> Unit,
    onAddImage: () -> Unit,
    onAddShape: () -> Unit,
    onBackground: () -> Unit,
    onLayers: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(DesignGlyph.TEXT, R.string.d_tool_text, onAddText)
        ToolButton(DesignGlyph.IMAGE, R.string.d_tool_image, onAddImage)
        ToolButton(DesignGlyph.SHAPE, R.string.d_tool_shape, onAddShape)
        ToolButton(DesignGlyph.BACKGROUND, R.string.d_tool_background, onBackground)
        ToolButton(DesignGlyph.LAYERS, R.string.d_tool_layers, onLayers)
        ProjectSaveButton(
            style = ProjectSaveStyle.TOOL_RAIL,
            hasContent = true,
            activeProjectTitle = projectTitle,
            defaultTitle = stringResource(R.string.projects_default_title_design),
            onSave = onSaveProject,
        )
    }
}

@Composable
private fun ToolButton(glyph: DesignGlyph, labelRes: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DesignGlyphMark(
            glyph = glyph,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
