/** 把「＋」放在连线最长段中点，避免正交折线时漂到节点正下方。 */

export function longestSegmentMidpoint(points) {
  if (!Array.isArray(points) || points.length < 2)
    return null
  let best = null
  for (let i = 1; i < points.length; i += 1) {
    const a = points[i - 1]
    const b = points[i]
    const length = Math.hypot(b.x - a.x, b.y - a.y)
    if (!best || length > best.length)
      best = { a, b, length }
  }
  if (!best || best.length < 12)
    return null
  return {
    x: (best.a.x + best.b.x) / 2,
    y: (best.a.y + best.b.y) / 2,
  }
}

export function verticalSegmentMidpoint(sourcePos, targetPos) {
  const sourceBottomY = sourcePos.y + sourcePos.height
  if (!targetPos)
    return sourceBottomY + 28
  const targetTopY = targetPos.y
  const gap = Math.max(targetTopY - sourceBottomY, 44)
  return sourceBottomY + gap / 2
}

export function buildAddButtonPositions({ nodes, edges, getNode, nodePositions, edgePaths }) {
  const list = []
  for (const edge of edges || []) {
    const source = getNode(edge.source)
    if (!source || ['condition', 'parallel', 'inclusive', 'end'].includes(source.nodeType))
      continue
    const sourcePos = nodePositions.get(edge.source)
    if (!sourcePos)
      continue
    const targetPos = nodePositions.get(edge.target)
    const path = edgePaths.get(edge.id)?.points
    list.push({
      id: edge.id,
      afterId: edge.source,
      position: longestSegmentMidpoint(path) || {
        x: sourcePos.x + sourcePos.width / 2,
        y: verticalSegmentMidpoint(sourcePos, targetPos),
      },
    })
  }
  return list
}
