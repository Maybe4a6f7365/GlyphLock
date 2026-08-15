import './style.css'

type ThemeId =
  | 'sentinel'
  | 'moth'
  | 'orbit'
  | 'neural_halo'
  | 'cipher_cathedral'
  | 'quantum_lattice'
  | 'fusion_core'
  | 'packet_bloom'
  | 'event_horizon'
  | 'tesseract_engine'
  | 'helix_array'
  | 'interference_field'
  | 'cryo_vault'
  | 'dyson_relay'
  | 'spectral_observatory'
  | 'recursive_monolith'
type EventId = 'mail' | 'calendar' | 'github' | 'security' | 'network' | 'model'
type MotionStyle = 'flow' | 'orbital' | 'circuit' | 'radial' | 'bloom' | 'wave' | 'fold'
type CompositionStyle = 'figure' | 'core' | 'orbital_band' | 'architecture' | 'splice' | 'field'
type TargetRole = 'title' | 'summary' | 'meta' | 'action' | 'structure'
type PushAxis = 'horizontal' | 'vertical' | 'radial'
type MotionProfile = 'calm' | 'cinematic' | 'hyper'
type VisualState = 'ambient' | 'revealing' | 'focused' | 'listening' | 'resultTransition' | 'result' | 'collapsing'

interface DemoEvent {
  id: EventId
  eyebrow: string
  title: string
  summary: string
  action: string
  resultTitle: string
  resultSummary: string
  glyphs: string
  accent: [number, number, number]
}

interface GlyphPoint {
  x: number
  y: number
  char: string
  alpha: number
  size: number
}

interface AmbientGlyph extends GlyphPoint {
  phase: number
  depth: number
}

interface TargetGlyph {
  x: number
  y: number
  char: string
  alpha: number
  size: number
  color: [number, number, number]
  text: boolean
  role: TargetRole
}

interface TextBand {
  bounds: { left: number; top: number; right: number; bottom: number }
  pushAxis: PushAxis
}

interface TargetLayout {
  targets: TargetGlyph[]
  textBands: TextBand[]
  bounds: { left: number; top: number; right: number; bottom: number }
  centerX: number
  centerY: number
}

interface MorphGlyph {
  source: GlyphPoint
  eventTarget: TargetGlyph
  resultTarget: TargetGlyph
  phase: number
  arc: number
  delay: number
  duration: number
}

interface ThemeSpec {
  id: ThemeId
  label: string
  semanticY: number
  semanticWidth: number
  palette: string
  tint: [number, number, number]
  atmosphereY: number
  densityBias: number
  motion: MotionStyle
  composition: CompositionStyle
  motionStrength: number
}

const EVENTS: DemoEvent[] = [
  {
    id: 'mail', eyebrow: 'IMPORTANT MAIL · 2 MIN AGO', title: 'DESIGN REVIEW MOVED',
    summary: 'Maya moved tomorrow’s review to 09:30. Current travel time leaves a ten-minute conflict.',
    action: 'HOLD TO CHECK CALENDAR', resultTitle: 'DRAFT READY',
    resultSummary: 'Thursday after 14:00 is clear. A concise reply is prepared for review.',
    glyphs: '@<>[]/\\MAYA0930', accent: [159, 221, 238],
  },
  {
    id: 'calendar', eyebrow: 'CALENDAR · CONFLICT', title: 'FRIDAY COLLISION',
    summary: 'Client review begins at 10:00. Your train arrives at 10:08 and the next free slot is 11:30.',
    action: 'HOLD TO FIND A SLOT', resultTitle: 'OPTION FOUND',
    resultSummary: '11:30 keeps every attendee and removes the travel risk. Ready to propose.',
    glyphs: ':|+−09101130', accent: [213, 204, 166],
  },
  {
    id: 'github', eyebrow: 'GITHUB · PRODUCTION', title: 'DEPLOYMENT RECOVERED',
    summary: 'The image built successfully. Migration failed after 42 seconds and rollback completed without downtime.',
    action: 'HOLD TO SHOW THE CAUSE', resultTitle: 'CAUSE ISOLATED',
    resultSummary: 'A missing index exceeded the migration window. A safe remediation plan is staged.',
    glyphs: '{}[]/#01FAIL42', accent: [186, 178, 238],
  },
  {
    id: 'security', eyebrow: 'SECURITY · PRIVATE EDGE', title: 'TLS ROTATION DUE',
    summary: 'The edge certificate expires in 36 hours. Two private services still pin the previous chain.',
    action: 'HOLD TO STAGE ROTATION', resultTitle: 'ROTATION PLAN READY',
    resultSummary: 'The new chain validates everywhere. Two pinned services are isolated for a controlled restart.',
    glyphs: 'X509TLS256[]{}CERT', accent: [246, 194, 119],
  },
  {
    id: 'network', eyebrow: 'NETWORK · HOME LAB', title: 'PACKET LOSS CLEARED',
    summary: 'A WireGuard route flap caused 3.8 percent loss for ninety seconds. Traffic is stable again.',
    action: 'HOLD TO TRACE THE PATH', resultTitle: 'ROUTE IDENTIFIED',
    resultSummary: 'An overlapping failover rule created the flap. A deterministic route order is ready.',
    glyphs: 'TCPIPWG0138:<>/\\', accent: [166, 240, 193],
  },
  {
    id: 'model', eyebrow: 'MODEL LAB · RUN 18420', title: 'SIMULATION CONVERGED',
    summary: 'The run reached target loss after 18,420 steps. One checkpoint dominates the evaluation set.',
    action: 'HOLD TO INSPECT THE RUN', resultTitle: 'CHECKPOINT SELECTED',
    resultSummary: 'Step 17,960 has the best stability margin and no regression on the private test slice.',
    glyphs: 'λψ∇LOSS18420<>01', accent: [196, 178, 255],
  },
]

