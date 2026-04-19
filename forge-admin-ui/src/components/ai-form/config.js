/**
 * AI Form 配置文件
 * 定义表单字段配置的类型和默认值
 */

/**
 * 表单字段配置示例
 *
 * @example
 * {
 *   field: 'username',          // 字段名（必填）
 *   label: '用户名',            // 标签（必填）
 *   type: 'input',              // 字段类型（必填）
 *   placeholder: '请输入用户名',
 *   defaultValue: '',           // 默认值
 *   required: true,             // 是否必填
 *   disabled: false,            // 是否禁用
 *   clearable: true,            // 是否可清空
 *   span: 1,                    // 栅格占位
 *   rules: [],                  // 自定义验证规则
 *   ...其他 Naive UI 组件支持的属性
 * }
 */

export const FIELD_TYPES = {
  INPUT: 'input', // 输入框
  TEXTAREA: 'textarea', // 多行文本
  NUMBER: 'number', // 数字输入
  SELECT: 'select', // 下拉选择
  RADIO: 'radio', // 单选框
  RADIO_BUTTON: 'radioButton', // 单选按钮组
  CHECKBOX: 'checkbox', // 多选框
  DATE: 'date', // 日期选择
  DATETIME: 'datetime', // 日期时间
  DATERANGE: 'daterange', // 日期范围
  MONTH: 'month', // 月份选择
  YEAR: 'year', // 年份选择
  TIME: 'time', // 时间选择
  SWITCH: 'switch', // 开关
  SLIDER: 'slider', // 滑块
  RATE: 'rate', // 评分
  COLOR: 'color', // 颜色选择器
  UPLOAD: 'upload', // 文件上传
  CASCADER: 'cascader', // 级联选择
  TREE_SELECT: 'treeSelect', // 树形选择
  TRANSFER: 'transfer', // 穿梭框
  SLOT: 'slot', // 自定义插槽
  TEXT: 'text', // 纯文本展示
  CUSTOM_SELECT: 'customSelect', // 远程搜索下拉选择
}

/**
 * 创建字段配置的辅助函数
 */
export function createField(config) {
  return {
    showLabel: true,
    showFeedback: true,
    clearable: true,
    ...config,
  }
}

/**
 * 快速创建常用字段
 */
export const FieldFactory = {
  // 输入框
  input(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.INPUT,
      placeholder: `请输入${label}`,
      ...config,
    })
  },

  // 多行文本
  textarea(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.TEXTAREA,
      placeholder: `请输入${label}`,
      rows: 3,
      ...config,
    })
  },

  // 下拉选择
  select(field, label, options, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.SELECT,
      placeholder: `请选择${label}`,
      options,
      filterable: true,
      ...config,
    })
  },

  // 单选框
  radio(field, label, options, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.RADIO,
      options,
      ...config,
    })
  },

  // 多选框
  checkbox(field, label, options, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.CHECKBOX,
      options,
      ...config,
    })
  },

  // 日期选择
  date(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.DATE,
      placeholder: `请选择${label}`,
      ...config,
    })
  },

  // 开关
  switch(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.SWITCH,
      ...config,
    })
  },

  // 数字输入
  number(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.NUMBER,
      placeholder: `请输入${label}`,
      ...config,
    })
  },

  // 单选按钮组
  radioButton(field, label, options, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.RADIO_BUTTON,
      options,
      ...config,
    })
  },

  // 日期范围
  dateRange(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.DATERANGE,
      ...config,
    })
  },

  // 日期时间
  dateTime(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.DATETIME,
      placeholder: `请选择${label}`,
      ...config,
    })
  },

  // 时间选择
  time(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.TIME,
      placeholder: `请选择${label}`,
      ...config,
    })
  },

  // 滑块
  slider(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.SLIDER,
      min: 0,
      max: 100,
      ...config,
    })
  },

  // 评分
  rate(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.RATE,
      ...config,
    })
  },

  // 颜色选择器
  color(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.COLOR,
      ...config,
    })
  },

  // 级联选择
  cascader(field, label, options, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.CASCADER,
      placeholder: `请选择${label}`,
      options,
      ...config,
    })
  },

  // 树形选择
  treeSelect(field, label, options, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.TREE_SELECT,
      placeholder: `请选择${label}`,
      options,
      ...config,
    })
  },

  // 穿梭框
  transfer(field, label, options, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.TRANSFER,
      options,
      ...config,
    })
  },

  // 文件上传
  upload(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.UPLOAD,
      ...config,
    })
  },

  // 自定义插槽
  slot(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.SLOT,
      ...config,
    })
  },

  // 纯文本展示
  text(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.TEXT,
      ...config,
    })
  },

  // 远程搜索下拉选择
  customSelect(field, label, config = {}) {
    return createField({
      field,
      label,
      type: FIELD_TYPES.CUSTOM_SELECT,
      placeholder: `请选择${label}`,
      filterable: true,
      ...config,
    })
  },
}

export default {
  FIELD_TYPES,
  createField,
  FieldFactory,
}
