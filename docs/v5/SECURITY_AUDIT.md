# Supfy V5 security review

Review date: 2026-11-02. Target: the native Myspoti-derived engine under `base/`, not the retired root WebView wrapper. Myspoti origin and GPL-3.0 notices remain applicable. This is a source review plus bounded regression testing, not a certification or a claim about the original developer's intent.

## Evidence and limits

The offline guard `python3 tools/v5_security_audit.py --self-test` inspects production sources in app, host-android, api-spotify, api and extensions. It excludes tests, generated files, build artifacts, license URLs, third-party library implementations and the unused host-cdp runner. It reports locations and rule identifiers without printing secret-bearing source values. Android platform logging imports/aliases are distinguished from a no-op `PrivacyLog` facade.

The first recorded source run was **FAIL**: 133 Android Log call sites, two unrestricted JavascriptInterface constructs, package/update repository configuration violations, full listening/search-result history persistence, raw relay captures and extension diagnostic persistence. The inspected intermediate snapshot SHA-256 was `014ceacd6a7902c92f2f525b7b3f6ee5462427319d033aa9cd59cef1e1677d81` (936 source inputs). Changes were happening concurrently; this is a reproducible digest of that input set, not a clean upstream revision or final APK claim. The baseline JSON was captured locally at `/tmp/v5-security-baseline.json`; the final run must be retained with the delivered build.

Final source/APK/runtime results are recorded in [TEST_RESULTS.md](TEST_RESULTS.md). Any unexecuted step remains **NOT TESTED**. No controlled Spotify account, live login, DRM playback, Bluetooth hardware or Android Auto head unit was available to this review. No credential values, live profile, packet capture or user history were collected.

## Architecture and attack surface

| Boundary | Actual source behavior and risk to check |
|---|---|
| Spotify identity | The official page in Android WebView owns sign-in cookies and DOM storage. `Gql.kt` requests `token.get` through the bridge, caches web/client tokens in memory and sends credentials to Spotify APIs. This app is therefore a privileged client once signed in; hiding the WebView does not remove that trust. |
| Native bridge | `PlayerChannel.kt` is the principal page-to-native boundary. Exact HTTPS player origin, source origin, current page and main frame must all be enforced. Upstream used a generic JavascriptInterface fallback; shipped V5 removes it and exposes an explicit unsupported-WebView capability failure instead. |
| Navigation and media permissions | `WebPlayerSecurityPolicy` limits trusted player URLs, user-started provider navigation and protected media permission. HTTPS with user info, nonstandard ports, lookalike hosts, untrusted origins and subframes need negative cases. Third-party frames still exist for Spotify sign-in. |
| Exported Android components | MainActivity accepts Spotify/short-link intents; ArtworkProvider serves approved HTTPS CDN images and indexed downloaded covers; PlaybackService supports media browsing; MediaButtonReceiver supports system controls. Production/debug CLI and profiler receiver registrations are removed; no profileable shell surface is shipped. Unknown exports are guard failures. |
| Artwork and Android Auto | ArtworkProvider accepts only HTTPS on named Spotify image hosts for network images; downloaded cover access is constrained to the current offline index. It is exported without a caller permission to serve Auto; any app can request accepted image resources. No claim of caller privacy isolation is made. PlaybackService checks caller UID/package membership and restricts library browsing; unknown apps intentionally receive an empty root and may still control the public media session. Device interaction remains NOT TESTED. |
| Extensions | Rhino scripts have permission/consent checks and per-extension storage. New or changed packages remain disabled until explicitly approved for their SHA-256. `spotify.raw` exposes authenticated GraphQL/service requests and approved Spotify HTTPS endpoints; writes require the extension action rules. Public Spotify CDN calls are separated from account requests, caller-controlled credential headers are dropped and known credential response fields are filtered. Installing an extension extends the trusted code/data surface; these controls are not proof that a particular extension is trustworthy. |
| Extension page | Shipped ExtensionPage uses WebMessageListener on a per-extension synthetic origin, checking exact source/current document, main frame, active lifetime and message size; unsupported WebViews show a localized error. Packaged HTML has CSP `frame-src 'none'`, no external scripts/forms/base/navigation, private/Spotify network deny and no file/content access. There is no JavascriptInterface fallback. Runtime hostile-page penetration testing remains unexecuted. |
| Native HTTP and local network | WebView, OkHttp, Cronet, DRM/media, lyrics/video, package updates, extensions, Connect and Cast have separate paths. Cast may serve media through an HTTP LAN server with a URL token. Blocking only WebView requests does not cover native clients or third-party stacks; see [NETWORK_REPORT.md](NETWORK_REPORT.md). |
| Update/install | Release lookup is fixed to `ebilgi992/supfy`; automatic checks default off and enabled checks are limited to one attempt per 24 hours. Download redirects are restricted to approved GitHub hosts, digest is required, and package/version/signers are checked before install. Download size/time limits and Android PackageInstaller user confirmation reduce risk. Final APK/signing and negative installation behavior still require execution evidence. |

## Data minimization review

