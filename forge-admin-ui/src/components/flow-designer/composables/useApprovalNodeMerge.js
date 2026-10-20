import { onBeforeUnmount, useId, watch } from 'vue'
import { useApprovalNodeMergeStore } from '@/stores/designer/approvalNodeMergeStore.js'

export function useApprovalNodeMerge({ designer, history, readonly, drawerVisible, onMerged }) {
  const mergeOwnerId = useId()
  const state = useApprovalNodeMergeStore()

  function openNodeMerge(node) {
    if (readonly.value || node?.nodeType !== 'approver')
      return
    state.open(mergeOwnerId, node.id, drawerVisible.value
      ? '请先确认或取消当前节点属性，再重新发起合并；未确认的属性不会被丢弃。'
      : '')
  }

  function confirmNodeMerge() {
    if (state.ownerId !== mergeOwnerId || readonly.value)
      return
    if (drawerVisible.value || state.blockedReason) {
      state.error = '请先确认或取消当前节点属性'
      return
    }
    try {
      const targetId = state.targetId
      designer.mergeApprovalNodes(state.sourceId, targetId, history.snapshot)
      state.close(mergeOwnerId)
      onMerged(designer.getNode(targetId))
    }
    catch (error) {
      state.error = error.message || '合并未完成，请重新检查节点配置'
    }
  }

  // 模型重载、撤销或外部修改后，旧选择必须失效，不能套用到另一个图。
  watch(designer.flowJson, () => state.close(mergeOwnerId), { flush: 'sync' })
  watch(readonly, () => state.close(mergeOwnerId), { flush: 'sync' })
  onBeforeUnmount(() => state.close(mergeOwnerId))
  return { mergeOwnerId, openNodeMerge, confirmNodeMerge }
}
