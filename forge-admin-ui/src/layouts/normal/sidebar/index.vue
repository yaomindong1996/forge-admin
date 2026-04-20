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
  color: var(--text-primary);
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  margin-top: var(--space-2);
}

/* 滚动条样式 - 使用设计Token */
.sidebar-menu::-webkit-scrollbar {
  width: 6px;
}

.sidebar-menu::-webkit-scrollbar-track {
  background: transparent;
  border-radius: var(--radius-sm);
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: var(--gray-200);
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.sidebar-menu::-webkit-scrollbar-thumb:hover {
  background: var(--gray-300);
}

/* 暗色模式滚动条 */
.dark .sidebar-menu::-webkit-scrollbar-thumb {
  background: var(--gray-700);
}

.dark .sidebar-menu::-webkit-scrollbar-thumb:hover {
  background: var(--gray-600);
}
</style>
