/**
 * @fileoverview 业务区块 spec（12 个 · scope F+L 为主）
 * @description AiCrudPage / AiTable / AiForm / subTable / search-form / toolbar / data-table /
 *   tree-panel / detail-info / step-form / signature-pad / sub-table-tabs
 */

const AiCrudPage = {
  type: 'AiCrudPage',
  aliases: ['crud', 'crudBlock', 'aiCrudPage'],
  scope: 'F+L',
  category: 'business',
  group: '数据',
  label: '数据列表',
  desc: '一体化 CRUD：筛选+表格+新增/编辑/删除弹窗',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 14 },
  container: false,
  meta: { unique: true },
  // 属性 key 必须与 AiCrudPage 运行时 props 一致（createDefaultAiCrudPageProps /
  // pickRuntimeTableProps 同源）：属性面板写入 block.props 后经 zones 同步链生效。
  // 此前的伪 key（modalMode/labelPlacement/formColumns/showAdd 等）写入后运行时不
  // 读取，改了不生效；引用不存在编辑器的 customEditor 属性已移除
  // （搜索布局/表格列/公共参数/Hook 规则由手写区专门编辑器管理，
  //   自定义按钮/行操作走“配置全部操作”，树配置走 tree-panel 区块）。
  propsSchema: { properties: {
    // ① 查询与列表
    showPagination: { group: '查询与列表', priority: 'common', type: 'boolean', title: '分页', default: true },
    striped: { group: '查询与列表', priority: 'common', type: 'boolean', title: '斑马纹', default: false },
    hideSelection: { group: '查询与列表', priority: 'common', type: 'boolean', title: '隐藏多选列', default: false },
    maxHeight: { group: '查询与列表', priority: 'common', type: 'number', title: '最大高度' },
    // ② 表单与弹窗
    formOpenMode: { group: '表单与弹窗', priority: 'common', type: 'select', title: '弹窗方式', default: 'modal', options: [
      { label: '弹窗', value: 'modal' },
      { label: '抽屉', value: 'drawer' },
      { label: '页签工作台', value: 'tabWorkspace' },
      { label: '平铺内联', value: 'flat' },
    ] },
    editLabelPlacement: { group: '表单与弹窗', priority: 'common', type: 'select', title: '标签位置', default: 'left', options: [
      { label: '左', value: 'left' },
      { label: '上', value: 'top' },
    ] },
    editLabelWidth: { group: '表单与弹窗', priority: 'common', type: 'select', title: '标签宽度', default: 'auto', options: [
      { label: '自适应', value: 'auto' },
      { label: '80px', value: 80 },
      { label: '100px', value: 100 },
      { label: '120px', value: 120 },
      { label: '140px', value: 140 },
      { label: '160px', value: 160 },
    ] },
    editGridCols: { group: '表单与弹窗', priority: 'common', type: 'number', title: '表单列数', default: 1, min: 1, max: 4 },
    editSize: { group: '表单与弹窗', priority: 'common', type: 'select', title: '表单尺寸', default: 'medium', options: [
      { label: '紧凑', value: 'small' },
      { label: '默认', value: 'medium' },
      { label: '宽松', value: 'large' },
    ] },
    modalWidth: { group: '表单与弹窗', priority: 'common', type: 'string', title: '弹窗宽度', default: '800px', placeholder: '如 800px / 92vw' },
    drawerPlacement: { group: '表单与弹窗', priority: 'common', type: 'select', title: '抽屉方向', default: 'right', options: [
      { label: '右侧', value: 'right' },
      { label: '左侧', value: 'left' },
      { label: '顶部', value: 'top' },
      { label: '底部', value: 'bottom' },
    ] },
    // ③ 工具栏与导入导出
    hideAdd: { group: '工具栏', priority: 'common', type: 'boolean', title: '隐藏新增按钮', default: false },
    hideBatchDelete: { group: '工具栏', priority: 'common', type: 'boolean', title: '隐藏批量删除', default: false },
    showImport: { group: '工具栏', priority: 'common', type: 'boolean', title: '导入按钮', default: true },
    showExport: { group: '工具栏', priority: 'common', type: 'boolean', title: '导出按钮', default: true },
    showExportTasks: { group: '工具栏', priority: 'common', type: 'boolean', title: '导出任务', default: true },
    enableCustomQuery: { group: '工具栏', priority: 'common', type: 'boolean', title: '自定义查询', default: true },
  } },
  dataSources: ['managed'],
  print: { hidden: false, breakAvoid: true },
}

