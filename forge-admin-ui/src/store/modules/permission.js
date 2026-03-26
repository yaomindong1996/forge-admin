import { hyphenate } from '@vueuse/core'
import { defineStore } from 'pinia'
import { isExternal } from '@/utils'

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    accessRoutes: [],
    permissions: [],
    menus: [],
    // 所有菜单（包括隐藏的），用于标题查找
    allMenus: [],
    // 添加菜单数据加载状态
    menuDataLoaded: false,
  }),
  actions: {
    setPermissions(permissions) {
      this.permissions = permissions
      this.menus = this.permissions
        .filter(item => item.type === 'MENU')
        .map(item => this.getMenuItem(item))
        .filter(item => !!item)
        .sort((a, b) => (a.order || 0) - (b.order || 0))
    },

    // 设置新的菜单数据（从新接口获取）
    setMenuData(menuData) {
      this.menus = this.processMenuData(menuData)
      // 从菜单数据中提取路由并生成 accessRoutes（可见菜单）
      this.generateAccessRoutesFromMenus(this.menus)
      // 从原始数据中提取隐藏菜单的路由，确保隐藏页面也有 meta.title
      this.generateHiddenMenuRoutes(menuData)
      // 设置菜单数据加载完成状态
      this.menuDataLoaded = true
      
      // 检查是否存在window.$homePath指定的路由
      if (menuData && menuData.length > 0) {
        const homePath = window.$homePath
        let homePathExists = false
        
        // 递归查找所有菜单项，检查是否存在指定的homePath
        const findHomePath = (menus) => {
          for (const menu of menus) {
            if (menu.path === homePath) {
              homePathExists = true
              return true
            }
            if (menu.children && menu.children.length > 0) {
              if (findHomePath(menu.children)) {
                return true
              }
            }
          }
          return false
        }
        
        findHomePath(menuData)
        
        // 如果没有找到指定的homePath，则使用第一个菜单项的路径
        if (!homePathExists) {
          const getFirstLeafPath = (menus) => {
            if (!menus || menus.length === 0) return '/'
            const firstMenu = menus[0]
            if (firstMenu.path) {
              return firstMenu.path
            }
            if (firstMenu.children && firstMenu.children.length > 0) {
              return getFirstLeafPath(firstMenu.children)
            }
            return '/'
          }
          
          window.$homePath = getFirstLeafPath(menuData)
        }
      }
    },

    // 重置菜单数据加载状态
    resetMenuDataLoaded() {
      this.menuDataLoaded = false
    },

    // 从新菜单数据结构生成路由
    generateAccessRoutesFromMenus(menuItems) {
      const routes = []

      const extractRoutes = (items) => {
        if (!items || !Array.isArray(items)) return

        items.forEach(item => {
          // 只为菜单类型(非目录)且有 path 和 component 的项生成路由
          if (item.type === 'menu' && item.path && item.component) {
            // 确保路径以 "/" 开头
            let routePath = item.path.trim()
            if (!routePath.startsWith('/') && !isExternal(routePath)) {
              routePath = '/' + routePath
            }

            // 处理外链
            let componentPath = item.component
            let originPath = null
            if (item.meta?.isExternal) {
              originPath = routePath
              componentPath = '/src/views/iframe/index.vue'
            }

            const route = {
              name: item.key || String(item.id),
              path: routePath,
              component: componentPath,
              meta: {
                title: item.label || item.name,
                icon: item.icon,
                type: item.type,
                keepAlive: item.meta?.keepAlive || false,
                alwaysShow: item.meta?.alwaysShow || false,
                redirect: item.meta?.redirect,
                originPath: originPath,
                perms: item.meta?.perms,
              }
            }
            routes.push(route)
          }

          // 递归处理子菜单
          if (item.children && item.children.length > 0) {
            extractRoutes(item.children)
          }
        })
      }

      extractRoutes(menuItems)
      // 替换 accessRoutes（不是合并，因为这是从菜单数据生成的完整路由）
      this.accessRoutes = routes
    },

    // 处理新的菜单数据结构 - 适配 UserResourceTreeVO
    processMenuData(menuItems) {
      if (!menuItems || !Array.isArray(menuItems)) {
        console.log('菜单数据为空或不是数组')
        return []
      }

      // 收集所有菜单（包括隐藏的）到扁平数组，用于标题查找
      const allMenusFlat = []
      const collectAllMenus = (items) => {
        if (!items || !Array.isArray(items)) return
        for (const item of items) {
          if (item.resourceType === 1 || item.resourceType === 2) {
            // 规范化路径，确保以 / 开头
            let normalizedPath = item.path?.trim() || ''
            if (normalizedPath && !normalizedPath.startsWith('/')) {
              normalizedPath = '/' + normalizedPath
            }
            allMenusFlat.push({
              path: normalizedPath,
              label: item.resourceName,
              name: item.resourceName
            })
          }
          if (item.children && item.children.length > 0) {
            collectAllMenus(item.children)
          }
        }
      }
      collectAllMenus(menuItems)
      this.allMenus = allMenusFlat
      console.log('[菜单] allMenus 收集完成, 共', allMenusFlat.length, '项:', allMenusFlat.map(m => m.path + ' -> ' + m.label))

      // 处理菜单项，将 UserResourceTreeVO 转换为前端菜单格式
      const processItems = (items) => {
        if (!items || !Array.isArray(items)) return []

        return items
          .filter(item => {
            // 只处理目录(1)和菜单(2)类型，过滤掉按钮(3)和API接口(4)
            return item.resourceType === 1 || item.resourceType === 2
          })
          .filter(item => {
            // 过滤掉隐藏的菜单 (visible=0 或 menuStatus=0)
            return item.visible !== 0 && item.menuStatus !== 0
          })
          .map(item => {
            // 处理 component 路径，统一格式为 /src/views/xxx.vue
            let componentPath = ''
            if (item.component) {
              let comp = item.component.trim()
              // 移除开头的 /
              if (comp.startsWith('/')) {
                comp = comp.substring(1)
              }
              // 如果没有 .vue 后缀，添加 .vue
              if (!comp.endsWith('.vue')) {
                comp = comp + '.vue'
              }
              // 添加 /src/views/ 前缀
              componentPath = `/src/views/${comp}`
            }

            const menuItem = {
              id: item.id,
              key: String(item.id),
              label: item.resourceName,
              name: item.resourceName,
              path: item.path || '',
              component: componentPath,
              icon: item.icon || '',
              type: item.resourceType === 1 ? 'module' : 'menu', // 1-目录(module)，2-菜单(menu)
              order: item.sort || 0,
              parentId: item.parentId,
              // 额外的元数据
              meta: {
                title: item.resourceName,
                icon: item.icon,
                keepAlive: item.keepAlive === 1,
                alwaysShow: item.alwaysShow === 1,
                redirect: item.redirect,
                isExternal: item.isExternal === 1,
                perms: item.perms,
              }
            }

            // 处理子菜单
            if (item.children && item.children.length > 0) {
              menuItem.children = processItems(item.children)
            }

            return menuItem
          })
          .sort((a, b) => (a.order || 0) - (b.order || 0)) // 按排序字段排序
      }

      const processedMenus = processItems(menuItems)
      return processedMenus
    },

    // 从原始菜单数据中提取隐藏菜单，生成路由（确保隐藏页面也有 meta.title）
    generateHiddenMenuRoutes(menuItems) {
      const hiddenRoutes = []
      const extract = (items) => {
        if (!items || !Array.isArray(items)) return
        for (const item of items) {
          // 隐藏菜单: resourceType=2(菜单) 且 visible=0 或 menuStatus=0
          if (item.resourceType === 2 && (item.visible === 0 || item.menuStatus === 0)) {
            let componentPath = ''
            if (item.component) {
              let comp = item.component.trim()
              if (comp.startsWith('/')) comp = comp.substring(1)
              if (!comp.endsWith('.vue')) comp += '.vue'
              componentPath = `/src/views/${comp}`
            }
            let routePath = item.path?.trim() || ''
            if (routePath && !routePath.startsWith('/')) {
              routePath = '/' + routePath
            }
            if (routePath && componentPath) {
              hiddenRoutes.push({
                name: String(item.id),
                path: routePath,
                component: componentPath,
                meta: {
                  title: item.resourceName,
                  icon: item.icon,
                  keepAlive: item.keepAlive === 1,
                }
              })
            }
          }
          if (item.children && item.children.length > 0) {
            extract(item.children)
          }
        }
      }
      extract(menuItems)
      if (hiddenRoutes.length > 0) {
        console.log('[菜单] 发现隐藏菜单路由:', hiddenRoutes.map(r => r.path + ' -> ' + r.meta.title))
        this.accessRoutes = [...this.accessRoutes, ...hiddenRoutes]
      }
    },

    // 转换菜单数据为适合侧边栏显示的格式（简化版本）
    transformMenusForSidebar(menuItems) {
      // 直接返回处理后的数据，不再额外转换
      return menuItems
    },

    getMenuItem(item, parent) {
      const route = this.generateRoute(item, item.show ? null : parent?.key)
      if (item.enable && route.path && !route.path.startsWith('http'))
        this.accessRoutes.push(route)
      const menuItem = {
        label: route.meta.title,
        key: route.name,
        path: route.path,
        originPath: route.meta.originPath,
        icon: () => h('i', { class: `${route.meta.icon} text-16` }),
        order: item.order ?? 0,
      }
      const children = item.children?.filter(item => item.type === 'MENU') || []
      if (children.length) {
        menuItem.children = children
          .map(child => this.getMenuItem(child, menuItem))
          .filter(item => !!item)
          .sort((a, b) => a.order - b.order)
        if (!menuItem.children.length)
          delete menuItem.children
      }
      if (!item.show)
        return null
      return menuItem
    },

    generateRoute(item, parentKey) {
      let originPath
      if (isExternal(item.path)) {
        originPath = item.path
        item.component = '/src/views/iframe/index.vue'
        item.path = `/iframe/${hyphenate(item.code)}`
      }
      return {
        name: item.code,
        path: item.path,
        redirect: item.redirect,
        component: item.component,
        meta: {
          originPath,
          icon: `${item.icon}?mask`,
          title: item.name,
          layout: item.layout,
          keepAlive: !!item.keepAlive,
          parentKey,
          btns: item.children
            ?.filter(item => item.type === 'BUTTON')
            .map(item => ({ code: item.code, name: item.name })),
        },
      }
    },

    resetPermission() {
      this.$reset()
    },
  },
})
