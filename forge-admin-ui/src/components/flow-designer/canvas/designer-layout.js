import { layoutFlow } from './layout-engine.js'

/** 只调整编辑画布几何；不改 BPMN 导出坐标或节点/边的顺序与协议。 */
export function layoutDesignerFlow(flow) {
  const layout = layoutFlow(flow, { NODE_WIDTH: 280, NODE_HEIGHT: 76, V_GAP: 56, H_GAP: 72, MARGIN_TOP: 92 })
  layout.branchCards = new Map()
  layout.branchJunctions = new Map()
  for (const gateway of flow.nodes || []) {
    if (!['condition', 'parallel', 'inclusive'].includes(gateway.nodeType))
      continue
    const edges = flow.edges.filter(edge => edge.source === gateway.id)
    const source = layout.nodePositions.get(gateway.id)
    const targets = edges.map(edge => layout.nodePositions.get(edge.target))
    if (!canUseBranchCards({ flow, edges, source, targets }))
      continue
    makeRoomForConditions(layout, source)
    const junctionY = targets[0].y - 116
    layout.branchJunctions.set(gateway.id, junctionY)
    edges.forEach((edge, index) => {
      const target = targets[index]
      const sourceX = source.x + source.width / 2
      const targetX = target.x + target.width / 2
      layout.branchCards.set(edge.id, { x: targetX, y: target.y - 60 })
      layout.edgePaths.set(edge.id, {
        type: 'rounded',
        points: [
          { x: sourceX, y: source.y + source.height },
          { x: sourceX, y: junctionY },
          { x: targetX, y: junctionY },
          { x: targetX, y: target.y },
        ],
      })
    })
  }
  return layout
}

function canUseBranchCards({ flow, edges, source, targets }) {
  // 共享后继、跨层和驳回回流不能铺满宽卡，继续使用现有避让标签，避免遮挡别的路径。
  if (!source || edges.length < 2 || targets.some(target => !target))
    return false
  if (targets.some(target => target.width < 224
    || target.y !== targets[0].y || target.y - source.y - source.height < 72)) {
    return false
  }
  if (edges.some(edge => flow.edges.filter(item => item.target === edge.target).length !== 1))
    return false
  const xs = targets.map(target => target.x).sort((a, b) => a - b)
  return xs.every((x, index) => index === 0 || x - xs[index - 1] >= 312)
}

function makeRoomForConditions(layout, source) {
  // 仅在需要条件卡的分支处增加一行；普通链路保持紧凑，避免整张流程缩成小字。
  const cutoff = source.y + source.height
  for (const position of layout.nodePositions.values()) {
    if (position.y > cutoff)
      position.y += 56
  }
  for (const path of layout.edgePaths.values()) {
    for (const point of path.points) {
      if (point.y > cutoff)
        point.y += 56
    }
  }
  layout.canvasBounds.maxY += 56
}
