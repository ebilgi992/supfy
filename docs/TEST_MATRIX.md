# Quantum native playback test matrix

These instrumented tests use the existing debug-only `WebViewTestActivity`, Android WebView,
and production `PlaybackWebController` / media notification service. They require no Spotify
account, login, API, proprietary media, or new dependencies. They do not clear application data.

| Test | Evidence | Limit / outcome |
| --- | --- | --- |
| `com.supfy.app.quantum.RealAudioPlaybackTest#nativeTapBackgroundAudioAndMediaSessionPauseUseVisibleMobileControl` | Generated 30-second mono PCM WAV (440 Hz sine, 16 kHz, looped data URL); real decoder ready state; Android native touch with trusted DOM click; `mediaPlaybackRequiresUserGesture=true`; real `currentTime` advances and `paused=false`; framework MediaStyle notification, real MediaSession token and playing state; ActivityScenario actually reaches CREATED with controller foreground false, clock advances after 2 seconds; MediaController pause stops the real audio clock; paused session state; disable removes notification. | **PASS** on API 30 / WebView 83. Local generated audio only. Does not prove live Spotify playback, DRM, locked-device behavior, audible speaker output, or long background survival. |
| Same test, mobile control regression | Hidden exact desktop `control-button-playpause` coexists with a visible button in `mobile-player`, labelled Oynat / Duraklat. Native play and session pause increment the visible button twice, hidden button stays at zero, actual audio pauses. | Exercises the mobile label fallback gap missed by DOM-only happy-path tests. |
| `com.supfy.app.quantum.NativeAudioMuteTest#supportedNativeMuteRoundTripRestoresOriginalStateOnRealPlayingAudio` | On supported WebViews, the public `WebViewCompat.setAudioMuted(true/false)` and `isAudioMuted` roundtrip while real PCM continues advancing; original native state restored in finally. | **BLOCKED** (JUnit assumption skip with reason) when `WebViewFeature.MUTE_AUDIO` is unsupported, including the known API 30 / WebView 83 environment. A passing native getter is API-state evidence, **not acoustic silence evidence**. JavaScript `.muted` is never substituted. |
| Acoustic output / silence | Requires an actual output measurement or consented Android AudioPlaybackCapture route, with source capture eligibility and valid user consent. | **BLOCKED / not implemented**: no capture consent available. No bypass, no claim of audible silence or restored audible output. |

The local HTML uses **both base URL and history URL**
`https://open.spotify.com/supfy-audio`. The fixture asserts `web.url`, the trusted production
origin policy and JavaScript origin. It blocks all intercepted requests and embeds its WAV;
it does not weaken production origin checks or use `about:blank` as the document URL.
`about:blank` is used only after assertions during WebView destruction.

## Execution

Additional native UI/migration classes (90 s per UI test; migration tests are synchronous):

```text
com.supfy.app.quantum.AppSettingsMigrationTest
com.supfy.app.quantum.QuantumNativeUiTest
```

`AppSettingsMigrationTest` redirects every `getSharedPreferences` call through a
`ContextWrapper` to a UUID namespace and deletes only that test namespace. It checks fresh
welcome requirements, persisted acknowledgement, upgrade preference preservation and an
explicit false acknowledgement marker. It never clears production preferences or site data.

`QuantumNativeUiTest#largeFontNativeWelcomeScrollReachesLoginSignupAndContinueCallbacks`
renders the production Compose component in the debug test host with a scoped 1.8x font
scale and test-owned lifecycle/saved-state owners. Native accessibility actions scroll to
and activate the actual login/signup/guest buttons, verifying each callback exactly once.
This checks component rendering and callback wiring, not production authentication.

`QuantumNativeUiTest#productionBottomBarSettingsDiagnosticsAndBackKeepNativeNavigationReachable`
launches the actual MainActivity, checks all four native bottom destinations, opens Ayarlar,
opens Durumu kontrol et, verifies Oynatıcı ve ses, and checks back navigation. It does not
change preferences or perform login. If the welcome overlay is active, it assumption-skips
with an explicit BLOCKED reason to preserve that state.

Clean-install first launch, automatic AUTH welcome dismissal, and explicit GUEST account
transitions remain **NOT RUN** until root executes an isolated-user/device scenario. Existing
account state is never reset to manufacture those results.

Run all four classes on an already installed matching debug + androidTest APK pair:

```sh
adb shell am instrument -w -r \
  -e class com.supfy.app.quantum.RealAudioPlaybackTest,com.supfy.app.quantum.NativeAudioMuteTest,com.supfy.app.quantum.AppSettingsMigrationTest,com.supfy.app.quantum.QuantumNativeUiTest \
  com.supfy.app.test/androidx.test.runner.AndroidJUnitRunner
```

Independent method selectors:

```text
com.supfy.app.quantum.RealAudioPlaybackTest#nativeTapBackgroundAudioAndMediaSessionPauseUseVisibleMobileControl
com.supfy.app.quantum.NativeAudioMuteTest#supportedNativeMuteRoundTripRestoresOriginalStateOnRealPlayingAudio
```

Allow 120 seconds for playback test and 90 seconds for native mute test. Bounds inside the
playback test: document load 30 s, decode 15 s, native play 10 s, notification 20 s,
session state 10/12 s, background wait 2 s, transport pause 12 s, notification removal 5 s.
These are individual upper bounds, not an assertion that every bound can be consumed within
the test timeout. Keep the emulator foreground/unlocked for the native input step. On Android
13+, notification posting permission must be granted by the test environment.

## Run record

Root executed the targeted suite on 2026-11-01, API 30 / Android System WebView 83.
`/workspace/quantum-release/android-targeted.log` records `OK (7 tests)` in 98.19 seconds:
**6 actual PASS and 1 BLOCKED assumption skip**, not seven fully exercised passes.

| Check | Actual result |
| --- | --- |
| Real native-tap PCM, foreground/background clock, MediaStyle/session pause, mobile control regression and notification removal | **PASS** |
| Public native mute roundtrip | **BLOCKED**: installed WebView does not support `MUTE_AUDIO`; runner status `-4` with explicit reason |
| Fresh preferences and persisted acknowledgement | **PASS** |
| Upgrade preserves existing preferences and skips welcome | **PASS** |
| Explicit unacknowledged marker requires welcome | **PASS** |
| Large-font native welcome scrolling and three callbacks | **PASS** |
| Actual MainActivity bottom labels, settings, diagnostics and back | **PASS** |

The initial run encountered a **System UI ANR overlay**; its preserved log is
`/workspace/quantum-release/android-quantum-systemui-anr.log`. Root's screenshot
`/workspace/quantum-release/quantum-ui-current.png` and window dump
`/workspace/quantum-release/quantum-window.txt` identified
`Application Not Responding: com.android.systemui` covering the application. Native touch
then failed with an `INJECT_EVENTS` SecurityException, and both UI checks failed to see
their expected app content. Root dismissed the system dialog and reran the **same APKs**;
the checks above passed without a test helper or product source change. The initial run
remains an environment failure, not evidence of an audio defect or a successful check.

No build, install, Gradle, or ADB command was executed by the QA author; root owned device
execution. The previous DOM-only controller test is not evidence of real playback or
audible muting. Acoustic silence, live Spotify and clean-install/account transition
limits remain as stated above.
