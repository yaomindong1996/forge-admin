<template>
  <div class="app-header-bar layout-chrome-header">
    <!-- 左侧：折叠按鈕 + Tab -->
    <div class="header-left">
      <MenuCollapse />
      <AppTab class="header-tabs" />
    </div>

    <!-- 右侧：工具区 -->
    <HeaderTools class="header-right" />
  </div>
</template>

<style scoped>
.app-header-bar {
  width: 100%;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--layout-header-bg-color);
  border-bottom: 1px solid var(--layout-header-border-color);
  padding: 0;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  height: 100%;
  padding-left: 4px;
  gap: 2px;
}

.header-tabs {
  flex: 1;
  min-width: 0;
  height: 38px;
  padding: 0 4px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
  padding: 0 8px;
  height: 100%;
}

.header-divider {
  width: 1px;
  height: 14px;
  background: var(--border-light);
  margin: 0 6px;
}
</style>

<script setup>
import { AppTab, MenuCollapse } from '@/layouts/components'
import HeaderTools from '@/layouts/components/HeaderTools.vue'
</script>
