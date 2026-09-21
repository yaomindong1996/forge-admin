import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { defineComponent, ref } from 'vue'
import ApproverConfig from '../ApproverConfig.vue'

const STUBS = {
  'n-tabs': { template: '<div class="n-tabs"><slot /></div>' },
  'n-tab-pane': { template: '<section class="n-tab-pane"><slot /></section>' },
  'n-tag': { template: '<span class="n-tag"><slot /></span>' },
  'n-form-item': { template: '<label><slot /></label>' },
  'n-empty': { template: '<div class="n-empty" />' },
  'n-button': {
    emits: ['click'],
    template: '<button type="button" v-bind="$attrs" @click="$emit(\'click\', $event)"><slot /></button>',
  },
  'n-collapse': { template: '<div><slot /></div>' },
  'n-collapse-item': { template: '<div><slot /></div>' },
  'n-input': {
    props: ['value'],
    emits: ['update:value'],
    template: '<input :value="value" @input="$emit(\'update:value\', $event.target.value)" />',
  },
  'n-checkbox': {
    props: ['checked', 'disabled'],
    emits: ['update:checked'],
    template: '<input type="checkbox" :checked="checked" :disabled="disabled" @change="$emit(\'update:checked\', $event.target.checked)" />',
  },
  'BasicConfig': true,
  'ApproverAssigneeForm': true,
  'ApprovalDutyConfig': true,
  'MultiInstanceConfig': true,
  'PermissionConfig': true,
  'OverdueReminderConfig': true,
  'ListenerConfig': true,
  'n-alert': { template: '<div class="n-alert"><slot /></div>' },
}

function mountApproverConfig(options = {}) {
  const Parent = defineComponent({
    components: { ApproverConfig },
    setup() {
      const node = ref({
        id: 'task_1',
        nodeType: 'approver',
        name: '审批',
        config: {
          formType: 'dynamic',
          formMode: 'BUSINESS_OBJECT_FORM',
          formKey: 'main_form',
          formName: '主表单',
          providerKey: '',
          formFieldPermissions: [
            {
              field: 'amount',
              label: '金额',
              visible: false,
              editable: false,
              readable: false,
              writable: false,
              required: false,
            },
          ],
        },
      })
      if (options.nodeConfig) {
        node.value.config = {
          ...node.value.config,
          ...options.nodeConfig,
        }
      }
      const formAssetOptions = options.formAssetOptions || [
        {
          formKey: 'main_form',
          formName: '主表单',
          formMode: 'BUSINESS_OBJECT_FORM',
          fieldCatalog: [
            { field: 'amount', label: '金额' },
            { field: 'reason', label: '申请原因' },
            { field: 'remark', label: '备注', required: true },
          ],
        },
      ]
      function updateConfig(patch) {
        node.value = {
          ...node.value,
          config: {
            ...node.value.config,
            ...patch,
          },
        }
      }
      return {
        node,
        formAssetOptions,
        updateConfig,
      }
    },
    template: `
      <ApproverConfig
        :node="node"
        :form-asset-options="formAssetOptions"
        @update:config="updateConfig"
      />
    `,
  })

  return mount(Parent, {
    global: { stubs: STUBS },
  })
}

describe('approverConfig', () => {
  it('点击当前表单资产时保留已有字段权限', async () => {
    const wrapper = mountApproverConfig()

    await wrapper.find('.asset-card').trigger('click')

    expect(wrapper.vm.node.config.formFieldPermissions).toEqual([
      {
        field: 'amount',
        fieldCode: 'amount',
        label: '金额',
        visible: false,
        editable: false,
        readable: false,
        writable: false,
        required: false,
      },
      {
        field: 'reason',
        fieldCode: 'reason',
        label: '申请原因',
        visible: true,
        editable: true,
        readable: true,
        writable: true,
        required: false,
      },
      {
        field: 'remark',
        fieldCode: 'remark',
        label: '备注',
        visible: true,
        editable: true,
        readable: true,
        writable: true,
        required: true,
      },
    ])

    wrapper.unmount()
  })

  it('旧 BPMN 只有 formKey 时也能回显应用表单资产', async () => {
    const wrapper = mountApproverConfig({
      nodeConfig: {
        formMode: '',
        formKey: 'purchase_form',
        formName: '',
        providerKey: '',
        formUrl: '',
        formRef: {},
        formFieldPermissions: [],
      },
      formAssetOptions: [
        {
          formKey: 'purchase_form',
          formName: '采购单审批表单',
          formMode: 'BUSINESS_CODE_FORM',
          providerKey: 'samplePurchaseOrder',
          formUrl: '/business/purchase-order-test',
          fieldCatalog: [],
        },
      ],
    })

    const asset = wrapper.find('.asset-card')
    expect(asset.classes()).toContain('selected')

    await asset.trigger('click')

    expect(wrapper.vm.node.config.formMode).toBe('BUSINESS_CODE_FORM')
    expect(wrapper.vm.node.config.formName).toBe('采购单审批表单')
    expect(wrapper.vm.node.config.providerKey).toBe('samplePurchaseOrder')
    expect(wrapper.vm.node.config.formUrl).toBe('/business/purchase-order-test')

    wrapper.unmount()
  })

  it('流程内置表单不会默认写成业务对象表单模式', async () => {
    const wrapper = mountApproverConfig({
      nodeConfig: {
        formMode: '',
        formKey: '',
        formName: '',
        providerKey: '',
        formUrl: '',
        formRef: {},
        formFieldPermissions: [],
      },
      formAssetOptions: [
        {
          formKey: 'leave_form',
          formName: '请假单',
          source: 'flowForm',
          fieldCatalog: [{ field: 'days', label: '天数' }],
        },
      ],
    })

    await wrapper.find('.asset-card').trigger('click')

    expect(wrapper.vm.node.config.formKey).toBe('leave_form')
    expect(wrapper.vm.node.config.formMode).toBe('')
    expect(wrapper.vm.node.config.formType).toBe('dynamic')
    wrapper.unmount()
  })
})
