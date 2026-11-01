# Current architecture and Quantum boundaries

Audit date: 2026-11-01. This is a source review of the working tree during the Quantum rebuild, not a report that every Spotify journey was exercised. The user reports that version 1027 displays login and plays real music. Keep that working baseline. Source locations below identify the reviewed components; the rebuild may move line numbers.

## Runtime and ownership

`MainActivity` is the single exported launcher activity (`singleTask`). There is no OAuth callback intent filter, registered developer client ID, redirect URI, client secret, application backend, or Spotify Web API client in the reviewed source. `SupfyApp` initializes supported WebView Safe Browsing. Kotlin/Compose owns the Android shell, settings and error handling. One main Android WebView owns Spotify's public pages, login form, redirects, history, DOM storage and player session.

`WebViewFactory` creates the WebView, `SupfyWebViewClient` gates navigation and observes page events, and `SupfyChromeClient` restricts permissions. `WebPageGuard` observes rendering health. `AccountController` observes bounded visible account UI. Playback and protection are separate original scripts/controllers attached to the player; they do not establish authentication. `PlaybackNotificationService` is a non-exported media foreground service. `AppSettings` stores preferences, not credentials.

The Compose `AndroidView` must retain the same WebView instance while changing native overlays or tabs. The native WebView layout parameters must remain `MATCH_PARENT` in both dimensions. The earlier blank login root was the interaction between Compose's `WRAP_CONTENT` layout parameters and Chromium's zero layout height path; measuring a full-size native view did not repair CSS viewport height. Do not replace this fix with cookie resets, TLS exceptions, altered Spotify markup, another login WebView, or an unconditional login redirect.

## Session and entry flow

The 1027 entry flow restores WebView state when available, otherwise loads `https://open.spotify.com/`. Cookies survive ordinary view replacement, process restart and upgrades signed with the same key/package. Page finish and activity pause flush the cookie jar. `clearCache()` preserves cookies; the explicit confirmed clear-data action removes cookies and WebStorage and rebuilds the view. Logout navigates Spotify's own logout route. Removing the app destroys its private session; an APK signing mismatch must never be solved by uninstalling without the user's informed choice.

The Quantum welcome policy has two inputs: a native first-launch preference and conservative observed account state. A genuinely new install can show the branded welcome immediately with an explicit browse option; this does not require clearing the WebView or forcing login. Existing preference/session evidence identifies returning users, who must not be forced back through welcome merely because the new welcome key is absent. After migration, persist the first-launch decision/dismissal. Account states retain these meanings:

| State | Evidence | Native behavior |
|---|---|---|
| UNKNOWN | Initial state, auth/account route, loading, hidden UI, timeout, changed selectors, renderer replacement, contradictory signals | Keep the valid session and current journey. Show an honest checking/unavailable state. Never infer logout, erase data or force login. |
| GUEST | A visible official login affordance with zero positive account signals on the trusted player page | A native welcome with sign-in, signup and browse actions may appear automatically. Show it once per relevant guest entry, not repeatedly during every poll or after an explicit browse choice. |
| AUTHENTICATED | At least two corroborating visible player account signals accepted by `account_probe.js` and `AccountController.decode` | Dismiss welcome and show player shell. Account name is optional. Do not treat the observation as an API token or verified subscription entitlement. |

UNKNOWN must remain distinct from GUEST after installation over an existing app. Start/restoration must allow the existing cookie session to resolve before using account state to decide welcome routing. An authenticated user may temporarily become UNKNOWN while on Spotify account/auth pages; that must not navigate away. A native first-run preference can record welcome dismissal but cannot prove a session exists or is absent. Explicit login/signup remains available without discarding cookies.

## Authentication boundary

Login is Spotify's public HTTPS UI in the existing main WebView, including Spotify e-mail/code/password/challenge pages. Supfy must not recreate credential fields, capture form values, replay POST bodies, read cookies/tokens, bypass CAPTCHA, suppress authentication consent, or pretend external provider success returns to the app.

The existing Google/Apple/Facebook route uses a short-lived user-interaction gate and a native explanation before opening HTTPS in the external browser. Browser and WebView cookie jars are separate. Logging into Spotify or an identity provider in Chrome does not create the player's WebView cookie session. The explanation is required; an in-app e-mail/code alternative remains available.

An official Web API Authorization Code + PKCE integration would require a developer application, public client ID, exact registered redirect URI, state/verifier handling and user consent. It returns scoped Web API authorization; it does not mint or import Spotify Web Player cookies. It therefore cannot repair WebView login or be presented as a substitute session. No arbitrary/example client ID, another application's credentials, invented redirect, client secret in an APK, private endpoint, cookie extraction, or token scraping may be added. A future authorized API feature would be a separate integration with explicit configuration and capability checks.

## Screen and action responsibility map

The baseline source had a WebView shell, settings and protection screens; welcome, bottom navigation and native mini/full-player are Quantum additions. This table defines their boundary without claiming unavailable features are implemented.

