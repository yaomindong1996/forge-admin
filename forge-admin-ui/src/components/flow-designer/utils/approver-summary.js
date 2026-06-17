/**
 * 审批人卡片摘要文案
 *
 * 输入：UserTask config（user-task-parser 的输出）
 * 输出：用于审批节点卡片正文的摘要字符串
 *
 * 规则：
 *   1) taskType=assignee
 *      - 静态变量：'发起人' / '上级领导' / '部门主管' / 'HR'
 *      - custom：'指定人员：张三'（assigneeUserName 优先）/ '指定表达式：${user_1001}'
 *      - spel：'SPEL 模板：DEPT_LEADER' / 'SPEL 表达式：${...}'
 *      - 简单变量 ${var}：'变量：${var}'
 *      - 兜底：原值
 *   2) taskType=candidateUsers
 *      - '候选人 (3)：张三、李四、王五'（最多展示前 3 个名称）
 *   3) taskType=candidateGroups
 *      - '候选角色 (2)：管理员、审核员'
 *   4) 多实例追加：'· 会签（全部通过）' / '· 会签（任一通过）' / '· 会签（70% 通过）'
 *   5) 未配置审批人：'点击配置审批人'
 */

const STATIC_LABELS = {
  '${initiator}': '发起人',
  '${initiatorLeader}': '上级领导',
  '${deptManager}': '部门主管',
  '${hr}': 'HR',
}

export function buildApproverSummary(config) {
  const c = config || {}
  const main = buildAssigneeText(c)
  const mi = buildMultiInstanceText(c)
  return mi ? `${main} · ${mi}` : main
}

function buildAssigneeText(c) {
  switch (c.taskType) {
    case 'candidateUsers':
      return formatList('候选人', c.candidateUserNames || c.candidateUsers || [])
    case 'candidateGroups':
      return formatList('候选角色', c.candidateGroupNames || c.candidateGroups || [])
    case 'assignee':
    default: {
      // 静态变量
      if (STATIC_LABELS[c.assignee])
        return STATIC_LABELS[c.assignee]
      // custom：${user_xxx}
      if (c.assignee === 'custom') {
        if (c.assigneeUserName)
          return `指定人员：${c.assigneeUserName}`
        if (c.assigneeExpr)
          return `指定表达式：${c.assigneeExpr}`
        return '点击配置审批人'
      }
      // spel
      if (c.assignee === 'spel') {
        if (c.spelTemplate)
          return `SPEL 模板：${c.spelTemplate}`
        if (c.assigneeExpr)
          return `SPEL 表达式：${c.assigneeExpr}`
        return '点击配置 SPEL 表达式'
      }
      // 简单变量 ${var}
      if (typeof c.assignee === 'string' && /^\$\{[a-z_$][\w$]*\}$/i.test(c.assignee))
        return `变量：${c.assignee}`
      // 兜底
      if (c.assignee)
        return String(c.assignee)
      return '点击配置审批人'
    }
  }
}

function buildMultiInstanceText(c) {
  if (!c.multiInstanceType || c.multiInstanceType === 'none')
    return ''
  const seq = c.multiInstanceType === 'sequential' ? '顺序' : ''
  switch (c.completionCondition) {
    case 'any':
      return `${seq ? `${seq}` : ''}会签（任一通过）`
    case 'ratio': {
      const n = Number(c.passRate)
      if (Number.isFinite(n) && n > 0 && n < 100)
        return `${seq ? `${seq}` : ''}会签（${n}% 通过）`
      return `${seq ? `${seq}` : ''}会签（全部通过）`
    }
    case 'all':
    default:
      return `${seq ? `${seq}` : ''}会签（全部通过）`
  }
}

function formatList(label, list) {
  const arr = Array.isArray(list) ? list.filter(Boolean) : []
  if (arr.length === 0)
    return `点击配置${label}`
  const head = arr.slice(0, 3).join('、')
  const more = arr.length > 3 ? `… 共 ${arr.length} 人` : ''
  return `${label} (${arr.length})：${head}${more ? ` ${more}` : ''}`
}
