<template>
  <div class="immersive-layout">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 顶部 Header -->
    <ImmersiveHeader />

    <!-- 主内容区 -->
    <div class="immersive-content">
      <slot />
    </div>

    <!-- Tab 标签栏 - 放在底部 -->
    <AppCard :bordered="false" :padding="false" class="px-10 py-3 immersive-tab-bar" shadow="none" radius="none">
      <AppTab class="w-0 flex-1" />
    </AppCard>
  </div>
</template>

<script setup>
import { AppCard } from '@/components/common'
import DemoBanner from '@/components/DemoBanner.vue'
import { AppTab } from '@/layouts/components'
import ImmersiveHeader from './header/index.vue'
</script>

<style scoped>
.immersive-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  overflow: hidden;
}

/* Tab 标签栏 - 底部 */
.immersive-tab-bar {
  height: 48px;
  flex-shrink: 0;
  padding: 4px 12px 0 12px;
  border-top: 1px solid var(--border-light);
  border-radius: 0;
}

.immersive-tab-bar :deep(#top-tab) {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

/* 主内容区 */
.immersive-content {
  flex: 1;
  overflow: auto;
  background: var(--gray-100);
  padding: 0;
  min-height: 0;
}

/* 滚动条 */
.immersive-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.immersive-content::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.immersive-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.immersive-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 暗色模式滚动条 */
.dark .immersive-content::-webkit-scrollbar-track {
  background: #1e293b;
}

.dark .immersive-content::-webkit-scrollbar-thumb {
  background: #475569;
}

.dark .immersive-content::-webkit-scrollbar-thumb:hover {
  background: #64748b;
}
</style>
