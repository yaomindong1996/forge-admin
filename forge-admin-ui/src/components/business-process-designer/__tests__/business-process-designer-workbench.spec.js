import { mount, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ActionAndApprovalNodeConfig from '../ActionAndApprovalNodeConfig.vue'

const businessAppApiMocks = vi.hoisted(() => ({
  ensureBusinessFlowStatusField: vi.fn(),
}))

vi.mock('@/api/business-app', () => businessAppApiMocks)

vi.mock('@/api/flow', () => ({
  default: {
    getModelDetail: vi.fn().mockResolvedValue({ data: { bpmnXml: '<definitions />' } }),
    createModel: vi.fn().mockResolvedValue({ data: { id: 'model-new', modelKey: 'created_approval' } }),
  },
}))
import flowApi from '@/api/flow'
import { createBusinessProcessSchema } from '../business-process-schema.js'
import BusinessProcessDesigner from '../BusinessProcessDesigner.vue'
import BusinessProcessNodeConfigDrawer from '../BusinessProcessNodeConfigDrawer.vue'
import BusinessProcessNodeRenderer from '../BusinessProcessNodeRenderer.vue'
import StartNodeConfig from '../StartNodeConfig.vue'

const objectRef = {
  objectId: '1900000000000001001',
  objectCode: 'sample_purchase_order',
}

const FORM_ASSET_STUBS = {
  FlowDesignPage: true,
  DingFlowViewer: true,
  TemplateVariableEditor: { template: '<div class="title-editor" />' },
  NModal: { template: '<div class="n-modal"><slot /></div>' },
  NSelect: { template: '<div class="n-select" />' },
  NButton: { template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>' },
  NTag: { template: '<span><slot /></span>' },
  NEmpty: { template: '<div class="n-empty"><slot /></div>' },
  'n-modal': { template: '<div class="n-modal"><slot /></div>' },
  'n-select': { template: '<div class="n-select" />' },
  'n-button': { template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>' },
  'n-tag': { template: '<span><slot /></span>' },
  'n-empty': { template: '<div class="n-empty"><slot /></div>' },
  'n-input': { template: '<input />' },
  'n-collapse': { template: '<div><slot /></div>' },
  'n-collapse-item': { template: '<div><slot /></div>' },
}

describe('business process node renderer', () => {
  it('renders business approval ports without BPMN element metadata', async () => {
    const node = {
      id: 'approval_purchase',
      type: 'APPROVAL',
      name: '采购审批',
      ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
      config: { flowModelKey: 'sample_purchase_order_approval' },
    }
    const wrapper = mount(BusinessProcessNodeRenderer, {
      props: {
        node,
        position: { x: 120, y: 80, width: 288, height: 92 },
        selected: true,
      },
    })

    expect(wrapper.text()).toContain('采购审批')
    expect(wrapper.findAll('[data-business-port]')).toHaveLength(4)
    expect(wrapper.attributes('data-node-id')).toBe('approval_purchase')
    expect(wrapper.attributes('data-bpmn-element-id')).toBeUndefined()

    await wrapper.trigger('click')
    expect(wrapper.emitted('select')[0][0]).toEqual(node)
  })
})

describe('structured business node configuration', () => {
  beforeEach(() => {
    businessAppApiMocks.ensureBusinessFlowStatusField.mockReset()
    flowApi.createModel.mockClear()
    flowApi.getModelDetail.mockClear()
    businessAppApiMocks.ensureBusinessFlowStatusField.mockResolvedValue({
      data: { fieldCode: 'flowStatus', fieldName: '流程状态' },
    })
  })

  it('switches start modes with a governed record source and no expression editor', async () => {
    const wrapper = mount(StartNodeConfig, {
      props: {
        type: 'START_MANUAL',
        config: { positions: ['ROW'], permission: 'ai:businessProcess:start' },
        fields: [{ fieldCode: 'status', fieldName: '状态' }],
      },
    })
    const triggerTypeSelect = wrapper.find('[data-start-type]')

    await triggerTypeSelect.setValue('START_EVENT')

    expect(wrapper.emitted('update:type')[0][0]).toBe('START_EVENT')
    expect(wrapper.emitted('update:recordIdSource')[0][0]).toBe('EVENT_RECORD')
    expect(wrapper.text()).not.toMatch(/JSON|SpEL|Java|SQL|Webhook/)
  })

  it('allows multiple start button visibility rules with AND/OR logic', async () => {
    const wrapper = mount(StartNodeConfig, {
      props: {
        type: 'START_MANUAL',
        config: {
          positions: ['ROW', 'DETAIL'],
          visibleCondition: {
            operator: 'AND',
            rules: [{ field: 'status', operator: 'EQ', value: 'DRAFT' }],
          },
        },
        fields: [
          { fieldCode: 'status', fieldName: '业务状态' },
          { fieldCode: 'priority', fieldName: '优先级' },
        ],
      },
    })

    expect(wrapper.findAll('.condition-row')).toHaveLength(1)
    await wrapper.find('.condition-logic-row button').trigger('click')
    expect(wrapper.findAll('.condition-row')).toHaveLength(2)
    await wrapper.find('.condition-logic-row select').setValue('OR')
    await wrapper.findAll('.condition-row')[1].find('select').setValue('priority')

    expect(wrapper.emitted('update:config').at(-1)[0]).toMatchObject({
      visibleCondition: {
        operator: 'OR',
        rules: [
          { field: 'status', operator: 'EQ', value: 'DRAFT' },
          { field: 'priority' },
        ],
      },
    })
  })

  it('keeps the manual start node name separate from its runtime button label', async () => {
    const wrapper = mount(StartNodeConfig, {
      props: {
        type: 'START_MANUAL',
        config: { positions: ['ROW'], buttonLabel: '提交审批' },
      },
    })

    const buttonLabel = wrapper.find('input[placeholder="例如：提交审批"]')
    expect(buttonLabel.element.value).toBe('提交审批')
    await buttonLabel.setValue('发起采购审批')

    expect(wrapper.emitted('update:config').at(-1)[0]).toMatchObject({
      buttonLabel: '发起采购审批',
    })
  })

  it('removes one start button visibility rule and keeps the remaining rules', async () => {
    const wrapper = mount(StartNodeConfig, {
      props: {
        type: 'START_MANUAL',
        config: {
          positions: ['ROW'],
          visibleCondition: {
            operator: 'AND',
            rules: [
              { field: 'status', operator: 'EQ', value: 'DRAFT' },
              { field: 'priority', operator: 'EQ', value: 'HIGH' },
            ],
          },
        },
        fields: [
          { fieldCode: 'status', fieldName: '业务状态' },
          { fieldCode: 'priority', fieldName: '优先级' },
        ],
      },
    })

    await wrapper.findAll('[data-condition-remove]')[0].trigger('click')

    expect(wrapper.findAll('.condition-row')).toHaveLength(1)
    expect(wrapper.emitted('update:config').at(-1)[0]).toMatchObject({
      visibleCondition: {
        operator: 'AND',
        rules: [{ field: 'priority', operator: 'EQ', value: 'HIGH' }],
      },
    })
  })

  it('clears the complete start button visibility condition from either delete entry point', async () => {
    const createWrapper = () => mount(StartNodeConfig, {
      props: {
        type: 'START_MANUAL',
        config: {
          positions: ['ROW'],
          visibleCondition: {
            operator: 'AND',
            rules: [{ field: 'status', operator: 'EQ', value: 'DRAFT' }],
          },
        },
        fields: [{ fieldCode: 'status', fieldName: '业务状态' }],
      },
    })
    const deleteWrapper = createWrapper()

    await deleteWrapper.find('[data-condition-remove]').trigger('click')

    expect(deleteWrapper.findAll('.condition-row')).toHaveLength(0)
    expect(deleteWrapper.emitted('update:config').at(-1)[0]).not.toHaveProperty('visibleCondition')
    expect(deleteWrapper.text()).toContain('未设置条件时，按钮始终显示')

    await deleteWrapper.find('[data-condition-add]').trigger('click')
    expect(deleteWrapper.findAll('.condition-row')).toHaveLength(1)
    expect(deleteWrapper.emitted('update:config').at(-1)[0]).toMatchObject({
      visibleCondition: {
        operator: 'AND',
        rules: [{ field: '', operator: 'EQ', source: 'record' }],
      },
    })

    const clearWrapper = createWrapper()
    await clearWrapper.find('[data-condition-clear]').trigger('click')

    expect(clearWrapper.findAll('.condition-row')).toHaveLength(0)
    expect(clearWrapper.emitted('update:config').at(-1)[0]).not.toHaveProperty('visibleCondition')
  })

  it('allows a manual start action on the edit form separately from detail', async () => {
    const wrapper = mount(StartNodeConfig, {
      props: {
        type: 'START_MANUAL',
        config: { positions: ['ROW'] },
        fields: [{ fieldCode: 'status', fieldName: '业务状态' }],
      },
    })
    const formLabel = wrapper.findAll('.inline-check').find(item => item.text().includes('编辑表单操作'))
    await formLabel.find('input').setValue(true)
    expect(wrapper.emitted('update:config').at(-1)[0].positions).toContain('FORM')
  })

  it('applies editable start templates while preserving the manual configuration path', async () => {
    const wrapper = mount(StartNodeConfig, {
      props: {
        type: 'START_MANUAL',
        config: {},
        fields: [{ fieldCode: 'dueDate', fieldName: '到期日期' }],
      },
    })

    expect(wrapper.findAll('[data-start-template]')).toHaveLength(4)
    await wrapper.find('[data-start-template="EVENT_STATUS_CHANGED"]').trigger('click')

    expect(wrapper.emitted('update:type').at(-1)[0]).toBe('START_EVENT')
    expect(wrapper.emitted('update:recordIdSource').at(-1)[0]).toBe('EVENT_RECORD')
    expect(wrapper.emitted('update:config').at(-1)[0]).toMatchObject({ eventType: 'STATUS_CHANGED' })

    await wrapper.setProps({ type: 'START_EVENT', config: { eventType: 'STATUS_CHANGED' } })
    await wrapper.find('select:not([data-start-type])').setValue('RECORD_UPDATED')
    expect(wrapper.emitted('update:config').at(-1)[0]).toMatchObject({ eventType: 'RECORD_UPDATED' })
  })

  it('applies action templates as governed config that remains editable', async () => {
    const wrapper = shallowMount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'action_update',
          type: 'ACTION',
          name: '更新采购单',
          ports: [],
          config: {},
        },
        objectCode: 'sample_purchase_order',
        businessActions: [{ actionCode: 'adjust_quantity', actionName: '调整库存数量' }],
      },
      global: {
        stubs: {
          NModal: { template: '<div><slot /></div>' },
        },
      },
    })

    expect(wrapper.findAll('[data-action-template]')).toHaveLength(4)
    await wrapper.find('[data-action-template="UPDATE_STATUS"]').trigger('click')
    expect(wrapper.emitted('update:config').at(-1)[0]).toMatchObject({
      actionType: 'UPDATE_RECORD',
      objectCode: 'sample_purchase_order',
      fieldMappings: [{ field: 'status', valueSource: 'CONSTANT', value: '' }],
    })

    await wrapper.find('[data-action-template="ADJUST_NUMBER"]').trigger('click')
    expect(wrapper.emitted('update:config').at(-1)[0]).toMatchObject({
      actionType: 'BUSINESS_ACTION',
      businessActionCode: '',
    })
  })

  it('shows business object names while retaining object codes as option values', async () => {
    const wrapper = mount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'action_target', type: 'ACTION', name: '更新', ports: [],
          config: { actionType: 'UPDATE_RECORD' },
        },
        objectCode: 'order',
        objectName: '订单',
        objects: [{ objectId: '2002', objectCode: 'customer', objectName: '客户', configKey: 'customer_runtime' }],
      },
      global: { stubs: FORM_ASSET_STUBS },
    })

    const options = wrapper.findAll('option')
    expect(options.map(option => option.text())).toContain('客户')
    expect(options.find(option => option.text() === '客户').attributes('value')).toBe('customer')
    expect(options.map(option => option.text())).not.toContain('customer')
    await wrapper.findAll('.config-field select')[1].setValue('customer')
    expect(wrapper.emitted('update:config').at(-1)[0]).toMatchObject({
      objectCode: 'customer',
      targetObjectId: '2002',
      targetConfigKey: 'customer_runtime',
    })
    wrapper.unmount()
  })

  it('opens the real flow designer for a selected deployed approval model', async () => {
    const wrapper = shallowMount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'approval_purchase',
          type: 'APPROVAL',
          name: '采购审批',
          ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
          config: {
            flowModelKey: 'sample_purchase_order_approval',
            versionPolicy: 'PINNED_AT_APPLICATION_PUBLISH',
          },
        },
        objectCode: 'sample_purchase_order',
        flowModels: [
          {
            modelId: '1900000000000002001',
            modelKey: 'sample_purchase_order_approval',
            modelName: '采购审批流程',
            status: 1,
            deploymentId: 'deployment-1',
          },
          {
            modelId: '1900000000000002002',
            modelKey: 'unpublished_approval',
            modelName: '未发布审批流程',
            status: 0,
            deploymentId: null,
          },
        ],
      },
      global: {
        stubs: {
          FlowDesignPage: true,
          DingFlowViewer: true,
          TemplateVariableEditor: { template: '<div class="title-editor" />' },
          NModal: { template: '<div class="n-modal"><slot /></div>' },
          NSelect: { template: '<div class="n-select" />' },
          NButton: { template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>' },
          NTag: { template: '<span><slot /></span>' },
          NEmpty: { template: '<div><slot /></div>' },
        },
      },
    })

    await wrapper.find('.open-flow-designer-button').trigger('click')

    expect(wrapper.emitted('openFlowDesigner')[0][0]).toMatchObject({
      modelId: '1900000000000002001',
      modelKey: 'sample_purchase_order_approval',
    })
    expect(wrapper.emitted('openFlowDesigner')).toHaveLength(1)
    expect(wrapper.find('.flow-designer-fullscreen').exists()).toBe(false)
    expect(wrapper.text()).toContain('审批人、会签、驳回和字段权限在真实流程设计器中维护')
    expect(wrapper.text()).toContain('在本页设计')
    expect(wrapper.text()).not.toMatch(/SpEL|Java|SQL|Webhook/)
  })

  it('defaults the current object form as the approval task form', async () => {
    const wrapper = mount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'approval_purchase',
          type: 'APPROVAL',
          name: '采购审批',
          ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
          config: {},
        },
        objectCode: 'sample_purchase_order',
        objectName: '采购单',
        formAssets: [
          {
            formKey: 'purchase_form',
            formName: '采购申请单',
            formMode: 'BUSINESS_OBJECT_FORM',
          },
        ],
        fields: [{ fieldCode: 'name', fieldName: '名称' }],
      },
      global: { stubs: FORM_ASSET_STUBS },
    })

    const latest = wrapper.emitted('update:config')?.at(-1)?.[0]
    expect(latest).toMatchObject({
      formAsset: {
        formKey: 'purchase_form',
        formName: '采购申请单',
      },
    })
    expect(wrapper.text()).toContain('采购申请单')
    expect(wrapper.findAll('.asset-card')).toHaveLength(1)
    expect(wrapper.find('.asset-card').classes()).toContain('selected')
    wrapper.unmount()
  })

  it('keeps the approval form unbound when the governed catalog is empty', async () => {
    const wrapper = mount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'approval_empty_catalog',
          type: 'APPROVAL',
          name: '审批',
          ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
          config: {},
        },
        objectCode: 'sample_purchase_order',
        objectName: '采购单',
        formAssets: [],
      },
      global: { stubs: FORM_ASSET_STUBS },
    })

    expect(wrapper.emitted('update:config') || []).not.toSatisfy(events => events.some(([config]) =>
      config?.formAsset?.formKey === 'sample_purchase_order',
    ))
    expect(wrapper.findAll('.asset-card')).toHaveLength(0)
    expect(wrapper.text()).toContain('当前对象还没有可绑定的表单')
    wrapper.unmount()
  })

  it('clears a stale low-code form reference when the catalog is empty', async () => {
    const wrapper = mount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'approval_stale_form',
          type: 'APPROVAL',
          name: '审批',
          ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
          config: {
            formAsset: {
              formKey: 'deleted_form',
              formMode: 'BUSINESS_OBJECT_FORM',
            },
          },
        },
        objectCode: 'sample_purchase_order',
        formAssets: [],
      },
      global: { stubs: FORM_ASSET_STUBS },
    })

    expect(wrapper.emitted('update:config')?.at(-1)?.[0]).toMatchObject({ formAsset: {} })
    wrapper.unmount()
  })

  it('keeps a stable application page form reference when the catalog is temporarily empty', async () => {
    const wrapper = mount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'approval_application_form',
          type: 'APPROVAL',
          name: '审批',
          ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
          config: {
            formAsset: {
              formKey: 'app_2089968247981060098_page_page_page_form_form_form',
              formMode: 'BUSINESS_OBJECT_FORM',
              applicationId: '2089968247981060098',
              pageId: 'page_page',
              formName: '测试',
            },
          },
        },
        objectCode: 'bo_2089974506884993026',
        formAssets: [],
      },
      global: { stubs: FORM_ASSET_STUBS },
    })

    expect(wrapper.emitted('update:config') || []).not.toSatisfy(events => events.some(([config]) =>
      config?.formAsset?.formKey === '',
    ))
    expect(wrapper.find('.asset-card').exists()).toBe(false)
    wrapper.unmount()
  })

  it('does not replace a stable application page form with an unrelated catalog asset', async () => {
    const wrapper = mount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'approval_application_form_catalog_mismatch',
          type: 'APPROVAL',
          name: '审批',
          ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
          config: {
            formAsset: {
              formKey: 'app_2089968247981060098_page_page_page_form_form_form',
              formMode: 'BUSINESS_OBJECT_FORM',
              applicationId: '2089968247981060098',
              pageId: 'page_page',
              formName: '测试',
            },
          },
        },
        objectCode: 'bo_2089974506884993026',
        formAssets: [{ formKey: 'app_other_page_page_form_form_other', formName: '其他页面' }],
      },
      global: { stubs: FORM_ASSET_STUBS },
    })

    expect(wrapper.emitted('update:config') || []).not.toSatisfy(events => events.some(([config]) =>
      config?.formAsset?.formKey === 'app_other_page_page_form_form_other',
    ))
    wrapper.unmount()
  })

  it('creates a business-form approval model bound to the object form', async () => {
    const wrapper = mount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'approval_purchase',
          type: 'APPROVAL',
          name: '采购审批',
          ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
          config: {},
        },
        objectCode: 'sample_purchase_order',
        objectName: '采购单',
        formAssets: [{ formKey: 'purchase_form', formName: '采购申请单', formMode: 'BUSINESS_OBJECT_FORM' }],
      },
      global: { stubs: FORM_ASSET_STUBS },
    })

    const createButton = wrapper.findAll('button').find(button => button.text().includes('新建并设计'))
    await createButton.trigger('click')
    await Promise.resolve()
    await Promise.resolve()

    expect(flowApi.createModel).toHaveBeenCalledWith(expect.objectContaining({
      formType: 'business',
    }))
    expect(flowApi.createModel.mock.calls.at(-1)[0].formJson).toContain('purchase_form')
    expect(wrapper.emitted('update:config').at(-1)[0]).toMatchObject({
      flowModelId: 'model-new',
      flowModelKey: 'created_approval',
      flowModelName: '采购单审批',
    })
    expect(wrapper.emitted('openFlowDesigner')).toHaveLength(1)
    expect(wrapper.emitted('openFlowDesigner')[0][0]).toMatchObject({
      modelId: 'model-new',
      modelKey: 'created_approval',
      businessFormKey: 'purchase_form',
    })
    expect(wrapper.find('.flow-designer-fullscreen').exists()).toBe(false)
    expect(wrapper.text()).toContain('待发布，可继续设计')
    wrapper.unmount()
  })

  it('provisions and binds an independent flow status instead of reusing business status', async () => {
    const wrapper = mount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'approval_status',
          type: 'APPROVAL',
          name: '业务审批',
          ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
          config: { statusField: 'status' },
        },
        objectId: '1900000000000001001',
        objectCode: 'sample_purchase_order',
        objectName: '采购单',
        fields: [{ fieldCode: 'status', fieldName: '业务状态' }],
      },
      global: { stubs: FORM_ASSET_STUBS },
    })

    expect(wrapper.text()).toContain('一键添加流程状态字段')
    expect(wrapper.text()).toContain('业务自己的“状态”字段不会被流程修改')

    const provisionButton = wrapper.findAll('button')
      .find(button => button.text().includes('一键添加流程状态字段'))
    await provisionButton.trigger('click')
    await Promise.resolve()

    expect(businessAppApiMocks.ensureBusinessFlowStatusField).toHaveBeenCalledWith('1900000000000001001')
    expect(wrapper.emitted('update:config').at(-1)[0]).toMatchObject({ statusField: 'flowStatus' })
    expect(wrapper.emitted('refreshFields').at(-1)[0]).toMatchObject({ fieldCode: 'flowStatus' })
    wrapper.unmount()
  })

  it('recognizes an existing snake-case flow status field', async () => {
    const wrapper = mount(ActionAndApprovalNodeConfig, {
      props: {
        node: {
          id: 'approval_existing_status', type: 'APPROVAL', name: '审批',
          ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'], config: {},
        },
        objectCode: 'order',
        fields: [{ fieldCode: 'flow_status', fieldName: '流程状态' }],
      },
      global: { stubs: FORM_ASSET_STUBS },
    })

    expect(wrapper.text()).not.toContain('一键添加流程状态字段')
    wrapper.unmount()
  })

  it('persists the default form onto the node without a confirm click', async () => {
    const wrapper = mount(BusinessProcessNodeConfigDrawer, {
      props: {
        visible: true,
        node: {
          id: 'approval_purchase',
          type: 'APPROVAL',
          name: '采购审批',
          ports: ['APPROVED', 'REJECTED', 'CANCELED', 'FAILED'],
          config: {},
        },
        objectCode: 'sample_purchase_order',
        objectName: '采购单',
        formAssets: [{ formKey: 'purchase_form', formName: '采购申请单' }],
      },
      global: { stubs: FORM_ASSET_STUBS },
    })

    await Promise.resolve()
    expect(wrapper.emitted('save')?.at(-1)?.[0].config.formAsset).toMatchObject({
      formKey: 'purchase_form',
      formName: '采购申请单',
    })
    wrapper.unmount()
  })

  it('renames an untouched start node when switching trigger type but keeps a custom name', async () => {
    const stubs = {
      'NButton': { template: '<button><slot /></button>' },
      'NModal': { template: '<div class="node-config-modal-stub"><slot /></div>' },
      'Modal': { template: '<div class="node-config-modal-stub"><slot /></div>' },
      'n-button': { template: '<button><slot /></button>' },
      'n-modal': { template: '<div class="node-config-modal-stub"><slot /></div>' },
    }
    const start = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }).nodes[0]
    const wrapper = mount(BusinessProcessNodeConfigDrawer, { props: { visible: true, node: start }, global: { stubs } })
    wrapper.findComponent(StartNodeConfig).vm.$emit('update:type', 'START_EVENT')
    await Promise.resolve()
    expect(wrapper.emitted('save').at(-1)[0]).toMatchObject({ type: 'START_EVENT', name: '事件触发' })
    wrapper.unmount()

    const custom = mount(BusinessProcessNodeConfigDrawer, { props: { visible: true, node: { ...start, name: '提交采购' } }, global: { stubs } })
    custom.findComponent(StartNodeConfig).vm.$emit('update:type', 'START_SCHEDULE')
    await Promise.resolve()
    expect(custom.emitted('save').at(-1)[0]).toMatchObject({ type: 'START_SCHEDULE', name: '提交采购' })
    custom.unmount()
  })

  it('only offers date fields for scheduled scans', () => {
    const wrapper = mount(StartNodeConfig, {
      props: {
        type: 'START_SCHEDULE',
        config: {},
        fields: [
          { fieldCode: 'reason', fieldName: '报销事由', fieldType: 'TEXT', dataType: 'varchar' },
          { fieldCode: 'amount', fieldName: '报销金额', fieldType: 'MONEY', dataType: 'decimal' },
          { fieldCode: 'occurDate', fieldName: '发生日期', fieldType: 'DATE', dataType: 'date' },
          { fieldCode: 'createTime', fieldName: '创建时间', fieldType: 'DATETIME', dataType: 'datetime' },
        ],
      },
    })
    const dueSelect = wrapper.findAll('select').find(select => select.text().includes('选择主业务对象日期字段'))
    expect(dueSelect.findAll('option').map(option => option.text()).slice(1)).toEqual(['发生日期', '创建时间'])
    wrapper.unmount()
  })

  it('describes event filters instead of button visibility for event triggers', () => {
    const wrapper = mount(StartNodeConfig, { props: { type: 'START_EVENT', config: { eventType: 'RECORD_CREATED' } } })
    expect(wrapper.text()).toContain('未设置条件时，每次事件都会触发流程')
    expect(wrapper.text()).toContain('添加过滤条件')
    expect(wrapper.text()).not.toContain('按钮始终显示')
    wrapper.unmount()
  })

  it('routes start and execution nodes to dedicated structured panels', async () => {
    const start = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef }).nodes[0]
    const wrapper = mount(BusinessProcessNodeConfigDrawer, {
      props: { visible: true, node: start },
      global: {
        stubs: {
          NButton: { template: '<button><slot /></button>' },
          NModal: { template: '<div class="node-config-modal-stub"><slot /></div>' },
          Modal: { template: '<div class="node-config-modal-stub"><slot /></div>' },
          'n-button': { template: '<button><slot /></button>' },
          'n-modal': { template: '<div class="node-config-modal-stub"><slot /></div>' },
        },
      },
    })
    expect(wrapper.find('[data-node-config="panel"]').exists()).toBe(true)
    expect(wrapper.findComponent(StartNodeConfig).exists()).toBe(true)

    await wrapper.setProps({
      node: {
        id: 'action_update',
        type: 'ACTION',
        name: '更新采购单',
        ports: [],
        config: { actionType: 'UPDATE_RECORD' },
      },
    })
    expect(wrapper.findComponent(ActionAndApprovalNodeConfig).exists()).toBe(true)
  })
})

