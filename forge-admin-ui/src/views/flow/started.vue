<template>
  <div class="started-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">我发起的</h2>
        <n-badge :value="pagination.itemCount" :max="99" type="info" />
      </div>
      <div class="header-right">
        <n-input
          v-model:value="queryParams.title"
          placeholder="搜索流程标题"
          clearable
          style="width: 220px"
          @keydown.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-material-symbols:search text-gray-400" />
          </template>
        </n-input>
        <n-select
          v-model:value="queryParams.category"
          placeholder="所有分类"
          clearable
          style="width: 140px"
          :options="categoryOptions"
        />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="handleReset">重置</n-button>
      </div>
    </div>

    <!-- 任务列表 -->
    <div class="table-container">
      <n-data-table
        :columns="columns"
        :data="dataSource"
        :loading="loading"
        :pagination="pagination"
        :remote="true"
        :row-key="row => row.id"
        :row-props="getRowProps"
        striped
      />
    </div>

    <!-- 流程进度抽屉 -->
    <n-drawer
      v-model:show="showDrawer"
      :width="720"
      placement="right"
    >
      <n-drawer-content :native-scrollbar="false" closable>
        <template #header>
          <div class="drawer-header">
            <span class="drawer-title">{{ currentTask?.title || '流程详情' }}</span>
            <n-tag
              v-if="currentTask"
              :type="statusMap[currentTask.status]?.type ?? 'default'"
              size="small"
              style="margin-left: 8px"
            >
              {{ statusMap[currentTask.status]?.text ?? '未知' }}
            </n-tag>
          </div>
        </template>

        <div v-if="currentTask" class="drawer-body">
          <!-- 基本信息 -->
          <n-card class="info-card" size="small">
            <n-descriptions :column="2" label-placement="left" size="small">
              <n-descriptions-item label="当前任务">
                <n-tag type="primary" size="small">{{ currentTask.taskName || '已结束' }}</n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="流程状态">
                <n-tag :type="statusMap[currentTask.status]?.type ?? 'default'" size="small">
                  {{ statusMap[currentTask.status]?.text ?? '未知' }}
                </n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="当前处理人">{{ currentTask.assigneeName || '-' }}</n-descriptions-item>
              <n-descriptions-item label="发起时间">{{ currentTask.createTime || '-' }}</n-descriptions-item>
            </n-descriptions>
          </n-card>

          <!-- 流程进度图 -->
          <div class="section">
            <div class="section-title">
              <i class="i-material-symbols:account-tree text-primary" />
              流程进度
            </div>
            <div class="diagram-container">
              <n-collapse>
                <n-collapse-item title="查看流程图" name="diagram">
                  <ProcessDiagramViewer
                    v-if="currentTask.processInstanceId"
                    :process-instance-id="currentTask.processInstanceId"
                    :compact="true"
                  />
                  <n-empty v-else description="暂无流程图" size="small" />
                </n-collapse-item>
              </n-collapse>
            </div>
          </div>

          <!-- 审批历史 -->
          <div class="section">
            <div class="section-title">
              <i class="i-material-symbols:history text-primary" />
              审批进度
            </div>
            <div v-if="approvalHistory.length > 0">
              <n-timeline>
                <n-timeline-item
                  v-for="(item, index) in approvalHistory"
                  :key="index"
                  :type="getCommentType(item.action)"
                  :title="item.taskName"
                  :time="item.completeTime || item.createTime"
                >
                  <div class="history-item">
                    <div class="history-user">
                      <n-avatar :size="18" round style="background: #2080f0">
                        {{ (item.assigneeName || '?')[0] }}
                      </n-avatar>
                      <span class="user-name">{{ item.assigneeName || '-' }}</span>
                      <n-tag :type="getActionType(item.action)" size="small">
                        {{ getActionText(item.action) }}
                      </n-tag>
                    </div>
                    <div v-if="item.comment" class="history-comment">{{ item.comment }}</div>
                  </div>
                </n-timeline-item>
              </n-timeline>
            </div>
            <n-empty v-else description="暂无审批记录" size="small" />
          </div>

          <!-- 撤回操作（仅流程在审批中时展示） -->
          <div v-if="canWithdraw" class="section withdraw-section">
            <div class="section-title">
              <i class="i-material-symbols:undo text-primary" />
              撤回申请
            </div>
            <n-input
              v-model:value="withdrawComment"
              type="textarea"
              :rows="2"
              placeholder="请输入撤回原因（可选）"
              :maxlength="200"
              show-count
            />
            <n-popconfirm
              @positive-click="submitWithdraw"
              positive-text="确认撤回"
              negative-text="取消"
            >
              <template #trigger>
                <n-button type="warning" :loading="withdrawLoading" style="margin-top: 12px">
                  <template #icon>
                    <i class="i-material-symbols:undo" />
                  </template>
                  撤回流程
                </n-button>
              </template>
              确认撤回该流程申请？
            </n-popconfirm>
          </div>
        </div>

        <template #footer>
          <n-space justify="end">
            <n-button @click="showDrawer = false">关闭</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h, onMounted } from 'vue'
