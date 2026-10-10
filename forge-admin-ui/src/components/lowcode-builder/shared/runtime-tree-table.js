/**
 * 嵌入式树形表（列表内展开树，非左树右表）运行态桥接。
 * 与 views/ai/crud-page.vue 的 treeTable 逻辑对齐，供 Portal / 自由布局复用。
 */
import { request } from '@/utils/request'

export function isEmbeddedTreeTableRuntime(config = {}) {
  const options = config.options || {}
  const treeConfig = options.treeConfig
  if (!treeConfig || typeof treeConfig !== 'object')
    return false
  // 必须显式 enabled=true。table/zone 残留的空 treeConfig（仅 parentField 默认值）
  // 不能把普通列表改写成 /tree，否则非树表会报「树形父级字段不存在: parentId」。
  if (!isTreeEnabledFlag(treeConfig.enabled))
    return false
  const layoutType = String(config.layoutType || options.layoutType || 'simple-crud')
  // 左树右表仍走筛选树壳；本表「已启用」只在设计器/模型侧表达，不在此把右表改成嵌入式树表
  // appType 误写入 layoutType（SINGLE/TREE）时仍按嵌入式树表处理
  if (layoutType === 'tree-crud')
    return false
  return true
}

function isTreeEnabledFlag(value) {
  return value === true || value === 1 || value === '1' || value === 'true'
}

function isTreeDisabledFlag(value) {
  return value === false || value === 0 || value === '0' || value === 'false'
}

export function resolveTreeLoadMode(treeConfig = {}) {
  return treeConfig.loadMode === 'lazy' ? 'lazy' : 'full'
}

/**
 * 左树选项源排序：仅用 tree-panel 显式配置，不用列表 defaultSort（会把右表 id desc 污染树节点序）。
 */
export function resolveLeftTreeSortParams({
  treePanelProps = {},
} = {}) {
  const panelField = String(treePanelProps.defaultSortField || treePanelProps.orderByColumn || '').trim()
  const panelOrder = String(treePanelProps.defaultSortOrder || treePanelProps.isAsc || '').trim()
  if (!panelField)
    return {}
  return {
    orderByColumn: panelField,
    isAsc: normalizeTreeSortOrder(panelOrder),
  }
}

function normalizeTreeSortOrder(value) {
  const text = String(value || '').trim().toLowerCase()
  if (text === 'asc' || text === 'ascending' || text === '1' || text === 'true')
    return 'asc'
  return 'desc'
}

/**
 * 与左侧 tree-panel 相同的树选项源（含排序），供查询区 treeSelect 复用。
 */
export function buildLeftTreeOptionSource({
  sourceConfigKey = '',
  treeApi = '',
  childrenField = 'children',
  loadMode = 'full',
  orderByColumn = '',
  isAsc = '',
  designPreview = false,
} = {}) {
  const rawApi = String(treeApi || '').trim()
    || (sourceConfigKey ? `get@/ai/crud/${sourceConfigKey}/tree` : '')
  if (!rawApi)
    return null
  const api = designPreview && !rawApi.includes('designPreview=')
    ? `${rawApi}${rawApi.includes('?') ? '&' : '?'}designPreview=1`
    : rawApi
  const params = {
    loadMode: loadMode === 'lazy' ? 'lazy' : 'full',
  }
  if (orderByColumn) {
    params.orderByColumn = orderByColumn
    params.isAsc = normalizeTreeSortOrder(isAsc)
  }
  return {
    type: 'tree',
    api,
    keyField: 'key',
    valueField: 'targetValue',
    labelField: 'label',
    childrenField: childrenField || 'children',
    params,
  }
}

/**
 * 查询区树选择与左树对齐：同一 tree API、同一排序、默认本级+子集。
 */
export function alignSearchSchemaWithLeftTree(searchSchema = [], {
  treePanelProps = {},
  runtimeProps = {},
  designPreview = false,
} = {}) {
  if (!Array.isArray(searchSchema) || !searchSchema.length)
    return Array.isArray(searchSchema) ? searchSchema : []
  const filterField = String(
    treePanelProps.filterField
    || treePanelProps.rightFilterField
    || treePanelProps.listFilterField
    || treePanelProps.parentField
    || runtimeProps?.options?.treeConfig?.filterField
    || runtimeProps?.treeConfig?.filterField
    || '',
  ).trim()
  const sourceConfigKey = String(
    treePanelProps.sourceConfigKey
    || treePanelProps.sourceModelCode
    || runtimeProps?.options?.treeConfig?.sourceConfigKey
    || '',
  ).trim()
  const treeApi = String(treePanelProps.treeApi || '').trim()
    || (sourceConfigKey ? `get@/ai/crud/${sourceConfigKey}/tree` : '')
    || String(runtimeProps?.apiConfig?.tree || '').trim()
  if (!treeApi && !sourceConfigKey)
    return searchSchema
  const sortParams = resolveLeftTreeSortParams({ treePanelProps })
  const optionSource = buildLeftTreeOptionSource({
    sourceConfigKey,
    treeApi,
    childrenField: treePanelProps.childrenField || 'children',
    loadMode: treePanelProps.loadMode,
    orderByColumn: sortParams.orderByColumn,
    isAsc: sortParams.isAsc,
    designPreview: designPreview || isDesignPreviewRuntime(runtimeProps),
  })
  if (!optionSource)
    return searchSchema
  return searchSchema.map((field) => {
    if (!field || typeof field !== 'object')
      return field
    const fieldName = String(field.field || field.prop || field.key || '').trim()
    const type = String(field.type || field.componentType || '').trim()
    if (!['treeSelect', 'orgTreeSelect'].includes(type))
      return field
    // 与左树筛选字段一致时强制共用左树（API / 排序 / 本级+子集）
    if (!filterField || fieldName !== filterField)
      return field
    const includeChildren = field.includeChildren !== false && field.props?.includeChildren !== false
    return {
      ...field,
      includeChildren: includeChildren ? true : field.includeChildren,
      optionSource,
      props: {
        ...(field.props || {}),
        optionSource,
        ...(includeChildren ? { includeChildren: true } : {}),
      },
    }
  })
}

