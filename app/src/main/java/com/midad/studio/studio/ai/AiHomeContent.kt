package com.midad.studio.studio.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.core.ai.AiModelStatus
import com.midad.studio.core.ai.ModelState
import com.midad.studio.core.ai.internal.ManifestProblem
import com.midad.studio.core.format.formatBytes
import com.midad.studio.ui.components.BorderedPanel
import com.midad.studio.ui.components.HairlineDivider
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.components.StudioScaffold
import com.midad.studio.ui.theme.MidadColors
import kotlin.math.roundToInt

@Composable
fun AiHomeContent(
    state: AiUiState,
    onBack: () -> Unit,
    onOpenCutout: () -> Unit,
    onOpenModels: () -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
) {
    StudioScaffold(title = stringResource(R.string.studio_ai_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AiHomeHeader()
            AiErrorBanner(error = state.error, onDismiss = onDismissError)
            SectionLabel(stringResource(R.string.a_tools_label))
            ToolCard(
                title = stringResource(R.string.a_tool_cutout_title),
                description = stringResource(R.string.a_tool_cutout_desc),
                status = when {
                    state.catalogueLoading -> stringResource(R.string.a_models_loading)
                    state.hasModel -> stringResource(R.string.a_tool_ready)
                    else -> stringResource(R.string.a_tool_needs_model)
                },
                ready = state.hasModel,
                glyph = AiGlyphKind.CUTOUT,
                onClick = if (state.hasModel) onOpenCutout else onOpenModels,
            )
            ToolCard(
                title = stringResource(R.string.a_tool_models_title),
                description = stringResource(R.string.a_tool_models_desc),
                status = when {
                    state.catalogueLoading -> stringResource(R.string.a_models_loading)
                    state.installedCount > 0 -> pluralStringResource(
                        R.plurals.a_tool_models_ready,
                        state.installedCount,
                        state.installedCount,
                    )
                    else -> stringResource(R.string.a_tool_needs_model)
                },
                ready = state.installedCount > 0,
                glyph = AiGlyphKind.MODELS,
                onClick = onOpenModels,
            )
            SectionLabel(stringResource(R.string.a_models_label))
            when {
                state.catalogueLoading -> LoadingRow()
                state.models.isEmpty() -> EmptyCataloguePanel(
                    problem = state.catalogueProblem,
                    onRefresh = onRefresh,
                )
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        state.models.forEach { status -> CompactModelRow(status) }
                    }
                    TextButton(onClick = onOpenModels) {
                        Text(stringResource(R.string.a_models_manage))
                    }
                }
            }
            HairlineDivider()
            ModelLicenceNote()
            Spacer(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun AiHomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidadColors.Sage.copy(alpha = 0.12f))
                    .border(1.dp, MidadColors.Sage.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                AiGlyph(AiGlyphKind.CUTOUT, MidadColors.Sage, Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.a_home_headline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.a_home_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OnDeviceBadge(stringResource(R.string.a_home_offline_badge))
    }
}

@Composable
private fun ToolCard(
    title: String,
    description: String,
    status: String,
    ready: Boolean,
    glyph: AiGlyphKind,
    onClick: () -> Unit,
) {
    val accent = if (ready) MidadColors.Sage else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                AiGlyph(glyph, accent, Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                )
            }
            Text(
                text = "\u203A",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(
            text = stringResource(R.string.a_models_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyCataloguePanel(problem: ManifestProblem?, onRefresh: () -> Unit) {
    val detail = when (problem) {
        ManifestProblem.MISSING -> stringResource(R.string.a_models_missing_catalogue)
        ManifestProblem.INVALID -> stringResource(R.string.a_models_invalid_catalogue)
        null -> stringResource(R.string.a_models_empty_body)
    }
    BorderedPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.a_models_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRefresh) { Text(stringResource(R.string.a_action_refresh)) }
        }
    }
}

@Composable
private fun CompactModelRow(status: AiModelStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = modelTitle(status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.a_model_size_fmt,
                    formatBytes(status.model.sizeBytes),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        if (status.state == ModelState.DOWNLOADING || status.state == ModelState.VERIFYING) {
            Text(
                text = stringResource(
                    R.string.a_models_progress_fmt,
                    (status.progress * 100f).roundToInt().coerceIn(0, 100),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
        }
        ModelStateChip(status)
    }
}
