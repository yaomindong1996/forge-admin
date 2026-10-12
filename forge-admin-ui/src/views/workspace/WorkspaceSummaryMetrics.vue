<template>
  <div class="workspace-summary-metrics" :class="{ 'is-compact': compact, 'is-readonly': readonly }">
    <NAlert v-if="loadError" class="summary-alert" type="warning" :show-icon="false">
      {{ loadError }}
    </NAlert>
    <NSpin :show="loading">
      <section class="summary-grid" :style="gridStyle">
        <button
          v-for="item in visibleMetricItems"
          :key="item.key"
          class="summary-metric"
          type="button"
          :disabled="readonly"
          @click="handleMetricClick(item)"
        >
          <span class="metric-icon" :class="item.tone">
            <i :class="item.icon" />
          </span>
          <span class="metric-copy">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
            <small>{{ item.desc }}</small>
          </span>
          <i v-if="!readonly" class="i-material-symbols:chevron-right metric-arrow" />
        </button>
      </section>
    </NSpin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getWorkspaceSummary } from '@/api/workspace'

const props = defineProps({
  routeTargets: {
    type: Object,
    default: () => ({}),
  },
  /** 显示哪些指标：todo / done / started / cc */
  visibleKeys: {
    type: Array,
    default: () => ['todo', 'done', 'started', 'cc'],
  },
  columns: {
    type: [Number, String],
    default: 4,
  },
  compact: {
    type: Boolean,
    default: false,
  },
  /** 设计态只展示，不跳转 */
  readonly: {
    type: Boolean,
    default: false,
  },
  autoLoad: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['navigate', 'loaded'])

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const summary = ref({
  todoCount: 0,
  doneWeekCount: 0,
  startedRunningCount: 0,
  ccUnreadCount: 0,
})

const metricCatalog = computed(() => [
  {
    key: 'todo',
    label: '我的待办',
    desc: '需要我处理或签收',
    value: summary.value.todoCount,
    systemPageId: 'system:todo',
    fallbackPath: '/workspace/todo',
    icon: 'i-material-symbols:pending-actions',
    tone: 'blue',
  },
  {
    key: 'done',
    label: '本周已办',
    desc: '本周完成的审批处理',
    value: summary.value.doneWeekCount,
    systemPageId: 'system:done',
    fallbackPath: '/workspace/done',
    icon: 'i-material-symbols:task-alt-outline',
    tone: 'green',
  },
  {
    key: 'started',
    label: '发起中',
    desc: '我发起且仍在流转',
    value: summary.value.startedRunningCount,
    systemPageId: 'system:sent',
    fallbackPath: '/workspace/started',
    icon: 'i-material-symbols:send-outline',
    tone: 'amber',
  },
  {
    key: 'cc',
    label: '未读抄送',
    desc: '抄送给我的未读消息',
    value: summary.value.ccUnreadCount,
    systemPageId: 'system:cc',
    fallbackPath: '/workspace/cc',
    icon: 'i-material-symbols:alternate-email',
    tone: 'red',
  },
])

const visibleMetricItems = computed(() => {
  const keys = new Set((props.visibleKeys || []).map(item => String(item)))
  const list = metricCatalog.value.filter(item => keys.has(item.key))
  return (list.length ? list : metricCatalog.value).map(item => ({
    ...item,
    target: resolveMetricTarget(item),
  }))
})

const gridStyle = computed(() => {
  const cols = Math.max(1, Math.min(4, Number(props.columns) || visibleMetricItems.value.length || 4))
  return { gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))` }
})

function resolveMetricTarget(item = {}) {
  const key = String(item.key || '')
  const fromProps = props.routeTargets?.[key]
    || props.routeTargets?.[key === 'started' ? 'sent' : key]
  if (fromProps)
    return fromProps
  // 应用门户 / 应用运行壳内：跳到同壳系统页，避免跑出应用到 /workspace
  if (['ApplicationPortal', 'BusinessApplicationRuntime'].includes(String(route.name || ''))) {
    const systemPageId = item.systemPageId
    if (systemPageId) {
      return {
        name: route.name,
        params: { ...route.params },
        query: {
          ...route.query,
          pageId: systemPageId,
          edit: undefined,
          designResource: undefined,
          designTab: undefined,
          designSection: undefined,
        },
      }
    }
  }
  return item.fallbackPath || '/workspace/todo'
}

watch(() => props.autoLoad, (enabled) => {
  if (enabled)
    loadSummary()
}, { immediate: false })

onMounted(() => {
  if (props.autoLoad)
    loadSummary()
})

async function loadSummary() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getWorkspaceSummary()
    const data = res.data || {}
    summary.value = {
      todoCount: Number(data.todoCount || 0),
      doneWeekCount: Number(data.doneWeekCount || 0),
      startedRunningCount: Number(data.startedRunningCount || 0),
      ccUnreadCount: Number(data.ccUnreadCount || 0),
    }
    emit('loaded', { ...summary.value })
  }
  catch (error) {
    loadError.value = error?.message || '工作台统计加载失败'
    summary.value = {
      todoCount: 0,
      doneWeekCount: 0,
      startedRunningCount: 0,
      ccUnreadCount: 0,
    }
  }
  finally {
    loading.value = false
  }
}

function handleMetricClick(item) {
  if (props.readonly || !item?.target)
    return
  emit('navigate', item)
  router.push(item.target)
}

defineExpose({ loadSummary, summary })
</script>

<style scoped>
.workspace-summary-metrics {
  width: 100%;
  min-width: 0;
}

.summary-alert {
  margin-bottom: 12px;
  border-radius: 8px;
}

.summary-grid {
  display: grid;
  gap: 12px;
}

.summary-metric {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 20px;
  gap: 12px;
  align-items: center;
  min-height: 118px;
  padding: 16px;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
  background: var(--bg-primary);
  color: #344256;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.summary-metric:disabled {
  cursor: default;
}

.summary-metric:not(:disabled):hover {
  border-color: #c9cdd4;
  box-shadow: 0 8px 22px rgba(31, 35, 41, 0.06);
  transform: translateY(-1px);
}

.is-compact .summary-metric {
  min-height: 96px;
  padding: 12px;
}

.metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  font-size: 22px;
}

.metric-icon.blue {
  background: #f0f2f5;
  color: var(--text-secondary);
}

.metric-icon.green {
  background: #e9f8ef;
  color: #15803d;
}

.metric-icon.amber {
  background: #fff6df;
  color: #b45309;
}

.metric-icon.red {
  background: #fff0f0;
  color: #dc2626;
}

.metric-copy {
  min-width: 0;
}

.metric-copy strong {
  display: block;
  color: var(--text-primary);
  font-size: 30px;
  font-weight: 750;
  line-height: 36px;
}

.is-compact .metric-copy strong {
  font-size: 24px;
  line-height: 30px;
}

.metric-copy span,
.metric-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-copy span {
  margin-top: 4px;
  color: #344256;
  font-size: 14px;
  font-weight: 650;
}

.metric-copy small {
  margin-top: 2px;
  color: #667085;
  font-size: 12px;
}

.metric-arrow {
  color: #98a2b3;
  font-size: 20px;
}

@media (max-width: 1180px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
