# Supfy 1026 — sıfır CSS yüksekliği kök neden düzeltmesi

31 Ekim 2026. Dal: `codex/fix-webview-zero-layout-height-1026`.

## Sonuç ve kanıt sınırı

**Siyah giriş ekranını üreten somut native yerleşim hatası Android'de tekrar
üretildi, tek değişkenli deneyle nedeni doğrulandı ve üretim kodu düzeltildi.**
Fiziksel Xiaomi/WebView156 üzerinde gerçek hesap girişi, oturum kalıcılığı ve
müzik/DRM ayrıca doğrulanmalıdır. Bu belge yerel testi gerçek hesap testi saymaz.

## Hata zinciri

`SupfyScreen` mevcut WebView'ı Compose `AndroidView` içinde kullanıyordu.
`WebViewFactory.create` native `layoutParams` vermiyordu. Compose1.7.6
AndroidViewHolder'ın parametresiz `addView(view)` çağrısı Android'in varsayılan
`WRAP_CONTENT/WRAP_CONTENT` parametrelerini oluşturuyordu.

Compose görünümü ekrana uygun ölçse de Chromium, **layoutParams.height** değerini
ayrıca okur. WRAP_CONTENT ise `forceZeroLayoutHeight` renderer tercihini açar.
Blink bu tercihle CSS yerleşim yüksekliğini0 yapar. Native WebView.height ve
JavaScript innerHeight doğru olabilir; bunları ölçmek tek başına hatayı göstermez.

Spotify giriş CSS'sinde `html,body {height:100%}` ve top/bottom0 ile konumlanan,
`overflow:auto` kullanan absolute main vardır. CSS kök yüksekliği0 olunca main
48px'e çöker; aşağıdaki gerçek input/button öğeleri DOM'da/geometride mevcut
olmasına rağmen kırpılır. elementFromPoint HTML köküne döner. Sonuç siyah formdur.

## Aynı belgede kontrollü Android deneyi

Aynı1025 APK, API30/WebView83.0.4103.120, aynı temizlenmiş public SSR/CSS baytları,
aynı yazılımsal native layer1; hesap/çerez/script/iframe/ağ isteği yok.

| Ölçüm | Varsayılan / başarısız | Yalnız height=MATCH_PARENT |
|---|---:|---:|
| Native child layoutHeight | -2 (WRAP_CONTENT) | -1 (MATCH_PARENT) |
| Native child layoutWidth | -2 | -2 |
| Native/innerHeight |438|494|
| HTML/body yüksekliği |0/0|494/494|
| main yüksekliği |48|494|
| DOM'daki görünür kontrol |2|2|
| Kök HTML hit |2|0|
| Kontrol hit |0|2|
| Overflow dışına düşen kontrol |2|0|
| Native düğme dokunması |FAIL: null|PASS: yes|
| Görüntü |Tamamen siyah|E-posta ve Continue görünür|

Düğme merkezi iki durumda da CSS(160,412.65625), pencere(160,502.65625).
Dolayısıyla bu deney koordinatların tahmini düzeltilerek geçirilmedi. Native
yükseklikteki56px artış READY aşamasında tanı alt satırının küçülmesinin sonucudur.
Width, renderer layer, CSS, çerez ve güvenlik politikası değiştirilmedi.

İlk tam29 testte28 PASS/1 FAIL vardı: public SSR gerçek dokunması başarısızdı
(496.817s). İlk çizim/Compose yerleşimi öncesi dokunma olasılığı için iki görsel
kare ve UI-idle beklemesi eklendi; aynı test yine50.574s'de FAIL oldu. O başarısız
anda kaydedilen screenshot ve sayısal geometri sıfır CSS kökünü ortaya çıkardı.
Sonrasında yalnız layoutHeight değiştirilen deney54.523s'de PASS oldu. Başarısız
koşular kanıtlarda korunur; silinip başarılı gibi sunulmaz.

## Kaynak koduyla doğrulanan mekanizma

- Compose1.7.6 gerçek önbellek AAR bytecode: AndroidViewHolder addView(view)
  parametresiz; child için ayrı MATCH_PARENT ataması yok.
- Android ViewGroup varsayılan LayoutParams WRAP_CONTENT/WRAP_CONTENT:
  https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/ViewGroup.java
