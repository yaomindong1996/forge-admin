import { paperGeometry } from '../protocol/units'
import { clampElementToContent, normalizeTableColumnWidths } from './commands'
import { descriptionBlockHeightMm } from './descriptions'

const MIN_BODY_MM = 20
const MIN_ELEMENT_MM = 0.1

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function refreshElementAfterWidthChange(element) {
  if (element?.type === 'DESCRIPTIONS' && element.descriptions)
    element.heightMm = Math.max(MIN_ELEMENT_MM, descriptionBlockHeightMm(element.descriptions))
  if (element?.type === 'STATIC_TABLE' && element.table?.columns?.length)
    normalizeTableColumnWidths(element.table.columns, element.widthMm)
}

function fitElementsToSurface(document, surface, maxHeightMm) {
  if (!surface?.elements?.length)
    return
  const limit = Number.isFinite(maxHeightMm) ? Math.max(MIN_ELEMENT_MM, maxHeightMm) : Number.POSITIVE_INFINITY
  surface.elements.forEach((element) => {
    clampElementToContent(document, element)
    refreshElementAfterWidthChange(element)
    if (!Number.isFinite(element.heightMm))
      return
    element.heightMm = Number(clamp(element.heightMm, MIN_ELEMENT_MM, limit).toFixed(3))
    const maxY = Math.max(0, limit - element.heightMm)
    element.yMm = Number(clamp(Number(element.yMm) || 0, 0, maxY).toFixed(3))
  })
  if (!Number.isFinite(surface.heightMm))
    return
  const bottom = surface.elements.reduce(
    (max, element) => Math.max(max, (Number(element.yMm) || 0) + (Number(element.heightMm) || 0)),
    0,
  )
  surface.heightMm = Number(clamp(Math.max(bottom, Math.min(surface.heightMm, limit)), 0, limit).toFixed(3))
}

/**
 * After paper size / orientation / margin changes, shrink and reposition free layout
 * so save and preview do not fail with out-of-bounds or single-page overflow.
 */
export function fitDocumentToPaper(document) {
  if (!document?.paper)
    return
  const geometry = paperGeometry(document)
  const contentWidth = geometry.contentWidthMm
  const sheet = document.paper.kind !== 'CONTINUOUS'
  const printableMm = Math.max(
    MIN_BODY_MM,
    geometry.heightMm - document.paper.marginMm.top - document.paper.marginMm.bottom,
  )

  if (sheet) {
    const header = document.header
    const footer = document.footer
    const bandBudget = Math.max(0, printableMm - MIN_BODY_MM)
    const headerHeight = Math.max(0, Number(header?.heightMm) || 0)
    const footerHeight = Math.max(0, Number(footer?.heightMm) || 0)
    const bandSum = headerHeight + footerHeight
    if (bandSum > bandBudget && bandSum > 0) {
      if (header && Number.isFinite(header.heightMm))
        header.heightMm = Number(((headerHeight / bandSum) * bandBudget).toFixed(3))
      if (footer && Number.isFinite(footer.heightMm))
        footer.heightMm = Number(((footerHeight / bandSum) * bandBudget).toFixed(3))
    }
    fitElementsToSurface(document, header, header?.heightMm ?? 0)
    fitElementsToSurface(document, footer, footer?.heightMm ?? 0)
  }
  else {
    fitElementsToSurface(document, document.header, Number.POSITIVE_INFINITY)
    fitElementsToSurface(document, document.footer, Number.POSITIVE_INFINITY)
  }

  const bodyLimit = sheet ? Math.max(MIN_BODY_MM, paperGeometry(document).contentHeightMm) : Number.POSITIVE_INFINITY
  ;(document.body || []).forEach((section) => {
    if (section.kind === 'FIXED') {
      fitElementsToSurface(document, section, bodyLimit)
      return
    }
    if (section.kind === 'TABLE' && section.columns?.length)
      normalizeTableColumnWidths(section.columns, contentWidth)
  })
}
