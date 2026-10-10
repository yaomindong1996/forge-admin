<template>
  <view class="message-page">
    <AiFeedbackHost />
    <AiTabHeader title="消息" :searchable="false" />
    <view class="message-content">
      <!-- 查询与类型切换 -->
      <view class="message-tools">
        <view class="message-query-row">
          <AiSearchBar v-model="keyword" placeholder="搜索消息" @search="refresh" @clear="refresh" />
          <button class="message-filter-trigger" :class="{ 'is-active': readFilter !== 'all' }" aria-label="筛选消息" @click="openFilters">
            <AiIcon name="filter" :color="readFilter !== 'all' ? '#0066ff' : '#747677'" size="sm" />
            <text class="message-filter-text">筛选</text>
          </button>
        </view>
        <view class="message-scope-row">
          <view class="message-scope-tabs">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              class="message-scope-tab"
              :class="{ active: activeTab === tab.key }"
              @click="switchTab(tab.key)"
            >
              {{ tab.label }}
              <text
                v-if="tabBadgeVisible[tab.key]"
                class="scope-count"
              >{{ tabBadgeText[tab.key] }}</text>
            </button>
          </view>
          <!-- 审批类消息由流程办理后自动已读，这里只批量处理普通消息 -->
          <button v-if="markableUnreadMessages.length" class="mark-read-button" :aria-label="markAllReadLabel" @click="markAllRead">
            <AiIcon name="check-circle" color="#747677" size="sm" />
            <text>全部已读</text>
          </button>
        </view>
      </view>

      <!-- 消息列表：钉钉式会话行，左侧分类图标 + 未读红点 -->
      <scroll-view class="message-list" scroll-y :show-scrollbar="false">
        <NoticeEntryRow />
        <AiListSkeleton v-if="loading" :rows="6" />

        <AiEmpty
          v-else-if="filteredMessages.length === 0"
          :title="activeTab === 'unread' ? '暂无未读' : '暂无消息'"
          :description="activeTab === 'unread' ? '当前没有未读站内消息。' : '当前筛选条件下没有站内消息。'"
          icon="inbox"
        />

        <view v-else class="message-group">
          <view
            v-for="item in filteredMessages"
            :key="item.id"
            class="message-row"
            :class="{ unread: isUnreadMessage(item) }"
            @click="openMessage(item)"
          >
            <view class="message-icon" :class="messageCategoryTone(item)">
              <AiIcon :name="messageCategoryIcon(item)" color="currentColor" size="md" class="message-icon__glyph" />
              <view v-if="isUnreadMessage(item)" class="message-dot" />
            </view>
            <view class="message-main">
              <view class="message-title-row">
                <text class="message-title">{{ item.title || '消息通知' }}</text>
                <text class="message-time">{{ formatMessageTime(item.createTime || item.receiveTime) }}</text>
              </view>
              <view class="message-desc-row">
                <text class="message-category">[{{ getMessageCategory(item) }}]</text>
                <text class="message-desc">{{ stripHtml(item.content || item.description || '-') }}</text>
              </view>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>
    <AiFilterSheet v-model="filterVisible" title="筛选消息" @reset="resetFilters" @apply="applyFilters">
      <view class="message-filter-field">
        <text class="message-filter-label">阅读状态</text>
        <AiSelect v-model="draftReadFilter" :options="readOptions" title="选择阅读状态" placeholder="全部" />
      </view>
    </AiFilterSheet>
    <AiTabBar active="message" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import AiEmpty from '@/components/AiEmpty.vue'
import AiFilterSheet from '@/components/AiFilterSheet.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import AiSelect from '@/components/AiSelect.vue'
import AiTabBar from '@/components/AiTabBar.vue'
import AiTabHeader from '@/components/AiTabHeader.vue'
import NoticeEntryRow from '@/components/notice/NoticeEntryRow.vue'
import api from '@/api'
import { useBadgeStore, useNoticeStore } from '@/store'
import { showConfirmDialog } from '@/utils/dialog'
import { takeTabHandoff } from '@/utils/tab-handoff'
import {
  buildFlowTaskDetailUrl,
  isFlowTaskRoute,
  isFlowTodoMessage,
  resolveFlowMessageMode,
  resolveFlowMessageTaskId,
} from '@/utils/message-flow-navigation'
import { toast } from '@/utils/notify'

