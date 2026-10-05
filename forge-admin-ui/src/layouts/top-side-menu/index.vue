<template>
  <div class="wh-full flex flex-col">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 顶部一级菜单 -->
    <header
      class="layout-header layout-chrome-header top-layout-header flex flex-shrink-0 items-center"
    >
      <div class="brand-section">
        <TheLogo class="brand-logo" />
        <TheTitle class="brand-title" />
      </div>
      <PageRefresh class="header-refresh-action" />
      <TopMenu class="top-menu-wrapper main-top-menu flex-1" />

      <HeaderTools class="header-actions" />
    </header>

    <div class="w-full flex flex-1 overflow-hidden">
      <!-- 左侧二级及以下菜单 - 只在有子菜单时显示 -->
      <aside
        v-if="showSidebar"
        class="side-menu-wrapper flex flex-col flex-shrink-0"
        :class="{ collapsed: appStore.collapsed }"
      >
        <SideMenu />
        <div class="side-collapse-dock">
          <MenuCollapse class="side-collapse-action" />
        </div>
      </aside>

      <!-- 主内容区域 -->
      <article class="w-0 flex flex-col flex-1">
        <AppCard :bordered="false" :padding="false" class="top-side-layout-tab-bar px-8 py-0" shadow="none" radius="none">
          <AppTab class="w-0 flex-1" />
        </AppCard>
        <div class="layout-page-content flex-1" :class="{ 'flow-task-layout-content': isFlowTaskListPage }">
          <slot />
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { AppCard } from '@/components/common'
import DemoBanner from '@/components/DemoBanner.vue'
import { TheTitle } from '@/components/index.js'
import {
  AppTab,
  MenuCollapse,
  PageRefresh,
} from '@/layouts/components'
import HeaderTools from '@/layouts/components/HeaderTools.vue'
import { useSidebarVisibility } from '@/layouts/composables/useSidebarVisibility'
import { useAppStore } from '@/store'
import { isFlowTaskListPath } from '@/utils/flow-task-layout'
import SideMenu from './components/SideMenu.vue'
import TopMenu from './components/TopMenu.vue'

const appStore = useAppStore()
const route = useRoute()
const isFlowTaskListPage = computed(() => isFlowTaskListPath(route.path))
const { showSidebar } = useSidebarVisibility()
</script>

<style scoped>
.top-layout-header,
.main-top-menu,
.header-search,
.header-actions {
  min-width: 0;
}

.top-layout-header {
  height: 56px !important;
  padding-right: 16px !important;
  padding-left: 16px !important;
  gap: 10px;
}

.brand-section {
  display: flex;
  flex: 0 1 auto;
  align-items: center;
  max-width: 320px;
  gap: 10px;
}

.brand-logo {
  flex-shrink: 0;
}

.brand-title {
  flex: 1;
  min-width: 0;
}

.header-refresh-action {
  flex-shrink: 0;
  color: var(--top-menu-text-color, var(--layout-header-text-color));
}

.header-search {
  margin-right: 4px !important;
  margin-left: 4px !important;
}

.header-actions-inner {
  color: var(--top-menu-text-color, var(--layout-header-text-color));
}

.header-actions :deep(.message-notification-wrapper) {
  color: var(--top-menu-text-color, var(--layout-header-text-color));
}

.layout-page-content {
  background: var(--bg-secondary);
  box-sizing: border-box;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px;
}

.top-side-layout-tab-bar {
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
}

.flow-task-layout-content {
  overflow: hidden;
  padding: 0 !important;
}

.side-menu-wrapper {
  width: var(--side-menu-width);
  border-right: 1px solid var(--side-menu-border-color);
  background: var(--side-menu-bg-color, var(--bg-primary));
}
.side-menu-wrapper.collapsed {
  width: var(--side-menu-collapsed-width);
}

.side-collapse-dock {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-shrink: 0;
  height: 40px;
  padding: 0 12px;
  border-top: 1px solid var(--side-menu-border-color, var(--border-light));
  background: color-mix(in srgb, var(--side-menu-bg-color, var(--bg-primary)) 94%, var(--text-primary) 6%);
}

.side-collapse-dock :deep(.menu-collapse-button) {
  width: 100%;
  height: 30px;
  justify-content: flex-start;
  border-radius: 3px;
  color: var(--text-secondary);
  font-size: 18px;
}

.side-menu-wrapper.collapsed .side-collapse-dock {
  justify-content: center;
  padding: 0;
}

.side-menu-wrapper.collapsed .side-collapse-dock :deep(.menu-collapse-button) {
  width: 32px;
  justify-content: center;
}

@media (max-width: 640px) {
  .top-layout-header {
    gap: 8px;
    padding-right: 12px !important;
    padding-left: 12px !important;
  }

  .brand-section {
    flex: 0 0 auto;
    min-width: 0;
    gap: 0;
  }

  .brand-title,
  .header-refresh-action,
  .header-search,
  .header-divider,
  .mobile-hidden-action {
    display: none !important;
  }

  .main-top-menu {
    flex: 1 1 auto;
  }

  .header-actions-inner {
    padding-right: 0 !important;
    padding-left: 0 !important;
  }

  .header-actions :deep(#user-dropdown .ml-8) {
    display: none !important;
  }
}
</style>
