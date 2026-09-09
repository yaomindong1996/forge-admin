/**
 * @fileoverview 字段组件 spec（33 个 · scope: F）
 * @description 涵盖输入(8)、选择(17)、业务(8)三个分组。
 *   每个组件含完整 propsSchema、fieldDefaults 和运行时别名（含 componentTypeAlias + field-* 包装）。
 */

const fieldCommonPrint = { hidden: false, breakAvoid: false }
const fieldCommonLayout = { defaultSpan: 12 }

// ─── 输入类（8 个）─────────────────────────────────────────

const input = {
  type: 'input',
  aliases: ['text'],
  scope: 'F',
  category: 'field',
  group: '输入',
  label: '输入框',
  desc: '单行文本录入',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 输入行为
    placeholder: { group: '输入行为', priority: 'common', type: 'string', title: '占位提示' },
    clearable: { group: '输入行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    maxLength: { group: '输入行为', priority: 'common', type: 'number', title: '最大长度', min: 0 },
    showCount: { group: '输入行为', priority: 'common', type: 'boolean', title: '显示字数', default: false },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    readonly: { group: '外观', priority: 'common', type: 'boolean', title: '只读', default: false },
    // 高级
    round: { group: '高级', priority: 'advanced', type: 'boolean', title: '圆角', default: false },
    pair: { group: '高级', priority: 'advanced', type: 'boolean', title: '成对输入', default: false, desc: '启用后输入框分为左右两部分' },
    passivelyTriggered: { group: '高级', priority: 'advanced', type: 'boolean', title: '被动触发', default: false, desc: '只在 blur 时触发 update' },
    loading: { group: '高级', priority: 'advanced', type: 'boolean', title: '加载中', default: false },
    autofocus: { group: '高级', priority: 'advanced', type: 'boolean', title: '自动聚焦', default: false },
    // 装饰
    prefix: { group: '装饰', priority: 'advanced', type: 'string', title: '前缀文本' },
    suffix: { group: '装饰', priority: 'advanced', type: 'string', title: '后缀文本' },
    inputProps: { group: '装饰', priority: 'advanced', type: 'json', title: '原生 input 属性', desc: '透传给原生 input 元素的属性' },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'TEXT', businessFieldType: 'TEXT', dataType: 'varchar', componentType: 'input', length: 128, precision: 2, queryType: 'like' },
}

const barcodeScanner = {
  type: 'barcodeScanner',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '输入',
  label: '扫码输入',
  desc: '移动端扫码录入文本',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 输入行为
    scanFormat: { group: '输入行为', priority: 'common', type: 'string', title: '扫码格式' },
    continuousScan: { group: '输入行为', priority: 'common', type: 'boolean', title: '连续扫码', default: false },
    placeholder: { group: '输入行为', priority: 'common', type: 'string', title: '占位提示' },
    // 常用 · 外观
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    clearable: { group: '外观', priority: 'common', type: 'boolean', title: '可清空', default: true },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'TEXT', businessFieldType: 'TEXT', dataType: 'varchar', componentType: 'barcodeScanner', length: 2048, precision: 2, queryType: 'eq' },
}

const textarea = {
  type: 'textarea',
  aliases: ['field-textarea'],
  scope: 'F',
  category: 'field',
  group: '输入',
  label: '多行文本',
  desc: '长文本录入',
  layout: { defaultSpan: 24 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 输入行为
    placeholder: { group: '输入行为', priority: 'common', type: 'string', title: '占位提示' },
    rows: { group: '输入行为', priority: 'common', type: 'number', title: '行数', default: 3, min: 1 },
    maxLength: { group: '输入行为', priority: 'common', type: 'number', title: '最大长度', min: 0 },
    autosize: { group: '输入行为', priority: 'common', type: 'boolean', title: '自适应行高', default: false },
    showCount: { group: '输入行为', priority: 'common', type: 'boolean', title: '显示字数', default: false },
    // 常用 · 外观
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    readonly: { group: '外观', priority: 'common', type: 'boolean', title: '只读', default: false },
    // 高级
    round: { group: '高级', priority: 'advanced', type: 'boolean', title: '圆角', default: false },
    autofocus: { group: '高级', priority: 'advanced', type: 'boolean', title: '自动聚焦', default: false },
    passivelyTriggered: { group: '高级', priority: 'advanced', type: 'boolean', title: '被动触发', default: false },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'MULTILINE', businessFieldType: 'MULTILINE', dataType: 'text', componentType: 'textarea', length: null, precision: 2, queryType: 'like' },
}

