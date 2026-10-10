import { resolveCrudPagePresentation } from '../shared/runtime-crud-page-mode'
import {
  appendDesignPreviewToApiValue,
  applyTableColumnLayout,
  ensureManagedFlowStatusColumns,
  filterCrudItemsByFieldRefs,
  includeCompiledChildColumnRefs,
  includeManagedRuntimeFieldRefs,
  isDesignPreviewCrudProps,
  normalizeTableRowGap,
  resolveCurrentConfigPlaceholder,
  resolveRuntimeBlockApi,
  shouldUseStaticCrudPreview,
} from '../shared/runtime-crud-props'
import { hydrateRuntimeFormLayout } from '../shared/runtime-form-layout'
import { applyCrudHookRules, CRUD_HOOK_RULE_TARGETS, normalizeCrudHookRules } from './crud-hook-rules'

/**
 * 左树右表运行配置会显式传 false；区块上残留的 true 不能盖掉。
 */
function resolveEnableTreeAddChild(blockProps = {}, runtimeProps = {}) {
  if (runtimeProps.enableTreeAddChild === false || blockProps.enableTreeAddChild === false)
    return false
  if (runtimeProps.enableTreeAddChild === true)
    return true
  return blockProps.enableTreeAddChild === true
}

/**
 * 查询条件：显式 searchFieldRefs 时优先用运行配置 searchSchema（草稿/发布）按 refs 过滤排序；
 * 只有运行配置为空时才回落画布本地 aiSearchSchema。空 refs 表示明确不展示查询项。
 */
export function resolveRuntimeSearchSchema({
  hasExplicitSearchFieldRefs = false,
  blockProps = {},
  runtimeSearchSchema = [],
  aiSearchSchema = [],
  configuredFieldRefs = [],
} = {}) {
  if (hasExplicitSearchFieldRefs) {
    const refs = Array.isArray(blockProps.searchFieldRefs) ? blockProps.searchFieldRefs.filter(Boolean) : []
    if (!refs.length)
      return []
    const source = Array.isArray(runtimeSearchSchema) && runtimeSearchSchema.length
      ? runtimeSearchSchema
      : (Array.isArray(aiSearchSchema) ? aiSearchSchema : [])
    return orderCrudItemsByFieldRefs(filterCrudItemsByFieldRefs(source, refs), refs)
  }
  return filterCrudItemsByFieldRefs(
    Array.isArray(runtimeSearchSchema) && runtimeSearchSchema.length
      ? runtimeSearchSchema
      : (Array.isArray(aiSearchSchema) ? aiSearchSchema : []),
    configuredFieldRefs,
  )
}

function orderCrudItemsByFieldRefs(items = [], fieldRefs = []) {
  if (!Array.isArray(items) || !items.length || !Array.isArray(fieldRefs) || !fieldRefs.length)
    return Array.isArray(items) ? items : []
  const byKey = new Map()
  items.forEach((item) => {
    const key = String(item?.prop || item?.field || item?.key || item?.dataIndex || '').trim()
    if (key && !byKey.has(key))
      byKey.set(key, item)
  })
  const ordered = fieldRefs
    .map(ref => byKey.get(String(ref)))
    .filter(Boolean)
  // 保留非字段项（如操作列），一般查询 schema 不会有
  const used = new Set(ordered.map(item => String(item?.prop || item?.field || item?.key || item?.dataIndex || '').trim()))
  items.forEach((item) => {
    const key = String(item?.prop || item?.field || item?.key || item?.dataIndex || '').trim()
    if (!key || used.has(key))
      return
    if (item?.type === 'action' || item?.fixed === 'right' || key === 'action')
      ordered.push(item)
  })
  return ordered
}

