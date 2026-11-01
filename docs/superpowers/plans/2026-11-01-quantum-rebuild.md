# Supfy Quantum Implementation Plan

> **For agentic workers:** Execute the user-authorized independent media, protection, UI, QA, research and architecture tasks; root integrates and exclusively runs Gradle/ADB.

**Goal:** Preserve Supfy sessions while repairing measured media gaps and shipping a testable original native shell.
**Architecture:** Compose and bounded StateFlows adapt public WebView state; framework MediaSession owns notification transport only. CookieManager remains the sole Spotify Web Player session store.
**Tech Stack:** Kotlin/JDK17, Compose Material3, Android WebView, framework MediaSession, Gradle; no new runtime dependency.
**Spec:** docs/MIGRATION_PLAN.md and docs/superpowers/specs/2026-11-01-quantum-design.md.

## Global Constraints
- Preserve `com.supfy.app`, signing and all existing preferences/cookies; no uninstall/data wipe.
- No private API, credentials, DRM/subscription bypass, Windows patch execution or fake catalog/downloads.
- Unsupported/live-account/device cases are BLOCKED, never PASS from local fixtures.

## Review Focus
- Hidden/disabled desktop control must not suppress the real visible mobile control.
- UNKNOWN account state must not clear or reject a valid session.
- Native mute support is not evidence of current applied or audible silence.
- Background service permission and actual posted media notification must be observed separately.
- 320dp/large text and back navigation must keep real login and transport actions reachable.

## Tasks
- [x] Preserve 1027 baseline on the separate local branch.
- [x] Research SpotX/REA sources/license/version requirements; isolate tooling, record failed provider runs.
- [x] Add failing responsive selection tests; implement candidate visibility fallback; rerun Node tests.
- [x] Add failing ignored mute setter test; verify setter and live numeric mute state; rerun Node tests.
- [ ] Integrate welcome/bottom navigation/mini/full player, exact official route callbacks and numeric diagnostics.
- [ ] Add first-launch preference regression and native UI/back/large-font tests.
- [ ] Build JVM/lint/debug/test APK, then install-r and run own PCM background/notification tests.
- [ ] Run prior login/security/protection regressions and record every failed/skipped test.
- [ ] Review final diff, create architecture/media/UI/testing/release commits, verify APK signer/version/SHA and actual HTTPS bytes.