- Kullanılan83.0.4103.120 provider AwContents.java717–725: container height==-2:
  https://chromium.googlesource.com/chromium/src/+/83.0.4103.120/android_webview/java/src/org/chromium/android_webview/AwContents.java#717
- AwLayoutSizer.java191–194 flag aktarımı:
  https://chromium.googlesource.com/chromium/src/+/83.0.4103.120/android_webview/java/src/org/chromium/android_webview/AwLayoutSizer.java#191
- aw_settings.cc426–427 renderer tercihi:
  https://chromium.googlesource.com/chromium/src/+/83.0.4103.120/android_webview/browser/aw_settings.cc#426
- Blink WebViewImpl2650–2658 CSS layout_size.height=0:
  https://chromium.googlesource.com/chromium/src/+/83.0.4103.120/third_party/blink/renderer/core/exported/web_view_impl.cc#2650

Aynı wrap-content mekanizması incelenen Chromium main kaynaklarında da mevcut.
WV156 binary'si doğrudan incelenmedi; kullanıcı cihazındaki doğrulama yerine geçmez.

## Üretim düzeltmesi

`WebViewFactory.kt`: WebView oluşturulurken, attachment ve ilk load öncesinde
`ViewGroup.LayoutParams(MATCH_PARENT,MATCH_PARENT)` atanır. Spotify sayfasına
height CSS'si enjekte edilmez; doğru native yerleşim niyeti sağlanır.

Final Android testi artık kendisi layoutParams değiştirmez veya deney parametresi
kullanmaz. Üretim factory değerlerini assert eder, aynı public CSS düzenini çizer,
pencereye gerçek touchscreen gönderir ve pikselleri doğrular.

1025'teki salt okunur tanı ve kontrollü normal çizim denemesi korunur. Sıfır yüksekliği
gideren temel değişiklik renderer/çerez/TLS tercihi değil native layoutParams'tır.
Yeni sürüm1026 /0.6.6-login-layout-fix; paket ve yerel debug imza korunur.

## Önceki denemeler neden yetmedi?

- Popup/ömür/yanlış EMPTY hatası gerçek ayrı problemlerdi; düzeltmeleri native
  child'ın WRAP_CONTENT niyetini değiştirmedi.
- Native boyut, JavaScript, input sayısı ve CSS görünürlük doğruydu; ancestor
  overflow ile kırpılan form için bunlar yeterli görünürlük kanıtı değildi.
- Sade sentetik form normal belge akışındaydı veya min-height kullandığı için
  aynı absolute/percent-height bağımlılığı yoktu. FrameLayout testleri ayrıca
  açıkça(-1,-1) veriyordu; Compose'un parametresiz yolundaki hatayı gizliyordu.
- Eski JS.click testleri gerçek native hit hedefinden geçmiyordu.
- Daha uzun timeout, cache temizliği, masaüstü UA, cookie veya software layer
  tek başına force-zero-layout-height ayarını değiştirmiyordu.
- Bu nedenle public SSR/CSS, gerçek Compose penceresi, native dokunma ve piksel
  kanıtları birlikte eklendi. Simülasyon hiçbir aşamada gerçek oturum sayılmadı.

## Güvenlik, OAuth ve kalan sınırlar

Manifest izinleri/HTTPS/Safe Browsing/TLS iptali, kapalı file/content erişimi,
üçüncü taraf çerez tercihi ve medya izin kapısı korunur. CAPTCHA, DOM güvenlik
katmanı veya doğrulama ekranı kaldırılmaz. Root/özel API/DRM aşma/sertifika
atlatma/token dışa aktarımı ve parola formu taklidi yoktur.

Supfy mevcut hâliyle Web Player cookie oturumunu kullanır. Kullanıcı kayıtlı
Spotify Developer uygulaması olmadığını bildirdi. ClientID/kayıtlı redirect/izinli
hesap olmadan yeni PKCE/callback/token entegrasyonu **BLOCKED**. AppAuth/Web API
OAuth tokenı WebView Web Player cookie oturumunu otomatik oluşturmaz; farklı bir
entegrasyonu çalışmış gibi göstermedik. Uygulama içine Client Secret konmaz.

