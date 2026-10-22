import { toCanvasComponentCatalog as _toCanvasComponentCatalog } from '@/components/lowcode-builder/designer-core'
import { createPageWidgetDefaultProps, createWidgetDataBinding, pageWidgetCatalog } from '@/components/lowcode-builder/shared/page-widget-schema'

import {
  isHiddenPageField,
  isReadonlySystemField,
  createPageModelRef,
} from './page-schema-fields.js'

export const LIST_PAGE_GRID_COLS = 12
export const LIST_PAGE_GRID_ROW_HEIGHT = 32
export const LIST_PAGE_GRID_GAP = 8
export const LIST_PAGE_DESIGN_WIDTH = 1366
export const LIST_PAGE_GRID_BASE_COL_WIDTH = Math.floor((LIST_PAGE_DESIGN_WIDTH - (LIST_PAGE_GRID_COLS - 1) * LIST_PAGE_GRID_GAP) / LIST_PAGE_GRID_COLS)

export const DATA_FIELD_BLOCK_TYPES = Object.freeze([
  'AiCrudPage',
  'AiForm',
  'AiTable',
  'data-table',
  'search-form',
  'detail-info',
])

export function isDataFieldBlockType(blockType) {
  return DATA_FIELD_BLOCK_TYPES.includes(blockType)
}

