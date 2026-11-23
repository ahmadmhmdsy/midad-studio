package com.midad.studio.projects

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import kotlinx.coroutines.launch

/** Where the save-project affordance is drawn: studio toolbars, a text row, or a tool rail. */
enum class ProjectSaveStyle { TOOLBAR_ACTION, TEXT_ACTION, TOOL_RAIL }

/**
 * The one save-project control used by all four studios.
 *
 * The first save asks for a name (prefilled with a localized studio default); later saves update the
 * same project without asking again. The suspend [onSave] is expected to flush the studio draft and
 * write it into the projects library, and returns whether that succeeded; a short localized Toast
 * reports the result either way.
 */
@Composable
fun ProjectSaveButton(
    style: ProjectSaveStyle,
    hasContent: Boolean,
    activeProjectTitle: String?,
    defaultTitle: String,
    onSave: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    if (!hasContent) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.projects_save_action)
    var showDialog by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val persist: (String) -> Unit = { title ->
        if (!saving) {
            saving = true
            scope.launch {
                val saved = runCatching { onSave(title) }.getOrDefault(false)
                saving = false
                val message = if (saved) R.string.projects_saved else R.string.projects_save_failed
                Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val requestSave: () -> Unit = {
        if (!saving) {
            val title = activeProjectTitle
            if (title != null) persist(title) else showDialog = true
        }
    }

    when (style) {
        ProjectSaveStyle.TOOLBAR_ACTION -> IconButton(
            onClick = requestSave,
            enabled = !saving,
            modifier = modifier.semantics { contentDescription = label },
        ) {
            SaveProjectGlyph(MaterialTheme.colorScheme.onSurface)
        }

        ProjectSaveStyle.TEXT_ACTION -> TextButton(
            onClick = requestSave,
            enabled = !saving,
            modifier = modifier,
        ) {
            Text(label)
        }

        ProjectSaveStyle.TOOL_RAIL -> Column(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = requestSave)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .semantics { contentDescription = label },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SaveProjectGlyph(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.projects_save_tool),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        ProjectTitleDialog(
            title = stringResource(R.string.projects_save_dialog_title),
            body = stringResource(R.string.projects_save_dialog_body),
            initialTitle = defaultTitle,
            confirmLabel = stringResource(R.string.projects_save_dialog_confirm),
            onDismiss = { showDialog = false },
            onConfirm = { title ->
                showDialog = false
                persist(title)
            },
        )
    }
}

/** Name/rename dialog shared by the save action and the Projects library. */
@Composable
fun ProjectTitleDialog(
    title: String,
    initialTitle: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    body: String? = null,
) {
    var text by remember(initialTitle) { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (body != null) {
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { value -> text = value.take(80) },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.projects_save_dialog_title)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim().ifBlank { initialTitle }) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun SaveProjectGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val width = size.width
        val height = size.height
        val path = Path().apply {
            moveTo(width * 0.26f, height * 0.16f)
            lineTo(width * 0.74f, height * 0.16f)
            lineTo(width * 0.74f, height * 0.84f)
            lineTo(width * 0.50f, height * 0.66f)
            lineTo(width * 0.26f, height * 0.84f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = size.minDimension * 0.09f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
