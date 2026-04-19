<template>
  <n-menu
    ref="menu"
    class="dropdown-menu"
    mode="horizontal"
    :options="menuOptions"
    :value="activeKey"
    responsive
    :theme-overrides="topMenuThemeOverrides"
    @update:value="handleMenuSelect"
  />
</template>

<script setup>
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IconRenderer from '@/components/IconRenderer.vue'
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

// 处理菜单数据
const menuOptions = computed(() => {
  const menus = permissionStore.menus || []
  // 处理菜单数据，提取所有菜单项的子菜单作为一级菜单
  const topMenus = processTopMenus(menus)

  // 转换为n-menu需要的格式
  const options = topMenus.map((item) => {
    return {
      ...item,
      key: item.id,
      label: item.label || item.name,
      // 保留 children 属性，用于下拉菜单显示
      children: item.children ? processMenuData(item.children) : null,
    }
  })
  return options
})

// 处理菜单数据
function processMenuData(menuItems) {
  if (!menuItems || !Array.isArray(menuItems)) {
    return []
  }

  return menuItems.map((item) => {
    const menuItem = {
      key: item.id,
      label: item.name || item.label || '',
      // 修复：直接传递图标字符串，让naive-ui自己处理渲染
      icon: item.icon ? () => h(IconRenderer, { icon: item.icon, fontSize: 16 }) : undefined,
    }

    // 设置路径
    if (item.path) {
      menuItem.path = item.path
    }

    // 如果有子菜单，递归处理
    if (item.children && item.children.length > 0) {
      menuItem.children = processMenuData(item.children)
    }

    return menuItem
  })
}

// 活跃菜单项
const activeKey = computed(() => {
  // 根据当前路由 path 查找对应的菜单项 id
  const findMenuIdByPath = (items, targetPath) => {
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

  const menuId = findMenuIdByPath(menuOptions.value, route.path)
  return menuId || route.path
})

// 处理菜单选择
function handleMenuSelect(key) {
  // 查找选中的菜单项
  const findMenuItem = (menuItems, targetKey) => {
    if (!menuItems || !Array.isArray(menuItems))
      return null

    for (const item of menuItems) {
      if ((item.key || item.id) === targetKey) {
        return item
      }
      if (item.children && item.children.length > 0) {
        const found = findMenuItem(item.children, targetKey)
        if (found)
          return found
      }
    }
    return null
  }

  // 在所有菜单中查找
  let selectedItem = null
  for (const menu of menuOptions.value) {
    if ((menu.key || menu.id) === key) {
      selectedItem = menu
      break
    }
    if (menu.children) {
      selectedItem = findMenuItem(menu.children, key)
      if (selectedItem)
        break
    }
  }

  if (!selectedItem || !selectedItem.path)
    return

  // 处理外部链接
  if (isExternal(selectedItem.path)) {
    window.$dialog.confirm({
      type: 'info',
      title: `请选择打开方式`,
      positiveText: '外链打开',
      negativeText: '在本站内嵌打开',
      confirm() {
        window.open(selectedItem.path)
      },
      cancel: () => {
        router.push(selectedItem.path)
      },
    })
    return
  }

  // iframe模式处理
  if (selectedItem.openMode === 'iframe' && selectedItem.subAppURL) {
    const iframePath = `/iframe?page=${encodeURIComponent(selectedItem.subAppURL + (selectedItem.path || ''))}`
    router.push(iframePath)
    return
  }

  // 普通路由跳转
  router.push(selectedItem.path)
}
</script>

<style scoped>
</style>
