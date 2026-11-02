#!/usr/bin/env python3
"""Report AndroidJUnitRunner results; a process crash or incomplete run cannot pass."""
import json
import re
import sys
from pathlib import Path

text = Path(sys.argv[1]).read_text(errors="replace")
block = {}
results = {}
expected = 0
for line in text.splitlines():
    if line.startswith("INSTRUMENTATION_STATUS: "):
        key, _, value = line[len("INSTRUMENTATION_STATUS: "):].partition("=")
        block[key] = value
        if key == "numtests":
            expected = max(expected, int(value))
    elif line.startswith("INSTRUMENTATION_STATUS_CODE:"):
        key = (block.get("class", ""), block.get("test", ""))
        if all(key):
            results[key] = int(line.partition(":")[2])
        block = {}
status_names = {0: "PASS", -1: "ERROR", -2: "FAIL", -3: "SKIP", -4: "SKIP", 1: "INTERRUPTED"}
cases = [{"class": c, "test": t, "status": status_names.get(v, "UNKNOWN")} for (c, t), v in results.items()]
complete = bool(re.search(r"OK \(\d+ tests?\)", text)) and len(cases) == expected and expected > 0
passed = complete and all(c["status"] in ("PASS", "SKIP") for c in cases) and "Process crashed" not in text
print(json.dumps({"status": "PASS" if passed else "FAIL", "expected": expected,
                  "passed": sum(c["status"] == "PASS" for c in cases),
                  "skipped": sum(c["status"] == "SKIP" for c in cases), "cases": cases}, indent=2))
sys.exit(0 if passed else 1)
