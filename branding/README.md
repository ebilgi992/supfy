# Supfy Marka Kimliği

**İsim:** Supfy (kullanıcı tarafından belirlendi).

**İşaret:** Uçları nota başıyla biten kalın bir "S". Hem uygulamanın baş harfini hem de bir müzik notasını anlatır. 48 piksele kadar net okunur.

## Renkler

| Ad | Hex | Kullanım |
|---|---|---|
| Yeşil | `#1DB954` | İşaret, koyu temada ana renk |
| Koyu yeşil | `#14833B` | Açık temada ana renk (beyaz zeminde okunabilirlik) |
| Parlak yeşil | `#1ED760` | Vurgu, "Bağlı" göstergesi |
| Zemin (koyu) | `#121212` | Arka plan |
| Yüzey (koyu) | `#181818` / `#282828` | Kartlar, çipler |
| Zemin (açık) | `#FFFFFF` / `#F4F4F4` | Arka plan, kartlar |
| Hata | `#F15E6C` / `#C62839` | Koyu / açık tema |

## Karıştırılma riski (yayımlama öncesi değerlendirme)

| Unsur | Risk | Not |
|---|---|---|
| Logo şekli | Düşük | Spotify'ın daire ve üç yay biçimli logosuyla şekil benzerliği yok. |
| Renk paleti | Orta | Yeşil-siyah, Spotify'ın paletine çok yakın. |
| İsim | Yüksek | "Supfy", "Spotify" ile aynı harfle başlayıp aynı "-fy" ekiyle bitiyor. |
| Bütünlük | Yüksek | Spotify'ın sayfasını açan, Spotify'a benzer adlı ve renkli bir uygulama, resmî uygulama izlenimi verebilir. |

**Sonuç:** Kişisel kullanım ve sideload için sorun yok. Google Play'de yayımlamak düşünülürse marka şikâyeti ve "taklit" (impersonation) politikası nedeniyle reddedilme ya da kaldırılma riski yüksek. O durumda isim ve paletin farklılaştırılması önerilir. Uygulamadaki "Hakkında" bölümünde bağımsızlık açıklaması zaten yer alıyor.

## Tipografi

Sistem yazı tipi (Roboto / üreticinin varsayılanı), Material 3 ölçeği. Başlıklar yarı kalın. Wordmark (logo yazısı): Inter/Roboto Bold.

## Dosyalar

- `supfy-icon.svg`, `supfy-icon-512.png`: tam uygulama ikonu
- `supfy-mark.svg`: arka plansız işaret
- `supfy-monochrome.svg`: tek renk (Android 13+ temalı ikonlar)
- `supfy-logo-dark.svg/.png`: koyu tema logosu (işaret + yazı)
- `supfy-logo-light.svg/.png`: açık tema logosu
- Android: `res/drawable/ic_launcher_*.xml`, `ic_supfy_mark.xml`, `mipmap-anydpi-v26/`; açılış ekranı `values-v31/themes.xml`
