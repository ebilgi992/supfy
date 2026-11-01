# Giriş güvenlik denetimi — 1023

**Sonuç: telefondaki ilk e-posta formunu engelleyen güvenlik kuralı henüz kanıtlanmadı.
Giriş sorunu çözülmüş değildir.** Telefonda doğrulanan son sürüm 1022'dir; 1023 cihaz
raporu bu aşamada alınmamıştır.

İzin listesi, ana çerçeve istek kapısı, HTTPS/sertifika doğrulaması, iframe ve çerez
kuralları kaynakta tekrar incelendi. İki yeni politika sözleşme testiyle bütün JVM
paketi çalıştırıldı: **50 test, sıfır başarısızlık/hata**. Bunlar telefon testi değildir.

| Kontrol | Bulgu |
|---|---|
| accounts.spotify.com/login ve /en/login, /tr/login | Uygulama içinde izinli |
| accounts.scdn.co ve www.scdn.co | İzinli; başlangıç betikleri bu kaynaklardan geliyor |
| Alt kaynak kapısı | NavigationRequestGate yalnızca ana çerçeveye uygulanıyor |
| Google/reCAPTCHA HTTPS iframe'leri | İzinli; korumalı medya izni verilmez |
| Üçüncü taraf çerezleri | Gerçek tarayıcı kontrolünde engelleme doğrulandı; form yine açıldı |
| TLS/sertifika | İptal davranışı ve sistem sertifika güveni korunuyor |
| Safe Browsing / dosya ve content erişimi | Korundu; ek izin veya güven kökü yok |
| Reklam filtresi | Accounts belgesi oyuncu kaynağı değil; filtre devre dışı |
| Site verileri | Silinmedi; yalnızca kullanıcının yaptığı önbellek denemesi var |

Canlı karşılaştırmada sentetik üçüncü taraf çerezi izinli profilde erişilir,
kısıtlı profilde erişilmez olarak ölçüldü. İki durumda da Spotify'ın herkese açık
ilk formu HTTP200 ve görünür input ile açıldı. Bu Chromium154 sonucudur; fiziksel
WebView156, hesap gönderimi veya CAPTCHA tamamlama kanıtı değildir.

Google/Facebook/Apple yolları dış sağlayıcıya 303 döndürüyor. Mevcut istemci bu
yönlendirmeleri her durumda WebView içinde tamamlayamaz. Sağlayıcı sitelerini
genel izin listesine eklemek resmî gömülü kimlik doğrulama uyumluluğu sağlamaz;
harici tarayıcı oturumu da WebView'e kendiliğinden geçmez. Bu gerçek sınırlama,
sağlayıcı seçilmeden boş kalan ilk formun kök nedeni olarak sunulmaz.

Kaynak denetimi ilk formda bir engel göstermediği için TLS, Safe Browsing veya
çerez kuralı değiştirilmedi. Son değişiklikler yalnızca politika testleri/belgedir;
1023 APK'sı yeniden oluşturulmadı veya sessizce değiştirilmedi.

1023 raporu; başarısız kaynak kategorisi/kodu, JavaScript sonucu, gerçek belge
kaynağı, alan/iframe sayıları ve CSS/ekran/örtülme sayıları ile bir sonraki dar
kapsamlı düzeltmeyi seçmek içindir. Parola, form değeri, çerez/token, tam URL veya
sayfa metni içermez. Kök neden yalnızca bu cihaz kanıtı veya eşdeğer gerçek cihaz
kaydı ile kesinleştirilebilir.
