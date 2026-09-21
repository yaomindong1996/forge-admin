/** Shared table selection helpers (static + data table designers). */

export function selectionBoundsFromCells(cells = []) {
  if (!cells.length)
    return null
  return {
    top: Math.min(...cells.map(cell => cell.row)),
    left: Math.min(...cells.map(cell => cell.column)),
    bottom: Math.max(...cells.map(cell => cell.row + (cell.rowSpan || 1) - 1)),
    right: Math.max(...cells.map(cell => cell.column + (cell.colSpan || 1) - 1)),
  }
}

function trackSpanMm(tracks, start, end, key) {
  return tracks.slice(start, end + 1).reduce((sum, item) => sum + Number(item?.[key] || 0), 0)
}

/** Paper-mm box of selected STATIC_TABLE cells, relative to the canvas origin. */
export function staticTableCellPaperBounds(element, cells = []) {
  const box = selectionBoundsFromCells(cells)
  const columns = element?.table?.columns
  const rows = element?.table?.rows
  if (!box || !columns?.length || !rows?.length)
    return null
  return {
    xMm: Number(element.xMm || 0) + trackSpanMm(columns, 0, box.left - 1, 'widthMm'),
    yMm: Number(element.yMm || 0) + trackSpanMm(rows, 0, box.top - 1, 'heightMm'),
    widthMm: trackSpanMm(columns, box.left, box.right, 'widthMm'),
    heightMm: trackSpanMm(rows, box.top, box.bottom, 'heightMm'),
  }
}

/** Paper-mm box of a DATA_TABLE selection range. */
export function dataTableRangePaperBounds(element, range) {
  const columns = element?.columns
  if (!range || !columns?.length)
    return null
  const rowCount = Math.max(1, Number(range.rowCount) || range.bottom + 1)
  const rowH = Number(element.heightMm || 0) / rowCount
  return {
    xMm: Number(element.xMm || 0) + trackSpanMm(columns, 0, range.left - 1, 'widthMm'),
    yMm: Number(element.yMm || 0) + range.top * rowH,
    widthMm: trackSpanMm(columns, range.left, range.right, 'widthMm'),
    heightMm: (range.bottom - range.top + 1) * rowH,
  }
}

/** Which outer edges of a cell sit on the selection rectangle (Excel-style). */
export function selectionEdgeFlags(cell, bounds) {
  if (!bounds || !cell)
    return { top: false, right: false, bottom: false, left: false }
  const top = cell.row
  const left = cell.column
  const bottom = cell.row + (cell.rowSpan || 1) - 1
  const right = cell.column + (cell.colSpan || 1) - 1
  return {
    top: top === bounds.top,
    right: right === bounds.right,
    bottom: bottom === bounds.bottom,
    left: left === bounds.left,
  }
}

export function selectionEdgeClass(cell, bounds) {
  const edges = selectionEdgeFlags(cell, bounds)
  return {
    'selected': true,
    'edge-t': edges.top,
    'edge-r': edges.right,
    'edge-b': edges.bottom,
    'edge-l': edges.left,
  }
}

/** Map a pointer position inside a CSS grid table to row/col indices. */
export function hitGridCell(root, clientX, clientY, rowCount, colCount) {
  if (!root || rowCount < 1 || colCount < 1)
    return null
  const rect = root.getBoundingClientRect()
  if (rect.width <= 0 || rect.height <= 0)
    return null
  const x = Math.min(rect.width - 0.001, Math.max(0, clientX - rect.left))
  const y = Math.min(rect.height - 0.001, Math.max(0, clientY - rect.top))
  const col = Math.min(colCount - 1, Math.floor((x / rect.width) * colCount))
  const row = Math.min(rowCount - 1, Math.floor((y / rect.height) * rowCount))
  return { row, column: col }
}
