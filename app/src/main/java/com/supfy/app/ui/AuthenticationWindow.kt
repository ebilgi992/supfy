package com.supfy.app.ui

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supfy.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationWindow(web: WebView, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Scaffold(Modifier.fillMaxSize(), topBar = {
            TopAppBar(title = { Text(stringResource(R.string.login_window_title)) }, actions = {
                TextButton(onClick = onClose) { Text(stringResource(R.string.action_close)) }
            })
        }) { padding ->
            AndroidView(factory = { (web.parent as? ViewGroup)?.removeView(web); web },
                modifier = Modifier.padding(padding).fillMaxSize())
        }
    }
}
