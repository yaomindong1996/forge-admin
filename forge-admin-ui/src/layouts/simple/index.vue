
<template>
  <div class="modern-layout-simple">
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
import { useAppStore } from '@/store'
import SideBar from './sidebar/index.vue'

const appStore = useAppStore()
</script>

<style scoped>
.modern-layout-simple {
  width: 100%;
  height: 100vh;
  display: flex;
  background: #F8FAFC;
}

/* 侧边栏 */
.sidebar-simple {
  flex-shrink: 0;
  width: 260px;
  background: white;
  border-right: 1px solid #E2E8F0;
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
  background: #F1F5F9;
}

.content-simple::-webkit-scrollbar-thumb {
  background: #CBD5E1;
  border-radius: 4px;
}

.content-simple::-webkit-scrollbar-thumb:hover {
  background: #94A3B8;
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
