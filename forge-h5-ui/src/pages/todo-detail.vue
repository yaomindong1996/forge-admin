<template>
  <view class="todo-detail-page">
    <view class="detail-nav">
      <button class="nav-back" @click="goBack">
        <AiIcon icon="/static/icons/ai-icon/arrow-left.svg" color="#1f2329" size="sm" />
      </button>
      <text class="nav-title">审批详情</text>
      <button class="nav-more" @click="refresh">
        <AiIcon icon="/static/icons/ai-icon/refresh-cw.svg" color="#4e5969" size="sm" />
      </button>
    </view>

    <scroll-view class="detail-scroll" scroll-y :show-scrollbar="false">
      <view v-if="loading" class="detail-skeleton">
        <AiListSkeleton :rows="2" />
        <AiListSkeleton :rows="4" compact />
      </view>
      <template v-else-if="task">
        <view class="task-summary">
          <text class="task-title">{{ taskTitle(task) }}</text>
          <text class="task-node">{{ task.taskName || task.name || '审批节点' }}</text>
          <view class="task-facts">
            <view class="task-fact"><text>申请人</text><text>{{ task.startUserName || task.createByName || '-' }}</text></view>
            <view class="task-fact"><text>发起部门</text><text>{{ task.startDeptName || '-' }}</text></view>
            <view class="task-fact"><text>流程分类</text><text>{{ task.categoryName || task.category || '-' }}</text></view>
            <view class="task-fact"><text>提交时间</text><text>{{ task.createTime || task.startTime || '-' }}</text></view>
          </view>
        </view>

        <AiTabs v-model="activeTabIndex" :tabs="detailTabs" class="detail-tabs">
          <AiTab :index="0">
          <view v-if="formLoading" class="page-hint">正在加载表单…</view>
          <view v-else-if="blockedReason" class="blocked-panel">
            <AiIcon icon="/static/icons/ai-icon/info.svg" color="#1677ff" size="md" />
            <text class="blocked-title">请在 PC 端处理</text>
            <text class="blocked-copy">{{ blockedReason }}</text>
          </view>

          <view v-else class="content-panel">
            <view v-if="businessProviderUnavailable" class="form-provider-notice">
              <text>流程服务未加载该业务表单 Provider，当前仅能展示表单字段结构；部署 Provider 后会自动加载实际数据和节点权限。</text>
            </view>
            <view v-if="businessFormHasWritableFields" class="form-panel-head">
              <text>业务表单</text>
              <button class="save-form-button" :disabled="actionLoading || formSaving" @click="saveBusinessFields">
                {{ formSaving ? '暂存中' : '暂存修改' }}
              </button>
            </view>
            <PageSectionRenderer
              v-if="hasLowcodeForm"
              :sections="pageSections"
              :main-fields="mainFields"
              :main-data="mainData"
              :children="allChildren"
              :child-data="childData"
              :mode="formMode"
              :dict-options="dictOptions"
              :runtime-context="runtimeContext"
              :flow-interaction="flowInteraction"
              :current-flow-node-key="currentFlowNodeKey"
              @set-main-form-ref="setMainFormRef"
              @set-child-form-ref="setChildFormRef"
            />
            <view v-else-if="formSchemaUnavailable" class="form-schema-notice">
              <text>该流程未返回可展示的业务字段配置，已隐藏内部字段和技术标识。</text>
            </view>

            <view v-if="responsibilityDescription || approvalPoints.length" class="approval-duty-panel">
              <view v-if="responsibilityDescription" class="duty-block">
                <text class="form-label">审批职责</text>
                <text class="duty-copy">{{ responsibilityDescription }}</text>
              </view>
              <view v-if="approvalPoints.length" class="duty-block">
                <text class="form-label">审批要点</text>
                <view
                  v-for="point in approvalPoints"
                  :key="point.id"
                  class="approval-point-row"
                  @click="toggleApprovalPoint(point)"
                >
                  <text class="approval-point-check">{{ approvalPointChecks[point.id] ? '☑' : '☐' }}</text>
                  <text class="approval-point-copy">{{ point.content }}</text>
                  <text class="approval-point-tag">{{ point.required ? '必审' : '非必审' }}</text>
                </view>
              </view>
            </view>
            <view v-if="!readonlyMode" class="comment-row">
              <text class="form-label">审批意见<text v-if="requireComment" class="required-mark"> *</text></text>
              <textarea v-model="comment" class="form-textarea comment" maxlength="500" placeholder="请输入审批意见" />
              <view v-if="commentPhrases.length" class="comment-presets">
                <button
                  v-for="phrase in commentPhrases"
                  :key="phrase.id"
                  class="comment-preset"
                  :class="{ active: comment === phrase.content }"
                  @click="comment = phrase.content"
                >
                  {{ phrase.content }}
                </button>
              </view>
              <view class="comment-preset-actions">
                <button
                  v-if="canSaveCommentPhrase"
                  class="comment-preset-link"
                  :disabled="phraseSaving"
                  @click="saveCommentPhrase"
                >
                  {{ phraseSaving ? '保存中' : '存为常用' }}
                </button>
              </view>
            </view>
            <view v-if="!readonlyMode && requireSignature" class="comment-row signature-row">
              <text class="form-label">手写签名<text class="required-mark"> *</text></text>
              <AiSignaturePad ref="approvalSignatureRef" v-model="signature" />
            </view>
          </view>
          </AiTab>
          <AiTab :index="1">
        <view class="history-panel">
          <AiListSkeleton v-if="historyLoading" :rows="4" compact />
          <view v-else-if="history.length" class="timeline">
            <view v-for="item in history" :key="historyKey(item)" class="timeline-item">
              <view class="timeline-dot" />
              <view class="timeline-copy">
                <text class="timeline-title">{{ item.activityName || item.taskName || item.name || '流程节点' }}</text>
                <text class="timeline-meta">{{ item.assigneeName || item.userName || item.operatorName || '-' }} · {{ item.endTime || item.createTime || item.startTime || '-' }}</text>
                <text v-if="item.comment" class="timeline-comment">{{ item.comment }}</text>
              </view>
            </view>
          </view>
          <view v-else class="page-hint">暂无审批记录</view>
        </view>
          </AiTab>
          <AiTab :index="2">
            <view class="history-panel process-panel">
              <AiListSkeleton v-if="diagramLoading" :rows="4" compact />
              <view v-else-if="processNodes.length" class="process-nodes">
                <view v-for="node in processNodes" :key="node.nodeId || node.id" class="process-node" :class="`is-${node.status || 'pending'}`">
                  <view class="process-node__mark" />
                  <view class="process-node__copy">
                    <text>{{ node.nodeName || node.name || '流程节点' }}</text>
                    <text>{{ node.assigneeNames?.join('、') || node.assigneeName || node.comment || node.statusText || node.status || '等待处理' }}</text>
                  </view>
                </view>
              </view>
              <view v-else class="page-hint">暂无可展示的流程节点</view>
            </view>
          </AiTab>
        </AiTabs>
      </template>
      <view v-else class="page-hint">待办不存在或已处理</view>
    </scroll-view>

    <view v-if="task && !readonlyMode" class="action-bar">
      <AiButton v-if="isCandidateTask" block size="sm" :loading="claimLoading" @click="claimTask">签收后处理</AiButton>
      <template v-else>
        <AiButton v-if="canDelegate || canTerminate" class="more-action" size="sm" variant="secondary" :disabled="actionLoading" @click="moreVisible = true">
          <template #leftIcon><AiIcon icon="/static/icons/ai-icon/more-horizontal.svg" color="#475569" size="sm" /></template>
          更多
        </AiButton>
        <AiButton v-if="canReject" size="sm" variant="danger" :disabled="Boolean(blockedReason) || actionLoading" @click="canChooseReturnTarget ? openRejectTarget() : submitAction('reject')">驳回</AiButton>
        <AiButton v-if="canApprove" size="sm" :loading="actionLoading && pendingAction === 'approve'" :disabled="Boolean(blockedReason) || actionLoading" @click="submitAction('approve')">同意</AiButton>
      </template>
    </view>

    <AiPopupSheet v-model="rejectTargetVisible" title="选择驳回节点" description="流程会从所选节点继续，而不是整单结束">
      <view class="return-target-list">
        <button
          v-for="option in returnTargetOptions"
          :key="option.value"
          class="return-target-item"
          :class="{ active: selectedReturnTarget === option.value }"
          @click="selectedReturnTarget = option.value"
        >
          {{ option.label }}
        </button>
      </view>
      <view class="delegate-comment" style="margin-top: 24rpx;">
        <AiButton block :disabled="!selectedReturnTarget" :loading="actionLoading && pendingAction === 'return'" @click="submitAction('return')">确认驳回</AiButton>
      </view>
    </AiPopupSheet>

    <AiPopupSheet v-model="moreVisible" title="更多操作" description="操作权限以当前审批节点配置为准">
      <view class="more-list">
        <button v-if="canDelegate" class="more-row" @click="openDelegate">
          <view class="more-row__icon"><AiIcon icon="/static/icons/ai-icon/user-plus.svg" color="#2563eb" size="sm" /></view>
          <view class="more-row__copy"><text>转办</text><text>交由其他成员继续处理</text></view>
          <AiIcon icon="/static/icons/ai-icon/chevron-right.svg" color="#94a3b8" size="sm" />
        </button>
        <button v-if="canTerminate" class="more-row danger" @click="submitAction('terminate')">
          <view class="more-row__icon"><AiIcon icon="/static/icons/ai-icon/x-circle.svg" color="#c2410c" size="sm" /></view>
          <view class="more-row__copy"><text>终结流程</text><text>结束当前流程，不可恢复</text></view>
          <AiIcon icon="/static/icons/ai-icon/chevron-right.svg" color="#94a3b8" size="sm" />
        </button>
      </view>
    </AiPopupSheet>

    <AiPopupSheet v-model="delegateVisible" title="转办任务" description="选择处理人后，再确认转办">
      <view class="delegate-search"><AiSearchBar v-model="userKeyword" placeholder="搜索姓名或用户名" @search="loadUsers" @clear="loadUsers" /></view>
      <view v-if="delegateUser" class="delegate-choice">
        <view class="delegate-choice__avatar">{{ userInitial(delegateUser) }}</view>
        <view class="delegate-choice__copy"><text>已选择</text><text>{{ delegateUserName(delegateUser) }}</text></view>
        <AiIcon icon="/static/icons/ai-icon/check-circle.svg" color="#2563eb" size="md" />
      </view>
      <view class="user-list">
        <AiListSkeleton v-if="usersLoading" :rows="3" compact />
        <button v-for="user in users" v-else :key="user.id" class="user-row" :class="{ active: isDelegateUserSelected(user) }" @click.stop="selectDelegateUser(user)">
          <view class="user-avatar">{{ userInitial(user) }}</view>
          <view class="user-copy">
            <text class="user-name">{{ delegateUserName(user) }}</text>
            <text class="user-meta">{{ user.username }}{{ user.deptName ? ` · ${user.deptName}` : '' }}</text>
          </view>
          <view class="user-check" :class="{ active: isDelegateUserSelected(user) }"><AiIcon v-if="isDelegateUserSelected(user)" icon="/static/icons/ai-icon/check.svg" color="#ffffff" size="xs" /></view>
        </button>
        <button v-if="!usersLoading && usersHasMore" class="load-more-users" @click="loadMoreUsers">加载更多成员</button>
        <view v-if="!usersLoading && !users.length" class="page-hint">未找到可转办人员</view>
      </view>
      <view class="delegate-comment">
        <text class="form-label">转办说明<text v-if="requireComment" class="required-mark"> *</text></text>
        <textarea v-model="delegateComment" class="form-textarea" maxlength="500" placeholder="请说明转办原因" />
      </view>
      <view v-if="requireSignature" class="delegate-signature">
        <text class="form-label">手写签名<text class="required-mark"> *</text></text>
        <AiSignaturePad ref="delegateSignatureRef" v-model="delegateSignature" />
      </view>
      <template #footer>
        <AiButton block :disabled="!delegateUser" :loading="actionLoading && pendingAction === 'delegate'" @click="submitAction('delegate')">确认转办</AiButton>
      </template>
    </AiPopupSheet>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AiButton from '@/components/AiButton.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import AiSignaturePad from '@/components/AiSignaturePad.vue'
