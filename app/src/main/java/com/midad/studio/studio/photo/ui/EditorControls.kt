package com.midad.studio.studio.photo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.midad.studio.ui.theme.colorNameRes
import com.midad.studio.ui.theme.hexColor

/** Label + value line above a slider; used by every adjustment panel. */
@Composable
fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    stateDescription = valueText
                },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            ),
        )
    }
}

/** Circular colour swatches with a ring on the current choice. */
@Composable
fun ColorSwatchRow(
    colors: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        colors.forEach { argb ->
            val isSelected = argb == selected
            val shape = CircleShape
            // The photo palette stores ARGB as Int, whose sign extension would turn a
            // 0xFFxxxxxx colour into 0xFFFFFFFFFFxxxxxx; widen without the sign.
            val value = argb.toLong() and 0xFFFFFFFFL
            val nameRes = colorNameRes(value)
            val description = if (nameRes != null) stringResource(nameRes) else hexColor(value)
            Box(
                modifier = Modifier
                    // 48 dp minimum target. The extra 14 dp is transparent padding around
                    // the 34 dp circle, so the palette does not grow on screen.
                    .size(48.dp)
                    .clip(shape)
                    .selectable(selected = isSelected, role = Role.RadioButton) {
                        onSelect(argb)
                    }
                    .semantics { contentDescription = description }
                    .padding(7.dp)
                    .background(Color(argb))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        shape = shape,
                    ),
            )
        }
    }
}

/** Compact choice cell used for filter presets: three colour bars and a caption. */
@Composable
fun SwatchCell(
    label: String,
    swatches: List<Int>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 66.dp, height = 48.dp)
                .clip(shape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = shape,
                ),
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                swatches.forEach { argb ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(Color(argb)),
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Small muted paragraph used by panel footers. */
@Composable
fun PanelHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}