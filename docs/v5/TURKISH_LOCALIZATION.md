# Turkish localization

`values-tr` contains the full 1,260-key default resource inventory (strings and plurals).
The final offline audit found zero missing or mismatched resource entries. Turkish
characters and format placeholders are checked by `tools/v5_localization_audit.py`;
its negative fixture tests run separately. Android resource compilation also runs in
Gradle, so the inventory check alone cannot hide malformed XML.

The first app locale is tr-TR, including owned notification resources. Both UI looks
use the same translated resources. Proper names, track/artist/album titles, extension
code and third-party sign-in pages are not automatically translated. Myspoti's own
support/donation attribution is intentionally retained.

Resource completeness is a source-level gate. Actual small/large-screen rendering,
font scaling, truncation, TalkBack and provider-rendered language require separate
runtime evidence in TEST_RESULTS.md; they are not implied by the key count.
