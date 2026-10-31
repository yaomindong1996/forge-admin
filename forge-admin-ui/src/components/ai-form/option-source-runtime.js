/**
 * 下拉/树选择选项来源运行时辅助：
 * 查询字段投影、扁平转树、加载限流、回显占位标签。
 */

/** 远程选项加载中的占位标签：避免 n-select 在 options 未就绪时把 value（id）当标签回显 */
export const REMOTE_OPTION_PENDING_LABEL = '\u200B'

/** 下拉等扁平选项默认每次拉取条数 */
export const DEFAULT_OPTION_PAGE_SIZE = 50

/** 树形全量默认上限（一次拼完整树） */
export const DEFAULT_TREE_OPTION_PAGE_SIZE = 200

/** 硬上限，防止配置过大拖垮页面 */
export const MAX_OPTION_PAGE_SIZE = 1000

const TREE_COMPONENT_TYPES = new Set([
  'treeSelect',
  'orgTreeSelect',
  'deptTreeSelect',
  'departmentTreeSelect',
  'cascader',
])

/**
 * 从 fieldMappings 抽出源字段名（选中回填依赖选项行上带这些列）。
 */
export function collectFieldMappingSourceFields(mappings = []) {
  const list = Array.isArray(mappings)
    ? mappings
    : (mappings && typeof mappings === 'object' ? Object.keys(mappings) : [])
  return list
    .map((item) => {
      if (typeof item === 'string')
        return item.trim()
      return String(item?.sourceField || item?.source || '').trim()
    })
    .filter(Boolean)
}

/**
 * 组装 BUSINESS_OBJECT / QUERY_SOURCE 执行时需要回传的字段列表。
 * 必须包含 valueField + labelField；树场景额外带上 parentField，否则扁平转树缺父级列。
 * fieldMappings / extraFields 一并投影，保证「选中后回填」能读到源字段。
 */
export function buildQuerySourceDisplayFields(source = {}, extraFields = []) {
  const fields = []
  const seen = new Set()
  const push = (raw) => {
    const text = String(raw || '').trim()
    if (!text)
      return
    const field = text.split(':', 2)[0].trim()
    if (!field || seen.has(field))
      return
    seen.add(field)
    fields.push(field)
  }

  ;(Array.isArray(source.fields) ? source.fields : []).forEach(push)
  ;(Array.isArray(source.displayFields) ? source.displayFields : []).forEach(push)
  collectFieldMappingSourceFields(source.fieldMappings || source.mappings).forEach(push)
  ;(Array.isArray(extraFields) ? extraFields : []).forEach(push)
  push(source.valueField)
  push(source.labelField)
  push(source.parentField)
  push(source.keyField)
  return fields
}

/**
 * 从选项行读取字段值；选择器记录常把展示字段放在顶层，完整行在 `_raw`。
 */
export function readOptionRowField(row, field) {
  const key = String(field || '').trim()
  if (!key || !row || typeof row !== 'object')
    return undefined
  if (Object.prototype.hasOwnProperty.call(row, key)
    && row[key] !== undefined
    && row[key] !== null
    && row[key] !== '') {
    return row[key]
  }
  const raw = row._raw
  if (raw && typeof raw === 'object'
    && Object.prototype.hasOwnProperty.call(raw, key)
    && raw[key] !== undefined
    && raw[key] !== null
    && raw[key] !== '') {
    return raw[key]
  }
  return undefined
}

export function resolveFirstFilledOptionField(row, fields = []) {
  const keys = fields.filter((field, index, all) => field && all.indexOf(field) === index)
  for (const key of keys) {
    const value = readOptionRowField(row, key)
    if (value !== undefined && value !== null && value !== '')
      return value
  }
  return undefined
}

/**
 * 当前值在 options 中尚无匹配时的回显标签：
 * - 优先伴随字段（xxxName）
 * - 远程加载中用不显示的占位符，禁止回落成 id
 * - 其它情况返回空，由调用方决定是否注入
 */
export function resolvePendingOptionLabel({
  companionLabel = '',
  remotePending = false,
} = {}) {
  if (companionLabel !== undefined && companionLabel !== null && String(companionLabel).trim() !== '')
    return String(companionLabel)
  if (remotePending)
    return REMOTE_OPTION_PENDING_LABEL
  return ''
}

export function isTreeOptionComponentType(componentType = '') {
  return TREE_COMPONENT_TYPES.has(String(componentType || '').trim())
}

/**
 * 是否应按树结构渲染选项（嵌套 children 或扁平+parentField）。
 */
export function shouldBuildTreeOptions(source = {}, componentType = '') {
  if (isTreeOptionComponentType(componentType))
    return true
  const type = String(source?.type || '').toLowerCase()
  if (type === 'tree')
    return true
  if (String(source?.structure || '').toLowerCase() === 'tree')
    return true
  return Boolean(String(source?.parentField || '').trim())
}

/**
 * full | lazy；非法值回落 full。
 */
export function resolveOptionLoadMode(source = {}) {
  const mode = String(source?.loadMode || source?.params?.loadMode || 'full').toLowerCase()
  return mode === 'lazy' ? 'lazy' : 'full'
}

