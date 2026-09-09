/**
 * @fileoverview Zone 操作组件 spec（13 个 · scope: L）
 * @description 列表页 Zone 内的操作按钮和功能组件：
 *   查询集、自定义查询、导入/导出/新增/重置按钮、操作按钮、按钮组、链接，
 *   以及信息面板、步骤条、时间线、空状态、自定义 HTML。
 */

const querySet = {
  type: 'query-set',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '操作',
  label: '查询集',
  desc: '选择查询字段、调整顺序',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 4 },
  container: false,
  meta: { multiField: true, zones: ['search', 'table'], defaultWidth: 640, defaultHeight: 132 },
  propsSchema: { properties: {
    fields: { type: 'customEditor', editor: 'QuerySetFieldsEditor', title: '查询字段' },
    layout: { type: 'select', title: '布局方式', options: [
      { label: '内联', value: 'inline' },
      { label: '栅格', value: 'grid' },
    ], default: 'inline' },
  } },
  dataSources: [],
  print: { hidden: true, breakAvoid: false },
}

const customQuery = {
  type: 'custom-query',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '操作',
  label: '自定义查询',
  desc: '高级查询入口',
  layout: { defaultSpan: 3, defaultW: 2, defaultH: 1 },
  container: false,
  meta: { zones: ['search', 'table'], defaultWidth: 128, defaultHeight: 40 },
  propsSchema: { properties: {
    text: { type: 'string', title: '按钮文案', default: '高级查询' },
    icon: { type: 'icon', title: '图标', default: 'filter' },
  } },
  dataSources: [],
  print: { hidden: true, breakAvoid: false },
}

const importButton = {
  type: 'import-button',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '操作',
  label: '导入',
  desc: 'Excel 批量导入',
  layout: { defaultSpan: 2, defaultW: 2, defaultH: 1 },
  container: false,
  meta: { zones: ['table'], defaultWidth: 104, defaultHeight: 40 },
  propsSchema: { properties: {
    text: { type: 'string', title: '按钮文案', default: '导入' },
    accept: { type: 'string', title: '文件类型', default: '.xlsx,.xls' },
    maxSize: { type: 'number', title: '大小上限（MB）', default: 10 },
    templateUrl: { type: 'string', title: '模板下载地址' },
  } },
  dataSources: ['managed'],
  print: { hidden: true, breakAvoid: false },
}

const exportButton = {
  type: 'export-button',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '操作',
  label: '导出',
  desc: 'Excel 动态导出',
  layout: { defaultSpan: 2, defaultW: 2, defaultH: 1 },
  container: false,
  meta: { zones: ['table'], defaultWidth: 104, defaultHeight: 40 },
  propsSchema: { properties: {
    text: { type: 'string', title: '按钮文案', default: '导出' },
    exportAll: { type: 'boolean', title: '导出全量', default: false },
    fileName: { type: 'string', title: '文件名' },
  } },
  dataSources: ['managed'],
  print: { hidden: true, breakAvoid: false },
}

const addButton = {
  type: 'add-button',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '操作',
  label: '新增',
  desc: '打开新增表单',
  layout: { defaultSpan: 2, defaultW: 2, defaultH: 1 },
  container: false,
  meta: { zones: ['table'], defaultWidth: 104, defaultHeight: 40 },
  propsSchema: { properties: {
    text: { type: 'string', title: '按钮文案', default: '新增' },
    icon: { type: 'icon', title: '图标', default: 'add' },
    type: { type: 'select', title: '按钮类型', options: [
      { label: '主要', value: 'primary' },
      { label: '默认', value: 'default' },
    ], default: 'primary' },
  } },
  dataSources: [],
  print: { hidden: true, breakAvoid: false },
}

const resetButton = {
  type: 'reset-button',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '操作',
  label: '重置',
  desc: '清空当前表单',
  layout: { defaultSpan: 2, defaultW: 2, defaultH: 1 },
  container: false,
  meta: { zones: ['search'], defaultWidth: 104, defaultHeight: 40 },
  propsSchema: { properties: {
    text: { type: 'string', title: '按钮文案', default: '重置' },
    icon: { type: 'icon', title: '图标', default: 'refresh' },
  } },
  dataSources: [],
  print: { hidden: true, breakAvoid: false },
}

