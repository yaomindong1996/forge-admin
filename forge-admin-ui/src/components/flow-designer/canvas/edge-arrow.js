/** 在折线末端画小三角箭头（不依赖 SVG marker，避免 marker 引用异常导致整条线不显示） */

export const ARROW_SIZE = 9

/**
 * 缩短末段，让描边停在箭头根部，避免线从三角中间穿出去。
 * @returns {Array<{x:number,y:number}>}
 */
export function shortenPointsForArrow(points, size = ARROW_SIZE) {
  if (!Array.isArray(points) || points.length < 2)
    return []
  const pts = points
    .filter(p => Number.isFinite(p?.x) && Number.isFinite(p?.y))
    .map(p => ({ x: p.x, y: p.y }))
  if (pts.length < 2)
    return pts
  const a = pts[pts.length - 2]
  const b = pts[pts.length - 1]
  const dx = b.x - a.x
  const dy = b.y - a.y
  const len = Math.hypot(dx, dy) || 1
  const cut = Math.min(Math.max(size, 0), Math.max(len - 2, 0))
  if (cut < 0.5)
    return pts
  pts[pts.length - 1] = {
    x: b.x - (dx / len) * cut,
    y: b.y - (dy / len) * cut,
  }
  return pts
}

export function buildArrowPolygon(points, size = ARROW_SIZE) {
  if (!Array.isArray(points) || points.length < 2)
    return null
  const a = points[points.length - 2]
  const b = points[points.length - 1]
  if (!Number.isFinite(a?.x) || !Number.isFinite(b?.x))
    return null
  const dx = b.x - a.x
  const dy = b.y - a.y
  const len = Math.hypot(dx, dy) || 1
  const ux = dx / len
  const uy = dy / len
  const px = -uy
  const py = ux
  // tip 贴在真实端点；描边已 shorten，不会穿三角
  const tip = { x: b.x, y: b.y }
  const base = { x: b.x - ux * size, y: b.y - uy * size }
  const left = { x: base.x + px * size * 0.55, y: base.y + py * size * 0.55 }
  const right = { x: base.x - px * size * 0.55, y: base.y - py * size * 0.55 }
  return `${tip.x},${tip.y} ${left.x},${left.y} ${right.x},${right.y}`
}
