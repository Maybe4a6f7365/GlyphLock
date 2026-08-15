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
type EventId = 'mail' | 'calendar' | 'github' | 'security' | 'network' | 'model'
type MotionStyle = 'flow' | 'orbital' | 'circuit' | 'radial' | 'bloom'
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
}

interface TargetLayout {
  targets: TargetGlyph[]
  bounds: { left: number; top: number; right: number; bottom: number }
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
  cavityY: number
  palette: string
  tint: [number, number, number]
  atmosphereY: number
  densityBias: number
  motion: MotionStyle
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
  { id: 'sentinel', label: 'Sentinel', cavityY: 1320, palette: ' .·,:;+=x1I|/\\()[]{}<>#08@', tint: [206, 229, 240], atmosphereY: 650, densityBias: 0, motion: 'flow', motionStrength: .72 },
  { id: 'moth', label: 'Moth', cavityY: 1370, palette: ' .·,:;~+xvV(){}<>*#@', tint: [230, 219, 188], atmosphereY: 910, densityBias: .006, motion: 'flow', motionStrength: .84 },
  { id: 'orbit', label: 'Orbit', cavityY: 1440, palette: ' .·,:;~<>/\\()0O@', tint: [208, 199, 244], atmosphereY: 815, densityBias: -.004, motion: 'orbital', motionStrength: .82 },
  { id: 'neural_halo', label: 'Neural Halo', cavityY: 1390, palette: ' .·:;~λψ∇01+<>@', tint: [137, 235, 221], atmosphereY: 840, densityBias: .012, motion: 'radial', motionStrength: 1.05 },
  { id: 'cipher_cathedral', label: 'Cipher Cathedral', cavityY: 1344, palette: ' .:;|[]{}0x#AF16+-', tint: [246, 194, 119], atmosphereY: 930, densityBias: .010, motion: 'circuit', motionStrength: .92 },
  { id: 'quantum_lattice', label: 'Quantum Lattice', cavityY: 1464, palette: ' .·:~λψ∂∇∞01()<>@', tint: [196, 178, 255], atmosphereY: 910, densityBias: .008, motion: 'orbital', motionStrength: 1.08 },
  { id: 'fusion_core', label: 'Fusion Core', cavityY: 1464, palette: ' .:;=+⊙○()[]|01#@', tint: [126, 221, 250], atmosphereY: 930, densityBias: .004, motion: 'orbital', motionStrength: 1.18 },
  { id: 'packet_bloom', label: 'Packet Bloom', cavityY: 1416, palette: ' .·:;<>[]{}:/\\01TCPIP+@', tint: [166, 240, 193], atmosphereY: 990, densityBias: .012, motion: 'bloom', motionStrength: 1.05 },
  { id: 'event_horizon', label: 'Event Horizon', cavityY: 1416, palette: ' .·:;~O0()[]<>∞λ01#@', tint: [255, 173, 112], atmosphereY: 920, densityBias: .010, motion: 'orbital', motionStrength: 1.24 },
  { id: 'tesseract_engine', label: 'Tesseract Engine', cavityY: 1368, palette: ' .:;|+-=[]{}<>01XYZW', tint: [164, 211, 255], atmosphereY: 870, densityBias: .008, motion: 'circuit', motionStrength: 1.08 },
  { id: 'helix_array', label: 'Helix Array', cavityY: 1464, palette: ' .·:;~ATCGλψ01/\\()[]', tint: [151, 238, 211], atmosphereY: 940, densityBias: .012, motion: 'flow', motionStrength: 1.03 },
  { id: 'interference_field', label: 'Interference Field', cavityY: 1392, palette: ' .·:;~≈∿λψ01()<>+@', tint: [239, 176, 242], atmosphereY: 840, densityBias: .006, motion: 'radial', motionStrength: 1.12 },
  { id: 'cryo_vault', label: 'Cryo Vault', cavityY: 1440, palette: ' .:;|[]{}HEXICE01+*', tint: [182, 232, 255], atmosphereY: 980, densityBias: .008, motion: 'circuit', motionStrength: .96 },
  { id: 'dyson_relay', label: 'Dyson Relay', cavityY: 1416, palette: ' .·:;O0()[]{}<>01+*#@', tint: [255, 214, 140], atmosphereY: 860, densityBias: .010, motion: 'orbital', motionStrength: 1.14 },
]

