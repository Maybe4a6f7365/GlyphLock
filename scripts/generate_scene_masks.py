#!/usr/bin/env python3
"""Generate original grayscale source masks for the GlyphLock prototype."""
from __future__ import annotations

from pathlib import Path
import math
import random
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


def save(name: str, img: Image.Image):
    OUT_WEB.mkdir(parents=True, exist_ok=True)
    OUT_ANDROID.mkdir(parents=True, exist_ok=True)
    img.save(OUT_WEB/f"scene_{name}.png", optimize=True)
    img.save(OUT_ANDROID/f"scene_{name}.png", optimize=True)


def thumbnail():
    src = Image.open(OUT_WEB/"scene_sentinel.png").resize((220,488), Image.Resampling.LANCZOS)
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
    p=ROOT/"apps/android/app/src/main/res/drawable/wallpaper_thumbnail.png"
    out.save(p,optimize=True)
    out.save(OUT_WEB/"wallpaper_thumbnail.png", optimize=True)


def main():
    save("sentinel", sentinel())
    save("moth", moth())
    save("orbit", orbit())
    save("neural_halo", neural_halo())
    save("cipher_cathedral", cipher_cathedral())
    save("quantum_lattice", quantum_lattice())
    save("fusion_core", fusion_core())
    save("packet_bloom", packet_bloom())
    thumbnail()
    print("generated")

if __name__ == "__main__": main()
