#!/usr/bin/env python3
"""Guard coherent source-to-language transport and wallpaper identity preservation."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_RENDERER = ROOT / "apps/android/app/src/main/java/dev/glyphlock/wallpaper/GlyphSceneRenderer.java"
ANDROID_MATCHER = ROOT / "apps/android/app/src/main/java/dev/glyphlock/wallpaper/SpatialGlyphMatcher.java"
ANDROID_MATH = ROOT / "apps/android/app/src/main/java/dev/glyphlock/wallpaper/GlyphMath.java"
WEB = ROOT / "apps/visual-lab/src/main.ts"
GENERATOR = ROOT / "scripts/generate_scene_masks.py"

REQUIRED = {
    ANDROID_RENDERER: (
        "SpatialGlyphMatcher.match",
        "identityFloorFor",
        "GlyphMath.localInfluence",
        "trailVisibility",
    ),
    ANDROID_MATCHER: (
        "MAX_LOCAL_RADIUS",
        "ordering penalty",
        "longTravelPenalty",
    ),
    ANDROID_MATH: (
        "distanceToRect",
        "localInfluence",
    ),
    WEB: (
        "assignCoherentTargets",
        "localInfluence",
        "identityFloorFor",
        "trailVisibility",
    ),
    GENERATOR: (
        "chrono_loom",
        "muon_chamber",
        "vector_shrine",
        "lagrange_garden",
        "ProcessPoolExecutor",
    ),
}

errors: list[str] = []
for path, tokens in REQUIRED.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        if token not in text:
            errors.append(f"{path.relative_to(ROOT)} is missing transport token: {token}")

if errors:
    raise SystemExit("\n".join(errors))
print("Coherent transport guard passed: local matching, identity floors, and signature systems are present.")
