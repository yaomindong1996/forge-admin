/**
 * 将统一 uiDocument 解析为 PC AiForm schema。
 *
 * - fields[]：字段定义与权限事实来源
 * - uiDocument.components：布局树
 * - uiDocument.sections：无组件树时的字段序回退
 */

import {
  isWeakControlType,
  normalizeRuntimeControlType,
  resolveDocumentNodeControlType,
  resolveFieldControlType,
} from '@/components/ai-form/control-type-utils'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'

const LAYOUT_NODE_TYPES = new Set([
  'row',
  'fcRow',
  'col',
  'card',
  'elCard',
  'tabs',
  'elTabs',
  'tabPane',
  'elTabPane',
  'collapse',
  'elCollapse',
  'collapseItem',
  'elCollapseItem',
  'divider',
  'elDivider',
  'AiFormSectionTitle',
  'aiFormSectionTitle',
  'formSectionTitle',
  'FormSectionTitle',
  'title',
  'fcTitle',
  'groupTitle',
  'button',
  'table',
  'tableGrid',
  'AiCrudPage',
  'aiCrudPage',
  'crud',
  'crudBlock',
  'subTable',
  'forgeSubTable',
  'childTable',
])

function fieldKey(field) {
  const key = String(field?.field || field?.fieldCode || '').trim()
  return key || ''
}

function protocolVersionOf(context) {
  return String(context?.protocolVersion ?? context?.uiDocument?.version ?? '').trim()
}

function collectSectionFieldOrder(uiDocument) {
  const sections = Array.isArray(uiDocument?.sections) ? uiDocument.sections : []
  const order = []
  const seen = new Set()
  sections.forEach((section) => {
    const codes = Array.isArray(section?.fields) ? section.fields : []
    codes.forEach((code) => {
      const key = String(code || '').trim()
      if (!key || seen.has(key))
        return
      seen.add(key)
      order.push(key)
    })
  })
  return order
}

function resolveLayoutNodeType(type = '') {
  const componentKey = String(type || '').trim()
  if (['row', 'fcRow'].includes(componentKey))
    return 'row'
  if (componentKey === 'col')
    return 'col'
  if (['card', 'elCard'].includes(componentKey))
    return 'card'
  if (['tabs', 'elTabs'].includes(componentKey))
    return 'tabs'
  if (['tabPane', 'elTabPane'].includes(componentKey))
    return 'tabPane'
  if (['collapse', 'elCollapse'].includes(componentKey))
    return 'collapse'
  if (['collapseItem', 'elCollapseItem'].includes(componentKey))
    return 'collapseItem'
  if (['divider', 'elDivider', 'AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle'].includes(componentKey))
    return 'divider'
  if (['title', 'fcTitle', 'groupTitle'].includes(componentKey))
    return 'groupTitle'
  if (['button', 'table', 'tableGrid'].includes(componentKey))
    return componentKey
  if (['AiCrudPage', 'aiCrudPage', 'crud', 'crudBlock'].includes(componentKey))
    return 'AiCrudPage'
  // 关联子表：无 field / 无 children，仍须保留布局节点供 AiFormLayoutNodes 渲染
  if (['subTable', 'forgeSubTable', 'childTable'].includes(componentKey))
    return 'subTable'
  return ''
}

function isLayoutComponentType(type = '') {
  return LAYOUT_NODE_TYPES.has(String(type || '').trim()) || Boolean(resolveLayoutNodeType(type))
}

/** 已知强控件（归一后的标准名）；用于覆盖 fields[] 里被盖成 input 的粗粒度类型 */
const DOCUMENT_CONTROL_TYPES = new Set([
  'select',
  'dictSelect',
  'radio',
  'radioButton',
  'checkbox',
  'userSelect',
  'orgTreeSelect',
  'objectReference',
  'recordSelector',
  'treeSelect',
  'cascader',
  'switch',
  'date',
  'datetime',
  'daterange',
  'datetimerange',
  'month',
  'year',
  'time',
  'timerange',
  'number',
  'textarea',
  'imageUpload',
  'fileUpload',
  'slider',
  'rate',
  'color',
  'regionTreeSelect',
  'transfer',
  'customSelect',
  'barcodeScanner',
])

function shouldPreferDocumentControlType(documentType = '', fieldType = '') {
  const doc = normalizeRuntimeControlType(documentType)
  const field = normalizeRuntimeControlType(fieldType)
  if (!doc || isWeakControlType(doc))
    return false
  if (!field || isWeakControlType(field) || field === 'text')
    return true
  return DOCUMENT_CONTROL_TYPES.has(doc) && doc !== field
}

