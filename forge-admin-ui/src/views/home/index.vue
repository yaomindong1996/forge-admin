<template>
  <div class="home-page">
    <!-- 顶部大统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card online" @click="goTo('/system/online')">
        <div class="stat-header">
          <div class="stat-icon">
            <i class="i-material-symbols:person-pin-rounded" />
          </div>
          <div class="stat-trend up">
            <i class="i-material-symbols:trending-up" />
            <span>12%</span>
          </div>
        </div>
        <div class="stat-body">
          <div class="stat-value">
            {{ onlineCount }}
          </div>
          <div class="stat-title">
            在线用户
          </div>
          <div class="stat-desc">
            当前活跃会话数
          </div>
        </div>
      </div>

      <div class="stat-card login" @click="goTo('/system/login-log')">
        <div class="stat-header">
          <div class="stat-icon">
            <i class="i-material-symbols:login-rounded" />
          </div>
          <div class="stat-trend up">
            <i class="i-material-symbols:trending-up" />
            <span>8%</span>
          </div>
        </div>
        <div class="stat-body">
          <div class="stat-value">
            {{ todayLoginCount }}
          </div>
          <div class="stat-title">
            今日登录
          </div>
          <div class="stat-desc">
            较昨日登录增长
          </div>
        </div>
      </div>

      <div class="stat-card user" @click="goTo('/system/user')">
        <div class="stat-header">
          <div class="stat-icon">
            <i class="i-material-symbols:group-rounded" />
          </div>
          <div class="stat-trend up">
            <i class="i-material-symbols:trending-up" />
            <span>5%</span>
          </div>
        </div>
        <div class="stat-body">
          <div class="stat-value">
            {{ totalUserCount }}
          </div>
          <div class="stat-title">
            总用户数
          </div>
          <div class="stat-desc">
            系统注册用户总数
          </div>
        </div>
      </div>

      <div class="stat-card todo" @click="goTo('/flow/todo')">
        <div class="stat-header">
          <div class="stat-icon">
            <i class="i-material-symbols:pending-actions" />
          </div>
          <div v-if="todoCount > 0" class="stat-badge pulse">
            {{ todoCount > 99 ? '99+' : todoCount }}
          </div>
        </div>
        <div class="stat-body">
          <div class="stat-value">
            {{ todoCount }}
          </div>
          <div class="stat-title">
            待办任务
          </div>
          <div class="stat-desc">
            需要处理的审批任务
          </div>
        </div>
      </div>

      <div class="stat-card done" @click="goTo('/flow/done')">
        <div class="stat-header">
          <div class="stat-icon">
            <i class="i-material-symbols:task-alt" />
          </div>
        </div>
        <div class="stat-body">
          <div class="stat-value">
            {{ doneCount }}
          </div>
          <div class="stat-title">
            已办任务
          </div>
          <div class="stat-desc">
            已完成的审批任务
          </div>
        </div>
      </div>

      <div class="stat-card started" @click="goTo('/flow/started')">
        <div class="stat-header">
          <div class="stat-icon">
            <i class="i-material-symbols:send" />
          </div>
          <div v-if="pendingStarted > 0" class="stat-badge">
            {{ pendingStarted }}审批中
          </div>
        </div>
        <div class="stat-body">
          <div class="stat-value">
            {{ startedCount }}
          </div>
          <div class="stat-title">
            发起的流程
          </div>
          <div class="stat-desc">
            我发起的流程实例
          </div>
        </div>
      </div>
    </div>

    <!-- 中间内容区域 -->
    <div class="content-grid">
      <!-- 左侧：待办任务大列表 -->
      <div class="todo-panel">
        <div class="panel-header">
          <div class="panel-title">
            <i class="i-material-symbols:pending-actions" />
            <span>待办任务</span>
            <n-badge v-if="todoCount > 0" :value="todoCount" :max="99" type="warning" />
          </div>
          <n-button text type="primary" @click="goTo('/flow/todo')">
            查看全部 <i class="i-material-symbols:arrow-right ml-4" />
          </n-button>
        </div>
        <n-spin :show="todoLoading">
          <div v-if="todoList.length === 0" class="empty-state">
            <i class="i-material-symbols:check-circle-outline empty-icon" />
            <div class="empty-text">
              暂无待办任务
            </div>
            <div class="empty-desc">
              所有审批任务已处理完毕
            </div>
          </div>
          <n-scrollbar v-else style="max-height: 320px">
            <div class="todo-list">
              <div
                v-for="task in todoList"
                :key="task.id"
                class="todo-item"
                @click="goTo(`/flow/todo?id=${task.id}`)"
              >
                <div class="todo-left">
                  <span v-if="task.priority >= 2" class="priority-tag" :class="getPriorityClass(task.priority)">
                    {{ getPriorityText(task.priority) }}
                  </span>
                  <div class="todo-content-item">
                    <div class="todo-title">
                      {{ task.title || task.processTitle }}
                    </div>
                    <div class="todo-meta">
                      <span class="todo-node">{{ task.taskName }}</span>
                      <span class="todo-starter">{{ task.startUserName }}发起</span>
                    </div>
                  </div>
                </div>
                <div class="todo-right">
                  <div class="todo-time">
                    {{ formatTime(task.createTime) }}
                  </div>
                  <i class="i-material-symbols:chevron-right todo-arrow" />
                </div>
              </div>
            </div>
          </n-scrollbar>
        </n-spin>
      </div>

      <!-- 右侧：通知公告 + 快捷入口 -->
      <div class="right-panel">
        <!-- 通知公告 -->
        <div class="notice-panel">
          <div class="panel-header">
            <div class="panel-title">
              <i class="i-material-symbols:notifications-active" />
              <span>通知公告</span>
              <n-badge v-if="unreadNotice > 0" :value="unreadNotice" :max="99" type="info" />
            </div>
            <n-button text type="primary" @click="goTo('/system/notice-list')">
              更多 <i class="i-material-symbols:arrow-right ml-4" />
            </n-button>
          </div>
          <n-scrollbar style="max-height: 280px">
            <div v-if="noticeList.length === 0" class="empty-state small">
              <i class="i-material-symbols:inbox empty-icon" />
              <div class="empty-text">
                暂无公告
              </div>
            </div>
            <div v-else class="notice-list">
              <div
                v-for="notice in noticeList"
                :key="notice.noticeId"
                class="notice-item"
                :class="{ unread: notice.isRead === 0 }"
                @click="openNotice(notice)"
              >
                <div class="notice-left">
                  <span v-if="notice.isRead === 0" class="unread-dot" />
                  <div class="notice-content">
                    <div class="notice-title">
                      {{ notice.noticeTitle }}
                    </div>
                    <div class="notice-meta">
                      <n-tag :type="getNoticeTypeColor(notice.noticeType)" size="small">
                        {{ getNoticeTypeText(notice.noticeType) }}
                      </n-tag>
                    </div>
                  </div>
                </div>
                <div class="notice-time">
                  {{ formatTime(notice.publishTime) }}
                </div>
              </div>
            </div>
          </n-scrollbar>
        </div>

        <!-- 快捷入口 -->
        <div class="quick-panel">
          <div class="panel-header">
            <div class="panel-title">
              <i class="i-material-symbols:grid-view-rounded" />
              <span>快捷入口</span>
            </div>
          </div>
          <div class="quick-grid">
            <div v-for="item in quickLinks" :key="item.path" class="quick-item" @click="goTo(item.path)">
              <div class="quick-icon" :style="{ background: item.gradient }">
                <i :class="item.icon" />
              </div>
              <div class="quick-title">
                {{ item.title }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部假统计图表 -->
    <div class="charts-grid">
      <div class="chart-card">
        <div class="panel-header">
          <div class="panel-title">
            <i class="i-material-symbols:bar-chart-rounded" />
            <span>访问量趋势</span>
          </div>
          <n-button text @click="refreshVisitChart">
            <i class="i-material-symbols:refresh" />
          </n-button>
        </div>
        <div ref="visitChartRef" class="chart-container" />
      </div>

      <div class="chart-card">
        <div class="panel-header">
          <div class="panel-title">
            <i class="i-material-symbols:trending-up-rounded" />
            <span>用户增长统计</span>
          </div>
          <n-button text @click="refreshUserChart">
            <i class="i-material-symbols:refresh" />
          </n-button>
        </div>
        <div ref="userChartRef" class="chart-container" />
      </div>
    </div>

    <!-- 通知详情弹窗 -->
    <n-modal v-model:show="showNoticeModal" preset="card" title="公告详情" style="width: 800px">
      <div v-if="currentNotice" class="notice-detail">
        <h3 class="detail-title">
          {{ currentNotice.noticeTitle }}
        </h3>
        <div class="detail-meta">
          <n-tag :type="getNoticeTypeColor(currentNotice.noticeType)" size="small">
            {{ getNoticeTypeText(currentNotice.noticeType) }}
          </n-tag>
          <span class="detail-time">发布时间：{{ currentNotice.publishTime }}</span>
        </div>
        <n-divider />
        <div class="detail-content" v-html="currentNotice.noticeContent" />
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import flowApi from '@/api/flow'
import { useUserStore } from '@/store'
import { request } from '@/utils'

const router = useRouter()
const userStore = useUserStore()

// 用户统计
const onlineCount = ref(0)
const todayLoginCount = ref(0)
const totalUserCount = ref(0)

// 流程统计
const todoCount = ref(0)
const doneCount = ref(0)
const startedCount = ref(0)
const pendingStarted = ref(0)

// 通知统计
const unreadNotice = ref(0)

// 列表数据
const todoLoading = ref(false)
const todoList = ref([])
const noticeList = ref([])

// 弹窗
const showNoticeModal = ref(false)
const currentNotice = ref(null)

// 图表
const visitChartRef = ref(null)
const userChartRef = ref(null)

// 快捷入口
const quickLinks = [
  { title: '发起流程', icon: 'i-material-symbols:add-circle-rounded', gradient: 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)', path: '/flow/template' },
  { title: '我的待办', icon: 'i-material-symbols:pending-actions', gradient: 'linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%)', path: '/flow/todo' },
  { title: '用户管理', icon: 'i-material-symbols:person-rounded', gradient: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)', path: '/system/user' },
  { title: '角色管理', icon: 'i-material-symbols:admin-panel-settings-rounded', gradient: 'linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)', path: '/system/role' },
  { title: '文件管理', icon: 'i-material-symbols:folder-open-rounded', gradient: 'linear-gradient(135deg, #06b6d4 0%, #0891b2 100%)', path: '/system/file' },
  { title: '配置中心', icon: 'i-material-symbols:settings-suggest', gradient: 'linear-gradient(135deg, #ec4899 0%, #be185d 100%)', path: '/system/config-center' },
]

// 加载用户统计
async function loadUserStats() {
  try {
    // 并行请求所有统计数据
    const [onlineRes, userRes] = await Promise.all([
      request.get('/auth/online/page', { params: { pageNum: 1, pageSize: 1 } }),
      request.get('/system/user/page', { params: { pageNum: 1, pageSize: 1 } }),
    ])

    onlineCount.value = onlineRes.data?.total || 0
    totalUserCount.value = userRes.data?.total || 0

    // 今日登录数
    const today = new Date().toISOString().split('T')[0]
    const loginRes = await request.get('/system/loginLog/page', {
      params: { pageNum: 1, pageSize: 1, startTime: today, endTime: today },
    })
    todayLoginCount.value = loginRes.data?.total || 0
  }
  catch {
    console.error('加载用户统计失败')
  }
}

// 加载流程统计和列表
async function loadFlowData() {
  if (!userStore.userId) {
    console.warn('用户ID未初始化，跳过加载流程数据')
    return
  }
  try {
    const [todoRes, doneRes, startedRes] = await Promise.all([
      flowApi.getTodoTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
      flowApi.getDoneTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
      flowApi.getStartedTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
    ])

    todoCount.value = todoRes.data?.total || 0
    doneCount.value = doneRes.data?.total || 0
    startedCount.value = startedRes.data?.total || 0

    // 统计审批中的流程
    if (startedRes.data?.records) {
      pendingStarted.value = startedRes.data.records.filter(r => r.status === 1).length
    }
  }
  catch {
    console.error('加载流程统计失败')
  }
}

// 加载待办列表
async function loadTodoList() {
  if (!userStore.userId) {
    console.warn('用户ID未初始化，跳过加载待办列表')
    return
  }
  todoLoading.value = true
  try {
    const res = await flowApi.getTodoTasks({ pageNum: 1, pageSize: 8, userId: userStore.userId })
    if (res.data) {
      todoList.value = res.data.records || []
    }
  }
  catch {
    console.error('加载待办列表失败')
  }
  finally {
    todoLoading.value = false
  }
}

// 加载通知公告
async function loadNoticeList() {
  try {
    const res = await request.get('/system/notice/user/page', { params: { pageNum: 1, pageSize: 10 } })
    if (res.data) {
      noticeList.value = res.data.records || []
      unreadNotice.value = noticeList.value.filter(n => n.isRead === 0).length
    }
  }
  catch {
    console.error('加载通知公告失败')
  }
}

// 跳转
function goTo(path) {
  router.push(path)
}

// 打开通知
async function openNotice(notice) {
  try {
    const res = await request.post('/system/notice/getById', null, { params: { noticeId: notice.noticeId } })
    if (res.data) {
      currentNotice.value = res.data
      showNoticeModal.value = true

      if (notice.isRead === 0) {
        await request.post('/system/notice/markAsRead', null, { params: { noticeId: notice.noticeId } })
        loadNoticeList()
      }
    }
  }
  catch {
    window.$message.error('获取详情失败')
  }
}

// 格式化时间
function formatTime(time) {
  if (!time)
    return '-'
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
  return time.split(' ')[0]
}

// 优先级
function getPriorityClass(p) {
  if (p >= 3)
    return 'urgent'
  if (p === 2)
    return 'high'
  return ''
}

function getPriorityText(p) {
  const m = { 0: '低', 1: '普通', 2: '高', 3: '紧急' }
  return m[p] || '普通'
}

// 通知类型
function getNoticeTypeText(type) {
  const typeMap = { NOTICE: '通知', ANNOUNCEMENT: '公告', NEWS: '新闻' }
  return typeMap[type] || type
}

function getNoticeTypeColor(type) {
  const colorMap = { NOTICE: 'info', ANNOUNCEMENT: 'warning', NEWS: 'success' }
  return colorMap[type] || 'default'
}

// 初始化访问量趋势图（假数据）
function initVisitChart() {
  const chart = echarts.init(visitChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#0f172a' },
      padding: [12, 16],
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#64748b', fontSize: 12 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#64748b', fontSize: 12 },
    },
    series: [
      {
        name: '访问量',
        type: 'bar',
        barWidth: '50%',
        data: [320, 502, 301, 434, 590, 530, 420],
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#6366f1' },
            { offset: 1, color: '#4f46e5' },
          ]),
        },
      },
    ],
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

