<template>
  <div class="message-notification-wrapper">
    <NBadge
      :value="totalUnreadCount"
      :max="99"
      :show="totalUnreadCount > 0"
      :offset="[-5, 5]"
    >
      <button class="notification-trigger" type="button" :aria-label="`通知中心，${totalUnreadCount} 条未读`" title="通知中心" @click="openPanel">
        <i class="i-material-symbols:notifications-outline" />
      </button>
    </NBadge>

    <NDrawer
      v-model:show="showPanel"
      placement="right"
      :width="panelWidth"
      :trap-focus="false"
      class="message-center-drawer"
    >
      <div class="message-center">
        <header class="message-center-header">
          <h2>通知中心</h2>
          <button class="icon-button" type="button" aria-label="关闭通知中心" @click="showPanel = false">
            <i class="i-material-symbols:close" />
          </button>
        </header>

        <div class="message-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="message-tab"
            :class="{ active: activeTab === tab.key }"
            type="button"
            :aria-pressed="activeTab === tab.key"
            @click="activeTab = tab.key"
          >
            <span>{{ tab.label }}</span>
            <span v-if="tab.showCount && tab.count > 0" class="tab-count">{{ tab.count }}</span>
          </button>
          <button class="tab-action" type="button" title="刷新通知" @click="initData">
            <i class="i-material-symbols:refresh" />
          </button>
        </div>

        <div class="message-filter-row">
          <NSelect v-model:value="readState" :options="readOptions" size="small" aria-label="阅读状态" />
          <NSelect
            v-model:value="rangeDays"
            :options="rangeOptions"
            size="small"
            class="range-select"
            aria-label="通知时间范围"
          />
        </div>

        <p class="notification-hint">
          未读数包含全部时间；公告打开详情后标记已读。
        </p>
        <NAlert v-if="messageError || noticeStore.error" type="warning" :show-icon="false" class="notification-error">
          {{ [messageError, noticeStore.error].filter(Boolean).join('；') }}
          <NButton text type="primary" @click="initData">
            重试
          </NButton>
        </NAlert>
        <NScrollbar class="message-scrollbar">
          <div v-if="loading || noticeStore.loading" class="message-loading">
            <NSpin size="small" />
          </div>

          <div v-else-if="filteredMessages.length > 0" class="message-list">
            <article
              v-for="msg in filteredMessages"
              :key="msg.key"
              class="message-card"
              :class="{ unread: msg.readFlag === 0, approval: isApprovalMessage(msg) }"
              role="button"
              tabindex="0"
              @keydown.enter="handleMessageClick(msg)"
              @keydown.space.prevent="handleMessageClick(msg)"
              @click="handleMessageClick(msg)"
            >
              <div class="message-card-main">
                <div class="message-icon" :class="{ approval: isApprovalMessage(msg) }">
                  <i :class="msg.source === 'notice' ? 'i-material-symbols:campaign-outline' : isApprovalMessage(msg) ? 'i-material-symbols:approval-delegation-outline' : 'i-material-symbols:mail-outline'" />
                </div>

                <div class="message-content">
                  <div class="message-meta">
                    <span>{{ getMessageCategory(msg) }}</span>
                    <time>{{ formatMessageTime(msg.createTime) }}</time>
                  </div>
                  <h3><span v-if="msg.readFlag === 0" class="unread-dot" />{{ msg.title || '消息通知' }}</h3>
                  <p>{{ msg.content || '-' }}</p>

                  <div v-if="isApprovalMessage(msg)" class="approval-meta">
                    <span>任务编号：{{ msg.bizKey || '-' }}</span>
                    <span>状态：{{ msg.readFlag === 0 ? '未读' : '已读' }}</span>
                  </div>
                </div>
              </div>

              <div v-if="msg.source === 'notice' || msg.readFlag === 0" class="message-card-actions" @click.stop @keydown.stop>
                <NButton v-if="msg.source === 'notice'" text type="primary" size="small" @click="handleMessageClick(msg)">
                  阅读公告
                </NButton>
                <NButton
                  v-if="isPendingApprovalMessage(msg)"
                  type="primary"
                  size="small"
                  class="approval-button"
                  @click="openApproval(msg)"
                >
                  去审批
                </NButton>
                <NButton
                  v-if="msg.source !== 'notice' && msg.readFlag === 0"
                  size="small"
                  ghost
                  @click="markRead(msg)"
                >
                  标记已读
                </NButton>
              </div>
            </article>
          </div>

          <NEmpty v-else description="暂无符合条件的通知" class="message-empty" />
          <div v-if="hasMore" class="load-more">
            <span class="load-more-text" @click="loadMore">
              <NSpin v-if="loading || noticeStore.loading" :size="12" />
              <template v-else>加载更多</template>
            </span>
          </div>
        </NScrollbar>

        <footer class="message-center-footer">
          <NButton quaternary size="small" @click="handleViewAll">
            {{ activeTab === 'notice' ? '查看全部公告' : '查看全部消息' }}
          </NButton>
          <NButton
            v-if="activeTab !== 'notice'"
            type="primary"
            ghost
            size="small"
            title="仅标记个人消息，不包含公告"
            :disabled="unreadCount <= 0"
            @click="handleMarkAllRead"
          >
            消息全部已读
          </NButton>
        </footer>
      </div>
    </NDrawer>
  </div>