import AiTab from '@/components/AiTab.vue'
import AiTabs from '@/components/AiTabs.vue'
import PageSectionRenderer from '@/components/lowcode/PageSectionRenderer.vue'
import api from '@/api'
import { useAuthStore } from '@/store'
import { showConfirmDialog } from '@/utils/dialog'
import { toast } from '@/utils/notify'
import { normalizeDictOptions } from '@/utils/lowcode-runtime'
import {
  adaptBusinessTaskFields,
  adaptChildrenConfig,
  buildDefaultPageSections,
  extractPageSections,
  extractMainData,
  extractChildData,
  collectDictTypes,
  buildFlowInteraction,
} from '@/utils/business-task-form-adapter'

const authStore = useAuthStore()
const taskId = ref('')
const task = ref(null)
const formInfo = ref(null)
const businessContext = ref(null)
const history = ref([])
const loading = ref(true)
const formLoading = ref(true)
const formSaving = ref(false)
const historyLoading = ref(true)
const diagramLoading = ref(true)
const diagramInfo = ref(null)
const activeTabIndex = ref(0)
const detailTabs = ['业务内容', '审批记录', '流程进度']
const pageMode = ref('todo')
const approvalPointChecks = ref({})
const comment = ref('')
const commentPhrases = ref([])
const phraseSaving = ref(false)
const signature = ref('')
const approvalSignatureRef = ref(null)
const mainData = reactive({})
const childData = reactive({})
const dictOptions = reactive({})
const mainSectionFormRefs = new Map()
const childFormRefs = new Map()
const actionLoading = ref(false)
const pendingAction = ref('')
const claimLoading = ref(false)
const moreVisible = ref(false)
const delegateVisible = ref(false)
const userKeyword = ref('')
const users = ref([])
const usersLoading = ref(false)
const userPageNum = ref(1)
const userTotal = ref(0)
const usersExhausted = ref(false)
const delegateUser = ref(null)
const delegateComment = ref('')
const delegateSignature = ref('')
const delegateSignatureRef = ref(null)

