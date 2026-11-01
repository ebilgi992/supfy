# Giriş incelemesi — yalnızca test derlemesi

30 Ekim 2026. Temel: `b22dc06` / teslim edilen 1021 APK.
Dal: `codex/investigate-login-rendering`.

İlk kaynak incelemesinde kullanıcının “giriş kısmını çözene kadar derleme yapma”
talimatına uyularak derleme yapılmadı. Kullanıcı daha sonra açıkça **“Yalnızca test
derlemesine izin veriyorum”** yanıtını verdi. Bundan sonraki derlemeler Android
doğrulaması içindir; yeni bir kurulum sürümü teslim edilmedi. Önceki teslim APK'sı
korundu; SHA-256 değeri:
`b722167ed39bfb5e7e9637ad9272a8975e0057fb8195550482834a29e28c6e84`.
Kaynak derlemesi başarılıdır; bu durum giriş formunun veya gerçek hesap girişinin
çalıştığını kanıtlamaz.

Son durum: **45 birim, 29 JavaScript, 8 tarayıcı denetimi ve 13 Android regresyon
testi geçti**. Android testleri yerel/sentetik form ve üretim gezinmesini kapsar.
Canlı Spotify formunun uygulama içinde görüntülenmesi ve gerçek hesap girişi
**doğrulanmadı**; bu çalışma tamamlanmış giriş düzeltmesi olarak teslim edilmez.

## Telefona ilişkin doğrulanan bilgiler

- Kullanıcıdaki WebView sürümü **156.0.8078.47**.
- Samsung Internet aynı `https://accounts.spotify.com/login` sayfasının giriş formunu gösteriyor.
- Supfy ekranında oturum penceresinin başlığı var; içerik siyah, yükleme/hata göstergesi yok.
- Eski WebView veya genel telefon bağlantısının bozuk olduğu varsayımı bu verilerle desteklenmez.
  Samsung Internet ile gömülü WebView aynı motor/istek davranışı değildir; tüm WebView sorunları
  bu karşılaştırmayla dışlanmış sayılmaz.

## Kodda saptanan iki gerçek hata

1. `AuthenticationWindow` içindeki AndroidView yalnızca `factory` ile ilk WebView'i tutuyordu.
   Aynı Compose karesinde eski popup kapatılıp yenisi yayımlanırsa, factory tekrar çalışmayıp
   yok edilmiş eski görünüm kalabilir. Yeni popup durumu ile çizilen görünüm farklı olabilir.
   Ana oynatıcı zaten `key(webView)` kullanıyor. Eski oturum bileşenine de `key(web)` eklendi;
   ayrıca bu bileşen üretim giriş yolundan tamamen çıkarıldı.
2. Eski görünür içerik denetimi boş bir SVG, ekran dışındaki düğme veya boş iframe için
   `true` döndürüyor; ardından yükleme ve zaman aşımı denetimini kalıcı durduruyordu.
   Üçü de gerçek Chromium'da kaynak kodundaki aynı JavaScript ile yeniden üretildi.
   Giriş denetimi artık görünür pencere alanındaki kullanılabilir form/eylem denetimlerini
   arıyor. Input değerleri, parola, çerez ve token okunmuyor.

Bu iki hata kaynakta kanıtlanmıştır. Telefonda ilk siyah ekranın tek sebebinin bunlardan
hangisi olduğu canlı telefon günlüğü olmadan kesinleştirilmiş değildir.

## Yapılan mimari düzeltme

- Ana sayfadaki ve menüdeki giriş artık **aynı mevcut WebView** içinde normal gezinmedir.
- `setSupportMultipleWindows(false)` kullanılır. Android'in resmî sözleşmesine göre
  `window.open` ve `target=_blank` istekleri bu modda mevcut WebView'in üst gezinmesi olur.
- Girişi yakalayıp yeni WebView/Dialog açan callback ve özel popup oluşturma kodu kaldırıldı.
- Oturum penceresinden oynatıcıya URL'yi GET olarak tekrar yükleme yolu da kaldırıldı.
  Yönlendirme ve POST, WebView'in kendi gezinme motoruna bırakıldı.
- Aynı görünüm, geçmiş ve CookieManager korunur. Giriş sırasında reklam motorunun ana
  çerçeve bağlamı da accounts.spotify.com'a geçer; oynatıcı koruma scripti oraya eklenmez.
- HTTPS/TLS iptali, Safe Browsing, kapalı dosya/content erişimi, üçüncü taraf çerez kısıtı,
  ana belge köken kapısı ve korumalı medya izin sınırı değiştirilmedi.