</template>

<script setup>
import { NAlert, NBadge, NButton, NDrawer, NEmpty, NScrollbar, NSelect, NSpin } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import messageApi from '@/api/message'
import { useNoticeStore } from '@/stores/system/noticeStore'
import {
  filterNotifications,
  isFlowApprovalMessage,
  isPendingFlowApprovalMessage,
  mergeMessageNavigationTarget,
  mergeNotifications,
  parseNotificationDate,
} from './message-notification-utils'

const props = defineProps({
  messageRoute: {
    type: [String, Object],
    default: '',
  },
  todoRoute: {
    type: [String, Object],
    default: '',
  },
  doneRoute: {
    type: [String, Object],
    default: '',
  },
})

const router = useRouter()
const noticeStore = useNoticeStore()
const unreadCount = ref(0)
const messages = ref([])
const showPanel = ref(false)
const loading = ref(false)
const activeTab = ref('all')
const readState = ref('all')
const rangeDays = ref(0)
const bizTypeOptions = ref([])
const panelWidth = 'min(460px, 100vw)'
const messageError = ref('')
const messagePage = ref(0)
const messageTotal = ref(0)
let requestSequence = 0
const totalUnreadCount = computed(() => unreadCount.value + noticeStore.unreadCount)
const hasMore = computed(() => activeTab.value === 'notice'
  ? noticeStore.hasMore
  : activeTab.value === 'all' ? noticeStore.hasMore || messages.value.length < messageTotal.value : messages.value.length < messageTotal.value)
const readOptions = [
  { label: '全部状态', value: 'all' },
  { label: '未读', value: 0 },
  { label: '已读', value: 1 },
]

const rangeOptions = [
  { label: '近30天', value: 30 },
  { label: '近90天', value: 90 },
  { label: '近180天', value: 180 },
  { label: '全部时间', value: 0 },
]

const tabs = computed(() => [
  { key: 'all', label: '全部', count: totalUnreadCount.value, showCount: true },
  { key: 'approval', label: '审批' },
  { key: 'notice', label: '公告', count: noticeStore.unreadCount, showCount: true },
  { key: 'other', label: '其他消息' },
])
const entries = computed(() => mergeNotifications(messages.value, noticeStore.notices, noticeStore.isUnread))
const filteredMessages = computed(() => filterNotifications(entries.value, activeTab.value, readState.value, rangeDays.value))

function isApprovalMessage(msg) {
  return isFlowApprovalMessage(msg)
}

function isPendingApprovalMessage(msg) {
  return isPendingFlowApprovalMessage(msg)
}

function getMessageCategory(msg) {
  if (isApprovalMessage(msg))
    return '审批'
  return msg.source === 'notice' ? '公告' : '其他消息'
}

