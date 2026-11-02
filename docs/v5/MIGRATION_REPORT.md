# V5 migration and rollback

- Legacy checkpoint: `24a3630`; separate worktree branch `supfy/quantum-privacy-shield`.
- Upstream source import: `fbde08a`; original checkout `/workspace/supfy` retained.
- Package remains `com.supfy.app`; version increases to `1.0.0-v5` / `1029`.
- Existing Supfy adaptive icon assets are reused. Namespace/source packages stay
  upstream-compatible. The old explicit activity component has a narrowly scoped alias.
- `SupfyDefaults` reads the legacy `supfy_ayarlar/arka_plan_oynatma` preference without
  modifying it. V5 changes are stored in `supfy_v5_defaults` separately.
- Turkish is the first-run default; an existing explicit app-locale choice is respected.
  Immersive is the unsaved-look fallback; an explicit Classic choice is preserved.
- There is no destructive schema migration, blanket preferences reset or app-data clear.
  Session/WebView storage, chosen media and offline collection records are retained.
- Only exact known diagnostic/history artifacts are removed by `PrivacyCleanup`.
  Boundary/symlink canaries exercise this code; broad directory deletion is not used.

The local debug signer is the signer used for legacy 1023–1028, SHA-256
`f0e5840358d25473a5aac702d6e93046121555a2bb88bc4cb4cdc79fda705640`.
Original 1018 used a different signer. Never uninstall to defeat a signature mismatch.
An actual upgrade/canary test must be recorded in TEST_RESULTS.md before device data
preservation is described as verified. Preserved bytes do not prove a provider session
is still valid; live account testing is separate.

Rollback keeps the checkpoint/source and prior APK available. Android normally
refuses a version downgrade; do not promise one-click data-preserving downgrade.
Use a later, same-signed recovery version based on the checkpoint if a real rollback
is needed. No automatic destructive downgrade is performed.
