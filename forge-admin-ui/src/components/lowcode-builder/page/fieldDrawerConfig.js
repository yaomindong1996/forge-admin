/**
 * 列表页字段配置共享模块（ListPageGridDesigner / FieldConfigDrawer 共用）。
 *
 * 包含字段抽屉的下拉选项常量、字段默认渲染推导等纯函数，
 * 以及跨面板复用的字段引用读取（resolveSelectedFieldRefs）。
 */

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
 *
 * 读取时只校验字段仍在目录中，不再用 isPageFieldVisible('search') 二次过滤；
 * 否则「查询」开关写入后会被读回过滤掉，表现为勾选不动。
 *
 * 注意：未配置 searchFieldRefs 时绝不能回落到列表 fieldRefs，
 * 否则列表已选字段的「查询」开关会全部误亮，但运行态仍走模型 searchable。
 */
export function resolveSelectedFieldRefs(block = null, zoneKey = 'table', fields = []) {
  if (!block)
    return []
  const fieldSet = new Set((fields || []).map(field => field?.field).filter(Boolean))
  if (zoneKey === 'search' && block.blockType === 'AiCrudPage') {
    if (Object.prototype.hasOwnProperty.call(block.props || {}, 'searchFieldRefs')) {
      const refs = Array.isArray(block.props.searchFieldRefs) ? block.props.searchFieldRefs : []
      return refs.filter(ref => fieldSet.has(ref))
    }
    // 旧区块：按模型 searchable 回显，与运行态默认搜索区一致
    return (fields || [])
      .filter(field => field?.field && field.searchable === true && fieldSet.has(field.field))
      .map(field => field.field)
  }
  return Array.isArray(block.fieldRefs) ? block.fieldRefs.filter(ref => !fieldSet.size || fieldSet.has(ref)) : []
}

/**
 * 写入查询开关前解析当前查询字段集：有显式配置用配置，否则用模型 searchable。
 */
export function resolveSearchFieldRefsForWrite(block = null, fields = []) {
  if (!block || block.blockType !== 'AiCrudPage')
    return []
  if (Object.prototype.hasOwnProperty.call(block.props || {}, 'searchFieldRefs')) {
    const refs = Array.isArray(block.props.searchFieldRefs) ? block.props.searchFieldRefs : []
    const fieldSet = new Set((fields || []).map(field => field?.field).filter(Boolean))
    return refs.filter(ref => fieldSet.has(ref))
  }
  return (fields || [])
    .filter(field => field?.field && field.searchable === true)
    .map(field => field.field)
}

const SEARCH_UI_COMPONENT_TYPES = new Set([
  'input',
  'textarea',
  'number',
  'select',
  'dictSelect',
  'orgTreeSelect',
  'userSelect',
  'regionTreeSelect',
  'treeSelect',
  'cascader',
  'date',
  'datetime',
  'time',
  'switch',
  'radio',
  'checkbox',
])

/**
 * 查询组件默认类型：优先字段 UI 组件类型（treeSelect 等），
 * 不能被存储类型 bigint/int 抢先改成 number/input。
 */
export function resolveDefaultSearchComponentType(field = {}) {
  const componentType = String(field.componentType || field.type || '').trim()
  if (SEARCH_UI_COMPONENT_TYPES.has(componentType))
    return componentType === 'inputNumber' ? 'number' : componentType
  if (field.dictType)
    return 'dictSelect'
  if (['int', 'bigint', 'decimal', 'double', 'float'].includes(field.dataType))
    return 'number'
  if (componentType === 'inputNumber')
    return 'number'
  return componentType || field.dataType || 'input'
}

/** 查询组件展示文案：优先字段/覆盖的 componentType，否则回落默认推导。 */
export function resolveSearchComponentLabel(field = {}) {
  const type = resolveDefaultSearchComponentType(field)
  const matched = searchComponentOptions.find(item => item.value === type)
  return matched?.label || type || '输入框'
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
