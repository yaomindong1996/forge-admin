import { describe, expect, it } from 'vitest'
import { createPrintDocument } from '../../protocol/types'
import { layoutPrintDocument } from '../layout'

const measure = {
  text: value => ({ lines: value.split('\n'), lineHeightMm: 5, insetMm: 0, heightMm: value.split('\n').length * 5 }),
  row: () => 10,
}
function paper() {
  const doc = createPrintDocument()
  doc.paper = { widthMm: 100, heightMm: 100, orientation: 'PORTRAIT', marginMm: { top: 10, right: 10, bottom: 10, left: 10 } }
  return doc
}
function fixed(id, heightMm) {
  return { id, kind: 'FIXED', heightMm, elements: [] }
}
const layout = doc => layoutPrintDocument(doc, { system: { generatedAt: '2026-09-18 12:00:00' } }, { measure, catalog: [] })

describe('ordered physical pagination', () => {
  it('does not create a trailing page on an exact boundary or a trailing gap', () => {
    const doc = paper()
    doc.body = [{ ...fixed('a', 80), gapAfterMm: 10 }]
    expect(layout(doc).pages).toHaveLength(1)
  })
  it('starts a new physical page at an explicit page break', () => {
    const doc = paper()
    doc.body = [fixed('before', 20), { id: 'break', kind: 'PAGE_BREAK' }, fixed('after', 20)]
    const result = layout(doc)
    expect(result.pages).toHaveLength(2)
    expect(result.pages.map(page => page.fragments.map(fragment => fragment.id))).toEqual([['before'], ['after']])
  })
  it('moves fixed sections as a whole and rejects oversized sections', () => {
    const doc = paper()
    doc.body = [fixed('a', 50), fixed('b', 40)]
    const result = layout(doc)
    expect(result.pages).toHaveLength(2)
    expect(result.pages[1].fragments[0].yMm).toBe(10)
    doc.body = [fixed('oversized', 81)]
    expect(() => layout(doc)).toThrow(expect.objectContaining({ code: 'ELEMENT_TOO_TALL', path: 'oversized' }))
  })
  it('splits text without losing blank lines or adding overlap', () => {
    const doc = paper()
    const lines = Array.from({ length: 35 }, (_, i) => i === 7 ? '' : `第${i}行`)
    doc.body = [{ id: 'long', kind: 'TEXT', binding: { source: 'CONSTANT', value: lines.join('\n') } }]
    const result = layout(doc)
    expect(result.pages).toHaveLength(3)
    expect(result.pages.flatMap(page => page.fragments.flatMap(fragment => fragment.lines))).toEqual(lines)
    expect(Object.isFrozen(result.pages[0].fragments)).toBe(true)
  })
  it('keeps a heading with the following section and fills page slots after pagination', () => {
    const doc = paper()
    doc.body = [fixed('first', 70), { ...fixed('heading', 8), keepWithNext: true }, fixed('next', 10)]
    doc.footer = { heightMm: 5, repeat: true, elements: [{ id: 'page', type: 'PAGE_NUMBER', pageNumberFormat: 'CURRENT_TOTAL', xMm: 0, yMm: 0, widthMm: 30, heightMm: 5 }] }
    const result = layout(doc)
    expect(result.pages).toHaveLength(2)
    expect(result.pages[1].fragments.map(fragment => fragment.id)).toEqual(['heading', 'next'])
    expect(result.pages[1].footer.elements[0].text).toBe('2 / 2')
  })
  it('rejects more than 50 pages and unauthorized field bindings', () => {
    const doc = paper()
    doc.body = Array.from({ length: 51 }, (_, i) => fixed(`page-${i}`, 80))
    expect(() => layout(doc)).toThrow(expect.objectContaining({ code: 'PAGE_LIMIT' }))
    doc.body = [{ id: 'secret', kind: 'TEXT', binding: { source: 'FIELD', path: 'main.salary' } }]
    expect(() => layout(doc)).toThrow(expect.objectContaining({ code: 'FIELD_NOT_ALLOWED' }))
  })
  it('grows continuous paper to content height and tiles labels onto a sheet', () => {
    const doc = paper()
    doc.paper.kind = 'CONTINUOUS'
    doc.body = [fixed('block', 40)]
    const continuous = layout(doc)
    expect(continuous.pages).toHaveLength(1)
    expect(continuous.geometry.heightMm).toBeLessThan(100)
    expect(continuous.geometry.heightMm).toBeGreaterThan(40)
    const label = paper()
    label.paper = { widthMm: 50, heightMm: 30, orientation: 'LANDSCAPE', marginMm: { top: 0, right: 0, bottom: 0, left: 0 }, tiling: { enabled: true, columns: 2, rows: 2, gapXMm: 0, gapYMm: 0, sheetWidthMm: 100, sheetHeightMm: 60, repeatToFill: true } }
    label.header.heightMm = 0
    label.footer.heightMm = 0
    label.body = [fixed('label', 20)]
    const tiled = layoutPrintDocument(label, { system: { generatedAt: '2026-09-18 12:00:00' } }, { measure, catalog: [] })
    expect(tiled.geometry.widthMm).toBe(100)
    expect(tiled.pages[0].fragments).toHaveLength(4)
    expect(tiled.pages[0].fragments[0].kind).toBe('TILE')
  })
  it('clips overflowing fixed text when textFit is CLIP', () => {
    const doc = paper()
    doc.body = [{
      id: 'head',
      kind: 'FIXED',
      heightMm: 20,
      elements: [{
        id: 'title',
        type: 'TEXT',
        xMm: 0,
        yMm: 0,
        widthMm: 80,
        heightMm: 5,
        binding: { source: 'CONSTANT', value: '一行\n两行\n三行' },
        style: { textFit: 'CLIP' },
      }],
    }]
    const result = layout(doc)
    expect(result.pages[0].fragments[0].elements[0].overflow).toBe('hidden')
    expect(result.pages[0].fragments[0].elements[0].heightMm).toBe(5)
  })
  it('keeps a consecutive heading chain with its first data block', () => {
    const doc = paper()
    doc.body = [fixed('before', 50), { ...fixed('heading1', 10), keepWithNext: true }, { ...fixed('heading2', 10), keepWithNext: true }, fixed('content', 20)]
    const result = layout(doc)
    expect(result.pages[1].fragments.map(fragment => fragment.id)).toEqual(['heading1', 'heading2', 'content'])
  })
  it('joins watermark text with selected field values', () => {
    const doc = paper()
    doc.watermark = { text: '内部资料', expression: 'main.name', fontSizePt: 18, color: '#64748b' }
    const result = layoutPrintDocument(doc, { system: { generatedAt: '2026-09-18 12:00:00' }, main: { name: '华能材料' } }, {
      measure,
      catalog: [{ path: 'main.name', type: 'TEXT' }],
    })
    expect(result.watermark).toMatchObject({
      text: '内部资料 · 华能材料',
      fontSizePt: 18,
      color: '#64748b',
    })
  })
})
