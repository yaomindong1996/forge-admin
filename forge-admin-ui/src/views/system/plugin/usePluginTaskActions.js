import { onScopeDispose, ref } from 'vue'
import {
  cancelPluginTask,
  confirmPluginTask,
  getPluginTask,
} from '@/api/system/pluginTask'
import { taskResponse } from './pluginTaskUtils'
import { useLatestPluginRequest } from './useLatestPluginRequest'
import { usePluginApprovalActions } from './usePluginApprovalActions'
import { usePluginUpload } from './usePluginUpload'

export function usePluginTaskActions(refresh) {
  const detailRequest = useLatestPluginRequest(async id => taskResponse(await getPluginTask(id)), true)
  const visible = ref(false)
  const busy = ref(false)
  const actionError = ref('')
  const uploader = usePluginUpload(run, busy, actionError)
  let selectedId = ''
  let disposed = false
  onScopeDispose(() => {
    disposed = true
  })
  const approval = usePluginApprovalActions(run, id => id === selectedId, () => disposed)

  async function open(id = selectedId) {
    if (busy.value)
      return
    selectedId = id
    visible.value = true
    actionError.value = ''
    await detailRequest.run(id)
    if (!disposed)
      approval.reconcile(detailRequest.data.value)
  }
  function close() {
    if (busy.value)
      return
    visible.value = false
    detailRequest.cancel()
  }
  async function refreshDetail() {
    await open()
    if (!disposed && visible.value && !busy.value)
      await refresh()
  }
  async function run(action) {
    if (busy.value)
      return
    busy.value = true
    actionError.value = ''
    try {
      const task = taskResponse(await action())
      if (disposed)
        return
      detailRequest.cancel()
      detailRequest.error.value = ''
      detailRequest.data.value = task
      selectedId = task.id
      approval.reconcile(task)
      visible.value = true
      uploader.clear()
      await refresh()
    }
    catch (error) {
      if (!disposed)
        actionError.value = error?.message || '操作失败；结果不确定时请刷新任务列表'
    }
    finally {
      if (!disposed)
        busy.value = false
    }
  }
  return {
    task: detailRequest.data,
    visible,
    busy,
    actionError,
    pendingUpload: uploader.pendingUpload,
    open,
    close,
    upload: uploader.upload,
    retryUpload: uploader.retryUpload,
    refreshDetail,
    confirm: () => run(() => confirmPluginTask(detailRequest.data.value)),
    cancel: () => run(() => cancelPluginTask(detailRequest.data.value)),
    review: approval.review,
    registerArtifact: approval.registerArtifact,
    detailLoading: detailRequest.loading,
    detailError: detailRequest.error,
  }
}
