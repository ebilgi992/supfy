package com.supfy.app.ui.quantum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuantumLoginScreen(
    loading: Boolean,
    error: String?,
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(QuantumPanel, QuantumInk, QuantumInk)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 28.dp),
    ) {
        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(4.dp), color = QuantumGreen, modifier = Modifier.size(8.dp)) {}
                Text("SUPFY", color = QuantumWhite, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            }
            Spacer(Modifier.height(36.dp))
            QuantumRecord(Modifier.widthIn(max = 188.dp).fillMaxWidth().align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(32.dp))
            Text("Müziğin\nseninle.", color = QuantumWhite,
                style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Text("Sevdiğin seslere, kendi ritminde.", color = QuantumMuted,
                style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onLogin, enabled = !loading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QuantumGreen, contentColor = QuantumInk,
                    disabledContainerColor = QuantumPanel, disabledContentColor = QuantumMuted),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
            ) {
                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("E-posta ile giriş yap", modifier = Modifier.padding(start = 10.dp), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSignUp, enabled = !loading, shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = QuantumWhite,
                    disabledContentColor = QuantumMuted),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
            ) {
                Text("Hesap oluştur", textAlign = TextAlign.Center)
            }
            if (loading) {
                Text("Resmi giriş sayfası açılıyor…", color = QuantumMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 14.dp).semantics { liveRegion = LiveRegionMode.Polite })
            }
            if (!error.isNullOrBlank()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp).semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            TextButton(onClick = onContinue,
                modifier = Modifier.align(Alignment.CenterHorizontally).defaultMinSize(minHeight = 48.dp)) {
                Text("Giriş yapmadan devam et", color = QuantumMuted, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = QuantumMuted,
                    modifier = Modifier.padding(top = 2.dp).size(16.dp))
                Text("Giriş ve kayıt, Spotify’ın resmi sayfasında tamamlanır. Supfy şifreni istemez.",
                    color = QuantumMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