// 初始化用户增长图（假数据）
function initUserChart() {
  const chart = echarts.init(userChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#0f172a' },
      padding: [12, 16],
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月'],
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#64748b', fontSize: 12 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#64748b', fontSize: 12 },
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: [820, 932, 901, 934, 1290, 1330],
        lineStyle: {
          width: 3,
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#34d399' },
            { offset: 1, color: '#10b981' },
          ]),
        },
        itemStyle: {
          color: '#10b981',
          borderColor: '#fff',
          borderWidth: 2,
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(110, 231, 183, 0.25)' },
            { offset: 1, color: 'rgba(52, 211, 153, 0.02)' },
          ]),
        },
      },
    ],
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

// 刷新图表
function refreshVisitChart() {
  initVisitChart()
}

function refreshUserChart() {
  initUserChart()
}

onMounted(() => {
  loadUserStats()
  loadFlowData()
  loadTodoList()
  loadNoticeList()

  nextTick(() => {
    initVisitChart()
    initUserChart()
  })
})
</script>

<style scoped>
.home-page {
  padding: 20px;
  min-height: 100%;
  padding-bottom: 60px;
  background: #f8fafc;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e2e8f0;
  position: relative;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-card.online .stat-icon {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}
.stat-card.login .stat-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}
.stat-card.user .stat-icon {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
}
.stat-card.todo .stat-icon {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
}
.stat-card.done .stat-icon {
  background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
}
.stat-card.started .stat-icon {
  background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
}

