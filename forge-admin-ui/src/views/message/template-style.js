export const CARD_STYLE_SNIPPETS = [
  {
    key: 'gray',
    label: '次要说明',
    hint: '灰色小字，适合卡片抬头',
    insert: '<div class="gray">次要说明</div>\n',
  },
  {
    key: 'normal',
    label: '正文',
    hint: '普通内容行',
    insert: '<div class="normal">正文</div>\n',
  },
  {
    key: 'highlight',
    label: '强调',
    hint: '蓝色提示，适合行动号召',
    insert: '<div class="highlight">请尽快处理</div>\n',
  },
]

export const HTML_STYLE_SNIPPETS = [
  {
    key: 'p',
    label: '一段话',
    hint: '邮件/站内信常用段落',
    insert: '<p>一段话</p>\n',
  },
  {
    key: 'b',
    label: '加粗',
    hint: '突出关键词',
    insert: '<b>重点</b>',
  },
  {
    key: 'link',
    label: '链接',
    hint: '跳转到业务页面',
    insert: `<a href="${'$'}{jumpUrl}">查看详情</a>`,
  },
]

export const MARKDOWN_STYLE_SNIPPETS = [
  {
    key: 'h3',
    label: '小标题',
    hint: '钉钉卡片标题',
    insert: '### 标题\n\n',
  },
  {
    key: 'li',
    label: '一条',
    hint: '列表项',
    insert: '- 内容\n',
  },
  {
    key: 'link',
    label: '链接',
    hint: '跳转链接',
    insert: `[查看详情](${'$'}{url})\n`,
  },
]

export const SYSTEM_BUILT_IN_VARIABLES = [
  { key: 'Title', label: '标题' },
  { key: 'CreatorUserName', label: '创建人' },
  { key: 'SendTime', label: '发送时间' },
  { key: 'userName', label: '接收人' },
  { key: 'content', label: '通知内容' },
  { key: 'taskName', label: '任务名称' },
  { key: 'taskTitle', label: '任务标题' },
  { key: 'deadline', label: '截止时间' },
  { key: 'flowName', label: '流程名称' },
  { key: 'processName', label: '流程名称' },
  { key: 'approver', label: '审批人' },
  { key: 'approveTime', label: '审批时间' },
  { key: 'code', label: '验证码' },
  { key: 'expireMinutes', label: '有效分钟' },
  { key: 'dueDate', label: '到期时间' },
  { key: 'overdueMinutes', label: '逾期分钟' },
  { key: 'jumpUrl', label: '跳转地址' },
  { key: 'result', label: '审批结果' },
  { key: 'applyUserName', label: '发起人' },
  { key: 'startUserName', label: '发起人' },
  { key: 'url', label: '跳转地址' },
]

export const TEMPLATE_VARIABLE_CATALOG = {
  SYSTEM_NOTICE: [
    { key: 'userName', label: '接收人' },
    { key: 'content', label: '通知内容' },
  ],
  TASK_ASSIGN: [
    { key: 'userName', label: '接收人' },
    { key: 'taskName', label: '任务名称' },
    { key: 'deadline', label: '截止时间' },
  ],
  SMS_VERIFY_CODE: [
    { key: 'code', label: '验证码' },
    { key: 'expireMinutes', label: '有效分钟' },
  ],
  APPROVAL_PASS: [
    { key: 'userName', label: '接收人' },
    { key: 'flowName', label: '流程名称' },
    { key: 'approver', label: '审批人' },
    { key: 'approveTime', label: '审批时间' },
  ],
  FLOW_TASK_OVERDUE: [
    { key: 'taskId', label: '任务ID' },
    { key: 'taskName', label: '任务名称' },
    { key: 'taskTitle', label: '任务标题' },
    { key: 'processName', label: '流程名称' },
    { key: 'processInstanceId', label: '流程实例' },
    { key: 'startUserName', label: '发起人' },
    { key: 'dueDate', label: '截止时间' },
    { key: 'overdueMinutes', label: '逾期分钟' },
    { key: 'jumpUrl', label: '跳转地址' },
  ],
  FLOW_TODO_CARD: [
    { key: 'taskTitle', label: '任务标题' },
    { key: 'processName', label: '流程名称' },
    { key: 'startUserName', label: '发起人' },
  ],
  FLOW_RESULT_CARD: [
    { key: 'processName', label: '流程名称' },
    { key: 'result', label: '审批结果' },
    { key: 'applyUserName', label: '发起人' },
  ],
  FLOW_CC_CARD: [
    { key: 'processName', label: '流程名称' },
  ],
}

export const TEMPLATE_SAMPLE_VALUES = {
  userName: '张三',
  content: '请尽快处理',
  Title: '系统通知',
  CreatorUserName: '管理员',
  SendTime: '2026-09-08 09:30',
  taskName: '合同审批',
  taskTitle: '采购单审批',
  taskId: 'task_1024',
  deadline: '2026-09-08 18:00',
  flowName: '采购审批',
  processName: '采购审批',
  processInstanceId: 'proc_20260908',
  startUserName: '王五',
  applyUserName: '王五',
  approver: '李四',
  approveTime: '2026-09-08 10:20',
  result: '已通过',
  code: '839201',
  expireMinutes: '5',
  dueDate: '2026-09-08 18:00:00',
  overdueMinutes: '35',
  jumpUrl: '/flow/todo?taskId=task_1024',
  url: '/flow/todo?taskId=task_1024',
}

