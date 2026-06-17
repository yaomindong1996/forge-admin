/**
 * SVG path d 构造工具
 *
 * 输入：waypoints `[{x,y}, ...]`
 * 输出：SVG path 的 d 属性字符串
 *
 * 支持类型：
 *   - 'straight'    M x1,y1 L x2,y2
 *   - 'orthogonal'  按 waypoints 直角折线（M + 多段 L）
 *   - 'rounded'     折线 + 拐角处加 quadratic 圆角（视觉更柔和）
 *   - 'bezier'      首尾两点之间的三次贝塞尔曲线（控制点垂直对齐）
 *
 * 状态着色辅助（getEdgeColor）按 spec 10.8 节统一颜色规范。
 */

const COLORS = Object.freeze({
  default: '#94a3b8',
  completed: '#10b981',
  running: '#3b82f6',
  pending: '#cbd5e1',
  rejected: '#ef4444',
  skipped: '#94a3b8',
  defaultBranch: '#f59e0b',
})

/**
 * 构造 path d 字符串。
 *
 * @param {Array<{x:number,y:number}>} points
 * @param {'straight'|'orthogonal'|'rounded'|'bezier'} type
 * @param {object} [opts]
 * @param {number} [opts.cornerRadius] rounded 模式拐角半径
 * @returns {string}
 */
export function buildPathD(points, type = 'orthogonal', opts = {}) {
  if (!Array.isArray(points) || points.length < 2)
    return ''
  const cornerRadius = Math.max(0, Number(opts.cornerRadius) || 8)

  if (type === 'bezier') {
    const a = points[0]
    const b = points[points.length - 1]
    const dy = (b.y - a.y) / 2
    return `M ${a.x},${a.y} C ${a.x},${a.y + dy} ${b.x},${b.y - dy} ${b.x},${b.y}`
  }

  if (type === 'straight' || points.length === 2) {
    const [a, b] = [points[0], points[points.length - 1]]
    return `M ${a.x},${a.y} L ${b.x},${b.y}`
  }

  if (type === 'rounded')
    return buildRoundedOrthogonal(points, cornerRadius)

  // orthogonal：默认折线，按 waypoints 直接连接
  let d = `M ${points[0].x},${points[0].y}`
  for (let i = 1; i < points.length; i += 1)
    d += ` L ${points[i].x},${points[i].y}`
  return d
}

/**
 * 圆角折线：在每个内部转角处用二次贝塞尔曲线（Q）替代尖角。
 */
function buildRoundedOrthogonal(points, r) {
  if (points.length < 3 || r <= 0)
    return buildPathD(points, 'orthogonal')

  let d = `M ${points[0].x},${points[0].y}`
  for (let i = 1; i < points.length - 1; i += 1) {
    const prev = points[i - 1]
    const cur = points[i]
    const next = points[i + 1]
    // 拐角缩进点
    const enter = pointAlong(prev, cur, r)
    const exit = pointAlong(next, cur, r)
    d += ` L ${enter.x},${enter.y} Q ${cur.x},${cur.y} ${exit.x},${exit.y}`
  }
  const last = points[points.length - 1]
  d += ` L ${last.x},${last.y}`
  return d
}

/**
 * 从 from 朝 to 方向，距离 to dist 的点。
 * 若 dist 大于线段长度，使用线段中点保护，避免越界。
 */
function pointAlong(from, to, dist) {
  const dx = from.x - to.x
  const dy = from.y - to.y
  const len = Math.sqrt(dx * dx + dy * dy)
  if (len === 0)
    return { x: to.x, y: to.y }
  const k = Math.min(dist, len / 2) / len
  return {
    x: to.x + dx * k,
    y: to.y + dy * k,
  }
}

/**
 * 取 waypoints 中点（用于条件标签定位）。
 */
export function getEdgeMidpoint(points) {
  if (!Array.isArray(points) || points.length === 0)
    return { x: 0, y: 0 }
  if (points.length === 1)
    return { ...points[0] }
  // 累计长度找 50% 处
  let total = 0
  const lens = []
  for (let i = 1; i < points.length; i += 1) {
    const dx = points[i].x - points[i - 1].x
    const dy = points[i].y - points[i - 1].y
    const seg = Math.sqrt(dx * dx + dy * dy)
    lens.push(seg)
    total += seg
  }
  if (total === 0)
    return { ...points[0] }
  const half = total / 2
  let acc = 0
  for (let i = 0; i < lens.length; i += 1) {
    if (acc + lens[i] >= half) {
      const t = (half - acc) / lens[i]
      const a = points[i]
      const b = points[i + 1]
      return { x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t }
    }
    acc += lens[i]
  }
  return { ...points[points.length - 1] }
}

/**
 * 边状态颜色解析。
 *
 * 优先级：edge.isDefault → defaultBranch；status === 'rejected'/'completed'/...; 否则 default。
 */
export function getEdgeColor(edge, status) {
  if (status && COLORS[status])
    return COLORS[status]
  if (edge?.isDefault)
    return COLORS.defaultBranch
  return COLORS.default
}

/**
 * 边虚线 dash-array：pending / rejected / skipped 使用虚线。
 */
export function getEdgeDashArray(status) {
  if (status === 'pending' || status === 'rejected' || status === 'skipped')
    return '6 4'
  return null
}

export { COLORS as EDGE_COLORS }
