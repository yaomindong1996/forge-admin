/**
 * bpmn-js 风格正交线段编辑：拖一段横/竖线平移，自动维护拐点。
 * 同一段反复拖拽必须幂等，禁止每次 pointermove 都插入新点。
 */

const EPS = 1.5
const MAX_STORED_BENDS = 12

export function distToSegment(px, py, a, b) {
  const dx = b.x - a.x
  const dy = b.y - a.y
  const len2 = dx * dx + dy * dy
  if (len2 < 1e-6)
    return Math.hypot(px - a.x, py - a.y)
  let t = ((px - a.x) * dx + (py - a.y) * dy) / len2
  t = Math.max(0, Math.min(1, t))
  return Math.hypot(px - (a.x + t * dx), py - (a.y + t * dy))
}

/** 找到距指针最近的线段（含斜线）；orthoOnly 时只认横/竖 */
export function findNearestSegment(points, x, y, maxDist = 14, orthoOnly = false) {
  if (!Array.isArray(points) || points.length < 2)
    return null
  let best = null
  for (let i = 0; i < points.length - 1; i++) {
    const a = points[i]
    const b = points[i + 1]
    const d = distToSegment(x, y, a, b)
    if (d > maxDist)
      continue
    const horizontal = Math.abs(a.y - b.y) < EPS
    const vertical = Math.abs(a.x - b.x) < EPS
    if (orthoOnly && !horizontal && !vertical)
      continue
    if (!best || d < best.dist)
      best = { index: i, dist: d, horizontal, vertical }
  }
  return best
}

/** 在最近点所在线段上插入拐点（点哪加哪） */
export function insertBendAt(points, x, y, maxDist = 18) {
  const pts = clonePoints(points || [])
  if (pts.length < 2)
    return { points: pts, bendIndex: -1 }
  const seg = findNearestSegment(pts, x, y, maxDist, false)
  if (!seg)
    return { points: pts, bendIndex: -1 }
  const a = pts[seg.index]
  const b = pts[seg.index + 1]
  // 投影到线段，避免点飘到线外
  const dx = b.x - a.x
  const dy = b.y - a.y
  const len2 = dx * dx + dy * dy
  let t = len2 < 1e-6 ? 0 : ((x - a.x) * dx + (y - a.y) * dy) / len2
  t = Math.max(0.05, Math.min(0.95, t))
  const nx = a.x + dx * t
  const ny = a.y + dy * t
  // 插入后先不 simplify：共线点若立刻去掉，用户点一下拐点会消失
  pts.splice(seg.index + 1, 0, { x: nx, y: ny })
  return { points: pts, bendIndex: seg.index }
}

function clonePoints(points) {
  return (points || []).map(p => ({ x: Number(p.x), y: Number(p.y) }))
}

function nearlySame(a, b) {
  return Math.abs(a.x - b.x) < EPS && Math.abs(a.y - b.y) < EPS
}

/** 去掉近重复点与共线冗余点，保留端点 */
export function simplifyOrthoPoints(points) {
  if (!points || points.length < 2)
    return clonePoints(points || [])
  const deduped = [points[0]]
  for (let i = 1; i < points.length; i++) {
    if (!nearlySame(deduped[deduped.length - 1], points[i]))
      deduped.push(points[i])
  }
  if (deduped.length < 3)
    return clonePoints(deduped)

  const out = [deduped[0]]
  for (let i = 1; i < deduped.length - 1; i++) {
    const prev = out[out.length - 1]
    const cur = deduped[i]
    const next = deduped[i + 1]
    const colinear
      = (Math.abs(prev.x - cur.x) < EPS && Math.abs(cur.x - next.x) < EPS)
      || (Math.abs(prev.y - cur.y) < EPS && Math.abs(cur.y - next.y) < EPS)
    if (!colinear)
      out.push({ x: cur.x, y: cur.y })
  }
  out.push(deduped[deduped.length - 1])
  return out
}

/**
 * 是否已是「首段已展开」形态：端点固定，下一点离开端点轴。
 * 已展开时重复拖首段只改坐标，不再 splice。
 */
function firstSegmentExpanded(pts, horizontal) {
  if (pts.length < 3)
    return false
  const a = pts[0]
  const b = pts[1]
  if (horizontal)
    return Math.abs(a.x - b.x) < EPS && Math.abs(a.y - b.y) >= EPS
  return Math.abs(a.y - b.y) < EPS && Math.abs(a.x - b.x) >= EPS
}

function lastSegmentExpanded(pts, horizontal) {
  if (pts.length < 3)
    return false
  const a = pts[pts.length - 2]
  const b = pts[pts.length - 1]
  if (horizontal)
    return Math.abs(a.x - b.x) < EPS && Math.abs(a.y - b.y) >= EPS
  return Math.abs(a.y - b.y) < EPS && Math.abs(a.x - b.x) >= EPS
}

