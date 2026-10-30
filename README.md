<p align="center"><img src="branding/supfy-logo-dark.png" alt="Supfy" width="320"></p>

# Supfy 0.6.1-login-fix — test sürümü

Mevcut Supfy'nin `test-1018` kaynakları üzerinde Kotlin + Compose + Android WebView geliştirmesi.
`com.supfy.app` kimliği korunur. Yerel sürüm kodu **1021**; minSdk 26, targetSdk 35.
Bu sürüm **deneysel koruma** içerir; doğrulanmış reklamsız Spotify ürünü değildir. Koyu tema ve telefon görünümü varsayılandır; kullanıcının açıkça seçtiği masaüstü tercihi korunur.

- Ana anahtar, dört özellik tercihi, modül durumu, gerçek sayaçlar ve son 40 olay koruma ekranına bağlıdır.
- Çoklu DOM kanıtıyla reklam sınıflandırılır. DOM medya ve destek varsa yalnızca bu WebView sesine özgü native mute kullanılır; önceki durum korunur.
- Bilinen reklam/tanıtım bannerları geri alınabilir biçimde gizlenir. Genel giriş/izin/ödeme pencereleri gizlenmez.
- İsteğe bağlı ağ filtresi bilinen üçüncü taraf reklam alanlarının GET alt istekleriyle sınırlıdır.
  Spotify oturum, ses, spclient ve Widevine lisans isteklerine müdahale edilmez.
- Koruma varsayılan olarak kapalıdır. Ayarlar cihazda saklanır; hata durumunda müdahaleler durur.
- Spotify içindeki giriş bağlantıları ve Supfy giriş menüsü aynı mobil oturum penceresine yönlendirilir. Gerçek belge, POST ve çerez oturumu korunur; ara /login sayfası oynatıcıya dönüş sayılmaz.
- Boş giriş sayfası, HTTP hatası ve 30 saniyelik yükleme sorunu görünür Türkçe hata/yeniden deneme sunar. Yeni belge commit edilmeden eski sayfa başarı sayılmaz; kapanmış pencerenin geç callback'leri yeni girişe taşınmaz.
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
Güncel sonuçlar ve kurulum: [1021 giriş düzeltmesi](docs/GIRIS_DUZELTMESI_1021.md). Önceki V11 test sonuçları tarihsel kayıttır. Sentetik testler gerçek Spotify hesabı/reklamı/DRM veya fiziksel cihaz doğrulaması değildir.

**Ana kabul kriteri karşılanmadı: canlı Spotify reklam işleme başarısı doğrulanmadı.**
Debug kaynak kümesindeki dışa kapalı WebViewTestActivity sentetik testlere yüzey sağlar; release uygulamasına dahil değildir.

## Kurulum ve imza

Bu düzeltme yerel V11 (1020) test APK'sıyla aynı sertifikayı kullanır. Aynı imzalı V11 üzerine veri silmeden güncelleme hedeflenir.

Gönderilen özgün 1018 APK'nın sertifikası ile bu ortamda üretilen debug sertifikası farklıdır.
Bu APK mevcut 1018 kurulumunun üzerine **kurulamaz**. Eski uygulamayı kaldırmak oturum ve site
verilerini siler; bu çalışma sırasında kaldırma yapılmadı. Güncelleme için aynı özel imzalama
anahtarıyla yeniden derleme gerekir. Anahtar APK'dan geri çıkarılamaz.

Kalıcı imza desteği korunur: `SUPFY_KEYSTORE_PATH`, `SUPFY_KEYSTORE_PASSWORD`,
`SUPFY_KEY_ALIAS`, `SUPFY_KEY_PASSWORD` güvenli derleme ortamından sağlanır. Anahtarlar depoya,
rapora veya teslim paketine girmez. CI sürüm kodu `2000 + GITHUB_RUN_NUMBER` kullanır.
Kalıcı imza tanımlı olmadan üretilen debug APK yalnızca test içindir.

Boş bir test cihazında APK'yı açıp Android kurulum istemini izleyin. Mevcut Supfy kuruluysa
[1021 kurulum notlarını](docs/GIRIS_DUZELTMESI_1021.md) önce okuyun. Güncelleme dosyasının son paket ve imza
kontrolünü Android Package Installer yapar; Supfy kendi kendine APK kurmaz.

## Güvenlik ve sınırlar

HTTPS, sertifika hatasında iptal, Safe Browsing, dosya/içerik erişiminin kapatılması,
üçüncü taraf çerezlerin kapalı olması ve yalnızca oynatıcıya verilen korumalı medya izni korunur.
`addJavascriptInterface` yoktur; oynatıcıdan sınırlı sayısal durum, yükleme denetiminde yalnızca görünür içerik var/yok sonucu alınır. Giriş formunun değerleri veya sayfa metni okunmaz.
Şifre/çerez/belirteçler okunmaz veya dışarı aktarılmaz. Günlükte URL veya kişisel veri yoktur.

Tam şarkı oynatma Spotify hesabının haklarına, güncel WebView'e, EME/Widevine'e ve Spotify
uyumluluğuna bağlıdır. DRM veya giriş korumaları aşılmaz. Google/Apple/Facebook gömülü girişi
sağlayıcı tarafından reddedilebilir; e-posta/kod ile Spotify'ın kendi girişini kullanın.
Native MUTE_AUDIO desteği yoksa DOM dışında üretilen medya susturulamayabilir. Native destek, gerçek reklam sesinin susturulduğu anlamına gelmez. WebSocket,
SW önbelleği ve genel ağ trafiği filtrelenmez. DRM uygunluğu EME varlığından çıkarılamaz.
Kilit ekranı MediaSession kontrolleri ve kesintisiz HyperOS arka plan hizmeti eklenmemiştir.

Reklam engelleme Spotify koşullarına aykırı olabilir; hesap kısıtlaması/kapatılma riski vardır.
Xiaomi HyperOS'ta arka plan çalışması işletim sistemi politikalarına bağlıdır; pil kısıtlaması
kullanıcı tarafından değiştirilebilir. Gerçek cihaz ölçümü olmadan pil/oynatma garantisi verilmez.

## Belgeler ve lisans

[V11 teknik ve güvenlik raporu](docs/TEKNIK_RAPOR_V11.md) · [Değişen dosyalar](docs/DEGISEN_DOSYALAR_V11.md) ·
[V11 sonuçları](docs/SONUC_RAPORU_V11.md) · [Kurulum](docs/KURULUM_V11.md).
V10 belgeleri tarihsel kayıttır; V11 sonuç raporu güncel durumu belirtir.

Supfy bağımsızdır; Spotify ile bağlantılı veya onaylı değildir. Spotify, Spotify AB'nin markasıdır.
[MIT](LICENSE) lisansı korunur. GPL myspoti ve erişilemeyen Spotilol kaynak kodu alınmadı.
[Üçüncü taraf bildirimleri](app/src/main/assets/THIRD_PARTY_NOTICES.txt) kaynak ve APK içindedir.
