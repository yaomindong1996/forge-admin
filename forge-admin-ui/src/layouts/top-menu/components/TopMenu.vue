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
import { useRoute } from 'vue-router'
import IconRenderer from '@/components/IconRenderer.vue'
import { useMenu } from '@/composables'
import { useAppStore, usePermissionStore } from '@/store'
import { getTopMenuThemeOverrides } from '@/utils/menu-theme.js'
import { processTopMenus } from '@/utils/menu-utils'

const route = useRoute()
const permissionStore = usePermissionStore()
const appStore = useAppStore()

const { handleMenuSelect: baseHandleMenuSelect } = useMenu()

// Top menu theme override
const topMenuThemeOverrides = computed(() => getTopMenuThemeOverrides())

// Process menu data for top menu with dropdown
const menuOptions = computed(() => {
  const menus = permissionStore.menus || []
  const topMenus = processTopMenus(menus)

  return topMenus.map(item => ({
    ...item,
    key: item.id,
    label: item.label || item.name,
    children: item.children ? processMenuData(item.children) : null,
  }))
})

function processMenuData(menuItems) {
  if (!menuItems || !Array.isArray(menuItems)) {
    return []
  }

  return menuItems.map((item) => {
    const menuItem = {
      key: item.id,
      label: item.name || item.label || '',
      icon: item.icon ? () => h(IconRenderer, { icon: item.icon, fontSize: 16 }) : undefined,
    }

    if (item.path) {
      menuItem.path = item.path
    }

    if (item.children && item.children.length > 0) {
      menuItem.children = processMenuData(item.children)
    }

    return menuItem
  })
}

// Active menu key based on route
const activeKey = computed(() => {
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

  return findMenuIdByPath(menuOptions.value, route.path) || route.path
})

// Wrapper for menu select to integrate with base composable
function handleMenuSelect(key) {
  baseHandleMenuSelect(key)
}
</script>

<style scoped>
</style>
