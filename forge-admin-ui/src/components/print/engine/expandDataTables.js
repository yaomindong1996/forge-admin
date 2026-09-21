function newPrintId() {
  return `p_${globalThis.crypto.randomUUID()}`
}

function normalizeTableColumnWidths(columns, targetWidthMm) {
  if (!columns?.length || !Number.isFinite(targetWidthMm) || targetWidthMm <= 0)
    return
  const sum = columns.reduce((total, column) => total + (Number(column.widthMm) || 0), 0)
  if (sum <= 0) {
    const each = Number((targetWidthMm / columns.length).toFixed(2))
    columns.forEach((column, index) => {
      column.widthMm = index === columns.length - 1
        ? Number((targetWidthMm - each * (columns.length - 1)).toFixed(2))
        : each
    })
    return
  }
  const scale = targetWidthMm / sum
  columns.forEach((column) => {
    column.widthMm = Number((column.widthMm * scale).toFixed(2))
  })
  const drift = Number((targetWidthMm - columns.reduce((total, column) => total + column.widthMm, 0)).toFixed(2))
  columns.at(-1).widthMm = Number((columns.at(-1).widthMm + drift).toFixed(2))
}

function dataTableToSection(element) {
  const section = {
    id: element.id,
    kind: 'TABLE',
    gapAfterMm: 2,
    collectionPath: element.collectionPath,
    repeatHeader: element.repeatHeader !== false,
    emptyText: element.emptyText ?? '暂无明细',
    columns: JSON.parse(JSON.stringify(element.columns || [])),
  }
  if (element.headerStyle)
    section.headerStyle = JSON.parse(JSON.stringify(element.headerStyle))
  if (element.style)
    section.style = JSON.parse(JSON.stringify(element.style))
  if (element.oddRowStyle)
    section.oddRowStyle = JSON.parse(JSON.stringify(element.oddRowStyle))
  if (element.evenRowStyle)
    section.evenRowStyle = JSON.parse(JSON.stringify(element.evenRowStyle))
  if (element.headerRows)
    section.headerRows = JSON.parse(JSON.stringify(element.headerRows))
  if (element.footer)
    section.footer = JSON.parse(JSON.stringify(element.footer))
  if (element.subtotal)
    section.subtotal = JSON.parse(JSON.stringify(element.subtotal))
  if (element.cellStyles)
    section.cellStyles = JSON.parse(JSON.stringify(element.cellStyles))
  if (Number.isFinite(element.minHeightMm))
    section.minHeightMm = element.minHeightMm
  normalizeTableColumnWidths(section.columns, element.widthMm || section.columns.reduce((sum, column) => sum + column.widthMm, 0))
  return section
}

/**
 * Designer places detail tables as free DATA_TABLE elements. Print layout still
 * needs flow TABLE sections for row pagination — expand in document order by y.
 */
export function expandDataTablesForLayout(document) {
  const body = []
  for (const section of document.body) {
    if (section.kind !== 'FIXED' || !Array.isArray(section.elements) || !section.elements.some(item => item.type === 'DATA_TABLE')) {
      body.push(section)
      continue
    }
    const blocks = [
      ...section.elements.filter(item => item.type !== 'DATA_TABLE').map(item => ({ kind: 'el', item, y: item.yMm })),
      ...section.elements.filter(item => item.type === 'DATA_TABLE').map(item => ({ kind: 'table', item, y: item.yMm })),
    ].sort((a, b) => a.y - b.y || a.item.xMm - b.item.xMm)

    let buffer = []
    const flush = () => {
      if (!buffer.length)
        return
      const minY = Math.min(...buffer.map(item => item.yMm))
      const bottom = Math.max(...buffer.map(item => item.yMm + item.heightMm), minY + 8)
      body.push({
        id: newPrintId(),
        kind: 'FIXED',
        heightMm: Number((bottom - minY).toFixed(3)),
        gapAfterMm: 2,
        elements: buffer.map(item => ({ ...item, yMm: Number((item.yMm - minY).toFixed(3)) })),
      })
      buffer = []
    }

    for (const block of blocks) {
      if (block.kind === 'table') {
        flush()
        body.push(dataTableToSection(block.item))
      }
      else {
        buffer.push(block.item)
      }
    }
    flush()
  }
  document.body = body
  return document
}
