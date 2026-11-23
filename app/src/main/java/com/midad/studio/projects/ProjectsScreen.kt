package com.midad.studio.projects

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.midad.studio.R
import com.midad.studio.core.draft.AiDraftStore
import com.midad.studio.core.draft.DesignDraftStore
import com.midad.studio.core.draft.PhotoDraftStore
import com.midad.studio.core.draft.VideoDraftStore
import com.midad.studio.core.export.ExportManager
import com.midad.studio.core.format.formatShortDate
import com.midad.studio.core.project.ExportRecord
import com.midad.studio.core.project.ProjectKind
import com.midad.studio.core.project.ProjectRecord
import com.midad.studio.core.project.ProjectRepository
import com.midad.studio.core.project.ProjectStore
import com.midad.studio.navigation.StudioKind
import com.midad.studio.ui.components.EmptyState
import com.midad.studio.ui.components.StudioScaffold
import kotlinx.coroutines.launch

/**
 * The library behind every studio: a Projects tab (saved, reopenable edits) and the Exports tab
 * (finished files that can be shared or deleted). Opening a project copies its payload into the
 * studio's own draft folder, then navigates to that studio with the project id.
 */
@Composable
fun ProjectsScreen(
    onBack: () -> Unit,
    onNewProject: (StudioKind) -> Unit,
    onOpenProject: (StudioKind, String) -> Unit,
    initialTab: Int = 0,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val projects by ProjectRepository.projects.collectAsStateWithLifecycle()
    val exports by ProjectStore.records.collectAsStateWithLifecycle()
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var showNewProject by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<ProjectRecord?>(null) }
    var renameTarget by remember { mutableStateOf<ProjectRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<ProjectRecord?>(null) }
    var openingId by remember { mutableStateOf<String?>(null) }
    var exportsDeleteTarget by remember { mutableStateOf<ExportRecord?>(null) }
    var draftWarningTarget by remember { mutableStateOf<ProjectRecord?>(null) }

    LaunchedEffect(Unit) {
        ProjectStore.refresh(context)
        ProjectRepository.refresh(context)
    }

    val performOpen: (ProjectRecord) -> Unit = { record ->
        if (openingId == null) {
            openingId = record.id
            scope.launch {
                val opened = openProjectPayload(context, record)
                openingId = null
                if (opened) {
                    onOpenProject(record.kind.toStudioKind(), record.id)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.projects_open_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }
    // Opening a project copies its payload into the studio's draft folder. If an unfinished
    // edit is still recoverable, ask first instead of silently replacing it.
    val openProject: (ProjectRecord) -> Unit = { record ->
        if (openingId == null) {
            if (hasRecoverableDraft(context)) draftWarningTarget = record else performOpen(record)
        }
    }

    StudioScaffold(
        title = stringResource(R.string.projects_library_title),
        onBack = onBack,
        actions = {
            if (tab == 0) {
                TextButton(onClick = { showNewProject = true }) {
                    Text(stringResource(R.string.projects_new_action))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.projects_library_title)) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.projects_title)) },
                )
            }
            if (tab == 0) {
                ProjectsTab(
                    projects = projects,
                    openingId = openingId,
                    onNewProject = { showNewProject = true },
                    onOpen = openProject,
                    onLongPress = { record -> actionTarget = record },
                )
            } else {
                ExportsTab(
                    records = exports,
                    onOpen = { record ->
                        ExportManager.share(
                            context,
                            Uri.parse(record.uri),
                            ExportManager.mimeTypeFor(record.fileName, record.kind),
                        )
                    },
                    onLongPress = { record -> exportsDeleteTarget = record },
                )
            }
        }
    }

    if (showNewProject) {
        NewProjectDialog(
            onDismiss = { showNewProject = false },
            onPick = { kind ->
                showNewProject = false
                onNewProject(kind)
            },
        )
    }

    val draftWarning = draftWarningTarget
    if (draftWarning != null) {
        AlertDialog(
            onDismissRequest = { draftWarningTarget = null },
            title = { Text(stringResource(R.string.projects_draft_warning_title)) },
            text = { Text(stringResource(R.string.projects_draft_warning_body)) },
            confirmButton = {
                TextButton(onClick = {
                    draftWarningTarget = null
                    performOpen(draftWarning)
                }) {
                    Text(stringResource(R.string.projects_draft_warning_open))
                }
            },
            dismissButton = {
                TextButton(onClick = { draftWarningTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val action = actionTarget
    if (action != null) {
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text(action.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            actionTarget = null
                            openProject(action)
                        },
                    ) {
                        Text(stringResource(R.string.projects_open_action))
                    }
                    TextButton(
                        onClick = {
                            actionTarget = null
                            renameTarget = action
                        },
                    ) {
                        Text(stringResource(R.string.projects_rename_action))
                    }
                    TextButton(
                        onClick = {
                            actionTarget = null
                            deleteTarget = action
                        },
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val renaming = renameTarget
    if (renaming != null) {
        ProjectTitleDialog(
            title = stringResource(R.string.projects_rename_title),
            initialTitle = renaming.title,
            confirmLabel = stringResource(R.string.projects_rename_confirm),
            onDismiss = { renameTarget = null },
            onConfirm = { title ->
                renameTarget = null
                scope.launch {
                    val renamed = ProjectRepository.rename(context, renaming.id, title)
                    if (!renamed) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.projects_save_failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
        )
    }

    val deleting = deleteTarget
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.projects_delete_project_title)) },
            text = { Text(stringResource(R.string.projects_delete_project_body)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        val deleted = ProjectRepository.delete(context, deleting.id)
                        if (!deleted) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.projects_save_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val exportTarget = exportsDeleteTarget
    if (exportTarget != null) {
        AlertDialog(
            onDismissRequest = { exportsDeleteTarget = null },
            title = { Text(stringResource(R.string.projects_delete_title)) },
            text = { Text(stringResource(R.string.projects_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    exportsDeleteTarget = null
                    scope.launch { ProjectStore.remove(context, exportTarget.id) }
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { exportsDeleteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Copies the payload into the right studio draft folder; returns true when the studio can open it. */
/** True when a studio still has a crash-recovery draft that opening a project would replace. */
private fun hasRecoverableDraft(context: android.content.Context): Boolean =
    PhotoDraftStore(context).hasDraft() ||
        DesignDraftStore(context).hasDraft() ||
        AiDraftStore(context).hasDraft() ||
        VideoDraftStore(context).hasDraft()

private suspend fun openProjectPayload(
    context: android.content.Context,
    record: ProjectRecord,
): Boolean {
    if (!ProjectRepository.hasValidPayload(context, record)) return false
    return when (record.kind) {
        ProjectKind.PHOTO -> ProjectRepository.restoreIntoDraftDir(
            context,
            record.id,
            PhotoDraftStore.rootDirectory(context),
        )
        ProjectKind.DESIGN -> ProjectRepository.restoreIntoDraftDir(
            context,
            record.id,
            DesignDraftStore.rootDirectory(context),
        )
        ProjectKind.AI -> ProjectRepository.restoreIntoDraftDir(
            context,
            record.id,
            AiDraftStore.rootDirectory(context),
        )
        ProjectKind.VIDEO -> true
    }
}

private fun ProjectKind.toStudioKind(): StudioKind = when (this) {
    ProjectKind.PHOTO -> StudioKind.PHOTO
    ProjectKind.DESIGN -> StudioKind.DESIGN
    ProjectKind.AI -> StudioKind.AI
    ProjectKind.VIDEO -> StudioKind.VIDEO
}

@Composable
private fun ProjectsTab(
    projects: List<ProjectRecord>,
    openingId: String?,
    onNewProject: () -> Unit,
    onOpen: (ProjectRecord) -> Unit,
    onLongPress: (ProjectRecord) -> Unit,
) {
    if (projects.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                EmptyState(
                    title = stringResource(R.string.projects_library_empty_title),
                    message = stringResource(R.string.projects_library_empty_body),
                )
                Button(onClick = onNewProject) {
                    Text(stringResource(R.string.projects_new_action))
                }
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(projects, key = { it.id }) { record ->
            ProjectCard(
                record = record,
                opening = openingId == record.id,
                onClick = { onOpen(record) },
                onLongClick = { onLongPress(record) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectCard(
    record: ProjectRecord,
    opening: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var bitmap by remember(record.id, record.hasThumbnail, record.updatedAt) {
        mutableStateOf<Bitmap?>(null)
    }
    val context = LocalContext.current
    LaunchedEffect(record.id, record.updatedAt) {
        bitmap = ProjectRepository.thumbnail(context, record.id)
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClickLabel = stringResource(R.string.projects_open_action),
                onLongClickLabel = stringResource(R.string.projects_more_actions),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            val current = bitmap
            if (current != null) {
                Image(
                    bitmap = current.asImageBitmap(),
                    contentDescription = record.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (opening) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp),
                    strokeWidth = 2.dp,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(record.kind.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.surface,
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.projects_updated_fmt,
                    formatShortDate(record.updatedAt),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExportsTab(
    records: List<ExportRecord>,
    onOpen: (ExportRecord) -> Unit,
    onLongPress: (ExportRecord) -> Unit,
) {
    if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                title = stringResource(R.string.projects_empty_title),
                message = stringResource(R.string.projects_empty),
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(records, key = { it.id }) { record ->
            ExportCell(
                record = record,
                onClick = { onOpen(record) },
                onLongClick = { onLongPress(record) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExportCell(
    record: ExportRecord,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var bitmap by remember(record.id) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(record.id) {
        bitmap = ProjectStore.thumbnail(record)
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClickLabel = stringResource(R.string.action_share),
                onLongClickLabel = stringResource(R.string.action_delete),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = record.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = if (record.kind == ProjectStore.KIND_VIDEO) {
                    stringResource(R.string.projects_kind_video)
                } else {
                    stringResource(R.string.projects_kind_photo)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onPick: (StudioKind) -> Unit,
) {
    val studios = listOf(
        StudioKind.PHOTO to R.string.projects_choose_photo,
        StudioKind.DESIGN to R.string.projects_choose_design,
        StudioKind.AI to R.string.projects_choose_ai,
        StudioKind.VIDEO to R.string.projects_choose_video,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.projects_new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.projects_new_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                for ((kind, labelRes) in studios) {
                    TextButton(
                        onClick = { onPick(kind) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
