/**
 * 菜单处理工具函数
 */

import { h } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'

function isExternalPath(path) {
  return /^[a-z][a-z\d+.-]*:\/\//i.test(path) || String(path || '').startsWith('//')
}

function normalizeMenuPath(path) {
  const value = String(path || '').trim()
  if (!value)
    return ''
  if (isExternalPath(value))
    return value

  const [pathWithoutHash] = value.split('#')
  const [pathname] = pathWithoutHash.split('?')
  const normalized = pathname.replace(/\/+/g, '/')
  if (!normalized || normalized === '/')
    return normalized
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function routePatternMatches(patternPath, targetPath) {
  if (!patternPath.includes(':'))
    return false

  const pattern = patternPath
    .split('/')
    .map(segment => segment.startsWith(':') ? '[^/]+' : escapeRegExp(segment))
    .join('/')
  return new RegExp(`^${pattern}$`).test(targetPath)
}

export function isSameMenuPath(menuPath, targetPath) {
  const normalizedMenuPath = normalizeMenuPath(menuPath)
  const normalizedTargetPath = normalizeMenuPath(targetPath)
  if (!normalizedMenuPath || !normalizedTargetPath)
    return false
  if (normalizedMenuPath === normalizedTargetPath)
    return true
  return routePatternMatches(normalizedMenuPath, normalizedTargetPath)
    || routePatternMatches(normalizedTargetPath, normalizedMenuPath)
}

function getMenuPathMatchScore(menuPath, targetPath) {
  const normalizedMenuPath = normalizeMenuPath(menuPath)
  const normalizedTargetPath = normalizeMenuPath(targetPath)
  if (!normalizedMenuPath || !normalizedTargetPath)
    return 0
  if (normalizedMenuPath === normalizedTargetPath)
    return 2
  if (routePatternMatches(normalizedMenuPath, normalizedTargetPath)
    || routePatternMatches(normalizedTargetPath, normalizedMenuPath)) {
    return 1
  }
  return 0
}

/**
 * 生成唯一ID
 * @param {string} prefix 前缀
 * @returns {string} 唯一ID
 */
function generateUniqueId(prefix = 'menu_') {
  return `${prefix + Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

/**
 * 处理顶级菜单数据，直接返回所有顶级菜单项
 * @param {Array} menuItems 原始菜单数据
 * @returns {Array} 处理后的顶级菜单数据
 */
export function processTopMenus(menuItems) {
  if (!menuItems || !Array.isArray(menuItems)) {
    return []
  }

  // 直接处理所有顶级菜单项，保持原有结构
  return menuItems.map((item, index) => {
    const processedItem = {
      ...item,
      id: item.id || generateUniqueId(`menu_${index}_`),
      key: item.key || item.id || generateUniqueId(`menu_key_${index}_`),
      label: item.name || item.label || '',
      icon: item.icon
        ? () => h(IconRenderer, {
            icon: item.icon,
          })
        : () => '',
    }

    // 如果有子菜单，递归处理
    if (item.children && item.children.length > 0) {
      processedItem.children = item.children.map((child, childIndex) => ({
        ...child,
        id: child.id || generateUniqueId(`child_${index}_${childIndex}_`),
        key: child.key || child.id || generateUniqueId(`child_key_${index}_${childIndex}_`),
        label: child.name || child.label || '',
        icon: child.icon
          ? () => h(IconRenderer, {
              icon: child.icon,
            })
          : () => '',
      }))
    }

    return processedItem
  })
}

/**
 * 处理菜单数据，转换为适合显示的格式
 * @param {Array} menuItems 菜单数据
 * @returns {Array} 处理后的菜单数据
 */
export function processMenuData(menuItems) {
  if (!menuItems || !Array.isArray(menuItems)) {
    return []
  }

  // 展平处理：过滤掉 type 为 subapp 的菜单项，但保留其 children
  const flattenedItems = menuItems.reduce((acc, item) => {
    if (item.type === 'subapp') {
      // 如果是 subapp 类型，将其 children 提升到当前层级
      if (item.children && item.children.length > 0) {
        acc.push(...item.children)
      }
    }
    else {
      // 不是 subapp 类型，正常保留
      acc.push(item)
    }
    return acc
  }, [])

  return flattenedItems
    .filter((item) => {
      if (item.type === 'module' && (!item.children || item.children.length === 0)) {
        return false
      }
      return true
    })
    .map((item, index) => {
    // 确保每个菜单项都有唯一ID和label
      const isModule = item.type === 'module'
      const menuItem = {
        key: String(item.key || item.id || generateUniqueId(`item_key_${index}_`)),
        label: item.name || item.label || '',
      }

      // 设置图标 - 处理字符串和函数两种情况
      if (item.icon) {
        if (typeof item.icon === 'function') {
          menuItem.icon = item.icon
        }
        else if (typeof item.icon === 'string' && item.icon.trim() !== '' && item.icon !== '-1') {
          menuItem.icon = () => h(IconRenderer, {
            icon: item.icon,
          })
        }
      }

      if (item.path && !isModule) {
        menuItem.path = item.path
      }

      // 处理子菜单
      if (item.children && item.children.length > 0) {
        const children = processMenuData(item.children)
        if (children.length > 0) {
          menuItem.children = children
        }
      }

      return menuItem
    })
}

/**
 * 查找当前活跃的顶级菜单
 * @param {Array} menus 菜单数据
 * @param {object} route 当前路由
 * @returns {object | null} 活跃的顶级菜单
 */
export function findActiveTopMenu(menus, route) {
  // 处理菜单数据，提取所有菜单项的子菜单作为一级菜单
  const topMenus = processTopMenus(menus)

  const findActiveMenu = (menuItems, parent = null) => {
    for (const item of menuItems) {
      // 检查当前项是否匹配当前路由
      if (item.path === route.path) {
        // 找到匹配的菜单项
        return parent || item
      }

      // 如果当前项有子菜单，递归查找
      if (item.children && item.children.length > 0) {
        const found = findActiveMenu(item.children, item)
        if (found) {
          return found
        }
      }
    }
    return null
  }

  const result = findActiveMenu(topMenus)
  return result
}

/**
 * 根据菜单ID查找菜单项
 * @param {Array} menuItems 菜单数据
 * @param {string} key 菜单ID
 * @returns {object | null} 找到的菜单项
 */
export function findMenuItem(menuItems, key) {
  if (!menuItems || !Array.isArray(menuItems))
    return null

  const matchKey = (itemKey, searchKey) => {
    return itemKey === searchKey
      || String(itemKey) === String(searchKey)
      || Number(itemKey) === Number(searchKey)
  }

  for (const item of menuItems) {
    const itemId = item.key || item.id
    if (matchKey(itemId, key)) {
      return item
    }
    if (item.children && item.children.length > 0) {
      const found = findMenuItem(item.children, key)
      if (found)
        return found
    }
  }
  return null
}

/**
 * 根据路由路径查找匹配的菜单ID，支持 /path/:param 动态路由。
 * 精确路径优先于动态路径，避免通用渲染页抢占具体业务菜单高亮。
 *
 * @param {Array} menuItems 菜单数据
 * @param {string} targetPath 当前路由路径
 * @returns {string | number | null} 匹配的菜单 key
 */
export function findMenuIdByPath(menuItems, targetPath) {
  if (!menuItems || !Array.isArray(menuItems))
    return null

  let bestKey = null
  let bestScore = 0

  const visit = (items) => {
    for (const item of items) {
      const score = getMenuPathMatchScore(item.path, targetPath)
      if (score > bestScore) {
        bestKey = item.key ?? item.id
        bestScore = score
        if (score === 2)
          return true
      }

      if (item.children && item.children.length > 0) {
        const foundExact = visit(item.children)
        if (foundExact)
          return true
      }
    }
    return false
  }

  visit(menuItems)
  return bestKey
}
