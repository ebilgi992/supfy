# Değişiklik Günlüğü

## [0.4.0] - 2026-10-29

### Düzeltildi
- **Yeni APK'nın ayrı uygulama gibi kurulması.** İki neden vardı: paket adı değişikliği ve her CI derlemesinin farklı bir geçici anahtarla imzalanması. `applicationId` artık `com.supfy.app` olarak sabit; imza GitHub Secrets'taki kalıcı anahtardan geliyor; sürüm kodu her derlemede artıyor (1000 + çalışma no).
- "Oturumu ve site verilerini sil" sonrası sayfa, çerez silme bitmeden yükleniyordu; artık silme tamamlanınca yükleniyor.
- Sayfa motoru kısa sürede tekrar çökerse sonsuz yeniden kurma döngüsü engellendi.
- Büyük WebView geçmişinde kayıt verisi sınırını aşıp çökme riski giderildi (200 KB sınırı).
- Kullanılmayan `ui-tooling` bağımlılıkları kaldırıldı.

### Eklendi
- Uygulama içi güncelleme denetimi (GitHub Releases): açılışta en fazla 12 saatte bir, "Daha Sonra" 24 saat erteler, Ayarlar'dan elle denetim. "Güncelle" doğrulanmış Release sayfasını açar; sessiz kurulum yok.
- CI: imzalı sürüm APK'sı; Release yalnızca imzalı derlemelerde yayımlanır.

## [0.3.0-prototip] - 2026-10-28

### Değişti
- Geliştirici adı uygulamadan, belgelerden ve paket adından kaldırıldı; yeni paket adı `com.supfy.app` (önceki sürüm ayrı uygulama olarak kalır, kaldırılmalıdır).

### Eklendi
- "Koruma ve durum" ekranı: etkin güvenlik korumaları, cihaz desteği göstergesi, bu oturumda gerçekten gerçekleşen işlemlerin sayaçları, sayfayı sıfırlama.

## [0.2.0-prototip] - 2026-10-28

### Değişti
- Uygulama adı **Supfy**; paket adı değişti (önceki Tını sürümü ayrı uygulama olarak kalır, kaldırılmalıdır).
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
