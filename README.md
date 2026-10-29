<p align="center"><img src="branding/supfy-logo-dark.png" alt="Supfy" width="320"></p>

# Supfy 0.5.0-v10

Mevcut Supfy'nin `test-1018` kaynakları üzerinde Kotlin + Compose + Android WebView geliştirmesi.
`com.supfy.app` kimliği korunur. Yerel sürüm kodu **1019**; minSdk 26, targetSdk 35.
Bu sürüm **deneysel koruma** içerir; doğrulanmış reklamsız Spotify ürünü değildir.

- Ana anahtar, dört özellik tercihi, modül durumu, gerçek sayaçlar ve son 40 olay koruma ekranına bağlıdır.
- Açık reklam DOM işaretlerinde yalnızca HTML medya öğesinin sesi susturulur; reklam sonunda geri yüklenir.
- Bilinen reklam/tanıtım bannerları geri alınabilir biçimde gizlenir. Genel giriş/izin/ödeme pencereleri gizlenmez.
- İsteğe bağlı ağ filtresi bilinen üçüncü taraf reklam alanlarının GET alt istekleriyle sınırlıdır.
  Spotify oturum, ses, spclient ve Widevine lisans isteklerine müdahale edilmez.
- Koruma varsayılan olarak kapalıdır. Ayarlar cihazda saklanır; hata durumunda müdahaleler durur.
- Giriş için kullanıcı hareketiyle açılan Spotify pencereleri aynı WebView'e yönlendirilir; çerezler korunur.
- “Tarayıcıda Aç” düğmesi kaldırıldı. Gereken güvenli dış bağlantı ve Release sayfası açma akışı korunur.
- Mevcut GitHub Releases güncelleyicisi korunur; yalnızca `build-<versionCode>` sürümleri önerilir.

## Derleme ve test

JDK 17, Android SDK platform 35 ve build tools gerekir:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
cd tools
npm ci --ignore-scripts
npm test
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Instrumentasyon APK:
`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
DOM ve Android fixture testleri gerçek Spotify reklamı/oturumu/DRM doğrulaması değildir.
Güncel ölçümler: [test raporu](docs/TEST_RAPORU_V10.md).

## Kurulum ve imza

Gönderilen eski APK'nın sertifikası ile bu ortamda üretilen debug sertifikası farklıdır.
Bu APK mevcut 1018 kurulumunun üzerine **kurulamaz**. Eski uygulamayı kaldırmak oturum ve site
verilerini siler; bu çalışma sırasında kaldırma yapılmadı. Güncelleme için aynı özel imzalama
anahtarıyla yeniden derleme gerekir. Anahtar APK'dan geri çıkarılamaz.

Kalıcı imza desteği korunur: `SUPFY_KEYSTORE_PATH`, `SUPFY_KEYSTORE_PASSWORD`,
`SUPFY_KEY_ALIAS`, `SUPFY_KEY_PASSWORD` güvenli derleme ortamından sağlanır. Anahtarlar depoya,
rapora veya teslim paketine girmez. CI sürüm kodu `2000 + GITHUB_RUN_NUMBER` kullanır.
Kalıcı imza tanımlı olmadan üretilen debug APK yalnızca test içindir.

Boş bir test cihazında APK'yı açıp Android kurulum istemini izleyin. Mevcut Supfy kuruluysa
[kurulum notlarını](docs/KURULUM_V10.md) önce okuyun. Güncelleme dosyasının son paket ve imza
kontrolünü Android Package Installer yapar; Supfy kendi kendine APK kurmaz.

## Güvenlik ve sınırlar

HTTPS, sertifika hatasında iptal, Safe Browsing, dosya/içerik erişiminin kapatılması,
üçüncü taraf çerezlerin kapalı olması ve yalnızca oynatıcıya verilen korumalı medya izni korunur.
`addJavascriptInterface` yoktur; yerel kod yalnızca oynatıcıdan sınırlı sayısal durum alır.
Şifre/çerez/belirteçler okunmaz veya dışarı aktarılmaz. Günlükte URL veya kişisel veri yoktur.

Tam şarkı oynatma Spotify hesabının haklarına, güncel WebView'e, EME/Widevine'e ve Spotify
uyumluluğuna bağlıdır. DRM veya giriş korumaları aşılmaz. Google/Apple/Facebook gömülü girişi
sağlayıcı tarafından reddedilebilir; e-posta/kod ile Spotify'ın kendi girişini kullanın.
WebView dışında üretilen veya DOM'da görünmeyen medya susturulamayabilir. WebSocket,
SW önbelleği ve genel ağ trafiği filtrelenmez. DRM uygunluğu EME varlığından çıkarılamaz.
Kilit ekranı MediaSession kontrolleri ve kesintisiz HyperOS arka plan hizmeti eklenmemiştir.

Reklam engelleme Spotify koşullarına aykırı olabilir; hesap kısıtlaması/kapatılma riski vardır.
Xiaomi HyperOS'ta arka plan çalışması işletim sistemi politikalarına bağlıdır; pil kısıtlaması
kullanıcı tarafından değiştirilebilir. Gerçek cihaz ölçümü olmadan pil/oynatma garantisi verilmez.

## Belgeler ve lisans

[Güvenlik raporu](docs/GUVENLIK_DENETIMI.md) · [Özellik matrisi](docs/OZELLIK_MATRISI.md) ·
[Kaynak araştırması](docs/KAYNAK_ARASTIRMASI_V10.md) · [Kurulum](docs/KURULUM_V10.md).

Supfy bağımsızdır; Spotify ile bağlantılı veya onaylı değildir. Spotify, Spotify AB'nin markasıdır.
[MIT](LICENSE) lisansı korunur. GPL myspoti ve erişilemeyen Spotilol kaynak kodu alınmadı.
[Üçüncü taraf bildirimleri](app/src/main/assets/THIRD_PARTY_NOTICES.txt) kaynak ve APK içindedir.