function indexFieldsByKey(fields = []) {
  const map = new Map()
  fields.forEach((field) => {
    const key = fieldKey(field)
    if (!key || map.has(key))
      return
    map.set(key, field)
  })
  return map
}

function countSchemaFields(nodes = []) {
  let count = 0
  const walk = (items) => {
    ;(Array.isArray(items) ? items : []).forEach((node) => {
      if (!node || typeof node !== 'object')
        return
      if (node.nodeType && node.nodeType !== 'field') {
        walk(node.children || [])
        return
      }
      if (fieldKey(node))
        count += 1
      if (Array.isArray(node.children) && node.children.length)
        walk(node.children)
    })
  }
  walk(nodes)
  return count
}

/**
 * 将 uiDocument.components 映射为 AiForm schema。
 * 字段节点用 fields[] 覆盖（权限/类型不变）；布局节点补 nodeType。
 */
export function mapUiDocumentComponentsToAiFormSchema(components = [], fields = []) {
  const fieldMap = indexFieldsByKey(fields)
  const usedKeys = new Set()

  const mapNodes = (nodes) => {
    const result = []
    ;(Array.isArray(nodes) ? nodes : []).forEach((node) => {
      if (!node || typeof node !== 'object')
        return

      const rawType = String(node.type || node.componentKey || node.componentType || '').trim()
      const type = resolveDocumentNodeControlType(node) || rawType
      const key = fieldKey(node)
      const children = Array.isArray(node.children) ? node.children : []
      const layoutType = resolveLayoutNodeType(rawType) || resolveLayoutNodeType(type)

      if (node.visible === false)
        return

      if (layoutType || (!key && children.length && isLayoutComponentType(rawType || type))) {
        const mappedChildren = mapNodes(children)
        // 空布局容器仍保留：设计器可放空 card/tabs/collapse/row/table，
        // 运行态 AiFormLayoutNodes 会渲染「暂无内容」占位，避免预览/发布静默丢容器与样式。
        const layoutNode = {
          ...node,
          type: layoutType || type || 'row',
          nodeType: layoutType || 'row',
          children: mappedChildren,
        }
        delete layoutNode.field
        delete layoutNode.fieldCode
        delete layoutNode.prop
        result.push(layoutNode)
        return
      }

      if (!key) {
        const controlType = normalizeRuntimeControlType(type || rawType)
        // 页面挂件：无字段绑定也必须渲染，否则编辑态可见、预览/发布静默丢失
        if (isPageWidgetComponentKey(rawType) || isPageWidgetComponentKey(type) || isPageWidgetComponentKey(controlType)) {
          const widgetKey = String(node.id || node.key || `widget_${rawType || type}`).trim()
          usedKeys.add(widgetKey)
          result.push({
            ...node,
            field: widgetKey,
            fieldCode: widgetKey,
            type: rawType || type || controlType,
            componentKey: rawType || type || controlType,
            nodeType: 'widget',
            label: node.label || rawType || type,
            props: node.props && typeof node.props === 'object' ? { ...node.props } : {},
            showLabel: false,
            showFeedback: false,
            fieldBinding: { mode: 'virtual' },
          })
          return
        }
        // 未绑定业务字段的表单控件：用组件 id 合成 field，保证三态可见一致
        if (DOCUMENT_CONTROL_TYPES.has(controlType) || DOCUMENT_CONTROL_TYPES.has(String(rawType || '').trim())) {
          const synthKey = String(node.id || node.key || `${controlType || rawType}_${result.length}`).trim()
          if (synthKey) {
            usedKeys.add(synthKey)
            const documentType = controlType || normalizeRuntimeControlType(rawType) || 'input'
            result.push({
              field: synthKey,
              fieldCode: synthKey,
              type: documentType,
              componentKey: rawType || type || documentType,
              label: node.label || synthKey,
              required: node.required === true,
              props: node.props && typeof node.props === 'object' ? { ...node.props } : {},
              span: node.span,
              gridStyle: node.gridStyle,
              nodeType: 'field',
            })
            return
          }
        }
        if (children.length) {
          const mappedChildren = mapNodes(children)
          if (mappedChildren.length) {
            result.push({
              ...node,
              type: type || 'row',
              nodeType: layoutType || 'row',
              children: mappedChildren,
            })
          }
        }
        return
      }

      const fieldDef = fieldMap.get(key)
      // 设计器有组件、发布态 fields 尚未收录时，仍按 uiDocument 渲染，避免静默丢字段
      const baseField = fieldDef || {
        field: key,
        fieldCode: key,
        type: type || 'input',
        label: node.label || key,
        props: node.props && typeof node.props === 'object' ? { ...node.props } : {},
        required: node.required === true,
        writable: node.editable !== false,
        readable: true,
        readonly: node.editable === false,
        disabled: node.editable === false,
      }

      usedKeys.add(key)
      // fields[] 是字段事实来源；uiDocument 节点上的 type/label/props 来自设计器编译，
      // 必须叠回去，否则 optionSource / fieldMappings / 组件类型会在协议解析时丢失，
      // 表现为下拉/人员/组织等运行态行为回退或失效。
      const documentType = normalizeRuntimeControlType(type)
      const baseType = resolveFieldControlType(baseField)
      const next = {
        ...baseField,
        type: baseType || documentType || 'input',
        label: baseField.label || node.label || key,
        required: baseField.required === true || node.required === true,
        props: {
          ...(baseField.props || {}),
          ...(node.props && typeof node.props === 'object' ? node.props : {}),
        },
      }
      // 设计器 componentKey / uiDocument.type 优先于发布态 fields 里被盖成 input 的粗粒度类型
      if (DOCUMENT_CONTROL_TYPES.has(documentType))
        next.type = documentType
      else if (documentType && !isWeakControlType(documentType) && shouldPreferDocumentControlType(documentType, next.type))
        next.type = documentType
      const documentComponentKey = String(node.componentKey || '').trim()
      if (documentComponentKey && (!next.componentKey || isWeakControlType(next.componentKey)))
        next.componentKey = documentComponentKey
      // type 仍弱但 componentKey 强时强制抬升（money/switch/userSelect/org 等）
      if (isWeakControlType(next.type) && documentComponentKey && !isWeakControlType(documentComponentKey))
        next.type = normalizeRuntimeControlType(documentComponentKey)
      // uiDocument 权限标记叠到字段定义（设计器只读 / 审批 writable）
      if (node.editable === false) {
        next.readonly = true
        next.disabled = true
        next.props = {
          ...(next.props || {}),
          readonly: true,
          disabled: true,
        }
      }
      if (node.span != null && next.span == null)
        next.span = node.span
      if (node.gridStyle && !next.gridStyle)
        next.gridStyle = node.gridStyle
      result.push(next)
    })
    return result
  }

  const mapped = mapNodes(components)
  // 仅当整棵树都映射不出节点时才回退空；纯挂件表单也必须可预览
  if (!mapped.length)
    return []

  // 有 uiDocument 组件树时，布局即「当前表单」事实来源。
  // 不再把 fields[] 里未入画布的字段追加回来（否则审批会渲染用户已从表单删除的字段）。
  return mapped
}

