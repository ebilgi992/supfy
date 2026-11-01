package com.supfy.app.ui.quantum

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.supfy.app.BuildConfig
import com.supfy.app.playback.PlaybackHealth
import com.supfy.app.playback.PlaybackServiceHealth
import com.supfy.app.protection.AudioDiagnostics

/** Deliberately limited to integer flags. No media/account text, URL, cookie or form value. */
fun runtimeDiagnosticReport(version:Int,sdk:Int,p:PlaybackHealth,s:PlaybackServiceHealth,a:AudioDiagnostics):String =
    "Supfy runtime diagnostics v1\nversion=$version\nsdk=$sdk\n"+
    "probe=${p.probeCode}\nprobeFlags=${p.probeFlags}\nevidence=${p.evidence}\n"+
    "start=${p.startCode}\nenabled=${p.enabled}\nforeground=${p.foreground}\navailable=${p.available}\nplaying=${p.playing}\n"+
    "service=${s.running}\nserviceForeground=${s.foreground}\nstop=${s.stopCode}\nnotificationFlags=${s.notificationFlags}\n"+
    "adPhase=${a.phase}\nadEvidence=${a.evidence}\nmuteRequested=${a.muteRequested}\nnativeMuted=${a.nativeGetter}\n"+
    "nativeStatus=${a.nativeStatus}\naudioBackend=${a.backend}\nmutedDom=${a.mutedDomCount}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeDiagnosticsScreen(p:PlaybackHealth,s:PlaybackServiceHealth,a:AudioDiagnostics,
    onClose:()->Unit,onCopy:(String)->Unit,onNotificationSettings:()->Unit) {
    Scaffold(topBar={TopAppBar(title={Text("Oynatıcı ve ses")},navigationIcon={
        IconButton(onClick=onClose){Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription="Geri")}
    })}) {padding->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Medya bildirimi",style=MaterialTheme.typography.titleLarge)
                Text(when {
                    s.notificationFlags and 4 != 0->"Oynatıcı bildirim kanalı Android ayarlarında kapalı."
                    s.notificationFlags and 8 != 0->"Medya bildirimi Android'de kayıtlı."
                    p.startCode==7 || s.stopCode==4->"Android medya servisi başlatılamadı."
                    p.enabled==0->"Arka plan oynatıcı tercihi kapalı."
                    p.available==0->"Bu sayfada oynatıcı henüz algılanmadı."
                    p.playing==0->"Oynatıcı duraklatılmış. Çalmayı başlatın."
                    else->"Oynatıcı bulundu; bildirim bekleniyor."
                })
                OutlinedButton(onClick=onNotificationSettings,modifier=Modifier.fillMaxWidth()){Text("Android bildirim ayarları")}
            }}
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Reklam sesi",style=MaterialTheme.typography.titleLarge)
                Text(when {
                    a.muteRequested==0->"Şu anda reklam sesi susturma isteği yok."
                    a.nativeGetter==1->"WebView API'sinin sessiz durumu açık."
                    a.mutedDomCount>0->"Sayfadaki medya öğeleri sessiz durumda."
                    else->"Susturma istendi; uygulanmış sessizlik doğrulanmadı."
                })
                Text("API durumu, hoparlörden çıkan sesin ölçümü değildir.",style=MaterialTheme.typography.bodySmall)
            }}
            Button(onClick={onCopy(runtimeDiagnosticReport(BuildConfig.VERSION_CODE,Build.VERSION.SDK_INT,p,s,a))},
                modifier=Modifier.fillMaxWidth()){Text("Tanıyı kopyala")}
            Text("Tanı yalnızca sayısal durumları içerir; hesap, şarkı, parola veya oturum bilgisi içermez.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
