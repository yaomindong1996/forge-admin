<template>
  <div class="sidebar-simple-wrapper">
    <SideLogo />
    <SideMenu class="sidebar-simple-menu" />
    <div class="sidebar-simple-footer">
      <UserAvatar v-if="!appStore.collapsed" />
      <MenuCollapse />
    </div>
  </div>
</template>

<script setup>
import { MenuCollapse, SideLogo, SideMenu, UserAvatar } from '@/layouts/components'
import { useAppStore } from '@/store'

const appStore = useAppStore()
</script>

<style scoped>
.sidebar-simple-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
}

.sidebar-simple-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  margin-top: 8px;
}

.sidebar-simple-footer {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 16px 12px;
  border-top: 1px solid #f1f5f9;
  gap: 8px;
}

/* 滚动条 */
.sidebar-simple-menu::-webkit-scrollbar {
  width: 6px;
}

.sidebar-simple-menu::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-simple-menu::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 10px;
}

.sidebar-simple-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}
</style>
