import { PRINT_LIMITS, PrintError } from '../protocol/types'
import { mmToPx, ptToMm, pxToMm } from '../protocol/units'
import { cellStyle, printStyle } from '../renderers/style'
import { MeasurementCache } from './measurementCache'

/** Browser measurements use the same physical CSS as the final paper renderers. */
export function createBrowserMeasurer(document = globalThis.document) {
  const host = document.createElement('div')
  host.style.cssText = 'all:initial;position:fixed;left:-100000px;top:0;visibility:hidden;contain:layout style;'
  document.body.append(host)
  const cache = new MeasurementCache()
  const segmenter = new Intl.Segmenter('zh', { granularity: 'grapheme' })

  function text(text, widthMm, style = {}) {
    if (text.length > PRINT_LIMITS.textLength) {
      throw new PrintError('TEXT_TOO_LONG', '文本超过限制')
    }
    const key = ['text', text, widthMm, style]
    const cached = cache.get(key)
    if (cached) {
      return cached
    }
    const paddingMm = (style.paddingMm || 0) + (style.borderWidthMm || 0)
    const usableWidth = mmToPx(widthMm - 2 * paddingMm)
    if (usableWidth <= 0) {
      throw new PrintError('NO_TEXT_AREA', '文本内边距未留下可用宽度')
    }
    const span = document.createElement('span')
    Object.assign(span.style, printStyle(style), { display: 'inline-block', padding: '0', border: '0', whiteSpace: 'pre' })
    host.append(span)
    const lines = []
    try {
      for (const paragraph of text.replace(/\r\n?/g, '\n').split('\n')) {
        const chars = Array.from(segmenter.segment(paragraph), item => item.segment)
        if (!chars.length) {
          lines.push('')
        }
        let start = 0
        while (start < chars.length) {
          let low = 1
          let high = chars.length - start
          let fit = 0
          while (low <= high) {
            const count = Math.floor((low + high) / 2)
            span.textContent = chars.slice(start, start + count).join('')
            if (span.getBoundingClientRect().width <= usableWidth + 0.01) {
              fit = count
              low = count + 1
            }
            else {
              high = count - 1
            }
          }
          if (!fit) {
            throw new PrintError('GLYPH_TOO_WIDE', '文本列宽小于单个字符宽度')
          }
          lines.push(chars.slice(start, start + fit).join(''))
          start += fit
        }
      }
    }
    finally {
      span.remove()
    }
    const lineHeightMm = ptToMm(style.fontSizePt ?? 10) * (style.lineHeight ?? 1.4)
    return cache.set(key, { lines, lineHeightMm, insetMm: paddingMm, heightMm: lines.length * lineHeightMm + 2 * paddingMm })
  }

  function row(cells) {
    const key = ['row', cells]
    const cached = cache.get(key)
    if (cached !== undefined) {
      return cached
    }
    let heightMm = 0
    for (const cell of cells) {
      if (cell.type === 'IMAGE' && cell.src) {
        const inset = (cell.style?.paddingMm ?? 1) + (cell.style?.borderWidthMm ?? 0.15)
        heightMm = Math.max(heightMm, cell.imageHeightMm + 2 * inset)
        continue
      }
      if (cell.text.length > PRINT_LIMITS.textLength) {
        throw new PrintError('TEXT_TOO_LONG', '明细单元格文本超过限制')
      }
      const inset = (cell.style?.paddingMm ?? 1) + (cell.style?.borderWidthMm ?? 0.15)
      if (cell.widthMm <= 2 * inset) {
        throw new PrintError('NO_TEXT_AREA', '表格列宽小于单元格内边距')
      }
      const node = document.createElement('div')
      Object.assign(node.style, cellStyle(cell.style), { width: `${cell.widthMm}mm` })
      node.textContent = cell.text || '\u200B'
      host.append(node)
      heightMm = Math.max(heightMm, pxToMm(node.getBoundingClientRect().height))
      node.remove()
    }
    // A small physical guard covers CSS pixel quantization in the separate print document.
    return cache.set(key, heightMm + 0.05)
  }

  return {
    text,
    row,
    dispose() {
      host.remove()
      cache.clear()
    },
  }
}
