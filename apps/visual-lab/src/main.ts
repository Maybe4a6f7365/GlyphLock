import { layoutWithLines, prepareWithSegments } from './text-layout.js'
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
type EventId = 'mail' | 'calendar' | 'github' | 'security' | 'network' | 'model'
type MotionStyle = 'flow' | 'orbital' | 'circuit' | 'radial' | 'bloom'
type MotionProfile = 'calm' | 'cinematic' | 'hyper'
type VisualState = 'ambient' | 'revealing' | 'focused' | 'listening' | 'result' | 'collapsing'

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

interface Particle {
  sx: number
  sy: number
  tx: number
  ty: number
  char: string
  phase: number
  arc: number
  size: number
  delay: number
  duration: number
}

interface AmbientGlyph extends GlyphPoint {
  phase: number
  depth: number
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
    id: 'mail',
    eyebrow: 'IMPORTANT MAIL · 2 MIN AGO',
    title: 'DESIGN REVIEW MOVED',
    summary: 'Maya moved tomorrow’s review to 09:30. Current travel time leaves a ten-minute conflict.',
    action: 'HOLD TO CHECK CALENDAR',
    resultTitle: 'DRAFT READY',
    resultSummary: 'Thursday after 14:00 is clear. A concise reply is prepared for review.',
    glyphs: '@<>[]/\MAYA0930',
    accent: [159, 221, 238],
  },
  {
    id: 'calendar',
    eyebrow: 'CALENDAR · CONFLICT',
    title: 'FRIDAY COLLISION',
    summary: 'Client review begins at 10:00. Your train arrives at 10:08 and the next free slot is 11:30.',
    action: 'HOLD TO FIND A SLOT',
    resultTitle: 'OPTION FOUND',
    resultSummary: '11:30 keeps every attendee and removes the travel risk. Ready to propose.',
    glyphs: ':|+−09101130',
    accent: [213, 204, 166],
  },
  {
    id: 'github',
    eyebrow: 'GITHUB · PRODUCTION',
    title: 'DEPLOYMENT RECOVERED',
    summary: 'The image built successfully. Migration failed after 42 seconds and rollback completed without downtime.',
    action: 'HOLD TO SHOW THE CAUSE',
    resultTitle: 'CAUSE ISOLATED',
    resultSummary: 'A missing index exceeded the migration window. A safe remediation plan is staged.',
    glyphs: '{}[]/#01FAIL42',
    accent: [186, 178, 238],
  },
  {
    id: 'security',
    eyebrow: 'SECURITY · PRIVATE EDGE',
    title: 'TLS ROTATION DUE',
    summary: 'The edge certificate expires in 36 hours. Two private services still pin the previous chain.',
    action: 'HOLD TO STAGE ROTATION',
    resultTitle: 'ROTATION PLAN READY',
    resultSummary: 'The new chain validates everywhere. Two pinned services are isolated for a controlled restart.',
    glyphs: 'X509TLS256[]{}CERT',
    accent: [246, 194, 119],
  },
  {
    id: 'network',
    eyebrow: 'NETWORK · HOME LAB',
    title: 'PACKET LOSS CLEARED',
    summary: 'A WireGuard route flap caused 3.8 percent loss for ninety seconds. Traffic is stable again.',
    action: 'HOLD TO TRACE THE PATH',
    resultTitle: 'ROUTE IDENTIFIED',
    resultSummary: 'An overlapping failover rule created the flap. A deterministic route order is ready.',
    glyphs: 'TCPIPWG0138:<>/\\',
    accent: [166, 240, 193],
  },
  {
    id: 'model',
    eyebrow: 'MODEL LAB · RUN 18420',
    title: 'SIMULATION CONVERGED',
    summary: 'The run reached target loss after 18,420 steps. One checkpoint dominates the evaluation set.',
    action: 'HOLD TO INSPECT THE RUN',
    resultTitle: 'CHECKPOINT SELECTED',
    resultSummary: 'Step 17,960 has the best stability margin and no regression on the private test slice.',
    glyphs: 'λψ∇LOSS18420<>01',
    accent: [196, 178, 255],
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
]

