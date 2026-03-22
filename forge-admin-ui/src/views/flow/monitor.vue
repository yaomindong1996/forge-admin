<template>
  <div class="flow-monitor-page">
    <!-- 统计卡片 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" class="stat-cards">
      <n-gi>
        <n-card size="small">
          <n-statistic label="运行中流程" :value="statistics.runningInstances">
            <template #prefix>
              <i class="i-mdi:play-circle text-blue-500"></i>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small">
          <n-statistic label="待办任务" :value="statistics.pendingTasks">
            <template #prefix>
              <i class="i-mdi:clipboard-list text-orange-500"></i>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small">
          <n-statistic label="今日完成" :value="statistics.todayCompleted">
            <template #prefix>
              <i class="i-mdi:check-circle text-green-500"></i>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small">
          <n-statistic label="超时任务" :value="statistics.timeoutTasks">
            <template #prefix>
              <i class="i-mdi:alert-circle text-red-500"></i>
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
          <n-space>
            <n-button type="primary" @click="handleSearch">
              <template #icon><i class="i-mdi:magnify" /></template>
              查询
            </n-button>
            <n-button @click="handleReset">
              <template #icon><i class="i-mdi:refresh" /></template>
              重置
            </n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-card>

    <!-- 流程实例列表 -->
    <n-card class="mt-4" title="流程实例监控">
      <template #header-extra>
        <n-space>
          <n-button @click="loadData">
            <template #icon><i class="i-mdi:refresh" /></template>
            刷新
          </n-button>
        </n-space>
      </template>
      
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
        @update:page="handlePageChange"
      />
    </n-card>

    <!-- 任务统计图表 -->
    <n-grid :cols="2" :x-gap="16" class="mt-4">
      <n-gi>
        <n-card title="任务处理趋势">
          <div ref="taskChartRef" style="height: 300px"></div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="流程分布统计">
          <div ref="processChartRef" style="height: 300px"></div>
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
          <n-descriptions-item label="流程名称">{{ currentInstance.processName }}</n-descriptions-item>
          <n-descriptions-item label="流程状态">
            <n-tag :type="getStatusType(currentInstance.status)">
              {{ getStatusText(currentInstance.status) }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="发起人">{{ currentInstance.initiatorName }}</n-descriptions-item>
          <n-descriptions-item label="发起时间">{{ currentInstance.startTime }}</n-descriptions-item>
          <n-descriptions-item label="当前节点">{{ currentInstance.currentNode }}</n-descriptions-item>
          <n-descriptions-item label="处理人">{{ currentInstance.currentAssignee }}</n-descriptions-item>
        </n-descriptions>

        <n-divider>处理历史</n-divider>
        
        <n-timeline>
          <n-timeline-item
            v-for="(item, index) in approvalHistory"
            :key="index"
            :type="getTimelineType(item.status)"
            :title="item.taskName"
            :time="item.endTime"
          >
            <p>处理人：{{ item.assigneeName }}</p>
            <p>处理结果：{{ item.result || '处理中' }}</p>
            <p v-if="item.comment">处理意见：{{ item.comment }}</p>
          </n-timeline-item>
        </n-timeline>

        <template #footer>
          <n-space>
            <n-button v-if="currentInstance.status === 'running'" type="warning" @click="handleSuspend">
              挂起流程
            </n-button>
            <n-button v-if="currentInstance.status === 'suspended'" type="primary" @click="handleActivate">
              激活流程
            </n-button>
            <n-button v-if="currentInstance.status === 'running'" type="error" @click="handleTerminate">
              终止流程
            </n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { request } from '@/utils'
import * as echarts from 'echarts'
import ProcessDiagramViewer from '@/components/bpmn/ProcessDiagramViewer.vue'

defineOptions({ name: 'FlowMonitor' })

const route = useRoute()
const message = window.$message

// 统计数据
const statistics = reactive({
  runningInstances: 0,
  pendingTasks: 0,
  todayCompleted: 0,
  timeoutTasks: 0
})

// 搜索表单
const searchForm = reactive({
  processName: '',
  initiator: '',
  status: null,
  dateRange: null,
  modelKey: null
})

// 状态选项
const statusOptions = [
  { label: '运行中', value: 'running' },
  { label: '已挂起', value: 'suspended' },
  { label: '已完成', value: 'completed' },
  { label: '已终止', value: 'terminated' }
]

// 表格数据
const tableData = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

// 表格列定义
const columns = [
  {
    title: '流程名称',
    key: 'processName',
    ellipsis: { tooltip: true }
  },
  {
    title: '发起人',
    key: 'initiatorName',
    width: 100
  },
  {
    title: '当前节点',
    key: 'currentNode',
    width: 150
  },
  {
    title: '当前处理人',
    key: 'currentAssignee',
    width: 100
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      const type = getStatusType(row.status)
      const text = getStatusText(row.status)
      return h(NTag, { type }, { default: () => text })
    }
  },
  {
    title: '发起时间',
    key: 'startTime',
    width: 160
  },
  {
    title: '运行时长',
    key: 'duration',
    width: 100
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { text: true, type: 'primary', onClick: () => showDetail(row) }, { default: () => '详情' }),
          h(NButton, { text: true, type: 'info', onClick: () => showDiagram(row) }, { default: () => '流程图' })
        ]
      })
    }
  }
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

