import { describe, expect, it } from 'vitest'
import { convertJsonToBpmn } from '../json-to-bpmn.js'
import { findElementsByLocalName, getAttr, getFlowableAttr, getRootProcess, parseBpmnXml } from '../xml-utils.js'

const DOLLAR = '$'

function baseJson() {
  return {
    processId: 'Process_1',
    processName: '请假流程',
    nodes: [
      { id: 'S', nodeType: 'start', name: '发起', config: { initiator: 'initiator', documentation: '请假发起' } },
      { id: 'T_appr', nodeType: 'approver', name: '部门经理审批', config: {
        taskType: 'assignee',
        assignee: 'custom',
        assigneeExpr: `${DOLLAR}{user_1001}`,
        assigneeUserName: '张三',
        allowApprove: true,
        allowReject: true,
        allowDelegate: true,
        allowReturn: true, // 非默认
        multiInstanceType: 'none',
      } },
      { id: 'E', nodeType: 'end', name: '结束', config: { endType: 'normal' } },
    ],
    edges: [
      { id: 'F1', source: 'S', target: 'T_appr', condition: '', isDefault: false },
      { id: 'F2', source: 'T_appr', target: 'E', condition: '', isDefault: false },
    ],
  }
}

describe('convertJsonToBpmn - 主结构', () => {
  it('生成完整 BPMN 2.0 文档（definitions + process + diagram）', () => {
    const xml = convertJsonToBpmn(baseJson())
    expect(xml).toContain('<?xml version="1.0"')
    expect(xml).toContain('<bpmn:definitions')
    expect(xml).toContain('<bpmn:process id="Process_1"')
    expect(xml).toContain('<bpmndi:BPMNDiagram')
    expect(xml).toContain('<bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1"')
  })

  it('节点全部出现', () => {
    const xml = convertJsonToBpmn(baseJson())
    const doc = parseBpmnXml(xml)
    expect(findElementsByLocalName(doc, 'startEvent').length).toBe(1)
    expect(findElementsByLocalName(doc, 'userTask').length).toBe(1)
    expect(findElementsByLocalName(doc, 'endEvent').length).toBe(1)
    expect(findElementsByLocalName(doc, 'sequenceFlow').length).toBe(2)
  })

  it('userTask 写入 flowable:assignee + 非默认权限', () => {
    const xml = convertJsonToBpmn(baseJson())
    const doc = parseBpmnXml(xml)
    const t = findElementsByLocalName(doc, 'userTask')[0]
    expect(getFlowableAttr(t, 'assignee')).toBe(`${DOLLAR}{user_1001}`)
    expect(getFlowableAttr(t, 'assigneeName')).toBe('张三')
    expect(getFlowableAttr(t, 'allowReturn')).toBe('true')
    // 默认权限不出现
    expect(getFlowableAttr(t, 'allowApprove')).toBe(null)
  })

  it('每个 BPMNShape 都有真实 bpmnElement 引用', () => {
    const xml = convertJsonToBpmn(baseJson())
    const doc = parseBpmnXml(xml)
    const shapes = findElementsByLocalName(doc, 'BPMNShape')
    const ids = new Set(['S', 'T_appr', 'E'])
    expect(shapes.length).toBe(3)
    for (const s of shapes)
      expect(ids.has(getAttr(s, 'bpmnElement'))).toBe(true)
  })
})

describe('convertJsonToBpmn - 边界场景', () => {
  it('空 flowJson 返回最小可解析定义', () => {
    const xml = convertJsonToBpmn(null)
    const doc = parseBpmnXml(xml)
    expect(getRootProcess(doc)).toBeTruthy()
  })

  it('terminate 结束节点写入 terminateEventDefinition', () => {
    const json = baseJson()
    json.nodes[2].config.endType = 'terminate'
    const xml = convertJsonToBpmn(json)
    expect(xml).toContain('<bpmn:terminateEventDefinition')
  })

  it('advanced 节点 rawXml 原样写出', () => {
    const json = {
      processId: 'P',
      nodes: [
        { id: 'S', nodeType: 'start', name: '', config: {} },
        { id: 'X', nodeType: 'advanced', name: '', config: {}, rawXml: '<bpmn:intermediateCatchEvent id="X"/>' },
        { id: 'E', nodeType: 'end', name: '', config: {} },
      ],
      edges: [
        { id: 'F1', source: 'S', target: 'X' },
        { id: 'F2', source: 'X', target: 'E' },
      ],
    }
    const xml = convertJsonToBpmn(json)
    expect(xml).toContain('<bpmn:intermediateCatchEvent id="X"/>')
  })
})
