import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 多个嵌入式设计器不能共享一次合并操作；ownerId 仅标识本地组件，不是业务权限。 */
export const useApprovalNodeMergeStore = defineStore('designer-approval-node-merge', () => {
  const ownerId = ref('')
  const sourceId = ref('')
  const targetId = ref(null)
  const blockedReason = ref('')
  const error = ref('')

  function open(owner, source, blocked = '') {
    ownerId.value = owner
    sourceId.value = source
    targetId.value = null
    blockedReason.value = blocked
    error.value = ''
  }

  function close(owner) {
    if (ownerId.value !== owner)
      return
    ownerId.value = ''
    sourceId.value = ''
    targetId.value = null
    blockedReason.value = ''
    error.value = ''
  }

  return { ownerId, sourceId, targetId, blockedReason, error, open, close }
})
