/** AiCrudPage setup part 2. */
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
import { flattenOptionNodes, isSameOptionValue } from '../../aiFormItemUtils'
import AiCrudRowExpand from '../../AiCrudRowExpand.vue'
import AiForm from '../../AiForm.vue'

export function applyAiCrudPagePart2(props, emit, deps = {}) {
  const {
    __impl, mut, isPrintRuntimeAction, resolveDetailActionIcon, renderActionColumn, mergeRuntimeActions, normalizeRuntimeAction, isStartFlowRuntimeHidden,
    resolveRuntimeObjectCode, firstNonBlankText, sameAction, isActionDisabled, isFlowRelatedReadOnly, renderFlowRelationTags, actionDisabledReason, showActionDisabledMessage,
    resolveActionDisplayLabel, resolveActionTextValue, handleCustomActionClick, hasRuntimePermission, matchDisplayCondition, resolveConditionValue, handleActionClick, handleConfiguredAction,
    runBeforeRowAction, handleFormValueUpdate, isChildRowActionVisible, isChildRowActionLoading, handleChildRowAction, handleChildToolbarAction, submitChildToolbarAction, closeChildToolbarActionModal,
    handleConfiguredActionSuccess, startProcessAction, loadList, enrichDocumentRuntimeRows, resolveDefaultRequestSortParams, applySearchTreeExpandedParams, handleSearch, handleReset,
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
    paginationConfig, searchSlots, tableSlots, formSlots, isDetailMode, resolvedFormOpenMode, usesInlineFormWorkspace, isTabWorkspaceMode,
    inlineWorkspaceVisible, showInlineListPane, showInlineFormWorkspacePane, tiledEditGridCols, activeInlineFormTab, activeInlineFormTitle, inlineFormModeLabel, showInlineFormModeTag,
    resolvedTabWorkspace, activeModalWidth, detailFlowTimelineVisible, detailFlowDiagramVisible, showDetailFlowTabs, dataAuditMeta, showDataChangeLogTab, showDetailExtraTabs,
    dataAuditObjectId, dataAuditRecordId, dataAuditEnabled, dataAuditHistoryAvailable, visibleChildrenConfig, hasChildrenConfig, showDefaultDetailContent, showDefaultDetailChildren,
    normalizedDetailPanelConfig, showDetailPanels, detailPanelRowKeyValue, hasSearchSchema, hasActiveListFilters, resolvedEmptyTitle, resolvedEmptyDescription, normalizedSearchSchema,
    modalFormSchema, offlineReplayAvailable, resolvedFormOnlyTitle, resolvedEditFormClass, DEFAULT_SELECTION_COLUMN_WIDTH, DEFAULT_EXPAND_COLUMN_WIDTH, DEFAULT_ACTION_COLUMN_WIDTH, DEFAULT_DATA_COLUMN_WIDTH,
    TABLE_SCROLL_X_BUFFER, computedScrollX, computedMaxHeight,
  } = deps

  async function startFlowAction(action, row) {
    const objectCode = resolveRuntimeObjectCode(action, row)
    const recordId = action.recordId || resolveRowKeyValue(row)
    if (!objectCode || !recordId) {
      window.$message.warning('缺少业务对象或记录ID，无法发起主流程')
      return
    }
    const loadingKey = getActionLoadingKey(action, row)
    if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
      window.$message.info('流程正在发起，请稍候')
      return
    }
    const confirmed = await confirmConfiguredAction(
      resolveActionText(action.confirmText || '确定要发起该记录的主流程吗？', row),
      { title: action.label || '发起主流程', positiveText: '发起流程' },
    )
    if (!confirmed)
      return
    setActionLoading(loadingKey, true)
    setFlowActionPageLoading(true, '正在发起流程...')
    try {
      const configRes = await businessFlowStartConfig(objectCode)
      const nodes = Array.isArray(configRes?.data?.initiatorSelectNodes)
        ? configRes.data.initiatorSelectNodes
        : []
      if (configRes?.code === 200 && nodes.length) {
        flowStartApproverNodes.value = nodes
        flowStartApproverSelections.value = {}
        flowStartApproverLabels.value = {}
        flowStartApproverContext.value = { objectCode, recordId, row, loadingKey }
        flowStartApproverModalVisible.value = true
        return
      }
      await submitFlowStartRequest({ objectCode, recordId, row })
    }
    catch (error) {
      window.$message.error(error.message || '发起主流程失败')
    }
    finally {
      setFlowActionPageLoading(false)
      setActionLoading(loadingKey, false)
    }
  }

  function resolveDocumentRuntime(row) {
    return row?._documentRuntime
      || row?.documentRuntime
      || (['detail', 'edit'].includes(modalStatus.value) ? detailRuntime.value : null)
      || null
  }

  function resolveMyTask(row) {
    return resolveDocumentRuntime(row)?.myTask || null
  }

  /**
   * 驳回至发起人后的重新提交。单据字段在普通编辑弹窗里改完并保存，
   * 这里只负责把发起人修改节点的待办办掉，让流程继续往下走。
   */
  async function resubmitFlowAction(action, row) {
    const myTask = resolveMyTask(row)
    if (!myTask?.taskId) {
      window.$message.warning('未找到待处理的修改节点，请刷新后重试')
      return
    }
    const loadingKey = getActionLoadingKey(action, row)
    if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
      window.$message.info('正在提交，请稍候')
      return
    }
    const confirmed = await confirmConfiguredAction(
      resolveActionText(action.confirmText || '确认已修改完成并重新提交审批吗？', row),
      { title: action.label || '修改后重提', positiveText: '重新提交' },
    )
    if (!confirmed)
      return
    setActionLoading(loadingKey, true)
    setFlowActionPageLoading(true, '正在重新提交审批...')
    try {
      const res = await resubmitBusinessDocumentFlow({
        taskId: myTask.taskId,
        taskDefKey: myTask.taskDefKey,
        processInstanceId: myTask.processInstanceId,
        businessKey: resolveDocumentRuntime(row)?.businessKey,
      })
      if (res?.code !== 200)
        throw new Error(res?.message || '重新提交失败')
      applyDocumentRuntimeSnapshot(row, res?.data, resolveRuntimeObjectCode(action, row))
      window.$message.success('已重新提交审批')
      queueFlowRuntimeRefresh(row)
    }
    catch (error) {
      window.$message.error(error?.message || '重新提交失败')
    }
    finally {
      setFlowActionPageLoading(false)
      setActionLoading(loadingKey, false)
    }
  }

  function handleTaskAction(row) {
    const myTask = resolveMyTask(row)
    if (!myTask?.taskId) {
      window.$message.warning('未找到待处理的审批任务，请刷新后重试')
      return
    }
    router.push({ path: '/flow/todo', query: { taskId: myTask.taskId } })
  }

  async function withdrawFlowAction(action, row) {
    const runtime = resolveDocumentRuntime(row)
    const processInstanceId = runtime?.processInstanceId
    if (!processInstanceId) {
      window.$message.warning('当前单据没有可撤回的流程')
      return
    }
    const loadingKey = getActionLoadingKey(action, row)
    if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
      window.$message.info('正在撤回，请稍候')
      return
    }
    const confirmed = await confirmConfiguredAction(
      resolveActionText(action.confirmText || '确定撤回该审批流程吗？撤回后流程状态将变为“已撤回”，仅保留查看审批记录。', row),
      { title: action.label || '撤回流程', positiveText: '确认撤回' },
    )
    if (!confirmed)
      return
    setActionLoading(loadingKey, true)
    setFlowActionPageLoading(true, '正在撤回流程...')
    try {
      const res = await withdrawBusinessDocumentFlow({
        objectCode: resolveRuntimeObjectCode(action, row),
        recordId: action.recordId || resolveRowKeyValue(row),
        processInstanceId,
        businessKey: runtime?.businessKey,
        comment: '申请人撤回',
      })
      if (res?.code !== 200)
        throw new Error(res?.message || '撤回失败')
      applyDocumentRuntimeSnapshot(row, res?.data, resolveRuntimeObjectCode(action, row))
      window.$message.success('流程已撤回')
      queueFlowRuntimeRefresh(row)
    }
    catch (error) {
      window.$message.error(error?.message || '撤回失败')
    }
    finally {
      setFlowActionPageLoading(false)
      setActionLoading(loadingKey, false)
    }
  }

  async function submitFlowStartWithApprovers() {
    const context = flowStartApproverContext.value
    if (!context)
      return
    let selections = {}
    try {
      selections = collectInitiatorSelectSelections(
        flowStartApproverNodes.value,
        flowStartApproverSelections.value,
      )
    }
    catch (error) {
      window.$message.warning(error?.message || '请选择审批人')
      return
    }
    flowStartApproverSubmitting.value = true
    setFlowActionPageLoading(true, '正在发起流程...')
    try {
      if (typeof context.submit === 'function') {
        const res = await context.submit({ PROCESS_START_USER: selections })
        if (res?.code !== 200)
          throw new Error(res?.message || '发起流程失败')
        applyApplicationProcessRuntimeSnapshot(context.row, context.processCode, res?.data)
        window.$message.success('流程已发起')
        queueFlowRuntimeRefresh(context.row)
      }
      else {
        await submitFlowStartRequest({
          objectCode: context.objectCode,
          recordId: context.recordId,
          row: context.row,
          variables: { PROCESS_START_USER: selections },
        })
      }
      flowStartApproverModalVisible.value = false
      flowStartApproverContext.value = null
    }
    catch (error) {
      window.$message.error(error?.message || '发起流程失败')
    }
    finally {
      setFlowActionPageLoading(false)
      flowStartApproverSubmitting.value = false
    }
  }

  async function submitFlowStartRequest({ objectCode, recordId, row, variables = {} }) {
    const res = await request.post('/ai/business/flow/start', {
      objectCode,
      recordId,
      variables,
    })
    if (res?.code !== 200)
      throw new Error(res?.message || '发起流程失败')
    applyDocumentRuntimeSnapshot(row, res?.data, objectCode)
    window.$message.success('流程已发起')
    queueFlowRuntimeRefresh(row)
  }

  async function refreshCurrentDetailRuntime(row = {}) {
    if (!(['detail', 'edit'].includes(modalStatus.value)) || !row)
      return
    const target = { ...row, ...formData.value }
    await loadDetailRuntime(target)
    if (detailRuntime.value) {
      formData.value = {
        ...formData.value,
        _documentRuntime: detailRuntime.value,
        _runtimeActions: detailRuntime.value.runtimeActions || [],
      }
    }
  }

  async function callConfiguredApiAction(action, row) {
    const requestInfo = buildConfiguredApiRequest(action, row)
    if (!requestInfo.url) {
      if (requestInfo.config.capabilityCode) {
        window.$message.info('已触发自定义能力事件')
        return null
      }
      window.$message.warning('未配置接口地址，无法调用 API')
      return null
    }
    const loadingKey = getActionLoadingKey(action, row)
    if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
      window.$message.info('操作正在执行，请稍候')
      return null
    }
    setActionLoading(loadingKey, true)
    try {
      const response = await sendConfiguredApiRequest(requestInfo)
      const successMessage = action.successMessage || requestInfo.config.successMessage || '操作成功'
      if (successMessage)
        window.$message.success(resolveActionText(successMessage, row))
      const successBehavior = action.successBehavior || requestInfo.config.successBehavior
      if (successBehavior) {
        handleConfiguredActionSuccess({ ...action, successBehavior })
      }
      else if (!isFalseLike(requestInfo.config.refreshAfter)) {
        await loadList()
      }
      return response
    }
    catch (error) {
      const failureMessage = action.failureMessage || requestInfo.config.failureMessage || error.message || '操作失败'
      window.$message.error(resolveActionText(failureMessage, row))
      return null
    }
    finally {
      setActionLoading(loadingKey, false)
    }
  }

  async function handleCommandAction(action, row) {
    const config = normalizeCommandActionRuntimeConfig(action)
    const hasInputForm = Array.isArray(config.inputSchema)
      ? buildBusinessActionInputFormSchema(config.inputSchema).length > 0
      : config.formSchema.length > 0
    if (hasInputForm) {
      commandActionContext.value = {
        action,
        row,
        attempt: {
          idempotencyKey: createBusinessActionIdempotencyKey(),
          lastPayloadDigest: '',
        },
      }
      commandActionFormData.value = buildCommandActionInitialData(config, row)
      commandActionModalVisible.value = true
      await nextTick()
      commandActionFormRef.value?.restoreValidation?.()
      return
    }
    await executeCommandAction(action, row, {})
  }

  async function submitCommandAction() {
    const context = commandActionContext.value
    if (!context?.action)
      return
    commandActionSubmitting.value = true
    try {
      await nextTick()
      await commandActionFormRef.value?.validate?.()
      const latestFormData = commandActionFormRef.value?.getFormData?.() || commandActionFormData.value || {}
      const attempt = resolveBusinessActionAttempt(context.attempt, latestFormData)
      context.attempt = attempt
      const success = await executeCommandAction(context.action, context.row, latestFormData, {
        fromModal: true,
        idempotencyKey: attempt.idempotencyKey,
      })
      if (success)
        closeCommandActionModal()
    }
    finally {
      commandActionSubmitting.value = false
    }
  }

  function closeCommandActionModal() {
    if (commandActionSubmitting.value)
      return
    commandActionModalVisible.value = false
    commandActionFormData.value = {}
    commandActionContext.value = null
  }

  async function executeCommandAction(action, row, formData = {}, options = {}) {
    const config = normalizeCommandActionRuntimeConfig(action)
    const objectCode = resolveRuntimeObjectCode(action, row)
    const recordId = action.recordId === undefined || action.recordId === null || action.recordId === ''
      ? resolveRowKeyValue(row)
      : action.recordId
    const actionCode = action.actionCode || action.key
    if (!objectCode || !actionCode) {
      window.$message?.warning('缺少业务对象或动作编码，无法执行')
      return false
    }
    const loadingKey = getActionLoadingKey({ ...action, actionType: 'COMMAND', objectCode, recordId }, row)
    if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
      window.$message?.info(resolveActionTextValue(action.loadingReason, row) || '操作正在执行，请稍候')
      return false
    }
    setActionLoading(loadingKey, true)
    try {
      const response = await executeBusinessAction(buildBusinessActionExecutePayload({
        action: { ...action, actionCode },
        config,
        objectCode,
        recordId,
        formData,
        routeQuery: route.query,
        idempotencyKey: options.idempotencyKey || createBusinessActionIdempotencyKey(),
        parentRecordId: action.childActionContext?.parentRecordId,
        childRecordId: action.childActionContext?.childRecordId,
        relationKey: action.childActionContext?.relationKey,
      }))
      const result = unwrapBusinessActionResult(response, action.failureMessage || '动作执行失败')
      const successMessage = action.successMessage || result.message || config.successMessage || '操作成功'
      if (successMessage)
        window.$message?.success(resolveActionText(successMessage, row))
      const successBehavior = action.successBehavior || config.successBehavior || 'refreshList'
      if (successBehavior && successBehavior !== 'none')
        handleConfiguredActionSuccess({ ...action, successBehavior })
      else if (!options.fromModal)
        await loadList()
      return true
    }
    catch (error) {
      const failureMessage = action.failureMessage || config.failureMessage || error?.message || '动作执行失败'
      window.$message?.error(resolveActionText(failureMessage, row))
      return false
    }
    finally {
      setActionLoading(loadingKey, false)
    }
  }

  function normalizeCommandActionRuntimeConfig(action = {}) {
    const config = parseActionConfig(action.actionConfig)
    return {
      ...config,
      inputSchema: Array.isArray(config.inputSchema) ? config.inputSchema : undefined,
      formSchema: Array.isArray(config.formSchema) ? config.formSchema : [],
      steps: Array.isArray(config.steps) ? config.steps : [],
      successBehavior: action.successBehavior || config.successBehavior || 'refreshList',
      successMessage: action.successMessage || config.successMessage || '',
      failureMessage: action.failureMessage || config.failureMessage || '',
    }
  }

  function buildCommandActionInitialData(config = {}, row = {}) {
    if (Array.isArray(config.inputSchema))
      return buildBusinessActionInitialData(config, row)
    const defaults = {}
    const defaultValues = config.defaultValues && typeof config.defaultValues === 'object' ? config.defaultValues : {}
    Object.entries(defaultValues).forEach(([key, value]) => {
      defaults[key] = typeof value === 'string' && value.startsWith('row.')
        ? resolveConditionValue(row, value.slice(4))
        : value
    })
    return defaults
  }

  function parseActionConfig(config) {
    if (!config)
      return {}
    if (typeof config === 'string') {
      try {
        return JSON.parse(config) || {}
      }
      catch {
        return {}
      }
    }
    return typeof config === 'object' ? { ...config } : {}
  }

  async function sendConfiguredApiRequest(requestInfo) {
    const { method, url, params, data, headers } = requestInfo
    if (method === 'postEncrypt')
      return postEncrypt(url, data, compactRequestOptions({ params, headers }))
    const requestConfig = {
      method,
      url,
      ...compactRequestOptions({ params, headers }),
    }
    if (method !== 'get' || Object.keys(data).length)
      requestConfig.data = data
    return request(requestConfig)
  }

  function compactRequestOptions(options = {}) {
    const result = {}
    Object.entries(options).forEach(([key, value]) => {
      if (value && typeof value === 'object' && !Array.isArray(value) && !Object.keys(value).length)
        return
      if (value === undefined || value === null)
        return
      result[key] = value
    })
    return result
  }

  function buildConfiguredApiRequest(action = {}, row = {}) {
    const config = normalizeConfiguredApiConfig(action)
    const urlTemplate = config.url
    let url = resolveActionText(urlTemplate, row)
    const method = normalizeConfiguredApiMethod(config.method)
    const params = {}
    const data = {}
    const headers = {}
    config.params.forEach((param) => {
      const name = String(param?.name || '').trim()
      if (!name)
        return
      const value = resolveActionParamRawValue(param, row)
      if (isEmptyApiParamValue(value))
        return
      const target = resolveApiParamTarget(param, method, urlTemplate)
      if (target === 'path') {
        url = replaceApiPathParam(url, name, value)
      }
      else if (target === 'header') {
        headers[name] = value
      }
      else if (target === 'query') {
        setApiObjectValue(params, name, value)
      }
      else {
        setApiObjectValue(data, name, value)
      }
    })
    return {
      method,
      url,
      params,
      data,
      headers,
      config,
    }
  }

  function normalizeConfiguredApiConfig(action = {}) {
    const actionConfig = action.actionConfig && typeof action.actionConfig === 'object' ? action.actionConfig : {}
    const parsed = parseConfiguredApiValue(
      actionConfig.apiConfigValue
      || actionConfig.api
      || actionConfig.request
      || action.apiConfigValue
      || action.api,
    )
    const params = Array.isArray(actionConfig.params) && actionConfig.params.length
      ? actionConfig.params
      : Array.isArray(actionConfig.paramMappings) && actionConfig.paramMappings.length
        ? actionConfig.paramMappings
        : Array.isArray(action.params)
          ? action.params
          : []
    return {
      ...actionConfig,
      method: normalizeConfiguredApiMethod(
        actionConfig.method
        || actionConfig.reqMethod
        || actionConfig.apiMethod
        || action.method
        || parsed.method
        || 'post',
      ),
      url: String(
        actionConfig.url
        || actionConfig.apiUrl
        || actionConfig.urlPath
        || actionConfig.path
        || action.url
        || action.apiUrl
        || parsed.url
        || action.routePath
        || '',
      ).trim(),
      capabilityCode: String(actionConfig.capabilityCode || action.capabilityCode || '').trim(),
      params: params.map(normalizeConfiguredApiParam).filter(Boolean),
    }
  }

  function parseConfiguredApiValue(value) {
    const text = String(value || '').trim()
    if (!text)
      return {}
    const parts = text.split('@')
    if (parts.length > 1 && /^[A-Z_]+$/i.test(parts[0]))
      return { method: normalizeConfiguredApiMethod(parts[0]), url: parts.slice(1).join('@') }
    return { url: text }
  }

  function normalizeConfiguredApiMethod(value) {
    const method = String(value || 'post')
      .replace('-', '_')
      .toLowerCase()
    if (['postencrypt', 'post_encrypt'].includes(method))
      return 'postEncrypt'
    if (['get', 'post', 'put', 'delete', 'patch'].includes(method))
      return method
    return 'post'
  }

  function normalizeConfiguredApiParam(param = {}) {
    if (!param || typeof param !== 'object')
      return null
    const sourceType = ['rowField', 'routeQuery', 'static', 'system'].includes(param.sourceType) ? param.sourceType : 'static'
    const target = ['path', 'query', 'body', 'header'].includes(param.target) ? param.target : ''
    return {
      name: String(param.name || '').trim(),
      target,
      sourceType,
      sourceField: String(param.sourceField || '').trim(),
      value: param.value,
    }
  }

  function resolveApiParamTarget(param = {}, method = 'post', urlTemplate = '') {
    if (param.target)
      return param.target
    const name = String(param.name || '').trim()
    if (name && (String(urlTemplate).includes(`:${name}`) || String(urlTemplate).includes(`{${name}}`)))
      return 'path'
    return method === 'get' ? 'query' : 'body'
  }

  function resolveActionParamRawValue(param = {}, row = {}) {
    const sourceType = param.sourceType || 'static'
    const sourceField = String(param.sourceField || '').trim()
    if (sourceType === 'rowField' && sourceField)
      return resolveObjectPathValue(row, sourceField)
    if (sourceType === 'routeQuery' && sourceField)
      return route.query?.[sourceField] ?? ''
    if (sourceType === 'system' && sourceField)
      return resolveSystemParamValue(sourceField)
    return resolveActionText(param.value, row)
  }

  function resolveObjectPathValue(source = {}, path = '') {
    const directValue = source?.[path]
    if (directValue !== undefined)
      return directValue
    return String(path || '').split('.').filter(Boolean).reduce((value, key) => value?.[key], source)
  }

  function replaceApiPathParam(url, name, value) {
    const encoded = encodeURIComponent(Array.isArray(value) ? value.join(',') : String(value))
    return String(url || '')
      .replaceAll(`:${name}`, encoded)
      .replaceAll(`{${name}}`, encoded)
  }

  function setApiObjectValue(target, name, value) {
    const keys = String(name || '').split('.').filter(Boolean)
    if (keys.length <= 1) {
      target[name] = value
      return
    }
    let cursor = target
    keys.forEach((key, index) => {
      if (index === keys.length - 1) {
        cursor[key] = value
        return
      }
      if (!cursor[key] || typeof cursor[key] !== 'object' || Array.isArray(cursor[key]))
        cursor[key] = {}
      cursor = cursor[key]
    })
  }

  function isEmptyApiParamValue(value) {
    if (value === null || value === undefined)
      return true
    if (Array.isArray(value))
      return value.length === 0
    return typeof value === 'string' && value === ''
  }

  function isFalseLike(value) {
    return value === false || value === 0 || String(value).toLowerCase() === 'false'
  }

  function confirmConfiguredAction(message, options = {}) {
    if (!window.$dialog?.warning) {
      const nativeConfirm = globalThis?.confirm
      return Promise.resolve(typeof nativeConfirm !== 'function' || nativeConfirm(message))
    }
    return new Promise((resolve) => {
      window.$dialog.warning({
        title: options.title || '确认操作',
        content: message,
        positiveText: options.positiveText || '确定',
        negativeText: '取消',
        onPositiveClick: () => resolve(true),
        onNegativeClick: () => resolve(false),
        onClose: () => resolve(false),
        onMaskClick: () => resolve(false),
      })
    })
  }

  function getActionLoadingKey(action, row) {
    const actionType = String(action?.actionType || action?.key || '').toUpperCase()
    const actionKey = action?.key || action?.actionCode || action?.label || ''
    const objectCode = resolveRuntimeObjectCode(action, row)
    const recordId = action?.recordId || resolveRowKeyValue(row) || ''
    return `${actionType}:${actionKey}:${objectCode}:${recordId}`
  }

  function isActionLoading(action, row) {
    const internalLoading = actionLoadingKeys.value.has(getActionLoadingKey(action, row))
    if (typeof action?.loading === 'function')
      return internalLoading || !!action.loading(row)
    return internalLoading || !!action?.loading
  }

  function setActionLoading(key, loading) {
    if (!key)
      return
    const next = new Set(actionLoadingKeys.value)
    if (loading)
      next.add(key)
    else
      next.delete(key)
    actionLoadingKeys.value = next
  }

  function setFlowActionPageLoading(loading, text = '') {
    flowActionPageLoading.value = loading
    flowActionPageLoadingText.value = loading ? (text || '正在处理，请稍候...') : ''
  }

  function buildActionTarget(action, row) {
    let target = resolveActionText(action.routePath, row)
    const params = Array.isArray(action.params) ? action.params : []
    const query = new URLSearchParams()
    if (action.targetFormKey)
      query.set('formKey', action.targetFormKey)
    params.forEach((param) => {
      const name = String(param?.name || '').trim()
      if (!name)
        return
      const value = resolveActionParamValue(param, row)
      if (value !== '')
        query.append(name, value)
    })
    const queryString = query.toString()
    if (!queryString)
      return target
    target += target.includes('?') ? '&' : '?'
    return `${target}${queryString}`
  }

  function resolveActionParamValue(param = {}, row = {}) {
    const sourceType = param.sourceType || 'static'
    const sourceField = String(param.sourceField || '').trim()
    if (sourceType === 'rowField' && sourceField)
      return row?.[sourceField] ?? ''
    if (sourceType === 'routeQuery' && sourceField)
      return route.query?.[sourceField] ?? ''
    if (sourceType === 'system' && sourceField)
      return resolveSystemParamValue(sourceField)
    return resolveActionText(param.value, row)
  }

  function resolveSystemParamValue(sourceField = '') {
    if (sourceField === 'now')
      return new Date().toISOString()
    if (sourceField === 'today')
      return new Date().toISOString().slice(0, 10)
    if (sourceField === 'userId')
      return userStore.userId || ''
    if (sourceField === 'tenantId')
      return userStore.userInfo?.tenantId || route.query?.tenantId || ''
    if (sourceField === 'selectedIds')
      return [...selectedKeys.value]
    return ''
  }

  function resolveActionText(template, row) {
    let text = String(template || '')
    const data = row || {}
    Object.keys(data).forEach((key) => {
      const value = data[key]
      if (!isUsableKeyValue(value))
        return
      text = text.replaceAll(`:${key}`, value)
      text = text.replaceAll(`\${${key}}`, value)
    })
    const idValue = resolveRowKeyValue(data)
    if (isUsableKeyValue(idValue))
      text = text.replaceAll(':id', idValue)
    Object.keys(route.query || {}).forEach((key) => {
      const value = route.query[key]
      if (!isUsableKeyValue(value))
        return
      text = text.replaceAll(resolveTemplatePlaceholder(`route.${key}`), Array.isArray(value) ? value[0] : value)
    })
    text = text
      .replaceAll(resolveTemplatePlaceholder('system.now'), new Date().toISOString())
      .replaceAll(resolveTemplatePlaceholder('system.today'), new Date().toISOString().slice(0, 10))
    return text
  }

  function resolveTemplatePlaceholder(name = '') {
    return ['$', '{', name, '}'].join('')
  }

  function resolveButtonType(action) {
    return action?.type && action.type !== 'default' ? action.type : undefined
  }

  function uniqueMainRecords(rows = []) {
    const seen = new Set()
    const uniqueRows = []
    const uniqueKeys = []
    for (const row of rows) {
      const key = resolveRowKeyValue(row)
      if (!isUsableKeyValue(key))
        continue
      const text = String(key)
      if (seen.has(text))
        continue
      seen.add(text)
      uniqueRows.push(row)
      uniqueKeys.push(key)
    }
    return { rows: uniqueRows, keys: uniqueKeys }
  }

  function isUsableKeyValue(value) {
    if (value === null || value === undefined)
      return false
    const textValue = String(value).trim()
    return textValue !== '' && textValue !== 'undefined' && textValue !== 'null'
  }

  function readBoolean(value, defaultValue = false) {
    if (value === null || value === undefined)
      return defaultValue
    if (typeof value === 'boolean')
      return value
    if (typeof value === 'number')
      return value !== 0
    return ['true', '1', 'yes'].includes(String(value).trim().toLowerCase())
  }

  function resolveRowKeyValue(row) {
    if (!row || typeof row !== 'object')
      return undefined

    const values = []
    try {
      values.push(rowKeyFn.value(row))
    }
    catch (error) {
      console.warn('[AiCrudPage] rowKey解析失败:', error)
    }

    if (typeof props.rowKey === 'string')
      values.push(row[props.rowKey])
    values.push(row.id, row.Id)

    return values.find(isUsableKeyValue)
  }

  function mergeHookRowWithOriginal(row, processedRow) {
    if (!processedRow || typeof processedRow !== 'object')
      return row
    const merged = row && typeof row === 'object'
      ? { ...row, ...processedRow }
      : { ...processedRow }
    const originalKey = resolveRowKeyValue(row)
    if (isUsableKeyValue(originalKey) && !isUsableKeyValue(resolveRowKeyValue(merged))) {
      const key = typeof props.rowKey === 'string' ? props.rowKey : 'id'
      merged[key] = originalKey
      if (!isUsableKeyValue(merged.id))
        merged.id = originalKey
    }
    return merged
  }

  function resolveFormDefaultValues() {
    return isPlainRecord(props.formDefaultValues) ? props.formDefaultValues : {}
  }

  function resolveSubmitDefaultParams() {
    return isPlainRecord(props.submitDefaultParams) ? props.submitDefaultParams : {}
  }

  function handleToolbarOverflowSelect(key) {
    if (key === 'batch-delete') {
      handleBatchDelete()
      return
    }
    if (key === 'import') {
      handleShowImport()
      return
    }
    if (key === 'export') {
      handleExport()
      return
    }
    if (key === 'export-tasks') {
      handleOpenExportTasks()
    }
  }

  function getColumnKey(col) {
    return String(col?.prop || col?.key || col?.dataIndex || '').trim()
  }

  function isActionColumnConfig(col) {
    const key = getColumnKey(col)
    const title = String(col?.label || col?.title || '').trim()
    return ['action', 'actions', 'operation', 'operations'].includes(key) || title === '操作'
  }

  function shouldDefaultActionFixedRight(col) {
    return col.fixed === undefined || col.fixed === null || col.fixed === ''
  }

  function collectExpandSlots() {
    return Object.keys(slots).reduce((result, name) => {
      if (name.startsWith('expand-'))
        result[name] = slotProps => slots[name]?.(slotProps)
      return result
    }, {})
  }

  function normalizeRowActions(actions = []) {
    const next = Array.isArray(actions) ? [...actions] : []
    if (!props.enableTreeAddChild)
      return next.filter(action => action?.key !== 'addChild')
    if (next.some(action => action?.key === 'addChild'))
      return next
    const editIndex = next.findIndex(action => action?.key === 'edit')
    const addChildAction = { label: '添加下级', key: 'addChild', type: 'success' }
    if (editIndex >= 0) {
      next.splice(editIndex + 1, 0, addChildAction)
      return next
    }
    next.unshift(addChildAction)
    return next
  }

  function findEditSchemaField(fieldKey) {
    if (!fieldKey)
      return null
    return flattenRuntimeFormFields(props.editSchema || []).find(field => field?.field === fieldKey) || null
  }

  /** 列表列伴随名称字段：renderConfig / labelValueField / 动态 optionSource 的 xxxName */
  function resolveColumnCompanionTextField(col = {}, editField = null, key = '') {
    const fromRenderConfig = col?.renderConfig?.textField || col?.renderConfig?.targetField
    if (fromRenderConfig && fromRenderConfig !== key)
      return String(fromRenderConfig)
    const fromRender = col?.render && typeof col.render === 'object'
      ? (col.render.targetField || col.render.textField)
      : ''
    if (fromRender && fromRender !== key)
      return String(fromRender)
    const explicit = editField?.props?.labelValueField || editField?.labelValueField
    if (explicit && explicit !== key)
      return String(explicit)
    const source = editField?.optionSource || editField?.props?.optionSource
    if (key && hasDynamicOptionSourceForColumn(source))
      return `${key}Name`
    // 静态选项走 options 映射，不再误读空的 xxxName
    if (resolveStaticOptionsFromField(editField).length)
      return ''
    // 普通 select 也可能冗余了 xxxName（历史发布缺 optionSource 元数据时仍尽量回显名称）
    const fieldType = String(editField?.type || editField?.componentType || editField?.componentKey || '').trim()
    if (key && ['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'cascader', 'treeSelect'].includes(fieldType))
      return `${key}Name`
    return ''
  }

  function hasDynamicOptionSourceForColumn(source) {
    if (!source || typeof source !== 'object')
      return false
    const type = String(source.type || '').trim().toUpperCase().replace(/-/g, '_')
    if (!type || type === 'STATIC')
      return false
    return ['QUERY_SOURCE', 'BUSINESS_OBJECT', 'REMOTE', 'API', 'DICT', 'CURRENT_CHILDREN'].includes(type)
      || Boolean(source.api || source.url || source.sourceKey)
  }

  /** 编辑 schema 上的静态选项（非动态 optionSource） */
  function resolveStaticOptionsFromField(editField = null) {
    if (!editField || typeof editField !== 'object')
      return []
    const source = editField.optionSource || editField.props?.optionSource
    if (hasDynamicOptionSourceForColumn(source))
      return []
    if (Array.isArray(editField.options) && editField.options.length)
      return editField.options
    if (Array.isArray(editField.props?.options) && editField.props.options.length)
      return editField.props.options
    return []
  }

  function mapValueToStaticOptionLabels(rawValue, options = []) {
    if (!Array.isArray(options) || !options.length)
      return null
    if (rawValue === null || rawValue === undefined || rawValue === '')
      return null
    const values = Array.isArray(rawValue)
      ? rawValue
      : (typeof rawValue === 'string' && rawValue.includes(',')
          ? rawValue.split(',').map(item => item.trim()).filter(item => item !== '')
          : [rawValue])
    if (!values.length)
      return null
    const flat = flattenOptionNodes(options)
    return values.map((value) => {
      const matched = flat.find(option => isSameOptionValue(option?.value ?? option?.key, value))
      return matched?.label ?? value
    })
  }

  function resolveRowCompanionText(row = {}, textField = '', valueField = '') {
    if (!row || typeof row !== 'object')
      return row?.[valueField]
    if (textField && row[textField] !== undefined && row[textField] !== null && String(row[textField]).trim() !== '')
      return row[textField]
    // snake_case 列名与 camelCase 读模型并存时兜底
    if (textField) {
      const camel = snakeToCamelKey(textField)
      if (camel && camel !== textField && row[camel] !== undefined && row[camel] !== null && String(row[camel]).trim() !== '')
        return row[camel]
      const snake = camelToSnakeKey(textField)
      if (snake && snake !== textField && row[snake] !== undefined && row[snake] !== null && String(row[snake]).trim() !== '')
        return row[snake]
    }
    return row[valueField]
  }

  function snakeToCamelKey(value = '') {
    return String(value || '').replace(/_([a-zA-Z0-9])/g, (_, ch) => String(ch).toUpperCase())
  }

  function camelToSnakeKey(value = '') {
    return String(value || '').replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase()
  }

  function resolveColumnRender(col) {
    const nextCol = { ...col }
    if (typeof col.render === 'function')
      return nextCol

    const key = col.prop || col.key || col.dataIndex
    const editField = findEditSchemaField(key)
    if (isSwitchColumnConfig(col, editField)) {
      if (!nextCol.width && !nextCol.minWidth)
        nextCol.width = 100
      if (!nextCol.align)
        nextCol.align = 'center'
      nextCol.render = row => renderInlineSwitchColumn(row, key, col, editField)
      return nextCol
    }

    // 动态下拉等把名称冗余到 xxxName / renderConfig.textField；列表优先显示名称
    const companionTextField = resolveColumnCompanionTextField(col, editField, key)
    const staticOptions = resolveStaticOptionsFromField(editField)

    if (!col.render || typeof col.render !== 'object') {
      if (staticOptions.length) {
        nextCol.render = (row) => {
          const labels = mapValueToStaticOptionLabels(row?.[key], staticOptions)
          return h(SystemTableCell, {
            values: splitTableCellValues(
              labels != null
                ? labels
                : resolveRowCompanionText(row, companionTextField, key),
            ),
          })
        }
        return nextCol
      }
      if (companionTextField) {
        nextCol.render = row => h(SystemTableCell, {
          values: splitTableCellValues(resolveRowCompanionText(row, companionTextField, key)),
        })
      }
      return nextCol
    }

    const renderType = col.render.type
    if (renderType === 'staticOptions') {
      const options = Array.isArray(col.render.options) && col.render.options.length
        ? col.render.options
        : staticOptions
      nextCol.render = (row) => {
        const labels = mapValueToStaticOptionLabels(row?.[key], options)
        return h(SystemTableCell, {
          values: splitTableCellValues(
            labels != null
              ? labels
              : resolveRowCompanionText(row, companionTextField, key),
          ),
        })
      }
    }
    else if (renderType === 'dictTag') {
      nextCol.render = row => h(DictTag, {
        dictType: col.render.dictType,
        value: row[key],
        size: 'small',
      })
    }
    else if (renderType === 'relationName') {
      const targetField = col.render.targetField || companionTextField || `${key}Name`
      nextCol.render = row => h(SystemTableCell, {
        values: splitTableCellValues(resolveRowCompanionText(row, targetField, key)),
      })
    }
    else if (renderType === 'orgName' || renderType === 'userName' || renderType === 'regionName') {
      const targetField = col.render.targetField || companionTextField || `${key}Name`
      nextCol.render = row => h(SystemTableCell, {
        values: splitTableCellValues(resolveRowCompanionText(row, targetField, key)),
      })
    }
    else if (renderType === 'imageUpload') {
      nextCol.render = (row) => {
        const items = resolveFileRenderItems(row[key], row[col.render.targetField || `${key}Name`])
        if (items.length === 0)
          return '-'
        return h('div', { style: 'display: flex; gap: 4px; flex-wrap: wrap;' }, items.map(item => h(AuthImage, {
          src: item.value,
          alt: item.name,
          title: item.name,
          imgClass: 'ai-crud-file-image',
          preview: true,
          style: 'width: 32px; height: 32px;',
          imgStyle: 'width: 100%; height: 100%; border-radius: 4px; object-fit: cover;',
        })))
      }
    }
    else if (renderType === 'fileUpload') {
      nextCol.render = (row) => {
        const items = resolveFileRenderItems(row[key], row[col.render.targetField || `${key}Name`])
        if (items.length === 0)
          return '-'
        return h('div', { style: 'display: flex; gap: 6px; align-items: center; flex-wrap: wrap;' }, items.map((item) => {
          if (isImageFileName(item.name)) {
            return h(AuthImage, {
              src: item.value,
              alt: item.name,
              title: item.name,
              imgClass: 'ai-crud-file-image',
              preview: true,
              style: 'width: 32px; height: 32px;',
              imgStyle: 'width: 100%; height: 100%; border-radius: 4px; object-fit: cover;',
            })
          }
          return h('span', { title: item.name }, item.name)
        }))
      }
    }
    return nextCol
  }

  function inlineSwitchUpdateKey(row, fieldKey) {
    return `${resolveRowKeyValue(row)}::${fieldKey}`
  }

  function isInlineSwitchUpdating(row, fieldKey) {
    return !!inlineSwitchUpdatingMap.value[inlineSwitchUpdateKey(row, fieldKey)]
  }

  function setInlineSwitchUpdating(row, fieldKey, updating) {
    const key = inlineSwitchUpdateKey(row, fieldKey)
    const next = { ...inlineSwitchUpdatingMap.value }
    if (updating)
      next[key] = true
    else
      delete next[key]
    inlineSwitchUpdatingMap.value = next
  }

  function canInlineSwitchUpdate(row) {
    if (props.formOnly)
      return false
    if (row?._dataScopeAccess === 'RELATED')
      return false
    return !!(props.apiConfig?.update || props.api)
  }

  function renderInlineSwitchColumn(row, fieldKey, col, editField) {
    const pair = resolveSwitchColumnValuePair(col, editField)
    const texts = resolveSwitchColumnTexts(col, editField)
    const currentValue = normalizeSwitchCellValue(row?.[fieldKey], pair.checkedValue, pair.uncheckedValue)
    const updating = isInlineSwitchUpdating(row, fieldKey)
    const readonlyField = editField?.readonly === true || editField?.disabled === true
    const interactive = canInlineSwitchUpdate(row) && !readonlyField
    const children = {}
    if (texts.checkedText)
      children.checked = () => texts.checkedText
    if (texts.uncheckedText)
      children.unchecked = () => texts.uncheckedText
    return h(NSwitch, {
      value: currentValue,
      size: 'small',
      checkedValue: pair.checkedValue,
      uncheckedValue: pair.uncheckedValue,
      loading: updating,
      disabled: !interactive || updating,
      ariaLabel: String(col.label || col.title || fieldKey || '开关'),
      onUpdateValue: value => handleInlineSwitchUpdate(row, fieldKey, value, currentValue),
    }, children)
  }

  async function handleInlineSwitchUpdate(row, fieldKey, nextValue, previousValue) {
    if (!canInlineSwitchUpdate(row) || isInlineSwitchUpdating(row, fieldKey))
      return
    const idValue = resolveRowKeyValue(row)
    if (!isUsableKeyValue(idValue)) {
      window.$message.warning(`缺少${props.rowKey}参数，无法更新`)
      return
    }
    const rowKey = typeof props.rowKey === 'string' && props.rowKey ? props.rowKey : 'id'
    const payload = {
      id: idValue,
      [rowKey]: idValue,
      [fieldKey]: nextValue,
    }
    setInlineSwitchUpdating(row, fieldKey, true)
    const previous = row[fieldKey]
    row[fieldKey] = nextValue
    try {
      const { method, url } = parseApiConfig(
        'update',
        `${props.api}/${idValue}`,
        'put',
        { id: idValue },
      )
      let requestMethod = method
      const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
      if (useEncrypt)
        requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
      else
        requestMethod = method.toLowerCase()

      if (useEncrypt && requestMethod === 'postEncrypt')
        await postEncrypt(url, payload)
      else
        await request({ method: requestMethod, url, data: payload })

      window.$message.success('更新成功')
    }
    catch (error) {
      row[fieldKey] = previous === undefined ? previousValue : previous
      console.error('[AiCrudPage] inline switch update failed', error)
      window.$message.error(error?.message || '更新失败')
    }
    finally {
      setInlineSwitchUpdating(row, fieldKey, false)
    }
  }

  function splitTableCellValues(value) {
    const rawValues = Array.isArray(value) ? value : [value]
    return rawValues
      .flatMap(item => String(item || '').split(/[、,，]/))
      .map(item => item.trim())
      .filter(Boolean)
  }

  function buildDetailFallbackSchema(columns = []) {
    const skippedKeys = new Set(['action', 'actions', 'operation', 'operations', 'selection', 'expand'])
    return (Array.isArray(columns) ? columns : [])
      .filter((column) => {
        const field = column?.prop || column?.key || column?.dataIndex
        return field && !skippedKeys.has(String(field)) && column?.visible !== false
      })
      .slice(0, 12)
      .map(column => ({
        field: column.prop || column.key || column.dataIndex,
        label: column.label || column.title || column.prop || column.key,
        type: 'input',
        disabled: true,
        readonly: true,
        props: {
          disabled: true,
          readonly: true,
        },
      }))
  }

  function toReadonlyField(field) {
    if (!field || field.type === 'divider')
      return field
    if (field.nodeType && field.nodeType !== 'field') {
      return {
        ...field,
        children: Array.isArray(field.children) ? field.children.map(toReadonlyField) : field.children,
      }
    }
    return {
      ...field,
      disabled: true,
      readonly: true,
      props: {
        ...(field.props || {}),
        disabled: true,
        readonly: true,
      },
    }
  }

  function refreshOfflineFormRuntime() {
    if (mut.offlineDraftSaveTimer) {
      window.clearTimeout(mut.offlineDraftSaveTimer)
      mut.offlineDraftSaveTimer = null
    }
    offlineDraftId.value = ''
    offlineBaseRecordVersion.value = ''
    offlineDraftNotice.value = ''
    try {
      offlineFormRuntime.value = createOfflineFormRuntime({
        config: props.offlineDraft,
        scope: {
          tenantId: userStore.tenantId,
          userId: userStore.userId,
        },
      })
    }
    catch (error) {
      offlineFormRuntime.value = null
      if (props.offlineDraft?.enabled === true)
        console.warn('[AiCrudPage] 离线草稿配置无效，已失败关闭:', error?.message || error)
    }
  }

  function resolveOfflineRecordId(row = currentRow.value) {
    if (!row || modalStatus.value !== 'edit')
      return ''
    return resolveRowKeyValue(row) ?? ''
  }

  function readOfflineRecordVersion(row = currentRow.value) {
    const field = offlineFormRuntime.value?.config.recordVersionField
      || props.offlineDraft?.recordVersionField
      || 'updateTime'
    const mainData = isMasterDetailPayload(formData.value) ? formData.value.main : formData.value
    return mainData?.[field]
      ?? mainData?.version
      ?? mainData?.recordVersion
      ?? row?.[field]
      ?? row?.version
      ?? row?.recordVersion
      ?? ''
  }

  function resolveOfflineRecordVersion() {
    return offlineBaseRecordVersion.value || readOfflineRecordVersion()
  }

  function offlineDraftPayload(data = formData.value) {
    if (hasChildrenConfig.value) {
      return {
        main: data && typeof data.main === 'object' ? data.main : { ...(formData.value || {}) },
        children: normalizeChildrenData(data?.children || childFormData.value),
      }
    }
    return data && typeof data === 'object' ? data : {}
  }

  function saveOfflineDraft(data = formData.value) {
    const runtime = offlineFormRuntime.value
    if (!runtime || !['add', 'edit'].includes(modalStatus.value) || offlineDraftHydrating.value || inlineFormHydrating.value)
      return null
    try {
      const draft = runtime.save({
        draftId: offlineDraftId.value || undefined,
        data: offlineDraftPayload(data),
        recordId: resolveOfflineRecordId(),
        baseRecordVersion: resolveOfflineRecordVersion(),
      })
      offlineDraftId.value = draft.draftId
      return draft
    }
    catch (error) {
      offlineDraftNotice.value = error?.message || '本地草稿保存失败'
      console.warn('[AiCrudPage] 本地草稿保存失败:', error?.code || error?.message || error)
      return null
    }
  }

  function scheduleOfflineDraftSave() {
    if (!offlineFormRuntime.value
      || !['add', 'edit'].includes(modalStatus.value)
      || offlineDraftHydrating.value
      || inlineFormHydrating.value) {
      return
    }
    if (mut.offlineDraftSaveTimer) {
      window.clearTimeout(mut.offlineDraftSaveTimer)
    }
    mut.offlineDraftSaveTimer = window.setTimeout(() => {
      mut.offlineDraftSaveTimer = null
      saveOfflineDraft()
    }, 300)
  }

  function flushOfflineDraftSave() {
    const shouldSave = Boolean(mut.offlineDraftSaveTimer || offlineDraftId.value)
    if (mut.offlineDraftSaveTimer) {
      window.clearTimeout(mut.offlineDraftSaveTimer)
      mut.offlineDraftSaveTimer = null
    }
    return shouldSave ? saveOfflineDraft() : null
  }

  function resetOfflineDraftSession() {
    if (mut.offlineDraftSaveTimer) {
      window.clearTimeout(mut.offlineDraftSaveTimer)
      mut.offlineDraftSaveTimer = null
    }
    offlineDraftId.value = ''
    offlineBaseRecordVersion.value = ''
    offlineDraftNotice.value = ''
  }

  async function restoreOfflineDraft(recordId = '') {
    const runtime = offlineFormRuntime.value
    if (!runtime)
      return null
    const draft = runtime.findDraft(recordId)
    if (!draft)
      return null
    offlineDraftId.value = draft.draftId
    offlineBaseRecordVersion.value = draft.baseRecordVersion || readOfflineRecordVersion()
    offlineDraftHydrating.value = true
    try {
      if (hasChildrenConfig.value && draft.data && typeof draft.data.main === 'object') {
        formData.value = normalizeEditData({ ...(formData.value || {}), ...draft.data.main })
        childFormData.value = normalizeChildrenData(draft.data.children)
      }
      else if (draft.data && typeof draft.data === 'object') {
        formData.value = normalizeEditData({ ...(formData.value || {}), ...draft.data })
      }
      offlineDraftNotice.value = draft.replayLog?.some(item => item.status !== 'COMPLETED')
        ? '已恢复待提交草稿，联网后可检查并重放。'
        : '已恢复本地草稿，提交前请确认数据。'
      return draft
    }
    finally {
      await nextTick()
      offlineDraftHydrating.value = false
    }
  }

  function isBrowserOffline() {
    return typeof navigator !== 'undefined' && navigator.onLine === false
  }

  function createOfflineReplayKey() {
    return `ui:offline:${Date.now().toString(36)}:${Math.random().toString(36).slice(2, 10)}`
  }

  function appendOfflineSubmitIntent(draft, data, isEdit, idValue) {
    const runtime = offlineFormRuntime.value
    if (!runtime?.config.replayActionCode || !draft)
      return null
    const sourceData = hasChildrenConfig.value ? (data?.main || {}) : data
    const intent = runtime.appendSubmitIntent(draft.draftId, {
      formData: sourceData,
      recordId: isEdit ? idValue : '',
      idempotencyKey: createOfflineReplayKey(),
      routeQuery: route.query,
    })
    if (intent)
      offlineDraftNotice.value = '当前网络不可用，已保存本地草稿和待提交意图。联网后请检查并重放。'
    return intent
  }

  function confirmOfflineReplay() {
    if (!offlineDraftId.value || offlineReplayLoading.value)
      return
    window.$dialog?.warning({
      title: '检查并重放本地草稿',
      content: '系统会先检查发布版本和记录版本，确认无冲突后按幂等键提交。是否继续？',
      positiveText: '继续检查',
      negativeText: '取消',
      onPositiveClick: replayOfflineDraft,
    })
  }

  async function replayOfflineDraft() {
    const runtime = offlineFormRuntime.value
    const draftId = offlineDraftId.value
    if (!runtime || !draftId)
      return
    offlineReplayLoading.value = true
    try {
      const result = await runtime.store.replayDraft(draftId, {
        confirmed: true,
        loadCurrent: () => loadOfflineCurrent(runtime, draftId),
        execute: async (intent) => {
          const response = await executeBusinessAction(buildBusinessActionExecutePayload({
            action: { actionCode: intent.actionCode },
            config: { suiteCode: runtime.config.suiteCode },
            objectCode: intent.objectCode,
            recordId: intent.recordId,
            formData: intent.formData,
            routeQuery: intent.routeQuery,
            idempotencyKey: intent.idempotencyKey,
            parentRecordId: intent.parentRecordId,
            childRecordId: intent.childRecordId,
            relationKey: intent.relationKey,
          }))
          return unwrapBusinessActionResult(response, '草稿重放失败')
        },
      })
      if (result.status === 'completed') {
        runtime.store.removeDraft(draftId)
        offlineDraftId.value = ''
        offlineDraftNotice.value = '本地草稿已按幂等键提交。'
        await loadList()
      }
      else if (result.status === 'conflict') {
        offlineDraftNotice.value = result.message || '草稿存在冲突，请重新加载。'
      }
      else if (result.status === 'failed') {
        offlineDraftNotice.value = result.draft?.failureMessage || '草稿重放失败，请修正后重试。'
      }
    }
    finally {
      offlineReplayLoading.value = false
    }
  }

  async function loadOfflineCurrent(runtime, draftId) {
    const draft = runtime.store.getDraft(draftId)
    const publication = await loadOfflinePublishedSnapshot(runtime)
    if (!draft?.recordId) {
      return {
        available: true,
        ...publication,
      }
    }
    const { method, url } = parseApiConfig(
      'detail',
      `${props.api}/${draft.recordId}`,
      'get',
      { id: draft.recordId },
    )
    try {
      const requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
      const urlHasId = url.endsWith(`/${draft.recordId}`)
        || url.includes(`/${draft.recordId}?`)
        || url.includes(`/${draft.recordId}/`)
      const idParam = typeof props.rowKey === 'string' ? props.rowKey : 'id'
      const params = ['post', 'postEncrypt'].includes(requestMethod) && !urlHasId
        ? { [idParam]: draft.recordId }
        : undefined
      const response = requestMethod === 'postEncrypt'
        ? await postEncrypt(url, {}, { params })
        : await request({ method: requestMethod, url, params })
      const payload = response?.data && typeof response.data === 'object' ? response.data : response
      const current = payload?.main && typeof payload.main === 'object' ? payload.main : payload
      return {
        available: Boolean(current),
        ...publication,
        recordVersion: current?.[runtime.config.recordVersionField]
          ?? current?.version
          ?? current?.recordVersion,
      }
    }
    catch {
      return { available: false, ...publication }
    }
  }

  async function loadOfflinePublishedSnapshot(runtime) {
    if (!runtime.config.configKey) {
      return {
        publishedVersion: runtime.config.publishedVersion,
        schemaHash: runtime.config.schemaHash,
      }
    }
    const response = await crudConfigRender(runtime.config.configKey, false)
    const currentConfig = response?.data && typeof response.data === 'object' ? response.data : null
    if (!currentConfig)
      throw new Error('无法读取最新发布配置')
    return createOfflinePublishedSnapshot(currentConfig, runtime.config.formCode)
  }

  function clearOfflineDraftAfterSubmit() {
    if (mut.offlineDraftSaveTimer) {
      window.clearTimeout(mut.offlineDraftSaveTimer)
      mut.offlineDraftSaveTimer = null
    }
    if (offlineFormRuntime.value && offlineDraftId.value)
      offlineFormRuntime.value.store.removeDraft(offlineDraftId.value)
    offlineDraftId.value = ''
    offlineBaseRecordVersion.value = ''
    offlineDraftNotice.value = ''
  }

  function handleBrowserOnline() {
    const runtime = offlineFormRuntime.value
    if (!runtime)
      return
    const draft = offlineDraftId.value
      ? runtime.store.getDraft(offlineDraftId.value)
      : runtime.findDraft(resolveOfflineRecordId())
    if (!draft)
      return
    offlineDraftId.value = draft.draftId
    offlineBaseRecordVersion.value = draft.baseRecordVersion || offlineBaseRecordVersion.value
    offlineDraftNotice.value = draft.replayLog?.some(item => item.status !== 'COMPLETED')
      ? '网络已恢复，请检查发布版本和记录版本后再重放。'
      : '网络已恢复，本地草稿仍保留，请确认后在线提交。'
  }

  function resolveColumnWidth(value) {
    if (typeof value === 'number')
      return value
    if (typeof value !== 'string')
      return 0
    const matched = value.trim().match(/^(\d+(?:\.\d+)?)px?$/i)
    return matched ? Number(matched[1]) : 0
  }

  async function callHook(hookName, params, success) {
    if (props[hookName] && typeof props[hookName] === 'function') {
      const result = props[hookName](params)

      // 如果是 Promise
      if (result instanceof Promise) {
        try {
          const data = await result
          return success ? success(data) : data
        }
        catch (error) {
          console.error(`Hook ${hookName} error:`, error)
          // 提交前 BLOCK 增强必须真正阻断保存，不能在异步失败后回退到原始表单继续提交。
          if (hookName === 'beforeSubmit')
            throw error
          return success ? success(params) : params
        }
      }
      else {
        return success ? success(result) : result
      }
    }
    else {
      return success ? success(params) : params
    }
  }

  function resolveRuntimeConfigKey() {
    const sample = props.apiConfig?.update || props.apiConfig?.list || props.apiConfig?.delete || props.api || ''
    const match = String(sample).match(/\/ai\/crud\/([^/?]+)/)
    return match?.[1] || ''
  }

  /**
   * 解析 API 配置
   * @param {string} key - API 配置键名
   * @param {string} defaultApi - 默认 API
   * @param {string} defaultMethod - 默认请求方法
   * @param {object} urlParams - URL 参数，用于替换 :id 等占位符
   * @returns {object} { method, url }
   */
  function parseApiConfig(key, defaultApi, defaultMethod = 'get', urlParams = {}) {
    const apiConfigValue = props.apiConfig[key]
    const normalizedUrlParams = normalizeUrlParams(urlParams)

    if (apiConfigValue) {
      const [method, url] = apiConfigValue.split('@')
      const finalMethod = method === 'postEncrypt' ? method : method.toLowerCase()
      let finalUrl = url
      let hasPlaceholder = false
      Object.keys(normalizedUrlParams).forEach((paramKey) => {
        const paramValue = normalizedUrlParams[paramKey]
        if (!isUsableKeyValue(paramValue)) {
          return
        }
        if (finalUrl.includes(`:${paramKey}`)) {
          hasPlaceholder = true
          finalUrl = finalUrl.replaceAll(`:${paramKey}`, paramValue)
        }
        if (finalUrl.includes(`{${paramKey}}`)) {
          hasPlaceholder = true
          finalUrl = finalUrl.replaceAll(`{${paramKey}}`, paramValue)
        }
        if (paramKey === 'id' && /\/id(?=\/|$|\?)/.test(finalUrl)) {
          hasPlaceholder = true
          finalUrl = finalUrl.replace(/\/id(?=\/|$|\?)/g, `/${paramValue}`)
        }
      })
      if (!hasPlaceholder && finalMethod === 'get' && Object.keys(normalizedUrlParams).length > 0) {
        const paramValues = resolveUrlParamValues(normalizedUrlParams).join('/')
        if (paramValues) {
          finalUrl = `${finalUrl}/${paramValues}`
        }
      }
      return { method: finalMethod, url: finalUrl }
    }

    return { method: defaultMethod, url: defaultApi }
  }

  function extractApiUrl(apiConfigValue) {
    if (!apiConfigValue) {
      return ''
    }
    const text = String(apiConfigValue)
    const parts = text.split('@')
    return parts.length > 1 ? parts.slice(1).join('@') : text
  }

  function normalizeUrlParams(urlParams = {}) {
    const normalized = { ...(urlParams || {}) }
    const rowKey = typeof props.rowKey === 'string' ? props.rowKey : ''
    if (rowKey && isUsableKeyValue(normalized[rowKey]) && !isUsableKeyValue(normalized.id))
      normalized.id = normalized[rowKey]
    if (rowKey && isUsableKeyValue(normalized.id) && !isUsableKeyValue(normalized[rowKey]))
      normalized[rowKey] = normalized.id
    return normalized
  }

  function resolveUrlParamValues(urlParams = {}) {
    const rowKey = typeof props.rowKey === 'string' ? props.rowKey : ''
    if (rowKey && isUsableKeyValue(urlParams[rowKey]))
      return [urlParams[rowKey]]
    if (isUsableKeyValue(urlParams.id))
      return [urlParams.id]
    return Object.values(urlParams).filter(isUsableKeyValue)
  }

  function stableSerialize(value, seen = new WeakSet()) {
    if (value === undefined)
      return 'undefined'
    if (typeof value === 'function' || typeof value === 'symbol')
      return JSON.stringify(String(value))
    if (value === null || typeof value !== 'object')
      return JSON.stringify(value)
    if (seen.has(value))
      return '"[Circular]"'
    seen.add(value)
    if (Array.isArray(value)) {
      const result = `[${value.map(item => stableSerialize(item, seen)).join(',')}]`
      seen.delete(value)
      return result
    }
    const keys = Object.keys(value).sort()
    const result = `{${keys.map(key => `${JSON.stringify(key)}:${stableSerialize(value[key], seen)}`).join(',')}}`
    seen.delete(value)
    return result
  }

  function hasFilledSearchParams(params = {}) {
    return Object.values(params || {}).some((value) => {
      if (Array.isArray(value))
        return value.length > 0
      if (value && typeof value === 'object')
        return Object.keys(value).length > 0
      return value !== null && value !== undefined && value !== ''
    })
  }

  function getNestedValue(source, path) {
    if (!path)
      return undefined
    return String(path)
      .split('.')
      .filter(Boolean)
      .reduce((value, key) => value?.[key], source)
  }

  function toFiniteNumber(value) {
    if (value === null || value === undefined || value === '')
      return undefined
    const number = Number(value)
    return Number.isFinite(number) ? number : undefined
  }

  function extractListRows(payload, dataField, depth = 0) {
    if (Array.isArray(payload))
      return payload
    if (!payload || typeof payload !== 'object' || depth > 4)
      return []

    const configuredRows = getNestedValue(payload, dataField)
    if (Array.isArray(configuredRows))
      return configuredRows

    for (const key of ['records', 'list', 'rows', 'items']) {
      if (Array.isArray(payload[key]))
        return payload[key]
    }

    if (Array.isArray(payload.data))
      return payload.data
    if (payload.data && typeof payload.data === 'object')
      return extractListRows(payload.data, dataField, depth + 1)

    return []
  }

  function extractListTotal(payload, totalField, fallbackTotal = 0, depth = 0) {
    if (Array.isArray(payload))
      return payload.length
    if (!payload || typeof payload !== 'object' || depth > 4)
      return fallbackTotal

    const configuredTotal = toFiniteNumber(getNestedValue(payload, totalField))
    if (configuredTotal !== undefined)
      return configuredTotal

    for (const key of ['total', 'count', 'itemCount', 'totalCount']) {
      const total = toFiniteNumber(payload[key])
      if (total !== undefined)
        return total
    }

    if (payload.data && typeof payload.data === 'object')
      return extractListTotal(payload.data, totalField, fallbackTotal, depth + 1)

    return fallbackTotal
  }


  __impl.startFlowAction = startFlowAction
  __impl.resolveDocumentRuntime = resolveDocumentRuntime
  __impl.resolveMyTask = resolveMyTask
  __impl.resubmitFlowAction = resubmitFlowAction
  __impl.handleTaskAction = handleTaskAction
  __impl.withdrawFlowAction = withdrawFlowAction
  __impl.submitFlowStartWithApprovers = submitFlowStartWithApprovers
  __impl.submitFlowStartRequest = submitFlowStartRequest
  __impl.refreshCurrentDetailRuntime = refreshCurrentDetailRuntime
  __impl.callConfiguredApiAction = callConfiguredApiAction
  __impl.handleCommandAction = handleCommandAction
  __impl.submitCommandAction = submitCommandAction
  __impl.closeCommandActionModal = closeCommandActionModal
  __impl.executeCommandAction = executeCommandAction
  __impl.normalizeCommandActionRuntimeConfig = normalizeCommandActionRuntimeConfig
  __impl.buildCommandActionInitialData = buildCommandActionInitialData
  __impl.parseActionConfig = parseActionConfig
  __impl.sendConfiguredApiRequest = sendConfiguredApiRequest
  __impl.compactRequestOptions = compactRequestOptions
  __impl.buildConfiguredApiRequest = buildConfiguredApiRequest
  __impl.normalizeConfiguredApiConfig = normalizeConfiguredApiConfig
  __impl.parseConfiguredApiValue = parseConfiguredApiValue
  __impl.normalizeConfiguredApiMethod = normalizeConfiguredApiMethod
  __impl.normalizeConfiguredApiParam = normalizeConfiguredApiParam
  __impl.resolveApiParamTarget = resolveApiParamTarget
  __impl.resolveActionParamRawValue = resolveActionParamRawValue
  __impl.resolveObjectPathValue = resolveObjectPathValue
  __impl.replaceApiPathParam = replaceApiPathParam
  __impl.setApiObjectValue = setApiObjectValue
  __impl.isEmptyApiParamValue = isEmptyApiParamValue
  __impl.isFalseLike = isFalseLike
  __impl.confirmConfiguredAction = confirmConfiguredAction
  __impl.getActionLoadingKey = getActionLoadingKey
  __impl.isActionLoading = isActionLoading
  __impl.setActionLoading = setActionLoading
  __impl.setFlowActionPageLoading = setFlowActionPageLoading
  __impl.buildActionTarget = buildActionTarget
  __impl.resolveActionParamValue = resolveActionParamValue
  __impl.resolveSystemParamValue = resolveSystemParamValue
  __impl.resolveActionText = resolveActionText
  __impl.resolveTemplatePlaceholder = resolveTemplatePlaceholder
  __impl.resolveButtonType = resolveButtonType
  __impl.uniqueMainRecords = uniqueMainRecords
  __impl.isUsableKeyValue = isUsableKeyValue
  __impl.readBoolean = readBoolean
  __impl.resolveRowKeyValue = resolveRowKeyValue
  __impl.mergeHookRowWithOriginal = mergeHookRowWithOriginal
  __impl.resolveFormDefaultValues = resolveFormDefaultValues
  __impl.resolveSubmitDefaultParams = resolveSubmitDefaultParams
  __impl.handleToolbarOverflowSelect = handleToolbarOverflowSelect
  __impl.getColumnKey = getColumnKey
  __impl.isActionColumnConfig = isActionColumnConfig
  __impl.shouldDefaultActionFixedRight = shouldDefaultActionFixedRight
  __impl.collectExpandSlots = collectExpandSlots
  __impl.normalizeRowActions = normalizeRowActions
  __impl.findEditSchemaField = findEditSchemaField
  __impl.resolveColumnCompanionTextField = resolveColumnCompanionTextField
  __impl.hasDynamicOptionSourceForColumn = hasDynamicOptionSourceForColumn
  __impl.resolveRowCompanionText = resolveRowCompanionText
  __impl.snakeToCamelKey = snakeToCamelKey
  __impl.camelToSnakeKey = camelToSnakeKey
  __impl.resolveColumnRender = resolveColumnRender
  __impl.inlineSwitchUpdateKey = inlineSwitchUpdateKey
  __impl.isInlineSwitchUpdating = isInlineSwitchUpdating
  __impl.setInlineSwitchUpdating = setInlineSwitchUpdating
  __impl.canInlineSwitchUpdate = canInlineSwitchUpdate
  __impl.renderInlineSwitchColumn = renderInlineSwitchColumn
  __impl.handleInlineSwitchUpdate = handleInlineSwitchUpdate
  __impl.splitTableCellValues = splitTableCellValues
  __impl.buildDetailFallbackSchema = buildDetailFallbackSchema
  __impl.toReadonlyField = toReadonlyField
  __impl.refreshOfflineFormRuntime = refreshOfflineFormRuntime
  __impl.resolveOfflineRecordId = resolveOfflineRecordId
  __impl.readOfflineRecordVersion = readOfflineRecordVersion
  __impl.resolveOfflineRecordVersion = resolveOfflineRecordVersion
  __impl.offlineDraftPayload = offlineDraftPayload
  __impl.saveOfflineDraft = saveOfflineDraft
  __impl.scheduleOfflineDraftSave = scheduleOfflineDraftSave
  __impl.flushOfflineDraftSave = flushOfflineDraftSave
  __impl.resetOfflineDraftSession = resetOfflineDraftSession
  __impl.restoreOfflineDraft = restoreOfflineDraft
  __impl.isBrowserOffline = isBrowserOffline
  __impl.createOfflineReplayKey = createOfflineReplayKey
  __impl.appendOfflineSubmitIntent = appendOfflineSubmitIntent
  __impl.confirmOfflineReplay = confirmOfflineReplay
  __impl.replayOfflineDraft = replayOfflineDraft
  __impl.loadOfflineCurrent = loadOfflineCurrent
  __impl.loadOfflinePublishedSnapshot = loadOfflinePublishedSnapshot
  __impl.clearOfflineDraftAfterSubmit = clearOfflineDraftAfterSubmit
  __impl.handleBrowserOnline = handleBrowserOnline
  __impl.resolveColumnWidth = resolveColumnWidth
  __impl.callHook = callHook
  __impl.resolveRuntimeConfigKey = resolveRuntimeConfigKey
  __impl.parseApiConfig = parseApiConfig
  __impl.extractApiUrl = extractApiUrl
  __impl.normalizeUrlParams = normalizeUrlParams
  __impl.resolveUrlParamValues = resolveUrlParamValues
  __impl.stableSerialize = stableSerialize
  __impl.hasFilledSearchParams = hasFilledSearchParams
  __impl.getNestedValue = getNestedValue
  __impl.toFiniteNumber = toFiniteNumber
  __impl.extractListRows = extractListRows
  __impl.extractListTotal = extractListTotal

  return {
    ...deps,
  }
}
