<template>
  <div class="message-notification-wrapper">
    <NBadge
      :value="unreadCount"
      :max="99"
      :show="unreadCount > 0"
      :offset="[-5, 5]"
    >
      <button class="notification-trigger" type="button" @click="openPanel">
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
          <h2>消息通知</h2>
          <button class="icon-button" type="button" @click="showPanel = false">
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
            @click="activeTab = tab.key"
          >
            <span>{{ tab.label }}</span>
            <span v-if="tab.showCount && tab.count > 0" class="tab-count">{{ tab.count }}</span>
          </button>
          <button class="tab-action" type="button" @click="handleViewAll">
            <i class="i-material-symbols:settings-outline" />
          </button>
        </div>

        <div class="message-filter-row">
          <NSelect
            v-model:value="rangeDays"
            :options="rangeOptions"
            size="small"
            class="range-select"
          />
        </div>

        <NScrollbar class="message-scrollbar">
          <div v-if="loading" class="message-loading">
            <NSpin size="small" />
          </div>

          <div v-else-if="filteredMessages.length > 0" class="message-list">
            <article
              v-for="msg in filteredMessages"
              :key="msg.id"
              class="message-card"
              :class="{ unread: msg.readFlag === 0, approval: isApprovalMessage(msg) }"
              @click="handleMessageClick(msg)"
            >
              <div class="message-card-main">
                <div class="message-icon" :class="{ approval: isApprovalMessage(msg) }">
                  <i :class="isApprovalMessage(msg) ? 'i-material-symbols:approval-delegation-outline' : 'i-material-symbols:mail-outline'" />
                </div>

                <div class="message-content">
                  <div class="message-meta">
                    <span>{{ getMessageCategory(msg) }}</span>
                    <time>{{ formatMessageTime(msg.createTime) }}</time>
                  </div>
                  <h3>{{ msg.title || '消息通知' }}</h3>
                  <p>{{ msg.content || '-' }}</p>

                  <div v-if="isApprovalMessage(msg)" class="approval-meta">
                    <span>任务编号：{{ msg.bizKey || '-' }}</span>
                    <span>状态：{{ msg.readFlag === 0 ? '未读' : '已读' }}</span>
                  </div>
                </div>
              </div>

              <div class="message-card-actions" @click.stop>
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
                  v-if="msg.readFlag === 0"
                  size="small"
                  ghost
                  @click="markRead(msg)"
                >
                  标记已读
                </NButton>
              </div>
            </article>
          </div>

          <NEmpty v-else description="暂无消息" class="message-empty" />
        </NScrollbar>

        <footer class="message-center-footer">
          <NButton quaternary size="small" @click="handleViewAll">
            查看全部消息
          </NButton>
          <NButton
            type="primary"
            ghost
            size="small"
            :disabled="unreadCount <= 0"
            @click="handleMarkAllRead"
          >
            全部标记为已读
          </NButton>
        </footer>
      </div>
    </NDrawer>
  </div>
</template>

<script setup>
import { NBadge, NButton, NDrawer, NEmpty, NScrollbar, NSelect, NSpin } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import messageApi from '@/api/message'
import { useAuthStore } from '@/store'
import {
  isFlowApprovalMessage,
  isPendingFlowApprovalMessage,
} from './message-notification-utils'

const router = useRouter()
const authStore = useAuthStore()
const unreadCount = ref(0)
const messages = ref([])
const showPanel = ref(false)
const loading = ref(false)
const activeTab = ref('approval')
const rangeDays = ref(180)
const bizTypeOptions = ref([])
const panelWidth = computed(() => (window.innerWidth < 520 ? window.innerWidth : 430))

const rangeOptions = [
  { label: '近30天', value: 30 },
  { label: '近90天', value: 90 },
  { label: '近180天', value: 180 },
  { label: '全部时间', value: 0 },
]

const approvalMessages = computed(() => messages.value.filter(isApprovalMessage))
const readMessages = computed(() => messages.value.filter(msg => msg.readFlag === 1))

const tabs = computed(() => [
  { key: 'approval', label: '审批', count: approvalMessages.value.filter(msg => msg.readFlag === 0).length, showCount: true },
  { key: 'unread', label: '未读', count: unreadCount.value, showCount: true },
  { key: 'read', label: '已读', count: readMessages.value.length, showCount: false },
  { key: 'all', label: '全部', count: messages.value.length, showCount: false },
])

