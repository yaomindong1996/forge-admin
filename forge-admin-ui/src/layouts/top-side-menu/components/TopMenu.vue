<template>
  <n-menu
    ref="menu"
    class="top-menu"
    mode="horizontal"
    :options="topMenuOptions"
    :value="activeKey"
    :theme-overrides="topMenuThemeOverrides"
    responsive
    @update:value="handleMenuSelect"
  />
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAppStore, usePermissionStore } from '@/store'
import { isExternal } from '@/utils'
import { getTopMenuThemeOverrides } from '@/utils/menu-theme.js'
import { processTopMenus } from '@/utils/menu-utils'

const router = useRouter()
const route = useRoute()
const permissionStore = usePermissionStore()
const appStore = useAppStore()

// 顶部菜单主题覆盖
const topMenuThemeOverrides = computed(() => getTopMenuThemeOverrides())

// 提取一级菜单项（处理所有菜单项的子菜单）
const topMenuOptions = computed(() => {
  const menus = permissionStore.menus || []
  // 处理菜单数据，提取所有菜单项的子菜单作为一级菜单
  const topMenus = processTopMenus(menus)

  // 转换为n-menu需要的格式
  const options = topMenus.map((item) => {
    return {
      ...item,
      key: item.id,
      label: item.label || item.name,
      children: null,
      // 注意：不设置 children 属性，让所有菜单项都可点击
      // children 属性会导致菜单项变成下拉菜单，不触发 update:value 事件
    }
  })
  return options
})

// 活跃菜单项
const activeKey = computed(() => {
  const menus = permissionStore.menus || []

  // 如果菜单数据还没加载完成，返回 null 避免闪烁
  if (!menus.length || !permissionStore.menuDataLoaded) {
    return null
  }

  const topMenus = processTopMenus(menus)

  // 递归查找包含当前路由的顶级菜单
  const findTopMenuByPath = (currentPath) => {
    // 查找与当前路由匹配的菜单项（递归查找所有层级）
    const findInMenu = (menuItems, topMenu) => {
      for (const item of menuItems) {
        if (item.path === currentPath) {
          return topMenu
        }
        if (item.children && item.children.length > 0) {
          const found = findInMenu(item.children, topMenu)
          if (found) {
            return found
          }
        }
      }
      return null
    }

    // 在每个顶级菜单中查找
    for (const topMenu of topMenus) {
      // 如果顶级菜单本身匹配
      if (topMenu.path === currentPath) {
        return topMenu
      }
      // 在顶级菜单的子菜单中查找
      if (topMenu.children && topMenu.children.length > 0) {
        const found = findInMenu(topMenu.children, topMenu)
        if (found) {
          return found
        }
      }
    }
    return null
  }

  const activeTopMenu = findTopMenuByPath(route.path)

  if (activeTopMenu) {
    // 同步更新store（使用 nextTick 避免重复触发）
    if (appStore.selectedTopMenuId !== activeTopMenu.id) {
      appStore.setSelectedTopMenuId(activeTopMenu.id)
    }
    return activeTopMenu.id
  }

  // 如果没有找到匹配的菜单，使用store中保存的值
  if (appStore.selectedTopMenuId) {
    return appStore.selectedTopMenuId
  }

  // 如果都没有，返回 null 而不是默认选择第一个
  // 避免在没有匹配路由时错误地高亮第一个菜单
  return null
})

const menu = ref(null)

function handleMenuSelect(key, item) {
  // 查找选中的一级菜单
  const menus = permissionStore.menus || []
  // 处理菜单数据，提取所有菜单项的子菜单作为一级菜单
  const topMenus = processTopMenus(menus)
  const selectedMenu = topMenus.find(menu => menu.id === key)

  if (!selectedMenu)
    return

  // 更新store中的选中状态，触发左侧菜单更新
  appStore.setSelectedTopMenuId(key)

  // 根据type字段判断行为
  if (selectedMenu.type === 'module') {
    // module类型：查找第一个有 path 的子菜单并跳转
    const findFirstMenuWithPath = (items) => {
      if (!items || !Array.isArray(items))
        return null

      for (const item of items) {
        // 如果是 menu 类型且有 path，返回
        if (item.type === 'menu' && item.path) {
          return item
        }
        // 如果有子菜单，递归查找
        if (item.children && item.children.length > 0) {
          const found = findFirstMenuWithPath(item.children)
          if (found)
            return found
        }
      }
      return null
    }

    const firstMenu = findFirstMenuWithPath(selectedMenu.children || [])
    if (firstMenu && firstMenu.path) {
      // 处理外部链接
      if (isExternal(firstMenu.path)) {
        $dialog.confirm({
          type: 'info',
          title: `请选择打开方式`,
          positiveText: '外链打开',
          negativeText: '在本站内嵌打开',
          confirm() {
            window.open(firstMenu.path)
          },
          cancel: () => {
            router.push(firstMenu.path)
          },
        })
        return
      }

      // iframe模式处理
      if (firstMenu.openMode === 'iframe' && firstMenu.subAppURL) {
        const iframePath = `/iframe?page=${encodeURIComponent(firstMenu.subAppURL + (firstMenu.path || ''))}`
        router.push(iframePath)
        return
      }

      // 普通路由跳转
      router.push(firstMenu.path)
    }
    return
  }

  if (selectedMenu.type === 'menu') {
    // menu类型：直接跳转
    if (selectedMenu.path) {
      // 处理外部链接
      if (isExternal(selectedMenu.path)) {
        $dialog.confirm({
          type: 'info',
          title: `请选择打开方式`,
          positiveText: '外链打开',
          negativeText: '在本站内嵌打开',
          confirm() {
            window.open(selectedMenu.path)
          },
          cancel: () => {
            router.push(selectedMenu.path)
          },
        })
        return
      }

      // 普通路由跳转
      router.push(selectedMenu.path)
    }
  }
}
</script>

<style>
.top-menu {
  border: none !important;
}
</style>
