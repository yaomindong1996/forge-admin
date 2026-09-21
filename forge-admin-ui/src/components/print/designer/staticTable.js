function nestedId() {
  return `p_${globalThis.crypto.randomUUID()}`
}

function blankCell(row, column, createId = nestedId) {
  return {
    id: createId(),
    row,
    column,
    rowSpan: 1,
    colSpan: 1,
    binding: { source: 'CONSTANT', value: '' },
  }
}

export function createStaticTable(columnCount = 3, rowCount = 3, widthMm = 75, rowHeightMm = 9, createId = nestedId, { headerRow = false } = {}) {
  const columnWidth = Number((widthMm / columnCount).toFixed(3))
  const columns = Array.from({ length: columnCount }, () => ({ id: createId(), widthMm: columnWidth }))
  columns.at(-1).widthMm = Number((widthMm - columns.slice(0, -1).reduce((sum, column) => sum + column.widthMm, 0)).toFixed(3))
  const rows = Array.from({ length: rowCount }, () => ({ id: createId(), heightMm: rowHeightMm }))
  const cells = rows.flatMap((_, row) => columns.map((__, column) => blankCell(row, column, createId)))
  if (headerRow && rowCount > 0) {
    cells.filter(cell => cell.row === 0).forEach((cell) => {
      cell.style = { fontWeight: 700, textAlign: 'center', backgroundColor: '#f1f5f9' }
      cell.binding = { source: 'CONSTANT', value: '表头' }
    })
  }
  return { columns, rows, cells }
}

const DEFAULT_TABLE_FILLS = new Set(['#fff', '#ffffff', '#f1f5f9'])

export function isDefaultTableFill(color) {
  return typeof color === 'string' && DEFAULT_TABLE_FILLS.has(color.trim().toLowerCase())
}

/**
 * First row uses headerStyle; cell.style still wins except leftover default fills
 * (#fff / #f1f5f9) which otherwise hide 样式 → 表头背景.
 */
export function staticTableCellLook(element, cell) {
  const cellStyle = cell?.style ? { ...cell.style } : {}
  const band = cell.row === 0 ? element?.headerStyle : null
  if (band?.backgroundColor && isDefaultTableFill(cellStyle.backgroundColor))
    delete cellStyle.backgroundColor
  return {
    borderWidthMm: 0.15,
    ...element?.style,
    ...band,
    ...cellStyle,
  }
}

/** Drop matching keys from a band so table-level 表头/表体 styles paint. */
export function clearStaticTableBandCellStyles(table, band, keys = []) {
  if (!table?.cells?.length || !keys.length)
    return
  table.cells.forEach((cell) => {
    const header = cell.row === 0
    if ((band === 'header') !== header || !cell.style)
      return
    keys.forEach(key => delete cell.style[key])
    if (!Object.keys(cell.style).length)
      delete cell.style
  })
}

export function staticTableSize(table) {
  return {
    widthMm: Number(table.columns.reduce((sum, column) => sum + column.widthMm, 0).toFixed(3)),
    heightMm: Number(table.rows.reduce((sum, row) => sum + row.heightMm, 0).toFixed(3)),
  }
}

export function renewStaticTableIds(element, createId = nestedId) {
  if (element.type !== 'STATIC_TABLE' || !element.table)
    return element
  element.table.columns.forEach(column => column.id = createId())
  element.table.rows.forEach(row => row.id = createId())
  element.table.cells.forEach(cell => cell.id = createId())
  return element
}

/** Clone-safe id renewal for paste/duplicate (static + detail tables). */
export function renewPrintElementIds(element, createId = nestedId) {
  const next = { ...element, id: createId(), locked: false }
  renewStaticTableIds(next, createId)
  if (next.type === 'DATA_TABLE' && Array.isArray(next.columns)) {
    next.columns = next.columns.map(column => ({ ...column, id: createId() }))
  }
  return next
}

export function appendStaticTableRow(table, createId = nestedId) {
  return insertStaticTableRow(table, table.rows.length, createId)
}

