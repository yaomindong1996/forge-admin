<template>
  <div class="workspace-summary-metrics" :class="{ 'is-compact': compact, 'is-readonly': readonly, 'is-loading': loading }">
    <NAlert v-if="loadError" class="summary-alert" type="warning" :show-icon="false">
      {{ loadError }}
    </NAlert>
    <!-- 先渲染卡片骨架，统计请求异步填充，避免整页转圈 -->
    <section class="summary-grid" :style="gridStyle">
      <button
        v-for="item in visibleMetricItems"
        :key="item.key"
        class="summary-metric"
        :class="[`tone-${item.tone}`, { 'is-emphasis': item.key === emphasisKey }]"
        type="button"
        :disabled="readonly || loading"
        @click="handleMetricClick(item)"
      >
        <span class="metric-icon" :class="item.tone">
          <i :class="item.icon" />
        </span>
        <span class="metric-copy">
          <strong>{{ loading ? '—' : item.value }}</strong>
          <span>{{ item.label }}</span>
          <small>{{ item.desc }}</small>
          <span class="metric-share" aria-hidden="true">
            <i :style="{ width: `${loading ? 0 : item.sharePercent}%` }" />
          </span>
        </span>
        <i v-if="!readonly" class="i-material-symbols:chevron-right metric-arrow" />
      </button>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAlert } from 'naive-ui'
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
  /** 当前页高亮的指标 key：todo / done / started / cc */
  emphasisKey: {
    type: String,
    default: '',
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
    icon: 'i-material-symbols:task-alt',
    tone: 'green',
  },
  {
    key: 'started',
    label: '发起中',
    desc: '我发起且仍在流转',
    value: summary.value.startedRunningCount,
    systemPageId: 'system:sent',
    fallbackPath: '/workspace/started',
    icon: 'i-material-symbols:send',
    tone: 'amber',
  },
  {
    key: 'cc',
    label: '未读抄送',
    desc: '抄送给我的未读消息',
    value: summary.value.ccUnreadCount,
    systemPageId: 'system:cc',
    fallbackPath: '/workspace/cc',
    icon: 'i-material-symbols:mark-email-unread',
    tone: 'red',
  },
])

const visibleMetricItems = computed(() => {
  const keys = new Set((props.visibleKeys || []).map(item => String(item)))
  const list = metricCatalog.value.filter(item => keys.has(item.key))
  const items = list.length ? list : metricCatalog.value
  const total = items.reduce((sum, item) => sum + Number(item.value || 0), 0)
  return items.map(item => ({
    ...item,
    target: resolveMetricTarget(item),
    sharePercent: total > 0
      ? Math.max(6, Math.round((Number(item.value || 0) / total) * 100))
      : 0,
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
  // 流程管理菜单下停留在 /flow/*，避免跳到 /workspace
  if (String(route.path || '').startsWith('/flow/')) {
    const flowPathMap = {
      todo: '/flow/todo',
      done: '/flow/done',
      started: '/flow/started',
      cc: '/flow/cc',
    }
    return flowPathMap[key] || item.fallbackPath || '/flow/todo'
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
    const code = error?.code ?? error?.response?.data?.code
    const rawMessage = error?.message ?? error?.response?.data?.message
    const message = typeof rawMessage === 'string' && rawMessage.trim() ? rawMessage.trim() : ''
    const isTimeout = code === 'ECONNABORTED'
      || /timeout/i.test(message)
      || error?.message === 'Network Error'
    // 本地未起 flow / 代理指错环境时常见 401；避免把布尔 false 等脏值当成文案
    if (code === 401 || code === '401')
      loadError.value = '流程服务未接通或登录态不一致，工作台统计暂不可用（请确认本地 forge-flow 已启动且与 admin 同库）'
    else if (isTimeout)
      loadError.value = '流程服务响应超时，工作台统计暂不可用（请确认 forge-flow 已启动）'
    else
      loadError.value = message || '工作台统计加载失败'
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

.workspace-summary-metrics.is-loading .summary-metric {
  opacity: 0.72;
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

.summary-metric.is-emphasis {
  border-color: color-mix(in srgb, var(--metric-accent, var(--primary-color, #2563eb)) 42%, #e5eaf3);
  box-shadow: inset 3px 0 0 var(--metric-accent, var(--primary-color, #2563eb));
}

.summary-metric.tone-blue {
  --metric-accent: #2563eb;
  background: linear-gradient(135deg, #eff6ff 0%, var(--bg-primary) 58%);
}

.summary-metric.tone-green {
  --metric-accent: #16a34a;
  background: linear-gradient(135deg, #ecfdf5 0%, var(--bg-primary) 58%);
}

.summary-metric.tone-amber {
  --metric-accent: #d97706;
  background: linear-gradient(135deg, #fffbeb 0%, var(--bg-primary) 58%);
}

.summary-metric.tone-red {
  --metric-accent: #dc2626;
  background: linear-gradient(135deg, #fff1f2 0%, var(--bg-primary) 58%);
}

.is-compact .summary-metric {
  min-height: 88px;
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
  background: #dbeafe;
  color: #1d4ed8;
}

.metric-icon.green {
  background: #dcfce7;
  color: #15803d;
}

.metric-icon.amber {
  background: #fef3c7;
  color: #b45309;
}

.metric-icon.red {
  background: #ffe4e6;
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

.metric-share {
  display: block;
  width: 100%;
  height: 4px;
  margin-top: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: color-mix(in srgb, var(--metric-accent, #2563eb) 14%, #eef2f7);
}

.metric-share > i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--metric-accent, #2563eb);
  transition: width 0.18s ease;
}

.is-compact .metric-share {
  margin-top: 6px;
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
