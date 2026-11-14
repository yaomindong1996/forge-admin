import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BusinessFlowFormAssetSelect from '../BusinessFlowFormAssetSelect.vue'

const STUBS = {
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
}

const formAssets = [
  {
    formKey: 'common_form',
    formName: '低代码表单',
    formMode: 'BUSINESS_OBJECT_FORM',
    objectCode: 'contract',
    fieldCatalog: [{ field: 'title', label: '标题' }],
  },
  {
    formKey: 'common_form',
    formName: '代码表单',
    formMode: 'BUSINESS_CODE_FORM',
    providerKey: 'contractProvider',
    formUrl: '/business/contract',
    fieldCatalog: [{ field: 'amount', label: '金额' }],
  },
]

describe('businessFlowFormAssetSelect', () => {
  it('同 formKey 不同来源时按 formMode 和 providerKey 切换选择', async () => {
    const wrapper = mount(BusinessFlowFormAssetSelect, {
      props: {
        nodeForm: {
          formMode: 'BUSINESS_OBJECT_FORM',
          formKey: 'common_form',
          providerKey: '',
        },
        formAssets,
        showAllModes: true,
      },
      global: { stubs: STUBS },
    })

    const cards = wrapper.findAll('.asset-card')
    expect(cards).toHaveLength(2)
    expect(cards[0].classes()).toContain('selected')
    expect(cards[1].classes()).not.toContain('selected')

    await cards[1].trigger('click')

    const payload = wrapper.emitted('update')?.[0]?.[0]
    expect(payload).toMatchObject({
      formMode: 'BUSINESS_CODE_FORM',
      formKey: 'common_form',
      formName: '代码表单',
      providerKey: 'contractProvider',
      formUrl: '/business/contract',
    })

    await wrapper.setProps({ nodeForm: payload })

    const nextCards = wrapper.findAll('.asset-card')
    expect(nextCards[0].classes()).not.toContain('selected')
    expect(nextCards[1].classes()).toContain('selected')

    wrapper.unmount()
  })

  it('流程表单没有字段目录时不显示「0 个字段」', () => {
    const wrapper = mount(BusinessFlowFormAssetSelect, {
      props: {
        formAssets: [{ formKey: 'expense_form', formName: '报销申请表单', source: 'flowForm', fieldCatalog: [] }],
        nodeForm: {},
      },
      global: { stubs: STUBS },
    })
    expect(wrapper.text()).not.toContain('0 个字段')
    expect(wrapper.text()).toContain('选中后加载字段')
    wrapper.unmount()
  })
})