const THEME_BY_ID = new Map(THEMES.map(theme => [theme.id, theme] as const))
const MOTION_PROFILES: Record<MotionProfile, { label: string; speed: number; amplitude: number; liveCount: number }> = {
  calm: { label: 'Calm', speed: .52, amplitude: .58, liveCount: 520 },
  cinematic: { label: 'Cinematic', speed: 1, amplitude: 1, liveCount: 980 },
  hyper: { label: 'Hyper', speed: 1.42, amplitude: 1.36, liveCount: 1450 },
}


const INTERNAL_W = 1080
const INTERNAL_H = 2400
const GLYPH_STEP = 10
const RAMP = '  .·,:;+i1tfLCG08@'
const BASE_GLYPHS = ' .·,:;+=x1I|/\\()[]{}<>#08@'

const clamp01 = (v: number) => Math.max(0, Math.min(1, v))
const mix = (a: number, b: number, t: number) => a + (b - a) * t
const ease = (t: number) => 1 - Math.pow(1 - clamp01(t), 3)
const smooth = (t: number) => {
  const x = clamp01(t)
  return x * x * (3 - 2 * x)
}
const hash = (x: number, y: number, seed = 0) => {
  const n = Math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453123
  return n - Math.floor(n)
}

class GlyphLockRenderer {
  readonly canvas: HTMLCanvasElement
  readonly ctx: CanvasRenderingContext2D
  private theme: ThemeId = 'sentinel'
  private eventIndex = 0
  private state: VisualState = 'ambient'
  private transitionStart = performance.now()
  private transitionDuration = 1800
  private masks = new Map<ThemeId, HTMLImageElement>()
  private maskCanvas = document.createElement('canvas')
  private maskCtx = this.maskCanvas.getContext('2d', { willReadFrequently: true })!
  private baseCanvas = document.createElement('canvas')
  private eventCanvas = document.createElement('canvas')
  private textMaskCanvas = document.createElement('canvas')
  private basePoints: GlyphPoint[] = []
  private ambientGlyphs: AmbientGlyph[] = []
  private textPoints: GlyphPoint[] = []
  private particles: Particle[] = []
  private motionProfile: MotionProfile = 'cinematic'
  private wakeStart = performance.now()
  private pointerParallax = { x: 0, y: 0 }
  private captureMotionMs: number | null = null
  private raf = 0
  private lastFrame = performance.now()
  private pointerStart: { x: number; y: number; at: number } | null = null
  private holdTimer = 0
  private captureT: number | null = null

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas
    const ctx = canvas.getContext('2d', { alpha: false })
    if (!ctx) throw new Error('Canvas 2D is unavailable')
    this.ctx = ctx
    for (const c of [canvas, this.maskCanvas, this.baseCanvas, this.eventCanvas, this.textMaskCanvas]) {
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
    if (!new URLSearchParams(location.search).has('capture')) {
      this.raf = requestAnimationFrame(this.render)
    }
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
    this.rebuildAmbientGlyphs()
    this.wake()
  }

  wake(): void {
    this.wakeStart = performance.now()
  }

  setEvent(id: EventId): void {
    const index = EVENTS.findIndex(event => event.id === id)
    if (index < 0 || index === this.eventIndex) return
    this.eventIndex = index
    this.rebuildEvent()
    this.reveal()
  }

  nextEvent(direction = 1): void {
    this.eventIndex = (this.eventIndex + direction + EVENTS.length) % EVENTS.length
    this.rebuildEvent()
    this.reveal()
    syncControls(this.theme, EVENTS[this.eventIndex]!.id)
  }

  reveal(): void {
    this.state = 'revealing'
    this.transitionStart = performance.now()
    this.transitionDuration = 1750
  }

