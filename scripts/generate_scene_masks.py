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
BLACK_CUTOFF = 10
GRAIN_DILATION = 15
ACTIVE_CUTOFF = 20
CLOCK_SAFE_BOUNDS = (
    round(W * 0.135),
    0,
    round(W * 0.865),
    round(H * 0.130),
)
GESTURE_SAFE_BOUNDS = (
    round(W * 0.055),
    round(H * 0.925),
    round(W * 0.945),
    H,
)


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
    """Add organic grain only around artwork and preserve literal black elsewhere."""
    if img.size != (W, H):
        raise ValueError(f"expected {(W, H)} mask, received {img.size}")
    active = img.point(lambda value: 255 if value >= BLACK_CUTOFF else 0)
    # Gaussian support expansion is separable and substantially cheaper than a
    # large MaxFilter at 1080x2400; thresholding restores a binary dilation.
    support = active.filter(ImageFilter.GaussianBlur(GRAIN_DILATION / 3))
    support = support.point(lambda value: 255 if value > 0 else 0)
    bounds = support.getbbox()
    if bounds is None:
        return Image.new("L", (W, H), 0)

    rng = np.random.default_rng(seed)
    crop = img.crop(bounds)
    support_arr = np.asarray(support.crop(bounds), dtype=np.uint8) > 0
    arr = np.asarray(crop, dtype=np.int16).copy()
    noise = rng.normal(0, amount, arr.shape).astype(np.int16)
    # Organic low-frequency density drift.
    crop_w, crop_h = crop.size
    low_h = max(4, round(60 * crop_h / H))
    low_w = max(4, round(27 * crop_w / W))
    small = rng.integers(0, 256, (low_h, low_w), dtype=np.uint8)
    low = Image.fromarray(small, "L").resize(crop.size, Image.Resampling.BICUBIC).filter(ImageFilter.GaussianBlur(28))
    low_arr = np.asarray(low, dtype=np.int16) - 128
    arr[support_arr] += noise[support_arr] + low_arr[support_arr] // 10
    arr[~support_arr] = 0
    arr = np.clip(arr, 0, 255)
    arr[arr < BLACK_CUTOFF] = 0
    result = Image.new("L", (W, H), 0)
    result.paste(Image.fromarray(arr.astype(np.uint8), "L"), bounds[:2])
    return result


def layered_canvas():
    base = Image.new("L", (W, H), 0)
    soft = Image.new("L", (W, H), 0)
    detail = Image.new("L", (W, H), 0)
    return base, soft, detail, ImageDraw.Draw(soft), ImageDraw.Draw(detail)


def composite(base, soft, detail, blur=14):
    soft = soft.filter(ImageFilter.GaussianBlur(blur))
    base = ImageChops.lighter(base, soft)
    return ImageChops.lighter(base, detail)


def _max_translated(
    destination: np.ndarray,
    source: np.ndarray,
    selected: np.ndarray,
    dx: int,
    dy: int,
    alpha: float,
) -> None:
    """Composite selected translated pixels without allocating a full-size copy."""
    src_x0 = max(0, -dx)
    src_x1 = min(source.shape[1], source.shape[1] - dx)
    src_y0 = max(0, -dy)
    src_y1 = min(source.shape[0], source.shape[0] - dy)
    if src_x1 <= src_x0 or src_y1 <= src_y0:
        return
    dst_x0 = src_x0 + dx
    dst_x1 = src_x1 + dx
    dst_y0 = src_y0 + dy
    dst_y1 = src_y1 + dy
    source_slice = source[src_y0:src_y1, src_x0:src_x1]
    selected_slice = selected[src_y0:src_y1, src_x0:src_x1]
    candidate = np.where(selected_slice, source_slice * alpha, 0.0)
    target = destination[dst_y0:dst_y1, dst_x0:dst_x1]
    np.maximum(target, candidate, out=target)


