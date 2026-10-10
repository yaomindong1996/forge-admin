import { computed, reactive, ref } from 'vue'
import api from '@/api'
import { useAuthStore } from '@/store'
import { resolveApiErrorMessage } from '@/utils/flow-page'
import {
  canRemindTask,
  isRemindCooling,
  isRemindThrottledMessage,
  markRemindCooldown,
  remindTaskId,
} from '@/utils/flow-remind'
import { toast } from '@/utils/notify'
import { FLOW_PERMISSIONS } from '@/utils/permission'

// 冷却只放在本次会话内存里，列表和详情共用；刷新页面后以后端 10 分钟限流为准。
const cooldowns = reactive({})

/** "我发起的"列表和只读审批详情的催办入口。 */
export function useFlowRemind() {
  const authStore = useAuthStore()
  const remindingId = ref('')
  const remindAllowed = computed(() => authStore.hasPermission(FLOW_PERMISSIONS.remind))

  function showRemind(task) {
    return remindAllowed.value && canRemindTask(task)
  }

  function isRemindCoolingDown(task) {
    return isRemindCooling(cooldowns, remindTaskId(task))
  }

  function isReminding(task) {
    return Boolean(remindingId.value) && remindingId.value === remindTaskId(task)
  }

  async function remind(task) {
    const taskId = remindTaskId(task)
    if (!taskId || remindingId.value || isRemindCoolingDown(task)) return
    remindingId.value = taskId
    try {
      await api.remindFlowTask(taskId)
      markRemindCooldown(cooldowns, taskId)
      toast('已催办', { type: 'success' })
    }
    catch (error) {
      const message = resolveApiErrorMessage(error, '催办失败')
      if (isRemindThrottledMessage(message)) markRemindCooldown(cooldowns, taskId)
      toast(message, { type: 'warning' })
    }
    finally {
      remindingId.value = ''
    }
  }

  return { showRemind, isRemindCoolingDown, isReminding, remind }
}