  collapse(): void {
    if (this.state === 'ambient') return
    this.state = 'collapsing'
    this.transitionStart = performance.now()
    this.transitionDuration = 1250
  }

  listen(): void {
    this.state = 'listening'
    this.transitionStart = performance.now()
    this.transitionDuration = 900
    window.setTimeout(() => {
      if (this.state !== 'listening') return
      this.state = 'result'
      this.transitionStart = performance.now()
      this.transitionDuration = 1000
      this.rebuildEvent(true)
    }, 1650)
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
    this.rebuildAmbientGlyphs()
    this.renderBaseBitmap()
    this.rebuildEvent()
  }

  private rebuildAmbientGlyphs(): void {
    const candidates = this.basePoints.filter(point => point.alpha > .18 && point.y > 170 && point.y < INTERNAL_H - 120)
    const count = Math.min(this.motionSpec.liveCount, candidates.length)
    const glyphs: AmbientGlyph[] = []
    for (let i = 0; i < count; i++) {
      const point = candidates[(i * 97 + this.theme.length * 53) % Math.max(1, candidates.length)]
      if (!point) continue
      const phase = hash(i, point.x, point.y) * Math.PI * 2
      glyphs.push({ ...point, phase, depth: .25 + .75 * hash(point.y, i, 11) })
    }
    this.ambientGlyphs = glyphs
  }

  private extractGlyphPoints(): GlyphPoint[] {
    const pixels = this.maskCtx.getImageData(0, 0, INTERNAL_W, INTERNAL_H).data
    const points: GlyphPoint[] = []
    for (let y = 120; y < INTERNAL_H - 90; y += GLYPH_STEP) {
      for (let x = 30; x < INTERNAL_W - 30; x += GLYPH_STEP) {
        const i = (y * INTERNAL_W + x) * 4
        const value = pixels[i]! / 255
        const n = hash(x, y, this.theme.length)
        const threshold = 0.034 + n * 0.098 - this.themeSpec.densityBias
        if (value < threshold && n < 0.92) continue
        const level = clamp01(value * 1.12 + (n - 0.5) * 0.10)
        const charIndex = Math.min(RAMP.length - 1, Math.floor(level * RAMP.length))
        let char = RAMP[charIndex] ?? '.'
        if (level < 0.20 || n > .88) char = this.themeSpec.palette[Math.floor(n * this.themeSpec.palette.length)] ?? BASE_GLYPHS[Math.floor(n * BASE_GLYPHS.length)] ?? '.'
        points.push({
          x: x + (n - 0.5) * 1.25,
          y: y + (hash(y, x, 3) - 0.5) * 1.15,
          char,
          alpha: clamp01(0.08 + level * 0.94),
          size: 9 + level * 4.2,
        })
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
      const mixTint = .12 + point.alpha * .13
      const r = Math.round(mix(value, tr, mixTint))
      const g = Math.round(mix(value, tg, mixTint))
      const b = Math.round(mix(value, tb, mixTint))
      ctx.fillStyle = `rgba(${r},${g},${b},${point.alpha * .92})`
      ctx.font = `${point.size}px "Roboto Mono", "SFMono-Regular", monospace`
      ctx.fillText(point.char, point.x, point.y)
    }
    const gradient = ctx.createRadialGradient(INTERNAL_W / 2, this.themeSpec.atmosphereY, 20, INTERNAL_W / 2, this.themeSpec.atmosphereY, 820)
    gradient.addColorStop(0, `rgba(${tr},${tg},${tb},.050)`)
    gradient.addColorStop(.45, `rgba(${tr},${tg},${tb},.018)`)
    gradient.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, INTERNAL_W, INTERNAL_H)
  }

