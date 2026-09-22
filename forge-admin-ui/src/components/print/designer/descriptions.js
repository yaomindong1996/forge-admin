import { createPrintDocument } from '../protocol/types'
import { paperGeometry } from '../protocol/units'
import { newPrintId } from './commands'

export const DESCRIPTION_ITEM_LIMIT = 40

export function descriptionFields(catalog = []) {
  const collections = catalog.filter(field => field.type === 'COLLECTION').map(field => field.path)
  return catalog
    .filter(field => field.type !== 'COLLECTION' && !collections.some(path => field.path.startsWith(`${path}.`)))
    .slice(0, DESCRIPTION_ITEM_LIMIT)
}

export function descriptionItemsFromCatalog(catalog = []) {
  const fields = descriptionFields(catalog)
  if (!fields.length) {
    return [{
      id: newPrintId(),
      label: '字段',
      span: 1,
      binding: { source: 'CONSTANT', value: '示例内容' },
    }]
  }
  return fields.map(field => ({
    id: newPrintId(),
    label: String(field.label || field.path.split('.').at(-1) || '字段').slice(0, 40),
    span: 1,
    binding: { source: 'FIELD', path: field.path },
  }))
}

export function descriptionBlockHeightMm(descriptions = {}) {
  const column = Math.max(1, Math.min(4, Number(descriptions.column) || 2))
  const spanSum = (descriptions.items || []).reduce((sum, item) => sum + Math.max(1, Math.min(column, Number(item.span) || 1)), 0)
  const rows = (descriptions.items || []).length ? Math.max(1, Math.ceil(spanSum / column)) : 0
  const rowMm = descriptions.size === 'small' ? 7 : descriptions.size === 'large' ? 10 : 8
  const titleMm = descriptions.title ? Math.max(8, Number(descriptions.titleFontSizePt) > 16 ? 12 : 8) : 0
  return Number((titleMm + rows * rowMm + (rows ? 2 : 1)).toFixed(3))
}

export function createDescriptionsConfig(catalog = [], overrides = {}) {
  return {
    bordered: true,
    column: 2,
    labelAlign: 'left',
    labelPlacement: 'left',
    labelBackground: '#d9e3f0',
    separator: '：',
    size: 'medium',
    title: '详情',
    titleFontSizePt: 12,
    titleColor: '#111827',
    titleAlign: 'left',
    titleBold: true,
    items: descriptionItemsFromCatalog(catalog),
    ...overrides,
  }
}

/** A one-page template whose canvas is a detail block filled from the form catalog. */
export function createFormDetailDocument(catalog = []) {
  const doc = createPrintDocument()
  const descriptions = createDescriptionsConfig(catalog)
  const heightMm = descriptionBlockHeightMm(descriptions)
  const widthMm = paperGeometry(doc).contentWidthMm
  doc.body = [{
    id: newPrintId(),
    kind: 'FIXED',
    heightMm: Math.max(40, Number((heightMm + 4).toFixed(3))),
    gapAfterMm: 2,
    elements: [{
      id: newPrintId(),
      type: 'DESCRIPTIONS',
      xMm: 0,
      yMm: 0,
      widthMm,
      heightMm,
      descriptions,
    }],
  }]
  return doc
}
