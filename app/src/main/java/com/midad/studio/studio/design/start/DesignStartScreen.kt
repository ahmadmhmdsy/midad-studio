package com.midad.studio.studio.design.start

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.studio.design.editor.DesignStage
import com.midad.studio.studio.design.model.CanvasPreset
import com.midad.studio.studio.design.model.DesignTemplate
import com.midad.studio.studio.design.ui.DesignGlyph
import com.midad.studio.studio.design.ui.DesignGlyphMark
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.components.StudioScaffold
import com.midad.studio.ui.theme.MidadColors

/** The studio's front door: canvases, the two ways in, and a shelf of templates. */
@Composable
fun DesignStartScreen(
    onBack: () -> Unit,
    templates: List<DesignTemplate>,
    onPreset: (CanvasPreset) -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenCollage: () -> Unit,
    onOpenTemplate: (DesignTemplate) -> Unit,
) {
    val presets = remember { CanvasPreset.values().toList() }
    StudioScaffold(
        title = stringResource(R.string.d_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.d_start_hero_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.d_start_hero_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionLabel(stringResource(R.string.d_start_canvas_label))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(presets) { preset ->
                    PresetCard(preset = preset, onClick = { onPreset(preset) })
                }
            }

            SectionLabel(stringResource(R.string.d_start_ways_label))
            Row(
                modifier = Modifier.fillMaxWidth().height(168.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EntryCard(
                    glyph = DesignGlyph.TEMPLATE,
                    titleRes = R.string.d_entry_templates_title,
                    bodyRes = R.string.d_entry_templates_body,
                    onClick = onOpenTemplates,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
                EntryCard(
                    glyph = DesignGlyph.COLLAGE,
                    titleRes = R.string.d_entry_collage_title,
                    bodyRes = R.string.d_entry_collage_body,
                    onClick = onOpenCollage,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.d_start_featured_label))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenTemplates) {
                    Text(stringResource(R.string.d_start_see_all))
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(templates, key = { template -> template.id }) { template ->
                    TemplateMini(
                        template = template,
                        onClick = { onOpenTemplate(template) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetCard(preset: CanvasPreset, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(112.dp, 140.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(preset.aspectRatio)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MidadColors.PaperElevated)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    .padding(8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(2.dp)
                            .background(MidadColors.Saffron),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(3.dp)
                            .background(MidadColors.Ink),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(3.dp)
                            .background(MidadColors.Ink.copy(alpha = 0.45f)),
                    )
                }
            }
        }
        Text(
            text = stringResource(preset.labelRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.d_canvas_dims_fmt, preset.width, preset.height),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EntryCard(
    glyph: DesignGlyph,
    titleRes: Int,
    bodyRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DesignGlyphMark(
            glyph = glyph,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TemplateMini(template: DesignTemplate, onClick: () -> Unit) {
    val document = template.document
    Column(
        modifier = Modifier
            .width(190.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            DesignStage(
                document = document,
                images = emptyMap(),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(template.nameRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text = stringResource(R.string.d_canvas_dims_fmt, document.width, document.height),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
