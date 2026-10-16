import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import PageWidgetRenderer from '../PageWidgetRenderer.vue'

vi.mock('@/utils', () => ({ request: vi.fn() }))

const VMdEditorStub = defineComponent({
  name: 'VMdEditor',
  props: {
    disabled: { type: Boolean, default: false },
    mode: { type: String, default: 'editable' },
    modelValue: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('textarea', {
      class: 'v-md-stub',
      disabled: props.disabled,
      'data-mode': props.mode,
      value: props.modelValue,
      onInput: e => emit('update:modelValue', e.target.value),
    })
  },
})

describe('PageWidgetRenderer readonly 透传', () => {
  it('markdown 在 readonly prop 为 true 时进入 preview 且 disabled', async () => {
    const wrapper = mount(PageWidgetRenderer, {
      props: {
        componentKey: 'markdown',
        readonly: true,
        propsData: {
          content: '# hello',
          previewMode: 'split',
        },
      },
      global: {
        stubs: {
          VMdEditor: VMdEditorStub,
        },
        config: { warnHandler: () => {} },
      },
    })

    await vi.waitFor(() => expect(wrapper.find('.v-md-stub').exists()).toBe(true))
    const editor = wrapper.find('.v-md-stub')
    expect(editor.attributes('disabled')).toBeDefined()
    expect(editor.attributes('data-mode')).toBe('preview')

    await editor.setValue('changed')
    expect(wrapper.emitted('update:propsData')).toBeFalsy()
    wrapper.unmount()
  })

  it('transfer 在 readonly prop 为 true 时 disabled', () => {
    const wrapper = mount(PageWidgetRenderer, {
      props: {
        componentKey: 'transfer',
        readonly: true,
        propsData: {
          options: [{ label: 'A', value: 'a' }],
          value: [],
        },
      },
      global: {
        stubs: {
          NTransfer: {
            name: 'NTransfer',
            props: { disabled: Boolean },
            template: '<div class="n-transfer-stub" :data-disabled="disabled" />',
          },
        },
        config: { warnHandler: () => {} },
      },
    })

    expect(wrapper.find('.n-transfer-stub').attributes('data-disabled')).toBe('true')
    wrapper.unmount()
  })

  it('markdown 可通过 inject aiFormReadonly 进入只读', async () => {
    const wrapper = mount(PageWidgetRenderer, {
      props: {
        componentKey: 'markdown',
        propsData: { content: '# hello', previewMode: 'split' },
      },
      global: {
        stubs: { VMdEditor: VMdEditorStub },
        provide: { aiFormReadonly: ref(true) },
        config: { warnHandler: () => {} },
      },
    })

    await vi.waitFor(() => expect(wrapper.find('.v-md-stub').exists()).toBe(true))
    expect(wrapper.find('.v-md-stub').attributes('data-mode')).toBe('preview')
    wrapper.unmount()
  })
})
