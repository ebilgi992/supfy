# Özellik Matrisi

Supfy sürümü: 0.4.0 · Tarih: 28 Ekim 2026

**Durum anahtarı:** Tamamlandı · Kısmen çalışıyor · Desteklenmiyor · Test bekliyor

## Supfy özellikleri

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
| Koruma ve durum ekranı | Test bekliyor | Etkin güvenlik korumaları ve cihaz desteği; sayaçlar yalnızca gerçek WebView olaylarından oluşur. |
| Sayfayı sıfırlama | Test bekliyor | WebView yeniden kurulur, oturum korunur. |
| Mevcut kurulumun üzerine güncelleme | Test bekliyor | Sabit applicationId + kalıcı imza + artan sürüm kodu. İlk imzalı sürümden sonraki güncellemede doğrulanabilir. |
| Uygulama içi güncelleme denetimi | Test bekliyor | Seçim mantığı 8 birim testiyle doğrulandı; ağ akışı cihazda denenmedi. |