describe('business process designer workbench', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('keeps a removed start button condition deleted after the node drawer saves', async () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    schema.nodes[0].config = {
      positions: ['ROW'],
      visibleCondition: {
        operator: 'AND',
        rules: [{ field: 'status', operator: 'EQ', value: 'DRAFT' }],
      },
    }
    const wrapper = mount(BusinessProcessDesigner, {
      props: { schema, autoSaveDelay: 60000 },
      global: {
        stubs: {
          BusinessProcessNodeConfigDrawer: {
            props: ['node'],
            emits: ['save'],
            methods: {
              saveWithoutCondition() {
                this.$emit('save', {
                  ...this.node,
                  config: { positions: ['ROW'] },
                })
              },
            },
            template: '<button data-save-without-condition @click="saveWithoutCondition">保存节点</button>',
          },
        },
      },
    })

    await wrapper.find('[data-save-without-condition]').trigger('click')

    const latestSchema = wrapper.emitted('update:schema').at(-1)[0]
    const start = latestSchema.nodes.find(node => node.id === 'start_manual')
    expect(start.config).toEqual({ positions: ['ROW'] })
    expect(start.config).not.toHaveProperty('visibleCondition')
    wrapper.unmount()
  })

  it('auto-saves dirty drafts, supports explicit validation and surfaces hash conflicts', async () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    const wrapper = mount(BusinessProcessDesigner, {
      props: {
        schema,
        autoSaveDelay: 20,
        saveState: 'idle',
      },
      global: {
        stubs: {
          BusinessProcessNodeConfigDrawer: true,
        },
      },
    })

    await wrapper.find('[data-node-type="ACTION"]').trigger('click')
    await vi.advanceTimersByTimeAsync(25)

    expect(wrapper.emitted('dirtyChange').at(-1)[0]).toBe(true)
    expect(wrapper.emitted('save').at(-1)[1]).toMatchObject({ reason: 'auto' })

    await wrapper.find('[data-designer-action="validate"]').trigger('click')
    expect(wrapper.emitted('validate')).toHaveLength(1)

    await wrapper.setProps({ saveState: 'conflict' })
    expect(wrapper.text()).toContain('草稿已被其他人更新')
    expect(wrapper.find('[data-designer-action="reload"]').exists()).toBe(true)
  })

  it('warns the browser before leaving a dirty draft', async () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    const wrapper = mount(BusinessProcessDesigner, {
      props: { schema, autoSaveDelay: 60000 },
      global: { stubs: { BusinessProcessNodeConfigDrawer: true } },
    })

    await wrapper.find('[data-node-type="ACTION"]').trigger('click')
    const event = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(event)

    expect(event.defaultPrevented).toBe(true)
    wrapper.unmount()
  })

  it('binds the object form when inserting an approval node', async () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    const wrapper = mount(BusinessProcessDesigner, {
      props: {
        schema,
        autoSaveDelay: 60000,
        objectName: '采购单',
        formAssets: [{ formKey: 'purchase_form', formName: '采购申请单' }],
      },
      global: { stubs: { BusinessProcessNodeConfigDrawer: true } },
    })

    await wrapper.find('[data-node-type="APPROVAL"]').trigger('click')

    const latestSchema = wrapper.emitted('update:schema').at(-1)[0]
    const approval = latestSchema.nodes.find(node => node.type === 'APPROVAL')
    expect(approval.config.formAsset).toMatchObject({
      formKey: 'purchase_form',
      formName: '采购申请单',
    })
    wrapper.unmount()
  })

  it('does not invent a form key when inserting an approval node with an empty catalog', async () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    const wrapper = mount(BusinessProcessDesigner, {
      props: {
        schema,
        autoSaveDelay: 60000,
        objectName: '采购单',
        formAssets: [],
      },
      global: { stubs: { BusinessProcessNodeConfigDrawer: true } },
    })

    await wrapper.find('[data-node-type="APPROVAL"]').trigger('click')

    const latestSchema = wrapper.emitted('update:schema').at(-1)[0]
    const approval = latestSchema.nodes.find(node => node.type === 'APPROVAL')
    expect(approval.config.formAsset?.formKey).toBeFalsy()
    expect(latestSchema.dependencies.formAssets).not.toContain('sample_purchase_order')
    wrapper.unmount()
  })

  it('renders selectable validation details and copies the complete issue', async () => {
    const originalClipboard = Object.getOwnPropertyDescriptor(window.navigator, 'clipboard')
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(window.navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    })
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    const issue = {
      level: 'ERROR',
      code: 'FORM_ASSET_UNAVAILABLE',
      message: '任务表单「missing_form」不存在、未发布或不属于当前应用',
      nodeId: 'start_manual',
      fieldPath: 'nodes[0].config.formAsset.formKey',
      suggestion: '重新选择当前对象的可用表单',
    }
    const wrapper = mount(BusinessProcessDesigner, {
      props: {
        schema,
        serverValidation: {
          issues: [
            {
              ...issue,
              message: '任务表单「missing_form」不存在、未发布或不属于当前应用',
              nodeId: null,
              fieldPath: 'dependencies.formAssets',
            },
            issue,
          ],
        },
      },
      global: { stubs: { BusinessProcessNodeConfigDrawer: true } },
    })

    const issueCard = wrapper.find('.issue-item')
    expect(wrapper.findAll('.issue-item')).toHaveLength(1)
    expect(issueCard.element.tagName).toBe('DIV')
    expect(issueCard.text()).toContain('missing_form')
    expect(issueCard.text()).toContain('FORM_ASSET_UNAVAILABLE')
    expect(issueCard.text()).toContain('nodes[0].config.formAsset.formKey')

    await issueCard.find('[data-issue-copy]').trigger('click')
    await Promise.resolve()

    expect(writeText).toHaveBeenCalledWith(expect.stringContaining('任务表单「missing_form」'))
    expect(writeText).toHaveBeenCalledWith(expect.stringContaining('重新选择当前对象的可用表单'))
    expect(wrapper.emitted('locateIssue')).toBeUndefined()

    await issueCard.find('[data-issue-locate]').trigger('click')
    expect(wrapper.emitted('locateIssue')?.[0]?.[0]).toEqual(issue)

    wrapper.unmount()
    if (originalClipboard)
      Object.defineProperty(window.navigator, 'clipboard', originalClipboard)
    else
      delete window.navigator.clipboard
  })

  it('drags a palette node onto a concrete canvas insertion edge', async () => {
    const schema = createBusinessProcessSchema({ processCode: 'purchase_submit', objectRef })
    const wrapper = mount(BusinessProcessDesigner, {
      props: { schema, autoSaveDelay: 60000 },
      global: { stubs: { BusinessProcessNodeConfigDrawer: true } },
    })
    const transferStore = new Map()
    const dataTransfer = {
      effectAllowed: 'none',
      dropEffect: 'none',
      setData: (key, value) => transferStore.set(key, value),
      getData: key => transferStore.get(key) || '',
    }

    const paletteItem = wrapper.find('[data-node-type="ACTION"]')
    expect(paletteItem.attributes('draggable')).toBe('true')
    await paletteItem.trigger('dragstart', { dataTransfer })
    expect(dataTransfer.effectAllowed).toBe('copy')
    expect([...transferStore.values()]).toContain('ACTION')

    const canvas = wrapper.find('.business-process-canvas')
    await canvas.trigger('dragover', { clientX: 300, clientY: 220, dataTransfer })
    expect(wrapper.find('[data-business-insert-edge]').classes()).toContain('is-active-target')
    await canvas.trigger('drop', { clientX: 300, clientY: 220, dataTransfer })

    const latestSchema = wrapper.emitted('update:schema').at(-1)[0]
    expect(latestSchema.nodes.some(node => node.type === 'ACTION')).toBe(true)
    expect(wrapper.findComponent(BusinessProcessNodeConfigDrawer).props('visible')).toBe(true)
  })
})
