import { onScopeDispose } from 'vue'
import { registerPluginArtifact, reviewPluginTask } from '@/api/system/pluginTask'
import { usePluginArtifactStore } from '@/stores/plugin/artifactStore'
import { usePluginReviewStore } from '@/stores/plugin/reviewStore'
import { taskResponse } from './pluginTaskUtils'

// 审批与登记的冻结请求都由各自Pinia管理；动作只处理当前任务和页面生命周期。
export function usePluginApprovalActions(run, isCurrent, isDisposed) {
  const reviews = usePluginReviewStore()
  const artifacts = usePluginArtifactStore()
  onScopeDispose(() => {
    reviews.clear()
    artifacts.clear()
  })
  function reconcile(task) {
    if (!task || !isCurrent(task.id))
      return
    reviews.reconcile(task)
    artifacts.select(task)
  }
  function review() {
    const command = reviews.pending
    if (!command || !isCurrent(command.taskId))
      return
    return run(() => reviewPluginTask(command).then((response) => {
      taskResponse(response)
      if (!isDisposed() && reviews.pending?.requestId === command.requestId)
        reviews.clear()
      return response
    }))
  }
  function registerArtifact() {
    const command = artifacts.pending
    if (!command || !isCurrent(command.taskId))
      return
    return run(() => registerPluginArtifact(command))
  }
  return { reconcile, review, registerArtifact }
}
