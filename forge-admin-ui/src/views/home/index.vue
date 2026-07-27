<template>
  <div class="home-page">
    <section class="welcome-pane dashboard-pane">
      <div class="welcome-profile">
        <div class="welcome-avatar">
          <img :src="welcomeAvatar" alt="用户头像">
        </div>
        <div class="welcome-copy">
          <div class="welcome-kicker">
            Forge Admin 工作台
          </div>
          <h1>{{ welcomeTitle }}</h1>
          <p>聚合审批、应用搭建、系统运维与消息通知，优先处理今天最需要关注的事项。</p>
        </div>
      </div>

      <div class="welcome-metrics">
        <button
          v-for="metric in topMetrics"
          :key="metric.label"
          type="button"
          class="welcome-metric"
          @click="goTo(metric.path)"
        >
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <em>{{ metric.desc }}</em>
        </button>
      </div>

      <div class="guide-pane">
        <div class="guide-head">
          <span>业务系统搭建路径</span>
          <button type="button" @click="goTo('/app-center')">
            进入应用中心
            <i class="i-material-symbols:chevron-right-rounded" />
          </button>
        </div>
        <div class="guide-list">
          <button
            v-for="(step, index) in guideSteps"
            :key="step.title"
            type="button"
            class="guide-item"
            @click="goTo(step.path)"
          >
            <span class="guide-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="guide-icon"><i :class="step.icon" /></span>
            <span class="guide-text">
              <strong>{{ step.title }}</strong>
              <em>{{ step.desc }}</em>
            </span>
          </button>
        </div>
      </div>
    </section>

    <section class="dashboard-pane support-pane top-support-pane">
      <div class="support-copy">
        <span>ForgeAdmin 社区</span>
        <strong>低代码配置、流程审批、插件扩展、业务二开，添加维护者微信，把场景和截图发过来一起梳理。</strong>
      </div>
      <div class="support-qrs">
        <n-image
          class="support-qr"
          :src="wechatGroupQr"
          :preview-src="wechatGroupQr"
          object-fit="contain"
          alt="ForgeAdmin 维护者微信二维码"
        />
        <n-image
          class="support-qr"
          :src="wechatGroupQrAlt"
          :preview-src="wechatGroupQrAlt"
          object-fit="contain"
          alt="ForgeAdmin 维护者微信二维码"
        />
        <n-image
          class="support-qr"
          :src="wechatSupportQr"
          :preview-src="wechatSupportQr"
          object-fit="contain"
          alt="ForgeAdmin 维护支持二维码"
        />
      </div>
    </section>

    <section class="workspace-grid">
      <div class="main-column">
        <section class="dashboard-pane flow-pane">
          <div class="dashboard-header">
            <div>
              <h2>审批中心</h2>
              <p>按当前角色聚合流程任务，待办优先处理。</p>
            </div>
            <button type="button" class="text-link" @click="goTo('/flow/todo')">
              查看全部
              <i class="i-material-symbols:chevron-right-rounded" />
            </button>
          </div>

          <div class="flow-grid">
            <button
              v-for="item in flowTiles"
              :key="item.title"
              type="button"
              class="flow-tile"
              :class="item.tone"
              @click="goTo(item.path)"
            >
              <span class="flow-icon"><i :class="item.icon" /></span>
              <span class="flow-title">{{ item.title }}</span>
              <strong>{{ item.value }}</strong>
              <em>{{ item.desc }}</em>
            </button>
          </div>
        </section>

        <section class="dashboard-pane todo-pane">
          <div class="dashboard-header">
            <div>
              <h2>待办任务</h2>
              <p>展示最近需要处理的审批事项。</p>
            </div>
            <button type="button" class="text-link" @click="goTo('/flow/todo')">
              全部待办
              <i class="i-material-symbols:chevron-right-rounded" />
            </button>
          </div>

          <n-spin :show="todoLoading">
            <div v-if="todoList.length === 0" class="empty-state">
              <i class="i-material-symbols:task-alt-rounded" />
              <strong>暂无待办任务</strong>
              <span>所有审批任务已处理完毕。</span>
            </div>
            <div v-else class="todo-list">
              <article
                v-for="task in todoList"
                :key="task.taskId || task.id"
                class="todo-item"
                @click="openTodoTask(task)"
              >
                <span class="todo-status" :class="task.status === 0 && !task.assignee ? 'candidate' : 'active'">
                  {{ task.status === 0 && !task.assignee ? '待签收' : '待处理' }}
                </span>
                <div class="todo-main">
                  <div class="todo-title-row">
                    <strong>{{ task.title || task.processTitle || task.taskName || '-' }}</strong>
                    <span v-if="task.priority >= 2" class="priority-tag" :class="getPriorityClass(task.priority)">
                      {{ getPriorityText(task.priority) }}
                    </span>
                  </div>
                  <div class="todo-meta">
                    <span>节点：{{ task.taskName || '-' }}</span>
                    <span>发起人：{{ task.startUserName || '-' }}</span>
                    <span v-if="task.startDeptName">部门：{{ task.startDeptName }}</span>
                  </div>
                </div>
                <div class="todo-side">
                  <span>{{ formatTime(task.createTime) }}</span>
                  <button type="button" @click.stop="openTodoTask(task)">
                    处理
                    <i class="i-material-symbols:chevron-right-rounded" />
                  </button>
                </div>
              </article>
            </div>
          </n-spin>
        </section>

        <section class="app-row">
          <div class="dashboard-pane app-pane">
            <div class="dashboard-header">
              <div>
                <h2>我的应用</h2>
                <p>常用业务搭建与应用入口。</p>
              </div>
            </div>
            <div class="app-list">
              <button
                v-for="app in appShortcuts"
                :key="app.title"
                type="button"
                class="app-item"
                @click="goTo(app.path)"
              >
                <span class="app-icon"><i :class="app.icon" /></span>
                <span>
                  <strong>{{ app.title }}</strong>
                  <em>{{ app.desc }}</em>
                </span>
              </button>
            </div>
          </div>

          <div class="dashboard-pane chart-pane">
            <div class="dashboard-header compact">
              <div>
                <h2>访问趋势</h2>
                <p>近 7 日系统访问概览。</p>
              </div>
              <button type="button" class="icon-action" title="刷新" @click="refreshVisitChart">
                <i class="i-material-symbols:refresh-rounded" />
              </button>
            </div>
            <div ref="visitChartRef" class="chart-container" />
          </div>
        </section>
      </div>

      <aside class="side-column">
        <section class="dashboard-pane quick-pane">
          <div class="dashboard-header compact">
            <div>
              <h2>快捷入口</h2>
              <p>高频管理功能。</p>
            </div>
          </div>
          <div class="quick-list">
            <button
              v-for="item in quickLinks"
              :key="item.path"
              type="button"
              class="quick-item"
              @click="goTo(item.path)"
            >
              <span><i :class="item.icon" /></span>
              <strong>{{ item.title }}</strong>
            </button>
          </div>
        </section>

        <section class="dashboard-pane notice-pane">
          <div class="dashboard-header compact">
            <div>
              <h2>公告消息</h2>
              <p>{{ unreadNotice > 0 ? `${unreadNotice} 条未读` : '暂无未读消息' }}</p>
            </div>
            <button type="button" class="text-link" @click="goTo('/system/notice-list')">
              更多
            </button>
          </div>

          <div v-if="noticeList.length === 0" class="empty-state small">
            <i class="i-material-symbols:inbox-rounded" />
            <strong>暂无公告</strong>
          </div>
          <div v-else class="notice-list">
            <button
              v-for="notice in noticeList.slice(0, 5)"
              :key="notice.noticeId"
              type="button"
              class="notice-item"
              :class="{ unread: notice.isRead === 0 }"
              @click="openNotice(notice)"
            >
              <span class="notice-dot" />
              <span class="notice-copy">
                <strong>{{ notice.noticeTitle }}</strong>
                <em>{{ getNoticeTypeText(notice.noticeType) }} · {{ formatTime(notice.publishTime) }}</em>
              </span>
            </button>
          </div>
        </section>

        <section class="dashboard-pane system-pane">
          <div class="dashboard-header compact">
            <div>
              <h2>系统概览</h2>
              <p>账户、流程与消息状态。</p>
            </div>
            <button type="button" class="icon-action" title="刷新用户增长" @click="refreshUserChart">
              <i class="i-material-symbols:refresh-rounded" />
            </button>
          </div>
          <div class="system-metrics">
            <div v-for="metric in systemMetrics" :key="metric.label" class="system-metric">
              <div class="system-metric-head">
                <span>{{ metric.label }}</span>
                <strong>{{ metric.value }}</strong>
              </div>
              <div class="metric-track">
                <span :style="{ width: `${metric.percent}%` }" />
              </div>
            </div>
          </div>
          <div ref="userChartRef" class="mini-chart" />
        </section>
      </aside>
    </section>

    <n-modal v-model:show="showNoticeModal" preset="card" title="公告详情" style="width: 800px">
      <div v-if="currentNotice" class="notice-detail">
        <h3>{{ currentNotice.noticeTitle }}</h3>
        <div class="detail-meta">
          <n-tag :type="getNoticeTypeColor(currentNotice.noticeType)" size="small">
            {{ getNoticeTypeText(currentNotice.noticeType) }}
          </n-tag>
          <span>发布时间：{{ currentNotice.publishTime }}</span>
        </div>
        <n-divider />
        <div class="detail-content" v-html="currentNotice.noticeContent" />
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import flowApi from '@/api/flow'
import wechatGroupQrAlt from '@/assets/images/forge-wechat-group1.png'
import wechatGroupQr from '@/assets/images/forge-wechat-group.png'
import wechatSupportQr from '@/assets/images/forge-wechat-support.png'
import welcomeAvatar from '@/assets/images/home-welcome-avatar.png'
import { useUserStore } from '@/store'
import { request } from '@/utils'

