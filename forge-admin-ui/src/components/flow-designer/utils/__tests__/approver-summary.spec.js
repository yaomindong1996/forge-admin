import { describe, expect, it } from 'vitest'
import { buildApproverSummary } from '../approver-summary.js'

const DOLLAR = '$'

describe('buildApproverSummary - taskType=assignee', () => {
  it('静态变量映射为中文标签', () => {
    expect(buildApproverSummary({ taskType: 'assignee', assignee: `${DOLLAR}{initiator}` })).toBe('发起人')
    expect(buildApproverSummary({ taskType: 'assignee', assignee: `${DOLLAR}{initiatorLeader}` })).toBe('上级领导')
    expect(buildApproverSummary({ taskType: 'assignee', assignee: `${DOLLAR}{deptManager}` })).toBe('部门主管')
    expect(buildApproverSummary({ taskType: 'assignee', assignee: `${DOLLAR}{hr}` })).toBe('HR')
  })

  it('custom：assigneeUserName 优先', () => {
    const s = buildApproverSummary({ taskType: 'assignee', assignee: 'custom', assigneeUserName: '张三' })
    expect(s).toBe('指定人员：张三')
  })

  it('custom：无名称时显示表达式', () => {
    const s = buildApproverSummary({ taskType: 'assignee', assignee: 'custom', assigneeExpr: `${DOLLAR}{user_1001}` })
    expect(s).toBe(`指定表达式：${DOLLAR}{user_1001}`)
  })

  it('spel：spelTemplate 优先', () => {
    const s = buildApproverSummary({ taskType: 'assignee', assignee: 'spel', spelTemplate: 'DEPT_LEADER' })
    expect(s).toBe('SPEL 模板：DEPT_LEADER')
  })

  it('spel：仅表达式', () => {
    const s = buildApproverSummary({ taskType: 'assignee', assignee: 'spel', assigneeExpr: `${DOLLAR}{x.y()}` })
    expect(s).toBe(`SPEL 表达式：${DOLLAR}{x.y()}`)
  })

  it('简单变量 ${var}', () => {
    const s = buildApproverSummary({ taskType: 'assignee', assignee: `${DOLLAR}{owner}` })
    expect(s).toBe(`变量：${DOLLAR}{owner}`)
  })

  it('空配置兜底', () => {
    expect(buildApproverSummary({})).toBe('点击配置审批人')
    expect(buildApproverSummary({ taskType: 'assignee' })).toBe('点击配置审批人')
  })
})

describe('buildApproverSummary - candidateUsers / candidateGroups', () => {
  it('候选人列表（≤3 全显）', () => {
    const s = buildApproverSummary({
      taskType: 'candidateUsers',
      candidateUsers: ['1', '2', '3'],
      candidateUserNames: ['张三', '李四', '王五'],
    })
    expect(s).toBe('候选人 (3)：张三、李四、王五')
  })

  it('候选人 > 3 显示 "等 共 N 人"', () => {
    const s = buildApproverSummary({
      taskType: 'candidateUsers',
      candidateUserNames: ['A', 'B', 'C', 'D', 'E'],
    })
    expect(s).toContain('候选人 (5)：A、B、C')
    expect(s).toContain('共 5 人')
  })

  it('候选角色 + 名称', () => {
    const s = buildApproverSummary({
      taskType: 'candidateGroups',
      candidateGroupNames: ['管理员', '审核员'],
    })
    expect(s).toBe('候选角色 (2)：管理员、审核员')
  })

  it('空候选人兜底', () => {
    expect(buildApproverSummary({ taskType: 'candidateUsers' })).toBe('点击配置候选人')
  })
})

describe('buildApproverSummary - multiInstance 追加', () => {
  it('all → 会签（全部通过）', () => {
    const s = buildApproverSummary({
      taskType: 'assignee',
      assignee: `${DOLLAR}{deptManager}`,
      multiInstanceType: 'parallel',
      completionCondition: 'all',
    })
    expect(s).toContain('会签（全部通过）')
  })

  it('any → 会签（任一通过）', () => {
    const s = buildApproverSummary({
      taskType: 'assignee',
      assignee: `${DOLLAR}{deptManager}`,
      multiInstanceType: 'parallel',
      completionCondition: 'any',
    })
    expect(s).toContain('任一通过')
  })

  it('ratio + passRate', () => {
    const s = buildApproverSummary({
      taskType: 'assignee',
      assignee: `${DOLLAR}{deptManager}`,
      multiInstanceType: 'parallel',
      completionCondition: 'ratio',
      passRate: 70,
    })
    expect(s).toContain('70% 通过')
  })

  it('sequential 标 "顺序"', () => {
    const s = buildApproverSummary({
      taskType: 'assignee',
      assignee: `${DOLLAR}{deptManager}`,
      multiInstanceType: 'sequential',
      completionCondition: 'all',
    })
    expect(s).toContain('顺序')
  })

  it('multiInstanceType=none 不追加', () => {
    const s = buildApproverSummary({
      taskType: 'assignee',
      assignee: `${DOLLAR}{deptManager}`,
      multiInstanceType: 'none',
    })
    expect(s).toBe('部门主管')
  })
})
