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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore, usePermissionStore } from '@/store'
import { useMenu, findTopMenuByPath, findFirstMenuWithPath } from '@/composables'
import { getTopMenuThemeOverrides } from '@/utils/menu-theme.js'
import { processTopMenus } from '@/utils/menu-utils'

const router = useRouter()
const route = useRoute()
const permissionStore = usePermissionStore()
const appStore = useAppStore()

const { handleMenuSelect: baseHandleMenuSelect } = useMenu()

// Top menu theme override
const topMenuThemeOverrides = computed(() => getTopMenuThemeOverrides())

// Extract first-level menu items
const topMenuOptions = computed(() => {
  const menus = permissionStore.menus || []
  const topMenus = processTopMenus(menus)

  return topMenus.map((item) => ({
    ...item,
    key: item.id,
    label: item.label || item.name,
    children: null,
  }))
})

// Active menu key
const activeKey = computed(() => {
  const menus = permissionStore.menus || []

  if (!menus.length || !permissionStore.menuDataLoaded) {
    return null
  }

  const topMenus = processTopMenus(menus)
  const activeTopMenu = findTopMenuByPath(topMenus, route.path)

  if (activeTopMenu) {
    if (appStore.selectedTopMenuId !== activeTopMenu.id) {
      appStore.setSelectedTopMenuId(activeTopMenu.id)
    }
    return activeTopMenu.id
  }

  if (appStore.selectedTopMenuId) {
    return appStore.selectedTopMenuId
  }

  return null
})

const menu = ref(null)

function handleMenuSelect(key, item) {
  const menus = permissionStore.menus || []
  const topMenus = processTopMenus(menus)
  const selectedMenu = topMenus.find(m => m.id === key)

  if (!selectedMenu)
    return

  // Update store for selected top menu
  appStore.setSelectedTopMenuId(key)

  if (selectedMenu.type === 'module') {
    // Module type: find first child with path and navigate
    const firstMenu = findFirstMenuWithPath(selectedMenu)
    if (firstMenu && firstMenu.path) {
      baseHandleMenuSelect(firstMenu.key || firstMenu.id)
    }
    return
  }

  if (selectedMenu.type === 'menu') {
    // Menu type: navigate directly
    if (selectedMenu.path) {
      baseHandleMenuSelect(selectedMenu.key || selectedMenu.id)
    }
  }
}
</script>

<style>
.top-menu {
  border: none !important;
}
</style>