| Screen or action | Native responsibility | Spotify public-site responsibility / truthful limit |
|---|---|---|
| Welcome / first guest entry | Original Supfy branding, clear login/signup/browse actions, dismissal/entry policy | Sign-in and signup open official public pages in the same view. |
| Home / recommendations / recent content | Bottom tab and route action | Player home content and recommendations remain Spotify-rendered; no invented cards or scraped catalog database. |
| Search / categories | Bottom tab and route action | Public player search, results and category detail; native shell does not claim its own catalog API. |
| Library / saved content / liked songs | Navigation and honest unavailable/guest message | Public player collection UI and account-dependent access; no fake saved library. |
| Playlist / album / artist / track / episode / show detail | Preserve back stack, safe route, loading/error chrome | Public player content, play and edit capabilities depend on account/site. Existing recognized `spotify:track/album/playlist/episode/artist` content IDs map to HTTPS; unrecognized schemes stay blocked. |
| Create/edit playlist, add/remove item, follow/like | Only user-requested, supported public UI action if actually wired | Spotify owns result and permissions. Disable or delegate actions without a verified public UI control; never fake success. |
| Mini-player | Bounded current playback snapshot and supported callback controls | Actual media/Spotify player state is authoritative. Unknown metadata is blank/unavailable, never a sample track. |
| Now playing / progress / seek / previous / next / shuffle / repeat | Original native presentation, enabled capability checks, user callbacks | Operate existing public player controls only. Unsupported seek or queue behavior must remain unavailable. No second player or playback engine. |
| Queue / lyrics / devices | Route/delegate supported public control and report missing capability | Spotify renders available queue/lyrics/device selection; no fabricated queue, lyrics, device list or remote connection. |
| Downloads / offline music | Explain unavailable capability if exposed | No supported offline download implementation. No download permission, downloader, cached-song export, fake completion, premium promise or private audio endpoint. |
| Profile / account / logout / signup | Conservative account status/card, sanitized visible name and public route callbacks | Public Spotify account/profile/menu/logout journeys; arbitrary viewed profiles must not become the signed-in identity. |
| Settings / appearance / storage / update / about | Compose controls and local preferences, explicit destructive-data confirmation, public release link | Spotify's internal website theme/account settings remain website-owned. Supfy theme does not imply every web page is recolored. |
| Protection / audio repair | Actual options, bounded counters/errors and explicit recovery | No entitlement, DRM or subscription bypass; recovery retains cookies and does not promise to control inaccessible media. |
| Login challenge / provider handoff | Honest loading/error diagnostics and safe browser handoff | Spotify/provider own challenge and acceptance. Native health checks must not hide a valid form. |
| Offline / HTTP / TLS / rendering failure | Clear native reason and scoped retry | No fake offline playback or SSL exception. |
| Background notification / lockscreen | Android MediaSession/foreground-service controls | Real currently observed player state only; OS/vendor lifecycle limits remain test obligations. |

## Security and persistence map

Main-frame HTTPS is restricted to the reviewed Spotify/CDN suffixes with a label boundary, no user-info and no non-default TLS port. Other HTTPS opens externally only through the gesture/provider gate; HTTP, file, data, intent and arbitrary script schemes are blocked. HTTPS frames remain permitted for normal provider/challenge content. POST main-frame navigation receives a separate request gate because `shouldOverrideUrlLoading` does not cover POST. Certificate errors cancel. Only protected-media permission from the exact player origin is granted; camera/microphone/geolocation are not granted.

First-party cookies and DOM storage are required. Third-party cookies, file/content access, mixed content and automatic multiple windows are disabled. No JavaScript-to-native interface is exposed. Evaluated original scripts return bounded summaries; login diagnostics contain numeric structural counters and classified hosts, not entered credentials, raw console messages or full URLs. Backups and data extraction are excluded. WebView data remains sensitive app-private material even though app code does not read cookie values.

`MainActivity` owns lifecycle attachment/detachment, resumes foreground observations, stops connectivity callbacks, closes scripts/controllers and destroys replaced views. Background media has its own narrow lifecycle requirements and must be tested separately from login. The network update client reads public GitHub release metadata over HTTPS with bounded bytes/timeouts and opens a validated release page; it does not install APKs silently.

## Validation boundary

This audit used current manifests, Kotlin, resources, assets, dependency declarations, tests and existing local reports. It ran no Gradle build, ADB session or authenticated account action. A later bounded public OSV query is documented in `SECURITY_AUDIT.md`; it is not a complete dependency scan. Existing older V10 feature matrices are historical and do not establish current runtime success. The root delivery report must record its actual build/test/device evidence and final screen inventory after integrating the native shell.

Separate media/audio review reproduced three responsive-selector regressions in which retained hidden/disabled desktop controls suppressed usable mobile controls. It also reproduced a DOM mute setter silently failing while previous status reported success. The corresponding original scripts/controllers now select usable controls and report bounded playback/service/audio health, including observed native mute state and current muted DOM count. Those fixture-level findings do not prove the actual native audible-output cause on the user's 1027 device, which remains unproven until device measurement. Merely replacing notification plumbing with Media3 cannot give it ownership of Spotify's protected WebView stream; an authorized playable source is absent.

Final integrated welcome rule: a confirmed GUEST intentionally sees welcome on each cold process unless dismissed during that visit. Persisted acknowledgement/migration prevents an UNKNOWN returning user from being forced into welcome solely by an upgrade; acknowledgement is not proof of authentication. Diagnostics keep Settings underneath, so Back returns there; the external notification-settings action handles an unavailable activity.
