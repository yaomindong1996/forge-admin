/**
 * 布局组件菜单相关组合式函数
 * 封装菜单数据处理、活跃项计算和导航跳转逻辑
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePermissionStore } from '@/store'
import { isExternal } from '@/utils'
import { findMenuIdByPath, findMenuItem, isSameMenuPath, processMenuData } from '@/utils/menu-utils'
import {
  DEFAULT_REPORT_BASE_URL,
  DEFAULT_SSO_TARGET_CLIENT,
  getDefaultSsoRedirectPath,
  normalizeSsoBaseUrl,
  normalizeSsoRedirectPath,
  parseExternalSsoTarget,
  REPORT_HOST_FALLBACK,
  REPORT_PATH_PREFIX,
  resolveSsoTargetBaseUrl,
  SSO_BRIDGE_ROUTE,
} from '@/utils/sso-target'

function normalizeLocalPath(path) {
  if (!path) {
    return ''
  }
  const targetPath = path.trim()
  if (!targetPath) {
    return ''
  }
  if (targetPath.startsWith('/') || isExternal(targetPath)) {
    return targetPath
  }
  return `/${targetPath}`
}

function collectReportBaseUrls() {
  return [
    DEFAULT_REPORT_BASE_URL,
    resolveSsoTargetBaseUrl({ targetClient: DEFAULT_SSO_TARGET_CLIENT }),
  ].filter(Boolean)
}

function getUrlPathname(value) {
  try {
    return new URL(`${value}/`).pathname.replace(/\/+$/, '') || '/'
  }
  catch {
    return ''
  }
}

function isReportBaseUrl(baseUrl) {
  if (!baseUrl) {
    return false
  }

  const normalizedBaseUrl = normalizeSsoBaseUrl(baseUrl)
  const reportBaseUrls = collectReportBaseUrls()
  if (reportBaseUrls.includes(normalizedBaseUrl)) {
    return true
  }

  try {
    const url = new URL(`${normalizedBaseUrl}/`)
    const reportPathPrefixes = [
      REPORT_PATH_PREFIX,
      ...reportBaseUrls.map(getUrlPathname),
    ].filter(prefix => prefix && prefix !== '/')

    return (REPORT_HOST_FALLBACK && url.host === REPORT_HOST_FALLBACK)
      || reportPathPrefixes.some(prefix => url.pathname.startsWith(prefix))
  }
  catch {
    return false
  }
}

function parseReportMenuTarget(rawTarget) {
  if (!rawTarget || !isExternal(rawTarget)) {
    return null
  }

  try {
    const targetUrl = new URL(rawTarget)
    const baseUrl = normalizeSsoBaseUrl(`${targetUrl.origin}${targetUrl.pathname}`)
    if (!isReportBaseUrl(baseUrl)) {
      return null
    }

    let redirectPath = ''
    if (targetUrl.hash) {
      const hashValue = targetUrl.hash.replace(/^#/, '')
      if (hashValue.startsWith('/')) {
        const [hashPath] = hashValue.split('?')
        redirectPath = hashPath
      }
    }

    if (!redirectPath) {
      redirectPath = getDefaultSsoRedirectPath(DEFAULT_SSO_TARGET_CLIENT)
    }

    return {
      baseUrl,
      redirectPath: normalizeSsoRedirectPath(redirectPath, DEFAULT_SSO_TARGET_CLIENT),
    }
  }
  catch {
    return null
  }
}

function buildSsoBridgeRoute(router, { targetClient, redirectPath, baseUrl, menuKey, title, display }) {
  const query = {
    targetClient,
    redirect: redirectPath,
  }

  if (baseUrl) {
    query.baseUrl = baseUrl
  }
  if (menuKey !== undefined && menuKey !== null && menuKey !== '') {
    query.menuKey = String(menuKey)
  }
  if (title) {
    query.title = title
  }
  if (display) {
    query.display = display
  }

  const location = {
    path: SSO_BRIDGE_ROUTE,
    query,
  }
  const resolved = router.resolve(location)

  return {
    location,
    fullPath: resolved.fullPath,
    href: resolved.href,
  }
}

function normalizeOpenTarget(openTarget) {
  return openTarget === '_blank' ? '_blank' : '_self'
}

function buildSsoBridgeDisplay(openTarget) {
  return normalizeOpenTarget(openTarget) === '_blank' ? 'redirect' : 'embed'
}

function navigateSsoBridge(router, bridgeRoute, openTarget) {
  if (!bridgeRoute) {
    return
  }

  if (normalizeOpenTarget(openTarget) === '_blank') {
    window.open(bridgeRoute.href || bridgeRoute.fullPath, '_blank', 'noopener,noreferrer')
    return
  }

  router.push(bridgeRoute.location || bridgeRoute.fullPath)
}

function buildConfiguredSsoTarget(originalItem) {
  if (Number(originalItem?.ssoEnabled) !== 1 || !originalItem?.ssoTargetClient) {
    return null
  }

  const targetClient = originalItem.ssoTargetClient
  const pathTarget = parseExternalSsoTarget(originalItem.path, targetClient)
  const subAppTarget = parseExternalSsoTarget(originalItem.subAppURL, targetClient)
  const preferredBaseUrl = subAppTarget?.baseUrl || pathTarget?.baseUrl || ''
  const baseUrl = resolveSsoTargetBaseUrl({ targetClient, preferredBaseUrl })
  const redirectPath = pathTarget?.redirectPath
    || normalizeSsoRedirectPath(originalItem.path || subAppTarget?.redirectPath, targetClient)

  return {
    targetClient,
    baseUrl,
    redirectPath,
    menuKey: originalItem.key || originalItem.id,
    title: originalItem.resourceName || originalItem.label || originalItem.name || '',
  }
}

function isNoMatchRoute(resolvedRoute) {
  if (!resolvedRoute) {
    return true
  }

  if (resolvedRoute.name === '404' || resolvedRoute.path === '/404') {
    return true
  }

  if (!resolvedRoute.matched || resolvedRoute.matched.length === 0) {
    return true
  }

  return resolvedRoute.matched.every(record => record.path === '/:pathMatch(.*)*')
}

/**
 * 查找包含指定路径的顶级菜单
 * @param {Array} menus - 权限仓库中的原始菜单
 * @param {string} targetPath - 要匹配的路由路径
 * @returns {object|null} 包含该路径的顶级菜单项，或null
 */
