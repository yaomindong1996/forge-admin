<template>
  <div class="wh-full flex flex-col">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 顶部一级菜单 -->
    <header
      class="layout-header layout-chrome-header top-layout-header flex flex-shrink-0 items-center"
    >
      <!-- 窄屏只挂载授权菜单抽屉，不压缩桌面横向菜单 -->
      <ResponsiveMenuToggle v-if="isNarrow" />
      <div class="brand-section">
        <TheLogo class="brand-logo" />
        <TheTitle class="brand-title" />
      </div>
      <PageRefresh class="header-refresh-action" />
      <TopMenu v-if="!isNarrow" class="top-menu-wrapper main-top-menu flex-1" />
      <HeaderTools class="header-actions" />
    </header>

    <!-- 主内容区域 -->
    <article class="w-full flex flex-col flex-1 overflow-hidden">
      <AppCard :bordered="false" :padding="false" class="top-layout-tab-bar px-8 py-0" shadow="none" radius="none">
        <AppTab class="w-0 flex-1" />
      </AppCard>
      <div class="layout-page-content flex-1" :class="{ 'flow-task-layout-content': isFlowTaskListPage }">
        <slot />
      </div>
    </article>
  </div>
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { AppCard } from '@/components/common'
import DemoBanner from '@/components/DemoBanner.vue'
import { TheTitle } from '@/components/index.js'
import {
  AppTab,
  PageRefresh,
} from '@/layouts/components'
import HeaderTools from '@/layouts/components/HeaderTools.vue'
import ResponsiveMenuToggle from '@/layouts/components/ResponsiveMenuToggle.vue'
import { isFlowTaskListPath } from '@/utils/flow-task-layout'
import TopMenu from './components/TopMenu.vue'

const route = useRoute()
const isNarrow = useMediaQuery('(max-width: 768px)')
const isFlowTaskListPage = computed(() => isFlowTaskListPath(route.path))
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

@media (max-width: 768px) {
  .header-actions {
    margin-left: auto;
  }
}

.layout-page-content {
  background: var(--bg-secondary);
  box-sizing: border-box;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px;
}

.top-layout-tab-bar {
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
