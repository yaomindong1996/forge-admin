export function normalizeSingleNumber(value, fallback = null) {
  if (Array.isArray(value)) {
    const first = value.find(item => item !== null && item !== undefined && item !== '')
    return normalizeSingleNumber(first, fallback)
  }
  if (value === null || value === undefined || value === '')
    return fallback
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? fallback : numberValue
}

export function normalizeNumberList(value) {
  const list = Array.isArray(value) ? value : (value === null || value === undefined || value === '' ? [] : [value])
  return Array.from(new Set(list
    .map(item => normalizeSingleNumber(item))
    .filter(item => item !== null)))
}

export function flattenOrgNodes(list = []) {
  return (list || []).flatMap((item) => {
    const current = [item]
    const children = flattenOrgNodes(item.children || [])
    return [...current, ...children]
  })
}

export function buildRoleUserOrgTreeOptions(list = [], scopedOrgIds = new Set(), globalScope = false) {
  return (list || [])
    .map((item) => {
      const value = normalizeSingleNumber(item.id)
      const children = buildRoleUserOrgTreeOptions(item.children || [], scopedOrgIds, globalScope)
      const selectable = value !== null && (globalScope || scopedOrgIds.has(value))
      if (!selectable && children.length === 0)
        return null
      return {
        label: item.orgName || item.label || '-',
        value,
        disabled: !selectable,
        children,
      }
    })
    .filter(Boolean)
}

export function getOrgNodeTone(node = {}) {
  if (!node.parentId || Number(node.parentId) === 0)
    return 'folder'
  return node.children?.length ? 'folder' : 'menu'
}
