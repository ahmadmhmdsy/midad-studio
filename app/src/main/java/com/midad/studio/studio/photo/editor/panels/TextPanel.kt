package com.midad.studio.studio.photo.editor.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.studio.photo.editor.PhotoEditorState
import com.midad.studio.studio.photo.editor.PhotoEditorViewModel
import com.midad.studio.studio.photo.editor.PhotoPalette
import com.midad.studio.studio.photo.ui.ColorSwatchRow
import com.midad.studio.studio.photo.ui.PanelHint
import com.midad.studio.studio.photo.ui.SliderRow
import com.midad.studio.ui.components.MidadChoiceChip
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.text.MidadNumbers
import kotlin.math.roundToInt

/** Text layers: add, edit, colour, size, opacity and font family. */
@Composable
fun TextPanel(
    state: PhotoEditorState,
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val selected = state.selectedText
    val defaultText = stringResource(R.string.p_text_default)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = { viewModel.addText(defaultText) }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(stringResource(R.string.p_text_add))
            }
            if (selected != null) {
                TextButton(onClick = { draft = selected.text; editing = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.p_text_edit))
                }
                TextButton(onClick = { viewModel.deleteSelectedText() }) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
        if (selected != null) {
            SectionLabel(stringResource(R.string.p_text_color))
            ColorSwatchRow(
                colors = PhotoPalette.text,
                selected = selected.color,
                onSelect = { viewModel.setTextColor(it) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MidadChoiceChip(
                    label = stringResource(R.string.p_text_font_serif),
                    selected = selected.serif,
                    onClick = { if (!selected.serif) viewModel.toggleTextSerif() },
                )
                MidadChoiceChip(
                    label = stringResource(R.string.p_text_font_sans),
                    selected = !selected.serif,
                    onClick = { if (selected.serif) viewModel.toggleTextSerif() },
                )
            }
            SliderRow(
                label = stringResource(R.string.p_text_size),
                valueText = MidadNumbers.percent((selected.sizeFraction * 100f).roundToInt()),
                value = selected.sizeFraction,
                valueRange = 0.02f..0.5f,
                onValueChange = { viewModel.setTextSizeLive(it) },
                onValueChangeFinished = { viewModel.commitLiveEdit() },
            )
            SliderRow(
                label = stringResource(R.string.p_text_opacity),
                valueText = MidadNumbers.percent((selected.opacity * 100f).roundToInt()),
                value = selected.opacity,
                valueRange = 0.05f..1f,
                onValueChange = { viewModel.setTextOpacityLive(it) },
                onValueChangeFinished = { viewModel.commitLiveEdit() },
            )
            PanelHint(stringResource(R.string.p_text_hint))
        } else {
            PanelHint(stringResource(R.string.p_text_hint))
        }
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.p_text_edit_title)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.p_text_placeholder)) },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = draft.trim()
                    if (trimmed.isNotEmpty()) viewModel.setTextContent(trimmed)
                    editing = false
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
