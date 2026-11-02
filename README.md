<p align="center"><img src="branding/supfy-logo-dark.png" alt="Supfy" width="320"></p>

# Supfy Quantum V5 — Privacy Shield

Supfy, Türkçe ve Immersive görünümü varsayılan kullanan bağımsız Android uygulamasıdır.
Classic görünümü ayarlardan seçilebilir. Spotify ile resmî bir ortaklığı yoktur.

V5'in Android çekirdeği [Myspoti](https://github.com/kota-zo/myspoti)'den türetilmiştir.
Upstream commit: `8e8898af03a21772357ae6248a41a33a28cd9b5e`.
Myspoti, Spotui ve üçüncü taraf telif bildirimleri korunur. Bu türev, tam kaynak
koduyla **GPL-3.0** altında sunulur; [LICENSE](LICENSE),
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) ve
[upstream belgelerine](docs/myspoti-upstream/) bakın.

V5 kaynakları `base/` altındadır. Kökteki eski `app/`, önceki Supfy sürümünün
kurtarma kaynağıdır; V5 APK'sını üretmez. Kökteki Gradle wrapper V5'e yönlendirir.

## Derleme

JDK 17, Android SDK platform `android-37.0`, build-tools 36.0.0 ve internet bağlantısı gerekir.
Gradle wrapper 9.8.2 depodadır. SDK yolunu ortamda `ANDROID_HOME` ile veya
`base/local.properties` içindeki `sdk.dir` ile belirtin; yerel dosyayı yayımlamayın.

```sh
./gradlew :api:test :api-spotify:test :extensions:test :host-android:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
python3 tools/v5_localization_audit.py
python3 tools/v5_security_audit.py --self-test
```

Telefon APK'sı varsayılan olarak ARM64/ARMv7 içerir:
`base/app/build/outputs/apk/debug/app-debug.apk`.
Emülatör için `-Pabis=x86_64` kullanın. Android test paketi:
`base/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

Paket: `com.supfy.app`; varsayılan sürüm: `1.0.0-v5` / `1029`.
Sürüm değişkenleri: `-PappVersionName=... -PappVersionCode=...`.
Üretim release derlemesi yetkili imza anahtarı gerektirir: `SUPFY_KEYSTORE`,
`SUPFY_KEYSTORE_PASSWORD`, `SUPFY_KEY_ALIAS`, `SUPFY_KEY_PASSWORD`.
Anahtarlar ve parolalar kaynak teslimatına dahil edilmez. Debug anahtarıyla
imzalanmış bir APK, farklı imzalı mevcut kuruluma güncelleme olarak yüklenemez.
Uygulamayı kaldırmak kullanıcı verisini siler; imza uyuşmazlığını böyle çözmeyin.

## Gizlilik davranışı

Uygulamaya ait tanı kaydı toplama/dışa aktarma ve yerel tam dinleme/arama geçmişi
kapatılmıştır. Önceki bilinen tanı/geçmiş dosyaları sınırlandırılmış temizlikle
kaldırılır. Oturum, tercihler ve kullanıcının seçtiği çevrimdışı medya korunur.

Şarkı sözü, çeviri, Genius açıklaması ve müzik videosu hizmetleri, sağlayıcı ve
paylaşılacak veriler açıklanıp izin alınana kadar istek göndermez. Ayarlardan
izin kaldırılması sonraki istekleri durdurur; başlamış istekler tamamlanabilir.
Üçüncü taraf Spotify/Android/WebView davranışları uygulamanın kendi kayıtlarından ayrıdır.

Otomatik güncelleme denetimi varsayılan olarak kapalıdır. Kullanıcı açarsa en sık
günde bir denetlenir; elle denetim kullanılabilir. Güncelleyici yalnızca
`ebilgi992/supfy` deposundaki Supfy APK'larını kabul eder; SHA-256, paket,
artan sürüm ve kurulu uygulamanın imzası doğrulanmadan Android yükleyicisine geçmez.
Uzantılar ilk kurulumda kapalıdır ve yeni sürüm/izinler yeniden onay ister.

## Doğrulama ve sınırlar

Güncel build, cihaz ve güvenlik kanıtları [docs/v5/TEST_RESULTS.md](docs/v5/TEST_RESULTS.md)
ve [docs/v5/SECURITY_AUDIT.md](docs/v5/SECURITY_AUDIT.md) içindedir. Kaynak taramasının
başarılı olması, tüm cihazlarda oynatmanın doğrulandığı anlamına gelmez.
Gerçek Spotify oturumu, lisanslı oynatma, Bluetooth ve Android Auto yalnızca
ilgili hesap/cihazla yürütülmüş testler varsa başarılı sayılır.

Eski kaynaklara ve veri geçişine ilişkin belgeler [docs/v5/](docs/v5/) içindedir.
