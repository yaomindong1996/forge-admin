import { describe, expect, it } from 'vitest'
import { NODE_TYPE } from '../../constants/node-types.js'
import { convertBpmnToJson } from '../bpmn-to-json.js'

const SAMPLE_LINEAR = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"',
  '                  xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="Process_1" name="请假流程" isExecutable="true">',
  '    <bpmn:startEvent id="Start_1" name="发起" flowable:initiator="initiator">',
  '      <bpmn:documentation>流程开始</bpmn:documentation>',
  '    </bpmn:startEvent>',
  '    <bpmn:serviceTask id="Service_1" name="发送通知"',
  '                      flowable:class="com.example.NotifyDelegate"',
  '                      flowable:async="true"/>',
  '    <bpmn:endEvent id="End_1" name="结束"/>',
  '    <bpmn:sequenceFlow id="Flow_1" sourceRef="Start_1" targetRef="Service_1"/>',
  '    <bpmn:sequenceFlow id="Flow_2" sourceRef="Service_1" targetRef="End_1"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

describe('bpmn-to-json - 线性流程', () => {
  it('processId / processName / nodes / edges 正确', () => {
    const json = convertBpmnToJson(SAMPLE_LINEAR)
    expect(json.processId).toBe('Process_1')
    expect(json.processName).toBe('请假流程')
    expect(json.nodes.map(n => n.id)).toEqual(['Start_1', 'Service_1', 'End_1'])
    expect(json.edges.map(e => e.id)).toEqual(['Flow_1', 'Flow_2'])
  })

  it('正确识别 nodeType', () => {
    const json = convertBpmnToJson(SAMPLE_LINEAR)
    const types = Object.fromEntries(json.nodes.map(n => [n.id, n.nodeType]))
    expect(types).toEqual({
      Start_1: NODE_TYPE.START,
      Service_1: NODE_TYPE.SERVICE,
      End_1: NODE_TYPE.END,
    })
  })

  it('start 节点提取 documentation 和 initiator', () => {
    const json = convertBpmnToJson(SAMPLE_LINEAR)
    const start = json.nodes.find(n => n.id === 'Start_1')
    expect(start.config.documentation).toBe('流程开始')
    expect(start.config.initiator).toBe('initiator')
  })

  it('serviceTask 提取 implementation / async', () => {
    const json = convertBpmnToJson(SAMPLE_LINEAR)
    const svc = json.nodes.find(n => n.id === 'Service_1')
    expect(svc.config.implementationType).toBe('class')
    expect(svc.config.implementation).toBe('com.example.NotifyDelegate')
    expect(svc.config.async).toBe(true)
  })

  it('普通 endEvent → endType=normal', () => {
    const json = convertBpmnToJson(SAMPLE_LINEAR)
    const e = json.nodes.find(n => n.id === 'End_1')
    expect(e.config.endType).toBe('normal')
  })

  it('sequenceFlow 默认字段', () => {
    const json = convertBpmnToJson(SAMPLE_LINEAR)
    expect(json.edges[0]).toMatchObject({
      id: 'Flow_1',
      source: 'Start_1',
      target: 'Service_1',
      condition: '',
      isDefault: false,
      branchId: null,
      conditionType: null,
    })
  })

  it('每个节点有 bpmnElementType 和 rawXml', () => {
    const json = convertBpmnToJson(SAMPLE_LINEAR)
    const svc = json.nodes.find(n => n.id === 'Service_1')
    expect(svc.bpmnElementType).toBe('bpmn:ServiceTask')
    expect(svc.rawXml).toBe(null)
  })
})
