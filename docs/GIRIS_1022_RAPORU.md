# Supfy 1022 — devam eden siyah giriş ekranı incelemesi

30 Ekim 2026. Dal: `codex/fix-persistent-login-1022`.

## Durum ve kanıt sınırı

Kullanıcı, 1021 test APK'sında 40 saniyeden sonra da siyah ekranın kaldığını bildirdi.
Bu nedenle önceki sürüm giriş sorunu çözülmüş sayılmaz. 1022 yeni bir test sürümüdür.
Gerçek Xiaomi 14T Pro, kullanıcı hesabı, oturum devamı, gerçek reklam ve DRM oynatması
bu ortamda doğrulanmadı. Test sonuçları aşağıda yerel/sentetik ve canlı herkese açık
sayfa olarak ayrı tutulur. GPU'nun telefondaki kök neden olduğu kanıtlanmadı.

## Ajanların bağımsız bulguları

Üç ajan görünüm/yaşam döngüsü, Spotify giriş akışı ve testlerin kapsamını ayrı inceledi.
Ana ajan değişiklikleri uygulayıp sonuçları doğruladı.

- Önceki APK kaynakla eşleşiyor; sorun yalnızca eski popup APK'sının yanlış teslimi değil.
- Guard bir kez uygun DOM öğesi görünce bütün denetimleri durduruyordu. Form daha
  sonra kaybolduğunda yükleme/hata göstergesi olmadan siyah sayfa kalabiliyordu.
- Görünürlük denetimi formun önüne gelen opak katmanı dikkate almıyordu.
- Tek sayfa içindeki `/login` geçişi yeni bir sayfa yükleme callback'i üretmeyebilir;
  önceki başarılı sayfanın denetimi girişe taşınmıyordu.
- Önceki üretim menüsü testi yalnızca URL ve aynı WebView nesnesini doğruluyordu;
  formun gerçek Compose penceresinde çizilmesini kanıtlamıyordu.
- Herkese açık Spotify giriş formu gerçek masaüstü Chromium'da HTTP200 ve görünür
  e-posta alanı ile açıldı. Bu, telefondaki WebView156 testi değildir.

Bu bulgularla kaynak/test düzeneğindeki açıklar kanıtlandı. Telefonun son siyah
karesinin DOM kaybından mı yoksa çizim katmanından mı kaynaklandığı fiziksel cihaz
kaydı olmadan kesinleştirilmedi.

## Kod değişiklikleri

- Giriş belgesinde ilk hazır durumundan sonra da sağlık denetimi devam eder.
  Denetim yalnızca önde çalışır; arka planda zamanlayıcıları durur.
- Form sonradan kaybolur/örtülürse süreli görünür hata oluşur. Form geç geri gelirse
  POST yeniden gönderilmeden, sayfa yeniden yüklenmeden toparlanır.
- Yeni belge için ilk yükleme sınırı 30 saniye; hazır formun sonradan kaybolması
  için tolerans 10 saniyedir. Uygulama arka plandayken bu süre işletilmez.
- Başlatılan giriş callback beklemeden izlenir; geçmiş geri yükleme, yönlendirme
  ve `pushState`/`replaceState` gezinmeleri de denetime alınır.
- DOM geometrisi tek başına yeterli sayılmaz: formun merkezinde başka DOM katmanı
  olup olmadığı ve Android `postVisualStateCallback` çizim hazırlığı kontrol edilir.
- Yalnızca giriş belgesi `LAYER_TYPE_SOFTWARE` kullanır. Oynatıcıya geçince normal
  çizim geri gelir. Bu resmî API ile bir uyumluluk yoludur; kanıtlanmış telefon GPU
  hatası düzeltmesi veya DRM yöntemi değildir.
- Native Türkçe giriş durum satırı gerçek yükleme aşamasını ve **1022** kodunu
  gösterir. Parola, form değeri, sayfa metni, çerez veya token okunmaz.
- Paket `com.supfy.app`, imza, CookieManager, kullanıcı tercihleri ve aynı WebView
  korunur. Sürüm kodu 1022, sürüm adı `0.6.2-login-recovery` oldu.

## Kırmızı → yeşil deneyler ve başarısız denemeler

1. Yeni iki saf tracker testi eski kodda başarısız oldu. Geç gelen formun EMPTY
   hatasından toparlanması testi de eklenince eski kodda üç başarısızlık görüldü.
2. Gerçek Android'de sonradan boşalan form ve opak katman testleri eski guard ile
   başarısız oldu. İki testte beklenen EMPTY yerine `null` vardı.
3. Yeni guard sonradan boşalan formu yakaladı. İlk opak katman fixture'ı API30'ın
   WebView83 sürümünde desteklenmeyen CSS `inset` kullandığı için alanı kaplamadı;
   bu koşu 3 testte 1 başarısızlık verdi. Fixture açık top/right/bottom/left ile
   düzeltildi; ayrıca kapladığı alan testte ölçülür. Üretim filtresi gevşetilmedi.
4. Çizim uyumluluğu testi uygulanmadan önce beklenen yazılımsal katman yerine
   normal katman gördüğü için başarısız oldu. Bu bir özellik/izolasyon testidir;
   telefonda GPU hatası tekrar üretimi olarak sunulmaz.
5. İlk 18 testlik regresyonda SPA testinin bekleme sınırı 16 saniyeydi; yeni giriş
   gezinmesinin ilk yükleme bütçesi ise 30 saniye. Test URL/olay doğrulaması ve
   36 saniyelik bekleme ile düzeltildi. Üretim zaman aşımı başarı göstermek için
   kısaltılmadı. Bu başarısız koşular kanıt paketinde korunur.

