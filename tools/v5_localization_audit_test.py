"""Regression checks for localization failures that can break Android UI resources."""
from pathlib import Path
import tempfile
import unittest

from v5_localization_audit import audit


class LocalizationAuditTest(unittest.TestCase):
    def fixture(self, default, turkish):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        root = Path(directory.name)
        for language, body in (("values", default), ("values-tr", turkish)):
            (root / language).mkdir()
            (root / language / "strings.xml").write_text(
                f"<resources>{body}</resources>", encoding="utf-8"
            )
        return root

    def test_ignores_nontranslatable_resource_but_reports_missing_ui_label(self):
        root = self.fixture(
            '<string name="secret" translatable="false">api</string>'
            '<string name="play">Play</string>',
            '',
        )
        _, _, errors = audit(root)
        self.assertEqual(["missing string/play (strings.xml)"], errors)

    def test_reordered_numbered_placeholders_are_valid(self):
        root = self.fixture(
            '<string name="count">%1$d songs by %2$s</string>',
            '<string name="count">%2$s sanatçısından %1$d şarkı</string>',
        )
        self.assertEqual([], audit(root)[2])

    def test_missing_placeholder_is_reported(self):
        root = self.fixture(
            '<string name="count">%1$d songs by %2$s</string>',
            '<string name="count">%1$d şarkı</string>',
        )
        self.assertIn("placeholder mismatch count", audit(root)[2])

    def test_plural_quantity_and_format_changes_are_reported(self):
        root = self.fixture(
            '<plurals name="tracks"><item quantity="one">%d track</item>'
            '<item quantity="other">%d tracks</item></plurals>',
            '<plurals name="tracks"><item quantity="other">%s parça</item></plurals>',
        )
        errors = audit(root)[2]
        self.assertIn("plural quantities differ tracks", errors)
        self.assertIn("placeholder mismatch tracks/other", errors)

    def test_array_cardinality_and_format_are_checked(self):
        root = self.fixture(
            '<string-array name="options"><item>%d songs</item><item>Off</item></string-array>',
            '<string-array name="options"><item>%s şarkı</item></string-array>',
        )
        errors = audit(root)[2]
        self.assertIn("array length mismatch options", errors)
        self.assertIn("placeholder mismatch options/0", errors)

    def test_actual_turkish_resources_cover_default_ui_resources(self):
        default, turkish, errors = audit()
        self.assertGreater(len(default), 1200)
        self.assertGreaterEqual(len(turkish), len(default))
        self.assertEqual([], errors)


if __name__ == "__main__":
    unittest.main()
