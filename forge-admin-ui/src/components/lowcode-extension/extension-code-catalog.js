const HOOK_GUIDES = {
  PAGE_INIT: {
    label: '页面初始化',
    description: '页面根节点准备完成后执行，适合初始化默认值和页面提示。',
  },
  FORM_CHANGE: {
    label: '字段变化',
    description: '白名单字段发生变化时执行，适合联动计算、赋值和即时提示。',
  },
  BEFORE_SUBMIT: {
    label: '提交前',
    description: '表单进入提交处理前执行，适合业务校验；抛出错误会使本次增强失败。',
  },
  AFTER_SUBMIT: {
    label: '提交后',
    description: '表单提交成功后执行，适合成功提示和触发已授权的后续动作。',
  },
  ROW_ACTION: {
    label: '列表行操作',
    description: '用户触发受控列表行操作时执行，只能调用已授权动作。',
  },
}

const JS_EXAMPLES = [
  {
    id: 'page-defaults',
    title: '初始化默认值',
    description: '页面打开时，在状态字段为空的情况下写入默认值。',
    hooks: ['PAGE_INIT'],
    testFields: ['status'],
    testContext: {
      record: { id: 1, status: null },
      allowedActions: [],
    },
    code: [
      "const status = readField('status')",
      '',
      'if (!status) {',
      "  setField('status', 'DRAFT')",
      "  showMessage('已初始化为草稿状态', 'info')",
      '}',
      '',
      'return { initialized: !status }',
    ].join('\n'),
  },
  {
    id: 'field-calculate',
    title: '字段联动计算',
    description: '数量或单价变化时，重新计算金额字段。',
    hooks: ['FORM_CHANGE'],
    testFields: ['quantity', 'unitPrice', 'amount'],
    testContext: {
      record: { id: 1, quantity: 2, unitPrice: 68, amount: 0 },
      allowedActions: [],
    },
    code: [
      "const quantity = Number(readField('quantity') || 0)",
      "const unitPrice = Number(readField('unitPrice') || 0)",
      'const amount = Number((quantity * unitPrice).toFixed(2))',
      '',
      "setField('amount', amount)",
      "showMessage('金额已自动计算', 'success')",
      '',
      'return { amount }',
    ].join('\n'),
  },
  {
    id: 'submit-validation',
    title: '提交前业务校验',
    description: '金额不合法时抛出错误，配合“阻断”失败策略终止当前动作。',
    hooks: ['BEFORE_SUBMIT'],
    testFields: ['amount'],
    testContext: {
      record: { id: 1, amount: 120 },
      allowedActions: [],
    },
    code: [
      "const amount = Number(readField('amount') || 0)",
      '',
      'if (amount <= 0) {',
      "  throw new Error('金额必须大于 0')",
      '}',
      '',
      "showMessage('提交校验通过', 'success')",
      'return { passed: true, amount }',
    ].join('\n'),
  },
  {
    id: 'submit-success',
    title: '提交成功后提示',
    description: '提交成功后显示反馈，不修改业务数据。',
    hooks: ['AFTER_SUBMIT'],
    testFields: [],
    testContext: {
      record: { id: 1 },
      allowedActions: [],
    },
    code: [
      "showMessage('保存成功', 'success')",
      '',
      "return { notified: true }",
    ].join('\n'),
  },
  {
    id: 'follow-up-action',
    title: '触发后续页面动作',
    description: '调用页面已经授权的动作；测试上下文需同时声明该动作编码。',
    hooks: ['AFTER_SUBMIT', 'ROW_ACTION'],
    testFields: [],
    testContext: {
      record: { id: 1 },
      allowedActions: ['refresh_business_list'],
    },
    code: [
      "triggerAction('refresh_business_list', { source: 'extension' })",
      "showMessage('已触发列表刷新', 'info')",
      '',
      "return { action: 'refresh_business_list' }",
    ].join('\n'),
  },
]

