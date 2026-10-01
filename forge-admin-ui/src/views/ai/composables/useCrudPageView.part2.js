/** crud-page.vue setup part 2. */
import { computed, defineAsyncComponent, h, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { crudConfigRender } from '@/api/ai'
import { businessDocumentRuntimeBatch } from '@/api/business-app'
import catalog from '@/catalog'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import { buildFormRuntimeContext } from '@/components/ai-form/form-runtime-context'
import { createOfflineSchemaHash } from '@/components/ai-form/offline-form-runtime'
import { normalizeRecordSelectorConfig } from '@/components/ai-form/record-selector-utils'
import { applyCrudHookRules, CRUD_HOOK_RULE_TARGETS, normalizeCrudHookRules } from '@/components/lowcode-builder/page/crud-hook-rules'
import ListPageGridDesigner from '@/components/lowcode-builder/page/ListPageGridDesigner.vue'
import FieldValueRenderer from '@/components/lowcode-builder/shared/FieldValueRenderer.vue'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import { hasRuntimeVisibilityRules } from '@/components/lowcode-builder/shared/runtime-rules'
import { getDictData } from '@/composables/useDict'
import { useTabStore } from '@/store'
import { postEncrypt, request } from '@/utils'
import { getDefaultPageTitle } from '@/utils/page-title'
import {
  compileUiDocument,
  resolveAiFormSchemaFromUiDocument,
  UI_DOCUMENT_PROTOCOL_VERSION,
  UI_DOCUMENT_UI_TYPES,
} from '@/protocols/ui-document'
import { normalizeMultiFormDesignerSchema } from '@/views/app-center/components/designer/form-first/formDesignerSchema'
export function applyCrudPageViewPart2(props, emit, deps = {}) {
  const {
    __impl, mut, applyRuntimeColumnPresentation, applyRuntimeFieldGovernance, applyRuntimeFieldLength, applyRuntimeFieldValidation, applyRuntimeFormGovernance, buildRuntimeColumnRoute,
    buildRuntimeFieldFromDesignerComponent, buildRuntimeFormAssets, buildRuntimeFormLayoutFromDesignerComponents, buildRuntimeFormLayoutNode, buildRuntimeFormProfile, buildRuntimeRelationSource, ensureDetailRowAction, extractRouteEntryPublicQuery,
    flattenDesignerComponents, hydrateRuntimeLayoutNode, isActionRuntimeLayoutNode, isBuiltinCreateToolbarAction, isGroupTitleRuntimeLayoutNode, isLegacyGroupTitleRuntimeLayoutNode, isRuntimeObjectReferenceField, isRuntimeRecordSelectorField,
    isRuntimeTextField, isSectionTitleRuntimeLayoutNode, isStandaloneRuntimeLayoutNode, isSystemRuntimeField, mergeRowActions, normalizeActionIdentity, normalizeActionPosition, normalizeActionType,
    normalizeDesignerRuntimeFieldType, normalizeFormGovernance, normalizeRouteParamValue, normalizeRuntimeField, normalizeRuntimePageAction, normalizeRuntimePageActions, normalizeRuntimeWidth, parseRouteRecordParam,
    resolveDateTimeProps, resolvePageSchemaEditFormStyle, resolveRuntimeButtonType, resolveRuntimeForm, resolveRuntimeLayoutNodeType, resolveRuntimeModalWidth, transformColumns, transformEditFields,
    transformFields, route, router, tabStore, loading, configLoaded, errorMsg, renderConfig,
    dictCache, runtimeCrudRef, lastInitialActionKey, runtimeDetailRecord, runtimeDetailLoading, embeddedRuntime, runtimeOpenMode, formOnlyRuntime,
    designPreview, currentTemplate, activeRuntimePageKey, activeRuntimeFormKey, runtimePages, activeRuntimePage, activeRuntimeGridLayout, runtimeGridLayout,
    standardListRuntime, shouldRenderRuntimeGrid, runtimeEffectiveLayoutType, runtimeFields, runtimeColumnSettings, runtimeAiCrudBlockProps, activeRuntimeFormProfile, routeEntryPublicQuery,
    routeEntryFormDefaultValues, routeEntrySubmitDefaultParams, RUNTIME_ROUTE_PARAM_KEYS,
  } = deps
  const crudProps = computed(() => {
    if (!renderConfig.value)
      return {}
    const cfg = renderConfig.value
    const options = cfg.options || {}
    // 表单设计器保存的 layout 是表单项配置的单一事实来源，优先于运行配置的平铺键
    const designerLayout = activeRuntimeFormProfile.value.designerLayout || {}
    const formOpenMode = resolveRuntimeFormOpenMode(options, cfg, designerLayout)
    const treeTable = isTreeTableRuntime(cfg, runtimeEffectiveLayoutType.value)
    const treeConfig = options.treeConfig || {}
    const gridCrudProps = runtimeAiCrudBlockProps.value || {}
    const treeLoadMode = resolveTreeLoadMode(treeConfig)
    const defaultSortParams = resolveDefaultSortParams(options.defaultSort)
    const crudHookHandlers = buildCrudHookHandlers(normalizeCrudHookRules(
      options.crudHookRules || cfg.crudHookRules || gridCrudProps.crudHookRules || {},
      options.beforeSubmitRules || cfg.beforeSubmitRules || gridCrudProps.beforeSubmitRules || [],
    ))
    const governanceEventHandlers = buildFormGovernanceEventHandlers(activeRuntimeFormProfile.value.governance?.events || [])
    const runtimeHookHandlers = composeRuntimeHookHandlers(crudHookHandlers, governanceEventHandlers)
    const configuredPublicParams = {
      ...(options.publicParams || cfg.publicParams || {}),
      ...(gridCrudProps.publicParams || {}),
    }
    const configuredPublicQuery = {
      ...(options.publicQuery || cfg.publicQuery || {}),
      ...(gridCrudProps.publicQuery || {}),
      ...routeEntryPublicQuery.value,
    }
    const formDefaultValues = {
      ...(options.formDefaultValues || cfg.formDefaultValues || {}),
      ...(gridCrudProps.formDefaultValues || {}),
      ...routeEntryFormDefaultValues.value,
    }
    const submitDefaultParams = {
      ...(options.submitDefaultParams || cfg.submitDefaultParams || {}),
      ...(gridCrudProps.submitDefaultParams || {}),
      ...routeEntrySubmitDefaultParams.value,
    }
    const runtimeApiConfig = treeTable
      ? { ...(cfg.apiConfig || {}), list: cfg.apiConfig?.tree || cfg.apiConfig?.list }
      : cfg.apiConfig || {}
    const apiConfig = designPreview.value
      ? appendDesignPreviewToApiConfig(runtimeApiConfig)
      : runtimeApiConfig
    const masterDetailConfig = options.masterDetailConfig || {}
    const runtimeFieldMetaMap = buildRuntimeFieldMetaMap(cfg.modelSchema)
    return {
      searchSchema: transformFields(cfg.searchSchema, runtimeFieldMetaMap),
      columns: transformColumns(cfg.columnsSchema, cfg.transConfig, {
        treeTable,
        includeDetailAction: true,
        rowActions: options.rowActions,
        columnSettings: runtimeColumnSettings.value,
        fitTableToContainer: shouldRenderRuntimeGrid.value,
      }),
      editSchema: transformEditFields(
        activeRuntimeFormProfile.value.editSchema,
        activeRuntimeFormProfile.value.editFormLayout || options.editFormLayout,
        runtimeFieldMetaMap,
        activeRuntimeFormProfile.value.uiDocument,
      ),
      childrenConfig: transformChildrenConfig(masterDetailConfig.children || []),
      detailPanels: options.detailPanels || cfg.detailPanels || [],
      apiConfig,
      options,
      rowKey: cfg.rowKey || 'id',
      formOpenMode,
      tabWorkspace: options.tabWorkspace || cfg.tabWorkspace || {},
      modalType: resolveRuntimeModalType(formOpenMode, options, cfg, designerLayout),
      modalWidth: resolveRuntimeModalWidth(options, cfg, activeRuntimeFormProfile.value),
      detailModalWidth: designerLayout.detailModalWidth || options.detailModalWidth || cfg.detailModalWidth || 'min(1080px, 92vw)',
      drawerPlacement: designerLayout.drawerPlacement || options.drawerPlacement || cfg.drawerPlacement || 'right',
      editGridCols: designerLayout.gridColumns || options.editGridCols || cfg.editGridCols || 1,
      editLabelWidth: designerLayout.labelWidth || options.editLabelWidth || cfg.editLabelWidth || 'auto',
      editLabelPlacement: designerLayout.labelPlacement || options.editLabelPlacement || cfg.editLabelPlacement || 'left',
      editLabelAlign: designerLayout.labelAlign || options.editLabelAlign || cfg.editLabelAlign || 'right',
      editSize: designerLayout.size || options.editSize || cfg.editSize || 'medium',
      editEnableCollapse: designerLayout.enableCollapse ?? options.editEnableCollapse ?? cfg.editEnableCollapse ?? false,
      editMaxVisibleFields: normalizeNumberOption(designerLayout.maxVisibleFields ?? options.editMaxVisibleFields ?? cfg.editMaxVisibleFields, 6),
      editShowFeedback: designerLayout.showFeedback ?? options.editShowFeedback ?? cfg.editShowFeedback ?? true,
      editFormClass: designerLayout.formClass || options.editFormClass || cfg.editFormClass || '',
      editFormStyle: designerLayout.formStyle || options.editFormStyle || cfg.editFormStyle,
      formAssets: activeRuntimeFormProfile.value.formAssets || options.formAssets || cfg.formAssets || [],
      fieldEvents: activeRuntimeFormProfile.value.governance?.fieldEvents || [],
      formInit: activeRuntimeFormProfile.value.governance?.formInit || {},
      offlineDraft: buildOfflineDraftConfig(cfg, options, activeRuntimeFormProfile.value),
      formRuntimeContext: buildFormRuntimeContext(),
      editXGap: normalizeNumberOption(designerLayout.columnGap ?? options.editXGap ?? cfg.editXGap, 12),
      editYGap: normalizeNumberOption(designerLayout.rowGap ?? options.editYGap ?? cfg.editYGap, 8),
      loadDetailOnEdit: options.loadDetailOnEdit ?? cfg.loadDetailOnEdit ?? true,
      searchGridCols: options.searchGridCols || cfg.searchGridCols || 4,
      hideAdd: !!options.hideAdd,
      hideBatchDelete: !!options.hideBatchDelete,
      showImport: !!options.showImport,
      showExport: !!options.showExport,
      showPagination: treeTable ? false : options.showPagination !== false,
      enableTreeAddChild: treeTable && (options.enableTreeAddChild === true || gridCrudProps.enableTreeAddChild === true),
      importApi: extractApiUrl(apiConfig?.import),
      exportApi: apiConfig?.export || '',
      importTemplateUrl: extractApiUrl(apiConfig?.importTemplate),
      enableCustomQuery: options.enableCustomQuery !== false,
      customQueryConfigKey: cfg.configKey,
      toolbarActions: normalizeRuntimePageActions(options.toolbarActions || [], 'toolbar'),
      runtimeActions: normalizeRuntimePageActions(options.runtimeActions || [], 'row'),
      businessObjectCode: resolveBusinessObjectCode(cfg),
      showDataChangeLog: designerLayout.showDataChangeLog === true || options.showDataChangeLog === true,
      dataAuditObjectId: options.dataAuditObjectId || '',
      publicParams: treeTable
        ? { ...configuredPublicParams, ...defaultSortParams, loadMode: treeLoadMode }
        : { ...configuredPublicParams, ...defaultSortParams },
      publicQuery: configuredPublicQuery,
      formDefaultValues,
      submitDefaultParams,
      ...runtimeHookHandlers,
      beforeRenderList: list => prepareRuntimeList(list, { treeTable, treeConfig }),
      treeConfig: treeTable ? treeConfig : {},
      tableProps: buildRuntimeTableProps(cfg),
      onSubmitSuccess: handleRuntimeSubmitSuccess,
      formOnly: formOnlyRuntime.value,
      formOnlyTitle: resolveRuntimeTitle(cfg),
      formOnlySubmitText: '提交',
      formOnlySuccessTitle: '提交成功',
      formOnlySuccessDescription: '单据已保存',
    }
  })

  function buildOfflineDraftConfig(cfg = {}, options = {}, formProfile = {}) {
    const governanceConfig = formProfile.governance?.offlineDraft
    const source = governanceConfig && Object.keys(governanceConfig).length
      ? governanceConfig
      : (options.offlineDraft || cfg.offlineDraft || {})
    if (source?.enabled !== true)
      return { enabled: false }
    const objectCode = resolveBusinessObjectCode(cfg)
    const formCode = activeRuntimeFormKey.value || source.formCode || 'default'
    return {
      ...source,
      enabled: true,
      applicationCode: source.applicationCode
        || cfg.applicationCode
        || cfg.suiteCode
        || options.applicationCode
        || cfg.configKey,
      objectCode: source.objectCode || objectCode,
      formCode,
      configKey: source.configKey || cfg.configKey || '',
      suiteCode: source.suiteCode || cfg.suiteCode || options.suiteCode || '',
      publishedVersion: cfg.publishedVersion || source.publishedVersion || '',
      schemaHash: source.schemaHash || createOfflineSchemaHash({
        modelSchema: cfg.modelSchema || {},
        pageSchema: cfg.pageSchema || {},
        formCode,
      }),
    }
  }

  function resolveRuntimeFormOpenMode(options = {}, cfg = {}, designerLayout = {}) {
    const value = designerLayout.formOpenMode || designerLayout.modalType
      || options.formOpenMode || cfg.formOpenMode || options.modalType || cfg.modalType || 'modal'
    const mode = String(value || '').trim()
    if (mode === 'tabWorkspace' || mode.toLowerCase() === 'tabworkspace')
      return 'tabWorkspace'
    const normalized = mode.toLowerCase()
    return ['modal', 'drawer', 'flat'].includes(normalized) ? normalized : 'modal'
  }

  function resolveRuntimeModalType(formOpenMode, options = {}, cfg = {}, designerLayout = {}) {
    if (['modal', 'drawer'].includes(formOpenMode))
      return formOpenMode
    const modalType = String(designerLayout.modalType || options.modalType || cfg.modalType || '').trim().toLowerCase()
    return ['modal', 'drawer'].includes(modalType) ? modalType : 'modal'
  }

  function resolveDefaultSortParams(defaultSort = {}) {
    const orderByColumn = defaultSort.orderByColumn || defaultSort.field || 'id'
    const isAsc = defaultSort.isAsc || defaultSort.order || 'desc'
    return {
      orderByColumn,
      isAsc,
    }
  }

  function buildRuntimeFieldMetaMap(modelSchema = {}) {
    const result = new Map()
    const fields = Array.isArray(modelSchema?.fields) ? modelSchema.fields : []
    fields.forEach((field) => {
      const fieldCode = field?.field || field?.fieldCode
      if (fieldCode)
        result.set(fieldCode, field)
    })
    return result
  }

  function applyRuntimeFieldMeta(field, meta = {}) {
    if (!field || !meta)
      return
    if (!field.dataType && meta.dataType)
      field.dataType = meta.dataType
    if (!field.fieldDataType && meta.dataType)
      field.fieldDataType = meta.dataType
    if (!field.componentType && meta.componentType)
      field.componentType = meta.componentType
    const formulaConfig = field.formulaConfig || meta.formulaConfig
    if (!hasRuntimeFormulaConfig(formulaConfig))
      return
    field.formulaConfig = formulaConfig
    field.required = false
    field.disabled = true
    field.readonly = true
    if (Array.isArray(field.rules))
      field.rules = field.rules.filter(rule => !rule?.required)
    field.props = {
      ...(field.props || {}),
      disabled: true,
      readonly: true,
    }
  }

  function hasRuntimeFormulaConfig(formulaConfig) {
    if (!formulaConfig)
      return false
    if (typeof formulaConfig === 'string')
      return formulaConfig.trim().length > 0
    return typeof formulaConfig === 'object' && Object.keys(formulaConfig).length > 0
  }

  function normalizeNumberOption(value, fallback) {
    const number = Number(value)
    return Number.isFinite(number) ? number : fallback
  }

  function buildCrudHookHandlers(rules = {}) {
    return CRUD_HOOK_RULE_TARGETS.reduce((handlers, target) => {
      const list = (rules[target.value] || []).filter(rule => rule.field)
      if (list.length)
        handlers[target.value] = data => applyCrudHookRules(data, list)
      return handlers
    }, {})
  }

  function buildFormGovernanceEventHandlers(events = []) {
    return (Array.isArray(events) ? events : []).reduce((handlers, eventItem) => {
      const hookName = normalizeFormGovernanceHook(eventItem?.hook)
      if (!hookName || !eventItem?.handler)
        return handlers
      if (!handlers[hookName])
        handlers[hookName] = []
      handlers[hookName].push(eventItem)
      return handlers
    }, {})
  }

  function normalizeFormGovernanceHook(hook = '') {
    const value = String(hook || '')
    if (value === 'beforeLoad' || value === 'afterLoad')
      return 'beforeRenderForm'
    if (value === 'beforeSubmit')
      return 'beforeSubmit'
    if (value === 'afterSubmit')
      return 'afterSubmit'
    return ''
  }

  function composeRuntimeHookHandlers(...handlerGroups) {
    const hookNames = new Set(handlerGroups.flatMap(group => Object.keys(group || {})))
    return Array.from(hookNames).reduce((handlers, hookName) => {
      const handlersForHook = handlerGroups
        .map(group => group?.[hookName])
        .filter(Boolean)
      handlers[hookName] = async (payload) => {
        let nextPayload = payload
        for (const handler of handlersForHook) {
          if (Array.isArray(handler)) {
            nextPayload = await runFormGovernanceEvents(handler, hookName, nextPayload)
          }
          else {
            nextPayload = await handler(nextPayload)
          }
          if (nextPayload === false)
            return false
        }
        return nextPayload
      }
      return handlers
    }, {})
  }

  async function runFormGovernanceEvents(events = [], hookName = '', payload) {
    for (const eventItem of events) {
      await runFormGovernanceEvent(eventItem, hookName, payload)
    }
    return payload
  }

  async function runFormGovernanceEvent(eventItem = {}, hookName = '', payload) {
    const action = String(eventItem.action || '')
    if (action === 'customScript') {
      return runWhitelistedFormScript(eventItem.handler, payload)
    }
    if (action === 'setFieldValue') {
      applySetFieldValueEvent(eventItem.handler, payload)
      return
    }
    if (action !== 'request')
      return
    const apiConfig = parseApiConfigValue(eventItem.handler)
    if (!apiConfig.url)
      return
    const method = apiConfig.method || 'post'
    const requestConfig = {
      method,
      url: apiConfig.url,
    }
    if (method === 'get')
      requestConfig.params = payload
    else
      requestConfig.data = payload
    try {
      const response = await request(requestConfig)
      applyFormEventResultMapping(payload, response?.data, eventItem.resultMapping)
    }
    catch (error) {
      console.warn(`[crud-page] 表单事件请求失败(${hookName}):`, error?.message || error)
      throw error
    }
  }

  function runWhitelistedFormScript(handler = '', payload = {}) {
    const name = String(handler || '').trim()
    if (!name || name === 'noop')
      return
    const scripts = {
      fillCurrentDate: () => {
        payload.currentDate = new Date().toISOString().slice(0, 10)
      },
      fillCurrentTime: () => {
        payload.currentTime = new Date().toISOString()
      },
    }
    if (!scripts[name]) {
      console.warn(`[crud-page] 表单事件脚本不在白名单内: ${name}`)
      return
    }
    scripts[name]()
  }

  function applySetFieldValueEvent(handler = '', payload = {}) {
    const [field, ...valueParts] = String(handler || '').split('=')
    const fieldName = field?.trim()
    if (!fieldName)
      return
    payload[fieldName] = valueParts.join('=').trim()
  }

  function applyFormEventResultMapping(payload = {}, responseData, mappingText = '') {
    const mappings = String(mappingText || '').split(',').map(item => item.trim()).filter(Boolean)
    mappings.forEach((item) => {
      const [from, to] = item.split('->').map(part => part?.trim())
      if (!from || !to)
        return
      const value = getByPath(responseData, from)
      if (value !== undefined)
        payload[to] = value
    })
  }

  function getByPath(source, path = '') {
    return String(path || '').split('.').filter(Boolean).reduce((value, key) => value?.[key], source)
  }

  function isTreeTableRuntime(cfg = {}, layoutType = '') {
    return !!cfg.options?.treeConfig && (layoutType || cfg.layoutType || 'simple-crud') !== 'tree-crud'
  }

  function resolveTreeLoadMode(treeConfig = {}) {
    return treeConfig.loadMode === 'lazy' ? 'lazy' : 'full'
  }

  function buildTreeTableProps(cfg = {}) {
    const treeConfig = cfg.options?.treeConfig || {}
    const loadMode = resolveTreeLoadMode(treeConfig)
    return {
      childrenKey: treeConfig.childrenField || 'children',
      defaultExpandAll: loadMode !== 'lazy',
      onLoad: loadMode === 'lazy' ? node => loadTreeTableChildren(node, cfg) : undefined,
    }
  }

  function buildRuntimeTableProps(cfg = {}) {
    const props = isTreeTableRuntime(cfg) ? buildTreeTableProps(cfg) : {}
    const rowGap = normalizeNumberOption(cfg.options?.tableRowGap, 8)
    const rowHeight = Math.max(34, 32 + rowGap)
    const rawRowProps = props.rowProps
    return {
      ...props,
      rowProps: (row, index) => {
        const base = typeof rawRowProps === 'function' ? rawRowProps(row, index) : {}
        const baseStyle = base.style && typeof base.style === 'object' && !Array.isArray(base.style) ? base.style : {}
        return {
          ...base,
          style: {
            ...baseStyle,
            height: `${rowHeight}px`,
          },
        }
      },
    }
  }

  async function loadTreeTableChildren(node, cfg = {}) {
    const treeConfig = cfg.options?.treeConfig || {}
    const treeApi = designPreview.value
      ? appendDesignPreviewToApiValue(cfg.apiConfig?.tree)
      : cfg.apiConfig?.tree
    if (!treeApi || !node)
      return
    const { method, url } = parseApiConfigValue(treeApi)
    const keyField = treeConfig.keyField || 'id'
    const parentValue = node[keyField] ?? node.key ?? node.targetValue
    const defaultSortParams = resolveDefaultSortParams(cfg.options?.defaultSort)
    try {
      const res = await request({
        method,
        url,
        params: {
          ...defaultSortParams,
          loadMode: 'lazy',
          parentValue,
        },
      })
      node[treeConfig.childrenField || 'children'] = normalizeTreeTableNodes(res?.data || [], treeConfig)
      if (!node[treeConfig.childrenField || 'children'].length)
        node.isLeaf = true
    }
    catch (error) {
      console.warn('[crud-page] 加载树形子节点失败', error)
      node.isLeaf = true
    }
  }

  function normalizeTreeTableNodes(nodes = [], treeConfig = {}) {
    if (!Array.isArray(nodes))
      return []
    const keyField = treeConfig.keyField || 'id'
    const childrenField = treeConfig.childrenField || 'children'
    return nodes.map((node) => {
      const children = Array.isArray(node?.[childrenField])
        ? normalizeTreeTableNodes(node[childrenField], treeConfig)
        : []
      const normalized = {
        ...(node || {}),
        key: node?.key ?? node?.[keyField],
      }
      if (children.length) {
        normalized[childrenField] = children
        normalized.isLeaf = false
      }
      else if (node?.isLeaf !== undefined) {
        normalized.isLeaf = !!node.isLeaf
      }
      return normalized
    })
  }

  async function prepareRuntimeList(list = [], options = {}) {
    const treeTable = !!options.treeTable
    const treeConfig = options.treeConfig || {}
    const normalizedList = treeTable ? normalizeTreeTableNodes(list, treeConfig) : list
    const objectCode = resolveBusinessObjectCode(renderConfig.value)
    if (!objectCode || !Array.isArray(normalizedList) || !normalizedList.length)
      return normalizedList
    await attachRuntimeActions(normalizedList, objectCode, treeConfig)
    return normalizedList
  }

  async function attachRuntimeActions(rows = [], objectCode, treeConfig = {}) {
    const items = collectRuntimeRows(rows, objectCode, treeConfig)
    if (!items.length)
      return

    const recordIds = [...new Set(
      items
        .map(item => item.recordId)
        .filter(recordId => recordId !== undefined && recordId !== null && recordId !== ''),
    )]
    if (!recordIds.length) {
      items.forEach(({ row }) => {
        row._runtimeActions = []
        row._documentRuntime = null
      })
      return
    }

    try {
      const res = await businessDocumentRuntimeBatch(objectCode, recordIds)
      const runtimeMap = normalizeRuntimeBatchMap(res?.data)
      items.forEach(({ row, recordId }) => {
        const runtime = runtimeMap.get(String(recordId)) || runtimeMap.get(recordId)
        row._runtimeActions = runtime?.runtimeActions || []
        row._documentRuntime = runtime || null
      })
    }
    catch (error) {
      items.forEach(({ row }) => {
        row._runtimeActions = []
        row._documentRuntime = null
      })
      console.warn('[crud-page] 批量加载单据运行态失败', error.message)
    }
  }

  function resolveRuntimeRecordId(row = {}) {
    const rowKey = renderConfig.value?.rowKey || 'id'
    return row[rowKey] ?? row.id ?? row.Id
  }

  function collectRuntimeRows(rows = [], objectCode, treeConfig = {}) {
    const childrenField = treeConfig.childrenField || 'children'
    const items = []
    const visit = (nodes = []) => {
      if (!Array.isArray(nodes))
        return
      nodes.forEach((row) => {
        if (!row || typeof row !== 'object')
          return
        row._runtimeObjectCode = objectCode
        const recordId = resolveRuntimeRecordId(row)
        if (recordId !== undefined && recordId !== null && recordId !== '')
          items.push({ row, recordId })
        if (Array.isArray(row[childrenField]) && row[childrenField].length)
          visit(row[childrenField])
      })
    }
    visit(rows)
    return items
  }

  function normalizeRuntimeBatchMap(data) {
    if (!data || typeof data !== 'object')
      return new Map()
    return new Map(Object.entries(data))
  }

  function firstRuntimeText(...values) {
    return values.map(value => String(value ?? '').trim()).find(Boolean) || ''
  }

  function resolveBusinessObjectCode(cfg = {}) {
    const options = cfg.options || {}
    const modelSchema = cfg.modelSchema || {}
    return cfg.businessObjectCode
      || cfg.targetObjectCode
      || cfg.referenceObjectCode
      || cfg.candidateObjectCode
      || options.businessObjectCode
      || options.targetObjectCode
      || options.referenceObjectCode
      || options.candidateObjectCode
      || modelSchema.objectCode
      || modelSchema.businessObjectCode
      || modelSchema.targetObjectCode
      || modelSchema.object?.code
      || modelSchema.object?.objectCode
      || cfg.objectCode
      || options.objectCode
      || modelSchema.modelCode
      || ''
  }

  function parseApiConfigValue(apiConfigValue) {
    const text = String(apiConfigValue || '')
    const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['get', text]
    return {
      method: String(method || 'get').toLowerCase(),
      url: urlParts.join('@') || text,
    }
  }

  function extractApiUrl(apiConfigValue) {
    if (!apiConfigValue)
      return ''
    const parts = String(apiConfigValue).split('@')
    return parts.length > 1 ? parts.slice(1).join('@') : apiConfigValue
  }

  function appendDesignPreviewToApiConfig(apiConfig = {}) {
    return Object.fromEntries(Object.entries(apiConfig || {}).map(([key, value]) => {
      return [key, appendDesignPreviewToApiValue(value)]
    }))
  }

  function appendDesignPreviewToApiValue(value) {
    if (!value || typeof value !== 'string' || value.includes('designPreview='))
      return value
    const separator = value.includes('?') ? '&' : '?'
    return `${value}${separator}designPreview=1`
  }

  function resolveRuntimeDetailRecordId() {
    return route.query?.recordId || route.query?.id || route.query?.[crudProps.value.rowKey || 'id'] || ''
  }

  function replaceRuntimeApiParams(url = '', params = {}) {
    let finalUrl = String(url || '')
    let hasPlaceholder = false
    Object.entries(params).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '')
        return
      const encoded = encodeURIComponent(String(value))
      if (finalUrl.includes(`:${key}`)) {
        finalUrl = finalUrl.replaceAll(`:${key}`, encoded)
        hasPlaceholder = true
      }
      if (finalUrl.includes(`{${key}}`)) {
        finalUrl = finalUrl.replaceAll(`{${key}}`, encoded)
        hasPlaceholder = true
      }
    })
    return { url: finalUrl, hasPlaceholder }
  }

  function resolveRuntimeDetailApi(recordId) {
    const cfg = renderConfig.value || {}
    const page = activeRuntimePage.value || {}
    const configuredDetailApi = page.detailApi || cfg.apiConfig?.detail || ''
    const rawApi = designPreview.value
      ? appendDesignPreviewToApiValue(configuredDetailApi)
      : configuredDetailApi
    if (!rawApi && cfg.api) {
      const detailUrl = `${cfg.api}/${encodeURIComponent(String(recordId))}`
      return {
        method: 'get',
        url: designPreview.value ? appendDesignPreviewToApiValue(detailUrl) : detailUrl,
        params: {},
      }
    }
    const parsed = parseApiConfigValue(rawApi)
    const rowKey = crudProps.value.rowKey || cfg.rowKey || 'id'
    const urlParams = {
      id: recordId,
      [rowKey]: recordId,
    }
    const { url, hasPlaceholder } = replaceRuntimeApiParams(parsed.url, urlParams)
    const method = String(parsed.method || page.detailMethod || 'get').toLowerCase()
    const params = {}
    let finalUrl = url
    if (!hasPlaceholder) {
      if (method === 'get') {
        finalUrl = `${url.replace(/\/$/, '')}/${encodeURIComponent(String(recordId))}`
      }
      else {
        params[rowKey] = recordId
      }
    }
    return { method, url: finalUrl, params }
  }

  function resolveRuntimeDetailData(payload, dataField = 'data') {
    if (!payload || typeof payload !== 'object')
      return payload || {}
    const field = String(dataField || '').trim()
    if (!field || field === '.' || field === '$')
      return payload
    return field.split('.').reduce((data, key) => {
      if (data && typeof data === 'object' && key in data)
        return data[key]
      return undefined
    }, payload) ?? payload.data ?? payload
  }

  async function loadRuntimeDetailRecord() {
    if (!configLoaded.value || formOnlyRuntime.value)
      return
    if (activeRuntimePageKey.value !== 'detail') {
      runtimeDetailRecord.value = {}
      return
    }
    const recordId = resolveRuntimeDetailRecordId()
    if (!recordId) {
      runtimeDetailRecord.value = {}
      return
    }
    runtimeDetailLoading.value = true
    try {
      const { method, url, params } = resolveRuntimeDetailApi(recordId)
      if (!url) {
        runtimeDetailRecord.value = {}
        return
      }
      const requestMethod = method === 'postencrypt' ? 'postEncrypt' : method
      const response = requestMethod === 'postEncrypt'
        ? await postEncrypt(url, {}, { params, needTip: false })
        : await request({ method: requestMethod, url, params, needTip: false })
      runtimeDetailRecord.value = resolveRuntimeDetailData(response, activeRuntimePage.value?.detailDataField || 'data') || {}
    }
    catch (error) {
      runtimeDetailRecord.value = {}
      console.warn('[crud-page] 加载详情页记录失败', error?.message || error)
    }
    finally {
      runtimeDetailLoading.value = false
    }
  }

  function resolveBaseRuntimeTitle(cfg = {}) {
    if (route.query?.title)
      return String(route.query.title)
    return cfg.menuName || cfg.appName || cfg.objectName || cfg.tableComment || cfg.configKey
  }

  function resolveRuntimeTitle(cfg = {}) {
    const baseTitle = resolveBaseRuntimeTitle(cfg)
    if (activeRuntimePageKey.value === 'list')
      return baseTitle

    const pageTitle = activeRuntimePage.value?.pageName
      || (activeRuntimePageKey.value === 'detail' ? '详情页' : activeRuntimePageKey.value)
    if (!baseTitle || baseTitle === pageTitle)
      return pageTitle
    return `${baseTitle} - ${pageTitle}`
  }

  function syncRuntimeTitle() {
    if (!props.syncDocumentTitle)
      return
    const title = resolveRuntimeTitle(renderConfig.value || {})
    if (!title)
      return
    route.meta.title = title
    document.title = `${title} | ${getDefaultPageTitle()}`
    tabStore.updateTabTitle(route.fullPath, title)
  }

  function normalizeConfigKey(value) {
    const key = Array.isArray(value) ? value[0] : value
    return String(key || '').trim()
  }

  function resolveRouteConfigKey() {
    const embeddedKey = normalizeConfigKey(props.runtimeConfig?.configKey)
    if (embeddedKey)
      return embeddedKey

    const paramKey = normalizeConfigKey(route.params?.configKey)
    if (paramKey)
      return paramKey

    const queryKey = normalizeConfigKey(route.query?.configKey)
    if (queryKey)
      return queryKey

    const prefix = '/ai/crud-page/'
    if (!route.path?.startsWith(prefix))
      return ''

    return normalizeConfigKey(decodeURIComponent(route.path.slice(prefix.length)))
  }

  function transformChildrenConfig(children = []) {
    return (children || []).map(child => ({
      ...child,
      fields: transformFields(child.fields || []),
    }))
  }

  /**
   * 预加载所有用到的字典数据
   */
  async function preloadDicts(cfg) {
    const types = new Set()

    // 从 columnsSchema 提取 dictType
    ;(cfg.columnsSchema || []).forEach((col) => {
      if (col.render?.dictType)
        types.add(col.render.dictType)
    })

    // 从 searchSchema / editSchema 提取 dictType
    ;[...(cfg.searchSchema || []), ...(cfg.editSchema || [])].forEach((field) => {
      if (field.dictType)
        types.add(field.dictType)
    })

    const children = cfg.options?.masterDetailConfig?.children || []
    children.forEach((child) => {
      ;(child.fields || []).forEach((field) => {
        if (field.dictType)
          types.add(field.dictType)
      })
    })

    for (const type of types) {
      if (!dictCache.value[type]) {
        try {
          dictCache.value[type] = await getDictData(type)
        }
        catch (e) {
          console.warn(`[crud-page] 加载字典 ${type} 失败`, e)
        }
      }
    }
  }

  async function loadConfig() {
    // 支持三种格式：
    // 1. /ai/crud-page/:configKey （route.params，unplugin-vue-router 动态路由）
    // 2. /ai/crud-page/order_manage （从 route.path 解析，permission.js 静态路由）
    // 3. /ai/crud-page?configKey=xxx （旧的 query 格式）
    const configKey = resolveRouteConfigKey()
    if (!configKey || configKey.startsWith('/')) {
      errorMsg.value = '缺少 configKey 参数'
      return
    }

    loading.value = true
    errorMsg.value = ''
    configLoaded.value = false

    try {
      const cfg = embeddedRuntime.value
        ? props.runtimeConfig
        : (await crudConfigRender(configKey, designPreview.value)).data
      if (!cfg || typeof cfg !== 'object')
        throw new Error('低代码运行配置为空')
      renderConfig.value = cfg
      // 动态页面的 Tab/浏览器标题以发布菜单名为准，避免再次点击 Tab 时回退成主模型名。
      syncRuntimeTitle()
      await preloadDicts(cfg)
      // 加载模板组件
      const layoutType = cfg.layoutType || 'simple-crud'
      const catalogEntry = catalog[layoutType]
      if (catalogEntry) {
        currentTemplate.value = defineAsyncComponent(catalogEntry.component)
      }
      else {
        // 未注册的模板，降级使用 AiCrudPage
        currentTemplate.value = null
      }
      configLoaded.value = true
      await loadRuntimeDetailRecord()
      scheduleInitialRuntimeAction()
    }
    catch (e) {
      errorMsg.value = e.message || '加载配置失败'
    }
    finally {
      loading.value = false
    }
  }

  async function scheduleInitialRuntimeAction() {
    if (!configLoaded.value)
      return
    if (formOnlyRuntime.value)
      return
    const mode = String(route.query?.mode || '').toLowerCase()
    if (!['create', 'detail'].includes(mode))
      return
    if (mode === 'detail' && activeRuntimePageKey.value !== 'list' && runtimeGridLayout.value)
      return
    const actionKey = `${route.fullPath}:${renderConfig.value?.configKey || ''}:${mode}`
    if (lastInitialActionKey.value === actionKey)
      return
    lastInitialActionKey.value = actionKey

    const crud = await waitRuntimeCrudRef()
    if (!crud)
      return
    if (mode === 'create') {
      crud.showAdd?.()
      return
    }
    const recordId = route.query?.recordId || route.query?.id
    if (recordId) {
      const rowKey = crudProps.value.rowKey || 'id'
      crud.showDetail?.({ [rowKey]: recordId, id: recordId })
    }
  }

  async function waitRuntimeCrudRef() {
    for (let i = 0; i < 10; i++) {
      await nextTick()
      const crud = runtimeCrudRef.value
      if (crud?.showAdd || crud?.showDetail)
        return crud
      await new Promise(resolve => window.setTimeout(resolve, 50))
    }
    return null
  }

  function handleRuntimeSubmitSuccess(payload = {}) {
    if (formOnlyRuntime.value)
      return
    if (payload.isEdit || String(route.query?.mode || '').toLowerCase() !== 'create')
      return
    const query = { ...route.query }
    delete query.mode
    router.replace({
      path: route.path,
      query,
      hash: route.hash,
    })
  }

  onMounted(() => {
    loadConfig()
  })

  // 监听 configKey 变化，兼容各种路由方式
  watch(
    () => resolveRouteConfigKey(),
    (newKey, oldKey) => {
      if (!embeddedRuntime.value && newKey && newKey !== oldKey) {
        loadConfig()
      }
    },
  )

  watch(
    () => props.runtimeConfig,
    (next, previous) => {
      if (next && next !== previous)
        loadConfig()
    },
    { deep: true },
  )

  watch(
    () => route.query?.mode,
    () => {
      scheduleInitialRuntimeAction()
    },
  )

  watch(
    () => [
      activeRuntimePageKey.value,
      route.query?.recordId || '',
      route.query?.id || '',
      route.query?._refresh || '',
      renderConfig.value?.configKey || '',
    ],
    () => {
      syncRuntimeTitle()
      loadRuntimeDetailRecord()
      scheduleInitialRuntimeAction()
    },
  )
  __impl.buildOfflineDraftConfig = buildOfflineDraftConfig
  __impl.resolveRuntimeFormOpenMode = resolveRuntimeFormOpenMode
  __impl.resolveRuntimeModalType = resolveRuntimeModalType
  __impl.resolveDefaultSortParams = resolveDefaultSortParams
  __impl.buildRuntimeFieldMetaMap = buildRuntimeFieldMetaMap
  __impl.applyRuntimeFieldMeta = applyRuntimeFieldMeta
  __impl.hasRuntimeFormulaConfig = hasRuntimeFormulaConfig
  __impl.normalizeNumberOption = normalizeNumberOption
  __impl.buildCrudHookHandlers = buildCrudHookHandlers
  __impl.buildFormGovernanceEventHandlers = buildFormGovernanceEventHandlers
  __impl.normalizeFormGovernanceHook = normalizeFormGovernanceHook
  __impl.composeRuntimeHookHandlers = composeRuntimeHookHandlers
  __impl.runFormGovernanceEvents = runFormGovernanceEvents
  __impl.runFormGovernanceEvent = runFormGovernanceEvent
  __impl.runWhitelistedFormScript = runWhitelistedFormScript
  __impl.applySetFieldValueEvent = applySetFieldValueEvent
  __impl.applyFormEventResultMapping = applyFormEventResultMapping
  __impl.getByPath = getByPath
  __impl.isTreeTableRuntime = isTreeTableRuntime
  __impl.resolveTreeLoadMode = resolveTreeLoadMode
  __impl.buildTreeTableProps = buildTreeTableProps
  __impl.buildRuntimeTableProps = buildRuntimeTableProps
  __impl.loadTreeTableChildren = loadTreeTableChildren
  __impl.normalizeTreeTableNodes = normalizeTreeTableNodes
  __impl.prepareRuntimeList = prepareRuntimeList
  __impl.attachRuntimeActions = attachRuntimeActions
  __impl.resolveRuntimeRecordId = resolveRuntimeRecordId
  __impl.collectRuntimeRows = collectRuntimeRows
  __impl.normalizeRuntimeBatchMap = normalizeRuntimeBatchMap
  __impl.firstRuntimeText = firstRuntimeText
  __impl.resolveBusinessObjectCode = resolveBusinessObjectCode
  __impl.parseApiConfigValue = parseApiConfigValue
  __impl.extractApiUrl = extractApiUrl
  __impl.appendDesignPreviewToApiConfig = appendDesignPreviewToApiConfig
  __impl.appendDesignPreviewToApiValue = appendDesignPreviewToApiValue
  __impl.resolveRuntimeDetailRecordId = resolveRuntimeDetailRecordId
  __impl.replaceRuntimeApiParams = replaceRuntimeApiParams
  __impl.resolveRuntimeDetailApi = resolveRuntimeDetailApi
  __impl.resolveRuntimeDetailData = resolveRuntimeDetailData
  __impl.loadRuntimeDetailRecord = loadRuntimeDetailRecord
  __impl.resolveBaseRuntimeTitle = resolveBaseRuntimeTitle
  __impl.resolveRuntimeTitle = resolveRuntimeTitle
  __impl.syncRuntimeTitle = syncRuntimeTitle
  __impl.normalizeConfigKey = normalizeConfigKey
  __impl.resolveRouteConfigKey = resolveRouteConfigKey
  __impl.transformChildrenConfig = transformChildrenConfig
  __impl.preloadDicts = preloadDicts
  __impl.loadConfig = loadConfig
  __impl.scheduleInitialRuntimeAction = scheduleInitialRuntimeAction
  __impl.waitRuntimeCrudRef = waitRuntimeCrudRef
  __impl.handleRuntimeSubmitSuccess = handleRuntimeSubmitSuccess

  return {
    ...deps, crudProps, appendDesignPreviewToApiConfig, appendDesignPreviewToApiValue, applyFormEventResultMapping,
    applyRuntimeFieldMeta, applySetFieldValueEvent, attachRuntimeActions, buildCrudHookHandlers,
    buildFormGovernanceEventHandlers, buildOfflineDraftConfig, buildRuntimeFieldMetaMap, buildRuntimeTableProps,
    buildTreeTableProps, collectRuntimeRows, composeRuntimeHookHandlers, extractApiUrl, firstRuntimeText, getByPath,
    handleRuntimeSubmitSuccess, hasRuntimeFormulaConfig, isTreeTableRuntime, loadConfig, loadRuntimeDetailRecord,
    loadTreeTableChildren, normalizeConfigKey, normalizeFormGovernanceHook, normalizeNumberOption,
    normalizeRuntimeBatchMap, normalizeTreeTableNodes, parseApiConfigValue, preloadDicts, prepareRuntimeList,
    replaceRuntimeApiParams, resolveBaseRuntimeTitle, resolveBusinessObjectCode, resolveDefaultSortParams,
    resolveRouteConfigKey, resolveRuntimeDetailApi, resolveRuntimeDetailData, resolveRuntimeDetailRecordId,
    resolveRuntimeFormOpenMode, resolveRuntimeModalType, resolveRuntimeRecordId, resolveRuntimeTitle,
    resolveTreeLoadMode, runFormGovernanceEvent, runFormGovernanceEvents, runWhitelistedFormScript,
    scheduleInitialRuntimeAction, syncRuntimeTitle, transformChildrenConfig, waitRuntimeCrudRef,
  }
}
