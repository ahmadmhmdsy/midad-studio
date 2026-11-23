package com.midad.studio.studio.video

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.transformer.CompositionPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.midad.studio.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val PREVIEW_MAX_LONG_EDGE = 960
private const val PREVIEW_END_EPSILON_MS = 100L

/**
 * Full-project preview: the exact Media3 Composition that the exporter writes, played back
 * locally with [CompositionPlayer]. The composition is built by [VideoComposition], so every
 * clip boundary, trim, colour look, output frame and title overlay is represented.
 *
 * [CompositionPlayer] is currently restricted to the Media3 library group but is the only
 * public-in-artifact composition preview in 1.5.1; the dependency is pinned, and the lint
 * suppression is intentional. The exporter itself never uses it.
 */
@SuppressLint("RestrictedApi")
@Composable
internal fun VideoFullPreviewDialog(
    spec: VideoExportSpec,
    totalDurationMs: Long,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playerState = remember { mutableStateOf<CompositionPlayer?>(null) }
    val titleBitmapState = remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(totalDurationMs.coerceAtLeast(1L)) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { playerState.value?.release() }
            playerState.value = null
            titleBitmapState.value?.let { bitmap ->
                if (!bitmap.isRecycled) bitmap.recycle()
            }
            titleBitmapState.value = null
        }
    }

    LaunchedEffect(spec) {
        loading = true
        failed = false
        val previewSpec = spec.forPreview()
        val bitmap = withContext(Dispatchers.Default) {
            TitleRenderer.render(previewSpec.title, previewSpec.outputWidth, previewSpec.outputHeight)
        }
        val composition = VideoComposition.build(previewSpec, bitmap)
        val created = runCatching {
            CompositionPlayer.Builder(context).build().apply {
                setComposition(composition)
                prepare()
            }
        }.getOrElse {
            bitmap?.recycle()
            failed = true
            loading = false
            return@LaunchedEffect
        }
        titleBitmapState.value = bitmap
        playerState.value = created
        runCatching { created.volume = if (spec.muted) 0f else 1f }
        runCatching { created.playWhenReady = true }
        val reported = runCatching { created.duration }.getOrDefault(C.TIME_UNSET)
        if (reported != C.TIME_UNSET && reported > 0L) durationMs = reported
        loading = false
    }

    val player = playerState.value
    DisposableEffect(player, lifecycleOwner) {
        if (player == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
                loading = false
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) runCatching { player.pause() }
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            runCatching { player.release() }
            playerState.value = null
        }
    }

    LaunchedEffect(durationMs, isPlaying) {
        while (true) {
            val current = runCatching { playerState.value?.currentPosition }.getOrNull()
            if (current != null) positionMs = current.coerceIn(0L, durationMs)
            delay(if (isPlaying) 120L else 400L)
        }
    }

    val ratio = spec.outputWidth.toFloat() / spec.outputHeight.coerceAtLeast(1).toFloat()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0A09)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.v_full_preview_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.v_full_preview_subtitle_fmt,
                                spec.clips.size,
                                spec.clips.size,
                                formatClipDuration(spec.clips.sumOf { it.endMs - it.startMs }),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_close), color = Color.White)
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val target = previewBoxSize(maxWidth, maxHeight, ratio)
                    Box(
                        modifier = Modifier
                            .size(target.first, target.second)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black),
                    ) {
                        if (player != null && !failed) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                                        setPlayer(player)
                                    }
                                },
                                update = { view -> view.player = player },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (loading) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                                Text(
                                    text = stringResource(R.string.v_full_preview_loading),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            }
                        }
                        if (failed) {
                            Text(
                                text = stringResource(R.string.v_full_preview_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .wrapContentHeight(),
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.v_full_preview_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    IconButton(
                        onClick = {
                            val active = playerState.value ?: return@IconButton
                            if (active.isPlaying) {
                                active.pause()
                            } else {
                                if (active.playbackState == Player.STATE_ENDED ||
                                    positionMs >= durationMs - PREVIEW_END_EPSILON_MS
                                ) {
                                    active.seekTo(0L)
                                }
                                active.play()
                            }
                        },
                        enabled = player != null && !failed,
                    ) {
                        PlayPauseGlyph(
                            playing = isPlaying,
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Slider(
                        value = positionMs.coerceIn(0L, durationMs).toFloat(),
                        onValueChange = { value ->
                            positionMs = value.toLong()
                            runCatching { playerState.value?.seekTo(value.toLong()) }
                        },
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                        modifier = Modifier.weight(1f),
                        enabled = player != null && !failed,
                    )
                    Text(
                        text = formatClipDuration(positionMs) + " / " + formatClipDuration(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.v_full_preview_done))
                }
            }
        }
    }
}

private fun previewBoxSize(maxWidth: Dp, maxHeight: Dp, ratio: Float): Pair<Dp, Dp> {
    if (maxWidth <= 0.dp || maxHeight <= 0.dp || ratio <= 0f) return 1.dp to 1.dp
    val widthIfFullHeight = maxHeight * ratio
    return if (widthIfFullHeight <= maxWidth) {
        widthIfFullHeight.coerceAtLeast(1.dp) to maxHeight
    } else {
        maxWidth to (maxWidth / ratio).coerceAtLeast(1.dp)
    }
}

/** Renders the preview at a bounded size; the look, fit and title are identical to the export. */
private fun VideoExportSpec.forPreview(): VideoExportSpec {
    val longEdge = maxOf(outputWidth, outputHeight)
    if (longEdge <= PREVIEW_MAX_LONG_EDGE) return this
    val scale = PREVIEW_MAX_LONG_EDGE.toFloat() / longEdge.toFloat()
    val width = (((outputWidth * scale).toInt()).coerceAtLeast(2) / 2) * 2
    val height = (((outputHeight * scale).toInt()).coerceAtLeast(2) / 2) * 2
    return copy(outputWidth = width, outputHeight = height)
}