const AiTable = {
  type: 'AiTable',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '数据',
  label: '数据表格',
  desc: '交互表格（独立于 CRUD）',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 9 },
  container: false,
  meta: { unique: true },
  propsSchema: { properties: {
    // 常用 · 数据
    pagination: { group: '数据', priority: 'common', type: 'boolean', title: '分页', default: true },
    density: { group: '数据', priority: 'common', type: 'select', title: '密度', options: [
      { label: '紧凑', value: 'small' },
      { label: '默认', value: 'medium' },
      { label: '宽松', value: 'large' },
    ] },
    showColumnSetting: { group: '数据', priority: 'common', type: 'boolean', title: '列设置', default: true },
    // 高级
    showSearch: { group: '高级', priority: 'advanced', type: 'boolean', title: '搜索切换', default: false },
    showFullscreen: { group: '高级', priority: 'advanced', type: 'boolean', title: '全屏', default: false },
    scrollX: { group: '高级', priority: 'advanced', type: 'number', title: '横向滚动宽度' },
    striped: { group: '高级', priority: 'advanced', type: 'boolean', title: '斑马纹', default: false },
    bordered: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示边框', default: true },
    maxHeight: { group: '高级', priority: 'advanced', type: 'number', title: '最大高度' },
  } },
  dataSources: ['managed'],
  print: { hidden: false, breakAvoid: true },
}

const AiForm = {
  type: 'AiForm',
  aliases: [],
  scope: 'F+L',
  category: 'business',
  group: '数据',
  label: '数据表单',
  desc: '按字段生成录入表单，可独立提交',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 6 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 表单
    formAsset: { group: '表单', priority: 'common', type: 'string', title: '表单资产引用' },
    labelPlacement: { group: '表单', priority: 'common', type: 'select', title: '标签位置', options: [
      { label: '左', value: 'left' },
      { label: '上', value: 'top' },
    ] },
    formColumns: { group: '表单', priority: 'common', type: 'number', title: '表单列数', default: 1, min: 1, max: 4 },
    // 高级
    submitAction: { group: '高级', priority: 'advanced', type: 'select', title: '提交动作', options: [
      { label: '保存', value: 'save' },
      { label: '提交并关闭', value: 'saveAndClose' },
    ] },
    showFeedback: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示反馈', default: true },
  } },
  dataSources: ['managed'],
  print: { hidden: false, breakAvoid: true },
}

const subTable = {
  type: 'subTable',
  aliases: [],
  scope: 'F',
  category: 'business',
  group: '数据',
  label: '关联子表',
  desc: '对象关系子表明细（随主表提交）',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 8 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 关联
    relationKey: { group: '关联', priority: 'common', type: 'string', title: '关联键' },
    displayMode: { group: '关联', priority: 'common', type: 'select', title: '展示方式', options: [
      { label: '内联表格', value: 'table' },
      { label: '卡片', value: 'card' },
    ] },
    columns: { group: '关联', priority: 'common', type: 'json', title: '列配置' },
    // 高级
    rowActions: { group: '高级', priority: 'advanced', type: 'json', title: '行操作' },
    bordered: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示边框', default: true },
    maxHeight: { group: '高级', priority: 'advanced', type: 'number', title: '最大高度' },
  } },
  dataSources: ['relation'],
  print: { hidden: false, breakAvoid: true },
}

