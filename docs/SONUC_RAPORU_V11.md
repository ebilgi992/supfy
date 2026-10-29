# Supfy V11 sonuç raporu — 29 Ekim 2026

**Teslim türü: deneysel debug APK. Ana kabul kriteri karşılanmadı.** Kullanıcının kendi
hesabıyla gerçek Spotify reklamının gözlemlenip işlendiği kanıtlanamadı. Kullanıcı hesabı,
Xiaomi 14T Pro veya başka bir fiziksel cihaz bağlı değildi. Şifre/token istenmedi;
gerçek hesaba giriş yapılmadı. Reklam görülmemesi başarı kanıtı olarak kullanılmadı.

## 1. Reklam motoru

Gerçek kaynak kod geliştirildi: bağımsız Kotlin durum/istek motoru, document-start JS,
çoklu DOM kanıtı, DOM medya ve feature-gated native WebView mute, geri alınabilir banner/
tanıtım temizleme, GET alt kaynak/SW filtresi, bileşen hataları, tercih/günlük ve gerçek
sayaçlar. Tespit/susturma/gizleme/ağ reddi/müzik dönüşü birbirinden ayrı gösterilir.
Koruma ekranı Kotlin'e bağlıdır; varsayılan kapalıdır. Normal oynatmaya dönüş çerez silmez.

| Kontrol | Sentetik kanıt | Gerçek Spotify / cihaz sonucu |
|---|---|---|
| Motor başlatıldı mı? | Kotlin ve DOM durum testleri geçti | Canlı sayfada doğrulanmadı |
| Reklam olayı algılandı mı? | Açık işaret + medya/oynatma kanıtı ve normal şarkı veto testleri geçti | Gerçek reklam gözlemlenmedi |
| Ağ filtresi uygulandı mı? | Saf Kotlin host/metot/köken kararları geçti | Gerçek WebView fetch fixture başarısız; Spotify ağı doğrulanmadı |
| Ses işlemi gerçekleştirildi mi? | DOM muted/geri alma ve sahte AudioPort testleri geçti | Gerçek Android ses çıkışı veya reklam susturma doğrulanmadı |
| Görsel reklam temizlendi mi? | DOM banner/stil geri alma ve CSS görünürlük testleri geçti | Canlı banner doğrulanmadı |
| Normal müzik devam etti mi? | Normal track + playing dönüş sayacı testleri geçti | Tam şarkı/DRM/şarkı geçişi doğrulanmadı |
| Yanlış pozitif oluştu mu? | Test edilen normal/gizli/adlı şarkı örnekleri engellenmedi | Canlı karşılaştırma yapılmadı; genelleme yok |
| Spotify giriş ekranı çalıştı mı? | URL/popup güvenlik kodu ve anonim HTTP erişimi incelendi | Gerçek WebView giriş ve hesap dönüşü doğrulanmadı |
| Gerçek cihazda doğrulandı mı? | Hayır | Xiaomi/diğer marka testleri bekliyor |

Başarısızlık sonrası tekrarlar: headless fixture yerine dışa kapalı debug Activity yüzeyine
bağlı WebView kullanıldı; history URL/JS sonuç kodlaması düzeltildi. Chromium renderer
kayıpları incelendi; ölü WebView'e JS/ses çağrısı yapan temizlik ayrıldı, motor hatası görünür
hâle getirildi. Bağımsız incelemede CSS class değişiminde eski kalan görünürlük, native ses
geri alma hatasında tekrar denemeyi bırakma ve üst POST gezinme sınırı bulundu; üçü de
kod/test değişikliğiyle ele alındı. DOM CSS regresyon testlerinin önce başarısız, sonra
başarılı olduğu kaydedildi. Android oynatma başarıya dönüştürülemedi; hata gizlenmedi.

## 2. Spotify login

V10 geçici popup'ı kapatıp URL'yi GET olarak yeniden açabiliyordu; JS/DOM Storage ve popup
ömrü de giriş akışını bozabilirdi. Gerçek popup belgesi ve POST korunacak şekilde düzeltildi.
CookieManager/HTTPS/SSL iptali/izin sınırı korunur. Resmî login continue adresi menüye eklendi.
Anonim accounts.spotify.com kontrolü 307→200 verdi; bu WebView veya hesap testi değildir.
Giriş ekranının görünmesi, hesabın kabulü, oynatıcıya dönüş, yeniden açılışta oturum,
çıkış ve tekrar giriş kabul senaryoları gerçek hesapla **geçmedi/doğrulanmadı**.
Harici sağlayıcının koruması aşılmaz; harici tarayıcı oturumu WebView'e kopyalanmaz.

