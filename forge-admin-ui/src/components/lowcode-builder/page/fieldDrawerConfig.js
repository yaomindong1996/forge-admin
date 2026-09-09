/**
 * 列表页字段配置共享模块（ListPageGridDesigner / FieldConfigDrawer 共用）。
 *
 * 包含字段抽屉的下拉选项常量、字段默认渲染推导等纯函数，
 * 以及跨面板复用的字段引用读取（resolveSelectedFieldRefs）。
 */
import { isPageFieldVisible } from './page-schema'

export const queryTypeOptions = [
  { label: '等于', value: 'eq' },
  { label: '包含', value: 'like' },
  { label: '大于等于', value: 'ge' },
  { label: '小于等于', value: 'le' },
  { label: '区间', value: 'between' },
  { label: '多值', value: 'in' },
]

export const searchComponentOptions = [
  { label: '自动', value: '' },
  { label: '输入框', value: 'input' },
  { label: '数字输入', value: 'number' },
  { label: '下拉选择', value: 'select' },
  { label: '字典选择', value: 'dictSelect' },
  { label: '组织树', value: 'orgTreeSelect' },
  { label: '用户选择', value: 'userSelect' },
  { label: '区划树', value: 'regionTreeSelect' },
  { label: '树形选择', value: 'treeSelect' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '时间', value: 'time' },
]

export const tableRenderOptions = [
  { label: '默认', value: '' },
  { label: '链接文本', value: 'link' },
  { label: '字典标签', value: 'dictTag' },
  { label: '组织名称', value: 'orgName' },
  { label: '用户名称', value: 'userName' },
  { label: '区划名称', value: 'regionName' },
  { label: '文件名称', value: 'fileUpload' },
  { label: '图片预览', value: 'imageUpload' },
]

export const alignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]

export const fixedColumnOptions = [
  { label: '不固定', value: '' },
  { label: '左侧固定', value: 'left' },
  { label: '右侧固定', value: 'right' },
]

export const columnClickActionOptions = [
  { label: '无', value: 'none' },
  { label: '跳转页面', value: 'navigate' },
]

/**
 * 读取区块在指定区域（search/table）的字段引用列表。
 * AiCrudPage 的查询字段独立存储在 props.searchFieldRefs。
 */
export function resolveSelectedFieldRefs(block = null, zoneKey = 'table', fields = []) {
  if (!block)
    return []
  if (zoneKey === 'search' && block.blockType === 'AiCrudPage') {
    const refs = Array.isArray(block.props?.searchFieldRefs)
      ? block.props.searchFieldRefs
      : block.fieldRefs || []
    const fieldSet = new Set(fields.filter(field => isPageFieldVisible(field, 'search')).map(field => field.field))
    return refs.filter(ref => fieldSet.has(ref))
  }
  return Array.isArray(block.fieldRefs) ? block.fieldRefs : []
}

export function resolveDefaultSearchComponentType(field = {}) {
  const componentType = field.componentType || field.dataType || 'input'
  if (field.dictType)
    return 'dictSelect'
  if (['int', 'bigint', 'decimal', 'double', 'float'].includes(field.dataType))
    return 'number'
  return componentType === 'inputNumber' ? 'number' : componentType
}

export function resolveDefaultTableRenderType(field = {}) {
  const componentType = field.componentType || ''
  if (field.dictType)
    return 'dictTag'
  if (componentType === 'orgTreeSelect')
    return 'orgName'
  if (componentType === 'userSelect')
    return 'userName'
  if (componentType === 'regionTreeSelect')
    return 'regionName'
  if (componentType === 'fileUpload' || componentType === 'imageUpload')
    return componentType
  return ''
}

export function isNameRenderType(renderType) {
  return ['orgName', 'userName', 'regionName', 'fileUpload', 'imageUpload'].includes(renderType)
}

/**
 * 事件/参数名归一化：仅保留字母、数字、下划线、点与中划线。
 */
export function normalizeParamName(value) {
  return String(value || '')
    .trim()
    .replace(/[^\w.-]/g, '')
}

/**
 * 读取 AiCrudPage 字段的快捷能力位（可搜索/可导入/可导出），兼容历史 showInSearch。
 */
export function resolveCrudFieldQuickValue(block = {}, fieldKey = '', settingKey = '', fallback = false) {
  const setting = block.props?.fieldSettings?.[fieldKey] || {}
  if (Object.prototype.hasOwnProperty.call(setting, settingKey))
    return setting[settingKey] === true
  if (settingKey === 'searchable' && Object.prototype.hasOwnProperty.call(setting, 'showInSearch'))
    return setting.showInSearch === true
  return fallback === true
}

/**
 * 生成快捷能力位对应的字段列表 patch（searchFields/importFields/exportFields）。
 */
export function buildCrudFieldListPatch(blockProps = {}, fieldKey = '', settingKey = '', enabled = false) {
  const propName = {
    searchable: 'searchFields',
    importable: 'importFields',
    exportable: 'exportFields',
  }[settingKey]
  if (!propName)
    return {}
  const currentList = Array.isArray(blockProps[propName]) ? blockProps[propName] : []
  const nextSet = new Set(currentList.map(item => String(item || '').trim()).filter(Boolean))
  if (enabled)
    nextSet.add(fieldKey)
  else
    nextSet.delete(fieldKey)
  return { [propName]: Array.from(nextSet) }
}