const number = {
  type: 'number',
  aliases: ['inputNumber', 'input-number', 'inputnumber', 'integer', 'field-number'],
  scope: 'F',
  category: 'field',
  group: '输入',
  label: '数字',
  desc: '数值录入',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 输入行为
    placeholder: { group: '输入行为', priority: 'common', type: 'string', title: '占位提示' },
    min: { group: '输入行为', priority: 'common', type: 'number', title: '最小值' },
    max: { group: '输入行为', priority: 'common', type: 'number', title: '最大值' },
    step: { group: '输入行为', priority: 'common', type: 'number', title: '步进', default: 1 },
    precision: { group: '输入行为', priority: 'common', type: 'number', title: '精度（小数位）' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    showButton: { group: '外观', priority: 'common', type: 'boolean', title: '显示按钮', default: true },
    buttonPlacement: { group: '外观', priority: 'common', type: 'select', title: '按钮位置', options: [
      { label: '两侧', value: 'both' },
      { label: '右侧', value: 'right' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    readonly: { group: '外观', priority: 'common', type: 'boolean', title: '只读', default: false },
    // 高级
    loading: { group: '高级', priority: 'advanced', type: 'boolean', title: '加载中', default: false },
    clearable: { group: '高级', priority: 'advanced', type: 'boolean', title: '可清空', default: false },
    keyboard: { group: '高级', priority: 'advanced', type: 'boolean', title: '键盘操作', default: true, desc: '允许通过键盘上下键调整数值' },
    autofocus: { group: '高级', priority: 'advanced', type: 'boolean', title: '自动聚焦', default: false },
    // 装饰
    prefix: { group: '装饰', priority: 'advanced', type: 'string', title: '前缀文本' },
    suffix: { group: '装饰', priority: 'advanced', type: 'string', title: '后缀文本' },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'NUMBER', businessFieldType: 'NUMBER', dataType: 'int', componentType: 'number', length: 11, precision: 0, queryType: 'eq' },
}

const money = {
  type: 'money',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '输入',
  label: '金额',
  desc: '金额录入（自动格式化）',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 输入行为
    precision: { group: '输入行为', priority: 'common', type: 'number', title: '精度', default: 2 },
    currencySymbol: { group: '输入行为', priority: 'common', type: 'string', title: '货币符号', default: '¥' },
    showChinese: { group: '输入行为', priority: 'common', type: 'boolean', title: '大写金额显示', default: false },
    placeholder: { group: '输入行为', priority: 'common', type: 'string', title: '占位提示' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    loading: { group: '高级', priority: 'advanced', type: 'boolean', title: '加载中', default: false },
    clearable: { group: '高级', priority: 'advanced', type: 'boolean', title: '可清空', default: false },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'MONEY', businessFieldType: 'MONEY', dataType: 'decimal', componentType: 'number', length: 18, precision: 2, queryType: 'eq' },
}

const slider = {
  type: 'slider',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '输入',
  label: '滑块',
  desc: '拖动选数值',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 输入行为
    min: { group: '输入行为', priority: 'common', type: 'number', title: '最小值', default: 0 },
    max: { group: '输入行为', priority: 'common', type: 'number', title: '最大值', default: 100 },
    step: { group: '输入行为', priority: 'common', type: 'number', title: '步进', default: 1 },
    range: { group: '输入行为', priority: 'common', type: 'boolean', title: '范围模式', default: false },
    // 常用 · 外观
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    vertical: { group: '外观', priority: 'common', type: 'boolean', title: '垂直方向', default: false },
    reverse: { group: '外观', priority: 'common', type: 'boolean', title: '反向', default: false },
    showTooltip: { group: '外观', priority: 'common', type: 'boolean', title: '显示提示', default: true },
    // 高级
    marks: { group: '高级', priority: 'advanced', type: 'json', title: '刻度标记', desc: '{ [value]: label }' },
    tooltip: { group: '高级', priority: 'advanced', type: 'boolean', title: '始终显示 tooltip', default: false },
    keyboard: { group: '高级', priority: 'advanced', type: 'boolean', title: '键盘操作', default: true },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'NUMBER', businessFieldType: 'NUMBER', dataType: 'int', componentType: 'slider', length: 11, precision: 0, queryType: 'eq' },
}

const rate = {
  type: 'rate',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '输入',
  label: '评分',
  desc: '星级评分',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 输入行为
    count: { group: '输入行为', priority: 'common', type: 'number', title: '星数', default: 5, min: 1 },
    allowHalf: { group: '输入行为', priority: 'common', type: 'boolean', title: '允许半星', default: false },
    clearable: { group: '输入行为', priority: 'common', type: 'boolean', title: '可清空', default: false },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    readonly: { group: '外观', priority: 'common', type: 'boolean', title: '只读', default: false },
    // 高级
    color: { group: '高级', priority: 'advanced', type: 'color', title: '选中颜色' },
    texts: { group: '高级', priority: 'advanced', type: 'json', title: '文案映射', desc: '{ [value]: text }' },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'NUMBER', businessFieldType: 'NUMBER', dataType: 'decimal', componentType: 'rate', length: 4, precision: 1, queryType: 'eq' },
}

const color = {
  type: 'color',
  aliases: ['colorPicker'],
  scope: 'F',
  category: 'field',
  group: '输入',
  label: '颜色选择',
  desc: '取色器',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 输入行为
    modes: { group: '输入行为', priority: 'common', type: 'select', title: '色板模式', options: [
      { label: 'RGB', value: 'rgb' },
      { label: 'HEX', value: 'hex' },
      { label: 'HSL', value: 'hsl' },
    ] },
    showAlpha: { group: '输入行为', priority: 'common', type: 'boolean', title: '支持透明度', default: false },
    clearable: { group: '输入行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    showPreview: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示预览', default: true },
    swatches: { group: '高级', priority: 'advanced', type: 'json', title: '默认色板', desc: '预定义颜色数组' },
    renderable: { group: '高级', priority: 'advanced', type: 'boolean', title: '可渲染', default: true },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'TEXT', businessFieldType: 'TEXT', dataType: 'varchar', componentType: 'color', length: 32, precision: 2, queryType: 'eq' },
}

