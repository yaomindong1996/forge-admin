import { describe, expect, it } from 'vitest'
import { createPrintDocument } from '../../protocol/types'
import { layoutPrintDocument } from '../layout'

function setup(count, footer = false) {
  const doc = createPrintDocument()
  doc.paper = { widthMm: 100, heightMm: 100, orientation: 'PORTRAIT', marginMm: { top: 10, right: 10, bottom: 10, left: 10 } }
  const table = { id: 'items', kind: 'TABLE', collectionPath: 'children.items', repeatHeader: true, columns: [{ id: 'name', field: 'name', title: '名称', widthMm: 80 }] }
  if (footer) {
    table.footer = { cells: [{ span: 1, binding: { source: 'CONSTANT', value: '合计' } }] }
  }
  doc.body = [table]
  const context = { children: { items: Array.from({ length: count }, (_, i) => ({ name: String(i) })) } }
  const options = { catalog: [{ path: 'children.items', type: 'COLLECTION' }, { path: 'children.items.name', type: 'TEXT' }], measure: { row: () => 10, text: value => ({ lines: [value], lineHeightMm: 5, insetMm: 0, heightMm: 5 }) } }
  return { doc, context, options }
}

describe('whole-row pagination', () => {
  it('repeats table headers and preserves every row once in order', () => {
    const { doc, context, options } = setup(14)
    const result = layoutPrintDocument(doc, context, options)
    expect(result.pages).toHaveLength(2)
    const rows = result.pages.flatMap(page => page.fragments.flatMap(fragment => fragment.rows))
    expect(rows.filter(row => row.kind === 'header')).toHaveLength(2)
    expect(rows.filter(row => row.kind === 'data').map(row => row.cells[0].text)).toEqual(context.children.items.map(row => row.name))
  })
  it('places a footer on its own continuation page when the last row exactly fills the body', () => {
    const { doc, context, options } = setup(7, true)
    const result = layoutPrintDocument(doc, context, options)
    expect(result.pages).toHaveLength(2)
    expect(result.pages[1].fragments[0].rows.map(row => row.kind)).toEqual(['header', 'footer'])
  })
  it('places a per-page subtotal and a last-page summary', () => {
    const { doc, context, options } = setup(12)
    doc.body[0].subtotal = { cells: [{ span: 1, binding: { source: 'EXPRESSION', expression: 'COUNT()' } }] }
    doc.body[0].footer = { cells: [{ span: 1, binding: { source: 'EXPRESSION', expression: 'SUM(1)' } }] }
    const result = layoutPrintDocument(doc, context, options)
    const dataPages = result.pages.filter(page => page.fragments[0].rows.some(row => row.kind === 'data'))
    expect(dataPages.every(page => page.fragments[0].rows.some(row => row.kind === 'subtotal'))).toBe(true)
    expect(result.pages.flatMap(page => page.fragments[0].rows.filter(row => row.kind === 'footer'))).toHaveLength(1)
  })
  it('renders explicit empty content and its footer once', () => {
    const { doc, context, options } = setup(0, true)
    const result = layoutPrintDocument(doc, context, options)
    expect(result.pages).toHaveLength(1)
    expect(result.pages[0].fragments[0].rows.map(row => row.kind)).toEqual(['header', 'empty', 'footer'])
  })
  it('rejects a row that cannot fit with its header without an empty-page loop', () => {
    const { doc, context, options } = setup(1)
    options.measure.row = cells => cells[0].text === '名称' ? 10 : 71
    expect(() => layoutPrintDocument(doc, context, options)).toThrow(expect.objectContaining({ code: 'ELEMENT_TOO_TALL' }))
  })
  it('accepts 500 rows within 50 pages and rejects a 501st row', () => {
    const { doc, context, options } = setup(500)
    options.measure.row = () => 5
    expect(layoutPrintDocument(doc, context, options).pages.length).toBeLessThanOrEqual(50)
    context.children.items.push({ name: '501' })
    expect(() => layoutPrintDocument(doc, context, options)).toThrow(expect.objectContaining({ code: 'ROW_LIMIT' }))
  })
  it('keeps prepared signatures as image cells instead of exposing file ids as text', () => {
    const { doc, context, options } = setup(1)
    doc.body[0].collectionPath = 'flow.history'
    doc.body[0].columns = [{ id: 'signature', field: 'signature', title: '签名', widthMm: 80 }]
    context.flow = { history: [{ signature: 'signature_1' }] }
    options.catalog = [{ path: 'flow.history', type: 'COLLECTION' }, { path: 'flow.history.signature', type: 'IMAGE' }]
    options.resources = { images: new Map([['table:items:0:signature', 'blob:signature']]) }
    const row = layoutPrintDocument(doc, context, options).pages[0].fragments[0].rows.find(item => item.kind === 'data')
    expect(row.cells[0]).toMatchObject({ type: 'IMAGE', text: '', src: 'blob:signature' })
    expect(row.cells[0].text).not.toContain('signature_1')
  })
})
