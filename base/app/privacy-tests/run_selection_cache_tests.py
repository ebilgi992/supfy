#!/usr/bin/env python3
"""Exercise real favorite/follow helpers against an in-memory Android preferences boundary."""
from pathlib import Path
import os
import subprocess
import tempfile

base = Path(__file__).resolve().parents[2]
fixtures = Path(__file__).resolve().parent
gradle_home = Path(os.environ.get("GRADLE_USER_HOME", "/workspace/.gradle"))
compiler = next(gradle_home.glob("wrapper/dists/gradle-9.8.2-bin/*/gradle-9.8.2/lib/kotlin-compiler-embeddable-*.jar"))
stdlib = next(compiler.parent.glob("kotlin-stdlib-*.jar"))
sources = [fixtures / name for name in ["AndroidContextFixture.kt", "SelectionModelsFixture.kt", "SelectionCachePrivacyCanary.kt"]]
sources.extend(base / f"app/src/main/java/com/music/spotui/data/preferences/{name}.kt" for name in ["LikedSongPref", "LikedAlbumPref", "FollowedArtistPref"])
with tempfile.TemporaryDirectory(prefix="v5-selection-tests-") as temporary:
    classes = Path(temporary) / "classes"
    subprocess.run(["java", "-cp", str(compiler.parent / "*"), "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
                    "-no-stdlib", "-no-reflect", "-classpath", str(stdlib), "-d", str(classes), *map(str, sources)], check=True)
    subprocess.run(["java", "-cp", f"{classes}:{stdlib}", "privacy.SelectionCachePrivacyCanaryKt"], check=True)
