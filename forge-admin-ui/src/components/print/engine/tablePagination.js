import { FIT_EPSILON } from './pageGeometry'
import { materializeTableBand } from './prepare'

export function tableMinimumHeight(section) {
  const subtotal = section.subtotalTemplate ? 1 : 0
  return section.headers.reduce((sum, row) => sum + row.heightMm, 0) + (section.rows[0]?.heightMm || 0) + subtotal * (section.rows[0]?.heightMm || 0)
}

export function paginateTable(section, cursor, options = {}) {
  const headerHeight = section.headers.reduce((sum, row) => sum + row.heightMm, 0)
  const dataRows = section.rows
  const allRecords = section.source || dataRows.map(row => row.record).filter(Boolean)
  const makeSubtotal = (records) => {
    if (!section.subtotalTemplate || !options.measure)
      return null
    return materializeTableBand(section, section.subtotalTemplate, records, 'subtotal', options.measure, options.context || {}, options.resources)
  }
  const sampleSubtotal = makeSubtotal(allRecords)
  const subtotalHeight = sampleSubtotal?.heightMm || 0
  let offset = 0
  let first = true
  let footerPlaced = !section.footer
  while (offset < dataRows.length) {
    const headers = first || section.repeatHeader ? section.headers : []
    const reservedMm = (headers.length ? headerHeight : 0) + subtotalHeight
    cursor.ensure(reservedMm + dataRows[offset].heightMm, `${section.id}:${dataRows[offset].key}`)
    const rows = [...headers]
    let heightMm = headers.length ? headerHeight : 0
    while (offset < dataRows.length && heightMm + dataRows[offset].heightMm + subtotalHeight <= cursor.remainingMm + FIT_EPSILON) {
      rows.push(dataRows[offset])
      heightMm += dataRows[offset].heightMm
      offset++
    }
    const pageRecords = rows.filter(row => row.kind === 'data').map(row => row.record).filter(Boolean)
    const subtotal = makeSubtotal(pageRecords) || sampleSubtotal
    if (subtotal) {
      rows.push(subtotal)
      heightMm += subtotal.heightMm
    }
    const last = offset >= dataRows.length
    if (last && section.footer && heightMm + section.footer.heightMm <= cursor.remainingMm + FIT_EPSILON) {
      rows.push(section.footer)
      heightMm += section.footer.heightMm
      footerPlaced = true
    }
    cursor.place({ id: section.id, kind: 'TABLE', type: 'TABLE', widthMm: section.widthMm, rows, heightMm })
    first = false
    if (offset < dataRows.length || (last && !footerPlaced))
      cursor.next()
  }
  if (!footerPlaced && section.footer) {
    const headers = section.repeatHeader ? section.headers : []
    const reservedMm = headers.length ? headerHeight : 0
    cursor.ensure(reservedMm + section.footer.heightMm, `${section.id}:footer`)
    const rows = [...headers, section.footer]
    cursor.place({
      id: section.id,
      kind: 'TABLE',
      type: 'TABLE',
      widthMm: section.widthMm,
      rows,
      heightMm: reservedMm + section.footer.heightMm,
    })
  }
}
