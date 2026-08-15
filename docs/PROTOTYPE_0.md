# Prototype 0: Visual Gate

## Objective

Prove that a lock-screen artwork can transition from an ambient, wallpaper-worthy form into useful information while remaining one coherent composition.

This phase does not test intelligence, connectors, autonomy, or cloud infrastructure. Demo events are static by design.

## Core hypothesis

> Text can begin as texture, become structure during motion, and resolve into language without feeling like a conventional notification card.

## Included fixtures

| Fixture | Signal | Visual emphasis | Simulated result |
|---|---|---|---|
| Important mail | A meeting moved | Cool converging glyphs | Availability checked and draft prepared |
| Calendar conflict | Travel overlap | Warm intersecting topology | Alternative slot found |
| Deployment | Rollback completed | Violet structured signal | Cause isolated |

## State machine

```text
AMBIENT
  ↓ wake or tap
REVEALING
  ↓
FOCUSED
  ↓ long hold
LISTENING
  ↓ local timer
RESULT TRANSITION
  ↓
RESULT
  ↓ tap or swipe up
COLLAPSING
  ↓
AMBIENT
```

## Acceptance criteria

### Aesthetic

- The ambient scene works as a standalone wallpaper.
- The result does not look like a card pasted on top.
- The artwork preserves negative space around the Android clock and unlock affordances.
- Motion has a clear direction and does not resemble random particle noise.
- Urgency is conveyed through density, motion, and composition—not color alone.

### Legibility

- Final title can be read immediately at normal phone distance.
- Summary is limited to approximately four lines.
- Only one event receives full focus.
- The reading state becomes mostly static.
- The event can collapse without leaving visual debris.

### Performance

- No network access occurs on the wake path.
- Dense artwork is pre-rendered; only the morph subset moves per frame.
- Hidden wallpaper runs no frame loop.
- The renderer caps its internal resolution to constrain memory.
- Scene construction occurs away from the UI/render thread.

### Interaction

- Tap reveals.
- Long hold reaches listening and result states.
- Left/right changes event.
- Up collapses.
- Preview and wallpaper use the same semantic interaction model.

## Review protocol

Evaluate on a real Android phone in a dark room and in daylight.

Ask reviewers only these questions:

1. Would you keep the resting artwork as a wallpaper?
2. Did the artwork transform, or did a UI panel appear?
3. What changed in the event?
4. Did the motion help you understand urgency?
5. Did any part feel noisy, cheap, or terminal-like?

Hermes work begins only after answers 1–3 are consistently positive.
