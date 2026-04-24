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

    <!-- 展开拖拽条（收起时贴在左侧边缘） -->
    <div
      v-show="appStore.collapsed"
      ref="expandBarRef"
      class="nexus-expand-bar"
      :style="{ top: expandBarTop + 'px' }"
      @click="handleBarClick"
      @mousedown="handleBarMouseDown"
    >
      <div class="expand-bar-icon">
        <i class="i-material-symbols:chevron-right" />
      </div>
    </div>

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
        <div class="nexus-page cus-scroll">
          <slot />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import DemoBanner from '@/components/DemoBanner.vue'
import { ref, onBeforeUnmount } from 'vue'
import { useAppStore } from '@/store'
import { AppTab } from '@/layouts/components'
import Header from './header/index.vue'
import Sidebar from './sidebar/index.vue'

const appStore = useAppStore()
const expandBarRef = ref(null)
const expandBarTop = ref(400)
const isDragging = ref(false)
const hasDragged = ref(false)
const dragStartY = ref(0)
const dragStartTop = ref(0)

function handleBarMouseDown(e) {
  e.preventDefault()
  isDragging.value = true
  hasDragged.value = false
  dragStartY.value = e.clientY
  dragStartTop.value = expandBarTop.value

  document.addEventListener('mousemove', handleBarMouseMove)
  document.addEventListener('mouseup', handleBarMouseUp)
}

function handleBarMouseMove(e) {
  if (!isDragging.value) return
  const delta = e.clientY - dragStartY.value
  const maxTop = window.innerHeight - 60

  // 移动超过 5px 就认为是拖拽，不是点击
  if (Math.abs(delta) > 5) {
    hasDragged.value = true
  }

  expandBarTop.value = Math.max(20, Math.min(maxTop, dragStartTop.value + delta))
}

function handleBarMouseUp() {
  isDragging.value = false
  document.removeEventListener('mousemove', handleBarMouseMove)
  document.removeEventListener('mouseup', handleBarMouseUp)
}

function handleBarClick(e) {
  if (hasDragged.value) {
    hasDragged.value = false
    return
  }
  appStore.collapsed = false
}

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', handleBarMouseMove)
  document.removeEventListener('mouseup', handleBarMouseUp)
})
</script>

<style scoped>
.nexus-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  background: var(--nexus-page-bg);
  overflow: hidden;
  font-family: var(--font-family-sans);
}

/* ═══════════════════════════════════════
 * 左侧浮岛侧边栏
 * ═══════════════════════════════════════ */
.nexus-sidebar-wrapper {
  width: 240px;
  flex-shrink: 0;
  padding: 12px 6px 12px 12px;
  display: flex;
  flex-direction: column;
  z-index: 20;
}

.nexus-sidebar-inner {
  flex: 1;
  background: var(--bg-primary);
  border: 1px solid var(--nexus-border);
  border-radius: 12px;
  box-shadow: var(--nexus-shadow-card);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ═══════════════════════════════════════
 * 展开拖拽条（左侧边缘）
 * ═══════════════════════════════════════ */
.nexus-expand-bar {
  position: fixed;
  left: 0;
  z-index: 30;
  width: 18px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 0 6px 6px 0;
  background: var(--nexus-active-bg);
  border: 1px solid rgba(22, 93, 255, 0.25);
  border-left: none;
  box-shadow: 0 0 8px rgba(22, 93, 255, 0.15);
  transition: all var(--transition-base);
  user-select: none;
  -webkit-user-drag: none;
}

.dark .nexus-expand-bar {
  background: rgba(37, 99, 235, 0.4);
  border-color: rgba(96, 165, 250, 0.3);
  box-shadow: 0 0 8px rgba(96, 165, 250, 0.15);
}

.nexus-expand-bar:hover {
  background: rgba(22, 93, 255, 0.15);
  border-color: rgba(22, 93, 255, 0.4);
  box-shadow: 0 0 16px rgba(22, 93, 255, 0.25);
  width: 20px;
}

.dark .nexus-expand-bar:hover {
  background: rgba(37, 99, 235, 0.6);
}

.nexus-expand-bar:active {
  cursor: grabbing;
  background: rgba(22, 93, 255, 0.2);
  box-shadow: 0 0 20px rgba(22, 93, 255, 0.3);
}

.dark .nexus-expand-bar:active {
  background: rgba(37, 99, 235, 0.7);
}

.expand-bar-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-500);
  font-size: 16px;
  transition: all var(--transition-base);
}

.dark .expand-bar-icon {
  color: var(--nexus-active-text);
}

