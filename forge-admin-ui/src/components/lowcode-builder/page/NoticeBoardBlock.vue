<template>
  <div
    class="notice-board"
    :class="{ 'is-compact': compact, 'is-card': variant === 'card' }"
    :style="{ '--nb-accent': accentColor || '#2563eb' }"
  >
    <header v-if="showHeader" class="nb-head">
      <div class="nb-head-copy">
        <strong>{{ title || '通知公告' }}</strong>
        <span v-if="subtitle">{{ subtitle }}</span>
      </div>
      <div class="nb-head-actions">
        <n-badge v-if="showUnreadBadge && unreadCount > 0" :value="unreadCount" :max="99">
          <span class="nb-unread-label">未读</span>
        </n-badge>
        <n-button v-if="showRefresh" size="tiny" quaternary :loading="loading" @click="reload">
          刷新
        </n-button>
        <n-button
          v-if="showMarkAll && interactive && unreadCount > 0"
          size="tiny"
          quaternary
          @click="markAllRead"
        >
          全部已读
        </n-button>
        <n-button v-if="showMoreLink && interactive" size="tiny" quaternary type="primary" @click="openMore">
          更多
        </n-button>
      </div>
    </header>

    <div v-if="showFilterTabs" class="nb-tabs">
      <button
        v-for="tab in filterTabs"
        :key="tab.value"
        type="button"
        class="nb-tab"
        :class="{ active: filter === tab.value }"
        @click="filter = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <n-spin :show="loading">
      <ul v-if="displayItems.length" class="nb-list" :class="`density-${density}`">
        <li
          v-for="item in displayItems"
          :key="item.id"
          class="nb-item"
          :class="{ unread: item.unread, 'is-interactive': interactive }"
          @click="openItem(item)"
        >
          <span v-if="showTypeTag" class="nb-type" :class="`type-${item.typeTone}`">
            {{ item.typeLabel }}
          </span>
          <div class="nb-body">
            <div class="nb-title-row">
              <i v-if="item.unread && showUnreadDot" class="nb-dot" aria-hidden="true" />
              <strong>{{ item.title }}</strong>
            </div>
            <p v-if="showSummary && item.summary" class="nb-summary">
              {{ item.summary }}
            </p>
            <div class="nb-foot">
              <span v-if="showSender && item.sender">{{ item.sender }}</span>
              <span v-if="showTime && item.time">{{ item.time }}</span>
            </div>
          </div>
        </li>
      </ul>
      <div v-else class="nb-empty">
        {{ emptyText || '暂无通知公告' }}
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NBadge, NButton, NSpin, useMessage } from 'naive-ui'
import messageApi from '@/api/message'

const props = defineProps({
  title: { type: String, default: '通知公告' },
  subtitle: { type: String, default: '' },
  pageSize: { type: [Number, String], default: 6 },
  compact: { type: Boolean, default: false },
  variant: { type: String, default: 'list' },
  density: { type: String, default: 'comfortable' },
  showHeader: { type: Boolean, default: true },
  showFilterTabs: { type: Boolean, default: true },
  showUnreadBadge: { type: Boolean, default: true },
  showUnreadDot: { type: Boolean, default: true },
  showTypeTag: { type: Boolean, default: true },
  showSummary: { type: Boolean, default: true },
  showSender: { type: Boolean, default: false },
  showTime: { type: Boolean, default: true },
  showRefresh: { type: Boolean, default: true },
  showMarkAll: { type: Boolean, default: true },
  showMoreLink: { type: Boolean, default: true },
  accentColor: { type: String, default: '#2563eb' },
  unreadOnly: { type: Boolean, default: false },
  emptyText: { type: String, default: '' },
  morePath: { type: String, default: '/message' },
  interactive: { type: Boolean, default: true },
  autoLoad: { type: Boolean, default: true },
  markReadOnOpen: { type: Boolean, default: true },
  previewItems: { type: Array, default: () => [] },
})

const emit = defineEmits(['loaded', 'open', 'open-more'])

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const items = ref([])
const unreadCount = ref(0)
const filter = ref(props.unreadOnly ? 'unread' : 'all')

const filterTabs = [
  { value: 'all', label: '全部' },
  { value: 'unread', label: '未读' },
]

function resolveTypeMeta(row = {}) {
  const raw = String(row.messageType || row.type || row.bizType || 'notice').toLowerCase()
  if (raw.includes('warn') || raw.includes('alert'))
    return { label: '提醒', tone: 'warning' }
  if (raw.includes('system'))
    return { label: '系统', tone: 'info' }
  if (raw.includes('flow') || raw.includes('task'))
    return { label: '流程', tone: 'primary' }
  return { label: '公告', tone: 'neutral' }
}