  private rebuildEvent(result = false): void {
    const event = EVENTS[this.eventIndex]!
    const ctx = this.eventCanvas.getContext('2d', { alpha: false })!
    ctx.fillStyle = '#000'
    ctx.fillRect(0, 0, INTERNAL_W, INTERNAL_H)
    ctx.globalAlpha = 0.34
    ctx.drawImage(this.baseCanvas, 0, 0)
    ctx.globalAlpha = 1

    const cavityY = this.themeSpec.cavityY
    const cavity = ctx.createRadialGradient(INTERNAL_W / 2, cavityY + 220, 60, INTERNAL_W / 2, cavityY + 220, 510)
    cavity.addColorStop(0, 'rgba(0,0,0,.98)')
    cavity.addColorStop(.62, 'rgba(0,0,0,.88)')
    cavity.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = cavity
    ctx.fillRect(0, cavityY - 230, INTERNAL_W, 900)

    const title = result ? event.resultTitle : event.title
    const summary = result ? event.resultSummary : event.summary
    const [r, g, b] = event.accent
    const left = 148
    const width = 784
    let y = cavityY

    ctx.textAlign = 'left'
    ctx.textBaseline = 'alphabetic'
    ctx.fillStyle = `rgba(${r},${g},${b},.78)`
    ctx.font = '600 22px "Roboto Mono", "SFMono-Regular", monospace'
    ctx.fillText(result ? 'LOCAL DEMO · ACTION SIMULATED' : event.eyebrow, left, y)
    y += 68

    ctx.fillStyle = 'rgba(244,248,250,.98)'
    ctx.font = '520 58px "Roboto Mono", "SFMono-Regular", monospace'
    ctx.fillText(title, left, y)
    y += 34

    ctx.strokeStyle = `rgba(${r},${g},${b},.44)`
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(left, y)
    ctx.lineTo(left + 156, y)
    ctx.stroke()
    y += 64

    const summaryFont = '400 31px "Roboto Mono", "SFMono-Regular", monospace'
    const prepared = prepareWithSegments(summary, summaryFont)
    const layout = layoutWithLines(prepared, width, 47)
    ctx.font = summaryFont
    ctx.fillStyle = 'rgba(226,233,237,.82)'
    for (const line of layout.lines.slice(0, 4)) {
      ctx.fillText(line.text, left, y)
      y += 47
    }
    y += 52

    ctx.fillStyle = `rgba(${r},${g},${b},.84)`
    ctx.font = '600 20px "Roboto Mono", "SFMono-Regular", monospace'
    ctx.fillText(result ? 'TAP TO RETURN' : event.action, left, y)

    // A very restrained command rail—not a conventional card border.
    ctx.strokeStyle = `rgba(${r},${g},${b},.22)`
    ctx.beginPath()
    ctx.moveTo(left - 28, cavityY - 56)
    ctx.lineTo(left - 28, y + 28)
    ctx.stroke()

    this.buildTextPoints(event, result)
    this.buildParticles(event)
  }

  private buildTextPoints(event: DemoEvent, result: boolean): void {
    const ctx = this.textMaskCanvas.getContext('2d', { willReadFrequently: true })!
    ctx.clearRect(0, 0, INTERNAL_W, INTERNAL_H)
    ctx.fillStyle = '#fff'
    ctx.textAlign = 'left'
    ctx.textBaseline = 'alphabetic'
    const cavityY = this.themeSpec.cavityY
    const left = 148
    let y = cavityY + 68
    ctx.font = '520 58px "Roboto Mono", "SFMono-Regular", monospace'
    ctx.fillText(result ? event.resultTitle : event.title, left, y)
    y += 98
    const summary = result ? event.resultSummary : event.summary
    const summaryFont = '400 31px "Roboto Mono", "SFMono-Regular", monospace'
    const prepared = prepareWithSegments(summary, summaryFont)
    const layout = layoutWithLines(prepared, 784, 47)
    ctx.font = summaryFont
    for (const line of layout.lines.slice(0, 4)) {
      ctx.fillText(line.text, left, y)
      y += 47
    }

    const pixels = ctx.getImageData(0, 0, INTERNAL_W, INTERNAL_H).data
    const points: GlyphPoint[] = []
    const step = 7
    for (let yy = cavityY - 20; yy < Math.min(INTERNAL_H - 220, y + 40); yy += step) {
      for (let xx = 105; xx < 975; xx += step) {
        const alpha = pixels[(yy * INTERNAL_W + xx) * 4 + 3]! / 255
        if (alpha < 0.3) continue
        const n = hash(xx, yy, this.eventIndex + 17)
        points.push({ x: xx, y: yy, char: event.glyphs[Math.floor(n * event.glyphs.length)] ?? '@', alpha: 0.88, size: 8 + n * 5 })
      }
    }
    this.textPoints = points
  }