.stat-trend.up {
  color: #059669;
  background: #d1fae5;
}

.stat-badge {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: #fff;
  font-size: 11px;
  padding: 4px 12px;
  border-radius: 12px;
  font-weight: 600;
}

.stat-badge.pulse {
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.stat-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.stat-desc {
  font-size: 12px;
  color: #64748b;
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
  align-items: stretch;
  min-height: 400px;
}

.todo-panel {
  display: flex;
  flex-direction: column;
}

/* 面板通用样式 */
.todo-panel,
.notice-panel,
.quick-panel,
.chart-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.todo-panel .n-spin {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.todo-panel .n-spin > :deep(div) {
  flex: 1;
  min-height: 0;
}

.todo-panel .n-scrollbar {
  height: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.panel-title i {
  font-size: 20px;
  color: #6366f1;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #94a3b8;
}

.empty-state.small {
  padding: 24px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.3;
}

.empty-text {
  font-size: 14px;
  font-weight: 500;
}

.empty-desc {
  font-size: 12px;
  margin-top: 4px;
}

/* 待办任务列表 */
.todo-list {
  padding: 12px;
}

.todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: #f8fafc;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.todo-item:hover {
  background: #eef2ff;
  transform: translateX(4px);
}

.todo-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.priority-tag {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 600;
  white-space: nowrap;
}

.priority-tag.urgent {
  background: #fee2e2;
  color: #dc2626;
}

.priority-tag.high {
  background: #fef3c7;
  color: #d97706;
}

.todo-content-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.todo-title {
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.todo-node {
  background: #e2e8f0;
  padding: 2px 8px;
  border-radius: 4px;
}

.todo-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.todo-time {
  font-size: 12px;
  color: #94a3b8;
}

.todo-arrow {
  font-size: 16px;
  color: #cbd5e1;
}

/* 右侧面板 */
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 通知公告列表 */
.notice-list {
  padding: 12px;
}

.notice-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: #f8fafc;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.notice-item:hover {
  background: #eef2ff;
}

.notice-item.unread {
  background: #fef3c7;
}

.notice-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f59e0b;
}

.notice-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.notice-title {
  font-size: 14px;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-item.unread .notice-title {
  font-weight: 600;
}

.notice-meta {
  display: flex;
  gap: 8px;
}

.notice-time {
  font-size: 12px;
  color: #94a3b8;
}

/* 快捷入口网格 */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 16px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px 12px;
  background: #f8fafc;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-item:hover {
  background: #eef2ff;
  transform: translateY(-2px);
}

.quick-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
}

