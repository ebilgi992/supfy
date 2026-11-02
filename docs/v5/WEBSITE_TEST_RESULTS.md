# Supfy V5 website checks

The local static website passed **19 browser checks** on 2026-11-02, from
09:37:42 to 09:38:00 UTC. Process exit code: **0**.

This verifies the website served locally under `/supfy/`. It does **not** verify
the Android APK, remote hosting, the APK download server, or live Spotify playback.

## Reproduce

The checked-in runner uses Node's built-in HTTP server and an existing
`playwright-core` installation. It installs no packages and adds no site dependency.
It starts a temporary loopback server, runs Chromium, closes both, and writes its
report and screenshots outside the repository.

From the repository root, the exact browser command used was:

```sh
SUPFY_PLAYWRIGHT_MODULE=/workspace/quantum-research/runner/node_modules/playwright-core \
SUPFY_BROWSER_PATH=/usr/bin/chromium \
SUPFY_SITE_ARTIFACT_DIR=/workspace/v5-build/site-checks \
node tools/v5_site_test.cjs
```

On another machine, point `SUPFY_PLAYWRIGHT_MODULE` and `SUPFY_BROWSER_PATH` at
existing installations. `SUPFY_SITE_ARTIFACT_DIR` defaults to
`/tmp/supfy-site-checks`. The runner needs permission to open a local listening
socket and launch Chromium. Observed Chromium version: **154.0.8037.57**.

Additional checks completed successfully:

```sh
node --check site/app.js
node --check tools/v5_site_test.cjs
python3 -m json.tool site/release.json
git diff --check
```

## Observed results

| Check group | Cases | Result |
| --- | ---: | --- |
| Layouts at 1440, 1024, 768, 390 and 320 px: no horizontal overflow | 5 | PASS |
| Unverified download stays disabled; verified fixture activates the exact configured URL; untrusted URL stays disabled | 3 | PASS |
| Malformed JSON and failed configuration fetch leave downloading disabled with visible feedback | 2 | PASS |
| Mobile menu: Escape closes it and restores focus; anchor navigation closes it | 2 | PASS |
| Native FAQ opens and closes with Enter | 1 | PASS |
| Real browser clipboard roundtrips for SHA-256 and version; denied clipboard shows manual-copy guidance | 3 | PASS |
| Internal anchor targets exist and element IDs are unique | 1 | PASS |
| Reduced-motion preference disables smooth scrolling | 1 | PASS |
| No uncaught page exception, external page request or HTTP error response during the checks | 1 | PASS |
| **Total** | **19** | **PASS** |

The download activation test intercepts the local `release.json` response with
a verified fixture. It checks the rendered link without following it or
requesting an APK. The checked-in configuration remained
`download.verified: false`; the visible terminal text was
`APK henüz yayıma açılmadı`.

The clipboard rejection and configuration error cases deliberately inject
failures at those browser boundaries. They test the visible failure handling.
The two successful clipboard cases use Chromium's actual clipboard API with
clipboard permissions granted to the test context.

An initial harness attempt timed out because Playwright correctly treated the
`aria-disabled` download link as disabled. The click check was corrected to use
`force: true`, simulating a physical click on that anchor and checking that its
handler prevents navigation. That was a test harness correction, not a website
failure. The corrected runner then passed all 19 checks. The initial report was
retained as `report-initial-harness.json` in the artifact directory.

## Evidence

Local artifacts from the observed run:

- `/workspace/v5-build/site-checks/report.json`: timestamps, Chromium version,
  each observed check, final result and SHA-256 values for the tested site files.
- `/workspace/v5-build/site-checks/desktop.png`: full-page 1440 px preview.
- `/workspace/v5-build/site-checks/mobile.png`: full-page 390 px preview.

The four principal file hashes recorded by that report were:

| File | SHA-256 |
| --- | --- |
| `site/index.html` | `580b66882f0f77f9ac98e19362cffcd088a5b03fff370afaaa57aad2fc34152e` |
| `site/styles.css` | `b5b32f3c1bd6431c7ccbdde6c0845ede8b1416d09f83f35e80d8bfcf6d7296ff` |
| `site/app.js` | `9a2f44178bae7fe8957545a1e81c4ce950da0cdde12016be7e5d558c5c4748ab` |
| `site/release.json` | `4034e0913f22eaa81f8134b7d49cda2aa7ca2ca980b566c0f7ae69b80811c813` |

## Publication status and limits

The compact feature catalog was added after reading the implementation paths
below, then all 19 browser checks were repeated successfully. These are source
capabilities, not claims of successful live-account or physical-device testing.

