#!/usr/bin/env python3
"""Build a deterministic contact sheet of every procedural source mask."""
from pathlib import Path
import re
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "apps/visual-lab/src/main.ts"
ASSETS = ROOT / "apps/visual-lab/public/assets"
OUTPUT = ROOT / "build/review/glyphlock-mask-contact-sheet.png"

text = SOURCE.read_text(encoding="utf-8")
themes = re.findall(r"\{\s*id:\s*'([a-z0-9_]+)'\s*,\s*label:\s*'([^']+)'", text)
if not themes:
    raise SystemExit("no themes found")

columns = 4
cell_w, image_h, label_h = 300, 666, 40
rows = (len(themes) + columns - 1) // columns
sheet = Image.new("RGB", (columns * cell_w, rows * (image_h + label_h)), (6, 8, 10))
draw = ImageDraw.Draw(sheet)
font = ImageFont.load_default()
for index, (theme_id, label) in enumerate(themes):
    path = ASSETS / f"scene_{theme_id}.png"
    if not path.exists():
        raise SystemExit(f"missing generated mask: {path}")
    image = Image.open(path).convert("RGB")
    image.thumbnail((cell_w - 18, image_h - 12), Image.Resampling.LANCZOS)
    col, row = index % columns, index // columns
    x = col * cell_w + (cell_w - image.width) // 2
    y = row * (image_h + label_h) + label_h
    sheet.paste(image, (x, y))
    draw.text((col * cell_w + 12, row * (image_h + label_h) + 12), label.upper(), fill=(188, 204, 214), font=font)
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
sheet.save(OUTPUT, optimize=True)
print(OUTPUT)
