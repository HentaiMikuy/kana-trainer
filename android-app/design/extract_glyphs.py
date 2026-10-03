# -*- coding: utf-8 -*-
"""
从真实字体中抽取假名字形轮廓，生成 SVG path 与预览。
不再手画笔画 —— 字形完全来自字体本身，保证是标准写法。
"""
import json
import os
import sys
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen

FONT_DIR = os.path.join(os.environ["WINDIR"], "Fonts")
OUT_DIR = os.path.dirname(os.path.abspath(__file__))

# (显示名, 字体文件, ttc 内索引, 说明)
FONTS = [
    ("UD Digi Kyokasho", "UDDigiKyokashoN-R.ttc", 0, "教科书楷书体，笔画带起收笔，最像手写示范"),
    ("BIZ UD Mincho", "BIZ-UDMinchoM.ttc", 0, "明朝体，横细竖粗、有顿笔，端正"),
    ("Yu Mincho", "yumin.ttf", 0, "游明朝，柔和一些的明朝体"),
    ("Noto Serif JP", "NotoSerifJP-VF.ttf", 0, "思源宋体，笔画对比强、结构清楚"),
    ("LXGW WenKai", "LXGWWenKai-Regular.ttf", 0, "霞鹜文楷，楷体手写风格，圆润"),
    ("Yu Gothic", "YuGothR.ttc", 0, "游黑体，无衬线，笔画均匀"),
]

CHARS = ["あ", "ア"]

# 目标：自适应图标 108 画布，安全核心区 66×66 -> 留边后按 62 单元归一化
SAFE = 62.0
CENTER = 54.0


def glyph_path(font_path, face_index, ch):
    """返回 (SVG path, 字形 bbox)。缺失字形返回 None。"""
    font = TTFont(font_path, fontNumber=face_index, lazy=True)
    try:
        cmap = font.getBestCmap()
        if ord(ch) not in cmap:
            return None
        name = cmap[ord(ch)]
        gs = font.getGlyphSet()
        pen = SVGPathPen(gs, ntos=lambda v: f"{v:.2f}")
        gs[name].draw(pen)
        d = pen.getCommands()
        if not d.strip():
            return None
        # 用控制点算包围盒（对笔画来说足够，且避免依赖 boundsPen）
        from fontTools.pens.boundsPen import BoundsPen
        bp = BoundsPen(gs)
        gs[name].draw(bp)
        if bp.bounds is None:
            return None
        return d, bp.bounds
    finally:
        font.close()


def normalize(d, bounds):
    """把字形按比例缩放并居中到安全区，翻转 y 轴（字体 y 向上，SVG y 向下）。"""
    x0, y0, x1, y1 = bounds
    w, h = x1 - x0, y1 - y0
    scale = SAFE / max(w, h)
    # 居中：字形中心对齐画布中心
    tx = CENTER - (x0 + w / 2) * scale
    ty = CENTER + (y0 + h / 2) * scale
    return f"translate({tx:.3f},{ty:.3f}) scale({scale:.5f},{-scale:.5f})", scale


results = []
skipped = []
for label, fname, idx, note in FONTS:
    path = os.path.join(FONT_DIR, fname)
    if not os.path.exists(path):
        skipped.append(f"字体不存在: {fname}")
        continue
    for ch in CHARS:
        got = glyph_path(path, idx, ch)
        if got is None:
            skipped.append(f"无字形: {fname} / {ch}")
            continue
        d, bounds = got
        transform, scale = normalize(d, bounds)
        results.append({
            "label": label, "file": fname, "char": ch, "note": note,
            "d": d, "transform": transform, "scale": scale,
            "bounds": [round(v, 1) for v in bounds],
            "size": [round(bounds[2] - bounds[0], 1), round(bounds[3] - bounds[1], 1)],
        })

# 进度写到 stderr，stdout 只输出 JSON，便于重定向落盘
for s in skipped:
    print("跳过 " + s, file=sys.stderr)
for r in results:
    print(f"OK {r['label']:18s} {r['char']}  bbox={r['bounds']}  scale={r['scale']:.4f}  path长度={len(r['d'])}", file=sys.stderr)
print(f"共 {len(results)} 个字形", file=sys.stderr)

print(json.dumps(results, ensure_ascii=False, separators=(",", ":")))