.quick-title {
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

/* 图表网格 */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-container {
  height: 280px;
  padding: 16px;
}

/* 弹窗样式 */
.notice-detail {
  padding: 8px 0;
}

.detail-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px 0;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-time {
  font-size: 12px;
  color: #64748b;
}

.detail-content {
  line-height: 1.8;
  font-size: 14px;
  color: #475569;
}

.detail-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .content-grid {
    grid-template-columns: 1.5fr 1fr;
  }
}

@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 12px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 22px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }

  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-container {
    height: 220px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }
}

/* 深色模式 */
.dark .home-page {
  background: #0f172a;
}

.dark .stat-card,
.dark .todo-panel,
.dark .notice-panel,
.dark .quick-panel,
.dark .chart-card {
  background: #1e293b;
  border-color: #334155;
}

.dark .stat-value,
.dark .panel-title,
.dark .todo-title,
.dark .notice-title,
.dark .detail-title {
  color: #f1f5f9;
}

.dark .stat-title {
  color: #cbd5e1;
}

.dark .stat-desc,
.dark .todo-meta,
.dark .todo-time,
.dark .notice-meta,
.dark .notice-time,
.dark .quick-title,
.dark .detail-time {
  color: #94a3b8;
}

.dark .todo-item,
.dark .notice-item,
.dark .quick-item {
  background: #334155;
}

.dark .todo-item:hover,
.dark .notice-item:hover {
  background: #475569;
}

.dark .notice-item.unread {
  background: #422006;
}

.dark .todo-node {
  background: #475569;
}

.dark .priority-tag.urgent {
  background: #7f1d1d;
  color: #fca5a5;
}

.dark .priority-tag.high {
  background: #78350f;
  color: #fcd34d;
}

.dark .detail-content {
  color: #cbd5e1;
}
</style>
