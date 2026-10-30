# Supfy 1021 — mobil giriş ve siyah ekran düzeltmesi

30 Ekim 2026. Temel: V11 `cdf9d54`, dal: `codex/fix-login-black-screen`.
Sürüm: **0.6.1-login-fix / 1021 / com.supfy.app**. Bu bir debug test APK'sıdır.

## Bulgular ve uygulanan değişiklikler

Kullanıcı Spotify sayfasındaki “Oturum aç” düğmesinin siyah ekrana götürdüğünü doğruladı.
Fotoğraflarda masaüstü sayfası telefon genişliğine sığmıyor; koruma ekranında “hazır” ile
eski zaman aşımı hatası birlikte görünüyor. Telefonda HTTP/JavaScript günlüğü alınmadığından
cihaza özgü siyah ekranın tek bir kesin sunucu/DRM nedeni bulunduğu iddia edilmez.

- Telefon görünümü varsayılan yapıldı. Daha önce açıkça kaydedilmiş masaüstü tercihi
  korunur. Spotify oynatıcısı hâlâ WebView tabanlıdır; yeni bir yerel Spotify istemcisi değildir.
- Spotify içindeki GET giriş gezinmesi ve Supfy menüsü aynı, varsayılan mobil WebView
  oturum penceresine bağlandı. JS/DOM Storage açık; çerez deposu paylaşılır. POST belgeleri
  GET olarak tekrar oynatılmaz. Giriş penceresine reklam motoru eklenmez.
- accounts.spotify.com ile normal/intl-xx Spotify login/signup/logout ara yolları ayrı
  sınıflandırılır. Ara `/login` belgesi oynatıcıya başarılı dönüş sayılıp erken kapatılmaz.
- Ana belge HTTP/ağ/SSL hataları Türkçe açıklama ve yeniden deneme sunar. Yükleme sırasında
  gösterge vardır; 30 saniyede görünür içerik oluşmazsa boş ekran/zaman aşımı durumu oluşur.
- Görünür içerik denetimi yalnızca yeni belgenin `onPageCommitVisible` olayından sonra başlar.
  Eski belgenin görünür olması yeni girişin yüklendiği anlamına gelmez. Kontrol yalnızca
  boolean alır; sayfa metni, alan değerleri, şifre, çerez veya token okumaz.
- Kapanmış istemci/pencere callback'leri yeni pencerenin hata/yükleme durumunu değiştiremez.
  Zamanlayıcılar iptal edilir. Koruma kapatılıp sağlıklı açıldığında eski hata mesajı temizlenir;
  olay geçmişi ayrı günlükte korunur.

HTTPS, Safe Browsing, TLS hatasında iptal, kapalı dosya/content erişimi ve izin sınırları
korundu. Yeni üretim bağımlılığı veya tehlikeli Android izni eklenmedi. Şifre/DRM koruması
aşılmadı. Kaynaklar MIT olarak kalır; üçüncü taraf telifleri korunur. Public push/Release yoktur.

## Testler

- Başlangıç V11 testDebugUnitTest geçti.
- Eski hata mesajı regresyonu önce başarısız oldu, düzeltmeyle geçti.
- Commit öncesi eski içeriğin başarı sayılması regresyonu önce **45 test / 1 başarısız**,
  düzeltmeyle **45/45 başarılı** oldu. Kimlik doğrulama köken/yol ve geç yanıt testleri dahil.
- Son dört görev: testDebugUnitTest, lintDebug, assembleDebug, assembleDebugAndroidTest:
  inceleme sonrası **BUILD SUCCESSFUL, 1m27s**; son test kaynağıyla tekrar **31 sn** içinde
  başarılı tamamlandı. Lint: **0 hata / 32 uyarı**; uyarılar gizlenmedi.
- Mevcut reklam JavaScript motorunun **29/29 DOM testi geçti**. Gerçek reklam değildir.
- API35 AOSP x86_64, WebView 124.0.6367.219, KVM olmadan yazılım emülasyonu kullanıldı.
  Kapanmış guard ve popup'ın yeni pencereye hata taşıması iki Android testinde önce
  **2/2 başarısız** olarak yeniden üretildi. Düzeltme sonrası bu ikisi dahil **7/7 Android
  sınır testi geçti (71.429 sn)**: mobil giriş ayarları/HTTP hatası, ara login sayfasının
  erken kapanmaması, mobil varsayılan/explicit masaüstü tercihi, POST köken kapısı ve
  tercih kalıcılığı. Callback'ler kontrollü çağrılır; bu gerçek Spotify form testi değildir.
- Ayrı yerel HTML mobil giriş formu/etkileşim denemesi ve güncelleme deneyi aşağıdaki son
  doğrulama bölümüne kaydedilir. Eski V11 renderer/SIGTRAP başarısızlıkları başarıya çevrilmez.
- Anonim HTTP denemesinde ana sayfa ve mobil/masaüstü giriş URL'leri 200 yanıt verdi;
  giriş /en/login'e yönlendi. 14 giriş betiği accounts.scdn.co'dan 200 döndü. Bu masaüstü
  HTTP kontrolüdür; telefonda formun çalıştığı veya hesabın açıldığı kanıtı değildir.
- Host Chromium denemesi sandbox yardımcı dosyasının sahipliği nedeniyle başlatılamadı.
  Sandbox veya TLS kapatılmadı. Kullanıcı hesabına giriş ve Xiaomi testi yapılmadı.

## APK ve kurulum

Dosya: `/workspace/deliverables-login-fix/Supfy-0.6.1-1021-debug.apk`.
Paket `com.supfy.app`, sürüm `0.6.1-login-fix`, kod `1021`, min/target SDK `26/35`.
Boyut **9.067.107 bayt**. RSA-2048 Android Debug / APK v2 imzası doğrulandı.
APK SHA-256:
`b722167ed39bfb5e7e9637ad9272a8975e0057fb8195550482834a29e28c6e84`