const THEME_BY_ID = new Map(THEMES.map(theme => [theme.id, theme] as const))
const MOTION_PROFILES: Record<MotionProfile, { label: string; speed: number; amplitude: number; liveCount: number; morphCount: number }> = {
  calm: { label: 'Calm', speed: .52, amplitude: .58, liveCount: 520, morphCount: 1450 },
  cinematic: { label: 'Cinematic', speed: 1, amplitude: 1, liveCount: 980, morphCount: 2300 },
  hyper: { label: 'Hyper', speed: 1.42, amplitude: 1.36, liveCount: 1450, morphCount: 3000 },
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
      const eventTarget = eventAssignments[index] ?? this.buildFillerTarget(source, eventLayout.bounds, index, false)
      const resultTarget = resultAssignments[index] ?? this.buildFillerTarget(source, resultLayout.bounds, index, true)
      const distance = Math.hypot(source.x - focusX, source.y - focusY) / diagonal
      const n = hash(index, source.x, source.y)
      return {
        source,
        eventTarget,
        resultTarget,
        phase: n * Math.PI * 2,
        arc: (28 + n * 88) * this.themeSpec.motionStrength,
        delay: .01 + .30 * distance + .075 * n,
        duration: .52 + .17 * (1 - distance) + .06 * n,
      }
    })
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
    const left = 124
    const contentWidth = 832
    const top = this.themeSpec.cavityY - 84
    let y = top
    const accent = event.accent

    const eyebrow = result ? 'LOCAL DEMO · ACTION SIMULATED' : event.eyebrow
    const eyebrowSize = 22
    y += eyebrowSize
    this.addLineTargets(targets, eyebrow, left, y, eyebrowSize, accent, .94, true, 600)

    y += 72
    const title = result ? event.resultTitle : event.title
    const titleSize = this.fitTextSize(title, contentWidth, 60, 38, 520)
    y += titleSize
    this.addLineTargets(targets, title, left, y, titleSize, [241, 247, 250], 1, true, 520)

    y += 66
    const summary = result ? event.resultSummary : event.summary
    const summarySize = 30
    for (const line of this.wrapText(summary, contentWidth, summarySize, 4)) {
      y += summarySize * 1.48
      this.addLineTargets(targets, line, left, y, summarySize, [214, 225, 231], .86, true, 400)
    }

    y += 80
    const action = result ? 'TAP TO RETURN' : event.action
    const actionSize = 20
    y += actionSize
    this.addLineTargets(targets, action, left, y, actionSize, accent, .96, true, 600)

    const bounds = { left: left - 58, top: top - 42, right: left + contentWidth + 58, bottom: y + 62 }
    this.addFragmentedRails(targets, bounds, result)
    return { targets, bounds }
  }

  private addLineTargets(
    targets: TargetGlyph[], text: string, left: number, baseline: number, size: number,
    color: [number, number, number], alpha: number, targetText: boolean, weight: number,
  ): void {
    this.ctx.font = `${weight} ${size}px ${FONT_STACK}`
    let x = left
    for (const char of text) {
      const advance = this.ctx.measureText(char).width
      if (!/\s/.test(char)) targets.push({ x: x + advance / 2, y: baseline, char, size, alpha, color, text: targetText })
      x += advance
    }
  }

  private addFragmentedRails(
    targets: TargetGlyph[], bounds: { left: number; top: number; right: number; bottom: number }, result: boolean,
  ): void {
    const color = mixColor(this.themeSpec.tint, this.event.accent, result ? .52 : .38)
    for (let i = 0; i < 38; i++) {
      if (i > 7 && i < 18) continue
      const x = mix(bounds.left, bounds.right, i / 37)
      const char = STRUCTURE_GLYPHS[(i * 5 + this.event.id.length) % STRUCTURE_GLYPHS.length] ?? '·'
      targets.push({ x, y: bounds.top, char, size: 11, alpha: .34, color, text: false })
      if (i % 2 === 0) targets.push({ x, y: bounds.bottom, char, size: 11, alpha: .24, color, text: false })
    }
    for (let i = 0; i < 22; i++) {
      if (i > 5 && i < 13) continue
      targets.push({ x: bounds.left, y: mix(bounds.top, bounds.bottom, i / 21), char: i % 2 === 0 ? '│' : '·', size: 11, alpha: .28, color, text: false })
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
        const score = dx * dx + dy * dy - source.alpha * INTERNAL_W * INTERNAL_W * .012
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
    bounds: { left: number; top: number; right: number; bottom: number },
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

    switch (spec.motion) {
      case 'orbital': {
        const rotation = (.20 + n * .26) * (n > .5 ? 1 : -1)
        const scale = .92 + n * .19
        const ca = Math.cos(rotation), sa = Math.sin(rotation)
        x = focusX + (vx * ca - vy * sa) * scale
        y = focusY + (vx * sa + vy * ca) * scale
        break
      }
      case 'circuit':
        x = Math.round((source.x + (n - .5) * INTERNAL_W * .08) / 44) * 44
        y = Math.round((source.y + (n - .5) * INTERNAL_H * .04) / 52) * 52
        break
      case 'radial': {
        const ring = Math.max(radius * (.92 + n * .20), INTERNAL_W * (.20 + n * .32))
        x = focusX + Math.cos(angle + seed * .10) * ring
        y = focusY + Math.sin(angle + seed * .10) * ring
        break
      }
      case 'bloom': {
        const petal = Math.sin(angle * 6 + seed) * INTERNAL_W * .05
        const scale = .92 + n * .18 + petal / Math.max(INTERNAL_W, radius) * .7
        x = focusX + vx * scale
        y = focusY + vy * scale
        break
      }
      case 'flow':
      default:
        x += Math.sin(source.y * .009 + seed + n * 4) * INTERNAL_W * .055
        y += Math.cos(source.x * .008 + seed + n * 5) * INTERNAL_H * .016
    }

    const marginX = 60, marginY = 62
    const protectedArea = { left: bounds.left - marginX, top: bounds.top - marginY, right: bounds.right + marginX, bottom: bounds.bottom + marginY }
    if (x >= protectedArea.left && x <= protectedArea.right && y >= protectedArea.top && y <= protectedArea.bottom) {
      const distances = [
        Math.abs(x - protectedArea.left),
        Math.abs(protectedArea.right - x),
        Math.abs(y - protectedArea.top) * .78,
        Math.abs(protectedArea.bottom - y) * .78,
      ]
      const edge = distances.indexOf(Math.min(...distances))
      if (edge === 0) x = protectedArea.left - marginX * (.25 + n * .6)
      else if (edge === 1) x = protectedArea.right + marginX * (.25 + n * .6)
      else if (edge === 2) y = protectedArea.top - marginY * (.3 + n * .8)
      else y = protectedArea.bottom + marginY * (.3 + n * .8)
    }

    x = Math.max(20, Math.min(INTERNAL_W - 20, x))
    y = Math.max(84, Math.min(INTERNAL_H - 60, y))
    const vocabulary = this.event.glyphs + spec.palette + STRUCTURE_GLYPHS
    const char = n > .70 ? vocabulary[Math.min(vocabulary.length - 1, Math.floor(n * vocabulary.length))] ?? source.char : source.char
    return {
      x, y, char,
      size: source.size * (.86 + n * .24),
      alpha: clamp01(source.alpha * (result ? .42 : .50)),
      color: mixColor(spec.tint, this.event.accent, result ? .38 : .27),
      text: false,
    }
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
        x += Math.sin(seconds * .34 + glyph.phase) * 2.7 * spec.motionStrength
        y += Math.cos(seconds * .27 + glyph.phase * 1.4) * 1.8 * spec.motionStrength
      }
      if (listening) {
        const cy = 2110
        const dx = x - INTERNAL_W / 2, dy = y - cy
        const distance = Math.max(1, Math.hypot(dx, dy))
        const pulse = Math.sin(seconds * 6.5 - distance * .020 + glyph.phase)
        const influence = Math.exp(-distance / 450)
        x += dx / distance * pulse * 11 * influence
        y += dy / distance * pulse * 11 * influence
      }

      const sourceFade = 1 - smooth((local - .20) / .52)
      const targetFade = smooth((local - .16) / .62)
      const sourceAlpha = glyph.source.alpha * sourceFade * handoff
      const eventAlpha = glyph.eventTarget.alpha * targetFade * (1 - result)
      const resultAlpha = glyph.resultTarget.alpha * targetFade * result

      if (sourceAlpha > .015) {
        ctx.font = `${mix(glyph.source.size, glyph.eventTarget.size, moveT)}px ${FONT_STACK}`
        const [r, g, b] = spec.tint
        ctx.fillStyle = `rgba(${r},${g},${b},${sourceAlpha * .91})`
        ctx.fillText(glyph.source.char, x, y)
      }
      if (eventAlpha > .015) {
        ctx.font = `${mix(glyph.source.size, glyph.eventTarget.size, moveT)}px ${FONT_STACK}`
        const [r, g, b] = glyph.eventTarget.color
        ctx.fillStyle = `rgba(${r},${g},${b},${eventAlpha})`
        ctx.fillText(glyph.eventTarget.char, x, y)
      }
      if (resultAlpha > .015) {
        ctx.font = `${mix(glyph.eventTarget.size, glyph.resultTarget.size, result)}px ${FONT_STACK}`
        const [r, g, b] = glyph.resultTarget.color
        ctx.fillStyle = `rgba(${r},${g},${b},${resultAlpha})`
        ctx.fillText(glyph.resultTarget.char, x, y)
      }
    }
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
    const baseAlpha = 1 - smooth(revealT / .38)
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