export const listPageBlockCatalog = dedupeListPageBlockCatalog([
  {
    blockType: 'search-form',
    group: 'data',
    title: '查询表单',
    desc: '查询字段集 + 查询/重置/收起',
    defaultW: 12,
    defaultH: 4,
    multiField: true,
    requireFields: true,
    unique: true,
  },
  {
    blockType: 'toolbar',
    group: 'action',
    title: '操作工具栏',
    desc: '新增 / 导入 / 导出 / 自定义查询',
    defaultW: 12,
    defaultH: 2,
    unique: true,
  },
  {
    blockType: 'back-button',
    group: 'page',
    title: '返回上一页',
    desc: '详情页返回入口',
    defaultW: 2,
    defaultH: 1,
  },
  {
    blockType: 'page-title',
    group: 'page',
    title: '页面标题',
    desc: '标题、副标题和状态提示',
    defaultW: 8,
    defaultH: 2,
  },
  {
    blockType: 'grid-layout',
    group: 'layout',
    title: '栅格布局',
    desc: '单行多列容器，每格可设置 span 和 gutter',
    defaultW: 12,
    defaultH: 8,
    container: true,
  },
  {
    blockType: 'detail-info',
    group: 'data',
    title: '详情信息',
    desc: 'Naive Descriptions 详情：字段 + 远程接口；支持边框/列数/标签位置等',
    defaultW: 12,
    defaultH: 10,
    multiField: true,
    requireFields: true,
  },
  {
    blockType: 'AiCrudPage',
    group: 'action',
    title: '数据列表',
    techTitle: 'AiCrudPage',
    desc: '选择业务对象，自动生成筛选、表格与新增、编辑、删除，内置数据表单弹窗',
    defaultW: 12,
    defaultH: 14,
    unique: true,
  },
  {
    blockType: 'AiTable',
    group: 'data',
    title: '数据表格',
    techTitle: 'AiTable',
    desc: '选择业务对象，生成支持分页、密度与列设置的交互表格',
    defaultW: 12,
    defaultH: 9,
    unique: true,
  },
  {
    blockType: 'AiForm',
    group: 'data',
    title: '数据表单',
    techTitle: 'AiForm',
    desc: '绑定业务对象生成录入表单；可设列数/标签，高度随字段自适应',
    defaultW: 12,
    defaultH: 12,
  },
  {
    blockType: 'data-table',
    group: 'data',
    title: '基础列表',
    desc: '绑定业务对象后拉取真实分页列表；右侧「数据」Tab 选对象与表单',
    defaultW: 12,
    defaultH: 10,
    multiField: true,
    requireFields: true,
    unique: true,
  },
  {
    blockType: 'tree-panel',
    group: 'data',
    title: '筛选树',
    desc: '左树右表模板中筛选右侧列表',
    defaultW: 3,
    defaultH: 14,
    unique: true,
    onlyFor: ['tree-crud'],
  },
  {
    blockType: 'stats-strip',
    group: 'extra',
    title: '指标卡片',
    desc: 'KPI 卡片：图标 / 色调 / 趋势 / 可点击跳转',
    defaultW: 12,
    defaultH: 2,
  },
  {
    blockType: 'quick-entry',
    group: 'extra',
    title: '快捷入口',
    desc: '图标入口：色调 / 页面或外链跳转 / 多种样式',
    defaultW: 12,
    defaultH: 3,
  },
  {
    blockType: 'bar-chart',
    group: 'extra',
    title: '柱状图',
    desc: '分类对比，支持手动数据或列表绑定',
    defaultW: 6,
    defaultH: 8,
  },
  {
    blockType: 'line-chart',
    group: 'extra',
    title: '折线图',
    desc: '趋势变化，支持手动数据或列表绑定',
    defaultW: 6,
    defaultH: 8,
  },
  {
    blockType: 'area-chart',
    group: 'extra',
    title: '面积图',
    desc: '趋势面积填充，适合占比随时间变化',
    defaultW: 6,
    defaultH: 8,
  },
  {
    blockType: 'pie-chart',
    group: 'extra',
    title: '饼图',
    desc: '占比分布，支持环形 / 南丁格尔图',
    defaultW: 6,
    defaultH: 8,
  },
  {
    blockType: 'workflow-task-list',
    group: 'extra',
    title: '我的待办/已办',
    desc: '待办与已办列表，可切换、搜索、状态标签与跳转办理',
    defaultW: 6,
    defaultH: 10,
  },
  {
    blockType: 'notice-board',
    group: 'extra',
    title: '通知公告',
    desc: '站内消息 / 公告列表，未读筛选、角标与标已读',
    defaultW: 6,
    defaultH: 10,
  },
  {
    blockType: 'welcome-banner',
    group: 'extra',
    title: '欢迎条',
    desc: '按时段问候 + 昵称/日期，可配渐变背景与快捷按钮',
    defaultW: 12,
    defaultH: 3,
  },
  {
    blockType: 'workspace-summary-metrics',
    group: 'extra',
    title: '工作台统计',
    desc: '我的待办 / 本周已办 / 发起中 / 未读抄送',
    defaultW: 12,
    defaultH: 3,
  },
  {
    blockType: 'info-panel',
    group: 'extra',
    title: '提示面板',
    desc: '说明 / 警告 / 成功提示，支持图标色与跳转',
    defaultW: 6,
    defaultH: 2,
  },
  {
    blockType: 'custom-html',
    group: 'extra',
    title: '说明文本',
    desc: '富文本 / Markdown 提示',
    defaultW: 6,
    defaultH: 3,
  },
  {
    blockType: 'action-button',
    group: 'action',
    title: '按钮',
    desc: '单个命令按钮',
    defaultW: 2,
    defaultH: 1,
  },
  {
    blockType: 'button-group',
    group: 'action',
    title: '按钮组',
    desc: '多个页面操作按钮',
    defaultW: 5,
    defaultH: 2,
  },
  {
    blockType: 'tag-list',
    group: 'extra',
    title: '标签列表',
    desc: '状态、分类、关键词展示',
    defaultW: 4,
    defaultH: 2,
  },
  {
    blockType: 'steps',
    group: 'extra',
    title: '步骤条',
    desc: '流程步骤展示',
    defaultW: 8,
    defaultH: 2,
  },
  {
    blockType: 'timeline',
    group: 'extra',
    title: '时间线',
    desc: '操作记录和流转轨迹',
    defaultW: 6,
    defaultH: 5,
  },
  {
    blockType: 'empty-state',
    group: 'extra',
    title: '空状态',
    desc: '暂无数据、引导操作',
    defaultW: 5,
    defaultH: 4,
  },
  {
    blockType: 'card',
    group: 'layout',
    title: '卡片容器',
    desc: '分组卡片：标题竖线、背景/渐变/图片，可嵌套子组件',
    defaultW: 6,
    defaultH: 5,
    container: true,
  },
  {
    blockType: 'panel-box',
    group: 'layout',
    title: '面板容器',
    desc: '类 div 容器：默认宽高、背景色/图，内部可放多个组件',
    defaultW: 6,
    defaultH: 5,
    container: true,
  },
  {
    blockType: 'tabs',
    group: 'layout',
    title: 'Tabs 标签页',
    desc: '多页签布局容器',
    defaultW: 12,
    defaultH: 6,
    container: true,
  },
  {
    blockType: 'divider',
    group: 'layout',
    title: '分隔线',
    desc: '横向 / 竖向分隔',
    defaultW: 12,
    defaultH: 1,
  },
  {
    blockType: 'spacer',
    group: 'layout',
    title: '留白占位',
    desc: '调整页面间距',
    defaultW: 12,
    defaultH: 1,
  },
  ...pageWidgetCatalog.map(item => ({
    blockType: item.blockType,
    group: item.group,
    title: item.title,
    desc: item.desc,
    defaultW: item.defaultW,
    defaultH: item.defaultH,
  })),
  {
    blockType: 'signature-pad',
    group: 'data',
    title: '手写签名',
    desc: '签名采集画布',
    defaultW: 6,
    defaultH: 5,
    // 暂时下线：组件面板 / 右键插入均不展示，存量页面仍可渲染
    hidden: true,
  },
  {
    blockType: 'step-form',
    group: 'data',
    title: '分步表单',
    desc: '按步骤组织表单字段',
    defaultW: 10,
    defaultH: 7,
  },
  // text-title / paragraph / statistic / text-tip 已由 pageWidgetCatalog 提供
  {
    blockType: 'link',
    group: 'action',
    title: '链接',
    desc: '页面跳转或外部链接',
    defaultW: 3,
    defaultH: 1,
  },
  // audio/video/avatar/iframe 已由 pageWidgetCatalog 提供，勿重复登记
  {
    blockType: 'box-layout',
    group: 'layout',
    title: '盒子布局',
    desc: 'Flex 容器布局',
    defaultW: 8,
    defaultH: 5,
    container: true,
  },
  {
    blockType: 'space',
    group: 'layout',
    title: '间距',
    desc: '横向或纵向间隔',
    defaultW: 4,
    defaultH: 2,
  },
  {
    blockType: 'sub-table-tabs',
    group: 'extra',
    title: '子表 Tab',
    desc: '关联模型分页签',
    defaultW: 12,
    defaultH: 8,
    onlyFor: ['master-detail-crud'],
  },
  {
    blockType: 'section-divider',
    group: 'extra',
    title: '分组标题',
    desc: '区块分隔与说明',
    defaultW: 12,
    defaultH: 1,
  },
])

