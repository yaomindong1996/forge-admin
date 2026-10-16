import { resolveCrudPagePresentation } from '../shared/runtime-crud-page-mode'
import {
  appendDesignPreviewToApiValue,
  applyTableColumnLayout,
  ensureManagedFlowStatusColumns,
  filterCrudItemsByFieldRefs,
  resolveColumnsByFieldRefs,
  includeCompiledChildColumnRefs,
  includeManagedRuntimeFieldRefs,
  isDesignPreviewCrudProps,
  normalizeTableRowGap,
  resolveCurrentConfigPlaceholder,
  resolveRuntimeBlockApi,
  shouldUseStaticCrudPreview,
} from '../shared/runtime-crud-props'
import {
  resolvePageSchemaSearchFieldRefs,
  resolvePageSchemaTableFieldRefs,
  resolvePreferredTableFieldRefs,
  resolveTableColumnFieldRefs,
} from './list-page-field-bindings'
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
 * 把列表设计器 searchFieldSettings（查询标签/方式/组件类型等）叠到运行 schema 上。
 * 运行配置 searchSchema 只提供字段目录与默认值；页面块覆盖必须优先生效，否则抽屉改完看起来「没保存」。
 */
export function applySearchFieldSettingsOverlay(schema = [], blockProps = {}) {
  const settings = blockProps?.searchFieldSettings && typeof blockProps.searchFieldSettings === 'object'
    ? blockProps.searchFieldSettings
    : {}
  if (!Object.keys(settings).length)
    return Array.isArray(schema) ? schema : []
  return (Array.isArray(schema) ? schema : []).map((item) => {
    const field = String(item?.field || item?.prop || item?.key || '').trim()
    const sourceField = String(item?.sourceField || '').trim()
    const setting = (sourceField && settings[sourceField])
      || (field && settings[field])
      || {}
    if (!Object.keys(setting).length)
      return item
    const componentType = String(setting.componentType || setting.type || '').trim()
    const next = { ...item }
    // 显式写过 label（含清空）才覆盖，避免空字符串被 || 吃掉后看起来「改了没生效」
    if (Object.prototype.hasOwnProperty.call(setting, 'label')) {
      const label = String(setting.label || '').trim()
      if (label)
        next.label = label
    }
    if (setting.queryType)
      next.queryType = setting.queryType
    if (Object.prototype.hasOwnProperty.call(setting, 'placeholder')) {
      const placeholder = String(setting.placeholder || '').trim()
      if (placeholder)
        next.placeholder = placeholder
    }
    if (componentType) {
      next.type = componentType
      next.componentType = componentType
    }
    if (setting.dictType)
      next.dictType = setting.dictType
    if (setting.optionSource || setting.props?.optionSource) {
      const optionSource = setting.optionSource || setting.props?.optionSource
      next.optionSource = optionSource
      next.props = {
        ...(item.props || {}),
        ...(setting.props || {}),
        optionSource,
      }
    }
    return next
  })
}

function indexCrudItemsByFieldKey(items = []) {
  const byKey = new Map()
  ;(Array.isArray(items) ? items : []).forEach((item) => {
    const key = String(item?.prop || item?.field || item?.key || item?.dataIndex || '').trim()
    if (key && !byKey.has(key))
      byKey.set(key, item)
    const sourceField = String(item?.sourceField || '').trim()
    if (sourceField && !byKey.has(sourceField))
      byKey.set(sourceField, item)
  })
  return byKey
}

/**
 * 按 searchFieldRefs 合并运行配置与画布本地 schema：
 * 每个 ref 优先用 runtime，缺列时用 aiSearchSchema 补齐，避免设计器勾了 ID、预览却被运行配置吃掉。
 */
export function mergeSearchSchemaByFieldRefs(primary = [], fallback = [], fieldRefs = []) {
  const refs = (Array.isArray(fieldRefs) ? fieldRefs : []).map(ref => String(ref || '').trim()).filter(Boolean)
  if (!refs.length)
    return []
  const primaryMap = indexCrudItemsByFieldKey(primary)
  const fallbackMap = indexCrudItemsByFieldKey(fallback)
  return refs.map((ref) => {
    const item = primaryMap.get(ref) || fallbackMap.get(ref)
    if (!item)
      return null
    return {
      ...item,
      field: item.field || item.prop || ref,
      sourceField: item.sourceField || ref,
    }
  }).filter(Boolean)
}

