# Değişiklik Günlüğü

## [0.2.0-prototip] - 2026-10-28

### Değişti
- Uygulama adı **Supfy**; paket adı `com.sametozdemir.supfy` (önceki Tını sürümü ayrı uygulama olarak kalır, kaldırılmalıdır).
- Yeni logo: uçları nota başıyla biten "S"; adaptive, monokrom, açılış ekranı, açık/koyu logo.

### Eklendi
- Tema seçimi: sistem / açık / koyu; durum çubuğu simgeleri temaya uyar.
- Ayrı "Önbelleği temizle" düğmesi (oturum korunur).
- Service Worker'lar için dosya/içerik erişimi kapatıldı.
- Belgeler: özellik matrisi, güvenlik denetimi, regresyon test planı.
- CI: her `main` derlemesinde APK GitHub Releases'a eklenir.

## [0.1.1-prototip] - 2026-10-28

### Değişti
- Yeşil-siyah renk paleti ve nota simgesi.

## [0.1.0-prototip] - 2026-10-28

### Eklendi
- open.spotify.com için güvenli WebView kabuğu (Kotlin + Jetpack Compose).
- Alan adı izin listesi, HTTPS zorunluluğu, Safe Browsing, DRM izin kapısı.
- Türkçe hata ekranları, bağlantı göstergesi, otomatik yeniden deneme.
- Ayarlar: arka planda çalma, masaüstü sitesi, veri silme, izinler, cihaz bilgisi, hakkında.
- İlk marka kimliği: adaptive ve monokrom ikon, Android 12+ açılış ekranı.
- Birim testleri, GitHub Actions (test, Lint, debug APK), Dependabot.
