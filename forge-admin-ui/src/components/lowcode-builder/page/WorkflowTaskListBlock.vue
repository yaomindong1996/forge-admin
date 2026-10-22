<template>
  <div
    class="workflow-task-list"
    :class="{ 'is-compact': compact, 'is-readonly': !interactive }"
    :style="{ '--wtl-accent': accentColor || '#2563eb' }"
  >
    <header v-if="showHeader" class="wtl-head">
      <div class="wtl-head-copy">
        <strong>{{ titleText }}</strong>
        <span v-if="subtitleText">{{ subtitleText }}</span>
      </div>
      <div class="wtl-head-actions">
        <n-button v-if="showRefresh" size="tiny" quaternary :loading="loading" @click="reload">
          刷新
        </n-button>
        <n-button v-if="showMoreLink && interactive" size="tiny" quaternary type="primary" @click="openMore">
          查看全部
        </n-button>
      </div>
    </header>

    <div v-if="showTabs" class="wtl-tabs" role="tablist">
      <button
        v-for="tab in tabOptions"
        :key="tab.value"
        type="button"
        class="wtl-tab"
        :class="{ active: activeMode === tab.value }"
        role="tab"
        :aria-selected="activeMode === tab.value"
        @click="activeMode = tab.value"
      >
        {{ tab.label }}
        <em v-if="tab.count != null">{{ tab.count }}</em>
      </button>
    </div>

    <div v-if="showSearch" class="wtl-search">
      <n-input
        v-model:value="keyword"
        size="small"
        clearable
        :placeholder="searchPlaceholder"
        @keyup.enter="reload"
        @clear="reload"
      >
        <template #suffix>
          <n-button text size="tiny" @click="reload">
            搜索
          </n-button>
        </template>
      </n-input>
    </div>

    <n-spin :show="loading">
      <ul v-if="displayItems.length" class="wtl-list">
        <li
          v-for="item in displayItems"
          :key="item.id"
          class="wtl-item"
          :class="{ 'is-interactive': interactive }"
          @click="openItem(item)"
        >
          <span class="wtl-item-tone" :class="`tone-${item.tone}`" aria-hidden="true" />
          <div class="wtl-item-body">
            <div class="wtl-item-title-row">
              <strong>{{ item.title }}</strong>
              <span v-if="showStatus && item.statusLabel" class="wtl-status" :class="item.statusClass">
                {{ item.statusLabel }}
              </span>
            </div>
            <p v-if="showNode && item.nodeName" class="wtl-item-meta">
              {{ item.nodeName }}
            </p>
            <div class="wtl-item-foot">
              <span v-if="showApplicant && item.applicant">{{ item.applicant }}</span>
              <span v-if="showTime && item.time">{{ item.time }}</span>
            </div>
          </div>
          <span v-if="interactive" class="wtl-item-arrow" aria-hidden="true">›</span>
        </li>
      </ul>
      <div v-else class="wtl-empty">
        {{ emptyTextResolved }}
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NInput, NSpin } from 'naive-ui'
import flowApi from '@/api/flow'
import { useUserStore } from '@/store'

const props = defineProps({
  title: { type: String, default: '我的待办' },
  subtitle: { type: String, default: '' },
  /** todo | done | both */
  mode: { type: String, default: 'both' },
  pageSize: { type: [Number, String], default: 6 },
  compact: { type: Boolean, default: false },
  showHeader: { type: Boolean, default: true },
  showTabs: { type: Boolean, default: true },
  showSearch: { type: Boolean, default: false },
  showStatus: { type: Boolean, default: true },
  showNode: { type: Boolean, default: true },
  showApplicant: { type: Boolean, default: true },
  showTime: { type: Boolean, default: true },
  showRefresh: { type: Boolean, default: true },
  showMoreLink: { type: Boolean, default: true },
  accentColor: { type: String, default: '#2563eb' },
  emptyText: { type: String, default: '' },
  searchPlaceholder: { type: String, default: '搜索任务名称' },
  todoPath: { type: String, default: '/flow/todo' },
  donePath: { type: String, default: '/flow/done' },
  interactive: { type: Boolean, default: true },
  autoLoad: { type: Boolean, default: true },
  /** 设计态预览占位 */
  previewItems: { type: Array, default: () => [] },
})

