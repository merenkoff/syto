"""Convert assets/icon/syto-icon.svg (rounded rect + dot grid) into adaptive-icon vector drawables.

Adaptive icon canvas is 108x108 dp; the launcher shows the inner 72 dp, safe zone is a 66 dp circle.
The 512-unit SVG is mapped onto the inner 72 dp: x' = 18 + x * 72/512.
"""
import re
from collections import OrderedDict

SVG = "assets/icon/syto-icon.svg"
OUT = "app/src/main/res"
SCALE = 72 / 512  # 0.140625
OFFSET = 18

src = open(SVG, encoding="utf-8").read()
circles = re.findall(
    r'<circle cx="([\d.]+)" cy="([\d.]+)" r="([\d.]+)" fill="(#[0-9A-Fa-f]{6})" opacity="([\d.]+)"/>', src
)
assert circles, "no circles parsed"

rows = OrderedDict()  # (cy, r, opacity, fill) -> [cx...]
for cx, cy, r, fill, op in circles:
    rows.setdefault((float(cy), float(r), op, fill), []).append(float(cx))

def fmt(v: float) -> str:
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return s if s else "0"

def circle_path(cx: float, cy: float, r: float) -> str:
    d = 2 * r
    return f"M{fmt(cx - r)},{fmt(cy)}a{fmt(r)},{fmt(r)} 0 1,0 {fmt(d)},0a{fmt(r)},{fmt(r)} 0 1,0 -{fmt(d)},0z"

def dot_paths(color_attr: str) -> str:
    out = []
    for (cy, r, op, fill), cxs in rows.items():
        data = "".join(circle_path(cx, cy, r) for cx in sorted(cxs))
        color = fill if color_attr == "svg" else color_attr
        out.append(
            f'        <path\n            android:fillColor="{color}"\n'
            f'            android:fillAlpha="{op}"\n            android:pathData="{data}" />'
        )
    return "\n".join(out)

HEADER = (
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<!-- Generated from assets/icon/syto-icon.svg by scripts/svg2vector.py. Do not edit by hand. -->\n'
)

def group(paths: str) -> str:
    return (
        f'    <group\n        android:translateX="{OFFSET}"\n        android:translateY="{OFFSET}"\n'
        f'        android:scaleX="{SCALE}"\n        android:scaleY="{SCALE}">\n{paths}\n    </group>\n'
    )

VECTOR_OPEN = (
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
    '    xmlns:aapt="http://schemas.android.com/aapt"\n'
    '    android:width="108dp"\n    android:height="108dp"\n'
    '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
)

foreground = HEADER + VECTOR_OPEN + group(dot_paths("svg")) + "</vector>\n"
monochrome = HEADER + VECTOR_OPEN.replace('    xmlns:aapt="http://schemas.android.com/aapt"\n', '') + group(dot_paths("#FFFFFFFF")) + "</vector>\n"

# Background: linear ink gradient over the full canvas + soft radial glow positioned relative to the visible 72 dp.
glow_cx = fmt(OFFSET + 0.42 * 72)
glow_cy = fmt(OFFSET + 0.88 * 72)
glow_r = fmt(0.6 * 72)
background = HEADER + VECTOR_OPEN + f'''    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108"
                android:startColor="#FF0E1A3C"
                android:endColor="#FF1C3068" />
        </aapt:attr>
    </path>
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="{glow_cx}"
                android:centerY="{glow_cy}"
                android:gradientRadius="{glow_r}"
                android:startColor="#4D6FB3FF"
                android:endColor="#006FB3FF" />
        </aapt:attr>
    </path>
</vector>
'''

adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
'''

open(f"{OUT}/drawable/ic_launcher_foreground.xml", "w", encoding="utf-8").write(foreground)
open(f"{OUT}/drawable/ic_launcher_monochrome.xml", "w", encoding="utf-8").write(monochrome)
open(f"{OUT}/drawable/ic_launcher_background.xml", "w", encoding="utf-8").write(background)
open(f"{OUT}/mipmap-anydpi/ic_launcher.xml", "w", encoding="utf-8").write(adaptive)
print(f"circles={len(circles)} rows={len(rows)}")
xs = [float(c[0]) for c in circles]; ys = [float(c[1]) for c in circles]
print(f"dot extent svg x={min(xs)}..{max(xs)} y={min(ys)}..{max(ys)}")
print(f"dot extent dp  x={OFFSET+min(xs)*SCALE:.1f}..{OFFSET+max(xs)*SCALE:.1f} y={OFFSET+min(ys)*SCALE:.1f}..{OFFSET+max(ys)*SCALE:.1f} (safe zone circle r=33 around 54,54)")
