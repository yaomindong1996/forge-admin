/**
 * UserTask 属性回写为 BPMN XML 片段（Task 5）
 *
 * 与 user-task-parser.js 的提取规则对偶，确保 XML→JSON→XML 语义等价。
 *
 * 写出范围：
 * - 属性：flowable:assignee / assigneeType / assigneeName / candidateUsers / candidateGroups
 *   / formKey / formJson / formUrl / priority / dueDate / 7 个权限布尔
 * - 子元素：multiInstanceLoopCharacteristics + completionCondition
 * - extensionElements：taskListener[] + executionListener[]
 *
 * 输出形式：纯 XML 字符串，外层不含 <userTask>，仅含 attribute 串与子元素串。
 * 调用方负责把 attribute 拼到开标签上、子元素拼到 <userTask> ... </userTask> 之间。
 */

import { buildCompletionExpression } from './completion-condition.js'
import { escapeXmlAttr, escapeXmlText } from './xml-escape.js'

const PERMISSION_KEYS = [
  'allowApprove',
  'allowReject',
  'allowDelegate',
  'allowReturn',
  'allowTerminate',
  'requireSignature',
  'requireComment',
]

const PERMISSION_DEFAULTS = {
  allowApprove: true,
  allowReject: true,
  allowDelegate: true,
  allowReturn: false,
  allowTerminate: false,
  requireSignature: false,
  requireComment: true,
}

const STATIC_ASSIGNEES = new Set([
  '${initiator}',
  '${initiatorLeader}',
  '${deptManager}',
  '${hr}',
])

/**
 * 把 UserTask config 写为属性串 + 子元素串。
 *
 * @param {object} config user-task-parser 的输出
 * @returns {{ attrs: string, children: string }}
 */
export function writeUserTaskConfig(config) {
  const cfg = config || {}
  const attrs = []
  const children = []

  // assignee 三种模式
  if (cfg.taskType === 'assignee') {
    let val = cfg.assignee
    let typeMark = null
    if (cfg.assignee === 'spel') {
      val = cfg.assigneeExpr || ''
      typeMark = 'spel'
    }
    else if (cfg.assignee === 'custom') {
      val = cfg.assigneeExpr || ''
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
  if (cfg.formKey)
    attrs.push(`flowable:formKey="${escapeXmlAttr(cfg.formKey)}"`)
  if (cfg.formJson)
    attrs.push(`flowable:formJson="${escapeXmlAttr(cfg.formJson)}"`)
  if (cfg.formUrl)
    attrs.push(`flowable:formUrl="${escapeXmlAttr(cfg.formUrl)}"`)

  // priority / dueDate
  if (typeof cfg.priority === 'number' && cfg.priority !== 50)
    attrs.push(`flowable:priority="${cfg.priority}"`)
  if (typeof cfg.dueDate === 'number' && cfg.dueDate > 0)
    attrs.push(`flowable:dueDate="P${cfg.dueDate}D"`)

  // 7 个权限布尔（仅写出与默认值不同的）
  for (const key of PERMISSION_KEYS) {
    const v = cfg[key]
    if (typeof v !== 'boolean')
      continue
    if (v === PERMISSION_DEFAULTS[key])
      continue
    attrs.push(`flowable:${key}="${v}"`)
  }

  // multiInstance
  if (cfg.multiInstanceType && cfg.multiInstanceType !== 'none') {
    const seq = cfg.multiInstanceType === 'sequential' ? 'true' : 'false'
    const expr = buildCompletionExpression(cfg.completionCondition, cfg.passRate)
    children.push(
      `<bpmn:multiInstanceLoopCharacteristics isSequential="${seq}">`
      + `<bpmn:completionCondition>${escapeXmlText(expr)}</bpmn:completionCondition>`
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

function buildListener(tag, l) {
  const event = escapeXmlAttr(l.event || '')
  const value = escapeXmlAttr(l.value || '')
  const type = l.type || 'class'
  return `<${tag} event="${event}" ${type}="${value}"/>`
}