const userId = computed(() => String(authStore.userInfo?.id || authStore.userInfo?.userId || authStore.userInfo?.user_id || ''))
const taskPolicySource = computed(() => formInfo.value || businessContext.value || {})
const requireComment = computed(() => taskPolicySource.value?.requireComment !== false)
const canSaveCommentPhrase = computed(() => {
  const value = String(comment.value || '').trim()
  return Boolean(value) && value.length <= 200 && !commentPhrases.value.some(item => item.content === value)
})
const responsibilityDescription = computed(() => formInfo.value?.responsibilityDescription || '')
const approvalPoints = computed(() => Array.isArray(formInfo.value?.approvalPoints) ? formInfo.value.approvalPoints : [])
const isCandidateTask = computed(() => Number(task.value?.status) === 0 && !task.value?.assignee)
const canApprove = computed(() => taskPolicySource.value?.allowApprove !== false)
const canReject = computed(() => taskPolicySource.value?.allowReject !== false)
const canReturn = computed(() => taskPolicySource.value?.allowReturn === true)
const returnTargetOptions = computed(() => (Array.isArray(taskPolicySource.value?.returnTargets)
  ? taskPolicySource.value.returnTargets
  : []).map(item => ({
  label: item.activityName || item.activityId,
  value: item.activityId,
})))
const canChooseReturnTarget = computed(() => taskPolicySource.value?.allowMultiReturn === true && returnTargetOptions.value.length > 0)
const selectedReturnTarget = ref('')
const rejectTargetVisible = ref(false)

function openRejectTarget() {
  selectedReturnTarget.value = ''
  rejectTargetVisible.value = true
}
const canDelegate = computed(() => taskPolicySource.value?.allowDelegate !== false)
const canTerminate = computed(() => taskPolicySource.value?.allowTerminate === true)
const readonlyMode = computed(() => pageMode.value === 'readonly')
const processNodes = computed(() => Array.isArray(diagramInfo.value?.nodes) ? diagramInfo.value.nodes : [])
const businessSchemaFallback = computed(() => {
  const context = businessContext.value || {}
  if (Array.isArray(context.fields) && context.fields.length) return []
  return context.formRef?.fields || context.formRef?.fieldCatalog || []
})
const businessProviderUnavailable = computed(() => {
  const warnings = Array.isArray(businessContext.value?.warnings) ? businessContext.value.warnings : []
  return warnings.some(item => String(item).includes('Provider未注册'))
})
const mainFields = computed(() => {
  const context = businessContext.value
  if (Array.isArray(context?.fields) && context.fields.length)
    return adaptBusinessTaskFields(context.fields)
  if (businessSchemaFallback.value.length)
    return adaptBusinessTaskFields(businessSchemaFallback.value)
  return adaptBusinessTaskFields(resolveTaskFormFields(formInfo.value))
})
const pageSections = computed(() =>
  extractPageSections(businessContext.value) || buildDefaultPageSections(mainFields.value),
)
const allChildren = computed(() => adaptChildrenConfig(businessContext.value?.childrenConfig || []))
const flowInteraction = computed(() => buildFlowInteraction(businessContext.value))
const currentFlowNodeKey = computed(() => String(businessContext.value?.taskDefKey || ''))
const runtimeContext = computed(() => ({
  routeQuery: { taskId: taskId.value },
  user: authStore.userInfo || {},
  currentUser: authStore.userInfo || {},
}))
const hasLowcodeForm = computed(() => mainFields.value.length > 0)
const formMode = computed(() => {
  if (readonlyMode.value || businessProviderUnavailable.value || businessSchemaFallback.value.length > 0)
    return 'detail'
  return mainFields.value.some(field => !field.readonly) ? 'edit' : 'detail'
})
const businessFormHasWritableFields = computed(() =>
  mainFields.value.some(field => !field.readonly) && formMode.value === 'edit',
)
const formSchemaUnavailable = computed(() =>
  !hasLowcodeForm.value && Boolean(
    Object.keys(extractMainData(businessContext.value?.recordData) || formInfo.value?.variables || {}).length,
  ),
)
const blockedReason = computed(() => {
  if (formInfo.value?.formType === 'external' && formInfo.value?.formUrl && !hasLowcodeForm.value)
    return '此节点未提供可移动端渲染的字段描述，不能跳过 PC 专属表单直接审批。'
  if (formInfo.value?.formType === 'dynamic' && formInfo.value?.formJson && !hasLowcodeForm.value)
    return '此动态表单没有可识别的字段描述，不能跳过填写直接审批。'
  return ''
})

