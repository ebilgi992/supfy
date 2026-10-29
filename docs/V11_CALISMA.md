# V11 geliştirme ve kanıt kaydı

Temel e08f049, dal codex/quantum-v11; mevcut kaynaklar ve veriler korunur.
Kullanıcı doğrudan geliştirme istedi; tasarım onayı için çalışma durdurulmaz.

1. Başlangıç: assembleDebug, testDebugUnitTest, lintDebug ve androidTest APK derlemesi geçti.
2. Giriş: geçici popup'ın loadUrl ile GET'e dönüştürülmesi ve iptali kaldırıldı;
   aynı CookieManager ile güvenli, kullanıcı tarafından kapatılabilen oturum penceresi.
3. Reklam: belge başlangıcında köprüsüz gözlem; çoklu kanıt değerlendirme; ses/görsel/ağ
   ayrı durumları ve gerçek sayılar; hata halinde ilgili bileşende geri dönüş.
4. Test: önce sentetik JVM/DOM davranışları, ardından ana Activity'nin ağ girişinden
   bağımsız gerçek WebView fixture testleri; gerçek Spotify ve gerçek cihaz ayrıca işaretlenir.
5. Sürüm 0.6.0-v11 / 1020, aynı com.supfy.app. Önceki V10 yerel anahtarı korunur.
   Özgün 1018 anahtarı yoksa o APK ile imza devamlılığı sağlanamaz; kullanıcı verisi silinmez.
6. Son: JVM/DOM/Lint/APK doğrulaması, kaynak/rapor/kurulum paketi; public push yok.

Gerçek Spotify hesabına kullanıcı kendi giriş yapmalıdır; şifre veya token toplanmaz.
Reklam görülmemesi veya modül yanıtı gerçek reklam engelleme başarısı sayılmaz.

## Uygulanan düzeltmeler ve gerçek kanıt

- Login URL/URI birim testleri önce eksik sınıf nedeniyle kırmızı, uygulama sonrası yeşil.
- JS document-start, normal track veto, bileşen hatası ve müzik dönüşü örnekleri önce
  başarısızdı; modül protokol v2 ve çoklu kanıt uygulamasıyla geçti.
- Bağımsız kod incelemesinde üç önemli bulgu giderildi: class değişiminde eski görünürlük,
  native geri alma hatasında vazgeçme ve POST gezinme köken sınırı. CSS regresyonları
  kırmızı/yeşil olarak, ses retry Kotlin sahte portla denetlendi.
- Son JVM 38/38, DOM 29/29; Lint 0 hata/32 uyarı; dört Gradle görevi geçti (1m44s).
- Android headless → Activity yüzeyi → motor olmadan plain WebView izolasyonu denendi.
  Tam yüzeyli ilk seri 7 test/6 başarısız; plain seri 1/1 başarısız. JS timeout,
  Chromium SIGTRAP ve startup ANR kaydı var; tam oynatma test başarısı yok.
- Son iki bağımsız Android sınır testi (POST request gate/tercih kalıcılığı) geçti.
  Bunlar gerçek Spotify girişi/reklam veya gerçek form POST testi diye sunulmaz.
- Son APK install -r Success; sentetik update_fixture verisi korundu, versionCode 1020.
  Özgün 1018 sertifikası farklı; kullanıcı uygulaması/verileri kaldırılmadı.
- Anonim login HTTP 307→200; gerçek hesap girişi yok. GitHub Releases kaynağı okunabildi;
  daha büyük build kaydı yok. Public kaynak/Release yayımlanmadı.
- Debug APK imza/CRC/asset/16KiB ZIP-ELF kontrolleri geçti. Anahtar teslim paketine alınmadı.

Teslim engeli: gerçek kullanıcı hesabında reklam işleme, giriş/oturum, tam oynatma,
Xiaomi ve özgün anahtarla güncelleme doğrulanmadı. Bu sürüm tamamlanmış AdBlock değildir.