const badgeStore = useBadgeStore()
const noticeStore = useNoticeStore()
const loading = ref(false)
const messages = ref([])
const bizTypes = ref([])
const unreadCount = ref(0)
const keyword = ref('')
const activeTab = ref('all')
const readFilter = ref('all')
const draftReadFilter = ref('all')
const filterVisible = ref(false)
const pendingOpenId = ref('')
const openingMessageId = ref('')

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'unread', label: '未读' },
  { key: 'system', label: '系统' },
  { key: 'business', label: '业务' },
]
const readOptions = [
  { label: '全部', value: 'all' },
  { label: '已读', value: 'read' },
]

function isUnreadMessage(item = {}) {
  return Number(item?.readFlag) !== 1
}

function getMessageGroup(item) {
  return ['SYSTEM', 'SMS', 'EMAIL'].includes(String(item?.type || '').toUpperCase()) && !isApprovalMessage(item) ? 'system' : 'business'
}

const filteredMessages = computed(() => {
  return messages.value.filter((item) => {
    // 「未读」Tab 优先；其余 Tab 可再叠加筛选抽屉的已读条件。
    if (activeTab.value === 'unread') {
      return isUnreadMessage(item)
    }
    if (readFilter.value === 'read' && isUnreadMessage(item)) {
      return false
    }
    if (activeTab.value === 'system' && getMessageGroup(item) !== 'system') {
      return false
    }
    if (activeTab.value === 'business' && getMessageGroup(item) !== 'business') {
      return false
    }
    return true
  })
})

const tabCounts = computed(() => ({
  all: messages.value.length,
  unread: messages.value.filter(item => isUnreadMessage(item)).length,
  system: messages.value.filter(item => getMessageGroup(item) === 'system').length,
  business: messages.value.filter(item => getMessageGroup(item) === 'business').length,
}))

const tabBadgeVisible = computed(() => {
  const visible = {}
  tabs.forEach((tab) => {
    const count = Number(tabCounts.value[tab.key] || 0)
    // 未读角标始终可见；其它 Tab 仅在选中时显示数量。
    visible[tab.key] = count > 0 && (tab.key === 'unread' || activeTab.value === tab.key)
  })
  return visible
})

const tabBadgeText = computed(() => {
  const text = {}
  tabs.forEach((tab) => {
    const count = Number(tabCounts.value[tab.key] || 0)
    text[tab.key] = count > 99 ? '99+' : String(count)
  })
  return text
})

const markableUnreadMessages = computed(() => messages.value.filter(item => isUnreadMessage(item) && !isApprovalMessage(item)))
const markAllReadLabel = computed(() => messages.value.some(item => isUnreadMessage(item) && isApprovalMessage(item))
  ? '其他消息全部已读'
  : '全部标为已读')
onLoad((query = {}) => {
  pendingOpenId.value = query.id ? String(query.id) : ''
  const tab = String(query.tab || query.readFilter || '').toLowerCase()
  if (['all', 'unread', 'system', 'business'].includes(tab)) {
    activeTab.value = tab === 'read' ? 'all' : tab
  }
  if (tab === 'unread') {
    readFilter.value = 'all'
    draftReadFilter.value = 'all'
  }
})

onShow(async () => {
  const handoff = takeTabHandoff('message')
  if (['all', 'unread', 'system', 'business'].includes(handoff?.tab)) {
    activeTab.value = handoff.tab
    readFilter.value = 'all'
    draftReadFilter.value = 'all'
  }
  await refresh()
  // tabBar 页面无法携带查询参数，首页通知使用一次性存储交接待打开的消息。
  const fromHome = uni.getStorageSync('forge_h5_pending_message_id')
  if (fromHome) {
    pendingOpenId.value = String(fromHome)
    uni.removeStorageSync('forge_h5_pending_message_id')
  }
  if (pendingOpenId.value) {
    const target = messages.value.find(item => String(item.id) === pendingOpenId.value)
    await openMessage(target || { id: pendingOpenId.value })
    pendingOpenId.value = ''
  }
})

