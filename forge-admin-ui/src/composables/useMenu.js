/**
 * Menu-related composable for layout components
 * Encapsulates menu processing, active key computation, and navigation logic
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePermissionStore } from '@/store'
import { isExternal } from '@/utils'
import { processMenuData, findMenuItem } from '@/utils/menu-utils'

/**
 * Find menu ID that matches the given route path
 * @param {Array} items - Processed menu items
 * @param {string} targetPath - Route path to match
 * @returns {string|null} Matching menu key or null
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
 * Find the top-level menu that contains a child matching the given path
 * @param {Array} menus - Raw menu items from permission store
 * @param {string} targetPath - Route path to match
 * @returns {object|null} Top-level menu item containing the path, or null
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
 * Find the first child menu item that has a valid path
 * @param {object} menuItem - Menu item to search within
 * @returns {object|null} First child with a path, or null
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
 * Composable for menu data processing and navigation
 * @returns {object} Menu composable API
 */
export function useMenu() {
  const route = useRoute()
  const router = useRouter()
  const permissionStore = usePermissionStore()

  /**
   * Processed menu data ready for Naive UI menu component
   */
  const processedMenus = computed(() => {
    const menus = permissionStore.menus || []
    return processMenuData(menus)
  })

  /**
   * Flat list of all menu items for easy lookup
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
   * Active menu key based on current route
   */
  const activeKey = computed(() => {
    // Priority 1: Use route.meta.parentKey for hidden secondary pages
    if (route.meta?.parentKey) {
      return route.meta.parentKey
    }

    // Priority 2: Exact path match
    let menuId = findMenuIdByPath(processedMenus.value, route.path)

    // Priority 3: Path prefix match (for hidden menus)
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
   * Handle menu selection with external link and iframe support
   * @param {string} key - Menu item key
   */
  function handleMenuSelect(key) {
    const originalItem = findMenuItem(permissionStore.menus, key)
    if (!originalItem)
      return

    // External link handling
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

    // iframe mode handling
    if (originalItem.openMode === 'iframe' && originalItem.subAppURL) {
      const iframePath = `/iframe?page=${encodeURIComponent(originalItem.subAppURL + (originalItem.path || ''))}`
      router.push(iframePath)
      return
    }

    // Normal route navigation
    if (originalItem.path) {
      router.push(originalItem.path)
    }
  }

  return {
    processedMenus,
    flatMenuItems,
    activeKey,
    handleMenuSelect,
    findMenuIdByPath: (targetPath) => findMenuIdByPath(processedMenus.value, targetPath),
    findTopMenuByPath: (targetPath) => findTopMenuByPath(permissionStore.menus, targetPath),
  }
}
