/**
 * UserTask 完整属性提取（Task 3）
 *
 * 1:1 复刻 NodePropertiesPanel.vue:1562-1730 的语义，但用 DOM 而非 bpmn-js moddle。
 *
 * 提取范围：
 * - taskType: assignee / candidateUsers / candidateGroups
 * - assignee 模式: custom（${user_xxx}） / spel / 静态变量（initiator/initiatorLeader/deptManager/hr）
 * - candidateUsers / candidateGroups 数组 + 名称列表
 * - 优先级 / 截止时间
 * - 表单（formKey / formJson / formUrl + 表单类型）
 * - 操作权限（8 个布尔字段，默认值与现有面板一致）
 * - 多实例（会签）：parallel / sequential，completionCondition：all / any / ratio + passRate
 * - 任务监听器 + 执行监听器（每个含 event + type + value）
 */

import { normalizeFlowFormPermissions } from '@/utils/flow-field-permissions'
import {
  getAttr,
  getChild,
  getExtensionElements,
  getFlowableAttr,
  getTextContent,
} from './xml-utils.js'

const DOLLAR = '$'
const STATIC_ASSIGNEES = new Set([
  `${DOLLAR}{initiator}`,
  `${DOLLAR}{initiatorLeader}`,
  `${DOLLAR}{deptManager}`,
  `${DOLLAR}{hr}`,
])

const DEFAULT_PERMISSIONS = Object.freeze({
  allowApprove: true,
  allowReject: true,
  allowRejectToStart: false,
  allowDelegate: true,
  allowReturn: false,
  allowTerminate: false,
  requireSignature: false,
  requireComment: true,
})

const DEFAULT_OVERDUE_REMINDER = Object.freeze({
  dueDateDays: 0,
  dueDateHours: 0,
  overdueReminderEnabled: false,
  overdueReminderTemplateCode: 'FLOW_TASK_OVERDUE',
  overdueReminderChannels: ['WEB'],
  overdueReminderRepeatMode: 'once',
  overdueReminderIntervalMinutes: 1440,
  overdueReminderMaxTimes: 1,
})

export function parseUserTaskConfig(taskElement) {
  const config = {
    taskType: 'assignee',
    assignee: '',
    assigneeUserId: '',
    assigneeExpr: '',
    assigneeUserName: '',
    candidateUsers: [],
    candidateUserNames: [],
    candidateGroups: [],
    candidateGroupNames: [],
    spelTemplate: '',
    priority: 50,
    dueDate: 0,
    formType: 'none',
    formMode: '',
    formKey: '',
    formName: '',
    providerKey: '',
    formJson: '',
    formUrl: '',
    viewKey: 'default',
    formRef: {},
    printTemplatePolicy: 'INHERIT',
    printTemplateIds: [],
    multiInstanceType: 'none',
    initiatorSelectNodeKey: '',
    completionCondition: 'all',
    passRate: 100,
    taskListeners: [],
    executionListeners: [],
    formFieldPermissions: [],
    formChildPermissions: [],
    formArrayPermissions: [],
    responsibilityDescription: '',
    approvalPoints: [],
    ...DEFAULT_PERMISSIONS,
    ...DEFAULT_OVERDUE_REMINDER,
  }
  if (!taskElement)
    return config

  applyAssignee(taskElement, config)
  applyForm(taskElement, config)
  applyFormFieldPermissions(taskElement, config)
  applyPriorityAndDueDate(taskElement, config)
  applyPermissions(taskElement, config)
  applyApprovalDuty(taskElement, config)
  applyMultiInstance(taskElement, config)
  applyListeners(taskElement, config)
  return config
}

function applyFormFieldPermissions(el, config) {
  const raw = getFlowableAttr(el, 'formFieldPermissions') || readExtensionText(el, 'formFieldPermissions')
  if (!raw)
    return
  try {
    const normalized = normalizeFlowFormPermissions(JSON.parse(raw))
    config.formFieldPermissions = normalized.fields
      .map(normalizeFormFieldPermission)
      .filter(item => item.field)
    config.formChildPermissions = normalized.children
    config.formArrayPermissions = normalized.arrays
  }
  catch {
    config.formFieldPermissions = []
    config.formChildPermissions = []
    config.formArrayPermissions = []
  }
}

function readExtensionText(el, localName) {
  const item = getExtensionElements(el, localName)[0]
  return item ? getTextContent(item) : ''
}