// ─── 选择类（17 个）─────────────────────────────────────────

const select = {
  type: 'select',
  aliases: ['field-select'],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '下拉选择',
  desc: '选项手动配置或远程接口获取',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    options: { group: '选择行为', priority: 'common', type: 'options', title: '选项' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    filterable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可搜索', default: false },
    multiple: { group: '选择行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    maxTagCount: { group: '高级', priority: 'advanced', type: 'number', title: '最大标签数', min: 0, desc: '多选时最多显示的标签数量' },
    tag: { group: '高级', priority: 'advanced', type: 'boolean', title: '可创建选项', default: false, desc: '允许用户创建新选项' },
    virtualScroll: { group: '高级', priority: 'advanced', type: 'boolean', title: '虚拟滚动', default: true, desc: '大数据量时启用虚拟滚动' },
    loading: { group: '高级', priority: 'advanced', type: 'boolean', title: '加载中', default: false },
    clearFilterAfterSelect: { group: '高级', priority: 'advanced', type: 'boolean', title: '选后清空搜索', default: false },
  } },
  dataSources: ['static', 'dict', 'remote', 'relation'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'DICT', businessFieldType: 'DICT', dataType: 'varchar', componentType: 'select', length: 64, precision: 2, queryType: 'eq' },
}

const dictSelect = {
  type: 'dictSelect',
  aliases: ['field-dict-select'],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '字典下拉',
  desc: '选项来自数据字典，字典统一维护',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    dictType: { group: '选择行为', priority: 'common', type: 'string', title: '字典类型' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    filterable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可搜索', default: false },
    multiple: { group: '选择行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    maxTagCount: { group: '高级', priority: 'advanced', type: 'number', title: '最大标签数', min: 0 },
    tag: { group: '高级', priority: 'advanced', type: 'boolean', title: '可创建选项', default: false },
    virtualScroll: { group: '高级', priority: 'advanced', type: 'boolean', title: '虚拟滚动', default: true },
    loading: { group: '高级', priority: 'advanced', type: 'boolean', title: '加载中', default: false },
  } },
  dataSources: ['dict'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'DICT', businessFieldType: 'DICT', dataType: 'varchar', componentType: 'dictSelect', length: 64, precision: 2, queryType: 'eq' },
}

