<template>
  <div class="ai-session-page">
    <div class="stats-grid">
      <div v-for="stat in statsData" :key="stat.label" class="stat-card">
        <div class="stat-card-inner">
          <div class="stat-header">
            <div class="stat-icon-wrapper" :style="{ background: stat.gradient }">
              <i :class="stat.icon" class="stat-icon-svg" />
            </div>
          </div>
          <div class="stat-body">
            <div class="stat-value">
              {{ stat.value }}
            </div>
            <div class="stat-title">
              {{ stat.label }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="trend-card">
      <div class="card-header">
        <div class="card-title">
          <i class="ai-icon:trending-up" />
          <span>会话趋势</span>
        </div>
      </div>
      <div class="card-body">
        <div ref="trendChartRef" class="chart-container" />
      </div>
    </div>

    <div class="session-table-card">
      <div class="card-header">
        <div class="card-title">
          <i class="ai-icon:message-square" />
          <span>会话列表</span>
        </div>
        <div class="search-bar">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索用户/会话标题"
            clearable
            size="small"
            style="width: 200px"
          >
            <template #prefix>
              <i class="ai-icon:search" />
            </template>
          </n-input>
          <n-date-picker
            v-model:value="dateRange"
            type="daterange"
            size="small"
            clearable
            @update:value="handleDateChange"
          />
        </div>
      </div>

      <n-data-table
        :columns="tableColumns"
        :data="sessionList"
        :row-key="row => row.id"
        :row-props="rowProps"
        :loading="loading"
        size="small"
        class="session-table"
      />
      <div class="pagination-wrap">
        <n-pagination
          v-model:page="pagination.page"
          :page-count="pagination.totalPages"
          size="small"
          @update:page="loadSessionList"
        />
      </div>
    </div>

    <n-modal
      v-model:show="showDetailModal"
      preset="card"
      :title="currentSessionTitle"
      :style="{ maxWidth: '900px', width: '90vw' }"
      :mask-closable="true"
      class="chat-modal"
    >
      <template #header-extra>
        <div class="modal-header-extra">
          <span v-if="currentSession" class="detail-meta">{{ currentSession.nickName }} · {{ currentSession.messageCount || 0 }} 条消息</span>
          <NPopconfirm @positive-click="handleDeleteSession(currentSessionId)">
            <template #trigger>
              <NButton text type="error" size="small" style="margin-left: 12px">
                <i class="ai-icon:trash-2" /> 删除
              </NButton>
            </template>
            确定删除该会话吗？
          </NPopconfirm>
        </div>
      </template>
      <n-scrollbar ref="messageScrollbarRef" class="message-scroll">
        <div class="message-list">
          <div
            v-for="msg in messageList"
            :key="msg.id"
            class="message-item"
            :class="msg.role === 'user' ? 'message-user' : 'message-assistant'"
          >
            <div class="msg-avatar-wrap">
              <div v-if="msg.role === 'user'" class="msg-avatar avatar-user" :style="{ background: getAvatarGradient(currentSession?.nickName) }">
                {{ (currentSession?.nickName || '我').charAt(0) }}
              </div>
              <div v-else class="msg-avatar avatar-ai">
                <i class="i-material-symbols:smart-toy-outline" style="font-size: 18px" />
              </div>
            </div>
            <div class="msg-body">
              <div class="msg-meta">
                <span class="msg-role-name">{{ msg.role === 'user' ? currentSession?.nickName || '用户' : 'AI 助手' }}</span>
                <span class="msg-time">{{ formatRelativeTime(msg.createTime) }}</span>
              </div>
              <div v-if="msg.role === 'user'" class="bubble bubble-user">
                <div class="bubble-content">
                  {{ msg.content }}
                </div>
              </div>
              <div v-else class="bubble bubble-assistant">
                <div class="bubble-content" v-html="renderMarkdown(msg.content)" />
              </div>
            </div>
          </div>
        </div>
      </n-scrollbar>
    </n-modal>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import hljs from 'highlight.js'
import { marked } from 'marked'
import { NButton, NEllipsis, NPopconfirm, NTag } from 'naive-ui'
import { h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { sessionDelete, sessionMessages, sessionPage, sessionStatistics } from '@/api/ai'
import AuthImage from '@/components/common/AuthImage.vue'
import { getEChartsTheme } from '@/utils/echarts-theme'

defineOptions({ name: 'AiSession' })

const searchKeyword = ref('')
const dateRange = ref(null)
const sessionList = ref([])
const currentSessionId = ref(null)
const currentSessionTitle = ref('')
const currentSession = ref(null)
const showDetailModal = ref(false)
const messageList = ref([])
const loading = ref(false)
const pagination = ref({ page: 1, totalPages: 1, total: 0 })
const statsData = ref([
  { label: '会话总数', value: '0', icon: 'ai-icon:message-square', gradient: 'linear-gradient(135deg, #4242F7 0%, #6366F1 100%)' },
  { label: '消息总数', value: '0', icon: 'ai-icon:mail', gradient: 'linear-gradient(135deg, #5AC8FA 0%, #38BDF8 100%)' },
  { label: '今日会话', value: '0', icon: 'ai-icon:clock', gradient: 'linear-gradient(135deg, #6EE7B7 0%, #34D399 100%)' },
  { label: 'Token消耗', value: '0', icon: 'ai-icon:zap', gradient: 'linear-gradient(135deg, #A78BFA 0%, #8B5CF6 100%)' },
])

const trendChartRef = ref(null)
const messageScrollbarRef = ref(null)
let trendChart = null

const avatarGradients = [
  'linear-gradient(135deg, #4242F7 0%, #6366F1 100%)',
  'linear-gradient(135deg, #5AC8FA 0%, #38BDF8 100%)',
  'linear-gradient(135deg, #6EE7B7 0%, #34D399 100%)',
  'linear-gradient(135deg, #A78BFA 0%, #8B5CF6 100%)',
  'linear-gradient(135deg, #F472B6 0%, #EC4899 100%)',
  'linear-gradient(135deg, #FBBF24 0%, #F59E0B 100%)',
]

function getAvatarGradient(name) {
  if (!name)
    return avatarGradients[0]
  const code = name.charCodeAt(0) || 0
  return avatarGradients[code % avatarGradients.length]
}

marked.setOptions({
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang))
      return hljs.highlight(code, { language: lang }).value
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true,
})