onPullDownRefresh(async () => {
  try { await refresh() }
  finally { uni.stopPullDownRefresh() }
})

async function refresh() {
  loading.value = true
  try {
    await Promise.all([
      fetchUnreadCount(),
      fetchMessages(),
      fetchBizTypes(),
      noticeStore.loadLatest(),
    ])
  }
  catch (error) {
    console.error('刷新消息失败:', error)
    toast('消息加载失败，请稍后重试', { type: 'error' })
  }
  finally {
    loading.value = false
  }
}

async function fetchMessages() {
  const params = {
    pageNum: 1,
    pageSize: 80,
  }
  if (keyword.value.trim()) {
    params.keyword = keyword.value.trim()
  }

  const res = await api.getMessagePage(params)
  messages.value = normalizeRecords(res?.data)
}

async function fetchUnreadCount() {
  const res = await api.getUnreadMessageCount()
  unreadCount.value = normalizeUnreadCount(res?.data)
  badgeStore.setUnreadCount(unreadCount.value)
}

async function fetchBizTypes() {
  try {
    const res = await api.getEnabledMessageBizTypes()
    bizTypes.value = Array.isArray(res?.data) ? res.data : []
  }
  catch {
    bizTypes.value = []
  }
}

function normalizeRecords(data) {
  const records = Array.isArray(data)
    ? data
    : data?.records || data?.list || data?.rows || []
  return records.map(item => ({
    ...item,
    // 仅明确已读记为 1；其余一律按未读，避免接口缺字段/字符串导致未读被滤掉。
    readFlag: Number(item.readFlag ?? item.readStatus ?? item.read_flag) === 1 ? 1 : 0,
  }))
}

function normalizeUnreadCount(data) {
  if (typeof data === 'number') {
    return data
  }
  return Number(data?.totalCount || data?.unreadCount || data?.count || 0)
}

function switchTab(key) {
  activeTab.value = key
}

function openFilters() { draftReadFilter.value = readFilter.value; filterVisible.value = true }
function resetFilters() { draftReadFilter.value = 'all' }
function applyFilters() { readFilter.value = draftReadFilter.value; filterVisible.value = false }

async function openMessage(item) {
  if (!item?.id || openingMessageId.value) return
  openingMessageId.value = String(item.id)
  try {
    const res = await api.getMessageDetail(item.id)
    const message = {
      ...item,
      ...(res?.data || {}),
      readFlag: Number((res?.data || item).readFlag ?? item.readFlag ?? 0),
    }
    const route = resolveBizRoute(message)
    if (isApprovalMessage(message) || isFlowTaskRoute(route)) {
      const taskId = resolveFlowMessageTaskId(message, route)
      if (taskId) {
        uni.navigateTo({ url: buildFlowTaskDetailUrl(taskId, resolveFlowMessageMode(message), message.id) })
      }
      else {
        uni.switchTab({ url: '/pages/todo' })
      }
      return
    }
    if (isUnreadMessage(message)) await markRead(message, { silent: true })
    if (route.startsWith('/pages/') && !route.startsWith('/pages/message/')) {
      uni.navigateTo({ url: route, fail: () => openMessagePage(message.id) })
      return
    }
    openMessagePage(message.id)
  }
  catch (error) {
    console.error('打开消息失败:', error)
    toast('打开消息失败，请稍后重试', { type: 'error' })
  }
  finally { openingMessageId.value = '' }
}

function openMessagePage(id) {
  uni.navigateTo({ url: `/pages/message/detail?id=${encodeURIComponent(String(id))}` })
}