onLoad(async (options = {}) => {
  taskId.value = String(options.taskId || '')
  pageMode.value = options.mode === 'readonly' ? 'readonly' : 'todo'
  await Promise.all([refresh(), loadCommentPhrases()])
})

async function loadCommentPhrases() {
  try {
    const res = await api.listUsableCommentPhrases()
    commentPhrases.value = Array.isArray(res?.data) ? res.data.filter(item => item?.id && item?.content) : []
  }
  catch (error) {
    commentPhrases.value = []
    console.warn('加载常用审批意见失败:', error)
  }
}

async function saveCommentPhrase() {
  const value = String(comment.value || '').trim()
  if (!value) {
    toast('请输入审批意见', { type: 'warning' })
    return
  }
  phraseSaving.value = true
  try {
    await api.createCommentPhrase({ content: value, scene: 'ALL', ownerType: 1 })
    toast('已保存为常用意见', { type: 'success' })
    await loadCommentPhrases()
  }
  catch (error) {
    toast(error?.message || '保存常用意见失败', { type: 'error' })
  }
  finally {
    phraseSaving.value = false
  }
}

async function refresh() {
  if (!taskId.value) return
  loading.value = true
  formLoading.value = true
  historyLoading.value = true
  diagramLoading.value = true
  formInfo.value = null
  businessContext.value = null
  history.value = []
  diagramInfo.value = null
  try {
    task.value = readCachedTask(taskId.value)
    try {
      const detail = await api.getFlowTaskDetail(taskId.value)
      task.value = detail?.data || task.value
    }
    catch (error) {
      if (!task.value) throw error
      console.warn('读取运行中任务详情失败，改用列表摘要:', error)
    }
    if (!task.value) return
    const currentTaskId = task.value.taskId || task.value.id || taskId.value
    const [businessResult, historyResult, diagramResult] = await Promise.allSettled([
      readonlyMode.value ? loadReadonlyBusinessContext({ taskId: currentTaskId }) : loadBusinessContext({ taskId: currentTaskId }),
      task.value.processInstanceId ? api.getFlowTaskHistory(task.value.processInstanceId) : Promise.resolve({ data: [] }),
      task.value.processInstanceId ? api.getFlowDiagramInfo(task.value.processInstanceId) : Promise.resolve({ data: null }),
    ])
    const isBusinessManaged = businessResult.status === 'fulfilled' && isConfiguredBusinessTaskForm(businessResult.value)
    if (!isBusinessManaged) {
      const formResult = readonlyMode.value
        ? await api.getFlowProcessForm(compact({ taskId: currentTaskId, processInstanceId: task.value.processInstanceId, businessKey: task.value.businessKey, processDefKey: task.value.processDefKey || task.value.processDefinitionKey, taskDefKey: task.value.taskDefKey || task.value.taskDefinitionKey }))
        : await api.getFlowTaskForm(currentTaskId)
      formInfo.value = formResult?.data || null
      seedApprovalPointChecks(formInfo.value)
      seedMainData(formInfo.value?.variables)
    }
    if (historyResult.status === 'fulfilled') history.value = Array.isArray(historyResult.value?.data) ? historyResult.value.data : []
    if (diagramResult.status === 'fulfilled') diagramInfo.value = diagramResult.value?.data || null
  }
  catch (error) {
    console.error('加载审批详情失败:', error)
    toast(resolveErrorMessage(error, '审批详情加载失败'), { type: 'error' })
  }
  finally {
    loading.value = false
    formLoading.value = false
    historyLoading.value = false
    diagramLoading.value = false
  }
}

async function loadReadonlyBusinessContext(overrides = {}) {
  const query = buildBusinessContextQuery(overrides)
  if (!hasBusinessContextQuery(query)) return null
  try {
    const res = await api.getBusinessTaskReadonlyContext(query)
    businessContext.value = res?.data || null
    seedMainData(extractMainData(businessContext.value?.recordData))
    seedChildData(extractChildData(businessContext.value?.recordData))
    await loadDictOptions()
    return businessContext.value
  }
  catch (error) {
    console.error('加载只读业务表单失败:', error)
    return null
  }
}

async function loadBusinessContext(overrides = {}) {
  const query = buildBusinessContextQuery(overrides)
  if (!hasBusinessContextQuery(query)) return null
  try {
    const res = await api.getBusinessTaskFormContext(query)
    businessContext.value = res?.data || null
    seedMainData(extractMainData(businessContext.value?.recordData))
    seedChildData(extractChildData(businessContext.value?.recordData))
    await loadDictOptions()
    return businessContext.value
  }
  catch (error) {
    console.error('加载业务表单失败:', error)
    return null
  }
}

function buildBusinessContextQuery(overrides = {}) {
  const info = formInfo.value || {}
  return compact({
    taskId: overrides.taskId || info.taskId || task.value?.taskId || taskId.value,
    businessKey: info.businessKey || task.value?.businessKey,
    processInstanceId: info.processInstanceId || task.value?.processInstanceId,
    processDefKey: info.processDefKey || task.value?.processDefKey || task.value?.processDefinitionKey,
    taskDefKey: info.taskDefKey || task.value?.taskDefKey || task.value?.taskDefinitionKey,
    objectCode: info.objectCode || task.value?.objectCode,
    recordId: info.recordId || task.value?.recordId,
    formKey: info.formKey,
  })
}
function hasBusinessContextQuery(query) {
  return Boolean(query.taskId || query.businessKey || query.processInstanceId || (query.objectCode && query.recordId))
}

function isConfiguredBusinessTaskForm(context) {
  return context?.configured === true && ['business-object', 'business-code'].includes(context?.formType)
}

function seedApprovalPointChecks(info) {
  approvalPointChecks.value = Object.fromEntries(
    (Array.isArray(info?.approvalPoints) ? info.approvalPoints : []).map(point => [point.id, false]),
  )
}

function toggleApprovalPoint(point) {
  if (readonlyMode.value || !point?.id)
    return
  approvalPointChecks.value = {
    ...approvalPointChecks.value,
    [point.id]: !approvalPointChecks.value[point.id],
  }
}

