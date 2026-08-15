#!/usr/bin/env python3
"""Generate original grayscale source masks for the GlyphLock prototype."""
from __future__ import annotations

from pathlib import Path
import math
import random
import argparse
import os
from concurrent.futures import ProcessPoolExecutor, as_completed
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageChops

ROOT = Path(__file__).resolve().parents[1]
OUT_WEB = ROOT / "apps" / "visual-lab" / "public" / "assets"
OUT_ANDROID = ROOT / "apps" / "android" / "app" / "src" / "main" / "res" / "drawable-nodpi"
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



def edge_fade(img: Image.Image, horizontal: float = 150.0, vertical: float = 190.0, floor: float = 0.24) -> Image.Image:
    arr = np.asarray(img, dtype=np.float32)
    yy, xx = np.mgrid[0:H, 0:W]
    edge = np.minimum.reduce([
        xx / horizontal,
        (W - 1 - xx) / horizontal,
        yy / vertical,
        (H - 1 - yy) / vertical,
    ])
    fade = np.clip(edge, floor, 1.0)
    return Image.fromarray(np.uint8(np.clip(arr * fade, 0, 255)), "L")


def polar(cx: float, cy: float, radius: float, angle: float) -> tuple[int, int]:
    return int(cx + math.cos(angle) * radius), int(cy + math.sin(angle) * radius)


def neural_halo() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx, cy = W // 2, 840
    rng = random.Random(8841)
    for r, fill, width in [(116, 210, 7), (205, 120, 5), (330, 58, 3), (505, 28, 2), (730, 14, 2)]:
        d.ellipse((cx-r, cy-r, cx+r, cy+r), outline=fill, width=width)
    nodes=[]
    for ring, count in [(150, 18),(260, 28),(385, 42),(535, 54),(720, 68)]:
        for i in range(count):
            a=2*math.pi*i/count + rng.uniform(-.08,.08)
            rr=ring+rng.uniform(-34,34)
            x,y=polar(cx,cy,rr,a)
            nodes.append((x,y,a,ring))
            rad=2 if ring>400 else 3
            d.ellipse((x-rad,y-rad,x+rad,y+rad), fill=int(82+130*(1-ring/760)))
    for i,(x,y,a,ring) in enumerate(nodes):
        candidates=nodes[max(0,i-7):min(len(nodes),i+8)]
        for x2,y2,a2,ring2 in candidates:
            if abs(ring2-ring)>180 or (x==x2 and y==y2): continue
            dist=math.hypot(x2-x,y2-y)
            if dist<145 and rng.random()<.23:
                d.line((x,y,x2,y2), fill=int(max(14,92-dist*.42)), width=1)
    # Long axons and lower body-like wake.
    for i in range(58):
        a=rng.uniform(math.pi*.10,math.pi*.90)
        start=polar(cx,cy,rng.uniform(120,380),a)
        end=(int(cx+rng.uniform(-470,470)), int(rng.uniform(1220,2220)))
        ctrl=(int((start[0]+end[0])/2+rng.uniform(-130,130)), int((start[1]+end[1])/2))
        d.line(bezier(start,ctrl,end,60), fill=rng.randint(18,74), width=rng.choice([1,1,2]))
    s.ellipse((cx-82,cy-82,cx+82,cy+82),fill=120)
    d.ellipse((cx-44,cy-44,cx+44,cy+44),outline=235,width=7)
    d.ellipse((cx-13,cy-13,cx+13,cy+13),fill=255)
    return add_grain(edge_fade(composite(base,soft,detail,16)),8841,8)


