import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FlowCommentPhraseInput from '../FlowCommentPhraseInput.vue'

const listUsableCommentPhrases = vi.fn()
const listMyCommentPhrases = vi.fn()
const createCommentPhrase = vi.fn()
const deleteCommentPhrase = vi.fn()

vi.mock('@/api/flow', () => ({
  default: {
    listUsableCommentPhrases: (...args) => listUsableCommentPhrases(...args),
    listMyCommentPhrases: (...args) => listMyCommentPhrases(...args),
    createCommentPhrase: (...args) => createCommentPhrase(...args),
    deleteCommentPhrase: (...args) => deleteCommentPhrase(...args),
  },
}))

vi.mock('@/composables/useDict', () => ({
  useDict: () => ({
    dict: {
      value: {
        flow_comment_phrase_scene: [
          { label: '同意', value: 'APPROVE' },
          { label: '驳回', value: 'REJECT' },
          { label: '通用', value: 'ALL' },
        ],
      },
    },
  }),
}))

const STUBS = {
  'n-input': {
    props: ['value', 'placeholder'],
    emits: ['update:value'],
    template: '<textarea class="stub-input" :value="value" :placeholder="placeholder" @input="$emit(\'update:value\', $event.target.value)" />',
  },
  'n-modal': {
    props: ['show'],
    template: '<div v-if="show" class="stub-modal"><slot /></div>',
  },
  'n-select': true,
  'NButton': {
    template: '<button class="stub-save" @click="$attrs.onClick?.()"><slot /></button>',
  },
}

describe('flowCommentPhraseInput', () => {
  beforeEach(() => {
    listUsableCommentPhrases.mockReset()
    listMyCommentPhrases.mockReset()
    createCommentPhrase.mockReset()
    deleteCommentPhrase.mockReset()
    listUsableCommentPhrases.mockResolvedValue({
      data: [
        { id: 1, content: '同意', scene: 'APPROVE' },
        { id: 2, content: '已阅', scene: 'APPROVE' },
      ],
    })
    listMyCommentPhrases.mockResolvedValue({ data: [] })
    createCommentPhrase.mockResolvedValue({ code: 200, data: { id: 9 } })
    window.$message = { success: vi.fn(), warning: vi.fn(), error: vi.fn() }
  })

  it('渲染常用意见芯片并点选回填', async () => {
    const wrapper = mount(FlowCommentPhraseInput, {
      props: { modelValue: '', scene: 'APPROVE' },
      global: { stubs: STUBS },
    })
    await flushPromises()

    expect(listUsableCommentPhrases).toHaveBeenCalledWith({ scene: 'APPROVE' })
    expect(wrapper.text()).toContain('同意')
    expect(wrapper.text()).toContain('已阅')

    await wrapper.findAll('.phrase-chip')[0].trigger('click')
    expect(wrapper.emitted()['update:modelValue'][0]).toEqual(['同意'])
    wrapper.unmount()
  })

  it('当前意见不在列表中时可以存为常用', async () => {
    const wrapper = mount(FlowCommentPhraseInput, {
      props: { modelValue: '请尽快处理', scene: 'APPROVE' },
      global: { stubs: STUBS },
    })
    await flushPromises()

    const saveLink = wrapper.findAll('.phrase-link').find(item => item.text() === '存为常用')
    expect(saveLink).toBeTruthy()
    await saveLink.trigger('click')
    await flushPromises()

    expect(createCommentPhrase).toHaveBeenCalledWith({
      content: '请尽快处理',
      scene: 'APPROVE',
      ownerType: 1,
    })
    wrapper.unmount()
  })
})
