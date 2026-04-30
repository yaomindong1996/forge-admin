<template>
  <div class="flow-monitor-page">
    <!-- 统计卡片 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" class="stat-cards">
      <n-gi>
        <n-card size="small">
          <n-statistic label="运行中流程" :value="statistics.runningInstances">
            <template #prefix>
              <i class="i-mdi:play-circle text-blue-500" />
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small">
          <n-statistic label="待办任务" :value="statistics.pendingTasks">
            <template #prefix>
              <i class="i-mdi:clipboard-list text-orange-500" />
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small">
          <n-statistic label="今日完成" :value="statistics.todayCompleted">
            <template #prefix>
              <i class="i-mdi:check-circle text-green-500" />
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <!-- 超时任务卡片：点击自动筛选到超时列表 -->
      <n-gi>
        <n-card
          size="small"
          class="timeout-card"
          :class="{ 'timeout-active': searchForm.status === 'timeout' }"
          hoverable
          style="cursor: pointer"
          @click="filterByTimeout"
        >
          <n-statistic label="超时任务" :value="statistics.timeoutTasks">
            <template #prefix>
              <i class="i-mdi:alert-circle text-red-500" />
            </template>
            <template #suffix>
              <span v-if="statistics.timeoutTasks > 0" class="timeout-hint">点击查看</span>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 搜索和操作区 -->
    <n-card class="mt-4" size="small">
      <n-form :model="searchForm" inline label-placement="left">
        <n-form-item label="流程名称">
          <n-input v-model:value="searchForm.processName" placeholder="请输入流程名称" clearable style="width: 200px" />
        </n-form-item>
        <n-form-item label="发起人">
          <n-input v-model:value="searchForm.initiator" placeholder="请输入发起人" clearable style="width: 150px" />
        </n-form-item>
        <n-form-item label="流程状态">
          <n-select
            v-model:value="searchForm.status"
            :options="statusOptions"
            placeholder="请选择状态"
            clearable
            style="width: 150px"
          />
        </n-form-item>
        <n-form-item label="时间范围">
          <n-date-picker
            v-model:value="searchForm.dateRange"
            type="daterange"
            clearable
            style="width: 260px"
          />
        </n-form-item>
        <n-form-item>
          <NSpace>
            <NButton type="primary" @click="handleSearch">
              <template #icon>
                <i class="i-mdi:magnify" />
              </template>
              查询
            </NButton>
            <NButton @click="handleReset">
              <template #icon>
                <i class="i-mdi:refresh" />
              </template>
              重置
            </NButton>
          </NSpace>
        </n-form-item>
      </n-form>
    </n-card>

    <!-- 流程实例列表 -->
    <n-card class="mt-4" title="流程实例监控">
      <template #header-extra>
        <NSpace>
          <NButton @click="loadData">
            <template #icon>
              <i class="i-mdi:refresh" />
            </template>
            刷新
          </NButton>
        </NSpace>
      </template>

      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :remote="true"
        :row-key="row => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </n-card>

    <!-- 任务统计图表 -->
    <n-grid :cols="2" :x-gap="16" class="mt-4">
      <n-gi>
        <n-card title="任务处理趋势">
          <template #header-extra>
            <n-radio-group v-model:value="chartPeriod" size="small" @update:value="refreshCharts">
              <n-radio-button :value="7">
                近7天
              </n-radio-button>
              <n-radio-button :value="30">
                近30天
              </n-radio-button>
            </n-radio-group>
          </template>
          <div ref="taskChartRef" style="height: 300px" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="流程分布统计">
          <div ref="processChartRef" style="height: 300px" />
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 流程图弹窗 -->
    <n-modal v-model:show="diagramModalVisible" preset="card" title="流程图" style="width: 90%; max-width: 1200px;">
      <ProcessDiagramViewer
        v-if="diagramModalVisible && currentDiagramInstanceId"
        :process-instance-id="currentDiagramInstanceId"
      />
    </n-modal>

    <!-- 流程详情抽屉 -->
    <n-drawer v-model:show="detailDrawerVisible" :width="600" title="流程详情">
      <n-drawer-content title="流程实例详情">
        <n-descriptions :column="2" label-placement="left">
          <n-descriptions-item label="流程名称">
            {{ currentInstance.processName }}
          </n-descriptions-item>
          <n-descriptions-item label="流程状态">
            <NTag :type="getStatusType(currentInstance.status)">
              {{ getStatusText(currentInstance.status) }}
            </NTag>
          </n-descriptions-item>
          <n-descriptions-item label="发起人">
            {{ currentInstance.initiatorName }}
          </n-descriptions-item>
          <n-descriptions-item label="发起时间">
            {{ currentInstance.startTime }}
          </n-descriptions-item>
          <n-descriptions-item label="当前节点">
            {{ currentInstance.currentNode }}
          </n-descriptions-item>
          <n-descriptions-item label="处理人">
            {{ currentInstance.currentAssignee }}
          </n-descriptions-item>
        </n-descriptions>

        <n-divider>处理历史</n-divider>

        <n-timeline>
          <n-timeline-item
            v-for="(item, index) in approvalHistory"
            :key="index"
            :type="getTimelineType(item.action)"
            :title="item.taskName"
            :time="item.createTime"
          >
            <p>处理人：{{ item.assigneeName }}</p>
            <p>
              处理结果：
              <NTag :type="getTimelineType(item.action)" size="small">
                {{ { approve: '同意', reject: '驳回', start: '发起', claim: '签收', delegate: '转办', withdraw: '撤回', pending: '待处理' }[item.action] || item.action || '处理中' }}
              </NTag>
            </p>
            <p v-if="item.comment">
              处理意见：{{ item.comment }}
            </p>
          </n-timeline-item>
        </n-timeline>

        <template #footer>
          <NSpace>
            <NButton v-if="currentInstance.status === 'running'" type="warning" @click="handleSuspend">
              挂起流程
            </NButton>
            <NButton v-if="currentInstance.status === 'suspended'" type="primary" @click="handleActivate">
              激活流程
            </NButton>
            <NButton v-if="currentInstance.status === 'running'" type="error" @click="handleTerminate">
              终止流程
            </NButton>
          </NSpace>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { NButton, NSpace, NTag } from 'naive-ui'
