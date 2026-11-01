# SpotX → Supfy uygulanabilirlik incelemesi

İnceleme tarihi: **2026-11-01**. Sonuç: SpotX'in Windows masaüstü yamaları Android WebView tabanlı Supfy'ye taşınamaz. Bu çalışmada SpotX çalıştırılmadı; Spotify ikilileri, DRM, abonelik veya reklam erişim denetimleri değiştirilmedi. Kaynak yalnızca okunarak incelendi; Supfy'ye SpotX kodu/yaması eklenmedi.

## İncelenen sürüm ve bakım durumu

| Alan | Doğrulanan durum |
| --- | --- |
| Depo | <https://github.com/SpotX-Official/SpotX> |
| Sabit kaynak | [`06f7fa2eac989c84613b2e0efc9b18acaf97f6cb`](https://github.com/SpotX-Official/SpotX/commit/06f7fa2eac989c84613b2e0efc9b18acaf97f6cb) |
| Son commit | 2026-10-28; `version bump to 1.3.7` |
| GitHub latest release | `1.9`, 2025-01-03; bu etiket güncel Spotify sürümü değildir |
| Çalışma ortamı | README: Windows 7–11, PowerShell ≥5.1, Microsoft Store dışındaki resmi masaüstü istemci |
| Kaynağın varsayılan hedefi | `run.ps1` parametresi `latest_full = "1.3.7"` |
| Eski platform sınırları | `last_win7_full = "1.2.5.1006.g22820f93"`; `last_x86_full = "1.2.53.440.g7b2f582a"` |
| Lisans | MIT, ©2021–2026 amd64fox; önemli kopyalarda telif/lisans metni korunmalı |

README'nin gereksinimleri ve commit etkinliği bakımın sürdüğünü gösterir; desteklenen bütün istemcilerde başarı ölçümü değildir. Supfy ise `com.supfy.app`, Kotlin/Compose ve Android WebView kullanır; bu kaynak ağacında minSdk26/targetSdk35, JDK17 ve Android SDK35 hedeflenir.

## Mimari ve Android sınırı

Sabit commit'teki [`run.ps1`](https://github.com/SpotX-Official/SpotX/blob/06f7fa2eac989c84613b2e0efc9b18acaf97f6cb/run.ps1) üzerinden doğrulanan yapı:

| Dosya / işlev | Rol | Supfy açısından sonuç |
| --- | --- | --- |
| `Resolve-SpotifyInstallerVersionFromManifest` | Windows kurucu sürümü/mimarisi seçimi | Android APK veya WebView sürüm seçimi değildir |
| `Get-PatchesJson`, `patches/patches.json` | Sürüm aralıklarına bağlı masaüstü dönüşümleri | Supfy'ye alınmadı; servis hakları/reklam erişim kontrolünü değiştirmek kapsam dışı |
| `extract`, `injection`, `Update-ZipEntry` | `Apps/xpui.spa` içindeki JS/CSS ve yardımcı kaynakları değiştirme | Android WebView uzak web belgelerini çalıştırır; yerel `xpui.spa` yoktur |
| `Invoke-VerifiedBinaryPatch` | Windows masaüstü ikililerine bağlı yama altyapısı | Android DEX/ART, WebView veya Android ABI'larına doğrudan uygulanamaz |
| `Test-SpotifyInstall` | `Spotify.exe`, `Apps/xpui.spa`, arşiv girişlerini denetleme | Yalnızca dosya bütünlüğünü önceden denetleme fikri bağımsız uygulanabilir |
| `Add-SpotifyDefenderExclusions` | Windows Defender dışlama yönetimi | Supfy için gereksiz; güvenlik ayarlarını zayıflatan davranış aktarılmamalı |

Sabit [`README.md`](https://github.com/SpotX-Official/SpotX/blob/06f7fa2eac989c84613b2e0efc9b18acaf97f6cb/README.md) projeyi açıkça **Windows masaüstü istemci yamalayıcısı** olarak tanımlar. Windows'ta Chromium kullanılması, JS/CSS veya ikili yamalarının Android'de geçerli olmasını sağlamaz. Masaüstü kurucusunun sürüm sabitlemesi Android WebView sağlayıcısını yönetmez. SpotX, Widevine uygunluğunu, Spotify hesabının haklarını veya gömülü giriş sağlayıcısının kabulünü kanıtlamaz.

## Gizlilik bulgusu

README'deki analitik kapatma iddiası tek başına tüm kodun ağsız olduğunu göstermez. [`js-helper/checkVersion.js`](https://github.com/SpotX-Official/SpotX/blob/06f7fa2eac989c84613b2e0efc9b18acaf97f6cb/js-helper/checkVersion.js) içinde `window.fetch` sarmalanır; `extractBearerToken` ve `runOnce` üzerinden yakalanan yetkilendirme belirteci `fetchDesktopUpdateAttempt` tarafından Spotify masaüstü güncelleme sorgusunda kullanılır. `sendSuccess` / `sendError` sürüm, istemci bağlamı ve tanı raporlarını harici Worker uçlarına gönderir. Bu gözlem **belirtecin rapor alıcısına gönderildiği iddiası değildir**; belirteç yakalama ve harici raporlama ayrı ayrı kaynaktan doğrulanmıştır. `run.ps1` yardımcıyı `sendversion_off` verilmediğinde enjekte eder.

Bu davranış Supfy'nin parola/çerez/token okumama ve dışa aktarmama sınırıyla uyumlu değildir; aktarılmadı. Spotify'ın yetkilendirme başlıkları veya özel güncelleme API'ları Supfy tanısı için kullanılmamalı.

## Aktarılabilecek bağımsız mühendislik ilkeleri

- Değişiklikten önce sürüm/yetenek kontrolü; desteklenmeyen durumda görünür, dürüst hata.
- Geri alınabilir yerel ayarlar, sınırlı günlükler ve açık değişiklik raporu.
- Kendi dosyalarının envanteri, özetleri ve sürüm bilgisini doğrulama.
- Kullanıcıya ait Supfy kaynakları üzerinde test ve lisans/üçüncü taraf bildirimlerini koruma.

Bunlar yalnızca genel tasarım ilkeleridir; SpotX uygulaması veya algoritmaları kopyalanmadı. MIT lisansı üçüncü taraf Spotify istemcisini yeniden dağıtma, servis haklarını değiştirme veya platform koşullarından muafiyet vermez. Tam müzik oynatımı resmi hesap haklarına ve platform uyumluluğuna bağlı kalır.

## Gerçekte yapılan kontrol

Depo `git clone --depth 1` ile araştırma dizinine alındı, commit/tarih, README, LICENSE, kurucu ve yardımcı kaynaklar statik okundu. GitHub latest-release yanıtı ayrıca alındı. İndirme, ortamın mevcut HTTPS proxy/CA güveniyle ve TLS doğrulaması açıkken yapıldı. SpotX kurulum komutları, PowerShell/BAT, yama ve Spotify istemcisi **çalıştırılmadı**. Android uygulanabilirliği mimari karşılaştırmadır; canlı Spotify testi değildir.

Lisans kaynağı: [`LICENSE`](https://github.com/SpotX-Official/SpotX/blob/06f7fa2eac989c84613b2e0efc9b18acaf97f6cb/LICENSE). Supfy'nin kendi kaynak analizi ve REA kısıtları: [REA_ANALYSIS.md](REA_ANALYSIS.md).
