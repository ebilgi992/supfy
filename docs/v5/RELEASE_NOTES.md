# Supfy Quantum V5 — 1.0.0-v5 /1029

Test build, 2026-11-02. Package `com.supfy.app`, minimum Android10/API29,
ARM64/ARMv7/x86_64. This is an optimized Gradle APK signed with a local **debug**
certificate, not a production signing key.

- Native Kotlin/Compose Myspoti-derived core at pinned commit
  `8e8898af03a21772357ae6248a41a33a28cd9b5e`; GPL-3.0 notices and credits preserved.
- Supfy branding, Turkish default, complete1260-key Turkish resource inventory,
  Immersive default and retained selectable Classic preference.
- Owned diagnostics/export and persistent full listening/search history disabled;
  narrow known-artifact cleanup preserves preferences, session and selected offline media.
- Revision-scoped explicit optional-provider consent in both looks; revocation gates
  future requests/ranges and stops stale optional video work.
- Exact-origin main-frame WebView messaging, no generic JavascriptInterface fallback,
  lifecycle/document boundary fixes and localized unsupported-WebView behavior.
- Supfy-only opt-in update checks with package/version/signature/SHA-256 preflight;
  extension install disabled by default with package/version/permission reapproval.
- Foreground promotion moved before cold media-session setup; pending starts
  deduplicated and logout/task-removal stop deferred until required start delivery.
- Turkish charcoal/green responsive website with real download/checksum, installation,
  privacy/test status, feature catalog, GPL credits and no Spotify affiliation notice.

## Delivery

- Website: https://ebilgi992.github.io/supfy/
- Public test APK: https://raw.githubusercontent.com/ebilgi992/supfy/apk-download-v5-1029/downloads/Supfy-V5-1029.apk
- Complete corresponding app source:
  https://github.com/ebilgi992/supfy/tree/5a64963662f3b7cd9a97b11ba6ce6262c914daea

- Development branch: `supfy/quantum-privacy-shield`.
- Website branch: `supfy-pages-v5`; APK delivery branch: `apk-download-v5-1029`.
- Original main branch/checkout/AVD retained. No real-device uninstall/data clear.

| Local artifact | SHA-256 |
|---|---|
| `/workspace/v5-release/Supfy-V5-1029.apk` | `3eacb22f46667c700dc42fa5c2cdb3f055aa1fde41cd0ed721c03eff7c787b40` |
| `/workspace/v5-release/Supfy-V5-1029-debug.apk` | `2d8bfe08cfb0c5b0c187e3128ea04cefe9cbe15ebc34360cbc7c47b2eec093e2` |
| `/workspace/v5-release/Supfy-V5-1029-androidTest.apk` | `9616de4a2bc5dcf5567ca7abc651460c156475a54460a6fcd279cbff46185e44` |

The public APK was downloaded without authentication and compared byte-for-byte
with the local artifact. Android SDK apksigner verifies its v2 signature. The local
signer matches1023–1028; it differs from original1018. Do not uninstall to defeat a
signer mismatch. Debug APK is retained locally; the smaller optimized test APK is
publicly linked. Secrets/keystores/raw private runtime logs are not distributed.

## Verification and remaining limits

958 JVM tests PASS; Turkish resource audit PASS; bounded source/APK audit PASS with
zero specified-rule findings and12 negative fixtures; scoped release-vital lint PASS.
Full lint remains FAIL. Website19 browser checks PASS from320px to1440px, including
clipboard, menu, FAQ, reduced motion and fail-closed download behavior.

The final device run passed15 cases, including owned PCM media session/notification,
system controls and background preference, then ended in a consent UI input-focus
ANR;3 remaining cases did not run. The ANR persisted with an awake/unlocked device.
Consent UI verification remains BLOCKED. The actual public APK installed, its device
checksum matched, and native Turkish Supfy welcome/login entry displayed. Provider
login page loading failed with a localized error; successful authentication is not
verified. Cold-start timing reported a timeout; no performance pass is claimed.

Controlled Spotify login/session refresh, live licensed catalogue/DRM playback,
Bluetooth, physical headset and Android Auto remain NOT TESTED. No trouble-free,
all-device reliability or100% security guarantee is made. Supfy is independent and
has no affiliation, sponsorship or approval from Spotify AB.

This is not a completed stable release: the consent UI ANR, provider authentication
and full lint remain open acceptance gates. See TEST_RESULTS.md and BUG_REPORT.md.
