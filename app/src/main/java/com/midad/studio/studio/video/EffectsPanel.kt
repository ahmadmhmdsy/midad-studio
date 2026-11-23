package com.midad.studio.studio.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.ui.components.BorderedPanel
import com.midad.studio.ui.components.MidadChoiceChip

@Composable
internal fun LookRow(selected: VideoLook, onSelect: (VideoLook) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VideoLook.entries.forEach { look ->
            MidadChoiceChip(
                label = videoLookLabel(look),
                selected = selected == look,
                onClick = { onSelect(look) },
            )
        }
    }
}

@Composable
internal fun FitRow(selected: OutputFit, onSelect: (OutputFit) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutputFit.entries.forEach { fit ->
            MidadChoiceChip(
                label = fitLabel(fit),
                selected = selected == fit,
                onClick = { onSelect(fit) },
            )
        }
    }
}

@Composable
internal fun TrimPanel(clip: VideoClip, onTrim: (Long, Long) -> Unit) {
    val duration = clip.durationMs.coerceAtLeast(1L)
    BorderedPanel {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            RangeSlider(
                value = clip.trimStartMs.toFloat()..clip.trimEndMs.toFloat(),
                onValueChange = { range -> onTrim(range.start.toLong(), range.endInclusive.toLong()) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(
                    R.string.v_trim_range_fmt,
                    formatClipDuration(clip.trimStartMs),
                    formatClipDuration(clip.trimEndMs),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(
                    R.string.v_trim_length_fmt,
                    formatClipDuration(clip.trimmedDurationMs),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun TitleEditor(
    title: VideoTitle,
    onText: (String) -> Unit,
    onPosition: (TitlePosition) -> Unit,
    onSize: (TitleSize) -> Unit,
    onColor: (TitleColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    BorderedPanel(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = title.text,
                onValueChange = onText,
                label = { Text(stringResource(R.string.v_title_field)) },
                placeholder = { Text(stringResource(R.string.v_title_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
            )
            Text(
                text = stringResource(R.string.v_title_position),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TitlePosition.entries.forEach { position ->
                    MidadChoiceChip(
                        label = titlePositionLabel(position),
                        selected = title.position == position,
                        onClick = { onPosition(position) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.v_title_color),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TitleColor.entries.forEach { color ->
                    ColorSwatch(
                        color = color,
                        selected = title.color == color,
                        onClick = { onColor(color) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.v_title_size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TitleSize.entries.forEach { size ->
                    MidadChoiceChip(
                        label = titleSizeLabel(size),
                        selected = title.size == size,
                        onClick = { onSize(size) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: TitleColor, selected: Boolean, onClick: () -> Unit) {
    val label = titleColorLabel(color)
    val shape = CircleShape
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .background(Color(color.argb))
            .border(if (selected) 3.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
    )
}

@Composable
internal fun videoLookLabel(look: VideoLook): String = stringResource(
    when (look) {
        VideoLook.NONE -> R.string.v_look_none
        VideoLook.WARM -> R.string.v_look_warm
        VideoLook.COOL -> R.string.v_look_cool
        VideoLook.NOIR -> R.string.v_look_noir
        VideoLook.PUNCH -> R.string.v_look_punch
    },
)

@Composable
internal fun fitLabel(fit: OutputFit): String = stringResource(
    when (fit) {
        OutputFit.FIT -> R.string.v_fit_fit
        OutputFit.FILL -> R.string.v_fit_fill
        OutputFit.RATIO_9_16 -> R.string.v_fit_9_16
        OutputFit.RATIO_1_1 -> R.string.v_fit_1_1
        OutputFit.RATIO_16_9 -> R.string.v_fit_16_9
    },
)

@Composable
internal fun titlePositionLabel(position: TitlePosition): String = stringResource(
    when (position) {
        TitlePosition.TOP -> R.string.v_title_position_top
        TitlePosition.CENTER -> R.string.v_title_position_center
        TitlePosition.BOTTOM -> R.string.v_title_position_bottom
    },
)

@Composable
internal fun titleSizeLabel(size: TitleSize): String = stringResource(
    when (size) {
        TitleSize.SMALL -> R.string.v_title_size_small
        TitleSize.MEDIUM -> R.string.v_title_size_medium
        TitleSize.LARGE -> R.string.v_title_size_large
    },
)

@Composable
internal fun titleColorLabel(color: TitleColor): String = stringResource(
    when (color) {
        TitleColor.PAPER -> R.string.v_title_color_paper
        TitleColor.INK -> R.string.v_title_color_ink
        TitleColor.SAFFRON -> R.string.v_title_color_saffron
        TitleColor.TERRACOTTA -> R.string.v_title_color_terracotta
    },
)
