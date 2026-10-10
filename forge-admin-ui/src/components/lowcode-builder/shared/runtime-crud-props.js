/**
 * 将已发布的 CRUD 配置转换为 AiCrudPage 的运行参数。
 *
 * 列表设计器预览和应用页面都使用这份轻量桥接：接口、表单和列表字段始终
 * 来自同一个 configKey，应用设计器只允许覆盖外观与局部行为，不另存一套接口。
 *
 * 表单布局：设计态 formDesignerSchema → 统一 uiDocument → AiForm schema，
 * 与审批端 TaskFormUiDocumentCompiler 同构，保证设计/渲染/后续 H5 协议一致。
 */
import {
  compileUiDocumentFromDesigner,
  UI_DOCUMENT_PROTOCOL_VERSION,
} from '@/protocols/ui-document'
import { applyEmbeddedTreeTableRuntimeProps } from './runtime-tree-table'

export function buildRuntimeCrudProps(config = {}, { designPreview = false } = {}) {
  const options = config.options || {}
  // 表单设计器保存的 layout 是表单项配置的单一事实来源，优先于运行配置的平铺键
  const fdsSource = options.formDesignerSchema || config.formDesignerSchema
  const designerLayout = resolveDesignerFormLayout(fdsSource)
  const formOpenMode = resolveFormOpenMode(options, config, designerLayout)
  const governance = resolveDesignerFormGovernance(fdsSource)
  const configKey = String(config.configKey || '').trim()
  const apiConfig = ensureRuntimeApiConfig(
    normalizeApiConfig(config.apiConfig, configKey, designPreview),
    configKey,
    designPreview,
  )
  const flatEditSchema = mergeDesignerEditSchema(
    normalizeFields(config.editSchema),
    fdsSource,
  )
  const uiDocument = fdsSource
    ? compileUiDocumentFromDesigner(fdsSource, { resolvedFields: flatEditSchema })
    : null
  const baseProps = {
    searchSchema: normalizeFields(config.searchSchema),
    columns: normalizeColumns(config.columnsSchema, config.transConfig),
    // 平铺 fields 保留给 fieldRefs 过滤；布局由 uiDocument / hydrateRuntimeFormLayout 合成
    editSchema: flatEditSchema,
    fieldCatalog: buildRuntimeFieldCatalog(config),
    childrenConfig: options.masterDetailConfig?.children || [],
    expandConfig: options.expandConfig || config.expandConfig || {},
    detailPanels: options.detailPanels || config.detailPanels || [],
    apiConfig,
    // 兜底根路径，避免 apiConfig.create 缺失时 POST 打到 axios baseURL（/dev-api）
    api: configKey ? `/ai/crud/${configKey}` : '',
    configKey,
    designPreview,
    layoutType: config.layoutType || options.layoutType || 'simple-crud',
    options,
    rowKey: config.rowKey || 'id',
    formOnly: options.formOnly ?? config.formOnly ?? false,
    formOnlyTitle: options.formOnlyTitle ?? config.formOnlyTitle,
    formOnlySubmitText: options.formOnlySubmitText ?? config.formOnlySubmitText,
    formOnlySuccessTitle: options.formOnlySuccessTitle ?? config.formOnlySuccessTitle,
    formOnlySuccessDescription: options.formOnlySuccessDescription ?? config.formOnlySuccessDescription,
    formOpenMode,
    tabWorkspace: options.tabWorkspace || config.tabWorkspace || {},
    modalType: resolveModalType(formOpenMode, options, config, designerLayout),
    modalWidth: designerLayout.modalWidth || options.modalWidth || config.modalWidth || '800px',
    detailModalWidth: designerLayout.detailModalWidth || options.detailModalWidth || config.detailModalWidth || 'min(1080px, 92vw)',
    drawerPlacement: designerLayout.drawerPlacement || options.drawerPlacement || config.drawerPlacement || 'right',
    editGridCols: designerLayout.gridColumns || options.editGridCols || config.editGridCols || 1,
    editLabelWidth: designerLayout.labelWidth || options.editLabelWidth || config.editLabelWidth || 'auto',
    editLabelPlacement: designerLayout.labelPlacement || options.editLabelPlacement || config.editLabelPlacement || 'left',
    editLabelAlign: designerLayout.labelAlign || options.editLabelAlign || config.editLabelAlign || 'right',
    editSize: designerLayout.size || options.editSize || config.editSize || 'medium',
    editEnableCollapse: designerLayout.enableCollapse ?? options.editEnableCollapse ?? config.editEnableCollapse ?? false,
    editMaxVisibleFields: numberOption(designerLayout.maxVisibleFields ?? options.editMaxVisibleFields ?? config.editMaxVisibleFields, 6),
    editShowFeedback: designerLayout.showFeedback ?? options.editShowFeedback ?? config.editShowFeedback ?? true,
    editFormClass: designerLayout.formClass || options.editFormClass || config.editFormClass || '',
    editFormStyle: designerLayout.formStyle || options.editFormStyle || config.editFormStyle,
    formAssets: options.formAssets || config.formAssets || [],
    // 表单设计器「自动化」配置（字段事件 / 初始化默认值）随运行配置下发，
    // 与 views/ai/crud-page.vue 的 buildRuntimeFormProfile 产出保持同构。
    fieldEvents: Array.isArray(governance.fieldEvents) ? governance.fieldEvents : [],
    formInit: governance.formInit && typeof governance.formInit === 'object' ? governance.formInit : {},
    editXGap: numberOption(designerLayout.columnGap ?? options.editXGap ?? config.editXGap, 12),
    editYGap: numberOption(designerLayout.rowGap ?? options.editYGap ?? config.editYGap, 8),
    tableRowGap: normalizeTableRowGap(options.tableRowGap ?? config.tableRowGap, 8),
    loadDetailOnEdit: options.loadDetailOnEdit ?? config.loadDetailOnEdit ?? true,
    searchGridCols: options.searchGridCols || config.searchGridCols || 4,
    showSearch: options.showSearch ?? config.showSearch ?? true,
    showPagination: options.showPagination ?? config.showPagination ?? true,
    hideAdd: options.hideAdd ?? config.hideAdd ?? false,
    hideBatchDelete: options.hideBatchDelete ?? config.hideBatchDelete ?? false,
    showImport: options.showImport ?? config.showImport ?? false,
    showExport: options.showExport ?? config.showExport ?? false,
    enableCustomQuery: options.enableCustomQuery ?? config.enableCustomQuery ?? true,
    customQueryConfigKey: config.configKey || '',
    // 左树右表：右表未开嵌入式树时强制关闭「添加下级」；已启用则跟随 options
    ...((() => {
      const layoutType = String(config.layoutType || options.layoutType || '')
      const embeddedOn = options.treeConfig?.enabled === true
        || options.treeConfig?.enabled === 1
        || options.treeConfig?.enabled === '1'
        || options.treeConfig?.enabled === 'true'
      if (layoutType === 'tree-crud' && !embeddedOn)
        return { enableTreeAddChild: false }
      return typeof options.enableTreeAddChild === 'boolean'
        ? { enableTreeAddChild: options.enableTreeAddChild }
        : {}
    })()),
    treeConfig: options.treeConfig && typeof options.treeConfig === 'object' ? options.treeConfig : {},
    // 左树 / TreeCrudTemplate 从 publicParams 读 orderBy；把 options.defaultSort 一并灌入
    publicParams: {
      ...(options.defaultSort && typeof options.defaultSort === 'object' ? options.defaultSort : {}),
      ...(options.publicParams || config.publicParams || {}),
    },
    publicQuery: { ...(options.publicQuery || config.publicQuery || {}) },
    formDefaultValues: { ...(options.formDefaultValues || config.formDefaultValues || {}) },
    submitDefaultParams: { ...(options.submitDefaultParams || config.submitDefaultParams || {}) },
    toolbarActions: Array.isArray(options.toolbarActions) ? options.toolbarActions : [],
    detailActions: Array.isArray(options.detailActions) ? options.detailActions : [],
    formActions: Array.isArray(options.formActions) ? options.formActions : [],
    runtimeActions: Array.isArray(options.runtimeActions) ? options.runtimeActions : [],
    businessObjectCode: config.objectCode || options.businessObjectCode || '',
    showDataChangeLog: designerLayout.showDataChangeLog === true || options.showDataChangeLog === true,
    dataAuditObjectId: options.dataAuditObjectId || '',
    // 统一渲染协议（H5 / 多端可直接消费）
    uiDocument: uiDocument || null,
    protocolVersion: uiDocument ? UI_DOCUMENT_PROTOCOL_VERSION : null,
    // 列表自由布局 / 模型：门户与 GridBlockRenderer 据此决定是否改走 RuntimeListGridFlow
    pageSchema: config.pageSchema && typeof config.pageSchema === 'object' ? config.pageSchema : null,
    modelSchema: config.modelSchema && typeof config.modelSchema === 'object' ? config.modelSchema : null,
  }
  return applyEmbeddedTreeTableRuntimeProps(baseProps, config, { designPreview })
}

