import { PRINT_LIMITS, PrintError } from '../protocol/types'
import { paperGeometry } from '../protocol/units'

function sheetSize(tiling, geometry) {
  return {
    widthMm: tiling.sheetWidthMm || geometry.widthMm,
    heightMm: tiling.sheetHeightMm || geometry.heightMm,
  }
}

export function assertTilingFits(document, geometry) {
  const tiling = document.paper?.tiling
  if (!tiling?.enabled)
    return
  const columns = tiling.columns || 1
  const rows = tiling.rows || 1
  const gapX = tiling.gapXMm || 0
  const gapY = tiling.gapYMm || 0
  const sheet = sheetSize(tiling, geometry)
  const needWidth = columns * geometry.widthMm + (columns - 1) * gapX
  const needHeight = rows * geometry.heightMm + (rows - 1) * gapY
  if (needWidth > sheet.widthMm + 0.05 || needHeight > sheet.heightMm + 0.05) {
    throw new PrintError('TILING_OVERFLOW', '标签拼版超出目标纸张，请减少行列、间隙或标签尺寸')
  }
}

export function tilePrintPages(pages, document, geometry) {
  const tiling = document.paper?.tiling
  if (!tiling?.enabled)
    return { pages, geometry }
  const columns = tiling.columns || 1
  const rows = tiling.rows || 1
  const gapX = tiling.gapXMm || 0
  const gapY = tiling.gapYMm || 0
  const sheet = sheetSize(tiling, geometry)
  const perSheet = columns * rows
  const source = [...pages]
  if ((tiling.repeatToFill || source.length === 1) && source.length && source.length % perSheet !== 0) {
    const fill = perSheet - (source.length % perSheet)
    for (let i = 0; i < fill; i++)
      source.push(pages[i % pages.length])
  }
  if (source.length > PRINT_LIMITS.pages * perSheet) {
    throw new PrintError('PAGE_LIMIT', `打印页数超过 ${PRINT_LIMITS.pages} 页`)
  }
  const sheets = []
  for (let offset = 0; offset < source.length; offset += perSheet) {
    const chunk = source.slice(offset, offset + perSheet)
    const fragments = chunk.map((page, index) => {
      const column = index % columns
      const row = Math.floor(index / columns)
      return {
        id: `tile-${page.number}-${index}`,
        kind: 'TILE',
        type: 'TILE',
        xMm: column * (geometry.widthMm + gapX),
        yMm: row * (geometry.heightMm + gapY),
        widthMm: geometry.widthMm,
        heightMm: geometry.heightMm,
        page,
        geometry,
      }
    })
    sheets.push({
      number: sheets.length + 1,
      header: { xMm: 0, yMm: 0, elements: [] },
      footer: { xMm: 0, yMm: 0, elements: [] },
      fragments,
    })
    if (sheets.length > PRINT_LIMITS.pages)
      throw new PrintError('PAGE_LIMIT', `打印页数超过 ${PRINT_LIMITS.pages} 页`)
  }
  const tiledGeometry = {
    ...paperGeometry({
      paper: {
        widthMm: Math.min(sheet.widthMm, sheet.heightMm),
        heightMm: Math.max(sheet.widthMm, sheet.heightMm),
        orientation: sheet.widthMm >= sheet.heightMm ? 'LANDSCAPE' : 'PORTRAIT',
        marginMm: { top: 0, right: 0, bottom: 0, left: 0 },
      },
      header: { heightMm: 0 },
      footer: { heightMm: 0 },
    }),
    widthMm: sheet.widthMm,
    heightMm: sheet.heightMm,
    contentWidthMm: sheet.widthMm,
    contentHeightMm: sheet.heightMm,
    bodyTopMm: 0,
    bodyLeftMm: 0,
    footerTopMm: sheet.heightMm,
  }
  return { pages: sheets, geometry: tiledGeometry }
}
