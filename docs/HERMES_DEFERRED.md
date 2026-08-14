# Hermes Integration: Deferred Boundary

Hermes is not part of Prototype 0.

The repository contains no:

- VPC endpoint
- agent protocol
- API key
- network permission
- connector
- speech upload
- OAuth scope
- action executor
- background event sync

The future integration point is deliberately narrow:

```text
Future Context Capsule
        ↓
SemanticPresentation
  eventClass
  urgency
  title
  summary
  actionHint
  visualMode
  semanticTokens
        ↓
Existing local renderer
```

Hermes should eventually return meaning, not frame-by-frame ASCII or arbitrary UI. The Android renderer will continue to own typography, motion, privacy masking, safe areas, and performance.

Before adding that boundary, the visual proof must pass `PROTOTYPE_0.md`.