async function markRead(item, options = {}) {
  if (!item?.id || !isUnreadMessage(item)) {
    return
  }
  try {
    await api.markMessageRead(item.id)
    item.readFlag = 1
    const listItem = messages.value.find(message => String(message.id) === String(item.id))
    if (listItem) {
      listItem.readFlag = 1
    }
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    badgeStore.setUnreadCount(unreadCount.value)
    if (!options.silent) {
      toast('已标记为已读', { type: 'success' })
    }
  }
  catch (error) {
    console.error('标记消息已读失败:', error)
    if (!options.silent) {
      toast('操作失败', { type: 'error' })
    }
  }
}

async function markAllRead() {
  const messageIds = markableUnreadMessages.value.map(item => item.id).filter(Boolean)
  if (!messageIds.length) return
  const confirmed = await showConfirmDialog({
    title: '标记消息已读',
    description: '普通通知和系统消息将标记为已读；待处理审批保留未读，办理完成后由流程自动更新。',
    confirmText: '确认标记',
    cancelText: '取消',
  })
  if (!confirmed) {
    return
  }
  try {
    await api.markMessagesReadBatch(messageIds)
    const idSet = new Set(messageIds.map(String))
    messages.value = messages.value.map(item => idSet.has(String(item.id)) ? { ...item, readFlag: 1 } : item)
    unreadCount.value = Math.max(0, unreadCount.value - messageIds.length)
    badgeStore.setUnreadCount(unreadCount.value)
    toast('普通消息已标记为已读', { type: 'success' })
  }
  catch (error) {
    console.error('全部标记已读失败:', error)
    toast('操作失败', { type: 'error' })
  }
}

function resolveBizRoute(message) {
  if (message?.jumpUrl) {
    return replaceRouteParams(message.jumpUrl, message)
  }
  if (!message?.bizType) {
    return ''
  }
  const config = bizTypes.value.find(item => item.bizType === message.bizType)
  return config?.jumpUrl ? replaceRouteParams(config.jumpUrl, message) : ''
}

function replaceRouteParams(route, message) {
  return String(route || '')
    .replace(/\$\{bizKey\}/g, message?.bizKey || '')
    .replace(/\$\{messageId\}/g, message?.id || '')
}

function isApprovalMessage(item) {
  return isFlowTodoMessage(item)
}

function getMessageCategory(item) {
  if (isApprovalMessage(item)) {
    return '审批'
  }
  const map = {
    SYSTEM: '系统',
    SMS: '短信',
    EMAIL: '邮件',
    CUSTOM: '通知',
  }
  return map[item?.type] || '通知'
}

function messageCategoryIcon(item = {}) {
  if (isApprovalMessage(item)) return 'check-square'
  const icons = { SYSTEM: 'settings', SMS: 'smartphone', EMAIL: 'mail' }
  return icons[String(item.type || '').toUpperCase()] || 'bell'
}

function messageCategoryTone(item = {}) {
  if (isApprovalMessage(item)) return 'tone-blue'
  const identity = `${getMessageCategory(item)} ${item.type || ''} ${item.bizType || ''}`.toLowerCase()
  const semanticTones = [
    [/(系统|system)/, 'tone-purple'],
    [/(短信|sms)/, 'tone-cyan'],
    [/(邮件|email)/, 'tone-emerald'],
    [/(通知|公告|notice|message|custom)/, 'tone-orange'],
  ]
  const matched = semanticTones.find(([pattern]) => pattern.test(identity))
  if (matched) return matched[1]
  return 'tone-rose'
}

function stripHtml(value) {
  return String(value || '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function formatMessageTime(value) {
  const date = parseMessageDate(value)
  if (!date) {
    return ''
  }
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff >= 0 && diff < 60 * 1000) {
    return '刚刚'
  }
  if (diff >= 0 && diff < 60 * 60 * 1000) {
    return `${Math.max(1, Math.floor(diff / 60000))}分钟前`
  }
  if (diff >= 0 && diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function parseMessageDate(value) {
  if (!value) {
    return null
  }
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    return new Date(year, month - 1, day, hour, minute, second)
  }
  const date = new Date(typeof value === 'string' ? value.replace(' ', 'T') : value)
  return Number.isNaN(date.getTime()) ? null : date
}

</script>

<style lang="scss" scoped src="../styles/message.scss"></style>
