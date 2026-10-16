import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'

vi.mock('@wangeditor/editor/dist/css/style.css', () => ({}))
vi.mock('@wangeditor/editor-for-vue', () => {
  const Editor = defineComponent({
    name: 'WangEditor',
    props: { modelValue: String },
    setup(props) {
      return () => h('div', { class: 'wang-stub' }, props.modelValue || '')
    },
  })
  const Toolbar = defineComponent({
    name: 'WangToolbar',
    setup() {
      return () => h('div', { class: 'toolbar-stub' })
    },
  })
  return { Editor, Toolbar }
})

import InlineRichText from '../InlineRichText.vue'

describe('InlineRichText click propagation', () => {
  it('只读态不拦截 click，供父级 shell 事件冒泡', async () => {
    const parentClick = vi.fn()
    const Parent = defineComponent({
      setup() {
        return () => h('div', { class: 'parent', onClick: parentClick }, [
          h(InlineRichText, { modelValue: '<h1>T</h1>', editable: false }),
        ])
      },
    })
    const wrapper = mount(Parent)
    await nextTick()
    await wrapper.find('.inline-rich-text').trigger('click')
    expect(parentClick).toHaveBeenCalled()
  })

  it('编辑态拦截 click，避免误选父区块', async () => {
    const parentClick = vi.fn()
    const Parent = defineComponent({
      setup() {
        return () => h('div', { class: 'parent', onClick: parentClick }, [
          h(InlineRichText, { modelValue: '<h1>T</h1>', editable: true }),
        ])
      },
    })
    const wrapper = mount(Parent)
    await nextTick()
    await wrapper.find('.inline-rich-text').trigger('click')
    expect(parentClick).not.toHaveBeenCalled()
  })
})
