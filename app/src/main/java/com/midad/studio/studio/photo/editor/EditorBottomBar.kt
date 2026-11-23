package com.midad.studio.studio.photo.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.midad.studio.studio.photo.editor.panels.AdjustPanel
import com.midad.studio.studio.photo.editor.panels.CropPanel
import com.midad.studio.studio.photo.editor.panels.DrawPanel
import com.midad.studio.studio.photo.editor.panels.FiltersPanel
import com.midad.studio.studio.photo.editor.panels.StickersPanel
import com.midad.studio.studio.photo.editor.panels.TextPanel
import com.midad.studio.studio.photo.ui.ToolGlyph
import com.midad.studio.ui.components.HairlineDivider

/** The tool rail, plus the expansion panel for the active tool. */
@Composable
fun EditorBottomBar(
    state: PhotoEditorState,
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier,
) {
    val activeTool = state.activeTool
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HairlineDivider()
        if (activeTool != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                when (activeTool) {
                    PhotoTool.ADJUST -> AdjustPanel(state, viewModel)
                    PhotoTool.FILTERS -> FiltersPanel(state, viewModel)
                    PhotoTool.CROP -> CropPanel(state, viewModel)
                    PhotoTool.TEXT -> TextPanel(state, viewModel)
                    PhotoTool.STICKERS -> StickersPanel(state, viewModel)
                    PhotoTool.DRAW -> DrawPanel(state, viewModel)
                }
            }
            HairlineDivider()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoTool.entries.forEach { tool ->
                val selected = tool == activeTool
                val tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        // Announces "Tab, selected" instead of an unnamed button; the
                        // tint alone was the only signal of the active tool.
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { viewModel.selectTool(tool) },
                        )
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ToolGlyph(tool = tool, color = tint, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(tool.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}