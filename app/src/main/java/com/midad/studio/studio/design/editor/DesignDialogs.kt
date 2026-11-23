package com.midad.studio.studio.design.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.studio.design.model.ShapeKind
import com.midad.studio.studio.design.ui.DesignGlyph
import com.midad.studio.studio.design.ui.DesignGlyphMark

@Composable
fun TextEditDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.d_text_edit_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.d_text_edit_hint)) },
                placeholder = { Text(stringResource(R.string.d_text_placeholder)) },
                minLines = 3,
                maxLines = 6,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapePickerSheet(
    onDismiss: () -> Unit,
    onPick: (ShapeKind) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.d_add_shape_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            ShapeOption(ShapeKind.RECTANGLE, DesignGlyph.RECTANGLE, R.string.d_shape_rectangle, onPick)
            ShapeOption(ShapeKind.ELLIPSE, DesignGlyph.ELLIPSE, R.string.d_shape_ellipse, onPick)
            ShapeOption(ShapeKind.LINE, DesignGlyph.LINE, R.string.d_shape_line, onPick)
            ShapeOption(ShapeKind.STAR, DesignGlyph.STAR, R.string.d_shape_star, onPick)
            ShapeOption(ShapeKind.RULE, DesignGlyph.RULE, R.string.d_shape_underline, onPick)
        }
    }
}

@Composable
private fun ShapeOption(
    kind: ShapeKind,
    glyph: DesignGlyph,
    labelRes: Int,
    onPick: (ShapeKind) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onPick(kind) }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesignGlyphMark(
            glyph = glyph,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun DeleteLayerDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.d_layer_delete_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.d_layer_delete_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun LeaveCanvasDialog(onStay: () -> Unit, onLeave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onStay,
        title = {
            Text(
                text = stringResource(R.string.d_leave_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.d_leave_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onLeave) {
                Text(stringResource(R.string.d_leave_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onStay) {
                Text(stringResource(R.string.d_leave_cancel))
            }
        },
    )
}

@Composable
fun ImageLoadErrorDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.d_tool_image)) },
        text = {
            Text(
                text = stringResource(R.string.d_image_error),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
    )
}
