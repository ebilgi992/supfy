<p align="center"><img src="branding/supfy-logo-dark.png" alt="Supfy" width="320"></p>

# Supfy — 1027 medya ve hesap güncellemesi

Mevcut Supfy'nin Kotlin + Compose + Android WebView kaynakları korunur.
`com.supfy.app`, minSdk26/targetSdk35; **0.7.0-media-account / 1027**.
Compose'un varsayılan WRAP_CONTENT parametresi Chromium'da CSS yüksekliğini sıfırlıyordu.
Aynı public giriş düzeni Android'de siyah/rootHTML hit2 ile tekrar üretildi; yalnız
native MATCH_PARENT yüksekliğiyle form çizildi ve gerçek pencere dokunması geçti.
WebViewFactory artık açık MATCH_PARENT parametreleri verir; Spotify CSS'sine,
oturum çerezlerine veya güvenlik protokollerine müdahale etmez.
Fiziksel WebView156'da gerçek hesap girişi/oturum kalıcılığı ayrıca doğrulanmalıdır.
Bu sürüm **deneysel koruma** içerir; doğrulanmış reklamsız Spotify ürünü değildir. Koyu tema ve telefon görünümü varsayılandır; kullanıcının açıkça seçtiği masaüstü tercihi korunur.

- Ana anahtar, dört özellik tercihi, modül durumu, gerçek sayaçlar ve son 40 olay koruma ekranına bağlıdır.
- Çoklu DOM kanıtıyla reklam sınıflandırılır. DOM medya ve destek varsa yalnızca bu WebView sesine özgü native mute kullanılır; önceki durum korunur.
- Bilinen reklam/tanıtım bannerları geri alınabilir biçimde gizlenir. Genel giriş/izin/ödeme pencereleri gizlenmez.
- İsteğe bağlı ağ filtresi bilinen üçüncü taraf reklam alanlarının GET alt istekleriyle sınırlıdır.
  Spotify oturum, ses, spclient ve Widevine lisans isteklerine müdahale edilmez.
- Koruma varsayılan olarak kapalıdır. Ayarlar cihazda saklanır; hata durumunda müdahaleler durur.
- Spotify içindeki ve menüdeki giriş aynı mevcut WebView üzerinde normal gezinmeye bırakıldı. Giriş için ikinci WebView/Dialog açılmaz; POST ve yönlendirmeler özel popup koduyla tekrar oynatılmaz.
- Girişte denetim ilk formdan sonra da sürer; sonradan kaybolan form görünür hata oluşturur. Form mevcutken yalnızca hit-test başarısızlığı boş sayfa sayılmaz. Geç gelen form, istek yeniden oynatılmadan toparlanabilir. Arka planda denetim durur.
- Girişteki mevcut yazılımsal çizim yoluna karşı aynı belge üzerinde “Normal çizimi dene” karşılaştırması sunulur; seçim giriş adımlarında korunur, oynatıcıya dönünce sıfırlanır. Telefonun GPU'sunun kök neden olduğu kanıtlanmadı. Native giriş durum satırı gerçek yükleme aşamasını ve sürüm kodunu gösterir.
- Boş giriş sayfası, HTTP hatası ve 30 saniyelik yükleme sorunu görünür Türkçe hata/yeniden deneme sunar. Yeni belge commit edilmeden eski sayfa başarı sayılmaz; kapanmış pencerenin geç callback'leri yeni girişe taşınmaz.
- “Tarayıcıda Aç” düğmesi kaldırıldı. Gereken güvenli dış bağlantı ve Release sayfası açma akışı korunur.
- Mevcut GitHub Releases güncelleyicisi korunur; yalnızca `build-<versionCode>` sürümleri önerilir.

## 1027 değişiklikleri

- Gerçek oynatma işaretlerinden beslenen Android MediaSession ve medya ön plan servisi; bildirimde oynat/duraklat ve destekleniyorsa önceki/sonraki. Arka plan tercihi kapatılınca servis kapanır.
- Ayarlarda Spotify hesap kartı: sayfada doğrulanabilen oturum ve ad, giriş/kayıt veya hesap menüsü/çıkış. E-posta, token ve çerezler okunmaz; başka kullanıcının profil başlığı hesap adı sayılmaz.
- `spotify:signup` resmi kayıt sayfasına yönlenir. Google/Apple/Facebook yönlendirmeleri kullanıcı etkileşimine bağlı, sınırlı bir izinle açıklama gösterir. Harici tarayıcı oturumu WebView'e aktarılmaz; uygulama içi giriş için Spotify e-posta/kod akışı gerekir. Kayıtlı geliştirici uygulaması olmadığından yeni bir OAuth entegrasyonu eklenmedi.
- Arka plan zamanlayıcı gecikmesi koruma motorunu kalıcı hataya düşürmez; müdahaleler güvenle geri alınır ve sınırlı yeniden deneme yapılır. Gerçek reklam susturma başarısı doğrulanmadı.

Güncel kapsam ve kanıtlar: [1027 raporu](docs/SURUM_1027_RAPORU.md).

## Derleme ve test

