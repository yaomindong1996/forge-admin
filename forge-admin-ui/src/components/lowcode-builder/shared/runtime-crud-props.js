/**
 * 将已发布的 CRUD 配置转换为 AiCrudPage 的运行参数。
 *
 * 列表设计器预览和应用页面都使用这份轻量桥接：接口、表单和列表字段始终
 * 来自同一个 configKey，应用设计器只允许覆盖外观与局部行为，不另存一套接口。
 */
export function buildRuntimeCrudProps(config = {}) {
  const options = config.options || {}
  const formOpenMode = resolveFormOpenMode(options, config)
  const configKey = String(config.configKey || '').trim()
  const apiConfig = normalizeApiConfig(config.apiConfig, configKey)
  return {
    searchSchema: normalizeFields(config.searchSchema),
    columns: normalizeColumns(config.columnsSchema, config.transConfig),
    editSchema: normalizeFields(config.editSchema),
    fieldCatalog: buildRuntimeFieldCatalog(config),
    childrenConfig: options.masterDetailConfig?.children || [],
    expandConfig: options.expandConfig || config.expandConfig || {},
    detailPanels: options.detailPanels || config.detailPanels || [],
    apiConfig,
    configKey,
    options,
    rowKey: config.rowKey || 'id',
    formOpenMode,
    tabWorkspace: options.tabWorkspace || config.tabWorkspace || {},
    modalType: resolveModalType(formOpenMode, options, config),
    modalWidth: options.modalWidth || config.modalWidth || '800px',
    editGridCols: options.editGridCols || config.editGridCols || 1,
    editLabelWidth: options.editLabelWidth || config.editLabelWidth || 'auto',
    editLabelPlacement: options.editLabelPlacement || config.editLabelPlacement || 'left',
    editLabelAlign: options.editLabelAlign || config.editLabelAlign || 'right',
    editSize: options.editSize || config.editSize || 'medium',
    editShowFeedback: options.editShowFeedback ?? config.editShowFeedback ?? true,
    editFormClass: options.editFormClass || config.editFormClass || '',
    editFormStyle: options.editFormStyle || config.editFormStyle,
    formAssets: options.formAssets || config.formAssets || [],
    editXGap: numberOption(options.editXGap ?? config.editXGap, 12),
    editYGap: numberOption(options.editYGap ?? config.editYGap, 8),
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
    publicParams: { ...(options.publicParams || config.publicParams || {}) },
    publicQuery: { ...(options.publicQuery || config.publicQuery || {}) },
    formDefaultValues: { ...(options.formDefaultValues || config.formDefaultValues || {}) },
    submitDefaultParams: { ...(options.submitDefaultParams || config.submitDefaultParams || {}) },
    toolbarActions: Array.isArray(options.toolbarActions) ? options.toolbarActions : [],
  }
}

function normalizeApiConfig(apiConfig, configKey) {
  return Object.fromEntries(Object.entries(apiConfig || {}).map(([key, value]) => [key, resolveCurrentConfigPlaceholder(value, configKey)]))
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

function normalizeFields(fields) {
  return (Array.isArray(fields) ? fields : []).map(field => ({ ...field }))
}

/** 供应用页属性面板使用的统一字段目录，和实际 CRUD 列表/弹窗同源。 */
function buildRuntimeFieldCatalog(config = {}) {
  const fields = new Map()
  const append = (source = [], patch = {}) => {
    ;(Array.isArray(source) ? source : []).forEach((item) => {
      const field = item?.field || item?.fieldCode || item?.prop || item?.key || item?.dataIndex
      if (!field || ['action', 'actions', 'operation', 'operations'].includes(field))
        return
      const current = fields.get(field) || {}
      fields.set(field, {
        ...current,
        ...item,
        ...patch,
        field,
        fieldCode: field,
        sourceField: item.sourceField || current.sourceField || field,
        fieldName: item.fieldName || item.label || item.title || current.fieldName || field,
        label: item.label || item.title || item.fieldName || current.label || field,
        listVisible: patch.listVisible ?? item.listVisible ?? current.listVisible ?? false,
        formVisible: patch.formVisible ?? item.formVisible ?? current.formVisible ?? false,
        fieldStatus: item.fieldStatus || current.fieldStatus || 'ENABLED',
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
    const key = column.prop || column.key || column.dataIndex || ''
    const next = { ...column, key, prop: key }
    const transform = transConfig?.[key]
    if (transform?.targetField && !next.render)
      next.renderConfig = { ...(next.renderConfig || {}), targetField: transform.targetField, type: transform.type }
    return next
  })
}

function resolveFormOpenMode(options = {}, config = {}) {
  const value = String(options.formOpenMode || config.formOpenMode || options.modalType || config.modalType || 'modal').trim()
  return value.toLowerCase() === 'tabworkspace' ? 'tabWorkspace' : (['modal', 'drawer', 'flat'].includes(value.toLowerCase()) ? value.toLowerCase() : 'modal')
}

function resolveModalType(formOpenMode, options = {}, config = {}) {
  if (['modal', 'drawer'].includes(formOpenMode))
    return formOpenMode
  const modalType = String(options.modalType || config.modalType || '').trim().toLowerCase()
  return ['modal', 'drawer'].includes(modalType) ? modalType : 'modal'
}

function numberOption(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) && number >= 0 ? number : fallback
}
