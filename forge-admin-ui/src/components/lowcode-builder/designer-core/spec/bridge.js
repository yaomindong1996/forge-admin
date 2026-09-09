/**
 * @fileoverview 桥接层 — 将统一注册表转换为存量消费方的数据格式
 * @description 现有设计器（表单/列表/页面）各自有独立的物料数据格式。
 *   桥接层从统一注册表读取 ComponentSpec，输出兼容格式，避免改动消费方代码。
 *   所有桥接函数均为纯函数，不修改注册表数据。
 */

import { getComponentSpec, listAllComponents, listComponentsByCategory } from './registry'

// ─── 表单设计器桥接 ────────────────────────────────────────

/** 表单画布消费方的历史 componentKey（spec.type → ForgeFormCanvas 存量键名，createForgeLayoutComponent 按旧键分发）；导出给统一面板接入方解析拖拽键名 */
export const FORM_COMPONENT_KEY_OVERRIDES = {
  'grid': 'row',
  'groupTitle': 'title',
  'formSectionTitle': 'AiFormSectionTitle',
  'action-button': 'button',
}

/**
 * 生成表单设计器的字段面板分组（兼容 FIELD_COMPONENT_PALETTE_GROUPS 格式）
 * @returns {Array<{ title: string, items: Array<{ componentKey: string, label: string }> }>} 分组面板数据（title + items）
 */
export function toFieldPaletteGroups() {
  const fields = listComponentsByCategory('field', 'F')
  const groupMap = new Map()

  for (const spec of fields) {
    const group = spec.group || '其他'
    if (!groupMap.has(group)) {
      groupMap.set(group, { title: group, items: [] })
    }
    groupMap.get(group).items.push({
      componentKey: spec.type,
      label: spec.label,
    })
  }

  // transfer 被 widget 版本覆盖，补充到“选择”分组（与原始 FIELD_COMPONENT_PALETTE_GROUPS 一致）
  const hasTransfer = fields.some(s => s.type === 'transfer')
  if (!hasTransfer) {
    const spec = getComponentSpec('transfer')
    if (spec) {
      const target = groupMap.get('选择') || groupMap.get('其他')
      if (target) {
        target.items.push({ componentKey: 'transfer', label: spec.label })
      }
    }
  }

  return Array.from(groupMap.values())
}

/**
 * 生成字段组件的 fieldDefaults 映射表（componentType / alias → defaults）
 * 包含所有别名键，兼容 fieldComponentCatalog.js 中 FIELD_COMPONENT_DEFAULTS 的别名覆盖。
 * @returns {{[key: string]: object}} componentType / alias → fieldDefaults 映射
 */
export function toFieldComponentDefaultsMap() {
  // 扫描所有组件，提取含 fieldDefaults 的条目（field 组件可能被 widget 版本覆盖，如 transfer）
  const allSpecs = listAllComponents()
  const defaults = {}

  for (const spec of allSpecs) {
    if (!spec.fieldDefaults)
      continue
    const fd = { ...spec.fieldDefaults }
    // 主 type
    defaults[spec.type] = fd
    // 所有别名
    for (const alias of (spec.aliases || [])) {
      defaults[alias] = fd
    }
  }

  // 补充历史别名（fieldComponentCatalog.js 中使用但 registry aliases 未覆盖的）
  const aliasExtra = {
    'inputNumber': 'number',
    'input-number': 'number',
    'inputnumber': 'number',
    'integer': 'number',
    'orgSelect': 'orgTreeSelect',
    'departmentSelect': 'orgTreeSelect',
    'departmentTreeSelect': 'orgTreeSelect',
    'deptSelect': 'orgTreeSelect',
    'deptTreeSelect': 'orgTreeSelect',
    'elTreeSelect': 'orgTreeSelect',
    'orgName': 'orgTreeSelect',
    'deptName': 'orgTreeSelect',
    'userPicker': 'userSelect',
    'userName': 'userSelect',
    'upload': 'fileUpload',
  }
  for (const [alias, sourceType] of Object.entries(aliasExtra)) {
    if (!defaults[alias] && defaults[sourceType]) {
      defaults[alias] = defaults[sourceType]
    }
  }

  return defaults
}

/**
 * 兼容旧名：仅返回主 type 的 fieldDefaults（不含别名展开）
 * @returns {{[key: string]: object}} 主 type → fieldDefaults 映射（不含别名展开）
 */
export function toFieldComponentDefaults() {
  const fields = listComponentsByCategory('field')
  const defaults = {}
  for (const spec of fields) {
    if (spec.fieldDefaults) {
      defaults[spec.type] = { ...spec.fieldDefaults }
    }
  }
  return defaults
}

