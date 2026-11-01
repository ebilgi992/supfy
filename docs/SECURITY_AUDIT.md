# Source security audit

Date: 2026-11-01. Scope: current `/workspace/supfy` application source, manifests, XML policy, script boundaries, dependency declarations and session/entry flow. This is a bounded source audit, with the limited OSV query documented below; it is not a penetration test, complete dependency scan, account test, signed-artifact inspection or security guarantee. Media/audio behavior is reviewed separately. No credentials, private tokens or account cookie values were requested or read.

## Result

No confirmed open P0/P1 security vulnerability was established in the reviewed source. The previously blocking login rendering defect has a concrete `MATCH_PARENT` fix in `WebViewFactory`; the user reports successful 1027 login visibility and real playback. Preserve that evidence without generalizing it to every account, provider, device or playback entitlement.

| Control | Current evidence | Boundary / follow-up |
|---|---|---|
| TLS and cleartext | `res/xml/network_security_config.xml` trusts system CAs only and denies cleartext; `onReceivedSslError` always calls `cancel()` | Do not add permissive trust managers, `proceed()`, custom hostname bypasses or debug CA configuration to release. |
| Main-frame navigation | `UrlPolicy` accepts HTTPS, no user-info, port absent/443 and trusted domain suffix boundary; `NavigationRequestGate` checks main-frame requests including POST | Trusted suffixes include all Spotify/CDN subdomains, not merely login/player. This is a deliberate broader trust surface; do not call it a tiny exact-host allowlist. No demonstrated exploit was found. |
| External navigation | `SupfyWebViewClient` requires gesture, or a one-use bounded provider grant; `MainActivity` exposes only HTTPS browser intents | Browser provider login cannot transfer into WebView. No callback filter or app-to-app cookie import exists. |
| Iframes | `UrlPolicy.allowSubframe` permits HTTPS frames | Needed for public web/challenge flows; does not mean arbitrary origins are trusted for native actions or protected-media grants. |
| WebView local resource boundary | `allowFileAccess=false`, `allowContentAccess=false`; Service Worker equivalents are feature-gated | No universal file access enablement or file chooser bridge in reviewed product source. |
| Mixed content / Safe Browsing | `MIXED_CONTENT_NEVER_ALLOW`, Safe Browsing enabled and startup requested when supported | Actual protection depends on the installed Android System WebView provider/version; no audit of provider internals was performed. |
| Permissions | Manifest uses Internet/network state and foreground media service permissions; service `exported=false` | No storage, microphone, camera, location, notification-listener, accessibility-service or overlay permission. Exact protected-media permission gate only for `https://open.spotify.com`; additional resources denied. |
| Entry points | Main launcher exported; no OAuth/deep-link filter; debug test host non-exported in debug source set | Exported launch activity is expected. It does not currently consume untrusted URL intent extras. Recheck if links are added. |
| Cookie session | One player/login WebView, first-party cookies accepted, third-party cookies rejected, flush on finish/pause, ordinary rebuild preserves jar | Cookie persistence is sensitive and intentional. Do not dump `CookieManager`, derive tokens from cookies or use cookie names as authentication proof. |
| Data storage / backup | `allowBackup=false`, `fullBackupContent=false` and cloud/device transfer exclusion rules | App-private data is not application-level encrypted. Device compromise/OS access is outside this source audit. No claim of secure erasure or full WebView-profile wipe. |
| JavaScript/native boundary | No `addJavascriptInterface`; original account/login scripts evaluate bounded schemas | Visible DOM selectors can change. Account status is a conservative UI observation, not authentication authorization for a backend. |
| Diagnostic privacy | Console messages are counted without text/source; login diagnostics whitelist numeric metrics/classified hosts; raw URLs/form values are not copied | Clipboard export occurs explicitly through user action; system clipboard behavior is Android-version dependent. Keep it out of the normal welcome/product experience. |
| Account display | `AccountNames.clean` bounds and sanitizes optional visible name; excludes email/token-looking strings | A visible display name is personal data and remains memory-only in the reviewed account controller. No subscription/identity guarantee. |
| Lifecycle | Controllers invalidate generations/cancel callbacks on detach, renderer failure closes client, WebView is removed/destroyed | Avoid retaining destroyed views when adding native sheets/player. Full audio/background correctness requires separate device testing. |
| Updates | HTTPS public GitHub release request, no credentials, 512 KiB/10-second bounds, redirect disabled, repository-bound release link, no silent install | Signing identity and artifact hash must be checked for the delivered APK. Release metadata validation does not itself verify a downloaded APK signature. |
| Signing secrets | Gradle reads configured keystore path/password/alias from environment; no hardcoded signing secret in reviewed source | Missing custom signing is not proof an update will install over an old build. Preserve application ID and verify the actual output certificate. |

## Authentication design decision

There is no registered Spotify developer client ID/redirect URI in the task configuration and no client secret belongs in a mobile APK. The current supported login is official Spotify UI in the existing WebView. Supfy does not handle passwords or authorization codes.

