# Morph-first semantic wallpaper architecture

GlyphLock v0.9 removes the event bitmap and notification-overlay model and defines the visual
intent of every full-screen system.

## Invariant

An event is allowed to appear only by changing the glyph topology of the wallpaper itself.

The renderer may draw the Android preview clock and system-like gesture hint separately, but it must not draw an event card, event bitmap, background panel, global protected rectangle, or conventional text overlay.

### Non-negotiable visual test

> A frozen focused frame must still look like ASCII artwork that learned to speak.

> A frozen ambient frame must look like a recognizable cinematic AI subsystem, not a random
> collection of fashionable rings, waves, rails, or particle noise.

At normal phone distance, the event must be readable as language. Up close, every stroke of that
language must visibly consist of multiple ASCII symbols recruited from the ambient artwork. The
source symbols create **macro-glyphs** through position, density, scale, and contrast; they do not
turn into a normal title, paragraph, or button row.

The following are regressions even when their coordinates technically belong to `MorphGlyph`s:

- assigning one source particle to one literal content character at ordinary font size;
- changing source symbols into the title/summary characters so normal text appears at the end;
- arranging a title, prose paragraph, and action label as a conventional typography stack;
- sampling thousands of tiny random points that preserve the idea but cannot be read;
- fading the artwork down until an independent-looking text block becomes the composition.
- surrounding the event copy with a complete or partial border derived from its text bounds;
- confining the source art or focused topology to a small island in the middle of the display;
- clipping, ellipsizing, or silently dropping event copy because a fixed line count was exhausted;
- adding geometry whose only justification is that it looks generically futuristic.

Semantic meaning must emerge from the **spatial arrangement of retained ASCII material**. If the
semantic particles are removed, both the language and the relevant part of the artwork disappear.

## Render pipeline

```text
procedural scene mask
        ↓
ambient glyph field
        ↓
select persistent morph topology
        ↓
compile concise event language into macro-glyph stroke targets
        ↓
compile full-screen subsystem hardware independently of text bounds
        ↓
assign each stroke cell to an actual source ASCII glyph
        ↓
deform remaining source glyphs around exact macro-glyph strokes
        ↓
source ASCII art → readable ASCII macro-topology → result topology → source ASCII art
```

The static base raster exists only as the low-cost ambient representation. During reveal it hands
off to up to 6,500 persistent source glyphs in Lux mode and remains only as a very quiet source
imprint. Those same source glyphs then:

- move from their original artwork coordinates;
- change size and opacity;
- retain recognizable source-symbol identity in the settled semantic state;
- collectively form readable title strokes, concise signal lines, metadata, and technical rails;
- preserve the wallpaper's identity through and around the semantic macro-forms;
- reorganize again for the simulated result;
- reverse back into the original source artwork during collapse.

## Readability without typesetting

Readability comes from a deliberate macro-glyph grid or stroke topology, strong hierarchy, and
local displacement—not a dark rectangle and not ordinary `Canvas` text. A title may use larger
stroke cells than a detail line, but both remain visibly made from recruited ASCII symbols.

Copy is measured before targets are compiled. The title and supporting copy wrap against curved-
screen-safe bounds, dynamically select a readable size, split unusually long tokens, and preserve
all accepted event content instead of cutting the last line off. Filler glyphs move only where they
collide with exact semantic strokes; the surrounding topology remains intact.

No event `Bitmap`, font-outline bitmap, text canvas, ordinary character target line, or global
reading cavity is created.

## Successive events

The source topology is retained for the lifetime of a scene. A new notification compiles a new set
of macro-glyph and structural destinations for those same particles:

```text
event A macro-topology → event B macro-topology → event C macro-topology
```

It must not rebuild the artwork, cut to ambient, crossfade two scenes, or place new text over the
old event. Every visible semantic particle receives a new destination and travels there.

## Full-screen system intent

Every theme has an operational identity. Decorative geometry is rejected unless it communicates
that identity and participates in the same retained-glyph transform:

- **Defense:** an armored visor acquires top-to-bottom and deploys bilateral plates.
- **Reactor:** a core ignites first and routes energy outward through conduits.
- **Navigation:** waypoints synchronize clockwise and resolve into one deliberate route.
- **Network:** packets enter from the physical edges and route toward a central bus.
- **Biometric:** paired strands decode progressively from top to bottom.
- **Vault:** the central seal releases before nested gates open outward.
- **Temporal:** calibrated ticks resolve in clock order and become a timeline.
- **Sensor:** rays acquire upward from a lower origin and a scan plane confirms the event.
- **Analysis:** a perspective data deck rises upward and locks onto measured markers.

Both ambient and focused states must read as **full-screen ASCII fields**. Significant topology must
reach the side, upper, lower, and corner zones while preserving calm clock and system-gesture safe
areas. Supporting geometry is composed in screen space and cleared only from exact macro-letter
strokes; it must never be computed from `semanticBounds` or resemble an event container.

Motion is ordered by the subsystem job, not randomized for spectacle. Structure activates first,
then metadata, identification, and supporting intelligence resolve. Once legible, semantic glyphs
remain perfectly stationary while structural glyphs receive only a brief, low-amplitude operational
tail.

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
- **Focused:** semantic macro-glyphs become still; only transformed background glyphs retain restrained life.
- **Event transition:** the same semantic particles move directly from the old event topology into the new one.
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
7. A focused screenshot resembles a normal title/paragraph/action text layout.
8. A semantic stroke is carried by a literal content character instead of multiple source symbols.
9. The event is technically particle-based but is not immediately readable at normal phone distance.
10. A successive event rebuilds or crossfades the scene instead of retargeting the same topology.
11. Ambient or focused art occupies only a central island instead of the whole display.
12. Supporting glyphs form a complete or partial border around the event copy.
13. Any accepted title or summary character is clipped because of width or fixed line limits.
14. Geometry has no identifiable subsystem purpose beyond looking abstract or futuristic.
15. Particle timing is generic randomness instead of expressing acquisition, ignition, routing,
    unlocking, scanning, or analysis.
