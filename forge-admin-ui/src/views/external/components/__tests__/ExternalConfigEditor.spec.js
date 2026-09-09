import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import ExternalConfigEditor from '../ExternalConfigEditor.vue'

function lastEmit(wrapper) {
  const events = wrapper.emitted('update:modelValue') || []
  return events.at(-1)?.[0]
}

function findButton(wrapper, label) {
  return wrapper.findAll('button').find(button => button.text().includes(label))
}

describe('external config editor', () => {
  it('shows add button even when hint is empty', async () => {
    const wrapper = mount(ExternalConfigEditor, {
      props: { modelValue: '', mode: 'key-value' },
    })
    await nextTick()

    expect(findButton(wrapper, '添加一项')).toBeTruthy()
  })

  it('emits JSON object after filling a key-value row', async () => {
    const wrapper = mount(ExternalConfigEditor, {
      props: { modelValue: '', mode: 'key-value' },
    })
    await nextTick()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('appKey')
    await inputs[1].setValue('secret-1')

    expect(lastEmit(wrapper)).toBe(JSON.stringify({ appKey: 'secret-1' }))
  })

  it('keeps add button usable after inserting another row', async () => {
    const wrapper = mount(ExternalConfigEditor, {
      props: { modelValue: JSON.stringify({ appKey: 'a' }), mode: 'key-value' },
    })
    await nextTick()

    await findButton(wrapper, '添加一项').trigger('click')
    await nextTick()

    expect(wrapper.findAll('.editor-row')).toHaveLength(2)
  })

  it('serializes mapping rows without requiring raw JSON', async () => {
    const wrapper = mount(ExternalConfigEditor, {
      props: { modelValue: '', mode: 'mapping' },
    })
    await nextTick()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('keyword')
    await inputs[1].setValue('key')

    expect(lastEmit(wrapper)).toBe(JSON.stringify({ keyword: 'key' }))
  })
})
