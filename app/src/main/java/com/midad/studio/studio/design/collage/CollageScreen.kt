package com.midad.studio.studio.design.collage

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.midad.studio.R
import com.midad.studio.projects.ProjectSaveButton
import com.midad.studio.projects.ProjectSaveStyle
import com.midad.studio.studio.design.CollageState
import com.midad.studio.studio.design.DesignViewModel
import com.midad.studio.studio.design.model.DesignPalette
import com.midad.studio.studio.design.ui.ChipRow
import com.midad.studio.studio.design.ui.ColorSwatchRow
import com.midad.studio.studio.design.ui.DesignGlyph
import com.midad.studio.studio.design.ui.DesignGlyphMark
import com.midad.studio.studio.design.ui.LabeledSlider
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.components.StudioScaffold
import com.midad.studio.ui.text.MidadNumbers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Pick photos, choose a grid, set the gaps and paper colour, then export. */
@Composable
fun CollageScreen(viewModel: DesignViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = viewModel.collage
    val layout = viewModel.collageLayout
    val imageBitmaps = remember(state.bitmaps) {
        state.bitmaps.map { bitmap -> bitmap.asImageBitmap() }
    }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(9),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch { viewModel.loadCollageImages(context, uris) }
        }
    }
    val launchPicker = {
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    BackHandler { viewModel.backToStart() }

    StudioScaffold(
        title = viewModel.activeProjectTitle ?: stringResource(R.string.d_collage_title),
        onBack = { viewModel.backToStart() },
        actions = {
            ProjectSaveButton(
                style = ProjectSaveStyle.TEXT_ACTION,
                hasContent = state.bitmaps.isNotEmpty(),
                activeProjectTitle = viewModel.activeProjectTitle,
                defaultTitle = stringResource(R.string.projects_default_title_design),
                onSave = { title -> viewModel.saveProject(context, title) },
            )
            TextButton(
                onClick = { scope.launch { viewModel.exportCollage(context) } },
                enabled = state.bitmaps.size >= 2 && !state.loading,
            ) {
                Text(stringResource(R.string.d_action_export))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.bitmaps.isEmpty()) {
                Text(
                    text = stringResource(R.string.d_collage_hero),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.d_collage_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PickButton(labelRes = R.string.d_collage_pick, onClick = launchPicker)
                if (state.error) {
                    Text(
                        text = stringResource(R.string.d_collage_need_two),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(380.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CollagePreview(
                        state = state,
                        layout = layout,
                        images = imageBitmaps,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.d_collage_selected_fmt,
                            state.bitmaps.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = launchPicker) {
                        Text(stringResource(R.string.d_collage_change))
                    }
                }
                if (state.loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.d_collage_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.error) {
                    Text(
                        text = stringResource(
                            if (state.bitmaps.size < 2) {
                                R.string.d_collage_need_two
                            } else {
                                R.string.d_collage_error
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                SectionLabel(stringResource(R.string.d_collage_layout))
                val layouts = remember(state.bitmaps.size) {
                    CollageLayouts.layoutsForCount(state.bitmaps.size)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(layouts, key = { item -> item.id }) { item ->
                        LayoutThumb(
                            layout = item,
                            selected = item.id == layout.id,
                            onClick = { viewModel.selectCollageLayout(item.id) },
                        )
                    }
                }

                SectionLabel(stringResource(R.string.d_collage_format))
                ChipRow(
                    labels = listOf(
                        stringResource(CollageAspect.SQUARE.labelRes),
                        stringResource(CollageAspect.STORY.labelRes),
                        stringResource(CollageAspect.WIDE.labelRes),
                    ),
                    selectedIndex = state.aspect.ordinal,
                    onSelect = { index ->
                        viewModel.selectCollageAspect(CollageAspect.values()[index])
                    },
                )

                LabeledSlider(
                    label = stringResource(R.string.d_collage_spacing),
                    valueText = MidadNumbers.format(state.spacing.toLong()),
                    value = state.spacing.toFloat(),
                    range = 0f..48f,
                    onValueChange = { value ->
                        viewModel.setCollageSpacing(value.roundToInt())
                    },
                    onValueChangeFinished = {},
                )

                SectionLabel(stringResource(R.string.d_collage_background))
                ColorSwatchRow(
                    colors = DesignPalette.swatches,
                    selected = state.background,
                    onPick = { color -> viewModel.setCollageBackground(color) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PickButton(labelRes: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        DesignGlyphMark(
            glyph = DesignGlyph.PLUS,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(stringResource(labelRes))
    }
}

@Composable
private fun CollagePreview(
    state: CollageState,
    layout: CollageLayout,
    images: List<ImageBitmap>,
) {
    Box(
        modifier = Modifier
            .aspectRatio(state.aspect.aspectRatio)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(state.background.toInt())),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacingPx = state.spacing.toFloat() * min(size.width, size.height) / 1080f
            val rects = CollageLayouts.cellRects(
                layout = layout,
                canvasWidth = size.width,
                canvasHeight = size.height,
                spacing = spacingPx,
            )
            rects.forEachIndexed { index, rect ->
                val image = images.getOrNull(index) ?: return@forEachIndexed
                drawCoverImage(
                    image = image,
                    left = rect.left,
                    top = rect.top,
                    width = rect.width(),
                    height = rect.height(),
                )
            }
        }
    }
}

private fun DrawScope.drawCoverImage(
    image: ImageBitmap,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
) {
    if (width <= 0f || height <= 0f || image.width <= 0 || image.height <= 0) return
    val imageWidth = image.width.toFloat()
    val imageHeight = image.height.toFloat()
    val factor = max(width / imageWidth, height / imageHeight)
    val sourceWidth = (width / factor).roundToInt().coerceIn(1, image.width)
    val sourceHeight = (height / factor).roundToInt().coerceIn(1, image.height)
    drawImage(
        image = image,
        srcOffset = IntOffset(
            (image.width - sourceWidth) / 2,
            (image.height - sourceHeight) / 2,
        ),
        srcSize = IntSize(sourceWidth, sourceHeight),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(
            width.roundToInt().coerceAtLeast(1),
            height.roundToInt().coerceAtLeast(1),
        ),
    )
}

@Composable
private fun LayoutThumb(layout: CollageLayout, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    val cellColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Canvas(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        CollageLayouts.cellRects(layout, size.width, size.height, 3f).forEach { rect ->
            drawRect(
                color = cellColor.copy(alpha = if (selected) 0.8f else 0.5f),
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width(), rect.height()),
            )
        }
    }
}
