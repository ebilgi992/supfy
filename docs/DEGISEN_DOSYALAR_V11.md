# V11 değişen dosyalar

Temel: V10 `e08f049`. Dal: `codex/quantum-v11`. Mevcut kaynaklar üzerinde devam edildi.

| Dosya | Değişiklik |
|---|---|
| `README.md` | V11 kapsamı, test sonucu, imza ve doğrulanmamış davranışlar. |
| `app/build.gradle.kts` | 0.6.0-v11 / 1020 sürümü. |
| `app/src/androidTest/java/com/supfy/app/WebViewProtectionTest.kt` | Yerel fixture, popup POST, request gate, tercih ve reklamsız WebView izolasyonu. |
| `app/src/debug/AndroidManifest.xml` | Debug test Activity dışa kapalı. |
| `app/src/debug/java/com/supfy/app/testing/WebViewTestActivity.kt` | Debug kaynak kümesinde test yüzeyi; release dışında. |
| `app/src/main/assets/protection.js` | Çoklu kanıt, normal müzik veto, bileşen hatası, CSS görünürlüğü ve v2 tanılama. |
| `app/src/main/java/com/supfy/app/MainActivity.kt` | Giriş penceresi, menü bağlantısı, ses/renderer kurtarma ve güvenli yaşam döngüsü. |
| `app/src/main/java/com/supfy/app/data/AppSettings.kt` | Yeni tercihler için koyu tema/masaüstü; mevcut ayarları koruma. |
| `app/src/main/java/com/supfy/app/protection/AdAudioSession.kt` | Bağımsız ses oturumu ve hata sonrası geri alma denetleyicisi. |
| `app/src/main/java/com/supfy/app/protection/ProtectionEngine.kt` | Ayrı bileşen durumları, native ses/müzik dönüşü sayımları ve hata durumu. |
| `app/src/main/java/com/supfy/app/protection/ProtectionWebController.kt` | Document-start, native ses, protokol v2, polling, geri alma ve renderer temizliği. |
| `app/src/main/java/com/supfy/app/ui/AuthenticationWindow.kt` | Gerçek login popup belgesini gösteren Compose pencere. |
| `app/src/main/java/com/supfy/app/ui/ProtectionScreen.kt` | Gerçek durumlar/sayaçlar/hatalar ve normal oynatmaya dönüş. |
| `app/src/main/java/com/supfy/app/ui/SupfyScreen.kt` | Spotify giriş menüsü ve desteklenmeyen oynatma bildirimi. |
| `app/src/main/java/com/supfy/app/web/NavigationRequestGate.kt` | POST dahil ana belge istekleri için güvenli köken kapısı. |
| `app/src/main/java/com/supfy/app/web/PlayerNavigation.kt` | Resmî login URL ve sıkı Spotify içerik URI dönüşümü. |
| `app/src/main/java/com/supfy/app/web/SupfyChromeClient.kt` | Güvenli JS/DOM Storage popup, POST koruma, çerezler/SSL/renderer. |
| `app/src/main/java/com/supfy/app/web/SupfyWebViewClient.kt` | İçerik linkleri, ana gezinme güvenliği ve erken sayfa commit. |
| `app/src/main/java/com/supfy/app/web/WebUiState.kt` | Oynatma yönlendirmesi uyarı durumu. |
| `app/src/main/res/values/strings.xml` | Türkçe giriş, koruma, ses, hata ve güncelleme metinleri. |
| `app/src/test/java/com/supfy/app/protection/AdAudioSessionTest.kt` | Otomatik regresyon testleri. |
| `app/src/test/java/com/supfy/app/protection/AudioRecoveryTest.kt` | Otomatik regresyon testleri. |
| `app/src/test/java/com/supfy/app/protection/ProtectionEngineTest.kt` | Otomatik regresyon testleri. |
| `app/src/test/java/com/supfy/app/web/PlayerNavigationTest.kt` | Otomatik regresyon testleri. |
| `docs/DEGISEN_DOSYALAR_V11.md` | Sonuç, araştırma, kurulum veya kanıt belgesi. |
| `docs/KURULUM_V11.md` | Sonuç, araştırma, kurulum veya kanıt belgesi. |
| `docs/SONUC_RAPORU_V11.md` | Sonuç, araştırma, kurulum veya kanıt belgesi. |
| `docs/TEKNIK_RAPOR_V11.md` | Sonuç, araştırma, kurulum veya kanıt belgesi. |
| `docs/V11_CALISMA.md` | Sonuç, araştırma, kurulum veya kanıt belgesi. |
| `tools/protection.test.cjs` | Otomatik regresyon testleri. |
