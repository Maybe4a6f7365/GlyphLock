# GlyphLock Android Prototype

Native, dependency-light Android implementation of the Prototype-0 visual state machine.

## Modules

- `MainActivity`: local fixture/theme controls and live-wallpaper installer.
- `PreviewActivity`: deterministic full-screen interaction test.
- `GlyphWallpaperService`: best-effort live-wallpaper interaction.
- `GlyphSceneRenderer`: hybrid pre-rendered glyph field plus live morph subset.
- `ExperienceController`: ambient/reveal/listen/result/collapse state machine.
- `TouchInterpreter`: semantic gesture mapping.

The manifest intentionally declares no internet, microphone, notification, contacts, calendar, or account permission.

## Build

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

A Gradle wrapper will be generated once the GitHub repository is created in an environment that can download Gradle distributions.