function normalizeFormFieldPermission(item = {}) {
  const field = String(item.field || item.fieldCode || item.code || '').trim()
  const readable = readBoolean(item.readable, readBoolean(item.visible, true))
  const writable = readable && readBoolean(item.writable, readBoolean(item.editable, true))
  const normalized = {
    field,
    fieldCode: field,
    label: String(item.label || field || '').trim(),
    visible: readable,
    editable: writable,
    readable,
    writable,
    required: writable && item.required === true,
  }
  if (String(item.scope || '').toLowerCase() === 'child' || item.childKey) {
    normalized.scope = 'child'
    normalized.childKey = String(item.childKey || item.relationKey || '').trim()
    normalized.childField = String(item.childField || field).trim()
  }
  else if (String(item.scope || '').toLowerCase() === 'array' || item.arrayKey) {
    normalized.scope = 'array'
    normalized.arrayKey = String(item.arrayKey || '').trim()
    normalized.itemField = String(item.itemField || field).trim()
  }
  return normalized
}

function readBoolean(value, defaultValue) {
  if (value === undefined || value === null || value === '')
    return defaultValue
  if (typeof value === 'boolean')
    return value
  if (typeof value === 'number')
    return value !== 0
  const text = String(value).trim().toLowerCase()
  if (['true', '1', 'yes'].includes(text))
    return true
  if (['false', '0', 'no'].includes(text))
    return false
  return defaultValue
}

function applyAssignee(el, config) {
  const assignee = getFlowableAttr(el, 'assignee')
  const candidateUsers = getFlowableAttr(el, 'candidateUsers')
  const candidateGroups = getFlowableAttr(el, 'candidateGroups')
  const assigneeName = getFlowableAttr(el, 'assigneeName') || ''
  const candidateUserNames = getFlowableAttr(el, 'candidateUserNames') || ''
  const candidateGroupNames = getFlowableAttr(el, 'candidateGroupNames') || ''
  const assigneeType = getFlowableAttr(el, 'assigneeType') || ''
  const spelTemplate = getFlowableAttr(el, 'spelTemplate') || ''

  if (assignee) {
    config.taskType = 'assignee'
    config.assigneeUserName = assigneeName

    if (assigneeType === 'initiatorSelect' || isInitiatorSelectAssignee(assignee)) {
      config.assignee = 'initiatorSelect'
    }
    else if (assigneeType === 'spel') {
      config.assignee = 'spel'
      config.assigneeExpr = assignee
    }
    else if (assigneeType === 'custom') {
      config.assignee = 'custom'
      config.assigneeUserId = extractFixedAssigneeUserId(assignee)
    }
    else if (extractLegacyFixedAssigneeUserId(assignee)) {
      config.assignee = 'custom'
      config.assigneeUserId = extractLegacyFixedAssigneeUserId(assignee)
    }
    else if (STATIC_ASSIGNEES.has(assignee)) {
      config.assignee = assignee
    }
    else if (isSimpleVariableExpression(assignee)) {
      config.assignee = assignee
    }
    else if (assignee.startsWith('${') && assignee.endsWith('}')) {
      config.assignee = 'spel'
      config.assigneeExpr = assignee
    }
    else {
      config.assignee = assignee
    }
  }
  else if (assigneeType === 'initiatorSelect') {
    config.taskType = 'assignee'
    config.assignee = 'initiatorSelect'
  }
  else if (assigneeType === 'spel') {
    config.taskType = 'assignee'
    config.assignee = 'spel'
  }
  else if (candidateUsers) {
    config.taskType = 'candidateUsers'
    config.candidateUsers = splitCsv(candidateUsers)
    config.candidateUserNames = splitCsv(candidateUserNames)
  }
  else if (candidateGroups) {
    config.taskType = 'candidateGroups'
    config.candidateGroups = splitCsv(candidateGroups)
    config.candidateGroupNames = splitCsv(candidateGroupNames)
  }

  if (assigneeType === 'spel')
    config.spelTemplate = spelTemplate
}

function isSimpleVariableExpression(s) {
  return /^\$\{[a-z_$][\w$]*\}$/i.test(s)
}

function extractFixedAssigneeUserId(value) {
  return extractLegacyFixedAssigneeUserId(value) || (/^\d+$/.test(String(value || '')) ? String(value) : '')
}

function extractLegacyFixedAssigneeUserId(value) {
  const match = String(value || '').match(/^\$\{user_(\d+)\}$/)
  return match?.[1] || ''
}

function splitCsv(str) {
  if (!str)
    return []
  return String(str).split(',').map(s => s.trim()).filter(Boolean)
}

