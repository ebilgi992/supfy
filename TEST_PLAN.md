# Test Planı

Her satır için sonucu **Geçti / Kaldı / Kısmen** olarak işaretleyin ve not düşün.
Ayarlar → Cihaz bilgisi bölümündeki WebView sürümünü de kaydedin.

**Cihaz:** Xiaomi 14T Pro · HyperOS ___ · Android ___ · WebView ___

## A. Prototip doğrulaması (zorunlu)

| # | Test | Nasıl | Sonuç | Not |
|---|---|---|---|---|
| 1 | Giriş ekranı açılıyor mu? | Uygulamayı aç → "Oturum aç" | | |
| 2 | Oturum açılabiliyor mu? | E-posta + şifre veya e-posta kodu | | |
| 3 | Oturum korunuyor mu? | Uygulamayı kapat (son uygulamalardan kaydır), tekrar aç | | |
| 4 | Oynatma başlıyor mu? | Bir şarkıya dokun | | |
| 5 | Tam uzunluk çalıyor mu? | Bir şarkıyı sonuna kadar dinle (önizleme mi tam mı?) | | |
| 6 | DRM hatası var mı? | Hata mesajı var mı? Yoksa "Masaüstü sitesini iste" ile tekrar dene | | |
| 7 | Ekran kilitlenince ne oluyor? | Çalarken ekranı kilitle, 5 dk bekle | | |
| 8 | Kulaklık / Bluetooth | Kablolu ve BT kulaklıkla çal; bağlantıyı kopar | | |
| 9 | HyperOS arka plan kısıtlaması | Çalarken ana ekrana dön, 15 dk bekle | | |

## B. Kararlılık

| Test | Beklenen |
|---|---|
| İlk açılış | Yükleme çubuğu, sonra Spotify sayfası |
| Uçak modu açık başlatma | "Sayfa açılamadı" + "Tekrar dene"; bağlantı gelince otomatik yükleme |
| Wi-Fi ↔ mobil veri geçişi | Gösterge güncellenir, çökme yok |
| Ekranı döndürme | Sayfa yenilenmez, çalma kesilmez |
| Geri tuşu | Önce sayfa geçmişi, sonra uygulamadan çıkış; Ayarlar açıkken önce Ayarlar kapanır |
| Zorla durdurma ve yeniden açma | Oturum korunur |
| Cihazı yeniden başlatma | Oturum korunur |
| Pil tasarrufu modu | Çökme yok; arka planda çalma kesilebilir (not edin) |
| "Verileri sil" | Oturum kapanır, giriş ekranı gelir |
| Spotify dışı bağlantı | Tarayıcıda açılır |

## C. Arayüz

Küçük ekran (≤ 6"), büyük ekran / tablet, yatay mod, sistem yazı boyutu en büyük, TalkBack ile üst çubuk düğmeleri, Türkçe karakterler (İ, ı, ş, ğ).

## D. Pil (Xiaomi 14T Pro)

1. Şarjı %100'e getirin, diğer uygulamaları kapatın.
2. 60 dk ekran kapalı müzik çalın → pil yüzdesi düşüşünü not edin.
3. Aynı testi resmi Spotify uygulamasıyla yapın ve karşılaştırın.
4. "Arka planda çalma" kapalıyken uygulamayı 60 dk arka planda bırakın → Ayarlar → Pil → Tını tüketimini not edin (≈0 beklenir).

## E. Diğer üreticiler

Samsung (One UI), Pixel, OnePlus, OPPO/realme (ColorOS), vivo, Motorola: en az A1–A5 ve B'deki döndürme/geri tuşu testleri.
