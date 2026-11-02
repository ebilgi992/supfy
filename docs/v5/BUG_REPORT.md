# V5 bug ledger

| ID / priority | Evidence and root cause | Fix / status |
| --- | --- | --- |
| V5-LOG-01 / High | Original owned diagnostic/logcat/export paths retained account/media diagnostics | Collection/export replaced with inert implementations; known old artifacts cleaned; source negative fixtures and real-code canaries PASS; two actual Android diagnostic-disabled tests PASS |
| V5-HISTORY-02 / High | Local full play/search and general screen caches retained activity unnecessarily | Persistent history removed; current-screen memory and chosen offline data retained; canary tests PASS |
| V5-BRIDGE-03 / High | Broad JavascriptInterface fallback and page bridge lacked narrow origin/lifecycle boundaries | Exact main-frame origin messaging and guarded synthetic extension origins; security policy tests PASS; runtime hostile-page tests not yet executed |
| V5-UPDATE-04 / High | Upstream updater could discover Myspoti binaries; default automatic checks | Fixed Supfy repository/package, opt-in automatic checks, digest/version/signer preflight; policy unit tests PASS; successful installation not inferred |
| V5-EXT-05 / High | Extension install/update policies did not meet V5 explicit reapproval rule | Disabled install state and version/permission approval gates; 206 extension tests PASS after aligning old auto-update expectations |
| V5-CORE-06 / High | Integrated Kotlin build found AppUpdater package shadowing and a composable used directly in a lazy scope | PrivacyLog import and keyed lazy item; subsequent complete debug build PASS |
| V5-SESSION-07 / High | TopStats could write a previous account's response after clear() | Generation-scoped memory cache rejects in-flight stale responses; deterministic regression tests PASS |
| V5-UI-08 / Medium | Recap copy promised persistent listening collection after privacy migration | English/Turkish copy now describes disabled local history |
| V5-AUTH-09 / Critical | Historical user reports: login not visible; new core source is integrated but not yet device-verified | Native Supfy Turkish welcome/login entry displayed on API30; provider login/session acceptance remains OPEN without a controlled account |
| V5-MEDIA-10 / High | Historical user reports: music continues but media notification absent | Owned PCM + Android media notification observed on API30; transport/background acceptance remains OPEN pending the corrected-service runtime test |
| V5-ENV-11 / Medium | Parallel lint and software emulator reached the actual 8 GiB cgroup memory limit | Stopped owned lint JVM; retained interrupted log; subsequent heavyweight work must run sequentially. Interrupted lint is NOT a pass |

Failed tests/builds remain in evidence logs. No device or authenticated-account case is
closed based only on source presence or a different legacy APK's tests.

## Subsequent runtime evidence — 2026-11-02

| ID / priority | Evidence and root cause | Fix / status |
|---|---|---|
| V5-DOC-12 / High | Actual Android run crashed on PLAYER_ORIGIN_REQUIRED after failed-document injection | Trusted-document/main-frame/epoch guards and narrow typed runtime error handling; five JVM regressions PASS; committed in 9bd0aed |
| V5-TEST-13 / Medium | Installer permission toggle killed the instrumented process; locale teardown overwrote restored app state | External permission driver and proper stored-choice restoration; subsequent runs passed the affected checks |
| V5-SYSTEM-14 / Medium | Android System UI ANR overlay intercepted focus and consent dialog input | Evidence captured; known System UI Wait dismissed. This does not resolve or excuse a Supfy ANR |
| V5-FGS-15 / High | Local PCM and media notification observed, then Android RemoteServiceException: startForegroundService did not then call startForeground | Cold promotion now precedes MediaSession construction; deduplicated pending starts and deferred logout/task-removal shutdown. Eight lifecycle JVM tests PASS (total958); corrected APK runtime verification pending |

V5-AUTH-09: native Turkish Supfy welcome and login entry rendered on API30. Provider
sign-in/session success remains unverified. V5-MEDIA-10: PCM + notification observed;
control/background acceptance remains OPEN because V5-FGS-15 interrupted the test.

| V5-UI-16 / Medium | Consent UI test used application-context English strings against an AppCompat-localized Turkish Activity on API30 | Test now queries Activity resources and asserts Turkish; repeat pending |
| V5-BUILD-17 / Medium | Late source edit overlapped debug compilation; bytecode lacked the final pending-notification scrub branch | Frozen-source rebuild PASS42s; apkanalyzer confirms final branch in debug; optimized release mapping already includes it |

Latest completed device suite:18/19PASS; the owned-media control/background case
passed without the prior foreground-service exception. The sole UI fixture failure
is retained in the test report. This does not establish live account playback.