const THEMES: ThemeSpec[] = [
  { id: 'sentinel', label: 'Sentinel', semanticY: .53, semanticWidth: .72, palette: ' .·,:;+=x1I|/\\()[]{}<>#08@', tint: [206, 229, 240], atmosphereY: 650, densityBias: 0, motion: 'flow', composition: 'figure', motionStrength: .72 },
  { id: 'moth', label: 'Moth', semanticY: .51, semanticWidth: .69, palette: ' .·,:;~+xvV(){}<>*#@', tint: [230, 219, 188], atmosphereY: 910, densityBias: .006, motion: 'bloom', composition: 'figure', motionStrength: .88 },
  { id: 'orbit', label: 'Orbit', semanticY: .55, semanticWidth: .72, palette: ' .·,:;~<>/\\()0O@', tint: [208, 199, 244], atmosphereY: 815, densityBias: -.004, motion: 'orbital', composition: 'orbital_band', motionStrength: .86 },
  { id: 'neural_halo', label: 'Neural Halo', semanticY: .53, semanticWidth: .68, palette: ' .·:;~λψ∇01+<>@', tint: [137, 235, 221], atmosphereY: 840, densityBias: .012, motion: 'radial', composition: 'core', motionStrength: 1.05 },
  { id: 'cipher_cathedral', label: 'Cipher Cathedral', semanticY: .53, semanticWidth: .62, palette: ' .:;|[]{}0x#AF16+-', tint: [246, 194, 119], atmosphereY: 930, densityBias: .010, motion: 'circuit', composition: 'architecture', motionStrength: .92 },
  { id: 'quantum_lattice', label: 'Quantum Lattice', semanticY: .56, semanticWidth: .72, palette: ' .·:~λψ∂∇∞01()<>@', tint: [196, 178, 255], atmosphereY: 910, densityBias: .008, motion: 'wave', composition: 'field', motionStrength: 1.08 },
  { id: 'fusion_core', label: 'Fusion Core', semanticY: .55, semanticWidth: .66, palette: ' .:;=+⊙○()[]|01#@', tint: [126, 221, 250], atmosphereY: 930, densityBias: .004, motion: 'orbital', composition: 'core', motionStrength: 1.18 },
  { id: 'packet_bloom', label: 'Packet Bloom', semanticY: .55, semanticWidth: .68, palette: ' .·:;<>[]{}:/\\01TCPIP+@', tint: [166, 240, 193], atmosphereY: 990, densityBias: .012, motion: 'bloom', composition: 'core', motionStrength: 1.05 },
  { id: 'event_horizon', label: 'Event Horizon', semanticY: .54, semanticWidth: .70, palette: ' .·:;~O0()[]<>∞λ01#@', tint: [255, 173, 112], atmosphereY: 920, densityBias: .010, motion: 'orbital', composition: 'orbital_band', motionStrength: 1.24 },
  { id: 'tesseract_engine', label: 'Tesseract Engine', semanticY: .52, semanticWidth: .60, palette: ' .:;|+-=[]{}<>01XYZW', tint: [164, 211, 255], atmosphereY: 870, densityBias: .008, motion: 'fold', composition: 'architecture', motionStrength: 1.08 },
  { id: 'helix_array', label: 'Helix Array', semanticY: .56, semanticWidth: .62, palette: ' .·:;~ATCGλψ01/\\()[]', tint: [151, 238, 211], atmosphereY: 940, densityBias: .012, motion: 'flow', composition: 'splice', motionStrength: 1.03 },
  { id: 'interference_field', label: 'Interference Field', semanticY: .54, semanticWidth: .72, palette: ' .·:;~≈∿λψ01()<>+@', tint: [239, 176, 242], atmosphereY: 840, densityBias: .006, motion: 'wave', composition: 'field', motionStrength: 1.12 },
  { id: 'cryo_vault', label: 'Cryo Vault', semanticY: .56, semanticWidth: .58, palette: ' .:;|[]{}HEXICE01+*', tint: [182, 232, 255], atmosphereY: 980, densityBias: .008, motion: 'circuit', composition: 'architecture', motionStrength: .96 },
  { id: 'dyson_relay', label: 'Dyson Relay', semanticY: .55, semanticWidth: .66, palette: ' .·:;O0()[]{}<>01+*#@', tint: [255, 214, 140], atmosphereY: 860, densityBias: .010, motion: 'orbital', composition: 'orbital_band', motionStrength: 1.14 },
  { id: 'spectral_observatory', label: 'Spectral Observatory', semanticY: .55, semanticWidth: .72, palette: ' .·:;~≈∿RFHz01[]<>/\\', tint: [145, 225, 255], atmosphereY: 936, densityBias: .011, motion: 'wave', composition: 'field', motionStrength: 1.08 },
  { id: 'recursive_monolith', label: 'Recursive Monolith', semanticY: .54, semanticWidth: .58, palette: ' .:;|+-=[]{}<>01∞', tint: [221, 205, 255], atmosphereY: 912, densityBias: .009, motion: 'fold', composition: 'architecture', motionStrength: 1.10 },
]

const THEME_BY_ID = new Map(THEMES.map(theme => [theme.id, theme] as const))
const MOTION_PROFILES: Record<MotionProfile, { label: string; speed: number; amplitude: number; liveCount: number; morphCount: number }> = {
  calm: { label: 'Calm', speed: .52, amplitude: .58, liveCount: 760, morphCount: 2200 },
  cinematic: { label: 'Cinematic', speed: 1, amplitude: 1, liveCount: 1450, morphCount: 4200 },
  hyper: { label: 'Hyper', speed: 1.42, amplitude: 1.36, liveCount: 1900, morphCount: 5000 },
}

const INTERNAL_W = 1080
const INTERNAL_H = 2400
const GLYPH_STEP = 10
const RAMP = '  .·,:;+i1tfLCG08@'
const BASE_GLYPHS = ' .·,:;+=x1I|/\\()[]{}<>#08@'
const STRUCTURE_GLYPHS = '·.:;|/\\+−=[]{}<>01'
const FONT_STACK = '"Roboto Mono", "SFMono-Regular", monospace'

const clamp01 = (v: number) => Math.max(0, Math.min(1, v))
const mix = (a: number, b: number, t: number) => a + (b - a) * t
const smooth = (t: number) => { const x = clamp01(t); return x * x * (3 - 2 * x) }
const ease = (t: number) => 1 - Math.pow(1 - clamp01(t), 3)
const hash = (x: number, y: number, seed = 0) => {
  const n = Math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453123
  return n - Math.floor(n)
}
const mixColor = (a: [number, number, number], b: [number, number, number], t: number): [number, number, number] => [
  Math.round(mix(a[0], b[0], t)),
  Math.round(mix(a[1], b[1], t)),
  Math.round(mix(a[2], b[2], t)),
]