const filteredMessages = computed(() => {
  return messages.value
    .filter((msg) => {
      if (activeTab.value === 'approval')
        return isApprovalMessage(msg)
      if (activeTab.value === 'unread')
        return msg.readFlag === 0
      if (activeTab.value === 'read')
        return msg.readFlag === 1
      return true
    })
})

function isApprovalMessage(msg) {
  return isFlowApprovalMessage(msg)
}

function isPendingApprovalMessage(msg) {
  return isPendingFlowApprovalMessage(msg)
}

function getMessageCategory(msg) {
  if (isApprovalMessage(msg))
    return '审批'
  const map = {
    SYSTEM: '系统',
    SMS: '短信',
    EMAIL: '邮件',
    CUSTOM: '通知',
  }
  return map[msg.type] || '通知'
}

function formatMessageTime(value) {
  const date = parseMessageDate(value)
  if (!date)
    return ''
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${month}月${day}日 ${hour}:${minute}`
}

function parseMessageDate(value) {
  if (!value)
    return null
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    return new Date(year, month - 1, day, hour, minute, second)
  }
  const date = new Date(typeof value === 'string' ? value.replace(' ', 'T') : value)
  return Number.isNaN(date.getTime()) ? null : date
}

function formatQueryDateTime(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  const second = `${date.getSeconds()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function buildMessageQuery() {
  if (!rangeDays.value)
    return {}

  const end = new Date()
  const start = new Date(end.getTime() - rangeDays.value * 24 * 60 * 60 * 1000)
  return {
    startTime: formatQueryDateTime(start),
    endTime: formatQueryDateTime(end),
  }
}

async function fetchUnreadCount() {
  const res = await messageApi.getUnreadCount()
  if (res.code === 200 && res.data)
    unreadCount.value = res.data.totalCount || 0
}

async function fetchLatestMessages() {
  const res = await messageApi.getMessagePage(buildMessageQuery(), 1, 100)
  if (res.code === 200 && res.data)
    messages.value = res.data.list || res.data.records || []
}

async function loadBizTypes() {
  const res = await messageApi.getBizTypeListEnabled()
  if (res.code === 200 && res.data)
    bizTypeOptions.value = res.data
}

async function initData() {
  if (!authStore.accessToken) {
    unreadCount.value = 0
    messages.value = []
    return
  }

  loading.value = true
  try {
    await Promise.all([
      fetchUnreadCount(),
      fetchLatestMessages(),
      loadBizTypes(),
    ])
  }
  catch (error) {
    console.error('初始化消息数据失败:', error)
  }
  finally {
    loading.value = false
  }
}

function openPanel() {
  showPanel.value = true
  initData()
}

async function handleMessageClick(msg) {
  if (isPendingApprovalMessage(msg)) {
    await openApproval(msg)
    return
  }
  if (isApprovalMessage(msg)) {
    showPanel.value = false
    router.push('/flow/done')
    return
  }
  await markRead(msg, false)
  const route = resolveBizRoute(msg)
  showPanel.value = false
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
    router.push('/flow/done')
    return
  }
  await markRead(msg, false)
  const taskId = msg.bizKey
  showPanel.value = false
  if (!taskId) {
    router.push('/flow/todo')
    return
  }
  router.push({
    path: '/flow/todo',
    query: {
      taskId,
      source: 'message',
      t: Date.now(),
    },
  })
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
  await messageApi.markMessageRead(msg.id)
  msg.readFlag = 1
  unreadCount.value = Math.max(0, unreadCount.value - 1)
  if (refresh) {
    await fetchUnreadCount()
  }
}

function handleViewAll() {
  showPanel.value = false
  router.push('/message/message-list')
}

async function handleMarkAllRead() {
  try {
    await messageApi.markAllMessagesRead()
    window.$message.success('已全部标记为已读')
    await initData()
  }
  catch {
    window.$message.error('操作失败')
  }
}

onMounted(() => {
  initData()
})

watch(
  () => authStore.accessToken,
  () => {
    initData()
  },
)

watch(
  () => rangeDays.value,
  () => {
    if (showPanel.value)
      fetchLatestMessages()
  },
)

defineExpose({
  refresh: initData,
})
</script>

<style scoped>
.message-notification-wrapper {
  margin-right: 8px;
  color: var(--top-menu-text-color, var(--layout-header-text-color));
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
  color: currentColor;
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
  padding: 12px 18px 8px;
}

.range-select {
  width: 100%;
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
