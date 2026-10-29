# V11 kaynak, mimari ve güvenlik raporu — 29 Ekim 2026

Mevcut proje V10 `e08f049` üzerinden `codex/quantum-v11` dalında geliştirildi; proje yeniden
oluşturulmadı. [V10 ayrıntılı referans incelemesi](KAYNAK_ARASTIRMASI_V10.md) korunur.
GitHub kaynakları V11 sırasında tekrar denetlendi; incelenen commit'ler değişmemişti.

| Referans | Commit / son değişim / lisans | Karar |
|---|---|---|
| YASMANTH-VEMALA/spotify-ad-blocker | `29f5bca4484873bb5a650605b8968af8270b8c30` / 2026-09-08 / MIT | manifest, background, content, page_hook, rules ve CSS incelendi. MV3 WebView'de yoktur. background.js olay başına varsayılan 30 sn tasarruf sayıyor; Supfy bunu kullanmaz. Seek/hız/fetch/media prototype yamaları alınmadı. |
| abertschi/ad-free | `351de41a89e383791ad123855f8bf31704ce5a00` / 2026-08-28 / Apache-2.0 | Modüler algılayıcı yaklaşımı değerlendirildi. NotificationListener ve STREAM_MUSIC değişikliği yerine WebView'e özgü ses denetimi seçildi; kod alınmadı. |
| kota-zo/myspoti | `d9903724ce868b1b5336a3d7424527b78a8ed469` / 2026-10-29 / GPL-3.0 | WebView/oturum/medya servisleri incelendi. Özel API, relay, fingerprint, medya proxy veya GPL kodu alınmadı. Kaynaktaki çalışma iddiaları test kanıtı sayılmadı. |
| lyssadev/Spotilol | V10 incelemesinde 404 / lisans belirsiz | Kod alınmadı; teknik doğrulama tamamlanamadı. |

Mevcut AndroidX WebKit 1.12.1 resmî API'leriyle bağımsız Kotlin/JS motoru seçildi.
Yeni üretim bağımlılığı eklenmedi. MIT ve üçüncü taraf telif bildirimleri korundu;
GPL yeniden kullanım onayı gerektiren bir kod aktarımı yapılmadı.

## Motor

ProtectionEngine durum/tercih/istek sınıflandırma/sayım/günlük tutar. ProtectionWebController
WebView bağlama, sınırlı JSON sorgusu, SW istemcisi, belge başlangıcı ve native ses kontrolünü
yönetir. AdAudioSession/AudioRecovery önceki sessiz durumunu saklar; geri alma hatasında
yeni susturmayı durdurur, geri alma denemelerine devam eder. Tercihler AppSettings,
arayüz ProtectionScreen içindedir.

- DOCUMENT_START_SCRIPT destek kontrolüyle yalnızca tam HTTPS open.spotify.com kökenine
  eklenir; destek yoksa commit/finish sonrasında enjeksiyon vardır. Giriş sayfalarına eklenmez.
  documentElement başlangıçta yoksa DOMContentLoaded beklenir.
- Görünür advertiser/overlay işareti, medya veya native destekli oynatma kontrolü ve normal
  track/episode bağlantısının bulunmaması birlikte aranır. Şarkı adı tek başına kanıt değildir.
  Seçiciler Spotify'ın resmî olay API'si değildir; değişebilir ve yanlış tespit olabilir.
- DOM muted ve destek kontrolüyle WebView MUTE_AUDIO kullanılır. Sistem ses düzeyi, hız,
  seek ve DRM değiştirilmez. Native API desteği fiziksel reklam sesinin susturulduğunu kanıtlamaz.
- Banner/tanıtım stilleri geri alınabilir; genel giriş/izin/ödeme modalları gizlenmez.
  class/style değişimleri ve durum sorgusuyla tarama izlenir.
- Müzik dönüşü normal içerik bağlantısı + oynatma kanıtıyla sayılır. Sessizden çıkma ayrı sayılır.
- Ağ filtresi oynatıcı bağlamında üç bilinen üçüncü taraf reklam host'unun HTTPS GET alt
  istekleriyle sınırlıdır. Spotify oturum/ses/spclient/lisans/POST/ana sayfa engellenmez.
  ServiceWorkerClientCompat feature gate ile kullanılır; SW cache, WebSocket ve redirect
  zincirinin tüm adımları üzerinde genel ağ engelleme yetkisi yoktur.