def glitch_dissolve(
    img: Image.Image,
    seed: int,
    *,
    amount: float = 0.34,
    core_threshold: int = 150,
    edge_radius: float = 8.0,
    block_size: tuple[int, int] = (42, 12),
    scan_height: int = 7,
    direction: tuple[float, float] = (1.0, 0.18),
    fragment_shift: int = 28,
) -> Image.Image:
    """Break weak contours into deterministic scan blocks while retaining bright mass.

    Fragment probability is driven primarily by a blurred edge gradient, with a
    smaller bias toward weak luminance.  Thick, bright regions survive as visual
    landmarks; removed edge pieces are re-emitted down the supplied direction so
    the dissolve reads as motion rather than random erosion.
    """
    if img.size != (W, H):
        raise ValueError(f"expected {(W, H)} mask, received {img.size}")
    amount = float(np.clip(amount, 0.0, 1.0))
    block_w = max(4, int(block_size[0]))
    block_h = max(3, int(block_size[1]))
    scan_height = max(2, int(scan_height))
    rng = np.random.default_rng(seed)

    active_mask = img.point(lambda value: 255 if value > 7 else 0)
    active_bounds = active_mask.getbbox()
    if active_bounds is None:
        return img.copy()
    padding = max(block_w, fragment_shift + 4, math.ceil(edge_radius * 4))
    left = max(0, active_bounds[0] - padding)
    top = max(0, active_bounds[1] - padding)
    right = min(W, active_bounds[2] + padding)
    bottom = min(H, active_bounds[3] + padding)
    bounds = (left, top, right, bottom)
    crop = img.crop(bounds)
    crop_w, crop_h = crop.size

    # Work only within the padded artwork bounds. The former full-canvas float
    # fields multiplied peak memory when three scene workers ran concurrently.
    arr = np.asarray(crop, dtype=np.float32)
    smooth = np.asarray(
        crop.filter(ImageFilter.GaussianBlur(max(0.5, edge_radius))),
        dtype=np.float32,
    )
    grad_y, edge = np.gradient(smooth)
    np.hypot(edge, grad_y, out=edge)
    del grad_y
    active = arr > 7.0
    scale = max(1.0, float(np.percentile(edge[active], 88)))
    edge /= scale
    np.clip(edge, 0.0, 1.0, out=edge)
    smooth /= max(1.0, float(core_threshold))
    np.clip(smooth, 0.0, 1.0, out=smooth)

    # A coarse 2-D field creates rectangular breaks; a shallow 1-D field turns
    # some of those breaks into scanline runs without making every row uniform.
    grid_h = math.ceil(crop_h / block_h)
    grid_w = math.ceil(crop_w / block_w)
    coarse = rng.integers(0, 256, (grid_h, grid_w), dtype=np.uint8)
    trigger = np.asarray(
        Image.fromarray(coarse, "L").resize(crop.size, Image.Resampling.NEAREST),
        dtype=np.float32,
    )
    trigger *= 0.78 / 255.0
    scan = rng.integers(0, 256, (math.ceil(crop_h / scan_height), 1), dtype=np.uint8)
    scanlines = np.asarray(
        Image.fromarray(scan, "L").resize(crop.size, Image.Resampling.NEAREST),
        dtype=np.float32,
    )
    trigger += scanlines * (0.22 / 255.0)
    del scanlines

    # Reuse the gradient and normalized blur buffers for probability and
    # weakness instead of retaining separate full-resolution float arrays.
    edge *= 0.70
    smooth *= -0.22
    smooth += 0.22
    edge += smooth
    edge += 0.08
    edge *= amount
    # Protect only genuinely thick bright areas. Thin bright contours still have
    # a low neighbourhood average and are allowed to fracture at their edges.
    protected_core = (arr >= core_threshold) & (smooth <= 0.22 * (1.0 - 0.72))
    removed = active & ~protected_core & (trigger < edge)

    dissolved = arr.copy()
    dissolved[removed] *= 0.10
    vector_length = max(1e-6, math.hypot(direction[0], direction[1]))
    ux, uy = direction[0] / vector_length, direction[1] / vector_length
    for fraction, alpha in ((0.55, 0.42), (1.0, 0.25)):
        dx = int(round(ux * fragment_shift * fraction))
        dy = int(round(uy * fragment_shift * fraction))
        _max_translated(dissolved, arr, removed, dx, dy, alpha)

    result = img.copy()
    result.paste(Image.fromarray(np.uint8(np.clip(dissolved, 0, 255)), "L"), bounds[:2])
    return result


