/**
 * BPMN 自动布局算法（用于 JSON→XML 时生成 BPMNDiagram）。
 *
 * 输入：flowJson（含 nodes / edges + 已 markBranches 的 branchId / mergeNode）
 * 输出：{ nodePositions: Map<nodeId, {x, y, width, height}>, edgeWaypoints: Map<edgeId, [{x,y}, ...]> }
 *
 * 算法（递归）：
 * 1. 从 startNode 开始，y = MARGIN_TOP，x = 中线
 * 2. 线性链：y 递增 NODE_HEIGHT + V_GAP，x 不变
 * 3. 网关：递归测算每个分支的子树宽度，分支横向并排
 * 4. 汇合点（入度 >= 2，标记 mergeNode=true）：x 回到网关中线
 * 5. 连线 waypoints：source 底中点 → target 顶中点（直线/折线）
 *
 * 退化场景：
 * - 没有 startNode：按 nodes 数组顺序纵向排列
 * - 复杂图（循环/多入边但非汇合）：visited 防死循环；未访问节点放在右侧补齐
 */

const DEFAULT_OPTS = {
  NODE_WIDTH: 180,
  NODE_HEIGHT: 70,
  V_GAP: 60,
  H_GAP: 40,
  MARGIN_TOP: 40,
  MARGIN_LEFT: 200, // 中线 x 起点（左侧留白用于复杂分支）
}

const GATEWAY_TYPES = new Set(['condition', 'parallel', 'inclusive'])