function normalizeApiConfig(apiConfig, configKey, designPreview) {
  const source = typeof apiConfig === 'string'
    ? safeParseJsonObject(apiConfig)
    : (apiConfig && typeof apiConfig === 'object' && !Array.isArray(apiConfig) ? apiConfig : {})
  return Object.fromEntries(Object.entries(source).map(([key, value]) => {
    const resolved = resolveCurrentConfigPlaceholder(value, configKey)
    return [key, designPreview ? appendDesignPreviewToApiValue(resolved) : resolved]
  }))
}

/** 补齐 CRUD 必备接口；缺 create 时新增会 POST 到空 URL（表现为 /dev-api 404）。 */
function ensureRuntimeApiConfig(apiConfig = {}, configKey = '', designPreview = false) {
  if (!configKey)
    return apiConfig
  const prefix = `/ai/crud/${configKey}`
  const defaults = {
    list: `get@${prefix}/page`,
    detail: `get@${prefix}/:id`,
    create: `post@${prefix}`,
    update: `put@${prefix}`,
    delete: `delete@${prefix}/:id`,
  }
  const next = { ...apiConfig }
  Object.entries(defaults).forEach(([key, value]) => {
    if (!String(next[key] || '').trim())
      next[key] = designPreview ? appendDesignPreviewToApiValue(value) : value
  })
  return next
}

