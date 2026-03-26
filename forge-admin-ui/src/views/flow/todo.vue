<template>
  <div class="todo-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">我的待办</h2>
        <n-badge :value="pagination.itemCount" :max="99" type="warning" />
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

    <!-- 审批详情抽屉 -->
    <n-drawer
      v-model:show="showDrawer"
      :width="720"
      placement="right"
      :mask-closable="!approveLoading"
    >
      <n-drawer-content :native-scrollbar="false" closable>
        <template #header>
          <div class="drawer-header">
            <span class="drawer-title">{{ currentTask?.title || '审批详情' }}</span>
            <n-tag
              v-if="currentTask"
              :type="currentTask.status === 0 ? 'warning' : 'info'"
              size="small"
              style="margin-left: 8px"
            >
              {{ currentTask.status === 0 ? '待办' : '已签收' }}
            </n-tag>
          </div>
        </template>

        <div v-if="currentTask" class="drawer-body">
          <!-- 基本信息卡片 -->
          <n-card class="info-card" size="small">
            <n-descriptions :column="2" label-placement="left" size="small">
              <n-descriptions-item label="当前节点">
                <n-tag type="primary" size="small">{{ currentTask.taskName }}</n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="发起人">
                <div class="user-info">
                  <n-avatar :size="20" round style="background: #18a058">
                    {{ (currentTask.startUserName || '?')[0] }}
                  </n-avatar>
                  <span style="margin-left: 6px">{{ currentTask.startUserName }}</span>
                </div>
              </n-descriptions-item>
              <n-descriptions-item label="发起部门">{{ currentTask.startDeptName || '-' }}</n-descriptions-item>
              <n-descriptions-item label="发起时间">{{ currentTask.createTime || '-' }}</n-descriptions-item>
              <n-descriptions-item label="流程分类">{{ currentTask.businessType || '-' }}</n-descriptions-item>
              <n-descriptions-item label="优先级">
                <n-tag :type="getPriorityType(currentTask.priority)" size="small">
                  {{ getPriorityText(currentTask.priority) }}
                </n-tag>
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

          <!-- 审批操作区（外部自定义表单 / 默认审批表单） -->
          <div class="section approve-section">
            <div class="section-title">
              <i class="i-material-symbols:rate-review text-primary" />
              审批操作
            </div>

            <!-- 外部自定义表单加载中 -->
            <div v-if="formInfoLoading" class="form-loading">
              <n-spin size="small" />
              <span style="margin-left:8px;color:#888">加载表单中...</span>
            </div>

            <!-- 外部自定义表单（formType=external 且已注册） -->
            <template v-else-if="useExternalForm">
              <FlowBusinessForm
                :form-url="taskFormInfo.formUrl"
                :task-id="taskFormInfo.taskId"
                :business-key="taskFormInfo.businessKey"
                :process-instance-id="taskFormInfo.processInstanceId"
                :task-def-key="taskFormInfo.taskDefKey"
                :process-def-key="taskFormInfo.processDefKey"
                :variables="taskFormInfo.variables || {}"
                :read-only="false"
                @submit="handleExternalFormSubmit"
                @cancel="showDrawer = false"
              />
            </template>

            <!-- 默认审批表单 -->
            <template v-else>
              <n-form :model="approveForm" label-placement="top" size="medium">
                <n-form-item label="审批意见" required>
                  <n-input
                    v-model:value="approveForm.comment"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入审批意见（必填）"
                    :maxlength="500"
                    show-count
                  />
                </n-form-item>
              </n-form>

              <div class="action-buttons">
                <n-popconfirm
                  @positive-click="() => submitApprove('approve')"
                  positive-text="确认通过"
                  negative-text="取消"
                >
                  <template #trigger>
                    <n-button
                      type="primary"
                      :loading="approveLoading && approveForm.action === 'approve'"
                      :disabled="approveLoading"
                      size="large"
                    >
                      <template #icon><i class="i-material-symbols:check-circle" /></template>
                      同意
                    </n-button>
                  </template>
                  确认同意该审批？
                </n-popconfirm>

                <n-popconfirm
                  @positive-click="() => submitApprove('reject')"
                  positive-text="确认驳回"
                  negative-text="取消"
                >
                  <template #trigger>
                    <n-button
                      type="error"
                      :loading="approveLoading && approveForm.action === 'reject'"
                      :disabled="approveLoading"
                      size="large"
                    >
                      <template #icon><i class="i-material-symbols:cancel" /></template>
                      驳回
                    </n-button>
                  </template>
                  确认驳回该审批？
                </n-popconfirm>

                <n-button @click="handleDelegate" :disabled="approveLoading" size="large">
                  <template #icon><i class="i-material-symbols:person-add" /></template>
                  转办
                </n-button>

                <n-button
                  v-if="currentTask.status === 0 && !currentTask.assignee"
                  type="info"
                  @click="handleClaim(currentTask)"
                  :disabled="approveLoading"
                  size="large"
                >
                  <template #icon><i class="i-material-symbols:assignment-ind" /></template>
                  签收
                </n-button>
              </div>
            </template>
          </div>
        </div>

        <template #footer>
          <n-space justify="end">
            <n-button @click="showDrawer = false">关闭</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 转办弹窗 -->
    <n-modal
      v-model:show="showDelegateModal"
      preset="card"
      title="转办任务"
      style="width: 480px"
      :mask-closable="false"
    >
      <n-form :model="delegateForm" label-placement="top">
        <n-form-item label="转办给">
          <n-input v-model:value="delegateForm.targetUserId" placeholder="请输入目标用户ID" />
        </n-form-item>
        <n-form-item label="转办说明">
          <n-input
            v-model:value="delegateForm.comment"
            type="textarea"
            :rows="2"
            placeholder="请输入转办说明"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showDelegateModal = false">取消</n-button>
          <n-button type="primary" :loading="delegateLoading" @click="submitDelegate">确认转办</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h, onMounted } from 'vue'
