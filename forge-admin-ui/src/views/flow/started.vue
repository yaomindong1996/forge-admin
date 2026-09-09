<template>
  <div class="flow-page">
    <n-alert v-if="loadError" type="error" class="mb-3" :show-icon="true">
      发起流程加载失败，请重试。
      <template #action>
        <NButton text type="primary" @click="loadData">
          重试
        </NButton>
      </template>
    </n-alert>
    <!-- 任务列表 -->
    <FlowTaskCardList
      v-model:search-value="queryParams.title"
      title="我发起的"
      :items="dataSource"
      :loading="loading"
      :pagination="pagination"
      :selectable="false"
      row-key="id"
      search-placeholder="搜索流程名称或编号..."
      empty-text="暂无发起的流程"
      status-title="当前节点状态"
      node-title="当前任务"
      user-title="处理人"
      @search="handleSearch"
      @refresh="loadData"
      @row-click="openDrawer"
      @update:page="pagination.onChange"
      @update:page-size="pagination.onUpdatePageSize"
    >
      <template #filters>
        <NTreeSelect v-model:value="queryParams.category" placeholder="流程分类" clearable class="category-select" :options="categoryTreeOptions" :default-expand-all="true" @update:value="handleSearch" />
        <n-select v-model:value="queryParams.status" placeholder="当前节点状态" clearable class="category-select" :options="statusOptions" @update:value="handleSearch" />
        <NButton secondary @click="handleReset">
          重置
        </NButton>
      </template>
      <template #batch-actions>
        <span v-if="pendingCount > 0" class="task-list-hint pending">
          <i class="i-material-symbols:schedule" />
          {{ pendingCount }} 审批中
        </span>
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
        {{ getTaskDisplayName(row, '已结束') }}
      </template>
      <template #user="{ row }">
        <span>{{ row.assigneeName || '-' }}</span>
        <small>{{ row.createTime || '-' }}</small>
      </template>
      <template #summary="{ row }">
        <FlowTaskBusinessSummary :row="row" />
      </template>
      <template #actions="{ row }">
        <button type="button" class="task-row-link-action" aria-label="查看进度" @click="openDrawer(row)">
          进度
        </button>
        <span class="task-row-action-separator" />
        <button type="button" class="task-row-link-action muted" aria-label="更多操作" @click="openDrawer(row)">
          <i class="i-lucide:more-horizontal" />
        </button>
      </template>
    </FlowTaskCardList>

    <!-- 流程进度弹窗 -->
    <FlowTaskDetailShell
      v-model:show="showDrawer"
      :busy="withdrawLoading"
      :title="currentTask ? getRowDisplayTitle(currentTask) : '流程详情'"
      :subtitle="getTaskDisplayName(currentTask, '') ? `当前任务：${getTaskDisplayName(currentTask)}` : '流程已结束'"
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
              <span class="approval-label">当前任务</span>
              <span class="approval-value">{{ getTaskDisplayName(currentTask, '已结束') }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">当前节点状态</span>
              <span class="approval-value">{{ getStatusText(currentTask.status) }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">流程分类</span>
              <span class="approval-value">{{ getCategoryDisplayName(currentTask) }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">当前处理人</span>
              <span class="approval-value approval-user-inline">
                <UserAvatar v-if="currentTask.assigneeName" :name="currentTask.assigneeName" :size="24" />
                {{ currentTask.assigneeName || '-' }}
              </span>
            </div>
            <div class="approval-field">
              <span class="approval-label">发起时间</span>
              <span class="approval-value">{{ currentTask.createTime || '-' }}</span>
            </div>
          </div>
        </section>

        <section v-if="canReassign" class="approval-detail-section">
          <div class="approval-warning-section">
            <div class="approval-section-header">
              <i class="i-material-symbols:person-edit-outline" />
              更换当前审批人
            </div>
            <p class="approval-reassign-tip">
              审批人休假或长期未处理时，可由流程发起人将当前任务改派给其他人员。
            </p>
            <NButton type="primary" secondary :disabled="withdrawLoading" @click="openReassignModal">
              选择新审批人
            </NButton>
            <NButton type="warning" secondary :disabled="signLoading" @click="openSignModal('add')">
              动态加签
            </NButton>
            <NButton tertiary :disabled="signLoading" @click="openSignModal('reduce')">
              动态减签
            </NButton>
          </div>
        </section>

        <section v-if="signRelations.length" class="approval-detail-section">
          <div class="approval-section-header">
            <i class="i-lucide:git-branch" />
            动态加签记录
          </div>
          <n-list bordered>
            <n-list-item v-for="relation in signRelations" :key="relation.id || `${relation.targetUserId}-${relation.createTime}`">
              <div class="flex items-center justify-between gap-3">
                <div>
                  <div class="font-medium">
                    {{ relation.targetUserId || '-' }}
                  </div>
                  <div class="text-xs text-gray-500">
                    {{ getLabel('flow_task_sign_mode', relation.signMode) || relation.signMode || '-' }}
                    · {{ relation.reason || '未填写原因' }}
                  </div>
                </div>
                <n-tag size="small" :type="relation.status === 1 ? 'success' : 'default'">
                  {{ getLabel('flow_task_sign_relation_status', relation.status === 1 ? 'ACTIVE' : 'REVOKED') || (relation.status === 1 ? '有效' : '已撤回') }}
                </n-tag>
              </div>
            </n-list-item>
          </n-list>
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

        <section v-if="canWithdraw" class="approval-detail-section">
          <div class="approval-warning-section">
            <div class="approval-section-header">
              <i class="i-material-symbols:undo" />
              撤回申请
            </div>
            <n-input v-model:value="withdrawComment" type="textarea" :rows="3" placeholder="请输入撤回原因（可选）" :maxlength="200" show-count />
            <div class="approval-action-buttons">
              <n-popconfirm @positive-click="submitWithdraw">
                <template #trigger>
                  <NButton type="warning" :loading="withdrawLoading">
                    <i class="i-material-symbols:undo mr-2" />
                    撤回流程
                  </NButton>
                </template>
                确认撤回该流程申请？
              </n-popconfirm>
            </div>
          </div>
        </section>
      </template>
    </FlowTaskDetailShell>

    <n-modal v-model:show="reassignVisible" preset="card" title="更换当前审批人" style="width: 480px" :mask-closable="false">
      <n-form label-placement="top">
        <n-form-item label="新审批人" required>
          <UserSelectPicker
            v-model="reassignUserId"
            v-model:label-value="reassignUserName"
            title="选择新审批人"
            placeholder="请选择新审批人"
          />
        </n-form-item>
        <n-form-item label="改派原因">
          <n-input v-model:value="reassignReason" type="textarea" :rows="3" :maxlength="200" show-count placeholder="例如：原审批人休假" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <NButton :disabled="reassignLoading" @click="reassignVisible = false">
            取消
          </NButton>
          <NButton type="primary" :loading="reassignLoading" @click="submitReassign">
            确认改派
          </NButton>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="signVisible" preset="card" title="动态加签/减签" style="width: 480px" :mask-closable="false">
      <n-form label-placement="top">
        <n-form-item label="操作" required>
          <n-radio-group v-model:value="signAction">
            <n-radio-button value="add">
              加签
            </n-radio-button>
            <n-radio-button value="reduce">
              减签
            </n-radio-button>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="加签模式" required>
          <n-select v-model:value="signMode" :options="signModeOptions" />
          <n-alert type="info" :show-icon="false" class="mt-2" :bordered="false">
            当前仅支持并行加签；前加签、后加签待流程编排能力完成后开放。
          </n-alert>
        </n-form-item>
        <n-form-item label="目标用户" required>
          <UserSelectPicker
            v-model="signUserId"
            v-model:label-value="signUserName"
            title="选择目标用户"
            placeholder="请选择目标用户"
          />
        </n-form-item>
        <n-form-item label="操作原因">
          <n-input v-model:value="signReason" type="textarea" :rows="3" :maxlength="200" show-count placeholder="请输入操作原因" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <NButton :disabled="signLoading" @click="signVisible = false">
            取消
          </NButton>
          <NButton type="primary" :loading="signLoading" @click="submitSign">
            确认{{ signAction === 'add' ? '加签' : '减签' }}
          </NButton>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NTreeSelect } from 'naive-ui'
import { computed, onMounted, reactive, ref } from 'vue'
import flowApi from '@/api/flow'
import UserAvatar from '@/components/common/UserAvatar.vue'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import DingFlowViewer from '@/components/flow-designer/viewer/DingFlowViewer.vue'
import FlowTaskBusinessSummary from '@/components/flow/FlowTaskBusinessSummary.vue'
import FlowTaskCardList from '@/components/flow/FlowTaskCardList.vue'
import FlowTaskDetailShell from '@/components/flow/FlowTaskDetailShell.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { createFlowActionCredentials } from '@/utils/flow-action-idempotency'
import { buildFlowCategoryTreeOptions, resolveFlowCategoryLabel } from './utils/categoryOptions'
import { getRowDisplayTitle, getTaskDisplayName } from './utils/processDisplay'

const userStore = useUserStore()
const { dict, getLabel } = useDict('flow_started_status', 'flow_task_sign_mode', 'flow_task_sign_relation_status')
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

const pendingCount = ref(0)

const showDrawer = ref(false)
const currentTask = ref(null)
const approvalHistory = ref([])

const withdrawComment = ref('')
const withdrawLoading = ref(false)
const canWithdraw = computed(() => currentTask.value && [0, 1].includes(currentTask.value.status))
const canReassign = computed(() => currentTask.value?.taskId && [0, 1].includes(currentTask.value.status))
const reassignVisible = ref(false)
const reassignLoading = ref(false)
const reassignUserId = ref(null)
const reassignUserName = ref('')
const reassignReason = ref('')
const signVisible = ref(false)
const signLoading = ref(false)
const signAction = ref('add')
const signUserId = ref(null)
const signUserName = ref('')
const signReason = ref('')
const signMode = ref('PARALLEL')
const signRelations = ref([])

const statusOptions = computed(() => toNumberOptions(dict.value.flow_started_status))
const signModeOptions = computed(() => (dict.value.flow_task_sign_mode || []).map(item => ({
  ...item,
  disabled: String(item.value).toUpperCase() !== 'PARALLEL',
})))

function getStatusTagClass(status) {
  const cls = { 0: 'warning', 1: 'info', 2: 'success', 3: 'error', 4: 'warning', 5: 'default', 6: 'default', 7: 'warning', 8: 'error' }
  return cls[status] || 'default'
}

function getStatusIcon(status) {
  const icons = {
    0: 'i-material-symbols:schedule',
    1: 'i-material-symbols:pending-actions',
    2: 'i-material-symbols:check-circle',
    3: 'i-material-symbols:cancel',
    4: 'i-material-symbols:keyboard-return',
    5: 'i-material-symbols:cancel',
    6: 'i-material-symbols:task-alt',
    7: 'i-material-symbols:keyboard-return',
    8: 'i-material-symbols:stop-circle',
  }
  return icons[status] || 'i-material-symbols:send'
}

function getStatusText(status) {
  return getLabel('flow_started_status', status) || '未知'
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
  signRelations.value = []
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
  if (row.taskId) {
    try {
      const relationRes = await flowApi.getSignRelations(row.taskId, { userId: String(userStore.userId) })
      if (relationRes.code === 200)
        signRelations.value = relationRes.data || []
    }
    catch {
      signRelations.value = []
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

function openReassignModal() {
  reassignUserId.value = null
  reassignUserName.value = ''
  reassignReason.value = ''
  reassignVisible.value = true
}

function openSignModal(action) {
  signAction.value = action
  signUserId.value = null
  signUserName.value = ''
  signReason.value = ''
  signMode.value = 'PARALLEL'
  signVisible.value = true
}

async function submitSign() {
  if (!signUserId.value) {
    window.$message.warning('请选择目标用户')
    return
  }
  signLoading.value = true
  try {
    const action = signAction.value === 'add' ? flowApi.addSign : flowApi.reduceSign
    const payload = {
      taskId: currentTask.value.taskId,
      userId: String(userStore.userId),
      targetUserId: String(signUserId.value),
      comment: signReason.value || undefined,
      signMode: signMode.value,
    }
    const credentials = await createFlowActionCredentials(`sign_${signAction.value}`, currentTask.value.taskId, payload)
    const res = await action({ ...payload, ...credentials })
    if (res.code !== 200) {
      window.$message.error(res.message || '操作失败')
      return
    }
    window.$message.success(signAction.value === 'add' ? '加签成功' : '减签成功')
    signVisible.value = false
    const relationRes = await flowApi.getSignRelations(currentTask.value.taskId, { userId: String(userStore.userId) })
    if (relationRes.code === 200)
      signRelations.value = relationRes.data || []
    loadData()
  }
  catch (error) {
    window.$message.error(error?.message || '操作失败')
  }
  finally {
    signLoading.value = false
  }
}

async function submitReassign() {
  if (!reassignUserId.value) {
    window.$message.warning('请选择新审批人')
    return
  }
  reassignLoading.value = true
  try {
    const res = await flowApi.reassignTask({
      taskId: currentTask.value.taskId,
      userId: String(userStore.userId),
      newAssignee: String(reassignUserId.value),
      reason: reassignReason.value || undefined,
    })
    if (res.code !== 200) {
      window.$message.error(res.message || '改派失败')
      return
    }
    window.$message.success('当前任务已改派')
    reassignVisible.value = false
    showDrawer.value = false
    loadData()
  }
  catch (error) {
    window.$message.error(error?.message || '改派失败')
  }
  finally {
    reassignLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  loadError.value = false
  try {
    const res = await flowApi.getStartedTasks({
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
      pendingCount.value = dataSource.value.filter(r => [0, 1].includes(r.status)).length
    }
    else {
      loadError.value = true
    }
  }
  catch {
    console.error('加载发起的流程失败')
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
