#!/usr/bin/env python3
"""Offline V5 regression guards. Source guards are not a penetration test.

Run: python3 tools/v5_security_audit.py --self-test
     python3 tools/v5_security_audit.py --apk FILE --apkanalyzer SDK/bin/apkanalyzer
The default source tree is base/ (the native V5 engine), not the retired wrapper.
No network, Gradle, ADB, credentials, or real user data are used. Findings print
locations and rule names only; never source strings, headers, or token values.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
MODULES = ("app", "host-android", "api-spotify", "api", "extensions")
EXPORTS = {
    "app.myspoti.MainActivity",
    "com.music.spotui.di.ArtworkProvider",
    "com.music.spotui.cli.CliReceiver",
    "androidx.media.session.MediaButtonReceiver",
    "app.myspoti.host.android.PlaybackService",
    # AndroidX profile installer is protected by the signature-level DUMP permission.
    "androidx.profileinstaller.ProfileInstallReceiver",
}
APP = "app/src/main/java/com/music/spotui/"
HOST = "host-android/src/main/kotlin/app/myspoti/host/android/"


def without_comments(source: str) -> str:
    # Preserve literals (including Kotlin raw JS strings): comment-like URL text
    # must not eat code that follows it. Preserve newlines for evidence locations.
    pattern = r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/'
    return re.sub(pattern, lambda m: re.sub(r"[^\n]", " ", m[0])
                  if m[0].startswith(("//", "/*")) else m[0], source)


def load_sources(root: Path) -> dict[str, str]:
    sources = {}
    for module in MODULES:
        for path in (root / module / "src/main").rglob("*"):
            if path.is_file() and path.suffix in {".kt", ".java", ".xml", ".js"}:
                sources[path.relative_to(root).as_posix()] = path.read_text(errors="replace")
    sources["app/build.gradle.kts"] = (root / "app/build.gradle.kts").read_text()
    return sources


def manifest_findings(source: str, path: str, packaged: bool = False) -> list[dict]:
    findings = []
    def fail(rule, detail):
        findings.append({"rule": rule, "path": path, "line": 1, "detail": detail})
    try:
        tree = ET.fromstring(source)
    except ET.ParseError:
        fail("manifest-parse", "manifest is not parseable XML")
        return findings
    app = tree.find("application")
    if app is None:
        return findings
    if packaged or path == "app/src/main/AndroidManifest.xml":
        if app.get(ANDROID + "allowBackup") != "false":
            fail("backup-disabled", "allowBackup must be explicitly false")
        if app.get(ANDROID + "usesCleartextTraffic") != "false":
            fail("cleartext-disabled", "usesCleartextTraffic must be explicitly false")
    if packaged:
        if tree.get("package") != "com.supfy.app":
            fail("apk-package", "APK package must be com.supfy.app")
        if app.get(ANDROID + "debuggable", "false") != "false":
            fail("apk-release-debuggable", "release APK must not be debuggable")
        if tree.find("uses-permission[@" + ANDROID + "name='android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION']") is not None:
            fail("update-user-action", "silent update permission must be absent")
    for component in app:
        name = component.get(ANDROID + "name", "")
        if component.get(ANDROID + "exported") != "true":
            continue
        # Reviewed compatibility alias: only the existing public activity, never a new receiver/service.
        legacy_alias = (component.tag == "activity-alias" and name == "com.supfy.app.MainActivity"
                        and component.get(ANDROID + "targetActivity") == "app.myspoti.MainActivity"
                        and component.find("intent-filter") is None)
        if name not in EXPORTS and not legacy_alias:
            fail("unexpected-export", "exported component requires explicit review: " + name)
        if name in {"com.music.spotui.cli.CliReceiver", "androidx.profileinstaller.ProfileInstallReceiver"}:
            if component.get(ANDROID + "permission") != "android.permission.DUMP":
                fail("export-permission", "debug/profiler receiver requires DUMP")
        if packaged and name == "com.music.spotui.cli.CliReceiver" and component.get(ANDROID + "enabled", "true") != "false":
            fail("apk-cli-disabled", "release CLI receiver must be disabled")
    return findings


def audit(sources: dict[str, str]) -> list[dict]:
    findings = []
    def fail(rule, path, match=None, detail="required security contract missing"):
        findings.append({"rule": rule, "path": path,
                         "line": sources.get(path, "").count("\n", 0, match.start()) + 1 if match else 1,
                         "detail": detail})
    clean = {p: without_comments(s) for p, s in sources.items()}
    for path, source in clean.items():
        if path.endswith((".kt", ".java")):
            aliases = re.findall(r"import\s+android\.util\.Log(?:\s+as\s+(\w+))?", source)
            names = {alias or "Log" for alias in aliases}
            pattern = r"\bandroid\.util\.Log\s*\.\s*\w+\s*\("
            if names:
                pattern += "|\\b(?:" + "|".join(map(re.escape, names)) + r")\s*\.\s*\w+\s*\("
            for match in re.finditer(pattern, source):
                fail("platform-log-sink", path, match, "production source must not call Android Log")
            for match in re.finditer(r"\b(?:System\.(?:out|err)\.(?:print|println)|printStackTrace)\s*\(", source):
                fail("console-log-sink", path, match, "production console/stacktrace sink")
            for match in re.finditer(r"(?:addJavascriptInterface\s*\(|@JavascriptInterface)", source):
                fail("javascript-interface", path, match, "unscoped JavascriptInterface forbidden")
            for match in re.finditer(r"setWebContentsDebuggingEnabled\s*\(\s*true\s*\)", source):
                # Require explicit boolean false on production initialization as well.
                fail("webview-debug-literal", path, match, "debug state must derive from application FLAG_DEBUGGABLE")
        if path.endswith("AndroidManifest.xml"):
            findings.extend(manifest_findings(sources[path], path))
    build_path = "app/build.gradle.kts"
    build = clean.get(build_path, "")
    if not re.search(r'applicationId\s*=\s*"com\.supfy\.app"', build) or "applicationIdSuffix" in build:
        fail("application-id", build_path, detail="all variants must use com.supfy.app without suffix")
    if "ebilgi992/supfy" not in build or "MYSPOTI_UPDATE_REPO" in build or 'findProperty("updateRepo")' in build:
        fail("update-repository", build_path, detail="updater repository must be fixed to ebilgi992/supfy")
    trust_path = APP + "data/api/UpdateTrustPolicy.kt"
    trust = clean.get(trust_path, "")
    if not re.search(r'const\s+val\s+REPOSITORY\s*=\s*"ebilgi992/supfy"', trust):
        fail("update-repository", trust_path, detail="runtime update trust policy must fix the Supfy repository")
    if not re.search(r'const\s+val\s+PACKAGE\s*=\s*"com\.supfy\.app"', trust):
        fail("update-package-policy", trust_path, detail="runtime package identity must be com.supfy.app")
    host_path = HOST + "WebPlayerHost.kt"
    host = clean.get(host_path, "")
    if "FLAG_DEBUGGABLE" not in host or not re.search(r"setWebContentsDebuggingEnabled\(debuggable\)", host):
        fail("webview-release-debug-state", host_path)
    channel_path = HOST + "PlayerChannel.kt"
    channel = clean.get(channel_path, "")
    for required in ("addWebMessageListener", "PLAYER_ORIGIN", "acceptMessage", "isMainFrame"):
        if required not in channel:
            fail("bridge-origin-frame", channel_path, detail="origin/main-frame listener enforcement missing")
            break
    if "SECURE_MESSAGE_CHANNEL_UNAVAILABLE" not in "\n".join(clean.values()):
        fail("bridge-unsupported-explicit", channel_path)
    # Detect reintroduced storage at the data boundary, not harmless call sites or UI names.
    for suffix, rule, patterns in (
        ("data/local/ListenHistory.kt", "listen-history-retention", (r"\.appendText\s*\(", r"\.writeText\s*\(")),
        ("data/preferences/RecentItemsPref.kt", "search-history-retention", (r"\.putString\s*\(",)),
    ):
        path = APP + suffix
        for pattern in patterns:
            for match in re.finditer(pattern, clean.get(path, "")):
                fail(rule, path, match, "native search/full listening history persistence forbidden")
    for suffix, rule, patterns in (
        (HOST + "Logbook.kt", "diagnostic-storage", (r"\.writeText\s*\(", r"lines\.add", r"facts\[.*\]\s*=")),
        (HOST + "ControlRelay.kt", "diagnostic-storage", (r'"ad-samples"',)),
        (APP + "extensions/ExtensionRegistry.kt", "extension-diagnostic-storage", (r"file\.appendText\s*\(",)),
        (APP + "data/api/AppUpdater.kt", "raw-update-error", (r"State\.Failed\(it\.message", r"getStringExtra\(PackageInstaller\.EXTRA_STATUS_MESSAGE")),
    ):
        for pattern in patterns:
            for match in re.finditer(pattern, clean.get(suffix, "")):
                fail(rule, suffix, match, "private diagnostics or raw remote exception must not be persisted/exposed")
    return findings


def self_test(sources: dict[str, str]) -> list[dict]:
    """Inject regressions into in-memory copies of actual inspected V5 inputs.

    This tests scanner sensitivity without editing another agent's live sources.
    Deliberately failing a mutated input is the success condition.
    """
    cases = [
        ("Android logging", HOST + "Logbook.kt", '\nfun leak() { android.util.Log.e("tag", "CANARY") }\n', "platform-log-sink"),
        ("debug enabled", HOST + "WebPlayerHost.kt", '\nfun debugLeak() { WebView.setWebContentsDebuggingEnabled(true) }\n', "webview-debug-literal"),
        ("legacy JS bridge", HOST + "PlayerChannel.kt", '\nfun legacy() { addJavascriptInterface(obj, "bridge") }\n', "javascript-interface"),
        ("full history", APP + "data/local/ListenHistory.kt", '\nfun persist() { file.appendText("CANARY") }\n', "listen-history-retention"),
        ("search history", APP + "data/preferences/RecentItemsPref.kt", '\nfun persist() { edit.putString("query", "CANARY") }\n', "search-history-retention"),
        ("raw install failure", APP + "data/api/AppUpdater.kt", '\nval failure = State.Failed(it.message)\n', "raw-update-error"),
        ("raw relay capture", HOST + "ControlRelay.kt", '\nval rawCapture = File(cache, "ad-samples")\n', "diagnostic-storage"),
        ("extension diagnostic storage", APP + "extensions/ExtensionRegistry.kt", '\nfun leak() { file.appendText("CANARY") }\n', "extension-diagnostic-storage"),
    ]
    results = []
    for name, path, appended, rule in cases:
        # Check the changed real production file; unrelated source files do not
        # need rescanning for a canary localized to this boundary.
        mutated = {path: sources.get(path, "")}
        mutated[path] = mutated.get(path, "") + appended
        # Existing findings cannot accidentally satisfy a canary's assertion.
        line = mutated[path].count("\n") - appended.count("\n") + 2
        caught = any(f["rule"] == rule and f["path"] == path and f["line"] >= line for f in audit(mutated))
        results.append({"case": name, "status": "PASS" if caught else "FAIL"})
    trust_path = APP + "data/api/UpdateTrustPolicy.kt"
    mutated = {trust_path: sources.get(trust_path, "")}
    mutated[trust_path] = sources.get(trust_path, "").replace("ebilgi992/supfy", "wrong-owner/wrong-repo")
    changed_repo = mutated[trust_path] != sources.get(trust_path, "")
    caught = any(f["rule"] == "update-repository" and f["path"] == trust_path for f in audit(mutated))
    results.append({"case": "wrong update repository", "status": "PASS" if changed_repo and caught else "NOT TESTED",
                    "detail": "requires actual fixed repository literal before mutation"})
    manifest = sources["app/src/main/AndroidManifest.xml"]
    injection = '<service android:name="example.Untrusted" android:exported="true" />'
    changed = manifest.replace("</application>", injection + "</application>")
    caught = any(f["rule"] == "unexpected-export" for f in manifest_findings(changed, "fixture/AndroidManifest.xml"))
    results.append({"case": "unexpected export", "status": "PASS" if caught else "FAIL"})
    alias_mutation = manifest.replace('android:targetActivity="app.myspoti.MainActivity"', 'android:targetActivity="example.OtherActivity"')
    caught_alias = any(f["rule"] == "unexpected-export" for f in manifest_findings(alias_mutation, "fixture/AndroidManifest.xml"))
    results.append({"case": "legacy alias target", "status": "PASS" if alias_mutation != manifest and caught_alias else "FAIL"})
    # Comment-only logging is deliberately not treated as an executable sink.
    path = HOST + "Logbook.kt"
    original = audit({path: sources[path]})
    mutated = {path: sources[path]}
    mutated[HOST + "Logbook.kt"] += '\n// android.util.Log.e("tag", "CANARY")\n'
    results.append({"case": "comment false positive", "status": "PASS" if audit(mutated) == original else "FAIL"})
    return results


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1] / "base")
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--apkanalyzer", default="apkanalyzer")
    args = parser.parse_args()
    sources = load_sources(args.root)
    findings = audit(sources)
    result = {"scope": str(args.root.resolve()), "source_files": len(sources),
              "source_sha256": hashlib.sha256("".join(p + "\0" + s for p, s in sorted(sources.items())).encode()).hexdigest(),
              "source_status": "FAIL" if findings else "PASS", "findings": findings,
              "apk_status": "NOT TESTED", "runtime_status": "NOT TESTED"}
    if args.self_test:
        result["negative_fixture_tests"] = self_test(sources)
    if args.apk:
        result["apk_sha256"] = hashlib.file_digest(args.apk.open("rb"), "sha256").hexdigest()
        try:
            run = subprocess.run([args.apkanalyzer, "manifest", "print", str(args.apk.resolve())],
                                 capture_output=True, text=True, check=True, timeout=120)
            apk_findings = manifest_findings(run.stdout, args.apk.name, packaged=True)
            result["apk_findings"] = apk_findings
            result["apk_status"] = "FAIL" if apk_findings else "PASS"
        except (OSError, subprocess.SubprocessError) as error:
            result["apk_error"] = type(error).__name__
    print(json.dumps(result, indent=2, ensure_ascii=False))
    fixture_failure = any(t["status"] != "PASS" for t in result.get("negative_fixture_tests", []))
    return 1 if findings or fixture_failure or (args.apk and result["apk_status"] != "PASS") else 0


if __name__ == "__main__":
    sys.exit(main())
