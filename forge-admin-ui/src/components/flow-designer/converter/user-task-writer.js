/**
 * UserTask 属性回写为 BPMN XML 片段（Task 5）
 *
 * 与 user-task-parser.js 的提取规则对偶，确保 XML→JSON→XML 语义等价。
 *
 * 写出范围：
 * - 属性：flowable:assignee / assigneeType / assigneeName / candidateUsers / candidateGroups
 *   / formKey / formJson / formUrl / priority / dueDate / 8 个权限布尔
 * - 子元素：multiInstanceLoopCharacteristics + completionCondition
 * - extensionElements：taskListener[] + executionListener[]
 *
 * 输出形式：纯 XML 字符串，外层不含 <userTask>，仅含 attribute 串与子元素串。
 * 调用方负责把 attribute 拼到开标签上、子元素拼到 <userTask> ... </userTask> 之间。
 */

import { normalizeFlowFormPermissions, serializeFlowFormPermissions } from '@/utils/flow-field-permissions'
import { buildCompletionExpression } from './completion-condition.js'
import { escapeXmlAttr, escapeXmlText } from './xml-escape.js'

const PERMISSION_KEYS = [
  'allowApprove',
  'allowReject',
  'allowRejectToStart',
  'allowDelegate',
  'allowReturn',
  'allowTerminate',
  'requireSignature',
  'requireComment',
]

const PERMISSION_DEFAULTS = {
  allowApprove: true,
  allowReject: true,
  allowRejectToStart: false,
  allowDelegate: true,
  allowReturn: false,
  allowTerminate: false,
  requireSignature: false,
  requireComment: true,
}

const OVERDUE_REMINDER_DEFAULTS = {
  templateCode: 'FLOW_TASK_OVERDUE',
  channels: ['WEB'],
  repeatMode: 'once',
  intervalMinutes: 1440,
  maxTimes: 1,
}

const DOLLAR = '$'
const STATIC_ASSIGNEES = new Set([
  `${DOLLAR}{initiator}`,
  `${DOLLAR}{initiatorLeader}`,
  `${DOLLAR}{deptManager}`,
  `${DOLLAR}{hr}`,
])

/**
 * 把 UserTask config 写为属性串 + 子元素串。
 *
 * @param {object} config user-task-parser 的输出
 * @returns {{ attrs: string, children: string }} 属性串与子元素串
 */
