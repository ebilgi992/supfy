#!/usr/bin/env python3
"""Check complete Turkish Android resource coverage and formatting compatibility."""
from pathlib import Path
import collections
import re
import sys
import xml.etree.ElementTree as ET

RESOURCE_DIR = Path(__file__).resolve().parents[1] / "base/app/src/main/res"
FORMATS = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")
KINDS = {"string", "plurals", "string-array"}


def resources(directory):
    result = {}
    for path in sorted(directory.glob("*.xml")):
        for element in ET.parse(path).getroot():
            if element.tag not in KINDS or element.get("translatable") == "false":
                continue
            key = (element.tag, element.attrib["name"])
            if key in result:
                raise ValueError(f"duplicate resource {key} in {path}")
            result[key] = (path, element)
    return result


def text(element):
    return "".join(element.itertext())


def signature(element):
    return collections.Counter(FORMATS.findall(text(element)))


def audit(root=RESOURCE_DIR):
    default, turkish = resources(root / "values"), resources(root / "values-tr")
    errors = []
    for key, (path, source) in default.items():
        translated = turkish.get(key)
        if translated is None:
            errors.append(f"missing {key[0]}/{key[1]} ({path.name})")
            continue
        _, target = translated
        if not text(target).strip():
            errors.append(f"empty {key[0]}/{key[1]}")
        if source.tag == "string":
            if signature(source) != signature(target):
                errors.append(f"placeholder mismatch {key[1]}")
        elif source.tag == "plurals":
            src = {x.attrib["quantity"]: x for x in source}
            dst = {x.attrib["quantity"]: x for x in target}
            if src.keys() != dst.keys():
                errors.append(f"plural quantities differ {key[1]}")
            for quantity in src.keys() & dst.keys():
                if signature(src[quantity]) != signature(dst[quantity]):
                    errors.append(f"placeholder mismatch {key[1]}/{quantity}")
        else:
            if len(source) != len(target):
                errors.append(f"array length mismatch {key[1]}")
            for i, (src, dst) in enumerate(zip(source, target)):
                if signature(src) != signature(dst):
                    errors.append(f"placeholder mismatch {key[1]}/{i}")
    return default, turkish, errors


if __name__ == "__main__":
    default, turkish, errors = audit()
    for error in errors:
        print(error)
    print(f"{len(default)} default resources; {len(turkish)} Turkish resources; {len(errors)} errors")
    sys.exit(bool(errors))
