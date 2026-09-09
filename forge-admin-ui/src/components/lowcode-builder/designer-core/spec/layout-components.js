/**
 * @fileoverview 布局容器 spec（11 + 4 包装节点 = 15 个 · scope: F+L）
 * @description grid / table / card / tabs / collapse / box / divider / spacer / space / groupTitle / formSectionTitle
 *   + 包装节点: tabPane / collapseItem / col / tableCell
 */

const grid = {
  type: 'grid',
  aliases: ['row', 'fcRow', 'grid-layout'],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: '栅格布局',
  desc: '单行多列栅格容器，每格独立 span',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 6 },
  container: true,
  maxDepth: 4,
  accept: [],
  // C1：属性 key 与渲染器真实消费 key 一一对齐 —
  // 列表画布 GridBlockRenderer（gridLayoutStyle / gridCellStyle）消费
  // columns / gutter / rowGap / alignItems / justifyItems / cellMinHeight / cellBackground / showCellBorder；
  // 表单画布 AiFormLayoutNodes（n-grid）消费 columns / gutter，其余 key 由表单侧排除表跳过。
  propsSchema: { properties: {
    // 常用 · 栅格
    columns: { group: '栅格', priority: 'common', type: 'number', title: '总列数', default: 24, min: 1, max: 24 },
    gutter: { group: '栅格', priority: 'common', type: 'number', title: '列间距', default: 16 },
    rowGap: { group: '栅格', priority: 'common', type: 'number', title: '行距', default: 0 },
    cellMinHeight: { group: '栅格', priority: 'common', type: 'number', title: '格子最小高度', default: 120, min: 24 },
    // 常用 · 外观
    alignItems: { group: '外观', priority: 'common', type: 'select', title: '垂直对齐', options: [
      { label: '垂直填满', value: 'stretch' },
      { label: '靠上', value: 'start' },
      { label: '垂直居中', value: 'center' },
      { label: '靠下', value: 'end' },
    ], default: 'stretch' },
    justifyItems: { group: '外观', priority: 'common', type: 'select', title: '水平对齐', options: [
      { label: '水平填满', value: 'stretch' },
      { label: '靠左', value: 'start' },
      { label: '水平居中', value: 'center' },
      { label: '靠右', value: 'end' },
    ], default: 'stretch' },
    showCellBorder: { group: '外观', priority: 'common', type: 'boolean', title: '格子边框', default: true },
    // 高级
    cellBackground: { group: '高级', priority: 'advanced', type: 'color', title: '格子背景' },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: true },
}

