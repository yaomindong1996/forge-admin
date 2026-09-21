/** Shared canvas zoom steps for the top toolbar and middle action bar. */
export const PRINT_ZOOM_LEVELS = Object.freeze([0.5, 0.65, 0.8, 1, 1.25, 1.5])

export function nextPrintZoom(current, step = 1) {
  const levels = PRINT_ZOOM_LEVELS
  const index = Math.max(0, levels.findIndex(value => value === current))
  const next = Math.min(levels.length - 1, Math.max(0, (index === -1 ? levels.indexOf(0.8) : index) + step))
  return levels[next]
}

export function formatPrintZoom(value) {
  return `${Math.round((Number(value) || 1) * 100)}%`
}

/** Map a scrollable viewport onto a minimap rectangle. */
export function computeMiniMapRegion(metrics, mapWidth, mapHeight) {
  const scrollWidth = Math.max(1, metrics?.scrollWidth || 1)
  const scrollHeight = Math.max(1, metrics?.scrollHeight || 1)
  const clientWidth = Math.max(0, metrics?.clientWidth || 0)
  const clientHeight = Math.max(0, metrics?.clientHeight || 0)
  const scrollLeft = Math.max(0, metrics?.scrollLeft || 0)
  const scrollTop = Math.max(0, metrics?.scrollTop || 0)
  const width = Math.min(mapWidth, (clientWidth / scrollWidth) * mapWidth)
  const height = Math.min(mapHeight, (clientHeight / scrollHeight) * mapHeight)
  const maxLeft = Math.max(0, mapWidth - width)
  const maxTop = Math.max(0, mapHeight - height)
  const left = Math.min(maxLeft, (scrollLeft / scrollWidth) * mapWidth)
  const top = Math.min(maxTop, (scrollTop / scrollHeight) * mapHeight)
  return {
    left: Number(left.toFixed(2)),
    top: Number(top.toFixed(2)),
    width: Number(width.toFixed(2)),
    height: Number(height.toFixed(2)),
  }
}

/** Convert a minimap point (region top-left) into viewport scroll offsets. */
export function scrollFromMiniMapPoint(left, top, metrics, mapWidth, mapHeight) {
  const scrollWidth = Math.max(1, metrics?.scrollWidth || 1)
  const scrollHeight = Math.max(1, metrics?.scrollHeight || 1)
  const maxScrollLeft = Math.max(0, scrollWidth - (metrics?.clientWidth || 0))
  const maxScrollTop = Math.max(0, scrollHeight - (metrics?.clientHeight || 0))
  return {
    scrollLeft: Math.min(maxScrollLeft, Math.max(0, (left / Math.max(1, mapWidth)) * scrollWidth)),
    scrollTop: Math.min(maxScrollTop, Math.max(0, (top / Math.max(1, mapHeight)) * scrollHeight)),
  }
}