function isDesignPreviewRuntime(runtimeProps = {}) {
  return runtimeProps?.designPreview === true || runtimeProps?.draftOnly === true
}

export function findTreePanelProps(blocks = []) {
  const list = Array.isArray(blocks) ? blocks : []
  const stack = [...list]
  while (stack.length) {
    const block = stack.shift()
    if (!block || typeof block !== 'object')
      continue
    if (block.blockType === 'tree-panel')
      return block.props && typeof block.props === 'object' ? block.props : {}
    if (Array.isArray(block.children))
      stack.push(...block.children)
    if (Array.isArray(block.blocks))
      stack.push(...block.blocks)
  }
  return null
}

/**
 * 查询区树选择展开本级+子集，与左树 buildLeftTreeFilterParams 对齐。
 * 返回 { value, includeChildren, expanded }；UI 仍用原始单值，提交时用 expanded。
 */
export function expandSearchTreeSelectValue(selectedValue, optionNodes = [], {
  includeChildren = true,
  childrenField = 'children',
} = {}) {
  if (selectedValue === undefined || selectedValue === null || selectedValue === '') {
    return { value: selectedValue, includeChildren: false, expanded: [] }
  }
  if (includeChildren === false) {
    return {
      value: selectedValue,
      includeChildren: false,
      expanded: [selectedValue],
    }
  }
  const node = findTreeOptionNode(optionNodes, selectedValue)
  const expanded = collectTreeFilterValues(node || { value: selectedValue }, {
    targetField: 'value',
    childrenField,
    includeChildren: true,
  })
  const unique = uniqueTreeFilterValues(expanded.length ? expanded : [selectedValue])
  return {
    value: selectedValue,
    includeChildren: true,
    expanded: unique,
  }
}

export function findTreeOptionNode(nodes = [], selectedValue) {
  const target = String(selectedValue)
  for (const node of Array.isArray(nodes) ? nodes : []) {
    if (!node || typeof node !== 'object')
      continue
    const candidates = [node.value, node.key, node.targetValue, node.id]
    if (candidates.some(item => item !== undefined && item !== null && String(item) === target))
      return node
    const child = findTreeOptionNode(node.children, selectedValue)
    if (child)
      return child
  }
  return null
}

/**
 * 左树选中后拼列表筛选参数。默认含下级（本级 + 全部子孙节点）。
 * values 已展开时直接作为 IN 列表；否则由后端按源表展开。
 */
export function buildLeftTreeFilterParams({
  filterField,
  value,
  includeChildren = true,
  expandedValues,
} = {}) {
  if (!filterField || value === undefined || value === null || value === '')
    return {}
  const uniqueExpanded = uniqueTreeFilterValues(expandedValues)
  if (includeChildren !== false && uniqueExpanded.length > 1) {
    return {
      [filterField]: uniqueExpanded.join(','),
      [`${filterField}_includeChildren`]: true,
    }
  }
  const params = { [filterField]: String(value) }
  if (includeChildren !== false)
    params[`${filterField}_includeChildren`] = true
  return params
}

function uniqueTreeFilterValues(values = []) {
  const result = []
  const seen = new Set()
  for (const item of Array.isArray(values) ? values : []) {
    if (item === undefined || item === null || item === '')
      continue
    const text = String(item)
    if (seen.has(text))
      continue
    seen.add(text)
    result.push(text)
  }
  return result
}

/**
 * 从已加载的树节点收集本级 + 全部子孙的筛选值（全量树时可前端直接展开）。
 */
