/**
 * 主子表运行态去重。
 *
 * 历史策略：布局有 subTable 时剥掉布局、只留底部 childrenConfig → 子表会沉到表单末尾，
 * 分区标题（如「联系信息」）下面变空，看起来像跑到下一节「银行账号」下面。
 *
 * 现行策略：优先保留布局位置的 subTable；底部 ChildTableEditor 再按布局键去重。
 * `stripChildrenCoveredSubTables` 仍保留给旧调用方/测试，新运行态请用
 * `filterChildrenConfigNotCoveredByLayout`。
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

function childConfigIdentityKeys(child = {}) {
  return [child?.key, child?.relationKey, child?.modelCode, child?.tableName, child?.field]
    .filter(Boolean)
    .map(value => String(value))
}

function collectChildrenConfigKeys(childrenConfig = []) {
  const keys = new Set()
  for (const child of childrenConfig || [])
    childConfigIdentityKeys(child).forEach(value => keys.add(value))
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
 * 收集布局中已内联的 subTable 关系键（含 modelCode / key / id）。
 */
export function collectLayoutSubTableKeys(schema = []) {
  const keys = new Set()
  const walk = (nodes) => {
    for (const node of nodes || []) {
      if (isSubTableNode(node)) {
        const key = resolveSubTableRelationKey(node)
        if (key)
          keys.add(key)
      }
      if (Array.isArray(node?.children) && node.children.length)
        walk(node.children)
    }
  }
  walk(Array.isArray(schema) ? schema : [])
  return keys
}

/**
 * 底部 childrenConfig 去掉已被布局 subTable 覆盖的项，避免同键画两遍、且位置错乱。
 */
export function filterChildrenConfigNotCoveredByLayout(childrenConfig = [], schema = []) {
  const layoutKeys = collectLayoutSubTableKeys(schema)
  if (!layoutKeys.size || !Array.isArray(childrenConfig) || !childrenConfig.length)
    return childrenConfig
  return childrenConfig.filter((child) => {
    const ids = childConfigIdentityKeys(child)
    if (!ids.length)
      return true
    return !ids.some(id => layoutKeys.has(id))
  })
}

/**
 * @deprecated 优先保留布局位置；新代码用 filterChildrenConfigNotCoveredByLayout。
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
