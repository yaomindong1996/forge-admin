/** AiCrudPage setup part 1. */
import { NButton, NDropdown, NIcon, NProgress, NSwitch, NTag } from 'naive-ui'
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, useSlots, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { crudConfigRender, customQueryExecute } from '@/api/ai'
import { businessDocumentRuntimeBatch, businessFlowStartConfig, executeBusinessAction, resubmitBusinessDocumentFlow, withdrawBusinessDocumentFlow } from '@/api/business-app'
import { businessProcessStartConfig, startBusinessProcess } from '@/api/business-process'
import { dataAuditRemove } from '@/api/data-audit'
import {
  applyDataAuditRemove,
  applyDataAuditSubmit,
  isAuditReasonRequiredError,
  isDataAuditHistoryAvailable,
  promptAuditReason,
  readDataAuditMeta,
  resolveDataAuditRemovePlan,
  shouldShowDataAuditHistory,
} from '@/components/data-audit/data-audit-submit'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import { postEncrypt } from '@/utils/encrypt-request'
import { collectInitiatorSelectSelections } from '@/utils/initiatorSelect'
import {
  isSwitchColumnConfig,
  normalizeSwitchCellValue,
  resolveSwitchColumnTexts,
  resolveSwitchColumnValuePair,
} from '../column-switch'
import {
  buildBusinessActionExecutePayload,
  buildBusinessActionInitialData,
  buildBusinessActionInputFormSchema,
  buildChildRowActionContext,
  createBusinessActionIdempotencyKey,
  isRuntimeActionForPosition,
  matchesRuntimeDisplayCondition,
  resolveBusinessActionAttempt,
  shouldHideProcessStartAction,
  shouldShowDetailFlowHistory,
  unwrapBusinessActionResult,
} from '../../business-action-runtime'
import { flattenRuntimeFormFields, useCrudFormula } from './useCrudFormula'
import { useCrudImportExport } from './useCrudImportExport'
import { useCrudPageHeight } from './useCrudPageHeight'
import { useFlowActionFeedback } from './useFlowActionFeedback'
import { useRecordFormLoader } from './useRecordFormLoader'
import { resolveFormInitRecordId } from '../../data-source-binding-runtime'
import { normalizeExpandConfig, shouldExpandRow } from '../../expand-utils'
import { isNumberFieldType } from '../../field-type-utils'
import { resolveRuntimeDefaultValue } from '@/views/app-center/components/designer/forge-form-designer/field-default-value'
import { isImageFileName, resolveFileRenderItems } from '../../file-render-utils'
import { buildFormRuntimeContext } from '../../form-runtime-context'
import {
  createOfflineFormRuntime,
  createOfflinePublishedSnapshot,
} from '../../offline-form-runtime'
import {
  Add,
  ArrowBackOutline,
  CloseOutline,
  CloudUploadOutline,
  DownloadOutline,
  EllipsisVertical,
  PrintOutline,
  RefreshOutline,
  TimeOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import AuthImage from '@/components/common/AuthImage.vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import AiCrudRowExpand from '../../AiCrudRowExpand.vue'
import AiForm from '../../AiForm.vue'

export function applyAiCrudPagePart1(props, emit, deps = {}) {
  const __impl = {}
  const mut = {
    offlineDraftSaveTimer: null,
    inlineFormTabSequence: 0,
    formChangeSequence: 0,
  }
  const startFlowAction = (...args) => __impl.startFlowAction(...args)
  const resolveDocumentRuntime = (...args) => __impl.resolveDocumentRuntime(...args)
  const resolveMyTask = (...args) => __impl.resolveMyTask(...args)
  const resubmitFlowAction = (...args) => __impl.resubmitFlowAction(...args)
  const handleTaskAction = (...args) => __impl.handleTaskAction(...args)
  const withdrawFlowAction = (...args) => __impl.withdrawFlowAction(...args)
  const submitFlowStartWithApprovers = (...args) => __impl.submitFlowStartWithApprovers(...args)
  const submitFlowStartRequest = (...args) => __impl.submitFlowStartRequest(...args)
  const refreshCurrentDetailRuntime = (...args) => __impl.refreshCurrentDetailRuntime(...args)
  const callConfiguredApiAction = (...args) => __impl.callConfiguredApiAction(...args)
  const handleCommandAction = (...args) => __impl.handleCommandAction(...args)
  const submitCommandAction = (...args) => __impl.submitCommandAction(...args)
  const closeCommandActionModal = (...args) => __impl.closeCommandActionModal(...args)
  const executeCommandAction = (...args) => __impl.executeCommandAction(...args)
  const normalizeCommandActionRuntimeConfig = (...args) => __impl.normalizeCommandActionRuntimeConfig(...args)
  const buildCommandActionInitialData = (...args) => __impl.buildCommandActionInitialData(...args)
  const parseActionConfig = (...args) => __impl.parseActionConfig(...args)
  const sendConfiguredApiRequest = (...args) => __impl.sendConfiguredApiRequest(...args)
  const compactRequestOptions = (...args) => __impl.compactRequestOptions(...args)
  const buildConfiguredApiRequest = (...args) => __impl.buildConfiguredApiRequest(...args)
  const normalizeConfiguredApiConfig = (...args) => __impl.normalizeConfiguredApiConfig(...args)
  const parseConfiguredApiValue = (...args) => __impl.parseConfiguredApiValue(...args)
  const normalizeConfiguredApiMethod = (...args) => __impl.normalizeConfiguredApiMethod(...args)
  const normalizeConfiguredApiParam = (...args) => __impl.normalizeConfiguredApiParam(...args)
  const resolveApiParamTarget = (...args) => __impl.resolveApiParamTarget(...args)
  const resolveActionParamRawValue = (...args) => __impl.resolveActionParamRawValue(...args)
  const resolveObjectPathValue = (...args) => __impl.resolveObjectPathValue(...args)
  const replaceApiPathParam = (...args) => __impl.replaceApiPathParam(...args)
  const setApiObjectValue = (...args) => __impl.setApiObjectValue(...args)
  const isEmptyApiParamValue = (...args) => __impl.isEmptyApiParamValue(...args)
  const isFalseLike = (...args) => __impl.isFalseLike(...args)
  const confirmConfiguredAction = (...args) => __impl.confirmConfiguredAction(...args)
  const getActionLoadingKey = (...args) => __impl.getActionLoadingKey(...args)
  const isActionLoading = (...args) => __impl.isActionLoading(...args)
  const setActionLoading = (...args) => __impl.setActionLoading(...args)
  const setFlowActionPageLoading = (...args) => __impl.setFlowActionPageLoading(...args)
  const buildActionTarget = (...args) => __impl.buildActionTarget(...args)
  const resolveActionParamValue = (...args) => __impl.resolveActionParamValue(...args)
  const resolveSystemParamValue = (...args) => __impl.resolveSystemParamValue(...args)
  const resolveActionText = (...args) => __impl.resolveActionText(...args)
  const resolveTemplatePlaceholder = (...args) => __impl.resolveTemplatePlaceholder(...args)
  const resolveButtonType = (...args) => __impl.resolveButtonType(...args)
  const uniqueMainRecords = (...args) => __impl.uniqueMainRecords(...args)
  const isUsableKeyValue = (...args) => __impl.isUsableKeyValue(...args)
  const readBoolean = (...args) => __impl.readBoolean(...args)
  const resolveRowKeyValue = (...args) => __impl.resolveRowKeyValue(...args)
  const mergeHookRowWithOriginal = (...args) => __impl.mergeHookRowWithOriginal(...args)
  const resolveFormDefaultValues = (...args) => __impl.resolveFormDefaultValues(...args)
  const resolveSubmitDefaultParams = (...args) => __impl.resolveSubmitDefaultParams(...args)
  const handleToolbarOverflowSelect = (...args) => __impl.handleToolbarOverflowSelect(...args)
  const getColumnKey = (...args) => __impl.getColumnKey(...args)
  const isActionColumnConfig = (...args) => __impl.isActionColumnConfig(...args)
  const shouldDefaultActionFixedRight = (...args) => __impl.shouldDefaultActionFixedRight(...args)
  const collectExpandSlots = (...args) => __impl.collectExpandSlots(...args)
  const normalizeRowActions = (...args) => __impl.normalizeRowActions(...args)
  const findEditSchemaField = (...args) => __impl.findEditSchemaField(...args)
  const resolveColumnCompanionTextField = (...args) => __impl.resolveColumnCompanionTextField(...args)
  const hasDynamicOptionSourceForColumn = (...args) => __impl.hasDynamicOptionSourceForColumn(...args)
  const resolveRowCompanionText = (...args) => __impl.resolveRowCompanionText(...args)
  const snakeToCamelKey = (...args) => __impl.snakeToCamelKey(...args)
  const camelToSnakeKey = (...args) => __impl.camelToSnakeKey(...args)
  const resolveColumnRender = (...args) => __impl.resolveColumnRender(...args)
  const inlineSwitchUpdateKey = (...args) => __impl.inlineSwitchUpdateKey(...args)
  const isInlineSwitchUpdating = (...args) => __impl.isInlineSwitchUpdating(...args)
  const setInlineSwitchUpdating = (...args) => __impl.setInlineSwitchUpdating(...args)
  const canInlineSwitchUpdate = (...args) => __impl.canInlineSwitchUpdate(...args)
  const renderInlineSwitchColumn = (...args) => __impl.renderInlineSwitchColumn(...args)
  const handleInlineSwitchUpdate = (...args) => __impl.handleInlineSwitchUpdate(...args)
  const splitTableCellValues = (...args) => __impl.splitTableCellValues(...args)
  const buildDetailFallbackSchema = (...args) => __impl.buildDetailFallbackSchema(...args)
  const toReadonlyField = (...args) => __impl.toReadonlyField(...args)
  const refreshOfflineFormRuntime = (...args) => __impl.refreshOfflineFormRuntime(...args)
  const resolveOfflineRecordId = (...args) => __impl.resolveOfflineRecordId(...args)
  const readOfflineRecordVersion = (...args) => __impl.readOfflineRecordVersion(...args)
  const resolveOfflineRecordVersion = (...args) => __impl.resolveOfflineRecordVersion(...args)
  const offlineDraftPayload = (...args) => __impl.offlineDraftPayload(...args)
  const saveOfflineDraft = (...args) => __impl.saveOfflineDraft(...args)
  const scheduleOfflineDraftSave = (...args) => __impl.scheduleOfflineDraftSave(...args)
  const flushOfflineDraftSave = (...args) => __impl.flushOfflineDraftSave(...args)
  const resetOfflineDraftSession = (...args) => __impl.resetOfflineDraftSession(...args)
  const restoreOfflineDraft = (...args) => __impl.restoreOfflineDraft(...args)
  const isBrowserOffline = (...args) => __impl.isBrowserOffline(...args)
  const createOfflineReplayKey = (...args) => __impl.createOfflineReplayKey(...args)
  const appendOfflineSubmitIntent = (...args) => __impl.appendOfflineSubmitIntent(...args)
  const confirmOfflineReplay = (...args) => __impl.confirmOfflineReplay(...args)
  const replayOfflineDraft = (...args) => __impl.replayOfflineDraft(...args)
  const loadOfflineCurrent = (...args) => __impl.loadOfflineCurrent(...args)
  const loadOfflinePublishedSnapshot = (...args) => __impl.loadOfflinePublishedSnapshot(...args)
  const clearOfflineDraftAfterSubmit = (...args) => __impl.clearOfflineDraftAfterSubmit(...args)
  const handleBrowserOnline = (...args) => __impl.handleBrowserOnline(...args)
  const resolveColumnWidth = (...args) => __impl.resolveColumnWidth(...args)
  const callHook = (...args) => __impl.callHook(...args)
  const resolveRuntimeConfigKey = (...args) => __impl.resolveRuntimeConfigKey(...args)
  const parseApiConfig = (...args) => __impl.parseApiConfig(...args)
  const extractApiUrl = (...args) => __impl.extractApiUrl(...args)
  const normalizeUrlParams = (...args) => __impl.normalizeUrlParams(...args)
  const resolveUrlParamValues = (...args) => __impl.resolveUrlParamValues(...args)
  const stableSerialize = (...args) => __impl.stableSerialize(...args)
  const hasFilledSearchParams = (...args) => __impl.hasFilledSearchParams(...args)
  const getNestedValue = (...args) => __impl.getNestedValue(...args)
  const toFiniteNumber = (...args) => __impl.toFiniteNumber(...args)
  const extractListRows = (...args) => __impl.extractListRows(...args)
  const extractListTotal = (...args) => __impl.extractListTotal(...args)
  const loadList = (...args) => __impl.loadList(...args)
  const enrichDocumentRuntimeRows = (...args) => __impl.enrichDocumentRuntimeRows(...args)
  const resolveDefaultRequestSortParams = (...args) => __impl.resolveDefaultRequestSortParams(...args)
  const applySearchTreeExpandedParams = (...args) => __impl.applySearchTreeExpandedParams(...args)
  const handleSearch = (...args) => __impl.handleSearch(...args)
  const handleReset = (...args) => __impl.handleReset(...args)
  const handleRefresh = (...args) => __impl.handleRefresh(...args)
  const handleSearchToggle = (...args) => __impl.handleSearchToggle(...args)
  const handleApplyCustomQuery = (...args) => __impl.handleApplyCustomQuery(...args)
  const handleClearCustomQuery = (...args) => __impl.handleClearCustomQuery(...args)
  const handleRenderModeChange = (...args) => __impl.handleRenderModeChange(...args)
  const handlePageChange = (...args) => __impl.handlePageChange(...args)
  const handlePageSizeChange = (...args) => __impl.handlePageSizeChange(...args)
  const normalizeEditData = (...args) => __impl.normalizeEditData(...args)
  const isMasterDetailPayload = (...args) => __impl.isMasterDetailPayload(...args)
  const resolveChildKey = (...args) => __impl.resolveChildKey(...args)
  const buildInitialChildrenData = (...args) => __impl.buildInitialChildrenData(...args)
  const normalizeChildrenData = (...args) => __impl.normalizeChildrenData(...args)
  const applyDetailData = (...args) => __impl.applyDetailData(...args)
  const buildMasterDetailSubmitData = (...args) => __impl.buildMasterDetailSubmitData(...args)
  const cloneInlineFormValue = (...args) => __impl.cloneInlineFormValue(...args)
  const buildInlineRecordTabKey = (...args) => __impl.buildInlineRecordTabKey(...args)
  const findReusableInlineFormTab = (...args) => __impl.findReusableInlineFormTab(...args)
  const activateReusableInlineFormTab = (...args) => __impl.activateReusableInlineFormTab(...args)
  const buildInlineFormTabKey = (...args) => __impl.buildInlineFormTabKey(...args)
  const openFormContainer = (...args) => __impl.openFormContainer(...args)
  const hydrateInlineFormTab = (...args) => __impl.hydrateInlineFormTab(...args)
  const persistActiveInlineFormTab = (...args) => __impl.persistActiveInlineFormTab(...args)
  const markActiveInlineFormClean = (...args) => __impl.markActiveInlineFormClean(...args)
  const handleInlineFormTabChange = (...args) => __impl.handleInlineFormTabChange(...args)
  const handleInlineWorkspaceListTab = (...args) => __impl.handleInlineWorkspaceListTab(...args)
  const inlineFormTabTitle = (...args) => __impl.inlineFormTabTitle(...args)
  const closeInlineFormTab = (...args) => __impl.closeInlineFormTab(...args)
  const handleCloseActiveInlineFormTab = (...args) => __impl.handleCloseActiveInlineFormTab(...args)
  const handleInlineFormCancel = (...args) => __impl.handleInlineFormCancel(...args)
  const handleInlineFormSubmitSuccess = (...args) => __impl.handleInlineFormSubmitSuccess(...args)
  const handleAdd = (...args) => __impl.handleAdd(...args)
  const handleAddChild = (...args) => __impl.handleAddChild(...args)
  const resolveTreeParentField = (...args) => __impl.resolveTreeParentField(...args)
  const resolveTreeParentValue = (...args) => __impl.resolveTreeParentValue(...args)
  const isPlainRecord = (...args) => __impl.isPlainRecord(...args)
  const handleEdit = (...args) => __impl.handleEdit(...args)
  const handleDetail = (...args) => __impl.handleDetail(...args)
  const loadDetail = (...args) => __impl.loadDetail(...args)
  const handleDelete = (...args) => __impl.handleDelete(...args)
  const handleBatchDelete = (...args) => __impl.handleBatchDelete(...args)
  const resolveBatchDeleteUrl = (...args) => __impl.resolveBatchDeleteUrl(...args)
  const fetchDataAuditMetaForDelete = (...args) => __impl.fetchDataAuditMetaForDelete(...args)
  const executePlainDelete = (...args) => __impl.executePlainDelete(...args)
  const performDelete = (...args) => __impl.performDelete(...args)
  const handleModalConfirm = (...args) => __impl.handleModalConfirm(...args)
  const handleModalCancel = (...args) => __impl.handleModalCancel(...args)
  const handleModalClose = (...args) => __impl.handleModalClose(...args)
  const resetFormOnly = (...args) => __impl.resetFormOnly(...args)
  const openFormOnlyWithRecordInit = (...args) => __impl.openFormOnlyWithRecordInit(...args)
  const resolveFormOnlyRecordInitId = (...args) => __impl.resolveFormOnlyRecordInitId(...args)

  const router = useRouter()

  const route = useRoute()

  const userStore = useUserStore()

  const slots = useSlots()

  /**
   * ==================== Refs ====================
   */
  const searchRef = ref(null)

  const tableRef = ref(null)

  const formRef = ref(null)

  const childFormRef = ref(null)

  const commandActionFormRef = ref(null)

  /**
   * ==================== 响应式数据 ====================
   */
  // 搜索参数
  const searchParams = ref({})

  // 表格数据
  const dataSource = ref([])

  const tableLoading = ref(false)

  const selectedKeys = ref([])

  const expandedRowKeys = ref([])

  const customQueryPayload = ref(null)

  const customQueryFields = ref([])

  const activeRenderMode = ref(props.renderMode || 'table')

  const searchPanelVisible = ref(props.showSearch !== false)

  // 分页
  const pagination = ref({
    page: props.pageNum,
    pageSize: props.pageSize,
    itemCount: 0,
  })

  // 弹窗
  const modalVisible = ref(false)

  const modalTitle = ref('')

  const modalStatus = ref('') // 'add' | 'edit' | 'detail'

  const formData = ref({})

  const childFormData = ref({})

  const confirmLoading = ref(false)

  const currentRow = ref(null)

  const fieldEventLoadToken = ref(0)

  const inlineFormTabs = ref([])

  const activeInlineFormTabKey = ref('')

  const INLINE_WORKSPACE_LIST_KEY = 'list'

  const activeInlineWorkspaceKey = ref(INLINE_WORKSPACE_LIST_KEY)

  const inlineFormHydrating = ref(false)

  const formOnlySubmitted = ref(false)

  const detailRuntime = ref(null)

  const detailRuntimeLoading = ref(false)

  const { loadRecordForm, loadDetailRuntime } = useRecordFormLoader({
    loadDetailOnEdit: () => props.loadDetailOnEdit,
    loadDetail,
    callHook,
    applyDetailData,
    fetchRuntime: (objectCode, recordId) => request.get(`/ai/business/document/${objectCode}/${recordId}/runtime`, { needTip: false }),
    resolveRuntimeObjectCode,
    resolveRowKeyValue,
    detailRuntime,
    detailRuntimeLoading,
  })

  const detailActiveTab = ref('business')

  const actionLoadingKeys = ref(new Set())

  const flowActionPageLoading = ref(false)

  const flowActionPageLoadingText = ref('')

  const flowStartApproverModalVisible = ref(false)

  const flowStartApproverSubmitting = ref(false)

  const flowStartApproverNodes = ref([])

  const flowStartApproverSelections = ref({})

  const flowStartApproverLabels = ref({})

  const flowStartApproverContext = ref(null)

  const commandActionModalVisible = ref(false)

  const commandActionSubmitting = ref(false)

  const commandActionFormData = ref({})

  const commandActionContext = ref(null)

  const childToolbarActionModalVisible = ref(false)

  const childToolbarActionSubmitting = ref(false)

  const childToolbarActionContext = ref(null)

  const childToolbarItemSelected = ref(null)

  const childToolbarQuantity = ref(1)

  const offlineFormRuntime = ref(null)

  const offlineDraftId = ref('')

  const offlineBaseRecordVersion = ref('')

  const offlineDraftNotice = ref('')

  const offlineDraftHydrating = ref(false)

  const offlineReplayLoading = ref(false)


  const {
    applyApplicationProcessRuntimeSnapshot,
    applyDocumentRuntimeSnapshot,
    disposeFlowRuntimeRefresh,
    queueFlowRuntimeRefresh,
  } = useFlowActionFeedback({
    dataSource,
    currentRow,
    detailRuntime,
    formData,
    resolveRowKeyValue,
    resolveDocumentRuntime,
    resolveRuntimeObjectCode,
    refreshCurrentDetailRuntime,
    loadList,
  })

  const commandActionTitle = computed(() => commandActionContext.value?.action?.label || commandActionContext.value?.action?.actionName || '业务动作')

  const commandActionFormSchema = computed(() => {
    const config = normalizeCommandActionRuntimeConfig(commandActionContext.value?.action || {})
    return Array.isArray(config.inputSchema)
      ? buildBusinessActionInputFormSchema(config.inputSchema)
      : (Array.isArray(config.formSchema) ? config.formSchema : [])
  })

  const commandActionFormContext = computed(() => ({
    row: commandActionContext.value?.row || null,
    action: commandActionContext.value?.action || null,
    mode: 'businessAction',
  }))

  const childToolbarActionTitle = computed(() =>
    childToolbarActionContext.value?.action?.label
    || childToolbarActionContext.value?.action?.actionName
    || '业务操作',
  )

  const childToolbarQuantityLabel = computed(() => {
    const actionCode = childToolbarActionContext.value?.action?.actionCode
    return actionCode === 'record_return' ? '退货数量' : '提货数量'
  })

  const childToolbarItemOptions = computed(() => {
    const items = childFormData.value?.presale_items || []
    return items
      .filter(item => item?.id)
      .map(item => ({
        label: `${item.productName || '商品'}（待提: ${item.pendingQuantity ?? 0}，已提: ${item.pickedQuantity ?? 0}）`,
        value: item.id,
      }))
  })

  const childToolbarQuantityMax = computed(() => {
    const items = childFormData.value?.presale_items || []
    const selectedItem = items.find(item => item?.id === childToolbarItemSelected.value)
    if (!selectedItem)
      return 999999
    const actionCode = childToolbarActionContext.value?.action?.actionCode
    if (actionCode === 'record_return')
      return selectedItem.pickedQuantity ?? 0
    return selectedItem.pendingQuantity ?? 0
  })


  const {
    importModalVisible,
    hasImportTemplate,
    exportLoading,
    exportTaskDrawerVisible,
    exportTaskLoading,
    exportTasks,
    exportTaskDrawerWidth,
    showExportTaskEntry,
    activeExportTask,
    exportTaskPaginationConfig,
    exportTaskColumns,
    handleShowImport,
    executeImport,
    handleImportSuccess,
    handleDownloadTemplate,
    handleExport,
    handleOpenExportTasks,
    clearExportTaskPollTimer,
    isExportTaskRunning,
    resolveExportTaskStatusText,
    resolveExportTaskTagType,
  } = useCrudImportExport({ props, searchParams, parseApiConfig, extractApiUrl, loadList })

  const visibleToolbarActions = computed(() => (Array.isArray(props.toolbarActions) ? props.toolbarActions : [])
    .filter(action => action?.visible !== false
      && hasRuntimePermission(action?.permissionCode)
      && matchDisplayCondition(action?.displayCondition || action?.visibleCondition, {})))

  /** 详情页动作独立于列表操作列，流程动作可在两个位置同时配置。 */
  const visibleDetailActions = computed(() => {
    const row = formData.value || currentRow.value || {}
    const configured = [
      ...(Array.isArray(props.detailActions) ? props.detailActions : []),
      ...(Array.isArray(props.runtimeActions)
        ? props.runtimeActions.filter(action => String(action?.position || '').toLowerCase() === 'detail')
        : []),
      ...(Array.isArray(detailRuntime.value?.runtimeActions)
        ? detailRuntime.value.runtimeActions.map(action => ({ ...action, position: action?.position || 'detail' }))
        : []),
    ]
    const actions = []
    for (const source of configured) {
      const action = normalizeRuntimeAction(source, row)
      if (!action || actions.some(item => sameAction(item, action)))
        continue
      actions.push(action)
    }
    return actions.filter((action) => {
      if (!isRuntimeActionForPosition(action, 'detail'))
        return false
      if (action.visible === false || !hasRuntimePermission(action.permissionCode))
        return false
      if (!matchDisplayCondition(action.displayCondition || action.visibleCondition, row))
        return false
      return !isStartFlowRuntimeHidden(action, row)
    })
  })

  /** 编辑表单页动作。显式 formActions 优先，兼容历史 runtimeActions.position=form。 */
  const visibleFormActions = computed(() => {
    const row = formData.value || currentRow.value || {}
    const configured = [
      ...(Array.isArray(props.formActions) ? props.formActions : []),
      ...(Array.isArray(props.runtimeActions)
        ? props.runtimeActions.filter(action => String(action?.position || '').toLowerCase() === 'form')
        : []),
      ...(Array.isArray(detailRuntime.value?.runtimeActions)
        ? detailRuntime.value.runtimeActions.map(action => ({ ...action, position: action?.position || 'form' }))
        : []),
    ]
    const actions = []
    for (const source of configured) {
      const action = normalizeRuntimeAction(source, row)
      if (!action || !isRuntimeActionForPosition(action, 'form') || actions.some(item => sameAction(item, action)))
        continue
      actions.push(action)
    }
    return actions.filter((action) => {
      if (String(action.position || '').toLowerCase() !== 'form')
        return false
      if (action.visible === false || !hasRuntimePermission(action.permissionCode))
        return false
      if (!matchDisplayCondition(action.displayCondition || action.visibleCondition, row))
        return false
      return !isStartFlowRuntimeHidden(action, row)
    })
  })

  /**
   * 操作列最大显示按钮数
   */
  const maxActionButtons = 2


  /**
   * ==================== 计算属性 ====================
   */
  /**

   * 行键函数

   */

  const rowKeyFn = computed(() => {
    if (typeof props.rowKey === 'function') {
      return props.rowKey
    }
    return row => row[props.rowKey]
  })

  const tableRowKeyFn = computed(() => {
    return (row) => {
      if (row && isUsableKeyValue(row.__listRowKey))
        return row.__listRowKey
      return rowKeyFn.value(row)
    }
  })

  const resolvedCustomQueryConfigKey = computed(() => {
    if (props.customQueryConfigKey) {
      return props.customQueryConfigKey
    }
    const listApi = props.apiConfig?.list || ''
    const match = listApi.match(/\/ai\/crud\/([^/]+)\/page/)
    return match?.[1] || ''
  })

  const toolbarOverflowOptions = computed(() => {
    const options = []
    if (!props.hideBatchDelete) {
      options.push({
        label: '批量删除',
        key: 'batch-delete',
        disabled: selectedKeys.value.length === 0,
        buttonType: 'error',
        secondary: true,
        iconComponent: TrashOutline,
      })
    }
    if (props.showImport) {
      options.push({
        label: '批量导入',
        key: 'import',
        buttonType: 'default',
        secondary: true,
        iconComponent: CloudUploadOutline,
      })
    }
    if (props.showExport) {
      options.push({
        label: exportLoading.value ? '导出中...' : props.exportButtonText,
        key: 'export',
        disabled: exportLoading.value,
        loading: exportLoading.value,
        buttonType: 'default',
        secondary: true,
        iconComponent: DownloadOutline,
      })
    }
    if (showExportTaskEntry.value) {
      options.push({
        label: '导出任务',
        key: 'export-tasks',
        buttonType: 'default',
        secondary: true,
        iconComponent: TimeOutline,
      })
    }
    return options
  })

  const toolbarDropdownOptions = computed(() => toolbarOverflowOptions.value.map(option => ({
    label: option.label,
    key: option.key,
    disabled: option.disabled,
    icon: () => h(NIcon, null, { default: () => h(option.iconComponent) }),
  })))

  const activeSourceColumns = computed(() => {
    if (!customQueryFields.value.length) {
      return props.columns
    }
    const actionColumns = props.columns.filter(isActionColumnConfig)
    const orderedColumns = customQueryFields.value
      .map(field => props.columns.find(col => getColumnKey(col) === field))
      .filter(Boolean)
    return [...orderedColumns, ...actionColumns]
  })

  /**
   * 表单上下文（传递 modalStatus 等信息）
   */
  const formContext = computed(() => {
    const runtimeContext = props.formRuntimeContext || {}
    return {
      // 未传 formRuntimeContext 的运行链路（低代码应用页等）兜底注入登录上下文，
      // 保证「初始化默认值 / 字段事件」的 currentUser.* 取值可用；调用方显式传入的优先。
      ...(runtimeContext.currentUser ? {} : buildFormRuntimeContext()),
      ...runtimeContext,
      modalStatus: modalStatus.value, // 'add' | 'edit' | 'detail'
      isEdit: modalStatus.value === 'edit',
      isAdd: modalStatus.value === 'add',
      isDetail: modalStatus.value === 'detail',
      currentRow: currentRow.value,
      formAssets: props.formAssets,
      fieldEvents: props.fieldEvents,
      fieldEventLoadToken: fieldEventLoadToken.value,
      formInit: props.formInit,
    }
  })

  const normalizedExpandConfig = computed(() => normalizeExpandConfig(props.expandConfig, props.childrenConfig))

  const hasExpandConfig = computed(() => normalizedExpandConfig.value.enabled && normalizedExpandConfig.value.panels.length > 0)

  const resolvedResizable = computed(() => props.tableProps?.resizable ?? props.resizable)

  /** 嵌入式树表：列表内父子展开（非左树右表）。 */
  function resolveEmbeddedTreeConfig() {
    const direct = props.treeConfig
    if (direct && typeof direct === 'object' && Object.keys(direct).length)
      return direct
    const fromOptions = props.options?.treeConfig
    if (fromOptions && typeof fromOptions === 'object' && Object.keys(fromOptions).length)
      return fromOptions
    // 仅有 apiConfig.tree 不足以认定嵌入式树表；残留 /tree 不能把平铺列表改成树查询
    return null
  }

  function isTreeEnabledFlag(value) {
    return value === true || value === 1 || value === '1' || value === 'true'
  }

  const isEmbeddedTreeTable = computed(() => {
    const treeConfig = resolveEmbeddedTreeConfig()
    if (!treeConfig)
      return false
    // 与 runtime-tree-table 一致：必须显式 enabled，避免残留 treeConfig 误打 /tree
    if (!isTreeEnabledFlag(treeConfig.enabled))
      return false
    const layoutType = String(props.layoutType || props.options?.layoutType || 'simple-crud')
    return layoutType !== 'tree-crud'
  })

  const embeddedTreeChildrenField = computed(() => resolveEmbeddedTreeConfig()?.childrenField || 'children')

  const effectiveShowPagination = computed(() => (isEmbeddedTreeTable.value ? false : props.showPagination !== false))

  const effectiveTableProps = computed(() => {
    const base = props.tableProps && typeof props.tableProps === 'object' ? { ...props.tableProps } : {}
    if (!isEmbeddedTreeTable.value)
      return base
    const loadMode = resolveEmbeddedTreeConfig()?.loadMode === 'lazy' ? 'lazy' : 'full'
    return {
      ...base,
      childrenKey: embeddedTreeChildrenField.value,
      defaultExpandAll: loadMode !== 'lazy' && base.defaultExpandAll !== false,
      ...(typeof base.onLoad === 'function' ? { onLoad: base.onLoad } : {}),
    }
  })

  function collectEmbeddedTreeExpandKeys(nodes = [], acc = []) {
    const childrenField = embeddedTreeChildrenField.value
    const keyField = resolveEmbeddedTreeConfig()?.keyField || (typeof props.rowKey === 'string' ? props.rowKey : 'id')
    ;(Array.isArray(nodes) ? nodes : []).forEach((node) => {
      if (!node || typeof node !== 'object')
        return
      const children = node[childrenField]
      if (!Array.isArray(children) || !children.length)
        return
      const key = node[keyField] ?? node.key ?? node.id
      if (key !== undefined && key !== null && key !== '')
        acc.push(key)
      collectEmbeddedTreeExpandKeys(children, acc)
    })
    return acc
  }

  const inlineSwitchUpdatingMap = ref({})

  /**
   * 表格列配置（添加操作列）
   */
  const tableColumns = computed(() => {
    // 行内开关 loading 态依赖此 map，变更时需重算列 render
    void inlineSwitchUpdatingMap.value
    const cols = []

    if (hasExpandConfig.value) {
      cols.push({
        type: 'expand',
        key: '__expand',
        width: 48,
        fixed: 'left',
        expandable: row => shouldExpandRow(normalizedExpandConfig.value.rowExpandable, row, formContext.value),
        renderExpand: row => h(AiCrudRowExpand, {
          config: normalizedExpandConfig.value,
          row,
          rowKeyValue: resolveRowKeyValue(row),
          context: formContext.value,
        }, collectExpandSlots()),
      })
    }

    // 判断是否是操作列（兼容 action / actions / operation 等写法）
    const isActionCol = (col) => {
      return isActionColumnConfig(col)
    }

    if ((dataSource.value || []).some(row => Array.isArray(row?._flowRelations) ? row._flowRelations.length : row?._dataScopeAccess === 'RELATED')) {
      cols.push({
        prop: '_flowRelations',
        label: '与我相关',
        width: 128,
        render: row => renderFlowRelationTags(row),
      })
    }

    activeSourceColumns.value.forEach((col) => {
      if (isActionCol(col) && col.actions) {
        const actionCol = { ...col }
        const actions = normalizeRowActions(col.actions)
        if (shouldDefaultActionFixedRight(actionCol)) {
          actionCol.fixed = 'right'
        }
        delete actionCol.actions
        delete actionCol._slot
        delete actionCol.slot
        actionCol.render = row => renderActionColumn(row, actions, col.maxActionButtons)
        cols.push(actionCol)
        return
      }
      if (isActionCol(col) && col.render) {
        cols.push({
          ...col,
          fixed: shouldDefaultActionFixedRight(col) ? 'right' : col.fixed,
        })
        return
      }
      if (isActionCol(col) && !col.actions && !col.render) {
        return
      }
      const resolvedCol = resolveColumnRender(col)
      cols.push(resolvedCol)
    })

    // 如果没有操作列且没有隐藏，添加默认操作列
    const hasActionColumn = cols.some(isActionCol)

    if (!hasActionColumn) {
      cols.push({
        prop: 'action',
        label: '操作',
        width: 200,
        fixed: 'right',
        render: (row) => {
          const actions = normalizeRowActions([
            { label: '编辑', key: 'edit', type: 'primary' },
            { label: '删除', key: 'delete', type: 'error' },
          ])
          return renderActionColumn(row, actions)
        },
      })
    }

    return cols
  })

  /**
   * 分页配置
   */
  const paginationConfig = computed(() => {
    if (!effectiveShowPagination.value) {
      return false
    }

    return {
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
      itemCount: pagination.value.itemCount,
      pageCount: Math.ceil(pagination.value.itemCount / pagination.value.pageSize),
      showSizePicker: true,
      pageSizes: props.pageSizes,
      showQuickJumper: true,
      prefix: ({ itemCount }) => `共${itemCount}条`,
    }
  })

  /**
   * 搜索表单插槽名称
   */
  const searchSlots = computed(() => {
    return props.searchSchema
      .filter(field => field.type === 'slot')
      .map(field => field.slotName || field.field)
  })

  /**
   * 表格插槽名称
   */
  const tableSlots = computed(() => {
    return props.columns
      .filter(col => (col.slot || col._slot) && !col.actions)
      .map(col => col.slot || col._slot)
  })

  /**
   * 表单插槽名称
   */
  const formSlots = computed(() => {
    return props.editSchema
      .filter(field => field.type === 'slot')
      .map(field => field.slotName || field.field)
  })

  const isDetailMode = computed(() => modalStatus.value === 'detail')

  const resolvedFormOpenMode = computed(() => {
    const configured = String(props.formOpenMode || '').trim()
    if (configured === 'tabWorkspace' || configured.toLowerCase() === 'tabworkspace')
      return 'tabWorkspace'
    const normalized = configured.toLowerCase()
    if (['modal', 'drawer', 'flat'].includes(normalized))
      return normalized
    return String(props.modalType || '').trim().toLowerCase() === 'drawer' ? 'drawer' : 'modal'
  })

  const usesInlineFormWorkspace = computed(() => ['flat', 'tabWorkspace'].includes(resolvedFormOpenMode.value))

  const isTabWorkspaceMode = computed(() => resolvedFormOpenMode.value === 'tabWorkspace')

  const inlineWorkspaceVisible = computed(() => usesInlineFormWorkspace.value && inlineFormTabs.value.length > 0)

  const showInlineListPane = computed(() => {
    return !inlineWorkspaceVisible.value
      || (isTabWorkspaceMode.value && activeInlineWorkspaceKey.value === INLINE_WORKSPACE_LIST_KEY)
  })

  const showInlineFormWorkspacePane = computed(() => {
    return inlineWorkspaceVisible.value
      && (!isTabWorkspaceMode.value || activeInlineWorkspaceKey.value !== INLINE_WORKSPACE_LIST_KEY)
  })

  const tiledEditGridCols = computed(() => {
    const configured = Number(props.editGridCols || 1)
    if (configured > 1)
      return configured
    const fieldCount = flattenRuntimeFormFields(props.editSchema || []).filter(field => field?.field && field.hidden !== true).length
    return fieldCount >= 2 ? 2 : 1
  })

  const activeInlineFormTab = computed(() => inlineFormTabs.value.find(tab => tab.key === activeInlineFormTabKey.value) || null)

  const activeInlineFormTitle = computed(() => activeInlineFormTab.value?.title || modalTitle.value || '编辑')

  const inlineFormModeLabel = computed(() => {
    if (modalStatus.value === 'add')
      return '新增'
    if (modalStatus.value === 'edit')
      return '编辑'
    if (modalStatus.value === 'detail')
      return '详情'
    return '表单'
  })

  const showInlineFormModeTag = computed(() => {
    const title = String(activeInlineFormTitle.value || '').trim()
    const mode = String(inlineFormModeLabel.value || '').trim()
    if (!mode)
      return false
    return title !== mode && !title.includes(mode)
  })

  const resolvedTabWorkspace = computed(() => {
    const config = props.tabWorkspace || {}
    return {
      maxTabs: Math.max(1, Number(config.maxTabs || 8)),
      reuseRecordTab: config.reuseRecordTab !== false,
      closeAfterSave: config.closeAfterSave === true,
      showDirtyMark: config.showDirtyMark !== false,
    }
  })

  const activeModalWidth = computed(() => {
    if (isDetailMode.value)
      return props.detailModalWidth
    return props.modalWidth
  })

  const detailFlowTimelineVisible = computed(() => {
    return readBoolean(detailRuntime.value?.detailFlowTimelineVisible, true)
  })

  const detailFlowDiagramVisible = computed(() => {
    return readBoolean(detailRuntime.value?.detailFlowDiagramVisible, true)
  })

  const showDetailFlowTabs = computed(() => {
    return shouldShowDetailFlowHistory({
      isDetailMode: isDetailMode.value,
      runtime: detailRuntime.value,
      timelineVisible: detailFlowTimelineVisible.value,
      diagramVisible: detailFlowDiagramVisible.value,
    })
  })

  const dataAuditMeta = computed(() => readDataAuditMeta(formData.value) || readDataAuditMeta(currentRow.value))

  const showDataChangeLogTab = computed(() => {
    const legacyConfigured = props.showDataChangeLog === true || props.options?.showDataChangeLog === true
    return isDetailMode.value && shouldShowDataAuditHistory(dataAuditMeta.value, legacyConfigured)
  })

  const showDetailExtraTabs = computed(() => showDetailFlowTabs.value || showDataChangeLogTab.value)

  const dataAuditObjectId = computed(() => {
    return dataAuditMeta.value?.objectId || props.dataAuditObjectId || props.options?.dataAuditObjectId || ''
  })

  const dataAuditRecordId = computed(() => {
    const key = props.rowKey || 'id'
    const value = formData.value?.[key] ?? currentRow.value?.[key]
    return value == null ? '' : String(value)
  })

  const dataAuditEnabled = computed(() => dataAuditMeta.value?.enabled === true)

  const dataAuditHistoryAvailable = computed(() => isDataAuditHistoryAvailable(dataAuditMeta.value))

  const visibleChildrenConfig = computed(() => {
    const status = modalStatus.value || 'add'
    return (props.childrenConfig || []).filter((child) => {
      if (!child?.fields?.length)
        return false
      if (status === 'add')
        return child.showInCreate !== false
      if (status === 'edit')
        return child.showInEdit !== false
      if (status === 'detail')
        return child.showInDetail !== false
      return true
    })
  })

  const hasChildrenConfig = computed(() => visibleChildrenConfig.value.some(child => child?.fields?.length))

  const showDefaultDetailContent = computed(() => {
    return !isDetailMode.value || !props.hideDefaultDetailContent
  })

  const showDefaultDetailChildren = computed(() => {
    return hasChildrenConfig.value && showDefaultDetailContent.value
  })

  const normalizedDetailPanelConfig = computed(() => normalizeExpandConfig({
    enabled: isDetailMode.value && Array.isArray(props.detailPanels) && props.detailPanels.length > 0,
    lazy: true,
    cache: false,
    layout: {
      mode: props.detailPanels?.length > 1 ? 'tabs' : 'single',
      density: 'compact',
      padding: 12,
    },
    panels: props.detailPanels || [],
  }, props.childrenConfig))

  const showDetailPanels = computed(() => isDetailMode.value && normalizedDetailPanelConfig.value.enabled && normalizedDetailPanelConfig.value.panels.length > 0)

  const detailPanelRowKeyValue = computed(() => {
    const value = resolveRowKeyValue(formData.value)
    if (isUsableKeyValue(value))
      return value
    return resolveRowKeyValue(currentRow.value) || `detail_${modalStatus.value || 'current'}`
  })

  const hasSearchSchema = computed(() => !props.formOnly && props.showSearch && props.searchSchema.length > 0)

  const hasActiveListFilters = computed(() => hasFilledSearchParams(searchParams.value) || !!customQueryPayload.value)

  const resolvedEmptyTitle = computed(() => hasActiveListFilters.value ? '没有匹配结果' : '暂无数据')

  const resolvedEmptyDescription = computed(() => {
    if (hasActiveListFilters.value)
      return '调整筛选条件后再试'
    return '当前列表还没有记录'
  })

  const normalizedSearchSchema = computed(() => {
    return (props.searchSchema || []).map((field) => {
      const propsData = { ...(field.props || {}) }
      delete propsData.showCount
      delete propsData.showWordLimit
      return {
        ...field,
        props: propsData,
        showCount: false,
        showWordLimit: false,
      }
    })
  })

  const modalFormSchema = computed(() => {
    if (!isDetailMode.value)
      return props.editSchema
    const schema = props.editSchema.map(toReadonlyField)
    return schema.length > 0 ? schema : buildDetailFallbackSchema(props.columns)
  })

  const {
    runtimeFormulaFields,
    runtimeFormulaCalculationEnabled,
    runtimeFormulaSignature,
    scheduleRuntimeFormulaCalculation,
    refreshRuntimeFormulas,
    clearRuntimeFormulaTimers,
  } = useCrudFormula({ props, formData, isDetailMode, formOnlySubmitted, modalVisible, inlineWorkspaceVisible })

  const offlineReplayAvailable = computed(() => {
    const runtime = offlineFormRuntime.value
    if (!runtime || !offlineDraftId.value)
      return false
    const draft = runtime.store.getDraft(offlineDraftId.value)
    return Boolean(draft?.replayLog?.some(item => item.status !== 'COMPLETED'))
  })

  const resolvedFormOnlyTitle = computed(() => props.formOnlyTitle || props.addButtonText || '单据填报')

  const resolvedEditFormClass = computed(() => [
    'ai-crud-edit-form',
    props.editFormClass,
  ].filter(Boolean))

  /**
   * 计算横向滚动宽度
   * 如果没有设置 scrollX，自动计算所有列的宽度总和
   */
  const DEFAULT_SELECTION_COLUMN_WIDTH = 48

  const DEFAULT_EXPAND_COLUMN_WIDTH = 48

  const DEFAULT_ACTION_COLUMN_WIDTH = 180

  const DEFAULT_DATA_COLUMN_WIDTH = 120

  const TABLE_SCROLL_X_BUFFER = 24

  const computedScrollX = computed(() => {
    if (props.scrollX !== undefined) {
      return props.scrollX
    }

    if (!tableColumns.value.length) {
      return undefined
    }

    let totalWidth = props.hideSelection ? 0 : DEFAULT_SELECTION_COLUMN_WIDTH
    tableColumns.value.forEach((col) => {
      const width = resolveColumnWidth(col.width ?? col.minWidth)
      if (width > 0) {
        totalWidth += width
        return
      }
      if (col.type === 'expand') {
        totalWidth += DEFAULT_EXPAND_COLUMN_WIDTH
        return
      }
      if (isActionColumnConfig(col)) {
        totalWidth += DEFAULT_ACTION_COLUMN_WIDTH
        return
      }
      totalWidth += DEFAULT_DATA_COLUMN_WIDTH
    })

    return totalWidth > 0 ? totalWidth + TABLE_SCROLL_X_BUFFER : undefined
  })

  /**
   * 表格高度交给外层 flex 容器和 Naive flex-height 处理，避免页面级滚动条。
   */
  const computedMaxHeight = computed(() => {
    if (props.maxHeight !== undefined) {
      return props.maxHeight
    }
    return undefined
  })

  const { crudRootRef, pageHeightStyle } = useCrudPageHeight({ props, showInlineFormWorkspacePane })

  /**
   * ==================== 方法 ====================
   */
  /**

   * 执行钩子函数

   * @param {string} hookName - 钩子函数名

   * @param {*} params - 参数

   * @param {Function} success - 成功回调

   * @returns {Promise} 处理后的钩子结果

   */

  function isPrintRuntimeAction(action = {}) {
    const key = String(action?.key || '')
    if (key.startsWith('forgePrint:'))
      return true
    const routePath = String(action?.routePath || '')
    return routePath === '/print/preview' || routePath.startsWith('/print/preview?')
  }

  function resolveDetailActionIcon(action = {}) {
    if (isPrintRuntimeAction(action))
      return PrintOutline
    return null
  }

  /**
   * 渲染操作列（支持自动折叠）
   * 使用文字链接风格，紧凑排列，超过 maxActionButtons 个时折叠到"更多"下拉
   * @param {object} row - 行数据
   * @param {Array} actions - 操作按钮配置 [{ label, key, type, onClick, visible }]
   */
  function renderActionColumn(row, actions, maxVisibleActions = maxActionButtons) {
    const mergedActions = mergeRuntimeActions(actions, row)
    // 过滤不可见的按钮
    const visibleActions = mergedActions.filter((action) => {
      if (typeof action.visible === 'function')
        return action.visible(row)
      if (action.visible === false)
        return false
      if (!hasRuntimePermission(action.permissionCode))
        return false
      if (!matchDisplayCondition(action.displayCondition || action.visibleCondition, row))
        return false
      if (isStartFlowRuntimeHidden(action, row))
        return false
      return true
    })

    if (visibleActions.length === 0) {
      return h('span', { style: { color: '#999' } }, '-')
    }

    // 所有按钮都能直接显示
    if (visibleActions.length <= maxVisibleActions) {
      return h('div', { class: 'table-action-column' }, visibleActions.map((action, index) => {
        const nodes = []
        if (index > 0) {
          nodes.push(h('span', { class: 'table-action-divider' }, ' | '))
        }
        const typeClass = action.type ? `type-${action.type}` : ''
        const isDanger = action.type === 'error' || action.type === 'danger'
        const loading = isActionLoading(action, row)
        const disabled = isActionDisabled(action, row) || loading
        nodes.push(h('a', {
          class: ['table-action-link', typeClass, isDanger ? 'danger' : '', disabled ? 'disabled' : '', loading ? 'loading' : ''],
          title: disabled ? actionDisabledReason(action, row) : action.label,
          onClick: (e) => {
            e?.stopPropagation()
            e?.preventDefault()
            if (disabled) {
              showActionDisabledMessage(action, row)
              return
            }
            if (action.onClick)
              handleCustomActionClick(action, row)
            else handleActionClick(action, row)
          },
        }, resolveActionDisplayLabel(action, row)))
        return nodes
      }).flat())
    }

    // 需要折叠：显示前 maxActionButtons 个，其余放入"更多"下拉
    const inlineActions = visibleActions.slice(0, maxVisibleActions)
    const dropdownOptions = visibleActions.slice(maxVisibleActions).map(action => ({
      label: resolveActionDisplayLabel(action, row),
      key: action.key || action.label,
      disabled: isActionDisabled(action, row) || isActionLoading(action, row),
    }))

    const inlineNodes = inlineActions.map((action, index) => {
      const nodes = []
      if (index > 0) {
        nodes.push(h('span', { class: 'table-action-divider' }, ' | '))
      }
      const typeClass = action.type ? `type-${action.type}` : ''
      const isDanger = action.type === 'error' || action.type === 'danger'
      const loading = isActionLoading(action, row)
      const disabled = isActionDisabled(action, row) || loading
      nodes.push(h('a', {
        class: ['table-action-link', typeClass, isDanger ? 'danger' : '', disabled ? 'disabled' : '', loading ? 'loading' : ''],
        title: disabled ? actionDisabledReason(action, row) : action.label,
        onClick: (e) => {
          e?.stopPropagation()
          e?.preventDefault()
          if (disabled) {
            showActionDisabledMessage(action, row)
            return
          }
          if (action.onClick)
            handleCustomActionClick(action, row)
          else handleActionClick(action, row)
        },
      }, resolveActionDisplayLabel(action, row)))
      return nodes
    }).flat()

    return h('div', { class: 'table-action-column' }, [
      ...inlineNodes,
      h('span', { class: 'table-action-divider' }, ' | '),
      h(NDropdown, {
        options: dropdownOptions,
        trigger: 'click',
        onSelect: (key) => {
          const action = visibleActions.find(a => (a.key || a.label) === key)
          if (action && isActionDisabled(action, row)) {
            showActionDisabledMessage(action, row)
            return
          }
          if (action && isActionLoading(action, row)) {
            showActionDisabledMessage(action, row)
            return
          }
          if (action?.onClick)
            handleCustomActionClick(action, row)
          else handleActionClick(action || key, row)
        },
      }, {
        default: () => h('a', {
          class: 'table-action-link',
          onClick: (e) => { e?.preventDefault() },
        }, '更多'),
      }),
    ])
  }

  function mergeRuntimeActions(actions = [], row) {
    const next = Array.isArray(actions) ? [...actions] : []
    const runtimeActions = [
      ...(Array.isArray(props.runtimeActions) ? props.runtimeActions : []),
      ...(Array.isArray(row?._runtimeActions) ? row._runtimeActions : []),
    ]
      .filter(action => isRuntimeActionForPosition(action, 'row'))
      .map(action => normalizeRuntimeAction(action, row))
      .filter(Boolean)
    runtimeActions.forEach((action) => {
      const existingIndex = next.findIndex(item => sameAction(item, action))
      if (existingIndex >= 0) {
        next[existingIndex] = {
          ...next[existingIndex],
          ...action,
          label: next[existingIndex].label || action.label,
        }
      }
      else {
        next.push(action)
      }
    })
    return next
  }

  function normalizeRuntimeAction(action, row) {
    if (!action || action.visible === false)
      return null
    const key = action.key || action.actionType || action.label
    if (!key)
      return null
    return {
      ...action,
      key,
      label: action.label || key,
      actionType: action.actionType || key,
      objectCode: resolveRuntimeObjectCode(action, row),
      recordId: action.recordId || resolveRowKeyValue(row),
    }
  }

  function isStartFlowRuntimeHidden(action = {}, row = {}) {
    const runtime = row?._documentRuntime
      || row?.documentRuntime
      || (modalStatus.value === 'detail' ? detailRuntime.value : null)
      || {}
    return shouldHideProcessStartAction(action, runtime)
  }

  function resolveRuntimeObjectCode(action = {}, row = {}) {
    return firstNonBlankText(
      action.objectCode,
      action.businessObjectCode,
      action.targetObjectCode,
      action.referenceObjectCode,
      action.candidateObjectCode,
      action.props?.objectCode,
      action.props?.businessObjectCode,
      props.businessObjectCode,
      row?._runtimeObjectCode,
      row?.objectCode,
      row?.businessObjectCode,
      row?.targetObjectCode,
      row?.referenceObjectCode,
    )
  }

  function firstNonBlankText(...values) {
    for (const value of values) {
      const text = String(value ?? '').trim()
      if (text)
        return text
    }
    return ''
  }

  function sameAction(left, right) {
    if (!left || !right)
      return false
    const leftKey = String(left.key || '').toUpperCase()
    const rightKey = String(right.key || '').toUpperCase()
    const leftType = String(left.actionType || '').toUpperCase()
    const rightType = String(right.actionType || '').toUpperCase()
    if (leftKey && rightKey && leftKey === rightKey)
      return true
    return leftType && rightType && leftType === rightType
  }

  function isActionDisabled(action, row) {
    if (isFlowRelatedReadOnly(action, row))
      return true
    if (typeof action.disabled === 'function')
      return !!action.disabled(row)
    return !!action.disabled
  }

  function isFlowRelatedReadOnly(action, row) {
    if (row?._dataScopeAccess !== 'RELATED')
      return false
    const key = String(action.key || action.label || '').toLowerCase()
    return ['edit', 'delete', 'remove', '编辑', '删除'].includes(key)
  }

  function renderFlowRelationTags(row) {
    const tags = []
    if (row?._dataScopeAccess === 'OWN')
      tags.push({ label: '我的', type: 'info' })
    else if (Array.isArray(row?._flowRelations) && row._flowRelations.includes('INITIATOR'))
      tags.push({ label: '我发起', type: 'info' })
    if (Array.isArray(row?._flowRelations) && row._flowRelations.includes('ASSIGNEE'))
      tags.push({ label: '我审批', type: 'success' })
    if (Array.isArray(row?._flowRelations) && row._flowRelations.includes('CC'))
      tags.push({ label: '抄送', type: 'warning' })
    if (!tags.length)
      return h('span', { style: { color: '#94a3b8' } }, '-')
    return h('div', { class: 'flow-relation-tags' }, tags.map(tag => h(NTag, {
      size: 'small',
      bordered: false,
      type: tag.type,
    }, { default: () => tag.label })))
  }

  function actionDisabledReason(action, row) {
    if (isActionLoading(action, row))
      return resolveActionTextValue(action.loadingReason, row) || '操作执行中，请稍候'
    if (isFlowRelatedReadOnly(action, row))
      return '流程经手可见仅支持查看，不能修改或删除'
    if (typeof action.disabledReason === 'function')
      return action.disabledReason(row)
    return action.disabledReason || '当前状态不可执行'
  }

  function showActionDisabledMessage(action, row) {
    window.$message?.warning(actionDisabledReason(action, row))
  }

  function resolveActionDisplayLabel(action, row) {
    if (isActionLoading(action, row)) {
      const loadingLabel = resolveActionTextValue(action.loadingLabel, row)
      if (loadingLabel)
        return loadingLabel
      const actionType = String(action?.actionType || action?.key || '').toUpperCase()
      if (['START_FLOW', 'START_APPROVAL', 'START_PROCESS'].includes(actionType)
        || actionType.startsWith('STARTPROCESS:')) {
        return '发起中...'
      }
      if (actionType === 'RESUBMIT_FLOW') {
        return '提交中...'
      }
      if (actionType === 'WITHDRAW_FLOW') {
        return '撤回中...'
      }
    }
    return resolveActionTextValue(action.label, row) || action.key || ''
  }

  function resolveActionTextValue(value, row) {
    if (typeof value === 'function')
      return value(row)
    if (value === undefined || value === null)
      return ''
    return String(value)
  }

  async function handleCustomActionClick(action, row) {
    const loadingKey = getActionLoadingKey(action, row)
    if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
      window.$message?.info(resolveActionTextValue(action.loadingReason, row) || '操作正在执行，请稍候')
      return
    }
    setActionLoading(loadingKey, true)
    try {
      if (!(await runBeforeRowAction(action, row)))
        return
      await action.onClick(row)
    }
    catch (error) {
      const failureMessage = resolveActionTextValue(action.failureMessage, row) || error?.message || '操作失败'
      window.$message?.error(failureMessage)
    }
    finally {
      setActionLoading(loadingKey, false)
    }
  }

  function hasRuntimePermission(permissionCode = '') {
    const code = String(permissionCode || '').trim()
    if (!code)
      return true
    if (userStore.isAdmin || userStore.isTenantAdmin)
      return true
    const permissions = [
      ...(Array.isArray(userStore.permissions) ? userStore.permissions : []),
      ...(Array.isArray(userStore.apiPermissions) ? userStore.apiPermissions : []),
      ...(Array.isArray(userStore.getDataPermission) ? userStore.getDataPermission : []),
    ]
    return permissions.includes('**') || permissions.includes(code)
  }

  function matchDisplayCondition(expression = '', row = {}) {
    return matchesRuntimeDisplayCondition(expression, row)
  }

  function resolveConditionValue(row = {}, path = '') {
    return String(path || '').split('.').filter(Boolean).reduce((value, key) => value?.[key], row)
  }

  /**
   * 处理操作列按钮点击（内置 key 映射）
   */
  async function handleActionClick(actionOrKey, row) {
    const action = typeof actionOrKey === 'string' ? { key: actionOrKey, label: actionOrKey } : actionOrKey || {}
    if (row?._dataScopeWritable === false && ['edit', 'delete', 'addChild'].includes(action.key)) {
      window.$message.warning('该节点仅用于导航展示，不能执行数据操作')
      return
    }
    if (!(await runBeforeRowAction(action, row)))
      return
    switch (action.key) {
      case 'addChild':
        handleAddChild(row)
        break
      case 'edit':
        handleEdit(row)
        break
      case 'detail':
        handleDetail(row)
        break
      case 'delete':
        handleDelete(row)
        break
      default:
        handleConfiguredAction(action, row, { skipExtensionHook: true })
        break
    }
  }

  async function handleConfiguredAction(action, row, options = {}) {
    if (!options.skipExtensionHook && !(await runBeforeRowAction(action, row)))
      return
    const actionType = action.actionType || 'route'
    const normalizedActionType = String(actionType).toUpperCase()
    if (actionType === 'START_FLOW' || action.key === 'START_FLOW') {
      await startFlowAction(action, row)
      return
    }
    if (normalizedActionType === 'RESUBMIT_FLOW' || action.key === 'RESUBMIT_FLOW') {
      await resubmitFlowAction(action, row)
      return
    }
    if (normalizedActionType === 'HANDLE_TASK' || action.key === 'HANDLE_TASK') {
      handleTaskAction(row)
      return
    }
    if (normalizedActionType === 'WITHDRAW_FLOW' || action.key === 'WITHDRAW_FLOW') {
      await withdrawFlowAction(action, row)
      return
    }
    if (normalizedActionType === 'START_PROCESS' || String(action.key || '').startsWith('startProcess:')) {
      await startProcessAction(action, row)
      return
    }
    emit('custom-action', { action, row })
    if (action.confirmText && !(await confirmConfiguredAction(resolveActionText(action.confirmText, row))))
      return
    if (normalizedActionType === 'COMMAND') {
      await handleCommandAction(action, row)
      return
    }
    if (actionType === 'refresh') {
      loadList()
      return
    }
    if (['CALL_API', 'REQUEST'].includes(normalizedActionType)) {
      await callConfiguredApiAction(action, row)
      return
    }
    const targetPageKey = String(action.targetPageKey || '').trim()
    if (targetPageKey && (
      actionType === 'page'
      || normalizedActionType === 'PAGE'
      || normalizedActionType === 'OPEN_PAGE'
      || !action.routePath
    )) {
      navigateCustomActionToPage(action, row)
      handleConfiguredActionSuccess(action)
      return
    }
    if (actionType === 'route' && action.routePath) {
      const path = buildActionTarget(action, row)
      if ((action.openTarget || '_self') === '_blank') {
        const route = router.resolve(path)
        window.open(route.href, '_blank')
      }
      else {
        router.push(path)
      }
      handleConfiguredActionSuccess(action)
      return
    }
    if (actionType === 'external' && action.routePath) {
      window.open(buildActionTarget(action, row), action.openTarget || '_blank')
      handleConfiguredActionSuccess(action)
    }
  }

  function navigateCustomActionToPage(action = {}, row = {}) {
    const targetPageKey = String(action.targetPageKey || '').trim()
    if (!targetPageKey)
      return
    const query = { ...route.query }
    if (isApplicationRuntimeRoute()) {
      query.pageId = targetPageKey
      delete query.pageKey
    }
    else {
      query.pageKey = targetPageKey
    }
    if (action.targetFormKey)
      query.formKey = action.targetFormKey
    else
      delete query.formKey
    const params = Array.isArray(action.params) ? action.params : []
    params.forEach((param) => {
      const name = String(param?.name || '').trim()
      if (!name)
        return
      const value = resolveActionParamValue(param, row)
      if (value !== '' && value !== undefined && value !== null)
        query[name] = value
    })
    const nextRoute = { path: route.path, query, hash: route.hash }
    if ((action.openTarget || '_self') === '_blank') {
      const resolved = router.resolve(nextRoute)
      window.open(resolved.href, '_blank')
      return
    }
    router.push(nextRoute)
  }

  function isApplicationRuntimeRoute() {
    return Boolean(route.params?.applicationCode)
      || String(route.name || '').includes('Application')
      || String(route.path || '').includes('/app-center/application/')
      || String(route.path || '').startsWith('/app/')
  }

  async function runBeforeRowAction(action, row) {
    if (typeof props.beforeRowAction !== 'function')
      return true
    try {
      return (await props.beforeRowAction({ action, row })) !== false
    }
    catch (error) {
      window.$message?.error(error?.message || '行操作增强执行失败')
      return false
    }
  }

  async function handleFormValueUpdate(value = {}) {
    if (typeof props.formChange !== 'function' || modalStatus.value === 'detail')
      return
    const sequence = ++mut.formChangeSequence
    try {
      const result = await props.formChange({
        data: { ...(value || {}) },
        record: { ...(value || {}) },
        modalStatus: modalStatus.value,
      })
      if (sequence !== mut.formChangeSequence || result === false)
        return
      const next = result?.record || result?.data || result
      if (next && typeof next === 'object' && !Array.isArray(next)
        && JSON.stringify(next) !== JSON.stringify(formData.value)) {
        formData.value = { ...next }
      }
    }
    catch (error) {
      window.$message?.error(error?.message || '字段联动增强执行失败')
    }
  }

  function isChildRowActionVisible(action, _child, row) {
    return action?.status !== 0
      && action?.visible !== false
      && hasRuntimePermission(action?.permissionCode)
      && matchDisplayCondition(action?.displayCondition || action?.visibleCondition, row)
  }

  function isChildRowActionLoading(action, _child, row) {
    const recordId = row?.id ?? row?.ID ?? ''
    return isActionLoading({ ...action, recordId }, row)
  }

  async function handleChildRowAction({ action, row, executionContext } = {}) {
    if (!action || !executionContext?.persisted) {
      window.$message?.warning('请先保存主记录和子表行')
      return
    }
    if (String(action.actionType || '').toUpperCase() !== 'COMMAND') {
      window.$message?.warning('子表行按钮仅支持事务型业务命令')
      return
    }
    await handleConfiguredAction({
      ...action,
      recordId: executionContext.childRecordId,
      childActionContext: executionContext,
    }, row)
  }

  async function handleChildToolbarAction({ action, child } = {}) {
    if (!action)
      return
    const parentRecordId = formData.value?.id
    if (!parentRecordId) {
      window.$message?.warning('请先保存主记录')
      return
    }
    const items = childFormData.value?.presale_items || []
    const persistedItems = items.filter(item => item?.id)
    if (!persistedItems.length) {
      window.$message?.warning('请先添加并保存商品明细')
      return
    }
    childToolbarActionContext.value = { action, child }
    childToolbarItemSelected.value = null
    childToolbarQuantity.value = 1
    childToolbarActionModalVisible.value = true
  }

  async function submitChildToolbarAction() {
    const context = childToolbarActionContext.value
    if (!context?.action)
      return
    const selectedItem = (childFormData.value?.presale_items || [])
      .find(item => item?.id === childToolbarItemSelected.value)
    if (!selectedItem) {
      window.$message?.warning('请选择商品')
      return
    }
    const executionContext = buildChildRowActionContext({
      child: { relationKey: context.action.relationKey || 'presale_items' },
      parentRecord: formData.value,
      childRecord: selectedItem,
    })
    if (!executionContext.persisted) {
      window.$message?.warning('记录未保存，无法执行操作')
      return
    }
    childToolbarActionModalVisible.value = false
    childToolbarActionSubmitting.value = true
    try {
      await executeCommandAction(
        { ...context.action, childActionContext: executionContext, recordId: executionContext.childRecordId },
        selectedItem,
        { quantity: childToolbarQuantity.value },
        { fromModal: true },
      )
    }
    finally {
      childToolbarActionSubmitting.value = false
    }
  }

  function closeChildToolbarActionModal() {
    if (childToolbarActionSubmitting.value)
      return
    childToolbarActionModalVisible.value = false
    childToolbarActionContext.value = null
    childToolbarItemSelected.value = null
    childToolbarQuantity.value = 1
  }

  function handleConfiguredActionSuccess(action = {}) {
    if (action.successBehavior === 'refreshList')
      loadList()
    else if (action.successBehavior === 'goBack')
      router.back()
  }

  async function startProcessAction(action, row) {
    const applicationCode = firstNonBlankText(action.applicationCode, action.props?.applicationCode)
    const processCode = firstNonBlankText(action.processCode, action.props?.processCode)
    const objectCode = resolveRuntimeObjectCode(action, row)
    const recordId = action.recordId || resolveRowKeyValue(row)
    if (!applicationCode || !processCode) {
      window.$message.warning('缺少应用或流程编码，无法启动业务流程')
      return
    }
    if (!recordId) {
      window.$message.warning('缺少业务记录ID，无法启动业务流程')
      return
    }
    const loadingKey = getActionLoadingKey(action, row)
    if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
      window.$message.info('业务流程正在启动，请稍候')
      return
    }
    const confirmed = await confirmConfiguredAction(
      resolveActionText(action.confirmText || `确定要启动“${action.label || processCode}”吗？`, row),
      { title: action.label || '启动业务流程', positiveText: '启动' },
    )
    if (!confirmed)
      return
    setActionLoading(loadingKey, true)
    setFlowActionPageLoading(true, '正在发起流程...')
    try {
      const processStartConfig = await businessProcessStartConfig(applicationCode, processCode)
      const processNodes = Array.isArray(processStartConfig?.data?.initiatorSelectNodes)
        ? processStartConfig.data.initiatorSelectNodes
        : []
      if (processStartConfig?.code === 200 && processNodes.length) {
        flowStartApproverNodes.value = processNodes
        flowStartApproverSelections.value = {}
        flowStartApproverLabels.value = {}
        flowStartApproverContext.value = {
          objectCode,
          recordId,
          row,
          loadingKey,
          processCode,
          submit: variables => startBusinessProcess(applicationCode, processCode, {
            recordId: String(recordId),
            objectCode: objectCode || undefined,
            variables,
          }),
        }
        flowStartApproverModalVisible.value = true
        return
      }
      const res = await startBusinessProcess(applicationCode, processCode, {
        recordId: String(recordId),
        objectCode: objectCode || undefined,
      })
      window.$message.success('业务流程已启动')
      applyApplicationProcessRuntimeSnapshot(row, processCode, res?.data)
      queueFlowRuntimeRefresh(row)
    }
    catch (error) {
      window.$message.error(error.message || '启动业务流程失败')
    }
    finally {
      setFlowActionPageLoading(false)
      setActionLoading(loadingKey, false)
    }
  }


  __impl.isPrintRuntimeAction = isPrintRuntimeAction
  __impl.resolveDetailActionIcon = resolveDetailActionIcon
  __impl.renderActionColumn = renderActionColumn
  __impl.mergeRuntimeActions = mergeRuntimeActions
  __impl.normalizeRuntimeAction = normalizeRuntimeAction
  __impl.isStartFlowRuntimeHidden = isStartFlowRuntimeHidden
  __impl.resolveRuntimeObjectCode = resolveRuntimeObjectCode
  __impl.firstNonBlankText = firstNonBlankText
  __impl.sameAction = sameAction
  __impl.isActionDisabled = isActionDisabled
  __impl.isFlowRelatedReadOnly = isFlowRelatedReadOnly
  __impl.renderFlowRelationTags = renderFlowRelationTags
  __impl.actionDisabledReason = actionDisabledReason
  __impl.showActionDisabledMessage = showActionDisabledMessage
  __impl.resolveActionDisplayLabel = resolveActionDisplayLabel
  __impl.resolveActionTextValue = resolveActionTextValue
  __impl.handleCustomActionClick = handleCustomActionClick
  __impl.hasRuntimePermission = hasRuntimePermission
  __impl.matchDisplayCondition = matchDisplayCondition
  __impl.resolveConditionValue = resolveConditionValue
  __impl.handleActionClick = handleActionClick
  __impl.handleConfiguredAction = handleConfiguredAction
  __impl.runBeforeRowAction = runBeforeRowAction
  __impl.handleFormValueUpdate = handleFormValueUpdate
  __impl.isChildRowActionVisible = isChildRowActionVisible
  __impl.isChildRowActionLoading = isChildRowActionLoading
  __impl.handleChildRowAction = handleChildRowAction
  __impl.handleChildToolbarAction = handleChildToolbarAction
  __impl.submitChildToolbarAction = submitChildToolbarAction
  __impl.closeChildToolbarActionModal = closeChildToolbarActionModal
  __impl.handleConfiguredActionSuccess = handleConfiguredActionSuccess
  __impl.startProcessAction = startProcessAction

  return {
    props, emit, __impl, mut, isPrintRuntimeAction, resolveDetailActionIcon, renderActionColumn, mergeRuntimeActions, normalizeRuntimeAction,
    isStartFlowRuntimeHidden, resolveRuntimeObjectCode, firstNonBlankText, sameAction, isActionDisabled, isFlowRelatedReadOnly, renderFlowRelationTags, actionDisabledReason,
    showActionDisabledMessage, resolveActionDisplayLabel, resolveActionTextValue, handleCustomActionClick, hasRuntimePermission, matchDisplayCondition, resolveConditionValue, handleActionClick,
    handleConfiguredAction, runBeforeRowAction, handleFormValueUpdate, isChildRowActionVisible, isChildRowActionLoading, handleChildRowAction, handleChildToolbarAction, submitChildToolbarAction,
    closeChildToolbarActionModal, handleConfiguredActionSuccess, startProcessAction, startFlowAction, resolveDocumentRuntime, resolveMyTask, resubmitFlowAction, handleTaskAction,
    withdrawFlowAction, submitFlowStartWithApprovers, submitFlowStartRequest, refreshCurrentDetailRuntime, callConfiguredApiAction, handleCommandAction, submitCommandAction, closeCommandActionModal,
    executeCommandAction, normalizeCommandActionRuntimeConfig, buildCommandActionInitialData, parseActionConfig, sendConfiguredApiRequest, compactRequestOptions, buildConfiguredApiRequest, normalizeConfiguredApiConfig,
    parseConfiguredApiValue, normalizeConfiguredApiMethod, normalizeConfiguredApiParam, resolveApiParamTarget, resolveActionParamRawValue, resolveObjectPathValue, replaceApiPathParam, setApiObjectValue,
    isEmptyApiParamValue, isFalseLike, confirmConfiguredAction, getActionLoadingKey, isActionLoading, setActionLoading, setFlowActionPageLoading, buildActionTarget,
    resolveActionParamValue, resolveSystemParamValue, resolveActionText, resolveTemplatePlaceholder, resolveButtonType, uniqueMainRecords, isUsableKeyValue, readBoolean,
    resolveRowKeyValue, mergeHookRowWithOriginal, resolveFormDefaultValues, resolveSubmitDefaultParams, handleToolbarOverflowSelect, getColumnKey, isActionColumnConfig, shouldDefaultActionFixedRight,
    collectExpandSlots, normalizeRowActions, findEditSchemaField, resolveColumnCompanionTextField, hasDynamicOptionSourceForColumn, resolveRowCompanionText, snakeToCamelKey, camelToSnakeKey,
    resolveColumnRender, inlineSwitchUpdateKey, isInlineSwitchUpdating, setInlineSwitchUpdating, canInlineSwitchUpdate, renderInlineSwitchColumn, handleInlineSwitchUpdate, splitTableCellValues,
    buildDetailFallbackSchema, toReadonlyField, refreshOfflineFormRuntime, resolveOfflineRecordId, readOfflineRecordVersion, resolveOfflineRecordVersion, offlineDraftPayload, saveOfflineDraft,
    scheduleOfflineDraftSave, flushOfflineDraftSave, resetOfflineDraftSession, restoreOfflineDraft, isBrowserOffline, createOfflineReplayKey, appendOfflineSubmitIntent, confirmOfflineReplay,
    replayOfflineDraft, loadOfflineCurrent, loadOfflinePublishedSnapshot, clearOfflineDraftAfterSubmit, handleBrowserOnline, resolveColumnWidth, callHook, resolveRuntimeConfigKey,
    parseApiConfig, extractApiUrl, normalizeUrlParams, resolveUrlParamValues, stableSerialize, hasFilledSearchParams, getNestedValue, toFiniteNumber,
    extractListRows, extractListTotal, loadList, enrichDocumentRuntimeRows, resolveDefaultRequestSortParams, applySearchTreeExpandedParams, handleSearch, handleReset,
    handleRefresh, handleSearchToggle, handleApplyCustomQuery, handleClearCustomQuery, handleRenderModeChange, handlePageChange, handlePageSizeChange, normalizeEditData,
    isMasterDetailPayload, resolveChildKey, buildInitialChildrenData, normalizeChildrenData, applyDetailData, buildMasterDetailSubmitData, cloneInlineFormValue, buildInlineRecordTabKey,
    findReusableInlineFormTab, activateReusableInlineFormTab, buildInlineFormTabKey, openFormContainer, hydrateInlineFormTab, persistActiveInlineFormTab, markActiveInlineFormClean, handleInlineFormTabChange,
    handleInlineWorkspaceListTab, inlineFormTabTitle, closeInlineFormTab, handleCloseActiveInlineFormTab, handleInlineFormCancel, handleInlineFormSubmitSuccess, handleAdd, handleAddChild,
    resolveTreeParentField, resolveTreeParentValue, isPlainRecord, handleEdit, handleDetail, loadDetail, handleDelete, handleBatchDelete,
    resolveBatchDeleteUrl, fetchDataAuditMetaForDelete, executePlainDelete, performDelete, handleModalConfirm, handleModalCancel, handleModalClose, resetFormOnly,
    openFormOnlyWithRecordInit, resolveFormOnlyRecordInitId, loadRecordForm, loadDetailRuntime, applyApplicationProcessRuntimeSnapshot, applyDocumentRuntimeSnapshot, disposeFlowRuntimeRefresh, queueFlowRuntimeRefresh,
    importModalVisible, hasImportTemplate, exportLoading, exportTaskDrawerVisible, exportTaskLoading, exportTasks, exportTaskDrawerWidth, showExportTaskEntry,
    activeExportTask, exportTaskPaginationConfig, exportTaskColumns, handleShowImport, executeImport, handleImportSuccess, handleDownloadTemplate, handleExport,
    handleOpenExportTasks, clearExportTaskPollTimer, isExportTaskRunning, resolveExportTaskStatusText, resolveExportTaskTagType, runtimeFormulaFields, runtimeFormulaCalculationEnabled, runtimeFormulaSignature,
    scheduleRuntimeFormulaCalculation, refreshRuntimeFormulas, clearRuntimeFormulaTimers, crudRootRef, pageHeightStyle, router, route, userStore,
    slots, searchRef, tableRef, formRef, childFormRef, commandActionFormRef, searchParams, dataSource,
    tableLoading, selectedKeys, expandedRowKeys, customQueryPayload, customQueryFields, activeRenderMode, searchPanelVisible, pagination,
    modalVisible, modalTitle, modalStatus, formData, childFormData, confirmLoading, currentRow, fieldEventLoadToken,
    inlineFormTabs, activeInlineFormTabKey, INLINE_WORKSPACE_LIST_KEY, activeInlineWorkspaceKey, inlineFormHydrating, formOnlySubmitted, detailRuntime, detailRuntimeLoading,
    detailActiveTab, actionLoadingKeys, flowActionPageLoading, flowActionPageLoadingText, flowStartApproverModalVisible, flowStartApproverSubmitting, flowStartApproverNodes, flowStartApproverSelections,
    flowStartApproverLabels, flowStartApproverContext, commandActionModalVisible, commandActionSubmitting, commandActionFormData, commandActionContext, childToolbarActionModalVisible, childToolbarActionSubmitting,
    childToolbarActionContext, childToolbarItemSelected, childToolbarQuantity, offlineFormRuntime, offlineDraftId, offlineBaseRecordVersion, offlineDraftNotice, offlineDraftHydrating,
    offlineReplayLoading, commandActionTitle, commandActionFormSchema, commandActionFormContext, childToolbarActionTitle, childToolbarQuantityLabel, childToolbarItemOptions, childToolbarQuantityMax,
    visibleToolbarActions, visibleDetailActions, visibleFormActions, maxActionButtons, rowKeyFn, tableRowKeyFn, resolvedCustomQueryConfigKey, toolbarOverflowOptions,
    toolbarDropdownOptions, activeSourceColumns, formContext, normalizedExpandConfig, hasExpandConfig, resolvedResizable, inlineSwitchUpdatingMap, tableColumns,
    isEmbeddedTreeTable, resolveEmbeddedTreeConfig, embeddedTreeChildrenField, effectiveShowPagination, effectiveTableProps, collectEmbeddedTreeExpandKeys,
    paginationConfig, searchSlots, tableSlots, formSlots, isDetailMode, resolvedFormOpenMode, usesInlineFormWorkspace, isTabWorkspaceMode,
    inlineWorkspaceVisible, showInlineListPane, showInlineFormWorkspacePane, tiledEditGridCols, activeInlineFormTab, activeInlineFormTitle, inlineFormModeLabel, showInlineFormModeTag,
    resolvedTabWorkspace, activeModalWidth, detailFlowTimelineVisible, detailFlowDiagramVisible, showDetailFlowTabs, dataAuditMeta, showDataChangeLogTab, showDetailExtraTabs,
    dataAuditObjectId, dataAuditRecordId, dataAuditEnabled, dataAuditHistoryAvailable, visibleChildrenConfig, hasChildrenConfig, showDefaultDetailContent, showDefaultDetailChildren,
    normalizedDetailPanelConfig, showDetailPanels, detailPanelRowKeyValue, hasSearchSchema, hasActiveListFilters, resolvedEmptyTitle, resolvedEmptyDescription, normalizedSearchSchema,
    modalFormSchema, offlineReplayAvailable, resolvedFormOnlyTitle, resolvedEditFormClass, DEFAULT_SELECTION_COLUMN_WIDTH, DEFAULT_EXPAND_COLUMN_WIDTH, DEFAULT_ACTION_COLUMN_WIDTH, DEFAULT_DATA_COLUMN_WIDTH,
    TABLE_SCROLL_X_BUFFER, computedScrollX, computedMaxHeight,
  }
}
