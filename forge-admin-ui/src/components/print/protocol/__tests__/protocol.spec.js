import { describe, expect, it } from 'vitest'
import { createPrintDocument } from '../types'
import { mmToPx, paperGeometry, pxToMm, screenDeltaToMm } from '../units'
import { assertPrintDocument, validatePrintDocument } from '../validate'

function textElement(overrides = {}) {
  return {
    id: 'title',
    type: 'TEXT',
    xMm: 0,
    yMm: 0,
    widthMm: 60,
    heightMm: 10,
    binding: { source: 'CONSTANT', value: '申请单' },
    ...overrides,
  }
}

function fixedDocument(element = textElement()) {
  const doc = createPrintDocument()
  doc.body.push({ id: 'head', kind: 'FIXED', heightMm: 20, elements: [element] })
  return doc
}

describe('print document protocol v1', () => {
  it('creates independent serializable defaults and preserves a template round trip', () => {
    const doc = fixedDocument()
    expect(assertPrintDocument(JSON.parse(JSON.stringify(doc)))).toEqual(doc)
    doc.paper.marginMm.top = 30
    expect(createPrintDocument().paper.marginMm.top).toBe(10)
  })

  it('rejects unknown protocols, versions, section kinds and element types', () => {
    for (const change of [
      doc => doc.protocol = 'hiprint',
      doc => doc.schemaVersion = 2,
      doc => doc.body[0].kind = 'HTML',
      doc => doc.body[0].elements[0].type = 'SCRIPT',
    ]) {
      const doc = fixedDocument()
      change(doc)
      expect(() => assertPrintDocument(doc)).toThrow()
    }
  })

  it('reports paths instead of silently dropping unsupported properties', () => {
    const doc = fixedDocument(textElement({ formatter: '() => alert(1)' }))
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({
      path: 'body[0].elements[0].formatter',
      code: 'UNKNOWN_PROPERTY',
    }))
  })

  it('checks finite dimensions, margins and the printable body', () => {
    for (const width of [0, -1, Number.NaN, Number.POSITIVE_INFINITY, '210']) {
      const doc = createPrintDocument()
      doc.paper.widthMm = width
      expect(validatePrintDocument(doc).length).toBeGreaterThan(0)
    }
    const doc = createPrintDocument()
    doc.header.heightMm = 280
    expect(() => assertPrintDocument(doc)).toThrow()
  })

  it('rejects elements outside their fixed section and duplicate IDs', () => {
    const doc = fixedDocument(textElement({ yMm: 15, heightMm: 10 }))
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({ code: 'OUT_OF_BOUNDS' }))
    const duplicate = fixedDocument()
    duplicate.body[0].elements.push(textElement())
    expect(validatePrintDocument(duplicate)).toContainEqual(expect.objectContaining({ code: 'DUPLICATE_ID' }))
  })

  it('allows primitive constants including zero and false, but rejects executable styles', () => {
    expect(validatePrintDocument(fixedDocument(textElement({ binding: { source: 'CONSTANT', value: 0 } })))).toEqual([])
    expect(validatePrintDocument(fixedDocument(textElement({ binding: { source: 'CONSTANT', value: false } })))).toEqual([])
    const doc = fixedDocument(textElement({ style: { backgroundImage: 'url(https://example.invalid/a)' } }))
    expect(() => assertPrintDocument(doc)).toThrow()
  })

  it('accepts transparent and picker colors, but still rejects named CSS colors', () => {
    expect(validatePrintDocument(fixedDocument(textElement({ style: { backgroundColor: 'transparent' } })))).toEqual([])
    expect(validatePrintDocument(fixedDocument(textElement({ style: { color: '#11223344' } })))).toEqual([])
    expect(validatePrintDocument(fixedDocument(textElement({ style: { backgroundColor: 'rgba(17, 34, 51, 1)' } })))).toEqual([])
    expect(validatePrintDocument(fixedDocument(textElement({ style: { backgroundColor: 'red' } })))).toContainEqual(expect.objectContaining({
      path: 'body[0].elements[0].style.backgroundColor',
      code: 'INVALID_COLOR',
    }))
  })

  it('accepts ellipse as a native non-executable shape', () => {
    const doc = fixedDocument(textElement({ type: 'ELLIPSE' }))
    delete doc.body[0].elements[0].binding
    expect(validatePrintDocument(doc)).toEqual([])
  })

  it('validates native transforms, locking and bounded visual styles', () => {
    const doc = fixedDocument(textElement({
      rotationDeg: -90,
      flipX: true,
      flipY: false,
      locked: true,
      style: { borderStyle: 'dashed', borderRadiusMm: 2, objectFit: 'cover' },
    }))
    expect(validatePrintDocument(doc)).toEqual([])
    doc.body[0].elements[0].rotationDeg = 181
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({ path: 'body[0].elements[0].rotationDeg' }))
    doc.body[0].elements[0].rotationDeg = 0
    doc.body[0].elements[0].locked = 'true'
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({ path: 'body[0].elements[0].locked' }))
    doc.body[0].elements[0].locked = true
    doc.body[0].elements[0].style.objectFit = 'none'
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({ path: 'body[0].elements[0].style.objectFit' }))
  })

  it('accepts expression bindings, overflow modes and paper tiling', () => {
    const doc = fixedDocument(textElement({
      binding: { source: 'EXPRESSION', expression: 'MONEY(12 * 10)' },
      format: { type: 'MONEY_UPPER' },
      style: { textFit: 'SHRINK', shrinkMinFontSizePt: 6 },
    }))
    doc.paper.kind = 'CONTINUOUS'
    doc.paper.tiling = { enabled: true, columns: 2, rows: 2, gapXMm: 2, gapYMm: 2, sheetWidthMm: 210, sheetHeightMm: 297, repeatToFill: true }
    doc.paper.designBackground = { fileId: 'overlay_1', opacity: 1, rotationDeg: 0, print: false }
    doc.watermark = { text: '内部资料', opacity: 0.1, rotateDeg: -20 }
    doc.exportFileName = '{template}-{{main.name}}-{timestamp}'
    expect(validatePrintDocument(doc)).toEqual([])
    doc.body[0].elements[0].style.opacity = 0.4
    expect(validatePrintDocument(doc)).toEqual([])
    doc.body[0].elements[0].style.letterSpacing = 0.2
    expect(validatePrintDocument(doc)).toEqual([])
    doc.body[0].elements[0].style.opacity = 1.5
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({ path: 'body[0].elements[0].style.opacity', code: 'INVALID_NUMBER' }))
    doc.body[0].elements[0].style.opacity = 0.4
    doc.body[0].elements[0].binding = { source: 'EXPRESSION', expression: 'eval(1)' }
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({ code: 'INVALID_EXPRESSION' }))
    doc.body[0].elements[0].binding = { source: 'EXPRESSION', expression: 'MONEY(12 * 10)' }
    doc.exportFileName = '../x'
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({ path: 'exportFileName', code: 'INVALID_TEXT' }))
  })

  it('accepts a native blank table and rejects overlaps or dimension drift', () => {
    const table = {
      columns: [{ id: 'col-a', widthMm: 30 }, { id: 'col-b', widthMm: 30 }],
      rows: [{ id: 'row-a', heightMm: 10 }],
      cells: [
        { id: 'cell-a', row: 0, column: 0, rowSpan: 1, colSpan: 1, binding: { source: 'CONSTANT', value: '甲' } },
        { id: 'cell-b', row: 0, column: 1, rowSpan: 1, colSpan: 1, binding: { source: 'FIELD', path: 'main.name' } },
      ],
    }
    const doc = fixedDocument(textElement({ type: 'STATIC_TABLE', widthMm: 60, table }))
    delete doc.body[0].elements[0].binding
    expect(validatePrintDocument(doc)).toEqual([])
    doc.body[0].elements[0].table.cells[1].column = 0
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({ code: 'INVALID_COVERAGE' }))
    doc.body[0].elements[0].table.cells[1].column = 1
    doc.body[0].elements[0].widthMm = 61
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({ code: 'TABLE_SIZE_MISMATCH' }))
  })

  it('checks table column widths, merged header spans and safe collection paths', () => {
    const doc = createPrintDocument()
    doc.body.push({
      id: 'items',
      kind: 'TABLE',
      collectionPath: 'children.items',
      repeatHeader: true,
      columns: [{ id: 'name', field: 'name', title: '名称', widthMm: 100 }],
      headerRows: [{ cells: [{ text: '明细', span: 1 }] }],
    })
    expect(validatePrintDocument(doc)).toEqual([])
    doc.body[0].headerRows[0].cells[0].span = 2
    expect(() => assertPrintDocument(doc)).toThrow()
    doc.body[0].headerRows[0].cells[0].span = 1
    doc.body[0].collectionPath = 'children.__proto__.items'
    expect(() => assertPrintDocument(doc)).toThrow()
  })

  it('accepts page breaks only between content sections without payload', () => {
    const doc = createPrintDocument()
    doc.body = [
      { id: 'before', kind: 'FIXED', heightMm: 20, elements: [] },
      { id: 'break', kind: 'PAGE_BREAK' },
      { id: 'after', kind: 'FIXED', heightMm: 20, elements: [] },
    ]
    expect(validatePrintDocument(doc)).toEqual([])
    doc.body[1].gapAfterMm = 2
    expect(validatePrintDocument(doc)).toContainEqual(expect.objectContaining({ path: 'body[1].gapAfterMm', code: 'UNKNOWN_PROPERTY' }))
    for (const body of [
      [{ id: 'leading', kind: 'PAGE_BREAK' }, { id: 'content', kind: 'FIXED', heightMm: 20, elements: [] }],
      [{ id: 'content', kind: 'FIXED', heightMm: 20, elements: [] }, { id: 'trailing', kind: 'PAGE_BREAK' }],
      [{ id: 'before', kind: 'FIXED', heightMm: 20, elements: [] }, { id: 'first-break', kind: 'PAGE_BREAK' }, { id: 'second-break', kind: 'PAGE_BREAK' }, { id: 'after', kind: 'FIXED', heightMm: 20, elements: [] }],
    ]) {
      const invalid = createPrintDocument()
      invalid.body = body
      expect(validatePrintDocument(invalid)).toContainEqual(expect.objectContaining({ code: 'INVALID_PAGE_BREAK' }))
    }
  })

  it('rejects prototype keys, cycles, excessive collections and foreign image URLs', () => {
    const poisoned = JSON.parse(JSON.stringify(createPrintDocument()).replace('"resources":[]', '"resources":[],"__proto__":{}'))
    expect(() => assertPrintDocument(poisoned)).toThrow()
    const circular = createPrintDocument()
    circular.body.push(circular)
    expect(() => assertPrintDocument(circular)).toThrow()
    const doc = fixedDocument(textElement({ type: 'IMAGE', binding: { source: 'CONSTANT', value: 'https://example.invalid/private.png' } }))
    expect(() => assertPrintDocument(doc)).toThrow()
  })
})

describe('physical paper coordinates', () => {
  it('round trips millimetres without rounding physical coordinates', () => {
    expect(mmToPx(25.4)).toBeCloseTo(96, 10)
    expect(pxToMm(mmToPx(210.125))).toBeCloseTo(210.125, 8)
  })

  it('converts screen movement by zoom without changing paper geometry', () => {
    for (const zoom of [0.5, 1, 1.5]) {
      expect(screenDeltaToMm(mmToPx(10) * zoom, zoom)).toBeCloseTo(10, 8)
    }
    expect(() => screenDeltaToMm(10, 0)).toThrow()
    const doc = createPrintDocument()
    doc.paper.orientation = 'LANDSCAPE'
    expect(paperGeometry(doc)).toMatchObject({ widthMm: 297, heightMm: 210, contentWidthMm: 277 })
  })
})
