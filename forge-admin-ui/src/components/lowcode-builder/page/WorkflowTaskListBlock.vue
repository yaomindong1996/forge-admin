<template>
  <div
    class="workflow-task-list"
    :class="{ 'is-compact': compact, 'is-readonly': !interactive }"
    :style="rootStyle"
  >
    <!-- 标题栏 -->
    <header v-if="showHeader" class="wtl-head">
      <div class="wtl-head-copy">
        <h3 class="wtl-title">
          {{ titleText }}
        </h3>
        <p v-if="subtitleText" class="wtl-subtitle">
          {{ subtitleText }}
        </p>
      </div>
      <div class="wtl-head-actions">
        <button
          v-if="showRefresh"
          type="button"
          class="wtl-text-btn"
          :disabled="loading"
          @click="reload"
        >
          {{ loading ? '刷新中' : '刷新' }}
        </button>
        <button
          v-if="showMoreLink && interactive"
          type="button"
          class="wtl-text-btn is-primary"
          @click="openMore"
        >
          全部
        </button>
      </div>
    </header>

    <!-- 待办 / 已办 -->
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
        <span>{{ tab.label }}</span>
        <em class="wtl-tab-count">{{ formatCount(tab.count) }}</em>
      </button>
    </div>

    <!-- 搜索 -->
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

    <!-- 列表 -->
    <n-spin :show="loading">
      <div v-if="displayItems.length" class="wtl-list" role="list">
        <button
          v-for="item in displayItems"
          :key="item.id"
          type="button"
          class="wtl-item"
          :class="{ 'is-interactive': interactive, [`tone-${item.tone}`]: true }"
          role="listitem"
          :disabled="!interactive"
          @click="openItem(item)"
        >
          <span class="wtl-item-bar" aria-hidden="true" />
          <div class="wtl-item-body">
            <div class="wtl-item-top">
              <strong class="wtl-item-title">{{ item.title }}</strong>
              <span
                v-if="showStatus && item.statusLabel"
                class="wtl-status"
                :data-tone="item.statusClass"
              >
                {{ item.statusLabel }}
              </span>
            </div>
            <div v-if="rowMetaVisible(item)" class="wtl-item-meta">
              <span v-if="showNode && item.nodeName" class="wtl-meta-node">{{ item.nodeName }}</span>
              <span v-if="showApplicant && item.applicant">{{ item.applicant }}</span>
              <span v-if="showTime && item.time" class="wtl-meta-time">{{ formatTime(item.time) }}</span>
            </div>
          </div>
        </button>
      </div>
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
  accentColor: { type: String, default: '#3370ff' },
  emptyText: { type: String, default: '' },
  searchPlaceholder: { type: String, default: '搜索任务名称' },
  /** 应用内待办页 id（优先于外部 path） */
  todoPageId: { type: String, default: '' },
  /** 应用内已办页 id（优先于外部 path） */
  donePageId: { type: String, default: '' },
  /** 兜底外部路径；留空则不跳工作台 */
  todoPath: { type: String, default: '' },
  donePath: { type: String, default: '' },
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

const rootStyle = computed(() => ({
  '--wtl-accent': props.accentColor || 'var(--primary-color, #3370ff)',
}))

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

function formatCount(value) {
  const n = Number(value)
  if (!Number.isFinite(n) || n < 0)
    return '0'
  if (n > 99)
    return '99+'
  return String(n)
}

function formatTime(value) {
  const raw = String(value || '').trim()
  if (!raw)
    return ''
  const match = raw.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})/)
  if (match)
    return `${match[2]}-${match[3]} ${match[4]}:${match[5]}`
  return raw
}

function rowMetaVisible(item) {
  return (props.showNode && item.nodeName)
    || (props.showApplicant && item.applicant)
    || (props.showTime && item.time)
}

