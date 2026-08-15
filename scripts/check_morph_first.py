#!/usr/bin/env python3
"""Fail CI if the semantic event regresses to an independent overlay renderer."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/android/app/src/main/java/dev/glyphlock/wallpaper/GlyphSceneRenderer.java"
WEB = ROOT / "apps/visual-lab/src/main.ts"
ARCHITECTURE = ROOT / "docs/MORPH_FIRST_ARCHITECTURE.md"

FORBIDDEN = {
    ANDROID: (
        "eventBitmap",
        "resultBitmap",
        "renderEventBitmap",
        "drawListening(",
        "assignNearestTargets",
        "addAlignedLineTargets",
    ),
    WEB: (
        "eventCanvas",
        "textMaskCanvas",
        "drawImage(this.eventCanvas",
        "private drawListening(",
        "assignNearestTargets",
    ),
}

REQUIRED = {
    ANDROID: (
        "class MorphGlyph",
        "assignCoherentTargets",
        "buildFillerTarget",
        "drawMorphField",
        "SpatialGlyphMatcher.match",
        "addMacroLineTargets",
        "addFullScreenSourceField",
        "addFullScreenEventStructure",
        "SystemIntent",
        "transformationOrder",
        "wrapTextFully",
        "withGlyph(eventTarget, source.glyph)",
    ),
    WEB: (
        "interface MorphGlyph",
        "assignCoherentTargets",
        "buildFillerTarget",
        "drawMorphField",
    ),
    ARCHITECTURE: (
        "Non-negotiable visual test",
        "macro-glyph",
        "one source particle to one literal content character",
        "A focused screenshot resembles a normal title/paragraph/action text layout",
        "full-screen ASCII fields",
        "must never be computed from `semanticBounds`",
        "Geometry has no identifiable subsystem purpose",
    ),
}

errors: list[str] = []
for path, tokens in FORBIDDEN.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        if token in text:
            errors.append(f"{path.relative_to(ROOT)} contains forbidden overlay/naive token: {token}")
for path, tokens in REQUIRED.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        if token not in text:
            errors.append(f"{path.relative_to(ROOT)} is missing morph-first token: {token}")

if errors:
    raise SystemExit("\n".join(errors))
print("Morph-first architecture guard passed.")