- Masaüstü görünümünü açıkça seçen kullanıcı tercihi korunur; bu profil girişte de korunur.
  Mevcut varsayılan telefon görünümüdür.

Resmî API:
https://developer.android.com/reference/android/webkit/WebSettings#setSupportMultipleWindows(boolean)

`AuthenticationWindow.kt` kaynak dosyası korunur fakat uygulamadan çağrılmaz. Kullanıcı
dosyası, oturum verisi veya çerez silinmedi. Uygulama sıfırdan oluşturulmadı.

## Gerçekte çalıştırılan kontroller

- Resmî masaüstü Chromium **124 ve 154** ile Spotify'ın canlı giriş URL'si açıldı. Her motorda
  normal, Android WebView tarayıcı tanımı ve masaüstü tanımıyla e-posta alanı görünür, HTTP200,
  yakalanan sayfa JavaScript hatası yok. Bunlar Android WebView testi değildir.
- Chromium124'te Android WebView124 client hints, Android15 ve Türkçe profil ile de canlı
  form görüntülendi. WebView156 motorunda çalıştırma yapılmış sayılmaz.
- Kaynaktaki yeni giriş kontrolü gerçek Chromium124'te **8/8** kontrolden geçti: boş SVG,
  ekran dışı düğme, boş iframe, gizli form, görünür e-posta, OTP, e-posta bekleme eylemi ve
  canlı Spotify formu. Hesap/e-posta/parola gönderilmedi; gerçek oturum açılmadı.
- Mevcut 1021 APK'nın gerçek Compose ekranı emülatörde açıldı. İlk popup'ın bağlı,
  320×576 boyutlu olduğu DevTools verisiyle görüldü; başlangıç yükleme göstergesi görüntülendi.
  Bu ilk oluşturmanın her zaman sıfır boyutlu olduğu varsayımını desteklemiyor.
- Emülatörde launcher/System UI ANR'ları ve önceki renderer sorunları var; alternatif
  yazılımsal grafik arka ucu da telefon doğrulamasının yerine geçmedi. Mevcut emülatör
  denemesi telefondaki kalıcı siyah ekranın kesin tekrar üretimi değildir.
- Bağımsız kaynak incelemesinde yeni tek pencere akışında Important/Critical bulgu ve
  kaldırılmış popup API'lerine kalan çağrı saptanmadı. Bu derleme sonucu değildir.
- Kullanıcının test derlemesi izninden sonra `testDebugUnitTest lintDebug assembleDebug
  assembleDebugAndroidTest` başarılı oldu. **45 birim testi**, sıfır başarısızlık;
  Lint **0 hata, 31 uyarı**. Sonraki test düzeneği değişiklikleri de derlendi.
- Gerçek `MainActivity`/Compose menüsündeki “Spotify giriş ekranı” eylemi Android'de
  aynı native WebView nesnesini koruyarak `accounts.spotify.com` adresine gitti:
  **1 test geçti, 47,292 saniye**. İlk test erişilebilirlik etiketi aramasındaki
  test hatası yüzünden başarısızdı; helper düzeltildikten sonra yeniden çalıştırıldı.
  Bu sonuç formun çizildiğini veya girişin tamamlandığını söylemez.
- Yerel giriş formunun gerçek belge commit'ini bekleyen Android testi, **48,412
  saniyede renderer belgeyi çizmeden çıktığı için başarısız oldu**. Bunu başarılı
  giriş veya yalnızca JavaScript zaman aşımı olarak raporlamıyoruz.
- Ayrı test APK sürecinde, `android.app.Application` ve yalnızca framework kullanan
  sade Java WebView kontrolü çalıştırıldı. Supfy/Compose/koruma istemcisi yokken,
  JavaScript hem kapalı hem açık durumda provider **124.0.6367.219** yerel belgeyi
  commit etmeden çöktü (`renderer exited; crashed=true`). Bu emülatördeki çökme
  Supfy'ye özgü değildir; telefondaki 156 sürümünün davranışının kanıtı değildir.
  İlk Kotlin kontrol düzeneğinde eksik Kotlin runtime hatası vardı; Java düzeneği
  bu hatayı kaldırdı ve renderer kaybını uygulamayı çökertmeden kaydetti.
- Temiz **Android API30 / WebView83.0.4103.120** ortamında ilk açılışın sistem
  ANR'ları sonrasında sade kontrol belgesi görünür biçimde çizildi ve JavaScript
  çalıştı. Ardından **Supfy'nin gerçek WebViewFactory + SupfyWebViewClient** ile
  yerel giriş formu çizim/etkileşim testi **20,420 saniyede geçti (1 test)**.
  Gerçek belge commit edildi; formun Devam eylemi ve yükleme durumunun kapanması
  doğrulandı. Bu yerel/sentetik bir formdur; canlı Spotify giriş sonucu değildir.
