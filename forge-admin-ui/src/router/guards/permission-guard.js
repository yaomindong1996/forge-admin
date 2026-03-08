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
        appStore.setRouteGuardCompleted(true) // 设置路由守卫完成
        if (WHITE_LIST.includes(to.path)) {
          next()
          return
        }
        next({ path: 'login', query: { ...to.query, redirect: to.path } })
        return
      }

      // 有token的情况
      if (to.path === '/login') {
        appStore.setRouteGuardCompleted(true) // 设置路由守卫完成
        next({ path: '/' })
        return
      }
      if (WHITE_LIST.includes(to.path)) {
        appStore.setRouteGuardCompleted(true) // 设置路由守卫完成
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

        // 动态注册路由
        const routeComponents = import.meta.glob('@/views/**/*.vue')
        const validRoutes = []

        permissionStore.accessRoutes.forEach((route) => {
          // 只有当组件存在或有子路由时才注册
          if (routeComponents[route.component] || route.children) {
            route.component = routeComponents[route.component] || route.component
            !router.hasRoute(route.name) && router.addRoute(route)
            validRoutes.push(route)
            console.log('注册路由:', route.path, '组件存在:', !!routeComponents[route.component])
          } else {
            console.warn('路由组件不存在，指向404:', route.path, '期望组件路径:', route.component)
            // 对于不存在的组件，创建指向404的路由
            const notFoundRoute = {
              ...route,
              component: routeComponents['/src/views/error-page/404.vue'],
              meta: {
                ...route.meta,
                title: route.meta?.title || '页面未找到'
              }
            }
            !router.hasRoute(route.name) && router.addRoute(notFoundRoute)
            validRoutes.push(notFoundRoute)
          }
        })

        // 更新accessRoutes为有效路由
        permissionStore.accessRoutes = validRoutes
        console.log('已注册路由数量:', validRoutes.length)

        // 检查目标路由是否已经注册，如果已注册则重新导航
        const routes = router.getRoutes()
        if (routes.find(route => route.name === to.name || route.path === to.path)) {
          appStore.setRouteGuardCompleted(true) // 设置路由守卫完成
          // 刚注册的路由必须重新导航才能生效
          next({ ...to, replace: true })
          return
        }

        // 如果目标路由仍未注册，尝试动态注册
        const componentPath = `/src/views${to.path}.vue`
        if (routeComponents[componentPath]) {
          // 从菜单数据中查找对应的 title 和 parentKey
          let title = ''
          let parentKey = null

          const findMenuInfo = (menus, path) => {
            for (const menu of menus) {
              if (menu.path === path) {
                return {
                  title: menu.label || menu.name || menu.meta?.title || '',
                  parentKey: null
                }
              }
              if (menu.children && menu.children.length > 0) {
                const childInfo = findMenuInfo(menu.children, path)
                if (childInfo) {
                  // 如果在子菜单中找到，设置父级菜单的 key 作为 parentKey
                  return {
                    ...childInfo,
                    parentKey: childInfo.parentKey || menu.key || menu.id
                  }
                }
              }
            }
            return null
          }

          const menuInfo = findMenuInfo(permissionStore.menus, to.path)
          if (menuInfo) {
            title = menuInfo.title
            parentKey = menuInfo.parentKey
          }

          // 如果没有找到菜单信息，尝试根据路径前缀匹配父级菜单
          if (!parentKey) {
            const pathSegments = to.path.split('/').filter(Boolean)
            console.log('尝试为路径查找父级菜单:', to.path, '路径段:', pathSegments)

            // 特殊处理：对于 /system/dictData，查找 /system 下包含 dict 的菜单
            if (pathSegments.length >= 2) {
              const firstSegment = pathSegments[0] // 例如 'system'
              const secondSegment = pathSegments[1] // 例如 'dictData'

              // 查找第一段路径对应的顶级菜单
              const findRelatedMenu = (menus) => {
                for (const menu of menus) {
                  // 检查是否是目标顶级菜单（例如 /system）
                  if (menu.path && menu.path.includes(`/${firstSegment}`)) {
                    // 在其子菜单中查找相关的菜单
                    if (menu.children && menu.children.length > 0) {
                      for (const child of menu.children) {
                        if (child.path) {
                          const childSegments = child.path.split('/').filter(Boolean)
                          // 检查路径是否相似（例如 dictType 和 dictData 都包含 dict）
                          if (childSegments.length >= 2 &&
                              childSegments[0] === firstSegment) {
                            // 提取关键词进行模糊匹配
                            const childKeyword = childSegments[1].toLowerCase()
                            const targetKeyword = secondSegment.toLowerCase()

                            // 如果有共同的前缀（至少4个字符），认为是相关的
                            const commonLength = Math.min(childKeyword.length, targetKeyword.length, 4)
                            if (commonLength >= 4 &&
                                childKeyword.substring(0, commonLength) === targetKeyword.substring(0, commonLength)) {
                              console.log('找到相关菜单:', child.path, '作为', to.path, '的父级')
                              return child.key || child.id
                            }
                          }
                        }
                      }
                    }
                  }

                  // 递归查找
                  if (menu.children && menu.children.length > 0) {
                    const found = findRelatedMenu(menu.children)
                    if (found) return found
                  }
                }
                return null
              }

              parentKey = findRelatedMenu(permissionStore.menus)
              if (parentKey) {
                console.log('通过相关菜单匹配找到父级 key:', parentKey)
              }
            }

            // 如果还是没找到，尝试路径前缀匹配
            if (!parentKey) {
              for (let i = pathSegments.length - 1; i > 0; i--) {
                const parentPath = '/' + pathSegments.slice(0, i).join('/')
                console.log('尝试匹配父级路径:', parentPath)

                const findMenuKey = (menus, path) => {
                  for (const menu of menus) {
                    if (menu.path === path) {
                      return menu.key || menu.id
                    }
                    if (menu.children && menu.children.length > 0) {
                      const childKey = findMenuKey(menu.children, path)
                      if (childKey) return childKey
                    }
                  }
                  return null
                }

                parentKey = findMenuKey(permissionStore.menus, parentPath)
                if (parentKey) {
                  console.log('通过路径前缀找到父级 key:', parentKey)
                  break
                }
              }
            }
          }

          const route = {
            name: to.path.replace(/\//g, '-'),
            path: to.path,
            component: routeComponents[componentPath],
            meta: {
              title: title,
              parentKey: parentKey // 设置父级菜单的 key
            }
          }
          console.log('动态注册路由:', {
            path: to.path,
            title: title,
            parentKey: parentKey
          })
          router.addRoute(route)
          appStore.setRouteGuardCompleted(true) // 设置路由守卫完成
          next({ ...to, replace: true })
          return
        }

        // 组件不存在，跳转 404
        appStore.setRouteGuardCompleted(true) // 设置路由守卫完成
        next({ name: '404', query: { path: to.fullPath } })
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

          // 重新获取菜单数据 - 等待完成
          const res = await api.getMenu(1)
          if (res.code === 200 && res.data) {
            permissionStore.setMenuData(res.data)

            // 重新注册路由
            const routeComponents = import.meta.glob('@/views/**/*.vue')
            const validRoutes = []

            permissionStore.accessRoutes.forEach((route) => {
              // 只有当组件存在或有子路由时才注册
              if (routeComponents[route.component] || route.children) {
                route.component = routeComponents[route.component] || route.component
                !router.hasRoute(route.name) && router.addRoute(route)
                validRoutes.push(route)
              } else {
                console.warn('路由组件不存在，指向404:', route.path, '期望组件路径:', route.component)
                // 对于不存在的组件，创建指向404的路由
                const notFoundRoute = {
                  ...route,
                  component: routeComponents['/src/views/error-page/404.vue'],
                  meta: {
                    ...route.meta,
                    title: route.meta?.title || '页面未找到'
                  }
                }
                !router.hasRoute(route.name) && router.addRoute(notFoundRoute)
                validRoutes.push(notFoundRoute)
              }
            })

            // 更新accessRoutes为有效路由
            permissionStore.accessRoutes = validRoutes

            // 标记需要重新导航，因为这是刷新后重新注册的路由
            // 检查目标路由是否已经注册
            const currentRoutes = router.getRoutes()
            if (currentRoutes.find(r => r.name === to.name || r.path === to.path)) {
              appStore.setRouteGuardCompleted(true)
              // 刷新后重新注册的路由，需要重新导航
              next({ ...to, replace: true })
              return
            }
          } else {
            console.error('重新获取菜单数据失败或格式不正确:', res)
          }
        } catch (error) {
          console.error('重新获取用户信息或菜单数据失败:', error)
        }
      }

      // 确保菜单数据已加载完成后再继续
      // 等待一段时间确保菜单数据加载完成（最多等待5秒）
      let waitCount = 0
      while (!permissionStore.menuDataLoaded && waitCount < 50) {
        await new Promise(resolve => setTimeout(resolve, 100))
        waitCount++
      }

      // 检查路由是否已注册
      const routes = router.getRoutes()
      if (routes.find(route => route.name === to.name || route.path === to.path)) {
        appStore.setRouteGuardCompleted(true) // 设置路由守卫完成
        next()
        return
      }

      // 如果路由未注册，尝试动态注册
      const routeComponents = import.meta.glob('@/views/**/*.vue')
      const componentPath = `/src/views${to.path}.vue`

      if (routeComponents[componentPath]) {
        // 从菜单数据中查找对应的 title 和 parentKey
        let title = ''
        let parentKey = null

        const findMenuInfo = (menus, path) => {
          for (const menu of menus) {
            if (menu.path === path) {
              return {
                title: menu.label || menu.name || menu.meta?.title || '',
                parentKey: null
              }
            }
            if (menu.children && menu.children.length > 0) {
              const childInfo = findMenuInfo(menu.children, path)
              if (childInfo) {
                // 如果在子菜单中找到，设置父级菜单的 key 作为 parentKey
                return {
                  ...childInfo,
                  parentKey: childInfo.parentKey || menu.key || menu.id
                }
              }
            }
          }
          return null
        }

        const menuInfo = findMenuInfo(permissionStore.menus, to.path)
        if (menuInfo) {
          title = menuInfo.title
          // 如果在菜单中找到了，说明是正常的菜单项，不需要 parentKey
          // parentKey 只用于隐藏的二级页面
          parentKey = null
        }

        // 如果没有找到菜单信息（说明是隐藏页面），尝试根据路径前缀匹配父级菜单
        if (!menuInfo) {
          const pathSegments = to.path.split('/').filter(Boolean)

          // 特殊处理：对于 /system/dictData，查找 /system 下包含 dict 的菜单
          if (pathSegments.length >= 2) {
            const firstSegment = pathSegments[0]
            const secondSegment = pathSegments[1]

            // 查找相关菜单
            const findRelatedMenu = (menus) => {
              for (const menu of menus) {
                if (menu.path && menu.path.includes(`/${firstSegment}`)) {
                  if (menu.children && menu.children.length > 0) {
                    for (const child of menu.children) {
                      if (child.path) {
                        const childSegments = child.path.split('/').filter(Boolean)
                        if (childSegments.length >= 2 && childSegments[0] === firstSegment) {
                          const childKeyword = childSegments[1].toLowerCase()
                          const targetKeyword = secondSegment.toLowerCase()
                          const commonLength = Math.min(childKeyword.length, targetKeyword.length, 4)
                          if (commonLength >= 4 &&
                              childKeyword.substring(0, commonLength) === targetKeyword.substring(0, commonLength)) {
                            return child.key || child.id
                          }
                        }
                      }
                    }
                  }
                }
                if (menu.children && menu.children.length > 0) {
                  const found = findRelatedMenu(menu.children)
                  if (found) return found
                }
              }
              return null
            }

            parentKey = findRelatedMenu(permissionStore.menus)
            if (parentKey) {
            }
          }
        }

        const route = {
          name: to.path.replace(/\//g, '-'),
          path: to.path,
          component: routeComponents[componentPath],
          meta: {
            title: title,
            parentKey: parentKey
          }
        }
        router.addRoute(route)
        appStore.setRouteGuardCompleted(true) // 设置路由守卫完成
        next({ ...to, replace: true })
        return
      }

      // 组件不存在，跳转 404
      appStore.setRouteGuardCompleted(true) // 设置路由守卫完成
      next({ name: '404', query: { path: to.fullPath } })
    } catch (error) {
      console.error('路由守卫发生错误:', error)
      appStore.setRouteGuardCompleted(true) // 即使发生错误也要设置完成状态
      next({ name: '404', query: { path: to.fullPath } })
    }
  })
}