Doğrulanmış sonuçlar ve APK bilgileri belgenin sonundadır.

## Güvenlik

TLS hatasında iptal, Safe Browsing, HTTPS zorunluluğu, kapalı dosya/content erişimi,
üçüncü taraf çerez kısıtı ve medya izin kapısı korunur. Root, sahte sertifika,
VPN, özel Spotify API'si, parola kaydı veya token dışa aktarımı yoktur.

PixelCopy yalnızca **androidTest** içindeki sabit, yerel, yeşil/mor test formunda
kullanılır; üretim APK'sında kullanıcı ekran görüntüsü alma/toplama kodu yoktur.
Lisans/telif belgeleri korundu; dış projeden yeni kod alınmadı. GitHub yayını yapılmadı.
Spotify koşulları reklam engellemeyi yasaklayabilir; hesap kısıtlaması riski sürer.

## Telefon testi

1. 1022 APK'yı önceki yerel 1021 testinin üzerine kur; uygulamayı/veriyi silme.
2. Menüden **Spotify giriş ekranı** aç. Alt satırda 1022 ve gerçek aşama görünmeli.
3. Spotify sayfasındaki **Oturum aç** bağlantısını da dene.
4. Form açılırsa kendi hesabınla giriş, oynatıcıya dönüş, kapatıp yeniden açma ve
   çıkış/yeniden giriş senaryolarını doğrula. Hesap bilgilerini rapora gönderme.
5. Siyah kalırsa alttaki aşama/hata bilgisinin ekran görüntüsü teşhisi ayırır.
   Bu durumda sürüm başarılı giriş ürünü olarak işaretlenmez.


## Doğrulanmış sonuçlar

| Kontrol | Sonuç |
|---|---|
| Son Gradle derleme / birim / Lint | Başarılı; 1 dakika 45 saniye |
| JVM birim testleri | 48 geçti, 0 hata/başarısızlık |
| Koruma JavaScript testleri | 29 geçti; sentetik DOM |
| Android tam regresyon | 18/18 geçti; 290.887 saniye |
| Test kaynak kapatma kontrolleri | Guard testlerinin 3/3 yeniden koşusu geçti; 96.899 saniye |
| Gerçek Compose penceresinde çizim | Yerel form ve Devam düğmesi PixelCopy ile doğrulandı |
| Giriş/oynatıcı çizim geçişi | Yazılımsal giriş → normal oynatıcı ve güvenlik ayarları doğrulandı |
| Formun kaybolması / örtülmesi / geri dönmesi | Yerel Android senaryolarında doğrulandı |
| Aynı belge içinde /login gezinmesi | URL/olay ve 30 saniyelik yükleme sınırı doğrulandı |
| Tarayıcı görünürlük karşılaştırması | 4 yerel durum + canlı herkese açık Spotify formu, Chromium154 |
| Lint | 0 hata, 31 uyarı |
| APK imzası / paket / versionCode | Doğrulandı |
| Mevcut yerel test kurulumu üzerine install -r | Başarılı; kaldırma/veri temizleme yapılmadı |
| Xiaomi 14T Pro / WebView156 | Bağlı fiziksel cihaz yok; doğrulanmadı |
| Gerçek hesap girişi / oturum / müzik / reklam | Doğrulanmadı |

Tam 18 test teslim edilen APK baytlarıyla çalıştı. Sonrasında test fixture'ının
teardown aşamasına client.close eklendi; sürekli denetimin test Activity'sinden
sonra zamanlayıcı bırakmaması için üç sağlık testi yeniden koşuldu. Üretim APK'sı
bu test düzeneği değişikliğinde değişmedi; SHA-256 eşleşmesi ayrıca doğrulandı.

## APK

```json
{
  "name": "Supfy-0.6.2-1022-login-test.apk",
  "path": "/workspace/deliverables-login-1022/Supfy-0.6.2-1022-login-test.apk",
  "sizeBytes": 9092069,
  "sha256": "b7822383f639e5b7826d6ceac8c7b853fb3a8e19fd9ee77aeb212bda480adf5d",
  "applicationId": "com.supfy.app",
  "versionName": "0.6.2-login-recovery",
  "versionCode": 1022,
  "signingType": "Android Debug",
  "certificateSha256": "f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640"
}
```

Bu imza önceki yerel 1021 test APK'sıyla aynıdır; sürüm kodu artmıştır. Orijinal
1018 dosyasının farklı imza anahtarı bu ortamda yoktur. İmza hatası alınırsa eski
uygulama kaldırılmamalıdır; kaldırmak oturum verilerini silebilir. Anahtar dosyası
kaynak/kanıt paketine dahil edilmez. Uygulama yayınlanmadı.

## Önceki teslim kaynak ZIP'ine göre değişen dosyalar

- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/supfy/app/MainActivityLoginTest.kt`
- `app/src/androidTest/java/com/supfy/app/WebViewProtectionTest.kt`
- `app/src/main/java/com/supfy/app/MainActivity.kt`
- `app/src/main/java/com/supfy/app/ui/SupfyScreen.kt`
- `app/src/main/java/com/supfy/app/web/PageLoadTracker.kt`
- `app/src/main/java/com/supfy/app/web/SupfyWebViewClient.kt`
- `app/src/main/java/com/supfy/app/web/WebPageGuard.kt`
- `app/src/main/java/com/supfy/app/web/WebUiState.kt`
- `app/src/main/java/com/supfy/app/web/WebViewFactory.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/supfy/app/web/PageLoadTrackerTest.kt`
- `docs/GIRIS_1022_RAPORU.md`

