# Güvenlik Politikası

## Açık bildirme

Güvenlik açığı bulduysanız herkese açık "Issue" açmayın. GitHub'ın **Security → Report a vulnerability** (özel bildirim) özelliğini kullanın. Bildirimler en kısa sürede yanıtlanır.

## Kapsam

- WebView yapılandırması (JavaScript, dosya erişimi, karışık içerik, Safe Browsing)
- Gezinme kuralları (`UrlPolicy`) ve DRM izin kapısı
- Dış bağlantı açma akışı
- Bağımlılıklar

## Uygulanan önlemler

- Şifresiz HTTP ve karışık içerik engelli; SSL hataları asla atlanmaz.
- JavaScript köprüsü yok; dosya/içerik erişimi kapalı.
- Yalnızca Spotify alan adları uygulama içinde açılır; diğerleri kullanıcı dokunuşuyla tarayıcıda açılır.
- `intent:`, `javascript:`, `file:` ve özel şemalar engellenir.
- Web sayfası izinleri: yalnızca `https://open.spotify.com` için korumalı medya; geri kalan her şey reddedilir.
- Sayfa motoru çökerse uygulama çökmeden yeni WebView kurulur.
- İmzalama anahtarları depoya konmaz (`.gitignore`).
- Bağımlılıklar Dependabot ile haftalık kontrol edilir.

Hiçbir yazılım tamamen risksiz değildir. En büyük risk yüzeyi, cihazdaki Android System WebView bileşenidir; Google Play üzerinden güncel tutulmalıdır.