export function insertStaticTableRow(table, index, createId = nestedId) {
  const at = Math.max(0, Math.min(table.rows.length, Number(index) || 0))
  const average = table.rows.reduce((sum, item) => sum + item.heightMm, 0) / table.rows.length || 9
  table.rows.splice(at, 0, { id: createId(), heightMm: Number(average.toFixed(3)) })
  table.cells.forEach((cell) => {
    if (cell.row >= at)
      cell.row++
  })
  const cells = table.columns.map((_, column) => blankCell(at, column, createId))
  table.cells.push(...cells)
  return cells.map(cell => cell.id)
}

export function insertStaticTableRows(table, index, count = 1, createId = nestedId) {
  const n = Math.max(1, Math.floor(Number(count) || 1))
  const at = Math.max(0, Math.min(table.rows.length, Number(index) || 0))
  let created = []
  for (let i = 0; i < n; i++)
    created = created.concat(insertStaticTableRow(table, at + i, createId))
  return created
}

export function appendStaticTableColumn(table, createId = nestedId) {
  return insertStaticTableColumn(table, table.columns.length, createId)
}

export function insertStaticTableColumn(table, index, createId = nestedId) {
  const at = Math.max(0, Math.min(table.columns.length, Number(index) || 0))
  const average = table.columns.reduce((sum, item) => sum + item.widthMm, 0) / table.columns.length || 20
  table.columns.splice(at, 0, { id: createId(), widthMm: Number(average.toFixed(3)) })
  table.cells.forEach((cell) => {
    if (cell.column >= at)
      cell.column++
  })
  const cells = table.rows.map((_, row) => blankCell(row, at, createId))
  table.cells.push(...cells)
  return cells.map(cell => cell.id)
}

export function insertStaticTableColumns(table, index, count = 1, createId = nestedId) {
  const n = Math.max(1, Math.floor(Number(count) || 1))
  const at = Math.max(0, Math.min(table.columns.length, Number(index) || 0))
  let created = []
  for (let i = 0; i < n; i++)
    created = created.concat(insertStaticTableColumn(table, at + i, createId))
  return created
}

function intersectsRow(cell, row) {
  return cell.row <= row && row < cell.row + cell.rowSpan
}

function intersectsColumn(cell, column) {
  return cell.column <= column && column < cell.column + cell.colSpan
}

export function deleteStaticTableRow(table, row) {
  if (table.rows.length <= 1 || row < 0 || row >= table.rows.length)
    return false
  table.rows.splice(row, 1)
  table.cells = table.cells.flatMap((cell) => {
    if (!intersectsRow(cell, row)) {
      if (cell.row > row)
        cell.row--
      return [cell]
    }
    if (cell.rowSpan === 1)
      return []
    cell.rowSpan--
    if (cell.row === row && row >= table.rows.length)
      cell.row--
    return [cell]
  })
  return true
}

/** Delete many rows (highest index first). Keeps at least one row. */
export function deleteStaticTableRows(table, rows = []) {
  const unique = [...new Set(rows.filter(row => Number.isInteger(row) && row >= 0 && row < table.rows.length))]
    .sort((a, b) => b - a)
  if (!unique.length)
    return false
  if (unique.length >= table.rows.length)
    unique.splice(-1, 1) // keep one row
  let changed = false
  for (const row of unique) {
    if (deleteStaticTableRow(table, row))
      changed = true
  }
  return changed
}

export function deleteStaticTableColumn(table, column) {
  if (table.columns.length <= 1 || column < 0 || column >= table.columns.length)
    return false
  table.columns.splice(column, 1)
  table.cells = table.cells.flatMap((cell) => {
    if (!intersectsColumn(cell, column)) {
      if (cell.column > column)
        cell.column--
      return [cell]
    }
    if (cell.colSpan === 1)
      return []
    cell.colSpan--
    if (cell.column === column && column >= table.columns.length)
      cell.column--
    return [cell]
  })
  return true
}