export function writeUserTaskConfig(config) {
  const cfg = config || {}
  const attrs = []
  const children = []

  // assignee 三种模式
  if (cfg.taskType === 'assignee') {
    let val = cfg.assignee
    let typeMark = null
    if (cfg.assignee === 'initiatorSelect') {
      // 发起时由申请人选择本节点审批人，实际任务通过 PROCESS_START_USER 多实例集合创建。
      val = ''
    }
    else if (cfg.assignee === 'spel') {
      val = cfg.assigneeExpr || ''
      typeMark = 'spel'
    }
    else if (cfg.assignee === 'custom') {
      val = normalizeFixedAssigneeUserId(cfg.assigneeUserId)
        || extractLegacyFixedAssigneeUserId(cfg.assigneeExpr)
      typeMark = 'custom'
    }
    else if (STATIC_ASSIGNEES.has(cfg.assignee)) {
      val = cfg.assignee
    }
    // 其他情况：assignee 字段直接是字符串值（兼容历史）
    if (val) {
      attrs.push(`flowable:assignee="${escapeXmlAttr(val)}"`)
      if (cfg.assigneeUserName)
        attrs.push(`flowable:assigneeName="${escapeXmlAttr(cfg.assigneeUserName)}"`)
      if (typeMark)
        attrs.push(`flowable:assigneeType="${typeMark}"`)
      if (typeMark === 'spel' && cfg.spelTemplate)
        attrs.push(`flowable:spelTemplate="${escapeXmlAttr(cfg.spelTemplate)}"`)
    }
    else if (cfg.assignee === 'spel') {
      attrs.push('flowable:assigneeType="spel"')
    }
    if (cfg.assignee === 'initiatorSelect') {
      const nodeKey = normalizeOptionalText(cfg.initiatorSelectNodeKey || cfg.bpmnElementId || cfg.nodeId)
      const useCountersign = cfg.multiInstanceType && cfg.multiInstanceType !== 'none'
      attrs.push(`flowable:assigneeType="initiatorSelect"`)
      attrs.push(`flowable:assignee="${useCountersign
        ? `${DOLLAR}{assignee}`
        : `${DOLLAR}{INITIATOR_SELECT_${sanitizeVariableName(nodeKey)}}`}"`)
    }
  }
  else if (cfg.taskType === 'candidateUsers' && cfg.candidateUsers?.length) {
    attrs.push(`flowable:candidateUsers="${escapeXmlAttr(cfg.candidateUsers.join(','))}"`)
    if (cfg.candidateUserNames?.length)
      attrs.push(`flowable:candidateUserNames="${escapeXmlAttr(cfg.candidateUserNames.join(','))}"`)
  }
  else if (cfg.taskType === 'candidateGroups' && cfg.candidateGroups?.length) {
    attrs.push(`flowable:candidateGroups="${escapeXmlAttr(cfg.candidateGroups.join(','))}"`)
    if (cfg.candidateGroupNames?.length)
      attrs.push(`flowable:candidateGroupNames="${escapeXmlAttr(cfg.candidateGroupNames.join(','))}"`)
  }

  // form
  if (cfg.formMode)
    attrs.push(`flowable:formMode="${escapeXmlAttr(cfg.formMode)}"`)
  if (cfg.formKey)
    attrs.push(`flowable:formKey="${escapeXmlAttr(cfg.formKey)}"`)
  if (cfg.formName)
    attrs.push(`flowable:formName="${escapeXmlAttr(cfg.formName)}"`)
  if (cfg.providerKey)
    attrs.push(`flowable:providerKey="${escapeXmlAttr(cfg.providerKey)}"`)
  if (cfg.formJson)
    attrs.push(`flowable:formJson="${escapeXmlAttr(cfg.formJson)}"`)
  if (cfg.formUrl)
    attrs.push(`flowable:formUrl="${escapeXmlAttr(cfg.formUrl)}"`)
  if (cfg.viewKey)
    attrs.push(`flowable:viewKey="${escapeXmlAttr(cfg.viewKey)}"`)
  const formRef = stringifyFormRef(cfg.formRef)
  if (formRef)
    attrs.push(`flowable:formRef="${escapeXmlAttr(formRef)}"`)
  if (String(cfg.printTemplatePolicy || 'INHERIT').toUpperCase() === 'RESTRICT') {
    attrs.push('flowable:printTemplatePolicy="RESTRICT"')
    const printTemplateIds = normalizePrintTemplateIds(cfg.printTemplateIds)
    if (printTemplateIds.length)
      attrs.push(`flowable:printTemplateIds="${escapeXmlAttr(printTemplateIds.join(','))}"`)
  }
  if ((Array.isArray(cfg.formFieldPermissions) && cfg.formFieldPermissions.length)
    || (cfg.formFieldPermissions && typeof cfg.formFieldPermissions === 'object')
    || (Array.isArray(cfg.formChildPermissions) && cfg.formChildPermissions.length)
    || (Array.isArray(cfg.formArrayPermissions) && cfg.formArrayPermissions.length)) {
    const existing = normalizeFlowFormPermissions(cfg.formFieldPermissions)
    const permissions = serializeFlowFormPermissions(
      existing.fields,
      Array.isArray(cfg.formChildPermissions) && cfg.formChildPermissions.length
        ? cfg.formChildPermissions
        : existing.children,
      Array.isArray(cfg.formArrayPermissions) && cfg.formArrayPermissions.length
        ? cfg.formArrayPermissions
        : existing.arrays,
    )
    if ((Array.isArray(permissions) && permissions.length) || (permissions && Object.keys(permissions).length))
      attrs.push(`flowable:formFieldPermissions="${escapeXmlAttr(JSON.stringify(permissions))}"`)
  }

  // priority / dueDate
  if (typeof cfg.priority === 'number' && cfg.priority !== 50)
    attrs.push(`flowable:priority="${cfg.priority}"`)
  const dueDateDuration = buildDueDateDuration(cfg)
  if (dueDateDuration)
    attrs.push(`flowable:dueDate="${dueDateDuration}"`)

  if (cfg.overdueReminderEnabled) {
    attrs.push('flowable:overdueReminderEnabled="true"')
    attrs.push(`flowable:overdueReminderTemplateCode="${escapeXmlAttr(cfg.overdueReminderTemplateCode || OVERDUE_REMINDER_DEFAULTS.templateCode)}"`)
    const channels = Array.isArray(cfg.overdueReminderChannels) && cfg.overdueReminderChannels.length
      ? cfg.overdueReminderChannels
      : OVERDUE_REMINDER_DEFAULTS.channels
    attrs.push(`flowable:overdueReminderChannels="${escapeXmlAttr(channels.join(','))}"`)
    attrs.push(`flowable:overdueReminderRepeatMode="${escapeXmlAttr(cfg.overdueReminderRepeatMode || OVERDUE_REMINDER_DEFAULTS.repeatMode)}"`)
    attrs.push(`flowable:overdueReminderIntervalMinutes="${normalizePositiveInt(cfg.overdueReminderIntervalMinutes, OVERDUE_REMINDER_DEFAULTS.intervalMinutes)}"`)
    attrs.push(`flowable:overdueReminderMaxTimes="${normalizePositiveInt(cfg.overdueReminderMaxTimes, OVERDUE_REMINDER_DEFAULTS.maxTimes)}"`)
  }

  // 7 个权限布尔（仅写出与默认值不同的）
  for (const key of PERMISSION_KEYS) {
    const v = cfg[key]
    if (typeof v !== 'boolean')
      continue
    if (v === PERMISSION_DEFAULTS[key])
      continue
    attrs.push(`flowable:${key}="${v}"`)
  }
  if (cfg.initiatorModify)
    attrs.push('flowable:initiatorModify="true"')

  writeApprovalDuty(cfg, attrs)

  const multiInstanceType = cfg.multiInstanceType && cfg.multiInstanceType !== 'none'
    ? cfg.multiInstanceType
    : 'none'
  if (multiInstanceType !== 'none')
    attrs.push(`flowable:multiInstanceType="${escapeXmlAttr(multiInstanceType)}"`)

  // multiInstance：不会签不写 loop，避免回读后又变成会签。
  const initiatorSelect = cfg.assignee === 'initiatorSelect'
  if (multiInstanceType !== 'none') {
    const seq = multiInstanceType === 'sequential' ? 'true' : 'false'
    const expr = buildCompletionExpression(cfg.completionCondition, cfg.passRate)
    const nodeKey = normalizeOptionalText(cfg.initiatorSelectNodeKey || cfg.bpmnElementId || cfg.nodeId)
    const collection = initiatorSelect
      ? `${DOLLAR}{PROCESS_START_USER['${nodeKey}']}`
      : normalizeOptionalText(cfg.multiInstanceCollection)
    const elementVariable = normalizeOptionalText(cfg.multiInstanceElementVariable) || 'assignee'
    const loopAttrs = [`isSequential="${seq}"`]
    let loopCardinalityXml = ''
    if (collection) {
      loopAttrs.push(`flowable:collection="${escapeXmlAttr(collection)}"`)
      loopAttrs.push(`flowable:elementVariable="${escapeXmlAttr(elementVariable)}"`)
    }
    else {
      const loopCardinality = normalizeOptionalText(cfg.multiInstanceLoopCardinality) || `${DOLLAR}{nrOfInstances}`
      loopCardinalityXml = `<bpmn:loopCardinality xsi:type="bpmn:tFormalExpression">${escapeXmlText(loopCardinality)}</bpmn:loopCardinality>`
    }
    children.push(
      `<bpmn:multiInstanceLoopCharacteristics ${loopAttrs.join(' ')}>${
        loopCardinalityXml
      }<bpmn:completionCondition xsi:type="bpmn:tFormalExpression">${escapeXmlText(expr)}</bpmn:completionCondition>`
      + `</bpmn:multiInstanceLoopCharacteristics>`,
    )
  }

  // listeners → 单一 extensionElements 容器
  const listenerXml = []
  for (const l of cfg.taskListeners || [])
    listenerXml.push(buildListener('flowable:taskListener', l))
  for (const l of cfg.executionListeners || [])
    listenerXml.push(buildListener('flowable:executionListener', l))
  if (listenerXml.length) {
    children.push(`<bpmn:extensionElements>${listenerXml.join('')}</bpmn:extensionElements>`)
  }

  return {
    attrs: attrs.join(' '),
    children: children.join(''),
  }
}