| Catalog item | Source examined |
| --- | --- |
| Home, Search, Library, playlists, albums and artists | `base/app/src/main/java/com/music/spotui/ui/navigation/MyNavHost.kt` routes |
| Player queue, previous/next, shuffle, repeat and sleep timer | `base/app/src/main/java/com/music/spotui/di/SongPlayer.kt`; `ui/screens/PlayerScreen.kt` |
| Local audio and supported-source offline storage | `base/app/src/main/java/com/music/spotui/di/LocalPlayer.kt`; `data/offline/OfflineDownloader.kt`; `data/offline/OfflineStore.kt` |
| Optional lyrics, translation, Genius explanations and video | `base/app/src/main/java/com/music/spotui/privacy/ConsentPolicy.kt`; `data/api/LyricsApi.kt`; `LyricTranslate.kt`; `GeniusMeaning.kt`; `video/MusicVideoController.kt` |
| Extensions disabled until the current package is approved | `base/app/src/main/java/com/music/spotui/extensions/Extensions.kt` initialization, installation and enabling paths |
| Home-screen media widget and Android Auto browser | `base/app/src/main/java/com/music/spotui/widget/NowPlayingWidget.kt`; `di/AutoBrowser.kt` |

The copy explicitly keeps provider/account restrictions, requires an appropriate
download-source extension for offline capabilities, offers no catalog-download
guarantee, and labels widget/Auto hardware use as unverified. The displayed JVM
test count remained 950 pending the separate Android-source build results.

The publishing agent reported that the GitHub Pages API operation returned
**HTTP 403** because the connection lacks Pages write permission. The prepared
website branch is **`supfy-pages-v5`**. The expected address is
`https://ebilgi992.github.io/supfy/`, but **the remote website is not live** and
that URL was not counted as a successful deployment or remote browser test.
An external repository Pages setting is still required.

These checks are not a full accessibility, contrast, cross-browser, performance
or penetration audit. They do not establish that all links on external GitHub
pages work, that an APK is safe or available, or that provider-dependent app
features work. The site retains the test-build/debug-signature disclosures,
historical device failures, latest interrupted media run, lint failure,
Myspoti/GPL-3.0 attribution and explicit lack of Spotify affiliation.

## Hosting status update

GitHub Pages subsequently became enabled with branch `supfy-pages-v5`, root path.
The GitHub Pages API reported `status=built`; an unauthenticated HTTPS GET to
https://ebilgi992.github.io/supfy/ returned 200 with the Supfy title and explicit
Spotify independence notice on 2026-11-02. Earlier403 output is retained as historical
publication evidence; it is no longer the current hosting state. The final APK URL
is activated separately only after public byte-for-byte verification.

## Verified-download configuration check

After publishing the actual APK, a complete unauthenticated download matched local
SHA-256 `3eacb22f46667c700dc42fa5c2cdb3f055aa1fde41cd0ed721c03eff7c787b40`
and `cmp` returned0. `download.verified` was then enabled. The same browser runner
was repeated against that configuration: **19/19PASS**, from 2026-11-02T10:04:05.902Z to
2026-11-02T10:04:19.902Z. Both unverified and verified link behavior are exercised with
explicit fixtures, so the test also works after actual publication. This check does
not treat link-integrity verification as Android runtime success.

## Delivery website configuration

Final local browser check:19/19PASS, 2026-11-02T10:42:37.209Z to 2026-11-02T10:42:51.376Z. The website now distinguishes15 passed device cases,1 ANR interruption and3 not-run cases; it does not count unexecuted cases as failures or passes. Public APK link integrity remains verified; this does not imply the app acceptance gates are met.

Current file SHA-256 values:

- `index.html`: `f62f974f6894ad2b441fffdab1c10328f680fed18f6febd5935a5a7f6c355cc8`
- `styles.css`: `b5b32f3c1bd6431c7ccbdde6c0845ede8b1416d09f83f35e80d8bfcf6d7296ff`
- `app.js`: `0980cd16585ccb646cc9104a15341f98eb7eae7bb52a8be670ccd6a8915e819e`
- `release.json`: `771ce6f2c928c4716c1ac4b7e4460110d8a71bf2e1c03885c811a89e8ac03623`

Final Pages commit `d53536446a795832926477dead2e6fad762ec6fa` reports built. All6 public HTTPS files match the19-check local browser input byte-for-byte, including the enabled download and explicit15PASS/1interrupted/3not-run status. Evidence: [website-https.json](evidence/website-https.json).
