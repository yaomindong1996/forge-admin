<template>
  <div class="sidebar-simple-wrapper">
    <SideLogo />
    <SideMenu class="sidebar-simple-menu" />
    <div class="sidebar-simple-footer">
      <MessageNotification />
      <CompactLayoutTools />
      <MenuCollapse />
    </div>
  </div>
</template>

<script setup>
import { MenuCollapse, SideLogo, SideMenu } from '@/layouts/components'
import CompactLayoutTools from '@/layouts/components/CompactLayoutTools.vue'
import MessageNotification from '@/layouts/components/MessageNotification.vue'
</script>

<style scoped>
.sidebar-simple-wrapper {
  --chrome-text: var(--side-menu-text-color);
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--side-menu-bg-color, #ffffff);
  color: var(--side-menu-text-color);
}

.sidebar-simple-menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  margin-top: 8px;
}

.sidebar-simple-footer {
  display: flex;
  align-items: center;
  justify-content: space-around;
  flex-wrap: wrap;
  padding: 8px;
  border-top: 1px solid var(--side-menu-border-color, #f1f5f9);
  background: var(--side-menu-bg-color, #ffffff);
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
