<template>
  <div class="modern-layout-simple">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 侧边栏 -->
    <aside
      v-if="!isNarrow"
      class="sidebar-simple"
      :class="{ 'sidebar-simple-collapsed': appStore.collapsed }"
    >
      <div class="sidebar-simple-inner">
        <SideBar />
      </div>
    </aside>

    <!-- 主内容区 -->
    <article class="main-content-simple">
      <!-- 仅窄屏保留必要导航，桌面不增加顶栏 -->
      <header v-if="isNarrow" class="simple-mobile-nav">
        <ResponsiveMenuToggle />
        <TheTitle class="simple-mobile-title" />
        <MessageNotification />
        <CompactLayoutTools placement="bottom-end" />
      </header>
      <div class="content-simple" :class="{ 'content-simple-flush': isFlowTaskListPage }">
        <slot />
      </div>
    </article>
  </div>
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TheTitle from '@/components/common/TheTitle.vue'
import DemoBanner from '@/components/DemoBanner.vue'
import CompactLayoutTools from '@/layouts/components/CompactLayoutTools.vue'
import MessageNotification from '@/layouts/components/MessageNotification.vue'
import ResponsiveMenuToggle from '@/layouts/components/ResponsiveMenuToggle.vue'
import { useAppStore } from '@/store'
import { isFlowTaskListPath } from '@/utils/flow-task-layout'
import SideBar from './sidebar/index.vue'

const appStore = useAppStore()
const isNarrow = useMediaQuery('(max-width: 768px)')
const route = useRoute()
const isFlowTaskListPage = computed(() => isFlowTaskListPath(route.path))
</script>

<style scoped>
.modern-layout-simple {
  width: 100%;
  height: 100vh;
  display: flex;
  background: var(--bg-tertiary, #f8fafc);
}

/* 侧边栏 */
.sidebar-simple {
  flex-shrink: 0;
  width: max(240px, var(--side-menu-width));
  background: var(--side-menu-bg-color, #ffffff);
  border-right: 1px solid var(--side-menu-border-color, #e2e8f0);
  display: flex;
  flex-direction: column;
}

.sidebar-simple-collapsed {
  width: var(--side-menu-collapsed-width);
}

.sidebar-simple-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 主内容区 */
.main-content-simple {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  min-height: 0;
}

.simple-mobile-nav {
  --chrome-text: var(--side-menu-text-color);
  --brand-title-text-color: var(--side-menu-text-color);
  display: none;
  align-items: center;
  gap: 8px;
  min-height: 48px;
  padding: 0 8px;
  flex-shrink: 0;
  background: var(--side-menu-bg-color);
  color: var(--side-menu-text-color);
  border-bottom: 1px solid var(--side-menu-border-color);
}
.simple-mobile-title {
  flex: 1;
}

.content-simple {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px;
  min-height: 0;
}

.content-simple-flush {
  overflow: hidden;
  padding: 0;
}

/* 滚动条 */
.content-simple::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.content-simple::-webkit-scrollbar-track {
  background: var(--bg-secondary, #f1f5f9);
}

.content-simple::-webkit-scrollbar-thumb {
  background: var(--border-default, #cbd5e1);
  border-radius: 4px;
}

.content-simple::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary, #94a3b8);
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar-simple {
    display: none;
  }
  .simple-mobile-nav {
    display: flex;
  }
}
</style>