// ─── 页面挂件桥接 ──────────────────────────────────────────

/** 中文分组 → 英文 group key（兼容 pageWidgetCatalog 原始格式） */
const WIDGET_GROUP_MAP = {
  内容: 'content',
  数据: 'data',
  高级: 'advanced',
  导航: 'navigation',
  布局: 'layout',
  媒体: 'media',
}

/** 挂件类型中包含的媒体组件（在注册表中属 media 分类，但消费方视为挂件） */
const WIDGET_MEDIA_TYPES = ['barcode', 'qrcode', 'audio-player', 'video-player', 'avatar', 'iframe']

/**
 * 生成页面挂件目录（兼容 pageWidgetCatalog 格式）
 * @returns {Array<{ blockType: string, componentKey: string, group: string, title: string, label: string, desc: string, defaultW: number, defaultH: number, container?: boolean }>} 挂件目录条目数组
 */
export function toPageWidgetCatalog() {
  const widgets = listComponentsByCategory('widget', 'L')
  const mediaWidgets = WIDGET_MEDIA_TYPES
    .map(type => getComponentSpec(type))
    .filter(Boolean)
  return [...widgets, ...mediaWidgets].map(spec => ({
    blockType: spec.type,
    componentKey: spec.type,
    group: WIDGET_GROUP_MAP[spec.group] || spec.group || 'content',
    title: spec.label,
    label: spec.label,
    desc: spec.desc || '',
    defaultW: spec.layout?.defaultW || 6,
    defaultH: spec.layout?.defaultH || 4,
    ...(spec.container ? { container: true } : {}),
  }))
}

/**
 * 生成页面挂件 componentKey 列表（兼容 pageWidgetComponentKeys）
 * @returns {string[]} 挂件 componentKey 列表
 */
export function toPageWidgetComponentKeys() {
  return toPageWidgetCatalog().map(item => item.componentKey)
}

// ─── 统一物料面板（两侧同显合集）───────────────────────────

/**
 * 统一物料面板两侧同显合集过滤函数（B1）
 * @description 表单/列表设计器左侧面板显示同一份组件合集（取并集去重，仅排除容器内部包装节点），
 *   画布能力差异由接入方 itemDisabledReason 以禁用态呈现，不再从面板隐藏。
 *   注册表是唯一事实源：以后加/改组件只改 spec，两侧面板自动同步。
 * @param {import('./types').ComponentSpec} spec
 * @returns {boolean} 是否进入两侧同显合集（true = 非包装节点）
 */
export function isPaletteUnionSpec(spec) {
  return spec.group !== '包装节点'
}

/** 旧 zone 画布（LowcodePageBuilder）专属组件类型：两侧新画布均不支持直接拖入 */
export const PALETTE_ZONE_ONLY_TYPES = [
  'query-set',
  'custom-query',
  'import-button',
  'export-button',
  'add-button',
  'reset-button',
]

/** 列表画布渲染器（GridBlockRenderer）暂无分支、待流式画布放开的类型 */
export const LIST_CANVAS_PENDING_TYPES = [
  'table',
  'collapse',
  'formSectionTitle',
]

// ─── 列表页区块桥接 ────────────────────────────────────────

/** techTitle 映射：当组件 type 含大写驼峰或需要技术名展示时使用 */
const TECH_TITLE_MAP = {
  AiCrudPage: 'AiCrudPage',
  AiTable: 'AiTable',
  AiForm: 'AiForm',
}

/**
 * 生成列表页区块目录（兼容 listPageBlockCatalog 格式）
 * 包含所有 L / F+L scope 的组件（排除包装节点和纯表单组件）
 * @returns {Array<{ blockType: string, group: string, title: string, desc: string, defaultW?: number, defaultH?: number, unique?: boolean, multiField?: boolean, requireFields?: boolean, onlyFor?: string[], container?: boolean, techTitle?: string }>}
 */

/** 列表画布消费方的历史类型名（spec.type → 存量 blockType，GridBlockRenderer 按旧名分发）；导出给统一面板接入方解析拖拽类型 */
export const LIST_BLOCK_TYPE_OVERRIDES = {
  grid: 'grid-layout',
  box: 'box-layout',
  groupTitle: 'section-divider',
}

/** 不进列表设计器面板的类型：zone 操作组件属于旧 zone 画布；LIST_CANVAS_PENDING_TYPES 列表画布渲染器暂无分支（待 P2.5 流式画布放开） */
const LIST_HIDDEN_TYPES = new Set([
  ...PALETTE_ZONE_ONLY_TYPES,
  ...LIST_CANVAS_PENDING_TYPES,
])

