<template>
  <div class="modern-sidebar">
    <SideLogo />
    <SideMenu class="sidebar-menu" />
  </div>
</template>

<script setup>
import { SideLogo, SideMenu } from '@/layouts/components'
</script>

<style scoped>
.modern-sidebar {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  margin-top: 8px;
}

/* 滚动条样式 */
.sidebar-menu::-webkit-scrollbar {
  width: 6px;
}

.sidebar-menu::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  transition: background 0.2s ease;
}

.sidebar-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}
</style>
