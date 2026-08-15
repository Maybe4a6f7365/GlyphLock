# Android architecture

## Components

```text
MainActivity
  ├── choose local theme/event/quality
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

## Scene construction

Scene construction is separated from drawing:

```text
background scene executor
  ├── decode procedural mask
  ├── sample dense glyph field
  ├── rasterise ambient base
  ├── select persistent morph sources
  ├── compile event/result target glyphs
  ├── assign targets spatially
  └── publish immutable Scene

wallpaper / preview thread
  ├── update ExperienceController frame
  ├── draw ambient raster during handoff
  ├── draw bounded ambient glyph subset
  ├── draw persistent semantic topology
  └── scale internal scene to surface
```

Stale background builds are rejected using a generation counter.

## Memory and quality profiles

The renderer keeps one dense ambient bitmap and glyph topology data. It does not keep event or result bitmaps.

- **Eco:** 540 px internal width, up to 1,200 morph glyphs.
- **Balanced:** 720 px internal width, up to 2,700 morph glyphs.
- **Lux:** 960 px internal width, up to 4,300 morph glyphs.

Lux is the current design-review default; device profiling will determine the eventual runtime default.

## Frame scheduling

- No frame loop while the wallpaper is invisible.
- Active scheduling during wake, reveal, listening, result, and collapse.
- The focused reading state retains only restrained structural movement.
- Scene rebuilds request a frame after immutable-scene publication.

## Future renderer

A production-quality next step could replace Canvas glyph drawing with GPU instancing and SDF/MSDF atlases without changing the scene, target, or interaction contracts.