function formatMessageTime(value) {
  const date = parseNotificationDate(value)
  if (!date)
    return ''
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${month}月${day}日 ${hour}:${minute}`
}

async function fetchUnreadCount() {
  const context = noticeStore.contextVersion
  const res = await messageApi.getUnreadCount()
  if (res.code === 200 && context === noticeStore.contextVersion)
    unreadCount.value = Number(res.data?.totalCount) || 0
}

async function fetchMessages(append = false) {
  const sequence = ++requestSequence
  const context = noticeStore.contextVersion
  const page = append ? messagePage.value + 1 : 1
  loading.value = true
  messageError.value = ''
  try {
    const res = await messageApi.getMessagePage({}, page, 50)
    if (context !== noticeStore.contextVersion || sequence !== requestSequence)
      return
    if (res.code !== 200)
      throw new Error('加载个人消息失败')
    const records = res.data?.list || res.data?.records || []
    messages.value = append ? [...messages.value, ...records] : records
    messageTotal.value = Number(res.data?.total) || 0
    messagePage.value = page
  }
  catch (error) {
    if (context === noticeStore.contextVersion && sequence === requestSequence)
      messageError.value = '加载个人消息失败，请重试'
    console.error('加载个人消息失败:', error)
  }
  finally {
    if (context === noticeStore.contextVersion && sequence === requestSequence)
      loading.value = false
  }
}

async function initData() {
  if (!noticeStore.ready)
    return
  const context = noticeStore.contextVersion
  await Promise.all([
    fetchMessages(),
    noticeStore.refresh(),
    fetchUnreadCount().catch(() => {
      if (context === noticeStore.contextVersion)
        messageError.value = '加载消息未读数失败'
    }),
    messageApi.getBizTypeListEnabled().then((res) => {
      if (context === noticeStore.contextVersion && res.code === 200)
        bizTypeOptions.value = res.data || []
    }).catch(error => console.error('加载消息业务类型失败:', error)),
  ])
}

async function loadMore() {
  if (loading.value || noticeStore.loading)
    return
  await Promise.all([
    activeTab.value !== 'notice' && messages.value.length < messageTotal.value ? fetchMessages(true) : null,
    ['all', 'notice'].includes(activeTab.value) && noticeStore.hasMore ? noticeStore.loadMore() : null,
  ])
}

function openPanel() {
  showPanel.value = true
  initData()
}

async function handleMessageClick(msg) {
  if (msg.source === 'notice') {
    await noticeStore.openNotice({ noticeId: msg.id })
    return
  }
  if (isPendingApprovalMessage(msg)) {
    await openApproval(msg)
    return
  }
  if (isApprovalMessage(msg)) {
    showPanel.value = false
    navigateTo(props.doneRoute || '/flow/done')
    return
  }
  await markRead(msg, false)
  showPanel.value = false
  if (props.messageRoute) {
    navigateTo(props.messageRoute, { messageId: msg.id })
    return
  }
  const route = resolveBizRoute(msg)
  if (route) {
    router.push(route)
  }
  else {
    router.push('/message/message-list')
  }
}

async function openApproval(msg) {
  if (!isPendingApprovalMessage(msg)) {
    showPanel.value = false
    navigateTo(props.doneRoute || '/flow/done')
    return
  }
  await markRead(msg, false)
  const taskId = msg.bizKey
  showPanel.value = false
  if (!taskId) {
    navigateTo(props.todoRoute || '/flow/todo')
    return
  }
  navigateTo(props.todoRoute || '/flow/todo', {
    taskId,
    source: 'message',
    t: Date.now(),
  })
}

function navigateTo(target, query = {}) {
  return router.push(mergeMessageNavigationTarget(target, query))
}

function resolveBizRoute(msg) {
  if (msg?.jumpUrl)
    return msg.jumpUrl
  if (msg?.bizType && msg?.bizKey) {
    const bizConfig = bizTypeOptions.value.find(opt => opt.bizType === msg.bizType)
    if (bizConfig?.jumpUrl) {
      return bizConfig.jumpUrl
        .replace(/\$\{bizKey\}/g, msg.bizKey)
        .replace(/\$\{messageId\}/g, msg.id)
    }
  }
  return ''
}

async function markRead(msg, refresh = true) {
  if (!msg?.id || msg.readFlag !== 0)
    return
  const context = noticeStore.contextVersion
  const result = await messageApi.markMessageRead(msg.id)
  if (result.code !== 200 || context !== noticeStore.contextVersion)
    return
  const original = messages.value.find(item => String(item.id) === String(msg.id))
  if (original)
    original.readFlag = 1
  unreadCount.value = Math.max(0, unreadCount.value - 1)
  if (refresh) {
    await fetchUnreadCount()
  }
}

function handleViewAll() {
  showPanel.value = false
  navigateTo(activeTab.value === 'notice' ? '/system/notice-list' : props.messageRoute || '/message/message-list')
}

async function handleMarkAllRead() {
  try {
    await messageApi.markAllMessagesRead()
    window.$message.success('个人消息已标记已读，公告需逐条阅读')
    await initData()
  }
  catch {
    window.$message.error('操作失败')
  }
}

watch(() => noticeStore.contextVersion, () => {
  ++requestSequence
  messages.value = []
  unreadCount.value = 0
  messagePage.value = 0
  messageTotal.value = 0
  bizTypeOptions.value = []
  loading.value = false
  messageError.value = ''
  showPanel.value = false
  initData()
}, { immediate: true })

defineExpose({
  refresh: initData,
})
</script>

<style scoped>
.message-notification-wrapper {
  margin-right: 8px;
  color: var(--chrome-text, var(--text-secondary));
}

.notification-trigger {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.notification-trigger i {
  font-size: 22px;
}

:deep(.n-badge-sup) {
  right: 2px;
  top: 2px;
  font-size: 12px;
  font-weight: 700;
  min-width: 20px;
  height: 20px;
  line-height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ef5b68;
  color: #fff;
  box-shadow: 0 0 0 2px #fff;
}

:deep(.message-center-drawer .n-drawer-content) {
  background: #fff;
}

:deep(.message-center-drawer .n-drawer-body-content-wrapper) {
  padding: 0;
}

.message-center {
  height: 100%;
  display: flex;
  flex-direction: column;
  color: #202124;
}

.message-center-header {
  height: 64px;
  padding: 18px 22px 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.message-center-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 650;
  letter-spacing: 0;
}

.icon-button,
.tab-action {
  width: 32px;
  height: 32px;
  border: 0;
  background: transparent;
  color: #5f6368;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
}

.icon-button:hover,
.tab-action:hover {
  background: #f1f3f4;
  color: #202124;
}

.icon-button i,
.tab-action i {
  font-size: 20px;
}

.message-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) 42px;
  align-items: end;
  gap: 0;
  padding: 0 22px;
  border-bottom: 1px solid #e8eaed;
}

.message-tab {
  min-width: 0;
  height: 44px;
  position: relative;
  border: 0;
  background: transparent;
  color: #1f1f1f;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0;
  cursor: pointer;
}

.message-tab.active {
  color: #627e4f;
}

.message-tab.active::after {
  content: '';
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 2px;
  border-radius: 3px 3px 0 0;
  background: #6a8355;
}

.tab-count {
  position: absolute;
  min-width: 20px;
  height: 18px;
  top: 4px;
  right: 4px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ef5b68;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  font-weight: 700;
}

.message-filter-row {
  display: flex;
  gap: 8px;
  padding: 12px 22px 8px;
}

.message-filter-row :deep(.n-select) {
  flex: 1;
  min-width: 0;
}

.notification-hint {
  margin: 0;
  padding: 0 22px 8px;
  font-size: 11px;
  color: #8a8d91;
  line-height: 1.4;
}

.message-scrollbar {
  flex: 1;
  min-height: 0;
}

.message-loading {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-list {
  padding: 8px 0;
}

.message-card {
  padding: 12px 22px 14px;
  border-bottom: 1px solid #edf0ed;
  cursor: pointer;
  transition: background-color 0.16s ease;
}

.message-card:hover {
  background: #fafbf8;
}

.message-card-main {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 10px;
}

.message-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
  background: #e8f0fe;
  color: #1a73e8;
}

.message-icon.approval {
  background: #e8f0fe;
  color: #1a73e8;
}

.message-icon i {
  font-size: 17px;
}

.message-content {
  min-width: 0;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #8a8d91;
  font-size: 12px;
  line-height: 18px;
}

.message-meta span {
  color: #2f3337;
  font-size: 13px;
  font-weight: 600;
}

.message-content h3 {
  margin: 6px 0 6px;
  color: #26292d;
  font-size: 14px;
  line-height: 1.45;
  font-weight: 600;
  letter-spacing: 0;
}

.message-content p {
  margin: 0;
  color: #7b8087;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}

.approval-meta {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  color: #7b8087;
  font-size: 12px;
}

.message-card-actions {
  margin-top: 10px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding-left: 42px;
}

.message-card-actions :deep(.n-button) {
  height: 30px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
}

.approval-button {
  --n-color: #6a8355 !important;
  --n-color-hover: #5f774c !important;
  --n-color-pressed: #526a42 !important;
  --n-color-focus: #6a8355 !important;
  --n-border: 1px solid #6a8355 !important;
  --n-border-hover: 1px solid #5f774c !important;
  --n-border-pressed: 1px solid #526a42 !important;
  --n-border-focus: 1px solid #6a8355 !important;
}

.message-center-footer {
  min-height: 52px;
  padding: 10px 18px;
  border-top: 1px solid #edf0ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.message-empty {
  margin-top: 80px;
}

.load-more {
  padding: 12px 22px;
  text-align: center;
}

.load-more-text {
  display: inline-block;
  font-size: 12px;
  color: #8a8d91;
  cursor: pointer;
  transition: color 0.16s ease;
}

.load-more-text:hover {
  color: var(--primary-color, #165dff);
}

@media (max-width: 520px) {
  .message-center-header {
    padding: 22px 20px 8px;
  }

  .message-tabs {
    padding: 0 16px;
  }

  .message-card {
    padding: 16px 20px 18px;
  }

  .message-card-actions {
    padding-left: 0;
  }
}
</style>
