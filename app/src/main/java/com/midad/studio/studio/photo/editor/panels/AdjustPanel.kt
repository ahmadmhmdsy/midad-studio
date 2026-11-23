package com.midad.studio.studio.photo.editor.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.studio.photo.editor.AdjustGroup
import com.midad.studio.studio.photo.editor.AdjustKey
import com.midad.studio.studio.photo.editor.PhotoEditorState
import com.midad.studio.studio.photo.editor.PhotoEditorViewModel
import com.midad.studio.studio.photo.ui.SliderRow
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.text.MidadNumbers
import kotlin.math.roundToInt

/**
 * The twelve adjustment sliders in three labelled groups (light, colour, detail), each with
 * its own reset. One render pass per value change.
 */
@Composable
fun AdjustPanel(
    state: PhotoEditorState,
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(stringResource(R.string.p_adjust_header), Modifier.weight(1f))
            TextButton(
                onClick = { viewModel.resetAdjustments() },
                enabled = !state.adjustments.isNeutral,
            ) {
                Text(stringResource(R.string.p_action_reset_all))
            }
        }
        AdjustGroup.entries.forEach { group ->
            val groupName = stringResource(group.labelRes)
            val groupNeutral = group.keys.all { key -> key.read(state.adjustments) == 0f }
            val resetDescription = stringResource(R.string.p_adj_reset_group_fmt, groupName)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(groupName, Modifier.weight(1f))
                TextButton(
                    onClick = { viewModel.resetAdjustments(group.keys) },
                    enabled = !groupNeutral,
                    // Every group has a reset; without this TalkBack hears three "Reset" buttons.
                    modifier = Modifier.semantics { contentDescription = resetDescription },
                ) {
                    Text(stringResource(R.string.p_action_reset))
                }
            }
            group.keys.forEach { key ->
                val value = key.read(state.adjustments)
                SliderRow(
                    label = stringResource(key.labelRes),
                    valueText = adjustmentValueText(key, value),
                    value = value,
                    valueRange = key.min..key.max,
                    onValueChange = { viewModel.setAdjustmentLive(key, it) },
                    onValueChangeFinished = { viewModel.commitLiveEdit() },
                )
            }
        }
    }
}

private fun adjustmentValueText(key: AdjustKey, value: Float): String {
    if (key.isExposure) {
        val rounded = (value * 10f).roundToInt() / 10f
        val sign = if (rounded > 0f) "+" else ""
        return sign + MidadNumbers.formatDecimal(rounded)
    }
    val rounded = value.roundToInt()
    return if (key.min < 0f) {
        (if (rounded > 0) "+" else "") + MidadNumbers.format(rounded.toLong())
    } else {
        MidadNumbers.format(rounded.toLong())
    }
}