function normalizeItem(row = {}, index = 0, mode = 'todo') {
  const id = String(row.taskId || row.id || `task_${index + 1}`)
  const processName = String(row.processName || row.title || row.name || '').trim()
  const nodeName = String(row.taskName || row.nodeName || row.currentNode || '').trim()
  // 标题优先流程名；节点单独展示，避免两者撞成同一句导致副行空白
  const title = processName || nodeName || `任务 ${index + 1}`
  const applicant = String(
    row.startUserName || row.applicantName || row.userName || row.assigneeName || '',
  ).trim()
  const time = String(
    row.createTime || row.submitTime || row.endTime || row.finishTime || row.completeTime || row.updateTime || '',
  ).trim()
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

/** 工作台待办/已办路径：禁止当作「全部」跳转目标，应选应用内页面 */
const WORKBENCH_TASK_PATHS = new Set([
  '/flow/todo',
  '/flow/done',
  '/workspace/todo',
  '/workspace/done',
])

function sanitizeExternalPath(path = '') {
  const raw = String(path || '').trim()
  if (!raw)
    return ''
  const bare = raw.split('?')[0].replace(/\/+$/, '') || '/'
  if (WORKBENCH_TASK_PATHS.has(bare))
    return ''
  return raw
}

function resolveTarget(mode = 'todo') {
  const isDone = mode === 'done'
  const configured = String(isDone ? props.donePageId : props.todoPageId || '').trim()
  // 未配置时默认进应用内系统页（与左侧「我的待办 / 我已办的」一致）
  const pageId = configured || (isDone ? 'system:done' : 'system:todo')
  return {
    mode,
    pageId,
    path: sanitizeExternalPath(isDone ? props.donePath : props.todoPath || ''),
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
  const target = resolveTarget(activeMode.value)
  emit('open-more', target)
  if (!props.interactive)
    return
  // 有应用内页面时交给父级 navigate；避免默认冲到工作台 /flow/todo
  if (target.pageId)
    return
  if (target.path)
    router.push(target.path)
}

function openItem(item) {
  const target = resolveTarget(item.mode)
  emit('open', { ...item, pageId: target.pageId, path: target.path })
  if (!props.interactive)
    return
  if (target.pageId)
    return
  const taskId = item.taskId
  if (!target.path)
    return
  if (!taskId) {
    router.push(target.path)
    return
  }
  router.push({
    path: target.path,
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
  gap: 8px;
  min-width: 0;
  min-height: 0;
  height: 100%;
  color: var(--text-primary, #1f2329);
}

.wtl-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  flex-shrink: 0;
}

.wtl-head-copy {
  min-width: 0;
}

.wtl-title {
  margin: 0;
  font-size: 14px;
  font-weight: 650;
  line-height: 1.35;
  color: var(--text-primary, #1f2329);
}

.wtl-subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 1.35;
  color: var(--text-tertiary, #8f959e);
}

.wtl-head-actions {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
}

.wtl-text-btn {
  border: 0;
  background: transparent;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.5;
  color: var(--text-secondary, #646a73);
  cursor: pointer;
  border-radius: 4px;
}

.wtl-text-btn:hover:not(:disabled) {
  color: var(--text-primary, #1f2329);
  background: var(--bg-secondary, #f2f3f5);
}

.wtl-text-btn.is-primary {
  color: var(--wtl-accent, var(--primary-color, #3370ff));
}

.wtl-text-btn.is-primary:hover:not(:disabled) {
  color: var(--wtl-accent, var(--primary-color, #3370ff));
  background: color-mix(in srgb, var(--wtl-accent, #3370ff) 10%, #fff);
}

.wtl-text-btn:disabled {
  opacity: 0.55;
  cursor: default;
}

/* 分段：实底选中，避免浅灰发虚 */
.wtl-tabs {
  display: inline-flex;
  gap: 2px;
  padding: 2px;
  width: fit-content;
  border-radius: 6px;
  background: #edf0f4;
  border: 1px solid #dde2ea;
  flex-shrink: 0;
}

.wtl-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 0;
  background: transparent;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 550;
  line-height: 1.3;
  color: #5b6470;
  cursor: pointer;
  border-radius: 4px;
}

.wtl-tab.active {
  color: #fff;
  background: var(--wtl-accent, var(--primary-color, #3370ff));
}

.wtl-tab:hover:not(.active) {
  color: #1f2329;
  background: #fff;
}

.wtl-tab-count {
  font-style: normal;
  min-width: 16px;
  padding: 0 4px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
  text-align: center;
  background: rgba(15, 23, 42, 0.1);
  color: inherit;
}

.wtl-tab.active .wtl-tab-count {
  background: rgba(255, 255, 255, 0.28);
  color: #fff;
}

.wtl-search {
  flex-shrink: 0;
}

/* 列表：行式密度，不用一张张浅色小卡片 */
.wtl-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
  overflow: auto;
  flex: 1;
  margin: 0;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  background: #fff;
}

.wtl-item {
  display: flex;
  align-items: stretch;
  gap: 0;
  width: 100%;
  text-align: left;
  border: 0;
  border-bottom: 1px solid #eef0f3;
  border-radius: 0;
  background: #fff;
  padding: 0;
  cursor: default;
  color: inherit;
  font: inherit;
  overflow: hidden;
  transition: background-color 120ms ease;
}

.wtl-item:last-child {
  border-bottom: 0;
}

.wtl-item.is-interactive {
  cursor: pointer;
}

.wtl-item.is-interactive:hover {
  background: color-mix(in srgb, var(--wtl-accent, #3370ff) 7%, #fff);
}

.wtl-item:disabled {
  cursor: default;
}

.wtl-item-bar {
  width: 4px;
  flex-shrink: 0;
  background: var(--wtl-accent, var(--primary-color, #3370ff));
}

.wtl-item.tone-warning .wtl-item-bar {
  background: #d97706;
}

.wtl-item.tone-success .wtl-item-bar {
  background: #16a34a;
}

.wtl-item-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 9px 10px 9px 10px;
}

.wtl-item-top {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.wtl-item-title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 550;
  line-height: 1.35;
  color: #1f2329;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wtl-item.is-interactive:hover .wtl-item-title {
  color: var(--wtl-accent, var(--primary-color, #3370ff));
}

.wtl-status {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.35;
  padding: 1px 6px;
  border-radius: 3px;
  color: #4e5969;
  background: #f2f3f5;
}

.wtl-status[data-tone='warn'] {
  color: #ad6800;
  background: #fff7e8;
}

.wtl-status[data-tone='info'] {
  color: #1d4ed8;
  background: #e8f0ff;
}

.wtl-status[data-tone='success'] {
  color: #0f7b3f;
  background: #e8f7ef;
}

.wtl-item-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0;
  font-size: 12px;
  line-height: 1.35;
  color: #8f959e;
}

.wtl-item-meta > span + span::before {
  content: '·';
  margin: 0 6px;
  color: #c0c4cc;
}

.wtl-meta-node {
  color: #646a73;
  font-weight: 500;
}

.wtl-meta-time {
  font-variant-numeric: tabular-nums;
}

.wtl-empty {
  padding: 24px 12px;
  text-align: center;
  font-size: 12px;
  color: #8f959e;
  border: 1px dashed #d0d3d9;
  border-radius: 6px;
  background: #fafbfc;
}

.is-compact .wtl-item-body {
  padding: 7px 10px;
}

.is-compact .wtl-item-meta {
  display: none;
}
</style>
