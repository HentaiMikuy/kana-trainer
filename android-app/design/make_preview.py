# -*- coding: utf-8 -*-
"""
用真实字体轮廓生成图标候选预览页（自包含 HTML，可直接无头截图）。
字形来自 extract_glyphs.py 的提取结果，不再手画笔画。

用法：python make_preview.py > gallery.html  然后用浏览器或无头截图查看
"""
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PY = sys.executable

# 复用提取脚本的输出
out = subprocess.run([PY, os.path.join(HERE, "extract_glyphs.py")],
                     capture_output=True, text=True, encoding="utf-8")
if out.returncode != 0:
    print(out.stderr, file=sys.stderr)
    raise SystemExit("提取字形失败")
glyphs = json.loads(out.stdout.strip().splitlines()[-1])

RED = "#D94738"
BG = "#F7F5EF"
PAPER = "#FFFDF8"
INK = "#20252B"
LINE = "#E2DED4"
MUTED = "#69727D"

# 每套配色：(背景, 字形填充, 名称)
THEMES = [
    (BG, RED, "米白底·红字"),
    (RED, "#FFFFFF", "红底·白字"),
    (INK, "url(#grad)", "墨底·渐变字"),
]

GRAD_DEF = f'''<linearGradient id="grad" x1="0" y1="0" x2="1" y2="1">
  <stop offset="0%" stop-color="#FFC9B8"/><stop offset="42%" stop-color="#F08A76"/>
  <stop offset="100%" stop-color="#D7A63B"/></linearGradient>'''


_seq = [0]


def icon(g, bg, fill, size, radius):
    """把一个字形渲染成 size×size 的图标方块。渐变 id 需唯一，否则跨 SVG 引用会失效。"""
    _seq[0] += 1
    gid = f"grad{_seq[0]}"
    grad = GRAD_DEF.replace('id="grad"', f'id="{gid}"')
    real_fill = fill.replace("url(#grad)", f"url(#{gid})")
    return f'''<div class="ic" style="width:{size}px;height:{size}px;border-radius:{radius};background:{bg}">
  <svg viewBox="0 0 108 108" width="100%" height="100%"><defs>{grad}</defs>
    <g transform="{g['transform']}"><path d="{g['d']}" fill="{real_fill}"/></g>
  </svg></div>'''


# 按字体分组，保持提取顺序
by_font = {}
for g in glyphs:
    by_font.setdefault((g["label"], g["note"]), {})[g["char"]] = g

cards = []
for (label, note), chars in by_font.items():
    big = "".join(
        f'''<div class="cell"><div class="ttl">{ch}</div>
        {icon(chars[ch], BG, RED, 150, "20px")}
        <div class="sz">150px · 米白底红字</div></div>'''
        for ch in ("あ", "ア") if ch in chars
    )
    small = "".join(
        f'''<div class="cell">{icon(chars[ch], BG, RED, 72, "16px")}
        {icon(chars[ch], RED, "#FFFFFF", 72, "16px")}
        {icon(chars[ch], INK, "url(#grad)", 72, "16px")}
        {icon(chars[ch], BG, RED, 48, "11px")}
        <div class="sz">{ch} · 72dp 三配色 + 48dp</div></div>'''
        for ch in ("あ", "ア") if ch in chars
    )
    cards.append(f'''<section class="card">
  <h2>{label} <code>{chars['あ']['file']}</code></h2>
  <p class="note">{note}</p>
  <div class="row">{big}</div>
  <div class="row wrap">{small}</div>
</section>''')

html = f'''<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">
<title>真实字体字形 · 图标候选</title><style>
*{{box-sizing:border-box}}
body{{margin:0;padding:26px 30px 40px;background:{BG};color:{INK};
  font-family:"Segoe UI","Microsoft YaHei",system-ui,sans-serif}}
h1{{font-size:20px;margin:0 0 4px}}
.lead{{color:{MUTED};font-size:13px;margin:0 0 20px}}
.card{{background:{PAPER};border:1px solid {LINE};border-radius:14px;padding:16px 18px;margin-bottom:16px}}
.card h2{{font-size:15px;margin:0 0 3px}}
.card h2 code{{font-size:11px;color:{MUTED};font-weight:400;background:{BG};
  border:1px solid {LINE};border-radius:4px;padding:1px 5px;margin-left:6px}}
.note{{margin:0 0 12px;font-size:12px;color:{MUTED}}}
.row{{display:flex;gap:18px;align-items:flex-end}}
.row.wrap{{flex-wrap:wrap;margin-top:12px}}
.cell{{text-align:center}}
.ttl{{font-size:12px;color:{MUTED};margin-bottom:5px}}
.sz{{font-size:10px;color:{MUTED};margin-top:5px}}
.ic{{overflow:hidden;border:1px solid {LINE};display:inline-block}}
.ic svg{{display:block}}
</style></head><body>
<h1>真实字体字形 · 应用图标候选</h1>
<p class="lead">字形轮廓直接取自字体文件（fontTools 提取），非手绘。每种字体给出平假名与片假名，以及 150dp 细节、72dp 三配色、48dp 小尺寸三档。<br>
注意：此页用于横向比较各字体字形，候选统一按固定尺寸归一化，<b>不代表当前图标实际大小</b>；实际图标见 icon-size-preview.png。</p>
{"".join(cards)}
</body></html>'''

sys.stdout.write(html)
print(f"（已生成 {len(glyphs)} 个字形、{len(cards)} 张卡片）", file=sys.stderr)