JDK 17, Android SDK platform 35 ve build tools gerekir:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
cd tools
npm ci --ignore-scripts
npm test
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Instrumentasyon APK:
`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
DOM ve Android fixture testleri gerçek Spotify reklamı/oturumu/DRM doğrulaması değildir.
Güncel kök neden ve doğrulama: [1026 raporu](docs/GIRIS_1026_KOK_NEDEN_VE_TEST.md). [1025 tanı kaydı](docs/GIRIS_1025_DERIN_INCELEME.md), [1024 raporu](docs/GIRIS_1024_RAPORU.md), [1023 raporu](docs/GIRIS_1023_RAPORU.md), [1022 raporu](docs/GIRIS_1022_RAPORU.md), [önceki giriş incelemesi](docs/GIRIS_KOK_NEDEN_INCELEMESI.md), 1021 ve V11 raporları tarihsel kayıttır. Sentetik testler gerçek Spotify hesabı/reklamı/DRM veya fiziksel cihaz doğrulaması değildir.

**Ana kabul kriterleri karşılanmadı: gerçek cihazda Spotify girişi ve canlı reklam işleme başarısı doğrulanmadı.**
Debug kaynak kümesindeki dışa kapalı WebViewTestActivity sentetik testlere yüzey sağlar; release uygulamasına dahil değildir.

## Kurulum ve imza

1027 APK, önceki yerel 1026 test APK'sıyla aynı sertifikayı kullanır ve sürüm kodu artmıştır. Mevcut test kurulumunu kaldırmadan güncellenebilir. **Tanıyı kopyala**, yalnızca sabit kodlar/sayılar içeren yerel raporu kullanıcı dokunuşuyla panoya koyar; kendiliğinden göndermez. Parola, form değeri, çerez, token, sayfa metni veya tam URL alınmaz.

Gönderilen özgün 1018 APK'nın sertifikası ile bu ortamda üretilen debug sertifikası farklıdır.
Bu APK mevcut 1018 kurulumunun üzerine **kurulamaz**. Eski uygulamayı kaldırmak oturum ve site
verilerini siler; bu çalışma sırasında kaldırma yapılmadı. Güncelleme için aynı özel imzalama
anahtarıyla yeniden derleme gerekir. Anahtar APK'dan geri çıkarılamaz.

Kalıcı imza desteği korunur: `SUPFY_KEYSTORE_PATH`, `SUPFY_KEYSTORE_PASSWORD`,
`SUPFY_KEY_ALIAS`, `SUPFY_KEY_PASSWORD` güvenli derleme ortamından sağlanır. Anahtarlar depoya,
rapora veya teslim paketine girmez. CI sürüm kodu `2000 + GITHUB_RUN_NUMBER` kullanır.
Kalıcı imza tanımlı olmadan üretilen debug APK yalnızca test içindir.

Boş bir test cihazında APK'yı açıp Android kurulum istemini izleyin. Mevcut Supfy kuruluysa
[1021 kurulum notlarını](docs/GIRIS_DUZELTMESI_1021.md) önce okuyun. Güncelleme dosyasının son paket ve imza
kontrolünü Android Package Installer yapar; Supfy kendi kendine APK kurmaz.

## Güvenlik ve sınırlar

HTTPS, sertifika hatasında iptal, Safe Browsing, dosya/içerik erişiminin kapatılması,
üçüncü taraf çerezlerin kapalı olması ve yalnızca oynatıcıya verilen korumalı medya izni korunur.
`addJavascriptInterface` yoktur. Hesap kartı yalnızca görünür kendi kullanıcı bileşenindeki adı, medya bildirimi yalnızca oynatıcı başlık/sanatçı/durum bilgisini yerel olarak kullanır. Giriş formunun değerleri okunmaz; bu bilgiler ağa veya loglara aktarılmaz.
Şifre/çerez/belirteçler okunmaz veya dışarı aktarılmaz. Günlükte URL veya kişisel veri yoktur.

Tam şarkı oynatma Spotify hesabının haklarına, güncel WebView'e, EME/Widevine'e ve Spotify
uyumluluğuna bağlıdır. DRM veya giriş korumaları aşılmaz. Google/Apple/Facebook gömülü girişi
sağlayıcı tarafından reddedilebilir; e-posta/kod ile Spotify'ın kendi girişini kullanın.
Native MUTE_AUDIO desteği yoksa DOM dışında üretilen medya susturulamayabilir. Native destek, gerçek reklam sesinin susturulduğu anlamına gelmez. WebSocket,
SW önbelleği ve genel ağ trafiği filtrelenmez. DRM uygunluğu EME varlığından çıkarılamaz.
MediaSession ve medya ön plan servisi eklendi. İşletim sisteminin süreci kapatmasından sonra çalma sürdürülmez; fiziksel cihazda kesintisiz arka plan/HyperOS testi yapılmadı.

Reklam engelleme Spotify koşullarına aykırı olabilir; hesap kısıtlaması/kapatılma riski vardır.
Xiaomi HyperOS'ta arka plan çalışması işletim sistemi politikalarına bağlıdır; pil kısıtlaması
kullanıcı tarafından değiştirilebilir. Gerçek cihaz ölçümü olmadan pil/oynatma garantisi verilmez.

## Belgeler ve lisans

[V11 teknik ve güvenlik raporu](docs/TEKNIK_RAPOR_V11.md) · [Değişen dosyalar](docs/DEGISEN_DOSYALAR_V11.md) ·
[V11 sonuçları](docs/SONUC_RAPORU_V11.md) · [Kurulum](docs/KURULUM_V11.md).
V10 belgeleri tarihsel kayıttır; V11 sonuç raporu güncel durumu belirtir.

Supfy bağımsızdır; Spotify ile bağlantılı veya onaylı değildir. Spotify, Spotify AB'nin markasıdır.
[MIT](LICENSE) lisansı korunur. GPL myspoti ve erişilemeyen Spotilol kaynak kodu alınmadı.
[Üçüncü taraf bildirimleri](app/src/main/assets/THIRD_PARTY_NOTICES.txt) kaynak ve APK içindedir.
