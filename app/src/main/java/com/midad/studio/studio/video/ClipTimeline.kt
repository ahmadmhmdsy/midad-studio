package com.midad.studio.studio.video

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.midad.studio.R

@Composable
internal fun ClipTimeline(
    clips: List<VideoClip>,
    selectedClipId: String?,
    loading: Boolean,
    onSelect: (String) -> Unit,
    onMoveEarlier: (String) -> Unit,
    onMoveLater: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items = clips, key = { clip -> clip.id }) { clip ->
                ClipCard(
                    clip = clip,
                    selected = clip.id == selectedClipId,
                    onSelect = { onSelect(clip.id) },
                )
            }
            if (clips.size < MAX_VIDEO_CLIPS) {
                item {
                    AddClipCard(loading = loading, onAdd = onAdd)
                }
            }
        }
        val selectedIndex = clips.indexOfFirst { it.id == selectedClipId }
        if (selectedIndex >= 0) {
            val selectedId = clips[selectedIndex].id
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { onMoveEarlier(selectedId) },
                    enabled = selectedIndex > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.v_move_earlier),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = { onMoveLater(selectedId) },
                    enabled = selectedIndex < clips.lastIndex,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.v_move_later),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { onDelete(selectedId) }) {
                    Text(
                        text = stringResource(R.string.v_remove_clip),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipCard(
    clip: VideoClip,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val borderWidth = if (selected) 2.dp else 1.dp
    val displayName = if (clip.name.isBlank()) {
        stringResource(R.string.v_clip_default_name)
    } else {
        clip.name
    }
    Column(modifier = Modifier.width(108.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(borderWidth, borderColor, shape)
                .clickable(onClick = onSelect),
        ) {
            val thumbnail = clip.thumbnail
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = stringResource(R.string.v_clip_no_thumbnail),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 6.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    text = formatClipDuration(clip.trimmedDurationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
        )
    }
}

@Composable
private fun AddClipCard(
    loading: Boolean,
    onAdd: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = Modifier.width(108.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape)
                .clickable(enabled = !loading, onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            PlusGlyph(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Text(
            text = stringResource(if (loading) R.string.v_loading_media else R.string.v_add_clip),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
        )
    }
}

@Composable
private fun PlusGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.12f
        drawLine(
            color = color,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = stroke,
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = stroke,
        )
    }
}
