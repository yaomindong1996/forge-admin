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

    <section class="community-support-panel">
      <div class="community-support-copy">
        <div class="community-eyebrow">
          <i class="i-material-symbols:forum-rounded" />
          <span>ForgeAdmin 社区</span>
        </div>
        <h2>把复杂后台，聊成可落地的方案</h2>
        <p>
          低代码配置、流程审批、插件扩展、业务二开，很多问题不是一句文档能讲清楚。添加维护者微信，把你的场景、截图和想法发过来，我们一起把路径理顺。
        </p>
        <div class="community-support-highlights">
          <span>问题排查</span>
          <span>二开建议</span>
          <span>版本动态</span>
        </div>
      </div>
      <div class="community-qr-grid">
        <article class="community-qr-card primary">
          <div class="community-qr-card-header">
            <span>添加维护者微信</span>
            <em>交流咨询</em>
          </div>
          <div class="community-qr-duo">
            <div class="community-qr-person">
              <div class="community-qr-preview">
                <n-image
                  class="community-qr-image"
                  :src="wechatGroupQr"
                  :preview-src="wechatGroupQr"
                  object-fit="contain"
                  alt="ForgeAdmin 维护者微信二维码"
                />
                <span class="community-qr-zoom">点击放大</span>
              </div>
              <span>维护者 A</span>
            </div>
            <div class="community-qr-person">
              <div class="community-qr-preview">
                <n-image
                  class="community-qr-image"
                  :src="wechatGroupQrAlt"
                  :preview-src="wechatGroupQrAlt"
                  object-fit="contain"
                  alt="ForgeAdmin 维护者微信二维码"
                />
                <span class="community-qr-zoom">点击放大</span>
              </div>
              <span>维护者 B</span>
            </div>
          </div>
          <p>扫码添加任一维护者，备注 ForgeAdmin，方便确认来源并拉你进入后续交流。</p>
        </article>
        <article class="community-qr-card support">
          <div class="community-qr-card-header">
            <span>支持维护</span>
            <em>随心支持</em>
          </div>
          <div class="community-qr-preview">
            <n-image
              class="community-qr-image"
              :src="wechatSupportQr"
              :preview-src="wechatSupportQr"
              object-fit="contain"
              alt="ForgeAdmin 维护支持二维码"
            />
            <span class="community-qr-zoom">点击放大</span>
          </div>
          <p>如果这个项目帮你节省了时间，可以请维护者喝杯咖啡，支持持续优化。</p>
        </article>
      </div>
    </section>

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
          <n-button text class="panel-link" @click="goTo('/flow/todo')">
            全部
            <i class="i-material-symbols:chevron-right" />
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
              <article
                v-for="task in todoList"
                :key="task.taskId || task.id"
                class="todo-item"
                @click="openTodoTask(task)"
              >
                <div class="todo-status" :class="task.status === 0 && !task.assignee ? 'candidate' : 'active'">
                  {{ task.status === 0 && !task.assignee ? '待签收' : '待处理' }}
                </div>
                <div class="todo-main">
                  <div class="todo-title-row">
                    <span class="todo-title">{{ task.title || task.processTitle || task.taskName || '-' }}</span>
                    <span v-if="task.priority >= 2" class="priority-tag" :class="getPriorityClass(task.priority)">
                      {{ getPriorityText(task.priority) }}
                    </span>
                  </div>
                  <div class="todo-meta">
                    <span><span class="todo-meta-label">节点</span>{{ task.taskName || '-' }}</span>
                    <span><span class="todo-meta-label">发起人</span>{{ task.startUserName || '-' }}</span>
                    <span v-if="task.startDeptName"><span class="todo-meta-label">部门</span>{{ task.startDeptName }}</span>
                  </div>
                </div>
                <div class="todo-side">
                  <span class="todo-time">{{ formatTime(task.createTime) }}</span>
                  <button type="button" class="todo-action" @click.stop="openTodoTask(task)">
                    <span>处理</span>
                    <i class="i-material-symbols:chevron-right" />
                  </button>
                </div>
              </article>
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
import wechatGroupQrAlt from '@/assets/images/forge-wechat-group1.png'
import wechatGroupQr from '@/assets/images/forge-wechat-group.png'
import wechatSupportQr from '@/assets/images/forge-wechat-support.png'
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
  { title: '发起流程', icon: 'i-material-symbols:add-circle-rounded', gradient: 'linear-gradient(135deg, #eef2ff 0%, #dbeafe 100%)', path: '/flow/template' },
  { title: '我的待办', icon: 'i-material-symbols:pending-actions', gradient: 'linear-gradient(135deg, #fff7ed 0%, #fef3c7 100%)', path: '/flow/todo' },
  { title: '用户管理', icon: 'i-material-symbols:person-rounded', gradient: 'linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%)', path: '/system/user' },
  { title: '角色管理', icon: 'i-material-symbols:admin-panel-settings-rounded', gradient: 'linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%)', path: '/system/role' },
  { title: '文件管理', icon: 'i-material-symbols:folder-open-rounded', gradient: 'linear-gradient(135deg, #ecfeff 0%, #cffafe 100%)', path: '/system/file' },
  { title: '配置中心', icon: 'i-material-symbols:settings-suggest', gradient: 'linear-gradient(135deg, #fdf2f8 0%, #fce7f3 100%)', path: '/system/config-center' },
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
    const silentConfig = { needTip: false }
    const [todoRes, doneRes, startedRes] = await Promise.all([
      flowApi.getTodoTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }, silentConfig),
      flowApi.getDoneTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }, silentConfig),
      flowApi.getStartedTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }, silentConfig),
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
    todoCount.value = 0
    doneCount.value = 0
    startedCount.value = 0
    pendingStarted.value = 0
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
    const res = await flowApi.getTodoTasks(
      { pageNum: 1, pageSize: 8, userId: userStore.userId },
      { needTip: false },
    )
    if (res.data) {
      todoList.value = res.data.records || []
    }
  }
  catch {
    todoList.value = []
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

function openTodoTask(task) {
  const taskId = task.taskId || task.id
  if (!taskId) {
    goTo('/flow/todo')
    return
  }
  router.push({
    path: '/flow/todo',
    query: {
      taskId,
      source: 'home',
      t: Date.now(),
    },
  })
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
            { offset: 0, color: '#60a5fa' },
            { offset: 1, color: '#93c5fd' },
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
            { offset: 0, color: '#2dd4bf' },
            { offset: 1, color: '#60a5fa' },
          ]),
        },
        itemStyle: {
          color: '#2dd4bf',
          borderColor: '#fff',
          borderWidth: 2,
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(45, 212, 191, 0.16)' },
            { offset: 1, color: 'rgba(96, 165, 250, 0.02)' },
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
  height: 100%;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  margin-bottom: 10px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px 14px;
  cursor: pointer;
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
  border: 1px solid #e8edf5;
  position: relative;
  overflow: hidden;
  animation: cardRise 0.56s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.stat-card:nth-child(1) {
  animation-delay: 0.02s;
}

.stat-card:nth-child(2) {
  animation-delay: 0.08s;
}

.stat-card:nth-child(3) {
  animation-delay: 0.14s;
}

.stat-card:nth-child(4) {
  animation-delay: 0.2s;
}

.stat-card:nth-child(5) {
  animation-delay: 0.26s;
}

.stat-card:nth-child(6) {
  animation-delay: 0.32s;
}

.stat-card::before {
  content: none;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(30, 41, 59, 0.06);
  border-color: #cbdaf5;
}

.stat-card:hover::before {
  left: 122%;
  opacity: 1;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #2563eb;
  box-shadow: none;
  transition:
    transform 0.26s ease,
    box-shadow 0.26s ease;
}

.stat-card:hover .stat-icon {
  transform: translateY(-1px);
  box-shadow: none;
}

.stat-card.online .stat-icon {
  background: #ecfdf5;
  color: #059669;
}
.stat-card.login .stat-icon {
  background: #eff6ff;
  color: #2563eb;
}
.stat-card.user .stat-icon {
  background: #eef2ff;
  color: #4f46e5;
}
.stat-card.todo .stat-icon {
  background: #fffbeb;
  color: #d97706;
}
.stat-card.done .stat-icon {
  background: #f0fdf4;
  color: #16a34a;
}
.stat-card.started .stat-icon {
  background: #f5f3ff;
  color: #7c3aed;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 999px;
}

.stat-trend.up {
  color: #059669;
  background: #ecfdf5;
}

.stat-badge {
  background: #fff1f2;
  color: #e11d48;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
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
  gap: 2px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  line-height: 30px;
}

.stat-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.stat-desc {
  font-size: 12px;
  color: #64748b;
}

/* 社区与维护支持 */
.community-support-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(520px, 680px);
  gap: 20px;
  align-items: stretch;
  margin-bottom: 10px;
  padding: 18px;
  border: 1px solid #dbe7f3;
  border-radius: 8px;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.96) 0%,
    rgba(248, 251, 255, 0.96) 48%,
    rgba(240, 253, 250, 0.92) 100%
  );
  box-shadow: 0 8px 20px rgba(30, 41, 59, 0.04);
}