import { h, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

import ProcessDiagramViewer from '@/components/bpmn/ProcessDiagramViewer.vue'
import { request } from '@/utils'

defineOptions({ name: 'FlowMonitor' })

const route = useRoute()
const message = window.$message

// 统计数据
const statistics = reactive({
  runningInstances: 0,
  pendingTasks: 0,
  todayCompleted: 0,
  timeoutTasks: 0,
})

// 搜索表单
const searchForm = reactive({
  processName: '',
  initiator: '',
  status: null,
  dateRange: null,
  modelKey: null,
})

// 状态选项
const statusOptions = [
  { label: '运行中', value: 'running' },
  { label: '已挂起', value: 'suspended' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已取消', value: 'canceled' },
  { label: '已终止', value: 'terminated' },
]

// 表格数据
const tableData = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

// 表格列定义
const columns = [
  {
    title: '流程名称',
    key: 'processName',
    ellipsis: { tooltip: true },
  },
  {
    title: '发起人',
    key: 'initiatorName',
    width: 100,
  },
  {
    title: '当前节点',
    key: 'currentNode',
    width: 150,
  },
  {
    title: '当前处理人',
    key: 'currentAssignee',
    width: 100,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      const type = getStatusType(row.status)
      const text = getStatusText(row.status)
      return h(NTag, { type }, { default: () => text })
    },
  },
  {
    title: '发起时间',
    key: 'startTime',
    width: 160,
  },
  {
    title: '运行时长',
    key: 'duration',
    width: 100,
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { text: true, type: 'primary', onClick: () => showDetail(row) }, { default: () => '详情' }),
          h(NButton, { text: true, type: 'info', onClick: () => showDiagram(row) }, { default: () => '流程图' }),
        ],
      })
    },
  },
]

// 详情抽屉
const detailDrawerVisible = ref(false)
const currentInstance = ref({})
const approvalHistory = ref([])

