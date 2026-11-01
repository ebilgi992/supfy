# Supfy 1025 — derin giriş incelemesi ve kontrollü çizim deneyi

31 Ekim 2026. Bu sürümün gerçek cihaz giriş sorunu çözülmüş değildir.

## Gerçek cihazdaki son durum: FAIL

1024 kullanıcının Xiaomi cihazında hâlâ tamamen siyah. SDK36, Android WebView
156.0.8078.47. `stage=8`, `error=-1`, `contentPresent=1`, `ready=0`.
Form oluşturulmuş: input1; boyut/viewport/CSS uygun kontrol2. İki kontrolün
merkez ve alternatif noktaları kök HTML öğesine gidiyor (`hitHtml=2`).
PointerNone/inert/clip-path-mask0; JS/console hata0; CSP sayacı1.
Native1220×2024 ile CSS375×622/DPR3.25 tutarlı.

1024 önceki yanlış EMPTY/native örtü sorununu giderdi; asıl WebView çizim sorunu
sürüyor. Native örtü tek başına document.elementFromPoint sonucunu HTML yapmaz.
Ağ/TLS hatası kaydı yoktur; bu tüm olası kaynakların başarılı olduğunu kanıtlamaz.
CSP sayacı1 tek başına güvenlik engeli veya çizim hatası nedeni değildir.

## Ajanların kanıtları

- Android/navigation: tek Activity + Compose + aynı WebView var. Ayrı splash,
  NavGraph, LoginViewModel, token/session doğrulama katmanı yok. MainActivity
  başlangıçta HOME_URL yükler. Yanlış OAuth callback, olmayan token veya route
  atlama public formun DOM'da var olup siyah çizilmesini açıklamaz.
- Auth: mevcut yol Spotify public login + CookieManager'dır. Client ID, PKCE,
  access/refresh token veya callback bulunmuyor. Çerez varlığı/gezinme URL'si
  geçerli kullanıcı oturumu gibi raporlanmamalıdır.
- Render: canlı public sayfada23 HTML/CSS/JS kaynak, pseudo/viewtransition/modal
  kontrolü yapıldı. Normal tarayıcıda şüpheli kök katman gözlenmedi. Telefon
  sunucu varyantının aynı olduğu varsayılmaz.
- Ayırıcı deney: html::after opak örtüsü ve hidden-backface dönüşümü iki farklı
  mekanizmayla telefonun eski metriklerini ve tamamen siyah pikselleri üretti.
  Dolayısıyla hitHtml2 belirli bir katmanın kaldırılması için yeterli kanıt değil.
- PaintHolding/GPU: güvenlik açık4 Chromium koşulunda public form göründü;
  PaintHolding açıklaması bu ortamda desteklenmedi. Android156 testi değildir.
- QA: önceki form eylem testleri JS.click kullanıyordu. PixelCopy çizimi doğrular,
  gerçek kullanıcı dokunmasını doğrulamaz. Yeni test actual window touchscreen
  olayını UiAutomation ile enjekte eder; üretim dokunmayı taklit etmez.

## Neden OAuth ekleyerek düzeltilmiş saymıyoruz?

Kullanıcı Supfy için kayıtlı Spotify Developer uygulamasının olmadığını bildirdi.
Gerçek public Client ID/kayıtlı redirect URI yok. Yeni PKCE uçtan uca doğrulaması
**BLOCKED**. APK içine client secret konmaz, başka projenin client ID'si alınmaz.

İncelenen resmi kaynaklar:

- https://github.com/spotify/android-sdk
- https://github.com/spotify/android-auth
- https://github.com/spotify/web-api-examples
- https://github.com/openid/AppAuth-Android
- https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow
- https://developer.spotify.com/documentation/web-api/concepts/redirect_uri

Depoların lisansları Apache-2.0; yeni kod kopyalanmadı. AppAuth external browser /
Custom Tabs kullanır; WebView'ı desteklemez. API tokenı almak WebView Web Player
çerez oturumunu oluşturmaz. Resmi kaynaklarda desteklenen bir token→WebView cookie
aktarım yolu bulunmadı. Dolayısıyla yeni native karşılama ekranı/API OAuth katmanı
bu siyah ekranı gidermiş gibi sunulmaz. Güncel redirect belgesi HTTPS/exact match
ister; loopback IP istisnası dışında uydurma localhost/custom scheme eklenmez.

## Güvenlik ve kapsam

TLS doğrulaması/Safe Browsing/HTTPS/file-content erişim sınırları ve üçüncü taraf
çerez tercihi korunur. DOM güvenlik/CAPTCHA katmanları kaldırılmaz; CSS değiştirme,
parola formu taklidi, token/çerez dışa aktarımı veya özel API yoktur. Tanılama
sabit sayısal alanlarla sınırlı ve kullanıcı kopyalama eylemiyle dışarı çıkar.
Reklam motoru bu aşamada değiştirilmez; canlı reklam başarısı iddia edilmez.

Yerel public HTML/CSS replay yalnız birlikte çalışabilirlik çizim deneyidir;
kimlik bilgisi/config/NextData/script/iframe/ağ kaynağı yoktur. Spotify görselleri
ve CSS'si üçüncü tarafa aittir; bu capture üretim APK'sına veya kaynak teslimine
konmaz. Yerel screenshot kullanıcı formunun görüntüsü değildir.
