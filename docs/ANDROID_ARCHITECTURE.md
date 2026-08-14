# Android Architecture

## Components

```text
MainActivity
  ├── choose local theme/event
  ├── open interactive preview
  └── launch live-wallpaper picker

PreviewActivity
  └── GlyphPreviewView
        ├── ExperienceController
        ├── TouchInterpreter
        └── GlyphSceneRenderer

GlyphWallpaperService
  └── GlyphEngine
        ├── ExperienceController
        ├── TouchInterpreter
        └── GlyphSceneRenderer
```

## Threading

Scene construction is intentionally separated from drawing:

```text
background scene executor
  ├── mask decoding
  ├── glyph sampling
  ├── static bitmap rasterization
  ├── text geometry
  └── particle mapping

main / wallpaper thread
  ├── immutable scene swap
  ├── frame state update
  ├── bitmap composition
  └── live particle drawing
```

Stale background builds are rejected using a generation counter.

## Frame scheduling

- No frame loop while wallpaper is invisible.
- 16 ms scheduling only during reveal, collapse, listening, and result transitions.
- Focused and ambient states are static after their final frame.
- Scene rebuilds request one new frame after completion.

## Memory strategy

The renderer caps its internal width at 720 pixels and scales the result to the surface. It keeps three state bitmaps:

- ambient
- focused event
- simulated result

This is intentionally conservative for a proof. A production renderer would likely use GPU instancing, SDF/MSDF glyph atlases, adaptive level of detail, and a cached static base layer.

## Lock-screen constraints

A live wallpaper is below system UI. It does not own the clock, biometric prompt, emergency affordance, or every touch gesture. OEM launchers and keyguards may intercept input. The full-screen preview is therefore the deterministic interaction test; wallpaper touch remains best effort.
