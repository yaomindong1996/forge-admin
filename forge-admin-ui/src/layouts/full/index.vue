<template>
  <div class="full-layout">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 侧边栏 -->
    <aside
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
      <AppCard class="tab-bar-full">
        <AppTab class="tab-content" />
      </AppCard>
      <div class="content-full">
        <slot />
      </div>
    </article>
  </div>
</template>

<script setup>
import DemoBanner from '@/components/DemoBanner.vue'
import { AppCard } from '@/components/index.js'
import { AppTab } from '@/layouts/components'
import { useAppStore } from '@/store'
import AppHeader from './header/index.vue'
import SideBar from './sidebar/index.vue'

const appStore = useAppStore()
</script>

<style scoped>
.full-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  background: #f8fafc;
}

/* 侧边栏 */
.sidebar-full {
  flex-shrink: 0;
  width: 200px;
  background: white;
  border-right: 1px solid #e2e8f0;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
}

.sidebar-full-collapsed {
  width: 64px;
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
  border-bottom: 1px solid #e2e8f0;
}

.tab-bar-full {
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-bottom: 1px solid #e2e8f0;
}

.tab-content {
  flex: 1;
  min-width: 0;
  padding: 0 12px;
}

.content-full {
  flex: 1;
  overflow: auto;
  padding: 16px;
  background: #f8fafc;
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

/* 响应式 */
@media (max-width: 768px) {
  .sidebar-full {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
  }

  .sidebar-full-collapsed {
    transform: translateX(-100%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .sidebar-full {
    transition: none;
  }
}
</style>
