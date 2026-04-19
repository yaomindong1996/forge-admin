<template>
  <div class="modern-layout">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 侧边栏 -->
    <aside
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
      <div class="content-area cus-scroll">
        <slot />
      </div>
    </article>
  </div>
</template>

<script setup>
import DemoBanner from '@/components/DemoBanner.vue'
import { useAppStore } from '@/store'
import AppHeader from './header/index.vue'
import SideBar from './sidebar/index.vue'

const appStore = useAppStore()
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
  width: 200px;
  transition: width var(--transition-slow);
  position: relative;
  z-index: 100;
  border-right: 1px solid var(--border-light);
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

.sidebar-wrapper.sidebar-collapsed {
  width: 56px;
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
  background: var(--gray-100);
}

/* 头部包裹 - 不设置大小，由内部 header 自己控制 */
.header-glass {
  flex-shrink: 0;
}

/* 内容区域 */
.content-area {
  flex: 1;
  background: var(--gray-100);
  overflow: auto;
  padding: 0;
  min-height: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar-wrapper {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
    border-right: none;
    box-shadow: var(--shadow-lg);
  }

  .sidebar-wrapper.sidebar-collapsed {
    transform: translateX(-100%);
  }

  .header-glass {
    height: 48px;
    min-height: 48px;
    padding: 0 8px 0 0;
  }
}

/* 动画优化 */
@media (prefers-reduced-motion: reduce) {
  .sidebar-wrapper {
    transition: none;
  }
}
</style>
