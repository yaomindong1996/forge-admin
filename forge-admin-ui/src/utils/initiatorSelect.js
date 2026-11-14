/**
 * 发起人自选审批人：单选（不会签）是字符串，会签是数组。
 */
export function normalizeInitiatorSelectIds(value) {
  // 多选人员选择器的值是逗号拼接的字符串（如 "1,70"），需拆成多个审批人
  const items = Array.isArray(value)
    ? value
    : value == null || value === '' ? [] : String(value).split(',')
  return [...new Set(items
    .map(item => String(item ?? '').trim())
    .filter(Boolean))]
}

export function collectInitiatorSelectSelections(nodes = [], selections = {}) {
  const result = {}
  for (const node of nodes) {
    const nodeKey = String(node?.nodeKey || '').trim()
    if (!nodeKey)
      continue
    const ids = normalizeInitiatorSelectIds(selections?.[nodeKey])
    if (!ids.length) {
      const name = node.nodeName || nodeKey
      throw new Error(`请选择「${name}」的审批人`)
    }
    result[nodeKey] = node.multiple === false ? [ids[0]] : ids
  }
  return result
}