import { NTag, NButton, NSpace, NBadge, NAvatar } from 'naive-ui'
import flowApi from '@/api/flow'
import { useUserStore } from '@/store'
import ProcessDiagramViewer from '@/components/bpmn/ProcessDiagramViewer.vue'
import FlowBusinessForm from '@/components/common/FlowBusinessForm.vue'

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
const historyLoading = ref(false)

// 业务自定义表单
const taskFormInfo = ref(null)   // TaskFormInfo 对象
const formInfoLoading = ref(false)
// 是否使用外部自定义表单
const useExternalForm = computed(() => taskFormInfo.value?.formType === 'external' && taskFormInfo.value?.formUrl)

// 审批表单
const approveLoading = ref(false)
const approveForm = reactive({
  action: '',   // 当前操作 'approve' | 'reject'
  comment: ''
})

// 转办
const showDelegateModal = ref(false)
const delegateLoading = ref(false)
const delegateForm = reactive({ targetUserId: '', comment: '' })

// 优先级映射
const priorityMap = {
  0: { text: '低', type: 'default' },
  1: { text: '普通', type: 'info' },
  2: { text: '高', type: 'warning' },
  3: { text: '紧急', type: 'error' }
}
function getPriorityType(p) { return (priorityMap[p] || priorityMap[1]).type }
function getPriorityText(p) { return (priorityMap[p] || priorityMap[1]).text }

// 审批动作映射
function getActionType(action) {
  const m = { approve: 'success', reject: 'error', delegate: 'warning', claim: 'info', start: 'primary', withdraw: 'default' }
  return m[action] || 'default'
}
function getActionText(action) {
  const m = { approve: '同意', reject: '驳回', delegate: '转办', claim: '签收', start: '发起', withdraw: '撤回', pending: '待处理' }
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
    title: '任务标题',
    key: 'title',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render: (row) => h('span', {
      class: 'task-title-link',
      onClick: () => openDrawer(row)
    }, row.title || row.taskName)
  },
  { title: '当前节点', key: 'taskName', width: 120 },
  {
    title: '发起人',
    key: 'startUserName',
    width: 100,
    render: (row) => h('div', { class: 'flex items-center gap-4' }, [
      h(NAvatar, { size: 20, round: true, style: 'background:#18a058;font-size:11px' }, () => (row.startUserName || '?')[0]),
      h('span', { style: 'margin-left:6px' }, row.startUserName || '-')
    ])
  },
  { title: '发起部门', key: 'startDeptName', width: 120, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) => {
      if (row.status === 0) return h(NTag, { type: 'warning', size: 'small', round: true }, () => '待办')
      if (row.status === 1) return h(NTag, { type: 'info', size: 'small', round: true }, () => '已签收')
      return h(NTag, { size: 'small', round: true }, () => '未知')
    }
  },
  { title: '发起时间', key: 'createTime', width: 155 },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    fixed: 'right',
    render: (row) => h(NSpace, { size: 4 }, () => [
      h(NButton, { size: 'small', type: 'primary', onClick: () => openDrawer(row) }, () => '去审批'),
      row.status === 0 && !row.assignee
        ? h(NButton, { size: 'small', type: 'info', onClick: (e) => { e.stopPropagation(); handleClaim(row) } }, () => '签收')
        : null
    ])
  }
]

// 行属性：点击行打开抽屉
function getRowProps(row) {
  return {
    style: 'cursor:pointer',
    onClick: () => openDrawer(row)
  }
}

