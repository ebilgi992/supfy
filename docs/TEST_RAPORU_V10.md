# V10 test raporu — 29 Ekim 2026

Kaynak: `ebilgi992/supfy`, `test-1018` / `8690ab2`. Yerel dal: `codex/quantum-v10-apk`.
Eski APK 0.4.0 / 1018 ile kaynak etiketi eşleştirildi. Yeni sürüm 0.5.0-v10 / 1019.

## Otomatik kontroller

- Temiz başlangıç: 20 JVM testi, lintDebug ve assembleDebug başarılı.
- V10: 31 JVM testi başarılı (URL/DRM kaynak sınırları, güncelleme seçimi/sıklığı,
  koruma durumu, gerçek sayaçlar, kapama, hata sonrası kurtarma, hatalı veri ve ağ filtresi).
- DOM: 22 jsdom testi başarılı. Açık reklam tespiti, müzik başlığından yanlış pozitif,
  önceden sessiz medya, detach, banner stili, genel giriş modalları, idempotence,
  pagehide, EME/güvenli kaynak sınırı, kullanıcı unmute tercihi, hata ve watchdog.
- Derleme komutu: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`.
- DOM komutu: `cd tools && npm ci --ignore-scripts && npm test`.
- İlk RED denemelerinde modül yokluğu, playing durumu ve kapat/aç tarihsel hata testleri
  başarısızdı; son JVM/DOM çalıştırmasında başarısız test yoktur.

Bu testler gerçek Spotify reklamı, gerçek oturum veya DRM oynatması değildir.

## Android emülatörü ve kurulum

Android 15 / API 35, AOSP x86_64, WebView 124.0.6367.219, KVM yok, TCG yazılım emülasyonu.
Emülatör ayrı yerel Docker konteynerinde çalıştırıldı. İlk açılış yaklaşık 11 dakika sürdü.
Xiaomi 14T Pro veya başka bir fiziksel cihazda test yapılmadı.

- Gönderilen özgün 1018 APK emülatöre kuruldu.
- V10 üzerine kurulum **INSTALL_FAILED_UPDATE_INCOMPATIBLE** ile reddedildi: imzalar farklı.
  Bu beklenen güvenlik davranışıdır; üzerine kurulum uyumluluğu sağlanmış değildir.
- Boş sentetik emülatörde eski test kurulumu kaldırıldı; kullanıcı cihazına dokunulmadı.
- test-1018 kaynakları yerel test sertifikasıyla 1018 olarak tekrar derlendi. Sentetik özel
  SharedPreferences dosyası yazıldı ve güncelleme öncesi okundu. Aynı sertifikalı V10 1019
  `adb install -r` ile kuruldu; **aynı sentetik veri güncelleme sonrasında korundu**.
  Bu, gönderilen özgün APK ile imza eşleşmesi kanıtı değildir.
- `am start -W` Status: ok döndürdü; ancak süreç çıkış kayıtlarında başlangıç ANR'ları görüldü.
  Emülatörün telefon/Bluetooth hizmetleri ve sistem arayüzü de yanıt vermeme hataları verdi.
  Bu nedenle ilk açılış/çökmesizlik kararlı biçimde geçmiş sayılmadı.
- Android instrumentasyon denemeleri aşağıda son durumuyla raporlanır. İlk deneme startup
  ANR ile `Process crashed`, sonraki fixture denemesi `JS timed out` sonucu verdi.
  Bu başarısızlıklar saklanmadı ve gerçek cihaz başarısı olarak sunulmadı.

## Zorunlu senaryolar

| # | Senaryo | Kanıt / durum |
|---|---|---|
| 1 | İlk açılış | Emülatör başlatma komutu başarılı; startup ANR gözlendi, kararlılık doğrulanmadı |
| 2 | Spotify oturum açma | Kaynak/URL/pencere akışı incelendi; gerçek hesapla giriş ve dönüş doğrulanmadı |
| 3 | Oturumun korunması | Çerez akışı korunur; gerçek Spotify oturumu testi yapılmadı |
| 4 | Normal oynatma | DOM play/pause durum simülasyonu geçti; gerçek şarkı/DRM testi yapılmadı |
| 5 | Şarkı değiştirme | Medya elemanı ekleme/çıkarma simülasyonu geçti; gerçek Spotify geçişi yapılmadı |
| 6 | Korumayı açma | Kotlin/DOM geçti; Android fixture sonucu aşağıda |
| 7 | Korumayı kapatma | Ses/stil geri alma Kotlin/DOM geçti; Android fixture sonucu aşağıda |
| 8 | Yanlış pozitif | Advertisement, Spotify, Reklam adlı şarkılar susturulmuyor; gizli işaretler yok sayılıyor |
| 9 | Reklam yönetimi | 22 DOM testi geçti; gerçek reklam olayları doğrulanmadı |
| 10 | İnternet kesilmesi | Emülatör uçak moduna alındı; oynatma/yeniden bağlanma uçtan uca doğrulanmadı |
| 11 | Ekran kilitleme | Gerçek cihaz testi bekliyor |
| 12 | Arka plana geçiş | Kaynak ve lifecycle incelemesi; gerçek uzun süreli oynatma/pil testi bekliyor |
| 13 | Çökme kontrolü | JVM/DOM hataları kontrol edildi; emülatör startup ANR var, çökmesizlik iddiası yok |
| 14 | Üzerine güncelleme | Aynı yerel test sertifikasında veri korundu; özgün APK imzası farklı olduğu için reddedildi |
| 15 | Sürüm kontrolü | Release seçimi, URL, sürüm, aralık/erteleme birim testleri geçti; yeni Release yayımlanmadı |

## Giriş ve tam oynatma değerlendirmesi

Kaynakta JavaScript ve DOM Storage açık, CookieManager birinci taraf çerezleri kabul ediyor;
accounts.spotify.com/open.spotify.com güvenli listede. Üçüncü taraf çerezleri kapalı bırakıldı.
Kullanıcı pencerelerinin kapalı olması incelenebilir bir giriş akışı eksikliğiydi; Spotify'a ait
kullanıcı pencereleri aynı WebView'e yönlendirilir ve çerezler sayfa bitiminde flush edilir.
Bunun kullanıcının bildirdiği tüm giriş hatalarını giderdiği **doğrulanmadı**.

EME API, korumalı medya izni ve gerçek DRM/hesap yetkisi ayrıdır. Tam şarkı oynatma ve
abonelik hakları aşılmaz. Sağlayıcıların gömülü giriş sınırlaması veya desteklenmeyen DRM,
başarılı oynatma olarak gösterilmez. Google/Apple/Facebook dönüşü doğrulanmadı.

## Bağımsız inceleme

Önemli bulgular: geçici WebView renderer kaybı ve görülmemiş tarihsel JS hatası sonrası kapat/aç.
Oynatma tanısının gezinme/kapamada eski kalması da ele alındı. Sonuçlar ve son APK doğrulaması
son durum bölümünde bulunur. Gerçek Spotify ve imza devamlılığı kaynak incelemesinden
çıkarılmadı; ayrıca deney/artefakt kanıtı arandı.

## Son durum

- Son derleme: **BUILD SUCCESSFUL**, 1m02s; 31 JVM testi, 0 başarısız / 0 hata.
- Son DOM çalıştırması: **22/22 başarılı**.
- Son Lint: **0 hata, 31 uyarı**. Uyarılar ağırlıkla eski bağımlılık/AGP sürüm
  önerileri, iki kullanılmayan renk ve eski kaynak klasörü/manifest niteliği hakkındadır.
  Uyarılar gizlenmedi; bağımlılıklar topluca değiştirilmedi.
- Debug ve androidTest APK'ları derlendi. Android fixture testleri **başarılı sayılmadı**:
  `popupRendererLossIsHandledWithoutKillingActivity` denemeleri startup ANR, JS zaman aşımı
  ve fixture yüklenmedi hatasıyla sonlandı; beklenen callback kontrolüne ulaşılamadı.
  `realWebViewMutesFixtureAndRestoresOnDisable` çalıştırılmadı. Fixture historyUrl düzeltmesi
  ve popup renderer temizliği son APK/test APK'sında derlendi; cihazda yeniden doğrulanmadı.
- Emülatör sistem ANR'ları nedeniyle durduruldu. Son Gradle süreci araç oturumu kurtarması
  sırasında kesilince aynı kontroller önbellekle tekrar çalıştırıldı ve yukarıdaki başarı alındı.
- APK: `Supfy-0.5.0-v10-1019-debug.apk`, **9006476 bayt**, `com.supfy.app`, 1019 / 0.5.0-v10.
- APK SHA256: `41a2e2573250692f8bab5bab821965bc38de80df2c632d5d646c66402ae5610a`.
- ZIP CRC: sağlam; assets/protection.js kaynak dosyayla birebir aynı.
- apksigner: geçerli, **v2 imza**, tek RSA 2048 bit test sertifikası.
- zipalign `-c -P 16 4`: geçti. Dört ABI'nin native PT_LOAD segment hizası 16384 bayt.
  Bu, 16 KiB sayfalı gerçek cihazda çalıştırma testi değildir.
- İzinler INTERNET ve ACCESS_NETWORK_STATE; JS köprüsü, HTTP izni, gizli anahtar veya
  geniş Android medya/kurulum/erişilebilirlik yetkisi eklenmedi.

**Kabul durumu:** Kod ve gerçek APK teslim edildi. Canlı Spotify reklam engelleme,
  giriş/oturum, tam DRM oynatma ve Xiaomi kararlılığı doğrulanmadığı için bu sürüm,
  tamamlanmış/üretime hazır bir AdBlock ürünü ilan edilmez. Eski APK'nın üzerine veri
  koruyarak kurulum için özgün imzalama anahtarı ayrıca gereklidir.