function applyForm(el, config) {
  config.formMode = getFlowableAttr(el, 'formMode') || ''
  config.formKey = getFlowableAttr(el, 'formKey') || ''
  config.formName = getFlowableAttr(el, 'formName') || ''
  config.providerKey = getFlowableAttr(el, 'providerKey') || ''
  config.formJson = getFlowableAttr(el, 'formJson') || ''
  config.formUrl = getFlowableAttr(el, 'formUrl') || ''
  config.viewKey = getFlowableAttr(el, 'viewKey') || 'default'
  config.formRef = parseJsonObject(getFlowableAttr(el, 'formRef'))
  config.printTemplatePolicy = String(getFlowableAttr(el, 'printTemplatePolicy') || 'INHERIT').trim().toUpperCase() === 'RESTRICT'
    ? 'RESTRICT'
    : 'INHERIT'
  config.printTemplateIds = splitCsv(getFlowableAttr(el, 'printTemplateIds')).filter(value => /^[1-9]\d*$/.test(value))
  if (config.formUrl)
    config.formType = 'external'
  else if (config.formKey || config.formJson)
    config.formType = 'dynamic'
  else
    config.formType = 'none'
}

function parseJsonObject(value) {
  if (!value)
    return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}

function applyPriorityAndDueDate(el, config) {
  const priority = getFlowableAttr(el, 'priority') ?? getAttr(el, 'priority')
  if (priority != null) {
    const n = Number.parseInt(priority, 10)
    if (Number.isFinite(n))
      config.priority = n
  }

  const dueDate = getFlowableAttr(el, 'dueDate') ?? getAttr(el, 'dueDate')
  if (dueDate != null) {
    const parsed = parseDueDateDuration(dueDate)
    config.dueDate = parsed.days
    config.dueDateDays = parsed.days
    config.dueDateHours = parsed.hours
  }

  applyOverdueReminder(el, config)
}

function applyOverdueReminder(el, config) {
  const enabled = parseBoolean(getFlowableAttr(el, 'overdueReminderEnabled'))
  if (enabled != null)
    config.overdueReminderEnabled = enabled

  const templateCode = getFlowableAttr(el, 'overdueReminderTemplateCode')
  if (templateCode)
    config.overdueReminderTemplateCode = templateCode

  const channels = getFlowableAttr(el, 'overdueReminderChannels')
  if (channels)
    config.overdueReminderChannels = splitCsv(channels)

  const repeatMode = getFlowableAttr(el, 'overdueReminderRepeatMode')
  if (repeatMode)
    config.overdueReminderRepeatMode = repeatMode

  const intervalMinutes = parseInteger(getFlowableAttr(el, 'overdueReminderIntervalMinutes'))
  if (intervalMinutes != null)
    config.overdueReminderIntervalMinutes = intervalMinutes

  const maxTimes = parseInteger(getFlowableAttr(el, 'overdueReminderMaxTimes'))
  if (maxTimes != null)
    config.overdueReminderMaxTimes = maxTimes
}

function parseDueDateDuration(value) {
  const text = String(value || '').trim()
  const match = text.match(/^P(?:(\d+)D)?(?:T(?:(\d+)H)?)?$/i)
  if (match) {
    return {
      days: Number.parseInt(match[1] || '0', 10) || 0,
      hours: Number.parseInt(match[2] || '0', 10) || 0,
    }
  }
  const legacy = text.match(/(\d+)/)
  return {
    days: legacy ? Number.parseInt(legacy[1], 10) || 0 : 0,
    hours: 0,
  }
}

function parseBoolean(value) {
  if (value == null)
    return null
  const normalized = String(value).trim().toLowerCase()
  if (['true', '1', 'y', 'yes'].includes(normalized))
    return true
  if (['false', '0', 'n', 'no'].includes(normalized))
    return false
  return null
}

function parseInteger(value) {
  if (value == null || value === '')
    return null
  const n = Number.parseInt(value, 10)
  return Number.isFinite(n) ? n : null
}

function applyPermissions(el, config) {
  for (const key of Object.keys(DEFAULT_PERMISSIONS)) {
    const raw = getFlowableAttr(el, key)
    if (raw == null) {
      config[key] = DEFAULT_PERMISSIONS[key]
    }
    else {
      const v = String(raw).trim().toLowerCase()
      config[key] = ['true', '1', 'y', 'yes'].includes(v)
    }
  }
  const initiatorModify = getFlowableAttr(el, 'initiatorModify')
  if (initiatorModify != null) {
    const v = String(initiatorModify).trim().toLowerCase()
    config.initiatorModify = ['true', '1', 'y', 'yes'].includes(v)
  }
}

