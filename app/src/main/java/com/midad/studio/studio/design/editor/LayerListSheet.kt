package com.midad.studio.studio.design.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.studio.design.model.DesignBackground
import com.midad.studio.studio.design.model.DesignDocument
import com.midad.studio.studio.design.model.DesignLayer
import com.midad.studio.studio.design.model.DesignSelection
import com.midad.studio.studio.design.model.LayerContent
import com.midad.studio.studio.design.model.ShapeKind
import com.midad.studio.studio.design.ui.DesignGlyph
import com.midad.studio.studio.design.ui.DesignGlyphMark
import com.midad.studio.studio.design.ui.GlyphIconButton

/** Top-first layer list with reorder and delete, plus the canvas background pinned at the end. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerListSheet(
    document: DesignDocument,
    selection: DesignSelection,
    onSelectLayer: (Long) -> Unit,
    onSelectBackground: () -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.d_layers_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                GlyphIconButton(
                    glyph = DesignGlyph.CLOSE,
                    description = stringResource(R.string.d_action_close_panel),
                    onClick = onDismiss,
                )
            }
            Spacer(Modifier.height(10.dp))
            val ordered = document.layers.asReversed()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(ordered, key = { _, layer -> layer.id }) { index, layer ->
                    LayerRow(
                        layer = layer,
                        selected = selection is DesignSelection.Layer && selection.id == layer.id,
                        canMoveUp = index > 0,
                        canMoveDown = index < ordered.lastIndex,
                        onSelect = { onSelectLayer(layer.id) },
                        onMoveUp = { onMoveUp(layer.id) },
                        onMoveDown = { onMoveDown(layer.id) },
                        onDelete = { onDelete(layer.id) },
                    )
                }
                item {
                    BackgroundRow(
                        background = document.background,
                        selected = selection is DesignSelection.Background,
                        onClick = onSelectBackground,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LayerRow(
    layer: DesignLayer,
    selected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onSelect)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesignGlyphMark(
            glyph = layerGlyph(layer),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = layerTitle(layer),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = layerSubtitle(layer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GlyphIconButton(
            glyph = DesignGlyph.UP,
            description = stringResource(R.string.d_layer_move_up),
            onClick = onMoveUp,
            enabled = canMoveUp,
        )
        GlyphIconButton(
            glyph = DesignGlyph.DOWN,
            description = stringResource(R.string.d_layer_move_down),
            onClick = onMoveDown,
            enabled = canMoveDown,
        )
        GlyphIconButton(
            glyph = DesignGlyph.TRASH,
            description = stringResource(R.string.d_layer_delete),
            onClick = onDelete,
        )
    }
}

@Composable
private fun BackgroundRow(
    background: DesignBackground,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val rowBackground = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesignGlyphMark(
            glyph = DesignGlyph.BACKGROUND,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.d_layer_background),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = backgroundLabel(background),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun layerGlyph(layer: DesignLayer): DesignGlyph = when (val content = layer.content) {
    is LayerContent.Text -> DesignGlyph.TEXT
    is LayerContent.Image -> DesignGlyph.IMAGE
    is LayerContent.Shape -> when (content.kind) {
        ShapeKind.RECTANGLE -> DesignGlyph.RECTANGLE
        ShapeKind.ELLIPSE -> DesignGlyph.ELLIPSE
        ShapeKind.LINE -> DesignGlyph.LINE
        ShapeKind.STAR -> DesignGlyph.STAR
        ShapeKind.RULE -> DesignGlyph.RULE
    }
}

@Composable
private fun layerTitle(layer: DesignLayer): String = when (val content = layer.content) {
    is LayerContent.Text -> content.text.lineSequence().firstOrNull()?.trim().orEmpty()
        .ifEmpty { stringResource(R.string.d_layer_text) }
    is LayerContent.Image -> stringResource(R.string.d_layer_image)
    is LayerContent.Shape -> shapeLabel(content.kind)
}

@Composable
private fun layerSubtitle(layer: DesignLayer): String = when (val content = layer.content) {
    is LayerContent.Text -> stringResource(R.string.d_layer_text)
    is LayerContent.Image -> stringResource(R.string.d_layer_image)
    is LayerContent.Shape -> stringResource(R.string.d_layer_shape)
}

@Composable
fun shapeLabel(kind: ShapeKind): String = stringResource(
    when (kind) {
        ShapeKind.RECTANGLE -> R.string.d_shape_rectangle
        ShapeKind.ELLIPSE -> R.string.d_shape_ellipse
        ShapeKind.LINE -> R.string.d_shape_line
        ShapeKind.STAR -> R.string.d_shape_star
        ShapeKind.RULE -> R.string.d_shape_underline
    },
)

@Composable
private fun backgroundLabel(background: DesignBackground): String = stringResource(
    when (background) {
        is DesignBackground.Solid -> R.string.d_bg_solid
        is DesignBackground.Gradient -> R.string.d_bg_gradient
        is DesignBackground.Pattern -> R.string.d_bg_pattern
        DesignBackground.Transparent -> R.string.d_bg_transparent
    },
)
