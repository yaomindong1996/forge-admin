
<template>
  <div class="modern-layout">
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
      <div class="content-area">
        <slot />
      </div>
    </article>
  </div>
</template>

<script setup>
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
}

/* 背景装饰 */
.modern-layout::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  top: -200px;
  right: -200px;
  pointer-events: none;
}

.modern-layout::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  bottom: -100px;
  left: -100px;
  pointer-events: none;
}

/* 侧边栏容器 */
.sidebar-wrapper {
  flex-shrink: 0;
  width: 260px;
  padding: 8px;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 100;
}

.sidebar-wrapper.sidebar-collapsed {
  width: 84px;
}

/* 玻璃态侧边栏 */
.sidebar-glass {
  height: 100%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
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
  padding: 12px 12px 12px 0;
}

/* 玻璃态头部 */
.header-glass {
  height: 64px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  margin-bottom: 12px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

/* 内容区域 */
.content-area {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  overflow: auto;
  padding: 10px;
}

/* 滚动条样式 */
.content-area::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.content-area::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 10px;
}

.content-area::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  transition: background 0.2s ease;
}

.content-area::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar-wrapper {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    padding: 8px;
    z-index: 1000;
  }

  .sidebar-wrapper.sidebar-collapsed {
    transform: translateX(-100%);
  }

  .main-content {
    padding: 8px;
  }

  .header-glass {
    border-radius: 12px;
    height: 56px;
    padding: 0 12px;
    margin-bottom: 8px;
  }

  .content-area {
    border-radius: 12px;
    padding: 12px;
  }
}

/* 动画优化 */
@media (prefers-reduced-motion: reduce) {
  .sidebar-wrapper,
  .sidebar-glass,
  .header-glass,
  .content-area {
    transition: none;
  }
}
</style>