function renderMarkdown(content) {
  if (!content)
    return ''
  return marked.parse(content)
}

function formatRelativeTime(time) {
  if (!time)
    return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute)
    return '刚刚'
  if (diff < hour)
    return `${Math.floor(diff / minute)}分钟前`
  if (diff < day)
    return `${Math.floor(diff / hour)}小时前`
  if (diff < 7 * day)
    return `${Math.floor(diff / day)}天前`
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${m}-${d}`
}

const tableColumns = [
  {
    title: '用户',
    key: 'nickName',
    width: 140,
    render(row) {
      return h('div', { style: 'display:flex;align-items:center;gap:8px' }, [
        row.avatar
          ? h(AuthImage, { src: row.avatar, imgStyle: { width: '28px', height: '28px', borderRadius: '50%', objectFit: 'cover' } })
          : h('div', {
              style: `width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;color:#fff;font-size:12px;font-weight:600;background:${getAvatarGradient(row.nickName)}`,
            }, (row.nickName || '?').charAt(0)),
        h('span', { style: 'font-weight:500' }, row.nickName || '未知用户'),
      ])
    },
  },
  {
    title: '会话标题',
    key: 'sessionName',
    ellipsis: { tooltip: true },
    render(row) {
      return h(NEllipsis, { lineClamp: 1 }, { default: () => row.sessionName || '新会话' })
    },
  },
  {
    title: '消息数',
    key: 'messageCount',
    width: 80,
    align: 'center',
    render(row) {
      return h(NTag, { size: 'small', round: true, type: 'info' }, { default: () => `${row.messageCount || 0}` })
    },
  },
  {
    title: 'Token',
    key: 'tokenUsage',
    width: 100,
    align: 'right',
    render(row) {
      const v = row.tokenUsage || 0
      return h('span', { style: 'color:#8b5cf6;font-weight:500' }, v > 0 ? v.toLocaleString() : '-')
    },
  },
  {
    title: '最后活跃',
    key: 'updateTime',
    width: 120,
    render(row) {
      return h('span', { style: 'color:#94a3b8;font-size:13px' }, formatRelativeTime(row.updateTime || row.createTime))
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    fixed: 'right',
    render(row) {
      return h('div', { style: 'display:flex;gap:8px' }, [
        h(NButton, { text: true, type: 'primary', size: 'small', onClick: () => handleSelectSession(row) }, { default: () => '查看详情' }),
        h(NPopconfirm, { onPositiveClick: () => handleDeleteSession(row.id) }, {
          trigger: () => h(NButton, { text: true, type: 'error', size: 'small' }, { default: () => '删除' }),
          default: () => '确定删除该会话吗？',
        }),
      ])
    },
  },
]

function rowProps(row) {
  return {
    style: currentSessionId.value === row.id ? 'background: rgba(66,66,247,0.06); cursor:pointer' : 'cursor:pointer',
    onClick: () => handleSelectSession(row),
  }
}