| Data | Location / lifecycle | Assessment |
|---|---|---|
| Cookies/session storage | Android WebView private storage; `WebPlayerHost.logout` removes cookies, WebStorage, history and disk cache. | Needed for sign-in persistence. Source review alone cannot establish complete deletion on a device. |
| Profile | `ProfileCache` holds display name, avatar URL and username in memory. A generation counter drops in-flight results from an old session. | Clear at logout and implicit session loss; account-switch race and retained UI checks required. |
| Feed/library/context cache | `filesDir/screen_cache1`, preferences for liked/library/playback state, view-model and HomeCache memory. `Api.HomeCache.clear` reaches ScreenCache's serialized clear queue. | Contains account-derived content. Explicit logout and implicit account changes must both invalidate it before a new account sees it. No on-disk account encryption claim. |
| Full listening history | Baseline `filesDir/listen_history.jsonl` appended exact timestamps, title, artist, album, URI/key, cover, duration and source after 30 seconds. Graph initialized it and SongPlayer recorded entries. | V5 requirement is no native full-history retention; storage boundary must discard new records and delete legacy data. |
| Search result history | Baseline `RecentItems/items` stored up to 20 selected search results and replay payload. Both search screen variants read it. | This is selected-result history, not evidence of typed-query retention. V5 must disable it and clear legacy preferences. Spotify's own remote/page history is separate. |
| Offline media | `filesDir/offline` and `offline-accounts/<account-key>`, indexes/pending queues, covers/audio and account owner file. | Explicit downloads persist across logout under account separation. This is retained user content; no claim that logout erases downloads. Confirmed clear-all action is needed if erasure is promised. |
| Playback identity/cache | `spotify/device-id`, `spotify/player-device`, `spotify/browser.json`, `spotify/gql-ops.json`; last-playback preferences; video and lyric caches. | Stable identifiers and media-derived data remain part of the app's data footprint. Identity files are playback state, not full history; retention must be disclosed/minimized. |
| Diagnostics | Baseline Logbook ring/export, Android Log sinks, BetaLogs, exit/watch diagnostics, `cache/ad-samples` and extension `log.txt`. | Account-independent regex masking cannot prove absence of private values. V5 disables collection/export/storage and uses a no-op facade. PrivacyCleanup deletes known legacy diagnostic/extension/sample files without traversing linked directories; source guard/canary execution is tracked separately. Library/WebView/OS logging still needs real-device validation. |
| Errors and URLs | Updater installer status messages, exception messages, redirect URLs and extension errors may contain untrusted/private values. | Show stable user-facing failure codes; do not relay raw service/installer exception text. No raw error values were captured during this review. |

## OWASP tracking

Reference versions: **MASVS 2.1.0**, **MASTG 2.0.0**. The table uses PASS / FAIL / NOT TESTED / NOT APPLICABLE. Each status applies only to the stated review scope; it is not a full-category compliance determination. A static guard PASS does not promote unexecuted MASTG device procedures to PASS.

| MASVS category | Status | Evidence / remaining MASTG work |
|---|---|---|
| STORAGE | NOT TESTED | Baseline history/log sinks demonstrated FAIL; final source guards and privacy canaries required. Device sandbox, backups, cache, downloads and logout erasure inspection not yet run. |
| CRYPTO | NOT TESTED | OfflineCrypto and platform TLS/DRM are present. No key extraction, cryptographic design, entropy or at-rest media verification completed. |
| AUTH | NOT TESTED | Source session/profile boundaries reviewed; controlled login, expiry, logout, multi-account and authorization scenarios not run. |
| NETWORK | NOT TESTED | Static endpoint/credential boundary inventory exists. Packet capture, redirect/TLS failure behavior and all native/WebView paths not observed on V5. |
| PLATFORM | NOT TESTED | Bridge/export/source regression guards prepared; built manifest and hostile-IPC/deep-link/WebView tests still required. |
| CODE | NOT TESTED | Source guards and existing unit-test suites provide bounded evidence only. Dependency, release-signing, package and error handling verification require final artifacts. |
| RESILIENCE | NOT TESTED | No tampering, hooking, reverse engineering resistance or runtime integrity assessment performed. This app makes no such certification claim. |
| PRIVACY | NOT TESTED | Profile/cache/history/log surfaces enumerated. Source no-op/canary results must be combined with device observation; account/hardware workflows remain blocked. |

No category is marked NOT APPLICABLE merely because a test was unavailable. No observed fact supports an allegation of intentional malicious collection or exfiltration by an upstream author.

## Final bounded audit evidence

The corrected-service APK has SHA-256
`3eacb22f46667c700dc42fa5c2cdb3f055aa1fde41cd0ed721c03eff7c787b40`.
The final source audit inspected 948 production inputs, digest
`a71296832fe4867c3c9636d21ffdd4fc746f61aec1ef8797dcc4b8f9e4aafed9`: source PASS,
packaged APK PASS, zero specified-rule findings, 12 negative fixtures PASS. Android
SDK apksigner independently verified the APK v2 signature. This bounded result does
not establish end-to-end runtime security, absence of vulnerabilities, provider-side
privacy or foreground-service reliability; device evidence is in TEST_RESULTS.md.
