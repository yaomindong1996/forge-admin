import api from '@/api'
import { request } from '@/utils'
import { initKeyExchange } from '@/utils/crypto/key-exchange'
import { useAuthStore, usePermissionStore, useUserStore, useTenantStore, useAppStore } from '@/store'
import { getPermissions, getUserInfo } from '@/store/helper'
import { lStorage, getFileUrl, initWebSocketClient } from '@/utils'
import { WHITE_LIST } from '@/config/whitelist.config.js'

// 应用租户配置
function applyTenantConfig(tenantConfig, appStore) {
  const tenantStore = useTenantStore()

  // 1. 应用系统布局
  if (tenantConfig.systemLayout) {
    appStore.setLayout(tenantConfig.systemLayout)
  }

  // 2. 应用完整的主题配置
  const themeConfigObj = tenantStore.themeConfig
  if (themeConfigObj) {
    // 导入默认配置
    import('@/config/theme.config').then(({ defaultThemeConfig }) => {
      // 深度合并：租户配置覆盖默认配置
      // 优先使用 systemTheme，如果没有才使用 themeConfig.primaryColor
      const primaryColor = tenantConfig.systemTheme || themeConfigObj.primaryColor || defaultThemeConfig.primaryColor

      const mergedConfig = {
        primaryColor: primaryColor,
        header: {
          ...defaultThemeConfig.header,
          ...themeConfigObj.header
        },
        headerDark: {
          ...defaultThemeConfig.headerDark,
          ...themeConfigObj.headerDark
        },
        topMenu: {
          ...defaultThemeConfig.topMenu,
          ...themeConfigObj.topMenu
        },
        topMenuDark: {
          ...defaultThemeConfig.topMenuDark,
          ...themeConfigObj.topMenuDark
        },
        sideMenu: {
          ...defaultThemeConfig.sideMenu,
          ...themeConfigObj.sideMenu
        },
        sideMenuDark: {
          ...defaultThemeConfig.sideMenuDark,
          ...themeConfigObj.sideMenuDark
        }
      }

      appStore.setThemeConfig(mergedConfig)
    })
  } else if (tenantConfig.systemTheme) {
    // 如果没有完整的主题配置，但有 systemTheme，直接应用
    appStore.setPrimaryColor(tenantConfig.systemTheme)
    appStore.setThemeColor(tenantConfig.systemTheme)
  }

  // 4. 应用浏览器标题
  if (tenantConfig.browserTitle) {
    document.title = tenantConfig.browserTitle
  }

  // 5. 应用浏览器图标
  if (tenantConfig.browserIcon) {
    const link = document.querySelector("link[rel*='icon']") || document.createElement('link')
    link.type = 'image/x-icon'
    link.rel = 'shortcut icon'
    // 使用 getFileUrl 转换 fileId 为完整的下载 URL
    const iconUrl = tenantConfig.browserIcon
    if (iconUrl.startsWith('http://') || iconUrl.startsWith('https://') || iconUrl.startsWith('data:')) {
      link.href = iconUrl
    } else {
      link.href = getFileUrl(iconUrl)
    }
    document.getElementsByTagName('head')[0].appendChild(link)
  }
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
        next({ path: '/login', query: { ...to.query, redirect: to.path } })
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
        } catch (error) {
          // token 无效，清除 token 并允许访问登录页面
          console.log('Token 验证失败，允许访问登录页面')
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
            getPermissions()
          ])
          userStore.setUser(user)
          // 同时存储到localStorage用于持久化
          const { userInfo, staffInfo, dataPermission } = user
          lStorage.set('userInfo', userInfo || {})
          lStorage.set('staffInfo', staffInfo || {})
          lStorage.set('dataPermission', dataPermission || [])

          // 获取租户配置（使用用户的租户ID）
          const tenantConfig = await tenantStore.loadTenantConfig(userInfo?.tenantId)

          // 应用租户配置
          if (tenantConfig) {
            applyTenantConfig(tenantConfig, appStore)
          }
          permissionStore.setPermissions(permissions)

          // 获取并设置菜单数据
          console.log('开始获取菜单数据')
          const res = await api.getMenu(1)
          console.log('获取菜单接口响应:', res)
          if (res.code === 200 && res.data) {
            console.log('设置菜单数据')
            permissionStore.setMenuData(res.data)
          } else {
            console.error('菜单数据获取失败或格式不正确:', res)
          }

          // 在成功获取用户信息和权限后初始化 WebSocket 客户端
          initWebSocketClient()
        } catch (error) {
          console.error('获取用户信息或菜单数据失败:', error)
          // 即使获取失败也继续，避免阻塞页面访问
        }

        // unplugin-vue-router 自动处理路由，无需手动注册
        appStore.setRouteGuardCompleted(true)
        next({ ...to, replace: true })
        return
      }

      // 用户信息已存在，但菜单数据可能为空，需要重新获取用户信息和菜单数据
      if (!permissionStore.menuDataLoaded) {
        try {
          const tenantStore = useTenantStore()
          // 重新获取用户信息和权限
          const [user, permissions] = await Promise.all([
            getUserInfo(),
            getPermissions()
          ])
          userStore.setUser(user)
          // 同时存储到localStorage用于持久化
          const { userInfo, staffInfo, dataPermission } = user
          lStorage.set('userInfo', userInfo || {})
          lStorage.set('staffInfo', staffInfo || {})
          lStorage.set('dataPermission', dataPermission || [])

          // 获取租户配置（使用用户的租户ID）
          const tenantConfig = await tenantStore.loadTenantConfig(userInfo?.tenantId)

          // 应用租户配置
          if (tenantConfig) {
            applyTenantConfig(tenantConfig, appStore)
          }
          permissionStore.setPermissions(permissions)

          // 重新获取菜单数据
          const res = await api.getMenu(1)
          if (res.code === 200 && res.data) {
            permissionStore.setMenuData(res.data)
          } else {
            console.error('重新获取菜单数据失败或格式不正确:', res)
          }
        } catch (error) {
          console.error('重新获取用户信息或菜单数据失败:', error)
        }
      }

      // unplugin-vue-router 自动处理路由，直接放行
      appStore.setRouteGuardCompleted(true)
      next()
    } catch (error) {
      console.error('路由守卫发生错误:', error)
      appStore.setRouteGuardCompleted(true)
      next({ name: '404', query: { path: to.fullPath } })
    }
  })
}
