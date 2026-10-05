# 调试：打印 ogg 文件的页序列详情
import struct
import sys
from pathlib import Path

data = Path(sys.argv[1]).read_bytes()
pos = 0
page_no = 0
while True:
    idx = data.find(b"OggS", pos)
    if idx < 0 or idx + 27 > len(data):
        break
    header_type = data[idx + 5]
    granule = struct.unpack_from("<q", data, idx + 6)[0]
    serial = struct.unpack_from("<I", data, idx + 14)[0]
    seq = struct.unpack_from("<I", data, idx + 18)[0]
    nseg = data[idx + 26]
    lacing = data[idx + 27: idx + 27 + nseg]
    body_len = sum(lacing)
    # 包划分
    packets = []
    cur = 0
    for lace in lacing:
        cur += lace
        if lace < 255:
            packets.append(cur)
            cur = 0
    unfinished = cur  # >0 表示末包延续到下一页
    print(f"page {page_no:2d} seq={seq:2d} type={header_type} granule={granule:7d} nseg={nseg:3d} "
          f"body={body_len:6d} pkts={len(packets):3d} unfinished_tail={unfinished} "
          f"first_pkts={packets[:6]}")
    pos = idx + 27 + nseg + body_len
    page_no += 1
    if page_no > 40:
        print("...")
        break
