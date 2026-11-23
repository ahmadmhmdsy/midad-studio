package com.midad.studio.studio.photo.editor.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

/** Freehand brush with an eraser that respects undo. */
@Composable
fun DrawPanel(
    state: PhotoEditorState,
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.p_draw_color))
        ColorSwatchRow(
            colors = PhotoPalette.brush,
            selected = state.brushColor,
            onSelect = { viewModel.setBrushColor(it) },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MidadChoiceChip(
                label = stringResource(R.string.p_draw_brush),
                selected = !state.erasing,
                onClick = { viewModel.setErasing(false) },
            )
            MidadChoiceChip(
                label = stringResource(R.string.p_draw_eraser),
                selected = state.erasing,
                onClick = { viewModel.setErasing(true) },
            )
            TextButton(
                onClick = { viewModel.clearStrokes() },
                enabled = state.strokes.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.p_draw_clear))
            }
        }
        SliderRow(
            label = stringResource(R.string.p_draw_width),
            valueText = brushWidthText(state.brushWidth),
            value = state.brushWidth,
            valueRange = 0.004f..0.14f,
            onValueChange = { viewModel.setBrushWidthLive(it) },
        )
        PanelHint(stringResource(R.string.p_draw_hint))
    }
}

private fun brushWidthText(width: Float): String {
    val step = ((width - 0.004f) / 0.136f * 100f).roundToInt().coerceIn(1, 100)
    return MidadNumbers.format(step.toLong())
}
