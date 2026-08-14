#!/usr/bin/env python3
"""Generate original grayscale source masks for the GlyphLock prototype."""
from __future__ import annotations

from pathlib import Path
import math
import random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageChops

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "scenes"
W, H = 1080, 2400


def bezier(a, c, b, n=40):
    pts = []
    for i in range(n):
        t = i / (n - 1)
        u = 1 - t
        pts.append((
            int(u*u*a[0] + 2*u*t*c[0] + t*t*b[0]),
            int(u*u*a[1] + 2*u*t*c[1] + t*t*b[1]),
        ))
    return pts


def add_grain(img: Image.Image, seed: int, amount: int = 11) -> Image.Image:
    rng = np.random.default_rng(seed)
    arr = np.asarray(img, dtype=np.int16)
    noise = rng.normal(0, amount, arr.shape).astype(np.int16)
    # Organic low-frequency density drift.
    small = rng.integers(0, 256, (60, 27), dtype=np.uint8)
    low = Image.fromarray(small, "L").resize((W, H), Image.Resampling.BICUBIC).filter(ImageFilter.GaussianBlur(28))
    low_arr = np.asarray(low, dtype=np.int16) - 128
    arr = np.clip(arr + noise + low_arr // 10, 0, 255).astype(np.uint8)
    return Image.fromarray(arr, "L")


def layered_canvas():
    base = Image.new("L", (W, H), 0)
    soft = Image.new("L", (W, H), 0)
    detail = Image.new("L", (W, H), 0)
    return base, soft, detail, ImageDraw.Draw(soft), ImageDraw.Draw(detail)


def composite(base, soft, detail, blur=14):
    soft = soft.filter(ImageFilter.GaussianBlur(blur))
    base = ImageChops.lighter(base, soft)
    return ImageChops.lighter(base, detail)


def sentinel() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx = W // 2

    # Architectural halo and very quiet signal grid.
    for r, fill, width in [(410, 34, 4), (590, 24, 3), (790, 17, 3), (1010, 12, 2)]:
        d.ellipse((cx-r, 390-r, cx+r, 390+r), outline=fill, width=width)
    for xx in (170, 290, 790, 910):
        d.line((xx, 180, xx, 1880), fill=11, width=2)
    d.line((cx, 160, cx, 2130), fill=34, width=3)

    # Wing membranes.
    left_poly = [(500, 520), (345, 330), (110, 245), (38, 640), (105, 1110), (290, 1490), (470, 1180)]
    right_poly = [(W-x, y) for x, y in left_poly]
    s.polygon(left_poly, fill=52)
    s.polygon(right_poly, fill=52)
    d.line(left_poly + [left_poly[0]], fill=96, width=7, joint="curve")
    d.line(right_poly + [right_poly[0]], fill=96, width=7, joint="curve")

    # Feather curves.
    for side in (-1, 1):
        root = (cx + side * 58, 610)
        for i in range(24):
            p = i / 23
            end_x = cx + side * int(230 + 325 * (math.sin(math.pi * (0.08 + 0.82*p)) ** 0.75))
            end_y = int(330 + 1110 * p)
            control = (cx + side * int(170 + 230 * math.sin(math.pi*p)), int(380 + 650*p))
            pts = bezier(root, control, (end_x, end_y), 46)
            shade = int(78 + 88*(1-abs(p-.48)))
            d.line(pts, fill=shade, width=max(3, int(5+4*p)), joint="curve")
            # Parallel split creates a finer feather texture.
            pts2 = [(x - side*8, y+8) for x, y in pts[5:]]
            d.line(pts2, fill=max(35, shade-45), width=2, joint="curve")

        outer = bezier((cx + side*48, 595), (cx + side*600, 215), (cx + side*430, 1460), 70)
        d.line(outer, fill=176, width=9, joint="curve")

    # Figure, built as a sculptural luminance mask.
    s.ellipse((455, 470, 625, 675), fill=155)              # head
    s.rounded_rectangle((485, 620, 595, 790), 50, fill=130) # neck
    s.ellipse((340, 680, 740, 1410), fill=125)             # torso
    s.ellipse((405, 1270, 675, 1590), fill=118)            # pelvis
    s.line((410, 790, 285, 1465), fill=120, width=120)     # arms
    s.line((670, 790, 795, 1465), fill=120, width=120)
    s.line((470, 1450, 390, 2250), fill=130, width=145)    # legs
    s.line((610, 1450, 690, 2250), fill=130, width=145)

    # Crisp contour and internal anatomy.
    d.ellipse((455, 470, 625, 675), outline=215, width=7)
    d.arc((332, 670, 748, 1418), 192, 348, fill=188, width=7)
    d.arc((332, 670, 748, 1418), 12, 168, fill=104, width=5)
    d.line((432, 730, 540, 850, 648, 730), fill=150, width=6, joint="curve")
    d.line((540, 845, 540, 1360), fill=92, width=5)
    d.line((490, 545, 572, 535, 590, 575, 535, 638), fill=170, width=5, joint="curve")
    d.line((432, 1450, 387, 2250), fill=174, width=7)
    d.line((648, 1450, 693, 2250), fill=112, width=5)

    # Halo around head.
    d.ellipse((390, 400, 690, 710), outline=162, width=7)
    d.ellipse((365, 375, 715, 735), outline=51, width=3)

    img = composite(base, soft, detail, blur=18)
    # Mild edge fade keeps the clock and command areas calm without hiding detail.
    arr = np.asarray(img, dtype=np.float32)
    yy, xx = np.mgrid[0:H, 0:W]
    edge = np.minimum.reduce([xx / 150.0, (W - 1 - xx) / 150.0, yy / 180.0, (H - 1 - yy) / 180.0])
    fade = np.clip(edge, 0.30, 1.0)
    img = Image.fromarray(np.uint8(np.clip(arr * fade, 0, 255)), "L")
    return add_grain(img, 1337, 9)


def moth() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx = W // 2
    # Background orbit.
    for rx, ry, fill in [(320, 430, 34), (500, 650, 20), (720, 930, 12)]:
        d.ellipse((cx-rx, 980-ry, cx+rx, 980+ry), outline=fill, width=3)

    # Wings.
    left_top = [(505, 815), (385, 500), (85, 300), (40, 720), (180, 1080), (480, 1070)]
    left_bottom = [(495, 995), (205, 1060), (75, 1540), (280, 1890), (500, 1460)]
    for poly in (left_top, left_bottom):
        s.polygon(poly, fill=58)
        d.line(poly+[poly[0]], fill=132, width=7, joint="curve")
        mirror = [(W-x, y) for x, y in poly]
        s.polygon(mirror, fill=58)
        d.line(mirror+[mirror[0]], fill=132, width=7, joint="curve")

    # Veins and eye spots.
    for side in (-1, 1):
        root = (cx+side*24, 885)
        for i in range(17):
            p = i/16
            end = (cx+side*int(230+290*math.sin(math.pi*(.05+.9*p))), int(390+1270*p))
            ctrl = (cx+side*int(160+230*p), int(610+620*p))
            d.line(bezier(root, ctrl, end, 42), fill=int(82+68*math.sin(math.pi*p)), width=4, joint="curve")
        for ex, ey, rr in [(cx+side*340, 720, 90), (cx+side*330, 1420, 72)]:
            d.ellipse((ex-rr,ey-rr,ex+rr,ey+rr), outline=176, width=9)
            d.ellipse((ex-rr//3,ey-rr//3,ex+rr//3,ey+rr//3), fill=190)
            d.ellipse((ex-rr*2,ey-rr*2,ex+rr*2,ey+rr*2), outline=46, width=4)

    # Body.
    s.ellipse((470, 610, 610, 790), fill=170)
    s.rounded_rectangle((500, 725, 580, 1690), 40, fill=150)
    d.ellipse((470, 610, 610, 790), outline=220, width=6)
    d.line((540, 745, 540, 1680), fill=210, width=5)
    # Antennae.
    d.line(bezier((520,650),(420,360),(300,430),50), fill=180, width=6, joint="curve")
    d.line(bezier((560,650),(660,360),(780,430),50), fill=180, width=6, joint="curve")

    img = composite(base, soft, detail, blur=16)
    return add_grain(img, 2204, 10)


def orbit() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx = W//2
    # Abstract sculptural face.
    s.ellipse((310, 390, 770, 1330), fill=102)
    s.ellipse((370, 720, 710, 1380), fill=135)
    s.ellipse((180, 1270, 900, 2200), fill=60)
    d.ellipse((310,390,770,1330), outline=176, width=8)
    for yy in range(520, 1210, 42):
        half = int(200*math.sqrt(max(0,1-((yy-860)/480)**2)))
        d.line((cx-half, yy, cx+half, yy+int(7*math.sin(yy/61))), fill=80, width=3)
    # Eye glint and contours.
    d.ellipse((418, 750, 466, 784), fill=234)
    d.line((420,720,500,680,650,735), fill=154, width=6, joint="curve")
    d.line((470,1040,540,1110,610,1045), fill=125, width=5, joint="curve")
    for r, fill in [(280,180),(390,80),(540,38),(720,18)]:
        d.ellipse((cx-r, 820-r, cx+r, 820+r), outline=fill, width=max(2,8-r//120))
    d.line((cx,140,cx,2210),fill=32,width=3)
    img = composite(base, soft, detail, blur=22)
    return add_grain(img, 404, 9)


def save(name: str, img: Image.Image):
    OUT.mkdir(parents=True, exist_ok=True)
    img.save(OUT / f"scene_{name}.png", optimize=True)


def thumbnail():
    src = Image.open(OUT / "scene_sentinel.png").resize((220,488), Image.Resampling.LANCZOS)
    out = Image.new("RGB", (512,512), "black")
    arr = np.asarray(src)
    draw = ImageDraw.Draw(out)
    ramp = " .:;+=x#@"
    for y in range(0,488,6):
        for x in range(0,220,6):
            v=int(arr[y:y+6,x:x+6].mean())
            if v<16: continue
            ch=ramp[min(len(ramp)-1,int(v/256*len(ramp)))]
            draw.text((146+x,12+y),ch,fill=(v,v,min(255,v+12)))
    out.save(OUT / "wallpaper_thumbnail.png", optimize=True)


def main():
    save("sentinel", sentinel())
    save("moth", moth())
    save("orbit", orbit())
    thumbnail()
    print("generated")

if __name__ == "__main__": main()
