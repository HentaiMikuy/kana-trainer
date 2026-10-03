# -*- coding: utf-8 -*-
"""
从 UD Digi Kyokasho 抽取平假名「あ」的真实轮廓，生成 Android 自适应图标的前景 VectorDrawable。
容器变换（平移/缩放/翻转 y 轴）全部烘焙进路径坐标，避免不同渲染器对 <group> 变换的处理差异。

用法：python make_launcher_icon.py [安全区尺寸，默认 54] > ic_launcher_foreground.xml
      数值越小字形越小、四周留白越多；上限 66（各遮罩的安全核心区）。
"""
import os
import sys
from fontTools.ttLib import TTFont
from fontTools.pens.recordingPen import RecordingPen
from fontTools.pens.transformPen import TransformPen
from fontTools.misc.transform import Transform
from fontTools.pens.svgPathPen import SVGPathPen

FONT = os.path.join(os.environ["WINDIR"], "Fonts", "UDDigiKyokashoN-R.ttc")
CHAR = "あ"

# 自适应图标：108×108 画布，必见区中央 72×72，安全核心区 66×66。
# 字形按此尺寸归一化后居中：数值越小，四周留白越多。
SAFE = float(sys.argv[1]) if len(sys.argv) > 1 else 54.0
if not 30.0 <= SAFE <= 66.0:
    raise SystemExit(f"安全区尺寸应在 30..66 之间，收到 {SAFE}")
CENTER = 54.0
# 1 SVG 单位对应的字体单位数，按最大边计算
MIN_THICKNESS_UNITS = 1900  # 约 0.97 个 SVG 单位，防止极细笔画在小尺寸下消失

font = TTFont(FONT, fontNumber=0, lazy=True)
cmap = font.getBestCmap()
glyph_name = cmap[ord(CHAR)]
gs = font.getGlyphSet()

# 先量包围盒
from fontTools.pens.boundsPen import BoundsPen
bp = BoundsPen(gs)
gs[glyph_name].draw(bp)
x0, y0, x1, y1 = bp.bounds
w, h = x1 - x0, y1 - y0
units_per_svg = max(w, h) / SAFE

# 居中 + 缩放 + 翻转 y（字体 y 向上，SVG y 向下）
t = Transform(
    1 / units_per_svg, 0,
    0, -1 / units_per_svg,
    CENTER - x0 / units_per_svg - (w / units_per_svg) / 2,
    CENTER + y0 / units_per_svg + (h / units_per_svg) / 2,
)

rec = RecordingPen()
gs[glyph_name].draw(TransformPen(rec, t))

path_pen = SVGPathPen(None, ntos=lambda v: f"{v:.2f}")
rec.replay(path_pen)
d = path_pen.getCommands()

print(f"字形 {CHAR} ({glyph_name})  bbox={bp.bounds}  尺寸={w:.0f}×{h:.0f} 字体单位", file=sys.stderr)
print(f"缩放：1 SVG 单位 = {units_per_svg:.1f} 字体单位", file=sys.stderr)
print(f"烘焙后路径长度 {len(d)} 字符", file=sys.stderr)

xml = f'''<?xml version="1.0" encoding="utf-8"?>
<!--
  平假名「あ」——字形轮廓取自 UD Digi Kyokasho（教科书楷书体），非手绘。
  由 android-app/design/make_launcher_icon.py 生成，请勿手工编辑。
  自适应图标规范：108×108 画布，中央 72×72 必定可见，66×66 为各遮罩安全核心区；
  字形按 62 归一化并居中，容器变换已烘焙进路径坐标。
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="{d}" />
</vector>
'''

sys.stdout.write(xml)
