import { describe, expect, it } from 'vitest'
import { NODE_TYPE } from '../../constants/node-types.js'
import { convertBpmnToJson } from '../bpmn-to-json.js'

const DOLLAR = '$'

const SAMPLE_COND = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">',
  '  <bpmn:process id="P4">',
  '    <bpmn:exclusiveGateway id="GW1" default="Flow_else"/>',
  '    <bpmn:userTask id="T1"/>',
  '    <bpmn:userTask id="T2"/>',
  '    <bpmn:sequenceFlow id="Flow_a" sourceRef="GW1" targetRef="T1">',
  `      <bpmn:conditionExpression>${DOLLAR}{days &gt; 3}</bpmn:conditionExpression>`,
  '    </bpmn:sequenceFlow>',
  '    <bpmn:sequenceFlow id="Flow_else" sourceRef="GW1" targetRef="T2"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

describe('bpmn-to-json - 条件分支基础', () => {
  it('提取 condition 文本与 conditionType=expression', () => {
    const json = convertBpmnToJson(SAMPLE_COND)
    const cond = json.edges.find(e => e.id === 'Flow_a')
    expect(cond.condition).toBe(`${DOLLAR}{days > 3}`)
    expect(cond.conditionType).toBe('expression')
  })

  it('网关 default 属性记录到 node.config.defaultFlowId', () => {
    const json = convertBpmnToJson(SAMPLE_COND)
    const gw = json.nodes.find(n => n.id === 'GW1')
    expect(gw.nodeType).toBe(NODE_TYPE.CONDITION)
    expect(gw.config.defaultFlowId).toBe('Flow_else')
  })

  it('未配置条件的 sequenceFlow 与默认分支字段', () => {
    const json = convertBpmnToJson(SAMPLE_COND)
    const fe = json.edges.find(e => e.id === 'Flow_else')
    expect(fe.condition).toBe('')
    expect(fe.conditionType).toBe(null)
  })
})
