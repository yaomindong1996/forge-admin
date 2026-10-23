/**
 * 受控表单控件不能把 schema 里的 value / modelValue / update 监听再绑一遍。
 * Vue 后写的 v-bind 会盖掉 :value，输入框表现为能聚焦、handleUpdate 有日志，但内容写不进去。
 *
 * readonly / disabled 由 AiFormItem 的 disabledHandler 统一处理（只读 → disabled 控件），
 * 不能再经 v-bind 渗入 Naive 组件：Select / DatePicker 等收到原生 readonly 时表现不稳定。
 * 不要用「纯文本替换」冒充只读——空值时看起来像组件消失。
 */
export function resolveControlProps(props = {}) {
  const source = props && typeof props === 'object' ? props : {}
  const {
    value: _value,
    defaultValue: _defaultValue,
    modelValue: _modelValue,
    'onUpdate:value': _onUpdateValue,
    'onUpdate:modelValue': _onUpdateModelValue,
    onUpdateValue: _onUpdateValueCamel,
    onUpdateModelValue: _onUpdateModelValueCamel,
    'on-update:value': _onUpdateValueKebab,
    'on-update:model-value': _onUpdateModelValueKebab,
    'onUpdate:label-value': _onUpdateLabelValue,
    'on-update:label-value': _onUpdateLabelValueKebab,
    readonly: _readonly,
    disabled: _disabled,
    // 设计器/运行态元数据，不是 Naive 控件 props
    optionSource: _optionSource,
    fieldMappings: _fieldMappings,
    mappings: _mappings,
    recordSelector: _recordSelector,
    runtimeRules: _runtimeRules,
    fieldEvents: _fieldEvents,
    // options 由 AiFormItem.currentOptions 统一接管；设计器常残留 options:[]，
    // 若经 v-bind 盖掉 :options，远程下拉会变成空列表、表现为「选不中」
    options: _options,
    labelValueField: _labelValueField,
    labelValue: _labelValue,
    displayFields: _displayFields,
    referenceObjectCode: _referenceObjectCode,
    referenceDisplayField: _referenceDisplayField,
    referenceValueField: _referenceValueField,
    dataBinding: _dataBinding,
    cascade: _cascade,
    cascadeConfig: _cascadeConfig,
    visibility: _visibility,
    validation: _validation,
    // 设计器私有字段：交互事件 / 样式戳 / E2E 标记，禁止渗入 Naive 组件
    __events: _events,
    __designerStyle: _designerStyle,
    e2eMarker: _e2eMarker,
    // 选项源元数据（已由 currentOptions / AiCustomSelect 专用 props 接管）
    dataSourceType: _dataSourceType,
    labelName: _labelName,
    valueName: _valueName,
    // 前后缀 / 图标 / 下拉插槽文案走 slot 或独立绑定，不能当 Naive props 透传
    prefix: _prefix,
    suffix: _suffix,
    prefixIcon: _prefixIcon,
    suffixIcon: _suffixIcon,
    clearIcon: _clearIcon,
    passwordVisibleIcon: _passwordVisibleIcon,
    passwordInvisibleIcon: _passwordInvisibleIcon,
    inputType: _inputType,
    currencySymbol: _currencySymbol,
    dropdownHeader: _dropdownHeader,
    dropdownAction: _dropdownAction,
    emptyText: _emptyText,
    arrowIcon: _arrowIcon,
    // DatePicker Slots 配置（走 AiFormDatePicker 插槽）
    dateIcon: _dateIcon,
    dateFooter: _dateFooter,
    prevYearIcon: _prevYearIcon,
    prevMonthIcon: _prevMonthIcon,
    nextMonthIcon: _nextMonthIcon,
    nextYearIcon: _nextYearIcon,
    // separator 留给 pair 输入 / 日期范围插槽，由 AiFormDatePicker props 显式绑定，勿透传
    separator: _separator,
    ...rest
  } = source
  // 兜底去掉其它 __ 前缀元数据，避免落到 DOM / 控件未知 props
  const cleaned = {}
  for (const [key, value] of Object.entries(rest)) {
    if (key.startsWith('__'))
      continue
    cleaned[key] = value
  }
  return cleaned
}