const searchForm = {
  type: 'search-form',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '数据',
  label: '查询表单',
  desc: '查询字段集 + 查询/重置/收起',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 4 },
  container: false,
  meta: { unique: true, multiField: true, requireFields: true },
  propsSchema: { properties: {
    // 常用 · 查询
    fields: { group: '查询', priority: 'common', type: 'customEditor', editor: 'SearchFieldsEditor', title: '查询字段集' },
    defaultCollapseCount: { group: '查询', priority: 'common', type: 'number', title: '默认折叠数', default: 3 },
    // 高级
    searchText: { group: '高级', priority: 'advanced', type: 'string', title: '查询文案', default: '查询' },
    resetText: { group: '高级', priority: 'advanced', type: 'string', title: '重置文案', default: '重置' },
    size: { group: '高级', priority: 'advanced', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
  } },
  dataSources: [],
  print: { hidden: true, breakAvoid: false },
}

const toolbar = {
  type: 'toolbar',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '操作',
  label: '操作工具栏',
  desc: '新增/导入/导出/自定义操作按钮排',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 2 },
  container: false,
  meta: { unique: true },
  propsSchema: { properties: {
    // 常用 · 操作
    showAdd: { group: '操作', priority: 'common', type: 'boolean', title: '新增', default: true },
    showImport: { group: '操作', priority: 'common', type: 'boolean', title: '导入', default: false },
    showExport: { group: '操作', priority: 'common', type: 'boolean', title: '导出', default: false },
    showCustomQuery: { group: '操作', priority: 'common', type: 'boolean', title: '自定义查询', default: false },
    // 高级
    customActions: { group: '高级', priority: 'advanced', type: 'json', title: '自定义按钮' },
    size: { group: '高级', priority: 'advanced', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
  } },
  dataSources: ['managed'],
  print: { hidden: true, breakAvoid: false },
}

const dataTable = {
  type: 'data-table',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '数据',
  label: '基础列表',
  desc: '配置式静态/接口列表',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 10 },
  container: false,
  meta: { unique: true, multiField: true, requireFields: true },
  propsSchema: { properties: {
    // 常用 · 数据
    columns: { group: '数据', priority: 'common', type: 'json', title: '展示列配置' },
    pagination: { group: '数据', priority: 'common', type: 'boolean', title: '分页', default: true },
    // 高级
    rowActions: { group: '高级', priority: 'advanced', type: 'json', title: '行操作' },
    bordered: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示边框', default: true },
    striped: { group: '高级', priority: 'advanced', type: 'boolean', title: '斑马纹', default: false },
    maxHeight: { group: '高级', priority: 'advanced', type: 'number', title: '最大高度' },
    density: { group: '高级', priority: 'advanced', type: 'select', title: '密度', options: [
      { label: '紧凑', value: 'small' },
      { label: '默认', value: 'medium' },
      { label: '宽松', value: 'large' },
    ] },
  } },
  dataSources: ['remote', 'managed'],
  print: { hidden: false, breakAvoid: true },
}

const treePanel = {
  type: 'tree-panel',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '数据',
  label: '筛选树',
  desc: '左树右表模板中筛选右侧列表',
  layout: { defaultSpan: 6, defaultW: 3, defaultH: 14 },
  meta: { unique: true, onlyFor: ['tree-crud'] },
  propsSchema: { properties: {
    // 常用 · 数据
    treeSourceModel: { group: '数据', priority: 'common', type: 'string', title: '树源模型' },
    treeField: { group: '数据', priority: 'common', type: 'string', title: '树字段' },
    // 高级
    filterField: { group: '高级', priority: 'advanced', type: 'string', title: '过滤联动字段' },
    defaultExpandAll: { group: '高级', priority: 'advanced', type: 'boolean', title: '默认展开', default: false },
    cascade: { group: '高级', priority: 'advanced', type: 'boolean', title: '级联选择', default: false },
  } },
  dataSources: ['managed'],
  print: { hidden: false, breakAvoid: false },
  container: false,
}

