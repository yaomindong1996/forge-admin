/**
 * 列表页查询区与表格区字段绑定必须分离：
 * - fieldRefs → 表格列（AiCrudPage / data-table）
 * - searchFieldRefs → 查询条件（仅 AiCrudPage.props）
 * 禁止把查询字段合并进 fieldRefs，否则预览/运行会多出查询列、丢掉真正的列表列（如 id）。
 */

/**
 * 从业务对象 pageSchema 读取列表设计器表格区字段（发布运行态的列成员真相来源）。
 * 应用页 AiCrudPage.fieldRefs 可能仍被历史查询字段污染，不能优先生效。
 */
function resolvePageSchemaListItems(pageSchema = {}) {
  return pageSchema.listGridLayout?.items
    || (Array.isArray(pageSchema.pages) ? pageSchema.pages : [])
      .find(page => page?.pageKey === 'list')
      ?.gridLayout
      ?.items
    || []
}

export function resolvePageSchemaTableFieldRefs(pageSchema = null, defaultTableFieldRefs = []) {
  if (!pageSchema || typeof pageSchema !== 'object')
    return []
  const zones = Array.isArray(pageSchema.zones) ? pageSchema.zones : []
  const tableZone = zones.find(zone => zone?.zoneKey === 'table')
  if (Array.isArray(tableZone?.fieldRefs) && tableZone.fieldRefs.length)
    return tableZone.fieldRefs.filter(Boolean)

  const crud = (Array.isArray(resolvePageSchemaListItems(pageSchema)) ? resolvePageSchemaListItems(pageSchema) : [])
    .find(item => item?.blockType === 'AiCrudPage')
  if (!crud)
    return []
  return repairAiCrudTableFieldRefs(
    crud.fieldRefs || [],
    crud.props?.searchFieldRefs || [],
    defaultTableFieldRefs,
  )
}

export function resolvePageSchemaSearchFieldRefs(pageSchema = null) {
  if (!pageSchema || typeof pageSchema !== 'object')
    return []
  const zones = Array.isArray(pageSchema.zones) ? pageSchema.zones : []
  const searchZone = zones.find(zone => zone?.zoneKey === 'search')
  const crud = (Array.isArray(resolvePageSchemaListItems(pageSchema)) ? resolvePageSchemaListItems(pageSchema) : [])
    .find(item => item?.blockType === 'AiCrudPage')
  if (Object.prototype.hasOwnProperty.call(crud?.props || {}, 'searchFieldRefs'))
    return Array.isArray(crud.props.searchFieldRefs) ? crud.props.searchFieldRefs.filter(Boolean) : []
  if (Array.isArray(searchZone?.fieldRefs))
    return searchZone.fieldRefs.filter(Boolean)
  return []
}

export function resolveCrudPreviewFieldBindings({
  tableFieldRefs = [],
  searchFieldRefs = [],
  fallbackTableFields = [],
} = {}) {
  const tableRefs = Array.isArray(tableFieldRefs) && tableFieldRefs.length
    ? tableFieldRefs.filter(Boolean)
    : (Array.isArray(fallbackTableFields) ? fallbackTableFields : [])
      .map(field => (typeof field === 'string' ? field : field?.field))
      .filter(Boolean)
  const searchRefs = Array.isArray(searchFieldRefs) ? searchFieldRefs.filter(Boolean) : []
  return {
    fieldRefs: tableRefs,
    searchFieldRefs: searchRefs,
  }
}

/**
 * 修复历史误把查询字段写入 AiCrudPage.fieldRefs 的数据：
 * 当 fieldRefs 与 searchFieldRefs 完全一致（或为空但查询有值）时，回退默认列表列。
 */
export function repairAiCrudTableFieldRefs(fieldRefs = [], searchFieldRefs = [], defaultTableFieldRefs = []) {
  const tableRefs = Array.isArray(fieldRefs) ? fieldRefs.filter(Boolean) : []
  const searchRefs = Array.isArray(searchFieldRefs) ? searchFieldRefs.filter(Boolean) : []
  const defaults = Array.isArray(defaultTableFieldRefs) ? defaultTableFieldRefs.filter(Boolean) : []
  if (!searchRefs.length)
    return tableRefs.length ? tableRefs : defaults
  if (!tableRefs.length && searchRefs.length)
    return defaults.length ? defaults : tableRefs
  const sameAsSearch = tableRefs.length === searchRefs.length
    && tableRefs.every(ref => searchRefs.includes(ref))
  return sameAsSearch && defaults.length ? defaults : tableRefs
}

function columnFieldKey(column = {}) {
  return String(column?.prop || column?.field || column?.key || column?.dataIndex || '').trim()
}

/**
 * 是否像「查询字段误写入列表 fieldRefs」的脏数据。
 * 仅在与 searchFieldRefs 完全一致时判定，避免把用户故意少选的列表列当成污染。
 */
export function isPollutedTableFieldRefs(fieldRefs = [], searchFieldRefs = []) {
  const refs = Array.isArray(fieldRefs) ? fieldRefs.filter(Boolean) : []
  const searchRefs = Array.isArray(searchFieldRefs) ? searchFieldRefs.filter(Boolean) : []
  if (!refs.length || !searchRefs.length)
    return false
  return refs.length === searchRefs.length && refs.every(ref => searchRefs.includes(ref))
}