export function findTopMenuByPath(menus, targetPath) {
  if (!menus || !Array.isArray(menus))
    return null

  const findInMenu = (items) => {
    for (const item of items) {
      if (isSameMenuPath(item.path, targetPath)) {
        return item
      }
      if (item.children && item.children.length > 0) {
        const found = findInMenu(item.children)
        if (found)
          return item
      }
    }
    return null
  }

  return findInMenu(menus)
}

/**
 * 查找菜单中第一个有有效路径的子项
 * @param {object} menuItem - 要搜索的菜单项
 * @returns {object|null} 第一个有路径的子项，或null
 */
export function findFirstMenuWithPath(menuItem) {
  if (!menuItem)
    return null

  if (menuItem.path && !isExternal(menuItem.path) && menuItem.type !== 'module') {
    return menuItem
  }

  if (menuItem.children) {
    for (const child of menuItem.children) {
      const found = findFirstMenuWithPath(child)
      if (found)
        return found
    }
  }
  return null
}

/**
 * 菜单数据处理与导航组合式函数
 * @returns {object} 菜单组合式API
 */
export function useMenu() {
  const route = useRoute()
  const router = useRouter()
  const permissionStore = usePermissionStore()

  function resolveReportMenuRoute(rawTarget, openTarget = '_self') {
    const reportTarget = parseReportMenuTarget(rawTarget)
    if (!reportTarget) {
      return ''
    }

    return buildSsoBridgeRoute(router, {
      targetClient: DEFAULT_SSO_TARGET_CLIENT,
      baseUrl: reportTarget.baseUrl,
      redirectPath: reportTarget.redirectPath,
      display: buildSsoBridgeDisplay(openTarget),
    })
  }

  function resolveConfiguredSsoRoute(originalItem) {
    const ssoTarget = buildConfiguredSsoTarget(originalItem)
    if (!ssoTarget) {
      return ''
    }

    return buildSsoBridgeRoute(router, {
      ...ssoTarget,
      display: buildSsoBridgeDisplay(originalItem?.openTarget),
    })
  }

  function resolveNoMatchSsoRoute(targetPath, options = {}) {
    if (!targetPath || isExternal(targetPath)) {
      return ''
    }

    const resolvedRoute = router.resolve(targetPath)
    if (!isNoMatchRoute(resolvedRoute)) {
      return ''
    }

    const baseUrl = resolveSsoTargetBaseUrl({ targetClient: DEFAULT_SSO_TARGET_CLIENT })
    if (!baseUrl) {
      return ''
    }

    return buildSsoBridgeRoute(router, {
      targetClient: DEFAULT_SSO_TARGET_CLIENT,
      baseUrl,
      redirectPath: targetPath,
      menuKey: options.menuKey,
      title: options.title,
      display: buildSsoBridgeDisplay(options.openTarget),
    })
  }

  function resolveReportFallbackSsoRoute(originalItem) {
    const targetPath = normalizeLocalPath(originalItem?.path)
    if (!targetPath || originalItem?.component) {
      return ''
    }

    return resolveNoMatchSsoRoute(targetPath, {
      menuKey: originalItem.key || originalItem.id,
      title: originalItem.resourceName || originalItem.label || originalItem.name || '',
      openTarget: originalItem.openTarget,
    })
  }

  /**
   * 已处理的菜单数据，可直接用于 Naive UI menu 组件
   */
  const processedMenus = computed(() => {
    const menus = permissionStore.menus || []
    return processMenuData(menus)
  })

  /**
   * 扁平化的菜单项列表，便于快速查找
   */
  const flatMenuItems = computed(() => {
    const flatten = (items) => {
      if (!items || !Array.isArray(items))
        return []
      return items.reduce((acc, item) => {
        acc.push(item)
        if (item.children && item.children.length > 0) {
          acc.push(...flatten(item.children))
        }
        return acc
      }, [])
    }
    return flatten(processedMenus.value)
  })

  /**
   * 当前路由对应的活跃菜单key
   */
  const activeKey = computed(() => {
    if (route.path === SSO_BRIDGE_ROUTE && route.query?.menuKey) {
      return String(route.query.menuKey)
    }

    // 优先级1: 使用 route.meta.parentKey（用于隐藏的二级页面）
    if (route.meta?.parentKey) {
      return route.meta.parentKey
    }

    // 优先级2: 精确路径匹配
    let menuId = findMenuIdByPath(processedMenus.value, route.path)

    // 优先级3: 路径前缀匹配（用于隐藏菜单）
    if (!menuId) {
      const pathSegments = route.path.split('/').filter(Boolean)
      for (let i = pathSegments.length - 1; i > 0; i--) {
        const parentPath = `/${pathSegments.slice(0, i).join('/')}`
        menuId = findMenuIdByPath(processedMenus.value, parentPath)
        if (menuId) {
          break
        }
      }
    }

    return menuId || route.name
  })

  /**
   * 处理菜单选择，支持外链和内嵌iframe
   * @param {string} key - 菜单项key
   */
  function handleMenuSelect(key, fallbackPath) {
    let originalItem = findMenuItem(permissionStore.menus, key)

    if (!originalItem && typeof key === 'string') {
      const numKey = Number(key)
      if (!Number.isNaN(numKey)) {
        originalItem = findMenuItem(permissionStore.menus, numKey)
      }
    }

    if (!originalItem && typeof key === 'number') {
      originalItem = findMenuItem(permissionStore.menus, String(key))
    }

    if (!originalItem) {
      if (fallbackPath) {
        const targetPath = normalizeLocalPath(fallbackPath)
        const reportRoute = resolveReportMenuRoute(targetPath)
        if (reportRoute) {
          router.push(reportRoute.location)
          return
        }
        const noMatchSsoRoute = resolveNoMatchSsoRoute(targetPath)
        if (noMatchSsoRoute) {
          router.push(noMatchSsoRoute.location)
          return
        }
        router.push(targetPath)
      }
      return
    }

    if (originalItem.type === 'module' && originalItem.children?.length) {
      const firstMenu = findFirstMenuWithPath(originalItem)
      if (firstMenu?.path) {
        handleMenuSelect(firstMenu.key || firstMenu.id, firstMenu.path)
      }
      return
    }

    const configuredSsoRoute = resolveConfiguredSsoRoute(originalItem)
    if (configuredSsoRoute) {
      navigateSsoBridge(router, configuredSsoRoute, originalItem.openTarget)
      return
    }

    const reportFallbackSsoRoute = resolveReportFallbackSsoRoute(originalItem)
    if (reportFallbackSsoRoute) {
      navigateSsoBridge(router, reportFallbackSsoRoute, originalItem.openTarget)
      return
    }

    if (isExternal(originalItem.path)) {
      const reportRoute = resolveReportMenuRoute(originalItem.path, originalItem.openTarget)
      if (reportRoute) {
        navigateSsoBridge(router, reportRoute, originalItem.openTarget)
        return
      }

      $dialog.confirm({
        type: 'info',
        title: '请选择打开方式',
        positiveText: '外链打开',
        negativeText: '在本站内嵌打开',
        confirm() {
          window.open(originalItem.path)
        },
        cancel: () => {
          router.push(originalItem.path)
        },
      })
      return
    }

    if (originalItem.openMode === 'iframe' && originalItem.subAppURL) {
      const iframeTarget = `${originalItem.subAppURL}${originalItem.path || ''}`
      const reportRoute = resolveReportMenuRoute(iframeTarget, originalItem.openTarget)
      if (reportRoute) {
        navigateSsoBridge(router, reportRoute, originalItem.openTarget)
        return
      }
      const iframePath = `/iframe?page=${encodeURIComponent(iframeTarget)}`
      router.push(iframePath)
      return
    }

    if (originalItem.path) {
      const targetPath = normalizeLocalPath(originalItem.path)
      const reportRoute = resolveReportMenuRoute(targetPath, originalItem.openTarget)
      if (reportRoute) {
        navigateSsoBridge(router, reportRoute, originalItem.openTarget)
        return
      }
      router.push(targetPath)
    }
  }

  return {
    processedMenus,
    flatMenuItems,
    activeKey,
    handleMenuSelect,
    findMenuIdByPath: (itemsOrTargetPath, maybeTargetPath) => {
      if (Array.isArray(itemsOrTargetPath)) {
        return findMenuIdByPath(itemsOrTargetPath, maybeTargetPath)
      }
      return findMenuIdByPath(processedMenus.value, itemsOrTargetPath)
    },
    findTopMenuByPath: targetPath => findTopMenuByPath(permissionStore.menus, targetPath),
  }
}
