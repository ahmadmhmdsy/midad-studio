package com.midad.studio.studio.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.core.ai.AiModelStatus
import com.midad.studio.core.ai.ModelState
import com.midad.studio.core.ai.internal.ModelFailure
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.theme.MidadColors

/** Canvas marks used by the AI Lab; drawn here so no icon font or third-party art is needed. */
enum class AiGlyphKind { CUTOUT, MODELS }

@Composable
fun AiGlyph(kind: AiGlyphKind, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val side = minOf(size.width, size.height)
        val stroke = Stroke(width = side * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            AiGlyphKind.CUTOUT -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.12f),
                    size = Size(size.width * 0.76f, size.height * 0.76f),
                    cornerRadius = CornerRadius(side * 0.10f),
                    style = stroke,
                )
                drawCircle(
                    color = color,
                    radius = side * 0.13f,
                    center = Offset(size.width * 0.50f, size.height * 0.38f),
                    style = stroke,
                )
                val shoulders = Path().apply {
                    moveTo(size.width * 0.30f, size.height * 0.72f)
                    cubicTo(
                        size.width * 0.38f, size.height * 0.56f,
                        size.width * 0.62f, size.height * 0.56f,
                        size.width * 0.70f, size.height * 0.72f,
                    )
                }
                drawPath(shoulders, color = color, style = stroke)
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.16f, size.height * 0.16f),
                    end = Offset(size.width * 0.29f, size.height * 0.29f),
                    strokeWidth = side * 0.06f,
                    cap = StrokeCap.Round,
                )
            }

            AiGlyphKind.MODELS -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.24f),
                    size = Size(size.width * 0.52f, size.height * 0.52f),
                    cornerRadius = CornerRadius(side * 0.09f),
                    style = stroke,
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.30f, size.height * 0.38f),
                    size = Size(size.width * 0.52f, size.height * 0.52f),
                    cornerRadius = CornerRadius(side * 0.09f),
                    style = stroke,
                )
                drawCircle(
                    color = color,
                    radius = side * 0.07f,
                    center = Offset(size.width * 0.73f, size.height * 0.25f),
                )
            }
        }
    }
}

@Composable
fun OnDeviceBadge(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MidadColors.Sage.copy(alpha = 0.12f))
            .border(1.dp, MidadColors.Sage.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(MidadColors.Sage),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Two-tone checkerboard shown behind transparent cut-outs. */
@Composable
fun CheckerboardBackground(modifier: Modifier = Modifier) {
    val light = MaterialTheme.colorScheme.surface
    val dark = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val cell = 12.dp.toPx()
        var row = 0
        var y = 0f
        while (y < size.height) {
            var column = 0
            var x = 0f
            while (x < size.width) {
                val color = if ((row + column) % 2 == 0) light else dark
                drawRect(color = color, topLeft = Offset(x, y), size = Size(cell, cell))
                x += cell
                column += 1
            }
            y += cell
            row += 1
        }
    }
}

/**
 * A state chip that reads at a glance without colour alone: ready models get a filled dot,
 * idle ones a hollow dot, and a working one a spinner. Downloading and verifying used to look
 * identical to "installed"; they now share a busy tone and are told apart by their label.
 */
@Composable
fun ModelStateChip(status: AiModelStatus, modifier: Modifier = Modifier) {
    val state = status.state
    val (label, tone) = when (state) {
        ModelState.INSTALLED -> stringResource(R.string.a_state_installed) to MaterialTheme.colorScheme.primary
        ModelState.BUNDLED -> stringResource(R.string.a_state_bundled) to MaterialTheme.colorScheme.tertiary
        ModelState.DOWNLOADING -> stringResource(R.string.a_state_downloading) to MaterialTheme.colorScheme.secondary
        ModelState.VERIFYING -> stringResource(R.string.a_state_verifying) to MaterialTheme.colorScheme.secondary
        ModelState.FAILED -> stringResource(R.string.a_state_failed) to MaterialTheme.colorScheme.error
        ModelState.NOT_INSTALLED -> stringResource(R.string.a_state_not_installed) to
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    val ready = state == ModelState.INSTALLED || state == ModelState.BUNDLED
    val busy = state == ModelState.DOWNLOADING || state == ModelState.VERIFYING
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tone.copy(alpha = if (ready) 0.14f else 0.06f))
            .border(1.dp, tone.copy(alpha = if (ready) 0.40f else 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        when {
            busy -> CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.5.dp,
                color = tone,
            )
            state == ModelState.NOT_INSTALLED -> Box(
                modifier = Modifier
                    .size(6.dp)
                    .border(1.dp, tone, CircleShape),
            )
            else -> Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(tone),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tone,
        )
    }
}

@Composable
fun AiErrorBanner(
    error: AiError?,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    if (error == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidadColors.Danger.copy(alpha = 0.10f))
            .border(1.dp, MidadColors.Danger.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = errorText(error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
        }
    }
}

@Composable
fun AiProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
fun cutoutStageLabel(stage: CutoutStage): String = stringResource(
    when (stage) {
        CutoutStage.IDLE -> R.string.a_stage_working
        CutoutStage.OPENING -> R.string.a_stage_opening
        CutoutStage.SEGMENTING -> R.string.a_stage_segmenting
        CutoutStage.COMPOSITING -> R.string.a_stage_compositing
        CutoutStage.SAVING -> R.string.a_stage_saving
    },
)

@Composable
fun modelFailureText(code: String?): String = stringResource(
    when (code) {
        ModelFailure.NETWORK -> R.string.a_error_network
        ModelFailure.CHECKSUM -> R.string.a_error_checksum
        ModelFailure.STORAGE -> R.string.a_error_storage
        ModelFailure.NO_URL -> R.string.a_error_model_link
        else -> R.string.error_generic
    },
)

@Composable
fun ModelLicenceNote() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(stringResource(R.string.a_licence_label))
        Text(
            text = stringResource(R.string.a_licence_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun errorText(error: AiError): String = stringResource(
    when (error) {
        AiError.DECODE -> R.string.a_error_decode
        AiError.NO_MODEL -> R.string.a_error_no_model
        AiError.MODEL_UNREADABLE -> R.string.a_error_model_unreadable
        AiError.RUNTIME_UNAVAILABLE -> R.string.a_error_runtime
        AiError.INFERENCE -> R.string.a_error_inference
        AiError.SAVE -> R.string.a_error_save
        AiError.LINK -> R.string.a_error_link
    },
)