const radio = {
  type: 'radio',
  aliases: ['radioGroup', 'field-radio'],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '单选',
  desc: '平铺选项点选一项',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    options: { group: '选择行为', priority: 'common', type: 'options', title: '选项' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    direction: { group: '外观', priority: 'common', type: 'select', title: '排列方向', options: [
      { label: '水平', value: 'horizontal' },
      { label: '垂直', value: 'vertical' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    name: { group: '高级', priority: 'advanced', type: 'string', title: 'name 属性', desc: '原生 input name，通常保持默认即可' },
  } },
  dataSources: ['static', 'dict', 'remote'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'RADIO', businessFieldType: 'RADIO', dataType: 'varchar', componentType: 'radio', length: 64, precision: 2, queryType: 'eq' },
}

const radioButton = {
  type: 'radioButton',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '按钮单选',
  desc: '按钮形态，适合少量选项',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    options: { group: '选择行为', priority: 'common', type: 'options', title: '选项' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    direction: { group: '外观', priority: 'common', type: 'select', title: '排列方向', options: [
      { label: '水平', value: 'horizontal' },
      { label: '垂直', value: 'vertical' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    name: { group: '高级', priority: 'advanced', type: 'string', title: 'name 属性', desc: '原生 input name，通常保持默认即可' },
  } },
  dataSources: ['static', 'dict', 'remote'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'RADIO', businessFieldType: 'RADIO', dataType: 'varchar', componentType: 'radioButton', length: 64, precision: 2, queryType: 'eq' },
}

const checkbox = {
  type: 'checkbox',
  aliases: ['checkboxGroup', 'field-checkbox'],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '多选',
  desc: '平铺选项勾选多项',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    options: { group: '选择行为', priority: 'common', type: 'options', title: '选项' },
    min: { group: '选择行为', priority: 'common', type: 'number', title: '最少选择数', min: 0 },
    max: { group: '选择行为', priority: 'common', type: 'number', title: '最多选择数', min: 0 },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    direction: { group: '外观', priority: 'common', type: 'select', title: '排列方向', options: [
      { label: '水平', value: 'horizontal' },
      { label: '垂直', value: 'vertical' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    maxTagCount: { group: '高级', priority: 'advanced', type: 'number', title: '最大标签数', min: 0, desc: '超过后折叠显示' },
  } },
  dataSources: ['static', 'dict', 'remote'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'CHECKBOX', businessFieldType: 'CHECKBOX', dataType: 'varchar', componentType: 'checkbox', length: 255, precision: 2, queryType: 'in' },
}

const transfer = {
  type: 'transfer',
  aliases: [],
  scope: 'F+L',
  category: 'field',
  group: '选择',
  label: '穿梭框',
  desc: '双栏穿梭多选',
  layout: { defaultSpan: 24 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    sourceTitle: { group: '选择行为', priority: 'common', type: 'string', title: '源栏标题', default: '可选项' },
    targetTitle: { group: '选择行为', priority: 'common', type: 'string', title: '目标栏标题', default: '已选项' },
    filterable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可过滤', default: true },
    showSelected: { group: '选择行为', priority: 'common', type: 'boolean', title: '显示已选', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    virtualScroll: { group: '高级', priority: 'advanced', type: 'boolean', title: '虚拟滚动', default: false },
    selectAllText: { group: '高级', priority: 'advanced', type: 'string', title: '全选文案' },
    clearText: { group: '高级', priority: 'advanced', type: 'string', title: '清空文案' },
    clearable: { group: '高级', priority: 'advanced', type: 'boolean', title: '可清空', default: true },
  } },
  dataSources: ['static', 'remote'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'MULTI_SELECT', businessFieldType: 'MULTI_SELECT', dataType: 'text', componentType: 'transfer', length: null, precision: null, queryType: 'in' },
}

const cascader = {
  type: 'cascader',
  aliases: ['field-cascader'],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '级联选择',
  desc: '树形级联单选/多选',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    options: { group: '选择行为', priority: 'common', type: 'options', title: '选项树' },
    checkStrategy: { group: '选择行为', priority: 'common', type: 'select', title: '选择策略', options: [
      { label: '子节点', value: 'child' },
      { label: '父节点', value: 'parent' },
      { label: '任意', value: 'all' },
    ] },
    filterable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可搜索', default: false },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    multiple: { group: '选择行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    separator: { group: '高级', priority: 'advanced', type: 'string', title: '分隔符', default: ' / ' },
    cascade: { group: '高级', priority: 'advanced', type: 'boolean', title: '级联勾选', default: true },
    leafOnly: { group: '高级', priority: 'advanced', type: 'boolean', title: '仅叶子节点', default: false },
    maxTagCount: { group: '高级', priority: 'advanced', type: 'number', title: '最大标签数', min: 0 },
    virtualScroll: { group: '高级', priority: 'advanced', type: 'boolean', title: '虚拟滚动', default: true },
    loading: { group: '高级', priority: 'advanced', type: 'boolean', title: '加载中', default: false },
    expandTrigger: { group: '高级', priority: 'advanced', type: 'select', title: '展开方式', options: [
      { label: '点击', value: 'click' },
      { label: '悬停', value: 'hover' },
    ] },
  } },
  dataSources: ['static', 'dict', 'remote'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'DICT', businessFieldType: 'DICT', dataType: 'varchar', componentType: 'cascader', length: 128, precision: 2, queryType: 'eq' },
}

const treeSelect = {
  type: 'treeSelect',
  aliases: ['field-tree-select'],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '树形选择',
  desc: '树形单选/多选',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    options: { group: '选择行为', priority: 'common', type: 'options', title: '选项树' },
    multiple: { group: '选择行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    checkStrategy: { group: '选择行为', priority: 'common', type: 'select', title: '选择策略', options: [
      { label: '子节点', value: 'child' },
      { label: '父节点', value: 'parent' },
      { label: '任意', value: 'all' },
    ] },
    filterable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可搜索', default: false },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    defaultExpandAll: { group: '高级', priority: 'advanced', type: 'boolean', title: '默认展开', default: false },
    cascade: { group: '高级', priority: 'advanced', type: 'boolean', title: '级联勾选', default: false },
    leafOnly: { group: '高级', priority: 'advanced', type: 'boolean', title: '仅叶子节点', default: false },
    maxTagCount: { group: '高级', priority: 'advanced', type: 'number', title: '最大标签数', min: 0 },
    virtualScroll: { group: '高级', priority: 'advanced', type: 'boolean', title: '虚拟滚动', default: true },
    loading: { group: '高级', priority: 'advanced', type: 'boolean', title: '加载中', default: false },
    checkable: { group: '高级', priority: 'advanced', type: 'boolean', title: '复选框模式', default: false },
    remote: { group: '高级', priority: 'advanced', type: 'boolean', title: '远程加载', default: false },
  } },
  dataSources: ['static', 'remote'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'SELECT', businessFieldType: 'SELECT', dataType: 'varchar', componentType: 'treeSelect', length: 128, precision: 2, queryType: 'eq' },
}

