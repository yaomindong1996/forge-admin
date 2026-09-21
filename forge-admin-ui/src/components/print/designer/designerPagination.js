import { PRINT_LIMITS } from '../protocol/types'
import { paperGeometry } from '../protocol/units'

const MIN_FLOW_HEIGHT_MM = 16
const MIN_TABLE_HEIGHT_MM = 24

export function estimateDesignerSectionHeight(section, { text = '', tableRows = 0, contentWidthMm = 190 } = {}) {
  if (section.kind === 'FIXED')
    return section.heightMm
  if (section.kind === 'TEXT') {
    const charactersPerLine = Math.max(12, Math.floor(contentWidthMm / 2.6))
    const lines = String(text).split('\n').reduce((sum, line) => sum + Math.max(1, Math.ceil(line.length / charactersPerLine)), 0)
    return Math.max(MIN_FLOW_HEIGHT_MM, 6 + lines * 5)
  }
  if (section.kind === 'TABLE')
    return Math.max(MIN_TABLE_HEIGHT_MM, Number(section.minHeightMm) || 0, 4 + tableRows * 8)
  return 0
}

/**
 * Gives the editor a stable paper estimate from designer sample data. The final
 * print engine still owns exact font and row measurement during preview/print.
 */
export function paginateDesignerBody(document, heightOf) {
  const capacityMm = paperGeometry(document).contentHeightMm
  const pages = [{ number: 1, sections: [], usedMm: 0, breakBefore: null, automaticBreakBefore: null, overflow: false }]
  let current = pages[0]
  const nextPage = (breakBefore = null, automaticBreakBefore = null) => {
    if (pages.length >= PRINT_LIMITS.pages) {
      current.overflow = true
      return false
    }
    current = { number: pages.length + 1, sections: [], usedMm: 0, breakBefore, automaticBreakBefore, overflow: false }
    pages.push(current)
    return true
  }

  for (const section of document.body) {
    if (section.kind === 'PAGE_BREAK') {
      nextPage(section)
      continue
    }
    const heightMm = Math.max(0, Number(heightOf(section)) || 0)
    if (current.sections.length && heightMm > capacityMm - current.usedMm + 0.0001)
      nextPage(null, section.id)
    current.sections.push({ section, heightMm })
    current.usedMm = Math.min(capacityMm, current.usedMm + heightMm + (section.gapAfterMm || 0))
    if (heightMm > capacityMm)
      current.overflow = true
  }
  return { pages, capacityMm, truncated: pages.at(-1).overflow && document.body.length > pages.flatMap(page => page.sections).length }
}