- Koruma JavaScript testleri yeniden çalıştırıldı: **29/29 geçti**. Giriş hazır
  olma denetiminin Chromium124 üzerindeki **8/8** kontrolü de yeniden geçti.
- `target=_blank` bağlantısı ve gerçek native dokunuşla tetiklenen `window.open`
  için iki yerel Android testi **29,161 saniyede geçti (2 test)**. İki durumda da
  giriş belgesi aynı WebView içinde commit edildi ve e-posta alanı bulundu.
  Ardından çalıştırılan 13 testlik regresyonda WebView testlerinin **12'si geçti**;
  son Compose menü testi giriş öğesini bulamadı (**1 başarısızlık**, 133,990 saniye).
  Test yardımcısı artık yalnızca aktif kökü değil uygulamanın açılır pencere
  köklerini de izliyor; eski/yavaş test ortamına uygun bekleme eklendi. Bu ilk
  düzeltme tek başına yeterli olmadı: menü testi tekrar başarısız oldu (62,992
  saniye). Ekran görüntüsü uygulamanın açık, menünün ise açılmamış olduğunu
  gösterdi. Testin gönderdiği olayı kabul ettirmesi, Compose düğmesinin gerçekten
  tıklanması değildi. Helper erişilebilirlik `ACTION_CLICK` ve native fallback
  için açık `SOURCE_TOUCHSCREEN` kullanacak şekilde düzeltildi. Ardından aynı
  üretim menüsü testi API30'da **29,877 saniyede geçti (1 test)**. Üretim kodu bu
  test yardımcısı hatalarını gizlemek için değiştirilmedi.
- Yardımcı düzeltildikten sonra iki Android test sınıfı birlikte yeniden
  çalıştırıldı: **13/13 geçti, 128,701 saniye**. Yerel form çizimi/etkileşimi,
  iki yeni pencere giriş biçimi, aynı native görünümün korunması, HTTP hata
  durumu, kapanmış guard callback'i, kullanıcı tercihi, girişte koruma
  scriptinin yokluğu, sentetik DOM mute/restore, gerçek WebView fetch'inin
  sentetik reklam alanında filtrelenmesi ve POST köken sınırı kapsandı.
  Bu başarılı koşu önceki başarısız koşuları geçersiz kılmaz; düzeltme sonrası
  sonuçtur. Gerçek reklam sesi/Spotify hesabı/DRM doğrulaması içermez.
- Son `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` kontrolü
  **25 saniyede başarılı** oldu (80 görev: 3 çalıştı, 77 güncel). Lint 0 hata/31
  uyarı; JVM sonuç kayıtlarında 45 test, sıfır hata/başarısızlık var.
- Regresyon sonrasında gerçek uygulama ADB ile açıldı. Gerçek açılır menüdeki
  “Spotify giriş ekranı” öğesinin sınırları UI hiyerarşisinden okundu ve dokunuldu.
  Canlı adres denemesi **“Sayfa açılamadı — Sunucuya ulaşılamadı”** hata ekranına
  döndü. Bu `NO_CONNECTION` sınıfıdır; DNS ve bağlantı alt nedenleri bu ekranla
  ayrıştırılmaz. Ekran görüntüsü `api30-live-login-result.png` olarak kaydedildi.
  Siyah ekranda sessizce kalma yerine görünür hata var; canlı giriş başarısı yok.

Tarayıcı deneylerinde sandbox ve TLS açık tutuldu. Sistem/Spotify doğrulaması kapatılmadı,
uygulamaya sertifika veya gizli JavaScript köprüsü eklenmedi. Ham tarayıcı profilleri ve
cookie yanıtları teslim paketine alınmaz.

## Açık doğrulama sınırları

`MainActivityLoginTest` üretim menüsü/yönlendirme kontrolünü geçti. Önceki FrameLayout
popup kontrolleri bu davranışı kanıtlamıyordu. Eski başarısız test kayıtları korunur;
yeni test başarısı diye sunulmaz. Android API30 için ayrı, temiz test AVD'sinde
yerel form testi de geçti; mevcut AVD ve kullanıcı verileri silinmedi.

**Giriş sorunu çözülmüş ilan edilmedi; yeni teslim APK'sı yoktur.** Test APK'sı üretildi
ve emülatöre kuruldu. Gerçek hesap oturumu, fiziksel Xiaomi ve canlı reklam testleri
doğrulanmadı. Telefonun WebView156 sürümüne erişim ve kullanıcı tarafından tamamlanan
hesap girişi olmadan bu sınırlar test sonucu gibi gösterilemez.