const router = useRouter()
const userStore = useUserStore()

const onlineCount = ref(0)
const todayLoginCount = ref(0)
const totalUserCount = ref(0)

const todoCount = ref(0)
const doneCount = ref(0)
const startedCount = ref(0)
const pendingStarted = ref(0)

const unreadNotice = ref(0)
const todoLoading = ref(false)
const todoList = ref([])
const noticeList = ref([])

const showNoticeModal = ref(false)
const currentNotice = ref(null)

const visitChartRef = ref(null)
const userChartRef = ref(null)
let visitChart = null
let userChart = null

const displayName = computed(() => userStore.realName || userStore.username || '管理员')
const welcomeTitle = computed(() => `Hi，${displayName.value}，欢迎使用`)

const topMetrics = computed(() => [
  { label: '在线用户', value: onlineCount.value, desc: '当前活跃会话', path: '/system/online' },
  { label: '今日登录', value: todayLoginCount.value, desc: '登录审计记录', path: '/system/login-log' },
  { label: '待办任务', value: todoCount.value, desc: '需要处理的审批', path: '/flow/todo' },
])

const flowTiles = computed(() => [
  { title: '我发起的', value: startedCount.value, desc: `${pendingStarted.value} 个审批中`, path: '/flow/started', icon: 'i-material-symbols:rocket-launch-rounded', tone: 'started' },
  { title: '我的待办', value: todoCount.value, desc: '待签收 / 待处理', path: '/flow/todo', icon: 'i-material-symbols:pending-actions-rounded', tone: 'todo' },
  { title: '我的已办', value: doneCount.value, desc: '已处理任务', path: '/flow/done', icon: 'i-material-symbols:task-alt-rounded', tone: 'done' },
  { title: '发起流程', value: '发起', desc: '选择流程模板', path: '/flow/template', icon: 'i-material-symbols:add-task-rounded', tone: 'create' },
  { title: '抄送我的', value: '查看', desc: '关注流转信息', path: '/flow/cc', icon: 'i-material-symbols:forward-to-inbox-rounded', tone: 'cc' },
  { title: '流程监控', value: '监控', desc: '运行状态追踪', path: '/flow/monitor', icon: 'i-material-symbols:monitor-heart-rounded', tone: 'monitor' },
])

