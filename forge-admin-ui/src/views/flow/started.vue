<template>
  <div class="flow-page">
    <!-- 统计卡片 -->
    <FlowStats
      :todo-count="todoCount"
      :done-count="doneCount"
      :started-count="startedCount"
      :cc-count="ccCount"
      :unread-cc="unreadCc"
      active-tab="started"
      @switch="handleSwitch"
    />

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <div class="title-icon started">
            <i class="i-material-symbols:send" />
          </div>
          <h2 class="page-title">
            我发起的
          </h2>
        </div>
        <div class="quick-stats">
          <span v-if="pendingCount > 0" class="stat-item pending">
            <i class="i-material-symbols:schedule" />
            {{ pendingCount }}审批中
          </span>
        </div>
      </div>
      <div class="header-right">
        <n-input v-model:value="queryParams.title" placeholder="搜索流程标题" clearable class="search-input" @keydown.enter="handleSearch">
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-select v-model:value="queryParams.category" placeholder="流程分类" clearable class="category-select" :options="categoryOptions" />
        <NButton type="primary" class="search-btn" @click="handleSearch">
          <i class="i-material-symbols:search mr-2" />查询
        </NButton>
        <NButton class="reset-btn" @click="handleReset">
          <i class="i-material-symbols:refresh mr-2" />重置
        </NButton>
      </div>
    </div>

    <!-- 任务列表 -->
    <div class="table-container">
      <n-data-table :columns="columns" :data="dataSource" :loading="loading" :pagination="pagination" :remote="true" :row-key="row => row.id" :row-props="getRowProps" striped />
    </div>

    <!-- 流程进度抽屉 -->
    <n-drawer v-model:show="showDrawer" :width="720" placement="right">
      <n-drawer-content :native-scrollbar="false" closable>
        <template #header>
          <div class="drawer-header">
            <div class="drawer-title-row">
              <div class="status-dot" :class="getStatusDotClass(currentTask?.status)" />
              <span class="drawer-title">{{ currentTask?.title || '流程详情' }}</span>
            </div>
            <span class="status-tag" :class="getStatusTagClass(currentTask?.status)">{{ statusMap[currentTask?.status]?.text ?? '未知' }}</span>
          </div>
        </template>

        <div v-if="currentTask" class="drawer-body">
          <div class="info-grid">
            <div class="info-card">
              <div class="info-header">
                <i class="i-material-symbols:info-outline" />流程信息
              </div>
              <div class="info-items">
                <div class="info-item">
                  <span class="info-label">当前任务</span><span class="info-value highlight">{{ currentTask.taskName || '已结束' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">流程状态</span><span class="status-tag-mini" :class="getStatusTagClass(currentTask.status)">{{ statusMap[currentTask.status]?.text ?? '未知' }}</span>
                </div>
              </div>
            </div>
            <div class="info-card">
              <div class="info-header">
                <i class="i-material-symbols:person-outline" />处理信息
              </div>
              <div class="info-items">
                <div class="info-item user-item">
                  <span class="info-label">当前处理人</span><div class="user-display">
                    <UserAvatar v-if="currentTask.assigneeName" :name="currentTask.assigneeName" :size="24" /><span class="info-value">{{ currentTask.assigneeName || '-' }}</span>
                  </div>
                </div>
                <div class="info-item">
                  <span class="info-label">发起时间</span><span class="info-value">{{ currentTask.createTime || '-' }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="section">
            <div class="section-header">
              <i class="i-material-symbols:account-tree" />流程进度
            </div>
            <n-collapse>
              <n-collapse-item title="查看流程图" name="diagram">
                <ProcessDiagramViewer v-if="currentTask.processInstanceId" :process-instance-id="currentTask.processInstanceId" :compact="true" />
                <n-empty v-else description="暂无流程图" size="small" />
              </n-collapse-item>
            </n-collapse>
          </div>

          <div class="section">
            <div class="section-header">
              <i class="i-material-symbols:history" />审批进度
            </div>
            <FlowTimeline v-if="approvalHistory.length > 0" :items="approvalHistory" />
            <n-empty v-else description="暂无审批记录" size="small" />
          </div>

          <!-- 撤回操作 -->
          <div v-if="canWithdraw" class="withdraw-section">
            <div class="section-header">
              <i class="i-material-symbols:undo" />撤回申请
            </div>
            <n-input v-model:value="withdrawComment" type="textarea" :rows="2" placeholder="请输入撤回原因（可选）" :maxlength="200" show-count />
            <n-popconfirm @positive-click="submitWithdraw">
              <template #trigger>
                <NButton type="warning" :loading="withdrawLoading" style="margin-top: 12px">
                  <i class="i-material-symbols:undo mr-2" />撤回流程
                </NButton>
              </template>
              确认撤回该流程申请？
            </n-popconfirm>
          </div>
        </div>

        <template #footer>
          <NSpace justify="end">
            <NButton @click="showDrawer = false">
              关闭
            </NButton>
          </NSpace>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { NButton, NSpace } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import flowApi from '@/api/flow'
import ProcessDiagramViewer from '@/components/bpmn/ProcessDiagramViewer.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import FlowStats from '@/components/flow/FlowStats.vue'
import FlowTimeline from '@/components/flow/FlowTimeline.vue'
import { useUserStore } from '@/store'

const userStore = useUserStore()
const loading = ref(false)
const dataSource = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page) => {
    pagination.page = page
    loadData()
  },
  onUpdatePageSize: (size) => {
    pagination.pageSize = size
    pagination.page = 1
    loadData()
  },
})

