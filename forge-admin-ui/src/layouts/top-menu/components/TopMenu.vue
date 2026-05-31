<template>
  <div
    ref="wrapRef"
    class="top-menu-scroll-wrap"
    :class="{ 'can-scroll-left': canScrollLeft, 'can-scroll-right': canScrollRight }"
  >
    <button
      v-show="canScrollLeft"
      class="menu-scroll-btn left"
      type="button"
      aria-label="向左查看更多菜单"
      @click="scrollMenu('left')"
    >
      <i class="i-material-symbols:chevron-left-rounded" />
    </button>
    <div ref="scrollRef" class="top-menu-scroll-track" @scroll="updateScrollState">
      <n-menu
        class="dropdown-menu"
        mode="horizontal"
        :options="menuOptions"
        :value="activeKey"
        :theme-overrides="topMenuThemeOverrides"
        @update:value="handleMenuSelect"
      />
    </div>
    <button
      v-show="canScrollRight"
      class="menu-scroll-btn right"
      type="button"
      aria-label="向右查看更多菜单"
      @click="scrollMenu('right')"
    >
      <i class="i-material-symbols:chevron-right-rounded" />
    </button>
  </div>
</template>

<script setup>
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import IconRenderer from '@/components/IconRenderer.vue'
import { useMenu } from '@/composables'
import { usePermissionStore } from '@/store'
import { getTopMenuThemeOverrides } from '@/utils/menu-theme.js'
import { findMenuIdByPath, processTopMenus } from '@/utils/menu-utils'

const route = useRoute()
const permissionStore = usePermissionStore()

const { handleMenuSelect: baseHandleMenuSelect } = useMenu()
const wrapRef = ref(null)
const scrollRef = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)
let resizeObserver = null

// Top menu theme override
const topMenuThemeOverrides = computed(() => getTopMenuThemeOverrides())

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
  const menus = permissionStore.menus || []
  const topMenus = processTopMenus(menus)

  return topMenus.map((item) => {
    const children = item.children ? processDropdownMenuData(item.children) : []
    return {
      ...item,
      key: item.id,
      label: item.label || item.name,
      children: children.length ? children : null,
    }
  })
})

function processDropdownMenuData(menuItems, parentLabels = []) {
  if (!menuItems || !Array.isArray(menuItems)) {
    return []
  }

  return menuItems.flatMap((item) => {
    const label = item.name || item.label || ''
    const children = item.children?.length
      ? processDropdownMenuData(item.children, [...parentLabels, label])
      : []

    if (item.type === 'module') {
      return children
    }

    const menuItem = {
      key: item.key || String(item.id),
      label: parentLabels.length ? `${parentLabels.join(' / ')} / ${label}` : label,
      icon: renderMenuIcon(item.icon),
    }

    if (item.path) {
      menuItem.path = item.path
    }

    return item.path ? [menuItem] : children
  })
}

// Active menu key based on route
const activeKey = computed(() => {
  return findMenuIdByPath(menuOptions.value, route.path) || route.path
})

// Wrapper for menu select to integrate with base composable
function handleMenuSelect(key) {
  baseHandleMenuSelect(key)
}

function updateScrollState() {
  const el = scrollRef.value
  if (!el)
    return

  const maxScrollLeft = Math.max(0, el.scrollWidth - el.clientWidth)
  canScrollLeft.value = el.scrollLeft > 1
  canScrollRight.value = el.scrollLeft < maxScrollLeft - 1
}

function scrollMenu(direction) {
  const el = scrollRef.value
  if (!el)
    return

  const distance = Math.max(160, Math.floor(el.clientWidth * 0.65))
  el.scrollBy({
    left: direction === 'left' ? -distance : distance,
    behavior: 'smooth',
  })
}

onMounted(() => {
  nextTick(updateScrollState)
  resizeObserver = new ResizeObserver(updateScrollState)
  if (wrapRef.value)
    resizeObserver.observe(wrapRef.value)
  if (scrollRef.value)
    resizeObserver.observe(scrollRef.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
})

watch(menuOptions, () => nextTick(updateScrollState), { deep: true })
</script>

<style scoped>
.top-menu-scroll-wrap {
  position: relative;
  min-width: 0;
  width: 100%;
}

.top-menu-scroll-track {
  min-width: 0;
  width: 100%;
  overflow-x: auto !important;
  overflow-y: hidden !important;
  scrollbar-width: none;
  scroll-behavior: smooth;
}

.top-menu-scroll-track::-webkit-scrollbar {
  display: none;
}

.dropdown-menu {
  width: max-content !important;
  max-width: none !important;
  overflow: visible !important;
}

.dropdown-menu :deep(.n-menu-item),
.dropdown-menu :deep(.n-submenu) {
  flex: 0 0 auto;
}

.menu-scroll-btn {
  position: absolute;
  top: 50%;
  z-index: 5;
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 50%;
  color: var(--top-menu-text-color, #fff);
  background: rgba(15, 23, 42, 0.28);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.16);
  cursor: pointer;
  transform: translateY(-50%);
}

.menu-scroll-btn:hover {
  background: rgba(15, 23, 42, 0.42);
}

.menu-scroll-btn.left {
  left: 0;
}

.menu-scroll-btn.right {
  right: 0;
}

.top-menu-scroll-wrap.can-scroll-left::before,
.top-menu-scroll-wrap.can-scroll-right::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  z-index: 4;
  width: 38px;
  pointer-events: none;
}

.top-menu-scroll-wrap.can-scroll-left::before {
  left: 0;
  background: linear-gradient(90deg, rgba(15, 23, 42, 0.16), transparent);
}

.top-menu-scroll-wrap.can-scroll-right::after {
  right: 0;
  background: linear-gradient(270deg, rgba(15, 23, 42, 0.16), transparent);
}
</style>
