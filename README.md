# GlyphLock — Prototype 0.3 Motion Art Pack

A private Android live-wallpaper experiment in which high-density glyph artwork continuously moves, then reorganizes itself into concise, event-shaped information.

**This repository intentionally contains no Hermes integration, no connectors, no network client, no microphone permission, no notification-listener permission, and no account permissions.** Prototype 0 exists to answer the visual question first: _is the wallpaper beautiful and alive enough to deserve a permanent place on the lock screen?_

![GlyphLock transformation](docs/media/transform.gif)

The new cinematic motion pack is generated and captured by the visual-lab tooling; CI publishes the complete interactive build as an artifact.

## What is implemented

- Eight original procedural glyph systems:
  - **Sentinel** — winged situational-awareness figure.
  - **Moth** — symmetric organic collision field.
  - **Orbit** — topographic systems ring.
  - **Neural Halo** — orbiting synaptic graph.
  - **Cipher Cathedral** — cyber-reliquary and circuit architecture.
  - **Quantum Lattice** — perspective probability grid and wave shells.
  - **Fusion Core** — tokamak/reactor telemetry form.
  - **Packet Bloom** — radial network-routing flower.
- Six local nerd-tech demo events: important email, calendar collision, recovered deployment, TLS certificate rotation, WireGuard packet loss, and model-run convergence.
- Continuous ambient glyph movement instead of a static ASCII image.
- Five motion grammars: **flow**, **orbital**, **circuit**, **radial**, and **bloom**.
- A wake ripple that travels through the glyph field when the wallpaper becomes visible.
- Artwork-to-information morphing rather than notification cards over a wallpaper.
- Theme-specific source-to-language paths: curved orbital migration, orthogonal circuit stepping, radial convergence, petal-like bloom, and flowing waves.
- Android `WallpaperService` implementation.
- Full-screen Android preview using the same scene renderer.
- Tap, long-hold, horizontal swipe, and upward-collapse interaction grammar.
- Simulated listening and simulated result states.
- Browser visual laboratory with **Calm**, **Cinematic**, and **Hyper** motion profiles.
- Deterministic capture hooks for visual regression and motion review.
- Local-only immutable fixtures; the app requests no runtime permissions.

## Product boundary

Prototype 0 remains deliberately narrow:

```text
LOCAL DEMO EVENT
      ↓
SEMANTIC VISUAL STATE
      ↓
THEME-SPECIFIC MOTION GRAMMAR
      ↓
GLYPH SCENE + TEXT GEOMETRY
      ↓
LIVE WALLPAPER / PREVIEW
```

Hermes will be introduced only after the visual gate passes. No placeholder agent SDK has been added, so the rendering architecture cannot quietly become coupled to a backend before it is ready.

## Motion behavior

The resting wallpaper is not frozen. A bounded live subset of the dense glyph field breathes, orbits, routes, pulses, or blooms according to the selected theme. The majority of the field remains stable so the scene keeps its detail and does not become visual noise.

When the screen becomes visible:

1. A radial wake pulse travels through the artwork.
2. The selected event begins resolving from the field after the configured delay.
3. Glyphs migrate in staggered spatial bands rather than exploding simultaneously.
4. The final text state becomes calm and readable while the surrounding structure retains restrained motion.
5. Dismissal returns the same characters to the artwork.

See [Motion system](docs/MOTION_SYSTEM.md) and [Visual grammar](docs/VISUAL_GRAMMAR.md).

## Android interaction

- **Wallpaper becomes visible:** wake ripple and optional automatic reveal of the selected fixture.
- **Tap lower area:** reveal/focus the current event.
- **Hold lower area:** simulate event-bound voice interaction, then show a local result.
- **Swipe left/right:** change fixture event.
- **Swipe up:** return the information to the artwork.

Wallpaper touch delivery varies by launcher and OEM. The interactive preview is the reliable evaluation surface; the wallpaper uses the same best-effort gestures.

## Repository layout

```text
apps/android/       Native Android live wallpaper and preview
apps/visual-lab/    TypeScript/Canvas visual and motion laboratory
scripts/            Procedural masks and deterministic capture tooling
docs/               Motion grammar, architecture, and acceptance gate
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

Create a single-file interactive build with:

```bash
python scripts/build_standalone_visual_lab.py --output GlyphLock-Visual-Lab.html
```

The laboratory uses a small text-layout adapter with the same conceptual boundary as Pretext (`prepare` then `layout`). The actual Pretext dependency remains deferred until the visual direction is approved; this keeps the visual proof dependency-light and leaves room to compare Pretext with native Android shaping later.

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

The GitHub workflow provisions Gradle and the Android SDK, regenerates all procedural scene masks, and uploads the debug APK as an artifact.

## Design review gate

Do not begin Hermes integration until the following are true:

1. The resting artwork is desirable with all event functionality disabled.
2. The ambient movement feels alive without looking like a screensaver.
3. The transformation reads as one artwork changing state, not a notification overlay.
4. The final event state is readable in under two seconds.
5. The motion remains coherent on a physical Android device.
6. The wallpaper stops rendering when hidden.
7. At least one scene is strong enough to serve as the project hero image without explanation.

See [Prototype 0 acceptance criteria](docs/PROTOTYPE_0.md).

## Generated artwork

All eight masks are original procedural assets generated by `scripts/generate_scene_masks.py`. The external references informed the desired density, fluidity, and level of detail, but their imagery is not included or traced in this repository.

## Status

- Browser visual and motion proof: **built and captured**
- Android motion renderer: **implemented**
- Android APK: **built by CI or a local Android SDK**
- Hermes: **explicitly deferred**
