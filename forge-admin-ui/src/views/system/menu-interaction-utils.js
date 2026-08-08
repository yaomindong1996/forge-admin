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
