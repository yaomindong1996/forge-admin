<template>
  <div
    id="menu-collapse"
    class="menu-collapse-button"
    :title="appStore.collapsed ? '展开菜单' : '收起菜单'"
    @click="appStore.switchCollapsed"
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
  color: inherit;
  background: transparent;
  border: none;
  flex-shrink: 0;
}

.menu-collapse-button:hover {
  background: rgba(128, 128, 128, 0.1);
}

.menu-collapse-button:active {
  background: rgba(128, 128, 128, 0.2);
}
</style>