const queryParams = reactive({ title: '', category: '' })
const categoryOptions = ref([])

const todoCount = ref(0)
const doneCount = ref(0)
const startedCount = ref(0)
const ccCount = ref(0)
const unreadCc = ref(0)
const pendingCount = ref(0)

const showDrawer = ref(false)
const currentTask = ref(null)
const approvalHistory = ref([])

const withdrawComment = ref('')
const withdrawLoading = ref(false)
const canWithdraw = computed(() => currentTask.value && [0, 1].includes(currentTask.value.status))

const statusMap = {
  0: { text: '审批中', type: 'warning' },
  1: { text: '已签收', type: 'info' },
  2: { text: '已通过', type: 'success' },
  3: { text: '已驳回', type: 'error' },
  4: { text: '已转办', type: 'warning' },
  5: { text: '已委派', type: 'info' },
  6: { text: '已撤回', type: 'default' },
}

function getStatusDotClass(status) {
  const cls = { 0: 'warning', 1: 'info', 2: 'success', 3: 'error', 4: 'warning', 5: 'info', 6: 'default' }
  return cls[status] || 'default'
}

function getStatusTagClass(status) {
  const cls = { 0: 'warning', 1: 'info', 2: 'success', 3: 'error', 4: 'warning', 5: 'info', 6: 'default' }
  return cls[status] || 'default'
}

const columns = [
  {
    title: '流程标题',
    key: 'title',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render: row => h('span', { class: 'task-title-link', onClick: () => openDrawer(row) }, row.title || row.taskName),
  },
  { title: '当前任务', key: 'taskName', width: 120 },
  {
    title: '当前处理人',
    key: 'assigneeName',
    width: 120,
    render: row => row.assigneeName
      ? h('div', { class: 'table-user' }, [
          h(UserAvatar, { name: row.assigneeName, size: 24 }),
          h('span', { class: 'user-name-text' }, row.assigneeName),
        ])
      : '-',
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: row => h('span', { class: ['status-tag-mini', getStatusTagClass(row.status)] }, statusMap[row.status]?.text ?? '未知'),
  },
  { title: '发起时间', key: 'createTime', width: 155 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: row => h(NButton, {
      size: 'small',
      type: 'primary',
      onClick: (e) => {
        e.stopPropagation()
        openDrawer(row)
      },
    }, () => '查看进度'),
  },
]

function getRowProps(row) {
  return { style: 'cursor:pointer', onClick: () => openDrawer(row) }
}

async function openDrawer(row) {
  currentTask.value = row
  approvalHistory.value = []
  withdrawComment.value = ''
  showDrawer.value = true
  if (row.processInstanceId) {
    try {
      const res = await flowApi.getProcessHistory(row.processInstanceId)
      if (res.code === 200)
        approvalHistory.value = res.data || []
    }
    catch {
      console.error('加载审批历史失败')
    }
  }
}

