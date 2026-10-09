/** AiCrudPage setup part 3. */
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

export function applyAiCrudPagePart3(props, emit, deps = {}) {
  const {
    __impl, mut, isPrintRuntimeAction, resolveDetailActionIcon, renderActionColumn, mergeRuntimeActions, normalizeRuntimeAction, isStartFlowRuntimeHidden,
    resolveRuntimeObjectCode, firstNonBlankText, sameAction, isActionDisabled, isFlowRelatedReadOnly, renderFlowRelationTags, actionDisabledReason, showActionDisabledMessage,
    resolveActionDisplayLabel, resolveActionTextValue, handleCustomActionClick, hasRuntimePermission, matchDisplayCondition, resolveConditionValue, handleActionClick, handleConfiguredAction,
    runBeforeRowAction, handleFormValueUpdate, isChildRowActionVisible, isChildRowActionLoading, handleChildRowAction, handleChildToolbarAction, submitChildToolbarAction, closeChildToolbarActionModal,
    handleConfiguredActionSuccess, startProcessAction, startFlowAction, resolveDocumentRuntime, resolveMyTask, resubmitFlowAction, handleTaskAction, withdrawFlowAction,
    submitFlowStartWithApprovers, submitFlowStartRequest, refreshCurrentDetailRuntime, callConfiguredApiAction, handleCommandAction, submitCommandAction, closeCommandActionModal, executeCommandAction,
    normalizeCommandActionRuntimeConfig, buildCommandActionInitialData, parseActionConfig, sendConfiguredApiRequest, compactRequestOptions, buildConfiguredApiRequest, normalizeConfiguredApiConfig, parseConfiguredApiValue,
    normalizeConfiguredApiMethod, normalizeConfiguredApiParam, resolveApiParamTarget, resolveActionParamRawValue, resolveObjectPathValue, replaceApiPathParam, setApiObjectValue, isEmptyApiParamValue,
    isFalseLike, confirmConfiguredAction, getActionLoadingKey, isActionLoading, setActionLoading, setFlowActionPageLoading, buildActionTarget, resolveActionParamValue,
    resolveSystemParamValue, resolveActionText, resolveTemplatePlaceholder, resolveButtonType, uniqueMainRecords, isUsableKeyValue, readBoolean, resolveRowKeyValue,
    mergeHookRowWithOriginal, resolveFormDefaultValues, resolveSubmitDefaultParams, handleToolbarOverflowSelect, getColumnKey, isActionColumnConfig, shouldDefaultActionFixedRight, collectExpandSlots,
    normalizeRowActions, findEditSchemaField, resolveColumnCompanionTextField, hasDynamicOptionSourceForColumn, resolveRowCompanionText, snakeToCamelKey, camelToSnakeKey, resolveColumnRender,
    inlineSwitchUpdateKey, isInlineSwitchUpdating, setInlineSwitchUpdating, canInlineSwitchUpdate, renderInlineSwitchColumn, handleInlineSwitchUpdate, splitTableCellValues, buildDetailFallbackSchema,
    toReadonlyField, refreshOfflineFormRuntime, resolveOfflineRecordId, readOfflineRecordVersion, resolveOfflineRecordVersion, offlineDraftPayload, saveOfflineDraft, scheduleOfflineDraftSave,
    flushOfflineDraftSave, resetOfflineDraftSession, restoreOfflineDraft, isBrowserOffline, createOfflineReplayKey, appendOfflineSubmitIntent, confirmOfflineReplay, replayOfflineDraft,
    loadOfflineCurrent, loadOfflinePublishedSnapshot, clearOfflineDraftAfterSubmit, handleBrowserOnline, resolveColumnWidth, callHook, resolveRuntimeConfigKey, parseApiConfig,
    extractApiUrl, normalizeUrlParams, resolveUrlParamValues, stableSerialize, hasFilledSearchParams, getNestedValue, toFiniteNumber, extractListRows,
    extractListTotal, loadRecordForm, loadDetailRuntime, applyApplicationProcessRuntimeSnapshot, applyDocumentRuntimeSnapshot, disposeFlowRuntimeRefresh, queueFlowRuntimeRefresh, importModalVisible,
    hasImportTemplate, exportLoading, exportTaskDrawerVisible, exportTaskLoading, exportTasks, exportTaskDrawerWidth, showExportTaskEntry, activeExportTask,
    exportTaskPaginationConfig, exportTaskColumns, handleShowImport, executeImport, handleImportSuccess, handleDownloadTemplate, handleExport, handleOpenExportTasks,
    clearExportTaskPollTimer, isExportTaskRunning, resolveExportTaskStatusText, resolveExportTaskTagType, runtimeFormulaFields, runtimeFormulaCalculationEnabled, runtimeFormulaSignature, scheduleRuntimeFormulaCalculation,
    refreshRuntimeFormulas, clearRuntimeFormulaTimers, crudRootRef, pageHeightStyle, router, route, userStore, slots,
    searchRef, tableRef, formRef, childFormRef, commandActionFormRef, searchParams, dataSource, tableLoading,
    selectedKeys, expandedRowKeys, customQueryPayload, customQueryFields, activeRenderMode, searchPanelVisible, pagination, modalVisible,
    modalTitle, modalStatus, formData, childFormData, confirmLoading, currentRow, fieldEventLoadToken, inlineFormTabs,
    activeInlineFormTabKey, INLINE_WORKSPACE_LIST_KEY, activeInlineWorkspaceKey, inlineFormHydrating, formOnlySubmitted, detailRuntime, detailRuntimeLoading, detailActiveTab,
    actionLoadingKeys, flowActionPageLoading, flowActionPageLoadingText, flowStartApproverModalVisible, flowStartApproverSubmitting, flowStartApproverNodes, flowStartApproverSelections, flowStartApproverLabels,
    flowStartApproverContext, commandActionModalVisible, commandActionSubmitting, commandActionFormData, commandActionContext, childToolbarActionModalVisible, childToolbarActionSubmitting, childToolbarActionContext,
    childToolbarItemSelected, childToolbarQuantity, offlineFormRuntime, offlineDraftId, offlineBaseRecordVersion, offlineDraftNotice, offlineDraftHydrating, offlineReplayLoading,
    commandActionTitle, commandActionFormSchema, commandActionFormContext, childToolbarActionTitle, childToolbarQuantityLabel, childToolbarItemOptions, childToolbarQuantityMax, visibleToolbarActions,
    visibleDetailActions, visibleFormActions, maxActionButtons, rowKeyFn, tableRowKeyFn, resolvedCustomQueryConfigKey, toolbarOverflowOptions, toolbarDropdownOptions,
    activeSourceColumns, formContext, normalizedExpandConfig, hasExpandConfig, resolvedResizable, inlineSwitchUpdatingMap, tableColumns,
    isEmbeddedTreeTable, effectiveShowPagination, effectiveTableProps, collectEmbeddedTreeExpandKeys, paginationConfig,
    searchSlots, tableSlots, formSlots, isDetailMode, resolvedFormOpenMode, usesInlineFormWorkspace, isTabWorkspaceMode, inlineWorkspaceVisible,
    showInlineListPane, showInlineFormWorkspacePane, tiledEditGridCols, activeInlineFormTab, activeInlineFormTitle, inlineFormModeLabel, showInlineFormModeTag, resolvedTabWorkspace,
    activeModalWidth, detailFlowTimelineVisible, detailFlowDiagramVisible, showDetailFlowTabs, dataAuditMeta, showDataChangeLogTab, showDetailExtraTabs, dataAuditObjectId,
    dataAuditRecordId, dataAuditEnabled, dataAuditHistoryAvailable, visibleChildrenConfig, hasChildrenConfig, showDefaultDetailContent, showDefaultDetailChildren, normalizedDetailPanelConfig,
    showDetailPanels, detailPanelRowKeyValue, hasSearchSchema, hasActiveListFilters, resolvedEmptyTitle, resolvedEmptyDescription, normalizedSearchSchema, modalFormSchema,
    offlineReplayAvailable, resolvedFormOnlyTitle, resolvedEditFormClass, DEFAULT_SELECTION_COLUMN_WIDTH, DEFAULT_EXPAND_COLUMN_WIDTH, DEFAULT_ACTION_COLUMN_WIDTH, DEFAULT_DATA_COLUMN_WIDTH, TABLE_SCROLL_X_BUFFER,
    computedScrollX, computedMaxHeight,
  } = deps

  /**
   * 加载列表数据
   */
  async function loadList() {
    if (!customQueryPayload.value && !props.api && !props.apiConfig.list && !props.apiConfig.tree) {
      console.warn('未配置 API 地址')
      return
    }

    tableLoading.value = true

    try {
      // 构建请求参数（查询区树本级+子集：用前端展开值覆盖单选值，与左树 field=1,5 一致）
      let params = applySearchTreeExpandedParams({
        ...searchParams.value,
        ...props.publicParams,
      })

      // 分页参数（嵌入式树表走全量 /tree，不能带 page 参数）
      if (effectiveShowPagination.value) {
        if (props.listMethod === 'get') {
          params = {
            ...params,
            ...props.publicQuery,
            pageNum: pagination.value.page,
            pageSize: pagination.value.pageSize,
          }
        }
        else {
          params.pageNum = pagination.value.page
          params.pageSize = pagination.value.pageSize
        }
      }

      // 调用 beforeLoadList 钩子
      params = await callHook('beforeLoadList', params, data => data)

      // 发送请求
      let response
      if (customQueryPayload.value && resolvedCustomQueryConfigKey.value) {
        response = await customQueryExecute(resolvedCustomQueryConfigKey.value, {
          ...resolveDefaultRequestSortParams(),
          ...customQueryPayload.value,
          pageNum: pagination.value.page,
          pageSize: pagination.value.pageSize,
        }, {
          globalLoading: false,
        })
      }
      else {
        // 嵌入式树表优先打 /tree，避免上游 props 未改写 list 时落到平铺 /page
        const listApiKey = isEmbeddedTreeTable.value && props.apiConfig?.tree ? 'tree' : 'list'
        const { method, url } = parseApiConfig(listApiKey, props.api, props.listMethod)

        // 确定使用哪种请求方法
        let requestMethod = method
        // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
        const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
        if (useEncrypt) {
          requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
        }
        else {
          requestMethod = method.toLowerCase()
        }

        if (useEncrypt && requestMethod === 'postEncrypt') {
          // 使用加密请求
          response = await postEncrypt(url, params, { globalLoading: false })
        }
        else {
          // 使用普通请求
          const requestConfig = {
            method: requestMethod,
            url,
            globalLoading: false,
          }

          if (requestMethod === 'get') {
            requestConfig.params = params
          }
          else {
            requestConfig.data = params
            requestConfig.params = props.publicQuery
          }

          response = await request(requestConfig)
        }
      }

      // 提取数据
      let list = []
      let total = 0

      const responseData = response?.data ?? response
      list = extractListRows(responseData, props.listDataField)
      total = extractListTotal(responseData, props.listTotalField, list.length)

      // 调用 beforeRenderList 钩子
      list = await callHook('beforeRenderList', list, data => data)

      // 列表动作需要按记录读取流程运行态。否则“发起主流程”只会依赖静态
      // 配置，流程启动后仍会留在操作列里，也无法正确展示流程状态。
      list = await enrichDocumentRuntimeRows(list)

      // 更新数据
      dataSource.value = list
      pagination.value.itemCount = total
      if (isEmbeddedTreeTable.value && effectiveTableProps.value?.defaultExpandAll !== false) {
        expandedRowKeys.value = collectEmbeddedTreeExpandKeys(list)
      }

      emit('load-list-success', { list, total })
    }
    catch (error) {
      console.error('加载列表失败:', error)
      window.$message.error('加载数据失败')
      emit('load-list-error', error)
    }
    finally {
      tableLoading.value = false
    }
  }

  async function enrichDocumentRuntimeRows(list = []) {
    const objectCode = String(props.businessObjectCode || '').trim()
    const rows = Array.isArray(list) ? list : []
    if (!objectCode || !rows.length)
      return rows
    const childrenField = props.treeConfig?.childrenField || 'children'
    const flatItems = []
    const visit = (nodes = []) => {
      if (!Array.isArray(nodes))
        return
      nodes.forEach((row) => {
        if (!row || typeof row !== 'object')
          return
        flatItems.push(row)
        if (Array.isArray(row[childrenField]) && row[childrenField].length)
          visit(row[childrenField])
      })
    }
    visit(rows)
    const recordIds = [...new Set(flatItems.map(row => resolveRowKeyValue(row)).filter(isUsableKeyValue))]
    if (!recordIds.length)
      return rows
    try {
      const response = await businessDocumentRuntimeBatch(objectCode, recordIds)
      const payload = response?.data ?? response ?? {}
      const runtimeMap = payload && typeof payload === 'object' ? payload : {}
      const applyRuntime = (nodes = []) => {
        if (!Array.isArray(nodes))
          return nodes
        return nodes.map((row) => {
          if (!row || typeof row !== 'object')
            return row
          const key = String(resolveRowKeyValue(row))
          const runtime = runtimeMap[key] || runtimeMap[resolveRowKeyValue(row)]
          const next = runtime
            ? {
                ...row,
                _documentRuntime: runtime,
                _runtimeActions: Array.isArray(runtime.runtimeActions) ? runtime.runtimeActions : [],
                _runtimeObjectCode: objectCode,
              }
            : { ...row, _runtimeObjectCode: objectCode }
          if (Array.isArray(row[childrenField]) && row[childrenField].length)
            next[childrenField] = applyRuntime(row[childrenField])
          return next
        })
      }
      return applyRuntime(rows)
    }
    catch (error) {
      // 流程运行态不是列表数据本身，接口异常不能阻断普通 CRUD 列表。
      console.warn('[AiCrudPage] 加载列表流程运行态失败:', error?.message || error)
      return rows
    }
  }

  function resolveDefaultRequestSortParams() {
    const orderByColumn = props.publicParams?.orderByColumn
    const isAsc = props.publicParams?.isAsc
    if (!orderByColumn && !isAsc)
      return {}
    return {
      ...(orderByColumn ? { orderByColumn } : {}),
      ...(isAsc ? { isAsc } : {}),
    }
  }

  /** 查询区树选择：field__treeExpanded=1,5 → field=1,5；多值强制 _searchTypes=in；去掉展示用 Name */
  function applySearchTreeExpandedParams(params = {}) {
    const next = { ...(params || {}) }
    Object.keys(next).forEach((key) => {
      if (!key.endsWith('__treeExpanded'))
        return
      const field = key.slice(0, -'__treeExpanded'.length)
      const expanded = next[key]
      if (field && expanded !== undefined && expanded !== null && String(expanded).trim() !== '')
        next[field] = expanded
      delete next[key]
    })

    // 去掉 treeSelect/userSelect 伴随展示字段（fieldTreeSelectName），避免多余 AND 条件拖垮结果
    Object.keys(next).forEach((key) => {
      if (!key.endsWith('Name') || key.length <= 4)
        return
      const base = key.slice(0, -4)
      if (Object.prototype.hasOwnProperty.call(next, base))
        delete next[key]
    })

    let searchTypes = {}
    try {
      if (typeof next._searchTypes === 'string' && next._searchTypes.trim())
        searchTypes = JSON.parse(next._searchTypes) || {}
      else if (next._searchTypes && typeof next._searchTypes === 'object')
        searchTypes = { ...next._searchTypes }
    }
    catch {
      searchTypes = {}
    }
    Object.keys(next).forEach((key) => {
      if (key.startsWith('_') || key.endsWith('_includeChildren'))
        return
      const value = next[key]
      const multi = Array.isArray(value)
        ? value.length > 1
        : (typeof value === 'string' && value.includes(','))
      if (multi)
        searchTypes[key] = 'in'
    })
    if (Object.keys(searchTypes).length)
      next._searchTypes = JSON.stringify(searchTypes)

    return next
  }

  /**
   * 搜索
   */
  async function handleSearch(params) {
    // 调用 beforeSearch 钩子
    const processedParams = await callHook('beforeSearch', params, data => data)

    // 如果钩子返回 false，中断搜索
    if (processedParams === false) {
      return
    }

    searchParams.value = { ...processedParams }
    pagination.value.page = 1
    loadList()
  }

  /**
   * 重置
   */
  function handleReset() {
    searchParams.value = {}
    pagination.value.page = 1
    loadList()
  }

  /**
   * 刷新列表
   */
  function handleRefresh() {
    loadList()
  }

  function handleSearchToggle(visible) {
    searchPanelVisible.value = visible
  }

  function handleApplyCustomQuery(payload) {
    customQueryPayload.value = payload
    customQueryFields.value = payload?.fields || []
    if (payload?.renderMode) {
      activeRenderMode.value = payload.renderMode
    }
    pagination.value.page = 1
    loadList()
  }

  function handleClearCustomQuery() {
    customQueryPayload.value = null
    customQueryFields.value = []
    activeRenderMode.value = props.renderMode || 'table'
    pagination.value.page = 1
    loadList()
  }

  /**
   * 列表/卡片渲染模式切换
   */
  function handleRenderModeChange(mode) {
    activeRenderMode.value = mode
    emit('render-mode-change', mode)
  }

  /**
   * 翻页
   */
  function handlePageChange(page) {
    pagination.value.page = page
    loadList()
  }

  /**
   * 改变每页条数
   */
  function handlePageSizeChange(pageSize) {
    pagination.value.pageSize = pageSize
    pagination.value.page = 1
    loadList()
  }

  /**
   * 统一规范化编辑表单回填数据
   * - number/inputNumber/input-number：字符串 → 数字
   * - select/radio/checkbox：数字 → 字符串（匹配字典选项的 string value）
   */
  function normalizeEditData(data) {
    if (!data || typeof data !== 'object')
      return data
    const result = {}
    for (const [key, value] of Object.entries(data)) {
      const fieldConfig = props.editSchema.find(f => f.field === key)
      if (!fieldConfig) {
        result[key] = value
        continue
      }
      if (isNumberFieldType(fieldConfig.type)) {
        // 数字字段：字符串转数字
        if (typeof value === 'string') {
          result[key] = Number.parseFloat(value)
        }
        else if (value === null || value === undefined) {
          result[key] = 0
        }
        else {
          result[key] = value
        }
      }
      else if (['select', 'radio', 'checkbox'].includes(fieldConfig.type)) {
        // 字典选择字段：根据 options 的 value 类型或 valueType 配置决定类型转换
        const isNumberOption = fieldConfig.props?.options?.some?.(o => typeof o.value === 'number')
          || fieldConfig.valueType === 'number'
        if (isNumberOption) {
          // options 的 value 是数字类型，保留数字类型
          if (typeof value === 'string') {
            result[key] = Number.parseFloat(value)
          }
          else if (value === null || value === undefined) {
            result[key] = 0
          }
          else {
            result[key] = value
          }
        }
        else {
          // 默认：数字转字符串
          if (typeof value === 'number') {
            result[key] = String(value)
          }
          else {
            result[key] = value
          }
        }
      }
      else {
        result[key] = value
      }
    }
    return result
  }

  function isMasterDetailPayload(data) {
    return data && typeof data === 'object' && ('main' in data || 'children' in data)
  }

  function resolveChildKey(child) {
    return child.key || child.modelCode || child.tableName || 'children'
  }

  function buildInitialChildrenData() {
    const result = {}
    visibleChildrenConfig.value.forEach((child) => {
      result[resolveChildKey(child)] = []
    })
    return result
  }

  function normalizeChildrenData(children) {
    const source = children && typeof children === 'object' ? children : {}
    const result = {}
    visibleChildrenConfig.value.forEach((child) => {
      const key = resolveChildKey(child)
      result[key] = Array.isArray(source[key]) ? source[key] : []
    })
    return result
  }

  function applyDetailData(data) {
    if (hasChildrenConfig.value && isMasterDetailPayload(data)) {
      formData.value = normalizeEditData({ ...resolveFormDefaultValues(), ...(data.main || {}) })
      childFormData.value = normalizeChildrenData(data.children)
      return
    }
    formData.value = normalizeEditData({ ...resolveFormDefaultValues(), ...(data || {}) })
    childFormData.value = buildInitialChildrenData()
  }

  function buildMasterDetailSubmitData(data) {
    if (!hasChildrenConfig.value) {
      return data
    }
    const payload = isMasterDetailPayload(data)
      ? {
          main: { ...(data.main || {}) },
          children: normalizeChildrenData(data.children),
        }
      : {
          main: { ...(data || {}) },
          children: childFormRef.value?.getValue?.() || childFormData.value || {},
        }
    if (modalStatus.value === 'edit') {
      const key = typeof props.rowKey === 'string' ? props.rowKey : 'id'
      const idValue = payload.main?.[key] ?? currentRow.value?.[key]
      if (idValue !== undefined && idValue !== null) {
        payload.main[key] = idValue
      }
    }
    return payload
  }

  function cloneInlineFormValue(value) {
    if (value === null || value === undefined || typeof value !== 'object')
      return value
    try {
      if (typeof structuredClone === 'function')
        return structuredClone(value)
    }
    catch {}
    try {
      return JSON.parse(JSON.stringify(value))
    }
    catch {
      return Array.isArray(value) ? [...value] : { ...value }
    }
  }

  function buildInlineRecordTabKey(status, row) {
    const idValue = resolveRowKeyValue(row)
    if (!isUsableKeyValue(idValue))
      return ''
    return `${status}:${idValue}`
  }

  function findReusableInlineFormTab(status, row) {
    if (!usesInlineFormWorkspace.value || !isTabWorkspaceMode.value || status === 'add')
      return null
    if (!resolvedTabWorkspace.value.reuseRecordTab)
      return null
    const key = buildInlineRecordTabKey(status, row)
    if (!key)
      return null
    return inlineFormTabs.value.find(tab => tab.key === key) || null
  }

  function activateReusableInlineFormTab(status, row) {
    const tab = findReusableInlineFormTab(status, row)
    if (!tab)
      return false
    handleInlineFormTabChange(tab.key)
    return true
  }

  function buildInlineFormTabKey(status, row) {
    if (!isTabWorkspaceMode.value)
      return 'flat'
    if (status !== 'add') {
      const reusableKey = buildInlineRecordTabKey(status, row)
      if (reusableKey)
        return reusableKey
    }
    mut.inlineFormTabSequence += 1
    return `${status || 'form'}:${mut.inlineFormTabSequence}`
  }

  function openFormContainer(status, title, row = null, context = {}) {
    if (!usesInlineFormWorkspace.value || props.formOnly) {
      fieldEventLoadToken.value += 1
      modalVisible.value = true
      return true
    }
    const existing = findReusableInlineFormTab(status, row)
    if (existing) {
      handleInlineFormTabChange(existing.key)
      return true
    }
    if (isTabWorkspaceMode.value && inlineFormTabs.value.length >= resolvedTabWorkspace.value.maxTabs) {
      window.$message.warning(`最多同时打开 ${resolvedTabWorkspace.value.maxTabs} 个录入页签`)
      return false
    }

    const tab = {
      key: buildInlineFormTabKey(status, row),
      status,
      title,
      row: cloneInlineFormValue(row),
      context: cloneInlineFormValue(context),
      formData: cloneInlineFormValue(formData.value || {}),
      childFormData: cloneInlineFormValue(childFormData.value || {}),
      detailRuntime: cloneInlineFormValue(detailRuntime.value),
      detailActiveTab: detailActiveTab.value,
      offlineDraftId: offlineDraftId.value,
      offlineBaseRecordVersion: offlineBaseRecordVersion.value,
      offlineDraftNotice: offlineDraftNotice.value,
      dirty: false,
    }
    if (isTabWorkspaceMode.value) {
      inlineFormTabs.value = [...inlineFormTabs.value, tab]
      activeInlineWorkspaceKey.value = tab.key
    }
    else {
      inlineFormTabs.value = [tab]
    }
    activeInlineFormTabKey.value = tab.key
    fieldEventLoadToken.value += 1
    hydrateInlineFormTab(tab)
    return true
  }

  function hydrateInlineFormTab(tab) {
    if (!tab)
      return
    inlineFormHydrating.value = true
    modalStatus.value = tab.status || ''
    modalTitle.value = tab.title || ''
    currentRow.value = cloneInlineFormValue(tab.row)
    detailRuntime.value = cloneInlineFormValue(tab.detailRuntime)
    detailActiveTab.value = tab.detailActiveTab || 'business'
    offlineDraftId.value = tab.offlineDraftId || ''
    offlineBaseRecordVersion.value = tab.offlineBaseRecordVersion || ''
    offlineDraftNotice.value = tab.offlineDraftNotice || ''
    formData.value = cloneInlineFormValue(tab.formData || {})
    childFormData.value = cloneInlineFormValue(tab.childFormData || {})
    nextTick(() => {
      formRef.value?.restoreValidation()
      inlineFormHydrating.value = false
    })
  }

  function persistActiveInlineFormTab({ dirty = false } = {}) {
    if (!usesInlineFormWorkspace.value || inlineFormHydrating.value)
      return
    const tab = activeInlineFormTab.value
    if (!tab)
      return
    tab.formData = cloneInlineFormValue(formData.value || {})
    tab.childFormData = cloneInlineFormValue(childFormData.value || {})
    tab.detailRuntime = cloneInlineFormValue(detailRuntime.value)
    tab.detailActiveTab = detailActiveTab.value
    tab.offlineDraftId = offlineDraftId.value
    tab.offlineBaseRecordVersion = offlineBaseRecordVersion.value
    tab.offlineDraftNotice = offlineDraftNotice.value
    if (dirty)
      tab.dirty = true
  }

  function markActiveInlineFormClean() {
    const tab = activeInlineFormTab.value
    if (!tab)
      return
    persistActiveInlineFormTab({ dirty: false })
    tab.dirty = false
  }

  function handleInlineFormTabChange(key) {
    if (!key)
      return
    flushOfflineDraftSave()
    persistActiveInlineFormTab()
    const tab = inlineFormTabs.value.find(item => item.key === key)
    if (!tab)
      return
    activeInlineWorkspaceKey.value = key
    if (key === activeInlineFormTabKey.value) {
      hydrateInlineFormTab(tab)
      return
    }
    activeInlineFormTabKey.value = key
    hydrateInlineFormTab(tab)
  }

  function handleInlineWorkspaceListTab() {
    flushOfflineDraftSave()
    persistActiveInlineFormTab()
    activeInlineWorkspaceKey.value = INLINE_WORKSPACE_LIST_KEY
    resetOfflineDraftSession()
  }

  function inlineFormTabTitle(tab) {
    return tab?.title || '表单'
  }

  function closeInlineFormTab(key = activeInlineFormTabKey.value, force = false) {
    const tab = inlineFormTabs.value.find(item => item.key === key)
    if (!tab)
      return
    if (!force && tab.dirty) {
      window.$dialog.warning({
        title: '关闭录入页签',
        content: '当前页签存在未保存变更，确定关闭吗？',
        positiveText: '关闭',
        negativeText: '取消',
        onPositiveClick: () => closeInlineFormTab(key, true),
      })
      return
    }

    if (activeInlineFormTabKey.value === key) {
      flushOfflineDraftSave()
      persistActiveInlineFormTab()
    }

    const listPaneActive = isTabWorkspaceMode.value && activeInlineWorkspaceKey.value === INLINE_WORKSPACE_LIST_KEY
    const workspacePaneActive = activeInlineWorkspaceKey.value === key
    const index = inlineFormTabs.value.findIndex(item => item.key === key)
    inlineFormTabs.value = inlineFormTabs.value.filter(item => item.key !== key)
    if (activeInlineFormTabKey.value !== key) {
      if (workspacePaneActive)
        activeInlineWorkspaceKey.value = INLINE_WORKSPACE_LIST_KEY
      return
    }
    const nextTab = inlineFormTabs.value[Math.max(0, index - 1)] || inlineFormTabs.value[0]
    if (nextTab) {
      activeInlineFormTabKey.value = nextTab.key
      if (!listPaneActive)
        activeInlineWorkspaceKey.value = nextTab.key
      hydrateInlineFormTab(nextTab)
    }
    else {
      activeInlineFormTabKey.value = ''
      activeInlineWorkspaceKey.value = INLINE_WORKSPACE_LIST_KEY
      modalStatus.value = ''
      modalTitle.value = ''
      currentRow.value = null
      detailRuntime.value = null
      resetOfflineDraftSession()
      formData.value = {}
      childFormData.value = {}
      emit('modal-close')
    }
  }

  function handleCloseActiveInlineFormTab() {
    flushOfflineDraftSave()
    persistActiveInlineFormTab()
    closeInlineFormTab()
  }

  function handleInlineFormCancel() {
    flushOfflineDraftSave()
    persistActiveInlineFormTab()
    closeInlineFormTab()
  }

  function handleInlineFormSubmitSuccess(response, isEdit) {
    const tab = activeInlineFormTab.value
    if (!tab)
      return
    const previousKey = tab.key
    if (!isEdit) {
      const savedRecord = response?.data && typeof response.data === 'object' ? response.data : response
      const idValue = resolveRowKeyValue(savedRecord || {})
      if (isUsableKeyValue(idValue)) {
        tab.status = 'edit'
        tab.row = cloneInlineFormValue(savedRecord)
        tab.title = modalTitle.value === props.addButtonText ? '编辑' : tab.title
        tab.key = isTabWorkspaceMode.value ? `edit:${idValue}` : tab.key
        activeInlineFormTabKey.value = tab.key
        if (activeInlineWorkspaceKey.value === previousKey)
          activeInlineWorkspaceKey.value = tab.key
        modalStatus.value = 'edit'
        currentRow.value = cloneInlineFormValue(savedRecord)
      }
    }
    markActiveInlineFormClean()
    if (resolvedTabWorkspace.value.closeAfterSave)
      closeInlineFormTab(tab.key, true)
  }

  /**
   * 新增
   */
  async function handleAdd(defaultValues = null, options = {}) {
    flushOfflineDraftSave()
    persistActiveInlineFormTab()
    resetOfflineDraftSession()
    formOnlySubmitted.value = false
    const presetValues = isPlainRecord(defaultValues)
      ? defaultValues
      : null
    modalTitle.value = options.title || props.addButtonText
    modalStatus.value = 'add'
    currentRow.value = null
    detailRuntime.value = null
    detailActiveTab.value = 'business'

    // 初始化表单数据，设置默认值（支持 $forge:today 等动态预设）
    const initialData = {}
    props.editSchema.forEach((field) => {
      if (field.field) {
        const raw = field.defaultValue
        initialData[field.field] = raw === undefined || raw === null
          ? null
          : resolveRuntimeDefaultValue(raw, field.type || field.componentType)
      }
    })

    // 调用 beforeRenderForm 钩子（新增时）
    const formDataFromHook = await callHook('beforeRenderForm', null, data => data)

    // 合并默认值和钩子返回的数据
    if (formDataFromHook && typeof formDataFromHook === 'object') {
      if (hasChildrenConfig.value && isMasterDetailPayload(formDataFromHook)) {
        formData.value = { ...initialData, ...resolveFormDefaultValues(), ...(formDataFromHook.main || {}), ...(presetValues || {}) }
        childFormData.value = normalizeChildrenData(formDataFromHook.children)
      }
      else {
        formData.value = { ...initialData, ...resolveFormDefaultValues(), ...formDataFromHook, ...(presetValues || {}) }
        childFormData.value = buildInitialChildrenData()
      }
    }
    else {
      formData.value = { ...initialData, ...resolveFormDefaultValues(), ...(presetValues || {}) }
      childFormData.value = buildInitialChildrenData()
    }

    if (!presetValues)
      await restoreOfflineDraft('')

    if (props.formOnly) {
      fieldEventLoadToken.value += 1
    }
    else if (!openFormContainer('add', modalTitle.value, null, options)) {
      return
    }

    await nextTick()
    formRef.value?.restoreValidation()
    refreshRuntimeFormulas(0)
    markActiveInlineFormClean()

    emit('add', { defaults: presetValues, context: options })
    emit('modal-open', { status: 'add', row: null, defaults: presetValues, context: options })
  }

  async function handleAddChild(row) {
    if (!row) {
      window.$message.warning('缺少父级数据，无法添加下级')
      return
    }
    const parentField = resolveTreeParentField()
    const parentValue = resolveTreeParentValue(row)
    if (!isUsableKeyValue(parentValue)) {
      window.$message.warning(`缺少${parentField}对应的父级值，无法添加下级`)
      return
    }
    await handleAdd({ [parentField]: parentValue }, { title: '新增下级', parentRow: row })
  }

  function resolveTreeParentField() {
    return props.treeConfig?.parentField || props.treeConfig?.filterField || 'parentId'
  }

  function resolveTreeParentValue(row = {}) {
    const keyField = props.treeConfig?.keyField || (typeof props.rowKey === 'string' ? props.rowKey : 'id')
    const rowKey = typeof props.rowKey === 'string' ? props.rowKey : ''
    return row?.[keyField] ?? (rowKey ? row?.[rowKey] : undefined) ?? row?.id ?? row?.key ?? row?.targetValue
  }

  function isPlainRecord(value) {
    if (!value || typeof value !== 'object')
      return false
    const prototype = Object.getPrototypeOf(value)
    return prototype === Object.prototype || prototype === null
  }

  /** 详情拉取失败时收起已先打开的表单容器，避免留下空白弹窗/页签 */
  function discardOpenedFormContainer() {
    if (usesInlineFormWorkspace.value && !props.formOnly) {
      const key = activeInlineFormTabKey.value
      if (key)
        closeInlineFormTab(key, true)
      return
    }
    modalVisible.value = false
  }

  /**
   * 编辑
   * 先用列表行数据打开表单，再拉详情回填，避免等接口时全屏 loading 闪一下。
   */
  async function handleEdit(row) {
    if (row?._dataScopeAccess === 'RELATED') {
      window.$message?.warning('流程经手可见仅支持查看，不能修改或删除')
      return
    }
    if (activateReusableInlineFormTab('edit', row)) {
      emit('edit', row)
      emit('modal-open', { status: 'edit', row })
      return
    }

    flushOfflineDraftSave()
    persistActiveInlineFormTab()
    resetOfflineDraftSession()

    modalTitle.value = row?.__modalTitle || '编辑'
    modalStatus.value = 'edit'
    currentRow.value = row

    // 调用 beforeRenderForm 钩子（编辑时）
    const processedRow = await callHook('beforeRenderForm', row, data => data)
    const renderRow = mergeHookRowWithOriginal(row, processedRow)

    // 先铺开表单，再异步补全详情
    applyDetailData(renderRow)
    if (!openFormContainer('edit', modalTitle.value, row))
      return

    if (!await loadRecordForm(renderRow)) {
      discardOpenedFormContainer()
      return
    }

    offlineBaseRecordVersion.value = readOfflineRecordVersion(renderRow)
    await restoreOfflineDraft(resolveRowKeyValue(row))

    // 清除上一次潜留的表单校验状态
    await nextTick()
    formRef.value?.restoreValidation()
    refreshRuntimeFormulas(0)
    markActiveInlineFormClean()
    persistActiveInlineFormTab()

    emit('edit', row)
    emit('modal-open', { status: 'edit', row })
  }

  /**
   * 查看详情
   * 先打开容器再拉详情，避免全屏遮罩闪烁。
   */
  async function handleDetail(row) {
    if (activateReusableInlineFormTab('detail', row)) {
      emit('detail', row)
      emit('modal-open', { status: 'detail', row })
      return
    }

    flushOfflineDraftSave()
    persistActiveInlineFormTab()
    resetOfflineDraftSession()

    modalTitle.value = row?.__modalTitle || '查看详情'
    modalStatus.value = 'detail'
    currentRow.value = row
    detailActiveTab.value = 'business'
    detailRuntime.value = row?._documentRuntime || null

    const processedRow = await callHook('beforeRenderForm', row, data => data)
    const renderRow = mergeHookRowWithOriginal(row, processedRow)

    applyDetailData(renderRow)
    if (!openFormContainer('detail', modalTitle.value, row))
      return

    if (!await loadRecordForm(renderRow)) {
      discardOpenedFormContainer()
      return
    }

    await nextTick()
    formRef.value?.restoreValidation()
    markActiveInlineFormClean()
    persistActiveInlineFormTab()

    emit('detail', row)
    emit('modal-open', { status: 'detail', row })
  }

  /**
   * 加载详情
   */
  async function loadDetail(row) {
    confirmLoading.value = true

    try {
      const idValue = resolveRowKeyValue(row)
      if (!isUsableKeyValue(idValue)) {
        console.warn('[AiCrudPage] loadDetail id缺失', { rowKey: props.rowKey, rowKeys: Object.keys(row || {}).slice(0, 20), row })
        window.$message.warning(`缺少${props.rowKey}参数，无法加载详情`)
        confirmLoading.value = false
        return false
      }
      const { method, url } = parseApiConfig(
        'detail',
        `${props.api}/${idValue}`,
        'get',
        { id: idValue },
      )

      // 确定使用哪种请求方法
      let requestMethod = method
      // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
      const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
      if (useEncrypt) {
        requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
      }
      else {
        requestMethod = method.toLowerCase()
      }

      // 构建请求参数
      const requestConfig = {
        method: requestMethod,
        url,
      }

      // 对于 POST 请求，如果 URL 中不包含 ID，则将主键作为 query 参数传递
      const urlHasId = url.endsWith(`/${idValue}`) || url.includes(`/${idValue}?`) || url.includes(`/${idValue}/`)
      if ((requestMethod === 'post' || requestMethod === 'postEncrypt') && !urlHasId) {
        const idKey = props.rowKey
        requestConfig.params = { [idKey]: idValue }
      }

      // 发送请求
      let response
      if (useEncrypt && requestMethod === 'postEncrypt') {
        // 使用加密请求
        response = await postEncrypt(url, {}, { params: requestConfig.params })
      }
      else {
        // 使用普通请求
        response = await request(requestConfig)
      }

      // 调用 beforeRenderDetail 钩子
      const data = await callHook('beforeRenderDetail', response.data, data => data)
      applyDetailData(data)
      return true
    }
    catch (error) {
      console.error('加载详情失败:', error)
      window.$message.error('加载详情失败')
      return false
    }
    finally {
      confirmLoading.value = false
    }
  }

  /**
   * 删除
   */
  async function handleDelete(row) {
    if (row?._dataScopeAccess === 'RELATED') {
      window.$message?.warning('流程经手可见仅支持查看，不能修改或删除')
      return
    }
    const rows = [row]
    const key = resolveRowKeyValue(row)
    if (!isUsableKeyValue(key)) {
      console.warn('[AiCrudPage] delete id缺失', { rowKey: props.rowKey, rowKeys: Object.keys(row || {}).slice(0, 20), row })
      window.$message.warning(`缺少${props.rowKey}参数，无法删除`)
      return
    }
    const keys = [key]

    await performDelete(rows, keys)
  }

  /**
   * 批量删除
   */
  async function handleBatchDelete() {
    const checked = uniqueMainRecords(tableRef.value?.getCheckedRows() || [])

    if (checked.rows.length === 0) {
      window.$message.warning('请先选择要删除的数据')
      return
    }

    await performDelete(checked.rows, checked.keys)
  }

  /**
   * 将单条删除 API 配置转换为批量删除 URL。
   * 例：DELETE@/ai/crud/xxx/:id → /ai/crud/xxx/batch
   */
  function resolveBatchDeleteUrl(apiConfigStr) {
    const atIndex = apiConfigStr.indexOf('@')
    const urlPart = atIndex >= 0 ? apiConfigStr.slice(atIndex + 1) : apiConfigStr
    const placeholders = [':id', `:${props.rowKey}`, '{id}', `{${props.rowKey}}`]
    let basePath = urlPart
    for (const ph of placeholders) {
      const idx = basePath.indexOf(ph)
      if (idx > 0) {
        basePath = basePath.slice(0, idx)
        break
      }
    }
    // 去掉末尾斜杠后拼 /batch
    return `${basePath.replace(/\/+$/, '')}/batch`
  }

  /**
   * 用详情接口补齐删除所需的审计元信息（列表可能尚未挂载 _dataAudit）
   */
  async function fetchDataAuditMetaForDelete(recordId, row) {
    const idValue = isUsableKeyValue(recordId) ? recordId : resolveRowKeyValue(row)
    if (!isUsableKeyValue(idValue))
      return null
    const { method, url } = parseApiConfig(
      'detail',
      `${props.api}/${idValue}`,
      'get',
      { id: idValue },
    )
    const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
    const requestMethod = useEncrypt
      ? (method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase())
      : method.toLowerCase()
    let response
    if (useEncrypt && requestMethod === 'postEncrypt') {
      response = await postEncrypt(url, {})
    }
    else {
      response = await request({ method: requestMethod, url, globalLoading: false })
    }
    const data = response?.data ?? response
    return readDataAuditMeta(data) || readDataAuditMeta(data?.main) || null
  }

  async function executePlainDelete(keys) {
    const deleteApiConfig = props.apiConfig.delete
    const hasIdPlaceholder = deleteApiConfig && (deleteApiConfig.includes(':id') || deleteApiConfig.includes(`:${props.rowKey}`) || deleteApiConfig.includes('{id}') || deleteApiConfig.includes(`{${props.rowKey}}`))

    if (hasIdPlaceholder && keys.length > 1) {
      const batchUrl = resolveBatchDeleteUrl(deleteApiConfig)
      const useEncrypt = props.isEncrypt
      if (useEncrypt) {
        await postEncrypt(batchUrl, keys)
      }
      else {
        await request({ method: 'delete', url: batchUrl, data: keys })
      }
      return
    }

    if (hasIdPlaceholder) {
      for (const key of keys) {
        const urlParams = { id: key }
        const { method, url } = parseApiConfig('delete', props.api, 'delete', urlParams)
        const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
        const requestMethod = useEncrypt
          ? (method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase())
          : method.toLowerCase()
        if (useEncrypt && requestMethod === 'postEncrypt') {
          await postEncrypt(url, key)
        }
        else {
          await request({ method: requestMethod, url })
        }
      }
      return
    }

    const { method, url } = parseApiConfig('delete', props.api, 'delete')
    const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
    const requestMethod = useEncrypt
      ? (method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase())
      : method.toLowerCase()
    if (useEncrypt && requestMethod === 'postEncrypt') {
      await postEncrypt(url, keys)
    }
    else {
      await request({ method: requestMethod, url, data: keys })
    }
  }

  /**
   * 执行删除。审计原因弹窗必须在确认框之前打开，避免嵌套 dialog 导致原因框不出现。
   */
  async function performDelete(rows, keys) {
    const shouldContinue = await callHook('beforeDelete', rows, result => result)
    if (shouldContinue === false) {
      return
    }

    const configKey = resolveRuntimeConfigKey()
    const plan = await resolveDataAuditRemovePlan({
      configKey,
      ids: keys,
      rows,
      fetchRecordMeta: configKey ? fetchDataAuditMetaForDelete : null,
    })

    let deleteReason = ''
    if (plan.mode === 'audit' && plan.reasonRequired) {
      deleteReason = await promptAuditReason('请填写删除原因', '请填写删除原因')
      if (deleteReason === false)
        return
    }

    window.$dialog.warning({
      title: '确认删除',
      content: `确定要删除选中的 ${keys.length} 条数据吗？此操作不可恢复！`,
      positiveText: '删除',
      negativeText: '取消',
      onPositiveClick: async () => {
        window.$loading?.show?.('正在删除，请稍候...')
        try {
          if (plan.mode === 'audit') {
            const auditHandled = await applyDataAuditRemove({
              configKey: plan.configKey,
              ids: plan.ids,
              rows: plan.rows,
              requestRemove: dataAuditRemove,
              reason: deleteReason,
              skipPrompt: true,
            })
            if (auditHandled === false)
              return false
            window.$message.success('删除成功')
            selectedKeys.value = []
            loadList()
            return true
          }

          try {
            await executePlainDelete(keys)
          }
          catch (error) {
            if (!configKey || !isAuditReasonRequiredError(error))
              throw error
            window.$loading?.close?.()
            const reason = deleteReason || await promptAuditReason('请填写删除原因', '请填写删除原因')
            if (reason === false)
              return false
            window.$loading?.show?.('正在删除，请稍候...')
            await dataAuditRemove(configKey, {
              ids: keys.map(id => String(id)),
              reason,
            })
          }

          window.$message.success('删除成功')
          selectedKeys.value = []
          loadList()
          return true
        }
        catch (error) {
          console.error('删除失败:', error)
          const message = error?.message || error?.response?.data?.message
          window.$message.error(message || '删除失败')
          return false
        }
        finally {
          window.$loading?.close?.()
        }
      },
    })
  }

  /**
   * 提交表单
   */
  async function handleModalConfirm() {
    if (isDetailMode.value) {
      if (usesInlineFormWorkspace.value)
        handleCloseActiveInlineFormTab()
      else
        modalVisible.value = false
      return
    }

    try {
      await nextTick()
      await formRef.value?.validate()
      await childFormRef.value?.validate?.()

      // 调用 beforeSubmit 钩子
      // getFormData 与父级 formData 合并：避免 props 回声竞态导致映射回填键已在 UI 显示却未进提交体
      const latestFormData = {
        ...(formData.value || {}),
        ...(formRef.value?.getFormData?.() || {}),
      }
      formData.value = latestFormData
      let data = await callHook('beforeSubmit', { ...latestFormData, ...resolveSubmitDefaultParams() }, data => data)

      if (data === false) {
        return
      }
      data = buildMasterDetailSubmitData(data)
      data = await callHook('afterBuildSubmitData', data, data => data)
      if (data === false) {
        return
      }
      data = await applyDataAuditSubmit(data, { formData: formData.value, isEdit: modalStatus.value === 'edit' })
      if (data === false) {
        return
      }
      // 统一处理时间戳，转换为标准日期格式
      data = JSON.parse(JSON.stringify(data), (key, value) => {
        // 判断是否是时间戳（数字且长度在10位（秒）到13位（毫秒）之间）
        if (typeof value === 'number' && (value.toString().length === 10 || value.toString().length === 13)) {
          const date = new Date(value.toString().length === 10 ? value * 1000 : value)
          // 格式化为 yyyy-MM-dd HH:mm:ss
          const year = date.getFullYear()
          const month = String(date.getMonth() + 1).padStart(2, '0')
          const day = String(date.getDate()).padStart(2, '0')
          const hours = String(date.getHours()).padStart(2, '0')
          const minutes = String(date.getMinutes()).padStart(2, '0')
          const seconds = String(date.getSeconds()).padStart(2, '0')
          return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
        }
        return value
      })

      confirmLoading.value = true

      const isEdit = modalStatus.value === 'edit'

      // 新增优先 create，兼容旧配置 add；二者都无时不要默认 add（会落到空 props.api → /dev-api 404）
      let createKey = 'create'
      if (!isEdit) {
        if (props.apiConfig?.create) {
          createKey = 'create'
        }
        else if (props.apiConfig?.add) {
          createKey = 'add'
        }
        else {
          createKey = 'create'
        }
      }

      const idValue = isEdit ? resolveRowKeyValue(currentRow.value) : null
      if (isEdit && !isUsableKeyValue(idValue)) {
        window.$message.warning(`缺少${props.rowKey}参数，无法提交编辑`)
        return
      }

      if (offlineFormRuntime.value && isBrowserOffline()) {
        const draft = saveOfflineDraft(data)
        if (!draft)
          throw new Error(offlineDraftNotice.value || '本地草稿保存失败')
        const intent = appendOfflineSubmitIntent(draft, data, isEdit, idValue)
        offlineDraftNotice.value = intent
          ? '当前网络不可用，已保存本地草稿和待提交意图。联网后请检查并重放。'
          : '当前网络不可用，仅保存了本地草稿。联网后请手动提交。'
        persistActiveInlineFormTab({ dirty: true })
        window.$message.warning(offlineDraftNotice.value)
        return
      }

      const fallbackApi = String(props.api || '').trim()
        || (props.configKey ? `/ai/crud/${props.configKey}` : '')
      const { method, url } = parseApiConfig(
        isEdit ? 'update' : createKey,
        isEdit ? `${fallbackApi}/${idValue}` : fallbackApi,
        isEdit ? 'put' : 'post',
        isEdit ? { id: idValue } : {},
      )
      if (!String(url || '').trim()) {
        window.$message.error('缺少新增/更新接口地址，请检查业务对象运行配置中的 apiConfig')
        return
      }

      // 确定使用哪种请求方法
      let requestMethod = method
      // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
      const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
      if (useEncrypt) {
        requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
      }
      else {
        requestMethod = method.toLowerCase()
      }

      // 发送请求
      let response
      if (useEncrypt && requestMethod === 'postEncrypt') {
        // 使用加密请求
        response = await postEncrypt(url, data)
      }
      else {
        // 使用普通请求
        response = await request({ method: requestMethod, url, data })
      }

      clearOfflineDraftAfterSubmit()
      window.$message.success(`${isEdit ? '编辑' : '新增'}成功`)
      if (props.formOnly) {
        formOnlySubmitted.value = true
      }
      else if (usesInlineFormWorkspace.value) {
        handleInlineFormSubmitSuccess(response, isEdit)
      }
      else {
        modalVisible.value = false
      }

      await callHook('afterSubmit', { data, response, isEdit }, data => data)

      // 触发提交成功事件
      emit('submit-success', { data, response, isEdit })

      if (!props.formOnly)
        loadList()
    }
    catch (error) {
      console.error('提交失败:', error)

      // 前端表单校验失败时 AiForm 已完成滚动和高亮定位，这里不再弹出重复提示
      if (Array.isArray(error) && error.length > 0) {
        console.error('验证错误详情:', error)
      }
      else {
        window.$message.error(error?.message || '提交失败')
      }

      // 触发提交失败事件
      emit('submit-error', { error, data: formData.value })
    }
    finally {
      confirmLoading.value = false
    }
  }

  /**
   * 弹窗取消
   */
  function handleModalCancel() {
    modalVisible.value = false
  }

  /**
   * 弹窗关闭后
   */
  function handleModalClose() {
    flushOfflineDraftSave()
    modalStatus.value = ''
    resetOfflineDraftSession()
    formData.value = {}
    childFormData.value = {}
    currentRow.value = null
    formRef.value?.restoreValidation()

    emit('modal-close')
  }

  function resetFormOnly() {
    formOnlySubmitted.value = false
    handleAdd()
  }

  /**
   * formOnly 页面初始化：formInit.recordLoad 命中记录 ID 时进入编辑态加载存量数据，否则保持新增默认行为。
   * 定位参数依次从页面地址参数和登录上下文（如 currentUser.staffId）取值，命中第一个即生效。
   */
  async function openFormOnlyWithRecordInit() {
    const recordId = resolveFormOnlyRecordInitId()
    if (recordId) {
      const rowKey = typeof props.rowKey === 'string' && props.rowKey ? props.rowKey : 'id'
      await handleEdit({ [rowKey]: recordId, __modalTitle: '编辑' })
      return
    }
    handleAdd()
  }

  function resolveFormOnlyRecordInitId() {
    const recordLoad = props.formInit?.recordLoad
    if (!recordLoad || recordLoad.enabled !== true)
      return ''
    return resolveFormInitRecordId(props.formInit, {
      routeQuery: route.query || {},
      context: props.formRuntimeContext || {},
    })
  }

  watch(runtimeFormulaSignature, () => {
    if (!runtimeFormulaCalculationEnabled.value)
      return
    runtimeFormulaFields.value.forEach(scheduleRuntimeFormulaCalculation)
  })

  watch(formData, () => {
    persistActiveInlineFormTab({ dirty: true })
    scheduleOfflineDraftSave()
  }, { deep: true })

  watch(childFormData, () => {
    persistActiveInlineFormTab({ dirty: true })
    scheduleOfflineDraftSave()
  }, { deep: true })

  watch([
    () => stableSerialize(props.offlineDraft || {}),
    () => String(userStore.tenantId || ''),
    () => String(userStore.userId || ''),
  ], refreshOfflineFormRuntime, { immediate: true })

  watch(detailActiveTab, () => {
    persistActiveInlineFormTab()
  })

  /**
   * 选中项变化
   */
  watch(selectedKeys, (newKeys) => {
    const checked = uniqueMainRecords(tableRef.value?.getCheckedRows?.() || [])
    if (checked.keys.length) {
      emit('selection-change', { keys: checked.keys, rows: checked.rows })
      return
    }
    emit('selection-change', { keys: newKeys, rows: [] })
  })

  watch(
    () => props.renderMode,
    (mode) => {
      if (!customQueryPayload.value) {
        activeRenderMode.value = mode || 'table'
      }
    },
  )

  watch(
    () => props.showSearch,
    (visible) => {
      searchPanelVisible.value = visible !== false
    },
  )

  watch(
    () => props.formOnly,
    (value, oldValue) => {
      if (value && !oldValue) {
        handleAdd()
        return
      }
      if (!value && oldValue && !props.lazy)
        loadList()
    },
  )

  /**
   * ==================== 生命周期 ====================
   */
  onMounted(() => {
    window.addEventListener('online', handleBrowserOnline)
    if (props.formOnly) {
      openFormOnlyWithRecordInit()
      return
    }
    if (!props.lazy) {
      loadList()
    }
  })

  onBeforeUnmount(() => {
    disposeFlowRuntimeRefresh()
    flushOfflineDraftSave()
    window.removeEventListener('online', handleBrowserOnline)
    clearExportTaskPollTimer()
    clearRuntimeFormulaTimers()
  })

  // 监听公共参数内容变化，避免设计器拖拽尺寸时仅对象引用变化导致重复请求
  watch(() => stableSerialize(props.publicParams || {}), () => {
    if (props.formOnly)
      return
    pagination.value.page = 1
    loadList()
  })

  watch(() => stableSerialize(props.publicQuery || {}), () => {
    if (props.formOnly)
      return
    pagination.value.page = 1
    loadList()
  })


  deps.aiCrudPageExposeApi = {
    /**
     * 外部触发搜索
     */
    search: handleSearch,

    /**
     * 刷新列表
     */
    refresh: loadList,

    /**
     * 加载列表
     */
    loadList,

    /**
     * 获取选中的行
     */
    getSelectedRows: () => tableRef.value?.getCheckedRows() || [],

    /**
     * 获取选中的键
     */
    getSelectedKeys: () => {
      const checked = uniqueMainRecords(tableRef.value?.getCheckedRows?.() || [])
      return checked.keys.length ? checked.keys : [...selectedKeys.value]
    },

    /**
     * 清除选中
     */
    clearSelection: () => {
      selectedKeys.value = []
      tableRef.value?.clearSelection()
    },

    /**
     * 设置选中的键
     */
    setSelectedKeys: (keys) => {
      selectedKeys.value = keys
      tableRef.value?.setCheckedKeys(keys)
    },

    /**
     * 获取表格数据
     */
    getTableData: () => dataSource.value,

    /**
     * 设置表格数据
     */
    setTableData: (data) => {
      dataSource.value = data
    },

    /**
     * 打开新增弹窗
     */
    showAdd: handleAdd,

    /**
     * 打开编辑弹窗
     */
    showEdit: handleEdit,

    /**
     * 打开详情弹窗
     */
    showDetail: handleDetail,

    /**
     * 编辑（同 showEdit）
     */
    handleEdit,

    /**
     * 查看详情（同 showDetail）
     */
    handleDetail,

    /**
     * 删除
     */
    handleDelete,

    /**
     * 批量删除
     */
    handleBatchDelete,

    /**
     * 提交当前弹窗表单
     */
    submitForm: handleModalConfirm,

    /** 按动作编码触发当前 CRUD 中已发布的受控动作。 */
    triggerAction: (actionCode, payload = {}) => {
      const code = String(actionCode || '')
      const action = [
        ...(Array.isArray(props.runtimeActions) ? props.runtimeActions : []),
        ...(Array.isArray(props.toolbarActions) ? props.toolbarActions : []),
      ].find(item => String(item?.actionCode || item?.key || '') === code)
      if (!action)
        throw new Error(`页面动作不存在或未发布: ${code}`)
      const row = payload?.row && typeof payload.row === 'object'
        ? payload.row
        : payload?.record && typeof payload.record === 'object' ? payload.record : formData.value
      return handleConfiguredAction(action, row || {}, { skipExtensionHook: true })
    },

    /**
     * 关闭弹窗
     */
    closeModal: () => {
      if (usesInlineFormWorkspace.value)
        handleCloseActiveInlineFormTab()
      else
        modalVisible.value = false
    },

    /**
     * 获取搜索参数
     */
    getSearchParams: () => searchParams.value,

    /**
     * 设置搜索参数
     */
    setSearchParams: (params) => {
      searchParams.value = params
    },

    /**
     * 重置搜索
     */
    resetSearch: () => {
      searchRef.value?.handleReset()
    },
  }

  __impl.loadList = loadList
  __impl.enrichDocumentRuntimeRows = enrichDocumentRuntimeRows
  __impl.resolveDefaultRequestSortParams = resolveDefaultRequestSortParams
  __impl.applySearchTreeExpandedParams = applySearchTreeExpandedParams
  __impl.handleSearch = handleSearch
  __impl.handleReset = handleReset
  __impl.handleRefresh = handleRefresh
  __impl.handleSearchToggle = handleSearchToggle
  __impl.handleApplyCustomQuery = handleApplyCustomQuery
  __impl.handleClearCustomQuery = handleClearCustomQuery
  __impl.handleRenderModeChange = handleRenderModeChange
  __impl.handlePageChange = handlePageChange
  __impl.handlePageSizeChange = handlePageSizeChange
  __impl.normalizeEditData = normalizeEditData
  __impl.isMasterDetailPayload = isMasterDetailPayload
  __impl.resolveChildKey = resolveChildKey
  __impl.buildInitialChildrenData = buildInitialChildrenData
  __impl.normalizeChildrenData = normalizeChildrenData
  __impl.applyDetailData = applyDetailData
  __impl.buildMasterDetailSubmitData = buildMasterDetailSubmitData
  __impl.cloneInlineFormValue = cloneInlineFormValue
  __impl.buildInlineRecordTabKey = buildInlineRecordTabKey
  __impl.findReusableInlineFormTab = findReusableInlineFormTab
  __impl.activateReusableInlineFormTab = activateReusableInlineFormTab
  __impl.buildInlineFormTabKey = buildInlineFormTabKey
  __impl.openFormContainer = openFormContainer
  __impl.hydrateInlineFormTab = hydrateInlineFormTab
  __impl.persistActiveInlineFormTab = persistActiveInlineFormTab
  __impl.markActiveInlineFormClean = markActiveInlineFormClean
  __impl.handleInlineFormTabChange = handleInlineFormTabChange
  __impl.handleInlineWorkspaceListTab = handleInlineWorkspaceListTab
  __impl.inlineFormTabTitle = inlineFormTabTitle
  __impl.closeInlineFormTab = closeInlineFormTab
  __impl.handleCloseActiveInlineFormTab = handleCloseActiveInlineFormTab
  __impl.handleInlineFormCancel = handleInlineFormCancel
  __impl.handleInlineFormSubmitSuccess = handleInlineFormSubmitSuccess
  __impl.handleAdd = handleAdd
  __impl.handleAddChild = handleAddChild
  __impl.resolveTreeParentField = resolveTreeParentField
  __impl.resolveTreeParentValue = resolveTreeParentValue
  __impl.isPlainRecord = isPlainRecord
  __impl.handleEdit = handleEdit
  __impl.handleDetail = handleDetail
  __impl.loadDetail = loadDetail
  __impl.handleDelete = handleDelete
  __impl.handleBatchDelete = handleBatchDelete
  __impl.resolveBatchDeleteUrl = resolveBatchDeleteUrl
  __impl.fetchDataAuditMetaForDelete = fetchDataAuditMetaForDelete
  __impl.executePlainDelete = executePlainDelete
  __impl.performDelete = performDelete
  __impl.handleModalConfirm = handleModalConfirm
  __impl.handleModalCancel = handleModalCancel
  __impl.handleModalClose = handleModalClose
  __impl.resetFormOnly = resetFormOnly
  __impl.openFormOnlyWithRecordInit = openFormOnlyWithRecordInit
  __impl.resolveFormOnlyRecordInitId = resolveFormOnlyRecordInitId

  return {
    ...deps,
  }
}
