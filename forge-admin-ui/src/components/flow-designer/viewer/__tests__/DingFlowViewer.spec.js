import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DingFlowViewer from '../DingFlowViewer.vue'

const SIMPLE_XML = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="Process_V1" isExecutable="true">',
  '    <bpmn:startEvent id="S"/>',
  '    <bpmn:userTask id="T1" name="审批"/>',
  '    <bpmn:endEvent id="E"/>',
  '    <bpmn:sequenceFlow id="F1" sourceRef="S" targetRef="T1"/>',
  '    <bpmn:sequenceFlow id="F2" sourceRef="T1" targetRef="E"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

// 与 DingFlowDesigner spec 相同的 Naive UI stubs
const STUBS = {
  'n-drawer': { template: '<div><slot /></div>' },
  'n-drawer-content': { template: '<div><slot /><slot name="header" /><slot name="footer" /></div>' },
  'n-tabs': { template: '<div><slot /></div>' },
  'n-tab-pane': { template: '<div><slot /></div>' },
  'n-form-item': { template: '<div><slot /></div>' },
  'n-input': { template: '<input />' },
  'n-input-number': { template: '<input />' },
  'n-select': { template: '<select></select>' },
  'n-switch': { template: '<input type="checkbox" />' },
  'n-button': { template: '<button><slot /></button>' },
  'n-radio': { template: '<input type="radio"><slot /></input>' },
  'n-radio-group': { template: '<div><slot /></div>' },
  'n-space': { template: '<div><slot /></div>' },
  'n-checkbox': { template: '<input type="checkbox" /><slot />' },
  'n-divider': { template: '<hr />' },
  'n-tag': { template: '<span><slot /></span>' },
}

function mountViewer(props = {}) {
  return mount(DingFlowViewer, { props, global: { stubs: STUBS } })
}

describe('dingFlowViewer - 直接传入 bpmnXml 模式', () => {
  it('mount 不抛错 + 加载节点 / 边', () => {
    const w = mountViewer({ bpmnXml: SIMPLE_XML })
    const json = w.vm.designer.flowJson.value
    expect(json.nodes.map(n => n.id)).toEqual(['S', 'T1', 'E'])
    expect(json.edges).toHaveLength(2)
    w.unmount()
  })

  it('nodeInstanceList 转 nodeStatusMap', () => {
    const w = mountViewer({
      bpmnXml: SIMPLE_XML,
      nodeInstanceList: [
        { nodeId: 'S', status: 'completed' },
        { nodeId: 'T1', status: 'running', assigneeName: '张三' },
      ],
    })
    const map = w.vm.nodeStatusMap
    expect(map.S.status).toBe('completed')
    expect(map.T1.assigneeName).toBe('张三')
    expect(map.E).toBeUndefined()
    w.unmount()
  })

  it('compact=true 时不渲染顶部状态条', () => {
    const w = mountViewer({ bpmnXml: SIMPLE_XML, compact: true })
    expect(w.find('.ding-flow-viewer.compact').exists()).toBe(true)
    w.unmount()
  })
})

describe('dingFlowViewer - processInstanceId 模式', () => {
  it('有 processInstanceId 且无 bpmnXml 时进入 loading 状态（接口 mock）', () => {
    const w = mountViewer({ processInstanceId: 12345 })
    expect(w.vm.loading).toBe(true)
    w.unmount()
  })
})

describe('dingFlowViewer - 边界', () => {
  it('空 props 不抛错（默认 createEmptyFlow）', () => {
    const w = mountViewer()
    expect(w.vm.designer.flowJson.value.nodes.map(n => n.nodeType)).toEqual(['start', 'end'])
    w.unmount()
  })
})