/**
 * 平移第 segIndex 段；返回完整折点（含两端端口）。
 * 拖靠近端口的段时首次插入拐点，之后幂等更新。
 */
export function moveOrthoSegment(points, segIndex, pointer) {
  if (!points || points.length < 2 || !pointer)
    return clonePoints(points || [])
  let pts = clonePoints(points)
  if (segIndex < 0 || segIndex >= pts.length - 1)
    return pts

  const a = pts[segIndex]
  const b = pts[segIndex + 1]
  const horizontal = Math.abs(a.y - b.y) < EPS
  const vertical = Math.abs(a.x - b.x) < EPS
  if (!horizontal && !vertical)
    return pts

  const lastSeg = pts.length - 2

  if (horizontal) {
    const newY = pointer.y
    if (segIndex === 0) {
      if (firstSegmentExpanded(pts, true)) {
        pts[1] = { x: a.x, y: newY }
        pts[2] = { x: pts[2].x, y: newY }
      }
      else {
        pts.splice(1, 0, { x: a.x, y: newY })
        pts[2] = { x: b.x, y: newY }
      }
    }
    else if (segIndex === lastSeg) {
      if (lastSegmentExpanded(pts, true)) {
        pts[pts.length - 2] = { x: a.x, y: newY }
        pts[pts.length - 3] = { x: pts[pts.length - 3].x, y: newY }
      }
      else {
        pts[segIndex] = { x: a.x, y: newY }
        pts.splice(pts.length - 1, 0, { x: b.x, y: newY })
      }
    }
    else {
      pts[segIndex] = { x: a.x, y: newY }
      pts[segIndex + 1] = { x: b.x, y: newY }
    }
  }
  else {
    const newX = pointer.x
    if (segIndex === 0) {
      if (firstSegmentExpanded(pts, false)) {
        pts[1] = { x: newX, y: a.y }
        pts[2] = { x: newX, y: pts[2].y }
      }
      else {
        pts.splice(1, 0, { x: newX, y: a.y })
        pts[2] = { x: newX, y: b.y }
      }
    }
    else if (segIndex === lastSeg) {
      if (lastSegmentExpanded(pts, false)) {
        pts[pts.length - 2] = { x: newX, y: a.y }
        pts[pts.length - 3] = { x: newX, y: pts[pts.length - 3].y }
      }
      else {
        pts[segIndex] = { x: newX, y: a.y }
        pts.splice(pts.length - 1, 0, { x: newX, y: b.y })
      }
    }
    else {
      pts[segIndex] = { x: newX, y: a.y }
      pts[segIndex + 1] = { x: newX, y: b.y }
    }
  }

  return simplifyOrthoPoints(pts)
}

/**
 * 拖拐角时保持正交：相对前后点投影到轴上，避免画出自由曲线。
 */
export function snapBendOrthogonal(points, fullIndex, pointer) {
  const pts = clonePoints(points)
  if (!pts[fullIndex] || !pointer)
    return pts
  const prev = pts[fullIndex - 1]
  const next = pts[fullIndex + 1]
  if (!prev || !next) {
    pts[fullIndex] = { x: pointer.x, y: pointer.y }
    return simplifyOrthoPoints(pts)
  }

  // 目标：prev→cur 与 cur→next 都正交。优先沿「原先主轴」吸附。
  const wasVert = Math.abs(prev.x - pts[fullIndex].x) < EPS
  let x = pointer.x
  let y = pointer.y
  if (wasVert) {
    x = prev.x
    // 下一段尽量水平接到 next
    if (Math.abs(next.y - y) > Math.abs(next.x - x))
      y = next.y
  }
  else {
    y = prev.y
    if (Math.abs(next.x - x) > Math.abs(next.y - y))
      x = next.x
  }
  pts[fullIndex] = { x, y }
  // 若仍不与 next 正交，补一个中间折点（只在必要时）
  if (Math.abs(pts[fullIndex].x - next.x) >= EPS && Math.abs(pts[fullIndex].y - next.y) >= EPS) {
    pts.splice(fullIndex + 1, 0, { x: next.x, y: pts[fullIndex].y })
  }
  return simplifyOrthoPoints(pts)
}

/** 完整折点 → 持久化中间拐点（不含两端）。只去近重复，保留共线点以便「点哪加哪」 */
export function toStoredWaypoints(fullPoints) {
  if (!fullPoints || fullPoints.length <= 2)
    return []
  const deduped = [fullPoints[0]]
  for (let i = 1; i < fullPoints.length; i++) {
    if (!nearlySame(deduped[deduped.length - 1], fullPoints[i]))
      deduped.push(fullPoints[i])
  }
  if (deduped.length <= 2)
    return []
  let bends = deduped.slice(1, -1).map(p => ({ x: p.x, y: p.y }))
  if (bends.length > MAX_STORED_BENDS) {
    const step = (bends.length - 1) / (MAX_STORED_BENDS - 1)
    const sampled = []
    for (let i = 0; i < MAX_STORED_BENDS; i++)
      sampled.push(bends[Math.round(i * step)])
    bends = sampled
  }
  return bends
}

