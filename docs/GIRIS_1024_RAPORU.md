# Supfy 1024 — mevcut formun yanlışlıkla gizlenmesi

## Cihazdan gelen kanıt

Kullanıcı 1023 tanılamasını gerçek cihazdan paylaştı: Android SDK36, WebView
156.0.8078.47. Native görünüm bağlı ve görünür; JavaScript/DOM storage açık.
Belge ve native kaynak `accounts.spotify.com/login` ile eşleşiyor.

| Denetim | İlk / son örnek |
|---|---|
| Görünür input | 1 / 1 |
| Uygun boyut ve ekran alanındaki kontrol | 2 / 2 |
| CSS görünürlük denetimini geçen kontrol | 2 / 3 |
| Merkez hit-test eşleşmesi | 0 / 0 |
| Farklı merkez hedefi | 2 / 2 |
| Alan kaldırılması | 0 / 0 |
| JavaScript hata/rejection | 0 / 0 |
| Kaydedilen ağ/SSL kaynak hatası | Yok |

Bu veri formun eksik veya kaldırılmış olduğunu desteklemiyor. Eski kod, yalnızca
merkez noktasının farklı öğeye denk gelmesini `EMPTY` sayıyor; ardından native
opak hata ekranı mevcut sayfayı kapatıyordu. Ayrıca ilk çizim isteği hit-test
başarısına bağlıydı. Bu döngü düzeltildi.

**Gerçek hit hedefinin bir etiket, üst öğe, CAPTCHA/başka katman veya WebView
hit-test farkı olduğu bu sayılarla kesinleşmez.** Bilinmeyen katman kaldırılmadı.
Chromium154'te tam ekran ölçüleri/DPR/dokunmatik profiliyle canlı formdaki aynı
merkez hatası tekrar üretilemedi; aşağıdaki yerel Android testleri uygulamanın
kendi oluşturduğu siyah katmanı yeniden üretiyor.

## Uygulanan düzeltme

- `contentPresent`, etkileşim bilgisinden (`ready`) ayrıldı. Kontrolün boyutu,
  ekran alanı ve CSS görünürlüğü uygunsa yalnızca hit-test farkıyla boş sayfa
  sayılmıyor.
- İçerik mevcutken native yükleme/hata katmanı ilk çizim callback'i beklenmeden
  kaldırılır. `postVisualStateCallback` içerik varlığıyla tetiklenir; hit-test
  başarısına bağlı değildir.
- Çizim ve JavaScript bekleme durumları ayrıdır. Bekleyen çizim, sağlık taramasını
  durdurmaz. Geç callback'ler ticket/terminal hata denetimlerine tabidir.
- Etkileşim kesin değilse `INTERACTION` aşaması ve Türkçe durum gösterilir;
  form/oturum başarıyla tamamlandı olarak gösterilmez.
- Merkez ve dört iç nokta ölçülür. Kontrolün kendisi/alt öğeleri veya yalnızca o
  kontrolle ilişkili label kabul edilir. Rastgele üst öğe veya alttaki öğenin
  `elementsFromPoint` listesinde bulunması başarı sayılmaz.
- Bilinmeyen engeller, CAPTCHA ve doğrulama pencereleri korunur; tıklama, focus,
  pointer-events değişikliği, DOM kaldırma veya otomatik form doldurma yoktur.
- Tanılamaya yalnızca sabit sayısal hit kategorileri/ilişkileri eklendi. Alan
  değerleri, parola, çerez/token veya sayfa metni okunmaz.

HTTPS, sertifika hatasında iptal, Safe Browsing, çerez ve dosya/content kısıtları,
medya izin kapısı, paket kimliği ve imza korunur. Hesap/DRM koruması aşılmaz.
Reklam motoru değiştirilmedi; canlı reklam başarısı iddia edilmez.

## Kırmızı → yeşil doğrulaması

Eski 1023 üretim APK'sıyla iki yeni Android testi başarısız oldu (106,720 saniye):

1. Görünür kontroller farklı hit hedefi aldığında `EMPTY` hatası oluştu.
2. Gerçek Compose penceresindeki yerel renkli form 30 saniye sınırından sonra
   native siyah hata katmanıyla örtüldü; PixelCopy renk kontrolü başarısız oldu.

Tarayıcıdaki ayrı test de içerik varlığı/etkileşim ayrımı olmadığından başarısızdı.
Yeni testler bu davranışları ve sayfanın kendi katmanının korunmasını sınar.

Son doğrulanmış sonuçlar belgenin sonundadır.

## Sınırlar ve kurulum

