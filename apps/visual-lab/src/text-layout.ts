export interface PreparedText {
  text: string
  font: string
}

export interface LayoutLine {
  text: string
  width: number
}

export interface LayoutResult {
  height: number
  lineCount: number
  lines: LayoutLine[]
}

const measurementCanvas = document.createElement('canvas')
const measurementContext = measurementCanvas.getContext('2d')!
if (!measurementContext) throw new Error('Canvas text measurement is unavailable')

/**
 * Prototype-zero text adapter.
 *
 * It deliberately mirrors the tiny subset of Pretext used by the visual lab,
 * while keeping this first proof build dependency-free. Replace this module
 * with @chenglou/pretext after the visual direction passes review.
 */
export function prepareWithSegments(text: string, font: string): PreparedText {
  return { text, font }
}

export function layoutWithLines(prepared: PreparedText, maxWidth: number, lineHeight: number): LayoutResult {
  measurementContext.font = prepared.font
  const segmenter = typeof Intl.Segmenter === 'function'
    ? new Intl.Segmenter(undefined, { granularity: 'word' })
    : null
  const tokens = segmenter
    ? Array.from(segmenter.segment(prepared.text), item => item.segment)
    : prepared.text.split(/(\s+)/)

  const lines: LayoutLine[] = []
  let current = ''

  const push = (text: string): void => {
    const normalized = text.trimEnd()
    if (!normalized) return
    lines.push({ text: normalized, width: measurementContext.measureText(normalized).width })
  }

  const breakLongToken = (token: string): void => {
    const graphemes = typeof Intl.Segmenter === 'function'
      ? Array.from(new Intl.Segmenter(undefined, { granularity: 'grapheme' }).segment(token), item => item.segment)
      : Array.from(token)
    let part = ''
    for (const grapheme of graphemes) {
      const candidate = part + grapheme
      if (part && measurementContext.measureText(candidate).width > maxWidth) {
        push(part)
        part = grapheme
      } else {
        part = candidate
      }
    }
    current = part
  }

  for (const token of tokens) {
    if (token.includes('\n')) {
      const pieces = token.split('\n')
      for (let i = 0; i < pieces.length; i++) {
        const piece = pieces[i] ?? ''
        if (piece) {
          const candidate = current + piece
          if (current && measurementContext.measureText(candidate).width > maxWidth) {
            push(current)
            current = piece.trimStart()
          } else {
            current = candidate
          }
        }
        if (i < pieces.length - 1) {
          push(current)
          current = ''
        }
      }
      continue
    }

    const candidate = current + token
    if (!current || measurementContext.measureText(candidate).width <= maxWidth) {
      current = candidate
      continue
    }

    push(current)
    const trimmed = token.trimStart()
    if (measurementContext.measureText(trimmed).width > maxWidth) breakLongToken(trimmed)
    else current = trimmed
  }
  push(current)

  return {
    lines,
    lineCount: lines.length,
    height: lines.length * lineHeight,
  }
}
