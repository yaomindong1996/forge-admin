/** 只折叠互斥支路上的等价审批；不能把“画布只有一个卡片”误当成运行时只办一次。 */
const GATEWAYS = new Set(['condition', 'parallel', 'inclusive'])
const EDGE_DISPLAY_KEYS = new Set(['id', 'bpmnElementId', 'source', 'branchId', 'waypoints'])

function indexFlow(flow) {
  const nodes = new Map((flow?.nodes || []).map(node => [node.id, node]))
  const incoming = new Map()
  const outgoing = new Map()
  for (const edge of flow?.edges || []) {
    incoming.set(edge.target, [...(incoming.get(edge.target) || []), edge])
    outgoing.set(edge.source, [...(outgoing.get(edge.source) || []), edge])
  }
  return { nodes, incoming, outgoing }
}

function endpoint(index, id) {
  const node = index.nodes.get(id)
  if (node?.nodeType !== 'approver')
    return { reason: '只能合并审批节点' }
  const incoming = index.incoming.get(id) || []
  const outgoing = index.outgoing.get(id) || []
  if (!incoming.length || outgoing.length !== 1)
    return { reason: '审批节点必须有前置路径，且只有一个后续节点' }
  if (!index.nodes.has(outgoing[0].target))
    return { reason: '后续节点不存在，请先修复流程连线' }
  return { node, incoming, outgoing }
}

function traceBranch(index, initialEdge, nodeId) {
  const visited = new Set([nodeId])
  let edge = initialEdge
  while (edge) {
    const parent = index.nodes.get(edge.source)
    if (!parent || visited.has(parent.id))
      return { reason: '存在回流或无效连线，不能安全合并' }
    visited.add(parent.id)
    const incoming = index.incoming.get(parent.id) || []
    const outgoing = index.outgoing.get(parent.id) || []
    if (outgoing.length > 1) {
      if (parent.nodeType !== 'condition' || incoming.length !== 1)
        return { reason: '仅支持互斥条件分支；并行、包容或复杂分叉不能直接合并' }
      return { gatewayId: parent.id, branchId: edge.id }
    }
    if (GATEWAYS.has(parent.nodeType) || parent.nodeType === 'advanced' || incoming.length !== 1)
      return { reason: '请在同一个条件网关的独立支路末端合并' }
    edge = incoming[0]
  }
  return { reason: '未找到所属条件分支' }
}

function branchOrigins(index, item) {
  const paths = item.incoming.map(edge => traceBranch(index, edge, item.node.id))
  const invalid = paths.find(path => path.reason)
  if (invalid)
    return invalid
  const gatewayIds = new Set(paths.map(path => path.gatewayId))
  const branchIds = new Set(paths.map(path => path.branchId))
  if (gatewayIds.size !== 1 || branchIds.size !== paths.length)
    return { reason: '节点已被不同网关或同一支路重复引用，不能直接合并' }
  return { gatewayId: paths[0].gatewayId, branchIds }
}

function canReach(index, from, to) {
  const stack = [from]
  const visited = new Set()
  while (stack.length) {
    const id = stack.pop()
    if (id === to)
      return true
    if (visited.has(id))
      continue
    visited.add(id)
    for (const edge of index.outgoing.get(id) || [])
      stack.push(edge.target)
  }
  return false
}

function canonical(value) {
  if (Array.isArray(value))
    return value.map(canonical)
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort()
      .filter(key => value[key] !== undefined).map(key => [key, canonical(value[key])]))
  }
  return value
}

function configOf(node) {
  // mergeNode 仅是本地布局标记，不能掩盖任何审批配置（包括未知扩展字段）。
  return Object.fromEntries(Object.entries(node.config || {}).filter(([key]) => key !== 'mergeNode'))
}

function executionEdge(edge) {
  const extras = Object.fromEntries(Object.entries(edge).filter(([key]) => !EDGE_DISPLAY_KEYS.has(key)))
  return {
    ...extras,
    condition: edge.condition || '',
    conditionType: edge.conditionType || null,
    isDefault: Boolean(edge.isDefault),
  }
}

function sameValue(a, b) {
  return JSON.stringify(canonical(a)) === JSON.stringify(canonical(b))
}

function checkTopology(index, source, target) {
  const a = branchOrigins(index, source)
  const b = branchOrigins(index, target)
  if (a.reason || b.reason)
    return a.reason || b.reason
  if (a.gatewayId !== b.gatewayId || [...a.branchIds].some(id => b.branchIds.has(id)))
    return '两个节点必须位于同一个条件网关的不同支路'
  const nextId = source.outgoing[0].target
  if (nextId !== target.outgoing[0].target)
    return '两个节点的后续节点不同，请先统一后续路径'
  if (canReach(index, nextId, source.node.id) || canReach(index, nextId, target.node.id))
    return '合并位置存在回流，不能安全合并'
  return ''
}

/** 返回资格与原因供 UI 展示；不修改输入图，也不依赖节点名称/审批人摘要推断等价。 */
export function inspectApprovalMerge(flow, sourceId, targetId) {
  const index = indexFlow(flow)
  const source = endpoint(index, sourceId)
  const target = endpoint(index, targetId)
  if (source.reason || target.reason)
    return { eligible: false, reason: source.reason || target.reason }
  if (sourceId === targetId)
    return { eligible: false, reason: '不能合并到自身' }
  const topologyReason = checkTopology(index, source, target)
  if (topologyReason)
    return { eligible: false, reason: topologyReason }
  if (!sameValue(executionEdge(source.outgoing[0]), executionEdge(target.outgoing[0])))
    return { eligible: false, reason: '后续连线条件或扩展配置不一致，不能丢弃任一路径规则' }
  const sourceConfig = configOf(source.node)
  const targetConfig = configOf(target.node)
  const differenceKeys = [...new Set([...Object.keys(sourceConfig), ...Object.keys(targetConfig)])]
    .filter(key => !sameValue(sourceConfig[key], targetConfig[key]))
  if (differenceKeys.length) {
    return { eligible: false, reason: '审批配置不一致，请先核对人员、表单权限、会签及其他配置', differenceKeys }
  }
  // rawXml 等节点级扩展同样不能因缩成一张卡而被静默覆盖。
  const nodeExtras = node => Object.fromEntries(Object.entries(node)
    .filter(([key]) => !['id', 'bpmnElementId', 'name', 'config'].includes(key)))
  if (!sameValue(nodeExtras(source.node), nodeExtras(target.node)))
    return { eligible: false, reason: '节点级扩展信息不一致，不能直接合并' }
  return { eligible: true, reason: '', nextId: source.outgoing[0].target }
}

export function getApprovalMergeCandidates(flow, sourceId) {
  return (flow?.nodes || [])
    .filter(node => node.nodeType === 'approver' && node.id !== sourceId)
    .map(node => ({ node, ...inspectApprovalMerge(flow, sourceId, node.id) }))
}

/** 保留所有入边 ID/条件/默认属性，只重定向端点；目标审批配置不做并集。 */
export function mergeApprovalNodes(flow, sourceId, targetId) {
  const inspection = inspectApprovalMerge(flow, sourceId, targetId)
  if (!inspection.eligible)
    throw new Error(inspection.reason)
  const next = JSON.parse(JSON.stringify(flow))
  next.nodes = next.nodes.filter(node => node.id !== sourceId)
  const target = next.nodes.find(node => node.id === targetId)
  target.config = { ...target.config, mergeNode: true }
  next.edges = next.edges.filter(edge => edge.source !== sourceId)
    .map(edge => edge.target === sourceId ? { ...edge, target: targetId } : edge)
  return next
}
