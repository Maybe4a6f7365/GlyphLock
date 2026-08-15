#!/usr/bin/env python3
"""Verify Android, browser, and generated scene catalogues stay in lockstep."""
from pathlib import Path
import ast
import re

ROOT = Path(__file__).resolve().parents[1]
GENERATOR_PATH = ROOT / "scripts/generate_scene_masks.py"
ANDROID_PATH = ROOT / "apps/android/app/src/main/java/dev/glyphlock/wallpaper/DemoCatalog.java"
WEB_PATH = ROOT / "apps/visual-lab/src/main.ts"

generator = GENERATOR_PATH.read_text(encoding="utf-8")
android = ANDROID_PATH.read_text(encoding="utf-8")
web = WEB_PATH.read_text(encoding="utf-8")

tree = ast.parse(generator)
generated: set[str] = set()
for node in tree.body:
    if isinstance(node, ast.Assign) and any(isinstance(target, ast.Name) and target.id == "SCENES" for target in node.targets):
        if not isinstance(node.value, ast.Dict):
            raise SystemExit("SCENES must be a dictionary literal")
        for key in node.value.keys:
            if not isinstance(key, ast.Constant) or not isinstance(key.value, str):
                raise SystemExit("SCENES keys must be string literals")
            generated.add(key.value)
        break
if not generated:
    raise SystemExit("could not find SCENES registry in generator")

android_masks = set(re.findall(r'R\.drawable\.scene_([a-z0-9_]+)', android))
web_themes = set(re.findall(r"\{\s*id:\s*'([a-z0-9_]+)'\s*,\s*label:", web))

if generated != android_masks or generated != web_themes:
    raise SystemExit(
        "theme catalogue mismatch\n"
        f"generated only: {sorted(generated - android_masks - web_themes)}\n"
        f"android only: {sorted(android_masks - generated)}\n"
        f"web only: {sorted(web_themes - generated)}"
    )

if len(generated) < 20:
    raise SystemExit(f"expected at least 20 themes, found {len(generated)}")

required = {
    "spectral_observatory",
    "recursive_monolith",
    "chrono_loom",
    "muon_chamber",
    "vector_shrine",
    "lagrange_garden",
}
missing = required - generated
if missing:
    raise SystemExit(f"missing signature-system themes: {sorted(missing)}")

android_compositions = set(re.findall(r'CompositionStyle\.([A-Z_]+)', android))
web_compositions = set(re.findall(r"composition:\s*'([a-z_]+)'", web))
if len(android_compositions) < 9 or len(web_compositions) < 9:
    raise SystemExit(
        f"expected nine composition grammars; android={sorted(android_compositions)} web={sorted(web_compositions)}"
    )

print(f"theme catalogues aligned: {len(generated)} themes, nine composition grammars")
