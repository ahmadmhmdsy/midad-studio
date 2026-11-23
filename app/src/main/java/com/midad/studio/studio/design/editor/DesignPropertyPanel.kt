package com.midad.studio.studio.design.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.studio.design.DesignViewModel
import com.midad.studio.studio.design.model.DesignBackground
import com.midad.studio.studio.design.model.DesignDocument
import com.midad.studio.studio.design.model.DesignPalette
import com.midad.studio.studio.design.model.DesignSelection
import com.midad.studio.studio.design.model.GradientDirection
import com.midad.studio.studio.design.model.LayerContent
import com.midad.studio.studio.design.model.PatternKind
import com.midad.studio.studio.design.model.TextAlignChoice
import com.midad.studio.studio.design.model.TextFont
import com.midad.studio.studio.design.ui.ChipRow
import com.midad.studio.studio.design.ui.ColorSwatchRow
import com.midad.studio.studio.design.ui.DesignGlyph
import com.midad.studio.studio.design.ui.GlyphIconButton
import com.midad.studio.studio.design.ui.LabeledSlider
import com.midad.studio.ui.components.MidadChoiceChip
import com.midad.studio.ui.text.MidadNumbers
import kotlin.math.roundToInt

/** Floating panel whose controls follow whatever is selected on the stage. */
@Composable
fun DesignPropertyPanel(
    viewModel: DesignViewModel,
    onEditText: (Long) -> Unit,
    onReplaceImage: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val document = viewModel.document ?: return
    val selection = viewModel.selection
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 348.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = panelTitle(document, selection),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.weight(1f))
                GlyphIconButton(
                    glyph = DesignGlyph.CLOSE,
                    description = stringResource(R.string.d_action_close_panel),
                    onClick = viewModel::clearSelection,
                )
            }
            when (selection) {
                DesignSelection.None -> Unit
                DesignSelection.Background -> BackgroundProperties(viewModel, document.background)
                is DesignSelection.Layer -> {
                    val layer = document.layer(selection.id)
                    if (layer != null) {
                        when (val content = layer.content) {
                            is LayerContent.Text -> TextProperties(
                                viewModel = viewModel,
                                layerId = layer.id,
                                content = content,
                                opacity = layer.opacity,
                                canvasWidth = document.width,
                                onEditText = onEditText,
                            )
                            is LayerContent.Shape -> ShapeProperties(
                                viewModel = viewModel,
                                layerId = layer.id,
                                content = content,
                                opacity = layer.opacity,
                                layerWidth = layer.width,
                            )
                            is LayerContent.Image -> ImageProperties(
                                viewModel = viewModel,
                                layerId = layer.id,
                                content = content,
                                opacity = layer.opacity,
                                layerWidth = layer.width,
                                onReplaceImage = onReplaceImage,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextProperties(
    viewModel: DesignViewModel,
    layerId: Long,
    content: LayerContent.Text,
    opacity: Float,
    canvasWidth: Int,
    onEditText: (Long) -> Unit,
) {
    val fontLabels = listOf(
        stringResource(R.string.d_font_serif),
        stringResource(R.string.d_font_sans),
    )
    val alignLabels = listOf(
        stringResource(R.string.d_align_start),
        stringResource(R.string.d_align_center),
        stringResource(R.string.d_align_end),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { onEditText(layerId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.d_text_edit))
        }
        FieldLabel(R.string.d_text_font)
        ChipRow(
            labels = fontLabels,
            selectedIndex = if (content.font == TextFont.SERIF) 0 else 1,
            onSelect = { index ->
                viewModel.updateText(layerId) { text ->
                    text.copy(font = if (index == 0) TextFont.SERIF else TextFont.SANS)
                }
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            MidadChoiceChip(
                label = stringResource(R.string.d_text_bold),
                selected = content.bold,
                onClick = {
                    viewModel.updateText(layerId) { text -> text.copy(bold = !text.bold) }
                },
            )
        }
        LabeledSlider(
            label = stringResource(R.string.d_text_size),
            valueText = MidadNumbers.format(content.size.roundToInt().toLong()),
            value = content.size,
            range = 8f..(canvasWidth * 0.45f),
            onValueChange = { value ->
                viewModel.checkpointStart()
                viewModel.updateText(layerId) { text -> text.copy(size = value) }
            },
            onValueChangeFinished = viewModel::checkpointEnd,
        )
        FieldLabel(R.string.d_text_color)
        ColorSwatchRow(
            colors = DesignPalette.swatches,
            selected = content.color,
            onPick = { color ->
                viewModel.updateText(layerId) { text -> text.copy(color = color) }
            },
        )
        FieldLabel(R.string.d_text_align)
        ChipRow(
            labels = alignLabels,
            selectedIndex = when (content.align) {
                TextAlignChoice.START -> 0
                TextAlignChoice.CENTER -> 1
                TextAlignChoice.END -> 2
            },
            onSelect = { index ->
                viewModel.updateText(layerId) { text ->
                    text.copy(
                        align = when (index) {
                            0 -> TextAlignChoice.START
                            1 -> TextAlignChoice.CENTER
                            else -> TextAlignChoice.END
                        },
                    )
                }
            },
        )
        LabeledSlider(
            label = stringResource(R.string.d_opacity),
            valueText = percentageText(opacity),
            value = opacity,
            range = 0.05f..1f,
            onValueChange = { value ->
                viewModel.checkpointStart()
                viewModel.updateLayer(layerId) { layer -> layer.copy(opacity = value) }
            },
            onValueChangeFinished = viewModel::checkpointEnd,
        )
    }
}

@Composable
private fun ShapeProperties(
    viewModel: DesignViewModel,
    layerId: Long,
    content: LayerContent.Shape,
    opacity: Float,
    layerWidth: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FieldLabel(R.string.d_shape_style)
        ChipRow(
            labels = listOf(
                stringResource(R.string.d_shape_fill),
                stringResource(R.string.d_shape_outline),
            ),
            selectedIndex = if (content.filled) 0 else 1,
            onSelect = { index ->
                viewModel.updateShape(layerId) { shape -> shape.copy(filled = index == 0) }
            },
        )
        FieldLabel(R.string.d_bg_color)
        ColorSwatchRow(
            colors = DesignPalette.swatches,
            selected = content.color,
            onPick = { color ->
                viewModel.updateShape(layerId) { shape -> shape.copy(color = color) }
            },
        )
        LabeledSlider(
            label = stringResource(R.string.d_stroke_width),
            valueText = MidadNumbers.format(content.strokeWidth.roundToInt().toLong()),
            value = content.strokeWidth,
            range = 1f..(layerWidth * 0.14f).coerceAtLeast(10f),
            onValueChange = { value ->
                viewModel.checkpointStart()
                viewModel.updateShape(layerId) { shape -> shape.copy(strokeWidth = value) }
            },
            onValueChangeFinished = viewModel::checkpointEnd,
        )
        LabeledSlider(
            label = stringResource(R.string.d_opacity),
            valueText = percentageText(opacity),
            value = opacity,
            range = 0.05f..1f,
            onValueChange = { value ->
                viewModel.checkpointStart()
                viewModel.updateLayer(layerId) { layer -> layer.copy(opacity = value) }
            },
            onValueChangeFinished = viewModel::checkpointEnd,
        )
    }
}

@Composable
private fun ImageProperties(
    viewModel: DesignViewModel,
    layerId: Long,
    content: LayerContent.Image,
    opacity: Float,
    layerWidth: Float,
    onReplaceImage: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FieldLabel(R.string.d_image_fit)
        ChipRow(
            labels = listOf(
                stringResource(R.string.d_fit_contain),
                stringResource(R.string.d_fit_cover),
                stringResource(R.string.d_fit_stretch),
            ),
            selectedIndex = content.fit.ordinal,
            onSelect = { index ->
                viewModel.updateImage(layerId) { image ->
                    image.copy(
                        fit = when (index) {
                            0 -> com.midad.studio.studio.design.model.ImageFit.CONTAIN
                            1 -> com.midad.studio.studio.design.model.ImageFit.COVER
                            else -> com.midad.studio.studio.design.model.ImageFit.STRETCH
                        },
                    )
                }
            },
        )
        LabeledSlider(
            label = stringResource(R.string.d_image_corners),
            valueText = MidadNumbers.format(content.cornerRadius.roundToInt().toLong()),
            value = content.cornerRadius,
            range = 0f..(layerWidth * 0.30f).coerceAtLeast(8f),
            onValueChange = { value ->
                viewModel.checkpointStart()
                viewModel.updateImage(layerId) { image -> image.copy(cornerRadius = value) }
            },
            onValueChangeFinished = viewModel::checkpointEnd,
        )
        LabeledSlider(
            label = stringResource(R.string.d_opacity),
            valueText = percentageText(opacity),
            value = opacity,
            range = 0.05f..1f,
            onValueChange = { value ->
                viewModel.checkpointStart()
                viewModel.updateLayer(layerId) { layer -> layer.copy(opacity = value) }
            },
            onValueChangeFinished = viewModel::checkpointEnd,
        )
        OutlinedButton(
            onClick = { onReplaceImage(layerId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.d_image_replace))
        }
    }
}

@Composable
private fun BackgroundProperties(viewModel: DesignViewModel, background: DesignBackground) {
    val baseColor = backgroundBaseColor(background)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChipRow(
            labels = listOf(
                stringResource(R.string.d_bg_solid),
                stringResource(R.string.d_bg_gradient),
                stringResource(R.string.d_bg_pattern),
                stringResource(R.string.d_bg_transparent),
            ),
            selectedIndex = backgroundMode(background),
            onSelect = { index ->
                viewModel.setBackground(
                    when (index) {
                        0 -> DesignBackground.Solid(baseColor)
                        1 -> DesignBackground.Gradient(
                            from = baseColor,
                            to = DesignPalette.SAFFRON_SOFT,
                            direction = GradientDirection.VERTICAL,
                        )
                        2 -> DesignBackground.Pattern(
                            base = baseColor,
                            ink = DesignPalette.INK_SOFT,
                        )
                        else -> DesignBackground.Transparent
                    },
                )
            },
        )
        when (background) {
            is DesignBackground.Solid -> {
                FieldLabel(R.string.d_bg_color)
                ColorSwatchRow(
                    colors = DesignPalette.swatches,
                    selected = background.color,
                    onPick = { color -> viewModel.setBackground(DesignBackground.Solid(color)) },
                )
            }
            is DesignBackground.Gradient -> {
                FieldLabel(R.string.d_bg_from)
                ColorSwatchRow(
                    colors = DesignPalette.swatches,
                    selected = background.from,
                    onPick = { color -> viewModel.setBackground(background.copy(from = color)) },
                )
                FieldLabel(R.string.d_bg_to)
                ColorSwatchRow(
                    colors = DesignPalette.swatches,
                    selected = background.to,
                    onPick = { color -> viewModel.setBackground(background.copy(to = color)) },
                )
                FieldLabel(R.string.d_bg_direction)
                ChipRow(
                    labels = listOf(
                        stringResource(R.string.d_dir_vertical),
                        stringResource(R.string.d_dir_horizontal),
                        stringResource(R.string.d_dir_diagonal),
                    ),
                    selectedIndex = when (background.direction) {
                        GradientDirection.VERTICAL -> 0
                        GradientDirection.HORIZONTAL -> 1
                        GradientDirection.DIAGONAL -> 2
                    },
                    onSelect = { index ->
                        viewModel.setBackground(
                            background.copy(
                                direction = when (index) {
                                    0 -> GradientDirection.VERTICAL
                                    1 -> GradientDirection.HORIZONTAL
                                    else -> GradientDirection.DIAGONAL
                                },
                            ),
                        )
                    },
                )
            }
            is DesignBackground.Pattern -> {
                FieldLabel(R.string.d_bg_color)
                ColorSwatchRow(
                    colors = DesignPalette.swatches,
                    selected = background.base,
                    onPick = { color -> viewModel.setBackground(background.copy(base = color)) },
                )
                FieldLabel(R.string.d_bg_ink)
                ColorSwatchRow(
                    colors = DesignPalette.swatches,
                    selected = background.ink,
                    onPick = { color -> viewModel.setBackground(background.copy(ink = color)) },
                )
                ChipRow(
                    labels = listOf(
                        stringResource(R.string.d_pattern_dots),
                        stringResource(R.string.d_pattern_lines),
                        stringResource(R.string.d_pattern_grid),
                        stringResource(R.string.d_pattern_arcs),
                    ),
                    selectedIndex = when (background.kind) {
                        PatternKind.DOTS -> 0
                        PatternKind.LINES -> 1
                        PatternKind.GRID -> 2
                        PatternKind.ARCS -> 3
                    },
                    onSelect = { index ->
                        viewModel.setBackground(
                            background.copy(
                                kind = when (index) {
                                    0 -> PatternKind.DOTS
                                    1 -> PatternKind.LINES
                                    2 -> PatternKind.GRID
                                    else -> PatternKind.ARCS
                                },
                            ),
                        )
                    },
                )
            }
            DesignBackground.Transparent -> Unit
        }
    }
}

@Composable
private fun FieldLabel(id: Int) {
    Text(
        text = stringResource(id),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun panelTitle(document: DesignDocument, selection: DesignSelection): String =
    when (selection) {
        is DesignSelection.Layer -> {
            val layer = document.layer(selection.id)
            when (layer?.content) {
                is LayerContent.Image -> stringResource(R.string.d_layer_image)
                is LayerContent.Shape -> stringResource(R.string.d_layer_shape)
                else -> stringResource(R.string.d_layer_text)
            }
        }
        DesignSelection.Background -> stringResource(R.string.d_layer_background)
        DesignSelection.None -> stringResource(R.string.d_title)
    }

private fun backgroundMode(background: DesignBackground): Int = when (background) {
    is DesignBackground.Solid -> 0
    is DesignBackground.Gradient -> 1
    is DesignBackground.Pattern -> 2
    DesignBackground.Transparent -> 3
}

private fun backgroundBaseColor(background: DesignBackground): Long = when (background) {
    is DesignBackground.Solid -> background.color
    is DesignBackground.Gradient -> background.from
    is DesignBackground.Pattern -> background.base
    DesignBackground.Transparent -> DesignPalette.PAPER
}

@Composable
private fun percentageText(value: Float): String =
    MidadNumbers.percent((value * 100f).roundToInt())
