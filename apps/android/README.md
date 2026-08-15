# GlyphLock Android visual prototype

Native, dependency-light Android implementation of the offline semantic-morph state machine.

## Modules

- `MainActivity`: local fixture, theme, quality, and live-wallpaper controls.
- `PreviewActivity`: deterministic full-screen interaction test.
- `GlyphWallpaperService`: best-effort live-wallpaper interaction.
- `GlyphSceneRenderer`: ambient raster plus persistent source-glyph topology.
- `ExperienceController`: ambient/reveal/listen/result/collapse state machine.
- `TouchInterpreter`: semantic gesture mapping.

The manifest intentionally declares no internet, microphone, notification, contacts, calendar, or account permission.

## Generate procedural resources

```bash
python -m pip install -r ../../scripts/requirements.txt
python ../../scripts/generate_scene_masks.py
```

## Build

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The v0.7 Lux profile uses a 960 px internal scene and up to 4,300 persistent morph glyphs. Eco and Balanced profiles remain available for device profiling.
