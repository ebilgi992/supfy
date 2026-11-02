#!/usr/bin/env python3
"""Run LookTest against the actual pure enum, without Gradle or Android dependencies.

This extracts Look verbatim from LookPref.kt. It tests enum fallback behavior only;
it does not compile the Android/Compose LookPref object or replace an app build.
"""
from pathlib import Path
import subprocess
import tempfile

BASE = Path(__file__).resolve().parents[1] / "base"
CACHE = Path("/workspace/.gradle/caches/modules-2/files-2.1")


def cached(pattern):
    matches = sorted(CACHE.glob(pattern))
    if not matches:
        raise FileNotFoundError(f"Missing cached JVM dependency: {pattern}")
    return matches[0]


def main():
    compiler = [cached(pattern) for pattern in (
        "org.jetbrains.kotlin/kotlin-compiler-embeddable/2.1.0/*/*.jar",
        "org.jetbrains.kotlin/kotlin-stdlib/2.1.0/*/*.jar",
        "org.jetbrains.kotlin/kotlin-script-runtime/2.1.0/*/*.jar",
        "org.jetbrains.kotlin/kotlin-reflect/2.0.21/*/*.jar",
        "org.jetbrains.intellij.deps/trove4j/*/*/*.jar",
        "org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.9.0/*/*.jar",
        "org.jetbrains/annotations/*/*/*.jar",
    )]
    junit = cached("junit/junit/4.13.2/*/*.jar")
    hamcrest = cached("org.hamcrest/hamcrest-core/1.3/*/*.jar")
    with tempfile.TemporaryDirectory(prefix="v5-look-test-") as temp:
        work = Path(temp)
        source = (BASE / "app/src/main/java/com/music/spotui/data/preferences/LookPref.kt").read_text()
        enum = source[source.index("enum class Look {"):source.index("\nobject LookPref")]
        (work / "Look.kt").write_text("package com.music.spotui.data.preferences\n" + enum)
        cp = ":".join(map(str, [compiler[1], compiler[-1], junit, hamcrest]))
        subprocess.run([
            "java", "-cp", ":".join(map(str, compiler)),
            "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", "-no-stdlib", "-no-reflect",
            "-classpath", cp, "-d", str(work / "classes"), str(work / "Look.kt"),
            str(BASE / "app/src/test/java/com/music/spotui/data/preferences/LookTest.kt"),
        ], check=True)
        subprocess.run([
            "java", "-cp", str(work / "classes") + ":" + cp,
            "org.junit.runner.JUnitCore", "com.music.spotui.data.preferences.LookTest",
        ], check=True)


if __name__ == "__main__":
    main()
