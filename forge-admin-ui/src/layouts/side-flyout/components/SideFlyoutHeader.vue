<template>
  <header class="side-flyout-header layout-chrome-header">
    <ResponsiveMenuToggle v-if="narrow" />
    <button
      v-if="!narrow && collapsed" type="button" class="chrome-icon-button"
      aria-label="展开菜单" title="展开菜单" @click="appStore.collapsed = false"
    >
      <i class="i-line-md-menu-unfold-left" />
    </button>
    <div class="side-flyout-brand">
      <TheLogo />
      <TheTitle />
    </div>
    <HeaderTools class="side-flyout-tools" show-appearance />
  </header>
</template>

<script setup>
import TheLogo from '@/components/common/TheLogo.vue'
import TheTitle from '@/components/common/TheTitle.vue'
import HeaderTools from '@/layouts/components/HeaderTools.vue'
import ResponsiveMenuToggle from '@/layouts/components/ResponsiveMenuToggle.vue'
import { useAppStore } from '@/store'

defineProps({
  narrow: Boolean,
  collapsed: Boolean,
})

const appStore = useAppStore()
</script>
