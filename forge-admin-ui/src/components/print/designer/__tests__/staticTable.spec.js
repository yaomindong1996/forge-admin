import { describe, expect, it } from 'vitest'
import { resizeHandlesForElement } from '../commands'
import { appendStaticTableColumn, appendStaticTableRow, createStaticTable, deleteStaticTableColumn, deleteStaticTableColumns, deleteStaticTableRow, deleteStaticTableRows, insertStaticTableColumn, insertStaticTableColumns, insertStaticTableRow, insertStaticTableRows, mergeStaticTableCells, splitStaticTableCell, staticTableCellAt, staticTableCellLook, staticTableIdsInRect, staticTableSize } from '../staticTable'
import { staticTableCellPaperBounds } from '../tableSelection'

function ids() {
  let value = 0
  return () => `t_${++value}`
}

describe('static table editing', () => {
  it('creates a fully covered physical grid and appends rows and columns', () => {
    const table = createStaticTable(3, 2, 75, 8, ids())
    expect(staticTableSize(table)).toEqual({ widthMm: 75, heightMm: 16 })
    expect(table.cells).toHaveLength(6)
    expect(appendStaticTableRow(table, ids())).toHaveLength(3)
    expect(appendStaticTableColumn(table, ids())).toHaveLength(3)
    expect(staticTableSize(table)).toEqual({ widthMm: 100, heightMm: 24 })
    expect(table.cells.filter(cell => cell.row === 0).every(cell => !cell.style && cell.binding?.value === '')).toBe(true)
  })

  it('applies table headerStyle only to the first row', () => {
    const table = createStaticTable(2, 2, 40, 8, ids())
    const element = { style: { color: '#111111' }, headerStyle: { backgroundColor: '#ddeeff', color: '#aa0000' }, table }
    expect(staticTableCellLook(element, table.cells[0])).toMatchObject({ backgroundColor: '#ddeeff', color: '#aa0000' })
    expect(staticTableCellLook(element, table.cells[2])).toMatchObject({ color: '#111111' })
    expect(staticTableCellLook(element, table.cells[2]).backgroundColor).toBeUndefined()
  })

  it('lets headerStyle background replace leftover default cell fills', () => {
    const table = createStaticTable(2, 2, 40, 8, ids())
    table.cells[0].style = { backgroundColor: '#ffffff', fontWeight: 700 }
    const element = { headerStyle: { backgroundColor: '#ffcc00', color: '#aa0000' }, table }
    expect(staticTableCellLook(element, table.cells[0])).toMatchObject({ backgroundColor: '#ffcc00', color: '#aa0000', fontWeight: 700 })
    table.cells[0].style.backgroundColor = '#112233'
    expect(staticTableCellLook(element, table.cells[0]).backgroundColor).toBe('#112233')
  })

  it('merges and splits a rectangular selection without leaving coverage holes', () => {
    const table = createStaticTable(3, 3, 75, 9, ids())
    const selected = [staticTableCellAt(table, 0, 0).id, staticTableCellAt(table, 0, 1).id, staticTableCellAt(table, 1, 0).id, staticTableCellAt(table, 1, 1).id]
    const merged = mergeStaticTableCells(table, selected, ids())
    expect(table.cells.find(cell => cell.id === merged)).toMatchObject({ row: 0, column: 0, rowSpan: 2, colSpan: 2 })
    expect(splitStaticTableCell(table, merged, ids())).toHaveLength(4)
    expect(table.cells).toHaveLength(9)
    expect(staticTableCellAt(table, 1, 1)).toBeTruthy()
  })

  it('refuses disconnected merges and adjusts merged spans while deleting tracks', () => {
    const table = createStaticTable(3, 3, 75, 9, ids())
    expect(mergeStaticTableCells(table, [staticTableCellAt(table, 0, 0).id, staticTableCellAt(table, 2, 2).id], ids())).toBeNull()
    const selected = [0, 1, 3, 4].map(index => table.cells[index].id)
    const merged = mergeStaticTableCells(table, selected, ids())
    expect(merged).toBeTruthy()
    expect(deleteStaticTableRow(table, 0)).toBe(true)
    expect(deleteStaticTableColumn(table, 0)).toBe(true)
    expect(table.rows).toHaveLength(2)
    expect(table.columns).toHaveLength(2)
    for (let row = 0; row < 2; row++) {
      for (let column = 0; column < 2; column++)
        expect(staticTableCellAt(table, row, column)).toBeTruthy()
    }
  })

  it('selects every cell intersecting a Word-like rectangle', () => {
    const table = createStaticTable(3, 3, 75, 9, ids(), { headerRow: false })
    const idsSelected = staticTableIdsInRect(table, 0, 0, 1, 1)
    expect(idsSelected).toHaveLength(4)
    expect(idsSelected).toEqual(expect.arrayContaining([
      staticTableCellAt(table, 0, 0).id,
      staticTableCellAt(table, 0, 1).id,
      staticTableCellAt(table, 1, 0).id,
      staticTableCellAt(table, 1, 1).id,
    ]))
  })

  it('inserts rows and columns at a relative index', () => {
    const table = createStaticTable(2, 2, 40, 8, ids(), { headerRow: false })
    const above = insertStaticTableRow(table, 0, ids())
    expect(above).toHaveLength(2)
    expect(table.rows).toHaveLength(3)
    expect(staticTableCellAt(table, 0, 0).id).toBe(above[0])
    const left = insertStaticTableColumn(table, 0, ids())
    expect(left).toHaveLength(3)
    expect(table.columns).toHaveLength(3)
    expect(staticTableCellAt(table, 1, 0).id).toBe(left[1])
  })

  it('inserts multiple rows and columns in order', () => {
    const table = createStaticTable(2, 2, 40, 8, ids(), { headerRow: false })
    const rows = insertStaticTableRows(table, 1, 3, ids())
    expect(table.rows).toHaveLength(5)
    expect(rows).toHaveLength(6)
    expect(staticTableCellAt(table, 1, 0).id).toBe(rows[0])
    expect(staticTableCellAt(table, 3, 0).id).toBe(rows[4])
    const cols = insertStaticTableColumns(table, 0, 2, ids())
    expect(table.columns).toHaveLength(4)
    expect(cols).toHaveLength(10)
    expect(staticTableCellAt(table, 0, 0).id).toBe(cols[0])
    expect(staticTableCellAt(table, 0, 1).id).toBe(cols[5])
  })

  it('places selected cell bounds on the cell, not the whole table', () => {
    const table = createStaticTable(2, 2, 40, 8, ids(), { headerRow: false })
    const element = { type: 'STATIC_TABLE', xMm: 10, yMm: 20, widthMm: 40, heightMm: 16, table }
    const cell = staticTableCellAt(table, 1, 1)
    expect(staticTableCellPaperBounds(element, [cell])).toEqual({
      xMm: 30,
      yMm: 28,
      widthMm: 20,
      heightMm: 8,
    })
    expect(resizeHandlesForElement(element)).toEqual(['n', 'ne', 'e', 'se', 's', 'sw', 'w'])
    expect(resizeHandlesForElement({ type: 'DATA_TABLE' })).toEqual(['n', 'ne', 'e', 'se', 's', 'sw', 'w'])
  })

  it('deletes multiple selected rows and columns (highest index first)', () => {
    const table = createStaticTable(4, 4, 80, 8, ids(), { headerRow: false })
    expect(deleteStaticTableRows(table, [1, 3])).toBe(true)
    expect(table.rows).toHaveLength(2)
    expect(deleteStaticTableColumns(table, [0, 2])).toBe(true)
    expect(table.columns).toHaveLength(2)
    for (let row = 0; row < table.rows.length; row++) {
      for (let column = 0; column < table.columns.length; column++)
        expect(staticTableCellAt(table, row, column)).toBeTruthy()
    }
  })
})