/**
 * 编辑勾选与运行列的统一优先级：
 * 1. 业务对象 pageSchema 表格区（列表设计器勾选的真相）
 * 2. 页面块 fieldRefs 仅在是表格区子集时生效（应用页进一步收窄）
 * 3. 块比表格区多出的字段视为历史污染（常把查询字段 fieldMoney 写进来）
 */
export function resolvePreferredTableFieldRefs({
  configuredFieldRefs = [],
  pageSchemaTableRefs = [],
  searchFieldRefs = [],
} = {}) {
  const blockRefs = Array.isArray(configuredFieldRefs) ? configuredFieldRefs.filter(Boolean) : []
  const schemaRefs = Array.isArray(pageSchemaTableRefs) ? pageSchemaTableRefs.filter(Boolean) : []
  const searchRefs = Array.isArray(searchFieldRefs) ? searchFieldRefs.filter(Boolean) : []

  if (schemaRefs.length && blockRefs.length) {
    if (isPollutedTableFieldRefs(blockRefs, searchRefs))
      return schemaRefs
    // 块比列表设计器表格区多列 → 旧快照/查询污染，用表格区
    const extras = blockRefs.filter(ref => !schemaRefs.includes(ref))
    if (extras.length)
      return schemaRefs
    // 块是表格区子集（含相等）→ 尊重更窄的勾选
    return blockRefs
  }
  if (schemaRefs.length)
    return schemaRefs
  if (blockRefs.length && !isPollutedTableFieldRefs(blockRefs, searchRefs))
    return blockRefs
  return blockRefs
}

/**
 * 解析真正用于表格列的 fieldRefs。
 * - 有显式勾选：严格按勾选，绝不扩成「所有 listVisible 默认列」
 * - 设计预览无勾选时，才回落 tableZone 生成的 columns
 * - 仅在与 searchFieldRefs 完全撞车时才修理污染
 */
export function resolveTableColumnFieldRefs({
  fieldRefs = [],
  searchFieldRefs = [],
  columns = [],
  designPreview = false,
  defaultTableFieldRefs = [],
} = {}) {
  const refs = Array.isArray(fieldRefs) ? fieldRefs.filter(Boolean) : []
  const searchRefs = Array.isArray(searchFieldRefs) ? searchFieldRefs.filter(Boolean) : []
  const defaults = Array.isArray(defaultTableFieldRefs) ? defaultTableFieldRefs.filter(Boolean) : []
  const columnKeys = (Array.isArray(columns) ? columns : [])
    .map(columnFieldKey)
    .filter(key => key && key !== 'action' && key !== 'actions' && key !== '__actions')

  // 显式勾选（含只勾一个业务字段）一律尊重，编辑态 / 运行态同口径
  if (refs.length && !isPollutedTableFieldRefs(refs, searchRefs))
    return refs

  if (designPreview && columnKeys.length)
    return columnKeys

  const repaired = repairAiCrudTableFieldRefs(refs, searchRefs, schemaOrColumnFallback(defaults, columnKeys))
  if (repaired.length)
    return repaired
  return columnKeys.length ? columnKeys : defaults
}

function schemaOrColumnFallback(defaults = [], columnKeys = []) {
  return defaults.length ? defaults : columnKeys
}

/**
 * 将分区字段变更同步到自由布局 grid 时：
 * - search → search-form.fieldRefs + AiCrudPage.props.searchFieldRefs（绝不改 AiCrudPage.fieldRefs）
 * - table → data-table/AiCrudPage/AiTable.fieldRefs
 */
export function patchCrudGridLayoutItems(items = [], gridPatch = {}) {
  const blockTypes = new Set(gridPatch.blockTypes || [])
  const zoneKey = gridPatch.zoneKey || ''
  return (Array.isArray(items) ? items : []).map((item) => {
    if (!blockTypes.has(item.blockType))
      return item

    if (zoneKey === 'search' && item.blockType === 'AiCrudPage') {
      const propsPatch = { ...(gridPatch.props || {}) }
      const searchFieldSettings = propsPatch.fieldSettings
      delete propsPatch.fieldSettings
      return {
        ...item,
        props: {
          ...(item.props || {}),
          ...propsPatch,
          ...(Array.isArray(gridPatch.fieldRefs) ? { searchFieldRefs: gridPatch.fieldRefs } : {}),
          ...(searchFieldSettings
            ? {
                searchFieldSettings: {
                  ...(item.props?.searchFieldSettings || {}),
                  ...searchFieldSettings,
                },
              }
            : {}),
        },
      }
    }

    if (zoneKey === 'search' && item.blockType === 'search-form') {
      return {
        ...item,
        fieldRefs: Array.isArray(gridPatch.fieldRefs) ? gridPatch.fieldRefs : item.fieldRefs,
        props: mergeItemProps(item.props, gridPatch.props),
      }
    }

    return {
      ...item,
      fieldRefs: Array.isArray(gridPatch.fieldRefs) ? gridPatch.fieldRefs : item.fieldRefs,
      props: mergeItemProps(item.props, gridPatch.props),
    }
  })
}

function mergeItemProps(currentProps = {}, patchProps) {
  if (!patchProps)
    return currentProps
  return {
    ...(currentProps || {}),
    ...patchProps,
    fieldSettings: patchProps.fieldSettings
      ? {
          ...(currentProps?.fieldSettings || {}),
          ...patchProps.fieldSettings,
        }
      : currentProps?.fieldSettings,
  }
}
