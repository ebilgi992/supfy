# Supfy 1023 — giriş belgesi tanılaması

**Durum: giriş sorunu çözülmedi.** Kullanıcının gerçek 1022 denemesinde form
bekleme/EMPTY hatası sürüyor. 1023 tamamlanmış giriş düzeltmesi veya AdBlock ürünü
olarak sunulmaz. Fiziksel cihazın sayısal raporu bekleniyor.

## Telefonda doğrulananlar

- WebView156.0.8078.47; Samsung Internet aynı herkese açık formu açıyor.
- Mobil ve masaüstü görünümünde aynı hata var.
- Wi-Fi ve mobil bağlantıda aynı hata var.
- Yalnızca uygulama önbelleğini temizlemek sonucu değiştirmedi; oturum/site verisi silinmedi.
- Son kullanıcı doğrulamasında telefondaki sürüm hâlâ 1022 idi; 1023 cihaz raporu alınmadı.

## Güvenlik incelemesi

Üçüncü taraf çerezleri için gerçek Chromium154'te kontrollü karşılaştırma yapıldı.
Sabit sentetik çerez kontrolü, izinli durumda üçüncü taraf çerezinin erişildiğini,
kısıtlı durumda erişilemediğini doğruladı. İki durumda da herkese açık Spotify
formu HTTP200, 1 input ve hazır durumuyla açıldı; JavaScript hatası veya form
kaldırılması görülmedi. Bu, hesap gönderimi/CAPTCHA veya fiziksel WebView156 testi
değildir. Çerez kuralını gevşetmek için kanıt bulunmadı.

`/login` → `/en/login` yönlendirmesi aynı izinli Spotify kaynağındadır. İlk 14
betiğin alanları accounts.scdn.co ve www.scdn.co izin kapsamındadır; alt kaynaklara
ana çerçeve kapısı uygulanmaz. Reklam filtresi accounts kaynağında kapalıdır.

Google/Facebook/Apple sağlayıcılarının dış kaynaklarına 303 yönlendirmeleri ayrı
bir sınırlamadır. Bunlar mevcut WebView içinde güvenilir biçimde tamamlanmış
sayılmaz. Sağlayıcıları genel izin listesine eklemek veya harici tarayıcının
çerezlerini WebView'e taşımak bir çözüm olarak uygulanmadı. Bu sınırlama ilk e-posta
formunun boş olmasını açıklamaz.

Karanlık/açık renk, azaltılmış hareket ve zorunlu renkler için altı canlı tarayıcı
karşılaştırması da başarılıdır. Başarılı örneklerde bir CSP olayı görülebilir;
CSP sayısının tek başına sıfırdan büyük olması arızayı kanıtlamaz.

## 1023 değişiklikleri

- Önceki boolean sonuç, JavaScript hatası/null ile gerçekten kullanılamaz belgeyi
  birbirine karıştırıyordu. Gerçek Android kırmızı testi GENERIC yerine EMPTY
  alarak bu hatayı gösterdi. Sonrasında düzeltildi.
- Auth denetimi artık sürümlü, sınırlandırılmış, yalnızca sayısal bir şema döndürür:
  belge/origin/route kodu, body/root çocukları, alan sayısı, devre dışı/salt okunur,
  boyut, ekran alanı, CSS ve üstte başka katman bulunması ayrı ayrı sayılır.
- İlk ve son örnek ayrı tutulur. Desteklenen WebView'de yalnızca accounts kaynağına
  belge başında geçici gözlemci kurulur; 15 saniye içinde alanın eklenip kaldırılması,
  hata/rejection/CSP sayıları içeriksiz kaydedilir. Gözlemci DOM'u değiştirmez.
- HTTP/ağ/SSL kaynak hataları yalnızca sabit alan kategorileri ve sayısal kodlarla
  tutulur. Konsol metni ve kaynak URL'si okunmadan hata sayılır; tam konsol iletileri
  logcat'e aktarılmaz.
- Giriş alt satırındaki **Tanıyı kopyala**, kullanıcının açık dokunuşuyla yerel raporu
  panoya koyar. Kendiliğinden ağ gönderimi veya kalıcı rapor dosyası yoktur.
- Sayfa metni, alan değeri, parola, çerez/token, tam URL, query veya kullanıcı kimliği
  alınmaz. Bilinmeyen JSON alanları/yanlış tipler atılır; sayılar ve rapor boyutu sınırlıdır.
- TLS iptali, Safe Browsing, HTTPS, kapalı dosya/content erişimi, üçüncü taraf çerez
  kısıtı, izin kapısı ve reklam motoru değiştirilmedi. Root veya sahte sertifika yoktur.

## Doğrulama

| Kontrol | Sonuç |
|---|---|
| Son derleme, birim testleri ve Lint | Başarılı |
| JVM testleri | 48 geçti, 0 başarısızlık/hata |
| Tam Android regresyonu | 23/23 geçti; 283,495 saniye; API30 / WebView83 |
| Odaklı tanılama/gizlilik/Compose kopyalama testi | 6/6 geçti; 74,492 saniye |
| Yerel + canlı yapısal tarayıcı durumları | 11 geçti; Chromium154; hesap gönderilmedi |
| Lint | 0 hata, 31 uyarı |
| Gerçek Xiaomi girişi | 1022 başarısız; 1023 raporu henüz alınmadı |
| Gerçek hesap/müzik/reklam/DRM | Bu aşamada doğrulanmadı |

Android testleri sabit yerel formlar ve sentetik hata olayları kullanır. Gerçek
Compose penceresinde formun çizilmesi, düğmeye etkileşim ve raporun formdaki sabit
özel değeri içermediği doğrulanır. Bu gerçek Spotify hesabına giriş kanıtı değildir.

## Kullanım

1. 1023 tanılama APK'sını 1022'nin üzerine kur; uygulamayı kaldırma.
2. Menü → Spotify giriş ekranı. Alt satırda **1023** ve **Tanıyı kopyala** görünmeli.
3. Hata sonrası bu düğmeye basıp `Supfy login diagnostics v1` metnini paylaş.

Native/doc origin kodu 1 accounts, 2 player, 0 opaque/bilinmeyen, 3 diğer anlamındadır.
Route 1 login, 2 authorize, 3 challenge, 0 diğer; route metni/query raporlanmaz.
Alanlar mevcutken style/viewport/hit sayılarının ayrılması, uygun tek sonraki
kod düzeltmesini seçmek içindir. Tam cihaz raporu olmadan kök neden ilan edilmez.


## APK bilgileri

```json
{
  "name": "Supfy-0.6.3-1023-TANI.apk",
  "path": "/workspace/deliverables-login-1023/Supfy-0.6.3-1023-TANI.apk",
  "versionName": "0.6.3-login-diagnostic",
  "versionCode": 1023,
  "applicationId": "com.supfy.app",
  "sizeBytes": 9083276,
  "sha256": "399a5dd0ed8799d08d7001506fe710b9a26b5c91a86456daf4132ccf805cf9dc",
  "signingType": "Android Debug",
  "certificateSha256": "f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640"
}
```

İmza 1022 test sürümüyle aynıdır; sürüm kodu artmıştır. Orijinal farklı imzalı
1018 için anahtar mevcut değildir. Uygulama kaldırılmadı, oturum verileri silinmedi,
özel anahtar veya gizli değer teslim paketine alınmadı.