function dedupeListPageBlockCatalog(items = []) {
  const seen = new Set()
  return items.filter((item) => {
    const key = String(item?.blockType || '').trim()
    if (!key || seen.has(key))
      return false
    seen.add(key)
    return true
  })
}

export function resolveListPageBlockMeta(blockType) {
  return listPageBlockCatalog.find(item => item.blockType === blockType) || null
}

export function resolveTreeSourceRefs(modelSchema = {}) {
  const refs = Array.isArray(modelSchema.pageModelRefs) && modelSchema.pageModelRefs.length
    ? modelSchema.pageModelRefs
    : [createPageModelRef({ modelSchema }, { primary: true })]
  return refs.filter(ref => ref?.modelCode || ref?.primary)
}

export function resolveDefaultTreeConfig(modelSchema = {}, overrides = {}) {
  const normalizedOverrides = normalizeTreeConfigAliases(overrides)
  const requestedSource = String(
    normalizedOverrides.sourceModelCode
    || normalizedOverrides.sourceConfigKey
    || '',
  ).trim()
  const hasExplicitSourceKey = Object.prototype.hasOwnProperty.call(overrides || {}, 'sourceModelCode')
    || Object.prototype.hasOwnProperty.call(overrides || {}, 'sourceConfigKey')
  const matchedSourceRef = requestedSource
    ? findTreeSourceRef(modelSchema, requestedSource)
    : null
  // 显式选了页面模型里没有的对象时，禁止回退到主模型字段，否则同步会把树字段打回当前列表
  if (requestedSource && !matchedSourceRef) {
    const keyField = normalizedOverrides.keyField || 'id'
    const parentField = normalizedOverrides.parentField || 'parentId'
    const labelField = normalizedOverrides.labelField || ''
    const targetField = normalizedOverrides.targetField || keyField
    const filterField = normalizedOverrides.filterField || parentField
    return {
      enabled: normalizedOverrides.enabled ?? true,
      sourceModelCode: String(normalizedOverrides.sourceModelCode || requestedSource).trim(),
      sourceModelName: normalizedOverrides.sourceModelName || '',
      sourceTableName: normalizedOverrides.sourceTableName || '',
      sourceConfigKey: String(normalizedOverrides.sourceConfigKey || '').trim(),
      sourceObjectId: normalizedOverrides.sourceObjectId ?? null,
      treeApi: normalizedOverrides.treeApi || '',
      keyField,
      parentField,
      labelField,
      filterField,
      targetField,
      childrenField: normalizedOverrides.childrenField || 'children',
      treeTitle: normalizedOverrides.treeTitle || '',
      loadMode: normalizedOverrides.loadMode || 'full',
    }
  }
  // 设计器故意留空「树数据来源」时，不要在同步里回填成当前列表对象
  if (hasExplicitSourceKey && !requestedSource) {
    const modelTreeConfig = modelSchema.treeConfig || {}
    const sourceFields = modelSchema.fields || []
    const keyField = normalizedOverrides.keyField
      || modelTreeConfig.keyField
      || pickSourceField(sourceFields, ['id'])
      || 'id'
    const parentField = normalizedOverrides.parentField
      || modelTreeConfig.parentField
      || pickSourceField(sourceFields, ['parentId', 'pid', 'parentCode'])
      || 'parentId'
    const labelField = normalizedOverrides.labelField
      || modelTreeConfig.labelField
      || pickSourceField(sourceFields, ['name', 'title', 'label'])
      || firstBusinessSourceField(sourceFields, [keyField, parentField])
      || ''
    return {
      enabled: normalizedOverrides.enabled ?? true,
      sourceModelCode: '',
      sourceModelName: '',
      sourceTableName: '',
      sourceConfigKey: '',
      sourceObjectId: null,
      treeApi: '',
      keyField,
      parentField,
      labelField,
      filterField: normalizedOverrides.filterField || parentField,
      targetField: normalizedOverrides.targetField || keyField,
      childrenField: normalizedOverrides.childrenField || modelTreeConfig.childrenField || 'children',
      treeTitle: normalizedOverrides.treeTitle || '',
      loadMode: normalizedOverrides.loadMode || modelTreeConfig.loadMode || 'full',
    }
  }

  const sourceRef = matchedSourceRef || resolveTreeSourceRef(modelSchema, normalizedOverrides.sourceModelCode)
  const primaryRef = resolvePrimaryModelRef(modelSchema)
  const sourceFields = sourceRef?.fields || modelSchema.fields || []
  const modelTreeConfig = sourceRef?.primary ? (modelSchema.treeConfig || {}) : {}
  const keyField = normalizedOverrides.keyField
    || modelTreeConfig.keyField
    || pickSourceField(sourceFields, ['id'])
    || 'id'
  const parentField = normalizedOverrides.parentField
    || modelTreeConfig.parentField
    || pickSourceField(sourceFields, ['parentId', 'pid', 'parentCode'])
    || 'parentId'
  const labelField = normalizedOverrides.labelField
    || modelTreeConfig.labelField
    || pickSourceField(sourceFields, ['name', 'title', 'label'])
    || firstBusinessSourceField(sourceFields, [keyField, parentField])
    || 'name'
  const relation = sourceRef?.primary ? null : findRelationToSource(primaryRef, sourceRef)
  const filterField = normalizedOverrides.filterField
    || (sourceRef?.primary ? parentField : relation?.sourceField)
    || parentField
  const targetField = normalizedOverrides.targetField
    || (sourceRef?.primary ? keyField : relation?.targetField)
    || keyField

  return {
    enabled: normalizedOverrides.enabled ?? true,
    sourceModelCode: sourceRef?.modelCode || '',
    sourceModelName: sourceRef?.modelName || modelSchema.businessName || '',
    sourceTableName: sourceRef?.tableName || modelSchema.tableName || '',
    sourceConfigKey: String(normalizedOverrides.sourceConfigKey || '').trim(),
    sourceObjectId: normalizedOverrides.sourceObjectId ?? null,
    treeApi: normalizedOverrides.treeApi || '',
    keyField,
    parentField,
    labelField,
    filterField,
    targetField,
    childrenField: normalizedOverrides.childrenField || modelTreeConfig.childrenField || 'children',
    treeTitle: normalizedOverrides.treeTitle || modelTreeConfig.treeTitle || `${sourceRef?.modelName || modelSchema.businessName || '业务'}树`,
    loadMode: normalizedOverrides.loadMode || modelTreeConfig.loadMode || 'full',
  }
}