const guideSteps = [
  { title: '创建应用', desc: '定义业务系统入口', path: '/app-center', icon: 'i-material-symbols:add-box-rounded' },
  { title: '设计对象', desc: '配置表单、列表和字段', path: '/ai/lowcode-apps', icon: 'i-material-symbols:edit-document-rounded' },
  { title: '编排流程', desc: '绑定审批与状态流转', path: '/flow/model', icon: 'i-material-symbols:account-tree-rounded' },
  { title: '发布授权', desc: '菜单发布并配置权限', path: '/system/menu', icon: 'i-material-symbols:shield-person-rounded' },
]

const appShortcuts = [
  { title: '应用中心', desc: '创建和维护业务应用', path: '/app-center', icon: 'i-material-symbols:apps-rounded' },
  { title: '低代码建模', desc: '业务对象与页面设计', path: '/ai/lowcode-apps', icon: 'i-material-symbols:data-object-rounded' },
  { title: '数据统计', desc: '业务数据看板入口', path: '/app-center/stats', icon: 'i-material-symbols:query-stats-rounded' },
  { title: '流程配置', desc: '流程模型和表单配置', path: '/flow/model', icon: 'i-material-symbols:conversion-path-rounded' },
]

const quickLinks = [
  { title: '用户管理', icon: 'i-material-symbols:person-rounded', path: '/system/user' },
  { title: '角色管理', icon: 'i-material-symbols:admin-panel-settings-rounded', path: '/system/role' },
  { title: '组织管理', icon: 'i-material-symbols:account-tree-rounded', path: '/system/org' },
  { title: '菜单管理', icon: 'i-material-symbols:menu-open-rounded', path: '/system/menu' },
  { title: '岗位管理', icon: 'i-material-symbols:badge-rounded', path: '/system/post' },
  { title: '文件中心', icon: 'i-material-symbols:folder-open-rounded', path: '/system/file-list' },
]

