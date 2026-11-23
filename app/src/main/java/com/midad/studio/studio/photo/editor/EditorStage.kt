package com.midad.studio.studio.photo.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.ui.theme.MidadColors
import com.midad.studio.studio.photo.engine.CropRect
import com.midad.studio.studio.photo.engine.DrawStroke
import com.midad.studio.studio.photo.engine.GeometryPlan
import com.midad.studio.studio.photo.engine.StickerLayer
import com.midad.studio.studio.photo.engine.TextLayer
import com.midad.studio.studio.photo.ui.drawStickerGlyph
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.rotate

/** Maps the geometry output onto the on-screen stage. */
private data class StageMetrics(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val imageWidth: Float,
    val imageHeight: Float,
) {
    fun outputToDisplay(x: Float, y: Float): Offset = Offset(offsetX + x * scale, offsetY + y * scale)

    fun displayToNormalised(plan: GeometryPlan, position: Offset): Offset {
        val outX = (position.x - offsetX) / scale
        val outY = (position.y - offsetY) / scale
        val mapped = plan.outputPxToBaseNormalized(outX, outY)
        return Offset(mapped[0], mapped[1])
    }
}

private data class MeasuredText(val layer: TextLayer, val layout: TextLayoutResult)

@Composable
fun PhotoStage(
    state: PhotoEditorState,
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier,
) {
    val frame = state.previewFrame
    val plan = state.plan
    val photoDescription = stringResource(R.string.p_content_desc_photo)
    val renderingDescription = stringResource(R.string.p_preview_rendering)
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MidadColors.NightBackground),
        contentAlignment = Alignment.Center,
    ) {
        if (frame == null || plan == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
            return@Box
        }
        val density = LocalDensity.current
        val imageBitmap = remember(frame) { frame.asImageBitmap() }
        val measurer = rememberTextMeasurer()
        val latestState: State<PhotoEditorState> = rememberUpdatedState(state)
        val handleThreshold = with(density) { 26.dp.toPx() }
        val minDisplaySize = with(density) { 24.dp.toPx() }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableWidth = constraints.maxWidth.toFloat()
            val availableHeight = constraints.maxHeight.toFloat()
            val outputWidth = plan.outputWidth.coerceAtLeast(1)
            val outputHeight = plan.outputHeight.coerceAtLeast(1)
            val scale = min(availableWidth / outputWidth, availableHeight / outputHeight)
            val imageWidth = outputWidth * scale
            val imageHeight = outputHeight * scale
            val offsetX = (availableWidth - imageWidth) / 2f
            val offsetY = (availableHeight - imageHeight) / 2f
            val metrics = StageMetrics(scale, offsetX, offsetY, imageWidth, imageHeight)

            val measuredTexts = remember(state.textLayers, plan.baseWidth, scale, density) {
                state.textLayers.map { layer ->
                    val sizePx = max(layer.sizeFraction * plan.baseWidth * scale, 4f)
                    val style = androidx.compose.ui.text.TextStyle(
                        fontSize = with(density) { sizePx.toSp() },
                        fontFamily = if (layer.serif) FontFamily.Serif else FontFamily.SansSerif,
                    )
                    MeasuredText(layer, measurer.measure(AnnotatedString(layer.text.ifEmpty { " " }), style = style))
                }
            }
            val textSizes = measuredTexts.associate { it.layer.id to it.layout.size }

            val draft = state.draftStroke
            val strokes = if (draft != null) state.strokes + draft else state.strokes
            val selectedText = state.selectedText
            val selectedSticker = state.selectedSticker

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // The canvas is the product's main surface: give TalkBack a name and,
                    // while the colour pass runs, say why the image is about to change.
                    .semantics {
                        contentDescription = photoDescription
                        if (state.renderingPreview) stateDescription = renderingDescription
                    }
                    .pointerInput(plan, scale, offsetX, offsetY, state.activeTool) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val snapshot = latestState.value
                            val currentPlan = snapshot.plan ?: return@awaitEachGesture
                            when (snapshot.activeTool) {
                                PhotoTool.CROP -> cropGesture(
                                    plan = currentPlan,
                                    metrics = metrics,
                                    viewModel = viewModel,
                                    readState = { latestState.value },
                                    down = down,
                                    threshold = handleThreshold,
                                    minSize = minDisplaySize,
                                )
                                PhotoTool.DRAW -> drawGesture(
                                    plan = currentPlan,
                                    metrics = metrics,
                                    viewModel = viewModel,
                                    readState = { latestState.value },
                                    down = down,
                                )
                                else -> selectGesture(
                                    plan = currentPlan,
                                    metrics = metrics,
                                    viewModel = viewModel,
                                    readState = { latestState.value },
                                    down = down,
                                    touchSlop = minDisplaySize,
                                )
                            }
                        }
                    },
            ) {
                val imageRect = Rect(offsetX, offsetY, offsetX + imageWidth, offsetY + imageHeight)
                clipRect(imageRect.left, imageRect.top, imageRect.right, imageRect.bottom) {
                    drawImage(
                        image = imageBitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(frame.width, frame.height),
                        dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                        dstSize = IntSize(imageWidth.roundToInt(), imageHeight.roundToInt()),
                        filterQuality = FilterQuality.Medium,
                    )
                }
                drawStrokes(strokes, plan, metrics, imageRect)
                clipRect(imageRect.left, imageRect.top, imageRect.right, imageRect.bottom) {
                    measuredTexts.forEach { measured ->
                        val layer = measured.layer
                        val center = metrics.outputToDisplay(
                            plan.baseNormalizedToOutputPx(layer.x, layer.y)[0],
                            plan.baseNormalizedToOutputPx(layer.x, layer.y)[1],
                        )
                        val halfW = measured.layout.size.width / 2f
                        val halfH = measured.layout.size.height / 2f
                        rotate(degrees = layer.rotation + plan.rotationDegrees, pivot = center) {
                            drawText(
                                textLayoutResult = measured.layout,
                                color = Color(layer.color).copy(alpha = layer.opacity.coerceIn(0f, 1f)),
                                topLeft = Offset(center.x - halfW, center.y - halfH),
                            )
                        }
                    }
                    state.stickers.forEach { sticker ->
                        val center = metrics.outputToDisplay(
                            plan.baseNormalizedToOutputPx(sticker.x, sticker.y)[0],
                            plan.baseNormalizedToOutputPx(sticker.x, sticker.y)[1],
                        )
                        drawStickerGlyph(
                            shape = sticker.shape,
                            center = center,
                            sizePx = sticker.sizeFraction * plan.baseWidth * scale,
                            rotationDegrees = sticker.rotation + plan.rotationDegrees,
                            color = Color(sticker.color).copy(alpha = sticker.opacity.coerceIn(0f, 1f)),
                        )
                    }
                }
                if (selectedText != null && state.activeTool != PhotoTool.CROP) {
                    val layoutSize = textSizes[selectedText.id]
                    drawSelection(
                        plan = plan,
                        metrics = metrics,
                        layerX = selectedText.x,
                        layerY = selectedText.y,
                        rotation = selectedText.rotation,
                        widthPx = layoutSize?.width?.toFloat()
                            ?: (selectedText.text.length.coerceAtLeast(2) * 0.55f * selectedText.sizeFraction * plan.baseWidth * scale),
                        heightPx = layoutSize?.height?.toFloat()
                            ?: (selectedText.sizeFraction * plan.baseWidth * scale * 1.3f),
                    )
                }
                if (selectedSticker != null && state.activeTool != PhotoTool.CROP) {
                    val side = selectedSticker.sizeFraction * plan.baseWidth * scale
                    drawSelection(
                        plan = plan,
                        metrics = metrics,
                        layerX = selectedSticker.x,
                        layerY = selectedSticker.y,
                        rotation = selectedSticker.rotation,
                        widthPx = side,
                        heightPx = side,
                    )
                }
                if (state.activeTool == PhotoTool.CROP) {
                    drawCropOverlay(state.geometry.crop, metrics, state.geometry.aspect.ratio != null)
                }
            }

            if (state.renderingPreview) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp),
                    color = Color.White.copy(alpha = 0.7f),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

