import { describe, expect, it } from 'vitest'
import {
  findElementsByLocalName,
  getDocumentation,
  getExtensionElement,
  getExtensionElements,
  getFlowableAttr,
  getFlowableBoolAttr,
  getRootProcess,
  getTextContent,
  parseBpmnXml,
} from '../xml-utils.js'

const DOLLAR = '$'

const SAMPLE_USERTASK = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"',
  '                  xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="P1" isExecutable="true">',
  '    <bpmn:startEvent id="Start_1" name="发起">',
  '      <bpmn:documentation>流程开始节点</bpmn:documentation>',
  '    </bpmn:startEvent>',
  `    <bpmn:userTask id="Task_1" name="部门经理审批"`,
  `                   flowable:assignee="${DOLLAR}{user_1001}"`,
  '                   flowable:assigneeType="custom"',
  '                   flowable:allowApprove="true"',
  '                   flowable:allowReject="false">',
  '      <bpmn:extensionElements>',
  '        <flowable:taskListener event="create" class="com.example.OnCreate"/>',
  `        <flowable:taskListener event="complete" expression="${DOLLAR}{onComplete}"/>`,
  '        <flowable:executionListener event="start" class="com.example.OnStart"/>',
  '      </bpmn:extensionElements>',
  '    </bpmn:userTask>',
  '    <bpmn:endEvent id="End_1"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

const SAMPLE_DEFAULT_NS = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"',
  '             xmlns:flowable="http://flowable.org/bpmn">',
  '  <process id="P1" isExecutable="true">',
  `    <userTask id="T1" name="审批" flowable:assignee="${DOLLAR}{initiator}"/>`,
  '  </process>',
  '</definitions>',
].join('\n')

describe('getFlowableAttr', () => {
  it('读取 flowable:* 属性', () => {
    const doc = parseBpmnXml(SAMPLE_USERTASK)
    const tasks = findElementsByLocalName(doc, 'userTask')
    expect(getFlowableAttr(tasks[0], 'assignee')).toBe(`${DOLLAR}{user_1001}`)
    expect(getFlowableAttr(tasks[0], 'assigneeType')).toBe('custom')
  })

  it('缺失返回 null', () => {
    const doc = parseBpmnXml(SAMPLE_USERTASK)
    const tasks = findElementsByLocalName(doc, 'userTask')
    expect(getFlowableAttr(tasks[0], 'noSuch')).toBe(null)
    expect(getFlowableAttr(null, 'assignee')).toBe(null)
    expect(getFlowableAttr(tasks[0], '')).toBe(null)
  })

  it('支持默认命名空间下的 flowable 属性', () => {
    const doc = parseBpmnXml(SAMPLE_DEFAULT_NS)
    const tasks = findElementsByLocalName(doc, 'userTask')
    expect(getFlowableAttr(tasks[0], 'assignee')).toBe(`${DOLLAR}{initiator}`)
  })
})

describe('getFlowableBoolAttr', () => {
  it('"true" → true，"false" / 缺失 → false', () => {
    const doc = parseBpmnXml(SAMPLE_USERTASK)
    const tasks = findElementsByLocalName(doc, 'userTask')
    expect(getFlowableBoolAttr(tasks[0], 'allowApprove')).toBe(true)
    expect(getFlowableBoolAttr(tasks[0], 'allowReject')).toBe(false)
    expect(getFlowableBoolAttr(tasks[0], 'allowDelegate')).toBe(false)
  })
})

describe('getExtensionElement(s)', () => {
  it('查找扩展子元素列表', () => {
    const doc = parseBpmnXml(SAMPLE_USERTASK)
    const tasks = findElementsByLocalName(doc, 'userTask')
    const taskListeners = getExtensionElements(tasks[0], 'taskListener')
    expect(taskListeners).toHaveLength(2)
    expect(taskListeners[0].getAttribute('event')).toBe('create')
    expect(taskListeners[1].getAttribute('event')).toBe('complete')
  })

  it('getExtensionElement 取第一个', () => {
    const doc = parseBpmnXml(SAMPLE_USERTASK)
    const tasks = findElementsByLocalName(doc, 'userTask')
    const exec = getExtensionElement(tasks[0], 'executionListener')
    expect(exec).toBeTruthy()
    expect(exec.getAttribute('event')).toBe('start')
  })

  it('未传 localName 返回所有扩展子节点', () => {
    const doc = parseBpmnXml(SAMPLE_USERTASK)
    const tasks = findElementsByLocalName(doc, 'userTask')
    const all = getExtensionElements(tasks[0])
    expect(all).toHaveLength(3)
  })

  it('没有 extensionElements 时返回空 / null', () => {
    const doc = parseBpmnXml(SAMPLE_USERTASK)
    const ends = findElementsByLocalName(doc, 'endEvent')
    expect(getExtensionElements(ends[0], 'taskListener')).toEqual([])
    expect(getExtensionElement(ends[0], 'taskListener')).toBe(null)
  })

  it('null 元素安全', () => {
    expect(getExtensionElements(null, 'foo')).toEqual([])
    expect(getExtensionElement(null, 'foo')).toBe(null)
  })
})

describe('getDocumentation / getTextContent', () => {
  it('读取 documentation 文本（去除空白）', () => {
    const doc = parseBpmnXml(SAMPLE_USERTASK)
    const proc = getRootProcess(doc)
    const start = findElementsByLocalName(proc, 'startEvent')[0]
    expect(getDocumentation(start)).toBe('流程开始节点')
  })

  it('没有 documentation 返回空字符串', () => {
    const doc = parseBpmnXml(SAMPLE_USERTASK)
    const tasks = findElementsByLocalName(doc, 'userTask')
    expect(getDocumentation(tasks[0])).toBe('')
    expect(getDocumentation(null)).toBe('')
  })

  it('getTextContent 处理 null', () => {
    expect(getTextContent(null)).toBe('')
  })
})
