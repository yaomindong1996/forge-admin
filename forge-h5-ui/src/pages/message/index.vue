<template>
  <view class="message-page">
    <AiFeedbackHost />
    <view class="message-content">
      <view class="page-head">
        <button class="back-button" @click="goBack">
          <AiIcon name="chevron-left" color="#4e5969" size="md" />
        </button>
        <view class="title-block">
          <text class="page-title">消息中心</text>
          <text class="page-subtitle">通知、审批提醒和系统消息集中处理</text>
        </view>
        <button class="refresh-button" @click="refresh">
          <AiIcon name="refresh-cw" color="#4266f7" size="sm" />
        </button>
      </view>

      <view class="filter-panel">
        <AiSearchBar
          v-model="keyword"
          placeholder="搜索标题或内容"
          @search="refresh"
          @clear="refresh"
        />
        <scroll-view class="tab-scroll" scroll-x :show-scrollbar="false">
          <view class="tab-row">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              class="filter-tab"
              :class="{ active: activeTab === tab.key }"
              @click="switchTab(tab.key)"
            >
              <text>{{ tab.label }}</text>
            </button>
          </view>
        </scroll-view>
        <button v-if="markableUnreadMessages.length" class="mark-read-button" @click="markAllRead">
          <AiIcon name="check" color="#4266f7" size="sm" />
          <text>{{ markAllReadLabel }}</text>
        </button>
      </view>

      <view class="message-list">
        <AiListSkeleton v-if="loading" :rows="6" />

        <AiEmpty
          v-else-if="filteredMessages.length === 0"
          title="暂无消息"
          description="当前筛选条件下没有站内消息。"
          icon="inbox"
        />

        <template v-else>
          <view
            v-for="item in filteredMessages"
            :key="item.id"
            class="message-card"
            :class="{ unread: item.readFlag === 0 }"
            @click="openDetail(item)"
          >
            <view class="message-icon" :class="getToneClass(item)">
              <AiIcon :name="isApprovalMessage(item) ? 'check-square' : 'message-square'" :color="getToneColor(item)" size="md" />
            </view>
            <view class="message-main">
              <view class="message-meta">
                <AiTag :type="getTagType(item)" size="small" round>
                  {{ getMessageCategory(item) }}
                </AiTag>
                <text class="message-time">{{ formatMessageTime(item.createTime || item.receiveTime) }}</text>
              </view>
              <view class="message-title-row">
                <text class="message-title">{{ item.title || '消息通知' }}</text>
                <view v-if="item.readFlag === 0" class="unread-dot" />
              </view>
              <text class="message-desc">{{ stripHtml(item.content || item.description || '-') }}</text>
            </view>
          </view>
        </template>
      </view>
    </view>

    <AiPopupSheet
      v-model="showDetail"
      :title="currentMessage?.title || '消息详情'"
      :description="detailDescription"
      max-height="84vh"
      body-max-height="calc(84vh - 230rpx - env(safe-area-inset-bottom))"
    >
      <view class="detail-content">
        <view class="detail-tags">
          <AiTag :type="getTagType(currentMessage)" size="small" round>
            {{ getMessageCategory(currentMessage) }}
          </AiTag>
          <AiTag :type="currentMessage?.readFlag === 0 ? 'danger' : 'success'" size="small" round>
            {{ currentMessage?.readFlag === 0 ? '未读' : '已读' }}
          </AiTag>
        </view>
        <rich-text v-if="detailHtml" class="detail-html" :nodes="detailHtml" />
        <text v-else class="detail-empty">暂无正文内容</text>

      </view>

      <template #footer>
        <view class="detail-actions">
          <AiButton
            v-if="currentMessage?.readFlag === 0 && !isApprovalMessage(currentMessage)"
            variant="secondary"
            size="sm"
            @click="markRead(currentMessage)"
          >
            标记已读
          </AiButton>
          <AiButton
            v-if="hasBusinessAction"
            size="sm"
            @click="openBiz(currentMessage)"
          >
            {{ businessActionText }}
          </AiButton>
        </view>
      </template>
    </AiPopupSheet>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import AiEmpty from '@/components/AiEmpty.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import AiTag from '@/components/AiTag.vue'
import api from '@/api'
import { showConfirmDialog } from '@/utils/dialog'
import {
  buildFlowTaskDetailUrl,
  isFlowTaskRoute,
  isFlowTodoMessage,
  resolveFlowMessageMode,
  resolveFlowMessageTaskId,
  shouldAutoMarkMessageRead,
} from '@/utils/message-flow-navigation'
import { toast } from '@/utils/notify'

const loading = ref(false)
const messages = ref([])
const bizTypes = ref([])
const unreadCount = ref(0)
const keyword = ref('')
const activeTab = ref('all')
const showDetail = ref(false)
const currentMessage = ref(null)
const pendingOpenId = ref('')

