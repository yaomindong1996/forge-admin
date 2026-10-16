import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import AiSearch from '../AiSearch.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {},
    params: {},
    path: '/',
    fullPath: '/',
    name: 'ai-search-schema-pick-test',
  }),
}))
vi.mock('@/api/business-app', () => ({
  queryBusinessRecordSelector: vi.fn(),
}))

const StubAiForm = defineComponent({
  name: 'AiForm',
  props: {
    value: { type: Object, default: () => ({}) },
    schema: { type: Array, default: () => [] },
    context: { type: Object, default: () => ({}) },
  },
  emits: ['update:value'],
  setup(_props, { expose, emit }) {
    expose({
      validate: async () => true,
      reset: () => emit('update:value', {}),
    })
    return () => h('div', { class: 'stub-ai-form' })
  },
})

describe('AiSearch schema pick', () => {
  it('getFormData / setFormData 只保留查询 schema 字段，丢弃交互污染键', async () => {
    const wrapper = mount(AiSearch, {
      props: {
        schema: [
          { field: 'fieldInput', label: 'E2E_input', type: 'input' },
          { field: 'qtyField', label: 'E2E_number', type: 'number' },
        ],
      },
      global: {
        stubs: {
          AiForm: StubAiForm,
          'n-space': true,
          'n-button': true,
          'n-icon': true,
        },
      },
    })

    wrapper.vm.setFormData({
      fieldInput: 'ok',
      qtyField: 1,
      cmp_e2e_textarea_x: 'SV_input',
      noise: 'nope',
    })
    await nextTick()

    expect(wrapper.vm.getFormData()).toEqual({
      fieldInput: 'ok',
      qtyField: 1,
    })
  })
})
