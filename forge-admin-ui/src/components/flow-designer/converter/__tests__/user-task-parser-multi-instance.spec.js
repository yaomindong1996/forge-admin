import { describe, expect, it } from 'vitest'
import { parseCompletionExpression, parseUserTaskConfig } from '../user-task-parser.js'
import { findElementsByLocalName, parseBpmnXml } from '../xml-utils.js'

const DOLLAR = '$'

function getTask(xml, id) {
  const doc = parseBpmnXml(xml)
  return findElementsByLocalName(doc, 'userTask').find(t => t.getAttribute('id') === id)
}

const SAMPLE_MI = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">',
  '  <bpmn:process id="P">',
  '    <bpmn:userTask id="T_all">',
  '      <bpmn:multiInstanceLoopCharacteristics isSequential="false">',
  `        <bpmn:completionCondition>${DOLLAR}{nrOfCompletedInstances/nrOfInstances == 1}</bpmn:completionCondition>`,
  '      </bpmn:multiInstanceLoopCharacteristics>',
  '    </bpmn:userTask>',
  '    <bpmn:userTask id="T_any">',
  '      <bpmn:multiInstanceLoopCharacteristics isSequential="false">',
  `        <bpmn:completionCondition>${DOLLAR}{nrOfCompletedInstances >= 1}</bpmn:completionCondition>`,
  '      </bpmn:multiInstanceLoopCharacteristics>',
  '    </bpmn:userTask>',
  '    <bpmn:userTask id="T_ratio">',
  '      <bpmn:multiInstanceLoopCharacteristics isSequential="false">',
  `        <bpmn:completionCondition>${DOLLAR}{nrOfCompletedInstances/nrOfInstances >= 0.6}</bpmn:completionCondition>`,
  '      </bpmn:multiInstanceLoopCharacteristics>',
  '    </bpmn:userTask>',
  '    <bpmn:userTask id="T_seq">',
  '      <bpmn:multiInstanceLoopCharacteristics isSequential="true"/>',
  '    </bpmn:userTask>',
  '    <bpmn:userTask id="T_none"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

describe('parseCompletionExpression', () => {
  it('all (==1)', () => {
    expect(parseCompletionExpression(`${DOLLAR}{nrOfCompletedInstances/nrOfInstances == 1}`))
      .toEqual({ condition: 'all', passRate: 100 })
  })
  it('any (>=1)', () => {
    expect(parseCompletionExpression(`${DOLLAR}{nrOfCompletedInstances >= 1}`))
      .toEqual({ condition: 'any', passRate: null })
  })
  it('ratio (>=0.6) → 60', () => {
    expect(parseCompletionExpression(`${DOLLAR}{nrOfCompletedInstances/nrOfInstances >= 0.6}`))
      .toEqual({ condition: 'ratio', passRate: 60 })
  })
  it('legacy == nrOfInstances → all', () => {
    expect(parseCompletionExpression(`${DOLLAR}{nrOfCompletedInstances == nrOfInstances}`))
      .toEqual({ condition: 'all', passRate: 100 })
  })
  it('空 / 无法解析 → all', () => {
    expect(parseCompletionExpression('')).toEqual({ condition: 'all', passRate: 100 })
    expect(parseCompletionExpression('garbage')).toEqual({ condition: 'all', passRate: 100 })
  })
})

describe('parseUserTaskConfig - multiInstance', () => {
  it('parallel + all', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_MI, 'T_all'))
    expect(cfg.multiInstanceType).toBe('parallel')
    expect(cfg.completionCondition).toBe('all')
    expect(cfg.passRate).toBe(100)
  })
  it('parallel + any', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_MI, 'T_any'))
    expect(cfg.multiInstanceType).toBe('parallel')
    expect(cfg.completionCondition).toBe('any')
    expect(cfg.passRate).toBe(null)
  })
  it('parallel + ratio 60%', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_MI, 'T_ratio'))
    expect(cfg.multiInstanceType).toBe('parallel')
    expect(cfg.completionCondition).toBe('ratio')
    expect(cfg.passRate).toBe(60)
  })
  it('sequential 无 completionCondition → 默认 all/100', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_MI, 'T_seq'))
    expect(cfg.multiInstanceType).toBe('sequential')
    expect(cfg.completionCondition).toBe('all')
    expect(cfg.passRate).toBe(100)
  })
  it('无 multiInstanceLoopCharacteristics → none', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_MI, 'T_none'))
    expect(cfg.multiInstanceType).toBe('none')
    expect(cfg.completionCondition).toBe('all')
    expect(cfg.passRate).toBe(100)
  })
})
