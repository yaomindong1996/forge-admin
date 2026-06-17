import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AddNodeButton from '../AddNodeButton.vue'
import AddNodePopover from '../AddNodePopover.vue'

describe('addNodePopover', () => {
  it('渲染所有 3 个分组', () => {
    const wrapper = mount(AddNodePopover)
    expect(wrapper.text()).toContain('审批流')
    expect(wrapper.text()).toContain('分支')
    expect(wrapper.text()).toContain('高级')
  })

  it('渲染所有 9 个节点类型按钮', () => {
    const wrapper = mount(AddNodePopover)
    expect(wrapper.findAll('button').length).toBe(9)
  })

  it('approver 按钮存在并带 data-type', () => {
    const wrapper = mount(AddNodePopover)
    const approverBtn = wrapper.find('button[data-type="approver"]')
    expect(approverBtn.exists()).toBe(true)
    expect(approverBtn.text()).toContain('审批人')
  })

  it('allowTypes 限制可见类型', () => {
    const wrapper = mount(AddNodePopover, {
      props: { allowTypes: ['approver', 'carbonCopy'] },
    })
    expect(wrapper.findAll('button').length).toBe(2)
    expect(wrapper.text()).toContain('审批流')
    expect(wrapper.text()).not.toContain('条件分支')
  })
})

describe('addNodeButton', () => {
  it('按 position 绝对定位', () => {
    const wrapper = mount(AddNodeButton, {
      props: { position: { x: 100, y: 50 }, size: 24 },
    })
    const div = wrapper.find('.add-node-button-wrap')
    expect(div.attributes('style')).toContain('left: 88px')
    expect(div.attributes('style')).toContain('top: 38px')
  })

  it('点击按钮后 popover 显示', async () => {
    const wrapper = mount(AddNodeButton, { props: { position: { x: 0, y: 0 } } })
    expect(wrapper.findComponent(AddNodePopover).exists()).toBe(false)
    await wrapper.find('button').trigger('click')
    expect(wrapper.findComponent(AddNodePopover).exists()).toBe(true)
  })

  it('readonly 模式禁用按钮', () => {
    const wrapper = mount(AddNodeButton, {
      props: { position: { x: 0, y: 0 }, readonly: true },
    })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })
})
