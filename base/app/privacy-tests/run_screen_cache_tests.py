#!/usr/bin/env python3
"""Run real ScreenCache persistence logic with primitive model/system-boundary fixtures."""
from pathlib import Path
import os
import subprocess
import tempfile

base = Path(__file__).resolve().parents[2]
fixtures = Path(__file__).resolve().parent
gradle_home = Path(os.environ.get("GRADLE_USER_HOME", "/workspace/.gradle"))
compiler = next(gradle_home.glob("wrapper/dists/gradle-9.8.2-bin/*/gradle-9.8.2/lib/kotlin-compiler-embeddable-*.jar"))
stdlib = next(compiler.parent.glob("kotlin-stdlib-*.jar"))
core = next(compiler.parent.glob("kotlinx-serialization-core-jvm-*.jar"))
json = next(compiler.parent.glob("kotlinx-serialization-json-jvm-*.jar"))
coroutines = next(compiler.parent.glob("kotlinx-coroutines-core-jvm-*.jar"))
sources = [fixtures / name for name in ["AndroidContextFixture.kt", "AtomicFileFixture.kt", "GraphFixture.kt", "ScreenCacheModelsFixture.kt", "ScreenCachePrivacyCanary.kt"]]
sources.append(base / "app/src/main/java/com/music/spotui/data/api/ScreenCache.kt")
with tempfile.TemporaryDirectory(prefix="v5-screen-cache-tests-") as temporary:
    classes = Path(temporary) / "classes"
    classpath = ":".join(map(str, [stdlib, core, json, coroutines]))
    subprocess.run(["java", "-cp", str(compiler.parent / "*"), "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
                    "-no-stdlib", "-no-reflect", "-classpath", classpath, "-d", str(classes), *map(str, sources)], check=True)
    subprocess.run(["java", "-cp", f"{classes}:{classpath}", "privacy.ScreenCachePrivacyCanaryKt"], check=True)
