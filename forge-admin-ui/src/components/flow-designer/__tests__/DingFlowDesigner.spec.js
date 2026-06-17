import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DingFlowDesigner from '../DingFlowDesigner.vue'

const SIMPLE_XML = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="Process_T1" name="测试" isExecutable="true">',
  '    <bpmn:startEvent id="S"/>',
  '    <bpmn:userTask id="T1" name="审批" flowable:assignee="${initiator}"/>',
  '    <bpmn:endEvent id="E"/>',
  '    <bpmn:sequenceFlow id="F1" sourceRef="S" targetRef="T1"/>',
  '    <bpmn:sequenceFlow id="F2" sourceRef="T1" targetRef="E"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

// 简化挂载：用 stubs 避免 Naive UI 全局组件未注册导致 warning 失控
const STUBS = {
  'n-drawer': { template: '<div class="n-drawer"><slot /></div>' },
  'n-drawer-content': { template: '<div class="n-drawer-content"><slot /><slot name="header" /><slot name="footer" /></div>' },
  'n-tabs': { template: '<div class="n-tabs"><slot /></div>' },
  'n-tab-pane': { template: '<div class="n-tab-pane"><slot /></div>' },
  'n-form-item': { template: '<div class="n-form-item"><slot /></div>' },
  'n-input': { template: '<input class="n-input" />' },
  'n-input-number': { template: '<input class="n-input-number" />' },
  'n-select': { template: '<select class="n-select"></select>' },
  'n-switch': { template: '<input type="checkbox" class="n-switch" />' },
  'n-button': { template: '<button class="n-button"><slot /></button>' },
  'n-radio': { template: '<input type="radio" class="n-radio"><slot /></input>' },
  'n-radio-group': { template: '<div class="n-radio-group"><slot /></div>' },
  'n-space': { template: '<div class="n-space"><slot /></div>' },
  'n-checkbox': { template: '<input type="checkbox" class="n-checkbox" /><slot />' },
  'n-divider': { template: '<hr class="n-divider" />' },
  'n-tag': { template: '<span class="n-tag"><slot /></span>' },
}

function mountDesigner(props = {}) {
  return mount(DingFlowDesigner, {
    props,
    global: { stubs: STUBS },
  })
}

describe('dingFlowDesigner - 基础 mount', () => {
  it('空 props 挂载默认 createEmptyFlow（start → end）', () => {
    const w = mountDesigner()
    const exposed = w.vm
    const json = exposed.designer.flowJson.value
    expect(json.nodes.map(n => n.nodeType)).toEqual(['start', 'end'])
    w.unmount()
  })

  it('暴露 setXML / getXML / reset / undo / redo', () => {
    const w = mountDesigner()
    const exposed = w.vm
    expect(typeof exposed.setXML).toBe('function')
    expect(typeof exposed.getXML).toBe('function')
    expect(typeof exposed.reset).toBe('function')
    expect(typeof exposed.undo).toBe('function')
    expect(typeof exposed.redo).toBe('function')
    w.unmount()
  })
})

describe('dingFlowDesigner - props.xml 输入加载', () => {
  it('xml prop 触发 import 后 nodes / edges 完整加载', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML })
    await new Promise(r => setTimeout(r, 50))
    const json = w.vm.designer.flowJson.value
    expect(json.nodes.map(n => n.id)).toEqual(['S', 'T1', 'E'])
    expect(json.edges).toHaveLength(2)
    expect(json.nodes.find(n => n.id === 'T1').config.assignee).toBe('${initiator}')
    w.unmount()
  })

  it('getXML 返回当前流程的 BPMN XML，包含 process id', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML })
    await new Promise(r => setTimeout(r, 50))
    const xml = w.vm.getXML()
    expect(xml).toContain('Process_T1')
    expect(xml).toContain('userTask')
    expect(xml).toContain('${initiator}')
    w.unmount()
  })

  it('xML → JSON → XML 往返保留关键节点', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML })
    await new Promise(r => setTimeout(r, 50))
    const xml2 = w.vm.getXML()
    expect(xml2).toContain('startEvent')
    expect(xml2).toContain('userTask')
    expect(xml2).toContain('endEvent')
    expect(xml2).toContain('sequenceFlow')
    expect(xml2).toContain('id="F1"')
    expect(xml2).toContain('id="F2"')
    w.unmount()
  })
})

describe('dingFlowDesigner - reset / setXML', () => {
  it('reset 清空回 start → end 默认状态', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML })
    await new Promise(r => setTimeout(r, 50))
    w.vm.reset()
    const json = w.vm.designer.flowJson.value
    expect(json.nodes.map(n => n.nodeType)).toEqual(['start', 'end'])
    w.unmount()
  })

  it('setXML 多次切换不报错', async () => {
    const w = mountDesigner()
    await w.vm.setXML(SIMPLE_XML)
    expect(w.vm.designer.flowJson.value.nodes.length).toBe(3)
    await w.vm.setXML(SIMPLE_XML)
    expect(w.vm.designer.flowJson.value.nodes.length).toBe(3)
    w.unmount()
  })
})

describe('dingFlowDesigner - readonly', () => {
  it('readonly=true 时不抛错', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML, readonly: true })
    await new Promise(r => setTimeout(r, 50))
    expect(w.vm.designer.flowJson.value.nodes.length).toBe(3)
    w.unmount()
  })
})