const systemMetrics = computed(() => {
  const userBase = Math.max(totalUserCount.value, 1)
  const flowBase = Math.max(startedCount.value + doneCount.value + todoCount.value, 1)
  const noticeBase = Math.max(noticeList.value.length, unreadNotice.value, 1)
  return [
    { label: '用户规模', value: totalUserCount.value, percent: clampPercent((totalUserCount.value / userBase) * 100) },
    { label: '流程处理', value: doneCount.value, percent: clampPercent((doneCount.value / flowBase) * 100) },
    { label: '未读公告', value: unreadNotice.value, percent: clampPercent((unreadNotice.value / noticeBase) * 100) },
  ]
})

function clampPercent(value) {
  return Math.max(6, Math.min(100, Math.round(value || 0)))
}

async function loadUserStats() {
  const today = new Date().toISOString().split('T')[0]
  const [onlineResult, userResult, loginResult] = await Promise.allSettled([
    request.get('/auth/online/page', {
      params: { pageNum: 1, pageSize: 1 },
      needTip: false,
    }),
    request.get('/system/user/page', {
      params: { pageNum: 1, pageSize: 1 },
      needTip: false,
    }),
    request.get('/system/loginLog/page', {
      params: { pageNum: 1, pageSize: 1, startTime: today, endTime: today },
      needTip: false,
    }),
  ])

  onlineCount.value = onlineResult.status === 'fulfilled' ? onlineResult.value.data?.total || 0 : 0
  totalUserCount.value = userResult.status === 'fulfilled' ? userResult.value.data?.total || 0 : 0
  todayLoginCount.value = loginResult.status === 'fulfilled' ? loginResult.value.data?.total || 0 : 0
}

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
    pendingStarted.value = startedRes.data?.records?.filter(item => item.status === 1).length || 0
  }
  catch {
    todoCount.value = 0
    doneCount.value = 0
    startedCount.value = 0
    pendingStarted.value = 0
    console.error('加载流程统计失败')
  }
}

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
    todoList.value = res.data?.records || []
  }
  catch {
    todoList.value = []
    console.error('加载待办列表失败')
  }
  finally {
    todoLoading.value = false
  }
}

async function loadNoticeList() {
  try {
    const res = await request.get('/system/notice/user/page', { params: { pageNum: 1, pageSize: 10 } })
    noticeList.value = res.data?.records || []
    unreadNotice.value = noticeList.value.filter(item => item.isRead === 0).length
  }
  catch {
    console.error('加载通知公告失败')
  }
}

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
  return String(time).split(' ')[0]
}

function getPriorityClass(priority) {
  if (priority >= 3)
    return 'urgent'
  if (priority === 2)
    return 'high'
  return ''
}

function getPriorityText(priority) {
  const textMap = { 0: '低', 1: '普通', 2: '高', 3: '紧急' }
  return textMap[priority] || '普通'
}

function getNoticeTypeText(type) {
  const typeMap = { NOTICE: '通知', ANNOUNCEMENT: '公告', NEWS: '新闻' }
  return typeMap[type] || type || '公告'
}

function getNoticeTypeColor(type) {
  const colorMap = { NOTICE: 'info', ANNOUNCEMENT: 'warning', NEWS: 'success' }
  return colorMap[type] || 'default'
}

function initVisitChart() {
  if (!visitChartRef.value)
    return
  if (visitChart)
    visitChart.dispose()
  visitChart = echarts.init(visitChartRef.value)
  visitChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#ffffff',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#0f172a' },
    },
    grid: { left: 32, right: 12, top: 18, bottom: 28 },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#64748b', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#64748b', fontSize: 11 },
    },
    series: [
      {
        name: '访问量',
        type: 'bar',
        barWidth: 18,
        data: [320, 502, 301, 434, 590, 530, 420],
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: '#2563eb',
        },
      },
    ],
  })
}

function initUserChart() {
  if (!userChartRef.value)
    return
  if (userChart)
    userChart.dispose()
  userChart = echarts.init(userChartRef.value)
  userChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#ffffff',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#0f172a' },
    },
    grid: { left: 8, right: 8, top: 16, bottom: 8 },
    xAxis: {
      type: 'category',
      show: false,
      data: ['1月', '2月', '3月', '4月', '5月', '6月'],
    },
    yAxis: { type: 'value', show: false },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: [820, 932, 901, 934, 1290, 1330],
        lineStyle: { width: 2, color: '#2563eb' },
        areaStyle: { color: 'rgba(37, 99, 235, 0.1)' },
      },
    ],
  })
}

