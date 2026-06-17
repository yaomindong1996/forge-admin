<template>
  <div class="flow-page">
    <!-- 统计卡片 -->
    <FlowStats
      :todo-count="todoCount"
      :done-count="doneCount"
      :started-count="startedCount"
      :cc-count="ccCount"
      :unread-cc="unreadCc"
      active-tab="todo"
      @switch="handleSwitch"
    />

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <div class="title-icon">
            <i class="i-material-symbols:pending-actions" />
          </div>
          <h2 class="page-title">
            我的待办
          </h2>
        </div>
        <div class="quick-stats">
          <span v-if="urgentCount > 0" class="stat-item urgent">
            <i class="i-material-symbols:warning" />
            {{ urgentCount }}紧急
          </span>
        </div>
      </div>
      <div class="header-right">
        <n-input
          v-model:value="queryParams.title"
          placeholder="搜索任务标题"
          clearable
          class="search-input"
          @keydown.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <NTreeSelect
          v-model:value="queryParams.category"
          placeholder="流程分类"
          clearable
          class="category-select"
          :options="categoryTreeOptions"
          :default-expand-all="true"
        />
        <n-select
          v-model:value="queryParams.status"
          placeholder="任务状态"
          clearable
          class="category-select"
          :options="statusOptions"
        />
        <NButton type="primary" class="search-btn" @click="handleSearch">
          <i class="i-material-symbols:search mr-2" />
          查询
        </NButton>
        <NButton class="reset-btn" @click="handleReset">
          <i class="i-material-symbols:refresh mr-2" />
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

    <!-- 审批详情弹窗 -->
    <n-modal
      v-model:show="showDrawer"
      :mask-closable="!approveLoading"
      preset="card"
      class="flow-task-detail-modal"
      :closable="!approveLoading"
      :bordered="false"
      :segmented="{ content: true, footer: true }"
      content-style="padding: 0; overflow: hidden;"
    >
      <template #header>
        <div class="drawer-header">
          <div class="drawer-title-row">
            <div class="status-dot" :class="currentTask?.status === 0 ? 'pending' : 'claimed'" />
            <span class="drawer-title">{{ currentTask?.title || '审批详情' }}</span>
          </div>
          <div class="drawer-tags">
            <span class="status-tag" :class="currentTask?.status === 0 ? 'pending' : 'claimed'">
              {{ getLabel('flow_todo_status', currentTask?.status) }}
            </span>
            <span v-if="currentTask?.priority >= 2" class="priority-tag" :class="getPriorityClass(currentTask?.priority)">
              {{ getPriorityText(currentTask?.priority) }}
            </span>
          </div>
        </div>
      </template>

      <div v-if="currentTask" class="drawer-body">
        <!-- 信息 Tabs -->
        <n-tabs v-model:value="activeDrawerTab" type="line" animated class="drawer-tabs">
          <n-tab-pane name="info" tab="基本信息">
            <div class="info-grid">
              <div class="info-card">
                <div class="info-header">
                  <i class="i-material-symbols:info-outline" />
                  任务信息
                </div>
                <div class="info-items">
                  <div class="info-item">
                    <span class="info-label">当前节点</span>
                    <span class="info-value highlight">{{ currentTask.taskName }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">流程分类</span>
                    <span class="info-value">{{ currentTask.businessType || '-' }}</span>
                  </div>
                </div>
              </div>
              <div class="info-card">
                <div class="info-header">
                  <i class="i-material-symbols:person-outline" />
                  发起信息
                </div>
                <div class="info-items">
                  <div class="info-item user-item">
                    <span class="info-label">发起人</span>
                    <div class="user-display">
                      <UserAvatar :name="currentTask.startUserName || '未知'" :size="24" />
                      <span class="info-value">{{ currentTask.startUserName || '-' }}</span>
                    </div>
                  </div>
                  <div class="info-item">
                    <span class="info-label">发起部门</span>
                    <span class="info-value">{{ currentTask.startDeptName || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">发起时间</span>
                    <span class="info-value">{{ currentTask.createTime || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </n-tab-pane>

          <n-tab-pane name="history" display-directive="show">
            <template #tab>
              <span>审批进度</span>
              <span v-if="approvalHistory.length > 0" class="tab-badge">{{ approvalHistory.length }}</span>
            </template>
            <FlowTimeline v-if="approvalHistory.length > 0" :items="approvalHistory" />
            <n-empty v-else description="暂无审批记录" size="small" />
          </n-tab-pane>

          <n-tab-pane name="diagram" tab="流程图" display-directive="show:lazy">
            <div class="diagram-pane">
              <ProcessDiagramViewer
                v-if="currentTask.processInstanceId"
                :process-instance-id="currentTask.processInstanceId"
                :compact="true"
              />
              <n-empty v-else description="暂无流程图" size="small" />
            </div>
          </n-tab-pane>
        </n-tabs>

        <!-- 审批操作区 -->
        <div class="approve-section">
          <div class="approve-header">
            <i class="i-material-symbols:rate-review" />
            审批操作
          </div>

          <div v-if="formInfoLoading" class="form-loading">
            <n-spin size="small" />
            <span>加载表单中...</span>
          </div>

          <template v-else-if="useExternalForm">
            <FlowBusinessForm
              :form-url="taskFormInfo.formUrl"
              :task-id="taskFormInfo.taskId"
              :business-key="taskFormInfo.businessKey"
              :process-instance-id="taskFormInfo.processInstanceId"
              :task-def-key="taskFormInfo.taskDefKey"
              :process-def-key="taskFormInfo.processDefKey"
              :variables="taskFormInfo.variables || {}"
              :approval-policy="approvalPolicy"
              :read-only="false"
              @submit="handleExternalFormSubmit"
              @cancel="showDrawer = false"
            >
              <template #actions>
                <NButton v-if="canDelegate" size="large" :disabled="approveLoading" @click="handleDelegate">
                  <i class="i-material-symbols:person-add mr-2" />
                  转办
                </NButton>

                <NButton
                  v-if="currentTask.status === 0 && !currentTask.assignee"
                  type="info"
                  size="large"
                  :disabled="approveLoading"
                  @click="handleClaim(currentTask)"
                >
                  <i class="i-material-symbols:assignment-ind mr-2" />
                  签收
                </NButton>
              </template>
            </FlowBusinessForm>
          </template>

          <template v-else>
            <div v-if="useDynamicForm" class="dynamic-form-section">
              <div class="dynamic-form-header">
                <div>
                  <div class="dynamic-form-title">
                    节点动态表单
                  </div>
                  <div class="dynamic-form-desc">
                    审批通过时会校验表单，并将填写内容作为流程变量提交
                  </div>
                </div>
                <span class="dynamic-form-key">{{ taskFormInfo.formKey || 'inline' }}</span>
              </div>
              <FlowFormCreateRenderer
                ref="dynamicFormRef"
                v-model="dynamicFormData"
                :schema="taskFormInfo.formJson"
              />
            </div>

            <n-form :model="approveForm" label-placement="top">
              <n-form-item label="审批意见" :required="requireComment">
                <n-input
                  v-model:value="approveForm.comment"
                  type="textarea"
                  :rows="3"
                  :placeholder="requireComment ? '请输入审批意见' : '请输入审批意见（可选）'"
                  :maxlength="500"
                  show-count
                />
              </n-form-item>
              <n-form-item v-if="requireSignature" label="审批签名" required>
                <SignaturePad
                  :key="approveSignatureKey"
                  ref="approveSignatureRef"
                  v-model="approveForm.signature"
                  :business-id="currentTask?.taskId || currentTask?.id || ''"
                />
              </n-form-item>
            </n-form>

            <div class="action-buttons">
              <n-popconfirm v-if="canApprove" @positive-click="() => submitApprove('approve')">
                <template #trigger>
                  <NButton type="success" size="large" :loading="approveLoading && approveForm.action === 'approve'" :disabled="approveLoading">
                    <i class="i-material-symbols:check-circle mr-2" />
                    同意
                  </NButton>
                </template>
                确认同意该审批？
              </n-popconfirm>

              <n-popconfirm v-if="canReject" @positive-click="() => submitApprove('reject')">
                <template #trigger>
                  <NButton type="error" size="large" :loading="approveLoading && approveForm.action === 'reject'" :disabled="approveLoading">
                    <i class="i-material-symbols:cancel mr-2" />
                    驳回
                  </NButton>
                </template>
                确认驳回该审批？
              </n-popconfirm>

              <n-popconfirm v-if="canReturn" @positive-click="() => submitApprove('return')">
                <template #trigger>
                  <NButton type="warning" size="large" :loading="approveLoading && approveForm.action === 'return'" :disabled="approveLoading">
                    <i class="i-material-symbols:keyboard-return mr-2" />
                    退回
                  </NButton>
                </template>
                确认退回上一审批节点？
              </n-popconfirm>

              <n-popconfirm v-if="canTerminate" @positive-click="() => submitApprove('terminate')">
                <template #trigger>
                  <NButton type="error" ghost size="large" :loading="approveLoading && approveForm.action === 'terminate'" :disabled="approveLoading">
                    <i class="i-material-symbols:stop-circle mr-2" />
                    终结
                  </NButton>
                </template>
                确认终结该流程？
              </n-popconfirm>

              <NButton v-if="canDelegate" size="large" :disabled="approveLoading" @click="handleDelegate">
                <i class="i-material-symbols:person-add mr-2" />
                转办
              </NButton>

              <NButton
                v-if="currentTask.status === 0 && !currentTask.assignee"
                type="info"
                size="large"
                :disabled="approveLoading"
                @click="handleClaim(currentTask)"
              >
                <i class="i-material-symbols:assignment-ind mr-2" />
                签收
              </NButton>
            </div>
          </template>
        </div>
      </div>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDrawer = false">
            关闭
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <!-- 转办弹窗 -->
    <n-modal v-model:show="showDelegateModal" preset="card" title="转办任务" style="width: 480px" :mask-closable="false">
      <n-form :model="delegateForm" label-placement="top">
        <n-form-item label="转办给" required>
          <div class="delegate-user-row">
            <div class="delegate-user-display">
              <template v-if="delegateTargetUser">
                <UserAvatar :name="delegateTargetUser.name || delegateTargetUser.username || 'U'" :size="24" />
                <span class="delegate-user-name">{{ delegateTargetUser.name || delegateTargetUser.username }}</span>
                <span class="delegate-user-id">{{ delegateTargetUser.username }}</span>
              </template>
              <span v-else class="delegate-placeholder">未选择转办人</span>
            </div>
            <NButton size="small" @click="showUserSelectModal = true">
              <i class="i-material-symbols:person-search mr-2" />
              选择人员
            </NButton>
          </div>
        </n-form-item>
        <n-form-item label="转办说明">
          <n-input
            v-model:value="delegateForm.comment"
            type="textarea"
            :rows="2"
            :placeholder="requireComment ? '请输入转办说明' : '请输入转办说明（可选）'"
          />
        </n-form-item>
        <n-form-item v-if="requireSignature" label="审批签名" required>
          <SignaturePad
            :key="delegateSignatureKey"
            ref="delegateSignatureRef"
            v-model="delegateForm.signature"
            :business-id="currentTask?.taskId || currentTask?.id || ''"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDelegateModal = false">
            取消
          </NButton>
          <NButton type="primary" :loading="delegateLoading" @click="submitDelegate">
            确认转办
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <!-- 用户选择弹窗 -->
    <UserSelectModal
      :show="showUserSelectModal"
      title="选择转办人"
      :multiple="false"
      @update:show="showUserSelectModal = $event"
      @confirm="handleUserSelected"
    />
  </div>
</template>

<script setup>
import { NButton, NSpace, NTreeSelect } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import flowApi from '@/api/flow'
import UserSelectModal from '@/components/common/UserSelectModal.vue'
import FlowBusinessForm from '@/components/common/FlowBusinessForm.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import ProcessDiagramViewer from '@/components/flow-designer/viewer/DingFlowViewer.vue'
import FlowStats from '@/components/flow/FlowStats.vue'
import FlowTimeline from '@/components/flow/FlowTimeline.vue'
import SignaturePad from '@/components/flow/SignaturePad.vue'
import FlowFormCreateRenderer from '@/components/form-create/FlowFormCreateRenderer.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { dict, getLabel } = useDict('flow_todo_status', 'flow_priority')
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

const queryParams = reactive({ title: '', category: '', status: null })
const categoryOptions = ref([])
const categoryTreeOptions = ref([])

function buildTreeSelectOptions(treeData) {
  return treeData.map(item => ({
    label: item.categoryName,
    value: item.id,
    key: item.id,
    children: item.children && item.children.length > 0 ? buildTreeSelectOptions(item.children) : undefined,
  }))
}

// 统计数据
const todoCount = ref(0)
const doneCount = ref(0)
const startedCount = ref(0)
const ccCount = ref(0)
const unreadCc = ref(0)
const urgentCount = ref(0)

// 抽屉状态
const showDrawer = ref(false)
const currentTask = ref(null)
const approvalHistory = ref([])
const activeDrawerTab = ref('info')

// 业务自定义表单
const taskFormInfo = ref(null)
const formInfoLoading = ref(false)
const useExternalForm = computed(() => taskFormInfo.value?.formType === 'external' && taskFormInfo.value?.formUrl)
const useDynamicForm = computed(() => taskFormInfo.value?.formType === 'dynamic' && taskFormInfo.value?.formJson)
const dynamicFormRef = ref(null)
const dynamicFormData = ref({})
const canApprove = computed(() => taskFormInfo.value?.allowApprove !== false)
const canReject = computed(() => taskFormInfo.value?.allowReject !== false)
const canDelegate = computed(() => taskFormInfo.value?.allowDelegate !== false)
const canReturn = computed(() => taskFormInfo.value?.allowReturn === true)
const canTerminate = computed(() => taskFormInfo.value?.allowTerminate === true)
const requireComment = computed(() => taskFormInfo.value?.requireComment !== false)
const requireSignature = computed(() => taskFormInfo.value?.requireSignature === true)
const approvalPolicy = computed(() => ({
  allowApprove: canApprove.value,
  allowReject: canReject.value,
  allowDelegate: canDelegate.value,
  allowReturn: canReturn.value,
  allowTerminate: canTerminate.value,
  requireComment: requireComment.value,
  requireSignature: requireSignature.value,
}))

// 审批表单
const approveLoading = ref(false)
const approveForm = reactive({ action: '', comment: '', signature: '' })
const approveSignatureRef = ref(null)
const approveSignatureKey = ref(0)

// 转办
const showDelegateModal = ref(false)
const showUserSelectModal = ref(false)
const delegateLoading = ref(false)
const delegateTargetUser = ref(null)
const delegateForm = reactive({ comment: '', signature: '' })
const delegateSignatureRef = ref(null)
const delegateSignatureKey = ref(0)
const routeTaskOpening = ref(false)

const statusOptions = computed(() => toNumberOptions(dict.value.flow_todo_status))

// 优先级
function getPriorityClass(p) {
  if (p >= 3)
    return 'urgent'
  if (p === 2)
    return 'high'
  return ''
}
function getPriorityText(p) {
  return getLabel('flow_priority', p) || '普通'
}

// 表格列
const columns = [
  {
    title: '任务标题',
    key: 'title',
    width: 180,
    ellipsis: { tooltip: true },
    render: row => h('span', { class: 'task-title-link', onClick: () => openDrawer(row) }, row.title || row.taskName),
  },
  { title: '当前节点', key: 'taskName', width: 100, ellipsis: { tooltip: true } },
  {
    title: '发起人',
    key: 'startUserName',
    width: 100,
    render: row => h('div', { class: 'table-user' }, [
      h(UserAvatar, { name: row.startUserName || '未知', size: 24 }),
      h('span', { class: 'user-name-text' }, row.startUserName || '-'),
    ]),
  },
  { title: '发起部门', key: 'startDeptName', width: 100, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 70,
    render: row => h('span', { class: ['status-tag-mini', row.status === 0 ? 'pending' : 'claimed'] }, getLabel('flow_todo_status', row.status)),
  },
  {
    title: '优先级',
    key: 'priority',
    width: 70,
    render: row => h('span', { class: ['priority-tag-mini', getPriorityClass(row.priority)] }, getPriorityText(row.priority)),
  },
  { title: '发起时间', key: 'createTime', width: 150 },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    fixed: 'right',
    render: row => h(NSpace, { size: 4 }, () => [
      h(NButton, { size: 'small', type: 'primary', onClick: () => openDrawer(row) }, () => '去审批'),
      row.status === 0 && !row.assignee
        ? h(NButton, {
            size: 'small',
            type: 'info',
            onClick: (e) => {
              e.stopPropagation()
              handleClaim(row)
            },
          }, () => '签收')
        : null,
    ]),
  },
]

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

function getRowProps(row) {
  return { style: 'cursor:pointer', onClick: () => openDrawer(row) }
}

async function openDrawer(row) {
  currentTask.value = row
  approveForm.comment = ''
  approveForm.action = ''
  approveForm.signature = ''
  approveSignatureKey.value += 1
  approvalHistory.value = []
  taskFormInfo.value = null
  dynamicFormData.value = {}
  activeDrawerTab.value = 'info'
  showDrawer.value = true

  const promises = []
  if (row.processInstanceId) {
    promises.push(
      flowApi.getProcessHistory(row.processInstanceId)
        .then((res) => {
          if (res.code === 200)
            approvalHistory.value = res.data || []
        })
        .catch(e => console.error('加载审批历史失败', e)),
    )
  }

  const taskId = row.taskId || row.id
  if (taskId) {
    formInfoLoading.value = true
    promises.push(
      flowApi.getTaskFormInfo(taskId)
        .then((res) => {
          if (res.code === 200) {
            taskFormInfo.value = res.data
            dynamicFormData.value = { ...(res.data?.variables || {}) }
          }
        })
        .catch(e => console.error('加载表单信息失败', e))
        .finally(() => { formInfoLoading.value = false }),
    )
  }

  await Promise.all(promises)
}

async function handleExternalFormSubmit({ action, comment, signature, variables }) {
  const approvalSignature = signature || variables?.signature
  if (!canRunAction(action))
    return
  if (!validateApprovalInput(comment, approvalSignature))
    return

  approveLoading.value = true
  try {
    const api = resolveActionApi(action)
    const res = await api({
      taskId: currentTask.value.taskId || currentTask.value.id,
      userId: userStore.userId,
      comment,
      signature: approvalSignature,
      variables,
    })
    if (res.code === 200) {
      window.$message.success(getActionSuccessText(action))
      showDrawer.value = false
      loadData()
    }
    else {
      window.$message.error(res.message || '操作失败')
    }
  }
  catch (error) {
    window.$message.error(error?.message || '操作失败')
  }
  finally {
    approveLoading.value = false
  }
}

function canRunAction(action) {
  const allowed = {
    approve: canApprove.value,
    reject: canReject.value,
    return: canReturn.value,
    terminate: canTerminate.value,
    delegate: canDelegate.value,
  }
  if (allowed[action] === false) {
    window.$message.warning('当前节点不允许执行该操作')
    return false
  }
  return true
}

function hasSignatureValue(signature, signatureRef) {
  return Boolean(signature?.trim()) || Boolean(signatureRef?.hasSignature?.())
}

async function resolveSignature(signatureRef, signature) {
  if (!requireSignature.value)
    return signature || ''
  if (!signatureRef?.upload)
    return signature || ''

  try {
    return await signatureRef.upload()
  }
  catch (error) {
    throw new Error(error?.message || '签名图片保存失败')
  }
}

function validateApprovalInput(comment, signature, signatureRef = null) {
  if (requireComment.value && !comment?.trim()) {
    window.$message.warning('请输入审批意见')
    return false
  }
  if (requireSignature.value && !hasSignatureValue(signature, signatureRef)) {
    window.$message.warning('请完成手写签名')
    return false
  }
  return true
}

function resolveActionApi(action) {
  const apiMap = {
    approve: flowApi.approveTask,
    reject: flowApi.rejectTask,
    return: flowApi.returnTask,
    terminate: flowApi.terminateTask,
  }
  return apiMap[action] || flowApi.approveTask
}

function getActionSuccessText(action) {
  const textMap = {
    approve: '审批通过',
    reject: '已驳回',
    return: '已退回',
    terminate: '流程已终结',
  }
  return textMap[action] || '操作成功'
}

async function submitApprove(action) {
  if (!canRunAction(action))
    return
  if (!validateApprovalInput(approveForm.comment, approveForm.signature, approveSignatureRef.value))
    return
  approveForm.action = action
  approveLoading.value = true
  try {
    const signature = await resolveSignature(approveSignatureRef.value, approveForm.signature)
    approveForm.signature = signature
    const api = resolveActionApi(action)
    const variables = await collectDynamicFormVariables(action)
    const res = await api({
      taskId: currentTask.value.taskId,
      userId: userStore.userId,
      comment: approveForm.comment,
      signature,
      variables,
    })
    if (res.code === 200) {
      window.$message.success(getActionSuccessText(action))
      showDrawer.value = false
      loadData()
    }
    else {
      window.$message.error(res.message || '操作失败')
    }
  }
  catch (error) {
    window.$message.error(error?.message || '操作失败')
  }
  finally {
    approveLoading.value = false
  }
}

async function collectDynamicFormVariables(action) {
  if (!useDynamicForm.value || !dynamicFormRef.value)
    return undefined
  if (action === 'approve') {
    await dynamicFormRef.value.validate()
  }
  return dynamicFormRef.value.getData()
}

function handleDelegate() {
  delegateTargetUser.value = null
  delegateForm.comment = ''
  delegateForm.signature = ''
  delegateSignatureKey.value += 1
  showDelegateModal.value = true
}

function handleUserSelected(user) {
  delegateTargetUser.value = user
}

async function submitDelegate() {
  if (!canRunAction('delegate'))
    return
  if (!delegateTargetUser.value) {
    window.$message.warning('请选择转办人')
    return
  }
  if (!validateApprovalInput(delegateForm.comment, delegateForm.signature, delegateSignatureRef.value))
    return
  delegateLoading.value = true
  try {
    const signature = await resolveSignature(delegateSignatureRef.value, delegateForm.signature)
    delegateForm.signature = signature
    const res = await flowApi.delegateTask({
      taskId: currentTask.value.taskId,
      userId: String(userStore.userId),
      targetUserId: String(delegateTargetUser.value.id),
      comment: delegateForm.comment,
      signature,
    })
    if (res.code === 200) {
      window.$message.success('转办成功')
      showDelegateModal.value = false
      showDrawer.value = false
      loadData()
    }
    else {
      window.$message.error(res.message || '转办失败')
    }
  }
  catch (error) {
    window.$message.error(error?.message || '转办失败')
  }
  finally {
    delegateLoading.value = false
  }
}

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
    }
    else {
      window.$message.error(res.message || '签收失败')
    }
  }
  catch {
    window.$message.error('签收失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await flowApi.getTodoTasks({
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
      todoCount.value = res.data.total || 0
      urgentCount.value = dataSource.value.filter(r => r.priority >= 3).length
    }
  }
  catch {
    console.error('加载待办任务失败')
  }
  finally {
    loading.value = false
  }
}

function getRouteTaskId() {
  const taskId = route.query.taskId
  if (Array.isArray(taskId))
    return taskId[0] ? String(taskId[0]) : ''
  return taskId ? String(taskId) : ''
}

async function openTaskFromRoute() {
  const taskId = getRouteTaskId()
  if (!taskId || routeTaskOpening.value)
    return

  if (showDrawer.value && currentTask.value?.taskId === taskId)
    return

  routeTaskOpening.value = true
  try {
    const existing = dataSource.value.find(row => row.taskId === taskId || row.id === taskId)
    if (existing) {
      await openDrawer(existing)
      return
    }

    const res = await flowApi.getTaskDetail(taskId)
    if (res.code === 200 && res.data) {
      await openDrawer(res.data)
    }
    else {
      window.$message.warning('待办任务不存在或已处理')
      clearRouteTaskId()
    }
  }
  catch {
    window.$message.warning('待办任务不存在或已处理')
    clearRouteTaskId()
  }
  finally {
    routeTaskOpening.value = false
  }
}

function clearRouteTaskId() {
  if (!getRouteTaskId())
    return
  const query = { ...route.query }
  delete query.taskId
  delete query.source
  delete query.t
  router.replace({ path: route.path, query })
}

async function loadStats() {
  try {
    const [doneRes, startedRes, ccRes] = await Promise.all([
      flowApi.getDoneTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
      flowApi.getStartedTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
      flowApi.getMyCc({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
    ])
    doneCount.value = doneRes.code === 200 ? doneRes.data?.total || 0 : 0
    startedCount.value = startedRes.code === 200 ? startedRes.data?.total || 0 : 0
    ccCount.value = ccRes.code === 200 ? ccRes.data?.total || 0 : 0
    if (ccRes.code === 200 && ccRes.data?.records) {
      unreadCc.value = ccRes.data.records.filter(r => r.isRead === 0).length
    }
  }
  catch {
    console.error('加载统计数据失败')
  }
}

async function loadCategories() {
  try {
    const res = await flowApi.getCategoryTreeSelect(false)
    if (res.code === 200 && res.data) {
      categoryTreeOptions.value = buildTreeSelectOptions(res.data)
      categoryOptions.value = res.data.map(item => ({ label: item.categoryName, value: item.id }))
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

function handleSwitch(tab) {
  const routes = { todo: '/flow/todo', done: '/flow/done', started: '/flow/started', cc: '/flow/cc' }
  if (routes[tab])
    window.$router?.push(routes[tab])
}

onMounted(async () => {
  loadCategories()
  loadStats()
  await loadData()
  await openTaskFromRoute()
})

watch(
  () => route.fullPath,
  async () => {
    if (route.path === '/flow/todo')
      await openTaskFromRoute()
  },
)
</script>

<style scoped>
:deep(.n-data-table .n-data-table-th),
:deep(.n-data-table .n-data-table-td) {
  padding: 6px 8px;
}

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
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
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
  background: #fef3c7;
  color: #b45309;
}

.stat-item.urgent {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #b91c1c;
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

.search-btn,
.reset-btn {
  display: flex;
  align-items: center;
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
:deep(.status-tag-mini.pending) {
  background: #fef3c7;
  color: #b45309;
}
:deep(.status-tag-mini.claimed) {
  background: #dbeafe;
  color: #1e40af;
}

:deep(.priority-tag-mini) {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  background: #f1f5f9;
  color: #64748b;
}
:deep(.priority-tag-mini.high) {
  background: #fef3c7;
  color: #b45309;
}
:deep(.priority-tag-mini.urgent) {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #b91c1c;
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
.status-dot.pending {
  background: #f59e0b;
}
.status-dot.claimed {
  background: #3b82f6;
}

.drawer-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.drawer-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}
.status-tag.pending {
  background: #fef3c7;
  color: #b45309;
}
.status-tag.claimed {
  background: #dbeafe;
  color: #1e40af;
}

.priority-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}
.priority-tag.high {
  background: #fef3c7;
  color: #b45309;
}
.priority-tag.urgent {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #b91c1c;
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: calc(100vh - 178px);
  overflow-y: auto;
  padding-bottom: 20px;
  padding: 18px 20px 20px;
}

.drawer-tabs {
  flex: 0 0 auto;
}

.tab-badge {
  background: #0369a1;
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  margin-left: 6px;
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

.diagram-pane {
  min-height: 200px;
}

.approve-section {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  padding: 16px;
}

.approve-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 12px;
}

.form-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 0;
  color: #64748b;
}

.dynamic-form-section {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid #d7dde7;
  border-radius: 8px;
  background: #f8fafc;
}

.dynamic-form-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.dynamic-form-title {
  font-size: 14px;
  font-weight: 700;
  color: #172033;
}

.dynamic-form-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #667085;
}

.dynamic-form-key {
  max-width: 180px;
  padding: 3px 8px;
  border: 1px solid #d7dde7;
  border-radius: 999px;
  background: #fff;
  color: #475467;
  font-size: 12px;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.delegate-user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.delegate-user-display {
  flex: 1;
  min-height: 36px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 6px 12px;
  background: #f8fafc;
}

.delegate-user-name {
  font-weight: 600;
  color: #0f172a;
}

.delegate-user-id {
  font-size: 12px;
  color: #64748b;
}

.delegate-placeholder {
  color: #94a3b8;
  font-size: 13px;
}

.flow-task-detail-modal {
  width: min(1120px, calc(100vw - 32px));
}

@media (max-width: 760px) {
  .flow-task-detail-modal {
    width: 100vw;
    height: 100vh;
    margin: 0;
  }

  .drawer-header,
  .dynamic-form-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .drawer-body {
    max-height: calc(100vh - 126px);
    padding: 14px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .action-buttons,
  .delegate-user-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
