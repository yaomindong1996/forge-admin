import { usePermissionStore, useTabStore } from '@/store'

const baseTitle = import.meta.env.VITE_TITLE

function findTitleFromAllMenus(allMenus, targetPath, menuKey) {
  if (!Array.isArray(allMenus))
    return null
  if (menuKey !== undefined && menuKey !== null && menuKey !== '') {
    const menu = allMenus.find(item => String(item.key || item.id) === String(menuKey))
    if (menu)
      return menu.label || menu.name || menu.meta?.title || null
  }
  const found = allMenus.find(menu => isSameRoutePath(menu.path, targetPath))
  return found?.label || found?.name || found?.meta?.title || null
}

function normalizeRoutePath(path) {
  const value = String(path || '').trim()
  const [pathWithoutHash] = value.split('#')
  const [pathname] = pathWithoutHash.split('?')
  const normalized = String(pathname || '').replace(/\/+/g, '/')
  if (!normalized || normalized === '/')
    return normalized
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function isSameRoutePath(menuPath, targetPath) {
  const normalizedMenuPath = normalizeRoutePath(menuPath)
  const normalizedTargetPath = normalizeRoutePath(targetPath)
  if (!normalizedMenuPath || !normalizedTargetPath)
    return false
  if (normalizedMenuPath === normalizedTargetPath)
    return true
  if (!normalizedMenuPath.includes(':'))
    return false
  const pattern = normalizedMenuPath
    .split('/')
    .map(segment => segment.startsWith(':') ? '[^/]+' : escapeRegExp(segment))
    .join('/')
  return new RegExp(`^${pattern}$`).test(normalizedTargetPath)
}

export function createPageTitleGuard(router) {
  router.afterEach((to) => {
    const permissionStore = usePermissionStore()
    const tabStore = useTabStore()
    let pageTitle = to.meta?.title
    if (to.path?.startsWith('/ai/crud-page/')) {
      const dynamicTitle = tabStore.tabs.find(tab => tab.path === to.fullPath || tab.key === to.fullPath)?.title
      if (dynamicTitle && dynamicTitle !== 'CRUD页面')
        pageTitle = dynamicTitle
      else if (to.query?.title)
        pageTitle = String(to.query.title)
      else if (!pageTitle || pageTitle === 'CRUD页面')
        pageTitle = findTitleFromAllMenus(permissionStore.allMenus, to.path, to.query?.menuKey || to.query?.menuResourceId) || pageTitle
    }
    if (pageTitle) {
      document.title = `${pageTitle} | ${baseTitle}`
    }
    else {
      document.title = baseTitle
    }
  })
}