const customSelect = {
  type: 'customSelect',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '远程选择',
  desc: '接口驱动的下拉',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    api: { group: '选择行为', priority: 'common', type: 'string', title: '接口地址' },
    method: { group: '选择行为', priority: 'common', type: 'select', title: '请求方式', options: [
      { label: 'GET', value: 'get' },
      { label: 'POST', value: 'post' },
    ], default: 'get' },
    labelField: { group: '选择行为', priority: 'common', type: 'string', title: '显示字段', default: 'label' },
    valueField: { group: '选择行为', priority: 'common', type: 'string', title: '值字段', default: 'value' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    filterable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可搜索', default: false },
    multiple: { group: '选择行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    paramsText: { group: '高级', priority: 'advanced', type: 'json', title: '请求参数', desc: 'JSON 格式请求参数' },
    cache: { group: '高级', priority: 'advanced', type: 'boolean', title: '缓存结果', default: true },
    maxTagCount: { group: '高级', priority: 'advanced', type: 'number', title: '最大标签数', min: 0 },
    virtualScroll: { group: '高级', priority: 'advanced', type: 'boolean', title: '虚拟滚动', default: true },
    loading: { group: '高级', priority: 'advanced', type: 'boolean', title: '加载中', default: false },
  } },
  dataSources: ['remote', 'managed'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'SELECT', businessFieldType: 'SELECT', dataType: 'varchar', componentType: 'customSelect', length: 128, precision: 2, queryType: 'eq' },
}

const date = {
  type: 'date',
  aliases: ['datePicker', 'field-date'],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '日期',
  desc: '日期选择',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    format: { group: '选择行为', priority: 'common', type: 'string', title: '显示格式', default: 'yyyy-MM-dd' },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    shortcuts: { group: '高级', priority: 'advanced', type: 'json', title: '快捷选项', desc: '{ label: string, value: () => number }' },
    firstDayOfWeek: { group: '高级', priority: 'advanced', type: 'select', title: '每周首日', options: [
      { label: '周日', value: 0 },
      { label: '周一', value: 1 },
    ] },
    actions: { group: '高级', priority: 'advanced', type: 'json', title: '操作按钮', desc: '[\'clear\',\'now\'] 或 null 隐藏' },
    defaultTime: { group: '高级', priority: 'advanced', type: 'string', title: '默认时间' },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'DATE', businessFieldType: 'DATE', dataType: 'date', componentType: 'date', length: null, precision: null, queryType: 'eq' },
}

const datetime = {
  type: 'datetime',
  aliases: ['field-datetime'],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '日期时间',
  desc: '日期+时间选择',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    format: { group: '选择行为', priority: 'common', type: 'string', title: '显示格式', default: 'yyyy-MM-dd HH:mm:ss' },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    shortcuts: { group: '高级', priority: 'advanced', type: 'json', title: '快捷选项' },
    defaultTime: { group: '高级', priority: 'advanced', type: 'string', title: '默认时间', desc: '如 12:00:00' },
    timePickerFormat: { group: '高级', priority: 'advanced', type: 'string', title: '时间面板格式', default: 'HH:mm:ss' },
    actions: { group: '高级', priority: 'advanced', type: 'json', title: '操作按钮', desc: '[\'clear\',\'now\',\'confirm\'] 或 null' },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'DATETIME', businessFieldType: 'DATETIME', dataType: 'datetime', componentType: 'datetime', length: null, precision: null, queryType: 'eq' },
}

