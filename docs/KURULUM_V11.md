# V11 kurulum ve Xiaomi test listesi

Test sürümü **com.supfy.app / 1020 / 0.6.0-v11**, Android 8.0+.
APK: Supfy-0.6.0-v11-1020-debug.apk. Canlı reklam işleme başarısı doğrulanmadı.

## İmza ve veriler

Gönderilen özgün 1018 sertifikası SHA-256:
`02d8c2e96227db320b50b058a7f0f47813789504a6cee040364e20a96f35c7bc`

Bu ortamın V10/V11 test sertifikası SHA-256:
`f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640`

**Bu APK özgün 1018 kurulumunun üzerine kurulamaz.** Önceki denemede Android
INSTALL_FAILED_UPDATE_INCOMPATIBLE verdi. Sürüm kodu farklı imzayı çözmez. Özgün özel
anahtar bulunmadı; APK'dan çıkarılamaz. Uygulamayı kaldırmayın/verisini temizlemeyin;
oturum ve site verileri kaybolur. Kullanıcı cihazına böyle işlem yapılmadı.

Aynı yerel anahtarla üretilen V10 test APK'sı için V11 `adb install -r` kullanılabilir;
sentetik emülatör kanıtının kapsamı budur. Özgün kurulum için aynı özel anahtarla güvenli
ortamda yeniden derleme gerekir. Anahtar/parolayı sohbete/GitHub'a koymayın; mevcut
SUPFY_KEYSTORE_* ortam yapılandırması kullanılabilir.

Boş test cihazında APK'yı açıp Android kurulum istemini onaylayın. İmza hatasında uygulamayı
kaldırarak devam etmeyin. Menüden Spotify girişini açın; şifre yalnızca Spotify HTTPS
sayfasına girilir. Korumayı isteğe bağlı açın. Sorunda “Normal oynatmaya dön” düğmesini
kullanın; çerezleri silmez. Supfy sessiz kurulum yapmaz.

## Sürüm denetimi

Depo ebilgi992/supfy; açılışta en fazla 12 saatte bir ve ayarlardan elle denetlenir.
Taslak olmayan, bu deponun HTTPS Release sayfasına ait daha büyük kodlu build-<kod>
kaydı önerilir. Türkçe Güncelle / Daha Sonra bildirimi korunur. Güncelle resmî Release
sayfasını açar; indirme/kurulum kullanıcı kararıdır. API/ağ hatası açılışı engellemez.
Yeni public Release yayımlanmadı. İmzanın son kontrolünü Android yapar.

## Xiaomi 14T Pro manuel kontrolü

WebView/Chrome güncelliğini kontrol edin. Hesaba yalnızca kullanıcı giriş yapmalı;
şifre/token paylaşılmamalıdır. Aynı imzalı test kurulumunu kullanın.

1. Veri silmeden güncelleme, sürüm 1020 ve eski tercihleri kontrol edin.
2. İlk açılışta menüden Spotify e-posta/kod giriş ekranını açın.
3. Girişi tamamlayıp oynatıcıya dönüşü ve yeniden açınca oturumun korunmasını denetleyin.
4. Çıkış/yeniden giriş yapın; gerekiyorsa harici sağlayıcı dönüşünü ayrı kaydedin.
5. Çalma listesi ve hesap haklarının izin verdiği tam şarkıyı oynatın.
6. Şarkı geçişlerini, spotify: linklerini ve uygulama önerisini deneyin.
7. Koruma kapalı karşılaştırması yapın; gerçek reklam görülüp görülmediğini kaydedin.
8. Korumayı açın; ayrı bileşen durumlarını/sayaçları önce ve sonra kaydedin.
9. Gerçek reklam çıkarsa tespit, DOM/native mute, banner ve ağ işlemlerini ayrı gözleyin.
10. Reklam sonunda normal şarkı/ses dönüşünü dinleyerek doğrulayın; gecikmeyi ölçün.
11. Normal şarkılarda yanlış pozitif ve kapatınca sesin geri dönüşünü kontrol edin.
12. Korumayı yeniden açın; ayarların uygulamayı kapatıp açınca korunmasını deneyin.
13. İnterneti kesip açın; hata/yeniden dene ve oynatmaya dönüşü sınayın.
14. Kilitleme/arka plan/döndürme/yeniden açmayı, bellek/pil/çökme kayıtlarını denetleyin.
15. Sürüm denetimi ve ertelemeyi deneyin; uygun Release yoksa bunu belirtin.

Reklam görülmemesi engelleme başarısı değildir. Hesap/DRM sınırını aşmayın. Logcat/URL/
ekran görüntülerini paylaşmadan önce kişisel verileri temizleyin.