export function calculateLayout(flowJson, opts = {}) {
  const cfg = { ...DEFAULT_OPTS, ...opts }
  const result = {
    nodePositions: new Map(),
    edgeWaypoints: new Map(),
  }

  if (!flowJson || !Array.isArray(flowJson.nodes) || flowJson.nodes.length === 0)
    return result

  const nodesById = new Map(flowJson.nodes.map(n => [n.id, n]))
  const edgesBySource = new Map()
  const edgesByTarget = new Map()
  for (const edge of flowJson.edges) {
    if (!edgesBySource.has(edge.source))
      edgesBySource.set(edge.source, [])
    edgesBySource.get(edge.source).push(edge)
    if (!edgesByTarget.has(edge.target))
      edgesByTarget.set(edge.target, [])
    edgesByTarget.get(edge.target).push(edge)
  }

  const inDegree = new Map(flowJson.nodes.map(n => [n.id, (edgesByTarget.get(n.id) || []).length]))
  const startNode = flowJson.nodes.find(n => n.nodeType === 'start') || flowJson.nodes[0]
  const centerX = cfg.MARGIN_LEFT
  const reworkClusters = findReworkClusters(flowJson.nodes, edgesBySource, edgesByTarget, nodesById)
  const reworkClusterByEntry = new Map(reworkClusters.map(cluster => [cluster.entryId, cluster]))
  const deferredReworkEntries = new Set()

  const visited = new Set()

  function placeChain(headId, x, y, options = {}) {
    const curX = x
    let curY = y
    let curId = headId

    while (curId && !visited.has(curId)) {
      if (options.stopNodeIds?.has(curId))
        return curY

      visited.add(curId)
      result.nodePositions.set(curId, {
        x: curX,
        y: curY,
        width: cfg.NODE_WIDTH,
        height: cfg.NODE_HEIGHT,
      })

      const out = edgesBySource.get(curId) || []
      if (out.length === 0)
        return curY

      if (out.length === 1) {
        const next = out[0].target
        // 入度 >= 2 的节点是汇合点，可能由其他分支负责放置；这里若已 visited 则停下
        if (visited.has(next))
          return curY
        // 仅在当前网关确认了真实汇合点时才截断分支。
        // 回退重提会让主线节点入度 >= 2，但它不是当前分支块的汇合点，不能中断主链路。
        if (options.stopNodeIds?.has(next))
          return curY

        curId = next
        curY += cfg.NODE_HEIGHT + cfg.V_GAP
        continue
      }

      // 网关分支
      const gatewayY = curY
      const branches = out
      const localMergeId = findLocalMergeNode(
        branches.map(b => b.target),
        edgesBySource,
        edgesByTarget,
        nodesById,
      )

      const defaultBranch = branches.find(edge => edge.isDefault)
      if (defaultBranch && !localMergeId) {
        const sideBranches = branches.filter(edge => edge.id !== defaultBranch.id)
        let maxBranchY = gatewayY

        for (let i = 0; i < sideBranches.length; i += 1) {
          const reworkCluster = reworkClusterByEntry.get(sideBranches[i].target)
          if (reworkCluster) {
            deferredReworkEntries.add(reworkCluster.entryId)
            continue
          }

          const direction = i % 2 === 0 ? 1 : -1
          const lane = Math.floor(i / 2) + 1
          const branchX = curX + direction * lane * (cfg.NODE_WIDTH + cfg.H_GAP)
          const branchEndY = placeChain(
            sideBranches[i].target,
            branchX,
            gatewayY + cfg.NODE_HEIGHT + cfg.V_GAP,
          )
          if (branchEndY > maxBranchY)
            maxBranchY = branchEndY
        }

        if (visited.has(defaultBranch.target))
          return Math.max(curY, maxBranchY)

        curId = defaultBranch.target
        curY += cfg.NODE_HEIGHT + cfg.V_GAP
        continue
      }

      const branchWidths = branches.map(b => measureBranchWidth(b.target))
      const totalW = branchWidths.reduce((s, w) => s + w, 0) + (branches.length - 1) * cfg.H_GAP
      const baseX = curX + cfg.NODE_WIDTH / 2 - totalW / 2

      let acc = 0
      let maxBranchY = gatewayY + cfg.NODE_HEIGHT + cfg.V_GAP
      const stopNodeIds = localMergeId ? new Set([localMergeId]) : new Set()
      for (let i = 0; i < branches.length; i += 1) {
        const bw = branchWidths[i]
        const branchX = baseX + acc + bw / 2 - cfg.NODE_WIDTH / 2
        const branchEndY = placeChain(
          branches[i].target,
          branchX,
          gatewayY + cfg.NODE_HEIGHT + cfg.V_GAP,
          { stopNodeIds },
        )
        if (branchEndY > maxBranchY)
          maxBranchY = branchEndY
        acc += bw + cfg.H_GAP
      }

      // 找到汇合点：第一个被 >= 2 个分支入边引用且未放置的目标
      const mergeId = localMergeId || findMergeNode(branches.map(b => b.target), edgesBySource, edgesByTarget, visited, nodesById)
      if (mergeId && !visited.has(mergeId)) {
        curId = mergeId
        curY = maxBranchY + cfg.NODE_HEIGHT + cfg.V_GAP
        // 汇合后位置回中线 curX
        continue
      }

      return maxBranchY
    }
    return curY
  }

  function measureBranchWidth(headId) {
    if (!headId)
      return cfg.NODE_WIDTH
    // 简化：分支宽度 = 沿线性链下走，遇到分支再递归
    const localVisited = new Set()
    return widthOf(headId, localVisited)
  }
  function widthOf(id, lv) {
    if (!id || lv.has(id))
      return cfg.NODE_WIDTH
    lv.add(id)
    const out = edgesBySource.get(id) || []
    if (out.length <= 1)
      return cfg.NODE_WIDTH
    let total = 0
    for (const e of out)
      total += widthOf(e.target, lv) + cfg.H_GAP
    total -= cfg.H_GAP
    return Math.max(total, cfg.NODE_WIDTH)
  }

  placeChain(startNode.id, centerX, cfg.MARGIN_TOP)
  placeDeferredReworkClusters()

  // 未访问节点（孤立节点 / advanced / 死锁分支）：放右侧
  let extraY = cfg.MARGIN_TOP
  const extraX = centerX + 600
  for (const node of flowJson.nodes) {
    if (visited.has(node.id))
      continue
    result.nodePositions.set(node.id, {
      x: extraX,
      y: extraY,
      width: cfg.NODE_WIDTH,
      height: cfg.NODE_HEIGHT,
    })
    extraY += cfg.NODE_HEIGHT + cfg.V_GAP
  }

  // 自由画布：覆盖人工拖拽坐标，保证 BPMNDiagram 与设计器一致
  for (const node of flowJson.nodes) {
    const ui = node.uiPosition
    if (!ui || !Number.isFinite(ui.x) || !Number.isFinite(ui.y))
      continue
    const prev = result.nodePositions.get(node.id) || {
      width: cfg.NODE_WIDTH,
      height: cfg.NODE_HEIGHT,
    }
    result.nodePositions.set(node.id, {
      x: ui.x,
      y: ui.y,
      width: prev.width || cfg.NODE_WIDTH,
      height: prev.height || cfg.NODE_HEIGHT,
    })
  }

  // 连线 waypoints：source 底中点 → target 顶中点
  for (const edge of flowJson.edges) {
    const s = result.nodePositions.get(edge.source)
    const t = result.nodePositions.get(edge.target)
    if (!s || !t)
      continue
    const sx = s.x + s.width / 2
    const sourceAboveTarget = s.y <= t.y
    const sy = sourceAboveTarget ? s.y + s.height : s.y
    const tx = t.x + t.width / 2
    const ty = sourceAboveTarget ? t.y : t.y + t.height
    if (Math.abs(sx - tx) < 1) {
      result.edgeWaypoints.set(edge.id, [
        { x: sx, y: sy },
        { x: tx, y: ty },
      ])
    }
    else if (reworkClusterByEntry.has(edge.target) && sourceAboveTarget) {
      const branchY = sy + Math.min(cfg.V_GAP / 2, 36)
      result.edgeWaypoints.set(edge.id, [
        { x: sx, y: sy },
        { x: sx, y: branchY },
        { x: tx, y: branchY },
        { x: tx, y: ty },
      ])
    }
    else {
      const midY = (sy + ty) / 2
      result.edgeWaypoints.set(edge.id, [
        { x: sx, y: sy },
        { x: sx, y: midY },
        { x: tx, y: midY },
        { x: tx, y: ty },
      ])
    }
  }

  return result

  function placeDeferredReworkClusters() {
    const entries = reworkClusters
      .map(cluster => cluster.entryId)
      .filter(entryId => deferredReworkEntries.has(entryId) || !visited.has(entryId))
    if (!entries.length)
      return

    const reworkX = centerX + cfg.NODE_WIDTH + cfg.H_GAP
    for (const entryId of entries) {
      if (visited.has(entryId))
        continue
      const sourceYs = (edgesByTarget.get(entryId) || [])
        .map(edge => result.nodePositions.get(edge.source))
        .filter(Boolean)
        .map(pos => pos.y + pos.height + cfg.V_GAP)
      const contentMaxY = currentMaxNodeY(result.nodePositions)
      const reworkY = Math.max(
        sourceYs.length ? Math.max(...sourceYs) : cfg.MARGIN_TOP,
        contentMaxY - cfg.NODE_HEIGHT,
      )
      placeChain(entryId, reworkX, reworkY)
    }
  }
}

