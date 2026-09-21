import { paperGeometry } from '../protocol/units'

/** Scale column widths so they sum exactly to targetWidthMm. */
export function normalizeTableColumnWidths(columns, targetWidthMm) {
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

export function newPrintId() {
  return `p_${globalThis.crypto.randomUUID()}`
}

export function findSurface(document, id) {
  return id === 'header' || id === 'footer' ? document[id] : document.body.find(section => `section:${section.id}` === id || section.id === id)
}

export function selectionBounds(elements) {
  if (!elements.length) {
    return null
  }
  const left = Math.min(...elements.map(e => e.xMm))
  const top = Math.min(...elements.map(e => e.yMm))
  return {
    xMm: left,
    yMm: top,
    widthMm: Math.max(...elements.map(e => e.xMm + e.widthMm)) - left,
    heightMm: Math.max(...elements.map(e => e.yMm + e.heightMm)) - top,
  }
}

function axisAnchors(start, size) {
  return [start, start + size / 2, start + size]
}

function nearestSnap(moving, targets, thresholdMm) {
  let best = null
  for (const source of moving) {
    for (const target of targets) {
      const adjustment = target - source
      if (Math.abs(adjustment) <= thresholdMm && (!best || Math.abs(adjustment) < Math.abs(best.adjustment))) {
        best = { adjustment, target }
      }
    }
  }
  return best
}

function snapAxis(moving, targets, gridSource, thresholdMm) {
  const edge = nearestSnap(moving, targets, thresholdMm)
  const gridTarget = Math.round(gridSource)
  const gridAdjustment = gridTarget - gridSource
  const grid = Math.abs(gridAdjustment) <= Math.min(0.35, thresholdMm)
    ? { adjustment: gridAdjustment, target: gridTarget }
    : null
  if (!edge)
    return grid
  if (!grid || Math.abs(edge.adjustment) <= Math.abs(grid.adjustment))
    return edge
  return grid
}

function surfaceSnapTargets(document, surfaceId, excludedIds = []) {
  const surface = findSurface(document, surfaceId)
  const width = paperGeometry(document).contentWidthMm
  const x = [0, width / 2, width]
  // Do not snap to surface.heightMm — that traps downward free-drag and prevents band auto-grow.
  const y = [0, Number.isFinite(surface?.heightMm) ? surface.heightMm / 2 : null].filter(Number.isFinite)
  for (const element of surface?.elements || []) {
    if (excludedIds.includes(element.id))
      continue
    x.push(...axisAnchors(element.xMm, element.widthMm))
    y.push(...axisAnchors(element.yMm, element.heightMm))
  }
  return { x, y }
}

export function snapTranslation(document, surfaceId, ids, dx, dy, thresholdMm = 0.8) {
  const surface = findSurface(document, surfaceId)
  const elements = surface?.elements?.filter(element => ids.includes(element.id)) || []
  const bounds = selectionBounds(elements)
  if (!bounds)
    return { dx, dy, guides: { x: [], y: [], position: null } }
  const targets = surfaceSnapTargets(document, surfaceId, ids)
  const nextX = bounds.xMm + dx
  const nextY = bounds.yMm + dy
  const xSnap = snapAxis(axisAnchors(nextX, bounds.widthMm), targets.x, nextX, thresholdMm)
  const ySnap = snapAxis(axisAnchors(nextY, bounds.heightMm), targets.y, nextY, thresholdMm)
  const snappedDx = dx + (xSnap?.adjustment || 0)
  const snappedDy = dy + (ySnap?.adjustment || 0)
  return {
    dx: snappedDx,
    dy: snappedDy,
    guides: {
      x: xSnap ? [Number(xSnap.target.toFixed(3))] : [],
      y: ySnap ? [Number(ySnap.target.toFixed(3))] : [],
      position: {
        xMm: Number((bounds.xMm + snappedDx).toFixed(3)),
        yMm: Number((bounds.yMm + snappedDy).toFixed(3)),
      },
    },
  }
}

export function snapResize(document, surfaceId, id, dx, dy, thresholdMm = 0.8, handle = 'se') {
  const surface = findSurface(document, surfaceId)
  const element = surface?.elements?.find(item => item.id === id)
  if (!element)
    return { dx, dy, guides: { x: [], y: [], position: null } }
  const targets = surfaceSnapTargets(document, surfaceId, [id])
  let nextLeft = element.xMm
  let nextTop = element.yMm
  let nextRight = element.xMm + element.widthMm
  let nextBottom = element.yMm + element.heightMm
  if (handle.includes('e'))
    nextRight += dx
  if (handle.includes('w'))
    nextLeft += dx
  if (handle.includes('s'))
    nextBottom += dy
  if (handle.includes('n'))
    nextTop += dy
  const movingX = []
  const movingY = []
  if (handle.includes('e'))
    movingX.push(nextRight)
  if (handle.includes('w'))
    movingX.push(nextLeft)
  if (handle.includes('s'))
    movingY.push(nextBottom)
  if (handle.includes('n'))
    movingY.push(nextTop)
  const xSnap = movingX.length ? snapAxis(movingX, targets.x, movingX[0], thresholdMm) : null
  const ySnap = movingY.length ? snapAxis(movingY, targets.y, movingY[0], thresholdMm) : null
  const snappedDx = dx + (xSnap?.adjustment || 0)
  const snappedDy = dy + (ySnap?.adjustment || 0)
  return {
    dx: snappedDx,
    dy: snappedDy,
    guides: {
      x: xSnap ? [Number(xSnap.target.toFixed(3))] : [],
      y: ySnap ? [Number(ySnap.target.toFixed(3))] : [],
      position: {
        xMm: Number((handle.includes('w') ? element.xMm + snappedDx : element.xMm).toFixed(3)),
        yMm: Number((handle.includes('n') ? element.yMm + snappedDy : element.yMm).toFixed(3)),
        widthMm: Number((element.widthMm + (handle.includes('e') ? snappedDx : 0) - (handle.includes('w') ? snappedDx : 0)).toFixed(3)),
        heightMm: Number((element.heightMm + (handle.includes('s') ? snappedDy : 0) - (handle.includes('n') ? snappedDy : 0)).toFixed(3)),
      },
    },
  }
}

const MIN_BODY_MM = 20

/** Header/footer may grow with drag, but leave a minimum body area on the page. Body FIXED bands grow freely. */
function maxGrowableHeightMm(document, surfaceId) {
  const surface = findSurface(document, surfaceId)
  if (!surface || !Number.isFinite(surface.heightMm))
    return Number.POSITIVE_INFINITY
  if (surfaceId !== 'header' && surfaceId !== 'footer')
    return Number.POSITIVE_INFINITY
  const geometry = paperGeometry(document)
  return Math.max(surface.heightMm, geometry.contentHeightMm + surface.heightMm - MIN_BODY_MM)
}

function growSurfaceToFit(document, surfaceId, bottomMm) {
  const surface = findSurface(document, surfaceId)
  if (!surface || !Number.isFinite(surface.heightMm) || !Number.isFinite(bottomMm))
    return
  const maxH = maxGrowableHeightMm(document, surfaceId)
  surface.heightMm = Number(Math.min(maxH, Math.max(surface.heightMm, bottomMm)).toFixed(3))
}

export function clampElementToContent(document, element) {
  if (!element || !Number.isFinite(element.widthMm))
    return
  const contentWidth = paperGeometry(document).contentWidthMm
  element.widthMm = Number(Math.min(Math.max(0.1, element.widthMm), contentWidth).toFixed(3))
  element.xMm = Number(Math.max(0, Math.min(element.xMm || 0, contentWidth - element.widthMm)).toFixed(3))
  if (element.type === 'DATA_TABLE' && element.columns?.length)
    normalizeTableColumnWidths(element.columns, element.widthMm)
}

export function translateElements(document, surfaceId, ids, dx, dy) {
  const surface = findSurface(document, surfaceId)
  const elements = surface?.elements?.filter(e => ids.includes(e.id)) || []
  const bounds = selectionBounds(elements)
  if (!bounds || !Number.isFinite(dx) || !Number.isFinite(dy)) {
    return
  }
  const maxX = paperGeometry(document).contentWidthMm - bounds.xMm - bounds.widthMm
  // X stays inside the printable content box; Y can grow header/footer/FIXED instead of hard-stopping.
  const maxH = maxGrowableHeightMm(document, surfaceId)
  const maxDy = Number.isFinite(maxH) ? maxH - bounds.yMm - bounds.heightMm : Number.POSITIVE_INFINITY
  const x = Math.min(maxX, Math.max(-bounds.xMm, Number(dx.toFixed(3))))
  const y = Math.min(maxDy, Math.max(-bounds.yMm, Number(dy.toFixed(3))))
  elements.forEach((e) => {
    e.xMm = e.xMm + x
    e.yMm = e.yMm + y
    clampElementToContent(document, e)
  })
  growSurfaceToFit(document, surfaceId, Math.max(...elements.map(e => e.yMm + e.heightMm)))
}

export function resizeElement(document, surfaceId, id, dx, dy, handle = 'se') {
  const surface = findSurface(document, surfaceId)
  const element = surface?.elements?.find(e => e.id === id)
  if (!element || !Number.isFinite(dx) || !Number.isFinite(dy)) {
    return
  }
  const previousWidth = element.widthMm
  const previousHeight = element.heightMm
  const contentWidth = paperGeometry(document).contentWidthMm
  const maxH = maxGrowableHeightMm(document, surfaceId)
  let xMm = element.xMm
  let yMm = element.yMm
  let widthMm = element.widthMm
  let heightMm = element.heightMm
  let nextDx = dx
  let nextDy = dy
  const lineAxis = element.type === 'LINE'
    ? (previousWidth <= previousHeight ? 'vertical' : 'horizontal')
    : null
  // Lines are axis-locked: vertical → height only; horizontal → width only.
  if (lineAxis === 'vertical') {
    nextDx = 0
    if (!handle.includes('n') && !handle.includes('s'))
      nextDy = 0
  }
  else if (lineAxis === 'horizontal') {
    nextDy = 0
    if (!handle.includes('e') && !handle.includes('w'))
      nextDx = 0
  }
  if (handle.includes('e'))
    widthMm += nextDx
  if (handle.includes('w')) {
    xMm += nextDx
    widthMm -= nextDx
  }
  if (handle.includes('s'))
    heightMm += nextDy
  if (handle.includes('n')) {
    yMm += nextDy
    heightMm -= nextDy
  }
  if (lineAxis === 'vertical')
    widthMm = Math.max(0.35, previousWidth)
  else if (lineAxis === 'horizontal')
    heightMm = Math.max(0.35, previousHeight)
  if (widthMm < 0.1) {
    if (handle.includes('w'))
      xMm -= 0.1 - widthMm
    widthMm = 0.1
  }
  if (heightMm < 0.1) {
    if (handle.includes('n'))
      yMm -= 0.1 - heightMm
    heightMm = 0.1
  }
  if (xMm < 0) {
    widthMm = Math.max(0.1, widthMm + xMm)
    xMm = 0
  }
  if (yMm < 0) {
    heightMm = Math.max(0.1, heightMm + yMm)
    yMm = 0
  }
  if (xMm + widthMm > contentWidth)
    widthMm = Math.max(0.1, contentWidth - xMm)
  if (Number.isFinite(maxH) && yMm + heightMm > maxH)
    heightMm = Math.max(0.1, maxH - yMm)
  element.xMm = Number(xMm.toFixed(3))
  element.yMm = Number(yMm.toFixed(3))
  element.widthMm = Number(widthMm.toFixed(3))
  element.heightMm = Number(heightMm.toFixed(3))
  clampElementToContent(document, element)
  growSurfaceToFit(document, surfaceId, element.yMm + element.heightMm)
  if (element.type === 'DATA_TABLE' && element.columns?.length && element.widthMm !== previousWidth)
    normalizeTableColumnWidths(element.columns, element.widthMm)
  if (element.type === 'STATIC_TABLE' && element.table && previousWidth > 0 && previousHeight > 0) {
    const scale = (items, key, ratio, total) => {
      items.forEach(item => item[key] = Number((item[key] * ratio).toFixed(3)))
      const rest = Number((total - items.reduce((sum, item) => sum + item[key], 0)).toFixed(3))
      items.at(-1)[key] = Number((items.at(-1)[key] + rest).toFixed(3))
    }
    scale(element.table.columns, 'widthMm', element.widthMm / previousWidth, element.widthMm)
    scale(element.table.rows, 'heightMm', element.heightMm / previousHeight, element.heightMm)
  }
}

export function isVerticalLine(element) {
  return element?.type === 'LINE' && Number(element.widthMm) <= Number(element.heightMm)
}

/** Keep line color/thickness in sync: fill, stroke and the thin box axis are the same thing. */
export function applyLineStyleSideEffects(element, stylePatch = {}) {
  if (element?.type !== 'LINE' || !stylePatch)
    return
  const previous = element.style || {}
  const color = ['backgroundColor', 'borderColor', 'color'].reduce((found, key) => {
    if (found)
      return found
    const value = stylePatch[key]
    return typeof value === 'string' && value !== previous[key] ? value : ''
  }, '')
  element.style = { ...previous, ...stylePatch }
  if (color) {
    element.style.borderColor = color
    element.style.backgroundColor = color
  }
  if (Number.isFinite(stylePatch.borderWidthMm) && stylePatch.borderWidthMm !== previous.borderWidthMm) {
    const mm = Math.max(0.15, Number(stylePatch.borderWidthMm))
    element.style.borderWidthMm = mm
    if (isVerticalLine(element))
      element.widthMm = mm
    else
      element.heightMm = mm
  }
}

export function applyLineGeometrySideEffects(element, patch = {}) {
  if (element?.type !== 'LINE')
    return
  const vertical = isVerticalLine(element)
  if (vertical && Number.isFinite(patch.widthMm))
    element.style = { ...element.style, borderWidthMm: patch.widthMm }
  if (!vertical && Number.isFinite(patch.heightMm))
    element.style = { ...element.style, borderWidthMm: patch.heightMm }
}

export function resizeHandlesForElement(element) {
  if (!element)
    return ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w']
  if (element.type === 'LINE')
    return isVerticalLine(element) ? ['n', 's'] : ['e', 'w']
  // Tables already have a top-left move handle; keep the other seven resize anchors.
  if (element.type === 'STATIC_TABLE' || element.type === 'DATA_TABLE')
    return ['n', 'ne', 'e', 'se', 's', 'sw', 'w']
  return ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w']
}
