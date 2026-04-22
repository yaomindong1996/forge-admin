import { usePermissionStore, useTabStore } from '@/store'

export const EXCLUDE_TAB = ['/404', '/403', '/login', '/login/callback']

/**
 * 从扁平菜单数组中查找路径对应的中文名称
 */
function findTitleFromAllMenus(allMenus, targetPath) {
  console.log('targetPath', targetPath)
  // 首页路径判断：支持 '/' 和 '/home'
  if (targetPath === '/' || targetPath === '/home' || targetPath === window.$homePath) {
    return '首页'
  }
  if (!allMenus || !Array.isArray(allMenus))
    return null
  const found = allMenus.find(menu => menu.path === targetPath)
  return found?.label || found?.name || null
}

export function createTabGuard(router) {
  router.afterEach(async (to) => {
    console.log('to', to)
    if (EXCLUDE_TAB.includes(to.path))
      return
    const tabStore = useTabStore()
    const permissionStore = usePermissionStore()
    const { name } = to
    // key 用 to.fullPath（含查询参数），避免同一页面不同参数共用同一 Tab
    // path 保存 fullPath，以便 Tab 点击时恢复完整路径
    const path = to.fullPath

    // 1. 优先使用 route.meta.title（由 permission-guard 注册路由时设置）
    let title = to.meta?.title

    if (to.path === '/home') {
      title = '首页'
    }

    // 2. 从所有菜单（包括隐藏的）中查找中文名
    if (!title && permissionStore.allMenus?.length) {
      title = findTitleFromAllMenus(permissionStore.allMenus, to.path)
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

    // 检查是否已存在相同 path 的 tab
    const existingTab = tabStore.tabs.find(item => item.path === path)
    if (!existingTab) {
      tabStore.addTab({ name, path, title: title || path, icon, keepAlive, key: path })
    }
    else if (title && existingTab.title !== title) {
      // 如果 tab 已存在但 title 为空，自动更新
      existingTab.title = title
    }
    tabStore.setActiveTab(path)
  })
}
