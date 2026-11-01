# Supfy 1027 — medya, hesap ve koruma

Paket `com.supfy.app`, sürüm `0.7.0-media-account` / `1027`, minSdk26, targetSdk35.

## Nedenler ve hedefli düzeltmeler

| Öncelik | Bulgu / kanıt | Düzeltme |
|---|---|---|
| P1 | 1026 kaynaklarında MediaSession/medya servisi yoktu; Android'e oynatıcı bildirimi sunulmuyordu. | `playback/*`, `assets/playback.js`, manifest ve MainActivity: gerçek oynatma işaretleriyle medya servisi, bildirim ve komut aktarımı. |
| P1 | `spotify:signup` resmi kayıt akışına eşlenmiyordu; sağlayıcı 302 yönlendirmesi kullanıcı hareketini kaybedince reddedilebiliyordu. | PlayerNavigation resmi signup eşlemesi; AuthenticationPolicy kayıt rotaları; AuthRedirectGate kesin sağlayıcı listesi, kullanıcı dokunuşundan sonra 15 saniyelik tek kullanımlık izin. |
| P1 | Sağlayıcıların gömülü giriş kısıtlaması ve tarayıcı/WebView çerez ayrılığı var. | Açıklayıcı seçim diyaloğu ve Spotify e-posta/kod yolu. Harici Google girişinin Supfy oturumuna dönüşü sağlanmış değildir. |
| P1 | JS 20 saniyelik heartbeat süresi aşılınca kalıcı ERROR; native 8 saniyelik callback gecikmesi de kalıcı hataya yol açabiliyordu. Telefon görüntüsü hangisinin oluştuğunu kanıtlamıyor. | protection.js ve ProtectionWebController: güvenli askıya alma, mute/görsel değişiklikleri geri alma, sınırlı yeniden deneme; gerçek hatalar anlamlı ve açık. |
| P2 | Yerel hesap kartı yoktu. | `account/*`, `account_probe.js`, SettingsScreen: çoklu görünür oturum kanıtı, kendi kullanıcı bileşeninden ad, giriş/kayıt/çıkış. Kanıt yoksa bilinmiyor gösterilir. |

1026'daki kanıtlanmış MATCH_PARENT WebView yüksekliği düzeltmesi korunmuştur. TLS/sertifika, Safe Browsing ve üçüncü taraf çerez sınırları gevşetilmedi. Parola, çerez, token veya özel Spotify API'si kullanılmadı. Yeni bağımlılık eklenmedi.

## Ajan incelemesi

Medya, hesap/giriş ve koruma bağımsız ajanlarla geliştirildi; ayrı salt okunur inceleme yapıldı. İnceleme başka profil başlığının yanlış ad sayılması ve hemen Home'a basınca ilk bildirim örneklemesinin kaçması risklerini buldu. İkisi de hedefli regresyonlarla düzeltildi. Başlangıç izni görünürlük veya en fazla üç saniyelik geçişle sınırlı; OS servis kısıtlamaları aşılmaz. Duraklatılmış servis beş dakika sonra kapanır; tekrar örnekleme bu süreyi uzatmaz.

## Ölçüm ve test sınırları

Derleme/lint, JVM, Node ve Android sonuçları teslim klasöründeki ham loglarda tutulur. Android ortamı API30/WebView83 emülatörüdür; bağlı fiziksel cihaz veya Spotify test hesabı yoktur. Kullanıcının WebView156/API36 cihazındaki gerçek reklam, kilit ekranı, canlı Google/kayıt ve kalıcı Spotify oturumu tamamlanmış sayılmaz. Yerel medya fixture'ı gerçek Spotify sesi değildir. Hesap kartı Web Player arayüzünden gözlem yapar; OAuth token doğrulama sistemi değildir.

Android yeni testlerinde gerçek servis, MediaStyle notification/MediaSession token, bildirimden duraklatmanın WebView DOM'una ulaşması, tercihi kapatınca bildirimin kaldırılması ve hesap sinyallerinin Android JSON çözümü kontrol edilir. İlk iki başarısızlık test kurulumundandı: OS oynatma zamanını ilerletiyordu; yerel sayfanın history URL'si about:blank kalıp üretimin köken kontrolünü doğru biçimde tetikliyordu. Testler düzeltildi; güvenlik kontrolü değiştirilmedi.

1026 üzerine aynı debug sertifikasıyla `adb install -r` kullanıldı; yerel sentinel dosyası korundu. Özgün 1018 imzası farklıdır. Son APK debug test sürümüdür.

## Son test sonuçları

| Kontrol | Sonuç | Kanıt |
|---|---|---|
| Derleme / APK / test APK | PASS | verified-build.log, BUILD SUCCESSFUL 43s |
| JVM | PASS | 64 test, 0 hata / atlama |
| JS koruma / medya / hesap | PASS | 33 + 10 + 11 = 54 test, js-tests.log |
| Lint | PASS | 0 hata, 4 düşük öncelikli uyarı (API niteliği/kaynaklar) |
| Android yeni medya/hesap | PASS | android-focused.log: 3 test |
| Android tüm senaryolar | PASS, tekrar koşusuyla | İlk tur 27/32 geçti; System UI ANR diyaloğu 5 menü/dokunma testini engelledi. failure-screen.png ve failure-window.txt kanıt. Uyarı kapatılıp kod/APK değişmeden aynı 5 test tekrarlandı: android-login-rerun.log OK (5 tests), 257.133s. İlk başarısız log saklandı. |
| Güncelleme kurulumu | PASS | install.log: iki install-r başarılı, sentinel mevcut |
| Gerçek Spotify hesabı/kayıt/çıkış kalıcılığı | BLOCKED | Test hesabı yok; kullanıcı parolası istenmedi |
| Google/Apple harici oturumun Supfy'ye dönüşü | BLOCKED | Kayıtlı OAuth uygulaması yok; tarayıcı çerezleri aktarılmaz |
| Gerçek reklam, fiziksel cihaz arka plan/kilit ekranı | BLOCKED | API30 yerel fixture testleri canlı Spotify sesi ve API36 fiziksel testi değildir |
| Pil / uzun süreli performans ölçümü | NOT RUN | Uzun süreli fiziksel cihaz ölçümü yok |
| Doğrudan HTTPS APK | PASS | Yetkilendirme başlığı olmadan HTTP200, 9,100,990 bayt; SHA256 birebir eşleşti |

APK: `/workspace/release-1027/Supfy-1027.apk`.
SHA256: `06beec4dd8b1e47687928eac98582231de7c01b4245c1bbc4ad970bbf59292d5`.
İmza SHA256: `f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640`.
İndirme: https://raw.githubusercontent.com/ebilgi992/supfy/apk-download-1027/downloads/Supfy-1027.apk
Yalnızca APK yeni indirme dalına eklendi. Yeni kaynak kodu/paketin yerel raporları/anahtarlar yüklenmedi; main aynı kaldı.

Ayarlar hesap kartı görünürlük kontrolü: **PASS**. Üretim MainActivity → Menü → Ayarlar, API30. `ui-settings.xml`, `settings-account.png` ve `settings-ui-result.txt`. Canlı hesap olmadığı için durum bilinmiyor; giriş ve kayıt CTA gerçekten çiziliyor.