/** 旧 zone 画布（LowcodePageBuilder）支持的组件类型 */
const CANVAS_ZONE_TYPES = new Set([
  ...PALETTE_ZONE_ONLY_TYPES,
  'data-table',
])

export function toListPageBlockCatalog() {
  const specs = listAllComponents().filter((spec) => {
    // 仅 L / F+L scope
    if (spec.scope !== 'L' && spec.scope !== 'F+L')
      return false
    // 排除包装节点（tabPane / collapseItem / col / tableCell）
    if (spec.group === '包装节点')
      return false
    // 排除纯表单字段（scope: F 的 field 组件）
    if (spec.category === 'field')
      return false
    // 排除列表画布渲染器不支持的类型与 zone 专属组件
    if (LIST_HIDDEN_TYPES.has(spec.type))
      return false
    return true
  })

  return specs.map((spec) => {
    const item = {
      blockType: LIST_BLOCK_TYPE_OVERRIDES[spec.type] || spec.type,
      group: spec.group || 'other',
      title: spec.label,
      desc: spec.desc || '',
    }
    if (spec.layout?.defaultW)
      item.defaultW = spec.layout.defaultW
    if (spec.layout?.defaultH)
      item.defaultH = spec.layout.defaultH
    if (spec.meta?.unique)
      item.unique = true
    if (spec.meta?.multiField)
      item.multiField = true
    if (spec.meta?.requireFields)
      item.requireFields = true
    if (spec.meta?.onlyFor)
      item.onlyFor = spec.meta.onlyFor
    if (spec.container)
      item.container = true
    if (TECH_TITLE_MAP[spec.type])
      item.techTitle = TECH_TITLE_MAP[spec.type]
    return item
  })
}

// ─── 页面画布桥接 ──────────────────────────────────────────

/**
 * 生成页面画布组件目录（兼容 canvasComponentCatalog 格式）
 * @returns {Array<{ group: string, componentKey: string, title: string, desc: string, zones?: string[], multiField?: boolean, defaultWidth?: number, defaultHeight?: number }>} 页面画布目录条目数组
 */
export function toCanvasComponentCatalog() {
  const specs = listAllComponents().filter((spec) => {
    if (spec.scope !== 'L' && spec.scope !== 'F+L')
      return false
    if (spec.group === '包装节点')
      return false
    if (spec.category === 'field')
      return false
    // 旧 zone 画布仅支持 zone 操作组件 + data-table，其余由 page-schema.js 的 field-* 包装补齐
    if (!CANVAS_ZONE_TYPES.has(spec.type))
      return false
    return true
  })

  return specs.map((spec) => {
    const item = {
      group: spec.group || 'other',
      componentKey: spec.type,
      title: spec.label,
      desc: spec.desc || '',
    }
    if (spec.meta?.zones)
      item.zones = spec.meta.zones
    if (spec.meta?.multiField)
      item.multiField = true
    // 像素尺寸（从 meta 读取精确值，由 zone-action-components 定义）
    if (spec.meta?.defaultWidth)
      item.defaultWidth = spec.meta.defaultWidth
    if (spec.meta?.defaultHeight)
      item.defaultHeight = spec.meta.defaultHeight
    return item
  })
}

// ─── 页面 Zone 桥接 ────────────────────────────────────────

/**
 * 生成页面 Zone 目录（兼容 pageZoneCatalog 格式）
 * @returns {Array<{ zoneKey: string, componentKey: string, title: string, desc: string }>} 页面 Zone 目录条目数组
 */
export function toPageZoneCatalog() {
  const zoneMap = {
    'search-form': { zoneKey: 'search', title: '查询页', desc: '查询集、重置、自定义筛选条件' },
    'data-table': { zoneKey: 'table', title: '列表页', desc: '列表列、导入导出、批量操作' },
    'AiForm': { zoneKey: 'edit', title: '表单与详情页', desc: '新增、编辑、详情展示共用字段' },
    'detail-info': { zoneKey: 'detail', title: '详情页兼容区', desc: '历史协议保留，新配置使用表单与详情页' },
  }

  return Object.entries(zoneMap)
    .filter(([type]) => getComponentSpec(type))
    .map(([type, info]) => ({
      zoneKey: info.zoneKey,
      componentKey: type,
      title: info.title,
      desc: info.desc,
    }))
}

// ─── 通用桥接 ──────────────────────────────────────────────

/**
 * 根据 blockType 或 componentKey 解析完整桥接 spec
 * @param {string} typeOrAlias - 组件 type 或别名
 * @returns {import('./types').ComponentSpec | null} 桥接 spec，未注册的类型返回 null
 */
export function resolveBridgeSpec(typeOrAlias) {
  return getComponentSpec(typeOrAlias) || null
}