/**
 * 查询条件：显式 searchFieldRefs 时按 refs 排序合并 runtime + 画布 schema；
 * 空 refs 表示明确不展示查询项。最后叠 searchFieldSettings，保证设计器覆盖生效。
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
    return applySearchFieldSettingsOverlay(
      mergeSearchSchemaByFieldRefs(runtimeSearchSchema, aiSearchSchema, refs),
      blockProps,
    )
  }
  return applySearchFieldSettingsOverlay(
    filterCrudItemsByFieldRefs(
      Array.isArray(runtimeSearchSchema) && runtimeSearchSchema.length
        ? runtimeSearchSchema
        : (Array.isArray(aiSearchSchema) ? aiSearchSchema : []),
      configuredFieldRefs,
    ),
    blockProps,
  )
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
  // 自由布局裸 CRUD：区块属性面板是表单 chrome 的编辑入口，优先于对象 runtime
  preferBlockFormChrome = false,
}) {
  if (!runtimeProps) {
    return null
  }
  const formChromeOpts = { preferBlockFormChrome: preferBlockFormChrome === true }
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
  const columnSource = Array.isArray(runtimeProps.columns) && runtimeProps.columns.length
    ? runtimeProps.columns
    : aiTableColumns
  // 仅作「查询污染」修理兜底，不能拿来覆盖用户勾选（否则会多出 ID/创建时间/更新时间）
  const defaultTableFieldRefs = (Array.isArray(runtimeProps.fieldCatalog) ? runtimeProps.fieldCatalog : [])
    .filter(field => field?.field && field.listVisible === true)
    .map(field => field.field)
  const searchFieldRefs = Array.isArray(blockProps.searchFieldRefs) && blockProps.searchFieldRefs.length
    ? blockProps.searchFieldRefs
    : resolvePageSchemaSearchFieldRefs(runtimeProps.pageSchema)
  const pageSchemaTableRefs = resolvePageSchemaTableFieldRefs(
    runtimeProps.pageSchema,
    defaultTableFieldRefs,
  )
  // 编辑勾选（block.fieldRefs）优先；仅污染或未勾选时才用 pageSchema 表格区
  const preferredFieldRefs = resolvePreferredTableFieldRefs({
    configuredFieldRefs,
    pageSchemaTableRefs,
    searchFieldRefs,
  })
  const tableColumnFieldRefs = resolveTableColumnFieldRefs({
    fieldRefs: preferredFieldRefs,
    searchFieldRefs,
    columns: columnSource,
    designPreview,
    defaultTableFieldRefs,
  })
  const runtimeTableFieldRefs = includeCompiledChildColumnRefs(
    includeManagedRuntimeFieldRefs(
      tableColumnFieldRefs,
      runtimeProps.fieldCatalog,
      blockProps.fieldSettings,
    ),
    columnSource,
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
    emptyTitle: blockProps.emptyTitle || runtimeProps.emptyTitle,
    emptyDescription: blockProps.emptyDescription || blockProps.emptyText || runtimeProps.emptyDescription || runtimeProps.emptyText,
    emptyText: blockProps.emptyText || blockProps.emptyDescription || runtimeProps.emptyText || runtimeProps.emptyDescription,
    emptyFilterTitle: blockProps.emptyFilterTitle || runtimeProps.emptyFilterTitle,
    emptyFilterDescription: blockProps.emptyFilterDescription || runtimeProps.emptyFilterDescription,
    // 列表列：设计预览跟 tableZone columns；运行态跟修理后的 fieldRefs，缺列从字段目录合成
    columns: ensureManagedFlowStatusColumns(
      applyTableColumnLayout(
        resolveColumnsByFieldRefs(
          columnSource,
          runtimeTableFieldRefs,
          runtimeProps.fieldCatalog,
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
    // 列表 fieldRefs 只约束表格列；绑定页面表单后不能再用列表字段把新增/编辑裁回旧表单
    editSchema: hydrateRuntimeFormLayout(
      filterCrudItemsByFieldRefs(
        runtimeProps.editSchema?.length ? runtimeProps.editSchema : aiFormSchema,
        (runtimeProps.pageFormAssetBound === true || String(blockProps.formAssetId || '').trim())
          ? []
          : configuredFieldRefs,
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
    modalWidth: preferBlockFormChrome
      ? (blockProps.modalWidth || runtimeProps.modalWidth)
      : (runtimeProps.modalWidth || blockProps.modalWidth),
    detailModalWidth: preferBlockFormChrome
      ? (blockProps.detailModalWidth || runtimeProps.detailModalWidth)
      : (runtimeProps.detailModalWidth || blockProps.detailModalWidth),
    formOpenMode: resolveEffectiveFormOpenMode(blockProps, runtimeProps, formChromeOpts),
    tabWorkspace: runtimeProps.tabWorkspace?.maxTabs ? runtimeProps.tabWorkspace : (blockProps.tabWorkspace || runtimeProps.tabWorkspace),
    modalType: resolveEffectiveModalType(blockProps, runtimeProps, formChromeOpts),
    drawerPlacement: preferBlockFormChrome
      ? (blockProps.drawerPlacement || runtimeProps.drawerPlacement)
      : (runtimeProps.drawerPlacement || blockProps.drawerPlacement),
    hideModalFooter: blockProps.hideModalFooter ?? runtimeProps.hideModalFooter,
    hideDefaultDetailContent: blockProps.hideDefaultDetailContent ?? runtimeProps.hideDefaultDetailContent,
    hideToolbar: blockProps.hideToolbar ?? runtimeProps.hideToolbar,
    hideAdd: blockProps.hideAdd ?? runtimeProps.hideAdd,
    hideBatchDelete: blockProps.hideBatchDelete ?? runtimeProps.hideBatchDelete,
    // 设计态也要跟开关一致，否则属性面板打开了画布却看不到「更多/导入导出/自定义查询」
    showImport: blockProps.showImport ?? runtimeProps.showImport ?? true,
    showExport: blockProps.showExport ?? runtimeProps.showExport ?? true,
    // 与属性面板 isAiCrudToolbarSwitchOn（!== false）默认亮起对齐
    showExportTasks: blockProps.showExportTasks ?? runtimeProps.showExportTasks ?? true,
    enableCustomQuery: blockProps.enableCustomQuery ?? runtimeProps.enableCustomQuery ?? true,
    customQueryConfigKey: blockProps.customQueryConfigKey
      || runtimeProps.customQueryConfigKey
      || runtimeConfigKey
      || '',
    exportTaskConfigKey: blockProps.exportTaskConfigKey
      || runtimeProps.exportTaskConfigKey
      || runtimeConfigKey
      || '',
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

export function resolveEffectiveFormOpenMode(blockProps = {}, runtimeProps = {}, options = {}) {
  const blockMode = normalizeFormOpenMode(blockProps.formOpenMode)
  const runtimeMode = normalizeFormOpenMode(runtimeProps.formOpenMode)
  const preferBlock = options.preferBlockFormChrome === true
  // 默认：表单设计器 layout 是单一事实来源；自由布局裸 CRUD 则区块优先
  if (preferBlock) {
    if (blockMode)
      return blockMode
    if (runtimeMode)
      return runtimeMode
  }
  else if (runtimeMode) {
    return runtimeMode
  }
  else if (blockMode) {
    return blockMode
  }
  if (preferBlock) {
    return normalizeModalType(blockProps.modalType) || normalizeModalType(runtimeProps.modalType) || 'modal'
  }
  return normalizeModalType(runtimeProps.modalType) || normalizeModalType(blockProps.modalType) || 'modal'
}

export function resolveEffectiveModalType(blockProps = {}, runtimeProps = {}, options = {}) {
  const formOpenMode = resolveEffectiveFormOpenMode(blockProps, runtimeProps, options)
  if (['modal', 'drawer'].includes(formOpenMode)) {
    return formOpenMode
  }
  if (options.preferBlockFormChrome === true) {
    return normalizeModalType(blockProps.modalType) || normalizeModalType(runtimeProps.modalType) || 'modal'
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
