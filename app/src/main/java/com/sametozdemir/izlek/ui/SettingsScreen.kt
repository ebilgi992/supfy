package com.sametozdemir.izlek.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sametozdemir.izlek.BuildConfig
import com.sametozdemir.izlek.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    backgroundPlayback: Boolean,
    desktopMode: Boolean,
    webViewVersion: String?,
    onBackgroundPlaybackChange: (Boolean) -> Unit,
    onDesktopModeChange: (Boolean) -> Unit,
    onClearData: () -> Unit,
    onClose: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionCard(stringResource(R.string.section_playback)) {
                SwitchRow(
                    title = stringResource(R.string.pref_background_title),
                    description = stringResource(R.string.pref_background_desc),
                    checked = backgroundPlayback,
                    onCheckedChange = onBackgroundPlaybackChange,
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SwitchRow(
                    title = stringResource(R.string.pref_desktop_title),
                    description = stringResource(R.string.pref_desktop_desc),
                    checked = desktopMode,
                    onCheckedChange = onDesktopModeChange,
                )
            }

            SectionCard(stringResource(R.string.section_privacy)) {
                BodyText(stringResource(R.string.privacy_body))
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { confirmClear = true }) {
                    Text(stringResource(R.string.action_clear_data))
                }
            }

            SectionCard(stringResource(R.string.section_permissions)) {
                BodyText(stringResource(R.string.permissions_body))
            }

            SectionCard(stringResource(R.string.section_device)) {
                BodyText(stringResource(R.string.device_model, Build.MANUFACTURER, Build.MODEL))
                BodyText(stringResource(R.string.device_android, Build.VERSION.RELEASE, Build.VERSION.SDK_INT))
                BodyText(stringResource(R.string.device_webview, webViewVersion ?: stringResource(R.string.unknown)))
            }

            SectionCard(stringResource(R.string.section_about)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                BodyText(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
                BodyText(stringResource(R.string.about_developer))
                Spacer(Modifier.height(8.dp))
                BodyText(stringResource(R.string.about_disclaimer))
                Spacer(Modifier.height(8.dp))
                BodyText(stringResource(R.string.about_licenses))
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_confirm_title)) },
            text = { Text(stringResource(R.string.clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearData() }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            BodyText(description)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
