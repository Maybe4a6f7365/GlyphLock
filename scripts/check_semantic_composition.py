#!/usr/bin/env python3
"""Fail CI when event readability regresses to a global empty rectangle."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/android/app/src/main/java/dev/glyphlock/wallpaper/GlyphSceneRenderer.java"
WEB = ROOT / "apps/visual-lab/src/main.ts"
CATALOG = ROOT / "apps/android/app/src/main/java/dev/glyphlock/wallpaper/DemoCatalog.java"

FORBIDDEN = {
    ANDROID: (
        "cavityFraction", "protectedArea", "readableBounds", "renderEventBitmap",
        "addSemanticStructure(targets",
    ),
    WEB: ("cavityY", "protectedArea", "readableBounds", "eventCanvas", "textMaskCanvas"),
}
REQUIRED = {
    ANDROID: (
        "CompositionStyle", "TextBand", "TargetRole", "warpFillerAroundBands",
        "addFullScreenEventStructure", "addStructureArc", "clearStructureFromTextBands",
        "identityFloorFor", "SystemIntent", "transformationOrder",
    ),
    WEB: (
        "CompositionStyle", "TextBand", "TargetRole", "warpFillerAroundBands",
        "addSemanticStructure", "addArcLineTargets", "identityFloorFor",
    ),
    CATALOG: (
        "SPECTRAL_OBSERVATORY", "RECURSIVE_MONOLITH", "CHRONO_LOOM",
        "MUON_CHAMBER", "VECTOR_SHRINE", "LAGRANGE_GARDEN",
        "semanticWidth", "compositionStyle", "DIAL", "CASCADE", "CONSTELLATION",
    ),
}

errors: list[str] = []
for path, tokens in FORBIDDEN.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        if token in text:
            errors.append(f"{path.relative_to(ROOT)} contains forbidden global-cavity token: {token}")
for path, tokens in REQUIRED.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        if token not in text:
            errors.append(f"{path.relative_to(ROOT)} is missing semantic-composition token: {token}")

if errors:
    raise SystemExit("\n".join(errors))
print("Semantic composition guard passed: language is embedded through local glyph-band warping.")
