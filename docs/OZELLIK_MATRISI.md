# Özellik matrisi — V10

0.5.0-v10 / 1019, 29 Ekim 2026. “Kod var” ile “gerçek Spotify üzerinde doğrulandı” ayrıdır.

| Özellik | Uygulama durumu | Doğrulama sınırı |
|---|---|---|
| Modüler reklam koruması ve tercihler | Kotlin motor/adaptör, yerel tercihler ve Compose kontrolleri | Birim ve DOM testleri; Android fixture sonucu test raporunda |
| Reklam tespiti | Görünür açık reklam işaretleri; şarkı başlığı sezgisi yok | Gerçek Spotify reklam seçicileri doğrulanmadı |
| Reklam sesi | DOM medya elemanına özel mute ve geri yükleme | Sistem/WebAudio/iframe sesi kapsanmaz |
| Oynatma durumu | play/pause/ended olayları, medya öğesi ölçümü | Koruma kapalıyken ölçülmez; Android MediaSession eklenmedi |
| Banner ve tanıtım | Dar data-testid seçicileri, geri alınabilir display | Genel modal kapatma yok; gerçek site seçicileri test bekliyor |
| Ağ filtresi | WebView ve destekliyse Service Worker alt GET istekleri | Sadece bilinen üçüncü taraf reklam alanları; ses/oturum/DRM engellenmez |
| Ana anahtar, hata, sayaç, günlük | Gerçek motor durumuna bağlı | Sayaçlar yalnızca bildirilen işlemler; oturumda sıfırlanır |
| Spotify giriş pencereleri | Güvenli kullanıcı penceresini aynı oturuma yönlendirme | Hesapla giriş ve oynatıcıya dönüş gerçek cihazda doğrulanmadı |
| Çerez/DOM storage | Mevcut akış korundu, sayfa bitiminde flush eklendi | Kimlik doğrulama sağlayıcıları WebView'i reddedebilir |
| Tam müzik ve DRM | Mevcut korumalı medya izin kapısı korundu, EME tanısı eklendi | Tam şarkı/abonelik hakkı ve Widevine uygunluğu doğrulanmadı |
| Tarayıcıda Aç | Uygulama menüsü ve hata ekranından kaldırıldı | Güncelleme ve gerekli güvenli dış bağlantılar korunur |
| Güncelleme bildirimi | Mevcut 12 saat/elle denetim, sürüm/URL testleri | GitHub'a yeni sürüm yayımlanmadı |
| Veri koruyan güncelleme | Aynı applicationId, artan kod, kalıcı imza yapılandırması | Eski verilen APK ile yeni imza farklı; bu APK onun üzerine kurulamaz |
| Güvenlik | HTTPS, SSL iptal, Safe Browsing, dosya erişimi kapalı, JS köprüsü yok | Tam güvenlik/CVE garantisi değildir |
| Arka plan ve ekran kilidi | Mevcut tercih korundu; koruma heartbeat 10 sn | HyperOS ve uzun süreli gerçek cihaz testi bekliyor |
| Kulaklık, Bluetooth, kilit ekranı kontrolleri | Önceki sınırlamalar korunur | Ayrı MediaSession/foreground service eklenmedi |
| APK | Debug APK üretilir, paket/imza/hash doğrulanır | Nihai ölçüler ve bağlantı teslim raporunda |

Önceki belgelerde yer alan 0.4.0 kullanıcı testleri bu yeni sürümün test sonucu sayılmaz.