  private buildParticles(event: DemoEvent): void {
    const sources = this.basePoints.filter(point => point.y > 360 && point.y < 2050 && point.alpha > 0.24)
    const targets = this.textPoints
    const count = Math.min(2500, Math.max(900, targets.length))
    const particles: Particle[] = []
    for (let i = 0; i < count; i++) {
      const target = targets[(i * 37) % Math.max(1, targets.length)] ?? { x: INTERNAL_W / 2, y: 1500, char: '@', alpha: 1, size: 10 }
      const source = sources[(i * 83 + this.eventIndex * 101) % Math.max(1, sources.length)] ?? target
      const n = hash(i, this.eventIndex, this.theme.length)
      const sourceBand = clamp01(source.y / INTERNAL_H)
      const targetBand = clamp01(target.y / INTERNAL_H)
      particles.push({
        sx: source.x,
        sy: source.y,
        tx: target.x,
        ty: target.y,
        char: event.glyphs[Math.floor(n * event.glyphs.length)] ?? target.char,
        phase: n * Math.PI * 2,
        arc: (30 + n * 108) * this.themeSpec.motionStrength,
        size: 8 + n * 6,
        delay: .015 + .27 * sourceBand + .08 * hash(i, source.x, 7),
        duration: .56 + .13 * (1 - targetBand) + .05 * n,
      })
    }
    this.particles = particles
  }

