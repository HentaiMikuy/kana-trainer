# 解析 Ogg 页结构：列出每个音频包的时间位置（由页 granule 插值）与字节大小，
# 用于观察录音是否包含多次重复发音（重复之间的大段静音对应极小的包）。
import struct
import sys
from pathlib import Path


def parse_pages(data: bytes):
    """返回 [(page_start, granule, seq, packet_sizes_in_page, is_header_end)]"""
    pages = []
    pos = 0
    while True:
        idx = data.find(b"OggS", pos)
        if idx < 0:
            break
        if idx + 27 > len(data):
            break
        version = data[idx + 4]
        header_type = data[idx + 5]
        granule = struct.unpack_from("<q", data, idx + 6)[0]
        seq = struct.unpack_from("<I", data, idx + 18)[0]
        nseg = data[idx + 26]
        lacing = data[idx + 27: idx + 27 + nseg]
        body_start = idx + 27 + nseg
        # 按 lacing 切分包：连续 255 表示包跨段延续
        packets = []
        cur = 0
        for lace in lacing:
            cur += lace
            if lace < 255:
                packets.append(cur)
                cur = 0
        body_len = sum(lacing)
        pages.append({
            "start": idx,
            "granule": granule,
            "seq": seq,
            "packets": packets,
            "header_type": header_type,
            "body_len": body_len,
        })
        pos = body_start + body_len
    return pages


def analyze(path: Path):
    data = path.read_bytes()
    pages = parse_pages(data)
    # Vorbis 前 3 个包是头（identification/comment/setup），各自独占页
    sample_rate = None
    idx = data.find(b"\x01vorbis")
    if idx >= 0:
        sample_rate = struct.unpack_from("<I", data, idx + 12)[0]

    # 收集音频包：从头包之后的页开始
    # 每个页给出本页最后一个包的累计 granule；上一页末尾 granule 用于线性插值
    audio_packets = []  # (approx_time, size, page_seq)
    prev_granule = 0
    header_pages = 0
    for i, p in enumerate(pages):
        if header_pages < 3 and p["header_type"] & 0x02 or header_pages < 3 and i < 3:
            # 前三个页是 Vorbis 头页（按 Vorbis-on-Ogg 规范各头包独占一页）
            header_pages += 1
            prev_granule = 0
            continue
        n = len(p["packets"])
        if n == 0:
            continue
        g_end = p["granule"]
        g_start = prev_granule
        for j, size in enumerate(p["packets"]):
            # 该包结束位置的近似 granule（页内线性插值）
            frac = (j + 1) / n
            g = g_start + (g_end - g_start) * frac
            t = g / sample_rate if sample_rate else 0
            audio_packets.append((t, size, p["seq"]))
        prev_granule = g_end
    return sample_rate, audio_packets


def main():
    for name in sys.argv[2:]:
        path = Path(sys.argv[1]) / name
        sr, packets = analyze(path)
        total = packets[-1][0] if packets else 0
        sizes = [s for _, s, _ in packets]
        med = sorted(sizes)[len(sizes) // 2] if sizes else 1
        print(f"\n=== {name}  rate={sr} packets={len(packets)} total={total:.2f}s median_pkt={med}B ===")
        # 压缩打印：每行 ~0.1s，显示该区间最大包大小（能量代理）
        bucket = 0.1
        line = []
        t0 = 0.0
        for t, s, _ in packets:
            while t > t0 + bucket:
                line.append(max(line) if line else 0)
                t0 += bucket
                line = []
            line.append(s)
        if line:
            line.append(max(line))
        # 用字符画波形
        peak = max(line) if line else 1
        out = []
        for v in line:
            r = v / peak if peak else 0
            out.append("#" if r > 0.5 else ("+" if r > 0.2 else "."))
        print("".join(out))
        print(f"每个字符 ≈ {bucket}s；#=大声 +=中等 .=接近静音")


if __name__ == "__main__":
    main()