function safeParseJsonObject(value = '') {
  try {
    const parsed = JSON.parse(String(value || ''))
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}

/**
 * 对单个 API 配置值追加 designPreview=1 参数（幂等）。
 * 值格式为 `method@url` 或纯 URL，参数追加在 URL 的 query string 中。
 */
export function appendDesignPreviewToApiValue(value) {
  const text = String(value || '').trim()
  if (!text || text.includes('designPreview='))
    return text
  // 值可能带 method 前缀（如 `get@/ai/crud/key/page`），需在 URL 部分追加参数。
  const atIndex = text.indexOf('@')
  const method = atIndex > -1 ? text.slice(0, atIndex) : ''
  const url = atIndex > -1 ? text.slice(atIndex + 1) : text
  const finalUrl = `${url}${url.includes('?') ? '&' : '?'}designPreview=1`
  return method ? `${method}@${finalUrl}` : finalUrl
}

/**
 * 对 apiConfig 对象中的所有值追加 designPreview=1 参数（幂等）。
 * 空值会被过滤。
 */
export function appendDesignPreviewToApiConfig(apiConfig = {}) {
  return Object.fromEntries(Object.entries(apiConfig || {})
    .map(([key, value]) => [key, appendDesignPreviewToApiValue(value)])
    .filter(([, value]) => value))
}

export function isDesignPreviewCrudProps(runtimeCrudProps = {}) {
  return runtimeCrudProps.designPreview === true || runtimeCrudProps.draftOnly === true
}

/**
 * 静态结构预览只属于不可交互的设计画布。
 *
 * 正式应用运行页即使没有保存 previewLiveData（该字段只是设计器开关），也必须
 * 正常查询和提交；否则会被误判成静态预览并触发 beforeSubmit 拦截。
 */
export function shouldUseStaticCrudPreview({
  blockType = '',
  runtimeInteractive = false,
  previewLiveData = false,
  hasConfiguredRequest = false,
} = {}) {
  if (blockType !== 'AiCrudPage' || runtimeInteractive === true)
    return false
  return !(previewLiveData === true && hasConfiguredRequest === true)
}

export function resolveCurrentConfigPlaceholder(value, configKey) {
  const text = String(value || '').trim()
  if (!text)
    return ''
  if (!text.includes('/ai/crud/当前配置'))
    return text
  // 没有真实 configKey 时不要把占位字符串传到运行时，避免请求当前页面 URL。
  if (!configKey)
    return ''
  return text.replaceAll('/ai/crud/当前配置', `/ai/crud/${configKey}`)
}

export function resolveRuntimeBlockApi(value, configKey, designPreview = false) {
  const resolved = resolveCurrentConfigPlaceholder(value, configKey)
  return designPreview ? appendDesignPreviewToApiValue(resolved) : resolved
}

/** 将页面区块的列布局覆盖到已编译的运行列上。 */
export function applyTableColumnLayout(columns = [], blockProps = {}) {
  const globalAlign = normalizeAlign(blockProps.globalAlign)
  const settings = blockProps.fieldSettings && typeof blockProps.fieldSettings === 'object'
    ? blockProps.fieldSettings
    : {}
  return (Array.isArray(columns) ? columns : [])
    .filter((column) => {
      const key = canonicalFlowStatusCode(
        String(column?.prop || column?.key || column?.dataIndex || '').trim(),
        column,
      )
      // 列表设计器眼睛隐藏写入 fieldSettings.visible=false，运行态必须尊重
      return key ? !isFieldExplicitlyHidden(settings, key) : true
    })
    .map((column) => {
      const key = column?.prop || column?.key || column?.dataIndex || ''
      const fieldAlign = normalizeAlign(settings[key]?.align)
      const align = fieldAlign || globalAlign
      return align
        ? { ...column, align, titleAlign: align }
        : { ...column }
    })
}

export function normalizeTableRowGap(value, fallback = 8) {
  const number = Number(value)
  return Number.isFinite(number) ? Math.max(0, Math.min(32, number)) : fallback
}

/**
 * 解析 CRUD 区块自己的查询字段目录。
 *
 * 新协议以 props.searchFieldRefs 为准；只有旧区块没有该属性时，才兼容使用
 * 列表 fieldRefs，避免列表列调整后把查询条件错误地一起改掉。
 */
export function filterCrudItemsByFieldRefs(items = [], fieldRefs = []) {
  if (!Array.isArray(items) || !items.length)
    return Array.isArray(items) ? items : []
  if (!Array.isArray(fieldRefs) || !fieldRefs.length)
    return items
  const allow = new Set(fieldRefs.filter(Boolean).map(value => canonicalFlowStatusCode(String(value))))
  return items.filter((item) => {
    const key = canonicalFlowStatusCode(String(item?.prop || item?.field || item?.key || item?.dataIndex || '').trim())
    if (!key || item?.type === 'action' || item?.fixed === 'right' || key === 'action')
      return true
    return allow.has(key)
  })
}

/**
 * 平台后补的流程状态字段不能被应用页面块较早保存的 fieldRefs 快照吞掉。
 * 用户在页面块中显式隐藏该字段时仍尊重隐藏配置。
 */
export function isManagedBusinessFlowField(field = {}) {
  const fieldCode = String(field?.field || field?.fieldCode || field?.prop || field?.key || '').trim()
  const columnName = String(field?.columnName || '').trim()
  const managedBy = String(field?.advancedProps?.managedBy || field?.props?.advancedProps?.managedBy || '').toUpperCase()
  const dictType = String(field?.dictType || field?.props?.dictType || '').trim().toLowerCase()
  if (managedBy === 'BUSINESS_FLOW')
    return true
  if (isFlowStatusCode(fieldCode) || isFlowStatusCode(columnName))
    return true
  return dictType === 'business_flow_status'
}

export function includeManagedRuntimeFieldRefs(fieldRefs = [], fieldCatalog = [], fieldSettings = {}) {
  const refs = Array.isArray(fieldRefs) ? [...fieldRefs] : []
  if (!refs.length)
    return refs
  const seen = new Set(refs.filter(Boolean).map(value => canonicalFlowStatusCode(String(value))))
  ;(Array.isArray(fieldCatalog) ? fieldCatalog : []).forEach((field) => {
    const fieldCode = canonicalFlowStatusCode(String(field?.field || field?.fieldCode || '').trim(), field)
    const active = !['DISABLED', 'HIDDEN'].includes(String(field?.fieldStatus || '').toUpperCase())
    if (!fieldCode || !isManagedBusinessFlowField(field) || !active || field?.listVisible === false)
      return
    if (isFieldExplicitlyHidden(fieldSettings, fieldCode) || seen.has(fieldCode))
      return
    seen.add(fieldCode)
    refs.push(fieldCode)
  })
  return refs
}

/**
 * 当后端旧发布配置的 columnsSchema 漏了 flowStatus，但字段目录里已有时，运行态补一列。
 * 显式隐藏仍尊重 fieldSettings.visible = false。
 * 平台默认列：只要目录里有 flowStatus（按字段名），即使缺 managedBy / listVisible 也会补。
 */
export function ensureManagedFlowStatusColumns(columns = [], fieldCatalog = [], fieldSettings = {}) {
  const list = (Array.isArray(columns) ? columns : []).map((column) => {
    const rawKey = String(column?.prop || column?.key || column?.dataIndex || '').trim()
    const key = canonicalFlowStatusCode(rawKey, column)
    // 历史发布快照可能把数据库列名写进 prop/dataIndex；AiTable 取值按 prop 读取，
    // 因此这里必须把流程状态列统一成业务字段编码，避免“列存在但整列为空”。
    return isFlowStatusCode(key)
      ? { ...column, key: 'flowStatus', prop: 'flowStatus', dataIndex: 'flowStatus' }
      : { ...column }
  })
  const keys = new Set(list
    .map(column => canonicalFlowStatusCode(String(column?.prop || column?.field || column?.key || column?.dataIndex || '').trim(), column))
    .filter(Boolean))
  const managedFields = (Array.isArray(fieldCatalog) ? fieldCatalog : []).filter(field => isManagedBusinessFlowField(field))
  managedFields.forEach((field) => {
    const fieldCode = canonicalFlowStatusCode(String(field?.field || field?.fieldCode || '').trim(), field)
    const active = !['DISABLED', 'HIDDEN'].includes(String(field?.fieldStatus || '').toUpperCase())
    if (!fieldCode || !active || field?.listVisible === false)
      return
    if (isFieldExplicitlyHidden(fieldSettings, fieldCode) || keys.has(fieldCode))
      return
    keys.add(fieldCode)
    const dictType = String(field?.dictType || field?.props?.dictType || 'business_flow_status').trim()
    const column = {
      key: fieldCode,
      prop: fieldCode,
      field: fieldCode,
      title: field.label || field.fieldName || field.title || '流程状态',
      dataIndex: fieldCode,
      render: dictType ? { type: 'dictTag', dictType } : undefined,
    }
    const actionIndex = list.findIndex(item => ['action', 'actions', 'operation', 'operations']
      .includes(String(item?.key || item?.type || item?.prop || item?.field || '').toLowerCase())
      || item?.type === 'action'
      || item?.fixed === 'right')
    if (actionIndex >= 0)
      list.splice(actionIndex, 0, column)
    else
      list.push(column)
  })
  return list
}

function isFieldExplicitlyHidden(fieldSettings = {}, fieldCode = '') {
  if (!fieldSettings || typeof fieldSettings !== 'object')
    return false
  if (fieldSettings[fieldCode]?.visible === false)
    return true
  if (!isFlowStatusCode(fieldCode))
    return false
  return Object.entries(fieldSettings).some(([key, setting]) =>
    isFlowStatusCode(key) && setting?.visible === false)
}

/**
 * 列表设计选出的子表列是 modelCode__field。应用页区块的 fieldRefs 往往还是表单主表字段快照，
 * 会把已经编译进 columns 的子表列滤掉。编译结果里的子表列要保留。
 */
export function includeCompiledChildColumnRefs(fieldRefs = [], columns = []) {
  const refs = Array.isArray(fieldRefs) ? fieldRefs.filter(Boolean).map(String) : []
  if (!refs.length)
    return refs
  const seen = new Set(refs)
  ;(Array.isArray(columns) ? columns : []).forEach((column) => {
    const key = String(column?.prop || column?.field || column?.key || column?.dataIndex || '').trim()
    if (!key || !key.includes('__') || seen.has(key))
      return
    seen.add(key)
    refs.push(key)
  })
  return refs
}

export function resolveCrudSearchFieldCatalog(fields = [], block = {}) {
  const fieldMap = new Map((Array.isArray(fields) ? fields : []).flatMap((field) => {
    const fieldCode = field?.field || field?.fieldCode || field?.prop || field?.key
    return fieldCode ? [[fieldCode, { ...field, field: fieldCode, fieldCode }]] : []
  }))
  const hasSearchFieldRefs = Object.prototype.hasOwnProperty.call(block.props || {}, 'searchFieldRefs')
  const refs = hasSearchFieldRefs
    ? (Array.isArray(block.props?.searchFieldRefs) ? block.props.searchFieldRefs : [])
    : (fields || []).filter(field => field?.field && field.searchable === true).map(field => field.field)
  return (Array.isArray(refs) ? refs : [])
    .map((fieldCode) => {
      const sourceField = fieldMap.get(fieldCode)
      if (!sourceField)
        return null
      const setting = block.props?.searchFieldSettings?.[fieldCode] || {}
      const requestedQueryField = String(setting.queryField || '').trim()
      const queryField = fieldMap.get(requestedQueryField) || sourceField
      const queryFieldCode = queryField.field
      // 查询组件类型始终跟表单字段走，忽略历史 searchFieldSettings.componentType
      const resolvedComponentType = queryField.componentType
        || sourceField.componentType
        || ''
      const optionSource = setting.optionSource
        || setting.props?.optionSource
        || queryField.optionSource
        || queryField.props?.optionSource
        || queryField.basicProps?.optionSource
        || sourceField.optionSource
        || sourceField.props?.optionSource
        || sourceField.basicProps?.optionSource
      return {
        ...sourceField,
        ...queryField,
        ...setting,
        field: queryFieldCode,
        fieldCode: queryFieldCode,
        sourceField: fieldCode,
        label: setting.label || sourceField.label || sourceField.fieldName || fieldCode,
        componentType: resolvedComponentType,
        queryType: setting.queryType || queryField.queryType || sourceField.queryType || 'eq',
        optionSource,
        props: {
          ...(sourceField.props || {}),
          ...(queryField.props || {}),
          ...(setting.props || {}),
          ...(optionSource ? { optionSource } : {}),
        },
      }
    })
    .filter(Boolean)
}

const SUPPORTED_SEARCH_TYPES = new Set([
  'eq',
  'ne',
  'like',
  'left_like',
  'right_like',
  'gt',
  'ge',
  'gte',
  'lt',
  'le',
  'lte',
  'in',
  'between',
  'is_null',
  'is_not_null',
])

/**
 * 页面查询方式属于动态 CRUD 控制信息，和用户输入值分开传输。
 */
export function buildCrudSearchTypeRequestParams(searchSchema = []) {
  const searchTypes = {}
  ;(Array.isArray(searchSchema) ? searchSchema : []).forEach((field) => {
    const fieldCode = String(field?.field || '').trim()
    const queryType = String(field?.queryType || field?.searchType || '').trim().toLowerCase()
    if (fieldCode && SUPPORTED_SEARCH_TYPES.has(queryType))
      searchTypes[fieldCode] = queryType
  })
  return Object.keys(searchTypes).length
    ? { _searchTypes: JSON.stringify(searchTypes) }
    : {}
}

/**
 * 构造真实 CRUD 预览的稳定请求身份。
 *
 * 预览结果状态会写回 block.props，但它不属于请求条件，不能因此再次加载列表。
 */
export function resolveCrudPreviewReloadKey(block = {}, runtimeCrudProps = {}) {
  const sourceBlock = block || {}
  const runtimeProps = runtimeCrudProps || {}
  const blockProps = sourceBlock.props || {}
  const previewLiveData = blockProps.previewLiveData === true
  return JSON.stringify({
    enabled: previewLiveData,
    mode: blockProps.previewMode || (previewLiveData ? 'realList' : 'mock'),
    recordId: String(blockProps.previewRecordId ?? ''),
    listApi: blockProps.listApi
      || blockProps.api
      || runtimeProps.apiConfig?.list
      || runtimeProps.api
      || '',
  })
}

function normalizeFields(fields) {
  return (Array.isArray(fields) ? fields : []).map(field => ({ ...field }))
}

/** 供应用页属性面板使用的统一字段目录，和实际 CRUD 列表/弹窗同源。 */
function buildRuntimeFieldCatalog(config = {}) {
  const fields = new Map()
  const append = (source = [], patch = {}) => {
    ;(Array.isArray(source) ? source : []).forEach((item) => {
      const rawField = item?.field || item?.fieldCode || item?.prop || item?.key || item?.dataIndex
      const field = canonicalFlowStatusCode(rawField, item)
      if (!field || ['action', 'actions', 'operation', 'operations'].includes(field))
        return
      const current = fields.get(field) || {}
      const merged = {
        ...current,
        ...item,
        ...patch,
        field,
        fieldCode: field,
        sourceField: item.sourceField || current.sourceField || field,
        fieldName: item.fieldName || item.label || item.title || current.fieldName || field,
        label: item.label || item.title || item.fieldName || current.label || field,
        fieldStatus: item.fieldStatus || current.fieldStatus || 'ENABLED',
      }
      // 平台流程状态默认列表可见；其它字段仍按显式配置，缺省 false。
      const defaultListVisible = isManagedBusinessFlowField(merged) ? true : false
      fields.set(field, {
        ...merged,
        listVisible: patch.listVisible ?? item.listVisible ?? current.listVisible ?? defaultListVisible,
        formVisible: patch.formVisible ?? item.formVisible ?? current.formVisible ?? false,
      })
    })
  }
  append(config.modelSchema?.fields || [])
  append(config.editSchema, { formVisible: true })
  append(config.columnsSchema, { listVisible: true })
  append(config.searchSchema, { searchable: true })
  return [...fields.values()]
}

function normalizeColumns(columns, transConfig = {}) {
  return (Array.isArray(columns) ? columns : []).map((column) => {
    const key = canonicalFlowStatusCode(column.prop || column.key || column.dataIndex || '', column)
    const next = isFlowStatusCode(key)
      ? { ...column, key: 'flowStatus', prop: 'flowStatus', dataIndex: 'flowStatus' }
      : { ...column, key, prop: key }
    const transform = transConfig?.[key]
    if (transform?.targetField && !next.render)
      next.renderConfig = { ...(next.renderConfig || {}), targetField: transform.targetField, type: transform.type }
    return next
  })
}

function isFlowStatusCode(value) {
  return String(value || '').replace(/[-_]/g, '').toLowerCase() === 'flowstatus'
}

function canonicalFlowStatusCode(value, field = {}) {
  const candidate = String(value || '').trim()
  const columnName = String(field?.columnName || '').trim()
  const dictType = String(field?.dictType || field?.props?.dictType || '').trim().toLowerCase()
  if (isFlowStatusCode(candidate) || isFlowStatusCode(columnName) || dictType === 'business_flow_status')
    return 'flowStatus'
  return candidate
}

function resolveFormOpenMode(options = {}, config = {}, designerLayout = {}) {
  const value = String(designerLayout.formOpenMode || designerLayout.modalType
    || options.formOpenMode || config.formOpenMode || options.modalType || config.modalType || 'modal').trim()
  return value.toLowerCase() === 'tabworkspace' ? 'tabWorkspace' : (['modal', 'drawer', 'flat'].includes(value.toLowerCase()) ? value.toLowerCase() : 'modal')
}

function resolveModalType(formOpenMode, options = {}, config = {}, designerLayout = {}) {
  if (['modal', 'drawer'].includes(formOpenMode))
    return formOpenMode
  const modalType = String(designerLayout.modalType || options.modalType || config.modalType || '').trim().toLowerCase()
  return ['modal', 'drawer'].includes(modalType) ? modalType : 'modal'
}

/** 提取表单设计器保存的 layout（兼容单表单与多表单结构）。 */
export function resolveDesignerFormLayout(formDesignerSchema) {
  if (!formDesignerSchema || typeof formDesignerSchema !== 'object')
    return {}
  if (Array.isArray(formDesignerSchema.forms) && formDesignerSchema.forms.length) {
    const defaultFormKey = formDesignerSchema.defaultFormKey
      || formDesignerSchema.settings?.defaultFormKey
    const form = formDesignerSchema.forms.find(item => item?.formKey === defaultFormKey)
      || formDesignerSchema.forms[0]
    const layout = (form?.schema || form)?.layout || {}
    return layout
  }
  return formDesignerSchema.layout || {}
}

/** 提取表单设计器保存的 governance（与 layout 取自同一个选中表单，兼容单表单与多表单结构）。 */
export function resolveDesignerFormGovernance(formDesignerSchema) {
  if (!formDesignerSchema || typeof formDesignerSchema !== 'object')
    return {}
  if (Array.isArray(formDesignerSchema.forms) && formDesignerSchema.forms.length) {
    const defaultFormKey = formDesignerSchema.defaultFormKey
      || formDesignerSchema.settings?.defaultFormKey
    const form = formDesignerSchema.forms.find(item => item?.formKey === defaultFormKey)
      || formDesignerSchema.forms[0]
    const schema = form?.schema || form || {}
    return schema.settings?.governance || schema.governance || {}
  }
  return formDesignerSchema.settings?.governance || formDesignerSchema.governance || {}
}

/**
 * 将设计器组件合并进平铺 editSchema，并叠 visibility。
 * 低代码运行若只用发布态 editSchema，设计器新拖入的字段不在 fields 里，
 * 随后 uiDocument 布局解析会因 fieldMap 缺键把整字段静默丢掉，表现为「组件看不见」。
 * 与 views/ai/crud-page.vue#buildRuntimeFormProfile 对齐：以设计器组件为字段事实来源。
 */
export function mergeDesignerEditSchema(fields = [], formDesignerSchema = null) {
  const baseList = Array.isArray(fields)
    ? fields.filter(field => field && typeof field === 'object')
    : []
  const components = flattenDesignerFieldComponents(formDesignerSchema)
  if (!components.length)
    return applyDesignerVisibilityToFields(baseList, formDesignerSchema)

  const baseMap = new Map()
  baseList.forEach((field) => {
    const code = String(field.field || field.fieldCode || '').trim()
    if (code)
      baseMap.set(code, field)
  })

  const merged = []
  const used = new Set()
  const visibilityMap = collectDesignerFieldVisibility(formDesignerSchema)
  components.forEach((component) => {
    const next = buildRuntimeFieldFromDesignerComponent(component, baseMap)
    if (!next?.field)
      return
    used.add(next.field)
    merged.push(next)
  })

  baseList.forEach((field) => {
    const code = String(field.field || field.fieldCode || '').trim()
    if (!code || used.has(code))
      return
    const visibility = visibilityMap.get(code)
    if (visibility?.hidden === true)
      return
    if (!visibility) {
      merged.push({ ...field, props: { ...(field.props || {}) } })
      return
    }
    const next = {
      ...field,
      props: { ...(field.props || {}) },
      visibility: {
        ...(field.visibility || {}),
        ...visibility,
      },
    }
    if (visibility.readonly === true) {
      next.readonly = true
      next.disabled = true
      next.props.readonly = true
      next.props.disabled = true
    }
    merged.push(next)
  })
  return merged
}

function flattenDesignerFieldComponents(formDesignerSchema) {
  const list = []
  if (!formDesignerSchema || typeof formDesignerSchema !== 'object')
    return list

  let schema = formDesignerSchema
  if (Array.isArray(formDesignerSchema.forms) && formDesignerSchema.forms.length) {
    const defaultFormKey = formDesignerSchema.defaultFormKey
      || formDesignerSchema.settings?.defaultFormKey
    const form = formDesignerSchema.forms.find(item => item?.formKey === defaultFormKey)
      || formDesignerSchema.forms[0]
    schema = form?.schema || form || {}
  }

  const walk = (nodes = []) => {
    ;(Array.isArray(nodes) ? nodes : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const code = String(component.fieldBinding?.fieldCode || component.field || component.fieldCode || '').trim()
      if (code)
        list.push(component)
      if (Array.isArray(component.children) && component.children.length)
        walk(component.children)
    })
  }
  walk(schema.components || schema.settings?.components || [])
  return list
}