  private drawParticles(t: number, now: number): void {
    if (t <= 0 || t >= 1) return
    const event = EVENTS[this.eventIndex]!
    const [r, g, b] = event.accent
    const ctx = this.ctx
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const visibility = Math.sin(Math.PI * t)
    const focusX = INTERNAL_W / 2
    const focusY = this.themeSpec.atmosphereY

    for (let i = 0; i < this.particles.length; i++) {
      const p = this.particles[i]!
      const local = smooth((t - p.delay) / Math.max(.001, p.duration))
      if (local <= 0 || local >= 1) continue
      const moveT = ease(local)
      const wave = Math.sin(Math.PI * local)
      let x: number
      let y: number

      switch (this.themeSpec.motion) {
        case 'circuit': {
          const horizontalFirst = Math.sin(p.phase) >= 0
          if (horizontalFirst) {
            x = mix(p.sx, p.tx, smooth(Math.min(1, moveT * 1.85)))
            y = mix(p.sy, p.ty, smooth(Math.max(0, (moveT - .46) / .54)))
          } else {
            y = mix(p.sy, p.ty, smooth(Math.min(1, moveT * 1.85)))
            x = mix(p.sx, p.tx, smooth(Math.max(0, (moveT - .46) / .54)))
          }
          x += Math.sin(p.phase * 1.7) * p.arc * .055 * wave
          y += Math.cos(p.phase * 1.3) * p.arc * .055 * wave
          break
        }
        case 'orbital': {
          const midX = (p.sx + p.tx) * .5
          const midY = (p.sy + p.ty) * .5
          const vx = midX - focusX
          const vy = midY - focusY
          const length = Math.max(1, Math.hypot(vx, vy))
          const direction = Math.sin(p.phase) >= 0 ? 1 : -1
          const controlX = midX - vy / length * p.arc * 1.35 * direction
          const controlY = midY + vx / length * p.arc * 1.35 * direction
          const u = 1 - moveT
          x = u * u * p.sx + 2 * u * moveT * controlX + moveT * moveT * p.tx
          y = u * u * p.sy + 2 * u * moveT * controlY + moveT * moveT * p.ty
          break
        }
        case 'radial': {
          const tangent = p.phase + moveT * 2.4
          const controlX = mix(p.sx, focusX, .58) + Math.cos(tangent) * p.arc
          const controlY = mix(p.sy, focusY, .58) + Math.sin(tangent) * p.arc * .66
          const u = 1 - moveT
          x = u * u * p.sx + 2 * u * moveT * controlX + moveT * moveT * p.tx
          y = u * u * p.sy + 2 * u * moveT * controlY + moveT * moveT * p.ty
          break
        }
        case 'bloom': {
          const sourceAngle = Math.atan2(p.sy - focusY, p.sx - focusX)
          const petal = Math.sin(sourceAngle * 6 + p.phase) * p.arc
          const controlX = focusX + Math.cos(sourceAngle) * (INTERNAL_W * .22 + petal)
          const controlY = focusY + Math.sin(sourceAngle) * (INTERNAL_W * .18 + petal * .55)
          const u = 1 - moveT
          x = u * u * p.sx + 2 * u * moveT * controlX + moveT * moveT * p.tx
          y = u * u * p.sy + 2 * u * moveT * controlY + moveT * moveT * p.ty
          break
        }
        case 'flow':
        default:
          x = mix(p.sx, p.tx, moveT) + Math.sin(p.phase + local * 5.2) * p.arc * wave
          y = mix(p.sy, p.ty, moveT) + Math.cos(p.phase * 1.3 + local * 4.1) * p.arc * .48 * wave
      }

      const alpha = visibility * wave * (0.22 + 0.72 * hash(i, this.eventIndex, 2))
      ctx.font = `${p.size}px "Roboto Mono", "SFMono-Regular", monospace`
      ctx.fillStyle = `rgba(${r},${g},${b},${alpha})`
      ctx.fillText(p.char, x, y)
    }

    const waveY = mix(380, 2020, t)
    const glow = ctx.createLinearGradient(0, waveY - 100, 0, waveY + 100)
    glow.addColorStop(0, 'rgba(0,0,0,0)')
    glow.addColorStop(.5, `rgba(${r},${g},${b},${0.035 * visibility})`)
    glow.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = glow
    ctx.fillRect(0, waveY - 100, INTERNAL_W, 200)
  }