const emit = defineEmits(['loaded', 'open', 'open-more'])

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const keyword = ref('')
const todoItems = ref([])
const doneItems = ref([])
const todoTotal = ref(0)
const doneTotal = ref(0)
const activeMode = ref(props.mode === 'done' ? 'done' : 'todo')

const titleText = computed(() => {
  if (props.mode === 'done')
    return props.title || '我已办的'
  if (props.mode === 'todo')
    return props.title || '我的待办'
  return props.title || '待办与已办'
})

const subtitleText = computed(() => String(props.subtitle || '').trim())

const showTabs = computed(() => props.mode === 'both' && props.showTabs !== false)

const tabOptions = computed(() => [
  { value: 'todo', label: '待办', count: todoTotal.value },
  { value: 'done', label: '已办', count: doneTotal.value },
])

const emptyTextResolved = computed(() => {
  if (props.emptyText)
    return props.emptyText
  return activeMode.value === 'done' ? '暂无已办任务' : '暂无待办任务'
})

const displayItems = computed(() => {
  const source = activeMode.value === 'done' ? doneItems.value : todoItems.value
  if (source.length)
    return source
  if (!props.interactive && Array.isArray(props.previewItems) && props.previewItems.length)
    return props.previewItems.map((item, index) => normalizeItem(item, index, activeMode.value))
  return []
})

function normalizeItem(row = {}, index = 0, mode = 'todo') {
  const id = String(row.taskId || row.id || `task_${index + 1}`)
  const title = String(row.title || row.processName || row.taskName || row.name || `任务 ${index + 1}`).trim()
  const nodeName = String(row.taskName || row.nodeName || row.name || '').trim()
  const applicant = String(row.startUserName || row.applicantName || row.userName || '').trim()
  const time = String(row.createTime || row.endTime || row.finishTime || row.updateTime || '').trim()
  const status = row.status
  let statusLabel = ''
  let statusClass = 'neutral'
  let tone = mode === 'done' ? 'success' : 'primary'
  if (mode === 'todo') {
    statusLabel = status === 0 ? '待签收' : '处理中'
    statusClass = status === 0 ? 'warn' : 'info'
    tone = status === 0 ? 'warning' : 'primary'
  }
  else {
    statusLabel = '已完成'
    statusClass = 'success'
  }
  return {
    id,
    taskId: row.taskId || row.id,
    title,
    nodeName: nodeName && nodeName !== title ? nodeName : '',
    applicant,
    time,
    statusLabel,
    statusClass,
    tone,
    raw: row,
    mode,
  }
}

async function fetchList(mode) {
  const pageSize = Math.max(1, Math.min(30, Number(props.pageSize) || 6))
  const params = {
    pageNum: 1,
    pageSize,
    userId: userStore.userId,
  }
  const kw = String(keyword.value || '').trim()
  if (kw)
    params.title = kw
  const api = mode === 'done' ? flowApi.getDoneTasks : flowApi.getTodoTasks
  const res = await api(params, { needTip: false })
  const records = Array.isArray(res?.data?.records) ? res.data.records : []
  const total = Number(res?.data?.total) || records.length
  const items = records.map((row, index) => normalizeItem(row, index, mode))
  return { items, total }
}

async function reload() {
  if (!userStore.userId) {
    todoItems.value = []
    doneItems.value = []
    return
  }
  loading.value = true
  try {
    if (props.mode === 'todo') {
      const result = await fetchList('todo')
      todoItems.value = result.items
      todoTotal.value = result.total
      doneItems.value = []
      doneTotal.value = 0
      activeMode.value = 'todo'
    }
    else if (props.mode === 'done') {
      const result = await fetchList('done')
      doneItems.value = result.items
      doneTotal.value = result.total
      todoItems.value = []
      todoTotal.value = 0
      activeMode.value = 'done'
    }
    else {
      const [todoResult, doneResult] = await Promise.all([
        fetchList('todo'),
        fetchList('done'),
      ])
      todoItems.value = todoResult.items
      todoTotal.value = todoResult.total
      doneItems.value = doneResult.items
      doneTotal.value = doneResult.total
    }
    emit('loaded', {
      todo: todoItems.value,
      done: doneItems.value,
      todoTotal: todoTotal.value,
      doneTotal: doneTotal.value,
    })
  }
  catch {
    todoItems.value = []
    doneItems.value = []
  }
  finally {
    loading.value = false
  }
}

