# -*- coding: utf-8 -*-
"""
生成字形尺寸对比预览页：对同一字形取多档安全区尺寸，展示 72dp / 48dp / 32dp 下的观感。
路径数据由 make_launcher_icon.py 实时生成，不另画一份，保证与最终资源同源。

用法：python compare_icon_sizes.py > sizes.html
"""
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
GEN = os.path.join(HERE, "make_launcher_icon.py")

SAFE_VALUES = [62, 58, 54, 50, 46]   # 目前是 62，越小字形越小
BG = "#F7F5EF"                        # ic_launcher_background
GLYPH = "#D94738"
LINE = "#E2DED4"
INK = "#20252B"
MUTED = "#69727D"


def path_for(safe):
    out = subprocess.run([sys.executable, GEN, str(safe)],
                         capture_output=True, text=True, encoding="utf-8")
    if out.returncode != 0:
        print(out.stderr, file=sys.stderr)
        raise SystemExit(f"生成 SAFE={safe} 失败")
    m = re.search(r'android:pathData="([^"]+)"', out.stdout)
    if not m:
        raise SystemExit(f"SAFE={safe} 未找到 pathData")
    return m.group(1)


def icon(d, size, radius, guide=False):
    g = ""
    if guide:
        g = (f'<circle cx="54" cy="54" r="33" fill="none" stroke="{MUTED}" '
             f'stroke-width="0.7" stroke-dasharray="3 3" opacity=".85"/>'
             f'<rect x="18" y="18" width="72" height="72" fill="none" '
             f'stroke="{MUTED}" stroke-width="0.7" opacity=".45"/>')
    return (f'<div class="ic" style="width:{size}px;height:{size}px;'
            f'border-radius:{radius};background:{BG}">'
            f'<svg viewBox="0 0 108 108" width="100%" height="100%">'
            f'<path d="{d}" fill="{GLYPH}"/>{g}</svg></div>')


rows = []
for safe in SAFE_VALUES:
    d = path_for(safe)
    tag = " ← 当前" if safe == 62 else ""
    rows.append(f'''<section class="card">
  <h2>安全区 {safe}{tag}</h2>
  <div class="row">
    <div class="cell">{icon(d, 132, "22px", guide=True)}<div class="sz">132px · 含必见区/核心区参考线</div></div>
    <div class="cell">{icon(d, 72, "50%")}<div class="sz">72dp 圆形</div></div>
    <div class="cell">{icon(d, 72, "16px")}<div class="sz">72dp 圆角方形</div></div>
    <div class="cell">{icon(d, 48, "11px")}<div class="sz">48dp</div></div>
    <div class="cell">{icon(d, 32, "8px")}<div class="sz">32dp</div></div>
  </div>
</section>''')

html = f'''<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">
<title>字形尺寸对比</title><style>
*{{box-sizing:border-box}}
body{{margin:0;padding:24px 28px 34px;background:{BG};color:{INK};
 font-family:"Segoe UI","Microsoft YaHei",system-ui,sans-serif}}
h1{{font-size:19px;margin:0 0 3px}}
.lead{{color:{MUTED};font-size:12.5px;margin:0 0 18px;line-height:1.6}}
.card{{background:#FFFDF8;border:1px solid {LINE};border-radius:14px;
 padding:14px 18px 18px;margin-bottom:14px}}
.card h2{{font-size:14px;margin:0 0 12px;font-weight:600}}
.row{{display:flex;gap:22px;align-items:flex-end;flex-wrap:wrap}}
.cell{{text-align:center}}
.sz{{font-size:10.5px;color:{MUTED};margin-top:6px;white-space:nowrap}}
.ic{{overflow:hidden;border:1px solid {LINE};display:inline-block}}
.ic svg{{display:block}}
</style></head><body>
<h1>启动图标字形尺寸对比</h1>
<p class="lead">数字是字形归一化时占用的画布单位（108 画布中，中央 72 必定可见、66 为各遮罩安全核心区）。<br>
数值越小 → 字形越小、四周留白越多。虚线圆 = 66 核心区参考，方框 = 72 必见区参考（实际图标不含参考线）。</p>
{"".join(rows)}
</body></html>'''

sys.stdout.write(html)
print(f"已生成 {len(SAFE_VALUES)} 档对比", file=sys.stderr)