const detailInfo = {
  type: 'detail-info',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '数据',
  label: '详情信息',
  desc: '只读详情字段展示',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 8 },
  container: false,
  meta: { multiField: true, requireFields: true },
  propsSchema: { properties: {
    // 常用 · 数据
    fields: { group: '数据', priority: 'common', type: 'json', title: '字段集' },
    columns: { group: '数据', priority: 'common', type: 'number', title: '布局列数', default: 2, min: 1, max: 4 },
    // 高级
    groups: { group: '高级', priority: 'advanced', type: 'json', title: '分组' },
    labelPlacement: { group: '高级', priority: 'advanced', type: 'select', title: '标签位置', options: [
      { label: '左', value: 'left' },
      { label: '上', value: 'top' },
    ] },
    bordered: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示边框', default: true },
    size: { group: '高级', priority: 'advanced', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
  } },
  dataSources: ['context', 'remote'],
  print: { hidden: false, breakAvoid: true },
}

const stepForm = {
  type: 'step-form',
  aliases: [],
  scope: 'F+L',
  category: 'business',
  group: '数据',
  label: '分步表单',
  desc: '按步骤组织的表单',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 8 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 步骤
    steps: { group: '步骤', priority: 'common', type: 'json', title: '步骤管理（每步标题/字段）' },
    // 高级
    size: { group: '高级', priority: 'advanced', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    vertical: { group: '高级', priority: 'advanced', type: 'boolean', title: '垂直布局', default: false },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: true },
}

const signaturePad = {
  type: 'signature-pad',
  aliases: [],
  scope: 'F+L',
  category: 'business',
  group: '数据',
  label: '手写签名',
  desc: '签名采集画布',
  layout: { defaultSpan: 12, defaultW: 6, defaultH: 4 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 外观
    width: { group: '外观', priority: 'common', type: 'number', title: '画布宽度', default: 400 },
    height: { group: '外观', priority: 'common', type: 'number', title: '画布高度', default: 200 },
    penColor: { group: '外观', priority: 'common', type: 'color', title: '笔色', default: '#000000' },
    penWidth: { group: '外观', priority: 'common', type: 'number', title: '笔宽', default: 2 },
    // 高级
    backgroundColor: { group: '高级', priority: 'advanced', type: 'color', title: '背景色', default: '#ffffff' },
  } },
  dataSources: [],
  print: { hidden: false, breakAvoid: false },
}

const subTableTabs = {
  type: 'sub-table-tabs',
  aliases: [],
  scope: 'L',
  category: 'business',
  group: '数据',
  label: '子表 Tab',
  desc: '关联模型分页签容器',
  layout: { defaultSpan: 24, defaultW: 12, defaultH: 8 },
  container: false,
  meta: { onlyFor: ['master-detail-crud'] },
  propsSchema: { properties: {
    // 常用 · 关联
    relatedModels: { group: '关联', priority: 'common', type: 'json', title: '关联模型' },
    tabField: { group: '关联', priority: 'common', type: 'string', title: '页签字段' },
    // 高级
    tabType: { group: '高级', priority: 'advanced', type: 'select', title: '页签类型', options: [
      { label: '线条', value: 'line' },
      { label: '卡片', value: 'card' },
      { label: '分段', value: 'segment' },
    ], default: 'line' },
    size: { group: '高级', priority: 'advanced', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
  } },
  dataSources: ['relation'],
  print: { hidden: false, breakAvoid: true },
}

export const businessComponentSpecs = [
  AiCrudPage,
  AiTable,
  AiForm,
  subTable,
  searchForm,
  toolbar,
  dataTable,
  treePanel,
  detailInfo,
  stepForm,
  signaturePad,
  subTableTabs,
]
