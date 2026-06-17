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
 * - 操作权限（7 个布尔字段，默认值与现有面板一致）
 * - 多实例（会签）：parallel / sequential，completionCondition：all / any / ratio + passRate
 * - 任务监听器 + 执行监听器（每个含 event + type + value）
 */

import {
  getAttr,
  getChild,
  getExtensionElements,
  getFlowableAttr,
  getTextContent,
} from './xml-utils.js'

const STATIC_ASSIGNEES = new Set([
  '${initiator}',
  '${initiatorLeader}',
  '${deptManager}',
  '${hr}',
])

const DEFAULT_PERMISSIONS = Object.freeze({
  allowApprove: true,
  allowReject: true,
  allowDelegate: true,
  allowReturn: false,
  allowTerminate: false,
  requireSignature: false,
  requireComment: true,
})

export function parseUserTaskConfig(taskElement) {
  const config = {
    taskType: 'assignee',
    assignee: '',
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
    formKey: '',
    formJson: '',
    formUrl: '',
    multiInstanceType: 'none',
    completionCondition: 'all',
    passRate: 100,
    taskListeners: [],
    executionListeners: [],
    formFieldPermissions: [],
    ...DEFAULT_PERMISSIONS,
  }
  if (!taskElement)
    return config

  applyAssignee(taskElement, config)
  applyForm(taskElement, config)
  applyPriorityAndDueDate(taskElement, config)
  applyPermissions(taskElement, config)
  applyMultiInstance(taskElement, config)
  applyListeners(taskElement, config)
  return config
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

    if (assigneeType === 'spel') {
      config.assignee = 'spel'
      config.assigneeExpr = assignee
    }
    else if (assignee.startsWith('${user_')) {
      config.assignee = 'custom'
      config.assigneeExpr = assignee
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

function splitCsv(str) {
  if (!str)
    return []
  return String(str).split(',').map(s => s.trim()).filter(Boolean)
}

function applyForm(el, config) {
  config.formKey = getFlowableAttr(el, 'formKey') || ''
  config.formJson = getFlowableAttr(el, 'formJson') || ''
  config.formUrl = getFlowableAttr(el, 'formUrl') || ''
  if (config.formUrl)
    config.formType = 'external'
  else if (config.formKey || config.formJson)
    config.formType = 'dynamic'
  else
    config.formType = 'none'
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
    const match = String(dueDate).match(/(\d+)/)
    if (match)
      config.dueDate = Number.parseInt(match[1], 10) || 0
  }
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
}

function applyMultiInstance(el, config) {
  const loop = getChild(el, 'multiInstanceLoopCharacteristics')
  if (!loop) {
    config.multiInstanceType = 'none'
    config.completionCondition = 'all'
    config.passRate = 100
    return
  }

  config.multiInstanceType = getAttr(loop, 'isSequential') === 'true' ? 'sequential' : 'parallel'

  const ccEl = getChild(loop, 'completionCondition')
  const expr = ccEl ? getTextContent(ccEl) : ''
  const parsed = parseCompletionExpression(expr)
  config.completionCondition = parsed.condition
  config.passRate = parsed.passRate

  const collection = getAttr(loop, 'collection') || getFlowableAttr(loop, 'collection')
  if (collection)
    config.multiInstanceCollection = collection
  const elementVariable = getAttr(loop, 'elementVariable')
  if (elementVariable)
    config.multiInstanceElementVariable = elementVariable
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
