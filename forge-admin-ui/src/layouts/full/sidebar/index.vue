<template>
  <div class="sidebar-full-wrapper">
    <SideLogo />
    <SideMenu class="sidebar-full-menu" />
  </div>
</template>

<script setup>
import { SideLogo, SideMenu } from '@/layouts/components'
</script>

<style scoped>
.sidebar-full-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary, #ffffff);
  color: var(--text-primary);
  overflow: hidden;
}

.sidebar-full-menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  margin-top: 8px;
}

/* 滚动条 */
.sidebar-full-menu::-webkit-scrollbar {
  width: 6px;
}

.sidebar-full-menu::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-full-menu::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 10px;
}

.sidebar-full-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}
</style>
