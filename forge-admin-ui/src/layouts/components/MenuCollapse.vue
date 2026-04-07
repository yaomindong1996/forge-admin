<template>
  <div
    id="menu-collapse"
    class="menu-collapse-button"
    @click="appStore.switchCollapsed"
    :title="appStore.collapsed ? '展开菜单' : '收起菜单'"
  >
    <i :class="appStore.collapsed ? 'i-line-md-menu-unfold-left' : 'i-line-md-menu-fold-left'" />
  </div>
</template>

<script setup>
import { useAppStore } from '@/store'

const appStore = useAppStore()
</script>

<style scoped>
.menu-collapse-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: 20px;
  color: var(--bg-primary);
  background: transparent;
  border: none;
  flex-shrink: 0;
}

.menu-collapse-button:hover {
  color: var(--text-primary);
  background: var(--bg-secondary);
}

.menu-collapse-button:active {
  background: var(--bg-tertiary);
}

/* 暗色模式适配 */
.dark .menu-collapse-button {
  color: var(--text-secondary);
}

.dark .menu-collapse-button:hover {
  color: var(--text-primary);
  background: var(--bg-secondary);
}
</style>
