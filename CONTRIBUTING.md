# Katkı Rehberi

1. Depoyu çatallayın ve `main` dalından yeni bir dal açın.
2. Değişikliğinizden önce ve sonra çalıştırın: `./gradlew testDebugUnitTest lintDebug assembleDebug`
3. Kullanıcıya görünen tüm metinler `res/values/strings.xml` içinde ve Türkçe olmalıdır.
4. Yeni izin, bağımlılık veya WebView ayarı ekliyorsanız gerekçesini PR açıklamasına yazın.
5. Gerçek cihazda test ettiyseniz cihaz modeli, Android ve WebView sürümünü belirtin (Ayarlar → Cihaz bilgisi).

**Kabul edilmeyen katkılar:** reklam engelleme/susturma/atlama, sayfaya kod enjeksiyonu, DRM veya güvenlik korumalarını aşma, kullanıcı verisi toplama.