const actionButton = {
  // 物料清单 §合并规则：action-button 合并表单侧 button（aliases 兼容存量 componentKey）
  type: 'action-button',
  aliases: ['button'],
  scope: 'F+L',
  category: 'business',
  group: '操作',
  label: '按钮',
  desc: '单个命令按钮',
  layout: { defaultSpan: 4, defaultW: 2, defaultH: 1 },
  container: false,
  meta: { defaultWidth: 104, defaultHeight: 40 },
  propsSchema: { properties: {
    text: { group: '基础', type: 'string', title: '按钮文案' },
    type: { group: '基础', type: 'select', title: '按钮类型', options: [
      { label: '主要', value: 'primary' },
      { label: '信息', value: 'info' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
      { label: '默认', value: 'default' },
    ], default: 'default' },
    size: { group: '基础', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
    icon: { group: '基础', type: 'icon', title: '图标' },
    // 外观（与两侧画布渲染端绑定的 props key 一一对应）
    secondary: { group: '外观', type: 'boolean', title: '次级按钮', default: false },
    tertiary: { group: '外观', type: 'boolean', title: '三级按钮', default: false },
    quaternary: { group: '外观', type: 'boolean', title: '四级按钮', default: false },
    dashed: { group: '外观', type: 'boolean', title: '虚线边框', default: false },
    round: { group: '外观', type: 'boolean', title: '圆角', default: false },
    block: { group: '外观', type: 'boolean', title: '块级按钮', default: false, desc: '宽度撑满所在容器' },
    // 状态
    loading: { group: '状态', type: 'boolean', title: '加载中', default: false },
    disabled: { group: '状态', type: 'boolean', title: '禁用', default: false },
  } },
  dataSources: [],
  print: { hidden: true, breakAvoid: false },
}

const buttonGroup = {
  type: 'button-group',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '操作',
  label: '按钮组',
  desc: '多个页面操作按钮',
  layout: { defaultSpan: 10, defaultW: 5, defaultH: 2 },
  container: false,
  meta: { defaultWidth: 200, defaultHeight: 40 },
  propsSchema: { properties: {
    buttons: { type: 'json', title: '按钮列表' },
    size: { type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
    gap: { type: 'number', title: '间距', default: 8 },
  } },
  dataSources: [],
  print: { hidden: true, breakAvoid: false },
}

const link = {
  type: 'link',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '操作',
  label: '链接',
  desc: '页面跳转或外部链接',
  layout: { defaultSpan: 6, defaultW: 3, defaultH: 1 },
  container: false,
  meta: { defaultWidth: 120, defaultHeight: 40 },
  propsSchema: { properties: {
    text: { type: 'string', title: '链接文案' },
    href: { type: 'string', title: '链接地址' },
    target: { type: 'select', title: '打开方式', options: [
      { label: '当前页', value: '_self' },
      { label: '新窗口', value: '_blank' },
    ], default: '_self' },
    type: { type: 'select', title: '类型', options: [
      { label: '主要', value: 'primary' },
      { label: '信息', value: 'info' },
      { label: '默认', value: 'default' },
    ], default: 'primary' },
  } },
  dataSources: [],
  print: { hidden: true, breakAvoid: false },
}

const infoPanel = {
  type: 'info-panel',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '内容',
  label: '提示面板',
  desc: '说明、警告、成功提示',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 2 },
  container: false,
  meta: { defaultWidth: 600, defaultHeight: 64 },
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    content: { type: 'string', title: '内容' },
    type: { type: 'select', title: '类型', options: [
      { label: '信息', value: 'info' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ], default: 'info' },
    closable: { type: 'boolean', title: '可关闭', default: false },
    showIcon: { type: 'boolean', title: '显示图标', default: true },
  } },
  dataSources: ['context'],
  print: { hidden: false, breakAvoid: false },
}

const steps = {
  type: 'steps',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '内容',
  label: '步骤条',
  desc: '流程步骤展示',
  layout: { defaultSpan: 16, defaultW: 8, defaultH: 2 },
  container: false,
  meta: { defaultWidth: 800, defaultHeight: 64 },
  propsSchema: { properties: {
    current: { type: 'number', title: '当前步骤', default: 0 },
    items: { type: 'json', title: '步骤列表' },
    direction: { type: 'select', title: '方向', options: [
      { label: '水平', value: 'horizontal' },
      { label: '垂直', value: 'vertical' },
    ], default: 'horizontal' },
    size: { type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
    ], default: 'medium' },
  } },
  dataSources: ['context'],
  print: { hidden: false, breakAvoid: true },
}

const timeline = {
  type: 'timeline',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '内容',
  label: '时间线',
  desc: '操作记录和流转轨迹',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 5 },
  container: false,
  meta: { defaultWidth: 600, defaultHeight: 160 },
  propsSchema: { properties: {
    items: { type: 'json', title: '时间线条目' },
    horizontal: { type: 'boolean', title: '水平方向', default: false },
    itemPlacement: { type: 'select', title: '内容位置', options: [
      { label: '左', value: 'left' },
      { label: '右', value: 'right' },
    ], default: 'left' },
  } },
  dataSources: ['context', 'remote'],
  print: { hidden: false, breakAvoid: true },
}

const emptyState = {
  type: 'empty-state',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '内容',
  label: '空状态',
  desc: '暂无数据、引导操作',
  layout: { defaultSpan: 10, defaultW: 5, defaultH: 4 },
  container: false,
  meta: { defaultWidth: 500, defaultHeight: 128 },
  propsSchema: { properties: {
    description: { type: 'string', title: '描述文案', default: '暂无数据' },
    image: { type: 'string', title: '图片地址' },
    showAction: { type: 'boolean', title: '显示操作按钮', default: false },
    actionText: { type: 'string', title: '按钮文案' },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: false },
}

const customHtml = {
  type: 'custom-html',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '内容',
  label: '说明文本',
  desc: '富文本 / Markdown 提示',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 3 },
  container: false,
  meta: { defaultWidth: 600, defaultHeight: 96 },
  propsSchema: { properties: {
    content: { type: 'string', title: '内容', format: 'html' },
    renderMode: { type: 'select', title: '渲染模式', options: [
      { label: 'HTML', value: 'html' },
      { label: 'Markdown', value: 'markdown' },
    ], default: 'html' },
  } },
  dataSources: ['context'],
  print: { hidden: false, breakAvoid: false },
}

const statsStrip = {
  type: 'stats-strip',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '数据',
  label: '指标卡片',
  desc: '顶部 KPI / 统计条',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 2 },
  container: false,
  meta: { defaultWidth: 1200, defaultHeight: 64 },
  propsSchema: { properties: {
    items: { type: 'json', title: '指标列表' },
    showDivider: { type: 'boolean', title: '分隔线', default: true },
  } },
  dataSources: ['context', 'remote'],
  print: { hidden: false, breakAvoid: true },
}

export const zoneActionComponentSpecs = [
  querySet,
  customQuery,
  importButton,
  exportButton,
  addButton,
  resetButton,
  actionButton,
  buttonGroup,
  link,
  infoPanel,
  steps,
  timeline,
  emptyState,
  customHtml,
  statsStrip,
]
