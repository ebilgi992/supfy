package com.sametozdemir.supfy.ui

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sametozdemir.supfy.R
import com.sametozdemir.supfy.web.PageError
import com.sametozdemir.supfy.web.PageErrorKind
import com.sametozdemir.supfy.web.WebUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupfyScreen(
    webView: WebView?,
    state: WebUiState,
    online: Boolean,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onOpenBrowser: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SupfyTopBar(
                online = online,
                progress = state.progress,
                onRefresh = onRefresh,
                onHome = onHome,
                onOpenBrowser = onOpenBrowser,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize(),
        ) {
            if (webView != null) {
                key(webView) {
                    AndroidView(
                        factory = {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            AnimatedVisibility(
                visible = !online,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.offline_banner),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                state.error?.let { ErrorOverlay(it, onRetry, onOpenBrowser) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupfyTopBar(
    online: Boolean,
    progress: Int,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_supfy_mark),
                        contentDescription = stringResource(R.string.cd_logo),
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    StatusChip(online)
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_home)) },
                            leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            onClick = { menuOpen = false; onHome() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_open_browser)) },
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            onClick = { menuOpen = false; onOpenBrowser() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_settings)) },
                            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            onClick = { menuOpen = false; onOpenSettings() },
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.background,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(online: Boolean) {
    val color = if (online) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(if (online) R.string.status_online else R.string.status_offline),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorOverlay(error: PageError, onRetry: () -> Unit, onOpenBrowser: () -> Unit) {
    val message = when (error.kind) {
        PageErrorKind.NO_CONNECTION -> R.string.error_no_connection
        PageErrorKind.TIMEOUT -> R.string.error_timeout
        PageErrorKind.SSL -> R.string.error_ssl
        PageErrorKind.GENERIC -> R.string.error_generic
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
                Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onOpenBrowser) { Text(stringResource(R.string.action_open_browser)) }
                    Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                }
            }
        }
    }
}
