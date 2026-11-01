# Supfy Quantum — geri alınabilir geçiş

## Kapsam ve karar

Kotlin/Compose, paket `com.supfy.app`, mevcut imza, SharedPreferences anahtarları ve tek WebView/CookieManager korunur. Yerel dal `supfy/quantum-rebuild`; teslim edilmiş 1027 kaynakları ayrı baseline commit ile saklandı. Veri tabanı veya oturum şeması değişmez. Yeni anahtar yalnızca karşılama ekranının görülmesini saklar; önceden tercihleri olan kurulumlar giriş yapmamış sayılmaz.

Tamamen native Spotify katalog/Media3 oynatıcısı, izinli medya URI'si ve geliştirici entegrasyonu olmadan yapılmaz. Web API OAuth tokenı Web Player çerezi yerine geçmez. Windows SpotX yamaları Android'e taşınmaz. Spotify parolası Supfy'de toplanmaz; resmi sayfa korunur.

## Ürün tasarımı

Native Supfy: ilk kurulum karşılama/giriş, alt gezinme, mini oynatıcı, geniş oynatıcı, hesap/ayarlar, hata ve tanı ekranları. Ana sayfa, arama, kitaplık, çalma listesi/albüm/sanatçı detayları ve sıra gerçek Spotify Web Player'a bağlıdır. Veri yoksa sahte öneri, kapak, favori veya çevrimdışı indirme gösterilmez. Yerel oynatıcı yalnızca doğrulanabilir komutları etkinleştirir. Renkler kömür/yeşil/teal, kaydırılabilir büyük metin, 48dp hedefler; animasyon ek yükü yoktur.

## Uygulama sırası

1. Responsive mobil kontrol seçimini RED→GREEN testleriyle düzelt; destek, mevcut durum ve bildirim başarısını ayrı sayısal tanılarla izle.
2. DOM mute setter sonucunu doğrula; native getter/istek/etkin backend ve geçmiş algılama sayacını ayır. Telefonun native sesinin duyulması gözlem olmadan çözülmüş sayılmaz.
3. Supfy native ekranlarını mevcut tek WebView yaşam döngüsüne bağla; resmi login ve MATCH_PARENT düzeltmesini koru.
4. Kendi ürettiğimiz PCM sesle gerçek Android ses zamanı, arka plan durumu, bildirim ve MediaSession komutu testi; native mute desteklenmiyorsa BLOCKED, akustik sonuç ayrıca BLOCKED.
5. JVM/JS/lint/Android regresyonu, kurulum güncellemesi, sürüm/imza/sha doğrulaması; yalnız gerçek APK paylaşımı.

## Geri dönüş

1027 kaynak ZIP'i, APK'sı ve baseline commit'i korunur. Kod geri alınabilir. Android'in eski sürümü doğrudan kurmayı reddetmesi ve özel imza gereksinimi korunur; veri silen kaldırma veya downgrade uygulanmaz. Gerekirse eski kod yeni artan versionCode ile aynı imzayla yeniden derlenir. Ana GitHub dalı değiştirilmez; kaynak push/merge yapılmadan yerel mantıksal commitler oluşturulur.
