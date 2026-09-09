import { safeKey } from '../child-table-section-config'
import { isFieldComponent } from '../form-first/formDesignerSchema'

// 布局承载分区：分区容器与关联子表容器的 componentKey 约定。
const SECTION_CONTAINER_KEYS = ['card', 'elCard', 'collapse', 'elCollapse']
const SUB_TABLE_COMPONENT_KEY = 'subTable'
const DEFAULT_SECTION_ID = 'section_default'
// 分区骨架键：派生时重新计算；其余键（visibleInModes、collapsible 等）按 sectionId 从存量分区继承。
const SECTION_SKELETON_KEYS = new Set(['sectionId', 'sectionType', 'title', 'fields', 'relationKey', 'displayMode'])

/**
 * 从表单布局组件树派生 pageSections：
 * - card/elCard、collapse/elCollapse 容器 → 内容分区（children 内字段的 fieldCode 即分区字段）；
 * - subTable 容器 → 子表分区（relationKey + 展示方式来自容器属性）；
 * - 不在任何分区容器内的散落字段 → “基本信息”默认分区，保证流程权限粒度覆盖全部字段；
 * - 布局中不存在任何分区容器但存量分区非空时，原样保留存量（不丢流程权限配置）。
 */
export function derivePageSectionsFromLayout(components = [], legacySections = []) {
  const legacyList = Array.isArray(legacySections) ? legacySections : []
  const contentSections = []
  const childTableSections = []
  const looseFieldCodes = []

  visitSectionScope(Array.isArray(components) ? components : [], (component) => {
    if (component.componentKey === SUB_TABLE_COMPONENT_KEY) {
      childTableSections.push(buildSection({
        sectionId: resolveChildTableSectionId(component, legacyList),
        sectionType: 'child_table',
        title: component.props?.header || component.label || '关联子表',
        relationKey: String(component.props?.relationKey || ''),
        displayMode: component.props?.displayMode || 'inline_grid',
      }, legacyList))
      return false
    }
    if (SECTION_CONTAINER_KEYS.includes(component.componentKey)) {
      contentSections.push(buildSection({
        sectionId: String(component.id || ''),
        sectionType: 'card',
        title: component.props?.header || component.props?.title || component.label || '分组',
        fields: collectFieldCodes(component.children),
      }, legacyList))
      return false
    }
    // 散落字段只收集自身，子树交给 descend 递归：collectFieldCodes([component]) 会递归收集整棵子树，
    // 再叠加 return true 的 descend 会把布局容器（row 等）内的字段重复收集两次，
    // 与 normalizePageSections 的 uniqueStrings 去重后结果永不相等，导致设计器 schema 同步死循环。
    if (isFieldComponent(component)) {
      const fieldCode = String(component.fieldBinding?.fieldCode || '').trim()
      if (fieldCode)
        looseFieldCodes.push(fieldCode)
    }
    return true
  })

  if (!contentSections.length && !childTableSections.length && legacyList.length)
    return legacyList

  const sections = [...contentSections]
  if (looseFieldCodes.length) {
    sections.push(buildSection({
      sectionId: DEFAULT_SECTION_ID,
      sectionType: 'card',
      title: '基本信息',
      fields: looseFieldCodes,
    }, legacyList))
  }
  return [...sections, ...childTableSections]
}

function visitSectionScope(components, visitor) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component || typeof component !== 'object')
      return
    const descend = visitor(component)
    if (descend !== false && Array.isArray(component.children))
      visitSectionScope(component.children, visitor)
  })
}

/**
 * 子表分区 sectionId 锚定：容器可显式声明 props.sectionId（同一 relationKey 需要
 * 多个分区视图时使用，如预售单“商品明细/提货退货”双视图）；否则按 relationKey 沿用
 * 存量分区（子表分区向导链路）的 sectionId，再按 child-table-section-config 的
 * 命名规则生成，保证流程权限等按 sectionId 的存量配置在画布容器化后不失配。
 */
function resolveChildTableSectionId(component = {}, legacySections = []) {
  const explicitSectionId = String(component.props?.sectionId || '').trim()
  if (explicitSectionId)
    return explicitSectionId
  const relationKey = String(component.props?.relationKey || '').trim()
  if (relationKey) {
    const legacy = legacySections.find(section => section?.sectionType === 'child_table' && String(section.relationKey || '').trim() === relationKey)
    if (legacy?.sectionId)
      return String(legacy.sectionId)
    return `child_${safeKey(relationKey)}`
  }
  return String(component.id || '')
}

function collectFieldCodes(components = []) {
  const codes = []
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component || typeof component !== 'object')
      return
    if (isFieldComponent(component)) {
      const fieldCode = String(component.fieldBinding?.fieldCode || '').trim()
      if (fieldCode)
        codes.push(fieldCode)
      return
    }
    if (Array.isArray(component.children))
      codes.push(...collectFieldCodes(component.children))
  })
  return codes
}

function buildSection(skeleton = {}, legacySections = []) {
  const legacy = legacySections.find(section => String(section?.sectionId || '') === String(skeleton.sectionId)) || null
  if (!legacy)
    return skeleton
  const inherited = {}
  Object.entries(legacy).forEach(([key, value]) => {
    if (!SECTION_SKELETON_KEYS.has(key))
      inherited[key] = value
  })
  return { ...skeleton, ...inherited }
}
