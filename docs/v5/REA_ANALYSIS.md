# V5 REA analysis

REA package: `rea-agents` 6.8.0, MIT, from morluto/rea. Its declared Node requirement
is `^22.23.4 || ^24.22.1 || >=26.12.1`; this run explicitly uses the retained
Node 24.22.1 binary instead of the environment's older Node 24.19.0.

`inspect-apk-signature` executed against the real V5 x86_64 debug APK after a successful
Gradle build; complete evidence is retained as `rea-v5-debug-signature.json` outside
private user storage. Its declared signer/scheme observations are static evidence,
not a cryptographic-verifier or playback claim. Android SDK apksigner is a separate
verification step for the final delivered artifact.

`doctor --target ...` executed and reported **healthy=false**: Node, host and target
checks passed, while Hopper, Ghidra, IDA, JADX MCP, Kuna, Codex registration and installed
REA skill identity were unavailable. CLI static analysis works without those providers.
Do not label the full REA provider environment installed/healthy. No proprietary
provider installation or unnecessary binary decompilation was performed; all V5 source
was available directly. Global agent configuration was not silently rewritten.

Final APK analysis and any actual Android observations must be tied to that artifact's
SHA-256 in TEST_RESULTS.md. Earlier 1027/1028 evidence is not V5 runtime evidence.