const daterange = {
  type: 'daterange',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '日期范围',
  desc: '起止日期',
  layout: { defaultSpan: 16 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    format: { group: '选择行为', priority: 'common', type: 'string', title: '显示格式', default: 'yyyy-MM-dd' },
    startPlaceholder: { group: '选择行为', priority: 'common', type: 'string', title: '起始占位' },
    endPlaceholder: { group: '选择行为', priority: 'common', type: 'string', title: '结束占位' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    shortcuts: { group: '高级', priority: 'advanced', type: 'json', title: '快捷区间' },
    firstDayOfWeek: { group: '高级', priority: 'advanced', type: 'select', title: '每周首日', options: [
      { label: '周日', value: 0 },
      { label: '周一', value: 1 },
    ] },
    actions: { group: '高级', priority: 'advanced', type: 'json', title: '操作按钮' },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'TEXT', businessFieldType: 'TEXT', dataType: 'text', componentType: 'daterange', length: null, precision: null, queryType: 'eq' },
}

const datetimerange = {
  type: 'datetimerange',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '日期时间范围',
  desc: '起止日期时间',
  layout: { defaultSpan: 16 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    format: { group: '选择行为', priority: 'common', type: 'string', title: '显示格式', default: 'yyyy-MM-dd HH:mm:ss' },
    startPlaceholder: { group: '选择行为', priority: 'common', type: 'string', title: '起始占位' },
    endPlaceholder: { group: '选择行为', priority: 'common', type: 'string', title: '结束占位' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    shortcuts: { group: '高级', priority: 'advanced', type: 'json', title: '快捷区间' },
    defaultTime: { group: '高级', priority: 'advanced', type: 'json', title: '默认时间', desc: '如 [\'00:00:00\',\'23:59:59\']' },
    actions: { group: '高级', priority: 'advanced', type: 'json', title: '操作按钮' },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'TEXT', businessFieldType: 'TEXT', dataType: 'text', componentType: 'datetimerange', length: null, precision: null, queryType: 'eq' },
}

const month = {
  type: 'month',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '月份',
  desc: '年月选择',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    format: { group: '选择行为', priority: 'common', type: 'string', title: '显示格式', default: 'yyyy-MM' },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    actions: { group: '高级', priority: 'advanced', type: 'json', title: '操作按钮', desc: '[\'clear\',\'now\'] 或 null' },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'TEXT', businessFieldType: 'TEXT', dataType: 'varchar', componentType: 'month', length: 7, precision: null, queryType: 'eq' },
}

const year = {
  type: 'year',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '年份',
  desc: '年份选择',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    format: { group: '选择行为', priority: 'common', type: 'string', title: '显示格式', default: 'yyyy' },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'TEXT', businessFieldType: 'TEXT', dataType: 'varchar', componentType: 'year', length: 4, precision: null, queryType: 'eq' },
}

const timerange = {
  type: 'timerange',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '时间范围',
  desc: '起止时间',
  layout: { defaultSpan: 16 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    format: { group: '选择行为', priority: 'common', type: 'string', title: '显示格式', default: 'HH:mm:ss' },
    startPlaceholder: { group: '选择行为', priority: 'common', type: 'string', title: '起始占位' },
    endPlaceholder: { group: '选择行为', priority: 'common', type: 'string', title: '结束占位' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    use12Hours: { group: '高级', priority: 'advanced', type: 'boolean', title: '12小时制', default: false },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'TEXT', businessFieldType: 'TEXT', dataType: 'text', componentType: 'timerange', length: null, precision: null, queryType: 'eq' },
}

const switchComp = {
  type: 'switch',
  aliases: ['field-switch'],
  scope: 'F',
  category: 'field',
  group: '选择',
  label: '开关',
  desc: '布尔开关',
  layout: { defaultSpan: 8 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    checkedText: { group: '选择行为', priority: 'common', type: 'string', title: '开文案' },
    uncheckedText: { group: '选择行为', priority: 'common', type: 'string', title: '关文案' },
    defaultValue: { group: '选择行为', priority: 'common', type: 'boolean', title: '默认值', default: false },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    round: { group: '外观', priority: 'common', type: 'boolean', title: '圆角', default: true },
    // 高级
    loading: { group: '高级', priority: 'advanced', type: 'boolean', title: '加载中', default: false },
    rubberBand: { group: '高级', priority: 'advanced', type: 'boolean', title: '橡皮筋效果', default: true },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'SWITCH', businessFieldType: 'SWITCH', dataType: 'tinyint', componentType: 'switch', length: 1, precision: 0, queryType: 'eq' },
}

// ─── 业务类（8 个）─────────────────────────────────────────

const userSelect = {
  type: 'userSelect',
  aliases: ['userPicker', 'userName', 'field-user-select'],
  scope: 'F',
  category: 'field',
  group: '业务',
  label: '人员选择',
  desc: '选系统用户（支持多选）',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    multiple: { group: '选择行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    filterable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可搜索', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    deptFilter: { group: '高级', priority: 'advanced', type: 'string', title: '部门范围过滤', desc: '限定搜索的部门 ID 或路径' },
    maxTagCount: { group: '高级', priority: 'advanced', type: 'number', title: '最大标签数', min: 0 },
  } },
  dataSources: ['builtin'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'USER', businessFieldType: 'USER', dataType: 'bigint', componentType: 'userSelect', length: null, precision: null, queryType: 'eq' },
}

