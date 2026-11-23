package com.midad.studio.studio.photo.editor.panels

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.studio.photo.editor.PhotoEditorState
import com.midad.studio.studio.photo.editor.PhotoEditorViewModel
import com.midad.studio.studio.photo.engine.PhotoFilters
import com.midad.studio.studio.photo.ui.SliderRow
import com.midad.studio.studio.photo.ui.SwatchCell
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.text.MidadNumbers
import kotlin.math.roundToInt

/** The twelve preset looks plus an untouched original. */
@Composable
fun FiltersPanel(
    state: PhotoEditorState,
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.p_tool_filters))
        SliderRow(
            label = stringResource(R.string.p_filters_intensity),
            valueText = MidadNumbers.format(state.filterIntensity.toLong()),
            value = state.filterIntensity.toFloat(),
            valueRange = 0f..100f,
            enabled = state.filterIndex != null,
            onValueChange = { viewModel.setFilterIntensityLive(it.roundToInt()) },
            onValueChangeFinished = { viewModel.commitLiveEdit() },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwatchCell(
                label = stringResource(R.string.p_filters_original),
                swatches = listOf(0xFFE9E4DA.toInt(), 0xFFA9A29A.toInt(), 0xFF5A544C.toInt()),
                selected = state.filterIndex == null,
                onClick = { viewModel.selectFilter(null) },
            )
            PhotoFilters.all.forEachIndexed { index, filter ->
                SwatchCell(
                    label = stringResource(filter.nameRes),
                    swatches = filter.swatches,
                    selected = state.filterIndex == index,
                    onClick = { viewModel.selectFilter(index) },
                )
            }
        }
    }
}