Ayrı native Supfy login/session gate henüz uygulanmış kabul edilmez; çerez veya
URL varlığı geçerli oturum gibi gösterilmedi. Web Player'ın gerçek giriş/oturum
sonucu kullanıcının desteklenen Spotify akışıyla ayrıca test edilmelidir.

Public replay yalnız yerel birlikte çalışabilirlik testidir. Spotify CSS/marka
capture'ı üretim APK'sına veya kaynak teslimine konmaz. Hesap/NextData/config/URL
query/token değerleri temizlenmiştir. Lisanslı dış auth kodu kopyalanmadı; Supfy
ve üçüncü taraf bildirimleri korunur. Reklam motoru değiştirilmedi; gerçek reklam
başarısı ve DRM/tam şarkı bu doğrulamadan çıkarılamaz.


## Son doğrulanmış sonuçlar

| Kontrol | Sonuç | Kanıt / sınır |
|---|---|---|
| Kök neden mekanizması | PASS | Gerçek Compose bytecode + Android/Chromium kaynak zinciri |
| Siyah sayfanın Android tekrarı | PASS | Eski 1025 public SSR/CSS: root0/main48/hitHTML2, native touch FAIL |
| Tek değişkenli deney | PASS | Aynı APK/provider/CSS/layer; yalnız height -2→-1;54.523s |
| 1026 üretim kodu hedefli testi | PASS | Test parametre değiştirmiyor; actual LP -1/-1; form ve native touch;52.86s |
| Temiz Gradle build/unit/lint/APK/testAPK | PASS | clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest;1m46s |
| JVM testleri | PASS |50 geçti; hata/başarısızlık0 |
| Android tam regresyon | PASS |29/29; 498.058s; API30/WebView83.0.4103.120; skip0 |
| Yeni public SSR çizim ve pencere dokunması | PASS | Üretim factory; CSS/main yüksekliği pozitif; hit2/rootHTML0; yerel çevrimdışı düzen |
| JavaScript reklam motoru testleri | PASS |29 sentetik DOM testi; önceki1025 koşusu; protection.js bu düzeltmede değişmedi |
| Tarayıcı tanı regresyonu | PASS |13 durum; aynı login_probe.js; native LayoutParams testi değildir |
| Lint | PASS |0 hata/4 uyarı; önemsiz mevcut kaynak/SDK uyarıları |
| 1024→1025→1026 install-r | PASS | Aynı imza; yerel files sentinel korundu; gerçek Spotify oturumu kanıtı değil |
| APK/Dex/manifest/imza | PASS | Bağımsız release incelemesi; LP(-1,-1) gerçek DEX içinde |
| Yeni 1026 fiziksel WebView156 | NOT RUN | Bağlı fiziksel ADB cihazı yok; kullanıcı doğrulaması bekleniyor |
| Android'de canlı Spotify hesabı/giriş/çıkış/kalıcılık | BLOCKED | Güvenli canlı ağ/test hesabı/bağlı telefon yok; TLS atlanmadı |
| Yeni OAuth PKCE/callback/token/refresh | BLOCKED | Kullanıcının kayıtlı Developer uygulaması/clientID/redirect yok |
| Ayrı native Supfy login/session gate | NOT RUN | Uygulanmış sayılmıyor; mevcut Web Player başlangıcı korunuyor |
| Gerçek reklam, DRM ve tam şarkı | NOT RUN | Bu login yerleşim doğrulamasından başarı çıkarılmaz |

Önemli ayrım: public SSR testi Spotify'ın anlık gerçek auth sunucusuyla bir oturum
oluşturmaz. Üretim APK'sı gerçek HTTPS sayfasını yükler; replay yalnız androidTest
istemcisindedir ve dış capture parametresi verilmezse bu tek test SKIP olur. Burada
`-e supfyReplayPath /data/local/tmp/supfy-login-replay.html` ile29 testin tamamı
çalıştı. Deneyde kullanılan `supfyMatchParent` kodu final test kaynağından çıkarıldı;
final test native parametreleri değiştirmeden üretim factory'sini doğrular.

## Değişen dosyalar

1024 son tam kaynak teslimine göre:

