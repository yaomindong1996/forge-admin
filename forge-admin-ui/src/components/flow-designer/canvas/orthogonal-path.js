/**
 * 四向锚点连线：支持手动 dock（边上任意点）+ 用户拐点（自由折线）。
 */
import { resolveDockPoints } from './edge-dock.js'

function route(start, end, offset = 0) {
  const sameX = Math.abs(start.x - end.x) < 1
  const sameY = Math.abs(start.y - end.y) < 1
  if (sameX || sameY) {
    return {
      type: 'straight',
      points: [
        { x: start.x, y: start.y },
        { x: end.x, y: end.y },
      ],
    }
  }

  const verticalStart = start.side === 'top' || start.side === 'bottom'
  const verticalEnd = end.side === 'top' || end.side === 'bottom'

  if (verticalStart && verticalEnd) {
    const midY = (start.y + end.y) / 2 + offset
    return {
      type: 'orthogonal',
      points: [
        { x: start.x, y: start.y },
        { x: start.x, y: midY },
        { x: end.x, y: midY },
        { x: end.x, y: end.y },
      ],
    }
  }

  if (!verticalStart && !verticalEnd) {
    const midX = (start.x + end.x) / 2 + offset
    return {
      type: 'orthogonal',
      points: [
        { x: start.x, y: start.y },
        { x: midX, y: start.y },
        { x: midX, y: end.y },
        { x: end.x, y: end.y },
      ],
    }
  }

  if (verticalStart) {
    return {
      type: 'orthogonal',
      points: [
        { x: start.x, y: start.y },
        { x: start.x, y: end.y },
        { x: end.x, y: end.y },
      ],
    }
  }
  return {
    type: 'orthogonal',
    points: [
      { x: start.x, y: start.y },
      { x: end.x, y: start.y },
      { x: end.x, y: end.y },
    ],
  }
}

/**
 * 接入结束节点顶边：始终从上方落入，禁止从侧面/底下绕一圈再抬头。
 */
function routeToEndTop(start, end) {
  const approachY = end.y - 32
  const sameX = Math.abs(start.x - end.x) < 1

  if (sameX) {
    // 已在正上方或正下方：竖直接入；若在下方先抬到 approach 再落下
    if (start.y <= end.y) {
      return {
        type: 'straight',
        points: [
          { x: start.x, y: start.y },
          { x: end.x, y: end.y },
        ],
      }
    }
    return {
      type: 'orthogonal',
      points: [
        { x: start.x, y: start.y },
        { x: start.x, y: approachY },
        { x: end.x, y: approachY },
        { x: end.x, y: end.y },
      ],
    }
  }

  // 起点已在顶边之上：横移到顶心正上再竖直落下
  if (start.y <= approachY) {
    return {
      type: 'orthogonal',
      points: [
        { x: start.x, y: start.y },
        { x: end.x, y: start.y },
        { x: end.x, y: end.y },
      ],
    }
  }

  // 起点偏下或侧面齐平：先抬到顶边上方，再横移，再落入（避免绕底）
  return {
    type: 'orthogonal',
    points: [
      { x: start.x, y: start.y },
      { x: start.x, y: approachY },
      { x: end.x, y: approachY },
      { x: end.x, y: end.y },
    ],
  }
}

/**
 * @param {{x,y,width,height}} source
 * @param {{x,y,width,height}} target
 * @param {{ lane?: number, sourceDock?: object, targetDock?: object, endTarget?: boolean }} [opts]
 */
export function buildOrthogonalPath(source, target, opts = {}) {
  if (!source || !target)
    return { points: [], type: 'orthogonal' }
  const endTarget = Boolean(opts.endTarget)
  const { start, end } = resolveDockPoints(
    source,
    target,
    opts.sourceDock,
    opts.targetDock,
    { endTarget },
  )
  // 结束节点：一律从上方落入顶心，避免侧面齐平横穿或绕底抬头
  if (endTarget)
    return routeToEndTop(start, { ...end, side: 'top' })
  const lane = Number(opts.lane) || 0
  return route(start, end, lane * 14)
}

/**
 * 带用户拐点：源锚点 → 拐点… → 目标锚点（允许斜线）。
 */
export function buildPathWithWaypoints(source, target, waypoints = [], opts = {}) {
  if (!source || !target)
    return { points: [], type: 'orthogonal' }
  const { start, end } = resolveDockPoints(
    source,
    target,
    opts.sourceDock,
    opts.targetDock,
    { endTarget: Boolean(opts.endTarget) },
  )
  const bends = waypoints
    .filter(p => Number.isFinite(p?.x) && Number.isFinite(p?.y))
    .map(p => ({ x: Number(p.x), y: Number(p.y) }))

  // 结束节点：拐点若绕到圆下方，丢弃重走上方正交，避免椭圆绕底
  if (opts.endTarget) {
    const below = bends.some(b => b.y > target.y + target.height * 0.5)
    if (below || !bends.length)
      return buildOrthogonalPath(source, target, opts)
  }

  if (!bends.length)
    return buildOrthogonalPath(source, target, opts)
  const points = [
    { x: start.x, y: start.y },
    ...bends,
    { x: end.x, y: end.y },
  ]
  return {
    type: points.length === 2 ? 'straight' : 'polyline',
    points,
  }
}

export function recomputeBounds(nodePositions, edgePaths) {
  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity
  for (const pos of nodePositions.values()) {
    minX = Math.min(minX, pos.x)
    minY = Math.min(minY, pos.y)
    maxX = Math.max(maxX, pos.x + pos.width)
    maxY = Math.max(maxY, pos.y + pos.height)
  }
  for (const path of edgePaths.values()) {
    for (const p of path.points || []) {
      minX = Math.min(minX, p.x)
      minY = Math.min(minY, p.y)
      maxX = Math.max(maxX, p.x)
      maxY = Math.max(maxY, p.y)
    }
  }
  if (!Number.isFinite(minX))
    return { minX: 0, minY: 0, maxX: 0, maxY: 0 }
  return { minX, minY, maxX, maxY }
}