export function normalizeTreeConfigAliases(source = {}) {
  const {
    nodeKeyField: _nodeKeyField,
    parentIdField: _parentIdField,
    displayField: _displayField,
    nameField: _nameField,
    rightFilterField: _rightFilterField,
    listFilterField: _listFilterField,
    nodeValueField: _nodeValueField,
    valueField: _valueField,
    title: _title,
    lazy: _lazy,
    ...canonicalSource
  } = source
  const targetField = source.targetField || source.nodeValueField || source.valueField
  return {
    ...canonicalSource,
    keyField: source.keyField || source.nodeKeyField,
    parentField: source.parentField || source.parentIdField,
    labelField: source.labelField || source.displayField || source.nameField,
    filterField: source.filterField || source.rightFilterField || source.listFilterField,
    targetField,
    treeTitle: source.treeTitle || source.title,
    loadMode: source.loadMode || (source.lazy === true ? 'lazy' : undefined),
  }
}

export function resolveTreeSourceRef(modelSchema = {}, sourceModelCode = '') {
  const refs = resolveTreeSourceRefs(modelSchema)
  if (!refs.length)
    return null
  const matched = findTreeSourceRef(modelSchema, sourceModelCode)
  if (matched)
    return matched
  // 未指定来源时才回退；显式来源找不到时由 resolveDefaultTreeConfig 保留外部对象配置
  if (String(sourceModelCode || '').trim())
    return null
  return refs.find(ref => !ref.primary) || refs.find(ref => ref.primary) || refs[0]
}

