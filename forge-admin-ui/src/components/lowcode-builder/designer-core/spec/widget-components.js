/**
 * @fileoverview 页面挂件 spec（18 个 · scope: F+L）
 * @description 表单 / 列表页两侧通用的挂件组件：富文本、穿梭框、水印、Vue 组件、HTML 标签、
 *   Markdown、日历、代码、倒计时、描述、公示、列表、日志、数值动画、面包屑、菜单、
 *   分页、面板分隔（表单画布 isPageWidgetComponentKey 全支持）。
 */

// defaultW / defaultH: 列表页 grid 尺寸（12 列）
const commonWidgetPrint = { hidden: false, breakAvoid: false }

const richText = {
  type: 'rich-text',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '内容',
  label: '富文本框',
  desc: '带工具栏的富文本编辑器',
  layout: { defaultSpan: 16, defaultW: 8, defaultH: 6 },
  container: false,
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    content: { type: 'string', title: '内容', format: 'html' },
    editorMode: { type: 'select', title: '编辑模式', options: [
      { label: '可视化', value: 'visual' },
      { label: '源码', value: 'source' },
    ], default: 'visual' },
    readonly: { type: 'boolean', title: '只读', default: false },
    minHeight: { type: 'number', title: '最小高度', default: 180 },
  } },
  dataSources: ['context'],
  print: commonWidgetPrint,
}

const transferWidget = {
  type: 'transfer',
  aliases: ['widget-transfer'],
  // 双重身份（字段 + 挂件）：表单设计器统一面板以字段模板形态拖入
  scope: 'F+L',
  category: 'widget',
  group: '数据',
  label: '穿梭框',
  desc: '支持静态选项和远程接口选项',
  layout: { defaultSpan: 16, defaultW: 8, defaultH: 6 },
  container: false,
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    sourceTitle: { type: 'string', title: '源栏标题', default: '可选项' },
    targetTitle: { type: 'string', title: '目标栏标题', default: '已选项' },
    filterable: { type: 'boolean', title: '可过滤', default: true },
    virtualScroll: { type: 'boolean', title: '虚拟滚动', default: false },
  } },
  dataSources: ['static', 'remote'],
  print: commonWidgetPrint,
  fieldDefaults: { fieldType: 'MULTI_SELECT', businessFieldType: 'MULTI_SELECT', dataType: 'text', componentType: 'transfer', length: null, precision: null, queryType: 'in' },
}

const watermark = {
  type: 'watermark',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '高级',
  label: '水印',
  desc: '区域水印背景',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 4 },
  container: false,
  propsSchema: { properties: {
    content: { type: 'string', title: '水印文本', default: '内部资料' },
    fontSize: { type: 'number', title: '字号', default: 14 },
    fontColor: { type: 'color', title: '字色', default: 'rgba(128,128,128,.3)' },
    rotate: { type: 'number', title: '旋转角度', default: 0 },
    width: { type: 'number', title: '宽度', default: 32 },
    height: { type: 'number', title: '高度', default: 32 },
  } },
  dataSources: ['context'],
  print: { hidden: true, breakAvoid: false },
}

const vueComponent = {
  type: 'vue-component',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '高级',
  label: 'Vue 组件',
  desc: '配置模板、脚本、样式和 props',
  layout: { defaultSpan: 16, defaultW: 8, defaultH: 6 },
  container: false,
  propsSchema: { properties: {
    componentName: { type: 'string', title: '组件名' },
    templateCode: { type: 'code', title: '模板', format: 'html' },
    scriptCode: { type: 'code', title: '脚本', format: 'javascript' },
    styleCode: { type: 'code', title: '样式', format: 'css' },
    propsJson: { type: 'json', title: 'Props JSON' },
    previewMode: { type: 'select', title: '预览模式', options: [
      { label: '安全模板', value: 'safe-template' },
      { label: '完整渲染', value: 'full' },
    ], default: 'safe-template' },
  } },
  dataSources: ['context'],
  print: commonWidgetPrint,
}

