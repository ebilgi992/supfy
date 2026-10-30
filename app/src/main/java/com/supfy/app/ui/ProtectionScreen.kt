package com.supfy.app.ui

import androidx.annotation.StringRes
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supfy.app.R
import androidx.compose.ui.text.font.FontFamily
import com.supfy.app.web.DeviceSupport
import com.supfy.app.web.DiagLog
import com.supfy.app.web.Protection
import com.supfy.app.web.ProtectionStatus
import com.supfy.app.web.Protections
import com.supfy.app.web.SessionStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionScreen(
    support: DeviceSupport,
    stats: SessionStats,
    diag: DiagLog,
    desktopMode: Boolean,
    onResetPage: () -> Unit,
    onDisableDesktopMode: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.protection_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
            // Özet
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        stringResource(
                            R.string.protection_summary,
                            Protections.activeCount(support),
                            Protection.entries.size,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.protection_summary_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Section(stringResource(R.string.protection_section_active)) {
                Protection.entries.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    val (title, desc) = texts(p)
                    StatusRow(
                        title = stringResource(title),
                        description = stringResource(desc),
                        active = Protections.statusOf(p, support) == ProtectionStatus.ACTIVE,
                    )
                }
            }

            Section(stringResource(R.string.protection_section_stats)) {
                StatRow(R.string.stat_blocked, stats.blockedNavigations)
                StatRow(R.string.stat_ssl, stats.sslRejected)
                StatRow(R.string.stat_denied, stats.deniedPermissions)
                StatRow(R.string.stat_drm, stats.drmGranted)
                StatRow(R.string.stat_external, stats.externalOpens)
                Spacer(Modifier.height(8.dp))
                Muted(stringResource(R.string.stat_note))
            }

            Section(stringResource(R.string.protection_section_trouble)) {
                Muted(stringResource(R.string.trouble_desc))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onResetPage) { Text(stringResource(R.string.action_reset_page)) }
                    if (desktopMode) {
                        OutlinedButton(onClick = onDisableDesktopMode) {
                            Text(stringResource(R.string.action_desktop_off))
                        }
                    }
                }
            }

            Section(stringResource(R.string.diag_title)) {
                Muted(stringResource(R.string.diag_desc))
                Spacer(Modifier.height(12.dp))
                if (diag.events.isEmpty()) {
                    Muted(stringResource(R.string.diag_empty))
                } else {
                    diag.events.takeLast(15).reversed().forEach { e ->
                        Text(
                            "${e.kind.name} ${e.detail}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (e.kind.name in ERROR_KINDS) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onCopyDiagnostics) { Text(stringResource(R.string.diag_copy)) }
            }
        }
    }
}

private val ERROR_KINDS = setOf("NET_ERROR", "HTTP_ERROR", "BLOCKED", "SSL", "CONSOLE", "RENDERER")

private fun texts(p: Protection): Pair<Int, Int> = when (p) {
    Protection.HTTPS_ONLY -> R.string.prot_https_title to R.string.prot_https_desc
    Protection.DOMAIN_ALLOWLIST -> R.string.prot_allowlist_title to R.string.prot_allowlist_desc
    Protection.SAFE_BROWSING -> R.string.prot_safebrowsing_title to R.string.prot_safebrowsing_desc
    Protection.DRM_GATE -> R.string.prot_drm_title to R.string.prot_drm_desc
    Protection.NO_JS_BRIDGE -> R.string.prot_bridge_title to R.string.prot_bridge_desc
    Protection.FILE_ACCESS_OFF -> R.string.prot_files_title to R.string.prot_files_desc
    Protection.SERVICE_WORKER_HARDENING -> R.string.prot_sw_title to R.string.prot_sw_desc
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
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
private fun StatusRow(
    title: String,
    description: String,
    active: Boolean,
) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Muted(description)
        }
        Spacer(Modifier.width(12.dp))
        StatusChip(active)
    }
}

@Composable
private fun StatusChip(active: Boolean) {
    val bg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg: Color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(shape = CircleShape, color = bg) {
        Text(
            stringResource(if (active) R.string.status_active else R.string.status_unsupported),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun StatRow(@StringRes label: Int, value: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
