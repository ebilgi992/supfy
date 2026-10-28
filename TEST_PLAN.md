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

## A2. Regresyon testleri (0.2.0)

Durum: ✅ geçti · ⏳ test bekliyor · ➖ uygulanmaz

| # | Test | Durum |
|---|---|---|
| 1 | Spotify giriş işlemi | ✅ Kullanıcı bildirdi (0.1.x) |
| 2 | Oturumun korunması | ✅ Kullanıcı bildirdi (0.1.x) |
| 3 | Şarkı oynatma | ✅ Kullanıcı bildirdi (0.1.x) |
| 4 | Şarkılar arası geçiş | ⏳ |
| 5 | Oynatmayı durdurma | ⏳ |
| 6 | Sayfa yenileme | ⏳ |
| 7 | Uygulamadan çıkıp dönme | ⏳ |
| 8 | Ekran kilitleme | ⏳ |
| 9 | İnternet kesilmesi | ⏳ |
| 10 | Wi-Fi ↔ mobil veri | ⏳ |
| 11 | Koruma kontrollerinin kapatılması | ➖ Reklam özelliği yok |
| 12 | Bellek ve pil tüketimi | ⏳ (bkz. D) |
| 13 | Reklam tespitinin doğruluğu | ➖ Reklam tespiti yok |
| 14 | Yanlış pozitiflerin oynatmaya etkisi | ➖ Reklam tespiti yok |
| 15 | Tema: sistem / açık / koyu geçişi | ⏳ |
| 16 | "Önbelleği temizle" sonrası oturumun kalması | ⏳ |

## A3. Güncelleme testleri (0.4.0)

| # | Test | Nasıl | Durum |
|---|---|---|---|
| U1 | İlk kurulum | Eski Supfy'yi kaldır, ilk imzalı APK'yı kur, giriş yap | ⏳ |
| U2 | Üzerine güncelleme | Sonraki imzalı APK'yı kaldırmadan kur → "Güncelle" demeli, ikinci simge oluşmamalı | ⏳ |
| U3 | Veriler korunuyor mu | U2 sonrası: oturum açık mı, tema/ayarlar aynı mı | ⏳ |
| U4 | Güncelleme penceresi | Daha yeni Release varken uygulamayı aç | ⏳ |
| U5 | "Daha Sonra" | Pencere 24 saat tekrar çıkmamalı; Ayarlar'dan elle denetim yine göstermeli | ⏳ |
| U6 | Güncel sürüm | Ayarlar → Güncellemeleri denetle → "Supfy güncel." | ⏳ |
| U7 | Bağlantı yok | Uçak modunda aç → açılış normal, pencere yok | ⏳ |
| U8 | Sürüm kodu / imza | Ayarlar'daki sürüm kodu Release adıyla aynı mı | ⏳ |

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
4. "Arka planda çalma" kapalıyken uygulamayı 60 dk arka planda bırakın → Ayarlar → Pil → Supfy tüketimini not edin (≈0 beklenir).

## E. Diğer üreticiler

Samsung (One UI), Pixel, OnePlus, OPPO/realme (ColorOS), vivo, Motorola: en az A1–A5 ve B'deki döndürme/geri tuşu testleri.
