<template>
  <div class="workspace-summary-page">
    <header class="summary-head">
      <h2 class="summary-head-title">个人工作台</h2>
      <div class="summary-head-actions">
        <NButton size="small" quaternary :loading="metricsLoading" @click="refreshMetrics">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          刷新
        </NButton>
      </div>
    </header>

    <PortalPageRenderer
      v-if="useFreeLayout"
      class="workspace-summary-layout"
      :page="resolvedPage"
      :page-id="pageId || 'system:workbench'"
      :node="layoutNode"
      :objects="objects"
      :entries="entries"
      :extensions="extensions"
      :application-id="applicationId"
      :application-code="applicationCode"
      :fill-host="false"
      :configurable="false"
    />
    <WorkspaceSummaryMetrics
      v-else
      ref="metricsRef"
      :route-targets="routeTargets"
      @loaded="metricsLoading = false"
    />
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, ref } from 'vue'
import { createDefaultWorkbenchPage, createWorkbenchVirtualNode, WORKBENCH_PAGE_ID } from '@/views/app-center/in-app-builder/workbench-page'
import SystemPageContentSkeleton from '@/views/app-center/components/portal/SystemPageContentSkeleton.vue'
import WorkspaceSummaryMetrics from './WorkspaceSummaryMetrics.vue'

const PortalPageRenderer = defineAsyncComponent({
  delay: 0,
  loadingComponent: SystemPageContentSkeleton,
  loader: () => import('@/views/app-center/components/portal/PortalPageRenderer.vue'),
})

const props = defineProps({
  routeTargets: {
    type: Object,
    default: () => ({}),
  },
  /** 自由布局页 schema（builder.pages[system:workbench]） */
  page: {
    type: Object,
    default: null,
  },
  pageId: {
    type: String,
    default: WORKBENCH_PAGE_ID,
  },
  objects: { type: Array, default: () => [] },
  entries: { type: Array, default: () => [] },
  extensions: { type: Array, default: () => [] },
  applicationId: { type: String, default: '' },
  applicationCode: { type: String, default: '' },
})

const metricsRef = ref(null)
const metricsLoading = ref(false)
const layoutNode = createWorkbenchVirtualNode()

const resolvedPage = computed(() => {
  const items = props.page?.layout?.gridLayout?.items
  if (Array.isArray(items) && items.length)
    return props.page
  return createDefaultWorkbenchPage(layoutNode.title)
})

const useFreeLayout = computed(() => {
  // 有 page 入参时走自由布局（含默认布局）；纯独立 /workspace 无 page 时用组件直渲
  return props.page != null || Boolean(props.applicationCode)
})

function refreshMetrics() {
  metricsLoading.value = true
  metricsRef.value?.loadSummary?.()
  // free layout 内部组件自行加载，这里只反馈按钮态
  window.setTimeout(() => {
    metricsLoading.value = false
  }, 400)
}
</script>

<style scoped>
.workspace-summary-page {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
}

.summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
  padding: 12px;
  margin: 0;
  border: 0;
  border-bottom: 1px solid var(--border-light, #e5eaf3);
  background: transparent;
}

.summary-head-title {
  margin: 0;
  color: var(--text-primary, #172033);
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}

.summary-head-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.workspace-summary-layout {
  min-height: 0;
}

@media (max-width: 640px) {
  .summary-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
