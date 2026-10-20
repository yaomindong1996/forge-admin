import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { findElementsByLocalName, getFlowableAttr, parseBpmnXml } from '../converter/xml-utils.js'
import DingFlowDesigner from '../DingFlowDesigner.vue'

const DOLLAR = '$'
const SIMPLE_XML = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="Process_T1" name="测试" isExecutable="true">',
  '    <bpmn:startEvent id="S"/>',
  `    <bpmn:userTask id="T1" name="审批" flowable:assignee="${DOLLAR}{initiator}"/>`,
  '    <bpmn:endEvent id="E"/>',
  '    <bpmn:sequenceFlow id="F1" sourceRef="S" targetRef="T1"/>',
  '    <bpmn:sequenceFlow id="F2" sourceRef="T1" targetRef="E"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

const LEGACY_FORM_XML = SIMPLE_XML.replace(
  `flowable:assignee="${DOLLAR}{initiator}"`,
  `flowable:assignee="${DOLLAR}{initiator}" flowable:formKey="Activity_069lyqd_form" flowable:formJson="[]"`,
)

const APPLICATION_PAGE_FORM = {
  value: 'app_2089968247981060098_page_page_page_form_form_form',
  formKey: 'app_2089968247981060098_page_page_page_form_form_form',
  formName: '测试',
  formMode: 'BUSINESS_OBJECT_FORM',
  applicationId: '2089968247981060098',
  pageId: 'page_page',
  pageCode: 'page_page',
  pageName: '测试',
  pageType: 'object',
  sourceFormKey: 'business_object_form',
  fieldCatalog: [
    { field: 'fieldSlider', label: '滑块' },
    { field: 'fieldInput', label: '输入框11' },
  ],
}

const ARRAY_APPLICATION_PAGE_FORM = {
  ...APPLICATION_PAGE_FORM,
  value: 'expense_page_form',
  formKey: 'expense_page_form',
  formName: '费用申请',
  fieldCatalog: [
    { field: 'expenseItems', label: '费用明细', componentType: 'group', dataType: 'array' },
    {
      scope: 'array',
      arrayKey: 'expenseItems',
      arrayLabel: '费用明细',
      itemField: 'amount',
      field: 'amount',
      label: '金额',
    },
  ],
}

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
  'n-radio': {
    props: ['checked', 'disabled'],
    emits: ['click'],
    template: '<label class="n-radio" @click="$emit(\'click\', $event)"><input type="radio" :checked="checked" :disabled="disabled"><slot /></label>',
  },
  'n-radio-group': { template: '<div class="n-radio-group"><slot /></div>' },
  'n-space': { template: '<div class="n-space"><slot /></div>' },
  'n-checkbox': {
    props: ['checked', 'disabled'],
    emits: ['update:checked'],
    template: '<input type="checkbox" class="n-checkbox" :checked="checked" :disabled="disabled" v-bind="$attrs" @change="$emit(\'update:checked\', $event.target.checked)" />',
  },
  'n-divider': { template: '<hr class="n-divider" />' },
  'n-tag': { template: '<span class="n-tag"><slot /></span>' },
}

function mountDesigner(props = {}) {
  return mount(DingFlowDesigner, {
    props,
    global: {
      plugins: [createPinia()],
      stubs: STUBS,
    },
  })
}

function leftOf(wrapper) {
  const style = wrapper.attributes('style') || ''
  const match = style.match(/left:\s*([\d.-]+)px/)
  return match ? Number(match[1]) : null
}

function topOf(wrapper) {
  const style = wrapper.attributes('style') || ''
  const match = style.match(/top:\s*([\d.-]+)px/)
  return match ? Number(match[1]) : null
}