function resizeCharts() {
  visitChart?.resize()
  userChart?.resize()
}

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
    window.addEventListener('resize', resizeCharts)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  visitChart?.dispose()
  userChart?.dispose()
})
</script>

<style scoped>
.home-page {
  --home-bg: #f6f8fb;
  --home-panel: #fff;
  --home-border: #e5e7eb;
  --home-text: #0f172a;
  --home-muted: #64748b;
  --home-soft: #f8fafc;
  --home-blue: #2563eb;
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 10px;
  background: var(--home-bg);
  color: var(--home-text);
}

.dashboard-pane {
  border: 1px solid var(--home-border);
  border-radius: 8px;
  background: var(--home-panel);
  box-shadow: none;
}

.welcome-pane {
  display: grid;
  grid-template-columns: minmax(260px, 0.72fr) minmax(280px, 0.62fr) minmax(420px, 1.15fr);
  gap: 12px;
  align-items: stretch;
  margin-bottom: 10px;
  padding: 12px;
}

.welcome-profile,
.welcome-metrics,
.guide-pane {
  min-width: 0;
}

.welcome-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
}

.welcome-avatar {
  width: 58px;
  height: 58px;
  flex: 0 0 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #bfdbfe;
  border-radius: 50%;
  background: #eff6ff;
  color: var(--home-blue);
  font-size: 22px;
  font-weight: 800;
  overflow: hidden;
}

.welcome-avatar img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.welcome-copy {
  min-width: 0;
}

.welcome-kicker {
  color: var(--home-blue);
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
}

.welcome-copy h1 {
  margin: 2px 0 4px;
  color: var(--home-text);
  font-size: 20px;
  font-weight: 760;
  line-height: 28px;
}

.welcome-copy p {
  margin: 0;
  color: var(--home-muted);
  font-size: 12px;
  line-height: 19px;
}

.welcome-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.welcome-metric {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--home-border);
  border-radius: 7px;
  background: var(--home-soft);
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.welcome-metric:hover,
.guide-item:hover,
.flow-tile:hover,
.app-item:hover,
.quick-item:hover,
.notice-item:hover,
.todo-item:hover {
  border-color: #bfdbfe;
  background: #f3f7ff;
}

.welcome-metric span,
.welcome-metric em {
  display: block;
  color: var(--home-muted);
  font-size: 11px;
  font-style: normal;
  line-height: 16px;
}

.welcome-metric strong {
  display: block;
  margin: 3px 0 1px;
  color: var(--home-text);
  font-size: 22px;
  font-weight: 780;
  line-height: 28px;
}

.guide-pane {
  padding: 8px;
  border: 1px solid #edf2f7;
  border-radius: 7px;
  background: #fbfcfe;
}

.guide-head,
.dashboard-header,
.system-metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.guide-head {
  margin-bottom: 8px;
}

.guide-head span,
.dashboard-header h2 {
  margin: 0;
  color: var(--home-text);
  font-size: 14px;
  font-weight: 720;
  line-height: 20px;
}

.guide-head button,
.text-link,
.icon-action {
  border: 0;
  background: transparent;
  color: var(--home-blue);
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
}

.guide-head button,
.text-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}

.guide-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.guide-item,
.flow-tile,
.app-item,
.quick-item,
.notice-item {
  min-width: 0;
  border: 1px solid var(--home-border);
  border-radius: 6px;
  background: #fff;
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease;
}

.guide-item {
  display: grid;
  grid-template-columns: auto 28px minmax(0, 1fr);
  gap: 7px;
  align-items: center;
  padding: 8px;
}

.guide-index {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 760;
}

.guide-icon,
.flow-icon,
.app-icon,
.quick-item span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #eff6ff;
  color: var(--home-blue);
}

.guide-icon {
  width: 28px;
  height: 28px;
}

.guide-icon i,
.flow-icon i,
.app-icon i,
.quick-item i {
  font-size: 16px;
}

