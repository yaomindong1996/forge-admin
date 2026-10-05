<template>
  <div class="modern-layout">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 侧边栏 -->
    <aside
      v-if="!isNarrow"
      class="sidebar-wrapper"
      :class="{ 'sidebar-collapsed': appStore.collapsed }"
    >
      <div class="sidebar-glass">
        <SideBar />
      </div>
    </aside>

    <!-- 主内容区 -->
    <article class="main-content">
      <AppHeader class="header-glass" />
      <div class="content-area cus-scroll" :class="{ 'content-area-flush': isFlowTaskListPage }">
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
import { useAppStore } from '@/store'
import { isFlowTaskListPath } from '@/utils/flow-task-layout'
import AppHeader from './header/index.vue'
import SideBar from './sidebar/index.vue'

const appStore = useAppStore()
const isNarrow = useMediaQuery('(max-width: 768px)')
const route = useRoute()
const isFlowTaskListPage = computed(() => isFlowTaskListPath(route.path))
</script>

<style scoped>
.modern-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  position: relative;
  overflow: hidden;
  background: var(--bg-primary);
}

/* 侧边栏 - 无外边距直接展开 */
.sidebar-wrapper {
  flex-shrink: 0;
  width: var(--side-menu-width);
  position: relative;
  z-index: 100;
  background: var(--side-menu-bg-color);
  display: flex;
  flex-direction: column;
}

.sidebar-wrapper.sidebar-collapsed {
  width: var(--side-menu-collapsed-width);
}

.sidebar-glass {
  height: 100%;
  background: transparent;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-secondary);
}

/* 头部包裹 - 不设置大小，由内部 header 自己控制 */
.header-glass {
  flex-shrink: 0;
}

/* 内容区域 */
.content-area {
  flex: 1;
  background: var(--bg-secondary);
  overflow-x: hidden;
  overflow-y: auto;
  box-sizing: border-box;
  padding: 8px;
  min-height: 0;
}

.content-area-flush {
  overflow: hidden;
  padding: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .header-glass {
    height: 48px;
    min-height: 48px;
    padding: 0 8px 0 0;
  }
}
</style>
