# Supfy V5 verification — 2026-11-02

**Delivery status: TEST BUILD. The acceptance criteria are not all satisfied.**
The public APK installs and displays the native Turkish Supfy welcome/login entry.
The consent UI test still ends in ANR, including an awake/unlocked repeat. Successful
Spotify authentication and live licensed playback are not verified. Full lint fails.

## Current evidence

| Check | Result | Exact scope |
|---|---|---|
| Production-source build | PASS | `v5-build-service-fix.log`, BUILD SUCCESSFUL7m52s; debug, test and optimized APKs; ARM64/ARMv7/x86_64 |
| Frozen-source debug consistency build | PASS |42s; final pending-notification scrub verified in bytecode. Later debug-only harness build1m35s and diagnostic test-package build28s also passed |
| JVM tests | PASS |958, no failures/errors/skips: api-spotify181, extensions206, host-android64, app507. Eight service-lifetime regressions included |
| Turkish resource audit | PASS |1260 default/Turkish entries, no missing/format errors. Does not imply every screen was rendered |
| Bounded source/APK audit | PASS |948 production inputs, zero specified-rule findings,12 negative fixtures; not a general security certification |
| Release-vital lint | PASS | Scoped Gradle release checks only |
| Full lint | FAIL | First complete local run1236 errors, including1095 other-language translation gaps; other Compose/API debt remains. Later fixes do not establish a full pass |
| Final Android instrumentation | **INCOMPLETE** |15PASS,1 interrupted by input-focus ANR,3NOTRUN. No skips. See below |
| Actual public APK installation | PASS | Same-signed1029 debug → optimized test APK installed on disposable API30/Android11 AVD. Installed `base.apk` SHA-256 equals the public/local artifact |
| Native welcome/login entry | PASS | Actual optimized APK shows Supfy, Turkish copy and “Spotify ile giriş yap”; [real screenshot](evidence/primary-welcome.png) |
| Provider login page | **NOT VERIFIED** | Tapping the button reached the Turkish “Giriş tamamlanmadı” / provider-page-load error. Root cause not established; this is not a successful official-page or login test |
| Cold-start timing | INCONCLUSIVE | `am start -W` reported timeout/11708ms; the subsequent UI hierarchy and screenshot show the native screen rendered. No startup/performance pass is inferred |
| APK signature | PASS | SDK apksigner verifies v2 signature; local debug certificate, not production key |
| Public download integrity | PASS | Unauthenticated full HTTPS download and byte-for-byte comparison; matching SHA-256 |
| Website | PASS |19 local real-browser checks,320–1440px; public Pages HTTPS200 and deployed-file byte checks. [Website scope](WEBSITE_TEST_RESULTS.md) |
| REA6.8.0 static APK inspection | EXECUTED | Node24.22.1; final artifact digest matches. Full provider doctor unhealthy; not a full reverse-engineering environment pass |

Machine-readable public evidence: [JVM](evidence/unit-tests.json),
[Android](evidence/android-final.json), [source/APK guards](evidence/security-audit.json),
[apksigner](evidence/apksigner.txt), [REA](evidence/rea-apk-signature.json).
Private runtime logs and provider payloads are not distributed.

## Artifact identities

| Artifact | SHA-256 |
|---|---|
| Public optimized test APK | `3eacb22f46667c700dc42fa5c2cdb3f055aa1fde41cd0ed721c03eff7c787b40` |
| Current local debug APK | `2d8bfe08cfb0c5b0c187e3128ea04cefe9cbe15ebc34360cbc7c47b2eec093e2` |
| Current local instrumentation APK | `9616de4a2bc5dcf5567ca7abc651460c156475a54460a6fcd279cbff46185e44` |

Production app source: commit5a64963. Later changes are debug-only test hosting,
instrumentation, website and documentation; they do not alter the public optimized
APK. Final source audit digest:
`a71296832fe4867c3c9636d21ffdd4fc746f61aec1ef8797dcc4b8f9e4aafed9`.

Signer SHA-256:
`f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640`.
It matches local1023–1028; original1018 used a different signer. Never uninstall
user data to bypass signature mismatch. CI builds use an ephemeral signer.

## Real Android scope

The isolated API30 AVD is a copy of the earlier test device; the original AVD and
original checkout were not changed. Software CPU emulation has no KVM. API36
repeatedly hit system-watchdog reboots and is retained/stopped, not counted as a pass.

A real1028 →1029 debug upgrade preserved a synthetic file canary and the real legacy
background preference bytes (matching before/after SHA-256). No app-data clear or
uninstall was used. Preserved bytes do not prove a provider session remains valid.

