import { describe, expect, it } from 'vitest'
import { convertBpmnToJson } from '../bpmn-to-json.js'
import { convertJsonToBpmn } from '../json-to-bpmn.js'
import { findElementsByLocalName, getAttr, parseBpmnXml } from '../xml-utils.js'

const DOLLAR = '$'

const SAMPLE_LINEAR = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="Process_R" name="请假流程" isExecutable="true">',
  '    <bpmn:startEvent id="S"/>',
  `    <bpmn:userTask id="T1" name="经理审批" flowable:assignee="${DOLLAR}{deptManager}" flowable:allowReturn="true"/>`,
  '    <bpmn:endEvent id="E"/>',
  '    <bpmn:sequenceFlow id="F1" sourceRef="S" targetRef="T1"/>',
  '    <bpmn:sequenceFlow id="F2" sourceRef="T1" targetRef="E"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

const SAMPLE_BRANCH = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">',
  '  <bpmn:process id="Process_R2" isExecutable="true">',
  '    <bpmn:startEvent id="S"/>',
  '    <bpmn:exclusiveGateway id="GW" default="F_else"/>',
  `    <bpmn:userTask id="T_a" flowable:assignee="${DOLLAR}{user_1001}"/>`,
  `    <bpmn:userTask id="T_b" flowable:assignee="${DOLLAR}{user_1002}"/>`,
  '    <bpmn:endEvent id="E"/>',
  '    <bpmn:sequenceFlow id="F_in" sourceRef="S" targetRef="GW"/>',
  '    <bpmn:sequenceFlow id="F_a" sourceRef="GW" targetRef="T_a">',
  `      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${DOLLAR}{days &gt; 3}</bpmn:conditionExpression>`,
  '    </bpmn:sequenceFlow>',
  '    <bpmn:sequenceFlow id="F_else" sourceRef="GW" targetRef="T_b"/>',
  '    <bpmn:sequenceFlow id="F_a_e" sourceRef="T_a" targetRef="E"/>',
  '    <bpmn:sequenceFlow id="F_b_e" sourceRef="T_b" targetRef="E"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

const SAMPLE_MI = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="P_MI" isExecutable="true">',
  '    <bpmn:startEvent id="S"/>',
  '    <bpmn:userTask id="T_mi" flowable:candidateUsers="1,2,3">',
  '      <bpmn:multiInstanceLoopCharacteristics isSequential="false">',
  `        <bpmn:completionCondition>${DOLLAR}{nrOfCompletedInstances/nrOfInstances >= 0.6}</bpmn:completionCondition>`,
  '      </bpmn:multiInstanceLoopCharacteristics>',
  '    </bpmn:userTask>',
  '    <bpmn:endEvent id="E"/>',
  '    <bpmn:sequenceFlow id="F1" sourceRef="S" targetRef="T_mi"/>',
  '    <bpmn:sequenceFlow id="F2" sourceRef="T_mi" targetRef="E"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

function getNode(doc, localName, id) {
  return findElementsByLocalName(doc, localName).find(n => getAttr(n, 'id') === id)
}

describe('roundtrip - 线性流程语义等价', () => {
  it('节点 + 边数量 + assignee + allowReturn 保留', () => {
    const json1 = convertBpmnToJson(SAMPLE_LINEAR)
    const xml2 = convertJsonToBpmn(json1)
    const json2 = convertBpmnToJson(xml2)

    expect(json2.nodes.length).toBe(json1.nodes.length)
    expect(json2.edges.length).toBe(json1.edges.length)

    const t1 = json2.nodes.find(n => n.id === 'T1')
    expect(t1.config.assignee).toBe(`${DOLLAR}{deptManager}`)
    expect(t1.config.allowReturn).toBe(true)
  })
})

describe('roundtrip - 排他分支语义等价', () => {
  it('分支 condition / default / 网关 default 属性都保留', () => {
    const xml2 = convertJsonToBpmn(convertBpmnToJson(SAMPLE_BRANCH))
    const doc = parseBpmnXml(xml2)
    const gw = getNode(doc, 'exclusiveGateway', 'GW')
    expect(getAttr(gw, 'default')).toBe('F_else')

    const fa = getNode(doc, 'sequenceFlow', 'F_a')
    expect(fa.textContent).toContain(`${DOLLAR}{days > 3}`)

    const fe = getNode(doc, 'sequenceFlow', 'F_else')
    // default 边不应有 conditionExpression
    expect(findElementsByLocalName(fe, 'conditionExpression').length).toBe(0)
  })
})

describe('roundtrip - 会签语义等价', () => {
  it('candidateUsers + ratio 60 → ratio 60 解析回来一致', () => {
    const json1 = convertBpmnToJson(SAMPLE_MI)
    const xml2 = convertJsonToBpmn(json1)
    const json2 = convertBpmnToJson(xml2)

    const t = json2.nodes.find(n => n.id === 'T_mi')
    expect(t.config.taskType).toBe('candidateUsers')
    expect(t.config.candidateUsers).toEqual(['1', '2', '3'])
    expect(t.config.multiInstanceType).toBe('parallel')
    expect(t.config.completionCondition).toBe('ratio')
    expect(t.config.passRate).toBe(60)
  })

  it('all / any / ratio 在二次往返后保持一致', () => {
    const variants = ['all', 'any', 'ratio']
    for (const c of variants) {
      const json = {
        processId: 'P',
        nodes: [
          { id: 'S', nodeType: 'start', name: '', config: {} },
          { id: 'T', nodeType: 'approver', name: '', config: {
            taskType: 'candidateUsers',
            candidateUsers: ['1'],
            multiInstanceType: 'parallel',
            completionCondition: c,
            passRate: c === 'ratio' ? 70 : (c === 'all' ? 100 : null),
          } },
          { id: 'E', nodeType: 'end', name: '', config: {} },
        ],
        edges: [
          { id: 'F1', source: 'S', target: 'T' },
          { id: 'F2', source: 'T', target: 'E' },
        ],
      }
      const xml = convertJsonToBpmn(json)
      const json2 = convertBpmnToJson(xml)
      const t = json2.nodes.find(n => n.id === 'T')
      expect(t.config.completionCondition).toBe(c)
      if (c === 'ratio')
        expect(t.config.passRate).toBe(70)
    }
  })
})