function buildRuntimeFieldFromDesignerComponent(component = {}, baseFieldMap = new Map()) {
  const fieldCode = String(component.fieldBinding?.fieldCode || component.field || component.fieldCode || '').trim()
  if (!fieldCode)
    return null
  const visibility = component.visibility && typeof component.visibility === 'object'
    ? component.visibility
    : {}
  const base = baseFieldMap.get(fieldCode) || { field: fieldCode, type: 'input', label: fieldCode }
  const props = { ...(base.props || {}), ...(component.props || {}) }
  const hasVisibilityRules = hasDesignerRuntimeVisibilityRules({ ...component, props })
  // 静态隐藏且无条件规则时不进表单；有条件规则时保留，交给运行态 resolveRuntimeControl
  if (visibility.hidden === true && !hasVisibilityRules)
    return null
  const validation = component.validation || {}
  const readonly = visibility.readonly === true || base.readonly === true
  if (readonly) {
    props.readonly = true
    props.disabled = true
  }
  return {
    ...base,
    field: fieldCode,
    label: component.label || base.label || fieldCode,
    type: normalizeDesignerRuntimeFieldType(component.componentKey || base.type),
    required: validation.required ?? base.required,
    readonly,
    disabled: readonly || base.disabled === true,
    hidden: visibility.hidden === true,
    visibility: {
      ...(base.visibility || {}),
      ...visibility,
    },
    defaultValue: props.defaultValue ?? base.defaultValue,
    dictType: props.dictType || base.dictType,
    validation,
    props,
  }
}