The final `android-awake-final.log` executed:

- Two diagnostic-disabled checks; locale-migration phase-order check; five V5
  identity/resource/consent/update-default/theme/background preference checks.
- Two real updater package/version rejection cases without an installer session.
- Two PCM/FLAC-to-AAC codec checks and two offline-crypto/player checks.
- **Owned local PCM playback, media-session notification/token, system pause/play,
  next/previous, advancing playback after Home when enabled, and pausing after Home
  when disabled: PASS.** This is not live Spotify/DRM or long-duration/Doze evidence.
- Consent disclosure in Immersive: input-focus ANR during composition/measurement;
  decline/allow and Classic coverage were not established. The final three Media3
  consent-range cases did not execute in this interrupted run; all three passed in
  earlier separately identified complete runs.

The device was explicitly awake/unlocked, with temporary stay-on enabled, during
the last repeat. A prior ANR snapshot also showed sleep/keyguard, but correcting
that precondition did not eliminate the ANR. Bounded test-only main-thread samples
progress through AlertDialog creation, accessibility IPC, composition, font resolution
and text measurement. They establish neither a stationary lock nor its root cause.
An app defect and emulator scheduling/performance are both not ruled out. No ANR-free
claim is made. Test-only diagnostics are absent from the production APK.

## Failures and corrective attempts — retained

| Evidence log | Observation | Change / outcome |
|---|---|---|
| `android-instrumentation-all.log` |9PASS, then process killed by REQUEST_INSTALL_PACKAGES permission change | Driver grants/restores outside instrumentation |
| `android-instrumentation-fixed.log` |14PASS, then focus/ANR interruption | Locale teardown now restores stored app choice; media waits for resumed/focused Activity |
| `android-media-diagnostic.log` | PLAYER_ORIGIN_REQUIRED runtime crash | Trusted committed-document/epoch guards and typed error handling, commit9bd0aed; five JVM regressions PASS |
| `android-final-all.log` |17/19PASS; media focus and consent visibility FAIL | Captured System UI ANR overlay; only its known Wait button dismissed |
| `android-final-clean.log` |14PASS, then foreground-service crash after PCM + notification | Early bootstrap promotion, pending-start deduplication and deferred stop, commit5a64963; eight JVM regressions PASS |
| `android-service-fixed-all.log` |18/19PASS; media case PASS; consent FAIL | Application-context English vs Activity Turkish query corrected; not sufficient to close UI verification |
| Debug bytecode inspection | Last small source edit overlapped debug compilation | Frozen-source rebuild; final pending-notification scrub confirmed in both debug and optimized APK bytecode |
| `android-final-consistent.log` |17/19PASS; focus/consent FAIL; System UI overlay captured | Known OS dialog dismissed; previous run remains FAIL |
| `android-final-targeted.log` |2FAIL: transient bootstrap lacks session token; consent not visible | Test now waits for real MediaSession notification; shared consent UI uses isolated unexported debug-only Activity |
| `android-harness-final-all.log` |15PASS; consent input-focus ANR; final3NOTRUN | Sleep/keyguard found in ANR snapshot; wake/unlock/stay-on fixture added |
| `android-awake-final.log` |15PASS; consent input-focus ANR persists; final3NOTRUN | **BLOCKED/OPEN**; method-stack evidence collected, no unsupported root-cause or pass claim |

No failing case was deleted or hidden. Raw evidence stays outside public source because
framework/provider logs can contain private data. The public JSON contains names and
statuses only.

## Independent GitHub CI

[Run41421673954](https://github.com/ebilgi992/supfy/actions/runs/41421673954) at
pre-service-fix commit4ade24a passed SDK setup, source guards, JVM tests, APK compilation,
APK upload and complete GPL source archive upload. Full lint failed; overall job FAIL.
The earlier SDK bootstrap failure was corrected using the pinned Ubuntu24.04 SDK.
Subsequent local service-fix evidence is separate from that remote run.

## Not verified / acceptance still open

Controlled Spotify successful/failed account login, session renewal/logout/account
switch, live licensed catalogue/DRM/Canvas/video, Bluetooth/headset/Android Auto,
network-loss/reconnect, screen-lock/Doze/long battery behavior, physical-device
performance, full navigation/font scaling/TalkBack and runtime hostile-IPC/security
penetration scenarios remain unverified. There is no claim of blanket OWASP compliance,
trouble-free operation, all-device compatibility or100% security.
