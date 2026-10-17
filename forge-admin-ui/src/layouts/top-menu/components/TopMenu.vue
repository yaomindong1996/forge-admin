<template>
  <TopMenuBar
    class="top-menu"
    :items="menuOptions"
    :active-key="activeKey"
    dropdown
    @select="handleMenuSelect"
  />
</template>

<script setup>
import { computed, h, onBeforeUnmount, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import IconRenderer from '@/components/IconRenderer.vue'
import { useMenu } from '@/composables'
import TopMenuBar from '@/layouts/components/TopMenuBar.vue'
import { usePermissionStore } from '@/store'
import { useWorkspaceBadgeStore } from '@/stores/flow/workspaceBadgeStore'
import { findMenuIdByPath, processTopMenus } from '@/utils/menu-utils'

const route = useRoute()
const permissionStore = usePermissionStore()
const workspaceBadgeStore = useWorkspaceBadgeStore()

const { handleMenuSelect: baseHandleMenuSelect } = useMenu()

let unsubscribeBadges = null
onMounted(() => {
  unsubscribeBadges = workspaceBadgeStore.subscribe()
})
onBeforeUnmount(() => {
  unsubscribeBadges?.()
  unsubscribeBadges = null
})

function renderMenuIcon(icon) {
  if (!icon)
    return undefined
  if (typeof icon === 'function')
    return icon
  if (typeof icon === 'string' && icon.trim() && icon !== '-1') {
    return () => h(IconRenderer, { icon, fontSize: 16 })
  }
  return undefined
}

// Process menu data for top menu with dropdown
const menuOptions = computed(() => {
  void workspaceBadgeStore.counts
  const menus = permissionStore.menus || []
  const topMenus = processTopMenus(menus)

  const mapped = topMenus.map((item) => {
    const children = item.children ? processDropdownMenuData(item.children) : []
    return {
      ...item,
      key: item.id,
      label: item.label || item.name,
      children: children.length ? children : null,
    }
  })
  return workspaceBadgeStore.attachBadges(mapped)
})

function processDropdownMenuData(menuItems) {
  if (!menuItems || !Array.isArray(menuItems)) {
    return []
  }

  return menuItems.flatMap((item) => {
    const label = item.name || item.label || ''
    const children = item.children?.length
      ? processDropdownMenuData(item.children)
      : []

    const menuItem = {
      key: item.key || String(item.id),
      label,
      icon: renderMenuIcon(item.icon),
    }

    if (item.path) {
      menuItem.path = item.path
    }
    if (children.length) {
      menuItem.children = children
    }

    // 目录节点保留为可展开分组，叶子节点只保留真实可访问路径。
    return item.path || children.length ? [menuItem] : []
  })
}

// Active menu key based on route
const activeKey = computed(() => {
  return findMenuIdByPath(menuOptions.value, route.path) || route.path
})

// Wrapper for menu select to integrate with base composable
function handleMenuSelect(item) {
  baseHandleMenuSelect(item.key, item.path)
}
</script>
