package com.midad.studio.home

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.midad.studio.BuildConfig
import com.midad.studio.R
import com.midad.studio.core.project.ExportRecord
import com.midad.studio.core.project.ProjectStore
import com.midad.studio.navigation.StudioKind
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.components.StudioGlyph
import com.midad.studio.ui.theme.MidadColors

@Composable
fun HomeScreen(
    onOpenStudio: (StudioKind) -> Unit,
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExports: () -> Unit = onOpenProjects,
) {
    val context = LocalContext.current
    val recent by ProjectStore.records.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { ProjectStore.ensureLoaded(context) }

    // Opaque blend of background into the raised surface. The previous 45%-alpha stop let
    // the (light) platform window background show through, which washed the dark theme out.
    val fade = lerp(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.surfaceVariant,
        0.45f,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, fade))),
    ) {
        HomeBackdrop()
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp, bottom = 32.dp),
            ) {
                HomeHeader(onOpenSettings = onOpenSettings)
                Spacer(Modifier.height(26.dp))
                Text(
                    text = stringResource(R.string.home_hero_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.home_hero_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))
                SectionLabel(stringResource(R.string.home_studios_label))
                Spacer(Modifier.height(12.dp))
                StudioGrid(onOpenStudio = onOpenStudio)
                Spacer(Modifier.height(16.dp))
                ProjectsEntry(onClick = onOpenProjects)
                Spacer(Modifier.height(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(
                        text = stringResource(R.string.home_recent_label),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onOpenExports) {
                        Text(
                            text = stringResource(R.string.action_see_all),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (recent.isEmpty()) {
                    EmptyRecent()
                } else {
                    RecentRow(records = recent.take(8), onOpenExports = onOpenExports)
                }
                Spacer(Modifier.height(40.dp))
                HomeFooter()
            }
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_wordmark_ar),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.app_wordmark_en),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                // 48 dp minimum touch target for the only global action on Home.
                .size(48.dp)
                .clip(CircleShape)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                    CircleShape,
                )
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.action_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Entry point for saved projects. The studios themselves are "start something"; this is
 * "continue something", which is why it sits between the studio grid and recent exports.
 */
@Composable
private fun ProjectsEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            FolderGlyph(color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.projects_library_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_projects_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The material-icons-core artifact has no folder icon; this keeps the bundle unchanged. */
@Composable
private fun FolderGlyph(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val radius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        // Back tab, then the body: two rounded rectangles read as a folder at 22 dp.
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.06f, h * 0.20f),
            size = Size(w * 0.46f, h * 0.26f),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.04f, h * 0.30f),
            size = Size(w * 0.92f, h * 0.56f),
            cornerRadius = radius,
        )
    }
}

@Composable
private fun StudioGrid(onOpenStudio: (StudioKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StudioCard(
                kind = StudioKind.PHOTO,
                title = stringResource(R.string.studio_photo_title),
                subtitle = stringResource(R.string.studio_photo_desc),
                accent = MidadColors.SaffronInk,
                onClick = { onOpenStudio(StudioKind.PHOTO) },
                modifier = Modifier.weight(1f),
            )
            StudioCard(
                kind = StudioKind.VIDEO,
                title = stringResource(R.string.studio_video_title),
                subtitle = stringResource(R.string.studio_video_desc),
                accent = MidadColors.TerracottaInk,
                onClick = { onOpenStudio(StudioKind.VIDEO) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StudioCard(
                kind = StudioKind.DESIGN,
                title = stringResource(R.string.studio_design_title),
                subtitle = stringResource(R.string.studio_design_desc),
                accent = MidadColors.Indigo,
                onClick = { onOpenStudio(StudioKind.DESIGN) },
                modifier = Modifier.weight(1f),
            )
            StudioCard(
                kind = StudioKind.AI,
                title = stringResource(R.string.studio_ai_title),
                subtitle = stringResource(R.string.studio_ai_desc),
                accent = MidadColors.SageInk,
                onClick = { onOpenStudio(StudioKind.AI) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StudioCard(
    kind: StudioKind,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StudioGlyph(kind = kind, color = accent, modifier = Modifier.size(34.dp))
            Spacer(Modifier.weight(1f))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentRow(records: List<ExportRecord>, onOpenExports: () -> Unit) {
    val openLabel = stringResource(R.string.action_open)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(records, key = { it.id }) { record ->
            var bitmap by remember(record.id) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(record.id) {
                bitmap = ProjectStore.thumbnail(record)
            }
            Column(modifier = Modifier.width(112.dp)) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        // A recent tile opens the export library; it no longer fires the
                        // share sheet as a side effect of tapping a thumbnail.
                        .clickable(onClickLabel = openLabel) { onOpenExports() },
                    contentAlignment = Alignment.Center,
                ) {
                    val current = bitmap
                    if (current != null) {
                        Image(
                            bitmap = current.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = stringResource(
                                if (record.kind == ProjectStore.KIND_VIDEO) {
                                    R.string.projects_kind_video
                                } else {
                                    R.string.projects_kind_photo
                                },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (record.kind == ProjectStore.KIND_VIDEO) {
                        stringResource(R.string.projects_kind_video)
                    } else {
                        stringResource(R.string.projects_kind_photo)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyRecent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_recent_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(MidadColors.Sage),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.home_offline_badge),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.home_version_fmt, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeBackdrop() {
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centers = listOf(Offset(width * 1.02f, height * 0.28f))
        val radii = listOf(width * 0.58f, width * 0.70f, width * 0.82f)
        centers.forEach { center ->
            radii.forEach { radius ->
                drawArc(
                    color = lineColor,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 1.2.dp.toPx()),
                )
            }
        }
        val dotColor = lineColor.copy(alpha = 0.55f)
        repeat(6) { column ->
            repeat(4) { row ->
                drawCircle(
                    color = dotColor,
                    radius = 1.3.dp.toPx(),
                    center = Offset(
                        width * 0.06f + column * 16.dp.toPx(),
                        height - 36.dp.toPx() - row * 16.dp.toPx(),
                    ),
                )
            }
        }
    }
}