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

  const visited = new Set()

  function placeChain(headId, x, y) {
    const curX = x
    let curY = y
    let curId = headId

    while (curId && !visited.has(curId)) {
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
        // 入度 >= 2 的节点：交给上层网关处理（它会在分支结束后统一放置）
        if ((inDegree.get(next) || 0) >= 2 && nodesById.get(next)?.config?.mergeNode)
          return curY

        curId = next
        curY += cfg.NODE_HEIGHT + cfg.V_GAP
        continue
      }

      // 网关分支
      const gatewayY = curY
      const branches = out
      const branchWidths = branches.map(b => measureBranchWidth(b.target))
      const totalW = branchWidths.reduce((s, w) => s + w, 0) + (branches.length - 1) * cfg.H_GAP
      const baseX = curX + cfg.NODE_WIDTH / 2 - totalW / 2

      let acc = 0
      let maxBranchY = gatewayY + cfg.NODE_HEIGHT + cfg.V_GAP
      for (let i = 0; i < branches.length; i += 1) {
        const bw = branchWidths[i]
        const branchX = baseX + acc + bw / 2 - cfg.NODE_WIDTH / 2
        const branchEndY = placeChain(branches[i].target, branchX, gatewayY + cfg.NODE_HEIGHT + cfg.V_GAP)
        if (branchEndY > maxBranchY)
          maxBranchY = branchEndY
        acc += bw + cfg.H_GAP
      }

      // 找到汇合点：第一个被 >= 2 个分支入边引用且未放置的目标
      const mergeId = findMergeNode(branches.map(b => b.target), edgesBySource, edgesByTarget, visited, nodesById)
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

  // 连线 waypoints：source 底中点 → target 顶中点
  for (const edge of flowJson.edges) {
    const s = result.nodePositions.get(edge.source)
    const t = result.nodePositions.get(edge.target)
    if (!s || !t)
      continue
    const sx = s.x + s.width / 2
    const sy = s.y + s.height
    const tx = t.x + t.width / 2
    const ty = t.y
    if (Math.abs(sx - tx) < 1) {
      result.edgeWaypoints.set(edge.id, [
        { x: sx, y: sy },
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