// 打开审批抽屉
async function openDrawer(row) {
  currentTask.value = row
  approveForm.comment = ''
  approveForm.action = ''
  approvalHistory.value = []
  taskFormInfo.value = null
  showDrawer.value = true

  // 并行加载：审批时间轴 + 任务表单信息
  const promises = []

  if (row.processInstanceId) {
    promises.push(
      flowApi.getProcessHistory(row.processInstanceId)
        .then(res => { if (res.code === 200) approvalHistory.value = res.data || [] })
        .catch(e => console.error('加载审批历史失败', e))
    )
  }

  const taskId = row.taskId || row.id
  if (taskId) {
    formInfoLoading.value = true
    promises.push(
      flowApi.getTaskFormInfo(taskId)
        .then(res => { if (res.code === 200) taskFormInfo.value = res.data })
        .catch(e => console.error('加载表单信息失败', e))
        .finally(() => { formInfoLoading.value = false })
    )
  }

  await Promise.all(promises)
}

// 外部自定义表单提交（由 FlowBusinessForm 回调）
async function handleExternalFormSubmit({ action, comment, variables }) {
  if (!comment?.trim()) {
    window.$message.warning('请输入审批意见')
    return
  }
  approveLoading.value = true
  try {
    const api = action === 'approve' ? flowApi.approveTask : flowApi.rejectTask
    const res = await api({
      taskId: currentTask.value.taskId || currentTask.value.id,
      userId: userStore.userId,
      comment,
      variables
    })
    if (res.code === 200) {
      window.$message.success(action === 'approve' ? '✅ 审批通过' : '❌ 已驳回')
      showDrawer.value = false
      loadData()
    } else {
      window.$message.error(res.message || '操作失败')
    }
  } catch (e) {
    window.$message.error('操作失败')
  } finally {
    approveLoading.value = false
  }
}

// 提交审批
async function submitApprove(action) {
  if (!approveForm.comment?.trim()) {
    window.$message.warning('请输入审批意见')
    return
  }
  approveForm.action = action
  approveLoading.value = true
  try {
    const api = action === 'approve' ? flowApi.approveTask : flowApi.rejectTask
    const res = await api({
      taskId: currentTask.value.taskId,
      userId: userStore.userId,
      comment: approveForm.comment
    })
    if (res.code === 200) {
      window.$message.success(action === 'approve' ? '✅ 审批通过' : '❌ 已驳回')
      showDrawer.value = false
      loadData()
    } else {
      window.$message.error(res.message || '操作失败')
    }
  } catch (e) {
    window.$message.error('操作失败')
  } finally {
    approveLoading.value = false
  }
}

// 转办
function handleDelegate() {
  delegateForm.targetUserId = ''
  delegateForm.comment = ''
  showDelegateModal.value = true
}
async function submitDelegate() {
  if (!delegateForm.targetUserId) {
    window.$message.warning('请输入转办目标用户')
    return
  }
  delegateLoading.value = true
  try {
    const res = await flowApi.delegateTask({
      taskId: currentTask.value.taskId,
      userId: userStore.userId,
      targetUserId: delegateForm.targetUserId,
      comment: delegateForm.comment
    })
    if (res.code === 200) {
      window.$message.success('转办成功')
      showDelegateModal.value = false
      showDrawer.value = false
      loadData()
    } else {
      window.$message.error(res.message || '转办失败')
    }
  } catch (e) {
    window.$message.error('转办失败')
  } finally {
    delegateLoading.value = false
  }
}

// 签收任务
async function handleClaim(row) {
  try {
    const res = await flowApi.claimTask(row.taskId, userStore.userId)
    if (res.code === 200) {
      window.$message.success('签收成功')
      if (currentTask.value && currentTask.value.taskId === row.taskId) {
        currentTask.value.status = 1
        currentTask.value.assignee = userStore.userId
      }
      loadData()
    } else {
      window.$message.error(res.message || '签收失败')
    }
  } catch (e) {
    window.$message.error('签收失败')
  }
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await flowApi.getTodoTasks({
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
    console.error('加载待办任务失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await flowApi.getEnabledCategories()
    if (res.code === 200 && res.data) {
      categoryOptions.value = res.data.map(item => ({
        label: item.categoryName,
        value: item.categoryCode
      }))
    }
  } catch (e) {
    console.error('加载分类失败:', e)
  }
}

function handleSearch() { pagination.page = 1; loadData() }
function handleReset() { queryParams.title = ''; queryParams.category = ''; pagination.page = 1; loadData() }

onMounted(() => { loadCategories(); loadData() })
</script>

<style scoped>
.todo-page {
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

.approve-section {
  background: #fff;
  border: 1px solid #e8e8e8;
}

.form-loading {
  display: flex;
  align-items: center;
  padding: 20px 0;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}
</style>