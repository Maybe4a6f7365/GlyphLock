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
        "drawGlyphTransition(",
        "glyph.eventTarget.char",
        "glyph.resultTarget.char",
        "if (!/\\s/.test(char)) targets.push",
        "targetDemand",
        "buildStructuralSourcePoints",
        "drawImage(strokeSampler",
        "private wrapText(",
        "const scan = ctx.createLinearGradient",
        "addSemanticStructure(",
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
        "MASK_BLACK_CUTOFF = 10f / 255f",
        "glyph.edgeMobility",
        "glyph.glow",
        "preserveSourceCountOutsideSafeZones",
        "preserveTargetCountOutsideSafeZones",
    ),
    WEB: (
        "interface MorphGlyph",
        "assignCoherentTargets",
        "buildFillerTarget",
        "drawMorphField",
        "sampleTextStrokeTargets",
        "strokeSamplerCtx",
        "withSourceChar",
        "char: source.char",
        "const eventTarget = this.withSourceChar(",
        "const resultTarget = this.withSourceChar(",
        "assignedEvent ?? this.buildFillerTarget",
        "assignedResult ?? this.buildFillerTarget",
        "ctx.fillText(glyph.source.char, x, y)",
        "appendStructuralSourceRails",
        "const count = Math.min(this.motionSpec.morphCount, MAX_MORPH_SOURCE_COUNT)",
        "clockSafeBottom",
        "gestureSafeTop",
        "MASK_BLACK_CUTOFF = 10 / 255",
        "MAX_RIBBON_GLYPHS = 420",
        "fitWrappedLines",
        "wrapTextFully",
        "prioritizeTargetsWithinBudget",
        "Math.sqrt(MOTION_PROFILES.cinematic.morphCount / this.motionSpec.morphCount)",
        "addFullScreenEventStructure",
        "clearStructureFromTextBands",
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

web_text = WEB.read_text(encoding="utf-8")
for method in ("addAlignedLineTargets", "addArcLineTargets"):
    start = web_text.find(f"private {method}(")
    end = web_text.find("\n  private ", start + 1)
    body = web_text[start:end if end >= 0 else None] if start >= 0 else ""
    if "sampleTextStrokeTargets(" not in body:
        errors.append(f"apps/visual-lab/src/main.ts {method} must sample macro-stroke destinations")

ribbon_start = web_text.find("private drawGlyphRibbonAtmosphere(")
ribbon_end = web_text.find("\n  private ", ribbon_start + 1)
ribbon_body = web_text[ribbon_start:ribbon_end if ribbon_end >= 0 else None] if ribbon_start >= 0 else ""
if "this.ribbonGlyphs" not in ribbon_body or "this.basePoints" in ribbon_body:
    errors.append("apps/visual-lab/src/main.ts ribbon draw must use only the bounded precomputed subset")

ambient_start = web_text.find("private drawAmbientMotion(")
ambient_end = web_text.find("\n  private ", ambient_start + 1)
ambient_body = web_text[ambient_start:ambient_end if ambient_end >= 0 else None] if ambient_start >= 0 else ""
if "semanticMix = smooth(reveal)" not in ambient_body or "this.themePalette" not in ambient_body:
    errors.append("apps/visual-lab/src/main.ts ambient palette must begin theme-only and blend with reveal")
if "derivePalette(" in ambient_body or "this.event.accent" in ambient_body:
    errors.append("apps/visual-lab/src/main.ts ambient draw must not derive directly from the event accent")
if "hash(" in ambient_body or "ambientIndex %" in ambient_body:
    errors.append("apps/visual-lab/src/main.ts ambient draw must use precomputed glyph metadata")
for token in ("p.edgeMobility", "p.energyCoordinate", "p.palettePhase", "p.glitchEligible", "p.echoPrimary", "p.glowEligible"):
    if token not in ambient_body:
        errors.append(f"apps/visual-lab/src/main.ts ambient draw is missing precomputed metadata: {token}")

topology_start = web_text.find("private rebuildTopology(")
topology_end = web_text.find("\n  private ", topology_start + 1)
topology_body = web_text[topology_start:topology_end if topology_end >= 0 else None] if topology_start >= 0 else ""
if "document.createElement('canvas')" in topology_body or "this.semanticStrokeSamplerCtx" not in topology_body:
    errors.append("apps/visual-lab/src/main.ts topology must reuse the compile-only semantic stroke sampler")

rails_start = web_text.find("private appendStructuralSourceRails(")
rails_end = web_text.find("\n  private ", rails_start + 1)
rails_body = web_text[rails_start:rails_end if rails_end >= 0 else None] if rails_start >= 0 else ""
if "this.event" in rails_body:
    errors.append("apps/visual-lab/src/main.ts supplemental source rails must be theme-only")

structure_start = web_text.find("private addFullScreenEventStructure(")
structure_end = web_text.find("\n  private ", structure_start + 1)
structure_body = web_text[structure_start:structure_end if structure_end >= 0 else None] if structure_start >= 0 else ""
if "bounds" in structure_body or "INTERNAL_W * .035" not in structure_body or "INTERNAL_H * .91" not in structure_body:
    errors.append("apps/visual-lab/src/main.ts event hardware must use the fixed physical safe frame")

layout_start = web_text.find("private buildTargetLayout(")
layout_end = web_text.find("\n  private ", layout_start + 1)
layout_body = web_text[layout_start:layout_end if layout_end >= 0 else None] if layout_start >= 0 else ""
if "this.addFullScreenEventStructure(targets, result)" not in layout_body or "this.clearStructureFromTextBands(targets, textBands)" not in layout_body:
    errors.append("apps/visual-lab/src/main.ts target layout must clear only hardware collisions after full-screen composition")

if errors:
    raise SystemExit("\n".join(errors))
print("Morph-first architecture guard passed.")