function normalizeFixedAssigneeUserId(value) {
  const text = String(value || '').trim()
  return /^\d+$/.test(text) ? text : ''
}

function extractLegacyFixedAssigneeUserId(value) {
  const match = String(value || '').match(/^\$\{user_(\d+)\}$/)
  return match?.[1] || ''
}

function buildDueDateDuration(cfg) {
  const days = normalizeNonNegativeInt(cfg.dueDateDays, null)
  const hours = normalizeNonNegativeInt(cfg.dueDateHours, null)
  const legacyDays = normalizeNonNegativeInt(cfg.dueDate, 0)
  const hasDayHourValue = (days || 0) > 0 || (hours || 0) > 0
  const finalDays = hasDayHourValue ? (days || 0) : legacyDays
  const finalHours = hours != null ? hours : 0

  if (finalDays <= 0 && finalHours <= 0)
    return ''
  if (finalDays > 0 && finalHours > 0)
    return `P${finalDays}DT${finalHours}H`
  if (finalDays > 0)
    return `P${finalDays}D`
  return `PT${finalHours}H`
}

function normalizeNonNegativeInt(value, fallback) {
  if (value == null || value === '')
    return fallback
  const n = Number.parseInt(value, 10)
  return Number.isFinite(n) && n >= 0 ? n : fallback
}