PKCE protects a configured public OAuth client's authorization-code exchange. It returns scoped API tokens after consent; it does not issue Spotify Web Player cookies. A successful official Web API login must not be represented as a logged-in WebView. Browser cookies are also isolated from the WebView jar. Do not insert fake OAuth configuration, borrow credentials from other projects, scrape private auth state or weaken TLS/cookie policy as a login workaround.

## Findings and residual risks

1. **P2, consistency, confirmed source pattern:** native navigation paths such as replacement/reset used direct `WebView.loadUrl`, bypassing `SupfyWebViewClient.loadUrl`'s early requested/rendering/protection staging. Standard callbacks usually recover state, so this is not a demonstrated authentication bypass. Route native loads through the helper to give every entry the same guards and stage reporting. See `BUG_REGISTER.md`.
2. **P2, privacy race candidate, runtime proof pending:** `clearSiteData()` clears WebStorage/cache while the old live page and controllers still run, then asynchronously removes cookies and replaces the view. Stop/detach the old document before deletion so it cannot immediately repopulate storage or keep publishing stale account/media state. `WebStorage.deleteAllData()` plus cache/cookie clearing must not be advertised as a guaranteed wipe of every Chromium Service Worker, Cache Storage or memory artifact.
3. **P2, public-DOM compatibility:** account controls depend on visible official DOM selectors. UNKNOWN is the safe fallback. A missing selector must never become fabricated authentication, a fake profile or automatic data deletion.
4. **Limited dependency coverage:** declared versions include AGP 8.7.3, Kotlin 2.1.0, WebKit 1.12.1, Compose BOM 2024.12.01, lifecycle 2.8.7 and coroutines 1.9.0. Exact versions are pinned in `gradle/libs.versions.toml`; no dynamic range was seen. A subsequent bounded OSV query is recorded below. Dependency age alone does not prove a vulnerability. Android System WebView is supplied by the device and must stay current independently of the app's AndroidX WebKit library.

## Required regression checks at delivery

- Keep first-party cookies and valid sessions through native welcome, bottom navigation, settings, mini/full-player overlays, normal relaunch and a signed update.
- Test fresh install, returning-user migration, GUEST and UNKNOWN separately: native first-launch welcome allows browsing; returning users and UNKNOWN/loading/challenge must not be forced into login or cookie clearing.
- Keep Spotify login in one non-zero-height WebView. No duplicate credential window, native credential form or top-level overlay that traps the form/keyboard.
- Confirm certificate errors remain rejected and no new permission, exported component, URL entry point, token storage or unbounded diagnostic field was introduced.
- Verify logout and explicit clear data stop stale native account/media reporting. Validate what is actually cleared rather than describing it as full secure erase.
- Confirm downloads/offline, premium/DRM rights and external provider return are described according to real capability.

These checks are obligations, not asserted pass results. Build/device results belong in the final test report.

## Public OSV follow-up

The HTTPS `https://api.osv.dev/v1/querybatch` request completed at **2026-11-01 06:27:35 UTC** through the inherited environment proxy, with normal TLS verification enabled. The sanitized result is `/workspace/quantum-release/dependency-osv.json`. Only public Maven coordinates and versions were sent; no source, account, device identifier, cookie or token was included.

| Maven coordinate | Queried version | Coverage | OSV response |
|---|---|---|---|
| `androidx.core:core-ktx` | `1.15.0` | Explicitly versioned direct runtime declaration | No matching advisories returned |
| `androidx.activity:activity-compose` | `1.9.3` | Explicitly versioned direct runtime declaration | No matching advisories returned |
| `androidx.lifecycle:lifecycle-runtime-compose` | `2.8.7` | Explicitly versioned direct runtime declaration | No matching advisories returned |
| `androidx.webkit:webkit` | `1.12.1` | Explicitly versioned direct runtime declaration | No matching advisories returned |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.9.0` | Explicitly versioned direct runtime declaration | No matching advisories returned |
| `org.jetbrains.kotlin:kotlin-stdlib` | `2.1.0` | Auxiliary reference at the declared Kotlin plugin version; actual resolved runtime coordinate/version not independently verified | No matching advisories returned |

The catalog supplies five explicitly versioned runtime libraries. The sixth query is labelled separately because the Kotlin plugin declaration is not proof of a direct or resolved stdlib runtime version. Compose libraries are managed by BOM `2024.12.01`; no resolved Compose artifact versions were guessed. This query does not cover transitives, build/test dependencies, APK-resolved contents, native libraries, Android OS or the installed Android System WebView binary. An empty OSV match is a database response for the supplied coordinate/version, not an attestation that the app, dependency or device is vulnerability-free.

## Final welcome and diagnostics policy

Confirmed GUEST intentionally receives welcome on each cold process unless the user dismisses it during that visit. The persisted acknowledgement and existing-preference migration keep UNKNOWN returning users from being forced into a new welcome/login journey solely because the app was upgraded. UNKNOWN never clears cookies or proves logout. Fresh installs can show branded welcome immediately with a browse action. Actual Spotify login remains in the same main WebView.

The final integration retains Settings beneath the diagnostics overlay so Back returns to Settings, and catches an unavailable Android notification-settings activity. Numeric diagnostic copy does not include media titles, account names, URLs, credentials or tokens. These source observations do not replace the root build/device report.
