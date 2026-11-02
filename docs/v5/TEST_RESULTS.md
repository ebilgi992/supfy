# Supfy V5 test results

Evidence date: 2026-11-02. Canonical project: `base/`, package `com.supfy.app`,
version `1.0.0-v5` / `1029`. Results from earlier Supfy builds are not counted as V5
results. This is a debug-certificate-signed **test build**, not a production release.

## Build and automated checks

| Check | Result | Scope |
|---|---|---|
| Gradle compilation, debug APK, Android test APK, optimized test APK | PASS | `v5-build-bridge-fix.log`: BUILD SUCCESSFUL in 7m39s; three ABIs ARM64, ARMv7 and x86_64 |
| JVM tests | PASS | 950 tests, zero failures/skips: api-spotify 181, extensions 206, host-android 56, app 507 |
| Source + packaged APK regression audit | PASS | Zero specified-rule findings; 12 negative fixtures. This is not a general security certification |
| Turkish resource audit | PASS | 1,260 default/Turkish entries; zero missing entries or format mismatches |
| Release-vital lint | PASS | Scoped Gradle release task only |
| Full Android lint | FAIL | First full run: 1,236 errors, including 1,095 other-language MissingTranslation findings. Other inherited Compose/API debts remain; later correctness fixes do not establish a full lint pass |
| APK cryptographic verification | PASS | Android SDK apksigner verified v2 signature; signer SHA-256 below |
| REA 6.8.0 APK inspection | EXECUTED | Supported Node 24.22.1; static APK signer/package inspection. Full analysis-provider doctor is unhealthy; see REA_ANALYSIS.md |

Source audit inputs and exact final artifact hashes are recorded with the release.
Passing the scanner means its listed contracts passed; it cannot establish that all
possible leaks, crashes, dependency vulnerabilities or provider-side processing are absent.

## Actual Android evidence

A disposable clone of the existing API 30 / Android 11 AVD was used; the original AVD
and original Supfy checkout were retained. Software CPU emulation has no KVM and
severe frame delays. The API 36 image repeatedly hit Android system-watchdog reboots;
it is retained and stopped, not counted as a compatibility pass.

- An actual same-signed 1028 → 1029 debug upgrade installed successfully.
- The native welcome screen displayed **Supfy**, Turkish text and a login entry button.
  This verifies the native entry screen, not successful provider authentication.
- A synthetic user-file canary survived; the real legacy background preference file's
  before/after SHA-256 matched. No app-data clearing/uninstall was used.
- Device tests exercised inert diagnostics, cleanup canaries, explicit optional-provider
  consent, update defaults and rejected package/version cases, stored locale/theme
  preferences, Media3 consent data sources, owned PCM/FLAC transcoding and offline crypto.
- Actual local PCM playback and an Android media notification were observed together
  before the media control test subsequently crashed. Notification presence alone is
  not a pass for pause/play, background lifecycle or full MediaSession compatibility.

### Failed and interrupted runs — retained

| Run | Observation | Action / status |
|---|---|---|
| Initial suite | 9 passed, then process killed when a test changed REQUEST_INSTALL_PACKAGES | Android's permission-change kill confirmed. Driver now grants/restores outside instrumentation |
| Next suite | 14 passed, then focus/ANR interruption | Locale fixture had polluted cold AppCompat restoration. Fixture restores the stored choice; media setup waits for resumed/focused activity |
| Media diagnostic | Uncaught PLAYER_ORIGIN_REQUIRED on an error-page injection | Production document boundary now checks trusted committed main-frame URL and failure state. Typed rejected-document errors handled; unexpected errors/cancellation propagate. Five focused JVM regressions passed |
| Full suite `android-final-all.log` | **17 passed, 2 failed**, all 19 executed | Media focus and consent-dialog visibility failed. Android System UI ANR overlay was captured; only its known Wait button was dismissed |
| Follow-up `android-final-clean.log` | First 14 passed, then media process crash; suite incomplete | Owned PCM and notification were observed, then foreground-service startup obligation failed. This remains an open high-priority runtime issue until a corrected build and rerun establish a pass |

No failing test was deleted or recounted as successful. Test-only bounded method-stack
samples contain no account payload and are not application logging/export features.
Raw runtime logs remain outside the public source tree because framework/provider output
can contain sensitive data. The public report records conclusions and generic error types.

## Not verified

- Controlled Spotify successful/failed login, session refresh, logout/account switching,
  live catalogue/DRM/Canvas/video playback: no controlled account was supplied.
- Bluetooth, physical headset and Android Auto behavior: no corresponding hardware/host.
- Release performance, ANR-free use, battery consumption or general device compatibility.
- Network loss/reconnect and screen-lock playback on a physical device.
- Full hostile IPC/subframe/runtime penetration testing and independent security audit.
- Guaranteed migration of a live provider session; preserved files do not prove its validity.

Signer SHA-256: `f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640`.
This local debug signer matches 1023–1028. Original 1018 used a different signer;
never uninstall a user's application to work around signature mismatch.