async function loadStatistics() {
  try {
    const res = await sessionStatistics()
    if (res.code === 200 && res.data) {
      const d = res.data
      statsData.value[0].value = (d.totalSessions ?? 0).toLocaleString()
      statsData.value[1].value = (d.totalMessages ?? 0).toLocaleString()
      statsData.value[2].value = (d.todaySessions ?? 0).toLocaleString()
      statsData.value[3].value = (d.totalTokenUsage ?? 0).toLocaleString()
      initTrendChart(d.dailyTrend || [])
    }
  }
  catch {}
}

async function loadSessionList() {
  loading.value = true
  try {
    const params = { pageNum: pagination.value.page, pageSize: 15 }
    if (searchKeyword.value)
      params.keyword = searchKeyword.value
    if (dateRange.value && dateRange.value[0] && dateRange.value[1]) {
      params.startTime = new Date(dateRange.value[0]).toISOString()
      params.endTime = new Date(dateRange.value[1]).toISOString()
    }
    const res = await sessionPage(params)
    if (res.code === 200 && res.data) {
      sessionList.value = res.data.records || []
      pagination.value.total = res.data.total || 0
      pagination.value.totalPages = Math.ceil((res.data.total || 0) / 15) || 1
    }
  }
  catch {}
  finally {
    loading.value = false
  }
}

async function loadMessages(sessionId) {
  try {
    const res = await sessionMessages(sessionId)
    if (res.code === 200 && res.data) {
      messageList.value = res.data || []
      await nextTick()
      scrollToBottom()
    }
  }
  catch {}
}

function scrollToBottom() {
  if (messageScrollbarRef.value) {
    const el = messageScrollbarRef.value
    if (el && el.scrollTo)
      el.scrollTo({ top: 999999, behavior: 'smooth' })
  }
}

function handleSelectSession(session) {
  currentSessionId.value = session.id
  currentSessionTitle.value = session.sessionName || '新会话'
  currentSession.value = session
  showDetailModal.value = true
  loadMessages(session.id)
}

function closeDetail() {
  showDetailModal.value = false
  currentSessionId.value = null
  currentSessionTitle.value = ''
  currentSession.value = null
  messageList.value = []
}

async function handleDeleteSession(sessionId) {
  try {
    const res = await sessionDelete(sessionId)
    if (res.code === 200) {
      window.$message.success('删除成功')
      if (currentSessionId.value === sessionId)
        closeDetail()
      loadSessionList()
      loadStatistics()
    }
  }
  catch {}
}

function handleDateChange() {
  pagination.value.page = 1
  loadSessionList()
}

watch(searchKeyword, () => {
  pagination.value.page = 1
  loadSessionList()
})

function initTrendChart(dailyTrend) {
  if (!trendChartRef.value)
    return
  if (trendChart)
    trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)
  const theme = getEChartsTheme()
  const trend = dailyTrend || []
  const days = trend.map(d => d.date ? d.date.substring(5) : '')
  const sessionCounts = trend.map(d => d.sessionCount ?? 0)
  const messageCounts = trend.map(d => d.messageCount ?? 0)

  trendChart.setOption({
    ...theme,
    tooltip: { ...theme.tooltip, trigger: 'axis' },
    legend: { ...theme.legend, data: ['会话数', '消息数'] },
    grid: { left: 50, right: 50, bottom: 60, top: 60, containLabel: true },
    xAxis: { ...theme.categoryAxis, type: 'category', data: days, boundaryGap: false },
    yAxis: [
      { ...theme.valueAxis, type: 'value', name: '会话数', position: 'left' },
      { ...theme.valueAxis, type: 'value', name: '消息数', position: 'right' },
    ],
    series: [
      { name: '会话数', type: 'line', smooth: true, showSymbol: false, yAxisIndex: 0, data: sessionCounts, lineStyle: { width: 2 }, areaStyle: { opacity: 0.1 }, itemStyle: { color: theme.color[0] } },
      { name: '消息数', type: 'line', smooth: true, showSymbol: false, yAxisIndex: 1, data: messageCounts, lineStyle: { width: 2 }, areaStyle: { opacity: 0.1 }, itemStyle: { color: theme.color[2] } },
    ],
  })
}

function handleResize() {
  if (trendChart)
    trendChart.resize()
}