const htmlTag = {
  type: 'html-tag',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '内容',
  label: 'HTML 标签',
  desc: '配置标签、属性和安全 HTML',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 5 },
  container: false,
  propsSchema: { properties: {
    tagName: { type: 'string', title: '标签名', default: 'section' },
    htmlContent: { type: 'string', title: 'HTML 内容', format: 'html' },
    renderMode: { type: 'select', title: '渲染模式', options: [
      { label: 'HTML', value: 'html' },
      { label: '纯文本', value: 'text' },
    ], default: 'html' },
    sanitize: { type: 'boolean', title: '安全过滤', default: true },
  } },
  dataSources: ['context'],
  print: commonWidgetPrint,
}

const markdown = {
  type: 'markdown',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '内容',
  label: 'Markdown',
  desc: 'Markdown 源码与预览',
  layout: { defaultSpan: 16, defaultW: 8, defaultH: 6 },
  container: false,
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    content: { type: 'string', title: 'Markdown 内容', format: 'markdown' },
    previewMode: { type: 'select', title: '预览模式', options: [
      { label: '分栏', value: 'split' },
      { label: '预览', value: 'preview' },
    ], default: 'split' },
    height: { type: 'number', title: '高度', default: 320 },
    breaks: { type: 'boolean', title: '换行转 br', default: true },
  } },
  dataSources: ['context'],
  print: commonWidgetPrint,
}

const calendar = {
  type: 'calendar',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '数据',
  label: '日历',
  desc: 'Naive UI 日历',
  layout: { defaultSpan: 16, defaultW: 8, defaultH: 8 },
  container: false,
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    size: { type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
    showTitle: { type: 'boolean', title: '显示标题', default: true },
  } },
  dataSources: ['context'],
  print: commonWidgetPrint,
}

const code = {
  type: 'code',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '内容',
  label: '代码',
  desc: '代码块展示',
  layout: { defaultSpan: 16, defaultW: 8, defaultH: 5 },
  container: false,
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    code: { type: 'code', title: '代码内容' },
    language: { type: 'select', title: '语言', options: [
      { label: 'JavaScript', value: 'javascript' },
      { label: 'Python', value: 'python' },
      { label: 'Java', value: 'java' },
      { label: 'SQL', value: 'sql' },
      { label: 'JSON', value: 'json' },
    ], default: 'javascript' },
    showLineNumbers: { type: 'boolean', title: '行号', default: true },
    wordWrap: { type: 'boolean', title: '自动换行', default: true },
  } },
  dataSources: ['context'],
  print: commonWidgetPrint,
}

const countdown = {
  type: 'countdown',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '数据',
  label: '倒计时',
  desc: '倒计时展示',
  layout: { defaultSpan: 8, defaultW: 4, defaultH: 3 },
  container: false,
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    duration: { type: 'number', title: '时长（ms）', default: 3600000 },
    active: { type: 'boolean', title: '激活', default: true },
    precision: { type: 'number', title: '精度（小数位）', default: 0 },
    separator: { type: 'string', title: '分隔符', default: ':' },
  } },
  dataSources: ['context'],
  print: commonWidgetPrint,
}

const descriptions = {
  type: 'descriptions',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '数据',
  label: '描述',
  desc: '描述信息列表',
  layout: { defaultSpan: 16, defaultW: 8, defaultH: 4 },
  container: false,
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    column: { type: 'number', title: '列数', default: 2, min: 1, max: 4 },
    bordered: { type: 'boolean', title: '边框', default: false },
    labelPlacement: { type: 'select', title: '标签位置', options: [
      { label: '左', value: 'left' },
      { label: '上', value: 'top' },
    ], default: 'left' },
    size: { type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'small' },
    itemsText: { type: 'json', title: '描述项 JSON' },
  } },
  dataSources: ['context'],
  print: commonWidgetPrint,
}

const announcement = {
  type: 'announcement',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '内容',
  label: '公示',
  desc: '信息公示栏',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 3 },
  container: false,
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
  print: commonWidgetPrint,
}

const listWidget = {
  type: 'list',
  aliases: ['widget-list'],
  scope: 'F+L',
  category: 'widget',
  group: '数据',
  label: '列表',
  desc: '通用列表展示',
  layout: { defaultSpan: 14, defaultW: 7, defaultH: 5 },
  container: false,
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    bordered: { type: 'boolean', title: '边框', default: false },
    hoverable: { type: 'boolean', title: '悬停效果', default: true },
    size: { type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'small' },
    itemsText: { type: 'json', title: '列表项 JSON' },
  } },
  dataSources: ['context', 'remote'],
  print: commonWidgetPrint,
}

