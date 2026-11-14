import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import { request } from '@/utils/http'
import UserSelectModal from '../UserSelectModal.vue'

vi.mock('@/utils/http', () => ({ request: { get: vi.fn() } }))

const users = {
  admin: { id: 1, username: 'admin', realName: '超级管理员' },
  test33: { id: 70, username: 'test33', realName: 'test33' },
}

const passthrough = name => defineComponent({ name, inheritAttrs: false, setup: (_, { slots }) => () => h('div', [slots.default?.(), slots.footer?.()]) })
const DataTable = defineComponent({
  name: 'NDataTable',
  props: ['data', 'checkedRowKeys'],
  emits: ['update:checked-row-keys'],
  setup: () => () => h('div', { class: 'table-stub' }),
})
const Input = defineComponent({
  name: 'NInput',
  props: ['value'],
  emits: ['update:value', 'keyup'],
  setup: (props, { emit }) => () => h('input', { value: props.value, onInput: e => emit('update:value', e.target.value) }),
})
const Button = defineComponent({ name: 'NButton', emits: ['click'], setup: (_, { slots, emit }) => () => h('button', { onClick: () => emit('click') }, slots.default?.()) })

function mountModal() {
  request.get.mockImplementation((url, { params } = {}) => {
    if (url === '/system/org/tree')
      return Promise.resolve({ code: 200, data: [] })
    const record = users[params?.keyword]
    return Promise.resolve({ code: 200, data: { records: record ? [record] : Object.values(users), total: 1 } })
  })
  return mount(UserSelectModal, {
    props: { show: false, multiple: true },
    global: {
      stubs: {
        'n-modal': passthrough('NModal'),
        'n-space': passthrough('NSpace'),
        'n-tree-select': passthrough('NTreeSelect'),
        'n-pagination': passthrough('NPagination'),
        'n-input': Input,
        'n-button': Button,
        'n-data-table': DataTable,
      },
    },
  })
}

async function search(wrapper, keyword) {
  await wrapper.find('input').setValue(keyword)
  await wrapper.findAll('button').find(b => b.text().includes('搜索')).trigger('click')
  await flushPromises()
}

describe('userSelectModal multiple selection', () => {
  it('keeps users checked under an earlier search when confirming', async () => {
    const wrapper = mountModal()
    await wrapper.setProps({ show: true })
    await flushPromises()
    const table = () => wrapper.findComponent(DataTable)

    await search(wrapper, 'admin')
    table().vm.$emit('update:checked-row-keys', ['1'])
    await search(wrapper, 'test33')
    table().vm.$emit('update:checked-row-keys', ['1', '70'])
    await flushPromises()

    expect(wrapper.text()).toContain('已选 2 人：超级管理员、test33')
    await wrapper.findAll('button').find(b => b.text().includes('确定')).trigger('click')
    expect(wrapper.emitted('confirm').at(-1)[0].map(u => u.username)).toEqual(['admin', 'test33'])
    wrapper.unmount()
  })
})
