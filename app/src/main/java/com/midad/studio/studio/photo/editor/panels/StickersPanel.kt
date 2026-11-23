package com.midad.studio.studio.photo.editor.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.midad.studio.studio.photo.editor.PhotoPalette
import com.midad.studio.studio.photo.engine.StickerShape
import com.midad.studio.studio.photo.ui.ColorSwatchRow
import com.midad.studio.studio.photo.ui.PanelHint
import com.midad.studio.studio.photo.ui.SliderRow
import com.midad.studio.studio.photo.ui.StickerGlyphView
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.text.MidadNumbers
import kotlin.math.roundToInt

/** Nine stickers drawn from vectors in code. */
@Composable
fun StickersPanel(
    state: PhotoEditorState,
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedSticker
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StickerShape.entries.chunked(3).forEach { rowShapes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowShapes.forEach { shape ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.addSticker(shape) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            StickerGlyphView(
                                shape = shape,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.height(38.dp).fillMaxWidth(0.72f),
                            )
                        }
                        Text(
                            text = stringResource(stickerShapeLabel(shape)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        if (selected != null) {
            SectionLabel(stringResource(R.string.p_sticker_size))
            SliderRow(
                label = stringResource(R.string.p_sticker_size),
                valueText = MidadNumbers.percent((selected.sizeFraction * 100f).roundToInt()),
                value = selected.sizeFraction,
                valueRange = 0.04f..0.8f,
                onValueChange = { viewModel.setStickerSizeLive(it) },
                onValueChangeFinished = { viewModel.commitLiveEdit() },
            )
            SectionLabel(stringResource(R.string.p_text_color))
            ColorSwatchRow(
                colors = PhotoPalette.stickers,
                selected = selected.color,
                onSelect = { viewModel.setStickerColor(it) },
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PanelHint(stringResource(R.string.p_sticker_hint), Modifier.weight(1f))
                TextButton(onClick = { viewModel.deleteSelectedSticker() }) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}

private fun stickerShapeLabel(shape: StickerShape): Int = when (shape) {
    StickerShape.ARROW -> R.string.p_sticker_arrow
    StickerShape.BUBBLE -> R.string.p_sticker_bubble
    StickerShape.STAR -> R.string.p_sticker_star
    StickerShape.HEART -> R.string.p_sticker_heart
    StickerShape.UNDERLINE -> R.string.p_sticker_underline
    StickerShape.HIGHLIGHT -> R.string.p_sticker_highlight
    StickerShape.QUOTE -> R.string.p_sticker_quote
    StickerShape.FRAME -> R.string.p_sticker_frame
    StickerShape.BURST -> R.string.p_sticker_burst
}
