import { describe, expect, it } from 'vitest'
import { parseUserTaskConfig } from '../user-task-parser.js'
import { writeUserTaskConfig } from '../user-task-writer.js'
import { findElementsByLocalName, parseBpmnXml } from '../xml-utils.js'

const DOLLAR = '$'

function roundTrip(config) {
  const { attrs, children } = writeUserTaskConfig(config)
  const xml = '<?xml version="1.0" encoding="UTF-8"?>'
    + '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">'
    + `<bpmn:process id="P"><bpmn:userTask id="T1" ${attrs}>${children}</bpmn:userTask></bpmn:process></bpmn:definitions>`
  const task = findElementsByLocalName(parseBpmnXml(xml), 'userTask')[0]
  return { attrs, children, parsed: parseUserTaskConfig(task) }
}

const countersign = {
  taskType: 'candidateUsers',
  candidateUsers: ['1', '70'],
  candidateUserNames: ['超级管理员', 'test33'],
  multiInstanceType: 'parallel',
  completionCondition: 'all',
  passRate: 100,
}

describe('candidate users countersign', () => {
  it('expands candidates into one assigned task each instead of an unresolvable loop cardinality', () => {
    const { attrs, children } = roundTrip(countersign)
    expect(children).not.toContain('nrOfInstances}</bpmn:loopCardinality>')
    expect(children).toContain(`flowable:collection="${DOLLAR}{flowSpelService.toUserList('1,70')}"`)
    expect(children).toContain('flowable:elementVariable="assignee"')
    expect(attrs).toContain(`flowable:assignee="${DOLLAR}{assignee}"`)
    expect(attrs).not.toContain('flowable:candidateUsers=')
  })

  it('reads countersign candidates back for the designer', () => {
    const { parsed } = roundTrip(countersign)
    expect(parsed).toMatchObject({
      taskType: 'candidateUsers',
      candidateUsers: ['1', '70'],
      candidateUserNames: ['超级管理员', 'test33'],
      multiInstanceType: 'parallel',
    })
    expect(parsed.multiInstanceCollection || '').toBe('')
  })

  it('keeps plain candidate users without countersign as candidates', () => {
    const { attrs, children } = roundTrip({ ...countersign, multiInstanceType: 'none' })
    expect(attrs).toContain('flowable:candidateUsers="1,70"')
    expect(children).not.toContain('multiInstanceLoopCharacteristics')
  })
})
