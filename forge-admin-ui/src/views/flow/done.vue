<template>
  <div class="flow-page">
    <n-alert v-if="loadError" type="error" class="mb-3" :show-icon="true">
      已办任务加载失败，请重试。
      <template #action>
        <NButton text type="primary" @click="loadData">
          重试
        </NButton>
      </template>
    </n-alert>
    <!-- 任务列表 -->
    <FlowTaskCardList
      v-model:search-value="queryParams.title"
      title="已办"
      :items="dataSource"
      :loading="loading"
      :pagination="pagination"
      :selectable="false"
      row-key="id"
      search-placeholder="搜索任务名称或编号..."
      empty-text="暂无已办任务"
      status-title="审批结果"
      node-title="处理节点"
      user-title="当前处理人"
      @search="handleSearch"
      @refresh="loadData"
      @row-click="openDrawer"
      @update:page="pagination.onChange"
      @update:page-size="pagination.onUpdatePageSize"
    >
      <template #filters>
        <NTreeSelect v-model:value="queryParams.category" placeholder="流程分类" clearable class="category-select" :options="categoryTreeOptions" :default-expand-all="true" @update:value="handleSearch" />
        <n-select v-model:value="queryParams.status" placeholder="审批结果" clearable class="category-select" :options="statusOptions" @update:value="handleSearch" />
        <NButton secondary @click="handleReset">
          重置
        </NButton>
      </template>
      <template #status="{ row }">
        <span class="task-status-pill" :class="getStatusTagClass(row.status)">
          {{ getStatusText(row.status) }}
        </span>
      </template>
      <template #title="{ row }">
        {{ getRowDisplayTitle(row) }}
      </template>
      <template #node="{ row }">
        {{ getTaskDisplayName(row) }}
      </template>
      <template #user="{ row }">
        <span>{{ getTaskHandlerName(row) }}</span>
        <small v-if="row.startUserName">申请人 {{ row.startUserName }}</small>
        <small>{{ row.completeTime || '-' }}</small>
      </template>
      <template #summary="{ row }">
        <FlowTaskBusinessSummary :row="row" />
      </template>
      <template #actions="{ row }">
        <button type="button" class="task-row-link-action" aria-label="查看详情" @click="openDrawer(row)">
          详情
        </button>
        <span class="task-row-action-separator" />
        <button type="button" class="task-row-link-action muted" aria-label="更多操作" @click="openDrawer(row)">
          <i class="i-lucide:more-horizontal" />
        </button>
      </template>
    </FlowTaskCardList>

    <!-- 详情弹窗 -->
    <FlowTaskDetailShell
      v-model:show="showDrawer"
      :title="currentTask ? getRowDisplayTitle(currentTask) : '审批详情'"
      :subtitle="getTaskDisplayName(currentTask, '') ? `处理节点：${getTaskDisplayName(currentTask)}` : ''"
      :status-text="getStatusText(currentTask?.status)"
      :status-class="getStatusTagClass(currentTask?.status)"
      :status-icon="getStatusIcon(currentTask?.status)"
      :records="approvalHistory"
      record-title="审批记录"
      fullscreen
    >
      <template v-if="currentTask">
        <section class="approval-detail-section">
          <div class="approval-section-header">
            <i class="i-material-symbols:info-outline" />
            基本信息
          </div>
          <div class="approval-field-grid">
            <div class="approval-field">
              <span class="approval-label">任务节点</span>
              <span class="approval-value">{{ getTaskDisplayName(currentTask) }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">审批结果</span>
              <span class="approval-value">{{ getStatusText(currentTask.status) }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">流程分类</span>
              <span class="approval-value">{{ getCategoryDisplayName(currentTask) }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">发起人</span>
              <span class="approval-value approval-user-inline">
                <UserAvatar :name="currentTask.startUserName || '未知'" :size="24" />
                {{ currentTask.startUserName || '-' }}
              </span>
            </div>
            <div class="approval-field">
              <span class="approval-label">发起部门</span>
              <span class="approval-value">{{ currentTask.startDeptName || '-' }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">完成时间</span>
              <span class="approval-value">{{ currentTask.completeTime || '-' }}</span>
            </div>
          </div>
        </section>

        <section class="approval-detail-section">
          <div class="approval-section-header">
            <i class="i-material-symbols:rate-review" />
            处理结果
          </div>
          <div class="approval-field-grid">
            <div class="approval-field full">
              <span class="approval-label">审批意见</span>
              <span class="approval-value approval-note">{{ currentTask.comment || '-' }}</span>
            </div>
            <div class="approval-field full">
              <span class="approval-label">审批签名</span>
              <span class="approval-value">
                <SignatureImage :value="currentTask.signature" />
              </span>
            </div>
          </div>
        </section>

        <section class="approval-detail-section">
          <div class="approval-section-header">
            <i class="i-material-symbols:fact-check" />
            表单内容
          </div>
          <FlowReadonlyFormPanel :row="currentTask" source="flowDone" />
        </section>

        <section class="approval-detail-section">
          <n-collapse arrow-placement="right">
            <n-collapse-item title="查看流程图" name="diagram">
              <div class="approval-diagram">
                <DingFlowViewer v-if="currentTask.processInstanceId" :process-instance-id="currentTask.processInstanceId" :compact="true" />
                <n-empty v-else description="暂无流程图" size="small" />
              </div>
            </n-collapse-item>
          </n-collapse>
        </section>
      </template>
    </FlowTaskDetailShell>
  </div>
</template>

<script setup>
import { NButton, NTreeSelect } from 'naive-ui'
import { computed, onMounted, reactive, ref } from 'vue'
import flowApi from '@/api/flow'
import UserAvatar from '@/components/common/UserAvatar.vue'
import DingFlowViewer from '@/components/flow-designer/viewer/DingFlowViewer.vue'
import FlowReadonlyFormPanel from '@/components/flow/FlowReadonlyFormPanel.vue'
import FlowTaskBusinessSummary from '@/components/flow/FlowTaskBusinessSummary.vue'
import FlowTaskCardList from '@/components/flow/FlowTaskCardList.vue'
import FlowTaskDetailShell from '@/components/flow/FlowTaskDetailShell.vue'
import SignatureImage from '@/components/flow/SignatureImage.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { buildFlowCategoryTreeOptions, resolveFlowCategoryLabel } from './utils/categoryOptions'
import { getRowDisplayTitle, getTaskDisplayName, getTaskHandlerName } from './utils/processDisplay'

const userStore = useUserStore()
const { dict, getLabel } = useDict('flow_done_status')
const loading = ref(false)
const loadError = ref(false)
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

const queryParams = reactive({ title: '', category: '', status: null })
const categoryTreeOptions = ref([])

const showDrawer = ref(false)
const currentTask = ref(null)
const approvalHistory = ref([])

const statusOptions = computed(() => toNumberOptions(dict.value.flow_done_status).filter(item => item.value !== 6))

function getStatusTagClass(status) {
  const cls = { 2: 'success', 3: 'error', 4: 'warning', 5: 'info', 6: 'default', 7: 'warning', 8: 'error' }
  return cls[status] || 'default'
}

function getStatusIcon(status) {
  const icons = {
    2: 'i-material-symbols:check-circle',
    3: 'i-material-symbols:cancel',
    4: 'i-material-symbols:keyboard-return',
    5: 'i-material-symbols:person-add',
    7: 'i-material-symbols:undo',
    8: 'i-material-symbols:stop-circle',
  }
  return icons[status] || 'i-material-symbols:task-alt'
}

function getStatusText(status) {
  return getLabel('flow_done_status', status) || '未知'
}

function getCategoryDisplayName(row) {
  return row?.categoryName || resolveFlowCategoryLabel(row?.category, categoryTreeOptions.value, '-') || '-'
}

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

async function openDrawer(row) {
  currentTask.value = row
  approvalHistory.value = []
  showDrawer.value = true
  if (!row.processInstanceId)
    return
  try {
    const res = await flowApi.getProcessHistory(row.processInstanceId)
    if (res.code === 200)
      approvalHistory.value = res.data || []
  }
  catch (e) {
    console.error('加载审批历史失败', e)
  }
}

async function loadData() {
  loading.value = true
  loadError.value = false
  try {
    const res = await flowApi.getDoneTasks({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      userId: userStore.userId,
      title: queryParams.title || undefined,
      category: queryParams.category || undefined,
      status: queryParams.status ?? undefined,
    })
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
    }
    else {
      loadError.value = true
    }
  }
  catch (e) {
    console.error('加载已办任务失败:', e)
    loadError.value = true
  }
  finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await flowApi.getCategoryTreeSelect(false)
    if (res.code === 200 && res.data) {
      categoryTreeOptions.value = buildFlowCategoryTreeOptions(res.data)
    }
  }
  catch (e) {
    console.error('加载分类失败', e)
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  queryParams.title = ''
  queryParams.category = ''
  queryParams.status = null
  pagination.page = 1
  loadData()
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped>
:deep(.n-data-table .n-data-table-th),
:deep(.n-data-table .n-data-table-td) {
  padding: 6px 8px;
}

.flow-page {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
  background: var(--bg-secondary);
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
.title-icon.done {
  background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
}
.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
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
  width: 132px;
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
:deep(.status-tag-mini.success) {
  background: #dcfce7;
  color: #15803d;
}
:deep(.status-tag-mini.error) {
  background: #fee2e2;
  color: #b91c1c;
}
:deep(.status-tag-mini.warning) {
  background: #fef3c7;
  color: #b45309;
}
:deep(.status-tag-mini.info) {
  background: #dbeafe;
  color: #1e40af;
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
.status-dot.success {
  background: #10b981;
}
.status-dot.error {
  background: #ef4444;
}
.status-dot.warning {
  background: #f59e0b;
}
.status-dot.info {
  background: #3b82f6;
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
.status-tag.success {
  background: #dcfce7;
  color: #15803d;
}
.status-tag.error {
  background: #fee2e2;
  color: #b91c1c;
}
.status-tag.warning {
  background: #fef3c7;
  color: #b45309;
}
.status-tag.info {
  background: #dbeafe;
  color: #1e40af;
}
.status-tag.default {
  background: #f1f5f9;
  color: #64748b;
}
.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(100vh - 178px);
  overflow-y: auto;
  padding-bottom: 20px;
  padding: 18px 20px 20px;
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
.signature-value {
  flex: 1;
  display: flex;
  justify-content: flex-end;
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

.flow-task-detail-modal {
  width: min(1080px, calc(100vw - 32px));
}

@media (max-width: 760px) {
  .flow-task-detail-modal {
    width: 100vw;
    height: 100vh;
    margin: 0;
  }

  .drawer-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .drawer-body {
    max-height: calc(100vh - 126px);
    padding: 14px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
