/**
 * 节点边框锚点（对接点）：四边任意位置，不限于中心。
 * dock = { side: 'top'|'right'|'bottom'|'left', t: 0~1 }
 */

const SIDES = ['top', 'right', 'bottom', 'left']

export function clamp01(v, lo = 0.02, hi = 0.98) {
  const n = Number(v)
  if (!Number.isFinite(n))
    return 0.5
  return Math.min(hi, Math.max(lo, n))
}

export function normalizeDock(dock) {
  if (!dock || !SIDES.includes(dock.side))
    return null
  return { side: dock.side, t: clamp01(dock.t, 0, 1) }
}

/** 由 dock 得到画布坐标 */
export function pointFromDock(box, dock) {
  if (!box)
    return null
  const d = normalizeDock(dock) || { side: 'bottom', t: 0.5 }
  const t = clamp01(d.t)
  const { x, y, width: w, height: h } = box
  switch (d.side) {
    case 'top':
      return { x: x + w * t, y, side: 'top', t }
    case 'bottom':
      return { x: x + w * t, y: y + h, side: 'bottom', t }
    case 'left':
      return { x, y: y + h * t, side: 'left', t }
    case 'right':
      return { x: x + w, y: y + h * t, side: 'right', t }
    default:
      return { x: x + w / 2, y: y + h, side: 'bottom', t: 0.5 }
  }
}

/** 四边中心（UI 端口默认位置） */
export function sideCenterDock(side) {
  return { side, t: 0.5 }
}

/** 将指针投影到指定边 → dock（边上任意 t，不限中心） */
export function dockOnSideFromPoint(box, side, px, py) {
  if (!box || !SIDES.includes(side))
    return sideCenterDock(side || 'bottom')
  const { x, y, width: w, height: h } = box
  if (side === 'top' || side === 'bottom')
    return { side, t: clamp01(w > 0 ? (px - x) / w : 0.5) }
  return { side, t: clamp01(h > 0 ? (py - y) / h : 0.5) }
}

/**
 * 指定边落点：默认吸到中心；只有明显点在边的两侧才保留偏移。
 * 避免点整边热区时起点飘到「顶框/偏左」而不是中间锚点。
 */
export function dockOnSidePreferCenter(box, side, px, py, centerSnap = 0.2) {
  const dock = dockOnSideFromPoint(box, side, px, py)
  if (Math.abs(dock.t - 0.5) <= centerSnap)
    return sideCenterDock(side)
  return dock
}

/** 结束节点只允许顶边接入（与可见锚点一致） */
export const END_INCOMING_DOCK = Object.freeze({ side: 'top', t: 0.5 })

export function isEndIncomingSide(side) {
  return side === 'top'
}

/**
 * 按源/目标相对位置选对接边（中心），避免点在卡片下半误挂到底边导致线穿心。
 * @param {{ endTarget?: boolean }} [opts] 目标为结束节点时强制顶边
 */
export function facingCenterDocks(source, target, opts = {}) {
  const auto = autoPickDocks(source, target)
  return {
    sourceDock: { side: auto.start.side, t: 0.5 },
    targetDock: opts.endTarget ? { ...END_INCOMING_DOCK } : { side: auto.end.side, t: 0.5 },
  }
}

const FREE_SLOT_TS = Object.freeze([0.2, 0.35, 0.5, 0.65, 0.8])

/**
 * 边上中心已被占用时，选最近空闲 t，避免多条线叠在同一中心点。
 * @param {number[]} occupiedTs 同侧已有对接点的 t
 * @param {number} [preferT=0.5]
 * @param {number} [minGap=0.12]
 */
export function pickFreeTOnSide(occupiedTs = [], preferT = 0.5, minGap = 0.12) {
  const occupied = occupiedTs
    .map(t => clamp01(t, 0, 1))
    .filter(Number.isFinite)
  const prefer = clamp01(preferT, 0, 1)
  const isFree = (t) => occupied.every(o => Math.abs(o - t) >= minGap)
  if (isFree(prefer))
    return prefer
  let best = null
  let bestDist = Infinity
  for (const slot of FREE_SLOT_TS) {
    if (!isFree(slot))
      continue
    const dist = Math.abs(slot - prefer)
    if (dist < bestDist) {
      best = slot
      bestDist = dist
    }
  }
  if (best != null)
    return best
  // 全满时仍避开最近占用点，轻微偏移
  const nearest = occupied.reduce((a, b) => (Math.abs(a - prefer) <= Math.abs(b - prefer) ? a : b), prefer)
  return clamp01(nearest >= prefer ? nearest + minGap : nearest - minGap)
}