function openMore() {
  const path = activeMode.value === 'done' ? props.donePath : props.todoPath
  emit('open-more', { mode: activeMode.value, path })
  if (!props.interactive)
    return
  router.push(path)
}

function openItem(item) {
  emit('open', item)
  if (!props.interactive)
    return
  const path = item.mode === 'done' ? props.donePath : props.todoPath
  const taskId = item.taskId
  if (!taskId) {
    router.push(path)
    return
  }
  router.push({
    path,
    query: { taskId, source: 'free-layout', t: Date.now() },
  })
}

watch(() => props.mode, (mode) => {
  activeMode.value = mode === 'done' ? 'done' : 'todo'
  if (props.autoLoad)
    reload()
})

watch(() => props.pageSize, () => {
  if (props.autoLoad)
    reload()
})

onMounted(() => {
  if (props.autoLoad)
    reload()
})

defineExpose({ reload })
</script>

<style scoped>
.workflow-task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  height: 100%;
}

.wtl-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.wtl-head-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.wtl-head-copy strong {
  font-size: 14px;
  color: var(--text-primary, #0f172a);
}

.wtl-head-copy span {
  font-size: 12px;
  color: var(--text-tertiary, #94a3b8);
}

.wtl-head-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.wtl-tabs {
  display: inline-flex;
  gap: 4px;
  padding: 3px;
  border-radius: 8px;
  background: var(--bg-secondary, #f6f8fb);
  width: fit-content;
}

.wtl-tab {
  border: 0;
  background: transparent;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  color: var(--text-secondary, #64748b);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.wtl-tab.active {
  background: var(--bg-primary, #fff);
  color: var(--wtl-accent, var(--primary-color, #2563eb));
  box-shadow: 0 0 0 1px var(--border-light, #e2e8f0);
}

.wtl-tab em {
  font-style: normal;
  min-width: 16px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--bg-tertiary, #eef2f7);
  font-size: 11px;
}

.wtl-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  overflow: auto;
}

.wtl-item {
  display: flex;
  align-items: stretch;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
}

.wtl-item.is-interactive {
  cursor: pointer;
}

.wtl-item.is-interactive:hover {
  border-color: color-mix(in srgb, var(--wtl-accent, #2563eb) 35%, var(--border-light, #e2e8f0));
  background: color-mix(in srgb, var(--wtl-accent, #2563eb) 4%, #fff);
}

.wtl-item-tone {
  width: 3px;
  border-radius: 999px;
  background: var(--wtl-accent, #2563eb);
  flex-shrink: 0;
}

.wtl-item-tone.tone-warning { background: #d97706; }
.wtl-item-tone.tone-success { background: #16a34a; }
.wtl-item-tone.tone-primary { background: var(--wtl-accent, #2563eb); }

.wtl-item-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.wtl-item-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wtl-item-title-row strong {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: var(--text-primary, #0f172a);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wtl-status {
  flex-shrink: 0;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-secondary, #64748b);
}

.wtl-status.warn { color: #b45309; background: rgba(217, 119, 6, 0.12); }
.wtl-status.info { color: #1d4ed8; background: rgba(37, 99, 235, 0.1); }
.wtl-status.success { color: #15803d; background: rgba(22, 163, 74, 0.12); }

.wtl-item-meta,
.wtl-item-foot {
  font-size: 12px;
  color: var(--text-tertiary, #94a3b8);
}

.wtl-item-foot {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.wtl-item-arrow {
  align-self: center;
  color: var(--text-tertiary, #94a3b8);
  font-size: 16px;
}

.wtl-empty {
  padding: 28px 12px;
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary, #94a3b8);
  border: 1px dashed var(--border-light, #e2e8f0);
  border-radius: 8px;
}

.is-compact .wtl-item {
  padding: 8px 10px;
}

.is-compact .wtl-item-meta {
  display: none;
}
</style>
