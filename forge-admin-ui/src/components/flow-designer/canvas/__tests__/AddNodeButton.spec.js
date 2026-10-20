import { mount } from '@vue/test-utils'
import { NPopover } from 'naive-ui'
import { describe, expect, it } from 'vitest'
import AddNodeButton from '../AddNodeButton.vue'
import AddNodePopover from '../AddNodePopover.vue'
import BranchAddButton from '../BranchAddButton.vue'

describe('addNodePopover', () => {
  it('常用节点前置，高级入口默认折叠', () => {
    const wrapper = mount(AddNodePopover)
    expect(wrapper.text()).toContain('添加节点')
    expect(wrapper.findAll('[data-type]')).toHaveLength(5)
    expect(wrapper.find('.add-node-advanced-toggle').attributes('aria-expanded')).toBe('false')
    expect(wrapper.text()).not.toContain('服务任务')
  })

  it('展开后保留全部 9 个节点类型并可选中高级节点', async () => {
    const wrapper = mount(AddNodePopover)
    await wrapper.find('.add-node-advanced-toggle').trigger('click')
    expect(wrapper.findAll('[data-type]')).toHaveLength(9)
    await wrapper.find('[data-type="script"]').trigger('click')
    expect(wrapper.emitted('select')[0]).toEqual(['script'])
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
    expect(wrapper.find('[data-type="carbonCopy"]').exists()).toBe(true)
    expect(wrapper.find('.add-node-advanced-toggle').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('条件分支')
  })
})

describe('addNodeButton', () => {
  it('浮层紧贴触发点并显示箭头，不使用大间距分离布局', () => {
    const wrapper = mount(AddNodeButton, { props: { position: { x: 100, y: 50 } } })
    const popover = wrapper.findComponent(NPopover)
    expect(popover.props('themeOverrides').space).toBe('10px')
    expect(popover.props('showArrow')).toBe(true)
    wrapper.unmount()
  })
  it('按 position 绝对定位', () => {
    const wrapper = mount(AddNodeButton, {
      props: { position: { x: 100, y: 50 } },
    })
    const div = wrapper.find('.add-node-button-wrap')
    // 新样式用 translate(-50%, 0) 居中，left 直接等于 position.x
    expect(div.attributes('style')).toContain('left: 100px')
    expect(div.attributes('style')).toContain('top: 50px')
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

  it('视口变化会关闭旧锚点浮层，防止留在屏幕之外', async () => {
    const wrapper = mount(AddNodeButton, { props: { position: { x: 100, y: 100 } } })
    await wrapper.find('button').trigger('click')
    expect(wrapper.find('button').attributes('aria-expanded')).toBe('true')
    window.dispatchEvent(new Event('resize'))
    await wrapper.vm.$nextTick()
    expect(wrapper.find('button').attributes('aria-expanded')).toBe('false')
    wrapper.unmount()
  })
})

describe('branchAddButton', () => {
  it('按 position 绝对定位并触发 click', async () => {
    const wrapper = mount(BranchAddButton, {
      props: { position: { x: 120, y: 88 } },
    })
    expect(wrapper.find('.branch-add-button-wrap').attributes('style')).toContain('left: 120px')
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('readonly 模式禁用按钮', () => {
    const wrapper = mount(BranchAddButton, {
      props: { position: { x: 0, y: 0 }, readonly: true },
    })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })
})