| Dosya | Değişiklik / kanıt |
|---|---|
|`README.md`|Güncel kök neden/kanıt sınırı|
|`app/build.gradle.kts`|1026 sürüm kimliği|
|`app/src/androidTest/java/com/supfy/app/MainActivityLoginTest.kt`|Asıl Compose LP assert + native window tap/piksel/public CSS regresyonu|
|`app/src/androidTest/java/com/supfy/app/WebViewProtectionTest.kt`|Recovery/terminal hata/tercih ve oynatıcı regresyonu|
|`app/src/main/assets/login_probe.js`|CSS kök/clip/transform geometri tanısı|
|`app/src/main/java/com/supfy/app/MainActivity.kt`|1025 kontrollü renderer bağlantısı; yeni wrapContentHeight sayısal alanı|
|`app/src/main/java/com/supfy/app/ui/SupfyScreen.kt`|1025 tanı/recovery kontrolü; başarı iddiası yok|
|`app/src/main/java/com/supfy/app/web/LoginDiagnostics.kt`|Sabit sayısal tanı alanları whitelist|
|`app/src/main/java/com/supfy/app/web/SupfyWebViewClient.kt`|1025 renderer tercihini auth adımlarında koruma|
|`app/src/main/java/com/supfy/app/web/WebPageGuard.kt`|1025 kontrollü recovery ve eski frame callback ayrımı|
|`app/src/main/java/com/supfy/app/web/WebUiState.kt`|Gerçek deneme durumu|
|`app/src/main/java/com/supfy/app/web/WebViewFactory.kt`|Asıl düzeltme: explicit MATCH_PARENT/MATCH_PARENT, yükleme öncesinde|
|`app/src/main/res/values/strings.xml`|Türkçe tanı/deneme metinleri|
|`docs/GIRIS_1025_DERIN_INCELEME.md`|Ara tanı/başarısızlık kaydı;1026 bu sonuca önceliklidir|
|`docs/GIRIS_1026_KOK_NEDEN_VE_TEST.md`|Bu yeni rapor|


## APK ve kurulum

```json
{
  "path": "/workspace/deliverables-login-1026/Supfy-0.6.6-1026-login-test.apk",
  "applicationId": "com.supfy.app",
  "versionCode": 1026,
  "versionName": "0.6.6-login-layout-fix",
  "sizeBytes": 9030217,
  "sha256": "9b1b0ea9821d1dd414eb2ca09e949ab8c02ee7fa254a56acbdc1f11395d94858",
  "signing": "Android Debug",
  "certificateSha256": "f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640"
}
```

1026 mevcut yerel 1024/1025 test kurulumunun üzerine kurulur. Uygulamayı/site verisini
silmeyin. Menü → Spotify giriş ekranı açın; altta 1026 yazmalı. Normal çizim düğmesine
basmadan formun görünmesi beklenir. Kullanıcı kendi desteklenen Spotify akışıyla
hesap girişini yapmalı; parola/çerez/token hiçbir rapora gönderilmemeli. Görünmezse
Tanıyı kopyala çıktısı wrapContentHeight ve root/main boyutlarını da içerir.

Orijinal 1018 farklı imzalıdır; o özel anahtar bu ortamda yoktur. Bu teslim önceki
aynı yerel debug imzasını korur. Release APK/mağaza/GitHub yayını yapılmadı; anahtar
kaynak/kanıt paketine konmaz. APK debug test sürümüdür, fiziksel hesap kabulü bekler.

## Önce/sonra görsel kanıt

Teslimde `once-android-yerel-siyah.png`, eski 1025 ile siyah replay penceresidir.
`sonra-android-yerel-form.png`, gerçek 1026 APK'sındaki aynı yerel düzenin tam
pencere görüntüsüdür. Bunlar emülatör/çevrimdışı test görüntüleridir; fiziksel
kullanıcı hesabı veya başarılı gerçek Spotify oturumu olarak sunulmaz.

Kalan ana kabul adımları: fiziksel156'da form + gerçek hesap girişi, oynatıcıya
dönüş, yeniden açınca oturum ve çıkış/yeniden giriş. Native login/session mimarisi
ve yeni PKCE entegrasyonu ayrı eksik kapsamdır; bu sürüm onları tamamlandı saymaz.