## 3. Oynatma

Geçerli spotify: içerik linkleri kullanıcı hareketiyle HTTPS oynatıcıya çevrilir.
Yeni tercihlerde masaüstü görünümü; mevcut tercihleri koruma ve desteklenmeyen yönlendirme
uyarısı eklendi. Keyfi intent/Spotify uygulamasını zorla açma azaltıldı. Tam şarkı,
EME/Widevine ve hesap hakları doğrulanmadı. Hiçbir DRM/abonelik/giriş koruması aşılmadı.
Kilit/arka plan/HyperOS/pil/bellek ölçümü fiziksel cihazda yapılmadı.

## 4. Derleme, test ve hata kayıtları

Başlangıç V10: testDebugUnitTest, lintDebug, assembleDebug, assembleDebugAndroidTest geçti.
Son kaynakta aynı dört görev: **BUILD SUCCESSFUL, 1m44s**.

- JVM: **38 test, 0 hata, 0 başarısız, 0 atlanan**. Release seçimi/URL/sıklık/erteleme
  için mevcut 9 test dahil; audio recovery ve renderer hata regresyonları eklendi.
- DOM: **29/29 geçti**. jsdom fiziksel WebView/ses değildir. CSSOM testi jsdom önbellek
  sınırı nedeniyle computed-style sınırını taklit eder; gerçek class değişimi ayrı sınanır.
- Lint: **0 hata / 32 uyarı**. Bağımlılık güncelliği, JS gerekliliği, kaynak/manifest
  önerileri sürüyor. Toplu bağımlılık yükseltmesi yapılmadı; rapor teslime dahildir.
- Android API35 AOSP x86_64, WebView 124.0.6367.219, KVM yok/TCG yazılım emülasyonu.
  Activity yüzeyine bağlı ilk tam denemede **7 test / 6 başarısız**; tercih kalıcılığı geçti,
  diğerleri synthetic JavaScript timeout verdi. Önceki headless 4 testte de 3 başarısızdı.
  Bunlar aynı sürümün tekrar denemeleridir; toplu başarı oranı hesaplanmadı.
- Native günlüklerde libwebviewchromium.so alt işleminde **SIGTRAP / exit 5** var.
  Sistem telefon/UI süreçlerinde de sorunlar ve Supfy başlangıç ANR'ı görüldü.
  Stripped native adresinden kesin alt kök neden çıkarılamadı. TLS/Safe Browsing/sandbox
  kapatılmadı. Bu nedenle Android fixture, giriş ve çökmesizlik kabulü başarısız/beklemede.

Son izolasyon/kurulum kanıtı aşağıdaki ek doğrulama bölümünde kaydedilir.
Tam dosya listesi: [DEGISEN_DOSYALAR_V11.md](DEGISEN_DOSYALAR_V11.md).
Kaynak/güvenlik/lisans: [TEKNIK_RAPOR_V11.md](TEKNIK_RAPOR_V11.md).

## 5. APK

| Alan | Değer |
|---|---|
| Dosya | Supfy-0.6.0-v11-1020-debug.apk |
| Tam yol | /workspace/deliverables-v11/Supfy-0.6.0-v11-1020-debug.apk |
| Sürüm | 0.6.0-v11 / 1020 |
| Paket | com.supfy.app |
| İmza | Android Debug / RSA-2048 / APK v2 |
| Boyut | 9.467.122 bayt |
| SHA-256 | 554b159baf9200d761fcdeb3af0367e2c42e96a4564c8eb70d0ed082214cb769 |

apksigner verify, ZIP CRC ve zipalign -c -P 16 4 geçti. Dört ABI native PT_LOAD hizası
16384 bayt. APK'daki protection.js kaynakla birebir; THIRD_PARTY_NOTICES mevcut.
Anahtar dosyası yok. Bu statik kontroller Android çalışma başarısı değildir.
Geçerli özgün release anahtarı bulunmadığından imzalı release teslimi yapılmadı.

