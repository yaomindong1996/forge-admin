import { useAuthStore } from '@/store'
import { notify } from '@/utils/notify'
import { buildLoginUrl, HOME_PAGE, isLoginRoute, LOGIN_PAGE } from '@/utils/route'
import { hasPermission, normalizeRoutePath, resolvePageAccess } from '@/utils/access-control'

const ROUTE_METHODS = ['navigateTo', 'redirectTo', 'reLaunch', 'switchTab']
const GUARD_FLAG = '__forgeAuthGuardPatched'
const SKIP_FLAG = '__skipAuthGuard'

export function setupNavigationGuard() {
  if (typeof uni === 'undefined' || uni[GUARD_FLAG]) {
    return
  }

  ROUTE_METHODS.forEach((method) => {
    if (typeof uni[method] !== 'function') {
      return
    }
    const rawMethod = uni[method].bind(uni)
    uni[method] = async (options = {}) => {
      if (options?.[SKIP_FLAG]) {
        return rawMethod(options)
      }
      const allowed = await guardRouteAccess(options?.url || '', {
        method,
        rawMethod,
        options,
      })
      if (allowed) {
        return rawMethod(options)
      }
      return false
    }
  })

  uni[GUARD_FLAG] = true
}

export async function ensureLaunchRouteAccess() {
  const launchUrl = getLaunchUrl()
  if (!launchUrl) {
    return true
  }
  return guardRouteAccess(launchUrl, {
    launch: true,
    method: 'reLaunch',
    rawMethod: uni?.reLaunch?.bind?.(uni) || null,
    options: { url: launchUrl },
  })
}

async function guardRouteAccess(url, context = {}) {
  const path = normalizeRoutePath(url)
  if (!path) {
    return true
  }

  const access = resolvePageAccess(path)
  const authStore = useAuthStore()

  if (access.public) {
    if (path === LOGIN_PAGE && authStore.isLogin) {
      routeTo(HOME_PAGE, 'reLaunch')
      return false
    }
    return true
  }

  if (!authStore.isLogin) {
    // 启动或拦截时如果目标已是登录页，直接放行，避免 redirect 自嵌套刷新。
    if (isLoginRoute(url)) {
      return true
    }
    if (!context.launch) {
      notify({
        title: '需要登录',
        description: '当前页面需要先登录后访问',
        type: 'warning',
      })
    }
    routeTo(buildLoginUrl(url), 'reLaunch')
    return false
  }

  if (!authStore.userInfo) {
    try {
      await authStore.fetchUserInfo()
    }
    catch (error) {
      authStore.resetAuth()
      if (isLoginRoute(url)) {
        return true
      }
      routeTo(buildLoginUrl(url), 'reLaunch')
      return false
    }
  }

  if (!authStore.menus.length && !authStore.permissions.length) {
    try {
      await authStore.fetchAccessSnapshot()
    }
    catch (error) {
      console.warn('[navigation-guard] 读取访问快照失败:', error)
    }
  }

  const requiredPermissions = access.permissions || []
  if (requiredPermissions.length && !hasPermission(authStore.permissions, requiredPermissions, access.permissionMode || 'any')) {
    notify({
      title: '无权限',
      description: access.denyMessage || '你没有访问当前页面的权限',
      type: 'warning',
    })
    routeTo(access.denyRedirect || HOME_PAGE, 'reLaunch')
    return false
  }

  return true
}

function routeTo(url, method = 'reLaunch') {
  if (typeof uni === 'undefined') {
    return
  }

  const target = { url, [SKIP_FLAG]: true }
  if (typeof uni[method] === 'function') {
    uni[method](target)
    return
  }
  if (typeof uni.reLaunch === 'function') {
    uni.reLaunch(target)
  }
}

function getLaunchUrl() {
  if (typeof uni === 'undefined' || typeof uni.getEnterOptionsSync !== 'function') {
    return ''
  }
  const options = uni.getEnterOptionsSync() || {}
  const path = normalizeRoutePath(options.path || '')
  if (!path) {
    return ''
  }
  const query = options.query || {}
  const queryString = Object.keys(query)
    .filter(key => query[key] !== undefined && query[key] !== null && query[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(query[key])}`)
    .join('&')
  return queryString ? `${path}?${queryString}` : path
}