/** 合并对象运行配置与当前区块配置；不修改任一输入或共享缓存。 */
export function buildRuntimeCrudBlockProps({
  runtimeProps,
  blockProps = {},
  runtimeInteractive = false,
  extensionHooks = {},
  configuredFieldRefs = [],
  blockApiConfig = {},
  aiTableColumns = [],
  aiSearchSchema = [],
  aiFormSchema = [],
  hasExplicitSearchFieldRefs = false,
  designerCrudPublicParams = {},
  preventStaticCrudSubmit,
  extensionRuntimeApi,
}) {
  if (!runtimeProps) {
    return null
  }
  const designPreview = isDesignPreviewCrudProps(runtimeProps)
  // 运行页 runtimeInteractive 为 true 时必须允许提交。designPreview 只表示读草稿配置，
  // 不能再和“未开真实数据预览”一起当成静态画布，否则有编辑权限的运行页无法新增。
  const staticDesignPreview = designPreview && shouldUseStaticCrudPreview({
    blockType: 'AiCrudPage',
    runtimeInteractive,
    previewLiveData: blockProps.previewLiveData === true,
    hasConfiguredRequest: true,
  })
  const rules = normalizeCrudHookRules(blockProps.crudHookRules || {}, blockProps.beforeSubmitRules || [])
  const hookHandlers = CRUD_HOOK_RULE_TARGETS.reduce((handlers, target) => {
    const list = (rules[target.value] || []).filter(rule => rule.field)
    if (list.length) {
      handlers[target.value] = data => applyCrudHookRules(data, list)
    }
    return handlers
  }, {})
  const runtimeConfigKey = runtimeProps.configKey || ''
  const runtimeTableFieldRefs = includeCompiledChildColumnRefs(
    includeManagedRuntimeFieldRefs(
      configuredFieldRefs,
      runtimeProps.fieldCatalog,
      blockProps.fieldSettings,
    ),
    runtimeProps.columns,
  )
  const runtimeBlockApi = resolveRuntimeBlockApi(blockProps.api, runtimeConfigKey, designPreview)
  const runtimeBlockApiConfig = Object.fromEntries(Object.entries(blockApiConfig)
    .map(([key, value]) => {
      const resolved = resolveCurrentConfigPlaceholder(value, runtimeConfigKey)
      return [key, designPreview ? appendDesignPreviewToApiValue(resolved) : resolved]
    })
    .filter(([, value]) => value))
  // 嵌入式树表：仅显式 enabled 时保留 /tree；残留 showPagination=false + tree 不能误改写
  const treeEnabled = runtimeProps.treeConfig?.enabled === true
    || runtimeProps.treeConfig?.enabled === 1
    || runtimeProps.treeConfig?.enabled === '1'
    || runtimeProps.treeConfig?.enabled === 'true'
    || runtimeProps.options?.treeConfig?.enabled === true
    || runtimeProps.options?.treeConfig?.enabled === 1
    || runtimeProps.options?.treeConfig?.enabled === '1'
    || runtimeProps.options?.treeConfig?.enabled === 'true'
  if (treeEnabled && runtimeProps.showPagination === false && runtimeProps.apiConfig?.tree) {
    const treeList = runtimeProps.apiConfig.list || runtimeProps.apiConfig.tree
    if (treeList)
      runtimeBlockApiConfig.list = treeList
  }
  return {
    ...runtimeProps,
    ...resolveCrudPagePresentation(blockProps, runtimeProps),
    ...hookHandlers,
    ...(typeof extensionHooks.beforeSubmit === 'function'
      ? {
          beforeSubmit: data => extensionHooks.beforeSubmit(
            hookHandlers.beforeSubmit ? hookHandlers.beforeSubmit(data) : data,
            extensionRuntimeApi(),
          ),
        }
      : {}),
    ...(typeof extensionHooks.afterSubmit === 'function'
      ? { afterSubmit: payload => extensionHooks.afterSubmit(payload, extensionRuntimeApi()) }
      : {}),
    ...(typeof extensionHooks.formChange === 'function'
      ? { formChange: payload => extensionHooks.formChange(payload, extensionRuntimeApi()) }
      : {}),
    ...(typeof extensionHooks.beforeRowAction === 'function'
      ? { beforeRowAction: payload => extensionHooks.beforeRowAction(payload, extensionRuntimeApi()) }
      : {}),
    ...(staticDesignPreview ? { beforeSubmit: preventStaticCrudSubmit } : {}),
    lazy: staticDesignPreview,
    api: runtimeBlockApi || runtimeProps.api || '',
    rowKey: blockProps.rowKey || runtimeProps.rowKey || 'id',
    title: blockProps.title || runtimeProps.title,
    columns: ensureManagedFlowStatusColumns(
      applyTableColumnLayout(
        filterCrudItemsByFieldRefs(
          runtimeProps.columns?.length ? runtimeProps.columns : aiTableColumns,
          runtimeTableFieldRefs,
        ),
        blockProps,
      ),
      runtimeProps.fieldCatalog,
      blockProps.fieldSettings,
    ),
    runtimeActions: Array.isArray(runtimeProps.runtimeActions)
      ? runtimeProps.runtimeActions
      : [],
    toolbarActions: Array.isArray(blockProps.toolbarActions)
      ? blockProps.toolbarActions
      : (runtimeProps.toolbarActions || []),
    detailActions: Array.isArray(blockProps.detailActions)
      ? blockProps.detailActions
      : (runtimeProps.detailActions || []),
    formActions: Array.isArray(blockProps.formActions)
      ? blockProps.formActions
      : (runtimeProps.formActions || []),
    businessObjectCode: runtimeProps.businessObjectCode || runtimeProps.objectCode || '',
    searchSchema: resolveRuntimeSearchSchema({
      hasExplicitSearchFieldRefs,
      blockProps,
      runtimeSearchSchema: runtimeProps.searchSchema,
      aiSearchSchema,
      configuredFieldRefs,
    }),
    editSchema: hydrateRuntimeFormLayout(
      filterCrudItemsByFieldRefs(
        runtimeProps.editSchema?.length ? runtimeProps.editSchema : aiFormSchema,
        configuredFieldRefs,
      ),
      runtimeProps.options?.editFormLayout,
      runtimeProps.uiDocument,
    ),
    apiConfig: {
      ...(runtimeProps.apiConfig || {}),
      ...runtimeBlockApiConfig,
    },
    showSearch: blockProps.showSearch ?? runtimeProps.showSearch,
    // 嵌入式树表运行配置会关掉分页；区块默认 true 不能盖掉
    showPagination: runtimeProps.showPagination === false
      ? false
      : (blockProps.showPagination ?? runtimeProps.showPagination),
    searchGridCols: blockProps.searchGridCols || runtimeProps.searchGridCols,
    searchLabelWidth: blockProps.searchLabelWidth || runtimeProps.searchLabelWidth,
    searchEnableCollapse: blockProps.searchEnableCollapse ?? runtimeProps.searchEnableCollapse,
    searchMaxVisibleFields: blockProps.searchMaxVisibleFields || runtimeProps.searchMaxVisibleFields,
    searchYGap: blockProps.searchYGap ?? runtimeProps.searchYGap,
    // 表单设计器 layout 是表单项配置的单一事实来源，优先于页面区块旧值；
    // 仅在区块有显式覆盖且设计器未配置时才回落 blockProps
    editGridCols: runtimeProps.editGridCols || blockProps.editGridCols,
    editLabelWidth: runtimeProps.editLabelWidth || blockProps.editLabelWidth,
    editLabelPlacement: runtimeProps.editLabelPlacement || blockProps.editLabelPlacement,
    editLabelAlign: runtimeProps.editLabelAlign || blockProps.editLabelAlign,
    editSize: runtimeProps.editSize || blockProps.editSize,
    editShowFeedback: runtimeProps.editShowFeedback ?? blockProps.editShowFeedback,
    editXGap: runtimeProps.editXGap ?? blockProps.editXGap,
    editYGap: runtimeProps.editYGap ?? blockProps.editYGap,
    tableRowGap: normalizeTableRowGap(runtimeProps.tableRowGap ?? blockProps.rowGap, 8),
    modalWidth: runtimeProps.modalWidth || blockProps.modalWidth,
    detailModalWidth: runtimeProps.detailModalWidth || blockProps.detailModalWidth,
    formOpenMode: resolveEffectiveFormOpenMode(blockProps, runtimeProps),
    tabWorkspace: runtimeProps.tabWorkspace?.maxTabs ? runtimeProps.tabWorkspace : (blockProps.tabWorkspace || runtimeProps.tabWorkspace),
    modalType: resolveEffectiveModalType(blockProps, runtimeProps),
    drawerPlacement: runtimeProps.drawerPlacement || blockProps.drawerPlacement,
    hideModalFooter: blockProps.hideModalFooter ?? runtimeProps.hideModalFooter,
    hideDefaultDetailContent: blockProps.hideDefaultDetailContent ?? runtimeProps.hideDefaultDetailContent,
    hideToolbar: blockProps.hideToolbar ?? runtimeProps.hideToolbar,
    hideAdd: blockProps.hideAdd ?? runtimeProps.hideAdd,
    hideBatchDelete: blockProps.hideBatchDelete ?? runtimeProps.hideBatchDelete,
    showImport: staticDesignPreview ? false : (blockProps.showImport ?? runtimeProps.showImport ?? true),
    showExport: staticDesignPreview ? false : (blockProps.showExport ?? runtimeProps.showExport ?? true),
    showExportTasks: staticDesignPreview ? false : (blockProps.showExportTasks ?? runtimeProps.showExportTasks),
    enableCustomQuery: staticDesignPreview ? false : (blockProps.enableCustomQuery ?? runtimeProps.enableCustomQuery ?? true),
    addButtonText: blockProps.addButtonText || runtimeProps.addButtonText,
    exportButtonText: blockProps.exportButtonText || runtimeProps.exportButtonText,
    exportFileName: blockProps.exportFileName || runtimeProps.exportFileName,
    renderMode: blockProps.renderMode || runtimeProps.renderMode,
    showRenderModeSwitch: blockProps.showRenderModeSwitch ?? runtimeProps.showRenderModeSwitch,
    enableTreeAddChild: resolveEnableTreeAddChild(blockProps, runtimeProps),
    tableSize: blockProps.tableSize || runtimeProps.tableSize,
    bordered: blockProps.bordered ?? runtimeProps.bordered,
    striped: blockProps.striped ?? runtimeProps.striped,
    hideSelection: blockProps.hideSelection ?? runtimeProps.hideSelection,
    maxHeight: blockProps.maxHeight || runtimeProps.maxHeight,
    scrollX: blockProps.scrollX || runtimeProps.scrollX,
    resizable: blockProps.resizable ?? runtimeProps.resizable,
    expandConfig: blockProps.expandConfig || runtimeProps.expandConfig || {},
    detailPanels: blockProps.detailPanels || runtimeProps.detailPanels || [],
    showDataChangeLog: blockProps.showDataChangeLog === true || runtimeProps.showDataChangeLog === true,
    dataAuditObjectId: blockProps.dataAuditObjectId || runtimeProps.dataAuditObjectId || '',
    listMethod: blockProps.listMethod || runtimeProps.listMethod,
    listDataField: blockProps.listDataField || runtimeProps.listDataField,
    listTotalField: blockProps.listTotalField || runtimeProps.listTotalField,
    isEncrypt: blockProps.isEncrypt ?? runtimeProps.isEncrypt,
    publicParams: {
      ...(runtimeProps.publicParams || {}),
      ...designerCrudPublicParams,
    },
    publicQuery: {
      ...(runtimeProps.publicQuery || {}),
      ...(blockProps.publicQuery || {}),
    },
    formDefaultValues: {
      ...(runtimeProps.formDefaultValues || {}),
      ...(blockProps.formDefaultValues || {}),
    },
    submitDefaultParams: {
      ...(runtimeProps.submitDefaultParams || {}),
      ...(blockProps.submitDefaultParams || {}),
    },
  }
}

