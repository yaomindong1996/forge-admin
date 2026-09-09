/**
 * 运行态导航树与权限判断的纯函数工具。
 */

export function flattenNodes(nodes, parentId = null, depth = 0, collapsedSet = null) {
  return nodes.filter(item => item.parentId === parentId).sort((a, b) => a.sort - b.sort).flatMap((item) => {
    const result = [{ ...item, depth }]
    // 如果是分组且已折叠，不展开子节点
    if (item.type === 'group' && collapsedSet?.has(item.id))
      return result
    return [...result, ...flattenNodes(nodes, item.id, depth + 1, collapsedSet)]
  })
}

export function isNavigationVisible(node = {}) {
  return (node.navigationVisible ?? node.settings?.navigationVisible) !== false
}

export function hasPermission(source, permission) {
  return Array.isArray(source) && (source.includes(permission) || source.includes('**') || source.includes('*:*:*'))
}
