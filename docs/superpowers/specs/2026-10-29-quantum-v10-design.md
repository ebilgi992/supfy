# Supfy V10 tasarımı

Yetki ve kapsam: kullanıcının V10 geliştirme talebi; uygulama ve APK teslimi isteniyor.
Başlangıç: `test-1018` / `8690ab2`, mevcut APK `com.supfy.app`, 1018 / 0.4.0.
Ana dal daha eski olduğundan APK etiketi temel alınır. Kullanıcı dosyaları korunur.

## Yaklaşım

Chrome MV3 API'leri Android'de yoktur. Bildirim erişimi ve sistem sesini değiştirme,
Supfy dışındaki uygulamaları etkilediği için seçilmedi. GPL myspoti kodu ve lisansı
belirsiz Spotilol kodu kopyalanmaz. Supfy'nin MIT lisansı korunur.

Seçim: bağımsız Kotlin koruma motoru, sınırlandırılmış WebView istek filtresi ve
yalnızca tam `https://open.spotify.com` kaynağında çalışan özgün DOM betiği.
Normal ses, hız, konum, oynatma/duraklatma ve hesap haklarına müdahale edilmez.
Reklam yalnızca açık reklam işaretlerinden anlaşılır; şarkı adı tek başına yeterli değildir.
Medya elemanının mute durumu saklanır; reklam sonu, kapatma, hata ve watchdog ile geri alınır.
Banner/promo seçicileri dar tutulur; giriş, izin, ödeme ve genel diyaloglar gizlenmez.
Ağ filtresi yalnızca bilinen üçüncü taraf reklam alanlarını GET alt isteklerinde engeller.
Spotify oturum, DRM, ses, spclient ve lisans uçları engellenmez. WebSocket/yanıt gövdesi
ve önbelleğe alınmış Service Worker yanıtları filtrelenemez.

Kotlin, ana çerçevede evaluateJavascript ile yalnızca sayısal durum alır; JS köprüsü yok.
Olay günlüğü sınırlı, yerel ve kişisel veri içermez. Modül varsayılan olarak kapalıdır;
koruma ekranı tercihleri, gerçek durum, sayılar ve hataları gösterir.

Giriş: mevcut güvenli çerez/HTTPS ayarlarını koru; kullanıcı hareketiyle açılan Spotify
pencerelerini aynı WebView'e yönlendir. Sağlayıcı engellerini aşma. Tam oynatma DRM,
WebView ve hesap haklarına bağlı; ölçülmeyen başarı iddia edilmez.

Güncelleme: mevcut sistemi koru; 1018'den yüksek versionCode, aynı applicationId.
Yeni imza eskiyle eşleşmezse üzerine kurulum iddia etme, kaldırma yapma.
Kalıcı anahtar yalnızca ortam değişkenleriyle sağlanır; hiçbir anahtar pakete/depo içine girmez.
Herkese açık push/release için ayrıca onay gerekir.

## Doğrulama

Önce mevcut sürüm derlemesi. Ardından Kotlin birim testleri, DOM simülasyonları,
lint ve APK derlemesi. İmza/paket/sürüm/hash doğrulaması. Gerçek cihaz veya emülatör
yoksa giriş/DRM/reklam/arka plan/üzerine güncelleme deneyleri açıkça beklemede kalır.
