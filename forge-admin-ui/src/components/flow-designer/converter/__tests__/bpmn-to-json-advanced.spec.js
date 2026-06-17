import { describe, expect, it } from 'vitest'
import { NODE_TYPE } from '../../constants/node-types.js'
import { convertBpmnToJson } from '../bpmn-to-json.js'

const SAMPLE_ADV = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">',
  '  <bpmn:process id="P3">',
  '    <bpmn:startEvent id="S"/>',
  '    <bpmn:intermediateCatchEvent id="ICE1" name="中间事件">',
  '      <bpmn:timerEventDefinition id="TED1">',
  '        <bpmn:timeDuration>PT5M</bpmn:timeDuration>',
  '      </bpmn:timerEventDefinition>',
  '    </bpmn:intermediateCatchEvent>',
  '    <bpmn:endEvent id="E"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

const SAMPLE_TERM = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">',
  '  <bpmn:process id="P5">',
  '    <bpmn:endEvent id="E_term">',
  '      <bpmn:terminateEventDefinition id="TT"/>',
  '    </bpmn:endEvent>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

describe('bpmn-to-json - advanced 兜底', () => {
  it('未识别 BPMN 元素 → advanced 节点 + rawXml', () => {
    const json = convertBpmnToJson(SAMPLE_ADV)
    const adv = json.nodes.find(n => n.id === 'ICE1')
    expect(adv.nodeType).toBe(NODE_TYPE.ADVANCED)
    expect(adv.rawXml).toBeTruthy()
    expect(adv.rawXml).toContain('intermediateCatchEvent')
    expect(adv.rawXml).toContain('PT5M')
  })

  it('start / end 节点不会被错误兜底', () => {
    const json = convertBpmnToJson(SAMPLE_ADV)
    expect(json.nodes.find(n => n.id === 'S').nodeType).toBe(NODE_TYPE.START)
    expect(json.nodes.find(n => n.id === 'E').nodeType).toBe(NODE_TYPE.END)
  })
})

describe('bpmn-to-json - End 节点类型', () => {
  it('terminateEventDefinition → endType=terminate', () => {
    const json = convertBpmnToJson(SAMPLE_TERM)
    const e = json.nodes.find(n => n.id === 'E_term')
    expect(e.nodeType).toBe(NODE_TYPE.END)
    expect(e.config.endType).toBe('terminate')
  })
})