function findTreeSourceRef(modelSchema = {}, sourceValue = '') {
  const value = String(sourceValue || '').trim()
  if (!value)
    return null
  return resolveTreeSourceRefs(modelSchema).find(ref => (
    String(ref.modelCode || '') === value
    || String(ref.configKey || '') === value
    || String(ref.objectCode || '') === value
  )) || null
}

export function resolveTreeFieldOptions(modelSchema = {}, sourceModelCode = '') {
  const sourceRef = resolveTreeSourceRef(modelSchema, sourceModelCode)
  const fields = sourceRef?.fields || modelSchema.fields || []
  return fields
    .filter(field => !isHiddenPageField(field))
    .map(field => ({
      label: (field.rawLabel || field.label)
        ? `${field.rawLabel || field.label}（${sourceFieldName(field)}）`
        : sourceFieldName(field),
      value: sourceFieldName(field),
    }))
}

function resolvePrimaryModelRef(modelSchema = {}) {
  const refs = resolveTreeSourceRefs(modelSchema)
  return refs.find(ref => ref.primary) || refs[0] || null
}

function sourceFieldName(field = {}) {
  return field?.sourceField || field?.field || ''
}

function pickSourceField(fields = [], names = []) {
  const field = fields.find((item) => {
    const sourceField = sourceFieldName(item)
    return names.includes(sourceField) || names.includes(item.field)
  })
  return sourceFieldName(field)
}

function firstBusinessSourceField(fields = [], excluded = []) {
  const excludedSet = new Set(excluded.filter(Boolean))
  const field = fields.find((item) => {
    const sourceField = sourceFieldName(item)
    return sourceField && !excludedSet.has(sourceField) && !isReadonlySystemField(item)
  }) || fields.find(item => sourceFieldName(item) && !excludedSet.has(sourceFieldName(item)))
  return sourceFieldName(field)
}

function findRelationToSource(primaryRef, sourceRef) {
  if (!primaryRef || !sourceRef?.modelCode)
    return null
  return (primaryRef.relations || []).find(relation => relation?.targetObjectCode === sourceRef.modelCode) || null
}