export function collectTreeFilterValues(node, {
  targetField = 'id',
  childrenField = 'children',
  includeChildren = true,
} = {}) {
  if (!node || typeof node !== 'object')
    return []
  const readValue = (item) => {
    if (!item || typeof item !== 'object')
      return null
    const raw = item[targetField] ?? item.targetValue ?? item.value ?? item.key ?? item.id
    return raw === undefined || raw === null || raw === '' ? null : String(raw)
  }
  const self = readValue(node)
  if (!includeChildren)
    return self ? [self] : []
  const values = []
  const walk = (item) => {
    const current = readValue(item)
    if (current)
      values.push(current)
    const children = item?.[childrenField]
    if (Array.isArray(children))
      children.forEach(walk)
  }
  walk(node)
  return uniqueTreeFilterValues(values)
}

export function normalizeTreeTableNodes(nodes = [], treeConfig = {}) {
  if (!Array.isArray(nodes))
    return []
  const keyField = treeConfig.keyField || 'id'
  const childrenField = treeConfig.childrenField || 'children'
  return nodes.map((node) => {
    if (!node || typeof node !== 'object')
      return node
    const children = Array.isArray(node?.[childrenField])
      ? normalizeTreeTableNodes(node[childrenField], treeConfig)
      : []
    const normalized = { ...node }
    if (children.length) {
      normalized[childrenField] = children
      normalized.isLeaf = false
    }
    else if (node?.isLeaf !== undefined) {
      normalized.isLeaf = !!node.isLeaf
    }
    else if (resolveTreeLoadMode(treeConfig) === 'lazy') {
      // 懒加载：未带 children 的节点默认视为可展开，真正叶子由接口回空后标 isLeaf
      if (normalized.isLeaf === undefined && normalized[keyField] != null)
        normalized.isLeaf = false
    }
    return normalized
  })
}

function parseApiConfigValue(value = '') {
  const text = String(value || '').trim()
  if (!text)
    return { method: 'get', url: '' }
  const atIndex = text.indexOf('@')
  if (atIndex < 0)
    return { method: 'get', url: text }
  return {
    method: text.slice(0, atIndex) || 'get',
    url: text.slice(atIndex + 1),
  }
}

export async function loadTreeTableChildren(node, config = {}, { designPreview = false } = {}) {
  const treeConfig = config.options?.treeConfig || {}
  const treeApi = config.apiConfig?.tree
  if (!treeApi || !node)
    return
  const { method, url } = parseApiConfigValue(treeApi)
  if (!url)
    return
  const keyField = treeConfig.keyField || 'id'
  const childrenField = treeConfig.childrenField || 'children'
  const parentValue = node[keyField] ?? node.key ?? node.targetValue ?? node.id
  try {
    const res = await request({
      method: method.toLowerCase() || 'get',
      url: designPreview && !String(url).includes('designPreview=')
        ? `${url}${url.includes('?') ? '&' : '?'}designPreview=1`
        : url,
      params: {
        loadMode: 'lazy',
        parentValue,
      },
      globalLoading: false,
    })
    const rows = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    node[childrenField] = normalizeTreeTableNodes(rows, treeConfig)
    if (!node[childrenField].length)
      node.isLeaf = true
  }
  catch (error) {
    console.warn('[runtime-tree-table] 加载树形子节点失败', error)
    node.isLeaf = true
  }
}

export function buildTreeTableProps(config = {}, { designPreview = false } = {}) {
  const treeConfig = config.options?.treeConfig || {}
  const loadMode = resolveTreeLoadMode(treeConfig)
  return {
    childrenKey: treeConfig.childrenField || 'children',
    defaultExpandAll: loadMode !== 'lazy',
    onLoad: loadMode === 'lazy'
      ? node => loadTreeTableChildren(node, config, { designPreview })
      : undefined,
  }
}

/**
 * 把嵌入式树表所需字段灌进 AiCrudPage runtime props。
 */
export function applyEmbeddedTreeTableRuntimeProps(props = {}, config = {}, { designPreview = false } = {}) {
  if (!isEmbeddedTreeTableRuntime(config))
    return props
  const treeConfig = config.options?.treeConfig || {}
  const loadMode = resolveTreeLoadMode(treeConfig)
  const apiConfig = { ...(props.apiConfig || {}) }
  if (apiConfig.tree)
    apiConfig.list = apiConfig.tree
  const previousBeforeRenderList = props.beforeRenderList
  // 嵌入式树表默认开启「添加下级」；区块/options 显式 false 时关闭
  const explicitAddChild = props.enableTreeAddChild ?? config.options?.enableTreeAddChild
  return {
    ...props,
    apiConfig,
    treeConfig,
    showPagination: false,
    enableTreeAddChild: explicitAddChild === undefined ? true : explicitAddChild === true,
    publicParams: {
      ...(props.publicParams || {}),
      loadMode,
    },
    tableProps: {
      ...(props.tableProps || {}),
      ...buildTreeTableProps(config, { designPreview }),
    },
    beforeRenderList: async (list) => {
      let next = normalizeTreeTableNodes(list, treeConfig)
      if (typeof previousBeforeRenderList === 'function')
        next = await previousBeforeRenderList(next)
      return next
    },
  }
}
