# GlyphLock — Prototype 0.6 Aesthetic Design Pack

A private Android live-wallpaper experiment in which a high-density glyph artwork continuously moves and then **turns its own body into event language**.

**This repository intentionally contains no Hermes integration, connectors, network client, microphone permission, notification-listener permission, or account permissions.** Prototype 0 exists to answer the visual question first: _is the wallpaper beautiful and alive enough to deserve a permanent place on the lock screen?_

![GlyphLock transformation](docs/media/transform.gif)

## v0.6: a broader wallpaper-quality art catalogue

The morph-first architecture remains unchanged. Version 0.6 expands the visual language from eight to fourteen original procedural systems and adds a CI parity guard so every Android theme, browser theme, and generated mask must exist together.

## v0.5 correction: the event is the transformed artwork

Earlier prototypes still used an event bitmap and a dark readability cavity. Even though glyph particles moved toward the information, the final composition could read as a background plus an overlay. That architecture has been removed.

Prototype 0.5 has one semantic material:

```text
AMBIENT ART GLYPHS
        ↓
PERSISTENT MORPH TOPOLOGY
        ↓
EVENT TITLE + SUMMARY + STRUCTURE
        ↓
RESULT LANGUAGE
        ↓
THE SAME AMBIENT ART GLYPHS
```

The event renderer now creates **no event bitmap, notification card, or black cavity**. A representative topology of up to 2,300 actual source glyphs receives event and result targets. During reveal those glyphs move from their original coordinates, change character, resize, recolor, clear their own readable region, and settle into the event.

The user must be able to watch the wallpaper becoming the information.

## What is implemented

- Fourteen original procedural glyph systems:
  - **Sentinel** — winged situational-awareness figure.
  - **Moth** — symmetric organic collision field.
  - **Orbit** — topographic systems ring.
  - **Neural Halo** — orbiting synaptic graph.
  - **Cipher Cathedral** — cyber-reliquary and circuit architecture.
  - **Quantum Lattice** — perspective probability grid and wave shells.
  - **Fusion Core** — tokamak/reactor telemetry form.
  - **Packet Bloom** — radial network-routing flower.
  - **Event Horizon** — gravitational lensing, accretion lanes, and relativistic jets.
  - **Tesseract Engine** — nested four-dimensional projection frames and coordinate rails.
  - **Helix Array** — genomic double-helix telemetry and sequencing splices.
  - **Interference Field** — phase-locked moiré waves and diffraction loci.
  - **Cryo Vault** — hexagonal archival chamber with frost branches and memory vials.
  - **Dyson Relay** — segmented stellar collectors, relay beams, and receiver architecture.
- Six local nerd-tech demo events: important email, calendar collision, recovered deployment, TLS certificate rotation, WireGuard packet loss, and model-run convergence.
- Continuous ambient glyph motion.
- Five motion grammars: **flow**, **orbital**, **circuit**, **radial**, and **bloom**.
- A wake ripple when the wallpaper becomes visible.
- Morph-first event composition using the original source glyphs.
- Spatial source-to-target assignment so the transformation remains coherent.
- Displacement-based readability: surrounding art moves around the event instead of being covered by a dark panel.
- Fragmented technical rails assembled from the same source topology.
- Android `WallpaperService` and a full-screen preview using the same renderer.
- Tap, long-hold, horizontal swipe, and upward-collapse interaction grammar.
- A listening pressure wave that deforms the current glyph topology instead of displaying a waveform widget.
- Event-to-result glyph morphing.
- Local-only immutable fixtures; the app requests no runtime permissions.

## Product boundary

```text
LOCAL DEMO EVENT
      ↓
EVENT TARGET GLYPHS
      ↓
SOURCE-TO-TARGET ASSIGNMENT
      ↓
THEME-SPECIFIC TOPOLOGY MORPH
      ↓
LIVE WALLPAPER / PREVIEW
```

Hermes will be introduced only after the morph-first visual gate passes. No placeholder agent SDK is present.

## Motion behavior

The resting wallpaper is not frozen. A bounded live subset breathes, orbits, routes, pulses, or blooms according to the selected theme, while the dense raster preserves visual detail.

When the screen becomes visible or an event is focused:

1. A wake pulse travels through the source artwork.
2. The dense ambient raster hands off to persistent source glyphs.
3. Those same glyphs move in staggered spatial bands.
4. Some become the exact characters of the title, summary, metadata, and action hint.
5. Every remaining morph glyph folds around the readable region and preserves the wallpaper's structural DNA.
6. Text glyphs settle; transformed decorative glyphs retain restrained movement.
7. Holding the interaction area sends a pressure wave through the same topology.
8. The result reorganizes the event glyphs again.
9. Dismissal reverses the topology into the source art.

See [Morph-first architecture](docs/MORPH_FIRST_ARCHITECTURE.md), [Motion system](docs/MOTION_SYSTEM.md), and [Visual grammar](docs/VISUAL_GRAMMAR.md).

## Android interaction

- **Wallpaper becomes visible:** wake ripple and optional automatic morph of the selected fixture.
- **Tap lower area:** morph the artwork into the current event.
- **Hold lower area:** deform the focused event into listening state, then morph into a local result.
- **Swipe left/right:** change fixture event.
- **Swipe up:** reverse the event topology into the ambient artwork.

Wallpaper touch delivery varies by launcher and OEM. The interactive preview is the reliable evaluation surface; the wallpaper uses the same best-effort gestures.

## Repository layout

```text
apps/android/       Native Android live wallpaper and preview
apps/visual-lab/    TypeScript/Canvas visual and motion laboratory
scripts/            Procedural masks and deterministic capture tooling
docs/               Morph grammar, architecture, and acceptance gate
.github/workflows/  Reproducible CI build
```

## Run the visual laboratory

```bash
python -m pip install pillow numpy
python scripts/generate_scene_masks.py
cd apps/visual-lab
npm install
npm run build
npm run serve
```

Open `http://localhost:4173`.

## Build the Android app

Requirements:

- JDK 21
- Gradle 9.4.1
- Android SDK 36

```bash
python -m pip install pillow numpy
python scripts/generate_scene_masks.py
cd apps/android
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The debug APK is produced at:

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions regenerates the procedural masks, runs unit tests and lint, assembles the APK, and uploads it as an artifact.

## Morph-first design gate

Do not begin Hermes integration until all are true:

1. The resting art is desirable with events disabled.
2. The event can only be explained as the artwork changing state.
3. Removing event glyphs would visibly remove part of the transformed artwork itself.
4. No event bitmap, card, cavity, or background panel is rendered.
5. Most promoted source glyphs visibly participate in the transformation.
6. The final event is readable in under two seconds after settling.
7. Collapse reverses the topology rather than crossfading to another composition.
8. The motion remains coherent on a physical Android device.
9. The wallpaper stops rendering when hidden.

## Generated artwork

All masks are original procedural assets generated by `scripts/generate_scene_masks.py`. External references informed the desired density, fluidity, and level of detail; their imagery is not included or traced.

## Status

- Morph-first Android renderer: **implemented in v0.5 and preserved in v0.6**
- Event/result overlays: **removed**
- Android APK: **built by CI or a local Android SDK**
- Hermes: **explicitly deferred**