const log = {
  type: 'log',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '数据',
  label: '日志',
  desc: '日志文本展示',
  layout: { defaultSpan: 16, defaultW: 8, defaultH: 5 },
  container: false,
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    log: { type: 'string', title: '日志内容' },
    rows: { type: 'number', title: '行数', default: 6 },
    fontSize: { type: 'number', title: '字号', default: 12 },
  } },
  dataSources: ['context', 'remote'],
  print: commonWidgetPrint,
}

const numberAnimation = {
  type: 'number-animation',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '数据',
  label: '数值动画',
  desc: '滚动数字展示',
  layout: { defaultSpan: 8, defaultW: 4, defaultH: 3 },
  container: false,
  propsSchema: { properties: {
    title: { type: 'string', title: '标题' },
    from: { type: 'number', title: '起始值', default: 0 },
    to: { type: 'number', title: '目标值', default: 10000 },
    precision: { type: 'number', title: '精度', default: 0 },
    duration: { type: 'number', title: '动画时长（ms）', default: 1200 },
    prefix: { type: 'string', title: '前缀' },
    suffix: { type: 'string', title: '后缀' },
    color: { type: 'color', title: '颜色' },
  } },
  dataSources: ['context'],
  print: commonWidgetPrint,
}

const breadcrumb = {
  type: 'breadcrumb',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '导航',
  label: '面包屑',
  desc: '路径导航',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 2 },
  container: false,
  propsSchema: { properties: {
    itemsText: { type: 'json', title: '面包屑项 JSON' },
    separator: { type: 'string', title: '分隔符', default: '/' },
  } },
  dataSources: ['context'],
  print: commonWidgetPrint,
}

const menu = {
  type: 'menu',
  aliases: ['widget-menu'],
  scope: 'F+L',
  category: 'widget',
  group: '导航',
  label: '菜单',
  desc: '导航菜单',
  layout: { defaultSpan: 10, defaultW: 5, defaultH: 6 },
  container: false,
  propsSchema: { properties: {
    mode: { type: 'select', title: '模式', options: [
      { label: '垂直', value: 'vertical' },
      { label: '水平', value: 'horizontal' },
    ], default: 'vertical' },
    collapsed: { type: 'boolean', title: '折叠', default: false },
    optionsText: { type: 'json', title: '菜单项 JSON' },
  } },
  dataSources: ['context', 'remote'],
  print: commonWidgetPrint,
}

const pagination = {
  type: 'pagination',
  aliases: ['widget-pagination'],
  scope: 'F+L',
  category: 'widget',
  group: '导航',
  label: '分页',
  desc: '分页控件',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 2 },
  container: false,
  propsSchema: { properties: {
    page: { type: 'number', title: '当前页', default: 1 },
    pageSize: { type: 'number', title: '每页条数', default: 10 },
    itemCount: { type: 'number', title: '总条数', default: 0 },
    showSizePicker: { type: 'boolean', title: '页码选择器', default: true },
    simple: { type: 'boolean', title: '简洁模式', default: false },
  } },
  dataSources: ['context'],
  print: { hidden: true, breakAvoid: false },
}

const split = {
  type: 'split',
  aliases: [],
  scope: 'F+L',
  category: 'widget',
  group: '布局',
  label: '面板分隔',
  desc: '可拖拽分隔面板',
  layout: { defaultSpan: 24, defaultW: 8, defaultH: 5 },
  container: true,
  maxDepth: 2,
  accept: [],
  propsSchema: { properties: {
    direction: { type: 'select', title: '方向', options: [
      { label: '水平', value: 'horizontal' },
      { label: '垂直', value: 'vertical' },
    ], default: 'horizontal' },
    defaultSize: { type: 'number', title: '初始比例', default: 0.5, min: 0.1, max: 0.9, step: 0.1 },
    min: { type: 'number', title: '最小比例', default: 0.2 },
    max: { type: 'number', title: '最大比例', default: 0.8 },
  } },
  dataSources: [],
  print: commonWidgetPrint,
}

export const widgetComponentSpecs = [
  richText,
  transferWidget,
  watermark,
  vueComponent,
  htmlTag,
  markdown,
  calendar,
  code,
  countdown,
  descriptions,
  announcement,
  listWidget,
  log,
  numberAnimation,
  breadcrumb,
  menu,
  pagination,
  split,
]