onMounted(async () => {
  loadStatistics()
  loadSessionList()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped>
.ai-session-page {
  padding: 24px;
  min-height: 100%;
  overflow-y: auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.06);
}

.stat-card-inner {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 26px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.stat-card:hover .stat-icon-wrapper {
  transform: scale(1.08) rotate(3deg);
}

.stat-icon-svg {
  font-size: 26px;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.stat-title {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.trend-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  overflow: visible;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.trend-card .card-header {
  border-radius: 20px 20px 0 0;
}

.session-table-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.5) 0%, transparent 100%);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.card-title i {
  font-size: 20px;
  color: #4242f7;
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.card-body {
  padding: 24px;
}

.chart-container {
  height: 320px;
  width: 100%;
}

.session-table {
  margin: 0;
}

.session-table :deep(.n-data-table-td) {
  padding: 10px 16px;
}

.session-table :deep(.n-data-table-row:hover) {
  background: rgba(66, 66, 247, 0.04) !important;
}

.pagination-wrap {
  padding: 12px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid rgba(226, 232, 240, 0.5);
}

.message-scroll {
  max-height: 60vh;
  overflow: hidden;
}

.message-list {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 10px;
}

.message-user {
  justify-content: flex-start;
}

.message-assistant {
  justify-content: flex-start;
}

.msg-avatar-wrap {
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 2px;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 13px;
  font-weight: 600;
}

.avatar-ai {
  background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.msg-body {
  max-width: 80%;
  min-width: 0;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.msg-role-name {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.msg-time {
  font-size: 11px;
  color: #94a3b8;
}

.bubble {
  border-radius: 16px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.bubble-user {
  background: linear-gradient(135deg, #4242f7 0%, #6366f1 100%);
  color: #ffffff;
  border-bottom-right-radius: 4px;
  display: inline-block;
}

.bubble-assistant {
  background: #f1f5f9;
  color: #334155;
  border-bottom-left-radius: 4px;
}

.bubble-content :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px;
  overflow-x: auto;
  margin: 8px 0;
  font-size: 13px;
  line-height: 1.5;
}

.bubble-content :deep(pre code) {
  background: transparent;
  padding: 0;
  font-size: 13px;
}

.bubble-content :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.bubble-content :deep(p) {
  margin: 0 0 8px 0;
}

.bubble-content :deep(p:last-child) {
  margin-bottom: 0;
}

.bubble-content :deep(ul),
.bubble-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.bubble-content :deep(blockquote) {
  margin: 8px 0;
  padding: 8px 16px;
  border-left: 3px solid #6366f1;
  background: rgba(99, 102, 241, 0.06);
  border-radius: 0 8px 8px 0;
}

.bubble-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.bubble-content :deep(th),
.bubble-content :deep(td) {
  border: 1px solid #e2e8f0;
  padding: 8px 12px;
  text-align: left;
}

.bubble-content :deep(th) {
  background: #f8fafc;
  font-weight: 600;
}

.modal-header-extra {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-meta {
  font-size: 12px;
  color: #94a3b8;
}

.dark .stat-card,
.dark .trend-card,
.dark .session-table-card {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(51, 65, 85, 0.5);
}

.dark .stat-card:hover,
.dark .trend-card:hover {
  border-color: rgba(71, 85, 105, 0.6);
}

.dark .stat-value {
  color: #f1f5f9;
}

.dark .stat-title {
  color: #94a3b8;
}

.dark .card-header {
  background: transparent;
  border-bottom-color: rgba(51, 65, 85, 0.4);
}

.dark .card-title {
  color: #e2e8f0;
}

.dark .card-title i {
  color: #6366f1;
}

.dark .session-table :deep(.n-data-table-row:hover) {
  background: rgba(67, 56, 202, 0.15) !important;
}

.dark .detail-meta,
.dark .msg-role-name,
.dark .msg-time {
  color: #64748b;
}

.dark .bubble-assistant {
  background: #334155;
  color: #e2e8f0;
}

.dark .bubble-content :deep(code) {
  background: rgba(255, 255, 255, 0.1);
}

.dark .bubble-content :deep(blockquote) {
  background: rgba(99, 102, 241, 0.1);
}

.dark .bubble-content :deep(th) {
  background: #1e293b;
}

.dark .bubble-content :deep(th),
.dark .bubble-content :deep(td) {
  border-color: #475569;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .ai-session-page {
    padding: 16px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .stat-card {
    padding: 18px;
  }

  .stat-value {
    font-size: 24px;
  }

  .stat-icon-wrapper {
    width: 48px;
    height: 48px;
  }

  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .search-bar {
    width: 100%;
    flex-wrap: wrap;
  }

  .chart-container {
    height: 280px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stat-card:hover {
    transform: none;
  }

  .stat-icon-wrapper {
    transition: none;
  }
}
</style>
