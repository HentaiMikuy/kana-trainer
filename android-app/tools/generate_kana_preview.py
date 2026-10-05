#!/usr/bin/env python3
"""Generate isolated Japanese kana samples via Edge online TTS (edge-tts).

Usage: python generate_kana_preview.py OUTPUT_DIRECTORY
Requires network access; sends only the kana strings below to the service.
Preview assets only: pronunciation must be reviewed before use in the app.
"""
import argparse
import asyncio
import json
from pathlib import Path
from urllib.request import getproxies

import edge_tts

SAMPLES = [
    ("a", "あ"), ("i", "い"), ("u", "う"), ("shi", "し"),
    ("chi", "ち"), ("tsu", "つ"), ("fu", "ふ"), ("n", "ん"),
    ("kya", "きゃ"), ("ryu", "りゅ"),
]
VOICES = {"nanami": "ja-JP-NanamiNeural", "keita": "ja-JP-KeitaNeural"}


async def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    proxy = getproxies().get("https")
    manifest = []
    for name, voice in VOICES.items():
        directory = args.output / name
        directory.mkdir(exist_ok=True)
        for key, kana in SAMPLES:
            target = directory / f"{key}.mp3"
            # Do not add carrier phrases: files must contain the kana alone.
            await edge_tts.Communicate(
                kana, voice, rate="-10%", proxy=proxy,
                connect_timeout=15, receive_timeout=30,
            ).save(str(target))
            if target.stat().st_size == 0:
                raise RuntimeError(f"Empty audio: {target}")
            manifest.append(dict(kana=kana, romaji=key, voice=voice,
                                 rate="-10%", file=f"{name}/{key}.mp3"))
            print(f"Generated {name}/{key}.mp3", flush=True)
    (args.output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    asyncio.run(main())
