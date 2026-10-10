export function normalizeInitiatorSelectIds(value) {
  const list = Array.isArray(value) ? value : value == null || value === '' ? [] : [value]
  return [...new Set(list.map(item => String(item ?? '').trim()).filter(Boolean))]
}

export function normalizeInitiatorSelectNodes(nodes) {
  return (Array.isArray(nodes) ? nodes : [])
    .filter(node => String(node?.nodeKey || '').trim())
    .map(node => ({
      nodeKey: String(node.nodeKey).trim(),
      nodeName: node.nodeName || String(node.nodeKey).trim(),
      multiple: node.multiple !== false,
    }))
}

// 与管理端 collectInitiatorSelectSelections 保持一致：每个节点必选，单选节点只取第一个。
export function collectInitiatorSelectSelections(nodes = [], selections = {}) {
  const result = {}
  for (const node of normalizeInitiatorSelectNodes(nodes)) {
    const ids = normalizeInitiatorSelectIds(selections?.[node.nodeKey])
    if (!ids.length) throw new Error(`请选择「${node.nodeName}」的审批人`)
    result[node.nodeKey] = node.multiple ? ids : [ids[0]]
  }
  return result
}
