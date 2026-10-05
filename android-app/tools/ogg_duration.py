# 读取 Ogg Vorbis 时长：最后一个 OggS 页的 granule position / Vorbis ID 头中的采样率。
import struct
import sys
from pathlib import Path

def ogg_duration(path: Path):
    data = path.read_bytes()
    # Vorbis ID header: \x01vorbis ... sample_rate at offset after packet_type(1)+'vorbis'(6)+version(4)+channels(1)
    idx = data.find(b"\x01vorbis")
    if idx < 0:
        return None
    sample_rate = struct.unpack_from("<I", data, idx + 12)[0]
    # 找最后一个 'OggS' 页头
    pos = data.rfind(b"OggS")
    if pos < 0:
        return None
    granule = struct.unpack_from("<q", data, pos + 6)[0]
    return granule / sample_rate if sample_rate else None

audio_dir = Path(sys.argv[1])
rows = []
for f in sorted(audio_dir.glob("*.ogg")):
    dur = ogg_duration(f)
    rows.append((f.name, dur, f.stat().st_size))

rows.sort(key=lambda r: -(r[1] or 0))
for name, dur, size in rows:
    print(f"{name:12s} {dur if dur else 0:6.2f}s  {size // 1024}KB")
