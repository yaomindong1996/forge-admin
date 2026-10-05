<template>
  <div class="nexus-sidebar">
    <!-- 品牌与所有侧栏共用，保留原始 Logo -->
    <SideLogo />
    <!-- 真实菜单与统一主题，支持任意层级 -->
    <SideMenu class="nexus-navigation" />
  </div>
</template>

<script setup>
import { SideLogo, SideMenu } from '@/layouts/components'
</script>

<style scoped>
.nexus-sidebar {
  --chrome-text: var(--side-menu-text-color);
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--side-menu-bg-color);
  color: var(--side-menu-text-color);
}
.nexus-navigation {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