const orgTreeSelect = {
  type: 'orgTreeSelect',
  aliases: ['orgSelect', 'departmentSelect', 'departmentTreeSelect', 'deptSelect', 'deptTreeSelect', 'elTreeSelect', 'orgName', 'deptName', 'field-org-tree-select'],
  scope: 'F',
  category: 'field',
  group: '业务',
  label: '部门选择',
  desc: '选组织部门树',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    multiple: { group: '选择行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    filterable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可搜索', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    maxLevel: { group: '高级', priority: 'advanced', type: 'number', title: '层级限制', desc: '限制显示的部门层级深度' },
    cascade: { group: '高级', priority: 'advanced', type: 'boolean', title: '级联勾选', default: false },
    checkStrategy: { group: '高级', priority: 'advanced', type: 'select', title: '选择策略', options: [
      { label: '子节点', value: 'child' },
      { label: '父节点', value: 'parent' },
      { label: '任意', value: 'all' },
    ] },
  } },
  dataSources: ['builtin'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'DEPT', businessFieldType: 'DEPT', dataType: 'bigint', componentType: 'orgTreeSelect', length: null, precision: null, queryType: 'eq' },
}

const regionTreeSelect = {
  type: 'regionTreeSelect',
  aliases: ['field-region-tree-select'],
  scope: 'F',
  category: 'field',
  group: '业务',
  label: '行政区划',
  desc: '省市区选择',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    level: { group: '选择行为', priority: 'common', type: 'select', title: '级数', options: [
      { label: '省', value: 1 },
      { label: '市', value: 2 },
      { label: '区', value: 3 },
    ], default: 3 },
    filterable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可搜索', default: true },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    multiple: { group: '选择行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    status: { group: '外观', priority: 'common', type: 'select', title: '状态', options: [
      { label: '默认', value: '' },
      { label: '成功', value: 'success' },
      { label: '警告', value: 'warning' },
      { label: '错误', value: 'error' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    cascade: { group: '高级', priority: 'advanced', type: 'boolean', title: '级联勾选', default: false },
    leafOnly: { group: '高级', priority: 'advanced', type: 'boolean', title: '仅叶子节点', default: false },
  } },
  dataSources: ['builtin'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'REGION', businessFieldType: 'REGION', dataType: 'varchar', componentType: 'regionTreeSelect', length: 32, precision: 2, queryType: 'eq' },
}

const objectReference = {
  type: 'objectReference',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '业务',
  label: '对象引用',
  desc: '引用另一业务对象的记录',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    targetObject: { group: '选择行为', priority: 'common', type: 'string', title: '目标对象' },
    displayField: { group: '选择行为', priority: 'common', type: 'string', title: '展示字段' },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    modalColumns: { group: '高级', priority: 'advanced', type: 'json', title: '弹窗列配置', desc: '引用弹窗中表格列定义' },
  } },
  dataSources: ['builtin'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'REFERENCE', businessFieldType: 'REFERENCE', dataType: 'bigint', componentType: 'objectReference', length: null, precision: null, queryType: 'eq' },
}

const recordSelector = {
  type: 'recordSelector',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '业务',
  label: '记录选择器',
  desc: '弹窗挑选记录（可多选、可搜索）',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 选择行为
    targetObject: { group: '选择行为', priority: 'common', type: 'string', title: '目标对象' },
    multiple: { group: '选择行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    clearable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可清空', default: true },
    placeholder: { group: '选择行为', priority: 'common', type: 'string', title: '占位提示' },
    filterable: { group: '选择行为', priority: 'common', type: 'boolean', title: '可搜索', default: true },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ] },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    // 高级
    modalColumns: { group: '高级', priority: 'advanced', type: 'json', title: '弹窗列配置', desc: '弹窗中表格列定义' },
    maxTagCount: { group: '高级', priority: 'advanced', type: 'number', title: '最大标签数', min: 0 },
  } },
  dataSources: ['builtin'],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'RECORD_SELECTOR', businessFieldType: 'RECORD_SELECTOR', dataType: 'bigint', componentType: 'recordSelector', length: null, precision: null, queryType: 'eq' },
}