class GlyphLockRenderer {
  readonly canvas: HTMLCanvasElement
  readonly ctx: CanvasRenderingContext2D
  private theme: ThemeId = 'sentinel'
  private eventIndex = 0
  private state: VisualState = 'ambient'
  private transitionStart = performance.now()
  private transitionDuration = 2450
  private resultStart = 0
  private masks = new Map<ThemeId, HTMLImageElement>()
  private maskCanvas = document.createElement('canvas')
  private maskCtx = this.maskCanvas.getContext('2d', { willReadFrequently: true })!
  private baseCanvas = document.createElement('canvas')
  private basePoints: GlyphPoint[] = []
  private ambientGlyphs: AmbientGlyph[] = []
  private morphGlyphs: MorphGlyph[] = []
  private motionProfile: MotionProfile = 'cinematic'
  private wakeStart = performance.now()
  private pointerParallax = { x: 0, y: 0 }
  private captureMotionMs: number | null = null
  private raf = 0
  private pointerStart: { x: number; y: number; at: number } | null = null
  private holdTimer = 0
  private captureT: number | null = null

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas
    const ctx = canvas.getContext('2d', { alpha: false })
    if (!ctx) throw new Error('Canvas 2D is unavailable')
    this.ctx = ctx
    for (const c of [canvas, this.maskCanvas, this.baseCanvas]) {
      c.width = INTERNAL_W
      c.height = INTERNAL_H
    }
    this.attachInput()
  }

  get currentTheme(): ThemeId { return this.theme }
  get currentEventId(): EventId { return EVENTS[this.eventIndex]!.id }
  get currentMotionProfile(): MotionProfile { return this.motionProfile }
  private get themeSpec(): ThemeSpec { return THEME_BY_ID.get(this.theme)! }
  private get motionSpec() { return MOTION_PROFILES[this.motionProfile] }
  private get event() { return EVENTS[this.eventIndex]! }

  async load(): Promise<void> {
    await Promise.all(THEMES.map(async ({ id }) => {
      const image = new Image()
      image.decoding = 'async'
      image.src = `/assets/scene_${id}.png`
      await image.decode()
      this.masks.set(id, image)
    }))
    this.rebuildScene()
    this.wake()
    if (!new URLSearchParams(location.search).has('capture')) this.raf = requestAnimationFrame(this.render)
  }

  setTheme(id: ThemeId): void {
    if (id === this.theme) return
    this.theme = id
    this.state = 'ambient'
    this.rebuildScene()
    this.wake()
  }

  setMotionProfile(profile: MotionProfile): void {
    if (profile === this.motionProfile) return
    this.motionProfile = profile
    this.rebuildTopology()
    this.wake()
  }

  wake(): void { this.wakeStart = performance.now() }

  setEvent(id: EventId): void {
    const index = EVENTS.findIndex(event => event.id === id)
    if (index < 0 || index === this.eventIndex) return
    this.eventIndex = index
    this.rebuildTopology()
    this.reveal()
  }

  nextEvent(direction = 1): void {
    this.eventIndex = (this.eventIndex + direction + EVENTS.length) % EVENTS.length
    this.rebuildTopology()
    this.reveal()
    syncControls(this.theme, this.event.id)
  }

  reveal(): void {
    this.state = 'revealing'
    this.transitionStart = performance.now()
    this.transitionDuration = 2450
  }

  collapse(): void {
    if (this.state === 'ambient') return
    this.state = 'collapsing'
    this.transitionStart = performance.now()
    this.transitionDuration = 1900
  }

  listen(): void {
    if (this.state === 'ambient') this.reveal()
    this.state = 'listening'
    this.transitionStart = performance.now()
    window.setTimeout(() => {
      if (this.state !== 'listening') return
      this.state = 'resultTransition'
      this.resultStart = performance.now()
    }, 1850)
  }

  setCaptureTime(t: number): void {
    this.captureT = clamp01(t)
    this.state = 'revealing'
    this.draw(performance.now())
  }

  setCaptureMotionTime(ms: number): void {
    this.captureMotionMs = Math.max(0, ms)
    this.draw(performance.now())
  }

  private rebuildScene(): void {
    const image = this.masks.get(this.theme)
    if (!image) return
    this.maskCtx.clearRect(0, 0, INTERNAL_W, INTERNAL_H)
    this.maskCtx.drawImage(image, 0, 0, INTERNAL_W, INTERNAL_H)
    this.basePoints = this.extractGlyphPoints()
    this.renderBaseBitmap()
    this.rebuildAmbientGlyphs()
    this.rebuildTopology()
  }

  private rebuildAmbientGlyphs(): void {
    const candidates = this.basePoints.filter(point => point.alpha > .18 && point.y > 170 && point.y < INTERNAL_H - 120)
    const count = Math.min(this.motionSpec.liveCount, candidates.length)
    const glyphs: AmbientGlyph[] = []
    for (let i = 0; i < count; i++) {
      const point = candidates[(i * 97 + this.theme.length * 53) % Math.max(1, candidates.length)]
      if (!point) continue
      glyphs.push({ ...point, phase: hash(i, point.x, point.y) * Math.PI * 2, depth: .25 + .75 * hash(point.y, i, 11) })
    }
    this.ambientGlyphs = glyphs
  }

  private rebuildTopology(): void {
    if (this.basePoints.length === 0) return
    const sources = this.selectMorphSources()
    const eventLayout = this.buildTargetLayout(false)
    const resultLayout = this.buildTargetLayout(true)
    const eventAssignments = this.assignNearestTargets(sources, eventLayout.targets)
    const resultAssignments = this.assignNearestTargets(sources, resultLayout.targets)
    const focusX = INTERNAL_W / 2
    const focusY = this.themeSpec.atmosphereY
    const diagonal = Math.hypot(INTERNAL_W, INTERNAL_H)

    this.morphGlyphs = sources.map((source, index) => {
      const eventTarget = eventAssignments[index] ?? this.buildFillerTarget(source, eventLayout, index, false)
      const resultTarget = resultAssignments[index] ?? this.buildFillerTarget(source, resultLayout, index, true)
      const distance = Math.hypot(source.x - focusX, source.y - focusY) / diagonal
      const n = hash(index, source.x, source.y)
      return {
        source,
        eventTarget,
        resultTarget,
        phase: n * Math.PI * 2,
        arc: (28 + n * 88) * this.themeSpec.motionStrength,
        delay: .01 + .225 * distance + this.roleDelay(eventTarget.role) + .045 * n,
        duration: .56 + .13 * (1 - distance) + .055 * n,
      }
    })
  }

  private roleDelay(role: TargetRole): number {
    if (role === 'title') return 0
    if (role === 'summary') return .026
    if (role === 'meta') return .052
    if (role === 'action') return .074
    return .095
  }

  private selectMorphSources(): GlyphPoint[] {
    const candidates = this.basePoints.filter(point => point.alpha > .15)
    if (candidates.length === 0) candidates.push(...this.basePoints)
    const count = Math.min(this.motionSpec.morphCount, candidates.length)
    const selected: GlyphPoint[] = []
    const used = new Uint8Array(candidates.length)
    let cursor = (this.theme.length * 149) % Math.max(1, candidates.length)
    for (let i = 0; i < count; i++) {
      cursor = (cursor + 97) % candidates.length
      while (used[cursor]) cursor = (cursor + 1) % candidates.length
      used[cursor] = 1
      selected.push(candidates[cursor]!)
    }
    return selected
  }

  private buildTargetLayout(result: boolean): TargetLayout {
    const event = this.event
    const targets: TargetGlyph[] = []
    const textBands: TextBand[] = []
    const spec = this.themeSpec
    const centerX = INTERNAL_W / 2
    const contentWidth = INTERNAL_W * spec.semanticWidth
    const left = centerX - contentWidth / 2
    const right = centerX + contentWidth / 2
    const anchorY = INTERNAL_H * spec.semanticY
    const topOffset = spec.composition === 'architecture' ? INTERNAL_H * .125
      : spec.composition === 'splice' ? INTERNAL_H * .118
      : spec.composition === 'field' ? INTERNAL_H * .112
      : spec.composition === 'orbital_band' ? INTERNAL_H * .105
      : INTERNAL_H * .108
    const top = anchorY - topOffset
    const accent = event.accent

    const eyebrow = result ? 'LOCAL DEMO · ACTION SIMULATED' : event.eyebrow
    const title = result ? event.resultTitle : event.title
    const summary = result ? event.resultSummary : event.summary
    const action = result ? 'TAP TO RETURN' : event.action
    const eyebrowSize = 20
    const titleSize = this.fitTextSize(title, contentWidth, 56, 35, 520)
    const summarySize = 27
    const actionSize = 19

    const metaY = top + eyebrowSize
    const titleY = metaY + INTERNAL_H * .034 + titleSize
    let summaryY = titleY + INTERNAL_H * .026
    const summaryLines = this.wrapText(summary, contentWidth, summarySize, 3)
    const summaryBaselines: number[] = []
    for (const _line of summaryLines) {
      summaryY += summarySize * 1.44
      summaryBaselines.push(summaryY)
    }
    const actionY = summaryY + INTERNAL_H * .034 + actionSize

    const primaryAlign: CanvasTextAlign = spec.composition === 'architecture' ? 'left' : 'center'
    const metaAlign: CanvasTextAlign = spec.composition === 'field' ? 'left' : primaryAlign
    const actionAlign: CanvasTextAlign = spec.composition === 'field' ? 'right' : primaryAlign
    const primaryAnchor = primaryAlign === 'left' ? left : centerX
    const metaAnchor = metaAlign === 'left' ? left : centerX
    const actionAnchor = actionAlign === 'right' ? right : primaryAnchor
    const pushAxis = this.pushAxisFor(spec.composition)

    // High-value language targets are assigned first; the remaining source field becomes structure.
    this.addAlignedLineTargets(targets, textBands, title, primaryAnchor, titleY, titleSize, [241, 247, 250], 1, primaryAlign, 'title', pushAxis, 520)
    summaryLines.forEach((line, index) => {
      const lineAnchor = spec.composition === 'field' ? centerX + (index % 2 === 0 ? -27 : 27) : primaryAnchor
      const lineAlign: CanvasTextAlign = spec.composition === 'field' ? 'center' : primaryAlign
      this.addAlignedLineTargets(targets, textBands, line, lineAnchor, summaryBaselines[index]!, summarySize, [214, 225, 231], .90, lineAlign, 'summary', pushAxis, 400)
    })
    this.addAlignedLineTargets(targets, textBands, eyebrow, metaAnchor, metaY, eyebrowSize, accent, .96, metaAlign, 'meta', pushAxis, 600)
    this.addAlignedLineTargets(targets, textBands, action, actionAnchor, actionY, actionSize, accent, .98, actionAlign, 'action', pushAxis, 600)

    const bounds = {
      left: left - INTERNAL_W * .035,
      top: top - INTERNAL_H * .015,
      right: right + INTERNAL_W * .035,
      bottom: actionY + INTERNAL_H * .022,
    }
    this.addSemanticStructure(targets, bounds, result)
    return { targets, textBands, bounds, centerX, centerY: anchorY }
  }

  private pushAxisFor(style: CompositionStyle): PushAxis {
    if (style === 'architecture' || style === 'splice') return 'horizontal'
    if (style === 'field') return 'vertical'
    return 'radial'
  }

  private addAlignedLineTargets(
    targets: TargetGlyph[], bands: TextBand[], text: string, anchorX: number, baseline: number,
    size: number, color: [number, number, number], alpha: number, align: CanvasTextAlign,
    role: TargetRole, pushAxis: PushAxis, weight: number,
  ): void {
    this.ctx.font = `${weight} ${size}px ${FONT_STACK}`
    const width = this.ctx.measureText(text).width
    const startX = align === 'center' ? anchorX - width / 2 : align === 'right' ? anchorX - width : anchorX
    let x = startX
    for (const char of text) {
      const advance = this.ctx.measureText(char).width
      if (!/\s/.test(char)) targets.push({ x: x + advance / 2, y: baseline, char, size, alpha, color, text: true, role })
      x += advance
    }
    const padX = size * .52
    bands.push({
      bounds: {
        left: startX - padX,
        top: baseline - size * 1.26,
        right: startX + width + padX,
        bottom: baseline + size * .52,
      },
      pushAxis,
    })
  }

  private addSemanticStructure(
    targets: TargetGlyph[], bounds: { left: number; top: number; right: number; bottom: number }, result: boolean,
  ): void {
    const color = mixColor(this.themeSpec.tint, this.event.accent, result ? .58 : .46)
    const centerX = (bounds.left + bounds.right) / 2
    const centerY = (bounds.top + bounds.bottom) / 2
    const width = bounds.right - bounds.left
    const height = bounds.bottom - bounds.top
    const add = (x: number, y: number, index: number, alpha: number, size = 11.5) => {
      const char = STRUCTURE_GLYPHS[((index * 5 + 3) % STRUCTURE_GLYPHS.length + STRUCTURE_GLYPHS.length) % STRUCTURE_GLYPHS.length] ?? '·'
      targets.push({ x, y, char, size, alpha, color, text: false, role: 'structure' })
    }

    switch (this.themeSpec.composition) {
      case 'figure':
        for (let i = 0; i < 68; i++) {
          const t = i / 67
          const y = mix(bounds.top - INTERNAL_H * .070, bounds.bottom + INTERNAL_H * .080, t)
          const spread = INTERNAL_W * (.235 + .115 * Math.sin(t * Math.PI))
          const pulse = Math.sin(t * Math.PI * 5) * INTERNAL_W * .012
          add(centerX - spread - pulse, y, i, .43)
          add(centerX + spread + pulse, y, i + 31, .43)
          if (i % 4 === 0) add(centerX + Math.sin(t * Math.PI * 6) * 16, y, i + 61, .30)
        }
        break
      case 'core':
        for (let ring = 0; ring < 4; ring++) {
          const rx = width * (.54 + ring * .090), ry = height * (.54 + ring * .105)
          const count = 54 + ring * 16
          for (let i = 0; i < count; i++) {
            if ((i + ring * 2) % 8 === 0) continue
            const a = Math.PI * 2 * i / count
            add(centerX + Math.cos(a) * rx, centerY + Math.sin(a) * ry, i + ring * 73, .42 - ring * .055)
          }
        }
        break
      case 'orbital_band':
        for (let band = 0; band < 2; band++) {
          const count = band === 0 ? 128 : 96
          for (let i = 0; i < count; i++) {
            const a = Math.PI * 2 * i / count + band * .21
            if (Math.abs(Math.sin(a)) < .12 && Math.cos(a) > 0 && i % 3 !== 0) continue
            const rx = width * (.57 + band * .12 + .04 * Math.sin(a * 3))
            const ry = height * (.62 + band * .11)
            add(centerX + Math.cos(a) * rx, centerY + Math.sin(a) * ry, i + band * 131, band === 0 ? .43 : .27)
          }
        }
        break
      case 'architecture':
        for (let i = 0; i < 72; i++) {
          const y = mix(bounds.top - INTERNAL_H * .055, bounds.bottom + INTERNAL_H * .065, i / 71)
          if (i % 5 !== 1) {
            add(bounds.left - INTERNAL_W * .067, y, i, .43)
            add(bounds.right + INTERNAL_W * .067, y, i + 37, .36)
          }
          if (i % 6 === 0) add(bounds.left - INTERNAL_W * .025, y, i + 73, .28)
        }
        for (let rail = 0; rail < 2; rail++) {
          const y = rail === 0 ? bounds.top - INTERNAL_H * .038 : bounds.bottom + INTERNAL_H * .043
          for (let i = 0; i < 42; i++) {
            if (i > 14 && i < 27 && rail === 0) continue
            add(mix(bounds.left - 52, bounds.right + 52, i / 41), y, i + rail * 51, rail === 0 ? .36 : .24)
          }
        }
        break
      case 'splice':
        for (let i = 0; i < 76; i++) {
          const t = i / 75, y = mix(bounds.top - INTERNAL_H * .085, bounds.bottom + INTERNAL_H * .085, t)
          const wave = Math.sin(t * Math.PI * 10) * INTERNAL_W * .072
          add(bounds.left - 64 + wave, y, i, .43)
          add(bounds.right + 64 - wave, y, i + 43, .43)
          if (i % 3 === 0) add(centerX + Math.sin(t * Math.PI * 10) * 10, y, i + 89, .25)
        }
        break
      case 'field':
        for (let row = -4; row <= 4; row++) {
          const y = centerY + row * INTERNAL_H * .056
          for (let i = 0; i < 66; i++) {
            if ((i + row + 13) % 5 === 0) continue
            const t = i / 65, x = mix(bounds.left - 96, bounds.right + 96, t)
            const wave = Math.sin(t * Math.PI * 5 + row * .77) * INTERNAL_H * .011
            add(x, y + wave, i + row * 29, .22 + .035 * (4 - Math.abs(row)))
          }
        }
        break
    }
  }

  private assignNearestTargets(sources: GlyphPoint[], targets: TargetGlyph[]): Array<TargetGlyph | undefined> {
    const assignments: Array<TargetGlyph | undefined> = new Array(sources.length)
    const used = new Uint8Array(sources.length)
    const verticalWeight = INTERNAL_W / INTERNAL_H * 1.8
    for (const target of targets) {
      let best = -1
      let bestScore = Number.POSITIVE_INFINITY
      for (let i = 0; i < sources.length; i++) {
        if (used[i]) continue
        const source = sources[i]!
        const dx = source.x - target.x
        const dy = (source.y - target.y) * verticalWeight
        const roleBias = target.text ? INTERNAL_W * INTERNAL_W * .010 : INTERNAL_W * INTERNAL_W * .002
        const score = dx * dx + dy * dy - source.alpha * INTERNAL_W * INTERNAL_W * .012 - roleBias
        if (score < bestScore) { bestScore = score; best = i }
      }
      if (best < 0) break
      used[best] = 1
      assignments[best] = target
    }
    return assignments
  }

  private buildFillerTarget(
    source: GlyphPoint,
    layout: TargetLayout,
    index: number,
    result: boolean,
  ): TargetGlyph {
    const spec = this.themeSpec
    const focusX = INTERNAL_W / 2
    const focusY = spec.atmosphereY
    const vx = source.x - focusX
    const vy = source.y - focusY
    const radius = Math.max(1, Math.hypot(vx, vy))
    const angle = Math.atan2(vy, vx)
    const n = hash(index, source.x, result ? 31 : 23)
    const seed = this.event.id.length * .31 + (result ? .42 : 0)
    let x = source.x
    let y = source.y

    // Filler glyphs remain recognisably part of the source artwork. Motion is a
    // structural deformation, not an evacuation around a notification rectangle.
    switch (spec.motion) {
      case 'orbital': {
        const rotation = (.08 + n * .13) * (n > .5 ? 1 : -1)
        const scale = .96 + n * .10
        const ca = Math.cos(rotation), sa = Math.sin(rotation)
        x = focusX + (vx * ca - vy * sa) * scale
        y = focusY + (vx * sa + vy * ca) * scale
        break
      }
      case 'circuit': {
        const gridX = INTERNAL_W * .034, gridY = INTERNAL_H * .018
        const gx = Math.round((source.x + (n - .5) * INTERNAL_W * .045) / gridX) * gridX
        const gy = Math.round((source.y + (n - .5) * INTERNAL_H * .025) / gridY) * gridY
        x = mix(source.x, gx, .58); y = mix(source.y, gy, .58)
        break
      }
      case 'radial': {
        const ring = radius * (.96 + n * .11)
        x = focusX + Math.cos(angle + seed * .055) * ring
        y = focusY + Math.sin(angle + seed * .055) * ring
        break
      }
      case 'bloom': {
        const petal = Math.sin(angle * 6 + seed) * INTERNAL_W * .024
        const scale = .97 + n * .09 + petal / Math.max(INTERNAL_W, radius) * .46
        x = focusX + vx * scale; y = focusY + vy * scale
        break
      }
      case 'wave':
        x += Math.sin(source.y * .0105 + seed + n * 4.5) * INTERNAL_W * .030
        y += Math.sin(source.x * .0090 - seed + n * 3.5) * INTERNAL_H * .010
        break
      case 'fold': {
        const depth = (source.y - focusY) / Math.max(1, INTERNAL_H)
        const fold = (n > .5 ? 1 : -1) * Math.abs(depth) * INTERNAL_W * .075
        x = focusX + vx * (.95 + n * .08) + fold
        y = focusY + vy * (.97 + n * .055)
        break
      }
      case 'flow':
      default:
        x += Math.sin(source.y * .009 + seed + n * 4) * INTERNAL_W * .030
        y += Math.cos(source.x * .008 + seed + n * 5) * INTERNAL_H * .010
    }

    ;[x, y] = this.warpFillerAroundBands(x, y, layout, n)
    x = Math.max(INTERNAL_W * .018, Math.min(INTERNAL_W * .982, x))
    y = Math.max(INTERNAL_H * .035, Math.min(INTERNAL_H * .975, y))
    const vocabulary = this.event.glyphs + spec.palette + STRUCTURE_GLYPHS
    const char = n > .82 ? vocabulary[Math.min(vocabulary.length - 1, Math.floor(n * vocabulary.length))] ?? source.char : source.char
    return {
      x, y, char,
      size: source.size * (.93 + n * .15),
      alpha: clamp01(source.alpha * (result ? .64 : .72)),
      color: mixColor(spec.tint, this.event.accent, result ? .46 : .35),
      text: false,
      role: 'structure',
    }
  }

  private warpFillerAroundBands(x: number, y: number, layout: TargetLayout, noise: number): [number, number] {
    let px = x, py = y
    for (const band of layout.textBands) {
      const b = band.bounds
      if (px < b.left || px > b.right || py < b.top || py > b.bottom) continue
      const marginX = INTERNAL_W * (.006 + noise * .007)
      const marginY = INTERNAL_H * (.0025 + noise * .0035)
      if (band.pushAxis === 'horizontal') {
        px = Math.abs(px - b.left) <= Math.abs(b.right - px) ? b.left - marginX : b.right + marginX
      } else if (band.pushAxis === 'vertical') {
        py = Math.abs(py - b.top) <= Math.abs(b.bottom - py) ? b.top - marginY : b.bottom + marginY
      } else {
        const dx = px - layout.centerX, dy = py - layout.centerY
        const nx = dx / Math.max(1, (b.right - b.left) * .5)
        const ny = dy / Math.max(1, (b.bottom - b.top) * .5)
        if (Math.abs(nx) > Math.abs(ny)) px = dx < 0 ? b.left - marginX : b.right + marginX
        else py = dy < 0 ? b.top - marginY : b.bottom + marginY
      }
      break
    }
    return [px, py]
  }

  private extractGlyphPoints(): GlyphPoint[] {
    const pixels = this.maskCtx.getImageData(0, 0, INTERNAL_W, INTERNAL_H).data
    const points: GlyphPoint[] = []
    for (let y = 120; y < INTERNAL_H - 90; y += GLYPH_STEP) {
      for (let x = 30; x < INTERNAL_W - 30; x += GLYPH_STEP) {
        const i = (y * INTERNAL_W + x) * 4
        const value = pixels[i]! / 255
        const n = hash(x, y, this.theme.length)
        const threshold = .034 + n * .098 - this.themeSpec.densityBias
        if (value < threshold && n < .92) continue
        const level = clamp01(value * 1.12 + (n - .5) * .10)
        let char = RAMP[Math.min(RAMP.length - 1, Math.floor(level * RAMP.length))] ?? '.'
        if (level < .20 || n > .88) char = this.themeSpec.palette[Math.floor(n * this.themeSpec.palette.length)] ?? BASE_GLYPHS[Math.floor(n * BASE_GLYPHS.length)] ?? '.'
        points.push({ x: x + (n - .5) * 1.25, y: y + (hash(y, x, 3) - .5) * 1.15, char, alpha: clamp01(.08 + level * .94), size: 9 + level * 4.2 })
      }
    }
    return points
  }

  private renderBaseBitmap(): void {
    const ctx = this.baseCanvas.getContext('2d', { alpha: false })!
    ctx.fillStyle = '#000'
    ctx.fillRect(0, 0, INTERNAL_W, INTERNAL_H)
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const [tr, tg, tb] = this.themeSpec.tint
    for (const point of this.basePoints) {
      const value = Math.floor(72 + 182 * point.alpha)
      const tintMix = .12 + point.alpha * .13
      ctx.fillStyle = `rgba(${Math.round(mix(value, tr, tintMix))},${Math.round(mix(value, tg, tintMix))},${Math.round(mix(value, tb, tintMix))},${point.alpha * .92})`
      ctx.font = `${point.size}px ${FONT_STACK}`
      ctx.fillText(point.char, point.x, point.y)
    }
    const gradient = ctx.createRadialGradient(INTERNAL_W / 2, this.themeSpec.atmosphereY, 20, INTERNAL_W / 2, this.themeSpec.atmosphereY, 820)
    gradient.addColorStop(0, `rgba(${tr},${tg},${tb},.050)`)
    gradient.addColorStop(.45, `rgba(${tr},${tg},${tb},.018)`)
    gradient.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, INTERNAL_W, INTERNAL_H)
  }

  private drawMorphField(reveal: number, result: number, now: number, listening: boolean): void {
    if (reveal <= 0) return
    const ctx = this.ctx
    const spec = this.themeSpec
    const focusX = INTERNAL_W / 2
    const focusY = spec.atmosphereY
    const handoff = smooth(reveal / .16)
    const seconds = now / 1000
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'

    for (const glyph of this.morphGlyphs) {
      const local = smooth((reveal - glyph.delay) / Math.max(.001, glyph.duration))
      const moveT = ease(local)
      const wave = Math.sin(Math.PI * local)
      let eventX: number
      let eventY: number
      const tx = glyph.eventTarget.x, ty = glyph.eventTarget.y

      switch (spec.motion) {
        case 'circuit': {
          const first = smooth(Math.min(1, moveT * 1.82))
          const second = smooth(Math.max(0, (moveT - .45) / .55))
          if (Math.sin(glyph.phase) >= 0) { eventX = mix(glyph.source.x, tx, first); eventY = mix(glyph.source.y, ty, second) }
          else { eventY = mix(glyph.source.y, ty, first); eventX = mix(glyph.source.x, tx, second) }
          break
        }
        case 'orbital': {
          const midX = (glyph.source.x + tx) * .5, midY = (glyph.source.y + ty) * .5
          const vx = midX - focusX, vy = midY - focusY
          const length = Math.max(1, Math.hypot(vx, vy))
          const direction = Math.sin(glyph.phase) >= 0 ? 1 : -1
          const cx = midX - vy / length * glyph.arc * direction
          const cy = midY + vx / length * glyph.arc * direction
          const u = 1 - moveT
          eventX = u * u * glyph.source.x + 2 * u * moveT * cx + moveT * moveT * tx
          eventY = u * u * glyph.source.y + 2 * u * moveT * cy + moveT * moveT * ty
          break
        }
        case 'radial': {
          const tangent = glyph.phase + moveT * 2.4
          const cx = mix(glyph.source.x, focusX, .58) + Math.cos(tangent) * glyph.arc
          const cy = mix(glyph.source.y, focusY, .58) + Math.sin(tangent) * glyph.arc * .66
          const u = 1 - moveT
          eventX = u * u * glyph.source.x + 2 * u * moveT * cx + moveT * moveT * tx
          eventY = u * u * glyph.source.y + 2 * u * moveT * cy + moveT * moveT * ty
          break
        }
        case 'bloom': {
          const sourceAngle = Math.atan2(glyph.source.y - focusY, glyph.source.x - focusX)
          const petal = Math.sin(sourceAngle * 6 + glyph.phase) * glyph.arc
          const cx = focusX + Math.cos(sourceAngle) * (INTERNAL_W * .22 + petal)
          const cy = focusY + Math.sin(sourceAngle) * (INTERNAL_W * .18 + petal * .55)
          const u = 1 - moveT
          eventX = u * u * glyph.source.x + 2 * u * moveT * cx + moveT * moveT * tx
          eventY = u * u * glyph.source.y + 2 * u * moveT * cy + moveT * moveT * ty
          break
        }
        case 'wave':
          eventX = mix(glyph.source.x, tx, moveT) + Math.sin(glyph.source.y * .012 + glyph.phase + local * 7) * glyph.arc * .68 * wave
          eventY = mix(glyph.source.y, ty, moveT) + Math.sin(glyph.source.x * .010 - glyph.phase + local * 5.5) * glyph.arc * .34 * wave
          break
        case 'fold': {
          const direction = Math.sin(glyph.phase) >= 0 ? 1 : -1
          const cx = focusX + direction * glyph.arc * .55
          const cy = mix(glyph.source.y, ty, .48)
          const u = 1 - moveT
          eventX = u * u * glyph.source.x + 2 * u * moveT * cx + moveT * moveT * tx
          eventY = u * u * glyph.source.y + 2 * u * moveT * cy + moveT * moveT * ty
          break
        }
        case 'flow':
        default:
          eventX = mix(glyph.source.x, tx, moveT) + Math.sin(glyph.phase + local * 5.2) * glyph.arc * wave
          eventY = mix(glyph.source.y, ty, moveT) + Math.cos(glyph.phase * 1.3 + local * 4.1) * glyph.arc * .48 * wave
      }

      let x = mix(eventX, glyph.resultTarget.x, result)
      let y = mix(eventY, glyph.resultTarget.y, result)
      const resultWave = Math.sin(Math.PI * result)
      x += Math.sin(glyph.phase * 1.7 + result * 4) * glyph.arc * .13 * resultWave
      y += Math.cos(glyph.phase * 1.3 + result * 3) * glyph.arc * .07 * resultWave

      const activeTarget = result < .5 ? glyph.eventTarget : glyph.resultTarget
      if (!activeTarget.text && local >= .98) {
        const drift = spec.motionStrength * .62
        x += Math.sin(seconds * .34 + glyph.phase) * 2.7 * drift
        y += Math.cos(seconds * .27 + glyph.phase * 1.4) * 1.8 * drift
      }
      if (listening) {
        const cy = INTERNAL_H * .88
        const dx = x - INTERNAL_W / 2, dy = y - cy
        const distance = Math.max(1, Math.hypot(dx, dy))
        const pulse = Math.sin(seconds * 6.5 - distance * .020 + glyph.phase)
        const influence = Math.exp(-distance / Math.max(1, INTERNAL_W * .42))
        x += dx / distance * pulse * INTERNAL_W * .010 * influence
        y += dy / distance * pulse * INTERNAL_W * .010 * influence
      }

      const eventBlend = smooth((local - .20) / .62)
      const resultBlend = smooth((result - .14) / .72)
      let size = mix(glyph.source.size, glyph.eventTarget.size, eventBlend)
      size = mix(size, glyph.resultTarget.size, resultBlend)
      let alpha = mix(glyph.source.alpha * .94, glyph.eventTarget.alpha, eventBlend)
      alpha = mix(alpha, glyph.resultTarget.alpha, resultBlend) * handoff
      const eventColor = mixColor(spec.tint, glyph.eventTarget.color, eventBlend)
      const finalColor = mixColor(eventColor, glyph.resultTarget.color, resultBlend)
      if (activeTarget.text && local > .88) alpha = Math.max(alpha, activeTarget.alpha * .94)
      if (alpha <= .012) continue

      ctx.font = `${size}px ${FONT_STACK}`
      const eventCharBlend = smooth((local - this.charStartFor(glyph.eventTarget.role)) / .16)
      if (result <= .001) {
        this.drawGlyphTransition(glyph.source.char, glyph.eventTarget.char, eventCharBlend, x, y, alpha, finalColor)
      } else {
        const eventChar = eventCharBlend >= .5 ? glyph.eventTarget.char : glyph.source.char
        const resultCharBlend = smooth((result - .40) / .18)
        this.drawGlyphTransition(eventChar, glyph.resultTarget.char, resultCharBlend, x, y, alpha, finalColor)
      }
    }

    this.drawMorphWave(reveal, result)
  }

  private charStartFor(role: TargetRole): number {
    if (role === 'title') return .30
    if (role === 'summary') return .38
    if (role === 'meta') return .46
    if (role === 'action') return .50
    return .44
  }

  private drawGlyphTransition(
    from: string, to: string, blend: number, x: number, y: number,
    alpha: number, color: [number, number, number],
  ): void {
    const [r, g, b] = color
    if (from === to || blend >= .84) {
      this.ctx.fillStyle = `rgba(${r},${g},${b},${alpha})`
      this.ctx.fillText(to, x, y)
      return
    }
    if (blend <= .16) {
      this.ctx.fillStyle = `rgba(${r},${g},${b},${alpha})`
      this.ctx.fillText(from, x, y)
      return
    }
    const local = (blend - .16) / .68
    this.ctx.fillStyle = `rgba(${r},${g},${b},${alpha * (1 - local)})`
    this.ctx.fillText(from, x, y)
    this.ctx.fillStyle = `rgba(${r},${g},${b},${alpha * local})`
    this.ctx.fillText(to, x, y)
  }

  private drawMorphWave(reveal: number, result: number): void {
    const visibility = Math.sin(Math.PI * reveal) * (1 - result * .65)
    if (visibility <= .001) return
    const waveY = mix(INTERNAL_H * .13, INTERNAL_H * .88, reveal)
    const gradient = this.ctx.createLinearGradient(0, waveY - INTERNAL_H * .046, 0, waveY + INTERNAL_H * .046)
    gradient.addColorStop(0, 'rgba(0,0,0,0)')
    const [r, g, b] = this.event.accent
    gradient.addColorStop(.5, `rgba(${r},${g},${b},${.047 * visibility})`)
    gradient.addColorStop(1, 'rgba(0,0,0,0)')
    this.ctx.fillStyle = gradient
    this.ctx.fillRect(0, waveY - INTERNAL_H * .046, INTERNAL_W, INTERNAL_H * .092)
  }

  private drawAmbientMotion(now: number, reveal: number): void {
    const ctx = this.ctx, spec = this.themeSpec, profile = this.motionSpec
    const seconds = now / 1000 * profile.speed
    const focusX = INTERNAL_W / 2, focusY = spec.atmosphereY
    const [r, g, b] = spec.tint
    const wakeT = clamp01((now - this.wakeStart) / 2200)
    const wakeRadius = mix(18, 980, ease(wakeT))
    const wakeStrength = 1 - smooth(wakeT)
    const fade = 1 - smooth(reveal / .54)
    if (fade <= .001 && wakeStrength <= .001) return

    ctx.save()
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.globalCompositeOperation = 'screen'
    for (const p of this.ambientGlyphs) {
      const strength = spec.motionStrength * profile.amplitude * p.depth
      const vx = p.x - focusX, vy = p.y - focusY
      const radius = Math.max(1, Math.hypot(vx, vy))
      let x = p.x + this.pointerParallax.x * (3 + 12 * p.depth)
      let y = p.y + this.pointerParallax.y * (3 + 10 * p.depth)
      switch (spec.motion) {
        case 'orbital': {
          const angle = Math.sin(seconds * .30 + p.phase) * .012 * strength + seconds * .0018 * (p.phase > Math.PI ? -1 : 1)
          const ca = Math.cos(angle), sa = Math.sin(angle)
          x = focusX + vx * ca - vy * sa; y = focusY + vx * sa + vy * ca
          x += Math.sin(seconds * .72 + p.phase) * 2.4 * strength
          break
        }
        case 'circuit':
          y += ((((seconds * 22 * (.35 + p.depth) + p.phase * 40) % 92) - 46) * .18 * strength)
          if (Math.sin(seconds * 1.4 + p.phase) > .74) x += 7 * strength
          break
        case 'radial': {
          const scale = 1 + Math.sin(seconds * .82 + p.phase) * .0068 * strength
          x = focusX + vx * scale; y = focusY + vy * scale
          const tangent = Math.sin(seconds * .5 + p.phase) * 4.2 * strength
          x += -vy / radius * tangent; y += vx / radius * tangent
          break
        }
        case 'bloom': {
          const angle = Math.atan2(vy, vx)
          const petal = Math.sin(angle * 6 + seconds * .66 + p.phase) * 6.5 * strength
          const scale = 1 + petal / Math.max(180, radius) * .42
          x = focusX + vx * scale; y = focusY + vy * scale
          break
        }
        case 'wave':
          x += Math.sin(seconds * .52 + p.y * .010 + p.phase) * 5.2 * strength
          y += Math.sin(seconds * .36 + p.x * .008 - p.phase) * 3.7 * strength
          break
        case 'fold': {
          const depth = (p.y - focusY) / Math.max(1, INTERNAL_H)
          const fold = Math.sin(seconds * .32 + p.phase) * Math.abs(depth) * 8.5 * strength
          x += fold * (Math.sin(p.phase) >= 0 ? 1 : -1)
          y += Math.cos(seconds * .27 + p.phase) * 1.7 * strength
          break
        }
        case 'flow':
        default:
          x += Math.sin(seconds * .55 + p.y * .006 + p.phase) * 5.5 * strength
          y += Math.cos(seconds * .38 + p.x * .005 + p.phase) * 3.2 * strength
      }
      const wakeBand = Math.exp(-Math.pow((radius - wakeRadius) / 86, 2)) * wakeStrength
      const twinkle = .58 + .42 * Math.sin(seconds * (1.1 + p.depth) + p.phase)
      const alpha = fade * (.045 + p.alpha * .24) * (.72 + twinkle * .28) + wakeBand * .46
      if (alpha <= .015) continue
      ctx.font = `${p.size * (1 + wakeBand * .12)}px ${FONT_STACK}`
      ctx.fillStyle = `rgba(${r},${g},${b},${alpha})`
      ctx.fillText(p.char, x, y)
    }
    ctx.restore()
  }

  private fitTextSize(text: string, maxWidth: number, preferred: number, minimum: number, weight: number): number {
    let low = minimum, high = preferred
    for (let i = 0; i < 10; i++) {
      const mid = (low + high) / 2
      this.ctx.font = `${weight} ${mid}px ${FONT_STACK}`
      if (this.ctx.measureText(text).width <= maxWidth) low = mid
      else high = mid
    }
    return low
  }

  private wrapText(text: string, maxWidth: number, size: number, maxLines: number): string[] {
    this.ctx.font = `400 ${size}px ${FONT_STACK}`
    const words = text.trim().split(/\s+/)
    const lines: string[] = []
    let current = ''
    for (const word of words) {
      const candidate = current ? `${current} ${word}` : word
      if (!current || this.ctx.measureText(candidate).width <= maxWidth) { current = candidate; continue }
      lines.push(current)
      current = word
      if (lines.length === maxLines - 1) break
    }
    if (current && lines.length < maxLines) lines.push(current)
    return lines.length ? lines : [text]
  }

  private render = (now: number): void => {
    this.draw(now)
    this.raf = requestAnimationFrame(this.render)
  }

  private draw(now: number): void {
    const ctx = this.ctx
    ctx.fillStyle = '#000'
    ctx.fillRect(0, 0, INTERNAL_W, INTERNAL_H)

    let reveal = 0
    let result = 0
    if (this.captureT !== null) reveal = this.captureT
    else if (this.state === 'ambient') reveal = 0
    else if (this.state === 'revealing') {
      reveal = clamp01((now - this.transitionStart) / this.transitionDuration)
      if (reveal >= 1) this.state = 'focused'
    } else if (this.state === 'collapsing') {
      reveal = 1 - clamp01((now - this.transitionStart) / this.transitionDuration)
      if (reveal <= 0) this.state = 'ambient'
    } else reveal = 1

    if (this.state === 'resultTransition') {
      result = clamp01((now - this.resultStart) / 1350)
      if (result >= 1) this.state = 'result'
    } else if (this.state === 'result') result = 1

    const revealT = smooth(reveal)
    const resultT = smooth(result)
    const motionNow = this.captureMotionMs ?? now
    const baseAlpha = 1 - smooth(revealT / .58)
    if (baseAlpha > .001) {
      const breathing = Math.sin(motionNow / 3600 * this.motionSpec.speed) * .0016 * this.motionSpec.amplitude
      ctx.save()
      ctx.translate(INTERNAL_W / 2, INTERNAL_H / 2)
      ctx.scale(1 + breathing, 1 + breathing)
      ctx.translate(-INTERNAL_W / 2, -INTERNAL_H / 2)
      ctx.globalAlpha = baseAlpha
      ctx.drawImage(this.baseCanvas, 0, 0)
      ctx.restore()
      ctx.globalAlpha = 1
    }

    this.drawAmbientMotion(motionNow, revealT)
    this.drawMorphField(revealT, resultT, motionNow, this.state === 'listening')

    ctx.globalCompositeOperation = 'screen'
    const scanY = (motionNow * .030 * this.motionSpec.speed) % INTERNAL_H
    const scan = ctx.createLinearGradient(0, scanY - 72, 0, scanY + 72)
    scan.addColorStop(0, 'rgba(255,255,255,0)')
    scan.addColorStop(.5, 'rgba(200,224,236,.014)')
    scan.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.fillStyle = scan
    ctx.fillRect(0, scanY - 72, INTERNAL_W, 144)
    ctx.globalCompositeOperation = 'source-over'
  }

  private attachInput(): void {
    const stage = this.canvas.closest('.stage') as HTMLElement
    stage.addEventListener('pointermove', event => {
      const rect = stage.getBoundingClientRect()
      this.pointerParallax.x = ((event.clientX - rect.left) / rect.width - .5) * 2
      this.pointerParallax.y = ((event.clientY - rect.top) / rect.height - .5) * 2
    })
    stage.addEventListener('pointerleave', () => { this.pointerParallax.x *= .35; this.pointerParallax.y *= .35 })
    stage.addEventListener('pointerdown', event => {
      const rect = stage.getBoundingClientRect()
      const x = (event.clientX - rect.left) / rect.width
      const y = (event.clientY - rect.top) / rect.height
      this.pointerStart = { x, y, at: performance.now() }
      if (y > .68) {
        window.clearTimeout(this.holdTimer)
        this.holdTimer = window.setTimeout(() => this.listen(), 560)
      }
      stage.setPointerCapture(event.pointerId)
    })
    stage.addEventListener('pointerup', event => {
      window.clearTimeout(this.holdTimer)
      if (!this.pointerStart) return
      const rect = stage.getBoundingClientRect()
      const x = (event.clientX - rect.left) / rect.width
      const y = (event.clientY - rect.top) / rect.height
      const dx = x - this.pointerStart.x
      const dy = y - this.pointerStart.y
      const held = performance.now() - this.pointerStart.at
      if (held < 520) {
        if (Math.abs(dx) > .15 && Math.abs(dx) > Math.abs(dy)) this.nextEvent(dx < 0 ? 1 : -1)
        else if (dy < -.12) this.collapse()
        else if (this.state === 'ambient') this.reveal()
        else if (this.state === 'result') this.collapse()
      }
      this.pointerStart = null
    })
    stage.addEventListener('pointercancel', () => { window.clearTimeout(this.holdTimer); this.pointerStart = null })
    window.addEventListener('keydown', event => {
      if (event.repeat) return
      if (event.key === 'ArrowLeft') this.nextEvent(-1)
      if (event.key === 'ArrowRight') this.nextEvent(1)
      if (event.key === 'ArrowUp') this.collapse()
      if (event.key === 'ArrowDown') this.state === 'ambient' ? this.reveal() : this.listen()
    })
  }
}