/** 中间拐点（选中态圆形手柄）；过密时只取转折 + 抽样 */
export function listCornerBendIndexes(points) {
  if (!Array.isArray(points) || points.length < 3)
    return []
  const corners = []
  for (let i = 1; i < points.length - 1; i++) {
    const prev = points[i - 1]
    const cur = points[i]
    const next = points[i + 1]
    const inVert = Math.abs(prev.x - cur.x) < EPS
    const outVert = Math.abs(cur.x - next.x) < EPS
    const inHorz = Math.abs(prev.y - cur.y) < EPS
    const outHorz = Math.abs(cur.y - next.y) < EPS
    // 方向变化，或斜线中的用户拐点
    if (inVert !== outVert || inHorz !== outHorz || (!inVert && !inHorz) || (!outVert && !outHorz))
      corners.push(i)
  }
  if (corners.length <= MAX_STORED_BENDS)
    return corners
  const step = (corners.length - 1) / (MAX_STORED_BENDS - 1)
  const sampled = []
  for (let i = 0; i < MAX_STORED_BENDS; i++)
    sampled.push(corners[Math.round(i * step)])
  return [...new Set(sampled)]
}

/**
 * 选中态线段中点胶囊：可上下/左右平移整段（仅横/竖段）
 * @returns {{ segIndex: number, x: number, y: number, horizontal: boolean }[]}
 */
export function listSegmentMidHandles(points) {
  if (!Array.isArray(points) || points.length < 2)
    return []
  const out = []
  for (let i = 0; i < points.length - 1; i++) {
    const a = points[i]
    const b = points[i + 1]
    const horizontal = Math.abs(a.y - b.y) < EPS
    const vertical = Math.abs(a.x - b.x) < EPS
    if (!horizontal && !vertical)
      continue
    const len = Math.hypot(b.x - a.x, b.y - a.y)
    if (len < 28)
      continue
    out.push({
      segIndex: i,
      x: (a.x + b.x) / 2,
      y: (a.y + b.y) / 2,
      horizontal,
    })
  }
  return out
}

/**
 * 鼠标投影到最近「纯横/竖」段上（蓝块跟手）。
 * 必须用「整线最近段」判断：若最近是斜线则返回 null，避免抢走拐点拖动。
 * @returns {{ segIndex: number, x: number, y: number, horizontal: boolean, t: number } | null}
 */
export function projectHoverOnOrthoSegment(points, x, y, maxDist = 16) {
  // 先找最近任意段；只有该段本身是横/竖才出蓝块（斜线留给拐点）
  const seg = findNearestSegment(points, x, y, maxDist, false)
  if (!seg || (!seg.horizontal && !seg.vertical))
    return null
  const a = points[seg.index]
  const b = points[seg.index + 1]
  const dx = b.x - a.x
  const dy = b.y - a.y
  const len2 = dx * dx + dy * dy
  let t = len2 < 1e-6 ? 0.5 : ((x - a.x) * dx + (y - a.y) * dy) / len2
  t = Math.max(0.08, Math.min(0.92, t))
  return {
    segIndex: seg.index,
    x: a.x + dx * t,
    y: a.y + dy * t,
    horizontal: Boolean(seg.horizontal),
    t,
  }
}

/**
 * 拖段时蓝块钉在指定横/竖段上（随折点更新），避免线走了块还停在原地。
 * preferHorizontal: true=横段，false=竖段，null=不限。
 */
export function pinHoverOnSegment(points, segIndex, t = 0.5, preferHorizontal = null) {
  if (!Array.isArray(points) || points.length < 2)
    return null
  let index = segIndex
  const wantH = preferHorizontal
  const match = (i) => {
    const a = points[i]
    const b = points[i + 1]
    if (!a || !b)
      return false
    const horizontal = Math.abs(a.y - b.y) < EPS
    const vertical = Math.abs(a.x - b.x) < EPS
    if (!horizontal && !vertical)
      return false
    if (wantH == null)
      return true
    return wantH ? horizontal : vertical
  }
  if (!match(index)) {
    index = -1
    for (let i = 0; i < points.length - 1; i++) {
      if (match(i)) {
        index = i
        break
      }
    }
  }
  if (index < 0)
    return null
  const a = points[index]
  const b = points[index + 1]
  const tt = Math.max(0.08, Math.min(0.92, Number.isFinite(t) ? t : 0.5))
  const dx = b.x - a.x
  const dy = b.y - a.y
  return {
    segIndex: index,
    x: a.x + dx * tt,
    y: a.y + dy * tt,
    horizontal: Math.abs(a.y - b.y) < EPS,
    t: tt,
  }
}