.community-support-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  padding: 8px 10px;
}

.community-eyebrow {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  padding: 5px 9px;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.community-eyebrow i {
  font-size: 15px;
}

.community-support-copy h2 {
  margin: 0;
  color: #0f172a;
  font-size: 26px;
  font-weight: 800;
  line-height: 34px;
}

.community-support-copy p {
  max-width: 620px;
  margin: 10px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 24px;
}

.community-support-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.community-support-highlights span {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border: 1px solid #bae6fd;
  border-radius: 999px;
  background: linear-gradient(135deg, #eff6ff, #f0fdfa);
  color: #0369a1;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.community-qr-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  min-width: 0;
}

.community-qr-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e3edf8;
  border-radius: 8px;
  background: #fff;
}

.community-qr-card.primary {
  border-color: #bfdbfe;
  background: #fbfdff;
}

.community-qr-card.support {
  border-color: #bbf7d0;
  background: #fbfffd;
}

.community-qr-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.community-qr-card-header em {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-style: normal;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.community-qr-card.support .community-qr-card-header em {
  background: #ecfdf5;
  color: #059669;
}

.community-qr-preview {
  position: relative;
  overflow: hidden;
  border: 1px solid #edf2f7;
  border-radius: 6px;
  background: #fff;
}

.community-qr-duo {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.community-qr-person {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 7px;
}

.community-qr-person > span {
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}

.community-qr-image {
  width: 100%;
  height: 260px;
  cursor: zoom-in;
}

.community-qr-image :deep(img) {
  width: 100%;
  height: 260px;
  object-fit: contain;
}

.community-qr-zoom {
  position: absolute;
  right: 8px;
  bottom: 8px;
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.68);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  pointer-events: none;
}

.community-qr-card p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 20px;
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
  align-items: stretch;
  min-height: 360px;
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
  border-radius: 8px;
  border: 1px solid #e8edf5;
  overflow: hidden;
  box-shadow: none;
  animation: panelRise 0.58s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.todo-panel {
  animation-delay: 0.18s;
}

.notice-panel {
  animation-delay: 0.24s;
}

.quick-panel {
  animation-delay: 0.3s;
}

.chart-card:nth-child(1) {
  animation-delay: 0.36s;
}

.chart-card:nth-child(2) {
  animation-delay: 0.42s;
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
  padding: 10px 14px;
  border-bottom: 1px solid #edf1f7;
  background: #fbfcfe;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.panel-title i {
  font-size: 18px;
  color: #3b82f6;
}

.panel-link {
  color: #177c7d;
  font-size: 13px;
  font-weight: 600;
}

.panel-link :deep(.n-button__content) {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px 16px;
  color: #94a3b8;
}

.empty-state.small {
  padding: 24px;
}

.empty-icon {
  font-size: 38px;
  margin-bottom: 8px;
  opacity: 0.24;
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
  padding: 4px 8px 8px;
}

.todo-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 58px;
  padding: 9px 10px;
  border-bottom: 1px solid #edf2f7;
  background: #fff;
  cursor: pointer;
  transition:
    background 0.18s ease,
    border-color 0.18s ease;
}

.todo-item:last-child {
  border-bottom: 0;
}

.todo-item:hover {
  background: #f7fbfb;
}

.todo-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: 0 8px;
  border: 1px solid #d7eeee;
  border-radius: 4px;
  background: #f5fbfb;
  color: #177c7d;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.todo-status.candidate {
  border-color: #dbeafe;
  background: #eff6ff;
  color: #2563eb;
}

