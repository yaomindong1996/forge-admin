import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { useApprovalNodeMergeStore } from '@/stores/designer/approvalNodeMergeStore.js'
import { approvalMergeFixture } from '../../utils/__tests__/approval-merge-fixture.js'
import { useApprovalNodeMerge } from '../useApprovalNodeMerge.js'
import { useFlowDesigner } from '../useFlowDesigner.js'
import { useFlowHistory } from '../useFlowHistory.js'

function harness() {
  const pinia = createPinia()
  const readonly = ref(false)
  const drawerVisible = ref(false)
  const onMerged = vi.fn()
  const designer = useFlowDesigner(approvalMergeFixture())
  const history = useFlowHistory(designer.flowJson)
  let merge
  const wrapper = mount(defineComponent({ setup() {
    merge = useApprovalNodeMerge({ designer, history, readonly, drawerVisible, onMerged })
    return () => null
  } }), { global: { plugins: [pinia] } })
  return { wrapper, readonly, drawerVisible, onMerged, designer, history, merge, state: useApprovalNodeMergeStore(pinia) }
}

describe('合并编排边界', () => {
  it('确认后原子提交一条历史，并打开保留节点；Pinia 操作状态清理', () => {
    const h = harness()
    h.merge.openNodeMerge(h.designer.getNode('A1'))
    h.state.targetId = 'A2'
    h.merge.confirmNodeMerge()
    expect(h.designer.getNode('A1')).toBeNull()
    expect(h.history.undoStack.value).toHaveLength(1)
    expect(h.onMerged).toHaveBeenCalledWith(h.designer.getNode('A2'))
    expect(h.state.ownerId).toBe('')
    h.wrapper.unmount()
  })

  it('readonly 不允许打开/执行合并，属性草稿未确认时也不执行', () => {
    const h = harness()
    h.readonly.value = true
    h.merge.openNodeMerge(h.designer.getNode('A1'))
    expect(h.state.ownerId).toBe('')
    h.readonly.value = false
    h.drawerVisible.value = true
    h.merge.openNodeMerge(h.designer.getNode('A1'))
    h.state.targetId = 'A2'
    h.merge.confirmNodeMerge()
    expect(h.designer.getNode('A1')).not.toBeNull()
    expect(h.state.error).toContain('请先确认或取消')
    expect(h.history.undoStack.value).toHaveLength(0)
    h.wrapper.unmount()
  })

  it('图重载后关闭旧操作，不应用过期目标；卸载清理自身状态', () => {
    const h = harness()
    h.merge.openNodeMerge(h.designer.getNode('A1'))
    h.state.targetId = 'A2'
    h.designer.loadJson(approvalMergeFixture('parallel'))
    expect(h.state.ownerId).toBe('')
    h.merge.confirmNodeMerge()
    expect(h.designer.getNode('A1')).not.toBeNull()
    h.merge.openNodeMerge(h.designer.getNode('A1'))
    h.wrapper.unmount()
    expect(h.state.ownerId).toBe('')
  })

  it('应用前重新检查配置，不能吞掉新的权限差异', () => {
    const h = harness()
    h.merge.openNodeMerge(h.designer.getNode('A1'))
    h.state.targetId = 'A2'
    // 即使调用方绕过不可变编辑约定，最终合并仍会校验当前图，而不是旧候选列表。
    h.designer.getNode('A2').config.requireSignature = true
    h.merge.confirmNodeMerge()
    expect(h.state.error).toContain('审批配置不一致')
    expect(h.history.undoStack.value).toHaveLength(0)
    expect(h.designer.getNode('A1')).not.toBeNull()
    h.wrapper.unmount()
  })
})
