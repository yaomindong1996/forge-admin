/**
 * 布局组件菜单相关组合式函数
 * 封装菜单数据处理、活跃项计算和导航跳转逻辑
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePermissionStore } from '@/store'
import { isExternal } from '@/utils'
import { processMenuData, findMenuItem } from '@/utils/menu-utils'

/**
 * 根据路由路径查找匹配的菜单ID
 * @param {Array} items - 已处理的菜单项
 * @param {string} targetPath - 要匹配的路由路径
 * @returns {string|null} 匹配的菜单key或null
 */
function findMenuIdByPath(items, targetPath) {
  if (!items || !Array.isArray(items))
    return null

  for (const item of items) {
    if (item.path === targetPath) {
      return item.key || item.id
    }
    if (item.children && item.children.length > 0) {
      const found = findMenuIdByPath(item.children, targetPath)
      if (found)
        return found
    }
  }
  return null
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
      if (item.path === targetPath) {
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

  if (menuItem.path && !isExternal(menuItem.path)) {
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
  function handleMenuSelect(key) {
    const originalItem = findMenuItem(permissionStore.menus, key)
    if (!originalItem)
      return

    // 外部链接处理
    if (isExternal(originalItem.path)) {
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

    // iframe模式处理
    if (originalItem.openMode === 'iframe' && originalItem.subAppURL) {
      const iframePath = `/iframe?page=${encodeURIComponent(originalItem.subAppURL + (originalItem.path || ''))}`
      router.push(iframePath)
      return
    }

    // 普通路由跳转
    if (originalItem.path) {
      router.push(originalItem.path)
    }
  }

  return {
    processedMenus,
    flatMenuItems,
    activeKey,
    handleMenuSelect,
    /** 根据路径查找菜单ID */
    findMenuIdByPath: (targetPath) => findMenuIdByPath(processedMenus.value, targetPath),
    /** 查找包含指定路径的顶级菜单 */
    findTopMenuByPath: (targetPath) => findTopMenuByPath(permissionStore.menus, targetPath),
  }
}
