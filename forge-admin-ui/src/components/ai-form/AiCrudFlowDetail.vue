<template>
  <div class="ai-crud-flow-detail">
    <n-spin :show="loading || historyLoading">
      <div v-if="processInstanceId" class="flow-detail-body">
        <div class="flow-summary">
          <div>
            <span>流程状态</span>
            <strong>{{ flowStatusText }}</strong>
          </div>
          <div>
            <span>流程实例</span>
            <strong>{{ processInstanceId }}</strong>
          </div>
        </div>

        <n-tabs v-if="showTimeline && showDiagram" type="segment" size="small" animated>
          <n-tab-pane name="timeline" tab="时间轴">
            <FlowTimeline v-if="history.length" :items="history" />
            <n-empty v-else description="暂无审批记录" size="small" />
          </n-tab-pane>
          <n-tab-pane name="diagram" tab="流程图" display-directive="show:lazy">
            <ProcessDiagramViewer :process-instance-id="processInstanceId" :compact="true" />
          </n-tab-pane>
        </n-tabs>

        <template v-else>
          <section v-if="showTimeline" class="flow-section">
            <h4>时间轴</h4>
            <FlowTimeline v-if="history.length" :items="history" />
            <n-empty v-else description="暂无审批记录" size="small" />
          </section>
          <section v-if="showDiagram" class="flow-section">
            <h4>流程图</h4>
            <ProcessDiagramViewer :process-instance-id="processInstanceId" :compact="true" />
          </section>
        </template>
      </div>
      <n-empty v-else description="当前记录尚未发起流程" size="small" />
    </n-spin>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import flowApi from '@/api/flow'
import ProcessDiagramViewer from '@/components/flow-designer/viewer/DingFlowViewer.vue'
import FlowTimeline from '@/components/flow/FlowTimeline.vue'

const props = defineProps({
  runtime: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  showTimeline: {
    type: Boolean,
    default: true,
  },
  showDiagram: {
    type: Boolean,
    default: true,
  },
})

const history = ref([])
const historyLoading = ref(false)

const processInstanceId = computed(() => props.runtime?.processInstanceId || '')
const flowStatusText = computed(() => {
  switch (String(props.runtime?.flowStatus || '').toUpperCase()) {
    case 'RUNNING':
    case 'STARTED':
      return '流程中'
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已驳回'
    case 'CANCELED':
      return '已撤回'
    case 'NOT_STARTED':
      return '未发起'
    default:
      return props.runtime?.flowStatus || '-'
  }
})

watch(processInstanceId, (value) => {
  loadHistory(value)
}, { immediate: true })

async function loadHistory(value) {
  history.value = []
  if (!value || !props.showTimeline)
    return
  historyLoading.value = true
  try {
    const res = await flowApi.getProcessHistory(value)
    if (res.code === 200)
      history.value = res.data || []
  }
  catch (error) {
    console.warn('[AiCrudFlowDetail] 加载流程时间轴失败:', error?.message || error)
  }
  finally {
    historyLoading.value = false
  }
}
</script>

<style scoped>
.ai-crud-flow-detail {
  min-height: 260px;
}

.flow-detail-body {
  display: grid;
  gap: 12px;
}

.flow-summary {
  display: grid;
  grid-template-columns: minmax(0, 160px) minmax(0, 1fr);
  gap: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px 12px;
}

.flow-summary div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.flow-summary span {
  color: #64748b;
  font-size: 12px;
}

.flow-summary strong {
  min-width: 0;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-section {
  display: grid;
  gap: 10px;
}

.flow-section h4 {
  margin: 0;
  color: #111827;
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 720px) {
  .flow-summary {
    grid-template-columns: 1fr;
  }
}
</style>
