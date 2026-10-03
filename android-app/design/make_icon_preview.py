# -*- coding: utf-8 -*-
"""
直接读取 ic_launcher_foreground.xml 的路径数据，生成各尺寸/各遮罩下的图标预览。
这样预览与实际打包进 APK 的矢量资源完全同源，不是另画一份。

用法：python make_icon_preview.py > sizes.html  然后用浏览器或无头截图查看
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DRAWABLE = os.path.normpath(os.path.join(
    HERE, "..", "app", "src", "main", "res", "drawable", "ic_launcher_foreground.xml"))

with open(DRAWABLE, encoding="utf-8") as f:
    xml = f.read()

m = re.search(r'android:pathData="([^"]+)"', xml)
if not m:
    raise SystemExit("未在 drawable 中找到 android:pathData")
path_d = m.group(1)

m_color = re.search(r'android:fillColor="([^"]+)"', xml)
glyph_color = m_color.group(1) if m_color else "#D94738"

BG = "#F7F5EF"          # ic_launcher_background
LINE = "#E2DED4"
INK = "#20252B"
MUTED = "#69727D"


def icon(size, radius, bg=BG, guide=False):
    """一个 size×size 的图标预览；guide 打开时画出 72×72 必见区与 66×66 安全核心区。"""
    guides = ""
    if guide:
        # 108 画布 -> 108 单位，圆心 54
        guides = (f'<circle cx="54" cy="54" r="33" fill="none" stroke="{MUTED}" '
                  f'stroke-width="0.7" stroke-dasharray="3 3" opacity=".8"/>'
                  f'<rect x="18" y="18" width="72" height="72" fill="none" '
                  f'stroke="{MUTED}" stroke-width="0.7" opacity=".5"/>')
    return f'''<div class="ic" style="width:{size}px;height:{size}px;border-radius:{radius};background:{bg}">
  <svg viewBox="0 0 108 108" width="100%" height="100%">
    <path d="{path_d}" fill="{glyph_color}"/>{guides}</svg></div>'''


sizes = [
    ("192dp 应用商店", 192, "34px", True),
    ("96dp", 96, "20px", False),
    ("72dp 圆形遮罩", 72, "50%", False),
    ("72dp 圆角方形", 72, "16px", False),
    ("72dp 水滴形", 72, "50% 50% 50% 12px", False),
    ("48dp 抽屉", 48, "11px", False),
    ("32dp 通知栏", 32, "8px", False),
]
cells = "".join(
    f'<div class="cell">{icon(px, r, guide=g)}<div class="sz">{name}</div></div>'
    for name, px, r, g in sizes
)

html = f'''<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">
<title>启动图标 · 真实尺寸预览</title><style>
*{{box-sizing:border-box}}
body{{margin:0;padding:24px 28px;background:{BG};color:{INK};
 font-family:"Segoe UI","Microsoft YaHei",system-ui,sans-serif}}
h1{{font-size:19px;margin:0 0 3px}}
.lead{{color:{MUTED};font-size:12.5px;margin:0 0 18px}}
.row{{display:flex;gap:26px;align-items:flex-end;flex-wrap:wrap;
 background:#FFFDF8;border:1px solid {LINE};border-radius:14px;padding:20px 22px}}
.cell{{text-align:center}}
.sz{{font-size:10.5px;color:{MUTED};margin-top:7px;white-space:nowrap}}
.ic{{overflow:hidden;border:1px solid {LINE};display:inline-block}}
.ic svg{{display:block}}
</style></head><body>
<h1>启动图标 · 真实尺寸预览</h1>
<p class="lead">路径数据直接取自 app/src/main/res/drawable/ic_launcher_foreground.xml，与打包进 APK 的资源同源。底色 {BG}、字形 {glyph_color}。字形按 54 单位归一化（此前为 62）。</p>
<div class="row">{cells}</div>
</body></html>'''

sys.stdout.write(html)
print("已生成预览页", file=sys.stderr)