// --------------------------------------------------------------------- drawing

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokes(
    strokes: List<DrawStroke>,
    plan: GeometryPlan,
    metrics: StageMetrics,
    imageRect: Rect,
) {
    if (strokes.isEmpty()) return
    val hasEraser = strokes.any { it.erase }
    clipRect(imageRect.left, imageRect.top, imageRect.right, imageRect.bottom) {
        if (!hasEraser) {
            strokes.forEach { stroke -> drawStroke(stroke, plan, metrics) }
        } else {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(
                    Rect(0f, 0f, size.width, size.height),
                    Paint(),
                )
            }
            strokes.forEach { stroke -> drawStroke(stroke, plan, metrics) }
            drawIntoCanvas { canvas -> canvas.restore() }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(
    stroke: DrawStroke,
    plan: GeometryPlan,
    metrics: StageMetrics,
) {
    if (stroke.points.size < 2) return
    val path = Path()
    stroke.points.forEachIndexed { index, point ->
        val output = plan.baseNormalizedToOutputPx(point.x, point.y)
        val x = metrics.offsetX + output[0] * metrics.scale
        val y = metrics.offsetY + output[1] * metrics.scale
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    val width = max(stroke.widthFraction * plan.baseWidth * metrics.scale, 1f)
    drawPath(
        path = path,
        color = if (stroke.erase) Color.Transparent else Color(stroke.color),
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = if (stroke.erase) BlendMode.Clear else BlendMode.SrcOver,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSelection(
    plan: GeometryPlan,
    metrics: StageMetrics,
    layerX: Float,
    layerY: Float,
    rotation: Float,
    widthPx: Float,
    heightPx: Float,
) {
    val output = plan.baseNormalizedToOutputPx(layerX, layerY)
    val center = metrics.outputToDisplay(output[0], output[1])
    val padding = 8.dp.toPx()
    val halfW = widthPx / 2f + padding
    val halfH = heightPx / 2f + padding
    rotate(degrees = rotation + plan.rotationDegrees, pivot = center) {
        drawRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(center.x - halfW, center.y - halfH),
            size = Size(halfW * 2f, halfH * 2f),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f),
            ),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCropOverlay(
    crop: CropRect,
    metrics: StageMetrics,
    aspectLocked: Boolean,
) {
    val left = metrics.offsetX + crop.left * metrics.imageWidth
    val top = metrics.offsetY + crop.top * metrics.imageHeight
    val right = metrics.offsetX + crop.right * metrics.imageWidth
    val bottom = metrics.offsetY + crop.bottom * metrics.imageHeight
    val imageRight = metrics.offsetX + metrics.imageWidth
    val imageBottom = metrics.offsetY + metrics.imageHeight
    val shade = Color.Black.copy(alpha = 0.55f)
    drawRect(shade, Offset(metrics.offsetX, metrics.offsetY), Size(metrics.imageWidth, max(top - metrics.offsetY, 0f)))
    drawRect(shade, Offset(metrics.offsetX, bottom), Size(metrics.imageWidth, max(imageBottom - bottom, 0f)))
    drawRect(shade, Offset(metrics.offsetX, top), Size(max(left - metrics.offsetX, 0f), max(bottom - top, 0f)))
    drawRect(shade, Offset(right, top), Size(max(imageRight - right, 0f), max(bottom - top, 0f)))

    val thin = Stroke(width = 1.dp.toPx())
    val gridColor = Color.White.copy(alpha = 0.35f)
    val thirdW = (right - left) / 3f
    val thirdH = (bottom - top) / 3f
    repeat(2) { index ->
        val x = left + thirdW * (index + 1)
        drawLine(gridColor, Offset(x, top), Offset(x, bottom), strokeWidth = thin.width)
        val y = top + thirdH * (index + 1)
        drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = thin.width)
    }
    drawRect(
        color = Color.White.copy(alpha = 0.95f),
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        style = Stroke(width = 2.dp.toPx()),
    )

    val handle = 18.dp.toPx()
    val handleStroke = 3.dp.toPx()
    val white = Color.White
    drawLine(white, Offset(left, top + handle), Offset(left, top), handleStroke)
    drawLine(white, Offset(left, top), Offset(left + handle, top), handleStroke)
    drawLine(white, Offset(right - handle, top), Offset(right, top), handleStroke)
    drawLine(white, Offset(right, top), Offset(right, top + handle), handleStroke)
    drawLine(white, Offset(left, bottom - handle), Offset(left, bottom), handleStroke)
    drawLine(white, Offset(left, bottom), Offset(left + handle, bottom), handleStroke)
    drawLine(white, Offset(right - handle, bottom), Offset(right, bottom), handleStroke)
    drawLine(white, Offset(right, bottom), Offset(right, bottom - handle), handleStroke)

    if (!aspectLocked) {
        val tick = 10.dp.toPx()
        val midX = (left + right) / 2f
        val midY = (top + bottom) / 2f
        drawLine(white, Offset(midX, top), Offset(midX, top + tick), handleStroke)
        drawLine(white, Offset(midX, bottom - tick), Offset(midX, bottom), handleStroke)
        drawLine(white, Offset(left, midY), Offset(left + tick, midY), handleStroke)
        drawLine(white, Offset(right - tick, midY), Offset(right, midY), handleStroke)
    }
}

// --------------------------------------------------------------------- gestures

private suspend fun AwaitPointerEventScope.cropGesture(
    plan: GeometryPlan,
    metrics: StageMetrics,
    viewModel: PhotoEditorViewModel,
    readState: () -> PhotoEditorState,
    down: PointerInputChange,
    threshold: Float,
    minSize: Float,
) {
    down.consume()
    val initial = readState().geometry.crop
    val aspect = readState().geometry.aspect.ratio
    val drag = detectCropDrag(
        position = down.position,
        crop = initial,
        metrics = metrics,
        threshold = threshold,
        aspectLocked = aspect != null,
    ) ?: return
    val expandedWidth = plan.outputWidth.toFloat()
    val expandedHeight = plan.outputHeight.toFloat()
    val downNormalised = cropNormalised(down.position, metrics)
    var pressed = true
    while (pressed) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.pressed }
        if (change == null) {
            pressed = false
            break
        }
        val pointer = cropNormalised(change.position, metrics)
        val next = when (drag) {
            is CropDrag.Corner -> cornerCrop(initial, drag.index, pointer, aspect, expandedWidth, expandedHeight)
            is CropDrag.Edge -> edgeCrop(initial, drag.index, pointer)
            CropDrag.Move -> moveCrop(initial, downNormalised, pointer)
        }
        viewModel.updateCropRectLive(next)
        change.consume()
    }
    viewModel.commitLiveEdit()
}

private suspend fun AwaitPointerEventScope.drawGesture(
    plan: GeometryPlan,
    metrics: StageMetrics,
    viewModel: PhotoEditorViewModel,
    readState: () -> PhotoEditorState,
    down: PointerInputChange,
) {
    down.consume()
    val start = metrics.displayToNormalised(plan, down.position)
    viewModel.beginStroke()
    viewModel.appendStrokePoint(start.x, start.y)
    var pressed = true
    while (pressed) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.pressed }
        if (change == null) {
            pressed = false
            break
        }
        val point = metrics.displayToNormalised(plan, change.position)
        viewModel.appendStrokePoint(point.x, point.y)
        change.consume()
    }
    viewModel.endStroke()
}