.guide-text,
.app-item span:last-child,
.notice-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.guide-text strong,
.app-item strong,
.notice-copy strong {
  overflow: hidden;
  color: var(--home-text);
  font-size: 12px;
  font-weight: 700;
  line-height: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-text em,
.app-item em,
.notice-copy em {
  overflow: hidden;
  color: var(--home-muted);
  font-size: 11px;
  font-style: normal;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 10px;
  align-items: start;
}

.main-column,
.side-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dashboard-header {
  min-height: 42px;
  padding: 9px 12px;
  border-bottom: 1px solid var(--home-border);
}

.dashboard-header.compact {
  min-height: 38px;
}

.dashboard-header p {
  margin: 1px 0 0;
  color: var(--home-muted);
  font-size: 11px;
  line-height: 16px;
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
}

.flow-tile {
  display: grid;
  grid-template-rows: auto auto auto auto;
  gap: 4px;
  padding: 10px;
}

.flow-icon {
  width: 34px;
  height: 34px;
  margin-bottom: 2px;
}

.flow-title {
  color: var(--home-muted);
  font-size: 12px;
  line-height: 16px;
}

.flow-tile strong {
  color: var(--home-text);
  font-size: 21px;
  font-weight: 780;
  line-height: 26px;
}

.flow-tile em {
  overflow: hidden;
  color: var(--home-muted);
  font-size: 11px;
  font-style: normal;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-tile.todo .flow-icon {
  background: #fff7ed;
  color: #c2410c;
}

.flow-tile.done .flow-icon {
  background: #f0fdf4;
  color: #16a34a;
}

.flow-tile.create .flow-icon {
  background: #eef2ff;
  color: #4f46e5;
}

.flow-tile.cc .flow-icon {
  background: #f0fdfa;
  color: #0f766e;
}

.flow-tile.monitor .flow-icon {
  background: #fef2f2;
  color: #dc2626;
}

.todo-pane :deep(.n-spin-container),
.todo-pane :deep(.n-spin-content) {
  min-height: 236px;
}

.todo-list {
  padding: 4px 10px 10px;
}

.todo-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 56px;
  padding: 8px 0;
  border-bottom: 1px solid #edf2f7;
  cursor: pointer;
}

.todo-item:last-child {
  border-bottom: 0;
}

.todo-status,
.priority-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  padding: 0 7px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.todo-status {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: var(--home-blue);
}

.todo-status.active {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.todo-main {
  min-width: 0;
}

.todo-title-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.todo-title-row strong {
  min-width: 0;
  overflow: hidden;
  color: var(--home-text);
  font-size: 13px;
  line-height: 19px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.priority-tag {
  background: #fff7ed;
  color: #c2410c;
}

.priority-tag.urgent {
  background: #fff1f2;
  color: #be123c;
}

.todo-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 12px;
  margin-top: 3px;
  color: var(--home-muted);
  font-size: 11px;
  line-height: 16px;
}

.todo-side {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--home-muted);
  font-size: 11px;
  white-space: nowrap;
}

.todo-side button {
  display: inline-flex;
  align-items: center;
  gap: 1px;
  height: 26px;
  padding: 0 4px 0 8px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--home-blue);
  cursor: pointer;
  font: inherit;
  font-weight: 700;
}

.app-row {
  display: grid;
  grid-template-columns: minmax(0, 0.96fr) minmax(320px, 0.8fr);
  gap: 10px;
}

.app-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
}

.app-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  padding: 10px;
}

.app-icon {
  width: 34px;
  height: 34px;
}

.chart-container {
  height: 184px;
}

.quick-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
}

.quick-item {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  padding: 9px;
}

.quick-item span {
  width: 30px;
  height: 30px;
}

.quick-item strong {
  overflow: hidden;
  color: var(--home-text);
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px;
}

.notice-item {
  display: grid;
  grid-template-columns: 6px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  padding: 8px;
}

.notice-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
}

.notice-item.unread .notice-dot {
  background: #f59e0b;
}

.notice-item.unread {
  background: #fffbeb;
  border-color: #fde68a;
}

.system-metrics {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 12px 4px;
}

.system-metric-head span {
  color: var(--home-muted);
  font-size: 12px;
}

.system-metric-head strong {
  color: var(--home-text);
  font-size: 13px;
}

.metric-track {
  height: 6px;
  margin-top: 5px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2f7;
}

.metric-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--home-blue);
}

.mini-chart {
  height: 82px;
  margin: 0 10px 10px;
}

.support-pane {
  padding: 10px;
}

.support-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

.support-copy span {
  color: var(--home-blue);
  font-size: 12px;
  font-weight: 760;
}

.support-copy strong {
  color: var(--home-text);
  font-size: 13px;
  line-height: 20px;
}

.support-qrs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.support-qr {
  width: 100%;
  height: 92px;
  overflow: hidden;
  border: 1px solid var(--home-border);
  border-radius: 6px;
  background: #fff;
  cursor: zoom-in;
}

.support-qr :deep(img) {
  width: 100%;
  height: 92px;
  object-fit: contain;
}

.empty-state {
  min-height: 190px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 18px;
  color: #94a3b8;
}

.empty-state.small {
  min-height: 124px;
}

.empty-state i {
  font-size: 32px;
  opacity: 0.42;
}

.empty-state strong {
  color: var(--home-text);
  font-size: 13px;
}

.empty-state span {
  font-size: 12px;
}

.icon-action {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  color: var(--home-muted);
}

