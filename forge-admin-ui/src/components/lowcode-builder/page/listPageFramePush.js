/**
 * 列表画布帧碰撞：拉高/拖动时用「下推」代替静默拒绝，避免高度调不动、块重叠。
 */

export function framesHorizontallyOverlap(a = {}, b = {}) {
  const aLeft = Number(a.x) || 0
  const aRight = aLeft + (Number(a.width) || 0)
  const bLeft = Number(b.x) || 0
  const bRight = bLeft + (Number(b.width) || 0)
  return aLeft < bRight && aRight > bLeft
}

/**
 * 自上而下整理垂直间距：有水平重叠且间距不足 minGap 时，把下方块下推。
 * @param {Array<{ id: string, x: number, y: number, width: number, height: number }>} frames
 * @param {number} minGap
 * @returns {Map<string, number>} id -> 新的 y
 */
export function resolveVerticalPushY(frames = [], minGap = 12) {
  const nextY = new Map()
  const list = frames
    .map(frame => ({
      id: frame.id,
      x: Number(frame.x) || 0,
      y: Number(frame.y) || 0,
      width: Math.max(0, Number(frame.width) || 0),
      height: Math.max(0, Number(frame.height) || 0),
    }))
    .filter(frame => frame.id)
    .sort((left, right) => left.y - right.y || left.x - right.x || String(left.id).localeCompare(String(right.id)))

  for (const frame of list)
    nextY.set(frame.id, frame.y)

  let changed = true
  let guard = 0
  while (changed && guard < list.length + 3) {
    changed = false
    guard += 1
    const ordered = list
      .map(frame => ({ ...frame, y: nextY.get(frame.id) }))
      .sort((left, right) => left.y - right.y || left.x - right.x || String(left.id).localeCompare(String(right.id)))

    for (let i = 0; i < ordered.length; i += 1) {
      const upper = ordered[i]
      for (let j = i + 1; j < ordered.length; j += 1) {
        const lower = ordered[j]
        if (!framesHorizontallyOverlap(upper, lower))
          continue
        const minTop = upper.y + upper.height + minGap
        if (lower.y < minTop) {
          nextY.set(lower.id, minTop)
          lower.y = minTop
          changed = true
        }
      }
    }
  }
  return nextY
}
