<template>
  <div class="full-layout">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 侧边栏 -->
    <aside
      v-if="showSidebar && !isNarrow"
      class="sidebar-full"
      :class="{ 'sidebar-full-collapsed': appStore.collapsed }"
    >
      <div class="sidebar-full-inner">
        <SideBar />
      </div>
    </aside>

    <!-- 主内容区 -->
    <article class="main-content-full">
      <AppHeader class="header-full" />
      <AppCard :bordered="false" :padding="false" class="tab-bar-full" shadow="none" radius="none">
        <AppTab class="tab-content" />
      </AppCard>
      <div class="content-full" :class="{ 'content-full-flush': isFlowTaskListPage }">
        <slot />
      </div>
    </article>
  </div>
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DemoBanner from '@/components/DemoBanner.vue'
import { AppCard } from '@/components/index.js'
import { AppTab } from '@/layouts/components'
import { useSidebarVisibility } from '@/layouts/composables/useSidebarVisibility'
import { useAppStore } from '@/store'
import { isFlowTaskListPath } from '@/utils/flow-task-layout'
import AppHeader from './header/index.vue'
import SideBar from './sidebar/index.vue'

const appStore = useAppStore()
const isNarrow = useMediaQuery('(max-width: 768px)')
const route = useRoute()
const isFlowTaskListPage = computed(() => isFlowTaskListPath(route.path))
const { showSidebar } = useSidebarVisibility()
</script>

<style scoped>
.full-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  background: var(--bg-secondary);
}

/* 侧边栏 */
.sidebar-full {
  flex-shrink: 0;
  width: var(--side-menu-width);
  background: var(--side-menu-bg-color, #ffffff);
  display: flex;
  flex-direction: column;
}

.sidebar-full-collapsed {
  width: var(--side-menu-collapsed-width);
}

.sidebar-full-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 主内容区 */
.main-content-full {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.header-full {
  height: 60px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--layout-header-border-color);
}

.tab-bar-full {
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 8px;
  border-bottom: 1px solid var(--border-light);
}

.tab-content {
  flex: 1;
  min-width: 0;
}

.content-full {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px;
  background: var(--bg-secondary);
  min-height: 0;
}

.content-full-flush {
  overflow: hidden;
  padding: 0;
}

/* 滚动条 */
.content-full::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.content-full::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.content-full::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.content-full::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
