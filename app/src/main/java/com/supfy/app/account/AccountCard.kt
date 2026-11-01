package com.supfy.app.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AccountCard(
    account:AccountSnapshot,
    onLogin:()->Unit,
    onSignUp:()->Unit,
    onAccountMenu:()->Unit,
    onLogout:()->Unit,
    onRefresh:()->Unit = {},
) {
    Card(modifier=Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.large,
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer,CircleShape),contentAlignment=Alignment.Center) {
                    Icon(Icons.Filled.Person,contentDescription=null,tint=MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text("Spotify hesabın",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
                    Text(when(account.status){
                        AccountStatus.AUTHENTICATED->account.displayName.ifBlank {"Spotify oturumu açık"}
                        AccountStatus.GUEST->"Henüz giriş yapılmadı"
                        AccountStatus.UNKNOWN->"Hesap durumu bekleniyor"
                    },style=MaterialTheme.typography.titleLarge)
                }
            }
            when(account.status) {
                AccountStatus.AUTHENTICATED->{
                    Text("Spotify profilin sayfada görünüyor.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick=onAccountMenu,modifier=Modifier.fillMaxWidth()){Text("Spotify hesap menüsü")}
                    TextButton(onClick=onLogout,modifier=Modifier.fillMaxWidth()){Text("Çıkış yap")}
                }
                AccountStatus.GUEST->{
                    Text("Müziğine kaldığın yerden devam etmek için hesabına giriş yap.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick=onLogin,modifier=Modifier.fillMaxWidth()){Text("Spotify ile giriş yap")}
                    OutlinedButton(onClick=onSignUp,modifier=Modifier.fillMaxWidth()){Text("Spotify hesabı oluştur")}
                }
                AccountStatus.UNKNOWN->{
                    Text("Spotify sayfası açıldığında hesap bilgilerin burada güncellenir.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick=onLogin,modifier=Modifier.fillMaxWidth()){Text("Giriş ekranını aç")}
                    OutlinedButton(onClick=onSignUp,modifier=Modifier.fillMaxWidth()){Text("Spotify hesabı oluştur")}
                    TextButton(onClick=onRefresh,modifier=Modifier.fillMaxWidth()){Text("Hesap durumunu yenile")}
                }
            }
        }
    }
}