// 加载统计数据
async function loadStatistics() {
  try {
    const res = await request.get('/api/flow/monitor/statistics')
    if (res.code === 200) {
      Object.assign(statistics, res.data)
    }
  } catch (error) {
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
      ...searchForm
    }
    const res = await request.get('/api/flow/monitor/instances', { params })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
    }
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
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
    modelKey: null
  })
  handleSearch()
}

// 分页变化
function handlePageChange(page) {
  pagination.page = page
  loadData()
}

// 显示详情
async function showDetail(row) {
  currentInstance.value = row
  detailDrawerVisible.value = true
  
  // 加载处理历史
  try {
    const res = await request.get(`/api/flow/comment/process/${row.id}`)
    if (res.code === 200) {
      approvalHistory.value = res.data || []
    }
  } catch (error) {
    console.error('加载处理历史失败', error)
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
  } catch (error) {
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
  } catch (error) {
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
  } catch (error) {
    message.error('操作失败')
  }
}

// 初始化图表
function initCharts() {
  nextTick(() => {
    // 任务处理趋势图
    if (taskChartRef.value) {
      taskChart = echarts.init(taskChartRef.value)
      taskChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['新增任务', '完成任务'] },
        xAxis: {
          type: 'category',
          data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        },
        yAxis: { type: 'value' },
        series: [
          { name: '新增任务', type: 'line', data: [120, 132, 101, 134, 90, 230, 210] },
          { name: '完成任务', type: 'line', data: [100, 112, 91, 124, 80, 200, 180] }
        ]
      })
    }

    // 流程分布统计图
    if (processChartRef.value) {
      processChart = echarts.init(processChartRef.value)
      processChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left' },
        series: [
          {
            name: '流程分布',
            type: 'pie',
            radius: '50%',
            data: [
              { value: 1048, name: '请假流程' },
              { value: 735, name: '报销流程' },
              { value: 580, name: '采购流程' },
              { value: 484, name: '合同审批' },
              { value: 300, name: '其他' }
            ]
          }
        ]
      })
    }
  })
}

// 获取状态类型
function getStatusType(status) {
  const map = {
    running: 'info',
    suspended: 'warning',
    completed: 'success',
    terminated: 'error'
  }
  return map[status] || 'default'
}

// 获取状态文本
function getStatusText(status) {
  const map = {
    running: '运行中',
    suspended: '已挂起',
    completed: '已完成',
    terminated: '已终止'
  }
  return map[status] || status
}

// 获取时间线类型
function getTimelineType(status) {
  const map = {
    approved: 'success',
    rejected: 'error',
    pending: 'info'
  }
  return map[status] || 'default'
}

// 引入必要的组件
import { h } from 'vue'
import { NTag, NButton, NSpace } from 'naive-ui'

onMounted(() => {
  // 处理从模型页面跳转过来的 modelKey 参数
  if (route.query.modelKey) {
    searchForm.modelKey = route.query.modelKey
  }
  loadStatistics()
  loadData()
  initCharts()
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
</style>