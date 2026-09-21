import { paperGeometry } from '../protocol/units'
import { newPrintId, normalizeTableColumnWidths } from './commands'
import { defaultFieldFormat } from './designerSample'

export { normalizeTableColumnWidths }

export function buildDetailTableColumns(doc, collection, fields) {
  const widthMm = paperGeometry(doc).contentWidthMm
  const columns = fields.map(field => ({
    id: newPrintId(),
    field: field.path.slice(collection.path.length + 1),
    title: field.label || field.path.split('.').at(-1),
    widthMm: 1,
    format: defaultFieldFormat(field),
  }))
  normalizeTableColumnWidths(columns, widthMm)
  return columns
}

export function createDataTableElement(doc, collection, fields, position = {}) {
  const contentWidth = paperGeometry(doc).contentWidthMm
  // Detail tables always span the full editable width (sv-print style).
  const widthMm = contentWidth
  const heightMm = Math.max(28, Number(position.heightMm) || 36)
  const columns = buildDetailTableColumns(doc, collection, fields)
  normalizeTableColumnWidths(columns, widthMm)
  return {
    id: newPrintId(),
    type: 'DATA_TABLE',
    xMm: 0,
    yMm: Math.max(0, Number(position.yMm) || 3),
    widthMm,
    heightMm,
    collectionPath: collection.path,
    repeatHeader: true,
    emptyText: '暂无明细',
    columns,
  }
}