.nexus-expand-bar:hover .expand-bar-icon {
  transform: scale(1.15);
}

.nexus-expand-bar:active .expand-bar-icon {
  transform: scale(1.2);
}

/* ═══════════════════════════════════════
 * 主内容区
 * ═══════════════════════════════════════ */
.nexus-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 12px 12px 12px 6px;
  gap: 12px;
}

/* 顶部 Header 浮岛 */
.nexus-header {
  flex-shrink: 0;
  height: 56px;
  min-height: 56px;
  background: var(--bg-primary);
  border: 1px solid var(--nexus-border);
  border-radius: 12px;
  box-shadow: var(--nexus-shadow-card);
  z-index: 20;
  display: flex;
  align-items: center;
}

/* 内容浮岛 */
.nexus-content {
  flex: 1;
  background: var(--bg-primary);
  border: 1px solid var(--nexus-border);
  border-radius: 12px;
  box-shadow: var(--nexus-shadow-content);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* Tab 标签栏 */
.nexus-tab-bar {
  height: 40px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--nexus-border);
  background: var(--nexus-tab-bg);
  padding: 0 8px;
  display: flex;
  align-items: flex-end;
  gap: 2px;
  overflow-x: auto;
}

.nexus-tab-bar :deep(#top-tab) {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

/* Tab 样式覆盖 */
.nexus-tab-bar :deep(.n-tabs-nav) {
  border-bottom: none !important;
}

.nexus-tab-bar :deep(.n-tabs-tab) {
  border: 1px solid transparent !important;
  border-bottom: none !important;
  border-radius: 8px 8px 0 0 !important;
  background: transparent !important;
  color: var(--text-tertiary) !important;
  font-size: 12px !important;
  font-weight: 600 !important;
  padding: 6px 12px !important;
  height: 32px !important;
  transition: all var(--transition-base) !important;
}

.nexus-tab-bar :deep(.n-tabs-tab:hover) {
  background: var(--bg-secondary) !important;
  color: var(--text-secondary) !important;
}

.nexus-tab-bar :deep(.n-tabs-tab.n-tabs-tab--active) {
  background: var(--bg-primary) !important;
  border-color: var(--nexus-border) !important;
  border-bottom-color: var(--bg-primary) !important;
  color: var(--text-primary) !important;
  z-index: 1;
  margin-bottom: -1px;
}

.nexus-tab-bar :deep(.n-tabs-tab-pane) {
  display: none;
}

/* 页面内容 */
.nexus-page {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: var(--bg-primary);
  min-height: 0;
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
.sidebar-slide-enter-active {
  transition: all var(--transition-slow);
}

.sidebar-slide-leave-active {
  transition: all var(--transition-slow);
}

.sidebar-slide-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.sidebar-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 展开条淡入/淡出 */
.expand-fade-enter-active,
.expand-fade-leave-active {
  transition: all var(--transition-base);
}

.expand-fade-enter-from,
.expand-fade-leave-to {
  opacity: 0;
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
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    width: 240px;
    padding: 12px;
    z-index: 100;
    transform: translateX(-100%);
    transition: transform var(--transition-slow);
  }

  .nexus-main {
    padding: 8px;
  }
}

/* 动画优化 */
@media (prefers-reduced-motion: reduce) {
  .nexus-sidebar-wrapper,
  .nexus-expand-trigger,
  .nexus-main {
    transition: none;
  }
}
</style>

<style>
/* 全局 Nexus 变量 */
:root {
  --nexus-page-bg: #f3f5f8;
  --nexus-border: rgba(226, 232, 240, 0.8);
  --nexus-shadow-card: 0 2px 16px rgba(0, 0, 0, 0.03);
  --nexus-shadow-content: 0 4px 24px rgba(0, 0, 0, 0.03);
  --nexus-tab-bg: #fafbfc;
  --nexus-active-bg: #eff6ff;
  --nexus-active-text: #1d4ed8;
  --nexus-hover-bg: #f8fafc;
  --primary-300: #93c5fd;
}

.dark {
  --nexus-page-bg: #0b1120;
  --nexus-border: rgba(51, 65, 85, 0.8);
  --nexus-shadow-card: 0 2px 16px rgba(0, 0, 0, 0.3);
  --nexus-shadow-content: 0 4px 24px rgba(0, 0, 0, 0.3);
  --nexus-tab-bg: #111827;
  --nexus-active-bg: #1e3a5f;
  --nexus-active-text: #60a5fa;
  --nexus-hover-bg: #1e293b;
  --primary-300: #1e3a5f;
}
</style>
