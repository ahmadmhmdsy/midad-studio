package com.midad.studio.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.midad.studio.BuildConfig
import com.midad.studio.R
import com.midad.studio.core.format.formatBytes
import com.midad.studio.core.prefs.ThemeMode
import com.midad.studio.core.project.ProjectStore
import com.midad.studio.ui.LocalAppState
import com.midad.studio.ui.components.HairlineDivider
import com.midad.studio.ui.components.MidadChoiceChip
import com.midad.studio.ui.components.SectionLabel
import com.midad.studio.ui.components.StudioScaffold
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val appState = LocalAppState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableStateOf(0L) }
    var cacheCleared by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    // Filled once the owner hosts docs/privacy/index.html; Play asks for the policy link
    // both in the listing and inside the app, and an empty value hides the row.
    val privacyUrl = stringResource(R.string.settings_privacy_url).trim()

    LaunchedEffect(Unit) { cacheBytes = ProjectStore.cacheSizeBytes(context) }

    StudioScaffold(title = stringResource(R.string.settings_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_appearance))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    MidadChoiceChip(
                        label = themeLabel(mode),
                        selected = appState.themeMode == mode,
                        onClick = { appState.updateThemeMode(mode) },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            SettingsRow(
                title = stringResource(R.string.settings_models_title),
                subtitle = stringResource(R.string.settings_models_desc),
                onClick = onOpenModels,
            )
            Spacer(Modifier.height(14.dp))
            SectionLabel(stringResource(R.string.settings_storage_title))
            Text(
                text = stringResource(R.string.settings_storage_used_fmt, formatBytes(cacheBytes)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    scope.launch {
                        ProjectStore.clearCache(context)
                        cacheBytes = ProjectStore.cacheSizeBytes(context)
                        cacheCleared = true
                    }
                },
            ) {
                Text(stringResource(R.string.settings_clear_cache))
            }
            if (cacheCleared) {
                Text(
                    text = stringResource(R.string.settings_cache_cleared),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel(stringResource(R.string.settings_legal_title))
            SettingsRow(
                title = stringResource(R.string.settings_licenses),
                onClick = onOpenLicenses,
            )
            SettingsRow(
                title = stringResource(R.string.settings_privacy),
                onClick = { showPrivacy = true },
            )
            Spacer(Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.settings_version_fmt, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.settings_privacy_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(stringResource(R.string.settings_privacy)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_privacy_body))
                    if (privacyUrl.isNotBlank()) {
                        TextButton(
                            onClick = {
                                showPrivacy = false
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            },
                        ) { Text(stringResource(R.string.settings_privacy_online)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "\u203A",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HairlineDivider()
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)