function seedMainData(source = {}) {
  if (!source || typeof source !== 'object') return
  Object.entries(source).forEach(([key, value]) => {
    if (mainData[key] === undefined) mainData[key] = value == null ? '' : value
  })
}

function seedChildData(source = {}) {
  if (!source || typeof source !== 'object' || Array.isArray(source)) return
  Object.entries(source).forEach(([key, value]) => {
    if (Array.isArray(value)) childData[key] = value
  })
}

async function loadDictOptions() {
  const types = collectDictTypes(mainFields.value, allChildren.value)
  await Promise.all([...types].map(async type => {
    try { dictOptions[type] = normalizeDictOptions((await api.getDictOptions(type))?.data) }
    catch { dictOptions[type] = [] }
  }))
}

function setMainFormRef({ sectionId, instance }) {
  const key = String(sectionId || 'main')
  if (instance) mainSectionFormRefs.set(key, instance)
  else mainSectionFormRefs.delete(key)
}

function setChildFormRef({ child, row, rowIndex, instance }) {
  if (!child) return
  const key = `${child.modelCode}:${row?.id || rowIndex || 0}`
  if (instance) childFormRefs.set(key, instance)
  else childFormRefs.delete(key)
}

async function claimTask() {
  claimLoading.value = true
  try {
    await api.claimFlowTask(task.value.taskId || task.value.id, userId.value)
    toast('签收成功', { type: 'success' })
    await refresh()
  }
  catch (error) {
    console.error('签收失败:', error)
    toast(resolveErrorMessage(error, '签收失败'), { type: 'error' })
  }
  finally {
    claimLoading.value = false
  }
}

async function openDelegate() {
  moreVisible.value = false
  delegateVisible.value = true
  delegateUser.value = null
  userKeyword.value = ''
  delegateComment.value = ''
  delegateSignature.value = ''
  userPageNum.value = 1
  userTotal.value = 0
  usersExhausted.value = false
  await loadUsers()
}

function selectDelegateUser(user) {
  delegateUser.value = user || null
}

function isDelegateUserSelected(user) {
  return String(delegateUser.value?.id || '') === String(user?.id || '')
}

function delegateUserName(user = {}) {
  return user.realName || user.name || user.nickname || user.username || '未命名成员'
}

function userInitial(user = {}) {
  return String(delegateUserName(user)).slice(0, 1).toUpperCase()
}

async function loadUsers() {
  userPageNum.value = 1
  users.value = []
  userTotal.value = 0
  usersExhausted.value = false
  await fetchUsers()
}

const usersHasMore = computed(() => !usersExhausted.value)

async function loadMoreUsers() {
  if (usersLoading.value || !usersHasMore.value) return
  await fetchUsers()
}

async function fetchUsers() {
  usersLoading.value = true
  try {
    const res = await api.getUserPage({ pageNum: userPageNum.value, pageSize: 30, keyword: userKeyword.value.trim() || undefined })
    const page = res?.data || {}
    const records = Array.isArray(page.records) ? page.records : []
    const selfId = String(userId.value || '')
    const currentAssignee = String(task.value?.assignee || '')
    const candidates = records.filter(user => String(user.id || '') !== selfId && String(user.id || '') !== currentAssignee)
    users.value = userPageNum.value === 1 ? candidates : users.value.concat(candidates)
    userTotal.value = Number(page.total || 0)
    usersExhausted.value = records.length < 30 || userPageNum.value * 30 >= userTotal.value
    userPageNum.value += 1
  }
  catch (error) {
    users.value = []
    console.error('加载转办人员失败:', error)
    toast(resolveErrorMessage(error, '转办人员加载失败'), { type: 'error' })
  }
  finally {
    usersLoading.value = false
  }
}

async function submitAction(action) {
  if (blockedReason.value) return
  if (action === 'delegate' && !delegateUser.value) {
    toast('请选择转办人员', { type: 'warning' })
    return
  }
  if (action === 'reject' && canChooseReturnTarget.value) {
    openRejectTarget()
    return
  }
  if (action === 'return' && canChooseReturnTarget.value && !selectedReturnTarget.value) {
    toast('请选择驳回至哪个已审批节点', { type: 'warning' })
    return
  }
  const actionComment = action === 'delegate' ? delegateComment.value : comment.value
  const actionSignature = action === 'delegate' ? delegateSignature.value : signature.value
  const signatureRef = action === 'delegate' ? delegateSignatureRef.value : approvalSignatureRef.value
  if (requireComment.value && !actionComment.trim()) {
    toast('请输入审批意见', { type: 'warning' })
    return
  }
  if (action === 'approve' && approvalPoints.value.some(point => point?.required === true && !approvalPointChecks.value?.[point.id])) {
    toast('请完成全部必审要点', { type: 'warning' })
    return
  }
  if (!validateRequiredFields() || !hasSignature(actionSignature, signatureRef)) return
  const labels = { approve: '同意', reject: '驳回', return: '退回', terminate: '终结流程', delegate: '转办' }
  const confirmed = await showConfirmDialog({ title: `确认${labels[action]}`, description: '提交后将按当前流程策略执行，不能撤销。', confirmText: labels[action], isDestructive: ['reject', 'terminate'].includes(action) })
  if (!confirmed) return

  actionLoading.value = true
  pendingAction.value = action
  try {
    await saveBusinessFieldsIfNeeded(action)
    const resolvedSignature = await resolveSignature(actionSignature, signatureRef)
    if (action === 'delegate') delegateSignature.value = resolvedSignature
    else signature.value = resolvedSignature
    const payload = buildActionPayload(action, actionComment, resolvedSignature)
    if (isConfiguredBusinessTaskForm(businessContext.value) && ['approve', 'reject'].includes(action)) {
      await api.completeBusinessTaskAction(payload)
    }
    else if (action === 'approve') await api.approveFlowTask(payload)
    else if (action === 'reject') await api.rejectFlowTask(payload)
    else if (action === 'return') await api.returnFlowTask({ ...payload, targetActivityId: selectedReturnTarget.value || undefined })
    else if (action === 'terminate') await api.terminateFlowTask(payload)
    else await api.delegateFlowTask(payload)
    toast(`${labels[action]}成功`, { type: 'success' })
    delegateVisible.value = false
    rejectTargetVisible.value = false
    setTimeout(() => uni.navigateBack(), 350)
  }
  catch (error) {
    console.error('提交审批动作失败:', error)
    toast(resolveErrorMessage(error, `${labels[action] || '提交'}失败`), { type: 'error' })
  }
  finally {
    actionLoading.value = false
    pendingAction.value = ''
  }
}

