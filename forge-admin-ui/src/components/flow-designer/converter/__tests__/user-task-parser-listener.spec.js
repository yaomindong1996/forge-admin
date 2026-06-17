import { describe, expect, it } from 'vitest'
import { parseUserTaskConfig } from '../user-task-parser.js'
import { findElementsByLocalName, parseBpmnXml } from '../xml-utils.js'

const DOLLAR = '$'

function getTask(xml, id) {
  const doc = parseBpmnXml(xml)
  return findElementsByLocalName(doc, 'userTask').find(t => t.getAttribute('id') === id)
}

const SAMPLE_LISTENER = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="P">',
  '    <bpmn:userTask id="T_listeners">',
  '      <bpmn:extensionElements>',
  '        <flowable:taskListener event="create" class="com.example.OnCreate"/>',
  `        <flowable:taskListener event="complete" expression="${DOLLAR}{onComplete}"/>`,
  '        <flowable:taskListener event="assignment" delegateExpression="assignBean"/>',
  '        <flowable:executionListener event="start" class="com.example.OnStart"/>',
  '        <flowable:executionListener event="end" class="com.example.OnEnd"/>',
  '      </bpmn:extensionElements>',
  '    </bpmn:userTask>',
  '    <bpmn:userTask id="T_no_listener"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

describe('parseUserTaskConfig - listeners', () => {
  it('提取 taskListener 列表（class / expression / delegateExpression）', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_LISTENER, 'T_listeners'))
    expect(cfg.taskListeners).toHaveLength(3)
    expect(cfg.taskListeners[0]).toMatchObject({ event: 'create', type: 'class', value: 'com.example.OnCreate' })
    expect(cfg.taskListeners[1]).toMatchObject({ event: 'complete', type: 'expression', value: `${DOLLAR}{onComplete}` })
    expect(cfg.taskListeners[2]).toMatchObject({ event: 'assignment', type: 'delegateExpression', value: 'assignBean' })
  })

  it('提取 executionListener 列表', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_LISTENER, 'T_listeners'))
    expect(cfg.executionListeners).toHaveLength(2)
    expect(cfg.executionListeners[0]).toMatchObject({ event: 'start', type: 'class', value: 'com.example.OnStart' })
    expect(cfg.executionListeners[1]).toMatchObject({ event: 'end', type: 'class', value: 'com.example.OnEnd' })
  })

  it('无监听器时返回空数组', () => {
    const cfg = parseUserTaskConfig(getTask(SAMPLE_LISTENER, 'T_no_listener'))
    expect(cfg.taskListeners).toEqual([])
    expect(cfg.executionListeners).toEqual([])
  })
})