private suspend fun AwaitPointerEventScope.selectGesture(
    plan: GeometryPlan,
    metrics: StageMetrics,
    viewModel: PhotoEditorViewModel,
    readState: () -> PhotoEditorState,
    down: PointerInputChange,
    touchSlop: Float,
) {
    down.consume()
    val state = readState()
    val sticker = state.stickers.asReversed().firstOrNull {
        stickerBounds(plan, metrics, it).let { bounds -> bounds.contains(down.position) }
    }
    val text = if (sticker == null) {
        state.textLayers.asReversed().firstOrNull {
            textBounds(plan, metrics, it).let { bounds -> bounds.contains(down.position) }
        }
    } else {
        null
    }
    if (sticker == null && text == null) {
        var moved = false
        var pressed = true
        while (pressed) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.pressed }
            if (change == null) {
                pressed = false
                break
            }
            if ((change.position - down.position).getDistance() > touchSlop) moved = true
            change.consume()
        }
        if (!moved) viewModel.clearSelection()
        return
    }
    if (sticker != null) viewModel.selectSticker(sticker.id) else viewModel.selectText(text!!.id)
    var transformed = false
    var pressed = true
    while (pressed) {
        val event = awaitPointerEvent()
        if (event.changes.none { it.pressed }) {
            pressed = false
            break
        }
        val zoom = event.calculateZoom()
        val rotation = event.calculateRotation()
        val pan = event.calculatePan()
        val panBase = plan.outputDeltaToBaseDelta(pan.x / metrics.scale, pan.y / metrics.scale)
        if (abs(zoom - 1f) > 0.001f || abs(rotation) > 0.05f || pan.getDistance() > 0.3f) {
            if (sticker != null) {
                viewModel.transformSelectedSticker(zoom, rotation, panBase[0], panBase[1])
            } else {
                viewModel.transformSelectedText(zoom, rotation, panBase[0], panBase[1])
            }
            transformed = true
        }
        event.changes.forEach { it.consume() }
    }
    if (transformed) viewModel.commitLiveEdit()
}