// 流程图弹窗
const diagramModalVisible = ref(false)
const currentDiagramInstanceId = ref(null)

// 图表引用
const taskChartRef = ref(null)
const processChartRef = ref(null)
let taskChart = null
let processChart = null

// 图表周期（天数）
const chartPeriod = ref(7)

// 加载统计数据
async function loadStatistics() {
  try {
    const res = await request.get('/api/flow/monitor/statistics')
    if (res.code === 200) {
      Object.assign(statistics, res.data)
    }
  }
  catch (error) {
    console.error('加载统计数据失败', error)
  }
}

// 加载表格数据
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...searchForm,
    }
    const res = await request.get('/api/flow/monitor/instances', { params })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
    }
  }
  catch (error) {
    console.error('加载数据失败', error)
  }
  finally {
    loading.value = false
  }
}

// 加载任务趋势数据
async function loadTaskTrend(days = 7) {
  try {
    const res = await request.get('/api/flow/monitor/taskTrend', { params: { days } })
    if (res.code === 200 && res.data) {
      return res.data
    }
  }
  catch (error) {
    console.error('加载任务趋势数据失败', error)
  }
  return { dates: [], created: [], completed: [] }
}

// 加载流程分布数据
async function loadProcessDistribution() {
  try {
    const res = await request.get('/api/flow/monitor/processDistribution')
    if (res.code === 200 && res.data) {
      return res.data
    }
  }
  catch (error) {
    console.error('加载流程分布数据失败', error)
  }
  return []
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  Object.assign(searchForm, {
    processName: '',
    initiator: '',
    status: null,
    dateRange: null,
    modelKey: null,
  })
  handleSearch()
}

// 分页变化
function handlePageChange(page) {
  pagination.page = page
  loadData()
}

// 每页条数变化
function handlePageSizeChange(pageSize) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

// 显示详情
async function showDetail(row) {
  currentInstance.value = row
  detailDrawerVisible.value = true

  // 加载审批时间轴
  try {
    const res = await request.get(`/api/flow/task/history/${row.id}`)
    if (res.code === 200) {
      approvalHistory.value = res.data || []
    }
  }
  catch (error) {
    console.error('加载审批历史失败', error)
  }
}

// 显示流程图
function showDiagram(row) {
  if (!row.id) {
    message.warning('无法获取流程实例')
    return
  }
  currentDiagramInstanceId.value = row.id
  diagramModalVisible.value = true
}

// 挂起流程
async function handleSuspend() {
  try {
    const res = await request.post(`/api/flow/instance/${currentInstance.value.id}/suspend`)
    if (res.code === 200) {
      message.success('流程已挂起')
      loadData()
    }
  }
  catch (error) {
    message.error('操作失败')
  }
}

// 激活流程
async function handleActivate() {
  try {
    const res = await request.post(`/api/flow/instance/${currentInstance.value.id}/activate`)
    if (res.code === 200) {
      message.success('流程已激活')
      loadData()
    }
  }
  catch (error) {
    message.error('操作失败')
  }
}

// 终止流程
async function handleTerminate() {
  try {
    const res = await request.post(`/api/flow/instance/${currentInstance.value.id}/terminate`)
    if (res.code === 200) {
      message.success('流程已终止')
      detailDrawerVisible.value = false
      loadData()
    }
  }
  catch (error) {
    message.error('操作失败')
  }
}

// 超时任务快捷筛选：点击卡片直接过滤列表
function filterByTimeout() {
  if (searchForm.status === 'timeout') {
    searchForm.status = null
  }
  else {
    searchForm.status = 'timeout'
  }
  pagination.page = 1
  loadData()
}

// 初始化图表（首次挂载）
async function initCharts() {
  await nextTick()
  if (taskChartRef.value) {
    taskChart = echarts.init(taskChartRef.value)
  }
  if (processChartRef.value) {
    processChart = echarts.init(processChartRef.value)
  }
  await refreshCharts()
}

