const BIZ_KEY_PLACEHOLDER = '$' + '{bizKey}'

export const DEFAULT_SAMPLE_BIZ_KEY = 'PO20260001'

export const JUMP_PRESETS = [
  {
    key: 'todo',
    label: '打开待办',
    hint: '适合审批提醒。业务编号填任务 ID。',
    url: `/flow/todo?taskId=${BIZ_KEY_PLACEHOLDER}`,
  },
  {
    key: 'done',
    label: '打开已办',
    hint: '适合办结通知。业务编号填任务 ID。',
    url: `/flow/done?taskId=${BIZ_KEY_PLACEHOLDER}`,
  },
  {
    key: 'none',
    label: '不跳转',
    hint: '只看消息内容，适合公告、系统提醒。',
    url: '',
  },
  {
    key: 'custom',
    label: '打开指定页面',
    hint: `填本系统页面路径，用 ${BIZ_KEY_PLACEHOLDER} 代表单据编号。`,
    url: '',
  },
]

export const USAGE_SCENARIOS = [
  {
    key: 'approve',
    title: '审批待办',
    desc: '有人提交单据后通知审批人。点消息进入待办，处理完自动不再打扰。',
    preset: 'todo',
    example: '请假、采购、合同审批',
  },
  {
    key: 'document',
    title: '单据提醒',
    desc: '提醒同事去看某一张单，例如发货、到期、补材料。点消息打开那张单。',
    preset: 'custom',
    example: '订单、合同、客户跟进',
  },
  {
    key: 'result',
    title: '结果通知',
    desc: '告诉发起人已经通过或驳回。一般打开已办或单据详情即可。',
    preset: 'done',
    example: '审批结果、会签完成',
  },
  {
    key: 'notice',
    title: '公告提醒',
    desc: '只需要把事情说清楚，不必跳到别的页面。',
    preset: 'none',
    example: '系统维护、制度宣贯',
  },
]

export function resolveJumpPreset(jumpUrl) {
  const url = String(jumpUrl || '').trim()
  if (!url)
    return 'none'
  const matched = JUMP_PRESETS.find(item => item.url && item.url === url)
  return matched ? matched.key : 'custom'
}

export function previewJumpUrl(jumpUrl, sampleKey = DEFAULT_SAMPLE_BIZ_KEY, sampleMessageId = '1001') {
  const template = String(jumpUrl || '').trim()
  if (!template)
    return ''
  return template
    .replace(/\$\{bizKey\}/g, sampleKey || DEFAULT_SAMPLE_BIZ_KEY)
    .replace(/\$\{messageId\}/g, sampleMessageId)
}

export function describeJumpResult(jumpUrl, jumpTarget, sampleKey = DEFAULT_SAMPLE_BIZ_KEY) {
  const preview = previewJumpUrl(jumpUrl, sampleKey)
  if (!preview)
    return `发送业务编号 ${sampleKey} 后，同事点开消息只会看到文字，不会跳到别的页面。`
  const where = jumpTarget === '_blank' ? '新窗口打开' : '在当前页打开'
  return `发送业务编号 ${sampleKey} 后，同事点「查看详情」会${where}：${preview}`
}

export function normalizeBizTypeCode(value) {
  return String(value || '')
    .trim()
    .replace(/\s+/g, '_')
    .toUpperCase()
}

export function isBuiltInFlowTodo(bizType) {
  return normalizeBizTypeCode(bizType) === 'FLOW_TODO'
}

export function buildJavaSendExample({ bizType, bizName, sampleKey } = {}) {
  const code = normalizeBizTypeCode(bizType) || 'PURCHASE_ORDER'
  const key = sampleKey || DEFAULT_SAMPLE_BIZ_KEY
  const title = bizName ? `${bizName}待处理` : '待处理通知'
  return `// 业务发生时发一条可点开的通知
MessageSendRequestDTO req = new MessageSendRequestDTO();
req.setTitle("${title}");
req.setContent("请处理 ${key}");
req.setUserIds(Set.of(approverId));
req.setSendScope("USERS");
req.setChannel("WEB");
req.setType("SYSTEM");
req.setBizType("${code}");
req.setBizKey("${key}");
messageService.sendIfAbsent(req, "${code}", "${key}");

// 单据处理完，把同一笔站内信标为已读
messageService.markWebReadByBiz("${code}", "${key}");`
}

export function buildHttpSendExample({ bizType, bizName, sampleKey } = {}) {
  const code = normalizeBizTypeCode(bizType) || 'PURCHASE_ORDER'
  const key = sampleKey || DEFAULT_SAMPLE_BIZ_KEY
  const title = bizName ? `${bizName}待处理` : '待处理通知'
  return `POST /api/message/send
{
  "title": "${title}",
  "content": "请处理 ${key}",
  "userIds": [1001],
  "sendScope": "USERS",
  "channel": "WEB",
  "type": "SYSTEM",
  "bizType": "${code}",
  "bizKey": "${key}"
}`
}
