# Supfy V5 architecture

Canonical Android sources are `base/`, derived from Myspoti commit
`8e8898af03a21772357ae6248a41a33a28cd9b5e`, GPL-3.0. The original copyrights,
AGENTS notice, LICENSE and third-party notices are retained. The former Supfy
`app/` remains a recovery source outside the V5 build.

| Module | Responsibility |
| --- | --- |
| `base/app` | Kotlin/Compose UI, Classic and Immersive navigation, account state, preferences, media coordination, optional service consent and updater |
| `base/api` | Platform-independent API and media contracts |
| `base/api-spotify` | Spotify API implementation and session-aware calls |
| `base/host-android` | Official Web Player WebView, exact-origin native messaging, media notification/session service, Android lifecycle and transport |
| `base/host-cdp` | Upstream desktop/CDP host retained for source completeness; not the Android playback backend |
| `base/extensions` | Extension manifests, sandbox, network boundaries and explicit approval policy |
| `base/extensions-cli` | Upstream development CLI; production Android CLI receivers are not registered |

The existing compatible architecture is preserved. No artificial Hilt/Room/module
rewrite was added. Media3 remains where upstream uses it; Android media-session
compatibility APIs remain the Web Player service's existing integration.

`MyspotiApplication` initializes `SupfyDefaults`, `ConsentPolicy` and narrow
`PrivacyCleanup` before Graph/runtime setup. `MainActivity` shows the native
welcome screen for a missing session and the provider's own WebView for sign-in.
A stored boolean is not sufficient evidence of authentication: session confirmation
requires the runtime's non-anonymous account result. Unsupported secure WebView
messaging produces a localized update message; no broad JavascriptInterface
fallback is exposed.

Owned diagnostic collection/export and persistent full listening/search histories
are disabled. Memory caches needed for the current screen remain. `SessionCache`
invalidation cancels late responses rather than restoring a previous account's
TopStats after logout. Explicit offline media and pending user library operations
remain user data, not disposable diagnostic caches.

Optional lyrics, translation, Genius explanations and video requests wait for a
provider-revision consent decision. Both UI looks share one disclosure host and
privacy controls. Automatic updates are opt-in and Supfy-only. Exact checks and
remaining limits are in SECURITY_AUDIT.md and NETWORK_REPORT.md. Source architecture
is not proof of live authenticated playback.
