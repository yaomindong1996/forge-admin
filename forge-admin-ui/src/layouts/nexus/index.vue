<template>
  <div class="nexus-layout">
    <!-- 左侧浮岛侧边栏 -->
    <Transition name="sidebar-slide">
      <aside v-if="!appStore.collapsed" class="nexus-sidebar-wrapper">
        <div class="nexus-sidebar-inner">
          <Sidebar />
        </div>
      </aside>
    </Transition>

    <!-- 主内容区 -->
    <div class="nexus-main">
      <!-- 演示环境提示条 -->
      <DemoBanner />
      <!-- 顶部浮岛 Header -->
      <Header class="nexus-header" />

      <!-- 内容浮岛 -->
      <main class="nexus-content">
        <!-- Tab 标签栏 -->
        <div class="nexus-tab-bar">
          <AppTab />
        </div>

        <!-- 页面内容 -->
        <div class="nexus-page cus-scroll" :class="{ 'nexus-page-flush': isFlowTaskListPage }">
          <slot />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DemoBanner from '@/components/DemoBanner.vue'
import { AppTab } from '@/layouts/components'
import { useAppStore } from '@/store'
import { isFlowTaskListPath } from '@/utils/flow-task-layout'
import Header from './header/index.vue'
import Sidebar from './sidebar/index.vue'

const appStore = useAppStore()
const route = useRoute()
const isFlowTaskListPage = computed(() => isFlowTaskListPath(route.path))
</script>

<style scoped>
.nexus-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  background: var(--bg-secondary);
  overflow: hidden;
  font-family: var(--font-family-sans);
}

/* ═══════════════════════════════════════
 * 左侧浮岛侧边栏
 * ═══════════════════════════════════════ */
.nexus-sidebar-wrapper {
  width: var(--side-menu-width);
  flex-shrink: 0;
  padding: 10px 5px 10px 10px;
  display: flex;
  flex-direction: column;
  z-index: 20;
}

.nexus-sidebar-inner {
  flex: 1;
  background: var(--side-menu-bg-color);
  border: 1px solid var(--side-menu-border-color);
  border-radius: 6px;
  box-shadow: none;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ═══════════════════════════════════════
 * 主内容区
 * ═══════════════════════════════════════ */
.nexus-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 10px 10px 10px 5px;
  gap: 10px;
}

/* 顶部 Header 浮岛 */
.nexus-header {
  flex-shrink: 0;
  height: 56px;
  min-height: 56px;
  background: var(--layout-header-bg-color);
  border: 1px solid var(--layout-header-border-color);
  border-radius: 6px;
  box-shadow: none;
  z-index: 20;
  display: flex;
  align-items: center;
}

/* 内容浮岛 */
.nexus-content {
  flex: 1;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  box-shadow: none;
  overflow: hidden;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* Tab 标签栏 */
.nexus-tab-bar {
  height: 38px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
  padding: 0 8px;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.nexus-tab-bar :deep(#top-tab) {
  flex: 1;
  min-width: 0;
}

/* 页面内容 */
.nexus-page {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px;
  background: var(--bg-primary);
  min-height: 0;
}

.nexus-page-flush {
  overflow: hidden;
  padding: 0;
}

/* 滚动条样式 */
.nexus-page::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.nexus-page::-webkit-scrollbar-track {
  background: transparent;
}

.nexus-page::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 3px;
}

.nexus-page::-webkit-scrollbar-thumb:hover {
  background: var(--border-default);
}

/* ═══════════════════════════════════════
 * 过渡动画
 * ═══════════════════════════════════════ */

/* 侧边栏滑入/滑出 */
.sidebar-slide-enter-active,
.sidebar-slide-leave-active {
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}

.sidebar-slide-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.sidebar-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* ═══════════════════════════════════════
 * 响应式
 * ═══════════════════════════════════════ */
@media (max-width: 1024px) {
  .nexus-sidebar-wrapper {
    width: 200px;
    padding: 8px 4px 8px 8px;
  }
}

@media (max-width: 768px) {
  .nexus-layout {
    flex-direction: column;
  }

  .nexus-sidebar-wrapper {
    display: none;
  }

  .nexus-main {
    padding: 8px;
  }
}

/* 动画优化 */
@media (prefers-reduced-motion: reduce) {
  .nexus-sidebar-wrapper,
  .sidebar-slide-enter-active,
  .sidebar-slide-leave-active,
  .nexus-main {
    transition: none;
  }
}
</style>
