# V10 kurulum ve güncelleme

Yeni sürüm: **com.supfy.app / 1019 / 0.5.0-v10**, Android 8.0 ve üzeri.
Bu debug APK test içindir. Üretim dağıtımı yapılmadı; GitHub'a push/release yapılmadı.

## Mevcut 0.4.0-1018 kurulumu varsa

Gönderilen APK sertifikası SHA256:
`02d8c2e96227db320b50b058a7f0f47813789504a6cee040364e20a96f35c7bc`

Bu ortamın debug sertifikası SHA256:
`f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640`

Sertifikalar farklıdır. Android yeni APK'yı eski kurulumun üzerine kabul etmez.
**Uygulamayı kaldırmayın:** kaldırma oturum ve site verilerini siler. Bu işlem kullanıcı
cihazında yapılmadı ve veri kaybına onay verilmiş sayılmadı. İmza uyuşmazlığını bir
versionCode değişikliği, APK yeniden paketleme veya güvenlik ayarıyla güvenli biçimde aşmak yoktur.

Veri koruyan yol: kaynak paketi eski kurulumun **aynı özel anahtarıyla** güvenli derleme
ortamında yeniden imzalamak. Anahtar ve parolaları sohbete/GitHub kaynaklarına koymayın.
Mevcut SUPFY_KEYSTORE_PATH/PASSWORD/KEY_ALIAS/KEY_PASSWORD yapılandırması kullanılabilir.
Eski anahtar geçici CI debug anahtarıysa ve artık yoksa aynı imzayı yeniden üretmek mümkün değildir.

## Boş test cihazı veya emülatör

1. APK'yı cihaza indirin ve dosya yöneticisinden açın.
2. Android isterse yalnızca kullandığınız dosya yöneticisine APK yükleme izni verin.
3. Kurulumdan sonra Supfy'yi açın. Giriş Spotify'ın kendi HTTPS sayfasındadır.
4. Koruma ve durum ekranından isteğe bağlı korumayı açın. Varsayılanı kapalıdır.
5. Modülün yanıt vermesi reklamın engellendiği anlamına gelmez; gerçek sayaç ve günlüğe bakın.
6. Giriş/oynatma etkilenirse korumayı kapatın. “Sayfayı sıfırla” çerezleri silmez.

ADB: boş test cihazında `adb install Supfy-0.5.0-v10-1019-debug.apk`.
Aynı imzalı önceki test kurulumunu veri koruyarak güncellemek için `adb install -r ...`.
Farklı imzada bu işlem başarısız olmalıdır. Kaldırıp yeniden kurmak ayrı ve veri silen bir karardır.

## Giriş ve oynatma sınırları

Spotify e-posta/kod girişini kullanın. Gömülü Google/Apple/Facebook akışları sağlayıcı tarafından
reddedilebilir; güvenlik korumasını aşmayın. Masaüstü sitesi seçeneği korunur; mobil web oynatıcı
sunulmazsa ayarlardan denenebilir. Tam müzik; hesap hakları, DRM ve cihazın WebView desteğine
bağlıdır. Koruma lisans, abonelik veya DRM gereksinimlerini kaldırmaz.

HyperOS arka plan kısıtları cihaz politikasına bağlıdır. İstenirse sistem uygulama pil ayarları
elle değiştirilebilir; Supfy otomatik pil muafiyeti istemez. Ekran kilidi ve arka plan oynatma
gerçek Xiaomi 14T Pro üzerinde ayrıca doğrulanmalıdır.

## Sürüm bildirimi

Uygulama internet geldiğinde en çok 12 saatte bir herkese açık GitHub Releases listesini
kontrol eder. Ayarlardaki elle denetim korunur. Yalnızca kendi deposundaki daha yüksek kodlu
`build-<kod>` kayıtları önerilir; `test-` kayıtları önerilmez. Türkçe bildirimin açtığı Release
sayfasından indirme kullanıcı kararıdır. Android son paket/imza/sürüm doğrulamasını yapar.
