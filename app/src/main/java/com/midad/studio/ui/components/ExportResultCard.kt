package com.midad.studio.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.midad.studio.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ExportMediaKind { IMAGE, VIDEO }

fun exportMediaKindFor(fileName: String): ExportMediaKind =
    if (fileName.endsWith(".mp4", ignoreCase = true)) ExportMediaKind.VIDEO else ExportMediaKind.IMAGE

/**
 * Shared success dialog for every studio. It answers the two questions the old dialogs left
 * unanswered: what the file looks like and where it went. Open uses ACTION_VIEW; Share keeps the
 * previous behaviour; Close leaves the editor as it was.
 */
@Composable
internal fun ExportSuccessDialog(
    title: String,
    message: String?,
    fileName: String,
    uri: Uri,
    inGallery: Boolean,
    kind: ExportMediaKind,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ExportResultCard(
                    fileName = fileName,
                    uri = uri,
                    inGallery = inGallery,
                    kind = kind,
                    onOpen = onOpen,
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpen) {
                Text(stringResource(R.string.action_open))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onShare) {
                    Text(stringResource(R.string.action_share))
                }
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.action_close))
                }
            }
        },
    )
}

/** Preview, name and location of one finished export. */
@Composable
internal fun ExportResultCard(
    fileName: String,
    uri: Uri,
    inGallery: Boolean,
    kind: ExportMediaKind,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var thumbnail by remember(uri, kind) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(uri, kind) { mutableStateOf(true) }
    LaunchedEffect(uri, kind) {
        loading = true
        thumbnail = withContext(Dispatchers.IO) { loadThumbnail(context, uri, kind) }?.asImageBitmap()
        loading = false
    }
    val location = if (inGallery) {
        stringResource(
            R.string.export_result_location_gallery_fmt,
            if (kind == ExportMediaKind.VIDEO) "Movies/Midad" else "Pictures/Midad",
        )
    } else {
        stringResource(R.string.export_result_location_app)
    }
    val previewLabel = stringResource(R.string.export_result_preview_fmt, fileName)

    BorderedPanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF151310))
                    .semantics {
                        contentDescription = previewLabel
                        role = Role.Button
                    }
                    .clickable(onClick = onOpen),
                contentAlignment = Alignment.Center,
            ) {
                if (kind == ExportMediaKind.IMAGE) {
                    Checkerboard(modifier = Modifier.matchParentSize())
                }
                val image = thumbnail
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.export_result_no_thumbnail),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                if (kind == ExportMediaKind.VIDEO) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "▶",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                }
            }
            Text(
                text = fileName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Neutral transparency grid so a cut-out with an empty or soft alpha does not read as a black box. */
@Composable
private fun Checkerboard(modifier: Modifier = Modifier) {
    val dark = Color(0xFF2A2622)
    val light = Color(0xFF3A352F)
    Canvas(modifier = modifier) {
        val tile = 12.dp.toPx()
        var row = 0
        var y = 0f
        while (y < size.height) {
            var col = 0
            var x = 0f
            while (x < size.width) {
                drawRect(
                    color = if ((row + col) % 2 == 0) dark else light,
                    topLeft = Offset(x, y),
                    size = Size(tile + 1f, tile + 1f),
                )
                x += tile
                col++
            }
            y += tile
            row++
        }
    }
}

private fun loadThumbnail(context: Context, uri: Uri, kind: ExportMediaKind): Bitmap? = runCatching {
    if (kind == ExportMediaKind.VIDEO) loadVideoFrame(context, uri) else loadImage(context, uri)
}.getOrNull()

private fun loadImage(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 720)
    }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
}

private fun loadVideoFrame(context: Context, uri: Uri): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 720, 720)
        } else {
            retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } ?: return null
        val rotation = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull()
            ?: 0
        if (rotation == 0) frame else frame.rotated(rotation)
    } finally {
        runCatching { retriever.release() }
    }
}

private fun Bitmap.rotated(degrees: Int): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun sampleSize(width: Int, height: Int, target: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (w / 2 >= target || h / 2 >= target) {
        w /= 2
        h /= 2
        sample *= 2
    }
    return sample
}
