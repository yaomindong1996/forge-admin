/**
 * Tab 标签页工具函数
 * 用于管理页面标签的打开、关闭、刷新等操作
 */

import { useRouterStore, useTabStore } from '@/store'

/**
 * 获取 router 实例
 */
function getRouter() {
  const routerStore = useRouterStore()
  return routerStore.router
}

/**
 * 关闭指定的 tab 页签
 * @param {object | string} target - 要关闭的页面对象或路径
 * @param {string} target.path - 页面路径
 * @param {string} target.name - 页面名称（可选）
 * @returns {Promise} 返回 Promise
 *
 * @example
 * // 关闭指定路径的 tab
 * closePage({ path: '/system/dictData' })
 *
 * // 关闭当前 tab
 * closePage()
 *
 * // 关闭指定路径并跳转到新页面
 * closePage({ path: '/system/dictData' }).then(() => {
 *   router.push('/system/dictType')
 * })
 */
export function closePage(target) {
  const tabStore = useTabStore()
  const router = getRouter()

  // 如果没有传参数，关闭当前页面
  if (!target) {
    const currentRoute = router?.currentRoute?.value
    if (currentRoute) {
      const currentTab = tabStore.tabs.find(tab => tab.path === currentRoute.fullPath)

      if (currentTab) {
        return new Promise((resolve) => {
          tabStore.removeTab(currentTab.key)
          resolve()
        })
      }
    }
    return Promise.resolve()
  }

  // 如果传入的是字符串，当作路径处理
  const targetPath = typeof target === 'string' ? target : target.path

  // 查找匹配的 tab（支持精确匹配和前缀匹配）
  const matchedTab = tabStore.tabs.find((tab) => {
    if (!tab.path)
      return false
    // 精确匹配
    if (tab.path === targetPath)
      return true
    // 前缀匹配（用于关闭带 query 参数的页面）
    if (tab.path.startsWith(targetPath))
      return true
    return false
  })

  if (matchedTab) {
    return new Promise((resolve) => {
      tabStore.removeTab(matchedTab.key)
      resolve()
    })
  }

  return Promise.resolve()
}

/**
 * 关闭多个 tab 页签
 * @param {Array<object | string>} targets - 要关闭的页面数组
 * @returns {Promise} 返回 Promise
 *
 * @example
 * // 关闭多个页面
 * closePages([
 *   { path: '/system/dictData' },
 *   { path: '/system/user' }
 * ])
 */
export function closePages(targets) {
  if (!Array.isArray(targets)) {
    return Promise.reject(new Error('targets must be an array'))
  }

  const promises = targets.map(target => closePage(target))
  return Promise.all(promises)
}

/**
 * 关闭 tab 并跳转到新页面
 * @param {object | string} closeTarget - 要关闭的页面
 * @param {object | string} openTarget - 要打开的页面
 * @returns {Promise} 返回 Promise
 *
 * @example
 * // 关闭旧页面并打开新页面
 * closeAndOpen(
 *   { path: '/system/dictData' },
 *   { path: '/system/dictData', query: { dictType: 'new_type' } }
 * )
 */
export function closeAndOpen(closeTarget, openTarget) {
  const router = getRouter()

  return closePage(closeTarget).then(() => {
    if (!router) {
      console.error('Router not available')
      return Promise.reject(new Error('Router not available'))
    }

    if (typeof openTarget === 'string') {
      return router.push(openTarget)
    }
    else {
      return router.push(openTarget)
    }
  })
}

/**
 * 刷新指定的 tab 页签
 * @param {string} path - 页面路径，不传则刷新当前页面
 * @returns {Promise} 返回 Promise
 *
 * @example
 * // 刷新当前页面
 * reloadPage()
 *
 * // 刷新指定页面
 * reloadPage('/system/user')
 */
export function reloadPage(path) {
  const tabStore = useTabStore()
  const router = getRouter()

  const targetPath = path || router?.currentRoute?.value?.fullPath
  if (!targetPath) {
    return Promise.resolve()
  }

  const tab = tabStore.tabs.find(t => t.path === targetPath)

  if (tab) {
    return tabStore.reloadTab(targetPath, tab.keepAlive)
  }

  return Promise.resolve()
}

/**
 * 关闭其他 tab 页签
 * @param {string} path - 要保留的页面路径，不传则保留当前页面
 * @returns {Promise} 返回 Promise
 */
export function closeOtherPages(path) {
  const tabStore = useTabStore()
  const router = getRouter()

  const targetPath = path || router?.currentRoute?.value?.fullPath
  if (targetPath) {
    tabStore.removeOther(targetPath)
  }

  return Promise.resolve()
}

/**
 * 关闭左侧 tab 页签
 * @param {string} path - 基准页面路径，不传则以当前页面为基准
 * @returns {Promise} 返回 Promise
 */
export function closeLeftPages(path) {
  const tabStore = useTabStore()
  const router = getRouter()

  const targetPath = path || router?.currentRoute?.value?.fullPath
  if (targetPath) {
    tabStore.removeLeft(targetPath)
  }

  return Promise.resolve()
}

/**
 * 关闭右侧 tab 页签
 * @param {string} path - 基准页面路径，不传则以当前页面为基准
 * @returns {Promise} 返回 Promise
 */
export function closeRightPages(path) {
  const tabStore = useTabStore()
  const router = getRouter()

  const targetPath = path || router?.currentRoute?.value?.fullPath
  if (targetPath) {
    tabStore.removeRight(targetPath)
  }

  return Promise.resolve()
}

/**
 * 关闭所有 tab 页签
 * @returns {Promise} 返回 Promise
 */
export function closeAllPages() {
  const tabStore = useTabStore()
  const router = getRouter()

  tabStore.resetTabs()
  if (router) {
    router.push('/')
  }

  return Promise.resolve()
}
