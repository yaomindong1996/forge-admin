import { isPageWidgetComponentKey } from './page-widget-schema'
import {
  resolveAiFormSchemaFromUiDocument,
  UI_DOCUMENT_PROTOCOL_VERSION,
} from '@/protocols/ui-document'

/**
 * 将运行态平铺编辑字段合成为 AiForm 可渲染的树形 schema。
 *
 * 优先级：
 * 1. 统一 uiDocument 协议（设计态编译产物，与审批端同构）
 * 2. 旧 editFormLayout / zones.edit.props.formLayout
 * 3. 平铺 fields
 *
 * 布局树中的字段节点从 fields 中取完整定义（字段不存在时该节点删除，避免空壳）；
 * 未出现在布局树中的字段按原顺序追加到末尾，保证字段不丢失；
 * 无布局树或输入本身已是树形时原样返回（旧配置 / 已合成链路的兼容路径）。
 */
export function hydrateRuntimeFormLayout(fields = [], layout = [], uiDocument = null) {
  const flatFields = Array.isArray(fields) ? fields.filter(Boolean) : []
  // 输入已是树形（含布局节点）时直接返回，避免二次合成时第一层无字段可映射、整棵树被误删
  if (flatFields.some(field => field?.nodeType && field?.nodeType !== 'field'))
    return flatFields

  if (uiDocument && typeof uiDocument === 'object') {
    const resolved = resolveAiFormSchemaFromUiDocument({
      protocolVersion: uiDocument.version || UI_DOCUMENT_PROTOCOL_VERSION,
      uiDocument,
      fields: flatFields,
    })
    if (Array.isArray(resolved) && resolved.length)
      return resolved
  }

  if (!Array.isArray(layout) || !layout.length)
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
    // 未绑定合成字段：fields[] 可能尚无定义，仍按节点本身渲染
    if (!field) {
      if (!node.field && !node.type && !node.componentKey)
        return null
      usedFields.add(node.field || node.key)
      return {
        ...node,
        nodeType: 'field',
        field: node.field || node.key,
        type: node.type || node.componentKey || 'input',
        label: node.label || node.field || node.key,
      }
    }
    usedFields.add(node.field)
    return {
      ...field,
      nodeType: 'field',
      key: node.key || field.field,
      span: node.span || field.span,
      gridStyle: node.gridStyle || field.gridStyle,
      style: node.style || field.style,
      props: {
        ...(field.props || {}),
        ...(node.props || {}),
      },
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