Bu düzeltme cihaz kaydında saptanan yanlış boş sayfa kararı ve native örtme
davranışını giderir. **Yeni APK'nın fiziksel Xiaomi/WebView156 üzerindeki gerçek
hesap girişi henüz test edilmedi.** Gerçek oturumun tamamlanması/kalıcılığı,
Google/Apple/Facebook harici sağlayıcı akışları ve DRM/müzik ayrı kabul testleridir.

1024 APK'yı 1023'ün üzerine kurun; uygulamayı/site verisini silmeyin. Menü → Spotify
giriş ekranı ile deneyin. Spotify'ın sunduğu doğrulama adımı varsa sayfada korunur.
Sorun sürerse mevcut Tanıyı kopyala düğmesi `contentPresent`, etkileşim ve ek hit
kategorilerini raporlar. Bu kayıt parola/oturum verisi içermez.

## 1023 kaynak paketine göre değişen dosyalar

- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/supfy/app/LoginDiagnosticsTest.kt`
- `app/src/androidTest/java/com/supfy/app/MainActivityLoginTest.kt`
- `app/src/androidTest/java/com/supfy/app/WebViewProtectionTest.kt`
- `app/src/main/assets/login_probe.js`
- `app/src/main/java/com/supfy/app/ui/SupfyScreen.kt`
- `app/src/main/java/com/supfy/app/web/LoginDiagnostics.kt`
- `app/src/main/java/com/supfy/app/web/WebPageGuard.kt`
- `app/src/main/java/com/supfy/app/web/WebUiState.kt`
- `app/src/main/res/values/strings.xml`
- `docs/GIRIS_1024_RAPORU.md` (yeni)


## Son doğrulanmış sonuçlar

| Kontrol | Sonuç |
|---|---|
| Gradle testDebugUnitTest / lintDebug / assembleDebug / assembleDebugAndroidTest | Başarılı; 1 dakika 50 saniye |
| JVM testleri | 50 geçti, 0 başarısızlık/hata |
| Koruma JavaScript testleri | 29 geçti; sentetik DOM |
| Android tam regresyonu | 26/26 geçti; 374.646 saniye; API30/WebView83 |
| Telefon raporuna karşılık gelen yerel Compose blackout testi | Eski APK'da başarısız; yeni APK'da piksel kontrolü geçti |
| Tarayıcı matrisi | 13 durum geçti; gerçek Chromium154, telefon boyutu/DPR/dokunmatik |
| Normal canlı herkese açık Spotify formu | Tarayıcıda içerik + etkileşim olumlu; hesap gönderilmedi |
| CAPTCHA/bilinmeyen katman korunması | Yerel testlerde doğrulandı; gerçek CAPTCHA çözülmedi |
| Lint | 0 hata, 31 uyarı |
| Fiziksel yeni 1024 / gerçek hesap girişi | Henüz doğrulanmadı |

İlk yeni kod koşusu 26 testte 1 test fixture beklentisiyle başarısızdı: inputun
ilişkili label'ı varken pointer-events:none uygulanmıştı. Yeni kod bu gerçek label
aktarımını doğru olarak etkileşim sayıyordu. Etiketsiz fixture ile belirsiz hedef
senaryosu ayrıldı; tek test 30,445 saniyede geçti, ardından tam 26 test yeniden
çalıştırıldı. Üretim APK'sı fixture düzeltmesinde değişmedi. Başarısız koşu kaydı
kanıt paketinde korunur; nihai yeşil sonuç yerine sunulmaz.

Bağımsız son kaynak incelemesinde önemli bulgu yok: ticket/terminal hata kapıları,
ayrı çizim beklemesi, bilinmeyen engellerin korunması ve yetki değişmemesi incelendi.
Bu inceleme gerçek cihaz girişinin yerine geçmez.

## APK

```json
{
  "name": "Supfy-0.6.4-1024-login-test.apk",
  "path": "/workspace/deliverables-login-1024/Supfy-0.6.4-1024-login-test.apk",
  "applicationId": "com.supfy.app",
  "versionCode": 1024,
  "versionName": "0.6.4-login-visibility",
  "signingType": "Android Debug",
  "certificateSha256": "f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640",
  "sizeBytes": 9012249,
  "sha256": "2e59ff5886fde6c84ec260472e26b0979d384913b7de2bb5c016adf16b53497f"
}
```

1023 ile aynı yerel imza ve daha yüksek versionCode kullanılır. Emülatöre install -r
başarılıdır; uygulama/site verisi temizlenmedi. Orijinal farklı imzalı 1018'in özel
anahtarı mevcut değildir. İmza dosyaları pakete dahil edilmez. GitHub yayını yapılmadı.