def cipher_cathedral() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx=W//2
    horizon=870
    # Perspective vaults and server columns.
    for depth in range(12):
        t=depth/11
        half=int(480*(1-t)+95*t)
        top=int(260*(1-t)+horizon*t)
        floor=int(2260*(1-t)+horizon*t)
        fill=int(22+115*(1-t))
        d.line((cx-half,top,cx-half//3,horizon),fill=fill,width=max(1,5-depth//3))
        d.line((cx+half,top,cx+half//3,horizon),fill=fill,width=max(1,5-depth//3))
        d.line((cx-half,floor,cx-half//3,horizon),fill=max(12,fill-20),width=max(1,4-depth//4))
        d.line((cx+half,floor,cx+half//3,horizon),fill=max(12,fill-20),width=max(1,4-depth//4))
    for x in range(75,W,65):
        perspective=abs(x-cx)/(W/2)
        top=int(330+300*(1-perspective))
        d.line((x,top,x,2160),fill=int(22+72*(1-perspective)),width=2)
        for y in range(top+25,2150,38):
            if (x//65+y//38)%3==0:
                d.line((x-13,y,x+13,y),fill=int(28+95*(1-perspective)),width=2)
    # Central encrypted reliquary.
    d.rounded_rectangle((cx-122,540,cx+122,1725),28,outline=205,width=8)
    d.rounded_rectangle((cx-92,605,cx+92,1650),20,outline=72,width=3)
    for y in range(660,1580,62):
        d.line((cx-70,y,cx+70,y),fill=65+(y//62)%3*24,width=3)
    for r,fill in [(56,236),(95,115),(160,42),(255,18)]:
        d.ellipse((cx-r,860-r,cx+r,860+r),outline=fill,width=max(2,7-r//60))
    d.ellipse((cx-16,844,cx+16,876),fill=255)
    return add_grain(edge_fade(composite(base,soft,detail,18)),2048,8)


def quantum_lattice() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx,cy=W//2,930
    rng=random.Random(7193)
    # Perspective lattice.
    for i in range(-13,14):
        x0=cx+i*53
        d.line((cx,cy,x0,2220),fill=22+int(56*(1-abs(i)/14)),width=2)
    for j in range(18):
        t=j/17
        y=int(cy+(2220-cy)*t*t)
        half=int(40+(W*.47-40)*t)
        d.line((cx-half,y,cx+half,y),fill=int(86*(1-t)+18),width=2)
    # Probability shells and entangled nodes.
    for k in range(7):
        rx=100+k*75
        ry=56+k*47
        offset=math.sin(k*1.7)*70
        d.ellipse((cx-rx,cy+offset-ry,cx+rx,cy+offset+ry),outline=45+k*18,width=3)
    pts=[]
    for i in range(72):
        a=2*math.pi*i/72
        r=165+115*math.sin(3*a)+rng.uniform(-20,20)
        x=int(cx+math.cos(a)*r)
        y=int(cy+math.sin(a)*r*.68)
        pts.append((x,y))
        d.ellipse((x-3,y-3,x+3,y+3),fill=rng.randint(100,230))
    for i in range(0,len(pts),3):
        x,y=pts[i]
        x2,y2=pts[(i+17)%len(pts)]
        d.line((x,y,x2,y2),fill=42,width=1)
    d.ellipse((cx-28,cy-28,cx+28,cy+28),fill=244)
    d.ellipse((cx-96,cy-96,cx+96,cy+96),outline=198,width=7)
    return add_grain(edge_fade(composite(base,soft,detail,16)),7193,8)


def fusion_core() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx,cy=W//2,930
    rng=random.Random(42420)
    # Tokamak / reactor rings.
    for r,fill,width in [(115,248,8),(180,160,6),(270,92,4),(390,48,3),(540,24,2),(730,12,2)]:
        d.ellipse((cx-r,cy-r,cx+r,cy+r),outline=fill,width=width)
    for a in np.linspace(0,2*math.pi,24,endpoint=False):
        p1=polar(cx,cy,135,a)
        p2=polar(cx,cy,475,a+0.16*math.sin(a*3))
        d.line((p1,p2),fill=82,width=3)
    # Plasma filaments.
    for i in range(64):
        a=rng.uniform(0,2*math.pi)
        start=polar(cx,cy,rng.uniform(70,150),a)
        mid=polar(cx,cy,rng.uniform(210,380),a+rng.uniform(-.6,.6))
        end=polar(cx,cy,rng.uniform(450,700),a+rng.uniform(-.35,.35))
        d.line(bezier(start,mid,end,50),fill=rng.randint(32,125),width=rng.choice([1,2,3]))
    # Lower containment tower.
    d.rounded_rectangle((cx-155,1110,cx+155,2020),70,outline=116,width=6)
    for y in range(1200,1960,55):
        half=int(115-40*math.sin((y-1200)/760*math.pi))
        d.line((cx-half,y,cx+half,y),fill=35+(y//55)%4*18,width=2)
    s.ellipse((cx-68,cy-68,cx+68,cy+68),fill=180)
    d.ellipse((cx-24,cy-24,cx+24,cy+24),fill=255)
    return add_grain(edge_fade(composite(base,soft,detail,18)),42420,8)


def packet_bloom() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx,cy=W//2,990
    rng=random.Random(1031)
    # Radial packet petals made of circuit paths.
    for petal in range(12):
        angle=2*math.pi*petal/12
        for lane in range(7):
            start=polar(cx,cy,70+lane*10,angle+lane*.018)
            mid=polar(cx,cy,260+lane*38,angle+math.sin(lane)*.10)
            end=polar(cx,cy,520+lane*36,angle+math.sin(petal)*.08)
            points=[start,(mid[0],start[1]),mid,(mid[0],end[1]),end] if petal%2==0 else [start,(start[0],mid[1]),mid,(end[0],mid[1]),end]
            d.line(points,fill=30+lane*17,width=1+lane//3,joint='curve')
            for x,y in (points[1],points[2],points[3]):
                d.rectangle((x-2,y-2,x+2,y+2),fill=100+lane*16)
    for r,fill in [(88,238),(170,126),(310,58),(500,24),(720,12)]:
        d.ellipse((cx-r,cy-r,cx+r,cy+r),outline=fill,width=max(2,7-r//110))
    # Falling packet trails.
    for i in range(80):
        x=rng.randint(70,W-70)
        y=rng.randint(1260,2200)
        length=rng.randint(35,240)
        d.line((x,y,x,y+length),fill=rng.randint(18,88),width=rng.choice([1,1,2]))
        if rng.random()<.45:
            d.line((x,y+length,x+rng.choice([-1,1])*rng.randint(15,70),y+length),fill=rng.randint(24,100),width=1)
    d.ellipse((cx-22,cy-22,cx+22,cy+22),fill=255)
    return add_grain(edge_fade(composite(base,soft,detail,15)),1031,8)



def event_horizon() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx, cy = W // 2, 910
    rng = random.Random(9102026)

    # Warped star field; density increases near the lens.
    for _ in range(420):
        x = rng.randint(50, W - 50)
        y = rng.randint(150, H - 150)
        dx, dy = x - cx, y - cy
        radius = max(1.0, math.hypot(dx, dy))
        bend = 1.0 + 88.0 / (radius + 95.0)
        x2 = int(cx + dx * bend)
        y2 = int(cy + dy * bend)
        if 35 < x2 < W - 35 and 35 < y2 < H - 35:
            value = rng.randint(28, 118)
            rr = 1 if value < 70 else 2
            d.ellipse((x2 - rr, y2 - rr, x2 + rr, y2 + rr), fill=value)

    # Accretion disk drawn as offset ellipses and broken hot lanes.
    for ring in range(34):
        r = 105 + ring * 14
        squash = 0.28 + ring * 0.003
        fill = min(232, 30 + ring * 5)
        width = 1 + ring // 12
        box = (cx - r, cy - int(r * squash), cx + r, cy + int(r * squash))
        start = 188 + (ring * 19) % 95
        end = 352 - (ring * 7) % 70
        d.arc(box, start, end, fill=fill, width=width)
        d.arc(box, 8 + ring % 11, 166 - ring % 17, fill=max(18, fill - 42), width=width)

    # Photon ring and dark singularity.
    d.ellipse((cx - 166, cy - 166, cx + 166, cy + 166), outline=188, width=8)
    d.ellipse((cx - 124, cy - 124, cx + 124, cy + 124), outline=244, width=5)
    s.ellipse((cx - 96, cy - 96, cx + 96, cy + 96), fill=12)

    # Relativistic jets.
    for side in (-1, 1):
        for lane in range(13):
            spread = lane * 7
            start = (cx + side * spread, cy + side * 118)
            end = (cx + side * rng.randint(-120, 120), cy + side * rng.randint(520, 1080))
            control = (cx + side * rng.randint(-65, 65), int((start[1] + end[1]) / 2))
            d.line(bezier(start, control, end, 58), fill=rng.randint(24, 96), width=rng.choice([1, 2, 3]))

    # Lensing guides become technical scaffolding around the event.
    for r, fill in [(260, 76), (390, 42), (560, 24), (760, 13)]:
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=fill, width=2)
    d.line((cx, 130, cx, 2210), fill=25, width=2)
    d.line((80, cy, W - 80, cy), fill=18, width=2)

    return add_grain(edge_fade(composite(base, soft, detail, 18)), 9102026, 8)


def tesseract_engine() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx, cy = W // 2, 960

    def projected_cube(scale: float, offset_x: float, offset_y: float, value: int, width: int) -> list[tuple[int, int]]:
        points = []
        for sx, sy in [(-1, -1), (1, -1), (1, 1), (-1, 1)]:
            x = cx + offset_x + sx * scale
            y = cy + offset_y + sy * scale * 1.35
            points.append((int(x), int(y)))
        d.line(points + [points[0]], fill=value, width=width, joint='curve')
        return points

    outer_a = projected_cube(375, -42, -95, 86, 5)
    outer_b = projected_cube(375, 62, 62, 132, 6)
    inner_a = projected_cube(205, -18, -46, 176, 6)
    inner_b = projected_cube(205, 34, 28, 228, 7)

    for first, second, value in [
        (outer_a, outer_b, 72),
        (inner_a, inner_b, 170),
        (outer_a, inner_a, 54),
        (outer_b, inner_b, 104),
    ]:
        for p1, p2 in zip(first, second):
            d.line((p1, p2), fill=value, width=3)

    # Repeated dimension frames and coordinate ticks.
    for step in range(8):
        scale = 90 + step * 58
        dx = math.sin(step * 0.9) * 44
        dy = math.cos(step * 0.7) * 36
        projected_cube(scale, dx, dy, 26 + step * 17, 2 + step // 4)

    for x in range(120, W - 100, 84):
        d.line((x, 220, x, 2160), fill=13 + (x // 84) % 3 * 7, width=1)
    for y in range(260, 2180, 82):
        d.line((100, y, W - 100, y), fill=12 + (y // 82) % 4 * 6, width=1)

    # Central transform engine.
    s.rounded_rectangle((cx - 88, cy - 210, cx + 88, cy + 210), 35, fill=96)
    d.rounded_rectangle((cx - 88, cy - 210, cx + 88, cy + 210), 35, outline=242, width=7)
    d.ellipse((cx - 34, cy - 34, cx + 34, cy + 34), fill=248)
    for a in np.linspace(0, 2 * math.pi, 16, endpoint=False):
        p1 = polar(cx, cy, 58, a)
        p2 = polar(cx, cy, 150, a + math.pi / 8)
        d.line((p1, p2), fill=118, width=2)

    return add_grain(edge_fade(composite(base, soft, detail, 15)), 4404, 7)


def helix_array() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx = W // 2
    top, bottom = 260, 2180
    turns = 5.6
    samples = 240

    left, right = [], []
    for i in range(samples):
        t = i / (samples - 1)
        y = top + (bottom - top) * t
        angle = turns * 2 * math.pi * t
        envelope = 245 + 48 * math.sin(t * math.pi)
        x1 = cx + math.sin(angle) * envelope
        x2 = cx + math.sin(angle + math.pi) * envelope
        left.append((int(x1), int(y)))
        right.append((int(x2), int(y)))

    d.line(left, fill=188, width=8, joint='curve')
    d.line(right, fill=118, width=7, joint='curve')
    for i in range(0, samples, 5):
        value = 52 + int(148 * (0.5 + 0.5 * math.cos(i / samples * turns * 2 * math.pi)))
        d.line((left[i], right[i]), fill=value, width=2)
        for x, y in (left[i], right[i]):
            rr = 3 + (i // 5) % 3
            d.ellipse((x - rr, y - rr, x + rr, y + rr), fill=min(242, value + 50))

    # Sequencing rails and telemetry ticks.
    for offset in (-390, 390):
        d.line((cx + offset, 230, cx + offset, 2200), fill=36, width=3)
        for y in range(300, 2160, 58):
            length = 18 + ((y // 58) % 5) * 7
            d.line((cx + offset - length, y, cx + offset + length, y), fill=54 + (y // 58) % 4 * 18, width=2)

    # A luminous splice at the semantic focal point.
    cy = 1030
    for r, fill, width in [(54, 250, 8), (116, 150, 5), (220, 68, 3), (370, 25, 2)]:
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=fill, width=width)
    d.ellipse((cx - 18, cy - 18, cx + 18, cy + 18), fill=255)

    return add_grain(edge_fade(composite(base, soft, detail, 16)), 2317, 8)


def interference_field() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx, cy = W // 2, 900

    # Two offset wave emitters create a living moire field.
    emitters = [(cx - 215, cy - 60), (cx + 215, cy + 58), (cx, cy + 510)]
    for ex, ey in emitters:
        for r in range(32, 860, 33):
            phase = (r // 33) % 6
            fill = 18 + phase * 15
            d.ellipse((ex - r, ey - r, ex + r, ey + r), outline=fill, width=1 + phase // 4)

    # Bright interference loci sampled analytically.
    for y in range(220, 2190, 22):
        for x in range(70, W - 70, 22):
            phase = 0.0
            for ex, ey in emitters[:2]:
                phase += math.sin(math.hypot(x - ex, y - ey) / 22.0)
            phase += 0.65 * math.sin(math.hypot(x - emitters[2][0], y - emitters[2][1]) / 31.0)
            if phase > 1.62:
                value = int(min(230, 72 + phase * 48))
                d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=value)

    # Phase lock rings and axial guides.
    for r, fill, width in [(90, 242, 7), (175, 152, 5), (300, 72, 3), (470, 33, 2)]:
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=fill, width=width)
    d.line((cx, 140, cx, 2200), fill=28, width=2)
    d.line((80, cy, W - 80, cy), fill=22, width=2)
    d.ellipse((cx - 24, cy - 24, cx + 24, cy + 24), fill=255)

    return add_grain(edge_fade(composite(base, soft, detail, 12)), 5150, 7)


def cryo_vault() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx = W // 2
    rng = random.Random(27315)

    # Hexagonal cold-storage chamber.
    chamber = [(cx, 260), (cx + 310, 470), (cx + 310, 1710), (cx, 2020), (cx - 310, 1710), (cx - 310, 470)]
    s.polygon(chamber, fill=38)
    d.line(chamber + [chamber[0]], fill=156, width=8, joint='curve')
    inner = [(cx, 365), (cx + 230, 535), (cx + 230, 1605), (cx, 1880), (cx - 230, 1605), (cx - 230, 535)]
    d.line(inner + [inner[0]], fill=86, width=4, joint='curve')

    # Cryogenic archive shelves.
    for y in range(520, 1640, 86):
        half = int(190 - 38 * abs((y - 1080) / 560))
        d.line((cx - half, y, cx + half, y), fill=42 + (y // 86) % 5 * 20, width=3)
        for x in range(cx - half + 18, cx + half, 52):
            d.rounded_rectangle((x, y - 22, x + 34, y + 22), 5, outline=68 + (x // 52) % 4 * 18, width=2)

    # Frost crystal branches around the chamber.
    for _ in range(90):
        x = rng.choice([rng.randint(40, 300), rng.randint(W - 300, W - 40)])
        y = rng.randint(220, 2170)
        length = rng.randint(30, 125)
        side = 1 if x < cx else -1
        d.line((x, y, x + side * length, y + rng.randint(-26, 26)), fill=rng.randint(24, 100), width=1)
        for branch in (-1, 1):
            bx = x + side * int(length * 0.55)
            by = y
            d.line((bx, by, bx + side * 24, by + branch * 24), fill=rng.randint(30, 112), width=1)

    # Core vial.
    s.rounded_rectangle((cx - 72, 690, cx + 72, 1410), 46, fill=108)
    d.rounded_rectangle((cx - 72, 690, cx + 72, 1410), 46, outline=232, width=7)
    for y in range(760, 1350, 44):
        d.line((cx - 46, y, cx + 46, y), fill=62 + (y // 44) % 4 * 30, width=2)
    d.ellipse((cx - 22, 1018, cx + 22, 1062), fill=255)

    return add_grain(edge_fade(composite(base, soft, detail, 17)), 27315, 8)


def dyson_relay() -> Image.Image:
    base, soft, detail, s, d = layered_canvas()
    cx, cy = W // 2, 870
    rng = random.Random(1960)

    # Segmented collector ring.
    for r, fill, width in [(124, 250, 9), (224, 154, 6), (355, 76, 4), (540, 32, 3), (750, 14, 2)]:
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=fill, width=width)

    segments = 32
    for i in range(segments):
        a0 = 2 * math.pi * i / segments
        a1 = a0 + math.pi / segments * 0.72
        inner = polar(cx, cy, 245, a0)
        outer = polar(cx, cy, 472 + 30 * math.sin(i * 1.7), a0)
        left = polar(cx, cy, 300, a0 - 0.035)
        right = polar(cx, cy, 300, a1 + 0.035)
        d.line((inner, outer), fill=72 + (i % 6) * 20, width=3)
        d.line((left, right), fill=115 + (i % 4) * 24, width=4)
        px, py = outer
        d.rectangle((px - 5, py - 5, px + 5, py + 5), fill=180 + (i % 3) * 22)

    # Relay beams and lower receiver architecture.
    for i in range(18):
        a = rng.uniform(0, 2 * math.pi)
        start = polar(cx, cy, rng.uniform(135, 250), a)
        end = polar(cx, cy, rng.uniform(510, 820), a + rng.uniform(-0.12, 0.12))
        d.line((start, end), fill=rng.randint(32, 112), width=rng.choice([1, 2, 3]))

    d.rounded_rectangle((cx - 150, 1240, cx + 150, 2040), 45, outline=118, width=6)
    d.line((cx, 1120, cx, 2110), fill=176, width=5)
    for y in range(1320, 1990, 56):
        half = 108 + int(24 * math.sin(y / 92))
        d.line((cx - half, y, cx + half, y), fill=36 + (y // 56) % 5 * 18, width=2)
    s.ellipse((cx - 64, cy - 64, cx + 64, cy + 64), fill=178)
    d.ellipse((cx - 23, cy - 23, cx + 23, cy + 23), fill=255)

    return add_grain(edge_fade(composite(base, soft, detail, 18)), 1960, 8)



def spectral_observatory() -> Image.Image:
    """A radio observatory whose dish, wavefronts, and spectrum become one signal field."""
    base, soft, detail, s, d = layered_canvas()
    cx, cy = W // 2, 970
    rng = random.Random(161803)

    # Celestial phase shells: restrained near the clock, increasingly dense toward the receiver.
    for index, (rx, ry) in enumerate(((250, 145), (390, 235), (565, 340), (760, 470), (960, 610))):
        fill = max(10, 70 - index * 12)
        d.ellipse((cx-rx, cy-ry, cx+rx, cy+ry), outline=fill, width=max(1, 4-index//2))

    # Parabolic dish. Multiple nested contours read as a precision instrument instead of a flat icon.
    rim_y = 900
    outer = []
    inner = []
    for i in range(121):
        t = i / 120
        x = cx - 410 + 820 * t
        u = (x - cx) / 410
        y = rim_y + 245 * (u * u)
        outer.append((int(x), int(y)))
        inner.append((int(x), int(y + 52 + 20 * (1 - u*u))))
    s.polygon(outer + list(reversed(inner)), fill=54)
    d.line(outer, fill=210, width=8, joint='curve')
    d.line(inner, fill=86, width=4, joint='curve')

    # Dish ribs and feed assembly.
    for i in range(17):
        t = i / 16
        x = int(cx - 390 + 780 * t)
        u = (x - cx) / 390
        y = int(rim_y + 230 * u*u)
        d.line((x, y, cx, cy + 275), fill=50 + (i % 4) * 20, width=2)
    d.line((cx, cy - 120, cx, cy + 480), fill=180, width=7)
    d.ellipse((cx - 34, cy - 152, cx + 34, cy - 84), fill=250)
    d.ellipse((cx - 88, cy - 206, cx + 88, cy - 30), outline=132, width=5)
    for arm in (-1, 1):
        d.line((cx, cy - 98, cx + arm * 212, rim_y + 42), fill=120, width=4)

    # Spectrum waterfall behind and below the focal point.
    for col in range(72):
        x = 88 + col * 13
        phase = math.sin(col * 0.47) + 0.52 * math.sin(col * 1.31)
        peak = int(300 + 235 * abs(phase))
        for band in range(0, peak, 18):
            y = 1420 + band
            value = int(24 + 120 * (1 - band / max(1, peak)))
            if col % 9 == 0:
                value += 35
            d.line((x, y, x, y + 10), fill=min(220, value), width=2)

    # Signal trajectories and interferometer stations.
    for i in range(22):
        angle = -1.05 + 2.1 * i / 21
        length = 410 + (i % 5) * 62
        end = (int(cx + math.sin(angle) * length), int(cy - 115 - math.cos(angle) * length * .55))
        control = (int(cx + math.sin(angle) * length * .45), int(cy - 115 - math.cos(angle) * length * .28))
        d.line(bezier((cx, cy - 115), control, end, 44), fill=28 + (i % 6) * 18, width=2, joint='curve')
        ex, ey = end
        d.ellipse((ex-4, ey-4, ex+4, ey+4), fill=120 + (i % 5) * 24)

    for _ in range(140):
        x = rng.randint(54, W - 54)
        y = rng.randint(220, 2130)
        if rng.random() < .58 and 760 < y < 1320:
            continue
        value = rng.randint(18, 92)
        d.point((x, y), fill=value)
        if rng.random() < .16:
            d.line((x - rng.randint(4, 14), y, x + rng.randint(4, 14), y), fill=value, width=1)

    # Pedestal and calibration rails.
    d.rounded_rectangle((cx - 118, 1240, cx + 118, 2110), 38, outline=112, width=6)
    for y in range(1340, 2040, 56):
        half = 76 + int(18 * math.sin(y / 73))
        d.line((cx-half, y, cx+half, y), fill=38 + (y // 56) % 5 * 18, width=2)

    return add_grain(edge_fade(composite(base, soft, detail, 15)), 161803, 8)


def recursive_monolith() -> Image.Image:
    """Nested impossible architecture with a deep semantic aperture and recursive rails."""
    base, soft, detail, s, d = layered_canvas()
    cx, cy = W // 2, 990

    # Sculptural monolith body with a subtle perspective taper.
    shell = [(cx-280, 260), (cx+280, 260), (cx+345, 2020), (cx, 2220), (cx-345, 2020)]
    s.polygon(shell, fill=34)
    d.line(shell + [shell[0]], fill=134, width=8, joint='curve')

    # Recursive projected doorways. Each frame is deliberately offset so the structure feels
    # impossible instead of becoming another centered tesseract.
    for depth in range(11):
        t = depth / 10
        top = int(350 + t * 430)
        bottom = int(1940 - t * 420)
        half_top = int(230 * (1 - t * .70))
        half_bottom = int(276 * (1 - t * .72))
        offset = int(math.sin(depth * 0.92) * 72 * (1 - t * .45))
        frame = [
            (cx - half_top + offset, top),
            (cx + half_top + offset, top),
            (cx + half_bottom - offset, bottom),
            (cx - half_bottom - offset, bottom),
        ]
        fill = 48 + depth * 15
        d.line(frame + [frame[0]], fill=min(224, fill), width=max(2, 7 - depth // 2), joint='curve')
        if depth < 10:
            # Asymmetric recursive connectors create a visual path through the stack.
            side = -1 if depth % 2 == 0 else 1
            d.line((frame[1 if side > 0 else 0], (cx + side * 42, int((top + bottom) * .5))), fill=max(30, fill - 12), width=2)

    # Central aperture: long enough to be transformed into language without reading like a card.
    s.rounded_rectangle((cx - 86, 720, cx + 86, 1500), 44, fill=112)
    d.rounded_rectangle((cx - 86, 720, cx + 86, 1500), 44, outline=236, width=7)
    for y in range(790, 1440, 42):
        width = 46 + int(16 * math.sin(y / 67))
        d.line((cx-width, y, cx+width, y), fill=56 + (y // 42) % 5 * 22, width=2)
    d.ellipse((cx - 24, cy - 24, cx + 24, cy + 24), fill=255)

    # Vertical recursion rails and horizon notches.
    for side in (-1, 1):
        rail_x = cx + side * 395
        d.line((rail_x, 240, rail_x, 2110), fill=34, width=3)
        for y in range(300, 2070, 61):
            length = 16 + ((y // 61) % 6) * 8
            d.line((rail_x - side * length, y, rail_x + side * 4, y), fill=42 + (y // 61) % 5 * 18, width=2)

    # Fine recursion echoes and diagonal fold planes.
    for i in range(16):
        y = 390 + i * 103
        span = 310 - min(230, i * 9)
        d.line((cx-span, y, cx+span, y + (i % 3 - 1) * 24), fill=22 + (i % 5) * 16, width=2)
    d.line((110, 430, cx-130, cy, 140, 1990), fill=32, width=3, joint='curve')
    d.line((W-110, 430, cx+130, cy, W-140, 1990), fill=32, width=3, joint='curve')

    return add_grain(edge_fade(composite(base, soft, detail, 17)), 271828, 8)



def chrono_loom() -> Image.Image:
    """Clockwork time loom: broken dials, woven phase threads, and a suspended escapement."""
    base, soft, detail, s, d = layered_canvas()
    cx, cy = W // 2, 890
    rng = random.Random(314159)

    for index, (rx, ry) in enumerate(((132, 132), (238, 224), (365, 326), (520, 430), (720, 560))):
        d.ellipse((cx-rx, cy-ry, cx+rx, cy+ry), outline=max(16, 210-index*38), width=max(2, 8-index))

    # Broken timing ring and radial ticks.
    for i in range(96):
        angle = math.tau * i / 96 - math.pi / 2
        inner = 260 + 18 * math.sin(i * .71)
        outer = inner + (58 if i % 8 == 0 else 28 if i % 4 == 0 else 13)
        a = polar(cx, cy, inner, angle)
        b = polar(cx, cy, outer, angle)
        d.line((a, b), fill=70 + (i % 8) * 18, width=4 if i % 8 == 0 else 2)

    # Woven phase threads remain legible behind the central semantic dial.
    for i in range(27):
        t = i / 26
        x0 = 95 + t * (W - 190)
        phase = i * .63
        points = []
        for step in range(91):
            u = step / 90
            y = 350 + u * 1580
            envelope = math.sin(math.pi * u) ** .72
            x = x0 + math.sin(u * math.pi * 5.5 + phase) * (58 + 74 * envelope)
            points.append((int(x), int(y)))
        d.line(points, fill=22 + (i % 6) * 15, width=2, joint='curve')

    # Escapement, time hands, and lower calibration architecture.
    s.ellipse((cx-92, cy-92, cx+92, cy+92), fill=120)
    d.ellipse((cx-92, cy-92, cx+92, cy+92), outline=242, width=8)
    d.line((cx, cy, cx + 210, cy - 158), fill=238, width=8)
    d.line((cx, cy, cx - 88, cy - 278), fill=170, width=6)
    d.ellipse((cx-20, cy-20, cx+20, cy+20), fill=255)
    d.line((cx, cy+90, cx, 1910), fill=118, width=5)
    d.rounded_rectangle((cx-108, 1280, cx+108, 2060), 42, outline=96, width=5)
    for y in range(1340, 2010, 47):
        half = 72 + int(21 * math.sin(y / 83))
        d.line((cx-half, y, cx+half, y), fill=34 + (y // 47) % 6 * 18, width=2)
    for _ in range(120):
        angle = rng.random() * math.tau
        radius = rng.uniform(180, 820)
        x, y = polar(cx, cy, radius, angle)
        value = rng.randint(18, 92)
        d.point((x, y), fill=value)

    return add_grain(edge_fade(composite(base, soft, detail, 16)), 314159, 8)


def muon_chamber() -> Image.Image:
    """Particle detector chamber with collision tracks and layered calorimeter geometry."""
    base, soft, detail, s, d = layered_canvas()
    cx, cy = W // 2, 930
    rng = random.Random(10566)

    for index, radius in enumerate((105, 205, 330, 485, 675, 880)):
        d.ellipse((cx-radius, cy-radius*.72, cx+radius, cy+radius*.72), outline=max(12, 220-index*34), width=max(2, 8-index))

    # Collision tracks use curved trajectories rather than icon-like spokes.
    for i in range(46):
        angle = rng.uniform(-math.pi, math.pi)
        length = rng.uniform(260, 880)
        bend = rng.uniform(-.75, .75)
        start = (cx + rng.randint(-18, 18), cy + rng.randint(-18, 18))
        end = (int(cx + math.cos(angle) * length), int(cy + math.sin(angle) * length * .74))
        control = (
            int(cx + math.cos(angle + bend) * length * .48),
            int(cy + math.sin(angle + bend) * length * .42),
        )
        track = bezier(start, control, end, 64)
        d.line(track, fill=rng.randint(42, 176), width=rng.choice((1, 2, 2, 3)), joint='curve')
        if i % 3 == 0:
            for point in track[10::12]:
                x, y = point
                d.ellipse((x-3, y-3, x+3, y+3), fill=rng.randint(105, 220))

    # Chamber plates and readout stack.
    for side in (-1, 1):
        x = cx + side * 410
        d.line((x, 390, x, 1740), fill=82, width=4)
        for y in range(430, 1710, 62):
            length = 34 + ((y // 62) % 6) * 10
            d.line((x-side*length, y, x+side*8, y), fill=36 + (y // 62) % 5 * 22, width=2)
    d.rounded_rectangle((cx-150, 1370, cx+150, 2110), 46, outline=108, width=6)
    for y in range(1440, 2040, 42):
        for x in range(cx-112, cx+113, 32):
            value = 28 + ((x + y) // 17) % 7 * 20
            d.rectangle((x-6, y-5, x+6, y+5), fill=value)
    s.ellipse((cx-55, cy-55, cx+55, cy+55), fill=180)
    d.ellipse((cx-20, cy-20, cx+20, cy+20), fill=255)

    return add_grain(edge_fade(composite(base, soft, detail, 15)), 10566, 8)


def vector_shrine() -> Image.Image:
    """Asymmetric cyber shrine with vector rails and a descending telemetry cascade."""
    base, soft, detail, s, d = layered_canvas()
    cx = W // 2

    shell = [(cx, 250), (cx+300, 540), (cx+238, 1940), (cx, 2190), (cx-330, 1940), (cx-260, 540)]
    s.polygon(shell, fill=32)
    d.line(shell + [shell[0]], fill=145, width=8, joint='curve')

    # Offset inner frames avoid another centered rectangular monolith.
    for depth in range(8):
        inset = depth * 30
        offset = int(math.sin(depth * .9) * 46)
        frame = [
            (cx-220+inset+offset, 500+inset*2),
            (cx+245-inset+offset, 500+inset),
            (cx+185-inset-offset, 1850-inset),
            (cx-245+inset-offset, 1850-inset*2),
        ]
        d.line(frame + [frame[0]], fill=48 + depth * 20, width=max(2, 7-depth//2), joint='curve')

    # Vector rails and telemetry steps.
    for side in (-1, 1):
        x = cx + side * 360
        d.line((x, 360, x, 2070), fill=42, width=3)
        for i, y in enumerate(range(420, 2020, 58)):
            length = 24 + (i % 7) * 11
            d.line((x-side*length, y, x+side*5, y), fill=44 + (i % 6) * 18, width=2)
    for row, y in enumerate(range(680, 1760, 54)):
        shift = int(math.sin(row * .74) * 68)
        half = 85 + (row % 5) * 17
        d.line((cx-half+shift, y, cx+half+shift, y), fill=46 + (row % 7) * 20, width=2)
        if row % 3 == 0:
            d.line((cx+shift, y-26, cx+shift, y+26), fill=118, width=3)

    s.rounded_rectangle((cx-76, 750, cx+76, 1560), 36, fill=98)
    d.rounded_rectangle((cx-76, 750, cx+76, 1560), 36, outline=232, width=7)
    d.ellipse((cx-22, 1080, cx+22, 1124), fill=255)
    d.line((cx, 300, cx, 2140), fill=84, width=4)

    return add_grain(edge_fade(composite(base, soft, detail, 17)), 424242, 8)


def lagrange_garden() -> Image.Image:
    """Orbital mechanics rendered as a living garden around five stable Lagrange nodes."""
    base, soft, detail, s, d = layered_canvas()
    cx, cy = W // 2, 900
    rng = random.Random(515151)

    for rx, ry, fill, width in ((170, 115, 220, 7), (310, 205, 120, 5), (500, 330, 58, 3), (730, 470, 26, 2)):
        d.ellipse((cx-rx, cy-ry, cx+rx, cy+ry), outline=fill, width=width)

    nodes = [
        (cx-350, cy, 'L3'),
        (cx+350, cy, 'L2'),
        (cx+130, cy, 'L1'),
        (cx+115, cy-270, 'L4'),
        (cx+115, cy+270, 'L5'),
    ]
    for index, (x, y, _label) in enumerate(nodes):
        radius = 35 if index < 3 else 48
        d.ellipse((x-radius, y-radius, x+radius, y+radius), outline=170 + index * 14, width=6)
        d.ellipse((x-8, y-8, x+8, y+8), fill=245)
        for petal in range(7):
            angle = math.tau * petal / 7 + index * .31
            px, py = polar(x, y, radius + 32, angle)
            d.ellipse((px-12, py-24, px+12, py+24), outline=54 + petal * 13, width=2)

    # Transfer orbits and botanical root trajectories.
    for i in range(21):
        angle = -1.15 + i * 2.3 / 20
        start = polar(cx, cy, 120, angle)
        end = (int(cx + math.sin(angle) * (360 + i * 18)), int(1320 + i * 34))
        control = (int(cx + math.sin(angle * 1.7) * 360), int(1040 + i * 24))
        d.line(bezier(start, control, end, 56), fill=28 + (i % 6) * 18, width=2, joint='curve')
    for _ in range(110):
        x = rng.randint(60, W-60)
        y = rng.randint(300, 2150)
        value = rng.randint(20, 92)
        d.point((x, y), fill=value)

    s.ellipse((cx-78, cy-78, cx+78, cy+78), fill=150)
    d.ellipse((cx-78, cy-78, cx+78, cy+78), outline=230, width=7)
    d.ellipse((cx-18, cy-18, cx+18, cy+18), fill=255)
    d.line((cx, 1080, cx, 2080), fill=70, width=4)

    return add_grain(edge_fade(composite(base, soft, detail, 16)), 515151, 8)

def save(name: str, img: Image.Image, compress_level: int = 6):
    OUT_WEB.mkdir(parents=True, exist_ok=True)
    OUT_ANDROID.mkdir(parents=True, exist_ok=True)
    img.save(OUT_WEB / f"scene_{name}.png", compress_level=compress_level)
    img.save(OUT_ANDROID / f"scene_{name}.png", compress_level=compress_level)


SCENES = {
    "sentinel": sentinel,
    "moth": moth,
    "orbit": orbit,
    "neural_halo": neural_halo,
    "cipher_cathedral": cipher_cathedral,
    "quantum_lattice": quantum_lattice,
    "fusion_core": fusion_core,
    "packet_bloom": packet_bloom,
    "event_horizon": event_horizon,
    "tesseract_engine": tesseract_engine,
    "helix_array": helix_array,
    "interference_field": interference_field,
    "cryo_vault": cryo_vault,
    "dyson_relay": dyson_relay,
    "spectral_observatory": spectral_observatory,
    "recursive_monolith": recursive_monolith,
    "chrono_loom": chrono_loom,
    "muon_chamber": muon_chamber,
    "vector_shrine": vector_shrine,
    "lagrange_garden": lagrange_garden,
}


def render_and_save(name: str, compress_level: int) -> str:
    save(name, SCENES[name](), compress_level=compress_level)
    return name


def thumbnail(compress_level: int = 6):
    source = OUT_WEB / "scene_sentinel.png"
    if not source.exists():
        return
    src = Image.open(source).resize((220, 488), Image.Resampling.LANCZOS)
    out = Image.new("RGB", (512, 512), "black")
    arr = np.asarray(src)
    draw = ImageDraw.Draw(out)
    ramp = " .:;+=x#@"
    for y in range(0, 488, 6):
        for x in range(0, 220, 6):
            value = int(arr[y:y+6, x:x+6].mean())
            if value < 16:
                continue
            glyph = ramp[min(len(ramp)-1, int(value / 256 * len(ramp)))]
            draw.text((146+x, 12+y), glyph, fill=(value, value, min(255, value+12)))
    path = ROOT / "apps/android/app/src/main/res/drawable/wallpaper_thumbnail.png"
    out.save(path, compress_level=compress_level)
    out.save(OUT_WEB / "wallpaper_thumbnail.png", compress_level=compress_level)


def parse_selection(values: list[str] | None) -> list[str]:
    if not values:
        return list(SCENES)
    selected: list[str] = []
    for raw in values:
        for name in raw.split(','):
            clean = name.strip()
            if not clean:
                continue
            if clean not in SCENES:
                raise SystemExit(f"unknown scene {clean!r}; available: {', '.join(SCENES)}")
            if clean not in selected:
                selected.append(clean)
    return selected


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--only', action='append', help='scene name or comma-separated scene names')
    parser.add_argument('--jobs', type=int, default=min(3, max(1, os.cpu_count() or 1)))
    parser.add_argument('--skip-existing', action='store_true')
    parser.add_argument('--compress-level', type=int, default=6, choices=range(0, 10))
    args = parser.parse_args()

    selected = parse_selection(args.only)
    if args.skip_existing:
        selected = [
            name for name in selected
            if not (OUT_WEB / f"scene_{name}.png").exists()
            or not (OUT_ANDROID / f"scene_{name}.png").exists()
        ]

    if args.jobs <= 1 or len(selected) <= 1:
        for name in selected:
            render_and_save(name, args.compress_level)
            print(f"generated {name}", flush=True)
    else:
        with ProcessPoolExecutor(max_workers=min(args.jobs, len(selected))) as executor:
            futures = {
                executor.submit(render_and_save, name, args.compress_level): name
                for name in selected
            }
            for future in as_completed(futures):
                print(f"generated {future.result()}", flush=True)

    thumbnail(args.compress_level)
    print(f"generated {len(selected)} scene(s)")


if __name__ == "__main__":
    main()
