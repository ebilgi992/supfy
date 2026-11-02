#!/usr/bin/env python3
"""Compile real history code against a minimal Android boundary and run synthetic canaries."""
from pathlib import Path
import os
import subprocess
import tempfile

base = Path(__file__).resolve().parents[2]
fixtures = Path(__file__).resolve().parent
gradle_home = Path(os.environ.get("GRADLE_USER_HOME", "/workspace/.gradle"))
compiler = next(gradle_home.glob("wrapper/dists/gradle-9.8.2-bin/*/gradle-9.8.2/lib/kotlin-compiler-embeddable-*.jar"))
stdlib = next(compiler.parent.glob("kotlin-stdlib-*.jar"))
android = Path(os.environ.get("ANDROID_HOME", "/workspace/android-sdk")) / "platforms/android-35/android.jar"
serialization = next(compiler.parent.glob("kotlinx-serialization-core-jvm-*.jar"))
sources = [fixtures / "AndroidContextFixture.kt", fixtures / "HistoryPrivacyCanary.kt", fixtures / "ScreenCacheCleanupFixture.kt",
           base / "app/src/main/java/com/music/spotui/data/PrivacyCleanup.kt",
           base / "app/src/main/java/com/music/spotui/data/preferences/LibraryCachePref.kt",
           base / "app/src/main/java/com/music/spotui/data/entity/LibraryEntry.kt",
           base / "app/src/main/java/com/music/spotui/data/entity/ArtistsModel.kt",
           base / "app/src/main/java/com/music/spotui/data/local/ListenHistory.kt",
           base / "app/src/main/java/com/music/spotui/data/preferences/RecentItemsPref.kt"]
with tempfile.TemporaryDirectory(prefix="v5-history-tests-") as temporary:
    classes = Path(temporary) / "classes"
    classpath = f"{stdlib}:{android}:{serialization}"
    subprocess.run(["java", "-cp", str(compiler.parent / "*"),
                    "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", "-no-stdlib", "-no-reflect",
                    "-classpath", classpath, "-d", str(classes), *map(str, sources)], check=True)
    subprocess.run(["java", "-cp", f"{classes}:{classpath}", "privacy.HistoryPrivacyCanaryKt"], check=True)