.todo-status.active {
  border-color: #d7eeee;
  background: #f5fbfb;
  color: #177c7d;
}

.priority-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  font-size: 11px;
  padding: 0 7px;
  border-radius: 4px;
  font-weight: 700;
  white-space: nowrap;
}

.priority-tag.urgent {
  background: #fff1f2;
  color: #be123c;
}

.priority-tag.high {
  background: #fff7ed;
  color: #c2410c;
}

.todo-main {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.todo-title-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.todo-title {
  min-width: 0;
  overflow: hidden;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  min-width: 0;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.todo-meta > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-meta-label {
  margin-right: 4px;
  color: #64748b;
  font-weight: 600;
}

.todo-side {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 112px;
}

.todo-time {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}

.todo-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 30px;
  padding: 0 3px 0 8px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #177c7d;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  transition:
    background-color 150ms ease,
    color 150ms ease;
}

.todo-action:hover {
  background: #effafa;
  color: #0f5f63;
}

.todo-action i {
  font-size: 18px;
  line-height: 1;
}

/* 右侧面板 */
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 通知公告列表 */
.notice-list {
  padding: 8px;
}

.notice-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 12px;
  margin-bottom: 6px;
  background: #fbfcfe;
  border: 1px solid #edf1f7;
  border-radius: 6px;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.notice-item:hover {
  background: #f8fbff;
  transform: translateX(2px);
  box-shadow: none;
}

.notice-item.unread {
  background: #fffbeb;
  border-color: #fde68a;
}

.notice-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.unread-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59e0b;
}