export function clampOptionPageSize(value, fallback = DEFAULT_OPTION_PAGE_SIZE) {
  const n = Number(value)
  if (!Number.isFinite(n) || n <= 0)
    return fallback
  return Math.min(Math.floor(n), MAX_OPTION_PAGE_SIZE)
}

/**
 * 解析每次请求条数：树全量默认 200，其它默认 50；尊重显式 pageSize/maxRows。
 */
export function resolveOptionPageSize(source = {}, { isTree = false } = {}) {
  const fallback = isTree ? DEFAULT_TREE_OPTION_PAGE_SIZE : DEFAULT_OPTION_PAGE_SIZE
  const raw = source?.pageSize ?? source?.maxRows ?? source?.params?.pageSize
  return clampOptionPageSize(raw, fallback)
}

function isRootParentValue(parentRaw, rootParentValue) {
  if (rootParentValue !== undefined && rootParentValue !== null && String(rootParentValue) !== '') {
    return String(parentRaw) === String(rootParentValue)
  }
  return parentRaw === null
    || parentRaw === undefined
    || parentRaw === ''
    || parentRaw === 0
    || parentRaw === '0'
}

/**
 * 扁平列表按 parentField 拼成树。节点需已带 value（或由 valueField 读取）。
 * 找不到父节点的行进入根层，避免数据丢失。
 */
export function buildTreeFromFlatRows(rows = [], {
  valueField = 'id',
  parentField = 'parentId',
  rootParentValue,
} = {}) {
  const list = Array.isArray(rows) ? rows.filter(row => row && typeof row === 'object') : []
  if (!list.length || !String(parentField || '').trim())
    return list

  const nodes = new Map()
  list.forEach((row) => {
    const value = row.value !== undefined && row.value !== null && row.value !== ''
      ? row.value
      : readOptionRowField(row, valueField)
    if (value === undefined || value === null || value === '')
      return
    const key = String(value)
    const existingChildren = Array.isArray(row.children) ? [...row.children] : []
    nodes.set(key, {
      ...row,
      value,
      key: row.key ?? value,
      children: existingChildren,
    })
  })

  const roots = []
  nodes.forEach((node) => {
    const parentRaw = readOptionRowField(node, parentField)
    if (isRootParentValue(parentRaw, rootParentValue)) {
      roots.push(node)
      return
    }
    const parent = nodes.get(String(parentRaw))
    if (parent && parent !== node) {
      parent.children.push(node)
      return
    }
    roots.push(node)
  })
  return roots
}

/**
 * 行里是否已有嵌套 children（非空数组）。
 */
export function rowsHaveNestedChildren(rows = [], childrenField = 'children') {
  const field = String(childrenField || 'children').trim() || 'children'
  return (Array.isArray(rows) ? rows : []).some(row => Array.isArray(row?.[field]) && row[field].length > 0)
}

/**
 * 为懒加载节点补 isLeaf：接口未声明时默认非叶子（允许展开探测）。
 */
export function decorateLazyTreeNodes(nodes = []) {
  return (Array.isArray(nodes) ? nodes : []).map((node) => {
    if (!node || typeof node !== 'object')
      return node
    const next = { ...node }
    if (Object.prototype.hasOwnProperty.call(next, 'isLeaf'))
      return next
    if (next.hasChildren === false || next.leaf === true) {
      next.isLeaf = true
      delete next.children
      return next
    }
    if (Array.isArray(next.children) && next.children.length > 0) {
      next.children = decorateLazyTreeNodes(next.children)
      next.isLeaf = false
      return next
    }
    next.isLeaf = false
    if (!Array.isArray(next.children))
      next.children = []
    return next
  })
}

/**
 * 全量树叶子标记：无下级时 isLeaf=true，并去掉空 children，避免展开箭头空转。
 */
export function decorateResolvedTreeLeafNodes(nodes = []) {
  return (Array.isArray(nodes) ? nodes : []).map((node) => {
    if (!node || typeof node !== 'object')
      return node
    const next = { ...node }
    const children = Array.isArray(next.children) ? next.children : []
    if (children.length > 0) {
      next.children = decorateResolvedTreeLeafNodes(children)
      next.isLeaf = false
      return next
    }
    next.isLeaf = true
    delete next.children
    return next
  })
}

/**
 * 设计器默认树映射：业务人员改「挂在谁下面」即可，其它给合理默认。
 */
export function createDefaultTreeOptionSourcePatch(previous = {}) {
  return {
    structure: 'tree',
    parentField: previous.parentField || 'parentId',
    childrenField: previous.childrenField || 'children',
    loadMode: previous.loadMode === 'lazy' ? 'lazy' : 'full',
    pageSize: previous.pageSize || DEFAULT_TREE_OPTION_PAGE_SIZE,
    rootParentValue: previous.rootParentValue ?? '',
  }
}

/**
 * 按目标「值字段」元数据得到本表应落库的存储类型。
 * 规则：与目标字段类型保持一致；拿不到真实类型时返回 null（不瞎猜）。
 */