const tabs = computed(() => [
  { key: 'all', label: '全部' },
  { key: 'unread', label: '未读' },
  { key: 'approval', label: '审批提醒' },
  { key: 'read', label: '已读' },
])

const filteredMessages = computed(() => {
  return messages.value.filter((item) => {
    if (activeTab.value === 'unread' && item.readFlag !== 0) {
      return false
    }
    if (activeTab.value === 'read' && item.readFlag !== 1) {
      return false
    }
    if (activeTab.value === 'approval' && !isApprovalMessage(item)) {
      return false
    }
    return true
  })
})
const markableUnreadMessages = computed(() => messages.value.filter(item => item.readFlag === 0 && !isApprovalMessage(item)))
const markAllReadLabel = computed(() => messages.value.some(item => item.readFlag === 0 && isApprovalMessage(item))
  ? '其他消息全部已读'
  : '全部标为已读')
const hasBusinessAction = computed(() => Boolean(
  currentMessage.value && (isApprovalMessage(currentMessage.value) || currentMessage.value.bizType || currentMessage.value.jumpUrl),
))
const businessActionText = computed(() => {
  if (!isApprovalMessage(currentMessage.value)) return '查看业务'
  return Number(currentMessage.value?.readFlag) === 0 ? '去处理' : '查看处理结果'
})

const detailHtml = computed(() => currentMessage.value?.content || '')
const detailDescription = computed(() => {
  const time = formatMessageTime(currentMessage.value?.createTime || currentMessage.value?.receiveTime)
  return time ? `接收时间 ${time}` : '消息详情'
})

onLoad((query = {}) => {
  pendingOpenId.value = query.id ? String(query.id) : ''
})

onShow(async () => {
  await refresh()
  if (pendingOpenId.value) {
    const target = messages.value.find(item => String(item.id) === pendingOpenId.value)
    await openDetail(target || { id: pendingOpenId.value })
    pendingOpenId.value = ''
  }
})

async function refresh() {
  loading.value = true
  try {
    await Promise.all([
      fetchUnreadCount(),
      fetchMessages(),
      fetchBizTypes(),
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
    readFlag: Number(item.readFlag ?? item.readStatus ?? 0),
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

async function openDetail(item) {
  try {
    const res = await api.getMessageDetail(item.id)
    currentMessage.value = {
      ...item,
      ...(res?.data || {}),
      readFlag: Number((res?.data || item).readFlag ?? item.readFlag ?? 0),
    }
    showDetail.value = true
    if (shouldAutoMarkMessageRead(currentMessage.value)) {
      await markRead(currentMessage.value, { silent: true })
    }
  }
  catch (error) {
    console.error('加载消息详情失败:', error)
    toast('消息详情加载失败', { type: 'error' })
  }
}

async function markRead(item, options = {}) {
  if (!item?.id || item.readFlag !== 0) {
    return
  }
  try {
    await api.markMessageRead(item.id)
    item.readFlag = 1
    const listItem = messages.value.find(message => String(message.id) === String(item.id))
    if (listItem) {
      listItem.readFlag = 1
    }
    if (String(currentMessage.value?.id || '') === String(item.id)) {
      currentMessage.value.readFlag = 1
    }
    unreadCount.value = Math.max(0, unreadCount.value - 1)
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
    toast('普通消息已标记为已读', { type: 'success' })
  }
  catch (error) {
    console.error('全部标记已读失败:', error)
    toast('操作失败', { type: 'error' })
  }
}

function openBiz(message) {
  const route = resolveBizRoute(message)
  showDetail.value = false
  if (isApprovalMessage(message) || isFlowTaskRoute(route)) {
    const taskId = resolveFlowMessageTaskId(message, route)
    if (taskId) {
      uni.navigateTo({ url: buildFlowTaskDetailUrl(taskId, resolveFlowMessageMode(message), message?.id) })
    }
    else {
      uni.switchTab({ url: '/pages/todo' })
    }
    return
  }
  if (route && route.startsWith('/pages/')) {
    uni.navigateTo({
      url: route,
      fail: () => toast('移动端业务页暂未接入', { type: 'info' }),
    })
    return
  }
  toast('该消息暂无移动端业务入口', { type: 'info' })
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

function getTagType(item) {
  if (isApprovalMessage(item)) {
    return 'success'
  }
  if (item?.readFlag === 0) {
    return 'primary'
  }
  return 'default'
}

function getToneClass(item) {
  if (isApprovalMessage(item)) {
    return 'tone-emerald'
  }
  return item?.readFlag === 0 ? 'tone-blue' : 'tone-slate'
}

function getToneColor(item) {
  if (isApprovalMessage(item)) {
    return '#16815d'
  }
  return item?.readFlag === 0 ? '#4266f7' : '#4e5969'
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

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped src="../styles/message.scss"></style>
