# Repository review — v0.7

## Goal used for the review

GlyphLock should feel like a wallpaper-quality technical organism that can turn its own body into the one event that matters. The event must not look like a notification rendered over a moving background.

## What was working in v0.6

- The ambient procedural masks were original, dense, and varied.
- The renderer no longer created event or result bitmaps.
- Source glyphs physically moved into event characters.
- The listening state deformed the current topology instead of showing an assistant widget.
- Android, browser, and generator catalogues were checked by CI.
- The project remained offline and permission-free.

## Problems found

### 1. One generic event composition served every theme

The source art changed, but the final information hierarchy was almost identical. A cathedral, helix, singularity, and figure could all collapse into the same left-aligned block. Theme identity was strongest before the event and weakest when the product was supposed to be most distinctive.

### 2. A global protected rectangle created a pseudo-card

The implementation technically avoided drawing a panel, but filler glyphs were pushed outside one large rectangular area. The resulting negative space could still read as a hidden notification card.

### 3. Character transitions were globally simultaneous

Source and destination characters overlapped for too long around the middle of the reveal. The animation demonstrated motion but produced a visually noisy, partially unreadable interval.

### 4. The dense source raster disappeared too early

The base scene handed off quickly, before enough live glyphs had established the next topology. This made the artwork seem to fade before it transformed.

### 5. Catalogue breadth outpaced composition quality

Fourteen masks looked good as a collection, but adding more masks alone would not improve the core product. The event-state grammar needed to become theme-aware before further expansion.

### 6. Review tooling was incomplete

The deterministic capture tool hard-coded only the original theme IDs, so new systems were easier to add than to review consistently.

## Decisions implemented

- Replace global protected regions with exact `TextBand` collision zones.
- Introduce six `CompositionStyle` grammars.
- Add semantic `TargetRole` ordering and role-specific character timing.
- Keep the ambient raster visible deeper into the reveal.
- Increase the Lux live topology budget while retaining Eco and Balanced modes.
- Add wave and fold motion paths.
- Strengthen event-state rails, rings, strands, and fields.
- Add two new systems only after the composition architecture was corrected.
- Make capture tooling derive its catalogue from source.
- Add CI guards for semantic composition, not merely the absence of bitmaps.

## Remaining risks

- Android Canvas may not sustain the Lux glyph count on every physical device.
- Some theme/event pairs still need hand-tuned semantic anchors.
- A long title may require a theme-specific fallback hierarchy.
- Browser and Android font metrics will not be pixel-identical.
- OEM lock screens may intercept gestures.
- The procedural masks are a strong research catalogue, but the daily-driver set should probably be curated down to three to five exceptional systems.

## Next gate

Install the v0.7 APK on the target phone and record:

- 60 fps and 30 fps frame pacing;
- peak PSS in Lux mode;
- surface wake latency;
- thermal change after ten reveal/listen/collapse loops;
- readability at arm’s length;
- whether each selected theme remains recognisable at the final event state.

Do not add Hermes until that device review passes.
