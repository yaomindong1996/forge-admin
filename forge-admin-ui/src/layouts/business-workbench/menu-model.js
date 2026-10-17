// 输入为权限仓库已授权的菜单，不为工作台附加任何示例菜单。
export function buildWorkbenchMenus(items = []) {
  return items.flatMap((item) => {
    if (item.meta?.hidden || item.visible === 0 || item.menuStatus === 0)
      return []
    const children = buildWorkbenchMenus(item.children || [])
    if (item.type === 'subapp')
      return children
    if (!children.length && (item.type === 'module' || !item.path))
      return []
    return [{ ...item, key: String(item.key ?? item.id), label: item.label || item.name || item.meta?.title || '', children }]
  })
}

export function findWorkbenchTrail(items, key) {
  if (key === null || key === undefined)
    return []
  for (const item of items) {
    if (item.key === String(key))
      return [item]
    const children = findWorkbenchTrail(item.children, key)
    if (children.length)
      return [item, ...children]
  }
  return []
}

export function buildMegaSections(menu) {
  return (menu?.children || []).map(child => ({
    key: child.key,
    title: child.label,
    icon: child.icon,
    entry: child,
    items: child.children,
    hasChildren: Boolean(child.children.length),
    badgeText: child.badgeText || '',
    badgeTone: child.badgeTone || '',
  }))
}

function includesKeyword(value, keyword) {
  return String(value || '').toLocaleLowerCase().includes(keyword)
}

function filterMenuItems(items, keyword) {
  return items.flatMap((item) => {
    if (includesKeyword(item.label, keyword))
      return [item]

    const children = filterMenuItems(item.children || [], keyword)
    return children.length ? [{ ...item, children }] : []
  })
}

export function filterMegaSections(sections = [], search = '') {
  const keyword = String(search || '').trim().toLocaleLowerCase()
  if (!keyword)
    return sections

  return sections.flatMap((section) => {
    if (includesKeyword(section.title, keyword))
      return [section]

    const items = filterMenuItems(section.items || [], keyword)
    return items.length ? [{ ...section, items }] : []
  })
}

export function countMenuLeaves(items = []) {
  return items.reduce((total, item) => total + (item.children?.length ? countMenuLeaves(item.children) : 1), 0)
}

function menuTreeContainsKey(items = [], key) {
  const target = key === null || key === undefined ? '' : String(key)
  if (!target)
    return false
  return items.some((item) => {
    if (String(item.key) === target)
      return true
    return menuTreeContainsKey(item.children || [], target)
  })
}

/**
 * 解析大面板左侧当前应高亮的二级：预览 > 路由所属二级。
 * 无匹配时不回退到首项，避免一打开一级菜单就默认蓝底第一个子菜单。
 */
export function resolveMegaActiveSection(sections = [], { previewKey = null, routeSectionKey = '', activeKey = '' } = {}) {
  const visible = Array.isArray(sections) ? sections : []
  if (!visible.length)
    return undefined

  const preview = previewKey === null || previewKey === undefined || previewKey === ''
    ? undefined
    : visible.find(section => String(section.key) === String(previewKey))
  if (preview)
    return preview

  const routeKey = routeSectionKey === null || routeSectionKey === undefined ? '' : String(routeSectionKey)
  const active = activeKey === null || activeKey === undefined ? '' : String(activeKey)

  const byRouteKey = routeKey
    ? visible.find(section => String(section.key) === routeKey)
    : undefined
  if (byRouteKey)
    return byRouteKey

  const byActiveKey = active
    ? visible.find(section => String(section.key) === active)
      || visible.find(section => menuTreeContainsKey(section.items || [], active))
    : undefined
  if (byActiveKey)
    return byActiveKey

  return undefined
}