.icon-action:hover {
  background: #eff6ff;
  color: var(--home-blue);
}

.notice-detail h3 {
  margin: 0 0 8px;
  color: var(--home-text);
  font-size: 18px;
  font-weight: 760;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--home-muted);
  font-size: 12px;
}

.detail-content {
  color: #475569;
  font-size: 14px;
  line-height: 1.8;
}

.detail-content :deep(img) {
  max-width: 100%;
  border-radius: 6px;
}

@media (max-width: 1500px) {
  .welcome-pane {
    grid-template-columns: 1fr;
  }

  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1180px) {
  .flow-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .app-row,
  .side-column {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .home-page {
    padding: 8px;
  }

  .welcome-profile {
    align-items: flex-start;
  }

  .welcome-metrics,
  .guide-list,
  .flow-grid,
  .app-list,
  .quick-list {
    grid-template-columns: 1fr;
  }

  .todo-item {
    grid-template-columns: 1fr;
  }

  .todo-side {
    justify-content: space-between;
  }
}

.dark .home-page {
  --home-bg: #0f172a;
  --home-panel: #111827;
  --home-border: #334155;
  --home-text: #f1f5f9;
  --home-muted: #94a3b8;
  --home-soft: #162033;
}

.dark .guide-pane,
.dark .welcome-metric,
.dark .guide-item,
.dark .flow-tile,
.dark .app-item,
.dark .quick-item,
.dark .notice-item {
  background: #162033;
}

.dark .welcome-metric:hover,
.dark .guide-item:hover,
.dark .flow-tile:hover,
.dark .app-item:hover,
.dark .quick-item:hover,
.dark .notice-item:hover {
  background: #1e293b;
  border-color: rgba(96, 165, 250, 0.32);
}

.dark .notice-item.unread {
  background: #422006;
  border-color: #92400e;
}

.dark .metric-track {
  background: #1e293b;
}

.dark .support-qr {
  background: #fff;
}

.dark .detail-content {
  color: #cbd5e1;
}

/* Stable workbench override: keep the home page compact and predictable. */
.home-page {
  box-sizing: border-box;
  min-height: 100%;
  padding: 10px;
  background: var(--home-bg);
}

.home-page *,
.home-page *::before,
.home-page *::after {
  box-sizing: border-box;
}

.home-page button {
  appearance: none;
  font: inherit;
}

.dashboard-pane {
  overflow: hidden;
  border: 1px solid var(--home-border);
  border-radius: 8px;
  background: var(--home-panel);
}

.welcome-pane {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  margin-bottom: 10px;
}

.welcome-profile {
  min-height: 74px;
  padding: 6px;
}

.welcome-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.welcome-metric,
.guide-item,
.flow-tile,
.app-item,
.quick-item,
.notice-item {
  display: block;
  border: 1px solid var(--home-border);
  background: #fff;
  color: var(--home-text);
  text-decoration: none;
}

.guide-pane {
  padding: 10px;
  border: 1px solid var(--home-border);
  border-radius: 7px;
  background: #fbfcfe;
}

.guide-list {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.guide-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  grid-template-areas:
    'icon title'
    'icon desc';
  gap: 2px 8px;
  min-height: 58px;
}

.guide-index {
  display: none;
}

.guide-icon {
  grid-area: icon;
}

.guide-text strong {
  grid-area: title;
}

.guide-text em {
  grid-area: desc;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 10px;
  align-items: start;
}

.main-column,
.side-column {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
}

.panel-header,
.dashboard-header {
  min-height: 42px;
  padding: 9px 12px;
  border-bottom: 1px solid var(--home-border);
  background: #fff;
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
}

.flow-tile {
  min-height: 86px;
  padding: 10px;
  border-radius: 6px;
}

.app-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.85fr);
  gap: 10px;
}

.app-list,
.quick-list {
  display: grid;
  gap: 8px;
  padding: 10px;
}

.app-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.quick-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.app-item,
.quick-item {
  min-height: 52px;
  padding: 9px;
  border-radius: 6px;
}

.todo-list,
.notice-list {
  padding: 8px 10px 10px;
}

.todo-item {
  grid-template-columns: auto minmax(0, 1fr) auto;
  padding: 9px 0;
  background: transparent;
}

.chart-container {
  height: 200px;
}

.mini-chart {
  height: 82px;
}

.support-pane {
  padding: 10px;
}

