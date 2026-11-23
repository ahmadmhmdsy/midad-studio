package com.midad.studio.studio.design.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.midad.studio.ui.components.MidadChoiceChip
import com.midad.studio.ui.theme.colorNameRes
import com.midad.studio.ui.theme.hexColor

@Composable
fun GlyphIconButton(
    glyph: DesignGlyph,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = description
                if (active) selected = true
            },
        contentAlignment = Alignment.Center,
    ) {
        DesignGlyphMark(glyph = glyph, color = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ColorSwatchRow(
    colors: List<Long>,
    selected: Long,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(colors) { value ->
            val isSelected = value == selected
            val nameRes = colorNameRes(value)
            val description =
                if (nameRes != null) stringResource(nameRes) else hexColor(value)
            Box(
                modifier = Modifier
                    // 48 dp minimum target with a 30 dp visual circle.
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(selected = isSelected, role = Role.RadioButton) {
                        onPick(value)
                    }
                    .semantics { contentDescription = description }
                    .padding(9.dp)
                    .background(Color(value.toInt()))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    stateDescription = valueText
                },
            valueRange = range,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

@Composable
fun ChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(labels) { index, label ->
            MidadChoiceChip(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}