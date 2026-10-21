import { describe, expect, it } from 'vitest'
import { adaptMarketPrintDocument, resolveCatalogPath } from '../adaptMarketPrintDocument'

const catalog = [
  { path: 'main.billNo', label: '单号', type: 'TEXT' },
  { path: 'main.applicant', label: '申请人', type: 'TEXT' },
  { path: 'main.reason', label: '事由', type: 'TEXT' },
  { path: 'children.lines', label: '明细', type: 'COLLECTION' },
  { path: 'children.lines.itemName', label: '名称', type: 'TEXT' },
  { path: 'children.lines.name', label: '品名', type: 'TEXT' },
  { path: 'flow.status', label: '审批状态', type: 'TEXT' },
]

describe('resolveCatalogPath', () => {
  it('keeps exact path', () => {
    expect(resolveCatalogPath(catalog, 'main.applicant')).toBe('main.applicant')
  })

  it('maps by leaf name across scopes', () => {
    expect(resolveCatalogPath(catalog, 'biz.applicant')).toBe('main.applicant')
    expect(resolveCatalogPath(catalog, 'main.docNo')).toBeNull()
  })

  it('falls back to the only collection when adapting market tables', () => {
    expect(resolveCatalogPath(catalog, 'children.items', { collection: true })).toBe('children.lines')
    expect(resolveCatalogPath(catalog, 'children.lines', { collection: true })).toBe('children.lines')
  })
})

describe('adaptMarketPrintDocument', () => {
  it('rebinds matching leaves and clears unmatched field paths', () => {
    const { document, remapped, cleared } = adaptMarketPrintDocument({
      header: { elements: [] },
      footer: { elements: [] },
      body: [{
        id: 'sec',
        kind: 'FIXED',
        elements: [
          { id: 'a', type: 'TEXT', binding: { source: 'FIELD', path: 'main.docNo' } },
          { id: 'b', type: 'TEXT', binding: { source: 'FIELD', path: 'biz.applicant' } },
          { id: 'c', type: 'TEXT', binding: { source: 'CONSTANT', value: '固定文案' } },
        ],
      }],
    }, catalog)

    expect(document.body[0].elements[0].binding).toEqual({ source: 'CONSTANT', value: '' })
    expect(document.body[0].elements[1].binding).toEqual({ source: 'FIELD', path: 'main.applicant' })
    expect(document.body[0].elements[2].binding).toEqual({ source: 'CONSTANT', value: '固定文案' })
    expect(remapped).toBe(1)
    expect(cleared).toBe(1)
  })

  it('rebinds table collection and relative columns by leaf', () => {
    const { document } = adaptMarketPrintDocument({
      header: { elements: [] },
      footer: { elements: [] },
      body: [{
        id: 'table',
        kind: 'TABLE',
        collectionPath: 'children.items',
        columns: [
          { id: 'c1', field: 'name', title: '名称' },
          { id: 'c2', field: 'qty', title: '数量' },
        ],
        elements: [],
      }],
    }, catalog)

    expect(document.body[0].collectionPath).toBe('children.lines')
    expect(document.body[0].columns[0].field).toBe('name')
    expect(document.body[0].columns[1].field).toBe('')
  })
})