function stringifyFormRef(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    return ''
  const cleaned = Object.entries(value).reduce((acc, [key, item]) => {
    if (item !== undefined && item !== null && item !== '')
      acc[key] = item
    return acc
  }, {})
  return Object.keys(cleaned).length ? JSON.stringify(cleaned) : ''
}

function normalizePositiveInt(value, fallback) {
  const n = Number.parseInt(value, 10)
  return Number.isFinite(n) && n > 0 ? n : fallback
}

function normalizeOptionalText(value) {
  const text = String(value == null ? '' : value).trim()
  return text || ''
}

function normalizePrintTemplateIds(value) {
  const source = Array.isArray(value) ? value : String(value || '').split(',')
  return [...new Set(source.map(item => String(item).trim()).filter(item => /^[1-9]\d*$/.test(item)))]
}

function sanitizeVariableName(value) {
  const text = normalizeOptionalText(value).replace(/\W/g, '_')
  return text || 'node'
}

function writeApprovalDuty(cfg, attrs) {
  const description = normalizeOptionalText(cfg.responsibilityDescription)
  if (description)
    attrs.push(`flowable:responsibilityDescription="${escapeXmlAttr(description)}"`)
  const points = normalizeApprovalPoints(cfg.approvalPoints)
  if (points.length) {
    attrs.push(`flowable:approvalPoints="${escapeXmlAttr(JSON.stringify(points))}"`)
    attrs.push(`flowable:responsibility="${escapeXmlAttr(points.map(item => item.content).join('\n'))}"`)
  }
}

function normalizeApprovalPoints(source = []) {
  return (Array.isArray(source) ? source : [])
    .map((item, index) => {
      const content = String(item?.content || '').trim()
      if (!content)
        return null
      return {
        id: String(item?.id || `point-${index + 1}`),
        content,
        required: item?.required === true,
        sort: Number.isFinite(Number(item?.sort)) ? Number(item.sort) : index + 1,
      }
    })
    .filter(Boolean)
}

function buildListener(tag, l) {
  const event = escapeXmlAttr(l.event || '')
  const value = escapeXmlAttr(l.value || '')
  const type = l.type || 'class'
  return `<${tag} event="${event}" ${type}="${value}"/>`
}