export function resolveContentStyleMode({ channel, templateCode } = {}) {
  const code = String(templateCode || '').toUpperCase()
  const ch = String(channel || '').toUpperCase()
  if (ch === 'SMS')
    return 'plain'
  if (code.includes('DINGTALK'))
    return 'markdown'
  if (code.includes('FEISHU'))
    return 'plain'
  if (ch === 'COLLABORATION' || code.includes('_CARD'))
    return 'card'
  return 'html'
}

export function getStyleSnippets(mode) {
  if (mode === 'card')
    return CARD_STYLE_SNIPPETS
  if (mode === 'markdown')
    return MARKDOWN_STYLE_SNIPPETS
  if (mode === 'html')
    return [...CARD_STYLE_SNIPPETS, ...HTML_STYLE_SNIPPETS]
  return []
}

export function getStyleGuide(mode) {
  if (mode === 'plain')
    return '短信和部分协同平台只认纯文字，颜色、加粗和链接都不会生效。'
  if (mode === 'markdown')
    return '钉钉卡片用 Markdown：### 标题、- 列表、[文字](链接)。不要写 HTML。'
  if (mode === 'card')
    return '企业微信卡片只认三种行样式：次要说明、正文、强调。点按钮插入即可，不要自己写 CSS。'
  return '站内信和邮件可以用简单 HTML。企业微信那三种行样式在站内信里同样生效。'
}

export function insertAtCursor(text, snippet, cursor) {
  const current = String(text || '')
  const insert = String(snippet || '')
  const index = typeof cursor === 'number' && cursor >= 0 && cursor <= current.length
    ? cursor
    : current.length
  return {
    value: `${current.slice(0, index)}${insert}${current.slice(index)}`,
    cursor: index + insert.length,
  }
}

export function extractTemplateVariables(...contents) {
  const result = []
  const pattern = /\$\{([a-z_]\w*)\}|\{([a-z_]\w*)\}/gi
  contents.forEach((content) => {
    String(content || '').replace(pattern, (_match, dollarKey, braceKey) => {
      const key = dollarKey || braceKey
      if (key && !result.includes(key))
        result.push(key)
      return _match
    })
  })
  return result
}

export function formatPlaceholder(key) {
  return key ? `\${${key}}` : ''
}

export function renderTemplatePreview(content, samples = TEMPLATE_SAMPLE_VALUES) {
  return String(content || '').replace(/\$\{([a-z_]\w*)\}|\{([a-z_]\w*)\}/gi, (_match, dollarKey, braceKey) => {
    const key = dollarKey || braceKey
    return samples[key] || `${key}示例`
  })
}

export function sanitizePreviewHtml(html) {
  return String(html || '')
    .replace(/<\s*(script|iframe|object|embed|link|meta|style)\b[^>]*>[\s\S]*?<\s*\/\s*\1\s*>/gi, '')
    .replace(/<\s*(?:script|iframe|object|embed|link|meta|style)\b[^>]*>/gi, '')
    .replace(/\son[a-z]+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)/gi, '')
    .replace(/(href|src)\s*=\s*(?:"\s*javascript:[^"]*"|'\s*javascript:[^']*')/gi, '$1="#"')
}

function escapeHtml(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

export function previewMarkdown(content) {
  const escaped = escapeHtml(renderTemplatePreview(content))
  return escaped
    .replace(/^### (.*)$/gm, '<strong>$1</strong>')
    .replace(/^- (.*)$/gm, '<div>• $1</div>')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>')
    .replace(/\n/g, '<br>')
}

export function previewPlain(content) {
  return escapeHtml(renderTemplatePreview(content)).replace(/\n/g, '<br>')
}

export function renderPreviewHtml(content, mode) {
  const filled = renderTemplatePreview(content)
  if (mode === 'markdown')
    return sanitizePreviewHtml(previewMarkdown(content))
  if (mode === 'plain')
    return previewPlain(content)
  return sanitizePreviewHtml(filled)
}

export function hasUnsupportedAtVariables(...contents) {
  return contents.some(content => /@[a-z_]\w*/i.test(String(content || '')))
}

export function normalizeTemplateCode(value) {
  return String(value || '')
    .trim()
    .replace(/\s+/g, '_')
    .toUpperCase()
}

export function buildJavaTemplateExample({ templateCode, variables } = {}) {
  const code = normalizeTemplateCode(templateCode) || 'FLOW_TODO_CARD'
  const keys = (variables || []).slice(0, 4)
  const paramLines = keys.length
    ? keys.map((key, index) => `    "${key}", "${TEMPLATE_SAMPLE_VALUES[key] || '示例'}"${index === keys.length - 1 ? '' : ','}`).join('\n')
    : '    "content", "请尽快处理"'
  return `MessageSendRequestDTO req = new MessageSendRequestDTO();
req.setTemplateCode("${code}");
req.setParams(Map.of(
${paramLines}
));
req.setUserIds(Set.of(userId));
req.setSendScope("USERS");
req.setChannel("WEB");
req.setType("SYSTEM");
messageService.send(req);`
}

export function buildHttpTemplateExample({ templateCode, variables } = {}) {
  const code = normalizeTemplateCode(templateCode) || 'FLOW_TODO_CARD'
  const keys = (variables || []).slice(0, 4)
  const params = keys.length
    ? keys.map(key => `    "${key}": "${TEMPLATE_SAMPLE_VALUES[key] || '示例'}"`).join(',\n')
    : '    "content": "请尽快处理"'
  return `POST /api/message/send
{
  "templateCode": "${code}",
  "params": {
${params}
  },
  "userIds": [1001],
  "sendScope": "USERS",
  "channel": "WEB",
  "type": "SYSTEM"
}`
}