function hasDesignerRuntimeVisibilityRules(target = {}) {
  const rules = target.props?.runtimeRules || target.runtimeRules || []
  if (!Array.isArray(rules) || !rules.length)
    return false
  return rules.some((rule) => {
    if (!rule || rule.enabled === false)
      return false
    const effect = rule.effect || rule
    return Object.prototype.hasOwnProperty.call(effect, 'visible')
      || Object.prototype.hasOwnProperty.call(effect, 'hidden')
      || effect.whenUnmatched === 'hidden'
      || effect.whenUnmatched === 'visible'
  })
}

function normalizeDesignerRuntimeFieldType(componentKey = '') {
  const key = String(componentKey || '').trim()
  const map = {
    inputNumber: 'number',
    integer: 'number',
    money: 'number',
    dictSelect: 'select',
    orgTreeSelect: 'treeSelect',
    deptTreeSelect: 'treeSelect',
    departmentTreeSelect: 'treeSelect',
    regionTreeSelect: 'treeSelect',
    userSelect: 'select',
    imageUpload: 'imageUpload',
    fileUpload: 'fileUpload',
  }
  return map[key] || key || 'input'
}

/**
 * 将设计器组件的 visibility.readonly / hidden 叠到平铺 editSchema。
 * 低代码运行页原先只编译 uiDocument 布局，字段定义仍用发布态 editSchema，
 * 导致「只读」等状态有的链路生效、有的完全丢失。
 *
 * 优先使用 mergeDesignerEditSchema（会补齐设计器新增字段）；本函数保留给仅需叠 visibility 的调用方。
 */