function reentryDecisionJson() {
  return {
    processId: 'P',
    nodes: [
      { id: 'S', nodeType: 'start', config: {} },
      { id: 'A1', nodeType: 'approver', config: { mergeNode: true } },
      { id: 'GW1', nodeType: 'condition', config: { defaultFlowId: 'F3' } },
      { id: 'A2', nodeType: 'approver', config: {} },
      { id: 'GW2', nodeType: 'condition', config: { defaultFlowId: 'F6' } },
      { id: 'A3', nodeType: 'approver', config: {} },
      { id: 'GW3', nodeType: 'condition', config: { defaultFlowId: 'F9' } },
      { id: 'MOD', nodeType: 'approver', config: { mergeNode: true } },
      { id: 'MOD_GW', nodeType: 'condition', config: { defaultFlowId: 'F12' } },
      { id: 'E_OK', nodeType: 'end', config: {} },
      { id: 'E_STOP', nodeType: 'end', config: {} },
    ],
    edges: [
      { id: 'F1', source: 'S', target: 'A1' },
      { id: 'F2', source: 'A1', target: 'GW1' },
      { id: 'F3', source: 'GW1', target: 'A2', isDefault: true, branchId: 'b1' },
      { id: 'F4', source: 'GW1', target: 'MOD', condition: `${DOLLAR}{approvalResult == 'reject'}`, branchId: 'b2' },
      { id: 'F5', source: 'A2', target: 'GW2' },
      { id: 'F6', source: 'GW2', target: 'A3', isDefault: true, branchId: 'b3' },
      { id: 'F7', source: 'GW2', target: 'MOD', condition: `${DOLLAR}{approvalResult == 'reject'}`, branchId: 'b4' },
      { id: 'F8', source: 'A3', target: 'GW3' },
      { id: 'F9', source: 'GW3', target: 'E_OK', isDefault: true, branchId: 'b5' },
      { id: 'F10', source: 'GW3', target: 'MOD', condition: `${DOLLAR}{approvalResult == 'reject'}`, branchId: 'b6' },
      { id: 'F11', source: 'MOD', target: 'MOD_GW' },
      { id: 'F12', source: 'MOD_GW', target: 'A1', isDefault: true, branchId: 'b7' },
      { id: 'F13', source: 'MOD_GW', target: 'E_STOP', condition: `${DOLLAR}{approvalResult == 'terminate'}`, branchId: 'b8' },
    ],
  }
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
    expect(json.nodes.find(n => n.id === 'T1').config.assignee).toBe(`${DOLLAR}{initiator}`)
    w.unmount()
  })

  it('getXML 返回当前流程的 BPMN XML，包含 process id', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML })
    await new Promise(r => setTimeout(r, 50))
    const xml = w.vm.getXML()
    expect(xml).toContain('Process_T1')
    expect(xml).toContain('userTask')
    expect(xml).toContain(`${DOLLAR}{initiator}`)
    w.unmount()
  })

  it('getXML 前会提交打开的节点抽屉草稿配置', async () => {
    const w = mountDesigner({
      xml: SIMPLE_XML,
      formFieldCatalog: [
        { field: 'amount', label: '金额', required: false },
      ],
    })
    await new Promise(r => setTimeout(r, 50))

    await w.find('[data-node-type="approver"]').trigger('click')
    await w.vm.$nextTick()

    const writableInputs = w.findAll('[data-test="permission-writable"]')
    expect(writableInputs.length).toBeGreaterThan(0)
    await writableInputs[0].setValue(false)

    const xml = w.vm.getXML()
    const doc = parseBpmnXml(xml)
    const task = findElementsByLocalName(doc, 'userTask').find(t => t.getAttribute('id') === 'T1')
    const permissions = JSON.parse(getFlowableAttr(task, 'formFieldPermissions'))

    expect(permissions[0]).toMatchObject({
      field: 'amount',
      readable: true,
      writable: false,
      visible: true,
      editable: false,
    })

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

  it('业务应用进入时把旧任务表单自动迁移到具体页面并补齐字段权限', async () => {
    const w = mountDesigner({
      xml: LEGACY_FORM_XML,
      autoBindBusinessForm: true,
      defaultFormKey: APPLICATION_PAGE_FORM.formKey,
      formAssetOptions: [APPLICATION_PAGE_FORM],
    })
    await new Promise(r => setTimeout(r, 80))

    const xml = w.vm.getXML()
    const doc = parseBpmnXml(xml)
    const task = findElementsByLocalName(doc, 'userTask').find(item => item.getAttribute('id') === 'T1')
    const formRef = JSON.parse(getFlowableAttr(task, 'formRef'))
    const permissions = JSON.parse(getFlowableAttr(task, 'formFieldPermissions'))

    expect(getFlowableAttr(task, 'formKey')).toBe(APPLICATION_PAGE_FORM.formKey)
    expect(getFlowableAttr(task, 'formMode')).toBe('BUSINESS_OBJECT_FORM')
    expect(getFlowableAttr(task, 'formJson')).toBe(null)
    expect(formRef).toMatchObject({
      applicationId: '2089968247981060098',
      pageId: 'page_page',
      pageName: '测试',
      sourceFormKey: 'business_object_form',
    })
    expect(permissions.map(item => item.field)).toEqual(['fieldSlider', 'fieldInput'])

    w.unmount()
  })

  it('自动绑定数组表单时生成 v3 行字段和行操作权限', async () => {
    const w = mountDesigner({
      xml: LEGACY_FORM_XML,
      autoBindBusinessForm: true,
      defaultFormKey: ARRAY_APPLICATION_PAGE_FORM.formKey,
      formAssetOptions: [ARRAY_APPLICATION_PAGE_FORM],
    })
    await new Promise(r => setTimeout(r, 80))

    const xml = w.vm.getXML()
    const doc = parseBpmnXml(xml)
    const task = findElementsByLocalName(doc, 'userTask').find(item => item.getAttribute('id') === 'T1')
    const permissions = JSON.parse(getFlowableAttr(task, 'formFieldPermissions'))

    expect(permissions).toMatchObject({
      version: 3,
      arrays: [{ arrayKey: 'expenseItems', allowCreate: false, allowUpdate: true, allowDelete: false }],
    })
    expect(permissions.fields).toEqual(expect.arrayContaining([
      expect.objectContaining({ field: 'expenseItems', writable: true }),
      expect.objectContaining({ scope: 'array', arrayKey: 'expenseItems', itemField: 'amount', writable: true }),
    ]))

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

describe('dingFlowDesigner - 发起节点配置', () => {
  it('发起人变量固定展示，且不再展示节点表单配置', async () => {
    const w = mountDesigner()
    await w.find('[data-node-type="start"]').trigger('click')
    await w.vm.$nextTick()

    const text = w.text()
    expect(text).toContain('当前登录用户')
    expect(text).toContain('initiator')
    expect(text).toContain('流程表单请在顶部“更多设置 / 表单配置”中维护')
    expect(text).not.toContain('流程表单请在右侧流程信息')
    expect(text).not.toContain('发起人变量')
    expect(text).not.toContain('表单 Key')
    expect(text).not.toContain('表单 URL')
    w.unmount()
  })
})

describe('dingFlowDesigner - 网关分支配置', () => {
  it('新增条件网关后打开抽屉能看到两条可读分支', async () => {
    const w = mountDesigner()
    w.vm.designer.addNode('StartEvent_1', 'condition')
    await w.vm.$nextTick()

    await w.find('[data-node-type="condition"]').trigger('click')
    await w.vm.$nextTick()

    expect(w.text()).toContain('该网关共 2 条分支')
    expect(w.text()).toContain('分支 1')
    expect(w.text()).toContain('下游节点：分支1审批')
    expect(w.text()).not.toContain('→ Node_')
    w.unmount()
  })

  it('点击分支标签时只打开当前分支配置，点击网关节点恢复全部分支', async () => {
    const w = mountDesigner()
    w.vm.designer.addNode('StartEvent_1', 'condition')
    await w.vm.$nextTick()

    const branchHeaders = w.findAll('.branch-header')
    expect(branchHeaders).toHaveLength(2)

    await branchHeaders[0].trigger('click')
    await w.vm.$nextTick()

    expect(w.text()).toContain('正在配置 分支')
    expect(w.findAll('.condition-branch')).toHaveLength(1)

    await w.find('[data-node-type="condition"]').trigger('click')
    await w.vm.$nextTick()

    expect(w.text()).toContain('该网关共 2 条分支')
    expect(w.findAll('.condition-branch')).toHaveLength(2)

    w.unmount()
  })

  it('画布分支区域可继续添加第三条分支并聚焦新分支', async () => {
    const w = mountDesigner()
    const gatewayId = w.vm.designer.addNode('StartEvent_1', 'condition')
    await w.vm.$nextTick()

    expect(w.find('[data-test="canvas-add-branch"]').exists()).toBe(true)
    await w.find('[data-node-type="condition"]').trigger('click')
    await w.vm.$nextTick()
    expect(w.find('.condition-add-branch').exists()).toBe(false)

    await w.find('[data-test="canvas-add-branch"]').trigger('click')
    await w.vm.$nextTick()

    const outgoing = w.vm.designer.getOutgoingEdges(gatewayId)
    expect(outgoing).toHaveLength(3)
    expect(outgoing.filter(edge => edge.isDefault)).toHaveLength(1)
    expect(w.text()).toContain('正在配置 分支 3')
    expect(w.findAll('.condition-branch')).toHaveLength(1)
    expect(w.findAll('.branch-header')).toHaveLength(3)

    w.unmount()
  })

  it('添加分支按钮居中，默认分支标签避开按钮', async () => {
    const w = mountDesigner()
    const gatewayId = w.vm.designer.addNode('StartEvent_1', 'condition')
    await w.vm.$nextTick()

    const gatewayWrap = w.find(`.node-renderer-wrap[data-node-id="${gatewayId}"]`)
    const branchAdd = w.find('.branch-add-button-wrap')
    const defaultHeader = w.findAll('.branch-header')
      .find(item => item.text().includes('默认'))

    const gatewayLeft = leftOf(gatewayWrap)
    const branchAddLeft = leftOf(branchAdd)
    const defaultHeaderLeft = leftOf(defaultHeader)

    expect(branchAddLeft).toBe(gatewayLeft + 22)
    expect(Math.abs(defaultHeaderLeft - branchAddLeft)).toBeGreaterThan(48)

    w.unmount()
  })

  it('网关本身不显示普通添加按钮，分支节点可继续添加', async () => {
    const w = mountDesigner()
    w.vm.designer.addNode('StartEvent_1', 'condition')
    await w.vm.$nextTick()

    expect(w.findAll('.add-node-button-wrap')).toHaveLength(3)
    w.unmount()
  })

  it('复杂驳回回路中分支标签不与加号或彼此重叠', async () => {
    const w = mountDesigner()
    w.vm.designer.loadJson(reentryDecisionJson())
    await w.vm.$nextTick()

    expect(w.text()).toContain('驳回修改')
    expect(w.text()).toContain('终止流程')

    const headers = w.findAll('.branch-header').map(item => ({
      left: leftOf(item),
      top: topOf(item),
    }))
    const addButtons = w.findAll('.branch-add-button-wrap').map(item => ({
      left: leftOf(item),
      top: topOf(item),
    }))

    expect(headers.length).toBeGreaterThan(4)
    for (let i = 0; i < headers.length; i += 1) {
      for (let j = i + 1; j < headers.length; j += 1) {
        const sameArea = Math.abs(headers[i].left - headers[j].left) < 128
          && Math.abs(headers[i].top - headers[j].top) < 30
        expect(sameArea).toBe(false)
      }
    }
    for (const header of headers) {
      for (const button of addButtons) {
        const overlapped = Math.abs(header.left - button.left) < 64
          && Math.abs(header.top - button.top) < 30
        expect(overlapped).toBe(false)
      }
    }

    w.unmount()
  })
})
