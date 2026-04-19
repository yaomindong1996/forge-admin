/**
 * 菜单处理工具函数
 */

import { h } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'

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

  return flattenedItems.map((item, index) => {
    // 确保每个菜单项都有唯一ID和label
    const menuItem = {
      key: String(item.key || item.id || generateUniqueId(`item_key_${index}_`)),
      label: item.name || item.label || '',
    }

    // 设置图标 - 处理字符串和函数两种情况
    if (item.icon) {
      if (typeof item.icon === 'function') {
        // 如果已经是函数，直接使用
        menuItem.icon = item.icon
      }
      else if (typeof item.icon === 'string' && item.icon.trim() !== '' && item.icon !== '-1') {
        // 如果是字符串，转换为渲染函数
        menuItem.icon = () => h(IconRenderer, {
          icon: item.icon,
        })
      }
    }

    // 设置路由路径
    if (item.path) {
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

  for (const item of menuItems) {
    if ((item.key || item.id) === key) {
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