const fileUpload = {
  type: 'fileUpload',
  aliases: ['upload', 'field-upload', 'field-upload'],
  scope: 'F',
  category: 'field',
  group: '业务',
  label: '文件上传',
  desc: '附件上传',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 输入行为
    maxCount: { group: '输入行为', priority: 'common', type: 'number', title: '数量上限', default: 5 },
    maxSize: { group: '输入行为', priority: 'common', type: 'number', title: '大小上限（MB）', default: 10 },
    accept: { group: '输入行为', priority: 'common', type: 'string', title: '类型限制', desc: '如 .pdf,.docx' },
    multiple: { group: '输入行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    // 常用 · 外观
    listType: { group: '外观', priority: 'common', type: 'select', title: '列表类型', options: [
      { label: '文字', value: 'text' },
      { label: '图片', value: 'image' },
      { label: '卡片', value: 'image-card' },
    ], default: 'text' },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    showRemoveButton: { group: '外观', priority: 'common', type: 'boolean', title: '显示删除', default: true },
    // 高级
    storageType: { group: '高级', priority: 'advanced', type: 'string', title: '存储位置', desc: 'TOS / local / S3' },
    showDownloadButton: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示下载', default: true },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'FILE', businessFieldType: 'FILE', dataType: 'varchar', componentType: 'fileUpload', length: 512, precision: 2, queryType: 'eq' },
}

const imageUpload = {
  type: 'imageUpload',
  aliases: ['field-image-upload'],
  scope: 'F',
  category: 'field',
  group: '业务',
  label: '图片上传',
  desc: '图片上传（可预览）',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 输入行为
    maxCount: { group: '输入行为', priority: 'common', type: 'number', title: '数量上限', default: 1 },
    maxSize: { group: '输入行为', priority: 'common', type: 'number', title: '大小上限（MB）', default: 5 },
    accept: { group: '输入行为', priority: 'common', type: 'string', title: '类型限制', default: 'image/*' },
    multiple: { group: '输入行为', priority: 'common', type: 'boolean', title: '多选', default: false },
    // 常用 · 外观
    listType: { group: '外观', priority: 'common', type: 'select', title: '列表类型', options: [
      { label: '图片', value: 'image' },
      { label: '卡片', value: 'image-card' },
    ], default: 'image-card' },
    disabled: { group: '外观', priority: 'common', type: 'boolean', title: '禁用', default: false },
    showRemoveButton: { group: '外观', priority: 'common', type: 'boolean', title: '显示删除', default: true },
    // 高级
    crop: { group: '高级', priority: 'advanced', type: 'boolean', title: '裁剪', default: false },
    watermark: { group: '高级', priority: 'advanced', type: 'boolean', title: '水印', default: false },
    showDownloadButton: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示下载', default: true },
  } },
  dataSources: [],
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'IMAGE', businessFieldType: 'IMAGE', dataType: 'varchar', componentType: 'imageUpload', length: 512, precision: 2, queryType: 'eq' },
}

const text = {
  type: 'text',
  aliases: [],
  scope: 'F',
  category: 'field',
  group: '业务',
  label: '文本展示',
  desc: '只读展示字段值',
  layout: fieldCommonLayout,
  container: false,
  propsSchema: { properties: {
    // 常用 · 外观
    template: { group: '外观', priority: 'common', type: 'string', title: '格式化模板', desc: '支持 {field} 占位符' },
    prefix: { group: '外观', priority: 'common', type: 'string', title: '前缀' },
    suffix: { group: '外观', priority: 'common', type: 'string', title: '后缀' },
    // 高级
    ellipsis: { group: '高级', priority: 'advanced', type: 'boolean', title: '文本省略', default: false },
    tooltip: { group: '高级', priority: 'advanced', type: 'boolean', title: '省略时提示', default: true },
    copyable: { group: '高级', priority: 'advanced', type: 'boolean', title: '可复制', default: false },
    strong: { group: '高级', priority: 'advanced', type: 'boolean', title: '加粗', default: false },
    italic: { group: '高级', priority: 'advanced', type: 'boolean', title: '斜体', default: false },
    underline: { group: '高级', priority: 'advanced', type: 'boolean', title: '下划线', default: false },
    delete: { group: '高级', priority: 'advanced', type: 'boolean', title: '删除线', default: false },
    code: { group: '高级', priority: 'advanced', type: 'boolean', title: '代码风格', default: false },
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
  print: fieldCommonPrint,
  fieldDefaults: { fieldType: 'TEXT', businessFieldType: 'TEXT', dataType: 'varchar', componentType: 'text', length: 255, precision: 2, queryType: 'like' },
}

// ─── 统一导出 ────────────────────────────────────────────

export const fieldComponentSpecs = [
  // 输入
  input,
  barcodeScanner,
  textarea,
  number,
  money,
  slider,
  rate,
  color,
  // 选择
  select,
  dictSelect,
  radio,
  radioButton,
  checkbox,
  transfer,
  cascader,
  treeSelect,
  customSelect,
  date,
  datetime,
  daterange,
  datetimerange,
  month,
  year,
  timerange,
  switchComp,
  // 业务
  userSelect,
  orgTreeSelect,
  regionTreeSelect,
  objectReference,
  recordSelector,
  fileUpload,
  imageUpload,
  text,
]
