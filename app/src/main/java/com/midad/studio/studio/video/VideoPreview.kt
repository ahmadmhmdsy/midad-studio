package com.midad.studio.studio.video

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.midad.studio.R
import com.midad.studio.ui.components.BorderedPanel
import kotlinx.coroutines.delay

private const val PREVIEW_RELOAD_DEBOUNCE_MS = 220L
private const val PREVIEW_END_EPSILON_MS = 100L

/**
 * ExoPlayer preview of the selected clip with its trim and colour look applied. This is the
 * trimming aid; [VideoFullPreviewDialog] plays the whole project as the exporter will write it.
 */
@Composable
internal fun VideoPreviewPanel(
    clip: VideoClip?,
    muted: Boolean,
    title: VideoTitle,
    look: VideoLook,
    exportActive: Boolean,
    onFullPreview: () -> Unit,
) {
    if (clip == null) {
        BorderedPanel {
            Text(
                text = stringResource(R.string.v_no_clip_selected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        PreviewContent(
            clip = clip,
            muted = muted,
            title = title,
            look = look,
            exportActive = exportActive,
            onFullPreview = onFullPreview,
        )
    }
}

@Composable
private fun PreviewContent(
    clip: VideoClip,
    muted: Boolean,
    title: VideoTitle,
    look: VideoLook,
    exportActive: Boolean,
    onFullPreview: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var failed by remember(clip.id) { mutableStateOf(false) }
    val trimmedDuration = clip.trimmedDurationMs.coerceAtLeast(1L)

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        }
        // Stop audio/decoding as soon as the screen is no longer visible.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                runCatching { exoPlayer.pause() }
            }
        }
        exoPlayer.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(clip.id, clip.trimStartMs, clip.trimEndMs) {
        delay(PREVIEW_RELOAD_DEBOUNCE_MS)
        failed = false
        runCatching {
            exoPlayer.pause()
            exoPlayer.setMediaItem(buildVideoMediaItem(clip.uri, clip.trimStartMs, clip.trimEndMs))
            exoPlayer.prepare()
        }.onFailure { failed = true }
        positionMs = 0L
    }

    LaunchedEffect(muted) {
        runCatching { exoPlayer.volume = if (muted) 0f else 1f }
    }

    // The selected-clip preview applies the same colour look as the export. Frame fit and the
    // title are only exact in the full-project preview.
    LaunchedEffect(look) {
        runCatching { exoPlayer.setVideoEffects(VideoEffects.lookEffects(look)) }
    }

    // The exporter needs the codecs too; release them while it runs.
    LaunchedEffect(exportActive) {
        if (exportActive) {
            runCatching { exoPlayer.stop() }
        }
    }

    LaunchedEffect(trimmedDuration, isPlaying) {
        while (true) {
            val current = runCatching { exoPlayer.currentPosition }.getOrNull()
            if (current != null) {
                positionMs = current.coerceIn(0L, trimmedDuration)
            }
            delay(if (isPlaying) 120L else 400L)
        }
    }

    val playPauseLabel = if (isPlaying) stringResource(R.string.v_pause) else stringResource(R.string.v_play)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        player = exoPlayer
                    }
                },
                update = { view -> view.player = exoPlayer },
                modifier = Modifier.fillMaxSize(),
            )
            if (title.text.isNotBlank()) {
                TitlePreviewOverlay(title = title)
            }
            if (failed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.v_preview_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                onClick = { togglePlayback(exoPlayer, isPlaying, positionMs, trimmedDuration) },
                modifier = Modifier.semantics { contentDescription = playPauseLabel },
            ) {
                PlayPauseGlyph(
                    playing = isPlaying,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Slider(
                value = positionMs.coerceIn(0L, trimmedDuration).toFloat(),
                onValueChange = { value ->
                    positionMs = value.toLong()
                    exoPlayer.seekTo(value.toLong())
                },
                valueRange = 0f..trimmedDuration.toFloat(),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatClipDuration(positionMs) + " / " + formatClipDuration(trimmedDuration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.v_preview_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onFullPreview,
            enabled = !exportActive,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.v_preview_full_action))
        }
    }
}

private fun togglePlayback(
    player: ExoPlayer,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
) {
    if (isPlaying) {
        player.pause()
    } else {
        if (player.playbackState == Player.STATE_ENDED ||
            positionMs >= durationMs - PREVIEW_END_EPSILON_MS
        ) {
            player.seekTo(0L)
        }
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        player.play()
    }
}

@Composable
private fun TitlePreviewOverlay(title: VideoTitle) {
    val alignment = when (title.position) {
        TitlePosition.TOP -> Alignment.TopCenter
        TitlePosition.CENTER -> Alignment.Center
        TitlePosition.BOTTOM -> Alignment.BottomCenter
    }
    val fontSize = when (title.size) {
        TitleSize.SMALL -> 16.sp
        TitleSize.MEDIUM -> 22.sp
        TitleSize.LARGE -> 30.sp
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = alignment,
    ) {
        Text(
            text = title.text,
            color = Color(title.color.argb),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
            ),
        )
    }
}

@Composable
internal fun PlayPauseGlyph(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (playing) {
            val barWidth = size.width * 0.28f
            val gap = size.width * 0.18f
            drawRect(
                color = color,
                topLeft = Offset(0f, 0f),
                size = Size(barWidth, size.height),
            )
            drawRect(
                color = color,
                topLeft = Offset(barWidth + gap, 0f),
                size = Size(barWidth, size.height),
            )
        } else {
            val triangle = Path().apply {
                moveTo(size.width * 0.16f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(size.width * 0.16f, size.height)
                close()
            }
            drawPath(path = triangle, color = color)
        }
    }
}