/** Delete many columns (highest index first). Keeps at least one column. */
export function deleteStaticTableColumns(table, columns = []) {
  const unique = [...new Set(columns.filter(column => Number.isInteger(column) && column >= 0 && column < table.columns.length))]
    .sort((a, b) => b - a)
  if (!unique.length)
    return false
  if (unique.length >= table.columns.length)
    unique.splice(-1, 1)
  let changed = false
  for (const column of unique) {
    if (deleteStaticTableColumn(table, column))
      changed = true
  }
  return changed
}

export function mergeStaticTableCells(table, selectedIds, createId = nestedId) {
  const selected = table.cells.filter(cell => selectedIds.includes(cell.id))
  if (selected.length < 2)
    return null
  const top = Math.min(...selected.map(cell => cell.row))
  const left = Math.min(...selected.map(cell => cell.column))
  const bottom = Math.max(...selected.map(cell => cell.row + cell.rowSpan))
  const right = Math.max(...selected.map(cell => cell.column + cell.colSpan))
  const area = (bottom - top) * (right - left)
  const covered = selected.reduce((sum, cell) => sum + cell.rowSpan * cell.colSpan, 0)
  const inside = selected.every(cell => cell.row >= top && cell.column >= left && cell.row + cell.rowSpan <= bottom && cell.column + cell.colSpan <= right)
  if (!inside || covered !== area)
    return null
  const first = selected.slice().sort((a, b) => a.row - b.row || a.column - b.column)[0]
  const merged = {
    ...first,
    id: createId(),
    row: top,
    column: left,
    rowSpan: bottom - top,
    colSpan: right - left,
  }
  table.cells = [...table.cells.filter(cell => !selectedIds.includes(cell.id)), merged]
  return merged.id
}

export function splitStaticTableCell(table, cellId, createId = nestedId) {
  const index = table.cells.findIndex(cell => cell.id === cellId)
  const cell = table.cells[index]
  if (!cell || (cell.rowSpan === 1 && cell.colSpan === 1))
    return []
  const cells = []
  for (let row = cell.row; row < cell.row + cell.rowSpan; row++) {
    for (let column = cell.column; column < cell.column + cell.colSpan; column++) {
      cells.push({
        ...blankCell(row, column, createId),
        ...(row === cell.row && column === cell.column
          ? { binding: cell.binding, format: cell.format, style: cell.style, contentType: cell.contentType }
          : {}),
      })
    }
  }
  table.cells.splice(index, 1, ...cells)
  return cells.map(item => item.id)
}

export function staticTableCellAt(table, row, column) {
  return table.cells.find(cell => cell.row <= row && row < cell.row + cell.rowSpan && cell.column <= column && column < cell.column + cell.colSpan)
}

/** Word-like rectangular selection: all cells intersecting [r0..r1] × [c0..c1]. */
export function staticTableIdsInRect(table, rowA, columnA, rowB, columnB) {
  const top = Math.min(rowA, rowB)
  const bottom = Math.max(rowA, rowB)
  const left = Math.min(columnA, columnB)
  const right = Math.max(columnA, columnB)
  return table.cells
    .filter(cell => !(cell.row + cell.rowSpan - 1 < top || cell.row > bottom || cell.column + cell.colSpan - 1 < left || cell.column > right))
    .map(cell => cell.id)
}

/** Apply common header look to every cell that intersects the given rows. */
export function styleStaticTableRowsAsHeader(table, rowIndexes = [0]) {
  const rows = new Set(rowIndexes)
  table.cells.forEach((cell) => {
    if (![...rows].some(row => cell.row <= row && row < cell.row + cell.rowSpan))
      return
    cell.style = {
      ...cell.style,
      fontWeight: 700,
      textAlign: 'center',
      backgroundColor: cell.style?.backgroundColor && cell.style.backgroundColor !== '#ffffff'
        ? cell.style.backgroundColor
        : '#f1f5f9',
    }
  })
}
