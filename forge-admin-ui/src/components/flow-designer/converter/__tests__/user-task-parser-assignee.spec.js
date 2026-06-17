import { describe, expect, it } from 'vitest'
import { parseUserTaskConfig } from '../user-task-parser.js'
import { findElementsByLocalName, parseBpmnXml } from '../xml-utils.js'

const DOLLAR = '$'

function getTask(xml, id) {
  const doc = parseBpmnXml(xml)
  return findElementsByLocalName(doc, 'userTask').find(t => t.getAttribute('id') === id)
}

const SAMPLE_ASSIGNEE = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="P">',
  `    <bpmn:userTask id="T_custom" flowable:assignee="${DOLLAR}{user_1001}" flowable:assigneeName="张三"/>`,
  `    <bpmn:userTask id="T_initiator" flowable:assignee="${DOLLAR}{initiator}"/>`,
  `    <bpmn:userTask id="T_leader" flowable:assignee="${DOLLAR}{initiatorLeader}"/>`,
  `    <bpmn:userTask id="T_dept" flowable:assignee="${DOLLAR}{deptManager}"/>`,
  `    <bpmn:userTask id="T_hr" flowable:assignee="${DOLLAR}{hr}"/>`,
  `    <bpmn:userTask id="T_spel_typed" flowable:assignee="${DOLLAR}{customExpr.list}" flowable:assigneeType="spel" flowable:spelTemplate="DEPT_LEADER"/>`,
  `    <bpmn:userTask id="T_spel_inferred" flowable:assignee="${DOLLAR}{some.method(arg)}"/>`,
  `    <bpmn:userTask id="T_spel_empty" flowable:assigneeType="spel"/>`,
  '    <bpmn:userTask id="T_cu" flowable:candidateUsers="1001,1002 ,1003" flowable:candidateUserNames="张三,李四,王五"/>',
  '    <bpmn:userTask id="T_cg" flowable:candidateGroups="role_admin,role_audit" flowable:candidateGroupNames="管理员,审核员"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

describe('parseUserTaskConfig - assignee 4 种模式', () => {
  it('custom：${user_xxx} → assignee=custom + assigneeExpr 保留 + assigneeUserName 回填', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_ASSIGNEE, 'T_custom'))
    expect(cfg.taskType).toBe('assignee')
    expect(cfg.assignee).toBe('custom')
    expect(cfg.assigneeExpr).toBe(`${DOLLAR}{user_1001}`)
    expect(cfg.assigneeUserName).toBe('张三')
  })

  it('static：4 种预定义变量直接保留原值', () => {
    expect(parseUserTaskConfig(getTask(SAMPLE_ASSIGNEE, 'T_initiator')).assignee).toBe(`${DOLLAR}{initiator}`)
    expect(parseUserTaskConfig(getTask(SAMPLE_ASSIGNEE, 'T_leader')).assignee).toBe(`${DOLLAR}{initiatorLeader}`)
    expect(parseUserTaskConfig(getTask(SAMPLE_ASSIGNEE, 'T_dept')).assignee).toBe(`${DOLLAR}{deptManager}`)
    expect(parseUserTaskConfig(getTask(SAMPLE_ASSIGNEE, 'T_hr')).assignee).toBe(`${DOLLAR}{hr}`)
  })

  it('spel：assigneeType=spel 优先识别，保留 spelTemplate', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_ASSIGNEE, 'T_spel_typed'))
    expect(cfg.assignee).toBe('spel')
    expect(cfg.assigneeExpr).toBe(`${DOLLAR}{customExpr.list}`)
    expect(cfg.spelTemplate).toBe('DEPT_LEADER')
  })

  it('spel 推断：${...} 表达式但无 assigneeType 标记 → 兜底为 spel', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_ASSIGNEE, 'T_spel_inferred'))
    expect(cfg.assignee).toBe('spel')
    expect(cfg.assigneeExpr).toBe(`${DOLLAR}{some.method(arg)}`)
  })

  it('spel 空 assignee：仅有 assigneeType 也保留 SPEL 模式', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_ASSIGNEE, 'T_spel_empty'))
    expect(cfg.assignee).toBe('spel')
    expect(cfg.assigneeExpr).toBe('')
  })

  it('candidateUsers：逗号分隔 + 空白 trim + 名称数组', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_ASSIGNEE, 'T_cu'))
    expect(cfg.taskType).toBe('candidateUsers')
    expect(cfg.candidateUsers).toEqual(['1001', '1002', '1003'])
    expect(cfg.candidateUserNames).toEqual(['张三', '李四', '王五'])
  })

  it('candidateGroups：逗号分隔 + 名称数组', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_ASSIGNEE, 'T_cg'))
    expect(cfg.taskType).toBe('candidateGroups')
    expect(cfg.candidateGroups).toEqual(['role_admin', 'role_audit'])
    expect(cfg.candidateGroupNames).toEqual(['管理员', '审核员'])
  })
})