function currentMaxNodeY(nodePositions) {
  let maxY = 0
  for (const pos of nodePositions.values())
    maxY = Math.max(maxY, pos.y + pos.height)
  return maxY
}

function findReworkClusters(nodes, edgesBySource, edgesByTarget, nodesById) {
  const clusters = []
  for (const node of nodes) {
    if (GATEWAY_TYPES.has(node.nodeType) || node.nodeType === 'start' || node.nodeType === 'end')
      continue

    const incomingGateways = (edgesByTarget.get(node.id) || [])
      .map(edge => edge.source)
      .filter(sourceId => GATEWAY_TYPES.has(nodesById.get(sourceId)?.nodeType))
    if (!incomingGateways.length)
      continue

    const outgoing = edgesBySource.get(node.id) || []
    if (outgoing.length !== 1)
      continue

    const decisionId = outgoing[0].target
    const decisionNode = nodesById.get(decisionId)
    if (!GATEWAY_TYPES.has(decisionNode?.nodeType))
      continue

    const decisionOut = edgesBySource.get(decisionId) || []
    const defaultEdge = decisionOut.find(edge => edge.isDefault || edge.id === decisionNode.config?.defaultFlowId)
    if (!defaultEdge)
      continue

    const returnsToApprovalPath = incomingGateways.some(gatewayId =>
      canReach(defaultEdge.target, gatewayId, edgesBySource),
    )
    if (!returnsToApprovalPath)
      continue

    clusters.push({ entryId: node.id, decisionId })
  }
  return clusters
}

function canReach(startId, targetId, edgesBySource) {
  if (!startId || !targetId)
    return false
  const queue = [startId]
  const visited = new Set()
  while (queue.length) {
    const cur = queue.shift()
    if (cur === targetId)
      return true
    if (!cur || visited.has(cur))
      continue
    visited.add(cur)
    for (const edge of edgesBySource.get(cur) || []) {
      if (!visited.has(edge.target))
        queue.push(edge.target)
    }
  }
  return false
}

function findLocalMergeNode(branchHeads, edgesBySource, edgesByTarget, nodesById) {
  if (!Array.isArray(branchHeads) || branchHeads.length < 2)
    return null

  const paths = branchHeads.map(head => collectLinearBranchPath(head, edgesBySource, nodesById))
  if (paths.some(path => path.length === 0))
    return null

  const commonIds = new Set(paths[0])
  for (const path of paths.slice(1)) {
    const ids = new Set(path)
    for (const id of [...commonIds]) {
      if (!ids.has(id))
        commonIds.delete(id)
    }
  }

  for (const id of paths[0]) {
    const node = nodesById.get(id)
    const inEdges = edgesByTarget.get(id) || []
    if (commonIds.has(id) && inEdges.length >= 2 && node?.config?.mergeNode)
      return id
  }
  return null
}

function collectLinearBranchPath(headId, edgesBySource, nodesById) {
  const path = []
  const guard = new Set()
  let cur = headId
  while (cur && !guard.has(cur)) {
    guard.add(cur)
    path.push(cur)

    const node = nodesById.get(cur)
    if (GATEWAY_TYPES.has(node?.nodeType))
      break

    const out = edgesBySource.get(cur) || []
    if (out.length !== 1)
      break
    cur = out[0].target
  }
  return path
}

function findMergeNode(branchHeads, edgesBySource, edgesByTarget, visited, nodesById) {
  // 沿每条分支前进，直到找到入度 >= 2 且 mergeNode=true 的节点
  for (const head of branchHeads) {
    let cur = head
    const guard = new Set()
    while (cur && !guard.has(cur)) {
      guard.add(cur)
      const node = nodesById.get(cur)
      const inEdges = edgesByTarget.get(cur) || []
      if (inEdges.length >= 2 && node?.config?.mergeNode)
        return cur
      const out = edgesBySource.get(cur) || []
      if (out.length !== 1)
        break
      cur = out[0].target
    }
  }
  return null
}