Sertifika SHA-256:
`f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640`
Bu, teslim edilen V11 1020 test APK'sıyla aynıdır. İmza/CRC/asset/ZIP hizalama kanıtları
teslimde bulunur. Anahtar dosyası kaynak veya rapor paketine eklenmez.

1. V11 test sürümü kuruluysa uygulamayı kaldırmadan bu APK'yı açıp güncellemeyi onaylayın.
2. Spotify sayfasındaki “Oturum aç” veya Supfy menüsündeki “Spotify giriş ekranı”nı deneyin.
   Giriş artık ayrı “Spotify oturum açma” penceresinde gösterilir. Şifre yalnızca Spotify'a girilir.
3. Görünüm hâlâ genişse Ayarlar → “Masaüstü sitesini iste”yi kapatın; açıkça kaydedilmiş
   kullanıcı tercihi otomatik değiştirilmez. Bu, hesap veya DRM gereksinimlerini kaldırmaz.
4. Sorun sürerse artık gösterilen hata türünü/HTTP kodunu ve Ayarlar'daki WebView sürümünü
   kaydedin. Kişisel veri, şifre veya oturum tokenı paylaşmayın. Uygulama verisini silmeyin.
5. İmza uyuşmazlığı görülürse uygulamayı kaldırmayın. Özgün 1018 APK farklı sertifikalıydı;
   o kurulum için aynı özel anahtar gerekir. Bu düzeltme bu sınırı aşmaz.

## Kalan doğrulama

Gerçek Spotify hesabıyla giriş, oturumun yeniden açılışta korunması, tam şarkı/DRM ve
Xiaomi 14T Pro doğrulanmadı. Reklam motoruna bu düzeltmede yeni engelleme iddiası eklenmedi;
canlı reklam işleme ana kabul kriteri hâlâ karşılanmış sayılmaz. Spotify koşulları reklam
engellemeyi yasaklayabilir; hesap kısıtlaması riski devam eder.

## Son doğrulama: gerçek görüntüleme sınırı

Yeni mobil oturum WebView'ine yerel HTTPS kökenli sentetik HTML, e-posta alanı ve Devam
düğmesi yerleştirildi. Gerçek DOM etkileşimi ve görünür içerik denetimi sınanmak istendi.
**1 test / 1 başarısız, 48.886 sn: Synthetic WebView JS timed out.** Bu test gerçek Spotify
sayfası/hesabı değildir; başarısızlığı gizlenmedi. Bu ortamda form görüntüleme kabulü geçmedi.
Yedi başarılı Android testi callback/ayar/tercih sınırlarını doğrular; bu başarısız gerçek
renderer denemesinin yerine geçmez. Aynı başarısız yöntem tekrar tekrar çalıştırılmadı.

Özgün görüntüler ve anonim HTML/cookie yanıtları kaynak/kanıt paketine alınmaz. Yalnızca
kontrollü test kayıtları, kamuya açık URL durumları ve APK doğrulama bilgileri teslim edilir.

## Üzerine kurulum ve son dosya doğrulaması

Yalnızca sentetik emülatörde, mevcut veriyi silmeden aynı imzalı V11 1020 test APK'sı
`adb install -r -d` ile güncelleme deneyi için hazırlandı; paket yöneticisinden 1020 okundu.
Bilinen sentetik `shared_prefs/update_fixture.xml` dosyası `fixture=retained` içeriyordu.
Son 1021 APK `adb install -r` ile **Success** verdi; paket yöneticisinden 1021 ve
0.6.1-login-fix okundu. Aynı sentetik dosya güncelleme sonrasında da korundu. Uygulama
kaldırılmadı, veri/çerez temizlenmedi. Bu hesap oturumu testi değildir; fiziksel cihazın
imza doğrulamasının yerine geçmez. Kullanıcının cihazına ADB işlemi yapılmadı.

Son APK ile Gradle çıktısı birebir karşılaştırıldı. ZIP CRC, apksigner ve 16 KiB zipalign
kontrolleri geçti; dört ABI'nin native PT_LOAD hizaları 16384 bayt. Koruma JavaScript'i
kaynakla aynı, üçüncü taraf bildirimleri mevcut, anahtar dosyası yok. 16 KiB kontrolü
statiktir; fiziksel 16 KiB cihaz çalıştırması değildir.

## Değişen dosyalar

- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/supfy/app/WebViewProtectionTest.kt`
- `app/src/main/java/com/supfy/app/MainActivity.kt`
- `app/src/main/java/com/supfy/app/data/AppSettings.kt`
- `app/src/main/java/com/supfy/app/protection/ProtectionEngine.kt`
- `app/src/main/java/com/supfy/app/ui/AuthenticationWindow.kt`
- `app/src/main/java/com/supfy/app/ui/SupfyScreen.kt`
- `app/src/main/java/com/supfy/app/web/AuthenticationPolicy.kt`
- `app/src/main/java/com/supfy/app/web/PageLoadTracker.kt`
- `app/src/main/java/com/supfy/app/web/SupfyChromeClient.kt`
- `app/src/main/java/com/supfy/app/web/SupfyWebViewClient.kt`
- `app/src/main/java/com/supfy/app/web/WebPageGuard.kt`
- `app/src/main/java/com/supfy/app/web/WebUiState.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/supfy/app/protection/ProtectionEngineTest.kt`
- `app/src/test/java/com/supfy/app/web/AuthenticationPolicyTest.kt`
- `app/src/test/java/com/supfy/app/web/PageLoadTrackerTest.kt`
- `docs/GIRIS_DUZELTMESI_1021.md`