.notice-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.notice-title {
  font-size: 13px;
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
  gap: 8px;
  padding: 10px;
}

.quick-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 9px 10px;
  background: #fbfcfe;
  border: 1px solid #edf1f7;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition:
    transform 0.22s ease,
    background 0.22s ease,
    box-shadow 0.22s ease;
}

.quick-item::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(14, 165, 233, 0));
  opacity: 0;
  transition: opacity 0.22s ease;
  pointer-events: none;
}

.quick-item:hover {
  background: #f8fbff;
  transform: translateY(-1px);
  box-shadow: none;
}

.quick-item:hover::before {
  opacity: 1;
}

.quick-icon {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  color: #2563eb;
  position: relative;
  z-index: 1;
  box-shadow: none;
  transition: transform 0.24s ease;
}

.quick-item:nth-child(2) .quick-icon {
  color: #d97706;
}

.quick-item:nth-child(3) .quick-icon {
  color: #2563eb;
}

.quick-item:nth-child(4) .quick-icon {
  color: #7c3aed;
}

.quick-item:nth-child(5) .quick-icon {
  color: #0891b2;
}

.quick-item:nth-child(6) .quick-icon {
  color: #be185d;
}

.quick-item:hover .quick-icon {
  transform: translateY(-2px) scale(1.06);
}

.quick-title {
  font-size: 12px;
  font-weight: 500;
  color: #334155;
  position: relative;
  z-index: 1;
  white-space: nowrap;
}

/* 图表网格 */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.chart-container {
  height: 240px;
  padding: 10px;
}

@keyframes cardRise {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes panelRise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
  .community-support-panel {
    grid-template-columns: 1fr;
  }

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

  .community-support-panel {
    padding: 12px;
  }

  .community-support-copy {
    padding: 0;
  }

  .community-support-copy h2 {
    font-size: 19px;
    line-height: 27px;
  }

  .community-qr-grid {
    grid-template-columns: 1fr;
  }

  .community-qr-duo {
    grid-template-columns: 1fr;
  }

  .community-qr-image {
    height: 320px;
  }

  .community-qr-image :deep(img) {
    height: 320px;
  }

  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .todo-item {
    grid-template-columns: auto minmax(0, 1fr);
    row-gap: 8px;
  }

  .todo-side {
    grid-column: 1 / -1;
    min-width: 0;
    justify-content: space-between;
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
  background:
    radial-gradient(circle at 8% 4%, rgba(99, 102, 241, 0.18) 0%, transparent 26%),
    radial-gradient(circle at 92% 12%, rgba(14, 165, 233, 0.14) 0%, transparent 24%), #0f172a;
}

.dark .stat-card,
.dark .community-support-panel,
.dark .todo-panel,
.dark .notice-panel,
.dark .quick-panel,
.dark .chart-card {
  background: #1e293b;
  border-color: #334155;
}

.dark .stat-value,
.dark .community-support-copy h2,
.dark .community-qr-card-header,
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
.dark .community-support-copy p,
.dark .community-qr-card p,
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
.dark .quick-item,
.dark .community-qr-card {
  background: #334155;
}

.dark .community-support-panel {
  background: #1e293b;
}

.dark .community-eyebrow {
  border-color: #1d4ed8;
  background: #172554;
  color: #bfdbfe;
}

.dark .community-qr-card.primary {
  border-color: #1d4ed8;
}

.dark .community-qr-card.support {
  border-color: #047857;
}

.dark .todo-item:hover,
.dark .notice-item:hover {
  background: #475569;
}

.dark .todo-status.active {
  border-color: #155e63;
  background: #123b40;
  color: #67e8f9;
}

.dark .todo-status.candidate {
  border-color: #1d4ed8;
  background: #172554;
  color: #bfdbfe;
}

.dark .todo-action {
  color: #5eead4;
}

.dark .todo-action:hover {
  background: #134e4a;
  color: #99f6e4;
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

.dark .notice-item.unread {
  background: #422006;
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