**Özgün 1018 APK üzerine kurulum sağlanamadı:** sertifika 02d8…7bc, test APK f0e5…640.
V10 deneyinde INSTALL_FAILED_UPDATE_INCOMPATIBLE ile reddedildi. Mevcut yerel V10 test
APK'sı aynı f0e5…640 anahtarına sahiptir; V11 onunla aynı imzayı korur. Özgün anahtar
olmadan veri koruyan özgün güncelleme üretilemez; kullanıcı uygulaması kaldırılmadı.
[Kurulum ve Xiaomi listesi](KURULUM_V11.md).

## 6. Güvenlik ve kabul durumu

INTERNET/ACCESS_NETWORK_STATE dışında tehlikeli izin eklenmedi. Birleştirilmiş APK'da
AndroidX'in signature seviyesinde DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION izni de vardır.
HTTPS, Safe Browsing ve TLS doğrulaması korunur; bridge/root/VPN/erişilebilirlik/token
çıkarma yoktur. Debuggable test APK üretim dağıtımı için uygun değildir. MIT ve telifler
korundu; GPL veya lisansı belirsiz kod alınmadı. Public push/Release yayımlanmadı.

Kod, otomatik sentetik testler ve gerçek APK teslim edildi. **Canlı reklam işlemesi,
hesap girişi, tam oynatma, fiziksel cihaz kararlılığı ve özgün APK üzerine imzalı güncelleme
eksik olduğundan proje tamamlanmış AdBlock ürünü olarak işaretlenmez.** Reklam engelleme
Spotify koşullarına aykırı olabilir ve hesap kısıtlanması/kapatılması riski taşır.

## Ek doğrulama: son APK ve Android sınır testleri

- Son APK SHA-256 yukarıdaki dosyayla eşleştirildi; 1020 ve 0.6.0-v11 cihazın paket
  yöneticisinden de okundu. `adb install -r` **Success** verdi. İşlem öncesi/sonrası yalnızca
  `shared_prefs/update_fixture.xml` sentetik dosyası okundu; ikisinde de `fixture=retained`.
  V10 test anahtarının V11 ile devamlılığı bu emülatör hattında korunur. Bu son işlem
  1020 test derlemesinden son 1020 APK'ya değişimdi; V10→ilk1020 kurulumları önceki loglarda
  başarılıdır. Özgün 1018 sertifikasıyla eşleşme anlamına gelmez; hesap oturumu sınanmadı.
- Son test APK'sıyla iki Android sınır testi **OK (2 tests), 26.119 sn**:
  `postRequestGateEnforcesOriginBeforeNetworkAccess` ve `preferencesSurviveSettingsRecreation`.
  Gerçek Android WebResourceResponse 403 ve SharedPreferences yeniden oluşturma denetlendi.
  İlki istemciyi doğrudan çağırır; gerçek formun ağ üzerinden POST yaptığı kanıtı değildir.
- Reklam istemcisi/script/controller kaldırılmış plain WebView fixture testi tek başına
  tekrarlandı: **1 test / 1 başarısız, Synthetic WebView JS timed out, 48.496 sn**.
  Reklam motorunu kaldırmak bu ortamda zaman aşımını çözmedi; bu, kesin native kök neden
  veya uygulama kodunun hatasızlığı kanıtı değildir. Çoklu izolasyon denemesi ayrıca
  başlangıç ANR'ıyla `Process crashed` verdi. Aynı başarısız yöntem körlemesine tekrarlanmadı.
- GitHub bağlantısından configured Releases kaynağı okundu: test-1018, build-16, build-15,
  build-13, build-11. 1020'den büyük build kaydı yoktu. Canlı kaynağın okunması + 9 seçim
  testi var; Android ağ denetimi/bildirim tıklaması uçtan uca doğrulanmadı. Test için
  sahte public Release oluşturulmadı. Kaynak meta verisi kanıt paketine eklendi.

Kanıt dosyaları: v11-delivery-build.log, v11-dom-final.log, JVM XML/Lint raporları,
v11-attached-tests.log, v11-plain-webview-test.log, v11-android-boundary-tests.log,
v11-final-update.log ve temizlenmiş native/ANR özetleri. Ham cookie/session verisi veya
crashpad binary dökümü teslim edilmez. Ayrı JSON APK dosya/imza/hash bilgilerini içerir.
