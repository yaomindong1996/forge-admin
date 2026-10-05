import { usePermissionStore, useTabStore } from '@/store'
import { isHomeTabPath } from '@/utils/home-path'
import { confirmDirtyTabs } from '@/utils/tab-interactions'

export const EXCLUDE_TAB = ['/404', '/403', '/login', '/login/callback', '/mcp-authorize', '/']

/**
 * 从扁平菜单数组中查找路径对应的中文名称
 */
function findTitleFromAllMenus(allMenus, targetPath, menuKey) {
  // 首页路径判断：支持 '/' 和 '/home'
  if (targetPath === '/' || targetPath === '/home' || targetPath === window.$homePath) {
    return '首页'
  }
  if (!allMenus || !Array.isArray(allMenus))
    return null
  if (menuKey !== undefined && menuKey !== null && menuKey !== '') {
    const menu = allMenus.find(item => String(item.key || item.id) === String(menuKey))
    if (menu)
      return menu.label || menu.name || null
  }
  const found = allMenus.find(menu => isSameRoutePath(menu.path, targetPath))
  return found?.label || found?.name || null
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

function isGenericRuntimeTitle(title, path) {
  return path?.startsWith('/ai/crud-page/') && (!title || title === '业务页面')
}

function isBusinessRuntimePath(path) {
  return path?.startsWith('/ai/crud-page/')
}

function shouldUpdateExistingTitle(existingTitle, nextTitle, path) {
  if (!nextTitle || existingTitle === nextTitle)
    return false
  if (isBusinessRuntimePath(path))
    return isGenericRuntimeTitle(existingTitle, path) && !isGenericRuntimeTitle(nextTitle, path)
  return true
}

export function createTabGuard(router) {
  router.beforeEach(async (to, from) => {
    if (to.fullPath === from.fullPath)
      return true
    const tabStore = useTabStore()
    const currentTab = tabStore.tabs.find(item => item.path === from.fullPath || item.key === from.fullPath)
    if (!currentTab?.dirty)
      return true
    if (tabStore.dirtyNavigationBypassPath === from.fullPath)
      return true
    if (!await confirmDirtyTabs(currentTab, '离开页面'))
      return false
    tabStore.authorizeDirtyNavigation(from.fullPath)
    return true
  })

  router.afterEach(async (to) => {
    if (EXCLUDE_TAB.includes(to.path) || to.matched?.every(record => record.redirect))
      return
    const tabStore = useTabStore()
    if (to.meta?.skipTab) {
      tabStore.removeTabSilently(to.fullPath)
      return
    }
    const permissionStore = usePermissionStore()
    const { name } = to
    // key 用 to.fullPath（含查询参数），避免同一页面不同参数共用同一 Tab
    // path 保存 fullPath，以便 Tab 点击时恢复完整路径
    const path = to.fullPath

    // 1. 优先使用 route.meta.title（由 permission-guard 注册路由时设置）
    let title = to.meta?.title
    if (isBusinessRuntimePath(to.path) && to.query?.title)
      title = String(to.query.title)
    if (isGenericRuntimeTitle(title, to.path) && permissionStore.allMenus?.length) {
      title = findTitleFromAllMenus(permissionStore.allMenus, to.path, to.query?.menuKey || to.query?.menuResourceId) || title
    }

    if (to.path === '/home' || isHomeTabPath(to.path)) {
      title = '首页'
    }

    // 2. 从所有菜单（包括隐藏的）中查找中文名
    if (!title && permissionStore.allMenus?.length) {
      title = findTitleFromAllMenus(permissionStore.allMenus, to.path, to.query?.menuKey || to.query?.menuResourceId)
    }

    const icon = to.meta?.icon
    const keepAlive = to.meta?.keepAlive

    // 3. 尝试从组件中读取 title
    if (!title && to.matched.length > 0) {
      const component = to.matched[to.matched.length - 1].components?.default
      if (component) {
        try {
          const resolvedComponent = typeof component === 'function' ? await component() : component
          const componentTitle = resolvedComponent?.default?.title || resolvedComponent?.title
          if (componentTitle) {
            title = componentTitle
          }
        }
        catch (e) {
          console.warn('无法读取组件 title:', e)
        }
      }
    }

    // 检查是否已存在相同 path 的 tab；首页 `/` 与 `/home` 视为同一页，避免品牌点击重复开 Tab
    const existingTab = tabStore.tabs.find(item => item.path === path)
      || (isHomeTabPath(to.path)
        ? tabStore.tabs.find(item => isHomeTabPath(item.path) || isHomeTabPath(item.key))
        : null)
    const forceClosable = !!to.meta?.forceClosable || isBusinessRuntimePath(to.path)
    const closable = to.meta?.closable !== false
    const tabKey = existingTab?.key || existingTab?.path || path
    if (!existingTab) {
      tabStore.addTab({ name, path, title: title || path, icon, keepAlive, key: path, closable, forceClosable })
    }
    else {
      tabStore.updateTabMeta(tabKey, { closable, forceClosable })
      if (shouldUpdateExistingTitle(existingTab.title, title, to.path)) {
        tabStore.updateTabTitle(tabKey, title)
      }
    }
    tabStore.setActiveTab(tabKey)
  })
}
