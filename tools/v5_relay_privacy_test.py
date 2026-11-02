#!/usr/bin/env python3
"""Regression guard: the relay may transport bodies but must never capture them."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
RELAY = ROOT / "base/host-android/src/main/kotlin/app/myspoti/host/android/ControlRelay.kt"


CAPTURE_PATTERNS = (
    r"\bkeepAdSample\b|ad-samples",
    r"\bFileOutputStream\b|\.(?:writeText|appendText|writeBytes|appendBytes|bufferedWriter|outputStream)\s*\(",
)


def capture_findings(source):
    return [match.group() for pattern in CAPTURE_PATTERNS for match in re.finditer(pattern, source)]


class RelayPrivacyTest(unittest.TestCase):
    def test_no_raw_request_response_sample_capture_in_any_variant(self):
        self.assertEqual([], capture_findings(RELAY.read_text(encoding="utf-8")))

    def test_guard_detects_capture_even_when_debug_only(self):
        captured = 'if (debuggable) File(cacheDir, "ad-samples").writeText(received)'
        self.assertEqual(["ad-samples", ".writeText("], capture_findings(captured))


if __name__ == "__main__":
    unittest.main()