- Ana gezinme güvenliği ayrı NavigationRequestGate katmanıdır. POST üst gezinmelerinde de
  güvenilmeyen köken I/O öncesi 403 alır; form içeriği okunmaz/tekrar gönderilmez.
- Foreground 1 sn, background 5 sn sorgu; master kapalıyken sürekli sorgu yoktur.
  Native geri alma için üç sınırlı deneme ve en fazla bir otomatik WebView yenileme vardır.
  Sonraki hata kullanıcıya normal oynatmaya dönüş sunar. Çerez/site verisi silinmez.
- JS 20 sn heartbeat ve 120 sn tek reklam sınırında müdahaleleri geri almayı dener.
  Renderer kaybında ölü WebView'e JS/ses API çağrısı yapılmaz; hata görünür, koruma durur.

DOM/native ses/filtre/görüntüleme işlemleri ayrı sayılır. Snapshot v2, en fazla 4096 karakter,
sayısal sınırlar ve sayaç gerileme kontrolü vardır. Son 40 statik Türkçe günlük URL/başlık/
kimlik bilgisi içermez. Sayılar çalışma oturumuna aittir; kalıcı geçmiş gibi sunulmaz.

## Giriş ve oynatma

V10 popup'ı durdurup GET olarak tekrar açabiliyor, JS'i kapatıyor ve süreyle yok ediyordu.
V11 gerçek popup belgesini, JS/DOM Storage'ı ve aynı çerez oturumunu kullanıcı kontrollü
Compose penceresinde korur. HTTPS/köken/POST/TLS/izin kontrolleri uygulanır. Oynatıcıya
dönüşte CookieManager flush edilir. Gerçek hesapla dönüş doğrulanmadı.

Resmî accounts.spotify.com login continue adresi menüye bağlandı. Anonim HTTP 307→200
yalnızca sunucu erişilebilirliğidir; WebView giriş testi değildir. Geçerli 22 karakterli
spotify: içerik bağlantıları kullanıcı hareketiyle HTTPS oynatıcıya çevrilir; keyfi intent
veya özel API yoktur. Yeni tercihlerde masaüstü görünümü mobil uygulama önerisini azaltmayı
amaçlar; oynatma garantisi değildir. Mevcut tercih değiştirilmez.

Harici Google/Apple/Facebook sağlayıcıların gömülü giriş kısıtları korunur. Kullanıcı hareketli
HTTPS dış kimlik doğrulama açılabilir; harici tarayıcı çerezleri WebView'e kopyalanmaz.
Bu sağlayıcılarla uçtan uca dönüş doğrulanmadı. Tam şarkı hesap hakları, EME/Widevine ve
Spotify WebView desteğine bağlıdır; EME API varlığı oynatma kanıtı değildir.

## Güvenlik ve performans

Üretim izinleri INTERNET ve ACCESS_NETWORK_STATE. Debug test Activity dışa kapalıdır;
release'e dahil değildir. HTTPS/TLS iptali/Safe Browsing açık; cleartext, dosya/content/
geolocation ve üçüncü taraf çerezler kapalıdır. Yalnızca tam oynatıcı kökenine korumalı
medya izni verilir; kamera/mikrofon verilmez. JavaScript köprüsü, root, VPN, bildirim
dinleyici ve erişilebilirlik yoktur. Şifre/çerez/token okunmaz veya dışarı aktarılmaz.

Debuggable APK test içindir. Özgün release anahtarı bulunmadığından o imzayla devamlılık
sağlanamadı. Anahtar/parolalar kaynak veya teslim paketine alınmadı. Public push/release yoktur.
Activity/WebView bağları ayrılır; renderer temizliği ayrı yapılır, tekrar çökmede yeniden
yükleme sınırı vardır. Döndürme mevcut configChanges ile korunur. Fiziksel bellek/pil/ANR,
uzun arka plan ve HyperOS davranışları ölçülmedi. TCG emülatör sonuçları cihaz performansı
olarak sunulmaz. Statik 16 KiB hizalama gerçek cihaz çalıştırma testi değildir.

Spotify koşullarına aykırılık hesap kısıtlanması/kapatılması riski taşır; arayüz ve README
uyarır. Samet Özdemir arayüzde gösterilmez; zorunlu telif bildirimleri korunur.