function reorderFieldsBySections(fields, uiDocument) {
  const order = collectSectionFieldOrder(uiDocument)
  if (!order.length)
    return fields

  const byField = new Map()
  fields.forEach((field) => {
    const key = fieldKey(field)
    if (!key || byField.has(key))
      return
    byField.set(key, field)
  })

  const ordered = []
  order.forEach((key) => {
    const field = byField.get(key)
    if (!field)
      return
    ordered.push(field)
    byField.delete(key)
  })
  byField.forEach((field) => {
    ordered.push(field)
  })
  return ordered
}

/**
 * 通用入口：审批 context / 低代码 profile 均可传入。
 * @param {{ protocolVersion?: string, uiDocument?: object, fields?: object[] }|null|undefined} context
 * @returns {object[]}
 */
export function resolveAiFormSchemaFromUiDocument(context) {
  const fields = Array.isArray(context?.fields) ? context.fields.filter(Boolean) : []
  const uiDocument = context?.uiDocument
  const components = Array.isArray(uiDocument?.components) ? uiDocument.components : []

  if (!fields.length) {
    // 发布态 fields 为空时，仍允许纯 uiDocument 组件树渲染（设计器已加字段、尚未进 editSchema）
    if (protocolVersionOf(context) === '1' && components.length)
      return mapUiDocumentComponentsToAiFormSchema(components, [])
    return []
  }

  if (protocolVersionOf(context) !== '1')
    return fields

  if (!uiDocument || typeof uiDocument !== 'object')
    return fields

  if (components.length) {
    const mapped = mapUiDocumentComponentsToAiFormSchema(components, fields)
    if (countSchemaFields(mapped) > 0)
      return mapped
  }

  return reorderFieldsBySections(fields, uiDocument)
}

/** @deprecated 使用 resolveAiFormSchemaFromUiDocument；保留别名兼容审批页 import */
export function resolveBusinessTaskAiFormSchema(context) {
  return resolveAiFormSchemaFromUiDocument(context)
}