// 刷新图表数据（切换周期时调用）
async function refreshCharts() {
  const [trendData, distributionData] = await Promise.all([
    loadTaskTrend(chartPeriod.value),
    loadProcessDistribution(),
  ])

  // 计算通过率（逐日 completed/created，避免除零）
  const passRateData = trendData.dates.map((_, i) => {
    const created = trendData.created[i] || 0
    const completed = trendData.completed[i] || 0
    return created > 0 ? Math.round((completed / created) * 100) : 0
  })

  if (taskChart) {
    taskChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter(params) {
          let html = `<b>${params[0].axisValue}</b><br/>`
          params.forEach((p) => {
            const suffix = p.seriesName === '通过率' ? '%' : '件'
            html += `${p.marker}${p.seriesName}：<b>${p.value}${suffix}</b><br/>`
          })
          return html
        },
      },
      legend: { data: ['新增流程', '完成流程', '通过率'] },
      grid: { right: 60 },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: trendData.dates.length > 0 ? trendData.dates : Array.from({ length: chartPeriod.value }, (_, i) => `第${i + 1}天`),
      },
      yAxis: [
        { type: 'value', name: '数量（件）', minInterval: 1 },
        { type: 'value', name: '通过率', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
      ],
      series: [
        {
          name: '新增流程',
          type: 'bar',
          data: trendData.created.length > 0 ? trendData.created : new Array(chartPeriod.value).fill(0),
          itemStyle: { color: '#2080f0', borderRadius: [3, 3, 0, 0] },
        },
        {
          name: '完成流程',
          type: 'bar',
          data: trendData.completed.length > 0 ? trendData.completed : new Array(chartPeriod.value).fill(0),
          itemStyle: { color: '#18a058', borderRadius: [3, 3, 0, 0] },
        },
        {
          name: '通过率',
          type: 'line',
          yAxisIndex: 1,
          data: passRateData,
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: '#f0a020', width: 2 },
          itemStyle: { color: '#f0a020' },
          areaStyle: { color: 'rgba(240,160,32,0.08)' },
        },
      ],
    }, true)
  }

  if (processChart) {
    const pieData = distributionData.length > 0 ? distributionData : [{ name: '暂无数据', value: 1 }]
    processChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}件 ({d}%)' },
      legend: { orient: 'vertical', left: 'left', top: 'center' },
      series: [
        {
          name: '流程分布',
          type: 'pie',
          radius: ['40%', '68%'],
          center: ['60%', '50%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, formatter: '{b}\n{d}%' },
          labelLine: { show: true, length: 10, length2: 8 },
          data: pieData,
        },
      ],
    }, true)
  }
}

// 窗口 resize 时自适应图表尺寸
function handleResize() {
  taskChart?.resize()
  processChart?.resize()
}

// 获取状态类型
function getStatusType(status) {
  const map = {
    running: 'info',
    suspended: 'warning',
    approved: 'success',
    rejected: 'error',
    canceled: 'default',
    terminated: 'error',
  }
  return map[status] || 'default'
}

// 获取状态文本
function getStatusText(status) {
  const map = {
    running: '审批中',
    suspended: '已挂起',
    approved: '已通过',
    rejected: '已驳回',
    canceled: '已取消',
    terminated: '已终止',
  }
  return map[status] || status || '未知'
}

// 获取时间线类型
function getTimelineType(action) {
  const map = {
    approve: 'success',
    reject: 'error',
    start: 'success',
    claim: 'info',
    delegate: 'warning',
    withdraw: 'default',
  }
  return map[action] || 'default'
}

onMounted(() => {
  if (route.query.modelKey) {
    searchForm.modelKey = route.query.modelKey
  }
  loadStatistics()
  loadData()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  taskChart?.dispose()
  processChart?.dispose()
})
</script>

<style scoped>
.flow-monitor-page {
  padding: 16px;
}

.stat-cards {
  margin-bottom: 16px;
}

.mt-4 {
  margin-top: 16px;
}

.timeout-card {
  transition: box-shadow 0.2s;
}

.timeout-card:hover {
  box-shadow: 0 2px 12px rgba(208, 48, 80, 0.18);
}

.timeout-active {
  border: 1px solid #d03050 !important;
}

.timeout-hint {
  font-size: 11px;
  color: #d03050;
  margin-left: 6px;
  font-weight: normal;
}
</style>
