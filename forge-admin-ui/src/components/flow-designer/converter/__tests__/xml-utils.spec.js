import { describe, expect, it } from 'vitest'
import {
  findElementsByLocalName,
  getAttr,
  getChild,
  getChildren,
  getLocalName,
  getRootProcess,
  parseBpmnXml,
  serializeXml,
} from '../xml-utils.js'

const DOLLAR = '$'

const SAMPLE_LINEAR = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"',
  '                  xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="Process_1" name="请假流程" isExecutable="true">',
  '    <bpmn:startEvent id="Start_1" name="发起"/>',
  `    <bpmn:userTask id="Task_1" name="部门经理审批" flowable:assignee="${DOLLAR}{user_1001}"/>`,
  '    <bpmn:endEvent id="End_1" name="结束"/>',
  '    <bpmn:sequenceFlow id="Flow_1" sourceRef="Start_1" targetRef="Task_1"/>',
  '    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="End_1"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

const SAMPLE_MULTI_PROCESS = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">',
  '  <bpmn:collaboration id="Collab_1"/>',
  '  <bpmn:process id="Process_A" isExecutable="false"/>',
  '  <bpmn:process id="Process_B" isExecutable="true">',
  '    <bpmn:startEvent id="Start_B"/>',
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

describe('parseBpmnXml', () => {
  it('解析合法 BPMN XML 返回 Document', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    expect(doc).toBeTruthy()
    expect(getLocalName(doc.documentElement)).toBe('definitions')
  })

  it('空字符串 / null / 空白都抛错', () => {
    expect(() => parseBpmnXml('')).toThrow()
    expect(() => parseBpmnXml('   ')).toThrow()
    expect(() => parseBpmnXml(null)).toThrow()
  })

  it('非法 XML 抛 parse error', () => {
    expect(() => parseBpmnXml('<not-xml<<>')).toThrow(/parse error/i)
  })
})

describe('serializeXml', () => {
  it('document 往返保留主要节点', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    const out = serializeXml(doc)
    expect(out).toContain('Process_1')
    expect(out).toContain('Task_1')
    expect(out).toContain('flowable:assignee')
  })

  it('null 文档返回空字符串', () => {
    expect(serializeXml(null)).toBe('')
  })
})

describe('getLocalName', () => {
  it('返回不带前缀的 tag', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    const tasks = findElementsByLocalName(doc, 'userTask')
    expect(getLocalName(tasks[0])).toBe('userTask')
  })

  it('null 返回空字符串', () => {
    expect(getLocalName(null)).toBe('')
  })
})

describe('getRootProcess', () => {
  it('单 process 直接返回', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    expect(getRootProcess(doc).getAttribute('id')).toBe('Process_1')
  })

  it('多 process 时返回 isExecutable=true 的那个', () => {
    const doc = parseBpmnXml(SAMPLE_MULTI_PROCESS)
    expect(getRootProcess(doc).getAttribute('id')).toBe('Process_B')
  })

  it('支持默认 BPMN 命名空间（无 bpmn: 前缀）', () => {
    const doc = parseBpmnXml(SAMPLE_DEFAULT_NS)
    expect(getRootProcess(doc).getAttribute('id')).toBe('P1')
  })

  it('null Document 返回 null', () => {
    expect(getRootProcess(null)).toBe(null)
  })
})

describe('findElementsByLocalName', () => {
  it('深度查找所有匹配元素', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    const flows = findElementsByLocalName(doc, 'sequenceFlow')
    expect(flows.map(f => f.getAttribute('id'))).toEqual(['Flow_1', 'Flow_2'])
  })

  it('找不到返回空数组', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    expect(findElementsByLocalName(doc, 'nonExistTag')).toEqual([])
  })

  it('null 安全', () => {
    expect(findElementsByLocalName(null, 'process')).toEqual([])
    expect(findElementsByLocalName(parseBpmnXml(SAMPLE_LINEAR), '')).toEqual([])
  })
})

describe('getChildren / getChild', () => {
  it('只返回直接子元素，不递归', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    const proc = getRootProcess(doc)
    expect(getChildren(proc, 'sequenceFlow')).toHaveLength(2)
    expect(getChildren(proc, 'extensionElements')).toHaveLength(0)
  })

  it('未传 localName 返回所有元素子节点', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    const proc = getRootProcess(doc)
    expect(getChildren(proc).length).toBe(5)
  })

  it('getChild 返回第一个匹配项', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    const proc = getRootProcess(doc)
    expect(getChild(proc, 'startEvent').getAttribute('id')).toBe('Start_1')
  })

  it('null 入参返回安全值', () => {
    expect(getChildren(null, 'foo')).toEqual([])
    expect(getChild(null, 'foo')).toBe(null)
  })
})

describe('getAttr', () => {
  it('读取存在的属性', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    const proc = getRootProcess(doc)
    expect(getAttr(proc, 'id')).toBe('Process_1')
    expect(getAttr(proc, 'isExecutable')).toBe('true')
  })

  it('缺失返回 null', () => {
    const doc = parseBpmnXml(SAMPLE_LINEAR)
    const proc = getRootProcess(doc)
    expect(getAttr(proc, 'no_such_attr')).toBe(null)
    expect(getAttr(null, 'id')).toBe(null)
  })
})
