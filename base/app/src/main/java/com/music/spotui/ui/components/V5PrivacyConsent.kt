package com.music.spotui.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.music.spotui.privacy.ConsentPolicy
import com.music.spotui.privacy.ConsentPolicy.Feature
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone

private fun Feature.titleResource(): Int = when (this) {
    Feature.LYRICS -> R.string.v5_privacy_lyrics
    Feature.TRANSLATION -> R.string.v5_privacy_translation
    Feature.GENIUS_MEANING -> R.string.v5_privacy_meaning
    Feature.MUSIC_VIDEO -> R.string.v5_privacy_video
}
private fun Feature.disclosureResource(): Int = when (this) {
    Feature.LYRICS -> R.string.v5_privacy_lyrics_disclosure
    Feature.TRANSLATION -> R.string.v5_privacy_translation_disclosure
    Feature.GENIUS_MEANING -> R.string.v5_privacy_meaning_disclosure
    Feature.MUSIC_VIDEO -> R.string.v5_privacy_video_disclosure
}

/** One host at the app root; accepting resumes the operation waiting for this disclosure. */
@Composable
fun V5PrivacyConsentHost() {
    val pending by ConsentPolicy.pending.collectAsState()
    val feature = pending ?: return
    AlertDialog(
        onDismissRequest = { ConsentPolicy.dismiss(feature) },
        title = { Text(stringResource(R.string.v5_privacy_consent_title, stringResource(feature.titleResource()))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(feature.disclosureResource()))
                Text(stringResource(R.string.v5_privacy_network_notice), Modifier.padding(top = 12.dp))
                Text(stringResource(R.string.v5_privacy_revoke_notice), Modifier.padding(top = 12.dp))
            }
        },
        confirmButton = { TextButton(onClick = { ConsentPolicy.grant(feature) }) { Text(stringResource(R.string.v5_privacy_allow)) } },
        dismissButton = { TextButton(onClick = { ConsentPolicy.dismiss(feature) }) { Text(stringResource(R.string.v5_privacy_decline)) } },
    )
}

/** Optional service controls shared by both settings layouts. Enabling always asks first. */
@Composable
fun V5PrivacySettings() {
    val grants by ConsentPolicy.grantedFeatures.collectAsState()
    val background by app.myspoti.SupfyDefaults.backgroundAllowed.collectAsState()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = tone(0xFF202020)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.v5_privacy_settings_title), color = AppText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(stringResource(R.string.v5_privacy_settings_detail), color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(stringResource(R.string.v5_background_playback),color=AppText,modifier=Modifier.weight(1f))
                Switch(checked=background,onCheckedChange=app.myspoti.SupfyDefaults::setBackgroundAllowed)
            }
            Feature.entries.forEach { feature ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(feature.titleResource()), color = AppText, modifier = Modifier.weight(1f))
                    Switch(
                        checked = feature in grants,
                        onCheckedChange = { enabled -> if (enabled) ConsentPolicy.requestConsent(feature) else ConsentPolicy.revoke(feature) },
                    )
                }
            }
            Text(stringResource(R.string.v5_privacy_revoke_notice), color = AppMuted, fontSize = 12.sp)
        }
    }
}
