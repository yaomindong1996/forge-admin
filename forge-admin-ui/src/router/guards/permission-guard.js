import api from '@/api'
import { WHITE_LIST } from '@/config/whitelist.config.js'
import { useAppStore, useAuthStore, usePermissionStore, useTenantStore, useUserStore } from '@/store'
import { getPermissions, getUserInfo } from '@/store/helper'
import { initWebSocketClient, lStorage, request } from '@/utils'
import { initKeyExchange } from '@/utils/crypto/key-exchange'
import { applyTenantConfig } from '@/utils/tenant-config'

const AUTH_ROUTE_ALLOWLIST = new Set([
  '/',
  '/home',
  '/profile',
  '/mcp-authorize',
  '/403',
])

const AUTH_ROUTE_PREFIX_ALLOWLIST = [
  '/workspace',
]

const PASSWORD_CHANGE_ROUTE = '/profile'

function normalizeRoutePath(path) {
  const value = String(path || '').trim()
  if (!value)
    return ''
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

function isSameRoutePath(routePath, targetPath) {
  const normalizedRoutePath = normalizeRoutePath(routePath)
  const normalizedTargetPath = normalizeRoutePath(targetPath)
  if (!normalizedRoutePath || !normalizedTargetPath)
    return false
  if (normalizedRoutePath === normalizedTargetPath)
    return true
  if (!normalizedRoutePath.includes(':'))
    return false
  const pattern = normalizedRoutePath
    .split('/')
    .map(segment => segment.startsWith(':') ? '[^/]+' : escapeRegExp(segment))
    .join('/')
  return new RegExp(`^${pattern}$`).test(normalizedTargetPath)
}

function canAccessRoute(to, permissionStore) {
  const targetPath = normalizeRoutePath(to.path)
  if (!targetPath)
    return true
  if (WHITE_LIST.includes(targetPath)
    || AUTH_ROUTE_ALLOWLIST.has(targetPath)
    || AUTH_ROUTE_PREFIX_ALLOWLIST.some(prefix => targetPath === prefix || targetPath.startsWith(`${prefix}/`))) {
    return true
  }
  return (permissionStore.accessRoutes || []).some(route => isSameRoutePath(route.path, targetPath))
}

function buildUnauthorizedRouteTarget(from) {
  const fromPath = normalizeRoutePath(from?.path)
  if (!fromPath || fromPath === '/login')
    return { path: window.$homePath || '/home', replace: true }
  const back = from?.fullPath && fromPath !== '/403' ? from.fullPath : undefined
  return {
    path: '/403',
    replace: true,
    state: {
      from: 'permission-guard',
      ...(back ? { back } : {}),
    },
  }
}

function shouldForcePasswordChange(userStore, to) {
  return userStore.forcePasswordChange && normalizeRoutePath(to.path) !== PASSWORD_CHANGE_ROUTE
}

export function createPermissionGuard(router) {
  router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore()
    const appStore = useAppStore()
    const token = authStore.accessToken

    try {
      /** 没有token */
      if (!token) {
        appStore.setRouteGuardCompleted(true)
        if (WHITE_LIST.includes(to.path)) {
          next()
          return
        }
        // 如果目标路径是登录页，不需要设置 redirect
        if (to.path === '/login') {
          next({ path: '/login' })
        }
        else {
          next({ path: '/login', query: { ...to.query, redirect: to.path } })
        }
        return
      }

      // 有token的情况
      if (to.path === '/login') {
        // 访问登录页面时，先尝试验证 token 是否有效
        // 如果 token 无效，允许继续访问登录页面
        try {
          // 先进行密钥交换
          await initKeyExchange(request)
          // 尝试获取用户信息来验证 token
          const userStore = useUserStore()
          if (!userStore.userInfo) {
            await getUserInfo()
          }
          // token 有效，重定向到首页
          appStore.setRouteGuardCompleted(true)
          next({ path: '/' })
          return
        }
        catch {
          // token 无效，清除 token 并允许访问登录页面
          console.warn('Token 验证失败，允许访问登录页面')
          authStore.resetToken()
          appStore.setRouteGuardCompleted(true)
          next()
          return
        }
      }

      if (WHITE_LIST.includes(to.path)) {
        appStore.setRouteGuardCompleted(true)
        next()
        return
      }

      const userStore = useUserStore()
      const permissionStore = usePermissionStore()

      // 【关键修复】确保在请求任何加密接口前完成密钥交换
      await initKeyExchange(request)

      // 如果没有用户信息，获取用户信息和基础权限
      if (!userStore.userInfo) {
        try {
          const tenantStore = useTenantStore()
          // 先获取用户信息和权限
          const [user, permissions] = await Promise.all([
            getUserInfo(),
            getPermissions(),
          ])
          userStore.setUser(user)
          // 同时存储到localStorage用于持久化
          const { userInfo, staffInfo, dataPermission } = user
          lStorage.set('userInfo', userInfo || {})
          lStorage.set('staffInfo', staffInfo || {})
          lStorage.set('dataPermission', dataPermission || [])

          if (shouldForcePasswordChange(userStore, to)) {
            appStore.setRouteGuardCompleted(true)
            next({ path: PASSWORD_CHANGE_ROUTE, replace: true })
            return
          }

          // 获取租户配置（使用用户的租户ID）
          const tenantConfig = await tenantStore.loadTenantConfig(userInfo?.tenantId)

          // 应用租户配置
          if (tenantConfig) {
            await applyTenantConfig(tenantConfig, appStore)
          }
          permissionStore.setPermissions(permissions)

          // 获取并设置菜单数据
          const res = await api.getMenu(1)
          if (res.code === 200 && res.data) {
            permissionStore.setMenuData(res.data)
          }
          else {
            console.error('菜单数据获取失败或格式不正确:', res)
          }

          // 在成功获取用户信息和权限后初始化 WebSocket 客户端
          initWebSocketClient()
        }
        catch (error) {
          console.error('获取用户信息或菜单数据失败:', error)
          // 即使获取失败也继续，避免阻塞页面访问
        }

        // unplugin-vue-router 自动处理路由，无需手动注册
        appStore.setRouteGuardCompleted(true)
        next({ ...to, replace: true })
        return
      }

      if (shouldForcePasswordChange(userStore, to)) {
        appStore.setRouteGuardCompleted(true)
        next({ path: PASSWORD_CHANGE_ROUTE, replace: true })
        return
      }

      // 用户信息已存在，但菜单数据可能为空，需要重新获取用户信息和菜单数据
      if (!permissionStore.menuDataLoaded) {
        try {
          const tenantStore = useTenantStore()
          // 重新获取用户信息和权限
          const [user, permissions] = await Promise.all([
            getUserInfo(),
            getPermissions(),
          ])
          userStore.setUser(user)
          // 同时存储到localStorage用于持久化
          const { userInfo, staffInfo, dataPermission } = user
          lStorage.set('userInfo', userInfo || {})
          lStorage.set('staffInfo', staffInfo || {})
          lStorage.set('dataPermission', dataPermission || [])

          if (shouldForcePasswordChange(userStore, to)) {
            appStore.setRouteGuardCompleted(true)
            next({ path: PASSWORD_CHANGE_ROUTE, replace: true })
            return
          }

          // 获取租户配置（使用用户的租户ID）
          const tenantConfig = await tenantStore.loadTenantConfig(userInfo?.tenantId)

          // 应用租户配置
          if (tenantConfig) {
            await applyTenantConfig(tenantConfig, appStore)
          }
          permissionStore.setPermissions(permissions)

          // 重新获取菜单数据
          const res = await api.getMenu(1)
          if (res.code === 200 && res.data) {
            permissionStore.setMenuData(res.data)
          }
          else {
            console.error('重新获取菜单数据失败或格式不正确:', res)
          }
        }
        catch (error) {
          console.error('重新获取用户信息或菜单数据失败:', error)
        }
      }

      if (permissionStore.menuDataLoaded && !canAccessRoute(to, permissionStore)) {
        appStore.setRouteGuardCompleted(true)
        next(buildUnauthorizedRouteTarget(from))
        return
      }

      // unplugin-vue-router 自动处理路由，直接放行
      appStore.setRouteGuardCompleted(true)
      next()
    }
    catch (error) {
      console.error('路由守卫发生错误:', error)
      appStore.setRouteGuardCompleted(true)
      next({ name: '404', query: { path: to.fullPath } })
    }
  })
}
