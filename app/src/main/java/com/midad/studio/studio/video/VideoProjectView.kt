package com.midad.studio.studio.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.ui.components.BorderedPanel
import com.midad.studio.ui.components.EmptyState
import com.midad.studio.ui.components.MidadChoiceChip
import com.midad.studio.ui.components.SectionLabel

@Composable
internal fun VideoEmptyState(
    modifier: Modifier = Modifier,
    loading: Boolean,
    notice: VideoNotice?,
    onPick: () -> Unit,
    onDismissNotice: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            title = stringResource(R.string.v_empty_title),
            message = stringResource(R.string.v_empty_message),
        )
        Text(
            text = stringResource(R.string.v_empty_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onPick, enabled = !loading) {
            Text(stringResource(if (loading) R.string.v_loading_media else R.string.v_pick))
        }
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        }
        notice?.let { current ->
            NoticeBanner(text = videoNoticeText(current), onDismiss = onDismissNotice)
        }
    }
}

@Composable
internal fun VideoProjectView(
    state: VideoUiState,
    viewModel: VideoStudioViewModel,
    onPickMore: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedClip
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VideoPreviewPanel(
            clip = selected,
            muted = state.muted,
            title = state.title,
            look = state.look,
            exportActive = state.busy,
            onFullPreview = onPreview,
        )
        SectionLabel(stringResource(R.string.v_section_clips))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.v_clip_count_fmt, state.clips.size, MAX_VIDEO_CLIPS),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.v_total_duration_fmt, formatClipDuration(state.totalDurationMs)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ClipTimeline(
            clips = state.clips,
            selectedClipId = state.selectedClipId,
            loading = state.loading,
            onSelect = viewModel::selectClip,
            onMoveEarlier = { viewModel.moveClip(it, -1) },
            onMoveLater = { viewModel.moveClip(it, 1) },
            onDelete = viewModel::deleteClip,
            onAdd = onPickMore,
        )
        state.notice?.let { current ->
            NoticeBanner(text = videoNoticeText(current), onDismiss = viewModel::clearNotice)
        }
        if (selected != null) {
            SectionLabel(stringResource(R.string.v_section_trim))
            TrimPanel(
                clip = selected,
                onTrim = { start, end -> viewModel.updateTrim(selected.id, start, end) },
            )
        }
        SectionLabel(stringResource(R.string.v_section_look))
        LookRow(selected = state.look, onSelect = viewModel::setLook)
        SectionLabel(stringResource(R.string.v_section_frame))
        FitRow(selected = state.fit, onSelect = viewModel::setFit)
        Text(
            text = stringResource(R.string.v_frame_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionLabel(stringResource(R.string.v_section_audio))
        MidadChoiceChip(
            label = stringResource(R.string.v_mute_label),
            selected = state.muted,
            onClick = { viewModel.setMuted(!state.muted) },
        )
        SectionLabel(stringResource(R.string.v_section_title))
        TitleEditor(
            title = state.title,
            onText = viewModel::setTitleText,
            onPosition = viewModel::setTitlePosition,
            onSize = viewModel::setTitleSize,
            onColor = viewModel::setTitleColor,
        )
        Text(
            text = stringResource(R.string.v_title_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun VideoExportBar(
    enabled: Boolean,
    totalDurationMs: Long,
    onPreview: () -> Unit,
    onExport: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 10.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.v_total_duration_fmt, formatClipDuration(totalDurationMs)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onPreview, enabled = enabled) {
                Text(stringResource(R.string.v_preview))
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onExport, enabled = enabled) {
                Text(stringResource(R.string.v_export))
            }
        }
    }
}

@Composable
internal fun NoticeBanner(text: String, onDismiss: () -> Unit) {
    BorderedPanel {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        }
    }
}

@Composable
internal fun videoNoticeText(notice: VideoNotice): String = stringResource(
    when (notice) {
        VideoNotice.LIMIT_REACHED -> R.string.v_notice_limit
        VideoNotice.SOME_UNREADABLE -> R.string.v_notice_some_unreadable
        VideoNotice.ALL_UNREADABLE -> R.string.v_notice_all_unreadable
        VideoNotice.DURATION_LIMIT -> R.string.projects_video_cap_notice
    },
)
