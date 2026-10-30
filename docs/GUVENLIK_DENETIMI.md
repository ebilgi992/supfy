# Güvenlik ve Performans Denetimi

Sürüm: 0.4.0 · Tarih: 28 Ekim 2026
Yöntem: kaynak kod incelemesi, Android 35 API'sine karşı tip kontrolü, birim testleri, GitHub Actions üzerinde Android Lint. Dinamik test (cihaz üzerinde trafik/bellek ölçümü) yapılmadı.

## Bulgular (önem sırasıyla)

### Giderildi (0.4.0): CI'daki debug anahtarı her derlemede değişiyordu
GitHub Actions her çalışmada yeni bir debug imza anahtarı üretir. Bu yüzden yeni APK eski kurulumun üzerine yüklenemez ("paket çakışıyor" hatası); her güncellemede kaldırıp yeniden kurmak ve yeniden giriş yapmak gerekir.
**Çözüm:** İmza artık GitHub Secrets'taki kalıcı anahtardan geliyor; anahtar ve şifreler depoda yok, CI çalışma sonunda anahtar dosyasını siliyor. Release yalnızca imzalı derlemelerde yayımlanıyor.

### Orta: Arka planda çalma açıkken WebView çalışmaya devam eder
"Arka planda çalmaya devam et" varsayılan olarak açık. Müzik çalmadığında bile Spotify sayfası arka planda bağlantısını sürdürebilir ve pil tüketir.
**Öneri:** Pil testinden (TEST_PLAN D bölümü) sonra varsayılanı yeniden değerlendirmek. İhtiyaç duymayan kullanıcı ayarı kapatabilir.

### Orta: Android System WebView en büyük saldırı yüzeyi
Güvenlik, cihazdaki WebView bileşeninin güncelliğine bağlı. Ayarlar → Cihaz bilgisi WebView sürümünü gösterir.
**Öneri:** WebView'i Google Play'den güncel tutmak.

### Düşük: Bağımlılık sürümleri Aralık 2024 tarihli
Dependabot güncelleme PR'ları açtı.
**Öneri:** CI yeşil olan PR'ları birleştirmek.

### Düşük: Gömülü WebView'de üçüncü taraf girişleri
Google, Facebook ve Apple girişleri dış tarayıcıya yönlendirilir ve oturum uygulamaya dönmez. Bu bir güvenlik açığı değil; kullanılabilirlik sınırlaması.

### Düşük (0.4.0 değişikliği): Üçüncü taraf çerezleri açıldı
Spotify'ın giriş sayfasındaki güvenlik doğrulaması (reCAPTCHA) iframe içinde çalışır ve üçüncü taraf çerezi ister; kapalıyken giriş ekranı takılabiliyordu. Ödün: Spotify sayfasına gömülü üçüncü taraf hizmetler kendi çerezlerini yazabilir. Çerezler yalnızca bu uygulamanın WebView'inde kalır ve "Oturumu ve site verilerini sil" ile silinir.

### Düşük: Güncelleme denetimi GitHub'a bağlanır
En fazla 12 saatte bir `api.github.com`'a kimliksiz bir GET isteği gider. Yanıt 512 KB ile sınırlı, 10 sn zaman aşımlı, yönlendirme izlenmiyor. Yalnızca `https://github.com/ebilgi992/supfy/releases/...` adreslerine güvenilir (birim testli). APK uygulama tarafından indirilmez veya kurulmaz.

### Bilgi: Doğrulanan korumalar
- İzinler: yalnızca `INTERNET` ve `ACCESS_NETWORK_STATE`.
- Şifresiz HTTP kapalı (network security config), karışık içerik engelli, SSL hataları iptal edilir.
- Safe Browsing açık; WebView kullanım istatistikleri kapalı (`MetricsOptOut`).
- Dosya ve içerik erişimi kapalı; Service Worker'lar için de kapalı.
- Uygulama, kimliğini Spotify'dan gizleyen hiçbir ayar (X-Requested-With bastırma, sahte istemci ipuçları) kullanmaz.
- JavaScript köprüsü (`addJavascriptInterface`) yok; sayfaya JS/CSS enjeksiyonu yok.
- Gezinme izin listesi: `spotify.com`, `scdn.co`, `spotifycdn.com`. `intent:`, `javascript:`, `file:` ve özel şemalar engelli. Benzer görünen alan adları (`evilspotify.com`) birim testleriyle doğrulandı.
- DRM izni yalnızca `https://open.spotify.com` kaynağına; mikrofon, kamera ve konum her zaman reddedilir.
- Yedekleme kapalı: oturum çerezleri Android yedeğine girmez.
- Loglama yok; kullanıcı verisi dış sunucuya gönderilmez.
- Sayfa motoru çökerse uygulama çökmeden yeni WebView kurulur; `onDestroy`'da WebView yok edilir (bellek sızıntısı önlemi).

## Performans

- Bağlantı takibi olay tabanlı; yoklama, WakeLock veya arka plan servisi yok.
- Kaynak projedeki 200 ms yoklama + tüm DOM'u izleyen MutationObserver yükü Supfy'de yok.
- Ekran dönünce Activity yeniden oluşmaz, sayfa yeniden yüklenmez.
- Release derlemesinde R8 küçültme ve kaynak budama açık.

Hiçbir uygulama tamamen risksiz değildir. Bu denetim, tespit edilen riskleri ve alınan önlemleri belgeler.