function buildActionPayload(action, actionComment = comment.value.trim(), actionSignature = signature.value) {
  const info = formInfo.value || {}
  const base = compact({
    action,
    taskId: info.taskId || task.value?.taskId || task.value?.id,
    businessKey: businessContext.value?.businessKey || info.businessKey || task.value?.businessKey,
    processInstanceId: businessContext.value?.processInstanceId || info.processInstanceId || task.value?.processInstanceId,
    processDefKey: businessContext.value?.processDefKey || info.processDefKey || task.value?.processDefKey,
    taskDefKey: businessContext.value?.taskDefKey || info.taskDefKey || task.value?.taskDefKey,
    objectCode: businessContext.value?.objectCode || info.objectCode || task.value?.objectCode,
    recordId: businessContext.value?.recordId || info.recordId || task.value?.recordId,
    formKey: businessContext.value?.formKey || info.formKey,
    userId: userId.value,
    comment: actionComment.trim(),
    signature: actionSignature || undefined,
    targetUserId: action === 'delegate' ? String(delegateUser.value?.id || '') : undefined,
    variables: { ...(info.variables || {}), ...mainData },
    approvalPointResults: approvalPoints.value.map(point => ({
      id: point.id,
      content: point.content,
      required: point.required === true,
      checked: Boolean(approvalPointChecks.value?.[point.id]),
    })),
  })
  return base
}

async function saveBusinessFieldsIfNeeded(action) {
  if (!['approve', 'reject', 'return'].includes(action) || !isConfiguredBusinessTaskForm(businessContext.value) || !businessFormHasWritableFields.value) return null
  const payload = buildActionPayload(action)
  const res = await api.saveBusinessTaskFormContext({ ...payload, data: { ...mainData } })
  businessContext.value = res?.data || businessContext.value
  seedMainData(extractMainData(businessContext.value?.recordData))
  return businessContext.value
}

async function saveBusinessFields() {
  if (!businessFormHasWritableFields.value || !validateRequiredFields()) return
  formSaving.value = true
  try {
    const payload = buildActionPayload('approve')
    const res = await api.saveBusinessTaskFormContext({ ...payload, data: { ...mainData } })
    businessContext.value = res?.data || businessContext.value
    seedMainData(extractMainData(businessContext.value?.recordData))
    toast('修改已暂存', { type: 'success' })
  }
  catch (error) {
    console.error('暂存业务表单失败:', error)
    toast(resolveErrorMessage(error, '暂存修改失败'), { type: 'error' })
  }
  finally {
    formSaving.value = false
  }
}

function validateRequiredFields() {
  const forms = [...mainSectionFormRefs.values(), ...childFormRefs.values()]
  const invalid = forms.find(form => form?.validate?.() === false)
  if (invalid) {
    toast('请完善必填字段', { type: 'warning' })
    return false
  }
  return true
}

function parseJson(value) {
  if (!value || typeof value === 'object') return value || []
  try {
    const parsed = JSON.parse(value)
    return typeof parsed === 'string' ? parseJson(parsed) : parsed
  }
  catch { return [] }
}

function resolveTaskFormFields(info = {}) {
  const candidates = [
    parseJson(info?.formJson),
    info?.fields,
    info?.formRef?.fields,
    info?.fieldCatalog,
    info?.formRef?.fieldCatalog,
    info?.formRef,
    info,
  ]
  return candidates.find(candidate => adaptBusinessTaskFields(candidate).length) || []
}

function compact(source) {
  return Object.fromEntries(Object.entries(source).filter(([, value]) => value !== undefined && value !== null && value !== ''))
}

