// 流程图接口按 BPMN 元素顺序返回节点（结束节点可能排在审批节点前），节点进度需按连线从开始节点逐层排序
export function orderProcessNodes(nodes = [], flows = []) {
  const list = Array.isArray(nodes) ? nodes.filter(Boolean) : []
  if (!list.length) return []
  const byId = new Map(list.map(node => [node.nodeId, node]))
  const outgoing = new Map()
  const incoming = new Set()
  for (const flow of Array.isArray(flows) ? flows : []) {
    if (!byId.has(flow?.sourceRef) || !byId.has(flow?.targetRef)) continue
    if (!outgoing.has(flow.sourceRef)) outgoing.set(flow.sourceRef, [])
    outgoing.get(flow.sourceRef).push(flow.targetRef)
    incoming.add(flow.targetRef)
  }
  const startEvents = list.filter(node => /start/i.test(node.nodeType || ''))
  const starts = startEvents.length ? startEvents : list.filter(node => !incoming.has(node.nodeId))
  const ordered = []
  const seen = new Set()
  const queue = starts.map(node => node.nodeId)
  while (queue.length) {
    const id = queue.shift()
    if (seen.has(id)) continue
    seen.add(id)
    ordered.push(byId.get(id))
    for (const next of outgoing.get(id) || []) {
      if (!seen.has(next)) queue.push(next)
    }
  }
  return [...ordered, ...list.filter(node => !seen.has(node.nodeId))]
}
