import { flushPromises, mount } from '@vue/test-utils'
import { NButton } from 'naive-ui'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useApprovalNodeMergeStore } from '@/stores/designer/approvalNodeMergeStore.js'
import { approvalMergeFixture } from '../../utils/__tests__/approval-merge-fixture.js'
import ApprovalNodeMergeModal from '../ApprovalNodeMergeModal.vue'

const modalStub = { props: ['show'], template: '<section v-if="show"><slot /><slot name="footer" /></section>' }
let pinia
beforeEach(() => {
  pinia = createPinia()
  setActivePinia(pinia)
})
function mountModal(ownerId = 'editor-A', flow = approvalMergeFixture()) {
  return mount(ApprovalNodeMergeModal, { props: { ownerId, flow }, global: { plugins: [pinia], stubs: { Modal: modalStub } } })
}

describe('合并审批确认界面', () => {
  it('展示真实来源/前置与保留节点，明确草稿生效边界；确认只发事件', async () => {
    const state = useApprovalNodeMergeStore()
    state.open('editor-A', 'A1')
    const flow = approvalMergeFixture()
    const wrapper = mountModal('editor-A', flow)
    await flushPromises()
    expect(state.targetId).toBe('A2')
    expect(wrapper.text()).toContain('公共审批1')
    expect(wrapper.text()).toContain('公共审批2')
    expect(wrapper.text()).toContain('前置节点：分支2经办')
    expect(wrapper.text()).toContain('已有运行实例不会随之合并')
    await wrapper.findAllComponents(NButton).find(button => button.text() === '确认合并').trigger('click')
    expect(wrapper.emitted('confirm')).toHaveLength(1)
    expect(flow.nodes.find(node => node.id === 'A1')).toBeTruthy()
    wrapper.unmount()
  })

  it('配置不一致禁用确认，说明差异字段', async () => {
    const state = useApprovalNodeMergeStore()
    const flow = approvalMergeFixture()
    flow.nodes.find(node => node.id === 'A2').config.requireSignature = true
    state.open('editor-A', 'A1')
    const wrapper = mountModal('editor-A', flow)
    await flushPromises()
    expect(wrapper.text()).toContain('requireSignature')
    expect(state.targetId).toBeNull()
    expect(wrapper.findAllComponents(NButton).find(button => button.text() === '确认合并').props('disabled')).toBe(true)
    wrapper.unmount()
  })

  it('取消不修改图；未确认属性会阻止合并', async () => {
    const state = useApprovalNodeMergeStore()
    state.open('editor-A', 'A1', '请先确认或取消当前节点属性')
    const wrapper = mountModal()
    expect(wrapper.text()).toContain('请先确认或取消当前节点属性')
    expect(wrapper.findAllComponents(NButton).find(button => button.text() === '确认合并').props('disabled')).toBe(true)
    await wrapper.findAllComponents(NButton).find(button => button.text() === '取消').trigger('click')
    expect(state.ownerId).toBe('')
    expect(wrapper.emitted('confirm')).toBeUndefined()
    wrapper.unmount()
  })

  it('另一个设计器的隐藏弹窗不能清空当前选择或关闭当前操作', async () => {
    const hidden = mountModal('editor-B')
    const state = useApprovalNodeMergeStore()
    state.open('editor-A', 'A1')
    const active = mountModal('editor-A')
    await flushPromises()
    expect(state.targetId).toBe('A2')
    await hidden.setProps({ flow: approvalMergeFixture('parallel') })
    expect(state.targetId).toBe('A2')
    state.close('editor-B')
    expect(state.ownerId).toBe('editor-A')
    expect(hidden.text()).toBe('')
    hidden.unmount()
    active.unmount()
  })
})
