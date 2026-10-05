export function flattenResourceTree(list = []) {
  const result = []
  const walk = (items, parent = null, level = 0) => {
    items.forEach((item) => {
      const normalized = { ...item, parent, level }
      result.push(normalized)
      if (item.children?.length)
        walk(item.children, normalized, level + 1)
    })
  }
  walk(Array.isArray(list) ? list : [])
  return result
}

export function resolveResourceContextRows(allResources, currentNode, options = {}) {
  const contextRows = currentNode
    ? (Array.isArray(currentNode.children) ? currentNode.children : [])
    : (Array.isArray(allResources) ? allResources : [])

  return options.includeDescendants ? flattenResourceTree(contextRows) : contextRows
}

export function resolveFreshResourceRow(flatResources, selectedRow) {
  if (!selectedRow?.id)
    return null

  return (Array.isArray(flatResources) ? flatResources : [])
    .find(item => item.id === selectedRow.id) || null
}

export function getResourceBreadcrumbs(flatResources, currentNode) {
  const nodes = new Map(flatResources.map(row => [String(row.id), row]))
  const visited = new Set()
  const path = []
  let current = currentNode
  // 兼容树接口只有 parentId 的节点，并防止脏数据形成父级循环。
  while (current && !visited.has(String(current.id))) {
    visited.add(String(current.id))
    path.unshift({ id: current.id, label: current.resourceName })
    current = current.parent || nodes.get(String(current.parentId))
  }
  return [{ id: 0, label: '全部资源' }, ...path]
}
