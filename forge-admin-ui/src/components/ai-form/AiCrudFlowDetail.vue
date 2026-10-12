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
            <span>提交轮次</span>
            <strong>{{ roundText }}</strong>
          </div>
          <div class="flow-summary-wide">
            <span>流程实例</span>
            <strong>{{ activeProcessInstanceId }}</strong>
          </div>
        </div>

        <div v-if="flowRounds.length > 1" class="flow-round-list">
          <button
            v-for="round in flowRounds"
            :key="round.processInstanceId"
            type="button"
            class="flow-round-chip"
            :class="{ active: round.processInstanceId === activeProcessInstanceId }"
            @click="selectRound(round)"
          >
            第 {{ round.roundNo || '-' }} 次
          </button>
        </div>

        <n-tabs v-if="showTimeline && showDiagram" type="segment" size="small" animated>
          <n-tab-pane name="timeline" tab="时间轴">
            <FlowTimeline v-if="history.length" :items="history" />
            <n-empty v-else description="暂无审批记录" size="small" />
          </n-tab-pane>
          <n-tab-pane name="diagram" tab="流程图" display-directive="show:lazy">
            <DingFlowViewer :process-instance-id="activeProcessInstanceId" :compact="true" />
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
            <DingFlowViewer :process-instance-id="activeProcessInstanceId" :compact="true" />
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
import DingFlowViewer from '@/components/flow-designer/viewer/DingFlowViewer.vue'
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
const selectedProcessInstanceId = ref('')

const processInstanceId = computed(() => props.runtime?.processInstanceId || '')
const flowRounds = computed(() => Array.isArray(props.runtime?.flowRounds) ? props.runtime.flowRounds : [])
const activeProcessInstanceId = computed(() => selectedProcessInstanceId.value || processInstanceId.value)
const roundText = computed(() => {
  const selected = flowRounds.value.find(item => item.processInstanceId === activeProcessInstanceId.value)
  const roundNo = selected?.roundNo || props.runtime?.roundNo
  return roundNo ? `第 ${roundNo} 次提交` : '-'
})
const flowStatusText = computed(() => {
  const selected = flowRounds.value.find(item => item.processInstanceId === activeProcessInstanceId.value)
  const status = selected?.flowStatus || selected?.result || props.runtime?.flowStatus
  switch (String(status || '').toUpperCase()) {
    case 'RUNNING':
    case 'STARTED':
    case 'IN_PROCESS':
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
      return status || '-'
  }
})

watch(processInstanceId, (value) => {
  selectedProcessInstanceId.value = value || ''
  loadHistory(value)
}, { immediate: true })

function selectRound(round) {
  if (!round?.processInstanceId || round.processInstanceId === activeProcessInstanceId.value)
    return
  selectedProcessInstanceId.value = round.processInstanceId
  loadHistory(round.processInstanceId)
}

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
  grid-template-columns: minmax(0, 120px) minmax(0, 140px) minmax(0, 1fr);
  gap: 10px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
  padding: 10px 12px;
}

.flow-summary-wide {
  min-width: 0;
}

.flow-summary div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.flow-summary span {
  color: var(--text-secondary);
  font-size: 12px;
}

.flow-summary strong {
  min-width: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-round-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.flow-round-chip {
  border: 1px solid #dbe3f0;
  border-radius: 999px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1;
  padding: 6px 10px;
  cursor: pointer;
}

.flow-round-chip.active {
  border-color: var(--primary-color, #165dff);
  background: color-mix(in srgb, var(--primary-color, #165dff) 10%, var(--bg-primary));
  color: var(--primary-color, #165dff);
}

.flow-section {
  display: grid;
  gap: 8px;
}

.flow-section h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}
</style>
