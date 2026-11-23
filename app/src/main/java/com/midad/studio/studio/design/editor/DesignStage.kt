package com.midad.studio.studio.design.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.studio.design.model.DesignDocument
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Gestures arrive as raw pointer events so one recognizer can own tap, drag, pinch and twist:
 * the first down selects, and movement past touch slop begins a transform batch.
 */
class DesignStageInteraction(
    val onSelect: (Long?) -> Unit,
    val onBegin: () -> Unit,
    val onEnd: () -> Unit,
    val onTransform: (
        centroidDoc: Offset,
        panDoc: Offset,
        zoom: Float,
        rotationDeg: Float,
        selectionHeightDoc: Float,
    ) -> Unit,
)

@Composable
fun DesignStage(
    document: DesignDocument,
    images: Map<Long, ImageBitmap>,
    modifier: Modifier = Modifier,
    selectedId: Long? = null,
    showGuides: Boolean = false,
    interaction: DesignStageInteraction? = null,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val selectionColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val checkerLight = MaterialTheme.colorScheme.surface
    val checkerDark = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val canvasLabel = stringResource(R.string.d_canvas_cd)

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val availableWidth = with(density) { maxWidth.toPx() }
        val availableHeight = with(density) { maxHeight.toPx() }
        if (availableWidth <= 0f || availableHeight <= 0f) return@BoxWithConstraints
        val scale = min(availableWidth / document.width, availableHeight / document.height)
        if (scale <= 0f) return@BoxWithConstraints
        val stageWidth = with(density) { (document.width * scale).toDp() }
        val stageHeight = with(density) { (document.height * scale).toDp() }
        val textLayoutCache = remember(measurer, density) {
            mutableMapOf<DesignTextLayoutKey, TextLayoutResult>()
        }
        val textLayouts = remember(document, scale, measurer) {
            measureDesignText(document, scale, measurer, density, textLayoutCache)
        }
        val latestDocument by rememberUpdatedState(document)
        val latestLayouts by rememberUpdatedState(textLayouts)
        val latestScale by rememberUpdatedState(scale)
        val stageInteraction = interaction
        val gestureModifier = if (stageInteraction != null) {
            Modifier.pointerInput(stageInteraction) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val activeDocument = latestDocument
                    val activeLayouts = latestLayouts
                    val activeScale = latestScale
                    val targetId = hitTestDesignLayer(
                        document = activeDocument,
                        textLayouts = activeLayouts,
                        scale = activeScale,
                        position = down.position,
                    )
                    stageInteraction.onSelect(targetId)
                    if (targetId == null) {
                        down.consume()
                        return@awaitEachGesture
                    }
                    stageInteraction.onBegin()
                    var dragged = false
                    var canceled = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { change -> change.isConsumed }) {
                            canceled = true
                            break
                        }
                        val zoomChange = event.calculateZoom()
                        val rotationChange = event.calculateRotation()
                        val panChange = event.calculatePan()
                        if (!dragged) {
                            val zoomMotion = abs(1f - zoomChange) * size.height
                            val rotationMotion =
                                abs(rotationChange * (PI.toFloat() / 180f)) * size.height
                            val panMotion = panChange.getDistance()
                            if (zoomMotion > viewConfiguration.touchSlop ||
                                rotationMotion > viewConfiguration.touchSlop ||
                                panMotion > viewConfiguration.touchSlop
                            ) {
                                dragged = true
                            }
                        }
                        val centroid = event.calculateCentroid()
                        if (dragged && centroid != Offset.Unspecified) {
                            stageInteraction.onTransform(
                                centroid / latestScale,
                                panChange / latestScale,
                                zoomChange,
                                rotationChange,
                                designSelectedHeight(
                                    document = activeDocument,
                                    textLayouts = activeLayouts,
                                    layerId = targetId,
                                    scale = latestScale,
                                ),
                            )
                            event.changes.forEach { change -> change.consume() }
                        }
                        if (event.changes.none { change -> change.pressed }) break
                    }
                    stageInteraction.onEnd()
                    if (canceled) return@awaitEachGesture
                }
            }
        } else {
            Modifier
        }
        Canvas(
            modifier = Modifier
                .size(stageWidth, stageHeight)
                .border(1.dp, borderColor)
                .then(gestureModifier)
                .semantics { contentDescription = canvasLabel },
        ) {
            drawDesignDocument(
                document = document,
                scale = scale,
                textLayouts = textLayouts,
                images = images,
                selectedId = selectedId,
                showGuides = showGuides,
                guideColor = guideColor,
                selectionColor = selectionColor,
                checkerLight = checkerLight,
                checkerDark = checkerDark,
            )
        }
    }
}

private fun hitTestDesignLayer(
    document: DesignDocument,
    textLayouts: Map<Long, TextLayoutResult>,
    scale: Float,
    position: Offset,
): Long? {
    if (scale <= 0f) return null
    val point = position / scale
    for (index in document.layers.indices.reversed()) {
        val layer = document.layers[index]
        val height = designLayerHeight(layer, textLayouts, scale) / scale
        if (height <= 0f) continue
        val centerX = layer.x + layer.width / 2f
        val centerY = layer.y + height / 2f
        val dx = point.x - centerX
        val dy = point.y - centerY
        val radians = -layer.rotation * (PI.toFloat() / 180f)
        val rotatedX = dx * cos(radians) - dy * sin(radians)
        val rotatedY = dx * sin(radians) + dy * cos(radians)
        if (abs(rotatedX) <= layer.width / 2f && abs(rotatedY) <= height / 2f) {
            return layer.id
        }
    }
    return null
}

private fun designSelectedHeight(
    document: DesignDocument,
    textLayouts: Map<Long, TextLayoutResult>,
    layerId: Long,
    scale: Float,
): Float {
    val layer = document.layer(layerId) ?: return 0f
    return designLayerHeight(layer, textLayouts, scale) / scale
}