.support-qrs {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.support-qr,
.support-qr :deep(img) {
  height: 88px;
}

.welcome-pane {
  grid-template-columns: minmax(320px, 0.74fr) minmax(520px, 1fr);
  grid-template-areas:
    'profile guide'
    'metrics guide';
  align-items: stretch;
}

.welcome-profile {
  grid-area: profile;
  align-items: center;
  border: 1px solid #edf2f7;
  border-radius: 7px;
  background: #fbfcfe;
}

.welcome-metrics {
  grid-area: metrics;
}

.guide-pane {
  grid-area: guide;
}

.top-support-pane {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(480px, 640px);
  gap: 14px;
  align-items: stretch;
  margin-bottom: 10px;
  padding: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 58%, #f0fdfa 100%);
}

.top-support-pane .support-copy {
  justify-content: center;
  margin-bottom: 0;
  padding: 4px 8px;
}

.top-support-pane .support-copy span {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 760;
}

.top-support-pane .support-copy strong {
  max-width: 640px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 720;
  line-height: 25px;
}

.top-support-pane .support-qrs {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.top-support-pane .support-qr,
.top-support-pane .support-qr :deep(img) {
  height: 148px;
}

.top-support-pane .support-qr {
  border-color: #dbeafe;
  background: #fff;
}

.workspace-grid {
  grid-template-columns: minmax(0, 1fr) 340px;
}

.flow-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.flow-tile {
  min-height: 92px;
}

.dark .welcome-metric,
.dark .guide-item,
.dark .flow-tile,
.dark .app-item,
.dark .quick-item,
.dark .notice-item,
.dark .dashboard-header {
  background: #162033;
}

@media (max-width: 1280px) {
  .welcome-pane,
  .top-support-pane {
    grid-template-columns: 1fr;
    grid-template-areas:
      'profile'
      'metrics'
      'guide';
  }

  .workspace-grid,
  .app-row {
    grid-template-columns: 1fr;
  }

  .side-column {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .welcome-metrics,
  .guide-list,
  .flow-grid,
  .app-list,
  .quick-list,
  .support-qrs,
  .side-column {
    grid-template-columns: 1fr;
  }

  .top-support-pane .support-qr,
  .top-support-pane .support-qr :deep(img) {
    height: 220px;
  }
}

/* Compact pass: reduce first-screen whitespace and keep guide icons visible. */
.home-page {
  padding: 8px;
}

.welcome-pane {
  gap: 8px;
  margin-bottom: 8px;
  padding: 8px;
}

.welcome-profile {
  min-height: 62px;
  padding: 6px 8px;
}

.welcome-avatar {
  width: 48px;
  height: 48px;
  flex-basis: 48px;
  font-size: 18px;
}

.welcome-copy h1 {
  margin: 0 0 2px;
  font-size: 18px;
  line-height: 24px;
}

.welcome-copy p {
  font-size: 12px;
  line-height: 17px;
}

.welcome-metrics {
  gap: 6px;
}

.welcome-metric {
  padding: 8px 9px;
}

.welcome-metric strong {
  margin: 1px 0;
  font-size: 20px;
  line-height: 24px;
}

.guide-pane {
  padding: 8px;
}

.guide-head {
  margin-bottom: 6px;
}

.guide-list {
  gap: 6px;
}

.guide-item {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  grid-template-areas: none;
  min-height: 48px;
  padding: 7px 8px;
  align-items: center;
}

.guide-icon {
  width: 30px;
  height: 30px;
  grid-area: auto;
}

.guide-icon i {
  display: block;
  font-size: 17px;
  line-height: 1;
}

.guide-text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 1px;
}

.top-support-pane {
  gap: 10px;
  margin-bottom: 8px;
  padding: 9px 10px;
}

.top-support-pane .support-copy {
  padding: 0 4px;
}

.top-support-pane .support-copy strong {
  font-size: 14px;
  line-height: 21px;
}

.top-support-pane .support-qr,
.top-support-pane .support-qr :deep(img) {
  height: 116px;
}

.workspace-grid,
.main-column,
.side-column,
.app-row {
  gap: 8px;
}

.dashboard-header {
  min-height: 36px;
  padding: 7px 10px;
}

.dashboard-header h2 {
  font-size: 13px;
  line-height: 18px;
}

.dashboard-header p {
  margin-top: 0;
}

.flow-grid,
.app-list,
.quick-list,
.notice-list,
.todo-list,
.system-metrics {
  padding: 8px;
}

.flow-grid {
  gap: 6px;
}

.flow-tile {
  min-height: 76px;
  padding: 8px;
}

.flow-icon {
  width: 28px;
  height: 28px;
}

.flow-tile strong {
  font-size: 18px;
  line-height: 22px;
}

.app-item,
.quick-item {
  min-height: 46px;
  padding: 7px 8px;
}

.todo-item {
  min-height: 48px;
  padding: 7px 0;
}

.chart-container {
  height: 160px;
}

.mini-chart {
  height: 66px;
  margin-bottom: 8px;
}
</style>
