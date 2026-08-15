# GlyphLock repository review — v0.8

## Goal under review

GlyphLock succeeds only when the wallpaper remains desirable as standalone art and the same glyph material becomes the event. The user should see a technical artwork reconfigure itself, not an application layer appear above it.

## What v0.7 established

- The event was compiled into glyph targets rather than rendered as an independent bitmap.
- Six composition grammars prevented every theme from collapsing into exactly the same rectangle.
- The resolved state retained more of the original wallpaper topology.
- Sixteen original procedural art systems were available in Android and the browser laboratory.

## Review findings

### 1. Transport was visually correct but computationally naive

The previous assignment searched every unused source glyph for every destination glyph. That was quadratic and permitted adjacent letters to recruit source glyphs from opposite sides of the artwork. Long crossing paths made some transitions look like a particle explosion rather than a coherent physical reconfiguration.

### 2. Final compositions still converged too much

Figure, architecture, orbital, splice, and field layouts were distinct, but many resolved states still used conventional horizontal typographic hierarchy. More themes alone would not solve this; the product needed more composition grammars.

### 3. Wallpaper identity weakened outside the language region

Filler glyphs could follow the theme motion across the entire screen. The result remained morph-first, but too much global displacement reduced recognition of the original artwork.

### 4. The generator was unnecessarily expensive

All masks were regenerated serially and PNG optimization dominated local iteration. That slowed the design loop, which is the most important loop in Prototype 0.

### 5. Idle frame scheduling was more expensive than necessary

Transitions need a high update rate. Ambient and reading states do not. A fixed thirty-frame loop spent more energy without meaningfully improving the visual experience.

## v0.8 decisions

- Replace quadratic assignment with deterministic grid-assisted spatial matching.
- Penalize long travel and line-order inversions to reduce crossing.
- Preserve source identity using a distance-based deformation field with a nonzero theme-specific floor.
- Add subtle transport echoes made from the same source glyph material.
- Add three composition grammars: dial, cascade, and constellation.
- Add four signature art systems designed around those grammars.
- Parallelize mask generation and support selective/incremental regeneration.
- Use 60-ish fps only during transitions, 24 fps for ambient life, and 15 fps for settled reading states.

## Remaining risks

- Android and browser renderers still duplicate composition logic; contract fixtures should eventually be shared.
- Canvas text measurement is good enough for the visual proof but not yet the final internationalized typography system.
- The Android renderer remains CPU/Canvas based. A GPU-instanced implementation may be required after physical-device profiling.
- Twenty themes are useful for exploration, but a future curation pass should promote only the strongest systems into the default set.

## Next gate

Install the v0.8 APK on the reference Android device and review:

1. Whether neighboring event letters visibly recruit neighboring source glyphs.
2. Whether the source artwork remains recognizable at full reveal.
3. Whether dial, cascade, and constellation feel materially different.
4. Whether the ambient frame reduction is still perceptually alive.
5. Whether Lux remains within acceptable frame-time and thermal limits.
