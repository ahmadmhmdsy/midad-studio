package com.midad.studio.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.midad.studio.R
import com.midad.studio.ui.components.HairlineDivider
import com.midad.studio.ui.components.StudioScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LicenseEntry(
    val name: String,
    val license: String,
    val note: String,
    /** Asset under assets/models/, when this row can be opened to read a licence text. */
    val asset: String? = null,
)

private val entries = listOf(
    LicenseEntry("AndroidX / Jetpack", "Apache License 2.0", "Core, lifecycle, activity and Compose libraries."),
    LicenseEntry("Kotlin and Kotlin Coroutines", "Apache License 2.0", "Language runtime and asynchronous plumbing."),
    LicenseEntry("Jetpack Compose", "Apache License 2.0", "Declarative user interface toolkit."),
    LicenseEntry("Jetpack Navigation", "Apache License 2.0", "In-app navigation."),
    LicenseEntry("AndroidX ExifInterface", "Apache License 2.0", "Photo orientation metadata."),
    LicenseEntry("Android Media3 (ExoPlayer and Transformer)", "Apache License 2.0", "Video playback and export pipeline."),
    LicenseEntry(
        "LiteRT",
        "Apache License 2.0",
        "On-device inference runtime for AI models (LiteRT 2.3.2; the maintained TensorFlow Lite runtime).",
        asset = "LiteRT-LICENSE.txt",
    ),
    LicenseEntry(
        "LiteRT — third-party attributions",
        "Apache License 2.0",
        "Third-party notices reproduced verbatim for the LiteRT runtime (Abseil, GPU delegates and others).",
        asset = "LiteRT-THIRD_PARTY_NOTICE.txt",
    ),
    LicenseEntry(
        "MediaPipe Selfie Segmentation",
        "Apache License 2.0",
        "Bundled cutout model by Google LLC; model card and citation in the model credits.",
        asset = "NOTICE.txt",
    ),
    LicenseEntry("MediaPipe Multiclass Segmentation", "Apache License 2.0", "Optional on-device download (SelfieMulticlass 256x256) by Google LLC; model card licensed under Apache 2.0."),
    LicenseEntry("MediaPipe Hair Segmentation", "Apache License 2.0", "Optional on-device download by Google (2019). Citation: Tkachenka et al., Real-time Hair segmentation and recoloring on Mobile GPUs, CVPR Workshop 2019."),
    LicenseEntry(
        "MediaPipe DeepLab-V3",
        "Apache License 2.0",
        "Optional general-object segmenter (257x257, 21 classes) by Google LLC; no MediaPipe model card, Apache-2.0 via the Kaggle Models API and the official LiteRT mirror card.",
        asset = "NOTICE.txt",
    ),
    LicenseEntry(
        "MODNet portrait matting",
        "Apache License 2.0",
        "Optional soft-matte portrait model by Zhanghan Ke, Jiayu Sun, Kaican Li, Qiong Yan and Rynson W.H. Lau (AAAI 2022); LiteRT conversion by litert-community, a modified derivative distributed under Apache-2.0.",
        asset = "NOTICE.txt",
    ),
    LicenseEntry(
        "Apache License 2.0 text",
        "Apache License 2.0",
        "The full licence text as distributed with the bundled MediaPipe models.",
        asset = "LICENSE-Apache-2.0.txt",
    ),
    LicenseEntry(
        "Model credits (NOTICE)",
        "Apache License 2.0",
        "Credits, model cards, papers and sources for every bundled and downloadable model.",
        asset = "NOTICE.txt",
    ),
    LicenseEntry("JUnit", "Eclipse Public License 1.0", "Unit tests only; not shipped in the app."),
)

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    var openEntry by remember { mutableStateOf<LicenseEntry?>(null) }

    StudioScaffold(title = stringResource(R.string.licenses_title), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { HairlineDivider() }
            items(entries, key = { it.name }) { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (entry.asset != null) {
                                Modifier.clickable { openEntry = entry }
                            } else {
                                Modifier
                            },
                        )
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        if (entry.asset != null) {
                            Text(
                                text = stringResource(R.string.action_open),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = entry.license,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HairlineDivider()
            }
            item {
                Text(
                    text = stringResource(R.string.licenses_models_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    openEntry?.let { entry ->
        LicenceTextDialog(entry = entry, onDismiss = { openEntry = null })
    }
}

@Composable
private fun LicenceTextDialog(entry: LicenseEntry, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var text by remember(entry.asset) { mutableStateOf<String?>(null) }
    var failed by remember(entry.asset) { mutableStateOf(false) }

    LaunchedEffect(entry.asset) {
        val asset = entry.asset ?: return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("models/$asset").bufferedReader().use { it.readText() }
            }.getOrNull()
        }
        if (loaded == null) failed = true else text = loaded
    }

    val chunks = remember(text) {
        text?.split('\n')?.chunked(160)?.map { it.joinToString("\n") } ?: emptyList()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = entry.license,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        failed -> Text(
                            text = stringResource(R.string.licenses_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        text == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(chunks) { chunk ->
                                Text(
                                    text = chunk,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }
}
