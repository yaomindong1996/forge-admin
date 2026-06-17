import { describe, expect, it } from 'vitest'
import { parseBpmnXml } from '../../converter/xml-utils.js'
import {
  bpmnTypeToNodeType,
  NODE_TYPE,
  NODE_TYPE_SET,
  NODE_TYPE_TO_BPMN_LOCAL_NAME,
} from '../node-types.js'

const SAMPLE_ALL_TYPES = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"',
  '                  xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="P1" isExecutable="true">',
  '    <bpmn:startEvent id="N_start"/>',
  '    <bpmn:endEvent id="N_end"/>',
  '    <bpmn:userTask id="N_user"/>',
  '    <bpmn:serviceTask id="N_cc" flowable:type="cc"/>',
  '    <bpmn:serviceTask id="N_svc"/>',
  '    <bpmn:scriptTask id="N_script"/>',
  '    <bpmn:exclusiveGateway id="N_excl"/>',
  '    <bpmn:parallelGateway id="N_par"/>',
  '    <bpmn:inclusiveGateway id="N_inc"/>',
  '    <bpmn:subProcess id="N_sub"/>',
  '    <bpmn:callActivity id="N_call"/>',
  '    <bpmn:intermediateCatchEvent id="N_inter"/>',
  '    <bpmn:boundaryEvent id="N_bound"/>',
  '    <bpmn:manualTask id="N_manual"/>',
  '    <bpmn:businessRuleTask id="N_rule"/>',
  '    <bpmn:complexGateway id="N_complex"/>',
  '    <bpmn:eventBasedGateway id="N_eb"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

const SAMPLE_DEFAULT_NS_CC = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"',
  '             xmlns:flowable="http://flowable.org/bpmn">',
  '  <process id="P1">',
  '    <serviceTask id="cc1" flowable:type="cc"/>',
  '    <serviceTask id="svc1"/>',
  '  </process>',
  '</definitions>',
].join('\n')

function findById(doc, id) {
  const all = doc.getElementsByTagNameNS('*', '*')
  for (let i = 0; i < all.length; i += 1) {
    if (all[i].getAttribute('id') === id)
      return all[i]
  }
  return null
}

describe('nODE_TYPE 常量', () => {
  it('包含 12 种节点类型', () => {
    expect(NODE_TYPE_SET.size).toBe(12)
    expect(NODE_TYPE_SET.has('start')).toBe(true)
    expect(NODE_TYPE_SET.has('approver')).toBe(true)
    expect(NODE_TYPE_SET.has('advanced')).toBe(true)
  })

  it('nODE_TYPE 对象不可被修改', () => {
    expect(Object.isFrozen(NODE_TYPE)).toBe(true)
  })
})

describe('bpmnTypeToNodeType', () => {
  it('识别 12 种基础节点类型 + 6 种走 advanced 兜底', () => {
    const doc = parseBpmnXml(SAMPLE_ALL_TYPES)
    const cases = [
      ['N_start', NODE_TYPE.START],
      ['N_end', NODE_TYPE.END],
      ['N_user', NODE_TYPE.APPROVER],
      ['N_cc', NODE_TYPE.CARBON_COPY],
      ['N_svc', NODE_TYPE.SERVICE],
      ['N_script', NODE_TYPE.SCRIPT],
      ['N_excl', NODE_TYPE.CONDITION],
      ['N_par', NODE_TYPE.PARALLEL],
      ['N_inc', NODE_TYPE.INCLUSIVE],
      ['N_sub', NODE_TYPE.SUB_PROCESS],
      ['N_call', NODE_TYPE.CALL_ACTIVITY],
      // advanced 兜底
      ['N_inter', NODE_TYPE.ADVANCED],
      ['N_bound', NODE_TYPE.ADVANCED],
      ['N_manual', NODE_TYPE.ADVANCED],
      ['N_rule', NODE_TYPE.ADVANCED],
      ['N_complex', NODE_TYPE.ADVANCED],
      ['N_eb', NODE_TYPE.ADVANCED],
    ]
    for (const [id, expected] of cases) {
      const el = findById(doc, id)
      expect(el, `element ${id} not found`).toBeTruthy()
      expect(bpmnTypeToNodeType(el), `id=${id} expect=${expected}`).toBe(expected)
    }
  })

  it('默认命名空间下也能区分 cc / 普通 service', () => {
    const doc = parseBpmnXml(SAMPLE_DEFAULT_NS_CC)
    expect(bpmnTypeToNodeType(findById(doc, 'cc1'))).toBe(NODE_TYPE.CARBON_COPY)
    expect(bpmnTypeToNodeType(findById(doc, 'svc1'))).toBe(NODE_TYPE.SERVICE)
  })

  it('null / 未知元素 → advanced', () => {
    expect(bpmnTypeToNodeType(null)).toBe(NODE_TYPE.ADVANCED)
    const doc = parseBpmnXml(SAMPLE_ALL_TYPES)
    const fakeText = doc.createTextNode('not-an-element')
    expect(bpmnTypeToNodeType(fakeText)).toBe(NODE_TYPE.ADVANCED)
  })
})

describe('nODE_TYPE_TO_BPMN_LOCAL_NAME', () => {
  it('每种 nodeType（除 advanced）都有反向映射', () => {
    const expected = {
      start: 'startEvent',
      end: 'endEvent',
      approver: 'userTask',
      carbonCopy: 'serviceTask',
      service: 'serviceTask',
      script: 'scriptTask',
      condition: 'exclusiveGateway',
      parallel: 'parallelGateway',
      inclusive: 'inclusiveGateway',
      subProcess: 'subProcess',
      callActivity: 'callActivity',
    }
    for (const [k, v] of Object.entries(expected))
      expect(NODE_TYPE_TO_BPMN_LOCAL_NAME[k]).toBe(v)
  })

  it('advanced 不参与默认映射', () => {
    expect(NODE_TYPE_TO_BPMN_LOCAL_NAME[NODE_TYPE.ADVANCED]).toBe(undefined)
  })
})
