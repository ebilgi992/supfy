# Kaynak araştırması — 29 Ekim 2026

İnceleme bağlı GitHub üzerinden yapıldı. Referans kaynaklar kaynak paketine veya uygulamaya
toplu olarak kopyalanmadı. Supfy MIT olarak kaldı. APK'da THIRD_PARTY_NOTICES.txt bulunur.

| Kaynak | İncelenen sürüm / lisans | Mimari ve seçim |
|---|---|---|
| [spotify-ad-blocker](https://github.com/YASMANTH-VEMALA/spotify-ad-blocker) | 29f5bca / MIT | content.js açık reklam işaretleri yanında başlık ve progressbar sezgileri kullanıyor; 200 ms yoklama, MutationObserver, CSS, MV3 declarativeNetRequest. page_hook.js global media/fetch yamaları, seek, 16x hız ve mute kullanıyor. Supfy daha dar açık DOM işaretleri kullanır; başlık sezgisi, seek/hız, fetch/prototype yamaları ve Chrome API'leri alınmadı. Üçüncü taraf ağ alanları için host sınırları doğrulanır; spclient/ad ses/DRM uçları engellenmez. |
| [ad-free](https://github.com/abertschi/ad-free) | 351de41 / Apache 2.0 | AdDetector olayları, ayrı algılayıcılar ve eklenti mimarisi. AudioController STREAM_MUSIC seviyesini saklayıp sıfırlıyor ve eski seviyeye dönüyor; bu tüm cihaz sesini etkileyebilir ve kullanıcının son ses tercihini ezebilir. NotificationListener ve eski Rx/Anko bağımlılıkları Supfy'ye alınmadı. Bağımsız motor ve medya öğesine özel mute tercih edildi. Kod kopyalanmadı. |
| [myspoti](https://github.com/kota-zo/myspoti) | d990372 / GPL-3.0 | WebPlayerHost oturum, desktop WebView, cookie ve korumalı medya; PlaybackService ayrı MediaSession/ön plan servisi; UpdateChecker sürüm sıklığı/Release seçimi. Güncel kaynakta özel API/relay/fingerprint/medya proxy bileşenleri de var. Supfy bu yolları, izin kapısını genişletmeyi veya köprü kodunu kullanmaz. GPL kodu kopyalanmadı; yeniden lisanslama gerekmedi. Supfy'nin mevcut güncelleyicisi korundu. |
| [Spotilol](https://github.com/lyssadev/Spotilol) | GitHub 404 | Erişilebilir kaynak/lisans doğrulanamadı. Teknik inceleme tamamlanamadı; kod alınmadı. |

Seçim ölçütleri: kullanıcı hesabı ve WebView güven sınırını koruma, lisans uyumu,
mevcut minSdk 26 desteği, az bağımlılık, saf Kotlin ve DOM testleri, geri alınabilir
müdahaleler, olay tabanlı tarama. Üretime yeni Android bağımlılığı eklenmedi;
yalnızca AndroidX instrumentasyon araçları ve jsdom test bağımlılıkları eklendi.

## Resmî Android API sınırları

- WebViewClient.shouldInterceptRequest ve destek kontrolüyle ServiceWorkerClientCompat
  alt kaynak isteklerinde sınırlı filtre sağlar. Yanıt içeriği, WebSocket, redirect zincirinin
  bütün adımları ve SW önbellek yanıtları üzerinde genel Chrome eklentisi yetkisi sağlamaz.
- evaluateJavascript yalnızca tam HTTPS oynatıcı kaynağında; kimlik doğrulama sayfalarında yok.
- WebChromeClient güvenli kullanıcı penceresi ve yalnızca korumalı medya izin kapısını yönetir.
- WebViewFeature ile Service Worker ve Safe Browsing yetenekleri denetlenir.
- EME API varlığı Widevine sertifikasyonu veya Spotify tam oynatma uygunluğu kanıtı değildir.

Kaynakların canlı Spotify üzerinde çalıştığı bağımsız olarak doğrulanmadı. Araştırmadaki
iddialar Supfy için başarı kanıtı sayılmadı. Reklam engelleme Spotify koşullarına aykırı
olabilir; hesap kısıtlaması veya kapatılma riski uygulamada ve belgelerde belirtilir.
