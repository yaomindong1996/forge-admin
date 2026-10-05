<template>
  <div
    ref="rail"
    class="side-flyout-rail"
    @keydown.esc="closeAndFocus"
  >
    <nav class="side-flyout-nav" aria-label="平台一级菜单">
      <button
        v-for="item in menus" :key="item.key" type="button" :data-menu-key="item.key"
        class="side-flyout-item"
        :class="{
          'is-active': highlightedRootKey === item.key,
          'is-open': store.activeMenu === item.key,
        }"
        :aria-expanded="item.children.length ? store.activeMenu === item.key : undefined"
        :aria-controls="item.children.length ? 'side-flyout-panel' : undefined"
        :aria-current="activeRootKey === item.key ? 'true' : undefined"
        :title="item.label"
        @click="activateMenu(item)"
        @keydown.down.prevent="moveFocus($event, 1)"
        @keydown.up.prevent="moveFocus($event, -1)"
      >
        <IconRenderer v-if="typeof item.icon === 'string' && item.icon" :icon="item.icon" :size="20" />
        <component :is="item.icon" v-else-if="item.icon" />
        <i v-else class="i-lucide:layout-grid" />
        <span>{{ item.label }}</span>
      </button>
      <span v-if="!menus.length" class="side-flyout-empty">
        {{ permissionStore.menuDataLoaded ? '暂无菜单' : '加载中' }}
      </span>
    </nav>
    <div class="side-flyout-dock">
      <MenuCollapse />
    </div>
    <WorkbenchMegaPanel variant="rail" />
  </div>
</template>

<script setup>
import { onClickOutside } from '@vueuse/core'
import { nextTick, ref } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import WorkbenchMegaPanel from '@/layouts/business-workbench/components/WorkbenchMegaPanel.vue'
import { useWorkbenchNavigation } from '@/layouts/business-workbench/useWorkbenchNavigation'
import { MenuCollapse } from '@/layouts/components'
import { usePermissionStore } from '@/store'
import { useBusinessWorkbenchStore } from '@/stores/layout/businessWorkbenchStore'

const store = useBusinessWorkbenchStore()
const permissionStore = usePermissionStore()
const { menus, activeRootKey, highlightedRootKey, activateMenu } = useWorkbenchNavigation()
const rail = ref(null)

onClickOutside(rail, () => store.closeMenus())

function moveFocus(event, direction) {
  const buttons = [...(rail.value?.querySelectorAll('.side-flyout-item') || [])]
  const index = buttons.indexOf(event.target)
  if (index >= 0)
    buttons[(index + direction + buttons.length) % buttons.length]?.focus()
}

async function closeAndFocus() {
  const key = store.activeMenu
  store.closeMenus()
  await nextTick()
  const button = [...(rail.value?.querySelectorAll('.side-flyout-item') || [])]
    .find(node => node.dataset.menuKey === key)
  button?.focus()
}
</script>
