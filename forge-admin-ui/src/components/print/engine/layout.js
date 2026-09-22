import { evaluateExpression } from '../protocol/expression'
import { validateFieldCatalog } from '../protocol/fieldCatalog'
import { PRINT_LIMITS, PrintError } from '../protocol/types'
import { paperGeometry } from '../protocol/units'
import { assertPrintDocument } from '../protocol/validate'
import { expandDataTablesForLayout } from './expandDataTables'
import { createPageCursor } from './pageGeometry'
import { fillPageNumbers } from './pageNumbers'
import { prepareElements, prepareSection } from './prepare'
import { paginateTable, tableMinimumHeight } from './tablePagination'
import { paginateText } from './textPagination'
import { assertTilingFits, tilePrintPages } from './tiling'

function freeze(value) {
  if (value && typeof value === 'object' && !Object.isFrozen(value)) {
    Object.values(value).forEach(freeze)
    Object.freeze(value)
  }
  return value
}

function layoutGeometry(document) {
  const geometry = paperGeometry(document)
  if (document.paper.kind !== 'CONTINUOUS')
    return geometry
  const maxHeight = PRINT_LIMITS.paperSizeMm
  const margin = document.paper.marginMm
  return {
    ...geometry,
    heightMm: maxHeight,
    contentHeightMm: maxHeight - margin.top - margin.bottom - document.header.heightMm - document.footer.heightMm,
    footerTopMm: maxHeight - margin.bottom - document.footer.heightMm,
  }
}

function shrinkContinuousGeometry(document, geometry, usedMm) {
  if (document.paper.kind !== 'CONTINUOUS')
    return geometry
  const margin = document.paper.marginMm
  const heightMm = Number((margin.top + document.header.heightMm + usedMm + document.footer.heightMm + margin.bottom).toFixed(3))
  if (heightMm > PRINT_LIMITS.paperSizeMm)
    throw new PrintError('PAGE_LIMIT', `连续纸高度超过 ${PRINT_LIMITS.paperSizeMm} mm`)
  return {
    ...geometry,
    heightMm,
    contentHeightMm: usedMm,
    footerTopMm: heightMm - margin.bottom - document.footer.heightMm,
  }
}

function resolveWatermark(document, context) {
  const watermark = document.watermark
  if (!watermark || watermark.enabled === false)
    return null
  const expressionText = watermark.expression
    ? String(evaluateExpression(watermark.expression, context) ?? '').trim()
    : ''
  const staticText = String(watermark.text ?? '').trim()
  const text = [staticText, expressionText].filter(Boolean).join(' · ')
  if (!text)
    return null
  return {
    text,
    opacity: watermark.opacity ?? 0.08,
    rotateDeg: watermark.rotateDeg ?? -24,
    gapXMm: watermark.gapXMm ?? 64,
    gapYMm: watermark.gapYMm ?? 42,
    fontSizePt: watermark.fontSizePt ?? 14,
    color: watermark.color || '#94a3b8',
  }
}

function minimumHeight(section) {
  if (!section) {
    return 0
  }
  if (section.kind === 'TEXT') {
    return section.lineHeightMm + 2 * section.insetMm
  }
  return section.kind === 'TABLE' ? tableMinimumHeight(section) : section.heightMm
}

export function layoutPrintDocument(input, context, { measure, resources, catalog = [], templateVersion = null } = {}) {
  // Clone first so expand never mutates the editable template.
  const document = JSON.parse(JSON.stringify(input))
  expandDataTablesForLayout(document)
  assertPrintDocument(document)
  const issues = validateFieldCatalog(document, catalog)
  if (issues.length) {
    throw new PrintError('FIELD_NOT_ALLOWED', issues[0].message, issues[0].path, issues)
  }
  const geometry = layoutGeometry(document)
  assertTilingFits(document, paperGeometry(document))
  const header = prepareElements(document.header.elements, context, measure, resources)
  const footer = prepareElements(document.footer.elements, context, measure, resources)
  const sections = document.body.map(section => prepareSection(section, context, measure, geometry, resources, catalog))
  if (sections.reduce((sum, section) => sum + (section.sourceRowCount || 0), 0) > PRINT_LIMITS.rows) {
    throw new PrintError('ROW_LIMIT', `所有明细合计最多 ${PRINT_LIMITS.rows} 行`)
  }
  const cursor = createPageCursor(geometry)
  if (document.paper.kind === 'CONTINUOUS') {
    cursor.next = () => {
      throw new PrintError('PAGE_LIMIT', '连续纸内容超过最大高度，请缩短内容或改用普通纸张')
    }
    cursor.forceBreak = () => {
      throw new PrintError('INVALID_PAGE_BREAK', '连续纸不支持手动分页')
    }
  }
  function requiredStart(index) {
    const section = sections[index]
    if (!section) {
      return 0
    }
    if (section.keepWithNext && index + 1 < sections.length) {
      return section.heightMm + (section.gapAfterMm || 0) + requiredStart(index + 1)
    }
    return minimumHeight(section)
  }
  sections.forEach((section, index) => {
    if (section.kind === 'PAGE_BREAK') {
      cursor.forceBreak()
      return
    }
    if (section.keepWithNext && index + 1 < sections.length) {
      cursor.ensure(requiredStart(index), section.id)
    }
    if (section.kind === 'TEXT') {
      paginateText(section, cursor)
    }
    else if (section.kind === 'TABLE') {
      paginateTable(section, cursor, { measure, context, resources, catalog })
    }
    else {
      cursor.place(section)
    }
    cursor.gap(section.gapAfterMm || 0)
  })
  const total = cursor.pages.length
  let resultGeometry = shrinkContinuousGeometry(document, geometry, cursor.usedMm)
  cursor.pages.forEach((page, index) => {
    page.number = index + 1
    page.header = { xMm: resultGeometry.bodyLeftMm, yMm: document.paper.marginMm.top, elements: document.header.repeat || index === 0 ? fillPageNumbers(header, index + 1, total, measure) : [] }
    page.footer = { xMm: resultGeometry.bodyLeftMm, yMm: resultGeometry.footerTopMm, elements: document.footer.repeat || index === total - 1 ? fillPageNumbers(footer, index + 1, total, measure) : [] }
    page.fragments = page.fragments.map(fragment => fragment.kind === 'FIXED' ? { ...fragment, elements: fillPageNumbers(fragment.elements, index + 1, total, measure) } : fragment)
  })
  const tiled = tilePrintPages(cursor.pages, document, resultGeometry)
  resultGeometry = tiled.geometry
  const overlaySrc = resources?.images.get('design-background') || ''
  return freeze({
    geometry: resultGeometry,
    pages: tiled.pages,
    warnings: [],
    templateVersion,
    generatedAt: context.system?.generatedAt ?? new Date().toISOString(),
    dataMode: 'CURRENT',
    watermark: resolveWatermark(document, context),
    overlay: overlaySrc
      ? { src: overlaySrc, print: !!document.paper.designBackground?.print, opacity: document.paper.designBackground?.opacity ?? 1, rotationDeg: document.paper.designBackground?.rotationDeg ?? 0 }
      : null,
  })
}