// --------------------------------------------------------------------- crop maths

private sealed interface CropDrag {
    data class Corner(val index: Int) : CropDrag
    data class Edge(val index: Int) : CropDrag
    data object Move : CropDrag
}

private fun detectCropDrag(
    position: Offset,
    crop: CropRect,
    metrics: StageMetrics,
    threshold: Float,
    aspectLocked: Boolean,
): CropDrag? {
    val left = metrics.offsetX + crop.left * metrics.imageWidth
    val top = metrics.offsetY + crop.top * metrics.imageHeight
    val right = metrics.offsetX + crop.right * metrics.imageWidth
    val bottom = metrics.offsetY + crop.bottom * metrics.imageHeight
    val corners = listOf(
        Offset(left, top),
        Offset(right, top),
        Offset(left, bottom),
        Offset(right, bottom),
    )
    var bestIndex = -1
    var bestDistance = Float.MAX_VALUE
    corners.forEachIndexed { index, corner ->
        val distance = (position - corner).getDistance()
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    if (bestDistance <= threshold) return CropDrag.Corner(bestIndex)
    if (!aspectLocked) {
        val edges = listOf(
            Offset((left + right) / 2f, top),
            Offset(right, (top + bottom) / 2f),
            Offset((left + right) / 2f, bottom),
            Offset(left, (top + bottom) / 2f),
        )
        edges.forEachIndexed { index, midpoint ->
            if ((position - midpoint).getDistance() <= threshold) return CropDrag.Edge(index)
        }
    }
    if (position.x in left..right && position.y in top..bottom) return CropDrag.Move
    return null
}

private fun cropNormalised(position: Offset, metrics: StageMetrics): Offset = Offset(
    ((position.x - metrics.offsetX) / metrics.imageWidth).coerceIn(0f, 1f),
    ((position.y - metrics.offsetY) / metrics.imageHeight).coerceIn(0f, 1f),
)

private const val MIN_CROP = 0.07f

private fun cornerCrop(
    initial: CropRect,
    corner: Int,
    pointer: Offset,
    aspect: Float?,
    expandedWidth: Float,
    expandedHeight: Float,
): CropRect {
    var left = if (corner == 0 || corner == 2) pointer.x else initial.left
    var top = if (corner == 0 || corner == 1) pointer.y else initial.top
    var right = if (corner == 1 || corner == 3) pointer.x else initial.right
    var bottom = if (corner == 2 || corner == 3) pointer.y else initial.bottom
    left = left.coerceIn(0f, 1f - MIN_CROP)
    top = top.coerceIn(0f, 1f - MIN_CROP)
    right = right.coerceIn(left + MIN_CROP, 1f)
    bottom = bottom.coerceIn(top + MIN_CROP, 1f)
    if (aspect != null && aspect > 0f) {
        val pixelFactor = expandedWidth / expandedHeight
        var width = right - left
        var height = bottom - top
        when (corner) {
            0 -> {
                height = width * pixelFactor / aspect
                if (height > bottom) {
                    height = bottom
                    width = height * aspect / pixelFactor
                }
                left = right - width
                top = bottom - height
            }
            1 -> {
                height = width * pixelFactor / aspect
                if (height > bottom) {
                    height = bottom
                    width = height * aspect / pixelFactor
                }
                right = left + width
                top = bottom - height
            }
            2 -> {
                height = width * pixelFactor / aspect
                if (height > 1f - top) {
                    height = 1f - top
                    width = height * aspect / pixelFactor
                }
                left = right - width
                bottom = top + height
            }
            else -> {
                height = width * pixelFactor / aspect
                if (height > 1f - top) {
                    height = 1f - top
                    width = height * aspect / pixelFactor
                }
                right = left + width
                bottom = top + height
            }
        }
    }
    val result = CropRect(left, top, right, bottom).clamped()
    if (aspect != null && aspect > 0f && result.width / result.height > 0.0001f) {
        // Keep the ratio after clamping by trimming the longer side around the anchor.
        val targetHeight = result.width * (expandedWidth / expandedHeight) / aspect
        return if (targetHeight <= result.height) {
            val newTop = if (corner == 0 || corner == 1) result.bottom - targetHeight else result.top
            CropRect(result.left, newTop.coerceIn(0f, 1f - targetHeight), result.right, (newTop + targetHeight).coerceIn(0f, 1f)).clamped()
        } else {
            val targetWidth = result.height * aspect / (expandedWidth / expandedHeight)
            val newLeft = if (corner == 0 || corner == 2) result.right - targetWidth else result.left
            CropRect(newLeft.coerceIn(0f, 1f - targetWidth), result.top, (newLeft + targetWidth).coerceIn(0f, 1f), result.bottom).clamped()
        }
    }
    return result
}

private fun edgeCrop(initial: CropRect, index: Int, pointer: Offset): CropRect {
    var left = initial.left
    var top = initial.top
    var right = initial.right
    var bottom = initial.bottom
    when (index) {
        0 -> top = pointer.y.coerceIn(0f, bottom - MIN_CROP)
        1 -> right = pointer.x.coerceIn(left + MIN_CROP, 1f)
        2 -> bottom = pointer.y.coerceIn(top + MIN_CROP, 1f)
        else -> left = pointer.x.coerceIn(0f, right - MIN_CROP)
    }
    return CropRect(left, top, right, bottom).clamped()
}

private fun moveCrop(initial: CropRect, from: Offset, to: Offset): CropRect {
    val width = initial.width
    val height = initial.height
    val left = (initial.left + (to.x - from.x)).coerceIn(0f, 1f - width)
    val top = (initial.top + (to.y - from.y)).coerceIn(0f, 1f - height)
    return CropRect(left, top, left + width, top + height)
}

// --------------------------------------------------------------------- hit tests

private fun textBounds(plan: GeometryPlan, metrics: StageMetrics, layer: TextLayer): Rect {
    val output = plan.baseNormalizedToOutputPx(layer.x, layer.y)
    val center = metrics.outputToDisplay(output[0], output[1])
    val baseSize = layer.sizeFraction * plan.baseWidth * metrics.scale
    val width = max(layer.text.length.coerceAtLeast(2) * 0.55f * baseSize, baseSize)
    val height = baseSize * 1.3f
    return Rect(
        center.x - width / 2f,
        center.y - height / 2f,
        center.x + width / 2f,
        center.y + height / 2f,
    )
}

private fun stickerBounds(plan: GeometryPlan, metrics: StageMetrics, layer: StickerLayer): Rect {
    val output = plan.baseNormalizedToOutputPx(layer.x, layer.y)
    val center = metrics.outputToDisplay(output[0], output[1])
    val side = layer.sizeFraction * plan.baseWidth * metrics.scale
    return Rect(
        center.x - side / 2f,
        center.y - side / 2f,
        center.x + side / 2f,
        center.y + side / 2f,
    )
}
