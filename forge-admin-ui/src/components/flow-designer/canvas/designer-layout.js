import { layoutFlow } from './layout-engine.js'

/** 只调整编辑画布几何；不改 BPMN 导出坐标或节点/边的顺序与协议。 */
export function layoutDesignerFlow(flow) {
  // 卡片更紧凑 + 间距加大，减轻节点堆叠感
  const layout = layoutFlow(flow, { NODE_WIDTH: 200, NODE_HEIGHT: 56, V_GAP: 72, H_GAP: 96, MARGIN_TOP: 92 })
  layout.branchCards = new Map()
  layout.branchJunctions = new Map()
  const hasManual = (flow.nodes || []).some(node => Number.isFinite(node?.uiPosition?.x) && Number.isFinite(node?.uiPosition?.y))

  for (const gateway of flow.nodes || []) {
    if (!['condition', 'parallel', 'inclusive'].includes(gateway.nodeType))
      continue
    const edges = flow.edges.filter(edge => edge.source === gateway.id)
    const source = layout.nodePositions.get(gateway.id)
    const targets = edges.map(edge => layout.nodePositions.get(edge.target))
    if (!source || edges.length < 2 || targets.some(target => !target))
      continue

    if (hasManual) {
      // 自由拖拽后仍保留并行/条件分支卡，只按当前坐标重画分叉，禁止整图下移。
      applyFreeLayoutBranchCards(layout, gateway, edges, source, targets)
      continue
    }

    if (!canUseBranchCards({ flow, edges, source, targets, gateway }))
      continue
    makeRoomForConditions(layout, source, gateway)
    applyTreeBranchCards(layout, gateway, edges, source, targets)
  }
  return layout
}

function branchCardOffsetY(gateway) {
  // 并行用窄标签，少占纵向空隙
  return gateway?.nodeType === 'parallel' ? 40 : 60
}

function applyTreeBranchCards(layout, gateway, edges, source, targets) {
  const cardOffset = branchCardOffsetY(gateway)
  const junctionY = targets[0].y - (cardOffset + 56)
  layout.branchJunctions.set(gateway.id, junctionY)
  edges.forEach((edge, index) => {
    const target = targets[index]
    const sourceX = source.x + source.width / 2
    const targetX = target.x + target.width / 2
    layout.branchCards.set(edge.id, { x: targetX, y: target.y - cardOffset })
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

/**
 * 自由布局分支卡：不要求同层等宽间距，只要网关下方有足够空隙就画分叉 + 卡片。
 * 避免拖一下其它节点后并行分支卡消失、只剩空线。
 */
function applyFreeLayoutBranchCards(layout, gateway, edges, source, targets) {
  const sourceBottom = source.y + source.height
  const minTargetY = Math.min(...targets.map(target => target.y))
  // 目标整体在网关上方或贴太近：仍贴卡片，但不强行改边（避免穿模）
  if (minTargetY - sourceBottom < 56) {
    edges.forEach((edge, index) => {
      const target = targets[index]
      layout.branchCards.set(edge.id, {
        x: target.x + target.width / 2,
        y: Math.min(target.y - 36, sourceBottom + 28),
      })
    })
    return
  }

  const compact = gateway?.nodeType === 'parallel'
  const junctionY = Math.max(
    sourceBottom + 24,
    Math.min(minTargetY - (compact ? 56 : 88), sourceBottom + (compact ? 48 : 72)),
  )
  layout.branchJunctions.set(gateway.id, junctionY)
  const sourceX = source.x + source.width / 2
  const cardOffset = branchCardOffsetY(gateway)

  edges.forEach((edge, index) => {
    const target = targets[index]
    const targetX = target.x + target.width / 2
    layout.branchCards.set(edge.id, {
      x: targetX,
      y: Math.min(target.y - Math.max(28, cardOffset - 12), junctionY + (compact ? 24 : 36)),
    })
    layout.edgePaths.set(edge.id, {
      type: 'rounded',
      points: [
        { x: sourceX, y: sourceBottom },
        { x: sourceX, y: junctionY },
        { x: targetX, y: junctionY },
        { x: targetX, y: target.y },
      ],
    })
  })
}

function canUseBranchCards({ flow, edges, source, targets, gateway }) {
  // 共享后继、跨层和驳回回流不能铺满宽卡，继续使用现有避让标签，避免遮挡别的路径。
  if (!source || edges.length < 2 || targets.some(target => !target))
    return false
  const minTargetWidth = gateway?.nodeType === 'parallel' ? 100 : 180
  const minVertical = gateway?.nodeType === 'parallel' ? 56 : 72
  if (targets.some(target => target.width < minTargetWidth
    || target.y !== targets[0].y || target.y - source.y - source.height < minVertical)) {
    return false
  }
  if (edges.some(edge => flow.edges.filter(item => item.target === edge.target).length !== 1))
    return false
  // 并行窄标签约 118px，条件宽卡 280px
  const minGap = gateway?.nodeType === 'parallel' ? 132 : 280
  const xs = targets.map(target => target.x).sort((a, b) => a - b)
  return xs.every((x, index) => index === 0 || x - xs[index - 1] >= minGap)
}

function makeRoomForConditions(layout, source, gateway) {
  // 仅在需要条件卡的分支处增加一行；普通链路保持紧凑，避免整张流程缩成小字。
  const lift = gateway?.nodeType === 'parallel' ? 36 : 56
  const cutoff = source.y + source.height
  for (const position of layout.nodePositions.values()) {
    if (position.y > cutoff)
      position.y += lift
  }
  for (const path of layout.edgePaths.values()) {
    for (const point of path.points) {
      if (point.y > cutoff)
        point.y += lift
    }
  }
  layout.canvasBounds.maxY += lift
}
