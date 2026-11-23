package com.midad.studio.studio.photo.editor.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.studio.photo.editor.PhotoEditorState
import com.midad.studio.studio.photo.editor.PhotoEditorViewModel
import com.midad.studio.studio.photo.engine.CropAspect
import com.midad.studio.studio.photo.ui.FlipGlyph
import com.midad.studio.studio.photo.ui.PanelHint
import com.midad.studio.studio.photo.ui.RotateGlyph
import com.midad.studio.studio.photo.ui.SliderRow
import com.midad.studio.ui.components.MidadChoiceChip
import com.midad.studio.ui.components.SectionLabel
import kotlin.math.roundToInt

/** Aspect presets, rotation, flips and straighten. */
@Composable
fun CropPanel(
    state: PhotoEditorState,
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CropAspect.entries.forEach { aspect ->
                MidadChoiceChip(
                    label = stringResource(cropAspectLabel(aspect)),
                    selected = state.geometry.aspect == aspect,
                    onClick = { viewModel.selectCropAspect(aspect) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CropAction(
                label = stringResource(R.string.p_crop_rotate_left),
                onClick = { viewModel.rotate(clockwise = false) },
                modifier = Modifier.weight(1f),
            ) {
                RotateGlyph(clockwise = false, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            CropAction(
                label = stringResource(R.string.p_crop_rotate_right),
                onClick = { viewModel.rotate(clockwise = true) },
                modifier = Modifier.weight(1f),
            ) {
                RotateGlyph(clockwise = true, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            CropAction(
                label = stringResource(R.string.p_crop_flip_horizontal),
                onClick = { viewModel.flip(horizontal = true) },
                modifier = Modifier.weight(1f),
            ) {
                FlipGlyph(horizontal = true, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            CropAction(
                label = stringResource(R.string.p_crop_flip_vertical),
                onClick = { viewModel.flip(horizontal = false) },
                modifier = Modifier.weight(1f),
            ) {
                FlipGlyph(horizontal = false, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
        }
        SliderRow(
            label = stringResource(R.string.p_crop_straighten),
            valueText = straightenText(state.geometry.straighten),
            value = state.geometry.straighten,
            valueRange = -45f..45f,
            onValueChange = { viewModel.setStraightenLive(it) },
            onValueChangeFinished = { viewModel.commitLiveEdit() },
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PanelHint(stringResource(R.string.p_crop_hint), Modifier.weight(1f))
            TextButton(
                onClick = { viewModel.resetGeometry() },
                enabled = !state.geometry.isIdentity,
            ) {
                Text(stringResource(R.string.p_crop_reset))
            }
        }
    }
}

@Composable
private fun CropAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private fun cropAspectLabel(aspect: CropAspect): Int = when (aspect) {
    CropAspect.FREE -> R.string.p_crop_free
    CropAspect.SQUARE -> R.string.p_crop_square
    CropAspect.PORTRAIT_4_5 -> R.string.p_crop_4_5
    CropAspect.LANDSCAPE_16_9 -> R.string.p_crop_16_9
    CropAspect.PORTRAIT_9_16 -> R.string.p_crop_9_16
    CropAspect.LANDSCAPE_3_2 -> R.string.p_crop_3_2
}

private fun straightenText(value: Float): String {
    val rounded = (value * 10f).roundToInt() / 10f
    val sign = if (rounded > 0f) "+" else ""
    return sign + rounded + "\u00B0"
}
