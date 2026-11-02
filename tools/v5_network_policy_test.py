#!/usr/bin/env python3
"""Compile actual pure updater/consent policy Kotlin and run JUnit without Gradle.

ConsentRevision is extracted verbatim from ConsentPolicy because the storage adapter
requires Android. This does not replace Android build or network/installer testing.
"""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "base"
CACHE = Path("/workspace/.gradle/caches/modules-2/files-2.1")

def cached(pattern):
    matches = sorted(CACHE.glob(pattern))
    if not matches:
        raise FileNotFoundError(pattern)
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
    with tempfile.TemporaryDirectory(prefix="v5-network-policy-") as temp:
        work = Path(temp)
        text = (BASE / "app/src/main/java/com/music/spotui/privacy/ConsentPolicy.kt").read_text()
        revision = text[text.index("object ConsentRevision {"):text.index("\nobject ConsentPolicy {")]
        (work / "ConsentRevision.kt").write_text("package com.music.spotui.privacy\n" + revision)
        cp = ":".join(map(str, [compiler[1], compiler[-1], compiler[-2], junit, hamcrest]))
        sources = [work / "ConsentRevision.kt", BASE / "app/src/main/java/com/music/spotui/privacy/ConsentQueue.kt", BASE / "app/src/test/java/com/music/spotui/privacy/ConsentQueueTest.kt", BASE / "app/src/main/java/com/music/spotui/data/api/UpdateTrustPolicy.kt",
                   BASE / "app/src/test/java/com/music/spotui/privacy/ConsentRevisionTest.kt",
                   BASE / "app/src/test/java/com/music/spotui/data/api/UpdateTrustPolicyTest.kt"]
        subprocess.run(["java", "-cp", ":".join(map(str, compiler)), "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
                        "-no-stdlib", "-no-reflect", "-classpath", cp, "-d", str(work / "classes"), *map(str, sources)], check=True)
        subprocess.run(["java", "-cp", str(work / "classes") + ":" + cp, "org.junit.runner.JUnitCore",
                        "com.music.spotui.privacy.ConsentRevisionTest", "com.music.spotui.privacy.ConsentQueueTest", "com.music.spotui.data.api.UpdateTrustPolicyTest"], check=True)

if __name__ == "__main__":
    main()
