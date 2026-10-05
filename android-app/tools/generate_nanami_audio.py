#!/usr/bin/env python3
"""Build staged Nanami audio from KanaData.kt; never overwrite app assets.

Dependencies: edge-tts, numpy, soundfile. Uses the approved preview parameters.
Run from any directory: python generate_nanami_audio.py OUTPUT_DIRECTORY
Existing raw files are reused to resume an interrupted generation.
"""
import argparse
import asyncio
import hashlib
import json
from pathlib import Path
import re
import shutil
from urllib.request import getproxies

import edge_tts
import numpy as np
import soundfile as sf

ANDROID = Path(__file__).resolve().parents[1]
VOICE = "ja-JP-NanamiNeural"
RATE = "-10%"
# Read は and へ as kana rather than particles; を uses modern standard /o/.
# Sentence-final punctuation gives い a shorter, finished utterance.
TEXT_OVERRIDES = {"i": "い。", "ha": "ハ", "he": "ヘ", "wo": "オ"}


def process(raw, target):
    samples, rate = sf.read(raw, always_2d=True)
    if not len(samples) or not np.isfinite(samples).all():
        raise ValueError(f"Invalid audio: {raw}")
    hop = round(rate * .01)
    rms = np.array([np.sqrt(np.mean(samples[i:i + hop] ** 2))
                    for i in range(0, len(samples), hop)])
    active = np.flatnonzero(rms > max(.00005, rms.max() * .03))
    if not len(active):
        raise ValueError(f"Silent audio: {raw}")
    start = max(0, int(active[0] * hop) - round(rate * .10))
    end = min(len(samples), int((active[-1] + 1) * hop) + round(rate * .16))
    clip = samples[start:end].copy()
    duration = len(clip) / rate
    if not .2 <= duration <= 1.5:
        raise ValueError(f"Unexpected duration ({duration:.2f}s): {raw}")
    gain = min(.12 / np.sqrt(np.mean(rms[active] ** 2)),
               .85 / np.max(np.abs(clip)))
    clip *= gain
    fade = round(rate * .005)
    clip[:fade] *= np.linspace(0, 1, fade)[:, None]
    clip[-fade:] *= np.linspace(1, 0, fade)[:, None]
    sf.write(target, clip, rate, format="OGG", subtype="VORBIS", compression_level=.2)
    decoded, decoded_rate = sf.read(target, always_2d=True)
    if decoded_rate != rate or decoded.shape != clip.shape:
        raise ValueError(f"Encoding mismatch: {target}")
    if not np.isfinite(decoded).all() or np.max(np.abs(decoded)) >= 1:
        raise ValueError(f"Invalid or clipped output: {target}")
    return dict(seconds=duration, sample_rate=rate, gain_db=float(20 * np.log10(gain)),
                start_sample=start, end_sample=end,
                sha256=hashlib.sha256(target.read_bytes()).hexdigest())


async def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    source = ANDROID / "app/src/main/java/com/konomip/kanatrainer/data/KanaData.kt"
    items = re.findall(r'RawKana\("([^"]+)", "([^"]+)", "([^"]+)"',
                       source.read_text(encoding="utf-8"))
    if len(items) != 104:
        raise ValueError("KanaData changed: review the extraction before generating")
    texts = {}
    for romaji, hiragana, _ in items:
        texts.setdefault(romaji.split("/")[0], hiragana)
    assert len(texts) == 102  # じ/ぢ and ず/づ share modern pronunciations.
    raw_dir, ogg_dir = args.output / "raw", args.output / "kana"
    raw_dir.mkdir(parents=True, exist_ok=True)
    ogg_dir.mkdir(parents=True, exist_ok=True)
    preview = ANDROID / "audio-preview/tts-20261005/nanami"
    manifest = []
    proxy = getproxies().get("https")
    for key, hiragana in texts.items():
        text = TEXT_OVERRIDES.get(key, hiragana)
        # Separate overrides from older cached raw files and approved previews.
        raw = raw_dir / (f"{key}-override.mp3" if key in TEXT_OVERRIDES else f"{key}.mp3")
        if text == hiragana and not raw.exists() and (preview / raw.name).exists():
            shutil.copyfile(preview / raw.name, raw)
        if not raw.exists():
            temporary = raw.with_suffix(".part")
            for attempt in range(3):
                try:
                    await edge_tts.Communicate(
                        text, VOICE, rate=RATE, proxy=proxy,
                        connect_timeout=15, receive_timeout=30,
                    ).save(str(temporary))
                    sf.read(temporary)  # Do not cache incomplete responses.
                    temporary.replace(raw)
                    break
                except Exception:
                    if attempt == 2:
                        raise
                    await asyncio.sleep(2)
        result = process(raw, ogg_dir / f"{key}.ogg")
        manifest.append(dict(key=key, kana=hiragana, input_text=text,
                             voice=VOICE, rate=RATE, **result))
        print(f"{len(manifest)}/{len(texts)} {key}: {result['seconds']:.2f}s", flush=True)
    (args.output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    asyncio.run(main())
