# V5 delivery ledger — 2026-11-02

- Public site live: https://ebilgi992.github.io/supfy/ . APK download is byte-verified.
- Public optimized test APK SHA-256:3eacb22f46667c700dc42fa5c2cdb3f055aa1fde41cd0ed721c03eff7c787b40.
- Version1.0.0-v5/1029, com.supfy.app, three ABIs, local debug signer.
- Actual public APK installed on cloned API30; installed checksum matches and native
  Turkish Supfy welcome/login entry rendered. Provider page load failed with native
  Turkish error. Startup wait timed out; no performance or successful login claim.
- 958 JVM tests PASS;1260 Turkish keys complete; bounded source/APK audit PASS,
  zero rule findings/12 negative fixtures; scoped vital lint PASS; full lint FAIL.
- Latest device run15PASS, consent UI ANR interruption,3NOTRUN. Native owned PCM,
  session notification, system controls and background preference PASS. Consent UI
  verification remains OPEN even after wake/unlock and isolated debug test hosting.
- All failed/interrupted runs retained; original checkout/AVD/main and user data
  preserved. No controlled-account login/DRM, Bluetooth or Android Auto verification.
- Complete source and Myspoti/GPL credits are public on supfy/quantum-privacy-shield.
- Details and evidence links: TEST_RESULTS.md, RELEASE_NOTES.md, WEBSITE_TEST_RESULTS.md.
