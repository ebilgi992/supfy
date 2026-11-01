package com.supfy.app.ui.quantum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.supfy.app.playback.PlaybackSnapshot

/** Visible only after the real player reports available media. */
@Composable
fun QuantumMiniPlayer(snapshot: PlaybackSnapshot, onExpand: () -> Unit, onPlayPause: () -> Unit) {
    if (!snapshot.available) return
    val media = snapshot.bounded()
    val title = media.title.ifBlank { "Parça bilgisi yok" }
    Surface(
        color = QuantumPanel,
        contentColor = QuantumWhite,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 76.dp)
                        .clickable(role = Role.Button, onClickLabel = "Tam oynatıcıyı aç", onClick = onExpand)
                        .padding(12.dp),
                ) {
                    QuantumRecord(Modifier.size(48.dp), quantumAccent(title), compact = true)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        Text(media.artist.ifBlank { "Sanatçı bilgisi yok" },
                            color = QuantumMuted, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onPlayPause, enabled = media.canPlayPause, modifier = Modifier.size(48.dp)) {
                    Icon(if (media.playing) QuantumIcons.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (media.playing) "Duraklat" else "Oynat",
                        tint = if (media.canPlayPause) QuantumGreen else QuantumMuted.copy(alpha = .4f),
                        modifier = Modifier.size(28.dp))
                }
            }
            if (media.durationMs > 0) {
                LinearProgressIndicator(
                    progress = { media.positionMs.toFloat() / media.durationMs },
                    color = QuantumGreen,
                    trackColor = QuantumGreen.copy(alpha = .08f),
                    modifier = Modifier.fillMaxWidth().height(2.dp)
                        .semantics { contentDescription = "Çalma ilerlemesi" },
                )
            }
        }
    }
}

@Composable
fun QuantumNowPlaying(
    snapshot: PlaybackSnapshot,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onQueue: () -> Unit,
) {
    val media = snapshot.bounded()
    val accent = quantumAccent(media.title)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(QuantumInk, accent, .18f), QuantumInk, QuantumInk)))
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Oynatıcıyı küçült", tint = QuantumWhite)
            }
            Text("ŞİMDİ ÇALIYOR", color = QuantumMuted,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f).semantics { heading() })
            Box(Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        QuantumRecord(Modifier.widthIn(max = 360.dp).fillMaxWidth(), accent)
        Spacer(Modifier.height(12.dp))
        Text("SUPFY OYNATICI", color = QuantumMuted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(28.dp))
        if (media.available) {
            Text(media.title.ifBlank { "Parça bilgisi yok" },
                color = QuantumWhite, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth().semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text(media.artist.ifBlank { "Sanatçı bilgisi yok" }, color = QuantumMuted,
                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(28.dp))
            if (media.durationMs > 0) {
                LinearProgressIndicator(
                    progress = { media.positionMs.toFloat() / media.durationMs }, color = accent,
                    trackColor = QuantumMuted.copy(alpha = .16f), modifier = Modifier.fillMaxWidth()
                        .semantics { contentDescription = "Çalma ilerlemesi; ileri sarma kullanılamıyor" },
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(playbackTime(media.positionMs), color = QuantumMuted, style = MaterialTheme.typography.labelSmall)
                    Text(playbackTime(media.durationMs), color = QuantumMuted, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text("Süre bilgisi yok", color = QuantumMuted, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(onClick = onPrevious, enabled = media.previous, modifier = Modifier.size(56.dp)) {
                    Icon(QuantumIcons.Previous, contentDescription = "Önceki parça",
                        tint = if (media.previous) QuantumWhite else QuantumMuted.copy(alpha = .3f),
                        modifier = Modifier.size(30.dp))
                }
                Button(
                    onClick = onPlayPause, enabled = media.canPlayPause, shape = CircleShape,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = QuantumInk,
                        disabledContainerColor = QuantumPanel, disabledContentColor = QuantumMuted),
                    modifier = Modifier.size(80.dp),
                ) {
                    Icon(if (media.playing) QuantumIcons.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (media.playing) "Duraklat" else "Oynat",
                        modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = onNext, enabled = media.next, modifier = Modifier.size(56.dp)) {
                    Icon(QuantumIcons.Next, contentDescription = "Sonraki parça",
                        tint = if (media.next) QuantumWhite else QuantumMuted.copy(alpha = .3f),
                        modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onQueue, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Icon(QuantumIcons.Queue, contentDescription = null, tint = QuantumWhite)
                Text("Çalma sırasını aç", color = QuantumWhite, modifier = Modifier.padding(start = 10.dp))
            }
        } else {
            Text("Müzik seni bekliyor", style = MaterialTheme.typography.headlineSmall,
                color = QuantumWhite, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Bir parça başlattığında çalma bilgileri ve desteklenen kontroller burada görünür.",
                color = QuantumMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = QuantumGreen,
                contentColor = QuantumInk), modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text("Müziğe dön")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun playbackTime(timeMs: Long): String {
    val seconds = timeMs.coerceAtLeast(0) / 1000
    val minutes = seconds / 60
    return "$minutes:${(seconds % 60).toString().padStart(2, '0')}"
}
