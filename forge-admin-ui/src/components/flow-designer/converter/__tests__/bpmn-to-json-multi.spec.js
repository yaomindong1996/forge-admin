import { describe, expect, it } from 'vitest'
import { NODE_TYPE } from '../../constants/node-types.js'
import { convertBpmnToJson } from '../bpmn-to-json.js'

const DOLLAR = '$'

const SAMPLE_MULTI = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"',
  '                  xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="P2">',
  '    <bpmn:startEvent id="S"/>',
  '    <bpmn:scriptTask id="SC1" name="脚本" scriptFormat="javascript">',
  '      <bpmn:script>print("hello");</bpmn:script>',
  '    </bpmn:scriptTask>',
  `    <bpmn:serviceTask id="CC1" name="抄送" flowable:type="cc" flowable:expression="${DOLLAR}{cc.send()}"/>`,
  '    <bpmn:subProcess id="SP1" name="子流程"/>',
  '    <bpmn:callActivity id="CA1" name="调用" calledElement="OtherProcess"/>',
  '    <bpmn:endEvent id="E"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

describe('bpmn-to-json - 多类型节点', () => {
  it('scriptTask 提取 scriptFormat / script', () => {
    const json = convertBpmnToJson(SAMPLE_MULTI)
    const sc = json.nodes.find(n => n.id === 'SC1')
    expect(sc.nodeType).toBe(NODE_TYPE.SCRIPT)
    expect(sc.config.scriptFormat).toBe('javascript')
    expect(sc.config.script).toBe('print("hello");')
  })

  it('serviceTask + flowable:type=cc → carbonCopy', () => {
    const json = convertBpmnToJson(SAMPLE_MULTI)
    const cc = json.nodes.find(n => n.id === 'CC1')
    expect(cc.nodeType).toBe(NODE_TYPE.CARBON_COPY)
    expect(cc.config.flowableType).toBe('cc')
    expect(cc.config.implementationType).toBe('expression')
    expect(cc.config.implementation).toBe(`${DOLLAR}{cc.send()}`)
  })

  it('subProcess / callActivity 识别', () => {
    const json = convertBpmnToJson(SAMPLE_MULTI)
    const sp = json.nodes.find(n => n.id === 'SP1')
    const ca = json.nodes.find(n => n.id === 'CA1')
    expect(sp.nodeType).toBe(NODE_TYPE.SUB_PROCESS)
    expect(ca.nodeType).toBe(NODE_TYPE.CALL_ACTIVITY)
    expect(ca.config.calledElement).toBe('OtherProcess')
  })
})
