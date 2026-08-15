# GlyphLock — v0.8 Signature Systems

GlyphLock is an Android live-wallpaper experiment in which high-density glyph artwork stays alive and then **reorganizes its own source topology into useful event language**.

The wallpaper is the interface. There is no event card, black information panel, independent event bitmap, chatbot window, avatar, or assistant orb.

> **Prototype boundary:** this repository intentionally contains no Hermes integration, connectors, network client, microphone permission, notification-listener permission, or account permission. All events are deterministic local fixtures until the wallpaper passes the visual gate.

## Product invariant

```text
AMBIENT GLYPH ART
        ↓
PERSISTENT SOURCE GLYPHS
        ↓
COHERENT LOCAL TRANSPORT
        ↓
THEME-SPECIFIC SEMANTIC TOPOLOGY
        ↓
RESULT TOPOLOGY
        ↓
THE SAME AMBIENT GLYPH ART
```

An event is valid only when its title, summary, metadata, action hint, and surrounding technical structure are assembled from the wallpaper’s existing glyph material.

## What v0.8 improves

Version 0.7 established real semantic compositions, but source-to-target assignment still scanned every source for every destination. That was expensive and sometimes let adjacent letters recruit glyphs from opposite sides of the artwork, causing crossing paths.

Version 0.8 adds:

- **Grid-assisted coherent transport:** nearby event characters recruit nearby source glyphs first.
- **Line-order protection:** neighboring characters resist source-order inversions, reducing tangled motion.
- **Long-travel penalties:** the solver avoids visually implausible cross-screen movement.
- **Identity floors:** deformation is strongest near semantic content and restrained elsewhere, keeping the wallpaper recognizable at full reveal.
- **Transport echoes:** faint source-glyph afterimages expose the path from artwork into language without introducing a second layer.
- **Adaptive frame budgets:** transitions update at 16 ms, ambient life at 42 ms, and settled reading states at 66 ms.
- **Parallel procedural generation:** selective and incremental mask generation dramatically shortens the design loop.
- **Three new composition grammars:** dial, cascade, and constellation.
- **Four new signature systems:** Chrono Loom, Muon Chamber, Vector Shrine, and Lagrange Garden.

## Semantic composition grammars

1. **Figure** — language inhabits a sculptural body while bilateral contours remain visible.
2. **Core** — event text phase-locks inside concentric technical rings.
3. **Orbital band** — language becomes the stable center of active orbital structures.
4. **Architecture** — text aligns with asymmetric rails and recursive framing.
5. **Splice** — language forms between interwoven signal strands.
6. **Field** — semantic roles settle at different phases of a wave field.
7. **Dial** — metadata and actions become calibrated arcs around a central reading.
8. **Cascade** — language descends through stepped rails and vector telemetry.
9. **Constellation** — semantic lines occupy related nodes inside a connected field.

Readability is created by **local text-band warping**. Only glyphs that collide with an exact line of language are moved aside. The renderer does not evacuate a global rectangle around the event.

## Wallpaper systems

The catalogue contains twenty original procedural systems:

1. Sentinel
2. Moth
3. Orbit
4. Neural Halo
5. Cipher Cathedral
6. Quantum Lattice
7. Fusion Core
8. Packet Bloom
9. Event Horizon
10. Tesseract Engine
11. Helix Array
12. Interference Field
13. Cryo Vault
14. Dyson Relay
15. Spectral Observatory
16. Recursive Monolith
17. **Chrono Loom** — chronograph rings, pendulum traces, and timing rails.
18. **Muon Chamber** — collision tracks and concentric detector walls.
19. **Vector Shrine** — recursive perspective frames and stepped telemetry.
20. **Lagrange Garden** — stable orbital nodes and living transfer trajectories.

Generated PNG masks are not committed as opaque source assets. They are recreated from `scripts/generate_scene_masks.py`.

## Motion system

Seven path grammars are implemented:

- flow
- orbital
- circuit
- radial
- bloom
- wave
- fold

When an event is revealed:

1. A wake pulse travels through the ambient field.
2. The dense raster hands off gradually to persistent source glyphs.
3. The spatial matcher assigns nearby high-value glyphs to event characters.
4. Remaining source glyphs deform according to the theme grammar and identity floor.
5. Exact text lines locally repel colliding filler glyphs.
6. Theme-specific technical structures form from the same source material.
7. Characters change late in the path, making the artwork-to-language transformation legible.
8. Settled text becomes calm while non-text structure retains restrained life.
9. Holding the interaction area sends a pressure wave through the same topology.
10. Collapse reverses the glyphs toward their original source coordinates.

## Local demo events

- Important email / meeting moved.
- Calendar and travel collision.
- Recovered GitHub deployment.
- TLS certificate rotation.
- WireGuard route instability.
- Model-run convergence.

## Android interaction

- **Wallpaper visible:** wake ripple; optional auto-reveal of the selected fixture.
- **Tap lower area:** reveal/focus the selected event.
- **Hold lower area:** enter the local listening deformation, then show a simulated result.
- **Swipe left/right:** move between fixture events.
- **Swipe up:** reverse the topology into ambient art.

Wallpaper touch delivery varies by launcher and OEM. The full-screen preview remains the deterministic evaluation surface.

## Repository layout

```text
apps/android/       Android live wallpaper, preview, and renderer
apps/visual-lab/    TypeScript/Canvas visual laboratory
scripts/            Procedural art, guards, capture, and review tooling
docs/               Architecture, grammar, reviews, and acceptance gates
.github/workflows/  Reproducible visual-lab and Android builds
```

## Run the visual laboratory

```bash
python -m pip install -r scripts/requirements.txt
python scripts/generate_scene_masks.py --jobs 3
python scripts/check_morph_first.py
python scripts/check_semantic_composition.py
python scripts/check_transport_integrity.py
python scripts/check_theme_catalog.py
cd apps/visual-lab
npm install
npm run build
npm run serve
```

Generate only selected systems during design work:

```bash
python scripts/generate_scene_masks.py \
  --only chrono_loom,muon_chamber,vector_shrine,lagrange_garden \
  --jobs 2 --compress-level 4
```

## Build Android

Requirements:

- JDK 21
- Gradle 9.4.1
- Android SDK 36

```bash
python -m pip install -r scripts/requirements.txt
python scripts/generate_scene_masks.py --jobs 3
cd apps/android
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The debug APK is produced at:

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

## CI quality gates

CI rejects:

- independent event/result bitmaps or canvases;
- event-card renderers;
- global protected rectangles or cavity coordinates;
- naive quadratic source-to-target assignment;
- missing identity-preservation and transport primitives;
- Android/browser/generator catalogue drift;
- fewer than twenty aligned procedural systems;
- fewer than nine semantic composition grammars;
- Android unit, lint, or build failures;
- TypeScript build failures.

CI publishes the visual laboratory, installable debug APK, build diagnostics, and a contact sheet of all procedural systems.

## Morph-first acceptance gate

Hermes remains deferred until all are true:

1. Ambient art is desirable with events disabled.
2. The user can visibly follow the artwork becoming the event.
3. The final event retains the selected theme’s visual identity.
4. Neighboring letters recruit neighboring source glyphs without obvious crossing.
5. Removing transformed source glyphs would remove the event itself.
6. No event bitmap, card, background panel, or global empty rectangle is rendered.
7. Collapse reverses the topology rather than crossfading to another composition.
8. Lux mode is smooth and thermally acceptable on the target Android device.
9. Hidden wallpaper stops frame scheduling.
10. At least three systems are strong enough to use daily before any AI is connected.

See:

- [Repository review v0.8](docs/REPO_REVIEW_V08.md)
- [Signature systems v0.8](docs/SIGNATURE_SYSTEMS_V08.md)
- [Semantic compositions v0.7](docs/SEMANTIC_COMPOSITIONS_V07.md)
- [Morph-first architecture](docs/MORPH_FIRST_ARCHITECTURE.md)
- [Prototype visual gate](docs/PROTOTYPE_0.md)
- [Hermes deferred boundary](docs/HERMES_DEFERRED.md)
