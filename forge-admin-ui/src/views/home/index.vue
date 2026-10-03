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
    </section>

    <!-- 业务搭建导航 -->
    <HomeBuildPath />

    <section class="workspace-grid">
      <div class="main-column">
        <!-- 审批概览与待办优先展示，数据仍由首页请求加载 -->
        <HomeApprovalCenter
          :todo-count="todoCount"
          :done-count="doneCount"
          :started-count="startedCount"
          :pending-started="pendingStarted"
        />
        <HomeTodoList :tasks="todoList" :loading="todoLoading" @open="openTodoTask" />

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
        <HomeQuickEntries />
        <HomeNoticePanel />

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
    <!-- 社区帮助放在业务工作区之后，保留二维码预览 -->
    <section class="dashboard-pane support-pane home-support-pane">
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
import HomeApprovalCenter from './components/HomeApprovalCenter.vue'
import HomeBuildPath from './components/HomeBuildPath.vue'
import HomeNoticePanel from './components/HomeNoticePanel.vue'
import HomeQuickEntries from './components/HomeQuickEntries.vue'
import HomeTodoList from './components/HomeTodoList.vue'
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

const appShortcuts = computed(() => distributedApplications.value.map(application => ({
  id: application.id,
  title: application.applicationName || application.applicationCode || '未命名应用',
  desc: application.description || '已发布业务应用',
  path: `/app/${encodeURIComponent(application.portalSlug || application.applicationCode)}`,
  icon: application.icon || 'i-material-symbols:apps-rounded',
})))

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
