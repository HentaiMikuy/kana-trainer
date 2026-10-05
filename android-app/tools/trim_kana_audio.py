#!/usr/bin/env python3
"""Keep the middle pronunciation in three-repeat recordings.

Requires numpy and soundfile. Writes to a separate, new directory for review:
    python trim_kana_audio.py SOURCE OUTPUT
Single-pronunciation recordings are copied byte for byte. Unexpected speech
group counts abort processing; no source file is overwritten.
"""

import argparse
import hashlib
import json
from pathlib import Path
import shutil

import numpy as np
import soundfile as sf


def speech_groups(samples, rate):
    mono = samples.mean(axis=1)
    hop = max(1, round(rate * 0.01))
    rms = np.array([
        np.sqrt(np.mean(mono[i:i + hop] ** 2))
        for i in range(0, len(mono), hop)
    ])
    active = np.flatnonzero(rms > max(0.003, float(rms.max()) * 0.035))
    groups = np.split(active, np.flatnonzero(np.diff(active) > 20) + 1)
    # Ignore isolated clicks shorter than 50 ms, e.g. at the end of ko.ogg.
    return [
        (int(g[0]) * hop / rate, (int(g[-1]) + 1) * hop / rate)
        for g in groups if len(g) >= 5
    ]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    sources = sorted(args.source.glob('*.ogg'))
    if not sources:
        parser.error('No Ogg recordings found')
    args.output.mkdir(parents=True, exist_ok=False)
    report = []
    for source in sources:
        samples, rate = sf.read(source, always_2d=True)
        groups = speech_groups(samples, rate)
        duration = len(samples) / rate
        target = args.output / source.name
        if len(groups) == 1 and duration < 1:
            shutil.copy2(source, target)
            start, end = 0, duration
            action = 'unchanged'
        elif len(groups) == 3:
            # The middle take avoids potentially truncated first/last takes.
            # Generous padding preserves quiet consonants and vowel tails.
            start = groups[1][0] - 0.20
            end = groups[1][1] + 0.18
            if start <= groups[0][1] + 0.10 or end >= groups[2][0] - 0.10:
                raise ValueError(f'{source.name}: insufficient separating silence')
            clip = samples[round(start * rate):round(end * rate)]
            # Tiny fades affect only the outer silence and prevent cut clicks.
            fade = max(1, round(rate * 0.005))
            clip = clip.copy()
            clip[:fade] *= np.linspace(0, 1, fade)[:, None]
            clip[-fade:] *= np.linspace(1, 0, fade)[:, None]
            sf.write(target, clip, rate, format='OGG', subtype='VORBIS',
                     compression_level=0.2)
            action = 'trimmed'
        else:
            raise ValueError(f'{source.name}: unexpected speech groups {groups}')
        decoded, new_rate = sf.read(target, always_2d=True)
        if new_rate != rate or decoded.shape[1] != samples.shape[1]:
            raise ValueError(f'{source.name}: audio format changed')
        if len(speech_groups(decoded, new_rate)) != 1:
            raise ValueError(f'{source.name}: output is not a single speech group')
        report.append(dict(
            file=source.name, action=action, sample_rate=rate,
            original_seconds=duration, start_seconds=start, end_seconds=end,
            output_seconds=len(decoded) / rate,
            original_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
            output_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),
        ))
    (args.output / 'trim-report.json').write_text(
        json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(f'Validated {len(report)} files: '
          f'{sum(r["action"] == "trimmed" for r in report)} trimmed')


if __name__ == '__main__':
    main()