export function applyDesignerVisibilityToFields(fields = [], formDesignerSchema = null) {
  const list = Array.isArray(fields) ? fields.map(field => (field && typeof field === 'object' ? { ...field } : field)) : []
  const visibilityMap = collectDesignerFieldVisibility(formDesignerSchema)
  if (!visibilityMap.size)
    return list

  return list
    .map((field) => {
      if (!field || typeof field !== 'object')
        return field
      const code = String(field.field || field.fieldCode || '').trim()
      if (!code || !visibilityMap.has(code))
        return field
      const visibility = visibilityMap.get(code)
      if (visibility.hidden === true)
        return null
      const next = {
        ...field,
        props: { ...(field.props || {}) },
        visibility: {
          ...(field.visibility || {}),
          ...visibility,
        },
      }
      if (visibility.readonly === true) {
        next.readonly = true
        next.disabled = true
        next.props.readonly = true
        next.props.disabled = true
      }
      return next
    })
    .filter(Boolean)
}

function collectDesignerFieldVisibility(formDesignerSchema) {
  const map = new Map()
  if (!formDesignerSchema || typeof formDesignerSchema !== 'object')
    return map

  let schema = formDesignerSchema
  if (Array.isArray(formDesignerSchema.forms) && formDesignerSchema.forms.length) {
    const defaultFormKey = formDesignerSchema.defaultFormKey
      || formDesignerSchema.settings?.defaultFormKey
    const form = formDesignerSchema.forms.find(item => item?.formKey === defaultFormKey)
      || formDesignerSchema.forms[0]
    schema = form?.schema || form || {}
  }

  const walk = (nodes = []) => {
    ;(Array.isArray(nodes) ? nodes : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const code = String(component.fieldBinding?.fieldCode || component.field || component.fieldCode || '').trim()
      const visibility = component.visibility && typeof component.visibility === 'object'
        ? component.visibility
        : null
      if (code && visibility) {
        map.set(code, {
          hidden: visibility.hidden === true,
          readonly: visibility.readonly === true,
        })
      }
      if (Array.isArray(component.children) && component.children.length)
        walk(component.children)
    })
  }
  walk(schema.components || schema.settings?.components || [])
  return map
}

function numberOption(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) && number >= 0 ? number : fallback
}

function normalizeAlign(value) {
  const align = String(value || '').trim().toLowerCase()
  return ['left', 'center', 'right'].includes(align) ? align : ''
}