## Sonuç tablosu

| Kontrol | Sonuç |
|---|---|
| Giriş mimarisinin kaynak değişikliği | Tek mevcut WebView; ikinci oturum penceresi üretim yolundan çıkarıldı |
| Yerel form çizimi ve etkileşim | Android API30 / WebView83 üzerinde geçti; sentetik |
| target=_blank ve window.open | Aynı WebView içinde yerel girişe geçiş doğrulandı |
| Üretim Compose menüsü | Düzeltme sonrası API30 regresyonunda geçti |
| Boş içerik denetimi | 8/8 tarayıcı kontrolü geçti; gerçek Chromium124 |
| Girişte koruma izolasyonu | Android sentetik testinde script eklenmedi/ses değiştirilmedi |
| Birim testleri | 45 geçti; 0 başarısızlık/hata |
| JavaScript koruma testleri | 29 geçti; sentetik DOM |
| Android regresyonu | Son koşu 13/13 geçti; önceki başarısız koşular saklandı |
| Lint | 0 hata, 31 uyarı |
| Canlı Spotify formu — Supfy | Emülatörde sunucu bağlantı hatası; başarı kanıtlanmadı |
| Gerçek hesap girişi / oturum korunması / çıkış | Test edilmedi |
| Gerçek müzik / reklam / DRM / HyperOS | Test edilmedi |
| Fiziksel Xiaomi 14T Pro | Bağlı cihaz yok; doğrulanmadı |
| Yeni kurulum sürümü | Yok; yalnızca izin verilen test derlemesi |

## Test APK'sının doğrulanan bilgileri

```json
{
  "package": "com.supfy.app",
  "versionName": "0.6.1-login-fix",
  "versionCode": 1021,
  "size": 9004536,
  "sha256": "b504cda7529ef61cfded89bb3268fb5feb98997649ac514d8651ca237913e8d5",
  "certificateSha256": "f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640",
  "signing": "Android Debug",
  "path": "/workspace/supfy/app/build/outputs/apk/debug/app-debug.apk",
  "purpose": "Emulator verification only; not a new installation release"
}
```

`apksigner verify --print-certs` doğrulaması başarılıdır. Sürüm kodu yeni yayın
olarak artırılmadı. Eski 1021 teslim APK'sının hash'i ile bu test APK'sının hash'i
farklıdır; dosyalar birbirinin yerine sunulmaz. Orijinal 1018 imza anahtarı bu
ortamda yoktur; orijinal 1018 üzerine kurulum uyumluluğu sağlanmış sayılmaz.
Kullanıcının uygulaması/verileri kaldırılmadı, anahtarlar pakete dahil edilmedi.

## Değişen dosyalar

- `README.md`
- `app/src/androidTest/AndroidManifest.xml`
- `app/src/androidTest/java/com/supfy/app/MainActivityLoginTest.kt`
- `app/src/androidTest/java/com/supfy/app/WebViewProtectionTest.kt`
- `app/src/androidTest/java/com/supfy/app/testing/BareWebViewActivity.java`
- `app/src/main/java/com/supfy/app/MainActivity.kt`
- `app/src/main/java/com/supfy/app/ui/AuthenticationWindow.kt`
- `app/src/main/java/com/supfy/app/web/SupfyChromeClient.kt`
- `app/src/main/java/com/supfy/app/web/SupfyWebViewClient.kt`
- `app/src/main/java/com/supfy/app/web/WebPageGuard.kt`
- `app/src/main/java/com/supfy/app/web/WebViewFactory.kt`
- `docs/GIRIS_KOK_NEDEN_INCELEMESI.md`

Android test manifesti ve BareWebViewActivity yalnızca instrumentasyon APK'sındadır;
Supfy'nin üretim manifestine izin/aktivite eklenmedi. Üçüncü taraf kod/lisansı
kopyalanmadı; mevcut telif ve lisans belgeleri korundu.

## Telefonda açık kalan kabul kontrolleri

1. Spotify sayfasındaki Oturum aç ve Supfy menüsündeki giriş ile e-posta formu görünür.
2. Kullanıcı kendi girişini tamamlar; parola uygulamaya/loglara kaydedilmez.
3. Spotify oynatıcıya dönüş ve kapatıp yeniden açınca beklenen oturum devamı görülür.
4. Çıkış ve yeniden giriş denenir; koruma açık/kapalı karşılaştırılır.
5. Başarısız ağ/boş sayfa durumunda görünür hata ve tekrar deneme kontrol edilir.

Bu kontroller tamamlanmadan telefon giriş hatası giderilmiş veya reklam motoru
canlı Spotify koşullarında başarılı olarak işaretlenmez.
