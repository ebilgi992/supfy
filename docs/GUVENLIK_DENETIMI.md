# Güvenlik ve performans raporu — 0.5.0-v10

29 Ekim 2026. Kaynak incelemesi, Kotlin/DOM testleri, Android Lint ve APK statik kontrolleri.
Dinamik trafik analizi, penetrasyon testi veya tam bir bağımlılık CVE taraması yapılmış değildir.

## Korunan güvenlik sınırları

- HTTPS network security config ve MIXED_CONTENT_NEVER_ALLOW; SSL hataları cancel edilir.
- Safe Browsing açık; desteklenen özellikler WebViewFeature ile denetlenir.
- WebView ve Service Worker dosya/içerik erişimi kapalı. Yedekleme kapalı.
- Uygulama izinleri INTERNET ve ACCESS_NETWORK_STATE; kamera/mikrofon/konum reddedilir.
- Korumalı medya izni sadece tam HTTPS open.spotify.com kaynağına; farklı port ve userinfo reddedilir.
- addJavascriptInterface yok. Koruma yalnızca HTTPS oynatıcı ana çerçevesinde evaluateJavascript
  kullanır. JavaScript dönüşü sayısal/sınırlı enum verileriyle doğrulanır; komut çalıştırmaz.
- Gezinmede host sınırları, userinfo ve port kontrolleri; kullanıcı hareketi olmayan dış pencereler kapalı.
- Oturum sayfasında reklam motoru durur. Spotify ses/DRM/kimlik doğrulama uçları filtrelenmez.
- Güncelleme sorgusu sabit GitHub API adresine; yönlendirme takibi kapalı, yanıt 512 KiB ve 10 sn ile sınırlı.
  Release adreslerinde yabancı repo/host, port, userinfo, query/fragment ve kodlanmış yol reddedilir.
- İmza anahtarı yalnızca mevcut SUPFY_* ortam değişkenleriyle; kaynak ve teslim paketinde anahtar yok.

## Değişiklikler

Bağımsız koruma motoru ve fail-open davranışları eklendi. Hata/zaman aşımı/kapama/navigasyon
sırasında medya mute ve banner stilleri geri yüklenir. 20 sn ana uygulama heartbeat sınırı ve
120 sn kesintisiz reklam sınırı kalıcı yanlış susturmayı azaltır. Normal ses seviyesi, seek,
çal/duraklat, hız veya sistem ses akışı değiştirilmez. Geçici Spotify penceresi aynı WebView
oturumuna yönlendirilir. Native callbacks gezinme/tercih nesliyle doğrulanır.

## Kalan riskler ve sınırlar

- JavaScript betiği Spotify sayfasıyla aynı JS ortamındadır. Spotify betiği devre dışı bırakabilir,
  DOM işaretlerini değiştirebilir veya sayısal tanılamayı etkileyebilir. Bunlar native yetki sağlamaz.
- EME varlığı Widevine/Spotify uygunluğu kanıtı değildir. WebView/hesap sınırları aşılmaz.
- Sadece DOM'da görülebilen HTML medya elemanları susturulur. WebAudio, iframe veya native ses
  kapsanmaz. Şarkı atlama ve reklam hızlandırma uygulanmaz.
- Koruma açıkken ana uygulama durum kontrolü önde 3 sn, arkada 10 sn; DOM değişimleri 100 ms
  birleştirilir. Koruma kapalıyken periyodik kontrol yoktur. Pil ve HyperOS süreç sonlandırma
  davranışı gerçek Xiaomi cihazında ölçülmelidir; foreground playback service eklenmedi.
- Günlükler son 40 sabit metinle sınırlı, kişisel veri içermez. İşlem sayıları aktif oturuma aittir.
- Cihazdaki Android System WebView güncel tutulmalıdır. Mevcut Android bağımlılıkları topluca
  yükseltilmedi; yeni üretim bağımlılığı yoktur. CVE'siz ürün iddiası yapılmaz.
- Koruma Spotify koşullarına aykırı olabilir; hesabın kısıtlanması/kapatılması riski vardır.
- İmza devamlılığı bu ortamda sağlanamıyor: önceki APK sertifikası ile yeni debug sertifikası
  farklı. Aynı applicationId ve yüksek versionCode tek başına üzerine kurulum için yeterli değil.

APK, Lint ve cihaz/fixture kontrolünün sayısal sonuçları [TEST_RAPORU_V10.md](TEST_RAPORU_V10.md)
içindedir. Statik kontrollerin geçmesi gerçek reklam engellemenin doğrulandığı anlamına gelmez.
