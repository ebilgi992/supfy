# Supfy gizlilik bildirimi — 1027

Giriş Spotify HTTPS sayfalarında yapılır. Uygulama şifre, çerez veya oturum belirteci okumaz,
kaydetmez ve dışarı aktarmaz. WebView kendi oturum çerezlerini cihazdaki özel alanında saklar;
yedekleme kapalıdır. Kullanıcının açık verileri silme seçimi dışında oturum temizlenmez.

İsteğe bağlı özgün koruma betiği yalnızca open.spotify.com ana çerçevesinde reklam DOM
işaretlerini, bilinen bannerları ve HTML medya öğelerinin durumunu değerlendirir. Giriş
sayfasında çalışmaz; form girdisi veya hesap bilgisi toplamaz. Yerel uygulama betikten yalnızca
sınırlı işlem sayıları, hata, EME varlığı ve oynatma durumu alır; JavaScript köprüsü yoktur.

Hesap kartı ayrı bir yerel gözlemle yalnızca görünür kendi kullanıcı bileşenindeki adı ve oturum işaretlerini kullanır. E-posta veya başka profil sayfasının başlığı alınmaz; hesap adı bellekte tutulur. Medya bildirimi oynatıcıda görünen başlık, sanatçı, süre ve oynatma durumunu Android MediaSession'a verir; kilit ekranında görünebilir. Bunlar Supfy sunucusuna veya analitik hizmetine gönderilmez. Resmi Spotify sayfalarındaki giriş ve çıkış kullanıcı eylemiyle başlatılır.

Tercihler cihazda SharedPreferences ile tutulur. Son 40 koruma olayı ve sayaçlar yalnızca
uygulama belleğindedir; URL, çerez, şarkı/kişi bilgisi içermez; analitik sunucuya gönderilmez.

Spotify içeriği ve cihazın Safe Browsing hizmeti kendi sağlayıcılarının gizlilik koşullarına
bağlıdır. GitHub Releases sürüm sorguları IP/ağ bağlantısı, User-Agent ve herkese açık depo
isteğini GitHub'a iletir; Spotify hesabıyla ilgili veri göndermez. Supfy'nin kendi analitik,
izleyici veya sunucusu yoktur. Reklam koruması varsayılan kapalıdır.

Oturumu ve site verilerini silme işlemi uygulamadaki açık onay penceresiyle çalışır.
Yeni APK'yı kurmak için eski uygulamayı kaldırmak da cihazdaki uygulama verilerini siler;
imza eşleşmesi olmadan veri koruyan güncelleme mümkün değildir.
