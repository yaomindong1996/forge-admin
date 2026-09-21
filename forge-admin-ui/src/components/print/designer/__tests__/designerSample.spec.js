import { describe, expect, it } from 'vitest'
import { isSafeImageReference } from '../../protocol/validate'
import { createDesignerSampleContext, designerBindingText, designerCellImageRef, designerTablePreview, designerTableRows, hasDesignerData, isDesignerImageCell, SAMPLE_IMAGE_DATA_URL } from '../designerSample'

const catalog = [
  { path: 'main.number', label: '采购单号', type: 'TEXT' },
  { path: 'main.total', label: '含税金额', type: 'MONEY' },
  { path: 'main.approved', label: '是否通过', type: 'BOOLEAN' },
  { path: 'main.photo', label: '附件图', type: 'IMAGE' },
  { path: 'main.tags', label: '标签', type: 'TEXT' },
  { path: 'children.items', label: '采购明细', type: 'COLLECTION' },
  { path: 'children.items.name', label: '物料名称', type: 'TEXT' },
  { path: 'children.items.amount', label: '金额', type: 'MONEY' },
]

describe('designer sample context', () => {
  it('creates stable labelled values without exposing binding paths', () => {
    const context = createDesignerSampleContext(catalog)
    expect(context.main.number).toBe('采购单号示例')
    expect(context.main.total).toBe(128800)
    expect(context.main.photo).toBe(SAMPLE_IMAGE_DATA_URL)
    expect(isSafeImageReference(SAMPLE_IMAGE_DATA_URL)).toBe(true)
    expect(context.children.items).toHaveLength(3)
    expect(context.children.items[0]).toMatchObject({ name: '物料名称1', amount: 128800 })
    expect(designerBindingText({ source: 'FIELD', path: 'main.number' }, undefined, catalog, context)).toBe('采购单号示例')
    expect(designerBindingText({ source: 'FIELD', path: 'main.total' }, { type: 'MONEY' }, catalog, context)).toBe('1288.00')
    expect(JSON.stringify(context)).not.toContain('main.number')
  })

  it('renders scalar arrays as joined text and guides object lists to detail tables', () => {
    const context = { main: { tags: ['急单', '加急'] }, children: { items: [{ name: 'A' }] } }
    expect(designerBindingText({ source: 'FIELD', path: 'main.tags' }, undefined, catalog, context)).toBe('急单、加急')
    expect(designerBindingText({ source: 'FIELD', path: 'children.items' }, undefined, catalog, context)).toContain('明细表')
  })

  it('detects image cells from contentType or IMAGE field bindings', () => {
    const context = createDesignerSampleContext(catalog)
    expect(isDesignerImageCell({ contentType: 'IMAGE', binding: { source: 'CONSTANT', value: 'f1' } }, catalog)).toBe(true)
    expect(isDesignerImageCell({ binding: { source: 'FIELD', path: 'main.photo' } }, catalog)).toBe(true)
    expect(designerCellImageRef({ binding: { source: 'FIELD', path: 'main.photo' } }, context)).toBe(SAMPLE_IMAGE_DATA_URL)
  })

  it('formats table example rows with the same column formats', () => {
    const context = createDesignerSampleContext(catalog)
    const section = {
      collectionPath: 'children.items',
      columns: [
        { field: 'name', widthMm: 40 },
        { field: 'amount', widthMm: 40, format: { type: 'MONEY' } },
      ],
    }
    expect(designerTableRows(section, catalog, context, 2)).toEqual([
      ['物料名称1', '1288.00'],
      ['物料名称2', '1524.00'],
    ])
    section.headerRows = [{ cells: [{ text: '采购明细', span: 2 }] }]
    section.footer = { cells: [{ binding: { source: 'CONSTANT', value: '合计' }, span: 1 }, { binding: { source: 'FIELD', path: 'main.total' }, span: 1, format: { type: 'MONEY' } }] }
    const preview = designerTablePreview(section, catalog, context, 1)
    expect(preview.map(row => row.kind)).toEqual(['header', 'data', 'footer'])
    expect(preview[0].cells[0]).toMatchObject({ text: '采购明细', widthMm: 80 })
    expect(preview[2].cells.map(cell => cell.text)).toEqual(['合计', '1288.00'])
  })

  it('applies odd and even row background styles in preview', () => {
    const context = createDesignerSampleContext(catalog)
    const section = {
      collectionPath: 'children.items',
      columns: [
        { id: 'c1', field: 'name', widthMm: 40 },
        { id: 'c2', field: 'amount', widthMm: 40, format: { type: 'MONEY' } },
      ],
      oddRowStyle: { backgroundColor: '#fff7ed' },
      evenRowStyle: { backgroundColor: '#eff6ff' },
    }
    const preview = designerTablePreview(section, catalog, context, 2)
    const dataRows = preview.filter(row => row.kind === 'data')
    expect(dataRows[0].cells.every(cell => cell.style.backgroundColor === '#fff7ed')).toBe(true)
    expect(dataRows[1].cells.every(cell => cell.style.backgroundColor === '#eff6ff')).toBe(true)
  })

  it('keeps header colors independent from body style', () => {
    const context = createDesignerSampleContext(catalog)
    const section = {
      collectionPath: 'children.items',
      headerStyle: { backgroundColor: '#112233', color: '#ffffff' },
      style: { backgroundColor: '#eeeeee', color: '#111111' },
      columns: [
        { id: 'c1', field: 'name', title: '名称', widthMm: 40 },
        { id: 'c2', field: 'amount', title: '金额', widthMm: 40, format: { type: 'MONEY' } },
      ],
    }
    const preview = designerTablePreview(section, catalog, context, 1)
    expect(preview[0].kind).toBe('header')
    expect(preview[0].cells.every(cell => cell.style.backgroundColor === '#112233' && cell.style.color === '#ffffff')).toBe(true)
    expect(preview[1].kind).toBe('data')
    expect(preview[1].cells.every(cell => cell.style.backgroundColor === '#eeeeee' && cell.style.color === '#111111')).toBe(true)
  })

  it('only replaces an actually empty design context', () => {
    expect(hasDesignerData({ main: {}, children: {}, flow: {} })).toBe(false)
    expect(hasDesignerData({ main: { number: 'PO-001' } })).toBe(true)
  })
})