function resolveErrorMessage(error, fallback) {
  const message = error?.data?.message
    || error?.response?.data?.message
    || error?.error?.data?.message
    || error?.message
    || error?.msg
  return message && String(message).trim() ? String(message) : fallback
}
function hasSignature(value, signatureRef) {
  if (!taskPolicySource.value?.requireSignature) return true
  if (String(value || '').trim()) return true
  if (signatureRef?.hasSignature?.()) return true
  toast('请完成手写签名', { type: 'warning' })
  return false
}
async function resolveSignature(value, signatureRef) {
  if (!taskPolicySource.value?.requireSignature) return value || ''
  if (String(value || '').trim() && !signatureRef?.hasSignature?.()) return value
  return signatureRef?.upload ? signatureRef.upload() : value || ''
}
function taskTitle(value = {}) { return value.title || value.businessTitle || value.processName || value.processDefinitionName || value.taskName || '审批任务' }
function historyKey(item) { return item.id || item.taskId || `${item.activityName || item.taskName}-${item.startTime || item.createTime}` }
function readCachedTask(id) { try { return uni.getStorageSync(`flow-task:${id}`) || null } catch { return null } }
function goBack() { uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/todo' }) }) }
</script>

<style lang="scss" scoped>
.todo-detail-page { display: flex; height: 100vh; flex-direction: column; background: var(--page-bg); }
.detail-nav { display: flex; height: calc(88rpx + env(safe-area-inset-top)); align-items: flex-end; gap: 18rpx; padding: 0 24rpx 14rpx; background: var(--page-bg); box-sizing: border-box; }
.nav-back, .nav-more { display: flex; width: 56rpx; height: 56rpx; align-items: center; justify-content: center; margin: 0; padding: 0; border: 0; border-radius: 10rpx; background: transparent; }
.nav-back::after, .nav-more::after { border: 0; }
.nav-title { flex: 1; color: var(--text-strong); font-size: 32rpx; font-weight: 600; text-align: center; }
.detail-scroll { height: 0; flex: 1; }
.detail-skeleton { display: flex; flex-direction: column; gap: 20rpx; padding: 24rpx; }
.task-summary, .content-panel, .history-panel { margin: 24rpx; padding: 28rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.task-title, .task-node, .task-fact text, .form-label, .form-readonly, .timeline-title, .timeline-meta, .timeline-comment, .blocked-title, .blocked-copy, .user-name, .user-meta, .business-child-head text, .business-child-field text { display: block; }
.task-title { color: var(--text-strong); font-size: 34rpx; font-weight: 600; line-height: 1.4; }
.task-node { margin-top: 12rpx; color: var(--primary-color); font-size: 25rpx; }
.task-facts { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18rpx 24rpx; margin-top: 24rpx; }
.task-fact { min-width: 0; }
.task-fact text:first-child { color: #94a3b8; font-size: 21rpx; }
.task-fact text:last-child { overflow: hidden; margin-top: 5rpx; color: #4e5969; font-size: 23rpx; text-overflow: ellipsis; white-space: nowrap; }
.detail-tabs { margin: 0 24rpx; }
.detail-tabs :deep(.ai-tabs-content) { min-width: 0; }
.form-panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; color: var(--text-strong); font-size: 28rpx; font-weight: 650; }
.form-provider-notice { margin-bottom: 22rpx; padding: 16rpx 18rpx; border: 1rpx solid #fde7b2; border-radius: 10rpx; color: #8a5a00; font-size: 22rpx; line-height: 1.55; background: #fffbeb; }
.form-provider-notice text { display: block; }
.form-schema-notice { padding: 32rpx 18rpx; border: 1rpx dashed #d7dee8; border-radius: 10rpx; color: #64748b; font-size: 24rpx; line-height: 1.6; text-align: center; background: #fafcff; }
.form-schema-notice text { display: block; }
.save-form-button { height: 54rpx; margin: 0; padding: 0 16rpx; border: 1rpx solid #bfdbfe; border-radius: 8rpx; color: var(--primary-color); font-size: 22rpx; line-height: 52rpx; background: #f8fbff; }
.save-form-button::after { border: 0; }
.save-form-button[disabled] { opacity: .55; }
.field-list { display: flex; flex-direction: column; gap: 26rpx; }
.form-row, .comment-row { display: flex; flex-direction: column; gap: 14rpx; }
.approval-duty-panel { display: flex; flex-direction: column; gap: 20rpx; margin-bottom: 16rpx; }
.duty-block { display: flex; flex-direction: column; gap: 10rpx; padding: 18rpx; border: 1px solid #eadfc9; border-radius: 12rpx; background: #fffaf0; }
.duty-copy { color: #475569; font-size: 26rpx; line-height: 1.6; white-space: pre-wrap; }
.approval-point-row { display: flex; align-items: flex-start; gap: 12rpx; }
.approval-point-check { color: #1677ff; font-size: 30rpx; line-height: 1.2; }
.approval-point-copy { flex: 1; color: #334155; font-size: 26rpx; line-height: 1.5; }
.approval-point-tag { color: #b42318; font-size: 22rpx; }
.form-label { color: #4e5969; font-size: 25rpx; }
.required-mark { color: #f53f3f; }
.form-input, .form-textarea { width: 100%; padding: 18rpx; border: 1rpx solid var(--border-color); border-radius: 8rpx; color: var(--text-strong); font-size: 27rpx; background: #fff; box-sizing: border-box; }
.form-input { height: 78rpx; }
.form-textarea { min-height: 148rpx; line-height: 1.5; }
.form-textarea.comment { margin-top: 4rpx; }
.comment-presets { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 12rpx; }
.comment-preset { max-width: 100%; height: 52rpx; padding: 0 16rpx; border: 1rpx solid var(--border-color); border-radius: 8rpx; color: #475569; font-size: 22rpx; line-height: 50rpx; background: #fff; }
.comment-preset::after,
.comment-preset-link::after { border: 0; }
.comment-preset.active { border-color: var(--primary-color); color: var(--primary-color); }
.comment-preset-actions { display: flex; justify-content: flex-end; }
.comment-preset-link { padding: 0; border: 0; color: var(--primary-color); font-size: 22rpx; line-height: 40rpx; background: transparent; }
.comment-preset-link[disabled] { opacity: .55; }
.return-target-list { display: flex; flex-direction: column; gap: 12rpx; margin-top: 8rpx; }
.return-target-item { width: 100%; min-height: 72rpx; padding: 16rpx 18rpx; border: 1rpx solid var(--border-color); border-radius: 10rpx; color: var(--text-strong); font-size: 26rpx; text-align: left; background: #fff; }
.return-target-item.active { border-color: var(--primary-color); color: var(--primary-color); background: #eff6ff; }
.form-select { width: 100%; }
.form-radio-group { padding: 2rpx 0; }
.form-date-picker, .form-file-value { display: flex; min-height: 78rpx; align-items: center; justify-content: space-between; gap: 16rpx; padding: 0 18rpx; border: 1rpx solid var(--border-color); border-radius: 8rpx; color: var(--text-strong); font-size: 27rpx; background: #fff; box-sizing: border-box; }
.form-date-picker.is-placeholder { color: #94a3b8; }
.form-file-value { justify-content: flex-start; color: #4e5969; }
.form-file-value text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.form-readonly { min-height: 42rpx; padding: 16rpx 0; color: #4e5969; font-size: 27rpx; }
.business-children { display: flex; flex-direction: column; gap: 18rpx; margin-top: 30rpx; padding-top: 24rpx; border-top: 1rpx solid #edf0f3; }
.business-child-card { overflow: hidden; border: 1rpx solid #e8edf3; border-radius: 14rpx; }
.business-child-head { display: flex; align-items: center; justify-content: space-between; padding: 16rpx 18rpx; color: var(--text-strong); font-size: 25rpx; font-weight: 650; background: #f8fafc; }
.business-child-head text:last-child { color: #94a3b8; font-size: 21rpx; font-weight: 400; }
.business-child-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18rpx; padding: 18rpx; border-top: 1rpx solid #edf0f3; }
.business-child-field { min-width: 0; }
.business-child-field text:first-child { color: #94a3b8; font-size: 20rpx; }
.business-child-field text:last-child { overflow: hidden; margin-top: 5rpx; color: #4e5969; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
.blocked-panel { display: flex; flex-direction: column; align-items: flex-start; gap: 14rpx; margin: 24rpx; padding: 32rpx; border: 1rpx solid #b7d7ff; border-radius: var(--radius-card); background: #f0f7ff; }
.blocked-title { color: var(--text-strong); font-size: 29rpx; font-weight: 600; }
.blocked-copy { color: #4e5969; font-size: 25rpx; line-height: 1.6; }
.page-hint { padding: 80rpx 32rpx; color: var(--text-muted); font-size: 26rpx; text-align: center; }
.timeline { padding: 4rpx 0; }
.timeline-item { position: relative; display: flex; gap: 20rpx; padding-bottom: 28rpx; }
.timeline-item:not(:last-child)::before { content: ''; position: absolute; top: 20rpx; bottom: 0; left: 8rpx; width: 2rpx; background: #e5e6eb; }
.timeline-dot { position: relative; z-index: 1; width: 18rpx; height: 18rpx; margin-top: 8rpx; border-radius: 50%; background: var(--primary-color); }
.timeline-copy { min-width: 0; flex: 1; }
.timeline-title { color: var(--text-strong); font-size: 27rpx; font-weight: 600; }
.timeline-meta { margin-top: 8rpx; color: var(--text-muted); font-size: 23rpx; line-height: 1.5; }
.timeline-comment { margin-top: 12rpx; color: #4e5969; font-size: 24rpx; line-height: 1.5; }
.process-nodes { display: flex; flex-direction: column; gap: 0; }
.process-node { position: relative; display: flex; gap: 16rpx; padding: 0 0 24rpx; }
.process-node:not(:last-child)::after { position: absolute; top: 20rpx; bottom: 0; left: 8rpx; width: 2rpx; background: #e5e7eb; content: ''; }
.process-node__mark { position: relative; z-index: 1; width: 18rpx; height: 18rpx; margin-top: 6rpx; border: 4rpx solid #cbd5e1; border-radius: 50%; background: #fff; box-sizing: border-box; }
.process-node.is-running .process-node__mark { border-color: #2563eb; background: #2563eb; box-shadow: 0 0 0 6rpx #dbeafe; }
.process-node.is-completed .process-node__mark { border-color: #16a34a; background: #16a34a; }
.process-node__copy { min-width: 0; flex: 1; }
.process-node__copy text { display: block; }
.process-node__copy text:first-child { color: var(--text-strong); font-size: 26rpx; font-weight: 650; }
.process-node__copy text:last-child { overflow: hidden; margin-top: 6rpx; color: #64748b; font-size: 22rpx; line-height: 1.45; text-overflow: ellipsis; white-space: nowrap; }
.action-bar { display: flex; align-items: center; gap: 12rpx; padding: 12rpx 24rpx calc(12rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid var(--border-color); background: #fff; }
.action-bar :deep(.ai-button) { flex: 1; padding: 0 18rpx; }
.action-bar :deep(.ai-button--block) { width: 100%; }
.action-bar :deep(.more-action) { flex: 0 0 144rpx; padding: 0 12rpx; }
.more-list, .user-list { display: flex; flex-direction: column; gap: 12rpx; }
.more-row { display: flex; width: 100%; min-height: 104rpx; align-items: center; gap: 16rpx; margin: 0; padding: 14rpx 6rpx; border: 0; border-bottom: 1rpx solid #edf0f3; color: var(--text-strong); font-size: 28rpx; text-align: left; background: #fff; box-sizing: border-box; }
.more-row__icon { display: flex; width: 54rpx; height: 54rpx; flex: 0 0 54rpx; align-items: center; justify-content: center; border-radius: 12rpx; background: #eff6ff; }
.more-row__copy { min-width: 0; flex: 1; }
.more-row__copy text { display: block; }
.more-row__copy text:first-child { color: var(--text-strong); font-size: 27rpx; font-weight: 650; }
.more-row__copy text:last-child { overflow: hidden; margin-top: 5rpx; color: #94a3b8; font-size: 21rpx; text-overflow: ellipsis; white-space: nowrap; }
.more-row.danger .more-row__icon { background: #fff7ed; }
.more-row.danger .more-row__copy text:first-child { color: #c2410c; }
.delegate-search { margin-bottom: 16rpx; }
.delegate-comment, .delegate-signature { display: flex; flex-direction: column; gap: 12rpx; margin-top: 18rpx; }
.delegate-choice { display: flex; align-items: center; gap: 14rpx; margin-bottom: 16rpx; padding: 14rpx 16rpx; border: 1rpx solid #bfdbfe; border-radius: 12rpx; background: #f8fbff; }
.delegate-choice__avatar, .user-avatar { display: flex; align-items: center; justify-content: center; border-radius: 50%; color: #1d4ed8; font-weight: 700; background: #dbeafe; }
.delegate-choice__avatar { width: 52rpx; height: 52rpx; flex: 0 0 52rpx; font-size: 24rpx; }
.delegate-choice__copy { min-width: 0; flex: 1; }
.delegate-choice__copy text { display: block; }
.delegate-choice__copy text:first-child { color: #64748b; font-size: 20rpx; }
.delegate-choice__copy text:last-child { overflow: hidden; margin-top: 3rpx; color: var(--text-strong); font-size: 26rpx; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.user-row { display: flex; width: 100%; min-height: 88rpx; align-items: center; gap: 14rpx; margin: 0; padding: 14rpx 8rpx; border: 1rpx solid #edf0f3; border-radius: 12rpx; color: var(--text-strong); font-size: 28rpx; text-align: left; background: #fff; box-sizing: border-box; }
.user-avatar { width: 50rpx; height: 50rpx; flex: 0 0 50rpx; font-size: 23rpx; }
.user-copy { min-width: 0; flex: 1; }
.user-check { display: flex; width: 32rpx; height: 32rpx; flex: 0 0 32rpx; align-items: center; justify-content: center; border: 1rpx solid #cbd5e1; border-radius: 50%; box-sizing: border-box; }
.user-check.active { border-color: #2563eb; background: #2563eb; }
.more-row::after, .user-row::after { border: 0; }
.user-row.active { border-color: #93c5fd; background: #f8fbff; }
.user-name { color: var(--text-strong); font-size: 27rpx; }
.user-meta { margin-top: 6rpx; color: var(--text-muted); font-size: 22rpx; }
.load-more-users { height: 60rpx; margin: 4rpx 0 0; border: 1rpx solid #e2e8f0; border-radius: 10rpx; color: #2563eb; font-size: 23rpx; background: #f8fbff; }
.load-more-users::after { border: 0; }
</style>
