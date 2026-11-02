# V5 design changes

Myspoti's existing Immersive and Classic Compose designs are preserved. Immersive is
the unsaved default; Classic remains selectable. No unrelated visual overhaul was added.
Supfy launcher/adaptive icons, app label, welcome wordmark, About title and share-card
wordmark replace the product identity; upstream credits and donation destinations remain.

Both settings layouts include the shared privacy card, background-playback preference
and optional provider controls. One root consent dialog describes providers, data sent,
IP visibility and revocation limits. An unsupported secure WebView displays a Turkish
explanation and update action instead of a permanent loading screen.

The history recap no longer promises to collect plays: its text explains that Supfy
keeps no local listening history and the provider may maintain its own records.
Runtime screenshots/tests, when executed, are listed separately in TEST_RESULTS.md.
