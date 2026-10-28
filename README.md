<p align="center"><img src="branding/supfy-logo-dark.png" alt="Supfy" width="320"></p>

# Supfy

Spotify Web Player'ı Android'de kendi sade penceresinde açan, tamamen Türkçe, açık kaynak bir WebView uygulaması.

> **Durum:** 0.2.0 prototip. Giriş, oturum ve oynatma Xiaomi 14T Pro'da kullanıcı tarafından doğrulandı; diğer testler için bkz. [TEST_PLAN.md](TEST_PLAN.md).
>
> Raporlar: [Özellik matrisi](docs/OZELLIK_MATRISI.md) · [Güvenlik denetimi](docs/GUVENLIK_DENETIMI.md) · [Marka](branding/README.md)

## Ne yapar, ne yapmaz

**Yapar**
- `https://open.spotify.com` adresini uygulama içinde açar; giriş Spotify'ın kendi sayfasında yapılır.
- Oturumu cihazda (WebView çerezleri) korur.
- Ekran dönünce sayfayı yenilemez; geri tuşu sayfa geçmişinde geri gider.
- Bağlantı durumunu gösterir, hataları Türkçe açıklar, bağlantı gelince otomatik yeniden dener.
- Oynatma mümkün olmazsa sayfayı tarayıcıda açma seçeneği sunar.

**Yapmaz**
- Reklam engellemez, susturmaz, hızlandırmaz veya atlamaz. Spotify Kullanıcı Yönergeleri bunu yasaklar.
- Sayfaya JavaScript/CSS enjekte etmez; ağ isteklerini değiştirmez.
- DRM korumasını atlatmaz; yalnızca sayfa istediğinde standart "korumalı içerik" iznini verir.
- Şifreni okumaz, kaydetmez veya bir sunucuya göndermez. Analiz/izleme aracı içermez.

## Güvenlik ayarları (özet)

| Ayar | Değer |
|---|---|
| JavaScript / DOM Storage | Açık (Spotify Web Player için zorunlu) |
| Dosya ve içerik erişimi | Kapalı |
| JavaScript köprüsü | Yok |
| Şifresiz HTTP | Engelli (network security config + karışık içerik kapalı) |
| Safe Browsing | Açık |
| Üçüncü taraf çerezler | Kapalı |
| DRM izni | Yalnızca `https://open.spotify.com` kaynağına |
| Uygulama içinde açılan alan adları | `spotify.com`, `scdn.co`, `spotifycdn.com` ve alt alan adları |
| Uygulama izinleri | `INTERNET`, `ACCESS_NETWORK_STATE` |
| Yedekleme | Kapalı (oturum çerezleri yedeğe girmez) |

## Teknoloji

Kotlin 2.1 · Jetpack Compose (Material 3) · AndroidX WebKit · Gradle Kotlin DSL · minSdk 26 (Android 8.0) · targetSdk 35

## Derleme

**Android Studio:** Projeyi açın, Gradle senkronizasyonunu bekleyin, `app` yapılandırmasını çalıştırın.

**Komut satırı:**
```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

**GitHub Actions:** `main` dalına her gönderimde testler, Lint ve debug APK derlenir; APK "Artifacts" bölümünden indirilir.

## Kurulum (debug APK)

En kolay yol: deponun **Releases** bölümünden en son `Supfy-...-debug.apk` dosyasını telefondan indirin.

1. APK dosyasını açın.
2. Dosyayı açın; istenirse "Bilinmeyen uygulamaları yükle" iznini yalnızca o dosya yöneticisi için verin.
3. Kurulumdan sonra Supfy'yi açın ve Spotify hesabınızla giriş yapın.

**Güncelleme:** Şimdilik her CI derlemesi farklı bir debug anahtarıyla imzalanır; yeni sürümü kurmadan önce eskisini kaldırmanız gerekir (bkz. güvenlik denetimi).

**Xiaomi / HyperOS:** Arka planda çalma kesiliyorsa Ayarlar → Uygulamalar → Supfy → Pil tasarrufu → "Kısıtlama yok" seçeneğini elle seçebilirsiniz. Uygulama bu ayarı kendisi değiştirmez.

## Bilinen sınırlamalar

- Google / Facebook / Apple ile giriş, gömülü WebView'de sağlayıcılar tarafından engellenebilir. E-posta ve şifre ya da e-posta ile giriş kodu kullanın.
- Spotify'ın mobil web sayfası oynatıcıyı açmazsa Ayarlar'daki "Masaüstü sitesini iste" seçeneğini deneyin.
- WebView, Android medya bildirimine ve kilit ekranı kontrollerine kendiliğinden bağlanmaz.
- "Tarayıcıda aç", telefonda Spotify uygulaması kuruluysa bağlantıyı o uygulamada açabilir.

## Kaynak proje hakkında

Araştırma aşamasında [spotify-ad-blocker](https://github.com/YASMANTH-VEMALA/spotify-ad-blocker) (MIT, Yasmanth Vemula) incelendi. Bu depoda o projeden **kod kopyalanmamıştır**. Reklam engelleme mekanizmaları bilerek alınmamıştır.

## Yasal

Supfy bağımsız bir projedir; Spotify ile bağlantılı değildir ve Spotify tarafından onaylanmamıştır. Spotify, Spotify AB'nin tescilli markasıdır.

Lisans: [MIT](LICENSE) · Geliştirici: Samet Özdemir
