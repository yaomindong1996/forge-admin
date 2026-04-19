<template>
  <div class="done-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          我的已办
        </h2>
        <NBadge :value="pagination.itemCount" :max="99" type="success" />
      </div>
      <div class="header-right">
        <n-input
          v-model:value="queryParams.title"
          placeholder="搜索任务标题"
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
        <NButton type="primary" @click="handleSearch">
          查询
        </NButton>
        <NButton @click="handleReset">
          重置
        </NButton>
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

    <!-- 详情抽屉 -->
    <n-drawer
      v-model:show="showDrawer"
      :width="720"
      placement="right"
    >
      <n-drawer-content :native-scrollbar="false" closable>
        <template #header>
          <div class="drawer-header">
            <span class="drawer-title">{{ currentTask?.title || '审批详情' }}</span>
            <NTag
              v-if="currentTask"
              :type="statusMap[currentTask.status]?.type ?? 'default'"
              size="small"
              style="margin-left: 8px"
            >
              {{ statusMap[currentTask.status]?.text ?? '未知' }}
            </NTag>
          </div>
        </template>

        <div v-if="currentTask" class="drawer-body">
          <!-- 基本信息 -->
          <n-card class="info-card" size="small">
            <n-descriptions :column="2" label-placement="left" size="small">
              <n-descriptions-item label="任务节点">
                <NTag type="primary" size="small">
                  {{ currentTask.taskName }}
                </NTag>
              </n-descriptions-item>
              <n-descriptions-item label="审批结果">
                <NTag :type="statusMap[currentTask.status]?.type ?? 'default'" size="small">
                  {{ statusMap[currentTask.status]?.text ?? '未知' }}
                </NTag>
              </n-descriptions-item>
              <n-descriptions-item label="发起人">
                <div class="user-info">
                  <NAvatar :size="20" round style="background: #18a058">
                    {{ (currentTask.startUserName || '?')[0] }}
                  </NAvatar>
                  <span style="margin-left: 6px">{{ currentTask.startUserName }}</span>
                </div>
              </n-descriptions-item>
              <n-descriptions-item label="发起部门">
                {{ currentTask.startDeptName || '-' }}
              </n-descriptions-item>
              <n-descriptions-item label="完成时间">
                {{ currentTask.completeTime || '-' }}
              </n-descriptions-item>
              <n-descriptions-item label="审批意见">
                {{ currentTask.comment || '-' }}
              </n-descriptions-item>
            </n-descriptions>
          </n-card>

          <!-- 流程进度 -->
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
                      <NAvatar :size="18" round style="background: #2080f0">
                        {{ (item.assigneeName || '?')[0] }}
                      </NAvatar>
                      <span class="user-name">{{ item.assigneeName || '-' }}</span>
                      <NTag :type="getActionType(item.action)" size="small">
                        {{ getActionText(item.action) }}
                      </NTag>
                    </div>
                    <div v-if="item.comment" class="history-comment">
                      {{ item.comment }}
                    </div>
                  </div>
                </n-timeline-item>
              </n-timeline>
            </div>
            <n-empty v-else description="暂无审批记录" size="small" />
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
import { NAvatar, NBadge, NButton, NSpace, NTag } from 'naive-ui'
import { h, onMounted, reactive, ref } from 'vue'
import flowApi from '@/api/flow'
import ProcessDiagramViewer from '@/components/bpmn/ProcessDiagramViewer.vue'
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
  onChange: (page) => { pagination.page = page; loadData() },
  onUpdatePageSize: (size) => { pagination.pageSize = size; pagination.page = 1; loadData() },
})

const queryParams = reactive({ title: '', category: '' })
const categoryOptions = ref([])

// 抽屉状态
const showDrawer = ref(false)
const currentTask = ref(null)
const approvalHistory = ref([])

// 状态映射
const statusMap = {
  2: { text: '已通过', type: 'success' },
  3: { text: '已驳回', type: 'error' },
  4: { text: '已转办', type: 'warning' },
  5: { text: '已委派', type: 'info' },
  6: { text: '已撤回', type: 'default' },
}

// 审批动作映射
function getActionType(action) {
  const m = { approve: 'success', reject: 'error', delegate: 'warning', claim: 'info', start: 'primary', withdraw: 'default' }
  return m[action] || 'default'
}
function getActionText(action) {
  const m = { approve: '同意', reject: '驳回', delegate: '转办', claim: '签收', start: '发起', withdraw: '撤回' }
  return m[action] || action || '操作'
}
function getCommentType(action) {
  if (action === 'approve' || action === 'start')
    return 'success'
  if (action === 'reject')
    return 'error'
  return 'default'
}

// 表格列
const columns = [
  {
    title: '任务标题',
    key: 'title',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render: row => h('span', {
      class: 'task-title-link',
      onClick: () => openDrawer(row),
    }, row.title || row.taskName),
  },
  { title: '任务名称', key: 'taskName', width: 120 },
  {
    title: '发起人',
    key: 'startUserName',
    width: 100,
    render: row => h('div', { class: 'flex items-center gap-4' }, [
      h(NAvatar, { size: 20, round: true, style: 'background:#18a058;font-size:11px' }, () => (row.startUserName || '?')[0]),
      h('span', { style: 'margin-left:6px' }, row.startUserName || '-'),
    ]),
  },
  { title: '发起部门', key: 'startDeptName', width: 120, ellipsis: { tooltip: true } },
  {
    title: '审批结果',
    key: 'status',
    width: 90,
    render: (row) => {
      const status = statusMap[row.status] || { text: '未知', type: 'default' }
      return h(NTag, { type: status.type, size: 'small', round: true }, () => status.text)
    },
  },
  { title: '审批意见', key: 'comment', width: 150, ellipsis: { tooltip: true } },
  { title: '完成时间', key: 'completeTime', width: 155 },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right',
    render: row => h(NButton, {
      size: 'small',
      type: 'primary',
      onClick: (e) => { e.stopPropagation(); openDrawer(row) },
    }, () => '查看详情'),
  },
]

// 行属性：点击行打开抽屉
function getRowProps(row) {
  return {
    style: 'cursor:pointer',
    onClick: () => openDrawer(row),
  }
}

// 打开抽屉
async function openDrawer(row) {
  currentTask.value = row
  approvalHistory.value = []
  showDrawer.value = true
  if (row.processInstanceId) {
    try {
      const res = await flowApi.getProcessHistory(row.processInstanceId)
      if (res.code === 200)
        approvalHistory.value = res.data || []
    }
    catch (e) {
      console.error('加载审批历史失败', e)
    }
  }
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await flowApi.getDoneTasks({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      userId: userStore.userId,
      title: queryParams.title || undefined,
      category: queryParams.category || undefined,
    })
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
    }
  }
  catch (e) {
    console.error('加载已办任务失败:', e)
  }
  finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await flowApi.getEnabledCategories()
    if (res.code === 200 && res.data) {
      categoryOptions.value = res.data.map(item => ({ label: item.categoryName, value: item.categoryCode }))
    }
  }
  catch (e) {}
}

function handleSearch() { pagination.page = 1; loadData() }
function handleReset() { queryParams.title = ''; queryParams.category = ''; pagination.page = 1; loadData() }

onMounted(() => { loadCategories(); loadData() })
</script>

<style scoped>
.done-page {
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
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
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
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
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

.user-info {
  display: flex;
  align-items: center;
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

.text-primary {
  color: #2080f0;
}

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
</style>