import { NTag, NButton, NSpace, NBadge, NAvatar } from 'naive-ui'
import flowApi from '@/api/flow'
import { useUserStore } from '@/store'
import ProcessDiagramViewer from '@/components/bpmn/ProcessDiagramViewer.vue'

const userStore = useUserStore()
const loading = ref(false)
const dataSource = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page) => { pagination.page = page; loadData() },
  onUpdatePageSize: (size) => { pagination.pageSize = size; pagination.page = 1; loadData() }
})

const queryParams = reactive({ title: '', category: '' })
const categoryOptions = ref([])

// 抽屉状态
const showDrawer = ref(false)
const currentTask = ref(null)
const approvalHistory = ref([])

// 撤回
const withdrawComment = ref('')
const withdrawLoading = ref(false)
const canWithdraw = computed(() => currentTask.value && [0, 1].includes(currentTask.value.status))

// 状态映射
const statusMap = {
  0: { text: '审批中', type: 'warning' },
  1: { text: '已签收', type: 'info' },
  2: { text: '已通过', type: 'success' },
  3: { text: '已驳回', type: 'error' },
  4: { text: '已转办', type: 'warning' },
  5: { text: '已委派', type: 'info' },
  6: { text: '已撤回', type: 'default' }
}

function getActionType(action) {
  const m = { approve: 'success', reject: 'error', delegate: 'warning', claim: 'info', start: 'primary', withdraw: 'default' }
  return m[action] || 'default'
}
function getActionText(action) {
  const m = { approve: '同意', reject: '驳回', delegate: '转办', claim: '签收', start: '发起', withdraw: '撤回' }
  return m[action] || action || '操作'
}
function getCommentType(action) {
  if (action === 'approve' || action === 'start') return 'success'
  if (action === 'reject') return 'error'
  return 'default'
}

// 表格列
const columns = [
  {
    title: '流程标题',
    key: 'title',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render: (row) => h('span', {
      class: 'task-title-link',
      onClick: () => openDrawer(row)
    }, row.title || row.taskName)
  },
  { title: '当前任务', key: 'taskName', width: 120 },
  { title: '当前处理人', key: 'assigneeName', width: 100 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) => {
      const status = statusMap[row.status] || { text: '未知', type: 'default' }
      return h(NTag, { type: status.type, size: 'small', round: true }, () => status.text)
    }
  },
  { title: '发起时间', key: 'createTime', width: 155 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: (row) => h(NButton, {
      size: 'small',
      type: 'primary',
      onClick: (e) => { e.stopPropagation(); openDrawer(row) }
    }, () => '查看进度')
  }
]

function getRowProps(row) {
  return {
    style: 'cursor:pointer',
    onClick: () => openDrawer(row)
  }
}

async function openDrawer(row) {
  currentTask.value = row
  approvalHistory.value = []
  withdrawComment.value = ''
  showDrawer.value = true
  if (row.processInstanceId) {
    try {
      const res = await flowApi.getProcessHistory(row.processInstanceId)
      if (res.code === 200) approvalHistory.value = res.data || []
    } catch (e) {
      console.error('加载审批历史失败', e)
    }
  }
}

async function submitWithdraw() {
  withdrawLoading.value = true
  try {
    const res = await flowApi.withdrawProcess({
      processInstanceId: currentTask.value.processInstanceId,
      userId: userStore.userId,
      comment: withdrawComment.value || '申请人撤回'
    })
    if (res.code === 200) {
      window.$message.success('撤回成功')
      showDrawer.value = false
      loadData()
    } else {
      window.$message.error(res.message || '撤回失败')
    }
  } catch (e) {
    window.$message.error('撤回失败')
  } finally {
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
      category: queryParams.category || undefined
    })
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
    }
  } catch (e) {
    console.error('加载发起的流程失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await flowApi.getEnabledCategories()
    if (res.code === 200 && res.data) {
      categoryOptions.value = res.data.map(item => ({ label: item.categoryName, value: item.categoryCode }))
    }
  } catch (e) {}
}

function handleSearch() { pagination.page = 1; loadData() }
function handleReset() { queryParams.title = ''; queryParams.category = ''; pagination.page = 1; loadData() }

onMounted(() => { loadCategories(); loadData() })
</script>

<style scoped>
.started-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-container {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  flex: 1;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
}

:deep(.task-title-link) {
  color: #2080f0;
  cursor: pointer;
  font-weight: 500;
}
:deep(.task-title-link:hover) {
  text-decoration: underline;
}

.drawer-header {
  display: flex;
  align-items: center;
}
.drawer-title {
  font-size: 16px;
  font-weight: 600;
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 20px;
}

.info-card {
  border-radius: 8px;
}

.section {
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  padding: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.text-primary { color: #2080f0; }

.history-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.history-user {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.user-name {
  font-weight: 500;
  color: #333;
}
.history-comment {
  font-size: 13px;
  color: #666;
  background: #fff;
  border-radius: 4px;
  padding: 6px 10px;
  border: 1px solid #e8e8e8;
  margin-top: 4px;
}

.withdraw-section {
  background: #fff;
  border: 1px solid #e8e8e8;
}
</style>
