/** todo.vue setup part 1. */
import { NButton, NSpace, NTreeSelect } from 'naive-ui'
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { businessFlowFormAssets, businessTaskFormContext, completeBusinessTaskAction, saveBusinessTaskFormContext } from '@/api/business-app'
import flowApi from '@/api/flow'
import { AiForm } from '@/components/ai-form'
import { formCreateToAiSchema } from '@/components/ai-form/adapters/formCreate'
import FlowBusinessForm from '@/components/common/FlowBusinessForm.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import UserSelectModal from '@/components/common/UserSelectModal.vue'
import DingFlowViewer from '@/components/flow-designer/viewer/DingFlowViewer.vue'
import FlowApprovalChecklist from '@/components/flow/FlowApprovalChecklist.vue'
import FlowCommentPhraseInput from '@/components/flow/FlowCommentPhraseInput.vue'
import FlowPrintAction from '@/components/flow/FlowPrintAction.vue'
import FlowTaskBusinessSummary from '@/components/flow/FlowTaskBusinessSummary.vue'
import FlowTaskCardList from '@/components/flow/FlowTaskCardList.vue'
import FlowTaskDetailShell from '@/components/flow/FlowTaskDetailShell.vue'
import SignaturePad from '@/components/flow/SignaturePad.vue'
import ChildTableEditor from '@/components/page-templates/ChildTableEditor.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { normalizeFieldPermissions, pickFirstNonEmptyFieldPermissions, pickFirstNonEmptyPermissionSource } from '@/utils/field-permissions'
import { createFlowActionCredentials } from '@/utils/flow-action-idempotency'
import { applyChildTableFieldPermissions, childTableKeysAlias } from '@/utils/flow-field-permissions'
import { buildFlowCategoryTreeOptions, resolveFlowCategoryLabel } from '../utils/categoryOptions'
import { FLOW_PRIORITY_LABEL_FALLBACK, getFlowPriorityClass, isUrgentFlowPriority, resolveFlowPriorityLevel, shouldShowFlowPriority } from '../utils/priority'
import { getBusinessFormDisplayTitle, getProcessDisplayName, getRowDisplayTitle, getTaskDisplayName, getTaskHandlerName } from '../utils/processDisplay'
import { resolveBusinessTaskAiFormSchema } from '../utils/resolveBusinessTaskAiFormSchema'
import { loadTaskFormBundle } from '../utils/task-form-bundle'
export function applyFlowTodoPart1() {
  const __impl = {}
  const mut = {}
  const clearRouteTaskId = (...args) => __impl.clearRouteTaskId(...args)
  const collectDynamicFormVariables = (...args) => __impl.collectDynamicFormVariables(...args)
  const executeQuickAction = (...args) => __impl.executeQuickAction(...args)
  const getRouteTaskId = (...args) => __impl.getRouteTaskId(...args)
  const handleClaim = (...args) => __impl.handleClaim(...args)
  const handleDelegate = (...args) => __impl.handleDelegate(...args)
  const handleReset = (...args) => __impl.handleReset(...args)
  const handleSearch = (...args) => __impl.handleSearch(...args)
  const handleUserSelected = (...args) => __impl.handleUserSelected(...args)
  const isClaimingTask = (...args) => __impl.isClaimingTask(...args)
  const loadCategories = (...args) => __impl.loadCategories(...args)
  const loadData = (...args) => __impl.loadData(...args)
  const openTaskFromRoute = (...args) => __impl.openTaskFromRoute(...args)
  const submitDelegate = (...args) => __impl.submitDelegate(...args)
  const submitQuickAction = (...args) => __impl.submitQuickAction(...args)

  const userStore = useUserStore()
  const route = useRoute()
  const router = useRouter()
  const { dict, getLabel } = useDict('flow_todo_status', 'flow_priority')
  // 首屏即 loading，避免挂载到 loadData 之间空列表闪白
  const loading = ref(true)
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

  const urgentCount = ref(0)
  const selectedTaskKeys = ref([])

  // 抽屉状态
  const showDrawer = ref(false)
  const currentTask = ref(null)
  const approvalHistory = ref([])

  // 业务自定义表单
  const taskFormInfo = ref(null)
  const approvalPointChecks = ref({})
  const formInfoLoading = ref(false)
  const dynamicFormRef = ref(null)
  const dynamicFormData = ref({})
  const dynamicFormSchema = computed(() => formCreateToAiSchema(taskFormInfo.value?.formJson || []))
  const businessFormContext = ref(null)
  const businessFormData = ref({})
  const businessChildFormData = ref({})
  const businessFormSavedSnapshot = ref('')
  const businessFormRef = ref(null)
  const businessChildFormRef = ref(null)
  const businessFormLoading = ref(false)
  // 统一的表单加载态：表单信息与业务上下文并行加载，模板只展示这一个 loading，避免先后两次转圈
  const taskFormLoading = computed(() => formInfoLoading.value || businessFormLoading.value)
  const businessFormSaving = ref(false)
  const useBusinessObjectForm = computed(() => businessFormContext.value?.configured === true && businessFormContext.value?.formType === 'business-object')
  const useBusinessCodeForm = computed(() => businessFormContext.value?.configured === true && businessFormContext.value?.formType === 'business-code')
  const useBusinessManagedForm = computed(() => useBusinessObjectForm.value || useBusinessCodeForm.value)
  const useDynamicForm = computed(() => {
    if (useBusinessManagedForm.value && Array.isArray(businessFormContext.value?.fields) && businessFormContext.value.fields.length > 0)
      return false
    return dynamicFormSchema.value.length > 0
  })
  const useExternalForm = computed(() => !useBusinessManagedForm.value && taskFormInfo.value?.formType === 'external' && taskFormInfo.value?.formUrl)
  const businessFormTitle = computed(() => getBusinessFormDisplayTitle(businessFormContext.value, '业务表单'))
  const businessFormWarnings = computed(() => Array.isArray(businessFormContext.value?.warnings) ? businessFormContext.value.warnings : [])
  const businessFormFieldPermissions = computed(() => pickFirstNonEmptyFieldPermissions([
    businessFormContext.value?.fieldPermissions,
    taskFormInfo.value?.fieldPermissions,
    taskFormInfo.value?.formFieldPermissions,
  ]))
  const businessFormChildrenConfig = computed(() => {
    const children = Array.isArray(businessFormContext.value?.childrenConfig) ? businessFormContext.value.childrenConfig : []
    return applyChildTableFieldPermissions(
      children.filter(child => child?.showInDetail !== false && Array.isArray(child.fields) && child.fields.length),
      [
        taskFormInfo.value?.formFieldPermissions,
        businessFormContext.value?.taskFormInfo?.formFieldPermissions,
        businessFormFieldPermissions.value,
      ],
    )
  })
  const businessFormHasWritableFields = computed(() => hasWritableBusinessFormFields(businessFormContext.value))
  const flowPrintHasUnsavedChanges = computed(() => {
    const businessDirty = Boolean(businessFormSavedSnapshot.value)
      && businessFormSavedSnapshot.value !== createBusinessFormSnapshot()
    const dynamicDirty = Boolean(taskFormInfo.value)
      && JSON.stringify(dynamicFormData.value) !== JSON.stringify(taskFormInfo.value?.variables || {})
    return businessDirty || dynamicDirty
  })
  const businessCodeFormUrl = computed(() => businessFormContext.value?.formUrl || businessFormContext.value?.formRef?.formUrl || '')
  const businessFormGridCols = computed(() => Math.max(1, Number(businessFormContext.value?.gridCols || 1)))
  const businessFormLabelPlacement = computed(() => ['left', 'top'].includes(businessFormContext.value?.labelPlacement)
    ? businessFormContext.value.labelPlacement
    : 'left')
  const businessFormLabelWidth = computed(() => businessFormContext.value?.labelWidth || '100')
  const businessFormSize = computed(() => ['small', 'medium', 'large'].includes(businessFormContext.value?.size)
    ? businessFormContext.value.size
    : 'medium')
  /** PC Phase A：有 uiDocument 时按分区序排 fields，无文档则原样 */
  const businessFormAiSchema = computed(() => resolveBusinessTaskAiFormSchema(businessFormContext.value))
  const useBusinessCodeComponentForm = computed(() => useBusinessCodeForm.value && Boolean(businessCodeFormUrl.value))
  const useComponentTaskForm = computed(() => useExternalForm.value || useBusinessCodeComponentForm.value)
  const businessFormMissingText = computed(() => {
    const warnings = Array.isArray(businessFormContext.value?.warnings)
      ? businessFormContext.value.warnings.filter(Boolean)
      : []
    if (warnings.length)
      return warnings[0]
    return '当前节点未加载到可渲染的业务应用表单。请确认流程已部署，且待办能关联到业务单据。'
  })
  const componentTaskFormUrl = computed(() => useBusinessCodeComponentForm.value ? businessCodeFormUrl.value : taskFormInfo.value?.formUrl)
  const componentTaskFormInfo = computed(() => ({
    taskId: businessFormContext.value?.taskId || taskFormInfo.value?.taskId,
    businessKey: businessFormContext.value?.businessKey || taskFormInfo.value?.businessKey,
    processInstanceId: businessFormContext.value?.processInstanceId || taskFormInfo.value?.processInstanceId,
    taskDefKey: businessFormContext.value?.taskDefKey || taskFormInfo.value?.taskDefKey,
    processDefKey: businessFormContext.value?.processDefKey || taskFormInfo.value?.processDefKey,
  }))
  const dynamicFormFieldPermissions = computed(() => pickFirstNonEmptyPermissionSource([
    taskFormInfo.value?.fieldPermissions,
    taskFormInfo.value?.formFieldPermissions,
  ]))
  const businessFormRenderContext = computed(() => ({
    task: currentTask.value,
    taskFormInfo: taskFormInfo.value,
    businessFormContext: businessFormContext.value,
    formAssets: businessFormContext.value?.formAssets || [],
    protocolVersion: businessFormContext.value?.protocolVersion || null,
    uiDocument: businessFormContext.value?.uiDocument || null,
  }))
  const taskPolicySource = computed(() => taskFormInfo.value || businessFormContext.value || {})
  const canApprove = computed(() => taskPolicySource.value?.allowApprove !== false)
  const canReject = computed(() => taskPolicySource.value?.allowReject !== false)
  const canRejectToStart = computed(() => taskPolicySource.value?.allowRejectToStart === true)
  const canDelegate = computed(() => taskPolicySource.value?.allowDelegate !== false)
  const canReturn = computed(() => taskPolicySource.value?.allowReturn === true)
  const returnTargetOptions = computed(() => (Array.isArray(taskPolicySource.value?.returnTargets)
    ? taskPolicySource.value.returnTargets
    : []).map(item => ({
    label: item.activityName || item.activityId,
    value: item.activityId,
  })))
  const canChooseReturnTarget = computed(() => {
    return taskPolicySource.value?.allowMultiReturn === true && returnTargetOptions.value.length > 0
  })
  const canDirectSend = computed(() => taskPolicySource.value?.allowDirectSend === true)
  const canTerminate = computed(() => taskPolicySource.value?.allowTerminate === true)
  const requireComment = computed(() => taskPolicySource.value?.requireComment !== false)
  const requireSignature = computed(() => taskPolicySource.value?.requireSignature === true)
  const approvalPolicy = computed(() => ({
    allowApprove: canApprove.value,
    allowReject: canReject.value,
    allowDelegate: canDelegate.value,
    allowReturn: canReturn.value,
    allowTerminate: canTerminate.value,
    requireComment: requireComment.value,
    requireSignature: requireSignature.value,
    allowDirectSend: canDirectSend.value,
    returnSourceActivityId: taskPolicySource.value?.returnSourceActivityId,
    returnSourceActivityName: taskPolicySource.value?.returnSourceActivityName,
  }))

  // 审批表单
  const approveLoading = ref(false)
  const approveForm = reactive({ action: '', comment: '', signature: '' })
  const selectedReturnTarget = ref(null)
  const rejectTargetVisible = ref(false)
  const pendingRejectSubmit = ref(null)
  const directSendAfterReturn = ref(false)
  const approveSignatureRef = ref(null)
  const approveSignatureKey = ref(0)
  const claimLoadingTaskId = ref('')
  const quickActionVisible = ref(false)
  const quickActionLoading = ref(false)
  const quickActionType = ref('approve')
  const quickActionTargets = ref([])
  const quickActionFailedTargets = ref([])
  const quickActionForm = reactive({ comment: '' })
  const quickActionInputRef = ref(null)
  const quickActionIsApprove = computed(() => quickActionType.value === 'approve')
  const quickActionTitle = computed(() => quickActionIsApprove.value ? '同意' : '驳回')
  const quickActionTitleId = 'flow-todo-quick-action-title'
  const quickActionSubject = computed(() => {
    if (quickActionTargets.value.length === 1)
      return getRowDisplayTitle(quickActionTargets.value[0])
    return `处理 ${quickActionTargets.value.length} 条待办`
  })
  const quickActionMeta = computed(() => {
    const rows = quickActionTargets.value
    if (rows.length === 1) {
      const row = rows[0]
      const node = getTaskDisplayName(row, '')
      const applicant = row?.startUserName
      return [node, applicant ? `申请人 ${applicant}` : ''].filter(Boolean).join(' · ')
    }
    if (!rows.length)
      return ''
    const names = rows.slice(0, 2).map(row => getRowDisplayTitle(row)).filter(Boolean)
    return names.join('、') + (rows.length > 2 ? ` 等${rows.length}条` : '')
  })

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
  const isApprovalBusy = computed(() => approveLoading.value || delegateLoading.value || businessFormSaving.value || Boolean(claimLoadingTaskId.value))

  function getPriorityClass(p) {
    return getFlowPriorityClass(p)
  }
  function getPriorityText(p) {
    if (!shouldShowFlowPriority(p))
      return ''
    const level = resolveFlowPriorityLevel(p)
    const label = getLabel('flow_priority', level)
    return String(label) === String(level) ? FLOW_PRIORITY_LABEL_FALLBACK[level] : label
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

  function resetBusinessTaskForm() {
    businessFormContext.value = null
    businessFormData.value = {}
    businessChildFormData.value = {}
    businessFormSavedSnapshot.value = ''
    businessFormLoading.value = false
    businessFormSaving.value = false
  }

  function createBusinessFormSnapshot() {
    return JSON.stringify({
      main: businessFormData.value || {},
      children: businessChildFormData.value || {},
    })
  }

  function rememberBusinessFormSnapshot() {
    businessFormSavedSnapshot.value = createBusinessFormSnapshot()
  }

  function normalizeBusinessRecordData(recordData) {
    if (recordData && typeof recordData === 'object' && !Array.isArray(recordData)) {
      const main = recordData.main
      if (main && typeof main === 'object' && !Array.isArray(main))
        return { ...main }
      const { children, ...mainRecord } = recordData
      return { ...mainRecord }
    }
    return {}
  }

  function normalizeBusinessChildrenData(recordData) {
    const source = recordData?.children && typeof recordData.children === 'object' && !Array.isArray(recordData.children)
      ? recordData.children
      : {}
    const result = {}
    const usedSourceKeys = new Set()
    businessFormChildrenConfig.value.forEach((child) => {
      const key = resolveBusinessChildKey(child)
      const matched = findBusinessChildRows(source, child, usedSourceKeys)
      result[key] = matched.rows
      if (matched.sourceKey)
        usedSourceKeys.add(matched.sourceKey)
    })
    return result
  }

  function findBusinessChildRows(source = {}, child = {}, usedSourceKeys = new Set()) {
    const candidates = [
      child.modelCode,
      child.relationKey,
      child.key,
      child.tableName,
    ].map(value => String(value || '').trim()).filter(Boolean)

    for (const candidate of candidates) {
      if (usedSourceKeys.has(candidate))
        continue
      if (Array.isArray(source[candidate]))
        return { rows: source[candidate], sourceKey: candidate }
    }

    let bestKey = ''
    let bestDelta = Number.POSITIVE_INFINITY
    Object.keys(source || {}).forEach((dataKey) => {
      if (usedSourceKeys.has(dataKey) || !Array.isArray(source[dataKey]))
        return
      const matched = candidates.some(candidate => childTableKeysAlias(candidate, dataKey))
      if (!matched)
        return
      const delta = Math.min(...candidates.map(candidate => Math.abs(candidate.length - dataKey.length)))
      if (delta < bestDelta) {
        bestDelta = delta
        bestKey = dataKey
      }
    })
    if (bestKey)
      return { rows: source[bestKey], sourceKey: bestKey }
    return { rows: [], sourceKey: '' }
  }

  function resolveBusinessChildKey(child = {}) {
    return child.modelCode || child.relationKey || child.key || child.tableName || 'children'
  }

  function logBusinessApprovalChildren(source, recordData) {
    console.warn('[FlowApprovalChildren]', {
      source,
      configKey: businessFormContext.value?.configKey,
      recordId: businessFormContext.value?.recordId,
      childrenConfig: businessFormChildrenConfig.value.map(child => ({
        key: resolveBusinessChildKey(child),
        modelCode: child.modelCode,
        tableName: child.tableName,
        relationType: child.relationType,
        sourceField: child.sourceField,
        targetField: child.targetField,
        fieldCount: Array.isArray(child.fields) ? child.fields.length : 0,
      })),
      recordChildren: summarizeBusinessChildren(recordData?.children),
      renderChildren: summarizeBusinessChildren(businessChildFormData.value),
    })
  }

  function summarizeBusinessChildren(children) {
    if (!children || typeof children !== 'object' || Array.isArray(children))
      return {}
    return Object.fromEntries(Object.entries(children).map(([key, rows]) => [
      key,
      {
        rows: Array.isArray(rows) ? rows.length : 0,
        rowIds: Array.isArray(rows) ? rows.slice(0, 5).map(row => row?.id) : [],
        firstFields: Array.isArray(rows) && rows[0] ? Object.keys(rows[0]).slice(0, 12) : [],
      },
    ]))
  }

  function isSyntheticTestBusinessKey(value) {
    const text = String(value || '').trim()
    return text === 'FLOW_TEST' || text.startsWith('FLOW_TEST:')
  }

  function resolveTaskIdentityBusinessKey(context = {}, formInfo = {}, row = {}) {
    const taskKey = formInfo.businessKey || row.businessKey || taskFormInfo.value?.businessKey || currentTask.value?.businessKey
    const contextKey = context.businessKey
    if (context.recordId && contextKey && !isSyntheticTestBusinessKey(contextKey) && !isSyntheticTestBusinessKey(taskKey))
      return contextKey
    return taskKey || contextKey
  }

  function buildBusinessTaskFormQuery(row = {}, formInfo = {}) {
    const formRef = resolveTaskFormRef(formInfo)
    const taskId = formInfo.taskId || row.taskId || row.id
    const businessKey = resolveTaskIdentityBusinessKey({}, formInfo, row)
    return compactParams({
      taskId,
      businessKey,
      processInstanceId: formInfo.processInstanceId || row.processInstanceId,
      processDefKey: formInfo.processDefKey || row.processDefKey || row.processDefinitionKey,
      taskDefKey: formInfo.taskDefKey || row.taskDefKey || row.taskDefinitionKey,
      objectCode: formInfo.objectCode || formRef.objectCode || row.objectCode,
      recordId: isSyntheticTestBusinessKey(businessKey)
        ? undefined
        : (formInfo.recordId || formRef.recordId || row.recordId),
      formKey: formInfo.formKey || formRef.formKey,
    })
  }

  function resolveTaskFormRef(formInfo = {}) {
    if (formInfo.formRef && typeof formInfo.formRef === 'object' && !Array.isArray(formInfo.formRef))
      return formInfo.formRef
    const raw = formInfo.formJson
    if (raw && typeof raw === 'object' && !Array.isArray(raw))
      return raw.formRef && typeof raw.formRef === 'object' ? { ...raw, ...raw.formRef } : raw
    if (typeof raw === 'string' && raw.trim()) {
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed))
          return parsed.formRef && typeof parsed.formRef === 'object' ? { ...parsed, ...parsed.formRef } : parsed
      }
      catch {
        return {}
      }
    }
    return {}
  }

  function compactParams(source = {}) {
    const result = {}
    Object.entries(source).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '')
        result[key] = value
    })
    return result
  }

  function hasBusinessTaskFormQuery(query = {}) {
    return Boolean(query.taskId || query.processInstanceId || query.businessKey || (query.objectCode && query.recordId))
  }

  function hasWritableBusinessFormFields(context) {
    const mainWritable = Array.isArray(context?.fields) && context.fields.some(field =>
      field?.writable === true && field?.readonly !== true && field?.disabled !== true,
    )
    const childWritable = Array.isArray(context?.childrenConfig) && context.childrenConfig.some((child) => {
      if (child?.allowCreate === true || child?.allowDelete === true || child?.allowUpdate === true)
        return true
      return Array.isArray(child?.fields) && child.fields.some(field =>
        field?.writable === true && field?.readonly !== true && field?.disabled !== true,
      )
    })
    return mainWritable || childWritable
  }

  async function loadBusinessTaskFormContext(row, formInfo) {
    businessFormContext.value = null
    businessFormData.value = {}
    businessChildFormData.value = {}

    const query = buildBusinessTaskFormQuery(row, formInfo)
    if (!hasBusinessTaskFormQuery(query))
      return null

    businessFormLoading.value = true
    try {
      const res = await businessTaskFormContext(query)
      if (res.code !== 200) {
        console.error('加载业务表单上下文失败', res.message)
        return null
      }
      businessFormContext.value = res.data || null
      if (res.data?.taskFormInfo && typeof res.data.taskFormInfo === 'object')
        applyTaskFormInfo(res.data.taskFormInfo)
      businessFormData.value = normalizeBusinessRecordData(res.data?.recordData)
      businessChildFormData.value = normalizeBusinessChildrenData(res.data?.recordData)
      rememberBusinessFormSnapshot()
      logBusinessApprovalChildren('todo', res.data?.recordData)
      return businessFormContext.value
    }
    catch (error) {
      console.error('加载业务表单上下文失败', error)
      return null
    }
    finally {
      businessFormLoading.value = false
    }
  }

  async function loadTaskFormInfo(taskId) {
    if (!taskId)
      return null
    try {
      const res = await flowApi.getTaskFormInfo(taskId)
      if (res.code === 200) {
        return applyTaskFormInfo(res.data)
      }
    }
    catch (error) {
      console.error('加载表单信息失败', error)
    }
    return null
  }

  function applyTaskFormInfo(formInfo) {
    if (!formInfo || typeof formInfo !== 'object')
      return null
    taskFormInfo.value = formInfo
    dynamicFormData.value = { ...(formInfo.variables || {}) }
    approvalPointChecks.value = Object.fromEntries(
      (formInfo.approvalPoints || []).map(point => [point.id, false]),
    )
    return taskFormInfo.value
  }

  function isConfiguredBusinessTaskForm(context) {
    return context?.configured === true && ['business-object', 'business-code'].includes(context?.formType)
  }

  async function hydrateBusinessFormFromAssets(formInfo) {
    if (!formInfo || isConfiguredBusinessTaskForm(businessFormContext.value))
      return
    const formRef = resolveTaskFormRef(formInfo)
    const objectCode = String(formInfo.objectCode || formRef.objectCode || '').trim()
    const formKey = String(formInfo.formKey || formRef.formKey || '').trim()
    const formType = String(formInfo.formType || formRef.formMode || formRef.type || '').toLowerCase()
    const isBusinessForm = formType === 'business'
      || formType === 'business_object_form'
      || formType === 'business-object'
      || String(formRef.formMode || formRef.type || '').toUpperCase() === 'BUSINESS_OBJECT_FORM'
    if (!isBusinessForm || !objectCode)
      return
    try {
      const res = await businessFlowFormAssets(objectCode, {
        includeInternal: true,
        applicationId: formRef.applicationId || undefined,
      })
      const assets = Array.isArray(res.data) ? res.data : res.data?.formAssets || []
      const selected = assets.find(item => String(item?.formKey || '') === formKey) || assets[0]
      const fieldPermissions = pickFirstNonEmptyFieldPermissions([
        formInfo.formFieldPermissions,
        formInfo.fieldPermissions,
      ])
      const fields = normalizeFallbackBusinessFields(selected, fieldPermissions)
      if (!fields.length)
        return
      businessFormContext.value = {
        configured: true,
        formType: 'business-object',
        formKey: selected?.formKey || formKey,
        formName: selected?.formName || formInfo.formName || formKey,
        objectCode,
        taskId: formInfo.taskId,
        processInstanceId: formInfo.processInstanceId,
        processDefKey: formInfo.processDefKey,
        taskDefKey: formInfo.taskDefKey,
        businessKey: formInfo.businessKey,
        fields,
        formAssets: assets,
        fieldPermissions,
        recordData: formInfo.variables || {},
      }
      businessFormData.value = { ...(formInfo.variables || {}) }
      rememberBusinessFormSnapshot()
    }
    catch (error) {
      console.error('按表单资产回退渲染失败', error)
    }
  }

  function isChildBusinessFormField(field = {}, fieldCode = '') {
    const scope = String(field?.scope || '').trim().toLowerCase()
    if (scope === 'child' || String(field?.childKey || '').trim())
      return true
    return String(fieldCode || '').includes('__')
  }

  function normalizeFallbackBusinessFields(asset = null, permissions = []) {
    if (!asset || typeof asset !== 'object')
      return []
    const catalog = Array.isArray(asset.fieldCatalog) && asset.fieldCatalog.length
      ? asset.fieldCatalog
      : Array.isArray(asset.fields) ? asset.fields : []
    const permissionMap = new Map(normalizeFieldPermissions(permissions).map(item => [item.field, item]))
    const seen = new Set()
    return catalog
      .map((field) => {
        const fieldCode = String(field?.field || field?.fieldCode || field?.name || field?.key || '').trim()
        if (!fieldCode || seen.has(fieldCode) || isChildBusinessFormField(field, fieldCode))
          return null
        seen.add(fieldCode)
        const permission = permissionMap.get(fieldCode)
        const readable = permission ? permission.readable !== false : true
        if (!readable)
          return null
        const writable = permission ? permission.writable === true : false
        const required = writable && permission?.required === true
        return {
          field: fieldCode,
          code: fieldCode,
          prop: fieldCode,
          label: String(field?.label || field?.title || field?.fieldName || fieldCode).trim(),
          type: field?.type || field?.componentType || 'input',
          required,
          readonly: !writable,
          disabled: !writable,
          writable,
          visible: true,
        }
      })
      .filter(Boolean)
  }

  function buildBusinessTaskFormSavePayload() {
    const context = businessFormContext.value || {}
    const businessKey = resolveTaskIdentityBusinessKey(context, taskFormInfo.value, currentTask.value)
    const writableMainData = {}
    ;(Array.isArray(context.fields) ? context.fields : []).forEach((field) => {
      if (field?.writable === true && field?.readonly !== true && field?.disabled !== true) {
        const code = field.field || field.fieldCode
        if (!code)
          return
        // 开关未勾选也要带上 false/0，否则后端必填校验会当成「未提交」
        if (Object.prototype.hasOwnProperty.call(businessFormData.value, code)) {
          writableMainData[code] = businessFormData.value[code]
          return
        }
        if (String(field.type || field.componentType || '').toLowerCase() === 'switch') {
          const unchecked = field.props?.uncheckedValue
          writableMainData[code] = unchecked === undefined || unchecked === null ? false : unchecked
        }
      }
    })
    const childValue = businessChildFormRef.value?.getValue?.() || businessChildFormData.value
    const childPayload = {}
    businessFormChildrenConfig.value.forEach((child) => {
      const key = resolveBusinessChildKey(child)
      const writableFields = new Set((Array.isArray(child.fields) ? child.fields : [])
        .filter(field => field?.writable === true && field?.readonly !== true && field?.disabled !== true)
        .flatMap(field => [field.field, field.fieldCode].filter(Boolean)))
      const rows = Array.isArray(childValue?.[key]) ? childValue[key] : []
      if (!writableFields.size && child.allowCreate !== true && child.allowUpdate !== true && child.allowDelete !== true)
        return
      childPayload[key] = rows.map((row) => {
        const next = {}
        Object.entries(row || {}).forEach(([field, value]) => {
          if (field === 'id' || field === 'ID' || field === '_deleted' || field === '__deleted' || writableFields.has(field))
            next[field] = value
        })
        return next
      })
    })
    return compactParams({
      taskId: context.taskId || taskFormInfo.value?.taskId || currentTask.value?.taskId || currentTask.value?.id,
      businessKey,
      processInstanceId: context.processInstanceId || taskFormInfo.value?.processInstanceId || currentTask.value?.processInstanceId,
      processDefKey: context.processDefKey || taskFormInfo.value?.processDefKey || currentTask.value?.processDefKey || currentTask.value?.processDefinitionKey,
      taskDefKey: context.taskDefKey || taskFormInfo.value?.taskDefKey || currentTask.value?.taskDefKey || currentTask.value?.taskDefinitionKey,
      objectCode: context.objectCode || taskFormInfo.value?.objectCode || currentTask.value?.objectCode,
      recordId: isSyntheticTestBusinessKey(businessKey)
        ? context.recordId
        : (context.recordId || taskFormInfo.value?.recordId || currentTask.value?.recordId),
      formKey: context.formKey || taskFormInfo.value?.formKey,
      data: {
        main: writableMainData,
        ...(businessFormChildrenConfig.value.length
          ? { children: childPayload }
          : {}),
      },
    })
  }

  async function saveBusinessTaskFormFields(options = {}) {
    if (!useBusinessManagedForm.value || !businessFormHasWritableFields.value)
      return null

    const { validate = true, silent = true } = options
    businessFormSaving.value = true
    try {
      if (validate)
        await businessFormRef.value?.validate?.()
      if (validate)
        await businessChildFormRef.value?.validate?.()

      const res = await saveBusinessTaskFormContext(buildBusinessTaskFormSavePayload())
      if (res.code !== 200)
        throw new Error(res.message || '业务字段保存失败')

      businessFormContext.value = res.data || businessFormContext.value
      businessFormData.value = normalizeBusinessRecordData(businessFormContext.value?.recordData || businessFormData.value)
      businessChildFormData.value = normalizeBusinessChildrenData(businessFormContext.value?.recordData)
      rememberBusinessFormSnapshot()
      if (!silent)
        window.$message.success('修改已暂存')
      return businessFormContext.value
    }
    catch (error) {
      if (!silent) {
        window.$message.error(error?.message || '业务字段保存失败')
        return null
      }
      throw error
    }
    finally {
      businessFormSaving.value = false
    }
  }

  async function buildBusinessTaskActionPayload(action, comment, signature, variables = {}) {
    const context = businessFormContext.value || {}
    const taskId = context.taskId || taskFormInfo.value?.taskId || currentTask.value?.taskId || currentTask.value?.id
    const actionVariables = buildActionVariables(action, variables)
    const data = useBusinessManagedForm.value && businessFormHasWritableFields.value
      ? buildBusinessTaskFormSavePayload().data
      : undefined
    const credentials = await createFlowActionCredentials(action, taskId, {
      comment,
      signature,
      variables: actionVariables,
      data,
    })
    return compactParams({
      action,
      taskId,
      businessKey: resolveTaskIdentityBusinessKey(context, taskFormInfo.value, currentTask.value),
      processInstanceId: context.processInstanceId || taskFormInfo.value?.processInstanceId || currentTask.value?.processInstanceId,
      processDefKey: context.processDefKey || taskFormInfo.value?.processDefKey || currentTask.value?.processDefKey || currentTask.value?.processDefinitionKey,
      taskDefKey: context.taskDefKey || taskFormInfo.value?.taskDefKey || currentTask.value?.taskDefKey || currentTask.value?.taskDefinitionKey,
      objectCode: context.objectCode || taskFormInfo.value?.objectCode || currentTask.value?.objectCode,
      recordId: context.recordId || taskFormInfo.value?.recordId || currentTask.value?.recordId,
      formKey: context.formKey || taskFormInfo.value?.formKey,
      userId: userStore.userId,
      comment,
      signature,
      variables: actionVariables,
      targetActivityId: action === 'return' ? selectedReturnTarget.value : undefined,
      data,
      approvalPointResults: buildApprovalPointResults(),
      ...credentials,
    })
  }

  async function submitTaskAction(action, comment, signature, variables = {}) {
    if (isConfiguredBusinessTaskForm(businessFormContext.value)) {
      return completeBusinessTaskAction(await buildBusinessTaskActionPayload(action, comment, signature, variables))
    }
    const api = resolveActionApi(action)
    const taskId = currentTask.value.taskId || currentTask.value.id
    const credentials = await createFlowActionCredentials(action, taskId, { comment, signature, variables })
    return api({
      taskId,
      userId: userStore.userId,
      comment,
      signature,
      variables: buildActionVariables(action, variables),
      targetActivityId: action === 'return' ? selectedReturnTarget.value : undefined,
      approvalPointResults: buildApprovalPointResults(),
      ...credentials,
    })
  }

  function buildActionVariables(action, variables = {}) {
    const result = variables && typeof variables === 'object' ? { ...variables } : {}
    if (action === 'approve' && canDirectSend.value)
      result.directSend = directSendAfterReturn.value
    return result
  }

  function openBusinessCodeForm() {
    const url = businessCodeFormUrl.value
    if (!url)
      return
    if (/^https?:\/\//i.test(url)) {
      window.open(url, '_blank', 'noopener,noreferrer')
      return
    }
    router.push({
      path: url,
      query: compactParams({
        taskId: businessFormContext.value?.taskId || taskFormInfo.value?.taskId || currentTask.value?.taskId,
        businessKey: businessFormContext.value?.businessKey,
        processInstanceId: businessFormContext.value?.processInstanceId,
        taskDefKey: businessFormContext.value?.taskDefKey,
        processDefKey: businessFormContext.value?.processDefKey,
        objectCode: businessFormContext.value?.objectCode,
        recordId: businessFormContext.value?.recordId,
        source: 'flowTodo',
      }),
    })
  }

  async function loadQuickBusinessTaskFormContext(row, formInfo) {
    const query = buildBusinessTaskFormQuery(row, formInfo)
    if (!hasBusinessTaskFormQuery(query))
      return null
    const res = await businessTaskFormContext(query)
    if (res.code !== 200)
      throw new Error(res.message || '业务表单策略加载失败')
    return res.data || null
  }

  async function openDrawer(row) {
    currentTask.value = row
    approveForm.comment = ''
    approveForm.action = ''
    approveForm.signature = ''
    selectedReturnTarget.value = null
    directSendAfterReturn.value = false
    approveSignatureKey.value += 1
    approvalHistory.value = []
    taskFormInfo.value = null
    approvalPointChecks.value = {}
    dynamicFormData.value = {}
    resetBusinessTaskForm()
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
      promises.push((async () => {
        try {
          // 业务上下文已携带服务端本次解析过的 Flow 表单快照，常规路径只发一个请求；
          // 直连 Flow 仅作为新旧服务滚动升级期间的兼容兜底。
          const { formInfo } = await loadTaskFormBundle({
            row,
            loadBusinessContext: loadBusinessTaskFormContext,
            loadFlowFormInfo: () => loadTaskFormInfo(taskId),
            isConfiguredBusinessContext: isConfiguredBusinessTaskForm,
          })
          if (!isConfiguredBusinessTaskForm(businessFormContext.value))
            await hydrateBusinessFormFromAssets(formInfo)
        }
        finally {
          formInfoLoading.value = false
          businessFormLoading.value = false
        }
      })())
    }

    await Promise.all(promises)
  }

  async function handleExternalFormSubmit({ action, comment, signature, variables }) {
    const approvalSignature = signature || variables?.signature
    if (action === 'reject' && canChooseReturnTarget.value) {
      pendingRejectSubmit.value = { comment, signature: approvalSignature, variables }
      openRejectTargetModal()
      return
    }
    if (!canRunAction(action))
      return
    if (!validateApprovalInput(comment, approvalSignature, null, action))
      return

    approveForm.action = action
    approveLoading.value = true
    try {
      const res = await submitTaskAction(action, comment, approvalSignature, variables)
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
      approveForm.action = ''
    }
  }

  function canRunAction(action) {
    const allowed = {
      approve: canApprove.value,
      reject: canReject.value,
      rejectToStart: canRejectToStart.value,
      return: canReturn.value || canChooseReturnTarget.value,
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

  function currentApprovalPoints() {
    return Array.isArray(taskFormInfo.value?.approvalPoints) ? taskFormInfo.value.approvalPoints : []
  }

  function requiredApprovalPointsIncomplete() {
    return currentApprovalPoints()
      .filter(point => point?.required === true)
      .some(point => !approvalPointChecks.value?.[point.id])
  }

  function buildApprovalPointResults() {
    return currentApprovalPoints()
      .filter(point => point?.id)
      .map(point => ({
        id: point.id,
        content: point.content,
        required: point.required === true,
        checked: Boolean(approvalPointChecks.value?.[point.id]),
      }))
  }

  function validateApprovalInput(comment, signature, signatureRef = null, action = '') {
    if (action === 'approve' && requiredApprovalPointsIncomplete()) {
      window.$message.warning('请完成全部必审要点')
      return false
    }
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
      rejectToStart: flowApi.rejectToStartTask,
      return: flowApi.returnTask,
      terminate: flowApi.terminateTask,
    }
    return apiMap[action] || flowApi.approveTask
  }

  function getActionSuccessText(action) {
    const textMap = {
      approve: '审批通过',
      reject: '已驳回',
      rejectToStart: '已退回发起人修改，可在原流程修改后重提',
      return: '已退回',
      terminate: '流程已终结',
    }
    return textMap[action] || '操作成功'
  }

  function openRejectTargetModal() {
    selectedReturnTarget.value = null
    rejectTargetVisible.value = true
  }

  async function confirmRejectToTarget() {
    if (!selectedReturnTarget.value) {
      window.$message.warning('请选择驳回至哪个已审批节点')
      return
    }
    const pending = pendingRejectSubmit.value
    pendingRejectSubmit.value = null
    rejectTargetVisible.value = false
    if (pending)
      await handleExternalFormSubmit({ action: 'return', ...pending })
    else
      await submitApprove('return')
  }

  async function submitApprove(action) {
    if (action === 'reject' && canChooseReturnTarget.value) {
      pendingRejectSubmit.value = null
      openRejectTargetModal()
      return
    }
    if (!canRunAction(action))
      return
    if (!validateApprovalInput(approveForm.comment, approveForm.signature, approveSignatureRef.value, action))
      return
    approveForm.action = action
    approveLoading.value = true
    try {
      const signature = await resolveSignature(approveSignatureRef.value, approveForm.signature)
      approveForm.signature = signature
      const variables = await collectDynamicFormVariables(action)
      if (useBusinessManagedForm.value && businessFormHasWritableFields.value) {
        await businessFormRef.value?.validate?.()
        await businessChildFormRef.value?.validate?.()
      }
      const res = await submitTaskAction(action, approveForm.comment, signature, variables)
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
      approveForm.action = ''
    }
  }

  function isActionLoading(action) {
    return approveLoading.value && approveForm.action === action
  }

  function resolveQuickActionTargets(targets = []) {
    return targets
      .map((target) => {
        if (target && typeof target === 'object')
          return target
        return dataSource.value.find(row => String(row.id) === String(target) || String(row.taskId) === String(target))
      })
      .filter(Boolean)
  }

  function openQuickAction(action, targets) {
    const resolvedTargets = resolveQuickActionTargets(targets)
    if (resolvedTargets.length === 0) {
      window.$message.warning('请选择待办任务')
      return
    }
    quickActionType.value = action
    quickActionTargets.value = resolvedTargets
    quickActionFailedTargets.value = []
    quickActionForm.comment = action === 'approve' ? '同意' : '驳回'
    quickActionVisible.value = true
    nextTick(() => quickActionInputRef.value?.focus?.())
  }

  function isCandidateTask(row) {
    return row?.status === 0 && !row?.assignee
  }

  async function claimTaskBeforeQuickAction(row, taskId) {
    if (!isCandidateTask(row))
      return
    const res = await flowApi.claimTask(taskId, userStore.userId)
    if (res.code !== 200)
      throw new Error(res.message || '签收失败')
  }

  function assertQuickActionAllowed(action, formInfo, businessFormContext = null) {
    const businessManaged = businessFormContext?.configured === true
      && ['business-object', 'business-code'].includes(businessFormContext?.formType)
    if (action === 'approve' && formInfo?.allowApprove === false)
      throw new Error('当前节点不允许同意')
    if (action === 'reject' && formInfo?.allowReject === false)
      throw new Error('当前节点不允许驳回')
    if (action === 'reject' && formInfo?.allowMultiReturn === true && Array.isArray(formInfo.returnTargets) && formInfo.returnTargets.length)
      throw new Error('该流程已开启指定节点驳回，请进入详情选择驳回节点')
    if (action === 'rejectToStart' && formInfo?.allowRejectToStart !== true)
      throw new Error('当前节点不允许退回发起人修改')
    if (formInfo?.requireSignature === true)
      throw new Error('需要手写签名，请进入详情处理')
    if (action === 'approve' && !businessManaged && formInfo?.formType === 'dynamic' && formInfo?.formJson)
      throw new Error('需要填写节点表单，请进入详情处理')
    if (action === 'approve' && !businessManaged && formInfo?.formType === 'external' && formInfo?.formUrl)
      throw new Error('需要填写业务表单，请进入详情处理')
    if (action === 'approve' && businessFormContext?.configured === true && businessFormContext?.formType === 'business-code')
      throw new Error('需要进入业务表单处理')
    if (action === 'approve' && businessFormContext?.configured === true && hasWritableBusinessFormFields(businessFormContext))
      throw new Error('需要填写业务表单，请进入详情处理')
    if (action === 'approve' && Array.isArray(formInfo?.approvalPoints) && formInfo.approvalPoints.some(point => point?.required === true))
      throw new Error('需要勾选审批要点，请进入详情处理')
  }

  __impl.getPriorityClass = getPriorityClass
  __impl.getPriorityText = getPriorityText
  __impl.getCategoryDisplayName = getCategoryDisplayName
  __impl.toNumberOptions = toNumberOptions
  __impl.resetBusinessTaskForm = resetBusinessTaskForm
  __impl.createBusinessFormSnapshot = createBusinessFormSnapshot
  __impl.rememberBusinessFormSnapshot = rememberBusinessFormSnapshot
  __impl.normalizeBusinessRecordData = normalizeBusinessRecordData
  __impl.normalizeBusinessChildrenData = normalizeBusinessChildrenData
  __impl.findBusinessChildRows = findBusinessChildRows
  __impl.resolveBusinessChildKey = resolveBusinessChildKey
  __impl.logBusinessApprovalChildren = logBusinessApprovalChildren
  __impl.summarizeBusinessChildren = summarizeBusinessChildren
  __impl.isSyntheticTestBusinessKey = isSyntheticTestBusinessKey
  __impl.resolveTaskIdentityBusinessKey = resolveTaskIdentityBusinessKey
  __impl.buildBusinessTaskFormQuery = buildBusinessTaskFormQuery
  __impl.resolveTaskFormRef = resolveTaskFormRef
  __impl.compactParams = compactParams
  __impl.hasBusinessTaskFormQuery = hasBusinessTaskFormQuery
  __impl.hasWritableBusinessFormFields = hasWritableBusinessFormFields
  __impl.loadBusinessTaskFormContext = loadBusinessTaskFormContext
  __impl.loadTaskFormInfo = loadTaskFormInfo
  __impl.applyTaskFormInfo = applyTaskFormInfo
  __impl.isConfiguredBusinessTaskForm = isConfiguredBusinessTaskForm
  __impl.hydrateBusinessFormFromAssets = hydrateBusinessFormFromAssets
  __impl.isChildBusinessFormField = isChildBusinessFormField
  __impl.normalizeFallbackBusinessFields = normalizeFallbackBusinessFields
  __impl.buildBusinessTaskFormSavePayload = buildBusinessTaskFormSavePayload
  __impl.saveBusinessTaskFormFields = saveBusinessTaskFormFields
  __impl.buildBusinessTaskActionPayload = buildBusinessTaskActionPayload
  __impl.submitTaskAction = submitTaskAction
  __impl.buildActionVariables = buildActionVariables
  __impl.openBusinessCodeForm = openBusinessCodeForm
  __impl.loadQuickBusinessTaskFormContext = loadQuickBusinessTaskFormContext
  __impl.openDrawer = openDrawer
  __impl.handleExternalFormSubmit = handleExternalFormSubmit
  __impl.canRunAction = canRunAction
  __impl.hasSignatureValue = hasSignatureValue
  __impl.resolveSignature = resolveSignature
  __impl.currentApprovalPoints = currentApprovalPoints
  __impl.requiredApprovalPointsIncomplete = requiredApprovalPointsIncomplete
  __impl.buildApprovalPointResults = buildApprovalPointResults
  __impl.validateApprovalInput = validateApprovalInput
  __impl.resolveActionApi = resolveActionApi
  __impl.getActionSuccessText = getActionSuccessText
  __impl.openRejectTargetModal = openRejectTargetModal
  __impl.confirmRejectToTarget = confirmRejectToTarget
  __impl.submitApprove = submitApprove
  __impl.isActionLoading = isActionLoading
  __impl.resolveQuickActionTargets = resolveQuickActionTargets
  __impl.openQuickAction = openQuickAction
  __impl.isCandidateTask = isCandidateTask
  __impl.claimTaskBeforeQuickAction = claimTaskBeforeQuickAction
  __impl.assertQuickActionAllowed = assertQuickActionAllowed

  return {
    __impl, mut, applyTaskFormInfo, assertQuickActionAllowed, buildActionVariables, buildApprovalPointResults, buildBusinessTaskActionPayload, buildBusinessTaskFormQuery,
    buildBusinessTaskFormSavePayload, canRunAction, claimTaskBeforeQuickAction, clearRouteTaskId, collectDynamicFormVariables, compactParams, confirmRejectToTarget, createBusinessFormSnapshot,
    currentApprovalPoints, executeQuickAction, findBusinessChildRows, getActionSuccessText, getCategoryDisplayName, getPriorityClass, getPriorityText, getRouteTaskId,
    handleClaim, handleDelegate, handleExternalFormSubmit, handleReset, handleSearch, handleUserSelected, hasBusinessTaskFormQuery, hasSignatureValue,
    hasWritableBusinessFormFields, hydrateBusinessFormFromAssets, isActionLoading, isCandidateTask, isChildBusinessFormField, isClaimingTask, isConfiguredBusinessTaskForm, isSyntheticTestBusinessKey,
    loadBusinessTaskFormContext, loadCategories, loadData, loadQuickBusinessTaskFormContext, loadTaskFormInfo, logBusinessApprovalChildren, normalizeBusinessChildrenData, normalizeBusinessRecordData,
    normalizeFallbackBusinessFields, openBusinessCodeForm, openDrawer, openQuickAction, openRejectTargetModal, openTaskFromRoute, rememberBusinessFormSnapshot, requiredApprovalPointsIncomplete,
    resetBusinessTaskForm, resolveActionApi, resolveBusinessChildKey, resolveQuickActionTargets, resolveSignature, resolveTaskFormRef, resolveTaskIdentityBusinessKey, saveBusinessTaskFormFields,
    submitApprove, submitDelegate, submitQuickAction, submitTaskAction, summarizeBusinessChildren, toNumberOptions, validateApprovalInput, userStore,
    route, router, loading, loadError, dataSource, pagination, queryParams, categoryTreeOptions,
    urgentCount, selectedTaskKeys, showDrawer, currentTask, approvalHistory, taskFormInfo, approvalPointChecks, formInfoLoading,
    dynamicFormRef, dynamicFormData, dynamicFormSchema, businessFormContext, businessFormData, businessChildFormData, businessFormSavedSnapshot, businessFormRef,
    businessChildFormRef, businessFormLoading, taskFormLoading, businessFormSaving, useBusinessObjectForm, useBusinessCodeForm, useBusinessManagedForm, useDynamicForm,
    useExternalForm, businessFormTitle, businessFormWarnings, businessFormFieldPermissions, businessFormChildrenConfig, businessFormHasWritableFields, flowPrintHasUnsavedChanges, businessCodeFormUrl,
    businessFormGridCols, businessFormLabelPlacement, businessFormLabelWidth, businessFormSize, businessFormAiSchema, useBusinessCodeComponentForm, useComponentTaskForm, businessFormMissingText,
    componentTaskFormUrl, componentTaskFormInfo, dynamicFormFieldPermissions, businessFormRenderContext, taskPolicySource, canApprove, canReject, canRejectToStart,
    canDelegate, canReturn, returnTargetOptions, canChooseReturnTarget, canDirectSend, canTerminate, requireComment, requireSignature,
    approvalPolicy, approveLoading, approveForm, selectedReturnTarget, rejectTargetVisible, pendingRejectSubmit, directSendAfterReturn, approveSignatureRef,
    approveSignatureKey, claimLoadingTaskId, quickActionVisible, quickActionLoading, quickActionType, quickActionTargets, quickActionFailedTargets, quickActionForm,
    quickActionInputRef, quickActionIsApprove, quickActionTitle, quickActionTitleId, quickActionSubject, quickActionMeta, showDelegateModal, showUserSelectModal,
    delegateLoading, delegateTargetUser, delegateForm, delegateSignatureRef, delegateSignatureKey, routeTaskOpening, statusOptions, isApprovalBusy,
    dict, getLabel, getRowDisplayTitle, getTaskDisplayName, getProcessDisplayName, getTaskHandlerName, isUrgentFlowPriority,
  }
}