const canvas = document.querySelector<HTMLCanvasElement>('#glyph-canvas')!
canvas.width = INTERNAL_W
canvas.height = INTERNAL_H
const renderer = new GlyphLockRenderer(canvas)

function syncControls(theme: ThemeId, event: EventId): void {
  document.querySelectorAll<HTMLButtonElement>('[data-theme]').forEach(button => button.classList.toggle('active', button.dataset.theme === theme))
  document.querySelectorAll<HTMLButtonElement>('[data-event]').forEach(button => button.classList.toggle('active', button.dataset.event === event))
  document.querySelectorAll<HTMLButtonElement>('[data-motion]').forEach(button => button.classList.toggle('active', button.dataset.motion === renderer.currentMotionProfile))
}

function buildControls(): void {
  const themeRoot = document.querySelector('#theme-controls')!
  for (const theme of THEMES) {
    const button = document.createElement('button')
    button.textContent = theme.label
    button.dataset.theme = theme.id
    button.addEventListener('click', () => { renderer.setTheme(theme.id); syncControls(renderer.currentTheme, renderer.currentEventId) })
    themeRoot.append(button)
  }
  const motionRoot = document.querySelector('#motion-controls')!
  for (const [id, profile] of Object.entries(MOTION_PROFILES) as [MotionProfile, typeof MOTION_PROFILES[MotionProfile]][]) {
    const button = document.createElement('button')
    button.textContent = profile.label
    button.dataset.motion = id
    button.addEventListener('click', () => { renderer.setMotionProfile(id); syncControls(renderer.currentTheme, renderer.currentEventId) })
    motionRoot.append(button)
  }
  const eventRoot = document.querySelector('#event-controls')!
  for (const event of EVENTS) {
    const button = document.createElement('button')
    button.dataset.event = event.id
    const title = document.createElement('strong'); title.textContent = event.title
    const sub = document.createElement('span'); sub.textContent = event.eyebrow
    button.append(title, sub)
    button.addEventListener('click', () => { renderer.setEvent(event.id); syncControls(renderer.currentTheme, renderer.currentEventId) })
    eventRoot.append(button)
  }
  document.querySelector<HTMLButtonElement>('#play-reveal')!.addEventListener('click', () => renderer.reveal())
  document.querySelector<HTMLButtonElement>('#collapse')!.addEventListener('click', () => renderer.collapse())
  syncControls('sentinel', 'mail')
}

function updateClock(): void {
  const now = new Date()
  document.querySelector('#clock')!.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  document.querySelector('#date')!.textContent = now.toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric' }).toUpperCase().replace(',', ' ·')
}

buildControls()
updateClock()
setInterval(updateClock, 30_000)
await renderer.load()

const query = new URLSearchParams(location.search)
if (query.has('capture')) {
  document.body.classList.add('capture')
  const theme = query.get('theme') as ThemeId | null
  const event = query.get('event') as EventId | null
  if (theme && THEMES.some(t => t.id === theme)) renderer.setTheme(theme)
  if (event && EVENTS.some(e => e.id === event)) renderer.setEvent(event)
  const t = Number(query.get('t') ?? '1')
  renderer.setCaptureTime(Number.isFinite(t) ? t : 1)
  document.documentElement.dataset.ready = 'true'
}

Object.assign(window, {
  __glyphlock: {
    renderer,
    reveal: () => renderer.reveal(),
    collapse: () => renderer.collapse(),
    setCaptureTime: (t: number) => renderer.setCaptureTime(t),
    setCaptureMotionTime: (ms: number) => renderer.setCaptureMotionTime(ms),
    wake: () => renderer.wake(),
  },
})
