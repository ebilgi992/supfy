# Supfy1.0.1 /1030 — interface and promotion update

App production source commit:f31ba94. Package com.supfy.app; same local signer as
1029; versionCode increases to1030. GPL3/Myspoti/kota-zo/third-party notices retained.

- Removed startup donation prompt, Myspoti support rows, Ko-fi/GitHub Sponsors
  actions and dormant supporter sheet/badges from both looks and About.
- Retained existing accent/user preferences; no data clear/schema change.
- Added real Supfy website ACTION_VIEW rows to shared About and both settings.
- Website now uses the exact app icon and product/download copy; QA/version/hash
  details are kept in engineering records instead of the promotional website.
- Seven imagegen-created vertical HD campaign posters use supplied app screenshots,
  the user-authorized public Instagram profile image and @smt.ozdemr attribution.
  Every poster prominently recommends using a backup account. They are promotional
  previews, not new functional verification of provider playback/download rights.

Build:1030-build.log, SUCCESS10m19s. App JVM tests:507,0 failures/errors/skips.
Turkish resource inventory:1262/1262,0 errors. SDK apksigner v2 verification PASS;
certificate unchanged f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640.
Optimized APK SHA256:0c79e62fbee97093bdf9f719cd021ae00cfdafbcb9a3f6050e344935d02a2014.

This is the existing local/debug-certificate distribution, not a newly authorized
production-signing key. No fresh device/provider end-to-end pass is claimed for1030;
the earlier core/runtime limitations in TEST_RESULTS.md have not been declared fixed.
The donation/website edits did not alter the media/authentication core.

Website checks and public artifact byte verification are recorded at publication.

## Publication evidence

- App source f31ba94, website source fa33a54; marketing site removes QA metadata
  without deleting technical failure records or claiming those issues resolved.
- Browser suite19/19PASS (1030-site-publish-checks/report.json), including320–1440px,
  exact app-logo bytes, no visible QA/hash/version details, correct download URL,
  defensive URL handling, FAQ/menu, anchors and reduced motion.
- Public APK and seven-poster ZIP fetched without authentication and SHA-256 compared
  with local files; both match. Individual numbered PNGs are also publicly available.
- Website https://ebilgi992.github.io/supfy/ ; download branch apk-download-v5-1030.
- Posters ZIP: https://raw.githubusercontent.com/ebilgi992/supfy/apk-download-v5-1030/downloads/Supfy-WhatsApp-7-Poster.zip
- Poster master PNGs941x1672 (nominal9:16 HD); originals retained. Instagram photo
  came from the public smt.ozdemr page with explicit user authorization; no social
  post/message was sent. Final artwork uses imagegen; no fabricated working QR code.
