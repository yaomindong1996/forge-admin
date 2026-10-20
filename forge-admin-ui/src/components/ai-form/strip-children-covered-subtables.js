/**
 * 主子表运行态去重：布局里的 subTable 与底部 childrenConfig 会各画一块明细。
 * 当 childrenConfig 已覆盖同一关系键时，从 schema 去掉对应布局节点，只保留 childrenConfig 那一块。
 */

const SUB_TABLE_TYPES = new Set(['subTable', 'forgeSubTable', 'childTable'])

/** 仅作分组的空壳：去掉子节点后若无 children 可整段丢弃，避免留下「产品明细」空标题 */
const PRUNE_EMPTY_WRAPPER_TYPES = new Set([
  'divider',
  'elDivider',
  'AiFormSectionTitle',
  'aiFormSectionTitle',
  'formSectionTitle',
  'FormSectionTitle',
  'title',
  'fcTitle',
  'sectionTitle',
  'groupTitle',
  'groupHeader',
  'GroupHeader',
  'titleBlock',
  'section',
  'card',
  'elCard',
  'row',
  'fcRow',
  'col',
  'split',
])

function resolveNodeType(node = {}) {
  return String(node.nodeType || node.type || node.componentKey || '').trim()
}

function isSubTableNode(node = {}) {
  return SUB_TABLE_TYPES.has(resolveNodeType(node))
}

function isPruneEmptyWrapper(node = {}) {
  return PRUNE_EMPTY_WRAPPER_TYPES.has(resolveNodeType(node))
}

function resolveSubTableRelationKey(node = {}) {
  const props = node.props || {}
  return String(props.relationKey || props.modelCode || node.key || node.id || '').trim()
}

function collectChildrenConfigKeys(childrenConfig = []) {
  const keys = new Set()
  for (const child of childrenConfig || []) {
    ;[child?.key, child?.relationKey, child?.modelCode, child?.tableName, child?.field]
      .filter(Boolean)
      .forEach(value => keys.add(String(value)))
  }
  return keys
}

function stripNode(node, coveredKeys) {
  if (!node || typeof node !== 'object')
    return null
  if (isSubTableNode(node) && coveredKeys.has(resolveSubTableRelationKey(node)))
    return null
  if (!Array.isArray(node.children) || !node.children.length)
    return node
  const children = node.children.map(child => stripNode(child, coveredKeys)).filter(Boolean)
  // 仅丢掉「因子表剥离而变空」的分组壳，保留原本就是叶子的分区标题等节点
  if (!children.length && isPruneEmptyWrapper(node))
    return null
  return { ...node, children }
}

/**
 * @param {Array} schema AiForm/editSchema 节点树
 * @param {Array} childrenConfig 主子表 childrenConfig
 * @returns {Array} 去掉已被 childrenConfig 覆盖的布局 subTable 后的 schema
 */
export function stripChildrenCoveredSubTables(schema = [], childrenConfig = []) {
  const coveredKeys = collectChildrenConfigKeys(childrenConfig)
  if (!coveredKeys.size || !Array.isArray(schema) || !schema.length)
    return schema
  return schema.map(node => stripNode(node, coveredKeys)).filter(Boolean)
}

export function schemaHasSubTableNode(schema = []) {
  const walk = (nodes) => {
    for (const node of nodes || []) {
      if (isSubTableNode(node))
        return true
      if (walk(node.children))
        return true
    }
    return false
  }
  return walk(schema)
}
