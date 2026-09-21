import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { printTemplates } from '@/api/print'
import PrintTemplatePolicyConfig from '../PrintTemplatePolicyConfig.vue'

vi.mock('@/api/print', () => ({ printTemplates: vi.fn() }))

const NSelect = {
  name: 'NSelect',
  props: ['value', 'options', 'multiple', 'disabled'],
  emits: ['update:value'],
  template: '<div class="select-stub" />',
}

beforeEach(() => {
  vi.resetAllMocks()
  printTemplates.mockResolvedValue({
    data: {
      records: [
        { id: '11', templateName: '采购审批单', templateCode: 'purchase', source: { sourceType: 'CODE', formKey: 'purchase_form', objectCode: 'purchase' } },
        { id: '12', templateName: '其他单据', templateCode: 'other', source: { sourceType: 'CODE', formKey: 'other_form', objectCode: 'other' } },
      ],
    },
  })
})

describe('printTemplatePolicyConfig', () => {
  it('只列出当前表单来源模板并保留历史模板标识', async () => {
    const wrapper = mount(PrintTemplatePolicyConfig, {
      props: {
        config: { formMode: 'BUSINESS_CODE_FORM', formKey: 'purchase_form', printTemplatePolicy: 'RESTRICT', printTemplateIds: ['11', '99'] },
        formAsset: { applicationId: '7', objectCode: 'purchase', formKey: 'purchase_form', formMode: 'BUSINESS_CODE_FORM' },
      },
      global: {
        stubs: {
          NSelect,
          NSpin: { template: '<div><slot /></div>' },
          NAlert: { template: '<div><slot /></div>' },
          NFormItem: { template: '<label><slot /></label>' },
        },
      },
    })
    await flushPromises()

    expect(printTemplates).toHaveBeenCalledWith({ applicationId: '7', pageNum: 1, pageSize: 100 })
    expect(wrapper.vm.templateOptions).toEqual([
      { label: '采购审批单（purchase）', value: '11' },
      { label: '模板 99', value: '99' },
    ])

    wrapper.vm.updateIds(['11', '12', 'bad'])
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:config').at(-1)[0]).toEqual({ printTemplateIds: ['11', '12'] })
    wrapper.unmount()
  })

  it('切回继承模式时清空节点模板子集', async () => {
    const wrapper = mount(PrintTemplatePolicyConfig, {
      props: {
        config: { printTemplatePolicy: 'RESTRICT', printTemplateIds: ['11'] },
        formAsset: null,
      },
      global: { stubs: { NSelect, NSpin: true, NAlert: true, NFormItem: { template: '<label><slot /></label>' } } },
    })
    wrapper.vm.updatePolicy('INHERIT')
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:config')[0][0]).toEqual({ printTemplatePolicy: 'INHERIT', printTemplateIds: [] })
    wrapper.unmount()
  })
})