export function resolveEffectiveFormOpenMode(blockProps = {}, runtimeProps = {}) {
  const blockMode = normalizeFormOpenMode(blockProps.formOpenMode)
  const runtimeMode = normalizeFormOpenMode(runtimeProps.formOpenMode)
  // 表单设计器 layout 是单一事实来源：runtimeMode 有值时直接生效，仅在未配置时才看区块覆盖
  if (runtimeMode) {
    return runtimeMode
  }
  if (blockMode) {
    return blockMode
  }
  return normalizeModalType(runtimeProps.modalType) || normalizeModalType(blockProps.modalType) || 'modal'
}

export function resolveEffectiveModalType(blockProps = {}, runtimeProps = {}) {
  const formOpenMode = resolveEffectiveFormOpenMode(blockProps, runtimeProps)
  if (['modal', 'drawer'].includes(formOpenMode)) {
    return formOpenMode
  }
  return normalizeModalType(runtimeProps.modalType) || normalizeModalType(blockProps.modalType) || 'modal'
}

function normalizeFormOpenMode(value) {
  const mode = String(value || '').trim()
  if (mode === 'tabWorkspace' || mode.toLowerCase() === 'tabworkspace') {
    return 'tabWorkspace'
  }
  const normalized = mode.toLowerCase()
  return ['modal', 'drawer', 'flat'].includes(normalized) ? normalized : ''
}

function normalizeModalType(value) {
  const normalized = String(value || '').trim().toLowerCase()
  return ['modal', 'drawer'].includes(normalized) ? normalized : ''
}
