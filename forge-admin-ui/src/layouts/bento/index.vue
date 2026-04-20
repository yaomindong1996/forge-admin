<template>
  <div class="bento-layout">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 左侧窄栏：图标导航 -->
    <BentoRail />

    <!-- 主内容区 -->
    <article class="bento-content">
      <!-- Tab 标签栏 -->
      <AppCard class="bento-tab-bar" shadow="none" :padding="false">
        <AppTab />
      </AppCard>

      <!-- 页面内容 -->
      <div class="bento-page">
        <slot />
      </div>
    </article>
  </div>
</template>

<script setup>
import DemoBanner from '@/components/DemoBanner.vue'
import { AppCard } from '@/components/common'
import { AppTab } from '@/layouts/components'
import BentoRail from './components/BentoRail.vue'
</script>

<style scoped>
.bento-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  background: var(--bg-page);
  overflow: hidden;
}

/* 主内容区 */
.bento-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* Tab 标签栏 */
.bento-tab-bar {
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-bottom: 1px solid var(--border-light);
  border-radius: 0;
  overflow: hidden;
}

.bento-tab-bar :deep(#top-tab) {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

/* 页面内容 */
.bento-page {
  flex: 1;
  overflow: auto;
  padding: 16px;
  background: var(--bg-page);
}

/* 滚动条 */
.bento-page::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.bento-page::-webkit-scrollbar-track {
  background: transparent;
}

.bento-page::-webkit-scrollbar-thumb {
  background: var(--border-default);
  border-radius: 4px;
}

.bento-page::-webkit-scrollbar-thumb:hover {
  background: var(--border-strong);
}

.dark .bento-page::-webkit-scrollbar-thumb {
  background: var(--gray-600);
}

.dark .bento-page::-webkit-scrollbar-thumb:hover {
  background: var(--gray-500);
}
</style>
