export function filterNavigationMenus(items, search = '') {
  const keyword = search.trim().toLocaleLowerCase()
  if (!keyword) {
    return items
  }
  return items.flatMap((item) => {
    // 命中父目录时保留完整子树，不能把目录变成无子项且无路由的死入口。
    if (String(item.label || '').toLocaleLowerCase().includes(keyword)) {
      return [item]
    }
    const children = filterNavigationMenus(item.children || [], keyword)
    return children.length ? [{ ...item, children }] : []
  })
}