export function resolveStorageTypeFromOptionValueMeta(meta = {}, valueField = '') {
  const field = String(valueField || meta.field || meta.value || '').trim()
  const rawType = String(meta.dataType || meta.type || meta.dbType || '').toLowerCase().trim()
  if (!rawType)
    return null

  const lengthRaw = meta.length == null || meta.length === '' ? null : Number(meta.length)
  const length = Number.isFinite(lengthRaw) && lengthRaw > 0 ? Math.floor(lengthRaw) : null
  const precisionRaw = meta.precision == null || meta.precision === '' ? null : Number(meta.precision)
  const precision = Number.isFinite(precisionRaw) ? Math.floor(precisionRaw) : null

  const normalized = rawType.includes('(') ? rawType.slice(0, rawType.indexOf('(')) : rawType
  if (['bigint', 'long', 'int64'].includes(normalized))
    return { dataType: 'bigint', length: null, precision: null, sourceField: field }
  if (['int', 'integer', 'int32', 'mediumint', 'smallint'].includes(normalized))
    return { dataType: 'int', length: length || 11, precision: 0, sourceField: field }
  if (['tinyint', 'boolean', 'bool'].includes(normalized))
    return { dataType: 'tinyint', length: length || 1, precision: 0, sourceField: field }
  if (['decimal', 'numeric', 'number', 'double', 'float', 'real'].includes(normalized))
    return { dataType: 'decimal', length: length || 18, precision: precision ?? 2, sourceField: field }
  if (['date'].includes(normalized))
    return { dataType: 'date', length: null, precision: null, sourceField: field }
  if (['datetime', 'timestamp'].includes(normalized))
    return { dataType: 'datetime', length: null, precision: null, sourceField: field }
  if (['text', 'longtext', 'mediumtext'].includes(normalized))
    return { dataType: 'text', length: null, precision: null, sourceField: field }
  if (['varchar', 'string', 'char', 'character'].includes(normalized))
    return { dataType: 'varchar', length: length || 64, precision: precision ?? 2, sourceField: field }

  // 未识别类型：原样透传（去掉长度括号），长度有则带上
  return {
    dataType: normalized,
    length,
    precision,
    sourceField: field,
  }
}

/**
 * 解析字段级联配置。
 *
 * 注意：主子表运行态 child.fields 常带 sourceField=自身列名（表示子表列编码），
 * 不能当成级联父字段；否则空值时 emptyStrategy=empty 会把下拉选项滤成「无数据」。
 */
export function resolveFieldCascadeConfig(field = {}) {
  const explicit = [field.props?.cascade, field.cascade, field.props?.cascadeConfig, field.cascadeConfig]
    .find(item => item && typeof item === 'object')

  let raw = null
  if (explicit) {
    // 设计器显式写了 cascade 对象：enabled:false 或没有 sourceField 都视为不级联
    if (explicit.enabled === false)
      return null
    if (!String(explicit.sourceField || '').trim())
      return null
    raw = explicit
  }
  else {
    const hasLegacyCascadeSignal = Boolean(
      field.matchMode
      || field.props?.matchMode
      || field.linkedDictType
      || field.props?.linkedDictType
      || field.emptyStrategy
      || field.props?.emptyStrategy
      || field.paramName
      || field.props?.paramName
      || field.clearOnParentChange != null
      || field.clearOnSourceChange != null
      || field.props?.clearOnParentChange != null
      || field.props?.clearOnSourceChange != null,
    )
    if (!hasLegacyCascadeSignal)
      return null
    raw = {
      sourceField: field.sourceField || field.props?.sourceField,
      sourceDictType: field.sourceDictType || field.props?.sourceDictType,
      linkedDictType: field.linkedDictType || field.props?.linkedDictType,
      mode: field.matchMode || field.props?.matchMode || field.mode || field.props?.mode,
      paramName: field.paramName || field.props?.paramName,
      emptyStrategy: field.emptyStrategy || field.props?.emptyStrategy,
      clearOnParentChange: field.clearOnParentChange ?? field.clearOnSourceChange
        ?? field.props?.clearOnParentChange ?? field.props?.clearOnSourceChange,
      includeChildren: field.includeChildren ?? field.props?.includeChildren,
    }
  }

  const sourceField = String(raw?.sourceField || '').trim()
  if (!sourceField)
    return null
  const selfField = String(field.field || field.fieldCode || field.sourceField || '').trim()
  // 子表列 sourceField 等于自身 field 时不是级联
  if (selfField && sourceField === selfField)
    return null
  if (raw.enabled === false)
    return null

  return {
    enabled: true,
    sourceField,
    sourceDictType: raw.sourceDictType || '',
    linkedDictType: raw.linkedDictType || '',
    mode: raw.mode || raw.matchMode || 'linkedDict',
    paramName: raw.paramName || '',
    includeChildren: raw.includeChildren !== false,
    emptyStrategy: raw.emptyStrategy || 'empty',
    clearOnParentChange: raw.clearOnParentChange !== false && raw.clearOnSourceChange !== false,
  }
}
