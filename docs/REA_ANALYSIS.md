# REA ile yalnızca Supfy kaynak incelemesi

İnceleme tarihi: **2026-11-01**. REA'nın desteklenen CLI'ı gerçekten çalıştırıldı. Kurulum önizlemesi ve Supfy'ye ait kaynak arşivi envanteri başarılı oldu; JavaScript semantik analizi worker hatasıyla tamamlanamadı. Bu sonuç tam güvenlik denetimi veya çalışan Spotify oturumu/oynatımı kanıtı değildir.

## Kaynak, lisans ve mimari

| Alan | Doğrulanan durum |
| --- | --- |
| Depo | <https://github.com/morluto/rea> |
| İncelenen main commit | [`e18594b24754ab95ebd119311cc32a1d1ad6fa13`](https://github.com/morluto/rea/commit/e18594b24754ab95ebd119311cc32a1d1ad6fa13), 2026-11-01 |
| Commit konusu | `refactor(composition): unify capability bindings and cleanup ownership (#8441)` |
| Main package sürümü | `6.8.0` |
| GitHub latest / npm latest | `rea-agents-6.8.0`, 2026-10-25 / `rea-agents@6.8.0` |
| Bu incelemede yürütülen paket | npm'den sabit `rea-agents@6.8.0`; main'deki her yeni özelliğin yayımlanmış olduğu varsayılmadı |
| Node gereksinimi | `^22.23.4 || ^24.22.1 || >=26.12.1` |
| Lisans | MIT, ©2026 morluto; harici araçlar ve bağımlılıklar kendi lisanslarını korur |

Sabit [README](https://github.com/morluto/rea/blob/e18594b24754ab95ebd119311cc32a1d1ad6fa13/README.md), [package.json](https://github.com/morluto/rea/blob/e18594b24754ab95ebd119311cc32a1d1ad6fa13/package.json), [LICENSE](https://github.com/morluto/rea/blob/e18594b24754ab95ebd119311cc32a1d1ad6fa13/LICENSE) ve [üçüncü taraf bildirimleri](https://github.com/morluto/rea/blob/e18594b24754ab95ebd119311cc32a1d1ad6fa13/THIRD_PARTY_NOTICES.md) okundu. Main commit tarihi aktif geliştirme kanıtıdır; bütün yüzeylerin kullanılabilirliği veya npm/main eşitliği değildir.

`scripts/rea.mjs` CLI/MCP yönlendiricisidir. `src/cli/` ve MCP sunucusu ortak `src/application/` iş akışlarını kullanır; `src/composition/` sağlayıcıları bağlar. `runCliJavaScriptApplicationAnalysis` inert kaynakları ayrı worker'da işler. `src/android/JadxSession.ts` içindeki `JadxSession` kendi JADX sürecini yönetir; `src/apkSignature/` cihaz/harici motor gerektirmeden imza bildirimlerini okur. Mimari referansı: [architecture.mermaid](https://github.com/morluto/rea/blob/e18594b24754ab95ebd119311cc32a1d1ad6fa13/docs/architecture.mermaid).

## Kurulum kaynağı önce incelendi

[`install.sh`](https://github.com/morluto/rea/blob/e18594b24754ab95ebd119311cc32a1d1ad6fa13/install.sh), `scripts/prepare.mjs`, `scripts/prepack.mjs`, `src/cli/setupCommands.ts` ve `src/application/Setup.ts` çalıştırma öncesi okundu. Shell kurucusu normalde global npm kurar; terminal varsa etkileşimli setup başlatabilir. Bu yol kullanılmadı. `runSetupWorkflow` içindeki `options.dryRun` dalı uygulamadan önce `status: planned` döndürür.

1. Mevcut Node `24.19.0` ile `bash install.sh --version 6.8.0 --dry-run --no-setup` çalıştırıldı: **exit 1**, desteklenmeyen Node; kurulum yapılmadı.
2. Araştırma dizinine resmi Node `24.22.1` indirildi. Yayıncının HTTPS `SHASUMS256.txt` listesine göre SHA-256 doğrulandı: `6c172a9ab6718f7b801fb5a7bd486af2ee5e32758086a04b84fcce2aa77095e6`. Bu, aynı kaynaktan alınan checksum doğrulamasıdır; ayrı imza doğrulaması yapılmadı.
3. `rea-agents@6.8.0`, `/workspace/quantum-research/runner` altına `npm install --prefix … --cache … --ignore-scripts --no-audit --no-fund` ile kuruldu. Global paket/agent ayarı değiştirilmedi; Supfy bağımlılıklarına eklenmedi.
4. `--version` çıktısı **6.8.0**. İndirmeler mevcut proxy/CA üzerinden, TLS doğrulaması açıkken yapıldı.

## Gerçek CLI sonuçları

Aşağıdaki komutlarda `rea`, araştırma dizinindeki Node `24.22.1` ile sabit paketin `scripts/rea.mjs` dosyasını çalıştıran CLI'ı ifade eder; global komut kurulmadı.

| Komut | Sonuç |
| --- | --- |
| `rea setup --client codex --skill=false --dry-run --json` | **exit 0**; `status=planned`, `appliedActions=[]`; yalnız `configure_client:codex` önerildi |
| `rea doctor --client codex --json` | **exit 1**; Node sağlıklı; Codex `config_drift`; değişiklik uygulanmadı |
| `rea analyze-javascript-application /workspace/supfy/app/src/main/assets --json` | **exit 1**, `unreadable_output`; worker terminal yanıtı vermeden exit 0 ile kapandı; stdout/stderr boş |
| Aynı analiz, yalnız `--source-path playback.js --max-heap-mb 256 --analysis-timeout-ms 30000` | **exit 1**, aynı worker/protokol hatası; yalnız büyük girdiye veya ilk dosyaya özgü olduğu gösterilemedi |
| `rea inspect-artifact …/assets --json` | **exit 1**, `directory_requires_file`; bu yol dizin kabul etmiyor |
| Supfy'nin kendi altı asset dosyasından oluşturulan ZIP için `rea inspect-artifact …/supfy-owned-assets.zip --json` | **exit 0**; `rea-artifact-graph` ile 7 düğüm / 7 occurrence / 6 kenar: ZIP + 6 dosya envanteri |
| `rea inspect-apk-signature /workspace/release-1027/Supfy-1027.apk --json` | **exit 0**; yalnız Supfy'nin mevcut 1027 APK'sı statik incelendi; v2 bildirimi ve eşleşen içerik digest'i bulundu |
| `rea inspect-apk-signature /workspace/quantum-release/Supfy-1028.apk --json` | **exit 0**; son teslim APK'sında v2 bildirimi, 1027 ile aynı sertifika bildirimi ve eşleşen içerik digest'i bulundu |

Başarılı ZIP envanteri Evidence kimliği: `ev_2be1795187c94890d2e16f8c8ddc3e543e84b72d7c172d8af1743745e65f22fe`. Arşiv SHA-256: `0e34185e66562e4874e3549a83172cdab1830434f48403b1f83bca2b38a73333`. Bu, dosya varlığı/özet envanteridir; JS davranışı veya güvenli olduğuna dair onay değildir.

Başarısız JS analizinin kısmi Evidence kimliği: `ev_f82d3d2885256e145530bd7347a6f118a23e2f7e16abf366126610916494c229`; asset kökü özeti `2d8a338f9e01e9a96a131bc1ca6801167839c55a371430ddcf69504e710afa92`. Beş JS asset'i sayıldı, beş parse scope `unavailable` kaldı; boş rota/bağımlılık sonucu **yokluk kanıtı değildir**. Üçüncü taraf REA koduna düzeltme uygulanmadı; hata kök nedeni kanıtlanmadı.

Ham JSON/stderr, paket kilidi ve indirilen araştırma kaynakları `/workspace/quantum-research` içindedir; uygulama/APK varlıklarına veya Supfy deposuna kopyalanmadı.

## Kendi 1027 APK'sının statik imza envanteri

İlk araştırmada `build/outputs` içinde APK bulunamamıştı; sonradan mevcut `/workspace/release-1027/Supfy-1027.apk` eseri belirlendi ve desteklenen REA imza okuyucusuyla incelendi. APK değiştirilmedi, açılmadı/çalıştırılmadı; ADB kullanılmadı. Bu, yeni derlenen sonraki APK'nın denetimi değildir.

| Alan | REA'nın gözlediği bildirim |
| --- | --- |
| Boyut / SHA-256 | 9.100.990 bayt / `06beec4dd8b1e47687928eac98582231de7c01b4245c1bbc4ad970bbf59292d5` |
| Evidence kimliği | `ev_a95f591cc395c77f2a5ed2bc0af1dc3ce2d705aace27ed21541fbf56be1bf3fa` |
| Sağlayıcı | `apk-signature`, REA APK signature reader |
| Şemalar | v2 var; v1/v3/v3.1 yok; source stamp yok |
| Sertifika bildirimi | `CN=Android Debug, O=Android, C=US`; RSA; seri `01` |
| Bildirilen sertifika SHA-256 | `f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640` |
| İçerik digest'i | SHA-256, `matches_apk=true`, `self_consistent=true` |
| Runtime | `not-performed` |

Önemli sınır: REA **kriptografik imza doğrulaması yapmadığını** açıkça bildirir. Sertifika zinciri/güvenilirliği, iptal durumu, yayıncı kimliği ve Android'in paketi kabulü doğrulanmadı. `matches_apk` yalnız bildirilen içerik digest'inin mevcut baytlarla eşleşmesidir; saldırganın digest'i yeniden yazabilmesi nedeniyle tek başına özgünlük kanıtı değildir. Bilinen imza alanlarının dışındaki 4 bayt yorumlanmadı; REA bunu tek başına bozuk imza olarak sınıflandırmaz. Ham çıktı: `/workspace/quantum-research/supfy-1027-signature.json`.

## Supfy üzerinde bağımsız statik okuma

Aşağıdakiler REA'nın tamamlanmış semantik sonucu değildir; Supfy kaynaklarının doğrudan okunmasıdır. Temel kaynak commit'i `b22dc06bc87abc69e07c77ebc346764a76b84b7a`; aynı çalışma sırasında yeni kaynak değişiklikleri gelebileceğinden bulgular sürüm sertifikası sayılmamalı.

| Kaynak / işlev | Gözlenen sınır |
| --- | --- |
| `web/WebViewFactory.kt`, `create`, `hardenServiceWorkers` | JS ve DOM storage açık; dosya/içerik erişimi, mixed content ve üçüncü taraf çerezler kapalı; Safe Browsing açık; `addJavascriptInterface` kullanılmıyor |
| `web/SupfyWebViewClient.kt`, `onReceivedSslError` | Sertifika hatasında `handler.cancel()` |
| `web/SupfyChromeClient.kt`, `onPermissionRequest` | Yalnız doğrulanmış oynatıcı origin'ine `RESOURCE_PROTECTED_MEDIA_ID`; diğer istekler reddedilir; bu DRM başarısı değildir |
| `res/xml/network_security_config.xml` | Cleartext kapalı, sistem CA'ları |
| `account/AccountController.kt`, `AccountSnapshot.kt`, `assets/account_probe.js` | Public hesap UI gözlemi; ad filtreleri; parola/çerez/token veya özel hesap API'si okunmuyor |
| `playback/PlaybackWebController.kt`, `assets/playback.js` | Public player DOM/medya durumu; bu gözlem tam şarkı hakkı veya arka plan sürekliliği kanıtlamaz |
| `web/LoginDiagnostics.kt`, `assets/login_probe.js` | Yapısal metrikler ve sınırlı hata kodları; form değerleri veya tam URL'ler rapora alınmıyor |
| `AndroidManifest.xml` | Launcher activity dışa açık; medya playback servisi dışa kapalı; gereksinim bildirimi runtime izin/başarı kanıtı değildir |

İncelenen kendi kaynaklarından iki örnek hash: `account_probe.js = 349ab142454738dea04249ae5e30dccef064fd2c5e3d42f53e81de6b71e9c2e0`; `playback.js = fd3b97285ecdec21017f9feb52172a9b5c7a466e162be7efeecfefb6c878e1fb`.

## Android sağlayıcıları ve kalan sınırlar

- [Android kılavuzu](https://github.com/morluto/rea/blob/e18594b24754ab95ebd119311cc32a1d1ad6fa13/docs/android-analysis.md): APK manifest/sınıf/metot incelemesi için **tam JDK ≥17**, Linux'ta procps uyumlu `ps` ve kullanıcı tarafından sağlanan **jadx-headless-mcp 0.7.1** gerekir. Ortamda JDK21 mevcut; doctor, JADX'ı yapılandırılmamış buldu.
- [`JadxRelease.ts`](https://github.com/morluto/rea/blob/e18594b24754ab95ebd119311cc32a1d1ad6fa13/src/android/JadxRelease.ts): revision `5844800a486d2248fa949b74c9896285f5b54de2`; JAR SHA-256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`; `REA_JADX_MCP_JAR` mutlak dosya yolu olmalı. JADX/JADX-headless Apache-2.0 koşulları ayrıca geçerli.
- JADX, Ghidra, IDA ve Kuna yapılandırılmamış; Hopper veya ticari sağlayıcı yüklenmedi/başlatılmadı. REA'nın MIT lisansı IDA, Hopper veya JEB lisansı vermez. Ücretli sağlayıcıya, kimlik bilgisine veya özel sunucuya ihtiyaç duyulmadı.
- `inspect-apk-signature` harici decompiler/ADB gerektirmez; mevcut kendi 1027 APK'sında yukarıdaki envanter tamamlandı. JADX sınıf/metot çözümlemesi yapılmadı.
- Hiçbir Spotify APK'sı/ikilisi açılmadı veya decompile edilmedi. Supfy APK'sı için ayrıca izin verilen statik araç kullanımı, yalnız Supfy eserine sınırlı olmalı. Runtime Spotify trafiği yakalanmadı; hesap/token/çerez toplanmadı.

## Native MUTE_AUDIO için doğrulanan sürüm sınırları

AndroidX kitaplık sürümü ve cihazın WebView motor sürümü ayrı gereksinimlerdir. Resmi [WebViewFeature.MUTE_AUDIO](https://developer.android.com/reference/androidx/webkit/WebViewFeature#MUTE_AUDIO), [WebViewCompat.setAudioMuted](https://developer.android.com/reference/androidx/webkit/WebViewCompat#setAudioMuted(android.webkit.WebView,boolean)) ve [WebKit sürüm notları](https://developer.android.com/jetpack/androidx/releases/webkit#1.11.0) API'nin **AndroidX WebKit 1.11.0** ile geldiğini doğrular; ilk alpha `1.11.0-alpha02`, 2024-02-21. Supfy'nin WebKit 1.12.1 bağımlılığı derleme API'sini içerir; bu, eski WebView motoruna özellik eklemez.

Chromium'un sabit sürüm kaynakları da kontrol edildi: `SupportLibWebViewChromiumFactory` desteklenen özellik listesinde `Features.MUTE_AUDIO`, **122.0.6261.57** ve **122.0.6261.64** etiketlerinde vardır; **121.0.6167.185** ve **122.0.6261.39** etiketlerinde yoktur. 122.0.6261.64'te `SupportLibWebViewChromium.setAudioMuted` / `isAudioMuted` çağrıları `AwContents`'e iletilir. Kaynaklar: [122.0.6261.57 factory](https://github.com/chromium/chromium/blob/122.0.6261.57/android_webview/support_library/java/src/org/chromium/support_lib_glue/SupportLibWebViewChromiumFactory.java), [122.0.6261.39 factory](https://github.com/chromium/chromium/blob/122.0.6261.39/android_webview/support_library/java/src/org/chromium/support_lib_glue/SupportLibWebViewChromiumFactory.java), [122.0.6261.64 uygulama](https://github.com/chromium/chromium/blob/122.0.6261.64/android_webview/support_library/java/src/org/chromium/support_lib_glue/SupportLibWebViewChromium.java).

Dolayısıyla doğrulanan sağlayıcı ailesi **WebView M122'nin özelliği içeren yapıları ve sonrası**dır; bütün `122.x` yapılarına veya yalnız Android API seviyesine dayanarak destek ilan edilemez. Kontrol edilen etiketler arasında ilk pozitif `.57`'dir; bu araştırma bütün ara etiketlerin veya OEM backport'larının mutlak ilk sürümünü saptamaz. Uygulama için karar `WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)` ile verilmeli; mute isteğinden sonra `WebViewCompat.isAudioMuted` getter sonucu gözlenmelidir. Getter, kullanıcıya ulaşan ses seviyesinin akustik ölçümü değildir.

Ana test çalışmasının bildirdiği mevcut **API30 / WebView83 emülatörü native MUTE_AUDIO testi için BLOCKED** kalır; sentetik DOM mute sonucu native getter kanıtı sayılamaz. Kullanıcının telefonunda bildirilen WebView156 sürümü de native getter gözlenene kadar yalnız sürüm bilgisidir. Bu araştırma ajanı cihazda test yapmadı.

## Resmi x86_64 test sağlayıcısı yolu

Chromium'un [Android derleme kılavuzu](https://chromium.googlesource.com/chromium/src/+/HEAD/docs/android_build_instructions.md#figuring-out-target_cpu) `x86_64 → target_cpu="x64"` eşlemesini, [WebView derleme kılavuzu](https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/build-instructions.md) `system_webview_apk` hedefini belgeler. Bu, x86_64 WebView derlenebildiği anlamına gelir; mevcut ortamda indirilebilir, uyumlu, resmi imzalı küçük bir hazır paket bulunduğu anlamına gelmez.

Resmi [kanal kılavuzu](https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/prerelease.md) Android10+ için Google Play üzerinden Stable/Beta/Dev/Canary yolunu belirtir. Android10+'ta Chrome, WebView sağlayıcısı olarak kullanılamaz. [Sağlayıcı gereksinimleri](https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/webview-providers.md) AOSP/GMS imajına göre izinli paket adı, user build'lerinde uygun imza, targetSdk, versionCode ve bütün profillerde kurulum/etkinlik koşullarını açıklar. Rastgele bir Chrome/APK indirmek bu gereksinimleri karşılamaz.

Bu oturumda resmi hazır x86_64 WebView paketi indirme/uygunluk testi tamamlanmadı; **uygun modern sağlayıcıyla native test hâlâ BLOCKED**. Yüzlerce MB imaj/sağlayıcı indirilmedi, mevcut sağlayıcı değiştirilmedi ve ADB çalıştırılmadı. Uygun resmi Google Play x86_64 test imajı/kanal güncellemesi veya ayrı bir Chromium x64 test derlemesi sonraki ortam hazırlığı olabilir; mevcut WebView83 sonucu için native başarı varsayılamaz.

### Resmi GCS snapshot listesinin sınırlı kontrolü

2026-11-01 tarihinde yalnız küçük `LAST_CHANGE`, GCS JSON nesne listesi ve `REVISIONS` yanıtları alındı; APK/ZIP indirilmedi. `https://storage.googleapis.com/chromium-browser-snapshots/Android_x64/LAST_CHANGE` **404** döndürdü. Bucket'ta `Android` öneki ve `/` delimiter ile listelenen platformlar `Android/`, `AndroidDesktop_arm64/`, `AndroidDesktop_x64/`, `Android_Arm64/`; `Android_x64/` yoktu.

| Resmi platform / son revizyon | Tam revizyon listesindeki nesneler | Boyut / değerlendirme |
| --- | --- | --- |
| `Android/1732368/` | `REVISIONS`, `chrome-android.zip` | ZIP 274.887.087 bayt; 200 MB sınırını aşar; mimari/minSdk/sağlayıcı paketi listeden doğrulanamaz |
| `AndroidDesktop_x64/1732366/` | `REVISIONS`, `chrome-android-desktop.zip` | ZIP 503.332.904 bayt; 200 MB sınırını aşar; adına bakılarak WebView sağlayıcısı sayılamaz |

Her iki tam revizyon listesinin `nextPageToken` alanı yoktu; bu revizyonlarda ayrı `SystemWebView.apk` nesnesi görülmedi. Resmi [Android revizyon listesi](https://storage.googleapis.com/storage/v1/b/chromium-browser-snapshots/o?prefix=Android%2F1732368%2F&fields=items(name,size),nextPageToken&maxResults=1000) ve [AndroidDesktop_x64 revizyon listesi](https://storage.googleapis.com/storage/v1/b/chromium-browser-snapshots/o?prefix=AndroidDesktop_x64%2F1732366%2F&fields=items(name,size),nextPageToken&maxResults=1000) bu sınırlı bulgunun kaynaklarıdır; geçmiş bütün revizyonların tarandığı iddia edilmez.

Sonuç: Bu resmi hazır yapı yollarında **≤200 MB standalone x86_64 SystemWebView APK URL'si doğrulanamadı**. APK manifest'i ve imza materyali alınmadığından minSdk, gerçek paket adı veya sertifika parmak izi raporlanamaz. `com.android.webview` yalnız AOSP yerel derlemesinin belgelenen varsayılan paketidir; bulunmuş bir indirmenin kimliği değildir. Ham küçük yanıtlar araştırma dizininde `gcs-*`, `android*-list.txt` ve `android*-revisions.txt` olarak saklandı. Proxy/TLS güveni korundu; mirror, kaynak derlemesi, sağlayıcı değişimi veya ADB kullanılmadı.

Sonraki anlamlı adım: REA worker/protokol hatası için kaynak içermeyen minimal rapor hazırlamak; ardından uygun ortamda kendi Supfy asset'lerini yeniden analiz etmek. Yeni APK oluştuğunda onun da ayrı statik imza ve yapılandırılmış JADX kontrolleri yapılabilir. Bunlar canlı giriş, hesap hakları, DRM veya fiziksel cihaz kabul testlerinin yerine geçmez.

## Son teslim 1028 APK'sı: gerçek REA sonucu

Son APK üzerinde desteklenen komut ayrıca çalıştırıldı: `rea inspect-apk-signature /workspace/quantum-release/Supfy-1028.apk --json`, **exit 0**. Sabit REA 6.8.0 / izole Node 24.22.1 kullanıldı; APK salt okunur işlendi. Python SHA-256/boyut kontrolü aynı eseri doğruladı.

| Alan | Doğrulanan sonuç |
| --- | --- |
| Dosya | `/workspace/quantum-release/Supfy-1028.apk` |
| Boyut | 9.202.734 bayt |
| SHA-256 | `f6eca7bd43ef3c473643774414c4d52abc1c05bcd502e1763a3b66b36df1f5f2` |
| Evidence | `ev_ce396209d3a0e60a5e64ac9dc436ee7478ed666dd1df992b554f2ae672f5bbb9` |
| Sağlayıcı | `apk-signature`, REA APK signature reader |
| Şemalar | v2 var; v1/v3/v3.1 yok; source stamp ve rotation bildirimi yok |
| Bildirilen sertifika | `CN=Android Debug, O=Android, C=US`; RSA; seri `01` |
| Sertifika SHA-256 | `f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640`; mevcut 1027 APK'sındakiyle aynı |
| Bildirilen içerik SHA-256 digest'i | `fb846fc1eb416ea40589f56d2b4378511c53efefbdc834b152733ccd11dbfa1c` |
| İçerik tutarlılığı | `matches_apk=true`, `self_consistent=true` |
| Runtime | `not-performed` |

Ham son sonuç `/workspace/quantum-research/supfy-1028-signature.json`; stderr aynı dizindeki `.stderr` dosyasıdır. REA'nın sınırlamaları değişmedi: imza kriptografik olarak doğrulanmaz; sertifika güveni/iptali, yayıncı özgünlüğü, Android paket kabulü veya runtime çalışma kanıtlanmaz. İmza alanları dışında 4 bayt yorumlanmadı. Ayrı ana test ajanı `apksigner` kriptografik doğrulamasının **PASS** ve 1027→1028 `install -r` sentinel kontrolünün başarılı olduğunu bildirdi; bunlar REA'nın sağladığı sonuçlar değildir.

Native mute kabul sınırı da değişmedi: API30/WebView83 ile native `MUTE_AUDIO` testi **BLOCKED**; bildirilen telefon WebView156 sürümünde native getter henüz gözlenmedi. API36 resmi imaj hazırlığının lisans ön kontrolü, XML'deki başvurulan lisansın mevcut kabul kayıtlarıyla eşleşmemesi nedeniyle durduruldu. İmaj indirilmedi/kurulmadı, yeni lisans kabul edilmedi; kullanıcı kararı bekleniyor. Bu son statik APK kontrolünde uygulama kaynakları, APK, sağlayıcılar veya cihaz durumu değiştirilmedi; ADB/Gradle çalıştırılmadı.
