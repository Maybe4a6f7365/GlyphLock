#!/usr/bin/env python3
"""Verify that Android, browser, and generated scene catalogues stay in lockstep."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

generator = (ROOT / "scripts/generate_scene_masks.py").read_text(encoding="utf-8")
android = (ROOT / "apps/android/app/src/main/java/dev/glyphlock/wallpaper/DemoCatalog.java").read_text(encoding="utf-8")
web = (ROOT / "apps/visual-lab/src/main.ts").read_text(encoding="utf-8")

generated = set(re.findall(r'save\("([a-z0-9_]+)"\s*,', generator))
android_masks = set(re.findall(r'R\.drawable\.scene_([a-z0-9_]+)', android))
web_themes = set(re.findall(r"\{\s*id:\s*'([a-z0-9_]+)'\s*,\s*label:", web))

if generated != android_masks or generated != web_themes:
    raise SystemExit(
        "theme catalogue mismatch\n"
        f"generated only: {sorted(generated - android_masks - web_themes)}\n"
        f"android only: {sorted(android_masks - generated)}\n"
        f"web only: {sorted(web_themes - generated)}"
    )

if len(generated) < 14:
    raise SystemExit(f"expected at least 14 themes, found {len(generated)}")

print(f"theme catalogues aligned: {len(generated)} themes")
