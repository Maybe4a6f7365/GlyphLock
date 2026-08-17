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
  | 'chrono_loom'
  | 'muon_chamber'
  | 'vector_shrine'
  | 'lagrange_garden'
type EventId = 'mail' | 'calendar' | 'github' | 'security' | 'network' | 'model'
type MotionStyle = 'flow' | 'orbital' | 'circuit' | 'radial' | 'bloom' | 'wave' | 'fold'
type CompositionStyle = 'figure' | 'core' | 'orbital_band' | 'architecture' | 'splice' | 'field' | 'dial' | 'cascade' | 'constellation'
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
  edgeMobility: number
  energyCoordinate: number
  palettePhase: number
  glitchEligible: boolean
  glitchDirection: number
  glitchJitter: number
  echoPrimary: boolean
  echoSecondary: boolean
  glowEligible: boolean
}

interface RibbonGlyph extends GlyphPoint {
  phase: number
  depth: number
  lane: number
  atmosphereEligible: boolean
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

interface FittedLines {
  lines: string[]
  size: number
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
  { id: 'chrono_loom', label: 'Chrono Loom', semanticY: .54, semanticWidth: .70, palette: ' .·:;⌁⌇⟲⟳01[]<>+@', tint: [255, 208, 132], atmosphereY: 888, densityBias: .010, motion: 'orbital', composition: 'dial', motionStrength: 1.15 },
  { id: 'muon_chamber', label: 'Muon Chamber', semanticY: .55, semanticWidth: .68, palette: ' .·:;μνλψ01()[]<>#@', tint: [206, 169, 255], atmosphereY: 936, densityBias: .012, motion: 'radial', composition: 'dial', motionStrength: 1.18 },
  { id: 'vector_shrine', label: 'Vector Shrine', semanticY: .53, semanticWidth: .62, palette: ' .:;|+-=[]{}<>XYZ01#', tint: [139, 230, 255], atmosphereY: 960, densityBias: .009, motion: 'circuit', composition: 'cascade', motionStrength: 1.02 },
  { id: 'lagrange_garden', label: 'Lagrange Garden', semanticY: .55, semanticWidth: .70, palette: ' .·:;~✦✧L₁L₂01()<>+', tint: [170, 242, 196], atmosphereY: 912, densityBias: .013, motion: 'bloom', composition: 'constellation', motionStrength: 1.10 },
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
const MASK_BLACK_CUTOFF = 10 / 255
const MAX_RIBBON_GLYPHS = 420
const MAX_MORPH_SOURCE_COUNT = Math.max(...Object.values(MOTION_PROFILES).map(profile => profile.morphCount))
const RAMP = '  .·,:;+i1tfLCG08@'
const BASE_GLYPHS = ' .·,:;+=x1I|/\\()[]{}<>#08@'
const STRUCTURE_GLYPHS = '·.:;|/\\+−=[]{}<>01'
const FONT_STACK = '"Roboto Mono", "SFMono-Regular", monospace'

const clamp01 = (v: number) => Math.max(0, Math.min(1, v))
const mix = (a: number, b: number, t: number) => a + (b - a) * t
const smooth = (t: number) => { const x = clamp01(t); return x * x * (3 - 2 * x) }
const ease = (t: number) => 1 - Math.pow(1 - clamp01(t), 3)
const distanceToRect = (
  x: number, y: number, left: number, top: number, right: number, bottom: number,
): number => {
  const dx = x < left ? left - x : x > right ? x - right : 0
  const dy = y < top ? top - y : y > bottom ? y - bottom : 0
  return Math.hypot(dx, dy)
}
const localInfluence = (distance: number, radius: number, floor: number): number => {
  const safeRadius = Math.max(.001, radius)
  const gaussian = Math.exp(-(distance * distance) / (2 * safeRadius * safeRadius))
  return clamp01(floor + (1 - floor) * gaussian)
}
const hash = (x: number, y: number, seed = 0) => {
  const n = Math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453123
  return n - Math.floor(n)
}
const mixColor = (a: [number, number, number], b: [number, number, number], t: number): [number, number, number] => [
  Math.round(mix(a[0], b[0], t)),
  Math.round(mix(a[1], b[1], t)),
  Math.round(mix(a[2], b[2], t)),
]

type Rgb = [number, number, number]

interface DerivedPalette {
  primary: Rgb
  secondary: Rgb
  tertiary: Rgb
  core: Rgb
}

// Every scene keeps one authored tint. The companion neon colors are derived so
// the twenty-theme catalog stays compact and future themes inherit the same depth.
const derivePalette = (primary: Rgb, accent: Rgb = primary): DerivedPalette => {
  const bridge = mixColor(primary, accent, .18)
  const secondary: Rgb = [
    Math.round(mix(bridge[0], 255, .18)),
    Math.round(mix(bridge[1], 72, .58)),
    Math.round(mix(bridge[2], 255, .66)),
  ]
  const tertiary: Rgb = [
    Math.round(mix(bridge[0], 70, .55)),
    Math.round(mix(bridge[1], 220, .55)),
    Math.round(mix(bridge[2], 255, .55)),
  ]
  return {
    primary,
    secondary,
    tertiary,
    core: mixColor(tertiary, [255, 255, 255], .58),
  }
}

const writeMixedColor = (out: Rgb, a: Rgb, b: Rgb, t: number): void => {
  out[0] = Math.round(mix(a[0], b[0], t))
  out[1] = Math.round(mix(a[1], b[1], t))
  out[2] = Math.round(mix(a[2], b[2], t))
}

const writePaletteColor = (out: Rgb, palette: DerivedPalette, phase: number): void => {
  const t = ((phase % 1) + 1) % 1
  if (t < .5) writeMixedColor(out, palette.primary, palette.secondary, smooth(t * 2))
  else writeMixedColor(out, palette.secondary, palette.tertiary, smooth((t - .5) * 2))
}

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
  // Compile-only font-stroke sampler. Its pixels become target coordinates;
  // this surface is never composited into the wallpaper renderer.
  private readonly semanticStrokeSamplerCtx = (() => {
    const sampler = document.createElement('canvas')
    sampler.width = INTERNAL_W
    sampler.height = INTERNAL_H
    const context = sampler.getContext('2d', { willReadFrequently: true })
    if (!context) throw new Error('Semantic stroke sampler is unavailable')
    return context
  })()
  private basePoints: GlyphPoint[] = []
  private ambientGlyphs: AmbientGlyph[] = []
  private ribbonGlyphs: RibbonGlyph[] = []
  private morphGlyphs: MorphGlyph[] = []
  private themePalette = derivePalette(THEMES[0]!.tint)
  private semanticPalette = derivePalette(THEMES[0]!.tint, EVENTS[0]!.accent)
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

  private refreshPalettes(): void {
    this.themePalette = derivePalette(this.themeSpec.tint)
    this.semanticPalette = derivePalette(this.themeSpec.tint, this.event.accent)
  }

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
    this.refreshPalettes()
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
    this.refreshPalettes()
    this.rebuildTopology()
    this.reveal()
  }