  private drawAmbientMotion(now: number, revealT: number): void {
    const ctx = this.ctx
    const spec = this.themeSpec
    const profile = this.motionSpec
    const seconds = now / 1000 * profile.speed
    const focusX = INTERNAL_W / 2
    const focusY = spec.atmosphereY
    const [r, g, b] = spec.tint
    const wakeAge = Math.max(0, now - this.wakeStart)
    const wakeT = clamp01(wakeAge / 2200)
    const wakeRadius = mix(18, 980, ease(wakeT))
    const wakeStrength = 1 - smooth(wakeT)
    const fade = 1 - revealT * .72

    ctx.save()
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.globalCompositeOperation = 'screen'
    for (let i = 0; i < this.ambientGlyphs.length; i++) {
      const p = this.ambientGlyphs[i]!
      const strength = spec.motionStrength * profile.amplitude * p.depth
      const vx = p.x - focusX
      const vy = p.y - focusY
      const radius = Math.max(1, Math.hypot(vx, vy))
      let x = p.x + this.pointerParallax.x * (3 + 12 * p.depth)
      let y = p.y + this.pointerParallax.y * (3 + 10 * p.depth)

      switch (spec.motion) {
        case 'orbital': {
          const angle = Math.sin(seconds * .30 + p.phase) * .012 * strength + seconds * .0018 * (p.phase > Math.PI ? -1 : 1)
          const ca = Math.cos(angle), sa = Math.sin(angle)
          x = focusX + vx * ca - vy * sa
          y = focusY + vx * sa + vy * ca
          x += Math.sin(seconds * .72 + p.phase) * 2.4 * strength
          break
        }
        case 'circuit': {
          const lane = ((seconds * 22 * (.35 + p.depth) + p.phase * 40) % 92) - 46
          y += lane * .18 * strength
          x += (Math.sin(seconds * 1.4 + p.phase) > .74 ? 7 : 0) * strength
          break
        }
        case 'radial': {
          const scale = 1 + Math.sin(seconds * .82 + p.phase) * .0068 * strength
          x = focusX + vx * scale
          y = focusY + vy * scale
          const tangent = Math.sin(seconds * .5 + p.phase) * 4.2 * strength
          x += -vy / radius * tangent
          y += vx / radius * tangent
          break
        }
        case 'bloom': {
          const angle = Math.atan2(vy, vx)
          const petal = Math.sin(angle * 6 + seconds * .66 + p.phase) * 6.5 * strength
          const scale = 1 + petal / Math.max(180, radius) * .42
          x = focusX + vx * scale
          y = focusY + vy * scale
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
      ctx.font = `${p.size * (1 + wakeBand * .12)}px "Roboto Mono", "SFMono-Regular", monospace`
      ctx.fillStyle = `rgba(${r},${g},${b},${alpha})`
      ctx.fillText(p.char, x, y)
    }
    ctx.restore()

    if (wakeStrength > .001) {
      const halo = ctx.createRadialGradient(focusX, focusY, Math.max(0, wakeRadius - 110), focusX, focusY, wakeRadius + 110)
      halo.addColorStop(0, 'rgba(0,0,0,0)')
      halo.addColorStop(.50, `rgba(${r},${g},${b},${.035 * wakeStrength})`)
      halo.addColorStop(1, 'rgba(0,0,0,0)')
      ctx.globalCompositeOperation = 'screen'
      ctx.fillStyle = halo
      ctx.fillRect(0, 0, INTERNAL_W, INTERNAL_H)
      ctx.globalCompositeOperation = 'source-over'
    }
  }

  private drawListening(now: number): void {
    const event = EVENTS[this.eventIndex]!
    const [r, g, b] = event.accent
    const ctx = this.ctx
    const pulse = 0.5 + 0.5 * Math.sin(now / 145)
    const cy = 2110
    const gradient = ctx.createRadialGradient(INTERNAL_W / 2, cy, 20, INTERNAL_W / 2, cy, 240 + pulse * 36)
    gradient.addColorStop(0, `rgba(${r},${g},${b},${0.12 + pulse * 0.08})`)
    gradient.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = gradient
    ctx.fillRect(230, 1850, 620, 470)
    ctx.textAlign = 'center'
    ctx.fillStyle = `rgba(${r},${g},${b},${0.72 + pulse * .22})`
    ctx.font = '600 20px "Roboto Mono", monospace'
    ctx.fillText('LISTENING TO THIS EVENT', INTERNAL_W / 2, 2130)
    for (let i = -9; i <= 9; i++) {
      const h = 8 + 38 * Math.abs(Math.sin(now / 180 + i * .62)) * (1 - Math.abs(i) / 13)
      ctx.fillRect(INTERNAL_W / 2 + i * 13, 2175 - h / 2, 3, h)
    }
  }

  private render = (now: number): void => {
    this.draw(now)
    this.lastFrame = now
    this.raf = requestAnimationFrame(this.render)
  }

  private draw(now: number): void {
    const ctx = this.ctx
    ctx.fillStyle = '#000'
    ctx.fillRect(0, 0, INTERNAL_W, INTERNAL_H)

    let revealT = 0
    if (this.captureT !== null) {
      revealT = this.captureT
    } else if (this.state === 'ambient') {
      revealT = 0
    } else if (this.state === 'revealing') {
      revealT = clamp01((now - this.transitionStart) / this.transitionDuration)
      if (revealT >= 1) this.state = 'focused'
    } else if (this.state === 'collapsing') {
      revealT = 1 - clamp01((now - this.transitionStart) / this.transitionDuration)
      if (revealT <= 0) this.state = 'ambient'
    } else {
      revealT = 1
    }

    const t = smooth(revealT)
    const motionNow = this.captureMotionMs ?? now
    const breathing = Math.sin(motionNow / 3600 * this.motionSpec.speed) * .0016 * this.motionSpec.amplitude
    ctx.save()
    ctx.translate(INTERNAL_W / 2, INTERNAL_H / 2)
    ctx.scale(1 + breathing, 1 + breathing)
    ctx.translate(-INTERNAL_W / 2, -INTERNAL_H / 2)
    ctx.globalAlpha = 1 - t * 0.78
    ctx.drawImage(this.baseCanvas, 0, 0)
    ctx.restore()
    this.drawAmbientMotion(motionNow, t)
    ctx.globalAlpha = t
    ctx.drawImage(this.eventCanvas, 0, 0)
    ctx.globalAlpha = 1
    this.drawParticles(t, motionNow)

    // Almost imperceptible ambient scan grain.
    ctx.globalCompositeOperation = 'screen'
    const scanY = (motionNow * 0.035 * this.motionSpec.speed) % INTERNAL_H
    const scan = ctx.createLinearGradient(0, scanY - 80, 0, scanY + 80)
    scan.addColorStop(0, 'rgba(255,255,255,0)')
    scan.addColorStop(.5, 'rgba(200,224,236,.018)')
    scan.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.fillStyle = scan
    ctx.fillRect(0, scanY - 80, INTERNAL_W, 160)
    ctx.globalCompositeOperation = 'source-over'

    if (this.state === 'listening') this.drawListening(motionNow)
  }

  private attachInput(): void {
    const stage = this.canvas.closest('.stage') as HTMLElement
    stage.addEventListener('pointermove', event => {
      const rect = stage.getBoundingClientRect()
      this.pointerParallax.x = ((event.clientX - rect.left) / rect.width - .5) * 2
      this.pointerParallax.y = ((event.clientY - rect.top) / rect.height - .5) * 2
    })
    stage.addEventListener('pointerleave', () => {
      this.pointerParallax.x *= .35
      this.pointerParallax.y *= .35
    })
    stage.addEventListener('pointerdown', event => {
      const rect = stage.getBoundingClientRect()
      const x = (event.clientX - rect.left) / rect.width
      const y = (event.clientY - rect.top) / rect.height
      this.pointerStart = { x, y, at: performance.now() }
      if (y > 0.68) {
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
    stage.addEventListener('pointercancel', () => {
      window.clearTimeout(this.holdTimer)
      this.pointerStart = null
    })

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
    button.addEventListener('click', () => {
      renderer.setTheme(theme.id)
      syncControls(renderer.currentTheme, renderer.currentEventId)
    })
    themeRoot.append(button)
  }

  const motionRoot = document.querySelector('#motion-controls')!
  for (const [id, profile] of Object.entries(MOTION_PROFILES) as [MotionProfile, typeof MOTION_PROFILES[MotionProfile]][]) {
    const button = document.createElement('button')
    button.textContent = profile.label
    button.dataset.motion = id
    button.addEventListener('click', () => {
      renderer.setMotionProfile(id)
      syncControls(renderer.currentTheme, renderer.currentEventId)
    })
    motionRoot.append(button)
  }

  const eventRoot = document.querySelector('#event-controls')!
  for (const event of EVENTS) {
    const button = document.createElement('button')
    button.dataset.event = event.id
    const title = document.createElement('strong')
    title.textContent = event.title
    const sub = document.createElement('span')
    sub.textContent = event.eyebrow
    button.append(title, sub)
    button.addEventListener('click', () => {
      renderer.setEvent(event.id)
      syncControls(renderer.currentTheme, renderer.currentEventId)
    })
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