const table = {
  type: 'table',
  aliases: ['fcTable'],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: '表格布局',
  desc: '行列式表格容器',
  layout: { defaultSpan: 24 },
  container: true,
  maxDepth: 4,
  accept: [],
  propsSchema: { properties: {
    // 常用 · 布局
    rows: { group: '布局', priority: 'common', type: 'number', title: '行数', default: 2, min: 1 },
    cols: { group: '布局', priority: 'common', type: 'number', title: '列数', default: 2, min: 1 },
    bordered: { group: '布局', priority: 'common', type: 'boolean', title: '单元格边框', default: true },
    cellPadding: { group: '布局', priority: 'common', type: 'number', title: '单元格内边距', default: 8 },
    // 高级
    striped: { group: '高级', priority: 'advanced', type: 'boolean', title: '斑马纹', default: false },
    size: { group: '高级', priority: 'advanced', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: true },
}

const card = {
  type: 'card',
  aliases: ['elCard'],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: '卡片容器',
  desc: '页面分组容器 / 信息卡片',
  layout: { defaultSpan: 24, defaultW: 6, defaultH: 5 },
  container: true,
  maxDepth: 4,
  accept: [],
  propsSchema: { properties: {
    // 常用 · 布局
    title: { group: '布局', priority: 'common', type: 'string', title: '标题' },
    size: { group: '布局', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
      { label: '超大', value: 'huge' },
    ], default: 'medium' },
    bordered: { group: '布局', priority: 'common', type: 'boolean', title: '边框', default: true },
    // 常用 · 外观
    embedded: { group: '外观', priority: 'common', type: 'boolean', title: '嵌入模式', default: false },
    hoverable: { group: '外观', priority: 'common', type: 'boolean', title: '悬停效果', default: false },
    // 高级
    collapsible: { group: '高级', priority: 'advanced', type: 'boolean', title: '可折叠', default: false },
    segmented: { group: '高级', priority: 'advanced', type: 'select', title: '分割线', options: [
      { label: '无', value: 'false' },
      { label: '内容区', value: 'content' },
      { label: '操作区', value: 'action' },
      { label: '页脚', value: 'footer' },
    ] },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: true },
}

const tabs = {
  type: 'tabs',
  aliases: ['elTabs'],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: 'Tabs 标签页',
  desc: '多页签布局容器',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 6 },
  container: true,
  maxDepth: 4,
  accept: ['tabPane'],
  propsSchema: { properties: {
    // 常用 · 布局（key 与两侧画布/运行态渲染端消费的 props.type 对齐）
    type: { group: '布局', priority: 'common', type: 'select', title: '页签样式', options: [
      { label: '线条', value: 'line' },
      { label: '卡片', value: 'card' },
      { label: '分段', value: 'segment' },
    ], default: 'line' },
    placement: { group: '布局', priority: 'common', type: 'select', title: '位置', options: [
      { label: '上', value: 'top' },
      { label: '下', value: 'bottom' },
      { label: '左', value: 'left' },
      { label: '右', value: 'right' },
    ], default: 'top' },
    size: { group: '布局', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
    // 高级
    trigger: { group: '高级', priority: 'advanced', type: 'select', title: '触发方式', options: [
      { label: '点击', value: 'click' },
      { label: '悬停', value: 'hover' },
    ], default: 'click' },
    animated: { group: '高级', priority: 'advanced', type: 'boolean', title: '切换动画', default: true },
    // Naive UI 仅在 type="card" 时渲染页签关闭按钮（Tab.mjs: mergedClosable && type === 'card'），
    // line/segment 下开关无效 —— desc 提示防止误判"点了没反应"
    closable: { group: '高级', priority: 'advanced', type: 'boolean', title: '可关闭', default: false, desc: '关闭按钮仅在"卡片"样式下显示' },
    addable: { group: '高级', priority: 'advanced', type: 'boolean', title: '可新增页签', default: false },
    tabsPadding: { group: '高级', priority: 'advanced', type: 'number', title: '页签区内边距', min: 0, default: 0 },
    justifyContent: { group: '高级', priority: 'advanced', type: 'select', title: '标签对齐', options: [
      { label: '起始', value: 'start' },
      { label: '居中', value: 'center' },
      { label: '末尾', value: 'end' },
      { label: '均分', value: 'space-around' },
      { label: '两端', value: 'space-between' },
      { label: '均等', value: 'space-evenly' },
    ] },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: true },
}

const collapse = {
  type: 'collapse',
  aliases: ['elCollapse'],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: '折叠面板',
  desc: '可折叠分组容器',
  layout: { defaultSpan: 24 },
  container: true,
  maxDepth: 4,
  accept: ['collapseItem'],
  propsSchema: { properties: {
    // 常用 · 布局
    accordion: { group: '布局', priority: 'common', type: 'boolean', title: '手风琴模式', default: false },
    // 高级
    defaultExpandedNames: { group: '高级', priority: 'advanced', type: 'json', title: '默认展开面板' },
    arrowPlacement: { group: '高级', priority: 'advanced', type: 'select', title: '箭头位置', options: [
      { label: '左', value: 'left' },
      { label: '右', value: 'right' },
    ], default: 'left' },
    displayDirective: { group: '高级', priority: 'advanced', type: 'select', title: '显示指令', options: [
      { label: 'if', value: 'if' },
      { label: 'show', value: 'show' },
    ], default: 'if' },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: true },
}

const box = {
  type: 'box',
  aliases: ['box-layout'],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: '盒子布局',
  desc: 'Flex 弹性容器（横向/纵向）',
  layout: { defaultSpan: 24 },
  container: true,
  maxDepth: 4,
  accept: [],
  // C1：direction / wrap / alignItems / justifyContent / gap 与 GridBlockRenderer boxLayoutStyle
  // 消费 key 及有效值一一对齐（flexDirection 期望 row/column，非 horizontal/vertical）；
  // showDivider 渲染器不消费，不声明。
  propsSchema: { properties: {
    // 常用 · 布局
    direction: { group: '布局', priority: 'common', type: 'select', title: '方向', options: [
      { label: '横向', value: 'row' },
      { label: '纵向', value: 'column' },
    ], default: 'row' },
    justifyContent: { group: '布局', priority: 'common', type: 'select', title: '主轴对齐', options: [
      { label: '起始', value: 'flex-start' },
      { label: '居中', value: 'center' },
      { label: '末尾', value: 'flex-end' },
      { label: '两端', value: 'space-between' },
      { label: '均匀', value: 'space-around' },
    ] },
    alignItems: { group: '布局', priority: 'common', type: 'select', title: '交叉轴对齐', options: [
      { label: '起始', value: 'flex-start' },
      { label: '居中', value: 'center' },
      { label: '末尾', value: 'flex-end' },
      { label: '拉伸', value: 'stretch' },
    ], default: 'stretch' },
    gap: { group: '布局', priority: 'common', type: 'number', title: '间距', default: 12 },
    // 高级
    wrap: { group: '高级', priority: 'advanced', type: 'boolean', title: '换行', default: true },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: false },
}

const divider = {
  type: 'divider',
  aliases: [],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: '分隔线',
  desc: '横向 / 竖向分隔',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 1 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 布局
    direction: { group: '布局', priority: 'common', type: 'select', title: '方向', options: [
      { label: '水平', value: 'horizontal' },
      { label: '垂直', value: 'vertical' },
    ], default: 'horizontal' },
    dashed: { group: '布局', priority: 'common', type: 'boolean', title: '虚线', default: false },
    titlePlacement: { group: '布局', priority: 'common', type: 'select', title: '文案位置', options: [
      { label: '左', value: 'left' },
      { label: '中', value: 'center' },
      { label: '右', value: 'right' },
    ] },
    // 高级
    vertical: { group: '高级', priority: 'advanced', type: 'boolean', title: '垂直模式', default: false },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: false },
}

const spacer = {
  type: 'spacer',
  aliases: [],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: '留白占位',
  desc: '调整页面间距',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 1 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 布局
    height: { group: '布局', priority: 'common', type: 'number', title: '高度（px）', default: 24 },
    // 高级
    backgroundColor: { group: '高级', priority: 'advanced', type: 'color', title: '背景色' },
  } },
  dataSources: [],
  print: { hidden: true, breakAvoid: false },
}

const space = {
  type: 'space',
  aliases: [],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: '间距',
  desc: '横/纵间距容器',
  layout: { defaultSpan: 12 },
  container: true,
  maxDepth: 4,
  accept: [],
  propsSchema: { properties: {
    // 常用 · 布局
    direction: { group: '布局', priority: 'common', type: 'select', title: '方向', options: [
      { label: '横向', value: 'horizontal' },
      { label: '纵向', value: 'vertical' },
    ], default: 'horizontal' },
    size: { group: '布局', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
    // 高级
    align: { group: '高级', priority: 'advanced', type: 'select', title: '对齐', options: [
      { label: '起始', value: 'start' },
      { label: '居中', value: 'center' },
      { label: '末尾', value: 'end' },
      { label: '基线', value: 'baseline' },
      { label: '拉伸', value: 'stretch' },
    ] },
    justify: { group: '高级', priority: 'advanced', type: 'select', title: '主轴对齐', options: [
      { label: '起始', value: 'start' },
      { label: '居中', value: 'center' },
      { label: '末尾', value: 'end' },
      { label: '两端', value: 'space-between' },
      { label: '均分', value: 'space-around' },
    ] },
    wrap: { group: '高级', priority: 'advanced', type: 'boolean', title: '换行', default: false },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: false },
}

const groupTitle = {
  type: 'groupTitle',
  aliases: ['title', 'fcTitle', 'sectionTitle', 'groupHeader', 'GroupHeader', 'titleBlock', 'section', 'section-divider'],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: '分组标题',
  desc: '分区标题与说明',
  layout: { defaultSpan: 24 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 布局
    title: { group: '布局', priority: 'common', type: 'string', title: '标题' },
    description: { group: '布局', priority: 'common', type: 'string', title: '描述' },
    // 高级
    badge: { group: '高级', priority: 'advanced', type: 'string', title: '角标' },
    size: { group: '高级', priority: 'advanced', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
    type: { group: '高级', priority: 'advanced', type: 'select', title: '标题级别', options: [
      { label: 'h1', value: '1' },
      { label: 'h2', value: '2' },
      { label: 'h3', value: '3' },
      { label: 'h4', value: '4' },
      { label: 'h5', value: '5' },
      { label: 'h6', value: '6' },
    ] },
    depth: { group: '高级', priority: 'advanced', type: 'select', title: '颜色深度', options: [
      { label: '1', value: 1 },
      { label: '2', value: 2 },
      { label: '3', value: 3 },
    ] },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: true },
}

const formSectionTitle = {
  type: 'formSectionTitle',
  aliases: ['AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle', 'elDivider'],
  scope: 'F+L',
  category: 'layout',
  group: '布局',
  label: '表单分隔线',
  desc: '表单分组标题与分隔线（运行时独立于 divider）',
  layout: { defaultSpan: 24 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 布局
    title: { group: '布局', priority: 'common', type: 'string', title: '标题' },
    description: { group: '布局', priority: 'common', type: 'string', title: '描述' },
    // 高级
    depth: { group: '高级', priority: 'advanced', type: 'select', title: '颜色深度', options: [
      { label: '1', value: 1 },
      { label: '2', value: 2 },
      { label: '3', value: 3 },
    ] },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: true },
}

// ─── 包装节点（容器内部层，接受任意子组件）──────────────

const tabPane = {
  type: 'tabPane',
  aliases: ['elTabPane'],
  scope: 'F+L',
  category: 'layout',
  group: '包装节点',
  label: '标签面板',
  desc: 'Tabs 容器内的标签页包装层',
  layout: { defaultSpan: 24 },
  container: true,
  maxDepth: 4,
  accept: [],
  propsSchema: { properties: {
    // 常用 · 布局
    label: { group: '布局', priority: 'common', type: 'string', title: '标签名称' },
    name: { group: '布局', priority: 'common', type: 'string', title: '唯一标识' },
    disabled: { group: '布局', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    closable: { group: '高级', priority: 'advanced', type: 'boolean', title: '可关闭', default: false },
    displayDirective: { group: '高级', priority: 'advanced', type: 'select', title: '显示指令', options: [
      { label: 'if', value: 'if' },
      { label: 'show', value: 'show' },
    ], default: 'if' },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: true },
}

const collapseItem = {
  type: 'collapseItem',
  aliases: ['elCollapseItem'],
  scope: 'F+L',
  category: 'layout',
  group: '包装节点',
  label: '折叠面板项',
  desc: 'Collapse 容器内的折叠项包装层',
  layout: { defaultSpan: 24 },
  container: true,
  maxDepth: 4,
  accept: [],
  propsSchema: { properties: {
    // 常用 · 布局
    title: { group: '布局', priority: 'common', type: 'string', title: '标题' },
    name: { group: '布局', priority: 'common', type: 'string', title: '唯一标识' },
    disabled: { group: '布局', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    displayDirective: { group: '高级', priority: 'advanced', type: 'select', title: '显示指令', options: [
      { label: 'if', value: 'if' },
      { label: 'show', value: 'show' },
    ], default: 'if' },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: true },
}

const col = {
  type: 'col',
  aliases: ['fcCol'],
  scope: 'F+L',
  category: 'layout',
  group: '包装节点',
  label: '栅格列',
  desc: 'Row 容器内的列包装层',
  layout: { defaultSpan: 6 },
  container: true,
  maxDepth: 4,
  accept: [],
  propsSchema: { properties: {
    // 常用 · 布局
    span: { group: '布局', priority: 'common', type: 'number', title: '占据列数', default: 6, min: 1, max: 24 },
    offset: { group: '布局', priority: 'common', type: 'number', title: '偏移列数', default: 0, min: 0, max: 24 },
    // 高级
    push: { group: '高级', priority: 'advanced', type: 'number', title: '右推列数', default: 0, min: 0, max: 24 },
    pull: { group: '高级', priority: 'advanced', type: 'number', title: '左拉列数', default: 0, min: 0, max: 24 },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: false },
}

const tableCell = {
  type: 'tableCell',
  aliases: ['tableGrid', 'fcTableGrid'],
  scope: 'F+L',
  category: 'layout',
  group: '包装节点',
  label: '表格单元格',
  desc: 'Table 容器内的单元格包装层',
  layout: { defaultSpan: 1 },
  container: true,
  maxDepth: 4,
  accept: [],
  propsSchema: { properties: {
    // 常用 · 布局
    span: { group: '布局', priority: 'common', type: 'number', title: '占据列数', default: 1, min: 1 },
    rowspan: { group: '布局', priority: 'common', type: 'number', title: '占据行数', default: 1, min: 1 },
    // 高级
    bordered: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示边框', default: true },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: false },
}

export const layoutComponentSpecs = [
  grid,
  table,
  card,
  tabs,
  collapse,
  box,
  divider,
  spacer,
  space,
  groupTitle,
  formSectionTitle,
  tabPane,
  collapseItem,
  col,
  tableCell,
]
