/**
 * @fileoverview 页面组件 spec（7 个 · scope: L）
 * @description 页面级装饰组件：返回、标题、文本、段落、统计、提示、标签。
 */

const backButton = {
  type: 'back-button',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '页面',
  label: '返回上一页',
  desc: '详情页返回入口',
  layout: { defaultSpan: 4, defaultW: 2, defaultH: 1 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 内容
    text: { group: '内容', priority: 'common', type: 'string', title: '按钮文案', default: '返回' },
    // 高级
    icon: { group: '高级', priority: 'advanced', type: 'icon', title: '图标', default: 'arrow-left' },
  } },
  dataSources: [],
  print: { hidden: true, breakAvoid: false },
}

const pageTitle = {
  type: 'page-title',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '页面',
  label: '页面标题',
  desc: '标题、副标题和状态提示',
  layout: { defaultSpan: 16, defaultW: 8, defaultH: 2 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 内容
    title: { group: '内容', priority: 'common', type: 'string', title: '标题' },
    subtitle: { group: '内容', priority: 'common', type: 'string', title: '副标题' },
    // 高级
    showBack: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示返回', default: false },
  } },
  dataSources: ['context'],
  print: { hidden: false, breakAvoid: true },
}

const textTitle = {
  type: 'text-title',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '内容',
  label: '标题',
  desc: '页面标题文本',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 2 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 内容
    text: { group: '内容', priority: 'common', type: 'string', title: '标题内容' },
    level: { group: '内容', priority: 'common', type: 'select', title: '标题级别', options: [
      { label: 'H1', value: 1 },
      { label: 'H2', value: 2 },
      { label: 'H3', value: 3 },
      { label: 'H4', value: 4 },
    ], default: 2 },
    // 高级
    align: { group: '高级', priority: 'advanced', type: 'select', title: '对齐', options: [
      { label: '左', value: 'left' },
      { label: '居中', value: 'center' },
      { label: '右', value: 'right' },
    ] },
    depth: { group: '高级', priority: 'advanced', type: 'select', title: '颜色深度', options: [
      { label: '1', value: 1 },
      { label: '2', value: 2 },
      { label: '3', value: 3 },
    ] },
    type: { group: '高级', priority: 'advanced', type: 'select', title: '文本类型', options: [
      { label: '默认', value: 'default' },
      { label: '主要', value: 'primary' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
      { label: '信息', value: 'info' },
    ] },
  } },
  dataSources: ['context'],
  print: { hidden: false, breakAvoid: true },
}

const paragraph = {
  type: 'paragraph',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '内容',
  label: '段落',
  desc: '多行说明文字',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 3 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 内容
    content: { group: '内容', priority: 'common', type: 'string', title: '段落内容' },
    // 高级
    align: { group: '高级', priority: 'advanced', type: 'select', title: '对齐', options: [
      { label: '左', value: 'left' },
      { label: '居中', value: 'center' },
      { label: '右', value: 'right' },
    ] },
    indent: { group: '高级', priority: 'advanced', type: 'boolean', title: '首行缩进', default: false },
    depth: { group: '高级', priority: 'advanced', type: 'select', title: '颜色深度', options: [
      { label: '1', value: 1 },
      { label: '2', value: 2 },
      { label: '3', value: 3 },
    ] },
  } },
  dataSources: ['context'],
  print: { hidden: false, breakAvoid: false },
}

const statistic = {
  type: 'statistic',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '数据',
  label: '统计数值',
  desc: '单个指标数值展示',
  layout: { defaultSpan: 6, defaultW: 3, defaultH: 3 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 数据
    label: { group: '数据', priority: 'common', type: 'string', title: '指标名称' },
    value: { group: '数据', priority: 'common', type: 'number', title: '数值' },
    prefix: { group: '数据', priority: 'common', type: 'string', title: '前缀' },
    suffix: { group: '数据', priority: 'common', type: 'string', title: '后缀' },
    // 高级
    precision: { group: '高级', priority: 'advanced', type: 'number', title: '精度' },
    animation: { group: '高级', priority: 'advanced', type: 'boolean', title: '数值动画', default: false },
    tabularNums: { group: '高级', priority: 'advanced', type: 'boolean', title: '等宽数字', default: false },
  } },
  dataSources: ['context', 'remote'],
  print: { hidden: false, breakAvoid: false },
}

const textTip = {
  type: 'text-tip',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '内容',
  label: '文字提示',
  desc: '轻量提示文本',
  layout: { defaultSpan: 10, defaultW: 5, defaultH: 2 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 内容
    content: { group: '内容', priority: 'common', type: 'string', title: '提示内容' },
    type: { group: '内容', priority: 'common', type: 'select', title: '类型', options: [
      { label: '信息', value: 'info' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ], default: 'info' },
    // 高级
    showIcon: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示图标', default: true },
    bordered: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示边框', default: true },
    closable: { group: '高级', priority: 'advanced', type: 'boolean', title: '可关闭', default: false },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: false },
}

const tagList = {
  type: 'tag-list',
  aliases: [],
  scope: 'L',
  category: 'page',
  group: '内容',
  label: '标签列表',
  desc: '状态、分类、关键词展示',
  layout: { defaultSpan: 8, defaultW: 4, defaultH: 2 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 内容
    tags: { group: '内容', priority: 'common', type: 'json', title: '标签列表' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
    // 高级
    closable: { group: '高级', priority: 'advanced', type: 'boolean', title: '可关闭', default: false },
    round: { group: '高级', priority: 'advanced', type: 'boolean', title: '圆角', default: false },
    bordered: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示边框', default: true },
    type: { group: '高级', priority: 'advanced', type: 'select', title: '标签类型', options: [
      { label: '默认', value: 'default' },
      { label: '信息', value: 'info' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
  } },
  dataSources: ['context', 'static'],
  print: { hidden: false, breakAvoid: false },
}

export const pageComponentSpecs = [
  backButton,
  pageTitle,
  textTitle,
  paragraph,
  statistic,
  textTip,
  tagList,
]
