<template>
  <div class="immersive-layout">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 顶部 Header -->
    <ImmersiveHeader />

    <!-- 主内容区 -->
    <div class="immersive-content" :class="{ 'immersive-content-flush': isFlowTaskListPage }">
      <slot />
    </div>

    <!-- Tab 标签栏 - 放在底部 -->
    <AppCard :bordered="false" :padding="false" class="immersive-tab-bar px-8 py-0" shadow="none" radius="none">
      <AppTab class="w-0 flex-1" />
    </AppCard>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { AppCard } from '@/components/common'
import DemoBanner from '@/components/DemoBanner.vue'
import { AppTab } from '@/layouts/components'
import { isFlowTaskListPath } from '@/utils/flow-task-layout'
import ImmersiveHeader from './header/index.vue'

const route = useRoute()
const isFlowTaskListPage = computed(() => isFlowTaskListPath(route.path))
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
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
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
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--bg-primary);
  padding: 8px;
  min-height: 0;
}

.immersive-content-flush {
  overflow: hidden;
  padding: 0;
}

/* 滚动条 */
.immersive-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.immersive-content::-webkit-scrollbar-track {
  background: var(--bg-tertiary);
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
