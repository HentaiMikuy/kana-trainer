#!/usr/bin/env python3
"""Create immutable APK + update.json assets from AGP's actual APK metadata."""
import argparse
import hashlib
import json
import re
import shutil
from datetime import datetime, timezone
from pathlib import Path


def prepare(apk, metadata, repository, min_sdk, notes, output):
    data = json.loads(metadata.read_text(encoding="utf-8"))
    elements = data["elements"]
    if len(elements) != 1 or elements[0].get("filters"):
        raise ValueError("Only a single universal APK is supported")
    item = elements[0]
    code, name = item["versionCode"], item["versionName"]
    if type(code) is not int or not 1 <= code <= 2_100_000_000:
        raise ValueError("Invalid versionCode")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9.-]{0,79}", name):
        raise ValueError("Unsafe versionName")
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        raise ValueError("Invalid repository")
    if data["applicationId"] != "com.konomip.kanatrainer":
        raise ValueError("Unexpected applicationId")
    if not 1 <= min_sdk <= 1000 or len(notes) > 32_000:
        raise ValueError("Invalid minSdk or oversized notes")
    size = apk.stat().st_size
    if not 0 < size <= 200 * 1024 * 1024:
        raise ValueError("APK size is outside updater limits")
    tag = f"android-{code}"
    filename = f"kana-trainer-{name}-{code}.apk"
    manifest = dict(schemaVersion=1, applicationId=data["applicationId"],
                    versionCode=code, versionName=name, minSdk=min_sdk,
                    apkUrl=f"https://github.com/{repository}/releases/download/{tag}/{filename}",
                    sizeBytes=size, sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
                    notes=notes, publishedAt=datetime.now(timezone.utc).isoformat())
    output.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(apk, output / filename)
    (output / "update.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--metadata", type=Path, required=True)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--min-sdk", type=int, required=True)
    parser.add_argument("--notes", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    prepare(args.apk, args.metadata, args.repository, args.min_sdk,
            args.notes.read_text(encoding="utf-8"), args.output)
