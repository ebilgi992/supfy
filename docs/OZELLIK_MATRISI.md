# Özellik Matrisi: Supfy ve spotify-ad-blocker

Kaynak: [YASMANTH-VEMALA/spotify-ad-blocker](https://github.com/YASMANTH-VEMALA/spotify-ad-blocker) (Chrome Manifest V3 eklentisi, MIT).
Supfy sürümü: 0.2.0-prototip · Tarih: 28 Ekim 2026

**Durum anahtarı:** Tamamlandı · Kısmen çalışıyor · Desteklenmiyor · Test bekliyor

## 1. Kaynak projenin reklam özellikleri

| # | Kaynak özellik | Kaynakta nasıl | WebView'de teknik karşılığı | Supfy durumu | Gerekçe |
|---|---|---|---|---|---|
| 1 | Reklam tespiti | `content.js`: 200 ms yoklama + MutationObserver, `data-testid` seçicileri | `evaluateJavascript` / `WebViewCompat.addDocumentStartJavaScript` ile aynı betik çalıştırılabilir | **Desteklenmiyor** | Tek amacı 2–6. maddeleri tetiklemek. Spotify Kullanıcı Yönergeleri madde 10 bu tür araçları yasaklıyor. |
| 2 | Reklam sesini susturma | `page_hook.js`: `HTMLMediaElement.muted = true` | Enjekte edilen JS ile mümkün | **Desteklenmiyor** | Reklamı atlatmak, yönergelerdeki yasağın tam kapsamında. |
| 3 | Reklamda 16× hızlandırma ve sona sarma | `playbackRate = 16`, `currentTime = duration` | Enjekte edilen JS ile mümkün | **Desteklenmiyor** | Aynı gerekçe. |
| 4 | Reklam sonrası normale dönme | `restoreMedia()` | Mümkün | **Desteklenmiyor** | 2–3 olmadan anlamsız. |
| 5 | Banner gizleme | `adblock.css` | CSS enjeksiyonu | **Desteklenmiyor** | Reklam alanlarını gizlemek de reklam engellemedir. |
| 6 | Premium pencerelerini otomatik kapatma | `dismissPromotions()` | Enjekte edilen JS | **Desteklenmiyor** | Spotify'ın tanıtımlarını otomatik atlatmak; aynı yasak. |
| 6b | Ağ isteği engelleme | `rules.json` (declarativeNetRequest), `fetch` kancası | `WebViewClient.shouldInterceptRequest` ile URL bazlı engelleme | **Desteklenmiyor** | Reklam uç noktalarını engellemek, yönergelerdeki yasağın kendisi. |
| 7 | Koruma aç/kapat | Popup anahtarı | Ayarlar anahtarı | **Desteklenmiyor** | Yönetilecek bir reklam özelliği yok. |
| 8 | İstatistik | Her reklam için sabit "30 sn kazanıldı" | DataStore sayacı | **Desteklenmiyor** | Kaynaktaki sayı ölçüm değil, tahmin. Supfy doğrulanamayan veri göstermez. |

### Hesap riski

Spotify Kullanıcı Yönergeleri, reklamları engellemeyi veya atlatmayı ve bunun için araç yapıp dağıtmayı açıkça yasaklıyor. Yaptırım: içerik kaldırma, hesabın askıya alınması veya kapatılması. Spotify bu kuralı 2019'dan beri uyguluyor. Deponun gizli olması ya da uygulamanın yalnızca kişisel kullanımda kalması kuralı değiştirmez. Engelleme Spotify'ın sunucularına karşı gerçekleşir ve hesap üzerinden tespit edilebilir.

## 2. Supfy'nin kendi özellikleri

| Özellik | Durum | Gerekçe / Not |
|---|---|---|
| open.spotify.com WebView oynatıcı | **Tamamlandı** | Kullanıcı testi: çalışıyor (28.10.2026, Xiaomi 14T Pro). |
| Spotify'ın kendi sayfasında giriş | **Tamamlandı** | Kullanıcı testi: çalışıyor. Google/Facebook/Apple ile giriş WebView'de engellenebilir. |
| Oturumun korunması | **Tamamlandı** | Kullanıcı testi: çalışıyor. Çerezler `onPause`'da diske yazılır. |
| Ekran döndürmede yenilememe | Test bekliyor | `configChanges` ile Activity yeniden oluşmaz. |
| Geri tuşu | Test bekliyor | Önce Ayarlar, sonra sayfa geçmişi, sonra çıkış. |
| Türkçe hata ekranları ve otomatik yeniden deneme | Test bekliyor | Bağlantı gelince otomatik yeniden yükler. |
| Bağlantı göstergesi | Test bekliyor | Olay tabanlı, yoklama yok. |
| Arka planda çalma / ekran kilidi | Test bekliyor | WebView duraklatılmaz. HyperOS süreci kapatabilir. |
| Kulaklık / Bluetooth | Test bekliyor | Ses sistem çıkışından geçer; medya düğmeleri WebView'e bağlı değil. |
| Kilit ekranı medya kontrolleri | **Desteklenmiyor** | WebView, Android MediaSession'a kendiliğinden bağlanmaz. |
| Tema seçimi (sistem/açık/koyu) | Test bekliyor | Yalnızca Supfy ekranları; Spotify sayfası değişmez. |
| Önbellek ve oturum temizleme | Test bekliyor | İki ayrı düğme. |
| Masaüstü sitesi modu | Kısmen çalışıyor | Yalnızca mobil sayfa oynatıcıyı açmazsa gerekir. |
