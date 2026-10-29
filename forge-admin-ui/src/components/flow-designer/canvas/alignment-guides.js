/**
 * 拖拽节点时的对齐辅助线：与其它节点的边/中心对齐，或判断连线是否近似竖直/水平。
 */

const DEFAULT_THRESHOLD = 6

function centers(box) {
  return {
    cx: box.x + box.width / 2,
    cy: box.y + box.height / 2,
    left: box.x,
    right: box.x + box.width,
    top: box.y,
    bottom: box.y + box.height,
  }
}

/**
 * @param {{ x:number,y:number,width:number,height:number }} dragging
 * @param {Array<{ id?:string, x:number,y:number,width:number,height:number }>} others
 * @param {number} [threshold]
 * @returns {{ snapX: number|null, snapY: number|null, lines: Array<{ orientation:'v'|'h', position:number, start:number, end:number }> }}
 */
export function computeAlignmentGuides(dragging, others = [], threshold = DEFAULT_THRESHOLD) {
  if (!dragging || !others.length) {
    return { snapX: null, snapY: null, lines: [] }
  }
  const d = centers(dragging)
  const xCandidates = []
  const yCandidates = []

  for (const other of others) {
    if (!other || other === dragging)
      continue
    const o = centers(other)
    const pairsX = [
      [d.left, o.left],
      [d.left, o.cx],
      [d.left, o.right],
      [d.cx, o.left],
      [d.cx, o.cx],
      [d.cx, o.right],
      [d.right, o.left],
      [d.right, o.cx],
      [d.right, o.right],
    ]
    const pairsY = [
      [d.top, o.top],
      [d.top, o.cy],
      [d.top, o.bottom],
      [d.cy, o.top],
      [d.cy, o.cy],
      [d.cy, o.bottom],
      [d.bottom, o.top],
      [d.bottom, o.cy],
      [d.bottom, o.bottom],
    ]
    for (const [from, to] of pairsX) {
      const delta = to - from
      if (Math.abs(delta) <= threshold)
        xCandidates.push({ delta, guide: to, other })
    }
    for (const [from, to] of pairsY) {
      const delta = to - from
      if (Math.abs(delta) <= threshold)
        yCandidates.push({ delta, guide: to, other })
    }
  }

  const bestX = pickBest(xCandidates)
  const bestY = pickBest(yCandidates)
  const snapX = bestX ? dragging.x + bestX.delta : null
  const snapY = bestY ? dragging.y + bestY.delta : null

  const lines = []
  const finalBox = {
    ...dragging,
    x: snapX ?? dragging.x,
    y: snapY ?? dragging.y,
  }
  const f = centers(finalBox)

  if (bestX) {
    const o = centers(bestX.other)
    lines.push({
      orientation: 'v',
      position: bestX.guide,
      start: Math.min(f.top, o.top) - 8,
      end: Math.max(f.bottom, o.bottom) + 8,
    })
  }
  if (bestY) {
    const o = centers(bestY.other)
    lines.push({
      orientation: 'h',
      position: bestY.guide,
      start: Math.min(f.left, o.left) - 8,
      end: Math.max(f.right, o.right) + 8,
    })
  }

  return { snapX, snapY, lines: dedupeLines(lines) }
}

function pickBest(candidates) {
  if (!candidates.length)
    return null
  return candidates.reduce((best, cur) => (Math.abs(cur.delta) < Math.abs(best.delta) ? cur : best))
}

function dedupeLines(lines) {
  const seen = new Set()
  return lines.filter((line) => {
    const key = `${line.orientation}:${Math.round(line.position)}`
    if (seen.has(key))
      return false
    seen.add(key)
    return true
  })
}