def add_directional_wake(
    img: Image.Image,
    seed: int,
    start: tuple[int, int],
    control: tuple[int, int],
    end: tuple[int, int],
    *,
    count: int = 54,
    spread: float = 54.0,
    length_range: tuple[int, int] = (18, 92),
    value_range: tuple[int, int] = (24, 112),
    width_range: tuple[int, int] = (1, 3),
) -> Image.Image:
    """Add sparse tapered fragments along a quadratic ribbon trajectory."""
    if img.size != (W, H):
        raise ValueError(f"expected {(W, H)} mask, received {img.size}")
    rng = random.Random(seed)
    wake = Image.new("L", (W, H), 0)
    glow = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(wake)
    g = ImageDraw.Draw(glow)
    minimum_length, maximum_length = sorted(length_range)
    minimum_value, maximum_value = sorted(value_range)
    minimum_width, maximum_width = sorted(width_range)

    for index in range(max(0, count)):
        # Stratified jitter leaves intentional gaps while avoiding accidental
        # clusters that would turn the wake into another solid band.
        t = np.clip((index + rng.random()) / max(1, count), 0.0, 1.0)
        u = 1.0 - t
        x = u*u*start[0] + 2*u*t*control[0] + t*t*end[0]
        y = u*u*start[1] + 2*u*t*control[1] + t*t*end[1]
        tx = 2*u*(control[0] - start[0]) + 2*t*(end[0] - control[0])
        ty = 2*u*(control[1] - start[1]) + 2*t*(end[1] - control[1])
        tangent_length = max(1e-6, math.hypot(tx, ty))
        tx, ty = tx / tangent_length, ty / tangent_length
        nx, ny = -ty, tx
        lane_spread = spread * (0.32 + 0.68 * t)
        offset = rng.gauss(0.0, lane_spread * 0.42)
        x += nx * offset + tx * rng.uniform(-10.0, 10.0)
        y += ny * offset + ty * rng.uniform(-10.0, 10.0)

        segment_length = rng.randint(minimum_length, maximum_length) * (0.58 + t * 0.70)
        angle_jitter = rng.uniform(-0.20, 0.20)
        ca, sa = math.cos(angle_jitter), math.sin(angle_jitter)
        dx = (tx * ca - ty * sa) * segment_length
        dy = (tx * sa + ty * ca) * segment_length
        x0, y0 = int(x - dx * 0.36), int(y - dy * 0.36)
        x1, y1 = int(x + dx * 0.64), int(y + dy * 0.64)
        value = int(rng.randint(minimum_value, maximum_value) * (1.0 - t * 0.42))
        width = rng.randint(minimum_width, maximum_width)
        d.line((x0, y0, x1, y1), fill=max(10, value), width=width)
        g.line((x0, y0, x1, y1), fill=max(5, value // 4), width=width + 4)
        if rng.random() < 0.30:
            radius = rng.randint(2, 6)
            d.rectangle((int(x)-radius, int(y)-radius, int(x)+radius, int(y)+radius), fill=min(190, value + 28))

    glow = glow.filter(ImageFilter.GaussianBlur(5))
    return ImageChops.lighter(img, ImageChops.lighter(glow, wake))


def carve_quiet_zones(
    img: Image.Image,
    zones: list[tuple[str, tuple[int, int, int, int], float, float]] | None = None,
    *,
    preserve_safe_areas: bool = True,
) -> Image.Image:
    """Attenuate seeded composition zones after grain so black remains quiet."""
    if img.size != (W, H):
        raise ValueError(f"expected {(W, H)} mask, received {img.size}")
    all_zones: list[tuple[str, tuple[int, int, int, int], float, float]] = []
    if preserve_safe_areas:
        # Central clock and bottom gesture/navigation regions.
        all_zones.extend([
            ("rounded", (145, -100, W - 145, 305), 0.94, 72.0),
            ("rounded", (60, H - 175, W - 60, H + 80), 0.92, 58.0),
        ])
    all_zones.extend(zones or [])

    attenuation = Image.new("L", (W, H), 255)
    for shape, bounds, strength, feather in all_zones:
        cut = Image.new("L", (W, H), 0)
        draw = ImageDraw.Draw(cut)
        fill = int(round(np.clip(strength, 0.0, 1.0) * 255))
        if shape == "ellipse":
            draw.ellipse(bounds, fill=fill)
        elif shape == "rect":
            draw.rectangle(bounds, fill=fill)
        elif shape == "rounded":
            radius = max(18, min(130, int(min(abs(bounds[2]-bounds[0]), abs(bounds[3]-bounds[1])) * 0.22)))
            draw.rounded_rectangle(bounds, radius=radius, fill=fill)
        else:
            raise ValueError(f"unknown quiet-zone shape {shape!r}")
        if feather > 0:
            cut = cut.filter(ImageFilter.GaussianBlur(feather))
        attenuation = ImageChops.multiply(attenuation, ImageChops.invert(cut))
    result = ImageChops.multiply(img, attenuation)
    # This helper runs after grain on flagship compositions, so attenuation must
    # not reintroduce near-black values that bypass the shared true-black gate.
    return result.point(lambda value: 0 if value < BLACK_CUTOFF else value)


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
        for i in range(16):
            p = i / 15
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
    img = glitch_dissolve(
        img, 13371, amount=.40, core_threshold=138, edge_radius=7,
        block_size=(40, 11), scan_height=6, direction=(.58, 1.0), fragment_shift=34,
    )
    # Broken wing lanes and a single asymmetric body wake give the static mask a
    # clear direction while leaving the head and torso as stable landmarks.
    img = add_directional_wake(
        img, 13372, (490, 650), (270, 510), (42, 1200),
        count=34, spread=48, length_range=(20, 105), value_range=(26, 122),
    )
    img = add_directional_wake(
        img, 13373, (590, 720), (810, 660), (1018, 1390),
        count=29, spread=42, length_range=(18, 96), value_range=(24, 108),
    )
    img = add_directional_wake(
        img, 13374, (650, 1080), (710, 1560), (870, 2130),
        count=24, spread=38, length_range=(16, 74), value_range=(20, 82),
    )
    img = add_grain(img, 1337, 9)
    return carve_quiet_zones(img, [
        ("ellipse", (-390, 930, 300, 2440), .78, 105),
        ("ellipse", (785, 870, 1480, 2440), .74, 105),
    ])


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
        for i in range(12):
            p = i/11
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
    img = glitch_dissolve(
        img, 22041, amount=.43, core_threshold=140, edge_radius=6,
        block_size=(46, 10), scan_height=6, direction=(-.34, 1.0), fragment_shift=30,
    )
    img = add_directional_wake(
        img, 22042, (500, 860), (270, 650), (45, 1240),
        count=35, spread=52, length_range=(18, 96), value_range=(24, 114),
    )
    img = add_directional_wake(
        img, 22043, (580, 920), (815, 825), (1035, 1510),
        count=28, spread=46, length_range=(16, 88), value_range=(22, 102),
    )
    img = add_directional_wake(
        img, 22044, (510, 1370), (430, 1720), (315, 2070),
        count=20, spread=34, length_range=(14, 70), value_range=(18, 76),
    )
    img = add_grain(img, 2204, 10)
    return carve_quiet_zones(img, [
        ("ellipse", (-330, 1640, 370, 2490), .76, 95),
        ("ellipse", (710, 1590, 1410, 2490), .82, 95),
    ])


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

    # Two offset emitters retain the interference identity, but broken arcs leave
    # broad nulls instead of coating the entire canvas with complete circles.
    emitters = [(cx - 205, cy - 52), (cx + 220, cy + 72)]
    for emitter_index, (ex, ey) in enumerate(emitters):
        for ring, r in enumerate(range(48, 690, 47)):
            phase = ring % 6
            fill = 18 + phase * 14
            width = 1 + phase // 4
            box = (ex - r, ey - r, ex + r, ey + r)
            offset = (ring * 19 + emitter_index * 43) % 46
            d.arc(box, 192 + offset // 4, 326 + offset // 3, fill=fill, width=width)
            d.arc(box, 12 + offset // 5, 126 + offset // 4, fill=max(12, fill - 10), width=width)

    # Bright loci are restricted to two crossing directional corridors.
    for y in range(260, 2100, 22):
        for x in range(70, W - 70, 22):
            phase = 0.0
            for ex, ey in emitters:
                phase += math.sin(math.hypot(x - ex, y - ey) / 22.0)
            diagonal_a = abs(y - (390 + x * .92))
            diagonal_b = abs(y - (1550 - x * .78))
            corridor = min(diagonal_a, diagonal_b)
            if phase > 1.47 and corridor < 285:
                value = int(min(230, 72 + phase * 48))
                d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=value)

    # A stable phase-lock core anchors the field; secondary rings are incomplete.
    d.ellipse((cx - 92, cy - 92, cx + 92, cy + 92), outline=242, width=7)
    d.arc((cx - 190, cy - 190, cx + 190, cy + 190), 198, 356, fill=150, width=5)
    d.arc((cx - 315, cy - 315, cx + 315, cy + 315), 16, 168, fill=65, width=3)
    d.line((cx - 330, cy + 300, cx + 350, cy - 280), fill=30, width=2)
    d.ellipse((cx - 24, cy - 24, cx + 24, cy + 24), fill=255)

    img = edge_fade(composite(base, soft, detail, 12))
    img = glitch_dissolve(
        img, 51501, amount=.46, core_threshold=188, edge_radius=5,
        block_size=(52, 9), scan_height=5, direction=(1.0, .24), fragment_shift=38,
    )
    img = add_directional_wake(
        img, 51502, (90, 430), (490, 665), (1030, 1340),
        count=58, spread=72, length_range=(20, 118), value_range=(22, 106),
    )
    img = add_directional_wake(
        img, 51503, (35, 1550), (520, 1110), (1045, 520),
        count=52, spread=64, length_range=(18, 106), value_range=(20, 96),
    )
    img = add_grain(img, 5150, 7)
    return carve_quiet_zones(img, [
        ("ellipse", (-420, 1090, 455, 2520), .88, 125),
        ("ellipse", (650, -230, 1450, 690), .84, 105),
        ("rounded", (20, 1760, 420, 2290), .72, 90),
    ])


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
    for path in (
        OUT_WEB / f"scene_{name}.png",
        OUT_ANDROID / f"scene_{name}.png",
    ):
        temporary = path.with_name(f".{path.name}.{os.getpid()}.tmp")
        try:
            img.save(temporary, format="PNG", compress_level=compress_level)
            temporary.replace(path)
        finally:
            temporary.unlink(missing_ok=True)


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


DEFAULT_VALIDATION_LIMITS = {
    "min_black_ratio": 0.58,
    "min_quiet_ratio": 0.60,
    "min_active_ratio": 0.025,
    "max_clock_active_ratio": 0.040,
    "max_gesture_active_ratio": 0.025,
    "min_peak": 180,
}

# The flagship figure silhouettes deliberately carry more contiguous mass than
# the technical line systems. Recursive Monolith's source pillar intentionally
# enters the clock region, while Interference Field is deliberately sparse.
VALIDATION_OVERRIDES = {
    "sentinel": {
        "min_black_ratio": 0.50,
        "min_quiet_ratio": 0.50,
        "max_gesture_active_ratio": 0.050,
    },
    "moth": {
        "min_black_ratio": 0.54,
        "min_quiet_ratio": 0.54,
    },
    "recursive_monolith": {
        "min_black_ratio": 0.50,
        "min_quiet_ratio": 0.50,
        "max_clock_active_ratio": 0.150,
    },
    "interference_field": {
        "min_active_ratio": 0.015,
    },
}


def mask_metrics(img: Image.Image) -> dict[str, float]:
    """Return fixed, inexpensive composition metrics for a generated mask."""
    if img.mode != "L" or img.size != (W, H):
        raise ValueError(
            f"expected L/{W}x{H} mask, "
            f"received {img.mode}/{img.size[0]}x{img.size[1]}"
        )
    arr = np.asarray(img, dtype=np.uint8)
    clock = arr[
        CLOCK_SAFE_BOUNDS[1]:CLOCK_SAFE_BOUNDS[3],
        CLOCK_SAFE_BOUNDS[0]:CLOCK_SAFE_BOUNDS[2],
    ]
    gesture = arr[
        GESTURE_SAFE_BOUNDS[1]:GESTURE_SAFE_BOUNDS[3],
        GESTURE_SAFE_BOUNDS[0]:GESTURE_SAFE_BOUNDS[2],
    ]
    return {
        "black_ratio": float(np.mean(arr == 0)),
        "quiet_ratio": float(np.mean(arr <= BLACK_CUTOFF)),
        "active_ratio": float(np.mean(arr > ACTIVE_CUTOFF)),
        "clock_active_ratio": float(np.mean(clock > ACTIVE_CUTOFF)),
        "gesture_active_ratio": float(np.mean(gesture > ACTIVE_CUTOFF)),
        "peak": float(arr.max()),
    }


def validate_mask(name: str, img: Image.Image) -> dict[str, float]:
    """Fail deterministically when a mask loses black space or usable sources."""
    metrics = mask_metrics(img)
    limits = DEFAULT_VALIDATION_LIMITS | VALIDATION_OVERRIDES.get(name, {})
    failures: list[str] = []
    for metric, limit_name in (
        ("black_ratio", "min_black_ratio"),
        ("quiet_ratio", "min_quiet_ratio"),
        ("active_ratio", "min_active_ratio"),
        ("peak", "min_peak"),
    ):
        if metrics[metric] < limits[limit_name]:
            failures.append(f"{metric}={metrics[metric]:.4f} < {limits[limit_name]:.4f}")
    for metric, limit_name in (
        ("clock_active_ratio", "max_clock_active_ratio"),
        ("gesture_active_ratio", "max_gesture_active_ratio"),
    ):
        if metrics[metric] > limits[limit_name]:
            failures.append(f"{metric}={metrics[metric]:.4f} > {limits[limit_name]:.4f}")
    if failures:
        raise ValueError(f"{name} mask validation failed: " + "; ".join(failures))
    return metrics


def format_metrics(metrics: dict[str, float]) -> str:
    return (
        f"black={metrics['black_ratio']:.3f} "
        f"quiet={metrics['quiet_ratio']:.3f} "
        f"active={metrics['active_ratio']:.3f} "
        f"clock={metrics['clock_active_ratio']:.3f} "
        f"gesture={metrics['gesture_active_ratio']:.3f} "
        f"peak={metrics['peak']:.0f}"
    )


def render_and_save(name: str, compress_level: int) -> tuple[str, dict[str, float]]:
    img = SCENES[name]()
    metrics = validate_mask(name, img)
    save(name, img, compress_level=compress_level)
    return name, metrics


def validate_saved(name: str) -> tuple[str, dict[str, float]]:
    web_path = OUT_WEB / f"scene_{name}.png"
    android_path = OUT_ANDROID / f"scene_{name}.png"
    for path in (web_path, android_path):
        if not path.exists():
            raise FileNotFoundError(f"missing generated mask: {path.relative_to(ROOT)}")
    with Image.open(web_path) as source:
        source.load()
        metrics = validate_mask(name, source)
    with Image.open(android_path) as source:
        source.load()
        validate_mask(name, source)
    if web_path.read_bytes() != android_path.read_bytes():
        raise ValueError(f"{name} web and Android masks are not byte-identical")
    return name, metrics


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
    parser.add_argument('--jobs', type=int, default=min(2, max(1, os.cpu_count() or 1)))
    parser.add_argument('--skip-existing', action='store_true')
    parser.add_argument('--validate-only', action='store_true', help='validate saved masks without regenerating')
    parser.add_argument('--compress-level', type=int, default=6, choices=range(0, 10))
    args = parser.parse_args()

    selected = parse_selection(args.only)
    if args.validate_only:
        for name in selected:
            validated_name, metrics = validate_saved(name)
            print(f"validated {validated_name} {format_metrics(metrics)}", flush=True)
        print(f"validated {len(selected)} scene(s)")
        return

    if args.skip_existing:
        selected = [
            name for name in selected
            if not (OUT_WEB / f"scene_{name}.png").exists()
            or not (OUT_ANDROID / f"scene_{name}.png").exists()
        ]

    if args.jobs <= 1 or len(selected) <= 1:
        for name in selected:
            rendered_name, metrics = render_and_save(name, args.compress_level)
            print(f"generated {rendered_name} {format_metrics(metrics)}", flush=True)
    else:
        with ProcessPoolExecutor(max_workers=min(args.jobs, len(selected))) as executor:
            futures = {
                executor.submit(render_and_save, name, args.compress_level): name
                for name in selected
            }
            for future in as_completed(futures):
                rendered_name, metrics = future.result()
                print(f"generated {rendered_name} {format_metrics(metrics)}", flush=True)

    thumbnail(args.compress_level)
    print(f"generated {len(selected)} scene(s)")


if __name__ == "__main__":
    main()