function normalizeItem(row = {}, index = 0) {
  const id = String(row.id || row.messageId || `msg_${index + 1}`)
  const typeMeta = resolveTypeMeta(row)
  const unread = row.readFlag === 0 || row.read === false || row.unread === true
  return {
    id,
    title: String(row.title || row.subject || row.messageTitle || `通知 ${index + 1}`).trim(),
    summary: String(row.content || row.summary || row.preview || '').replace(/<[^>]+>/g, '').trim().slice(0, 120),
    sender: String(row.senderName || row.createByName || row.fromUserName || '').trim(),
    time: String(row.createTime || row.sendTime || row.updateTime || '').trim(),
    unread,
    typeLabel: typeMeta.label,
    typeTone: typeMeta.tone,
    raw: row,
  }
}

const displayItems = computed(() => {
  if (items.value.length)
    return items.value
  if (!props.interactive && Array.isArray(props.previewItems) && props.previewItems.length)
    return props.previewItems.map((item, index) => normalizeItem(item, index))
  return []
})

async function reload() {
  loading.value = true
  try {
    const pageSize = Math.max(1, Math.min(30, Number(props.pageSize) || 6))
    const query = {}
    if (filter.value === 'unread' || props.unreadOnly)
      query.readFlag = 0
    const [listRes, unreadRes] = await Promise.all([
      messageApi.getMessagePage(query, 1, pageSize),
      messageApi.getUnreadCount().catch(() => ({ data: 0 })),
    ])
    const records = Array.isArray(listRes?.data?.records) ? listRes.data.records : []
    items.value = records.map((row, index) => normalizeItem(row, index))
    unreadCount.value = Number(unreadRes?.data ?? unreadRes?.data?.count ?? 0) || items.value.filter(i => i.unread).length
    emit('loaded', { items: items.value, unreadCount: unreadCount.value })
  }
  catch {
    items.value = []
  }
  finally {
    loading.value = false
  }
}

async function markAllRead() {
  if (!props.interactive)
    return
  try {
    await messageApi.markAllMessagesRead()
    message.success('已全部标为已读')
    await reload()
  }
  catch (error) {
    message.error(error?.message || '标记失败')
  }
}

async function openItem(item) {
  emit('open', item)
  if (!props.interactive)
    return
  if (props.markReadOnOpen && item.unread && item.id) {
    try {
      await messageApi.markMessageRead(item.id)
      item.unread = false
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
    catch {
      // ignore
    }
  }
  router.push({
    path: props.morePath,
    query: { messageId: item.id, t: Date.now() },
  })
}

function openMore() {
  emit('open-more', { path: props.morePath })
  if (!props.interactive)
    return
  router.push(props.morePath)
}

watch(() => props.pageSize, () => {
  if (props.autoLoad)
    reload()
})

watch(filter, () => {
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
.notice-board {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  height: 100%;
}

.nb-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.nb-head-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.nb-head-copy strong {
  font-size: 14px;
  color: var(--text-primary, #0f172a);
}

.nb-head-copy span,
.nb-unread-label {
  font-size: 12px;
  color: var(--text-tertiary, #94a3b8);
}

.nb-head-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.nb-tabs {
  display: inline-flex;
  gap: 4px;
  padding: 3px;
  border-radius: 8px;
  background: var(--bg-secondary, #f6f8fb);
  width: fit-content;
}

.nb-tab {
  border: 0;
  background: transparent;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  color: var(--text-secondary, #64748b);
  cursor: pointer;
}

.nb-tab.active {
  background: var(--bg-primary, #fff);
  color: var(--nb-accent, var(--primary-color, #2563eb));
  box-shadow: 0 0 0 1px var(--border-light, #e2e8f0);
}

.nb-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
  min-height: 0;
}

.nb-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 12px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
}

.nb-item.unread {
  background: color-mix(in srgb, var(--nb-accent, #2563eb) 4%, #fff);
}

.nb-item.is-interactive {
  cursor: pointer;
}

.nb-item.is-interactive:hover {
  border-color: color-mix(in srgb, var(--nb-accent, #2563eb) 35%, var(--border-light, #e2e8f0));
}

.nb-type {
  flex-shrink: 0;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-secondary, #64748b);
}

.nb-type.type-warning { color: #b45309; background: rgba(217, 119, 6, 0.12); }
.nb-type.type-info { color: #0369a1; background: rgba(2, 132, 199, 0.12); }
.nb-type.type-primary { color: #1d4ed8; background: rgba(37, 99, 235, 0.1); }

.nb-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nb-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.nb-title-row strong {
  font-size: 13px;
  color: var(--text-primary, #0f172a);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nb-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--nb-accent, #2563eb);
  flex-shrink: 0;
}

.nb-summary {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary, #64748b);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.nb-foot {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--text-tertiary, #94a3b8);
}

.nb-empty {
  padding: 28px 12px;
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary, #94a3b8);
  border: 1px dashed var(--border-light, #e2e8f0);
  border-radius: 8px;
}

.is-compact .nb-item {
  padding: 8px 10px;
}

.is-compact .nb-summary {
  display: none;
}

.density-compact .nb-item {
  padding: 7px 9px;
}

.is-card .nb-list {
  gap: 10px;
}
</style>