function applyApprovalDuty(el, config) {
  config.responsibilityDescription = getFlowableAttr(el, 'responsibilityDescription') || ''
  const raw = getFlowableAttr(el, 'approvalPoints') || readExtensionText(el, 'approvalPoints')
  if (raw) {
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) {
        config.approvalPoints = parsed
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
    }
    catch {
      config.approvalPoints = []
    }
  }
  if (!config.approvalPoints.length) {
    const legacy = getFlowableAttr(el, 'responsibility') || ''
    if (legacy) {
      config.approvalPoints = [{
        id: 'legacy-responsibility',
        content: legacy,
        required: false,
        sort: 1,
      }]
    }
  }
}

function isInitiatorSelectAssignee(assignee) {
  return /^\$\{INITIATOR_SELECT_\w+\}$/.test(String(assignee || ''))
}

function applyMultiInstance(el, config) {
  const explicitType = getFlowableAttr(el, 'multiInstanceType')
  const loop = getChild(el, 'multiInstanceLoopCharacteristics')
  if (!loop) {
    config.multiInstanceType = explicitType === 'parallel' || explicitType === 'sequential'
      ? explicitType
      : 'none'
    config.completionCondition = 'all'
    config.passRate = 100
    return
  }

  config.multiInstanceType = explicitType === 'none'
    ? 'none'
    : (getAttr(loop, 'isSequential') === 'true' ? 'sequential' : 'parallel')

  const ccEl = getChild(loop, 'completionCondition')
  const expr = ccEl ? getTextContent(ccEl) : ''
  const parsed = parseCompletionExpression(expr)
  config.completionCondition = parsed.condition
  config.passRate = parsed.passRate

  const collection = getAttr(loop, 'collection') || getFlowableAttr(loop, 'collection')
  if (collection) {
    config.multiInstanceCollection = collection
    const match = String(collection).match(/^\$\{PROCESS_START_USER\[['"]([^'"\]]+)['"]\]\}$/)
    if (match) {
      config.assignee = 'initiatorSelect'
      config.initiatorSelectNodeKey = match[1]
    }
  }
  const elementVariable = getAttr(loop, 'elementVariable') || getFlowableAttr(loop, 'elementVariable')
  if (elementVariable)
    config.multiInstanceElementVariable = elementVariable
  const loopCardinality = getChild(loop, 'loopCardinality')
  if (loopCardinality)
    config.multiInstanceLoopCardinality = getTextContent(loopCardinality)
}

/**
 * 多实例 completionCondition 表达式 → { condition, passRate }
 *   ${nrOfCompletedInstances/nrOfInstances == 1}      → all (passRate=100)
 *   ${nrOfCompletedInstances == nrOfInstances}        → all (向后兼容)
 *   ${nrOfCompletedInstances >= 1}                    → any (passRate=null)
 *   ${nrOfCompletedInstances/nrOfInstances >= 0.6}    → ratio (passRate=60)
 */
export function parseCompletionExpression(expr) {
  const text = String(expr || '').trim()
  if (!text)
    return { condition: 'all', passRate: 100 }

  if (/nrOfCompletedInstances\s*>=\s*1\b/.test(text) && !text.includes('/'))
    return { condition: 'any', passRate: null }

  if (/==\s*1\b/.test(text) || /==\s*nrOfInstances\b/.test(text))
    return { condition: 'all', passRate: 100 }

  const ratio = text.match(/>=\s*([\d.]+)/)
  if (ratio) {
    const v = Number.parseFloat(ratio[1])
    if (Number.isFinite(v))
      return { condition: 'ratio', passRate: Math.round(v * 100) }
  }

  return { condition: 'all', passRate: 100 }
}

function applyListeners(el, config) {
  config.taskListeners = parseListenerList(el, 'taskListener')
  config.executionListeners = parseListenerList(el, 'executionListener')
}

/**
 * 解析 <flowable:taskListener> / <flowable:executionListener> 列表。
 * 每项 { event, type, value }；type ∈ class / expression / delegateExpression。
 * 同时携带 'class' 属性方便与现有 NodePropertiesPanel 字段名兼容。
 */
function parseListenerList(el, localName) {
  const list = []
  for (const item of getExtensionElements(el, localName)) {
    const event = getAttr(item, 'event') || ''
    const cls = getAttr(item, 'class')
    const expr = getAttr(item, 'expression')
    const delegate = getAttr(item, 'delegateExpression')

    let type = 'class'
    let value = ''
    if (cls != null) {
      type = 'class'
      value = cls
    }
    else if (expr != null) {
      type = 'expression'
      value = expr
    }
    else if (delegate != null) {
      type = 'delegateExpression'
      value = delegate
    }

    list.push({ event, type, value, class: type === 'class' ? value : '' })
  }
  return list
}
