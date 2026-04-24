<template>
  <div class="modern-layout-simple">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 侧边栏 -->
    <aside
      class="sidebar-simple"
      :class="{ 'sidebar-simple-collapsed': appStore.collapsed }"
    >
      <div class="sidebar-simple-inner">
        <SideBar />
      </div>
    </aside>

    <!-- 主内容区 -->
    <article class="main-content-simple">
      <div class="content-simple">
        <slot />
      </div>
    </article>
  </div>
</template>

<script setup>
import DemoBanner from '@/components/DemoBanner.vue'
import { useAppStore } from '@/store'
import SideBar from './sidebar/index.vue'

const appStore = useAppStore()
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
  width: 260px;
  background: var(--bg-primary, #ffffff);
  border-right: 1px solid var(--border-light, #e2e8f0);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
}

.sidebar-simple-collapsed {
  width: 72px;
}

.sidebar-simple-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 主内容区 */
.main-content-simple {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.content-simple {
  flex: 1;
  overflow: auto;
  padding: 16px;
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
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
  }

  .sidebar-simple-collapsed {
    transform: translateX(-100%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .sidebar-simple {
    transition: none;
  }
}
</style>