const CSS_EXAMPLES = [
  {
    id: 'business-card',
    title: '业务卡片',
    description: '调整当前页面业务卡片的边框、圆角和留白。',
    hooks: ['PAGE_INIT'],
    code: [
      '.business-card {',
      '  padding: 16px;',
      '  border: 1px solid var(--border-light, #d9e2ef);',
      '  border-radius: 8px;',
      '  background: var(--bg-primary, #ffffff);',
      '  box-shadow: 0 2px 10px rgba(31, 41, 55, 0.06);',
      '}',
    ].join('\n'),
  },
  {
    id: 'important-field',
    title: '重点字段提示',
    description: '突出当前页面中带有 amount-field 类名的表单区域。',
    hooks: ['PAGE_INIT'],
    code: [
      '.amount-field {',
      '  padding: 10px 12px;',
      '  border-left: 3px solid #d99a2b;',
      '  border-radius: 4px;',
      '  background: rgba(217, 154, 43, 0.08);',
      '}',
      '',
      '.amount-field .n-form-item-label {',
      '  font-weight: 600;',
      '}',
    ].join('\n'),
  },
  {
    id: 'status-cell',
    title: '列表状态强调',
    description: '给当前页面中不同状态的单元格增加克制的颜色区分。',
    hooks: ['PAGE_INIT'],
    code: [
      '.status-cell {',
      '  display: inline-flex;',
      '  padding: 2px 8px;',
      '  border-radius: 4px;',
      '  font-weight: 600;',
      '}',
      '',
      '.status-cell[data-status="warning"] {',
      '  color: #9a6700;',
      '  background: rgba(217, 154, 43, 0.12);',
      '}',
    ].join('\n'),
  },
]

export const JS_CAPABILITIES = [
  {
    code: "readField('fieldCode')",
    title: '读取表单字段',
    description: '只能读取“测试允许字段”或运行时白名单中的字段。',
    snippet: "readField('fieldCode')",
  },
  {
    code: "setField('fieldCode', value)",
    title: '修改表单字段',
    description: '写回当前页面白名单字段，不直接操作 DOM。',
    snippet: "setField('fieldCode', value)",
  },
  {
    code: "showMessage('内容', 'info')",
    title: '显示页面消息',
    description: '级别支持 info、success、warning、error。',
    snippet: "showMessage('提示内容', 'info')",
  },
  {
    code: "triggerAction('actionCode', payload)",
    title: '触发授权动作',
    description: '只能调用页面上下文明确授权的动作编码。',
    snippet: "triggerAction('actionCode', { source: 'extension' })",
  },
]

export const CSS_CAPABILITIES = [
  {
    code: '.business-card',
    title: '页面业务组件',
    description: '增强表单区块、卡片、详情块等已配置 class 的页面元素；className 可在表单设计器属性面板设置。',
    snippet: '.business-card {\n  border-color: #c8d4e3;\n}',
  },
  {
    code: '.field-class .n-input',
    title: '字段内部控件',
    description: '从表单设计器中配置的字段 className 向内调整输入框、选择器等局部控件。',
    snippet: '.field-class .n-input {\n  font-weight: 600;\n}',
  },
  {
    code: '[data-status="warning"]',
    title: '业务状态元素',
    description: '按页面元素已有的业务属性呈现状态，不修改数据本身。',
    snippet: '.status-cell[data-status="warning"] {\n  color: #9a6700;\n}',
  },
]

export const JS_BOUNDARIES = [
  { allowed: true, text: '读取和修改当前页面白名单字段' },
  { allowed: true, text: '显示受控消息并触发已授权页面动作' },
  { allowed: false, text: '不能访问 DOM、window、Cookie、浏览器存储或网络' },
  { allowed: false, text: '不能异步执行、加载模块或使用动态代码' },
]

export const CSS_BOUNDARIES = [
  { allowed: true, text: '只增强当前应用、当前页面根节点内的元素' },
  { allowed: true, text: '支持普通选择器及受控 media、supports、container、layer' },
  { allowed: false, text: '不能覆盖 html、body、Forge 导航、侧栏或全局布局' },
  { allowed: false, text: '不能使用外部 URL、@import、position: fixed 或超高层级' },
]

export function getExtensionCodeExamples(mode, hookCode) {
  const source = mode === 'css' ? CSS_EXAMPLES : JS_EXAMPLES
  return source.filter(item => !hookCode || item.hooks.includes(hookCode))
}

export function getExtensionHookGuide(hookCode) {
  return HOOK_GUIDES[hookCode] || {
    label: '当前触发点',
    description: '增强只会在所选标准触发点执行。',
  }
}