  nextEvent(direction = 1): void {
    this.eventIndex = (this.eventIndex + direction + EVENTS.length) % EVENTS.length
    this.refreshPalettes()
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
    this.appendStructuralSourceRails()
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
      const phase = hash(i, point.x, point.y) * Math.PI * 2
      glyphs.push({
        ...point,
        phase,
        depth: .25 + .75 * hash(point.y, i, 11),
        edgeMobility: .34 + .66 * (1 - point.alpha),
        energyCoordinate: point.y + point.x * .24,
        palettePhase: phase / (Math.PI * 2) * .31 + point.x / INTERNAL_W * .18 + point.y / INTERNAL_H * .11,
        glitchEligible: hash(point.x, point.y, 91) > .973,
        glitchDirection: hash(point.y, point.x, 103) > .5 ? 1 : -1,
        glitchJitter: hash(point.x, point.y, 107) - .5,
        echoPrimary: hash(i, point.x, 191) > .835,
        echoSecondary: hash(i, point.y, 193) > .945,
        glowEligible: hash(point.x, i, 197) > .89,
      })
    }
    this.ambientGlyphs = glyphs
  }

  private rebuildTopology(): void {
    const strokeSamplerCtx = this.semanticStrokeSamplerCtx
    const eventLayout = this.buildTargetLayout(false, strokeSamplerCtx)
    const resultLayout = this.buildTargetLayout(true, strokeSamplerCtx)
    const sources = this.selectMorphSources()
    this.rebuildRibbonGlyphs()
    if (sources.length === 0) return
    const eventAssignments = this.assignCoherentTargets(sources, eventLayout.targets)
    const resultAssignments = this.assignCoherentTargets(sources, resultLayout.targets)
    const focusX = INTERNAL_W / 2
    const focusY = this.themeSpec.atmosphereY
    const diagonal = Math.hypot(INTERNAL_W, INTERNAL_H)

    this.morphGlyphs = sources.map((source, index) => {
      const assignedEvent = eventAssignments[index]
      const assignedResult = resultAssignments[index]
      const eventTarget = this.withSourceChar(
        assignedEvent ?? this.buildFillerTarget(source, eventLayout, index, false),
        source,
      )
      const resultTarget = this.withSourceChar(
        assignedResult ?? this.buildFillerTarget(source, resultLayout, index, true),
        source,
      )
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

  private withSourceChar(target: TargetGlyph, source: GlyphPoint): TargetGlyph {
    return { ...target, char: source.char }
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
    const count = Math.min(this.motionSpec.morphCount, MAX_MORPH_SOURCE_COUNT)
    const selected: GlyphPoint[] = []
    // A loaded scene has already been padded to the hard topology limit. An
    // empty/partial pre-load scene should wait for rebuildScene instead of
    // changing its source cardinality.
    if (candidates.length < count) return selected
    const used = new Uint8Array(candidates.length)
    let cursor = (this.theme.length * 149) % candidates.length
    for (let i = 0; i < count; i++) {
      cursor = (cursor + 97) % candidates.length
      while (used[cursor]) cursor = (cursor + 1) % candidates.length
      used[cursor] = 1
      selected.push(candidates[cursor]!)
    }
    return selected
  }

  private appendStructuralSourceRails(): void {
    const available = this.basePoints.reduce((count, point) => count + (point.alpha > .15 ? 1 : 0), 0)
    const count = Math.max(0, MAX_MORPH_SOURCE_COUNT - available)
    if (count === 0) return
    const clockSafeBottom = INTERNAL_H * .15
    const gestureSafeTop = INTERNAL_H * .91
    const top = clockSafeBottom
    const bottom = gestureSafeTop
    const left = INTERNAL_W * .035
    const right = INTERNAL_W * .965
    const width = right - left, height = bottom - top
    const vocabulary = this.themeSpec.palette + STRUCTURE_GLYPHS
    const themeIndex = Math.max(0, THEMES.findIndex(theme => theme.id === this.theme))
    const seed = (themeIndex + 1) * .071
    const railCount = Math.max(8, Math.min(20, Math.ceil(count / 150)))
    const pointsPerRail = Math.ceil(count / railCount)
    for (let index = 0; index < count; index++) {
      const rail = index % railCount
      const step = Math.floor(index / railCount)
      const t = pointsPerRail <= 1 ? .5 : step / (pointsPerRail - 1)
      const n = hash(index, themeIndex, 163)
      const railU = (rail + .5) / railCount
      const baseX = left + railU * width
      const curl = Math.sin(t * Math.PI * (2.2 + rail % 4 * .38) + seed * 19 + rail * .63) * INTERNAL_W * (.018 + (rail % 3) * .006)
      const crossCurl = Math.cos(t * Math.PI * 5.4 - seed * 11 + rail) * INTERNAL_W * .008
      const x = Math.max(left, Math.min(right, baseX + curl + crossCurl))
      const y = top + t * height
      const char = vocabulary[Math.min(vocabulary.length - 1, Math.floor(n * vocabulary.length))] ?? '·'
      this.basePoints.push({ x, y, char, alpha: .155 + n * .095, size: 8.2 + n * 2.4 })
    }
  }

  private rebuildRibbonGlyphs(): void {
    const candidates: Array<{ point: GlyphPoint; lane: number; score: number; order: number }> = []
    for (let pointIndex = 0; pointIndex < this.basePoints.length; pointIndex++) {
      const point = this.basePoints[pointIndex]!
      if (point.alpha < .16) continue
      const normalizedX = point.x / INTERNAL_W
      let nearestLane = 0
      let nearestDistance = Number.POSITIVE_INFINITY
      for (let lane = 0; lane < 3; lane++) {
        const laneOffset = (lane - 1) * INTERNAL_H * .15
        const ribbonY = this.themeSpec.atmosphereY + laneOffset
          + Math.sin(normalizedX * Math.PI * (2.10 + lane * .32) + lane * 1.7) * INTERNAL_H * (.030 + lane * .006)
          + Math.sin(normalizedX * Math.PI * 5.2 + lane) * INTERNAL_H * .010
        const distance = Math.abs(point.y - ribbonY)
        if (distance < nearestDistance) { nearestDistance = distance; nearestLane = lane }
      }
      if (nearestDistance > 132) continue
      const order = Math.floor(normalizedX * 24) * 3 + nearestLane
      const score = nearestDistance - point.alpha * 58 + hash(point.x, point.y, 171) * 18
      candidates.push({ point, lane: nearestLane, score, order })
    }
    candidates.sort((a, b) => a.order - b.order || a.score - b.score)
    const selected: RibbonGlyph[] = []
    const bucketUse = new Uint16Array(72)
    for (const candidate of candidates) {
      if (selected.length >= MAX_RIBBON_GLYPHS) break
      if (bucketUse[candidate.order]! >= 6) continue
      bucketUse[candidate.order]! += 1
      const point = candidate.point
      selected.push({
        ...point,
        lane: candidate.lane,
        phase: hash(point.x, point.y, 173) * Math.PI * 2,
        depth: .30 + hash(point.y, point.x, 179) * .70,
        atmosphereEligible: hash(point.x, point.y, candidate.lane + 57) >= .28,
      })
    }
    this.ribbonGlyphs = selected
  }

  private buildTargetLayout(result: boolean, strokeSamplerCtx: CanvasRenderingContext2D): TargetLayout {
    const event = this.event
    const targets: TargetGlyph[] = []
    const textBands: TextBand[] = []
    const spec = this.themeSpec
    const centerX = INTERNAL_W / 2
    const contentWidth = INTERNAL_W * spec.semanticWidth
    const left = centerX - contentWidth / 2
    const right = centerX + contentWidth / 2
    const anchorY = INTERNAL_H * spec.semanticY
    const topOffset = spec.composition === 'architecture' || spec.composition === 'cascade' ? INTERNAL_H * .125
      : spec.composition === 'splice' ? INTERNAL_H * .118
      : spec.composition === 'field' || spec.composition === 'constellation' ? INTERNAL_H * .112
      : spec.composition === 'dial' ? INTERNAL_H * .102
      : spec.composition === 'orbital_band' ? INTERNAL_H * .105
      : INTERNAL_H * .108
    const top = anchorY - topOffset
    const accent = event.accent

    const eyebrow = result ? 'LOCAL DEMO · ACTION SIMULATED' : event.eyebrow
    const title = result ? event.resultTitle : event.title
    const summary = result ? event.resultSummary : event.summary
    const action = result ? 'TAP TO RETURN' : event.action
    const eyebrowSize = 20
    const preferredTitle = spec.composition === 'cascade' ? 52 : 56
    const titleSize = this.fitTextSize(strokeSamplerCtx, title, contentWidth, preferredTitle, 35, 520)
    const fittedSummary = this.fitWrappedLines(strokeSamplerCtx, summary, contentWidth, 3, 27, 20, 400)
    const summarySize = fittedSummary.size
    const actionSize = 19

    const metaY = top + eyebrowSize
    const titleY = metaY + INTERNAL_H * .034 + titleSize
    let summaryY = titleY + INTERNAL_H * .026
    const summaryLines = fittedSummary.lines
    const summaryBaselines: number[] = []
    for (const _line of summaryLines) {
      summaryY += summarySize * 1.44
      summaryBaselines.push(summaryY)
    }
    const actionY = summaryY + INTERNAL_H * .034 + actionSize

    const primaryAlign: CanvasTextAlign = spec.composition === 'architecture' || spec.composition === 'cascade' ? 'left' : 'center'
    const metaAlign: CanvasTextAlign = spec.composition === 'field' || spec.composition === 'constellation'
      ? 'left'
      : spec.composition === 'cascade' ? 'right' : primaryAlign
    const actionAlign: CanvasTextAlign = spec.composition === 'field' || spec.composition === 'constellation'
      ? 'right'
      : spec.composition === 'cascade' ? 'left' : primaryAlign
    const primaryAnchor = primaryAlign === 'left' ? left : centerX
    const metaAnchor = metaAlign === 'left' ? left : metaAlign === 'right' ? right : centerX
    const actionAnchor = actionAlign === 'right' ? right : primaryAnchor
    const pushAxis = this.pushAxisFor(spec.composition)

    this.addAlignedLineTargets(strokeSamplerCtx, targets, textBands, title, primaryAnchor, titleY, titleSize, [241, 247, 250], 1, primaryAlign, 'title', pushAxis, 520)
    summaryLines.forEach((line, index) => {
      let lineAnchor = primaryAnchor
      let lineAlign = primaryAlign
      if (spec.composition === 'field') {
        lineAnchor = centerX + (index % 2 === 0 ? -27 : 27)
        lineAlign = 'center'
      } else if (spec.composition === 'cascade') {
        lineAnchor = left + INTERNAL_W * (.018 + index * .026)
        lineAlign = 'left'
      } else if (spec.composition === 'constellation') {
        const offsets = [-.052, .052, 0]
        lineAnchor = centerX + INTERNAL_W * (offsets[Math.min(index, offsets.length - 1)] ?? 0)
        lineAlign = 'center'
      }
      this.addAlignedLineTargets(strokeSamplerCtx, targets, textBands, line, lineAnchor, summaryBaselines[index]!, summarySize, [214, 225, 231], .90, lineAlign, 'summary', pushAxis, 400)
    })

    if (spec.composition === 'dial') {
      const arcCenterY = anchorY + INTERNAL_H * .003
      this.addArcLineTargets(strokeSamplerCtx, targets, textBands, eyebrow, centerX, arcCenterY, contentWidth * .55, INTERNAL_H * .092, -2.62, -.52, eyebrowSize, accent, .96, 'meta', 600)
      this.addArcLineTargets(strokeSamplerCtx, targets, textBands, action, centerX, arcCenterY, contentWidth * .53, INTERNAL_H * .102, .52, 2.62, actionSize, accent, .98, 'action', 600)
    } else {
      this.addAlignedLineTargets(strokeSamplerCtx, targets, textBands, eyebrow, metaAnchor, metaY, eyebrowSize, accent, .96, metaAlign, 'meta', pushAxis, 600)
      this.addAlignedLineTargets(strokeSamplerCtx, targets, textBands, action, actionAnchor, actionY, actionSize, accent, .98, actionAlign, 'action', pushAxis, 600)
    }

    const bounds = {
      left: left - INTERNAL_W * .035,
      top: top - INTERNAL_H * .015,
      right: right + INTERNAL_W * .035,
      bottom: Math.max(actionY + INTERNAL_H * .022, anchorY + INTERNAL_H * .115),
    }
    this.addFullScreenEventStructure(targets, result)
    this.clearStructureFromTextBands(targets, textBands)
    return { targets: this.prioritizeTargetsWithinBudget(targets), textBands, bounds, centerX, centerY: anchorY }
  }

  private sampleTextStrokeTargets(
    sampler: CanvasRenderingContext2D,
    targets: TargetGlyph[], bounds: { left: number; top: number; right: number; bottom: number },
    size: number, color: Rgb, alpha: number, role: TargetRole,
  ): void {
    const left = Math.max(0, Math.floor(bounds.left))
    const top = Math.max(0, Math.floor(bounds.top))
    const right = Math.min(INTERNAL_W, Math.ceil(bounds.right))
    const bottom = Math.min(INTERNAL_H, Math.ceil(bounds.bottom))
    const width = right - left, height = bottom - top
    if (width <= 0 || height <= 0) return
    const pixels = sampler.getImageData(left, top, width, height).data
    const stepRatio = role === 'title' ? .13 : role === 'summary' ? .18 : .21
    const samplingScale = Math.max(1, Math.sqrt(MOTION_PROFILES.cinematic.morphCount / this.motionSpec.morphCount))
    const step = Math.max(4, Math.min(11, Math.round(size * stepRatio * samplingScale)))
    const destinationSize = Math.max(5.2, Math.min(7.8, step * 1.12))
    for (let cellY = 0; cellY < height; cellY += step) {
      for (let cellX = 0; cellX < width; cellX += step) {
        let bestAlpha = 0, bestX = cellX, bestY = cellY
        const endY = Math.min(height, cellY + step)
        const endX = Math.min(width, cellX + step)
        for (let py = cellY; py < endY; py++) {
          for (let px = cellX; px < endX; px++) {
            const sampleAlpha = pixels[(py * width + px) * 4 + 3]!
            if (sampleAlpha > bestAlpha) { bestAlpha = sampleAlpha; bestX = px; bestY = py }
          }
        }
        if (bestAlpha < 42) continue
        targets.push({
          x: left + bestX,
          y: top + bestY,
          char: '·',
          size: destinationSize,
          alpha: alpha * (.68 + bestAlpha / 255 * .32),
          color,
          text: true,
          role,
        })
      }
    }
  }

  private pushAxisFor(style: CompositionStyle): PushAxis {
    if (style === 'architecture' || style === 'splice' || style === 'cascade') return 'horizontal'
    if (style === 'field') return 'vertical'
    return 'radial'
  }

  private addAlignedLineTargets(
    sampler: CanvasRenderingContext2D,
    targets: TargetGlyph[], bands: TextBand[], text: string, anchorX: number, baseline: number,
    size: number, color: [number, number, number], alpha: number, align: CanvasTextAlign,
    role: TargetRole, pushAxis: PushAxis, weight: number,
  ): void {
    sampler.clearRect(0, 0, INTERNAL_W, INTERNAL_H)
    sampler.font = `${weight} ${size}px ${FONT_STACK}`
    sampler.textAlign = align
    sampler.textBaseline = 'alphabetic'
    sampler.fillStyle = '#fff'
    const width = sampler.measureText(text).width
    const startX = align === 'center' ? anchorX - width / 2 : align === 'right' ? anchorX - width : anchorX
    const padX = size * .52
    const bounds = {
      left: startX - padX,
      top: baseline - size * 1.34,
      right: startX + width + padX,
      bottom: baseline + size * .56,
    }
    sampler.fillText(text, anchorX, baseline)
    this.sampleTextStrokeTargets(sampler, targets, bounds, size, color, alpha, role)
    bands.push({ bounds, pushAxis })
  }

  private addArcLineTargets(
    sampler: CanvasRenderingContext2D,
    targets: TargetGlyph[], bands: TextBand[], text: string,
    centerX: number, centerY: number, radiusX: number, radiusY: number,
    startAngle: number, endAngle: number, size: number,
    color: [number, number, number], alpha: number, role: TargetRole, weight: number,
  ): void {
    sampler.clearRect(0, 0, INTERNAL_W, INTERNAL_H)
    sampler.font = `${weight} ${size}px ${FONT_STACK}`
    sampler.textAlign = 'center'
    sampler.textBaseline = 'middle'
    sampler.fillStyle = '#fff'
    const advances = [...text].map(char => sampler.measureText(char).width)
    const total = Math.max(1, advances.reduce((sum, value) => sum + value, 0))
    let cursor = 0
    ;[...text].forEach((char, index) => {
      const advance = advances[index] ?? 0
      const u = (cursor + advance * .5) / total
      const angle = mix(startAngle, endAngle, u)
      const x = centerX + Math.cos(angle) * radiusX
      const y = centerY + Math.sin(angle) * radiusY
      if (!/\s/.test(char)) {
        const tangent = Math.atan2(Math.cos(angle) * radiusY, -Math.sin(angle) * radiusX)
        sampler.save()
        sampler.translate(x, y)
        sampler.rotate(tangent)
        sampler.fillText(char, 0, 0)
        sampler.restore()
        const pad = size * .60
        bands.push({ bounds: { left: x - pad, top: y - pad, right: x + pad, bottom: y + pad }, pushAxis: 'radial' })
      }
      cursor += advance
    })
    this.sampleTextStrokeTargets(sampler, targets, {
      left: centerX - radiusX - size * 1.5,
      top: centerY - radiusY - size * 1.5,
      right: centerX + radiusX + size * 1.5,
      bottom: centerY + radiusY + size * 1.5,
    }, size, color, alpha, role)
  }

  // Full-screen successor to the former addSemanticStructure strategy.
  private addFullScreenEventStructure(targets: TargetGlyph[], result: boolean): void {
    const color = mixColor(this.themeSpec.tint, this.event.accent, result ? .58 : .46)
    const left = INTERNAL_W * .035
    const right = INTERNAL_W * .965
    const top = INTERNAL_H * .15
    const bottom = INTERNAL_H * .91
    const width = right - left
    const height = bottom - top
    const centerX = INTERNAL_W / 2
    const centerY = Math.max(top + height * .34, Math.min(bottom - height * .30, this.themeSpec.atmosphereY + INTERNAL_H * .13))
    const phase = result ? .72 : .10
    const themeIndex = Math.max(0, THEMES.findIndex(theme => theme.id === this.theme))
    const systemSeed = 3109 + themeIndex * 173 + (result ? 997 : 0)
    const add = (x: number, y: number, index: number, alpha: number, size = 11.5) => {
      const char = STRUCTURE_GLYPHS[((index * 5 + 3) % STRUCTURE_GLYPHS.length + STRUCTURE_GLYPHS.length) % STRUCTURE_GLYPHS.length] ?? '·'
      targets.push({
        x: Math.max(left, Math.min(right, x)),
        y: Math.max(top, Math.min(bottom, y)),
        char, size, alpha, color, text: false, role: 'structure',
      })
    }
    const line = (x1: number, y1: number, x2: number, y2: number, count: number, seed: number, alpha: number, size = 11.5) => {
      const safe = Math.max(2, count)
      for (let i = 0; i < safe; i++) {
        const t = i / (safe - 1)
        add(mix(x1, x2, t), mix(y1, y2, t), seed + i, alpha, size)
      }
    }

    // Registration fragments pin the grammar to the physical display without
    // enclosing the semantic stack or borrowing any text-derived coordinate.
    line(left, top, INTERNAL_W * .205, top + INTERNAL_H * .011, 21, systemSeed, .29)
    line(INTERNAL_W * .765, top + INTERNAL_H * .054, right, top + INTERNAL_H * .034, 24, systemSeed + 31, .24)
    line(left, bottom - INTERNAL_H * .075, INTERNAL_W * .165, bottom, 19, systemSeed + 67, .22)
    line(right, INTERNAL_H * .735, right - INTERNAL_W * .018, bottom, 25, systemSeed + 101, .27)

    switch (this.themeSpec.composition) {
      case 'figure':
        for (let i = 0; i < 68; i++) {
          const t = i / 67
          const y = mix(top, bottom, t)
          const spread = INTERNAL_W * (.235 + .115 * Math.sin(t * Math.PI))
          const pulse = Math.sin(t * Math.PI * 5) * INTERNAL_W * .012
          add(centerX - spread - pulse, y, i, .43)
          add(centerX + spread + pulse, y, i + 31, .43)
          if (i % 4 === 0) add(centerX + Math.sin(t * Math.PI * 6) * 16, y, i + 61, .30)
        }
        break
      case 'core':
        for (let ring = 0; ring < 4; ring++) {
          const rx = width * (.275 + ring * .061), ry = height * (.150 + ring * .060)
          const count = 54 + ring * 16
          for (let i = 0; i < count; i++) {
            if ((i + ring * 2) % 8 === 0) continue
            const a = Math.PI * 2 * i / count + phase + ring * .11
            add(centerX + Math.cos(a) * rx, centerY + Math.sin(a) * ry, i + ring * 73, .42 - ring * .055)
          }
        }
        break
      case 'orbital_band':
        for (let band = 0; band < 2; band++) {
          const count = band === 0 ? 128 : 96
          for (let i = 0; i < count; i++) {
            const a = Math.PI * 2 * i / count + band * .21 + phase
            if (Math.abs(Math.sin(a)) < .12 && Math.cos(a) > 0 && i % 3 !== 0) continue
            const rx = width * (.315 + band * .070 + .025 * Math.sin(a * 3))
            const ry = height * (.190 + band * .065)
            add(centerX + Math.cos(a) * rx, centerY + Math.sin(a) * ry, i + band * 131, band === 0 ? .43 : .27)
          }
        }
        break
      case 'architecture':
        for (let i = 0; i < 72; i++) {
          const y = mix(top, bottom, i / 71)
          if (i % 5 !== 1) {
            add(left + INTERNAL_W * .032, y, i, .43)
            add(right - INTERNAL_W * .032, y, i + 37, .36)
          }
          if (i % 6 === 0) add(left + INTERNAL_W * .074, y, i + 73, .28)
        }
        for (let rail = 0; rail < 2; rail++) {
          const y = rail === 0 ? top + INTERNAL_H * .028 : bottom - INTERNAL_H * .028
          for (let i = 0; i < 42; i++) {
            if (i > 14 && i < 27 && rail === 0) continue
            add(mix(left, right, i / 41), y, i + rail * 51, rail === 0 ? .36 : .24)
          }
        }
        break
      case 'splice':
        for (let i = 0; i < 76; i++) {
          const t = i / 75, y = mix(top, bottom, t)
          const wave = Math.sin(t * Math.PI * 10) * INTERNAL_W * .072
          add(left + INTERNAL_W * .105 + wave, y, i, .43)
          add(right - INTERNAL_W * .105 - wave, y, i + 43, .43)
          if (i % 3 === 0) add(centerX + Math.sin(t * Math.PI * 10) * 10, y, i + 89, .25)
        }
        break
      case 'dial':
        for (let ring = 0; ring < 3; ring++) {
          const count = 112 - ring * 14
          const rx = width * (.270 + ring * .060), ry = height * (.145 + ring * .055)
          for (let i = 0; i < count; i++) {
            const angle = Math.PI * 2 * i / count - Math.PI * .5 + phase
            if ((i + ring * 3) % 11 === 0) continue
            add(centerX + Math.cos(angle) * rx, centerY + Math.sin(angle) * ry, i + ring * 127, .43 - ring * .075)
          }
        }
        for (let tick = 0; tick < 36; tick++) {
          const angle = Math.PI * 2 * tick / 36 - Math.PI * .5 + phase
          line(
            centerX + Math.cos(angle) * width * .245,
            centerY + Math.sin(angle) * height * .120,
            centerX + Math.cos(angle) * width * .275,
            centerY + Math.sin(angle) * height * .145,
            tick % 3 === 0 ? 4 : 2,
            tick * 7,
            tick % 3 === 0 ? .46 : .27,
          )
        }
        break
      case 'cascade': {
        const leftRail = left + INTERNAL_W * .045
        const rightRail = right - INTERNAL_W * .045
        for (let i = 0; i < 78; i++) {
          const t = i / 77, y = mix(top, bottom, t)
          if (i % 4 !== 1) {
            add(leftRail, y, i, .44)
            add(rightRail, y, i + 41, .34)
          }
          if (i % 5 === 0) {
            const stair = (i % 10 < 5 ? -1 : 1) * INTERNAL_W * (.030 + (i % 3) * .012)
            line(centerX + stair, y - INTERNAL_H * .018, centerX - stair * .35, y + INTERNAL_H * .018, 5, i + 83, .32)
          }
        }
        for (let row = 0; row < 8; row++) {
          const y = top + INTERNAL_H * .020 + row * INTERNAL_H * .023
          const inset = row * INTERNAL_W * .018
          line(left + inset, y, right - inset, y, 28 - row * 2, row * 31, .34 - row * .025)
        }
        break
      }
      case 'constellation': {
        const nodes: Array<[number, number]> = [
          [centerX - width * .42, centerY - height * .22],
          [centerX - width * .28, centerY + height * .30],
          [centerX, centerY - height * .34],
          [centerX + width * .30, centerY + height * .28],
          [centerX + width * .42, centerY - height * .16],
          [centerX, centerY + height * .35],
        ]
        const edges: Array<[number, number]> = [[0, 2], [2, 4], [0, 1], [1, 5], [5, 3], [3, 4], [1, 2], [2, 3], [0, 5], [4, 5]]
        nodes.forEach(([x, y], nodeIndex) => {
          for (let ring = 0; ring < 3; ring++) {
            const count = 12 + ring * 4
            const rx = INTERNAL_W * (.018 + ring * .011), ry = INTERNAL_H * (.010 + ring * .005)
            for (let point = 0; point < count; point++) {
              const angle = Math.PI * 2 * point / count
              add(x + Math.cos(angle) * rx, y + Math.sin(angle) * ry, nodeIndex * 43 + ring * 17 + point, .46 - ring * .10)
            }
          }
        })
        edges.forEach(([fromIndex, toIndex], edgeIndex) => {
          const from = nodes[fromIndex]!, to = nodes[toIndex]!
          line(from[0], from[1], to[0], to[1], 22, edgeIndex * 37, .25)
        })
        break
      }
      case 'field':
        for (let row = -4; row <= 8; row++) {
          const y = centerY + row * INTERNAL_H * .056
          for (let i = 0; i < 66; i++) {
            if ((i + row + 13) % 5 === 0) continue
            const t = i / 65, x = mix(left, right, t)
            const wave = Math.sin(t * Math.PI * 5 + row * .77) * INTERNAL_H * .011
            add(x, y + wave, i + row * 29, .22 + .035 * (4 - Math.abs(row)))
          }
        }
        break
    }
  }

  private clearStructureFromTextBands(targets: TargetGlyph[], bands: TextBand[]): void {
    const horizontalClearance = INTERNAL_W * .017
    const verticalClearance = INTERNAL_H * .007
    for (let targetIndex = targets.length - 1; targetIndex >= 0; targetIndex--) {
      const target = targets[targetIndex]!
      if (target.text) continue
      const collides = bands.some(band => target.x >= band.bounds.left - horizontalClearance
        && target.x <= band.bounds.right + horizontalClearance
        && target.y >= band.bounds.top - verticalClearance
        && target.y <= band.bounds.bottom + verticalClearance)
      if (collides) targets.splice(targetIndex, 1)
    }
  }

  private assignCoherentTargets(sources: GlyphPoint[], targets: TargetGlyph[]): Array<TargetGlyph | undefined> {
    const columns = 18, rows = 36, maxRadius = 7
    const buckets: number[][] = Array.from({ length: columns * rows }, () => [])
    const clampColumn = (value: number) => Math.max(0, Math.min(columns - 1, value))
    const clampRow = (value: number) => Math.max(0, Math.min(rows - 1, value))
    const bucketIndex = (x: number, y: number) => clampRow(Math.floor(y / INTERNAL_H * rows)) * columns + clampColumn(Math.floor(x / INTERNAL_W * columns))
    sources.forEach((source, index) => buckets[bucketIndex(source.x, source.y)]!.push(index))

    const assignments: Array<TargetGlyph | undefined> = new Array(sources.length)
    const used = new Uint8Array(sources.length)
    let previousTargetX = Number.NaN
    let previousTargetY = Number.NaN
    let previousSourceX = Number.NaN
    let previousWasText = false

    const scoreFor = (source: GlyphPoint, target: TargetGlyph): number => {
      const dx = source.x - target.x
      const dy = (source.y - target.y) * (target.text ? 1.34 : 1.08)
      const travel = dx * dx + dy * dy
      const alphaReward = source.alpha * INTERNAL_W * INTERNAL_W * (target.text ? .020 : .009)
      const normalized = Math.sqrt(travel) / Math.hypot(INTERNAL_W, INTERNAL_H)
      const threshold = target.text ? .38 : .27
      const longPenalty = normalized > threshold ? INTERNAL_W * INTERNAL_W * (normalized - .27) * .70 : 0
      return travel - alphaReward + longPenalty
    }

    for (const target of targets) {
      const centerColumn = clampColumn(Math.floor(target.x / INTERNAL_W * columns))
      const centerRow = clampRow(Math.floor(target.y / INTERNAL_H * rows))
      let best = -1
      let bestScore = Number.POSITIVE_INFINITY
      for (let radius = 0; radius <= maxRadius && best < 0; radius++) {
        const minColumn = Math.max(0, centerColumn - radius), maxColumn = Math.min(columns - 1, centerColumn + radius)
        const minRow = Math.max(0, centerRow - radius), maxRow = Math.min(rows - 1, centerRow + radius)
        for (let row = minRow; row <= maxRow; row++) {
          for (let column = minColumn; column <= maxColumn; column++) {
            if (radius > 0 && column > minColumn && column < maxColumn && row > minRow && row < maxRow) continue
            for (const candidate of buckets[row * columns + column]!) {
              if (used[candidate]) continue
              const source = sources[candidate]!
              let score = scoreFor(source, target)
              if (target.text && previousWasText && Math.abs(target.y - previousTargetY) < INTERNAL_H * .018 && !Number.isNaN(previousSourceX)) {
                const targetMovesRight = target.x >= previousTargetX
                const sourceMovesRight = source.x >= previousSourceX
                if (targetMovesRight !== sourceMovesRight) score += INTERNAL_W * INTERNAL_W * .24
              }
              if (score < bestScore) { bestScore = score; best = candidate }
            }
          }
        }
      }
      if (best < 0) {
        sources.forEach((source, candidate) => {
          if (used[candidate]) return
          const score = scoreFor(source, target)
          if (score < bestScore) { bestScore = score; best = candidate }
        })
      }
      if (best < 0) break
      used[best] = 1
      assignments[best] = target
      previousTargetX = target.x
      previousTargetY = target.y
      previousSourceX = sources[best]!.x
      previousWasText = target.text
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

    const semanticDistance = distanceToRect(source.x, source.y, layout.bounds.left, layout.bounds.top, layout.bounds.right, layout.bounds.bottom)
    const influence = localInfluence(semanticDistance, INTERNAL_W * .30, this.identityFloorFor(spec.composition))
    x = mix(source.x, x, influence)
    y = mix(source.y, y, influence)

    ;[x, y] = this.warpFillerAroundBands(x, y, layout, n)
    x = Math.max(INTERNAL_W * .018, Math.min(INTERNAL_W * .982, x))
    y = Math.max(INTERNAL_H * .035, Math.min(INTERNAL_H * .975, y))
    return {
      x, y, char: source.char,
      size: source.size * (.93 + n * .15),
      alpha: clamp01(source.alpha * (result ? .70 : .78)),
      color: mixColor(spec.tint, this.event.accent, result ? .46 : .35),
      text: false,
      role: 'structure',
    }
  }

  private identityFloorFor(style: CompositionStyle): number {
    if (style === 'core') return .38
    if (style === 'orbital_band' || style === 'dial') return .42
    if (style === 'architecture' || style === 'cascade') return .34
    if (style === 'splice') return .36
    if (style === 'field' || style === 'constellation') return .30
    return .32
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
        const nearZero = value < MASK_BLACK_CUTOFF
        // Empty mask space stays empty. A deterministic 1.3% dust allowance
        // supplies rare stars without recreating the former uniform 8% haze.
        if (nearZero && n < .987) continue
        if (!nearZero && value < threshold && n < .955) continue
        const level = nearZero
          ? .055 + ((n - .987) / .013) * .045
          : clamp01(value * 1.12 + (n - .5) * .10)
        let char = RAMP[Math.min(RAMP.length - 1, Math.floor(level * RAMP.length))] ?? '.'
        if (level < .20 || n > .88) char = this.themeSpec.palette[Math.floor(n * this.themeSpec.palette.length)] ?? BASE_GLYPHS[Math.floor(n * BASE_GLYPHS.length)] ?? '.'
        points.push({ x: x + (n - .5) * 1.25, y: y + (hash(y, x, 3) - .5) * 1.15, char, alpha: clamp01(.08 + level * .94), size: 9 + level * 4.2 })
      }
    }
    return points
  }

  private renderBaseBitmap(): void {
    const ctx = this.baseCanvas.getContext('2d')!
    ctx.clearRect(0, 0, INTERNAL_W, INTERNAL_H)
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const palette = this.themePalette
    const focusX = INTERNAL_W / 2, focusY = this.themeSpec.atmosphereY
    for (const point of this.basePoints) {
      const value = Math.floor(72 + 182 * point.alpha)
      const neutral: Rgb = [value, value, value]
      const tonePhase = .5 + .5 * Math.sin(point.x * .0082 + point.y * .0034 + this.theme.length * .41)
      const tone = tonePhase < .5
        ? mixColor(palette.primary, palette.secondary, tonePhase * 2)
        : mixColor(palette.secondary, palette.tertiary, (tonePhase - .5) * 2)
      const focusDistance = Math.hypot(point.x - focusX, (point.y - focusY) * .72)
      const focusLift = Math.exp(-focusDistance / Math.max(1, INTERNAL_W * .62))
      const coreStrength = smooth((point.alpha - .48) / .48) * (.72 + focusLift * .28)
      const colored = mixColor(neutral, tone, .30 + point.alpha * .42)
      const finalColor = mixColor(colored, palette.core, coreStrength * .36)
      if (coreStrength > .54 && hash(point.x, point.y, 181) > .64) {
        ctx.font = `${point.size * (1.18 + coreStrength * .16)}px ${FONT_STACK}`
        ctx.fillStyle = `rgba(${palette.core[0]},${palette.core[1]},${palette.core[2]},${Math.min(.060, coreStrength * .055)})`
        ctx.fillText(point.char, point.x, point.y)
      }
      ctx.fillStyle = `rgba(${finalColor[0]},${finalColor[1]},${finalColor[2]},${Math.min(.96, point.alpha * .94)})`
      ctx.font = `${point.size}px ${FONT_STACK}`
      ctx.fillText(point.char, point.x, point.y)
      if (coreStrength > .66) {
        ctx.fillStyle = `rgba(${palette.core[0]},${palette.core[1]},${palette.core[2]},${coreStrength * .13})`
        ctx.fillText(point.char, point.x, point.y)
      }
    }
    const gradient = ctx.createRadialGradient(focusX, focusY, 16, focusX, focusY, 560)
    gradient.addColorStop(0, `rgba(${palette.secondary[0]},${palette.secondary[1]},${palette.secondary[2]},.018)`)
    gradient.addColorStop(.38, `rgba(${palette.tertiary[0]},${palette.tertiary[1]},${palette.tertiary[2]},.006)`)
    gradient.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, INTERNAL_W, INTERNAL_H)
  }

  private drawGlyphRibbonAtmosphere(now: number, reveal: number): void {
    const ctx = this.ctx, spec = this.themeSpec, profile = this.motionSpec
    const seconds = now / 1000 * profile.speed
    const semanticMix = smooth(reveal)
    const themeColor: Rgb = [0, 0, 0]
    const semanticColor: Rgb = [0, 0, 0]
    const color: Rgb = [0, 0, 0]
    // These ribbon echoes are ambient source material only; the semantic topology
    // takes full responsibility once the reveal settles.
    const focusCalm = 1 - smooth(reveal / .78)
    const wakeT = clamp01((now - this.wakeStart) / 2100)
    const wakeLift = (1 - smooth(wakeT)) * .28
    const visibility = Math.min(1, focusCalm + wakeLift)
    if (visibility <= .002) return

    ctx.save()
    ctx.globalCompositeOperation = 'screen'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    for (const point of this.ribbonGlyphs) {
      const lane = point.lane
      const normalizedX = point.x / INTERNAL_W
      const laneOffset = (lane - 1) * INTERNAL_H * .15
      const ribbonY = spec.atmosphereY + laneOffset
        + Math.sin(normalizedX * Math.PI * (2.10 + lane * .32) + seconds * (.12 + lane * .026) + lane * 1.7) * INTERNAL_H * (.030 + lane * .006)
        + Math.sin(normalizedX * Math.PI * 5.2 - seconds * .075 + lane) * INTERNAL_H * .010
      const distance = Math.abs(point.y - ribbonY)
      const ribbonWeight = Math.exp(-(distance * distance) / (2 * 54 * 54)) * point.alpha
      if (ribbonWeight < .075 || !point.atmosphereEligible) continue
      const flow = Math.sin(normalizedX * Math.PI * 3.4 + seconds * .24 + lane * 2.2)
      const x = point.x + flow * (3.5 + lane * 1.8) * profile.amplitude
      const y = point.y + Math.cos(normalizedX * Math.PI * 2.7 - seconds * .18 + lane) * (2.2 + lane) * profile.amplitude
      const colorPhase = normalizedX * .42 + seconds * .018 + lane * .23 + point.phase * .018
      writePaletteColor(themeColor, this.themePalette, colorPhase)
      writePaletteColor(semanticColor, this.semanticPalette, colorPhase)
      writeMixedColor(color, themeColor, semanticColor, semanticMix)
      const alpha = Math.min(.070, ribbonWeight * (.035 + lane * .006) * visibility)
      if (alpha <= .008) continue
      ctx.font = `${point.size * (.94 + ribbonWeight * .08)}px ${FONT_STACK}`
      ctx.fillStyle = `rgba(${color[0]},${color[1]},${color[2]},${alpha * .48})`
      ctx.fillText(point.char, x - flow * 5.5, y + flow * 2.1)
      ctx.fillStyle = `rgba(${color[0]},${color[1]},${color[2]},${alpha})`
      ctx.fillText(point.char, x, y)
    }
    ctx.restore()
  }

  private drawMorphField(reveal: number, result: number, now: number, listening: boolean): void {
    if (reveal <= 0) return
    const ctx = this.ctx
    const spec = this.themeSpec
    const focusX = INTERNAL_W / 2
    const focusY = spec.atmosphereY
    const handoff = smooth(reveal / .16)
    const seconds = now / 1000 * this.motionSpec.speed
    const palette = this.semanticPalette
    const travelingColor: Rgb = [0, 0, 0]
    const eventColor: Rgb = [0, 0, 0]
    const finalColor: Rgb = [0, 0, 0]
    const echoColor: Rgb = [0, 0, 0]
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'

    for (let glyphIndex = 0; glyphIndex < this.morphGlyphs.length; glyphIndex++) {
      const glyph = this.morphGlyphs[glyphIndex]!
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
      writeMixedColor(eventColor, spec.tint, glyph.eventTarget.color, eventBlend)
      writeMixedColor(finalColor, eventColor, glyph.resultTarget.color, resultBlend)
      if (!activeTarget.text) {
        const huePhase = seconds * .024 + glyph.phase / (Math.PI * 2) * .24 + x / INTERNAL_W * .18 + y / INTERNAL_H * .12
        writePaletteColor(travelingColor, palette, huePhase)
        const bandSpan = INTERNAL_H + INTERNAL_W * .20
        const bandPosition = (seconds * 58 + this.event.id.length * 47) % bandSpan - INTERNAL_W * .10
        const bandDistance = y + x * .12 - bandPosition
        const energyBand = Math.exp(-(bandDistance * bandDistance) / (2 * 92 * 92))
        writeMixedColor(finalColor, finalColor, travelingColor, .18 + energyBand * .17)
        alpha = Math.min(.78, alpha * (1 + energyBand * .10))
      }
      if (activeTarget.text && local > .88) alpha = Math.max(alpha, activeTarget.alpha * .94)
      if (alpha <= .012) continue

      if (result <= .001 && glyphIndex % 5 === 0 && local > .08 && local < .92) {
        const trailVisibility = Math.sin(Math.PI * local) * handoff * .11
        const echoProgress = clamp01((moveT - .10) / .90)
        const echoX = mix(glyph.source.x, eventX, echoProgress)
        const echoY = mix(glyph.source.y, eventY, echoProgress)
        writeMixedColor(echoColor, spec.tint, finalColor, .34)
        ctx.font = `${mix(glyph.source.size, size, .46)}px ${FONT_STACK}`
        ctx.fillStyle = `rgba(${echoColor[0]},${echoColor[1]},${echoColor[2]},${alpha * trailVisibility})`
        ctx.fillText(glyph.source.char, echoX, echoY)
      }

      ctx.font = `${size}px ${FONT_STACK}`
      ctx.fillStyle = `rgba(${finalColor[0]},${finalColor[1]},${finalColor[2]},${alpha})`
      ctx.fillText(glyph.source.char, x, y)
    }

    this.drawMorphWave(reveal, result)
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
    const semanticMix = smooth(reveal)
    const themeGlyphColor: Rgb = [0, 0, 0]
    const semanticGlyphColor: Rgb = [0, 0, 0]
    const glyphColor: Rgb = [0, 0, 0]
    const echoColor: Rgb = [0, 0, 0]
    const coreColor: Rgb = [0, 0, 0]
    const secondaryColor: Rgb = [0, 0, 0]
    const tertiaryColor: Rgb = [0, 0, 0]
    writeMixedColor(coreColor, this.themePalette.core, this.semanticPalette.core, semanticMix)
    writeMixedColor(secondaryColor, this.themePalette.secondary, this.semanticPalette.secondary, semanticMix)
    writeMixedColor(tertiaryColor, this.themePalette.tertiary, this.semanticPalette.tertiary, semanticMix)
    const wakeT = clamp01((now - this.wakeStart) / 2200)
    const wakeRadius = mix(18, 980, ease(wakeT))
    const wakeStrength = 1 - smooth(wakeT)
    const fade = 1 - smooth(reveal / .54)
    if (fade <= .001 && wakeStrength <= .001) return

    const energySpan = INTERNAL_H + INTERNAL_W * .44
    const energyPosition = (seconds * 92 + this.theme.length * 71) % energySpan - INTERNAL_W * .16

    ctx.save()
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.globalCompositeOperation = 'screen'
    for (const p of this.ambientGlyphs) {
      const strength = spec.motionStrength * profile.amplitude * p.depth
      const vx = p.x - focusX, vy = p.y - focusY
      const radius = Math.max(1, Math.hypot(vx, vy))
      const sourceX = p.x + this.pointerParallax.x * (3 + 12 * p.depth)
      const sourceY = p.y + this.pointerParallax.y * (3 + 10 * p.depth)
      let x = sourceX
      let y = sourceY
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

      // Two divergence-like fields move whole neighborhoods together while a
      // finer curl keeps the surface alive. Both operate on existing glyphs.
      const largeXPhase = p.x * .0036 + seconds * .13 + p.phase * .18
      const largeYPhase = p.y * .0027 - seconds * .10 - p.phase * .11
      const fineXPhase = p.x * .0107 - seconds * .31 + p.phase * .47
      const fineYPhase = p.y * .0081 + seconds * .24 - p.phase * .39
      const curlLargeX = Math.sin(largeXPhase) * Math.cos(largeYPhase)
      const curlLargeY = -Math.cos(largeXPhase) * Math.sin(largeYPhase)
      const curlFineX = Math.sin(fineXPhase) * Math.cos(fineYPhase)
      const curlFineY = -Math.cos(fineXPhase) * Math.sin(fineYPhase)
      const curlGain = strength * (.64 + p.depth * .36) * p.edgeMobility
      x += (curlLargeX * 5.8 + curlFineX * 1.9) * curlGain
      y += (curlLargeY * 4.5 + curlFineY * 1.5) * curlGain

      const energyDistance = p.energyCoordinate - energyPosition
      const rawEnergyBand = Math.exp(-(energyDistance * energyDistance) / (2 * 72 * 72))
      const energyBand = rawEnergyBand * Math.max(fade, wakeStrength * .34)
      x += Math.sin(p.phase + seconds * .8) * energyBand * 4.8 * strength
      y -= Math.cos(p.phase * .7 - seconds * .6) * energyBand * 3.1 * strength

      const glitchPulse = p.glitchEligible
        ? smooth((Math.sin(seconds * (1.75 + p.depth * .42) + p.phase * 2.8) - .82) / .18)
        : 0
      x += p.glitchDirection * glitchPulse * (6 + p.depth * 11) * profile.amplitude * p.edgeMobility
      y += glitchPulse * p.glitchJitter * 3.2 * p.edgeMobility

      const wakeBand = Math.exp(-Math.pow((radius - wakeRadius) / 86, 2)) * wakeStrength
      const twinkle = .58 + .42 * Math.sin(seconds * (1.1 + p.depth) + p.phase)
      const alpha = Math.min(
        .44,
        fade * (.034 + p.alpha * .22) * (.72 + twinkle * .28)
          + energyBand * (.050 + p.alpha * .075)
          + wakeBand * .34,
      )
      if (alpha <= .015) continue

      const huePhase = seconds * .026 + p.palettePhase
      writePaletteColor(themeGlyphColor, this.themePalette, huePhase)
      writePaletteColor(semanticGlyphColor, this.semanticPalette, huePhase)
      writeMixedColor(glyphColor, themeGlyphColor, semanticGlyphColor, semanticMix)
      writeMixedColor(glyphColor, glyphColor, coreColor, Math.min(.52, energyBand * .46 + wakeBand * .24))
      const motionX = x - sourceX, motionY = y - sourceY
      const motionDistance = Math.hypot(motionX, motionY)
      const glyphSize = p.size * (1 + wakeBand * .10 + energyBand * .075)
      ctx.font = `${glyphSize}px ${FONT_STACK}`

      if (p.echoPrimary && motionDistance > 1.2) {
        writeMixedColor(echoColor, tertiaryColor, glyphColor, .36)
        const echoAlpha = Math.min(.048, alpha * (.085 + p.depth * .065) * (1 + energyBand * .7))
        ctx.fillStyle = `rgba(${echoColor[0]},${echoColor[1]},${echoColor[2]},${echoAlpha})`
        ctx.fillText(p.char, x - motionX * .58, y - motionY * .58)
        if (energyBand > .28 && p.echoSecondary) {
          ctx.fillStyle = `rgba(${secondaryColor[0]},${secondaryColor[1]},${secondaryColor[2]},${echoAlpha * .38})`
          ctx.fillText(p.char, x - motionX * .91, y - motionY * .91)
        }
      }
      if (glitchPulse > .05) {
        const glitchAlpha = Math.min(.065, alpha * glitchPulse * .26)
        ctx.fillStyle = `rgba(${tertiaryColor[0]},${tertiaryColor[1]},${tertiaryColor[2]},${glitchAlpha})`
        ctx.fillText(p.char, x - p.glitchDirection * (4 + glitchPulse * 4), y)
      }

      if (energyBand > .34 && p.depth > .48 && p.glowEligible) {
        ctx.font = `${glyphSize * (1.24 + energyBand * .16)}px ${FONT_STACK}`
        ctx.fillStyle = `rgba(${coreColor[0]},${coreColor[1]},${coreColor[2]},${Math.min(.045, energyBand * .040)})`
        ctx.fillText(p.char, x, y)
        ctx.font = `${glyphSize}px ${FONT_STACK}`
      }
      ctx.fillStyle = `rgba(${glyphColor[0]},${glyphColor[1]},${glyphColor[2]},${alpha})`
      ctx.fillText(p.char, x, y)
    }
    ctx.restore()
  }

  private prioritizeTargetsWithinBudget(targets: TargetGlyph[]): TargetGlyph[] {
    const budget = Math.min(this.motionSpec.morphCount, MAX_MORPH_SOURCE_COUNT)
    if (targets.length <= budget) return targets
    const primaryTextTargets = targets.filter(target => target.text && (target.role === 'title' || target.role === 'summary'))
    const supportTextTargets = targets.filter(target => target.text && target.role !== 'title' && target.role !== 'summary')
    const structureTargets = targets.filter(target => !target.text)
    if (primaryTextTargets.length >= budget) return this.selectEvenly(primaryTextTargets, budget)
    const primaryAndSupport = primaryTextTargets.concat(supportTextTargets)
    if (primaryAndSupport.length >= budget) {
      return primaryTextTargets.concat(this.selectEvenly(supportTextTargets, budget - primaryTextTargets.length))
    }
    return primaryAndSupport.concat(this.selectEvenly(structureTargets, budget - primaryAndSupport.length))
  }

  private selectEvenly<T>(items: T[], count: number): T[] {
    if (count <= 0) return []
    if (items.length <= count) return items
    if (count === 1) return [items[Math.floor(items.length / 2)]!]
    const selected: T[] = []
    for (let index = 0; index < count; index++) {
      selected.push(items[Math.round(index * (items.length - 1) / (count - 1))]!)
    }
    return selected
  }

  private fitTextSize(
    measurer: CanvasRenderingContext2D,
    text: string, maxWidth: number, preferred: number, minimum: number, weight: number,
  ): number {
    let low = minimum, high = preferred
    for (let i = 0; i < 10; i++) {
      const mid = (low + high) / 2
      measurer.font = `${weight} ${mid}px ${FONT_STACK}`
      if (measurer.measureText(text).width <= maxWidth) low = mid
      else high = mid
    }
    return low
  }

  private fitWrappedLines(
    measurer: CanvasRenderingContext2D,
    text: string, maxWidth: number, preferredMaximumLines: number,
    preferredSize: number, minimumSize: number, weight: number,
  ): FittedLines {
    measurer.font = `${weight} ${preferredSize}px ${FONT_STACK}`
    const preferredLines = this.wrapTextFully(measurer, text, maxWidth)
    if (preferredLines.length <= preferredMaximumLines) return { lines: preferredLines, size: preferredSize }

    measurer.font = `${weight} ${minimumSize}px ${FONT_STACK}`
    const minimumLines = this.wrapTextFully(measurer, text, maxWidth)
    if (minimumLines.length > preferredMaximumLines) {
      // The preferred line count is soft: at minimum size, preserve all content.
      return { lines: minimumLines, size: minimumSize }
    }

    let low = minimumSize, high = preferredSize
    let bestLines = minimumLines
    for (let iteration = 0; iteration < 12; iteration++) {
      const candidateSize = (low + high) / 2
      measurer.font = `${weight} ${candidateSize}px ${FONT_STACK}`
      const candidateLines = this.wrapTextFully(measurer, text, maxWidth)
      if (candidateLines.length <= preferredMaximumLines) {
        low = candidateSize
        bestLines = candidateLines
      } else high = candidateSize
    }
    return { lines: bestLines, size: low }
  }

  private wrapTextFully(measurer: CanvasRenderingContext2D, text: string, maxWidth: number): string[] {
    const clean = text.trim()
    if (!clean) return ['']
    const lines: string[] = []
    let current = ''
    for (const rawWord of clean.split(/\s+/)) {
      let word = rawWord
      const candidate = current ? `${current} ${word}` : word
      if (measurer.measureText(candidate).width <= maxWidth) {
        current = candidate
        continue
      }
      if (current) {
        lines.push(current)
        current = ''
      }
      while (word && measurer.measureText(word).width > maxWidth) {
        const characters = [...word]
        let low = 1, high = characters.length
        while (low < high) {
          const middle = Math.ceil((low + high) / 2)
          if (measurer.measureText(characters.slice(0, middle).join('')).width <= maxWidth) low = middle
          else high = middle - 1
        }
        lines.push(characters.slice(0, low).join(''))
        word = characters.slice(low).join('')
      }
      current = word
    }
    if (current) lines.push(current)
    return lines
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
    this.drawGlyphRibbonAtmosphere(motionNow, revealT)
    const baseAlpha = 1 - .96 * smooth(revealT / .68)
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
