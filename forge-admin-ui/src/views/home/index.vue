<template>
  <div class="home-page">
    <!-- 欢迎与业务概览 -->
    <section class="welcome-pane dashboard-pane">
      <HomeWelcomeProfile :title="welcomeTitle" />

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
          <span>业务搭建路径</span>
          <button type="button" class="text-link" @click="goTo('/app-center')">
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
        <span>社区与支持</span>
        <strong>低代码配置、流程审批与插件扩展问题，可扫码联系维护者协助排查。</strong>
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
              <WorkspaceIllustration artwork="workflow" size="small" />
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
            <n-spin :show="appLoading">
              <div v-if="!appLoading && appShortcuts.length === 0" class="empty-state small app-empty-state">
                <WorkspaceIllustration artwork="application" size="small" />
                <strong>暂无已投放应用</strong>
                <span>应用发布后可在发布页添加到 Forge 工作台。</span>
              </div>
              <div v-else class="app-list">
                <button
                  v-for="app in appShortcuts"
                  :key="app.id"
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
            </n-spin>
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
        <HomeNoticePanel />
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
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { businessApplicationWorkbench } from '@/api/business-application'
import flowApi from '@/api/flow'
import wechatGroupQrAlt from '@/assets/images/forge-wechat-group1.png'
import wechatGroupQr from '@/assets/images/forge-wechat-group.png'
import wechatSupportQr from '@/assets/images/forge-wechat-support.png'
import WorkspaceIllustration from '@/components/common/WorkspaceIllustration.vue'
import { useUserStore } from '@/store'
import { useNoticeStore } from '@/stores/system/noticeStore'
import { request } from '@/utils'
import HomeNoticePanel from './components/HomeNoticePanel.vue'
import HomeWelcomeProfile from './components/HomeWelcomeProfile.vue'

const router = useRouter()
const userStore = useUserStore()

const onlineCount = ref(0)
const todayLoginCount = ref(0)
const totalUserCount = ref(0)

const todoCount = ref(0)
const doneCount = ref(0)
const startedCount = ref(0)
const pendingStarted = ref(0)

const noticeStore = useNoticeStore()
const unreadNotice = computed(() => noticeStore.unreadCount)
const todoLoading = ref(false)
const appLoading = ref(false)
const todoList = ref([])
const distributedApplications = ref([])

const visitChartRef = ref(null)
const userChartRef = ref(null)
let visitChart = null
let userChart = null

const displayName = computed(() => userStore.realName || userStore.username || '管理员')
const welcomeTitle = computed(() => `${displayName.value}，欢迎回来`)

const topMetrics = computed(() => [
  { label: '在线用户', value: onlineCount.value, desc: '当前活跃会话', path: '/system/online' },
  { label: '今日登录', value: todayLoginCount.value, desc: '登录审计记录', path: '/system/login-log' },
  { label: '待办任务', value: todoCount.value, desc: '需要处理的审批', path: '/flow/todo' },
])

const flowTiles = computed(() => [
  { title: '我发起的', value: startedCount.value, desc: `${pendingStarted.value} 个审批中`, path: '/flow/started', icon: 'i-material-symbols:rocket-launch-rounded' },
  { title: '我的待办', value: todoCount.value, desc: '待签收 / 待处理', path: '/flow/todo', icon: 'i-material-symbols:pending-actions-rounded' },
  { title: '我的已办', value: doneCount.value, desc: '已处理任务', path: '/flow/done', icon: 'i-material-symbols:task-alt-rounded' },
  { title: '发起流程', value: '发起', desc: '选择流程模板', path: '/flow/template', icon: 'i-material-symbols:add-task-rounded' },
  { title: '抄送我的', value: '查看', desc: '关注流转信息', path: '/flow/cc', icon: 'i-material-symbols:forward-to-inbox-rounded' },
  { title: '流程监控', value: '监控', desc: '运行状态追踪', path: '/flow/monitor', icon: 'i-material-symbols:monitor-heart-rounded' },
])

const guideSteps = [
  { title: '创建应用', desc: '定义业务系统入口', path: '/app-center', icon: 'i-material-symbols:add-box-rounded' },
  { title: '设计对象', desc: '配置表单、列表和字段', path: '/ai/lowcode-apps', icon: 'i-material-symbols:edit-document-rounded' },
  { title: '编排流程', desc: '绑定审批与状态流转', path: '/flow/model', icon: 'i-material-symbols:account-tree-rounded' },
  { title: '发布授权', desc: '菜单发布并配置权限', path: '/system/menu', icon: 'i-material-symbols:shield-person-rounded' },
]

const appShortcuts = computed(() => distributedApplications.value.map(application => ({
  id: application.id,
  title: application.applicationName || application.applicationCode || '未命名应用',
  desc: application.description || '已发布业务应用',
  path: `/app/${encodeURIComponent(application.portalSlug || application.applicationCode)}`,
  icon: application.icon || 'i-material-symbols:apps-rounded',
})))

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
  const noticeBase = Math.max(noticeStore.total, unreadNotice.value, 1)
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

async function loadWorkbenchApplications() {
  appLoading.value = true
  try {
    const response = await businessApplicationWorkbench()
    distributedApplications.value = response.data || []
  }
  catch {
    distributedApplications.value = []
  }
  finally {
    appLoading.value = false
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
          color: '#0e42d2',
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
        lineStyle: { width: 2, color: '#0e42d2' },
        areaStyle: { color: 'rgba(14, 66, 210, 0.08)' },
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
  loadWorkbenchApplications()

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

<style scoped src="./home.css"></style>