async function submitWithdraw() {
  withdrawLoading.value = true
  try {
    const res = await flowApi.withdrawProcess({ processInstanceId: currentTask.value.processInstanceId, userId: userStore.userId, comment: withdrawComment.value || '申请人撤回' })
    if (res.code === 200) {
      window.$message.success('撤回成功')
      showDrawer.value = false
      loadData()
    }
    else { window.$message.error(res.message || '撤回失败') }
  }
  catch {
    window.$message.error('撤回失败')
  }
  finally {
    withdrawLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await flowApi.getStartedTasks({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      userId: userStore.userId,
      title: queryParams.title || undefined,
      category: queryParams.category || undefined,
    })
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
      startedCount.value = res.data.total || 0
      pendingCount.value = dataSource.value.filter(r => [0, 1].includes(r.status)).length
    }
  }
  catch {
    console.error('加载发起的流程失败')
  }
  finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const [todoRes, doneRes, ccRes] = await Promise.all([
      flowApi.getTodoTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
      flowApi.getDoneTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
      flowApi.getMyCc({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
    ])
    todoCount.value = todoRes.code === 200 ? todoRes.data?.total || 0 : 0
    doneCount.value = doneRes.code === 200 ? doneRes.data?.total || 0 : 0
    ccCount.value = ccRes.code === 200 ? ccRes.data?.total || 0 : 0
    if (ccRes.code === 200 && ccRes.data?.records)
      unreadCc.value = ccRes.data.records.filter(r => r.isRead === 0).length
  }
  catch {
    console.error('加载统计数据失败')
  }
}

async function loadCategories() {
  try {
    const res = await flowApi.getEnabledCategories()
    if (res.code === 200 && res.data)
      categoryOptions.value = res.data.map(item => ({ label: item.categoryName, value: item.categoryCode }))
  }
  catch {
    console.error('加载分类失败')
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  queryParams.title = ''
  queryParams.category = ''
  pagination.page = 1
  loadData()
}
function handleSwitch(tab) {
  const routes = { todo: '/flow/todo', done: '/flow/done', started: '/flow/started', cc: '/flow/cc' }
  if (routes[tab])
    window.$router?.push(routes[tab])
}

onMounted(() => {
  loadCategories()
  loadStats()
  loadData()
})
</script>

<style scoped>
.flow-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.page-header {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.title-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}
.title-icon.started {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
}
.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}
.quick-stats {
  display: flex;
  align-items: center;
  gap: 8px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
}
.stat-item.pending {
  background: #fef3c7;
  color: #b45309;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.search-input {
  width: 220px;
}
.category-select {
  width: 140px;
}
.table-container {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  flex: 1;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}
:deep(.task-title-link) {
  color: #0369a1;
  cursor: pointer;
  font-weight: 600;
}
:deep(.task-title-link:hover) {
  text-decoration: underline;
}
:deep(.table-user) {
  display: flex;
  align-items: center;
  gap: 8px;
}
:deep(.user-name-text) {
  font-weight: 500;
  color: #0f172a;
}
:deep(.status-tag-mini) {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}
:deep(.status-tag-mini.warning) {
  background: #fef3c7;
  color: #b45309;
}
:deep(.status-tag-mini.info) {
  background: #dbeafe;
  color: #1e40af;
}
:deep(.status-tag-mini.success) {
  background: #dcfce7;
  color: #15803d;
}
:deep(.status-tag-mini.error) {
  background: #fee2e2;
  color: #b91c1c;
}
:deep(.status-tag-mini.default) {
  background: #f1f5f9;
  color: #64748b;
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.drawer-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.status-dot.warning {
  background: #f59e0b;
}
.status-dot.info {
  background: #3b82f6;
}
.status-dot.success {
  background: #10b981;
}
.status-dot.error {
  background: #ef4444;
}
.status-dot.default {
  background: #94a3b8;
}
.drawer-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}
.status-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}
.status-tag.warning {
  background: #fef3c7;
  color: #b45309;
}
.status-tag.info {
  background: #dbeafe;
  color: #1e40af;
}
.status-tag.success {
  background: #dcfce7;
  color: #15803d;
}
.status-tag.error {
  background: #fee2e2;
  color: #b91c1c;
}
.status-tag.default {
  background: #f1f5f9;
  color: #64748b;
}
.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 20px;
}
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.info-card {
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
}
.info-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 10px;
}
.info-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.info-label {
  font-size: 12px;
  color: #64748b;
}
.info-value {
  font-size: 13px;
  color: #0f172a;
  font-weight: 500;
}
.info-value.highlight {
  color: #0369a1;
  font-weight: 600;
}
.user-item {
  align-items: flex-start;
}
.user-display {
  display: flex;
  align-items: center;
  gap: 8px;
}
.section {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  padding: 16px;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 12px;
}
.withdraw-section {
  background: #fef3c7;
  border-radius: 10px;
  border: 1px solid #fcd34d;
  padding: 16px;
}
</style>
