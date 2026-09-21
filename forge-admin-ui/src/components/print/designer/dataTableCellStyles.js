/** Key for DATA_TABLE per-cell style overrides: kind:rowIndexInKind:columnId */
export function dataTableCellStyleKey(kind, rowIndexInKind, columnId) {
  return `${kind}:${rowIndexInKind}:${columnId}`
}

export function mergeDataTableCellStyle(base = {}, cellStyles = {}, kind, rowIndexInKind, columnId) {
  const extra = cellStyles?.[dataTableCellStyleKey(kind, rowIndexInKind, columnId)]
  return extra ? { ...base, ...extra } : { ...base }
}

/**
 * Walk preview rows and yield { kind, kindIndex, previewRowIndex, columnIndex, columnId }
 * for every cell inside the inclusive selection range.
 */
export function iterDataTableSelectionCells(previewRows, columns, range) {
  if (!range || !previewRows?.length || !columns?.length)
    return []
  const top = Math.max(0, Math.min(range.top, range.bottom))
  const bottom = Math.min(previewRows.length - 1, Math.max(range.top, range.bottom))
  const left = Math.max(0, Math.min(range.left, range.right))
  const right = Math.min(columns.length - 1, Math.max(range.left, range.right))
  const kindCounters = { header: 0, data: 0, footer: 0, subtotal: 0 }
  const hits = []
  for (let row = 0; row < previewRows.length; row += 1) {
    const kind = previewRows[row]?.kind || 'data'
    const kindIndex = kindCounters[kind] ?? 0
    kindCounters[kind] = kindIndex + 1
    if (row < top || row > bottom)
      continue
    for (let col = left; col <= right; col += 1) {
      const columnId = columns[col]?.id
      if (!columnId)
        continue
      hits.push({ kind, kindIndex, previewRowIndex: row, columnIndex: col, columnId })
    }
  }
  return hits
}
