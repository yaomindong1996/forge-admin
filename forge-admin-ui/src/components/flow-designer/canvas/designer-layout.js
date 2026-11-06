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

/** 线旁胶囊约 24px 高 */
function branchChipHalfHeight() {
  return 12
}

/** 横线 → 胶囊、胶囊 → 目标 的最小空隙 */
function branchGap() {
  return 16
}

function hasManualWaypoints(edge) {
  return Array.isArray(edge?.uiWaypoints) && edge.uiWaypoints.length > 0
}

/** 横分叉 + 竖降到目标；卡片挂在竖段中部，与横线、目标都留出空隙 */
function placeBranchGeometry(layout, gateway, edges, source, targets, junctionY) {
  const sourceX = source.x + source.width / 2
  const sourceBottom = source.y + source.height
  const half = branchChipHalfHeight()
  const gap = branchGap()
  layout.branchJunctions.set(gateway.id, junctionY)

  edges.forEach((edge, index) => {
    const target = targets[index]
    const targetX = target.x + target.width / 2
    // 理想：横线下方 gap + 半高；再与目标上方 gap + 半高取更靠上的，保证不压到目标
    const idealCardY = junctionY + gap + half
    const maxCardY = target.y - gap - half
    const cardY = Math.min(idealCardY, maxCardY)
    layout.branchCards.set(edge.id, { x: targetX, y: cardY })
    if (hasManualWaypoints(edge))
      return
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

function applyTreeBranchCards(layout, gateway, edges, source, targets) {
  const half = branchChipHalfHeight()
  const gap = branchGap()
  // 网关底 → 横线 → 空隙 → 胶囊标注 → 空隙 → 目标
  const junctionY = targets[0].y - (half * 2 + gap * 2 + 28)
  placeBranchGeometry(layout, gateway, edges, source, targets, junctionY)
}

/**
 * 自由布局分支卡：不要求同层等宽间距，只要网关下方有足够空隙就画分叉 + 卡片。
 * 避免拖一下其它节点后并行分支卡消失、只剩空线。
 */
function applyFreeLayoutBranchCards(layout, gateway, edges, source, targets) {
  const sourceBottom = source.y + source.height
  const minTargetY = Math.min(...targets.map(target => target.y))
  const half = branchChipHalfHeight()
  const gap = branchGap()
  const needed = half * 2 + gap * 2 + 20
  // 目标整体在网关上方或贴太近：仍贴标注，但不强行改边（避免穿模）
  if (minTargetY - sourceBottom < needed) {
    edges.forEach((edge, index) => {
      const target = targets[index]
      layout.branchCards.set(edge.id, {
        x: target.x + target.width / 2,
        y: Math.min(target.y - half - gap, sourceBottom + half + gap),
      })
    })
    return
  }

  // 横线尽量居中于「网关底 ↔ 目标」可用带，并保证胶囊上下都有 gap
  const minJunction = sourceBottom + 24
  const maxJunction = minTargetY - (half * 2 + gap * 2)
  const junctionY = Math.max(minJunction, Math.min(maxJunction, (sourceBottom + minTargetY) / 2 - half))
  placeBranchGeometry(layout, gateway, edges, source, targets, junctionY)
}

function canUseBranchCards({ flow, edges, source, targets, gateway }) {
  // 共享后继、跨层和驳回回流不能铺满宽卡，继续使用现有避让标签，避免遮挡别的路径。
  if (!source || edges.length < 2 || targets.some(target => !target))
    return false
  const minVertical = 56
  if (targets.some(target => target.y !== targets[0].y
    || target.y - source.y - source.height < minVertical)) {
    return false
  }
  if (edges.some(edge => flow.edges.filter(item => item.target === edge.target).length !== 1))
    return false
  // 线旁胶囊约 132px，分支间距不必再按大卡片预留
  const minGap = 148
  const xs = targets.map(target => target.x).sort((a, b) => a - b)
  return xs.every((x, index) => index === 0 || x - xs[index - 1] >= minGap)
}

function makeRoomForConditions(layout, source, _gateway) {
  // 为「横线 + 空隙 + 条件胶囊」腾出少量纵向空间即可
  const lift = 36
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
