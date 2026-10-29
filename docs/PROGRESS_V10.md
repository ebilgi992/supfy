# V10 geliştirme kaydı

- Kaynak: ebilgi992/supfy, test-1018, 8690ab2; dal codex/quantum-v10-apk.
- Kullanıcı doğrudan uygulama geliştirme/derleme istedi; ek tasarım onayı beklenmez.
- Çalışma alanında önce kaynak yoktu; bağlı GitHub'dan mevcut proje alındı.
- İlk denemeler: SDK/JDK eksik, yazılamayan varsayılan Android dizini ve proxy gereksinimi.
  Derleme araçları /workspace içine kuruluyor; uygulamanın TLS ayarları değiştirilmedi.
- Eski APK: com.supfy.app / 1018 / 0.4.0; SHA256 03ac5e8dd05c7392e5f0ef71940d7b23903eaa2dea8076a9a44643d14a75a732.
- Eski sertifika SHA256: 02d8c2e96227db320b50b058a7f0f47813789504a6cee040364e20a96f35c7bc.
- Temiz test-1018 kaynakları ayrı /workspace/supfy-baseline dizininde 20 JVM testi, lint ve
  assembleDebug ile geçti (2m48s). Daha önceki ortam hataları ürün hatası sayılmadı.
- Koruma motoru / JS / UI bağlandı. DOM testleri eksik modülde 13 başarısızdan 17 başarılıya;
  oynatma, pagehide ve yaşam döngüsü regresyonlarıyla 22 başarılıya çıktı.
- Kotlin motor ve URL/sürüm testleri eklendi. Önce eksik sınıf derleme hatası, ardından 29/29;
  inceleme regresyonları önce 2 başarısız, düzeltmeyle 31/31 geçti.
- Bağımsız inceleme: geçici popup renderer kaybı ve görünmeyen tarihsel JS hatasında
  kapat/aç sorunu önemli; eski playing tanısı da düzeltilecek. Popup testi Android fixture ile RED/GREEN.
- Karar: otomatik güncelleyici test-* sürümlerini önermez; farklı geçici imza riski nedeniyle
  mevcut build-* kanal ayrımı korunur. Kodlanmış Release yolları ve userinfo reddedilir.
- Android 15 AOSP x86_64 emülatörü KVM yokluğunda TCG ile Docker'da açıldı.
  İlk açılış yaklaşık 11 dk sürdü. Gerçek Xiaomi veya Spotify hesabı yok.
- Verilen orijinal APK emülatöre kuruldu. V10 üzerine kurulum beklenen şekilde
  INSTALL_FAILED_UPDATE_INCOMPATIBLE ile reddedildi; otomatik kaldırma kullanıcı cihazında yapılmadı.

- Aynı yerel test sertifikasıyla 1018 → 1019 güncellemesi ve sentetik SharedPreferences
  verisinin önce/sonra korunması doğrulandı (ilk harness denemesinde Docker stdin aktarımı
  eksikti; fixture önce okunarak doğrulanan ikinci deneme başarıyla tamamlandı).
- Android runtime ilk denemede startup ANR, devamında JS timeout ve fixture yüklenmedi hataları
  verdi. Emülatörün sistem/telefon/Bluetooth süreçlerinde de ANR/çökme görüldü. Bunlar geçiş sayılmadı.
- Önemli inceleme düzeltmeleri: geçici popup onRenderProcessGone temizleme/true dönüşü;
  sağlıklı sayfada tarihsel JS hata sayacının yeniden fault yaratmaması. Playing tanısı
  kapama ve gezinmede temizlendi. Detached banner referansları tarama sırasında bırakılır.

- Son doğrulama: 31 JVM + 22 DOM geçti, Lint 0 hata / 31 uyarı, debug + androidTest
  APK build başarılı. APK 41a2e2573250692f8bab5bab821965bc38de80df2c632d5d646c66402ae5610a. ZIP CRC, imza, paket/sürüm ve 16 KiB hizalama geçti.
- Android runtime başarılı sayılmadı: startup ANR ve fixture timeout/yüklenmedi sonuçları var.
  Son historyUrl fixture düzeltmesi derlendi, tekrar çalıştırılmadı. Popup runtime regresyonu
  kanıtlanamadı; kaynak düzeltmesi ve instrumentasyon testi teslim edildi.
- Kaynak lisansı MIT; GPL kaynak kodu veya erişilemeyen Spotilol kodu alınmadı. Remote push,
  PR, merge ve Release yayını yok. Kullanıcı dosyası ve fiziksel cihaz verisi silinmedi.
