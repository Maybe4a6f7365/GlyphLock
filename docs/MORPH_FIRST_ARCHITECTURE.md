# Morph-first semantic wallpaper architecture

GlyphLock v0.5 removes the event bitmap and notification-overlay model.

## Invariant

An event is allowed to appear only by changing the glyph topology of the wallpaper itself.

The renderer may draw the Android preview clock and system-like gesture hint separately, but it must not draw an event card, event bitmap, background panel, global protected rectangle, or conventional text overlay.

## Render pipeline

```text
procedural scene mask
        ↓
ambient glyph field
        ↓
select persistent morph topology
        ↓
compile event language into target glyphs
        ↓
assign target characters to actual source glyphs
        ↓
deform every remaining source glyph and locally warp only text-line collisions
        ↓
source art → event topology → result topology → source art
```

The static base raster exists only as the low-cost ambient representation. During reveal it hands off to up to 4,300 persistent source glyphs in Lux mode and disappears. Those same source glyphs then:

- move from their original artwork coordinates;
- change size and opacity;
- change character identity;
- form the event title, summary, metadata, action hint, and fragmented technical rails;
- preserve the wallpaper's identity as transformed low-density structure around the text;
- reorganize again for the simulated result;
- reverse back into the original source artwork during collapse.

## Readability without a card

The readable region is created through displacement, not a dark rectangle. Each exact line produces a `TextBand`. Only filler glyphs that collide with that line are displaced along a theme-appropriate axis; the surrounding topology remains intact.

Actual event characters are target glyphs in the same morph list as the surrounding art. No event `Bitmap` is created.

## Theme grammars

- **Flow:** the field shears around the semantic region.
- **Orbital:** source glyphs rotate through curved trajectories and settle into language.
- **Circuit:** source glyphs follow orthogonal routes and form broken data rails.
- **Radial:** the artwork contracts and expands through rings around the event.
- **Bloom:** glyph petals open, make room for language, and close again.
- **Wave:** phase displacement travels across the field.
- **Fold:** architectural planes fold through the semantic state.

## Interaction states

- **Wake:** a pulse travels through the ambient field.
- **Reveal:** the base raster hands off to persistent source glyphs over 2.45 seconds.
- **Focused:** text glyphs become still; only transformed background glyphs retain restrained life.
- **Listening:** the same event topology develops a simulated pressure wave near the lower interaction area. No waveform widget is overlaid.
- **Result:** event glyphs morph into result glyphs over 1.35 seconds.
- **Collapse:** the topology reverses into the original artwork over 1.9 seconds.

## Acceptance tests

A visual pass is rejected when any of the following is true:

1. The event can be removed while leaving the wallpaper visually unchanged beneath it.
2. A separate event bitmap, card, global empty rectangle, or conventional notification layer is rendered.
3. Most source glyphs remain static while a small independent set writes text on top.
4. The artwork disappears before the viewer can see it becoming the event.
5. The final event state does not preserve visible structural DNA from the selected wallpaper.
6. Returning to ambient requires a cut or crossfade rather than reversing the glyph topology.
