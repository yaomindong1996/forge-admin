import { isPageWidgetComponentKey } from './page-widget-schema'

/**
 * 将运行态平铺编辑字段按表单设计器布局树（options.editFormLayout / zones.edit.props.formLayout）
 * 合成为 AiForm 可渲染的树形 schema。
 *
 * 布局树中的字段节点从 fields 中取完整定义（字段不存在时该节点删除，避免空壳）；
 * 未出现在布局树中的字段按原顺序追加到末尾，保证字段不丢失；
 * 无布局树或输入本身已是树形时原样返回（旧配置 / 已合成链路的兼容路径）。
 */
export function hydrateRuntimeFormLayout(fields = [], layout = []) {
  const flatFields = Array.isArray(fields) ? fields.filter(Boolean) : []
  if (!Array.isArray(layout) || !layout.length)
    return flatFields
  // 输入已是树形（含布局节点）时直接返回，避免二次合成时第一层无字段可映射、整棵树被误删
  if (flatFields.some(field => field?.nodeType && field?.nodeType !== 'field'))
    return flatFields

  const fieldMap = new Map(flatFields.map(field => [field.field, field]).filter(([fieldCode]) => fieldCode))
  const usedFields = new Set()
  const nodes = layout
    .map(node => hydrateRuntimeLayoutNode(node, fieldMap, usedFields))
    .filter(Boolean)

  flatFields.forEach((field) => {
    if (field.field && !usedFields.has(field.field))
      nodes.push(field)
  })
  return nodes
}

function hydrateRuntimeLayoutNode(node = {}, fieldMap, usedFields) {
  if (!node || typeof node !== 'object')
    return null
  const nodeType = resolveRuntimeLayoutNodeType(node)
  if (node.nodeType === 'field') {
    const field = fieldMap.get(node.field)
    if (!field)
      return null
    usedFields.add(node.field)
    return {
      ...field,
      nodeType: 'field',
      key: node.key || field.field,
      span: node.span || field.span,
      gridStyle: node.gridStyle || field.gridStyle,
    }
  }

  const children = (node.children || [])
    .map(child => hydrateRuntimeLayoutNode(child, fieldMap, usedFields))
    .filter(Boolean)
  if (!children.length && !isStandaloneRuntimeLayoutNode({ ...node, nodeType }))
    return null
  return {
    ...node,
    nodeType,
    children,
  }
}

function resolveRuntimeLayoutNodeType(node = {}) {
  if (isGroupTitleRuntimeLayoutNode(node))
    return 'groupTitle'
  if (isLegacyGroupTitleRuntimeLayoutNode(node))
    return 'groupTitle'
  if (isSectionTitleRuntimeLayoutNode(node))
    return 'divider'
  if (isActionRuntimeLayoutNode(node))
    return node.componentKey || node.type || node.nodeType
  return node.nodeType
}

function isGroupTitleRuntimeLayoutNode(node = {}) {
  return ['title', 'fcTitle', 'sectionTitle', 'groupTitle', 'groupHeader', 'GroupHeader', 'titleBlock', 'section']
    .includes(node.componentKey || node.type || node.nodeType)
}

function isSectionTitleRuntimeLayoutNode(node = {}) {
  return ['divider', 'elDivider', 'AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle']
    .includes(node.componentKey || node.type || node.nodeType)
}

function isLegacyGroupTitleRuntimeLayoutNode(node = {}) {
  const props = node.props || {}
  return node.nodeType === 'divider'
    && !node.componentKey
    && Object.prototype.hasOwnProperty.call(props, 'description')
    && !Object.prototype.hasOwnProperty.call(props, 'title')
}

function isStandaloneRuntimeLayoutNode(node = {}) {
  return isSectionTitleRuntimeLayoutNode(node)
    || isGroupTitleRuntimeLayoutNode(node)
    || isActionRuntimeLayoutNode(node)
    || node.nodeType === 'widget'
    || isPageWidgetComponentKey(node.componentKey || node.type || node.nodeType)
}

function isActionRuntimeLayoutNode(node = {}) {
  return ['button', 'table', 'tableGrid', 'AiCrudPage', 'aiCrudPage', 'crud', 'crudBlock']
    .includes(node.componentKey || node.type || node.nodeType)
}