export function resolveDockAvoidingOccupied(dock, occupiedTs) {
  const d = normalizeDock(dock) || { side: 'bottom', t: 0.5 }
  return { side: d.side, t: pickFreeTOnSide(occupiedTs, d.t) }
}

export function portsOf(box) {
  return {
    top: pointFromDock(box, { side: 'top', t: 0.5 }),
    bottom: pointFromDock(box, { side: 'bottom', t: 0.5 }),
    left: pointFromDock(box, { side: 'left', t: 0.5 }),
    right: pointFromDock(box, { side: 'right', t: 0.5 }),
  }
}

/**
 * 将点投影到矩形边框最近位置 → dock
 * （对齐 bpmn.io：可落在边上任意处，不限中心）
 */
export function nearestDockOnBox(box, px, py) {
  if (!box || !Number.isFinite(px) || !Number.isFinite(py))
    return { side: 'bottom', t: 0.5 }
  const { x, y, width: w, height: h } = box
  const right = x + w
  const bottom = y + h
  const cx = Math.min(right, Math.max(x, px))
  const cy = Math.min(bottom, Math.max(y, py))

  const distTop = Math.abs(py - y)
  const distBottom = Math.abs(py - bottom)
  const distLeft = Math.abs(px - x)
  const distRight = Math.abs(px - right)
  const min = Math.min(distTop, distBottom, distLeft, distRight)

  if (min === distTop)
    return { side: 'top', t: clamp01(w > 0 ? (cx - x) / w : 0.5) }
  if (min === distBottom)
    return { side: 'bottom', t: clamp01(w > 0 ? (cx - x) / w : 0.5) }
  if (min === distLeft)
    return { side: 'left', t: clamp01(h > 0 ? (cy - y) / h : 0.5) }
  return { side: 'right', t: clamp01(h > 0 ? (cy - y) / h : 0.5) }
}

/** 无手动 dock 时，按相对位置自动选边中心 */
export function autoPickDocks(source, target) {
  const s = portsOf(source)
  const t = portsOf(target)
  const dx = (target.x + target.width / 2) - (source.x + source.width / 2)
  const dy = (target.y + target.height / 2) - (source.y + source.height / 2)
  const gapX = dx >= 0
    ? target.x - (source.x + source.width)
    : source.x - (target.x + target.width)
  const gapY = dy >= 0
    ? target.y - (source.y + source.height)
    : source.y - (target.y + target.height)

  if (gapY >= 12 && Math.abs(dy) >= Math.abs(dx) * 0.35) {
    return dy >= 0
      ? { start: s.bottom, end: t.top }
      : { start: s.top, end: t.bottom }
  }
  if (gapX >= 12) {
    return dx >= 0
      ? { start: s.right, end: t.left }
      : { start: s.left, end: t.right }
  }
  if (Math.abs(dy) >= Math.abs(dx)) {
    return dy >= 0
      ? { start: s.bottom, end: t.top }
      : { start: s.top, end: t.bottom }
  }
  return dx >= 0
    ? { start: s.right, end: t.left }
    : { start: s.left, end: t.right }
}

/**
 * @param {object} [sourceDock]
 * @param {object} [targetDock]
 * @param {{ endTarget?: boolean }} [opts]
 */
export function resolveDockPoints(source, target, sourceDock, targetDock, opts = {}) {
  const auto = autoPickDocks(source, target)
  const start = normalizeDock(sourceDock)
    ? pointFromDock(source, sourceDock)
    : auto.start
  // 结束节点：无视历史 left/bottom dock，一律顶心，避免线挂侧面或绕底穿圈
  const resolvedTargetDock = opts.endTarget
    ? END_INCOMING_DOCK
    : (normalizeDock(targetDock) ? targetDock : null)
  const end = resolvedTargetDock
    ? pointFromDock(target, resolvedTargetDock)
    : auto.end
  return { start, end }
}
